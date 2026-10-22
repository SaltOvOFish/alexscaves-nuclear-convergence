#!/usr/bin/env node
/**
 * Ponder 示意图 (.nbt) 检查工具
 *
 * 用法：
 *   node ponder_nbt_tool.mjs <file.nbt> [--size 7] [--map]
 *
 * 可选参数：
 *   --size N   期望的底板尺寸（对应 configureBasePlate(_, _, N)），检查 x/z 是否超出
 *   --map      输出每一层 y 的俯视图
 *
 * 退出码：0 = 通过（可能带警告），1 = 有错误
 */
import fs from 'node:fs';
import zlib from 'node:zlib';

class Reader {
  constructor(buf) { this.b = buf; this.p = 0; }
  u1() { return this.b[this.p++]; }
  i1() { return this.b.readInt8(this.p++); }
  i2() { const v = this.b.readInt16BE(this.p); this.p += 2; return v; }
  u2() { const v = this.b.readUInt16BE(this.p); this.p += 2; return v; }
  i4() { const v = this.b.readInt32BE(this.p); this.p += 4; return v; }
  i8() { const v = this.b.readBigInt64BE(this.p); this.p += 8; return v; }
  f4() { const v = this.b.readFloatBE(this.p); this.p += 4; return v; }
  f8() { const v = this.b.readDoubleBE(this.p); this.p += 8; return v; }
  str() { const n = this.u2(); const s = this.b.toString('utf8', this.p, this.p + n); this.p += n; return s; }
  payload(type) {
    switch (type) {
      case 1: return this.i1();
      case 2: return this.i2();
      case 3: return this.i4();
      case 4: return this.i8();
      case 5: return this.f4();
      case 6: return this.f8();
      case 7: { const n = this.i4(); const v = this.b.subarray(this.p, this.p + n); this.p += n; return v; }
      case 8: return this.str();
      case 9: {
        const et = this.u1(); const n = this.i4(); const items = [];
        for (let i = 0; i < n; i++) items.push(this.payload(et));
        return { __list: et, items };
      }
      case 10: {
        const obj = {};
        for (;;) { const t = this.u1(); if (t === 0) break; const name = this.str(); obj[name] = this.payload(t); }
        return obj;
      }
      case 11: { const n = this.i4(); const a = []; for (let i = 0; i < n; i++) a.push(this.i4()); return a; }
      case 12: { const n = this.i4(); const a = []; for (let i = 0; i < n; i++) a.push(this.i8()); return a; }
      default: throw new Error(`未知 tag 类型 ${type}（偏移 ${this.p}），文件可能已损坏`);
    }
  }
}

const list = (v) => (v && v.__list ? v.items : Array.isArray(v) ? v : []);
const chunk = (arr, n) => arr.reduce((a, v, i) => (i % n ? a[a.length - 1].push(v) : a.push([v]), a), []);

function parse(file) {
  let raw = fs.readFileSync(file);
  const gzipped = raw[0] === 0x1f && raw[1] === 0x8b;
  if (gzipped) raw = zlib.gunzipSync(raw);
  const r = new Reader(raw);
  const rootType = r.u1();
  if (rootType !== 10) throw new Error(`根标签不是 compound（type=${rootType}）`);
  const rootName = r.str();
  return { root: r.payload(10), gzipped, rootName, bytes: raw.length };
}

const args = process.argv.slice(2);
const files = [];
let wantSize = null;
let showMap = false;
let showStates = false;
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--size') { wantSize = Number(args[++i]); continue; }
  if (args[i] === '--map') { showMap = true; continue; }
  if (args[i] === '--states') { showStates = true; continue; }
  if (args[i].startsWith('--')) continue;
  files.push(args[i]);
}
if (!files.length) {
  console.log('用法: node ponder_nbt_tool.mjs <file.nbt> [更多文件...] [--size N] [--map]');
  process.exit(1);
}

let errors = 0, warns = 0;
const err = (m) => { errors++; console.log('  [错误] ' + m); };
const warn = (m) => { warns++; console.log('  [警告] ' + m); };
const ok = (m) => console.log('  [通过] ' + m);

for (const file of files) {
  console.log('\n=== ' + file);
  let parsed;
  try { parsed = parse(file); } catch (e) { err('解析失败：' + e.message); continue; }
  const { root, gzipped } = parsed;

  gzipped ? ok('gzip 压缩（Ponder 要求）') : err('未压缩！Ponder 读取的是 gzip NBT，请用结构方块导出或 gzip 压缩');

  for (const key of ['size', 'palette', 'blocks', 'entities']) {
    if (!(key in root)) { err(`缺少根标签 "${key}"`); }
  }
  if (!('size' in root) || !('palette' in root) || !('blocks' in root)) { continue; }
  if (root.__malformed) err('根 compound 没有正确结束（缺 TAG_End）');

  const size = list(root.size);
  console.log(`  尺寸: ${size.join(' × ')}`);
  const palette = list(root.palette);
  const records = list(root.blocks);
  const entities = list(root.entities ?? {});
  console.log(`  palette: ${palette.length} 项 | 方块记录: ${records.length} | 实体: ${entities.length}`);

  // 索引方块
  const grid = new Map();
  for (const rec of records) {
    const p = list(rec.pos);
    if (p.length !== 3) { err('存在非法的 pos'); continue; }
    const state = palette[rec.state];
    if (!state) { err(`方块记录引用了不存在的 palette 下标 ${rec.state}`); continue; }
    grid.set(p.join(','), { p, state, nbt: rec.nbt });
  }

  const inside = (p) => p[0] >= 0 && p[0] < size[0] && p[1] >= 0 && p[1] < size[1] && p[2] >= 0 && p[2] < size[2];
  const outside = [...grid.values()].filter((b) => !inside(b.p));
  if (outside.length) err(`${outside.length} 个方块坐标超出 size 范围`);

  // y 层与空气
  const nonAir = [...grid.values()].filter((b) => b.state.Name !== 'minecraft:air');
  const airCount = records.length - nonAir.length;
  const ys = [...new Set(nonAir.map((b) => b.p[1]))].sort((a, b) => a - b);
  const min = [Infinity, Infinity, Infinity], max = [-1, -1, -1];
  for (const b of nonAir) for (let i = 0; i < 3; i++) { min[i] = Math.min(min[i], b.p[i]); max[i] = Math.max(max[i], b.p[i]); }
  console.log(`  非空气方块: ${nonAir.length}（空气 ${airCount}）`);
  if (nonAir.length) console.log(`  范围: x ${min[0]}..${max[0]}  y ${min[1]}..${max[1]}  z ${min[2]}..${max[2]}`);
  console.log(`  有内容的 y 层: ${ys.join(', ') || '（无）'}`);

  // 关键规则：Ponder 底板在 y=0
  if (ys.includes(0)) {
    warn('y=0 有方块。Ponder 底板画在 y=0，结构一般应从 y=1 开始；若这是刻意的地面装饰可忽略');
  }
  if (!ys.includes(1) && nonAir.length) warn('y=1 没有方块，结构可能悬空或整层错位');

  if (nonAir.length === 0) err('文件里没有任何方块（只有空气），Ponder 里会是一片空白');

  // 底板尺寸
  if (wantSize != null) {
    if (Math.max(size[0], size[2]) > wantSize) {
      err(`结构 ${size[0]}×${size[2]} 超出底板 ${wantSize}×${wantSize}（configureBasePlate 需 ≥ 结构尺寸）`);
    } else ok(`底板 ${wantSize}×${wantSize} 可容纳结构 ${size[0]}×${size[2]}`);
    if (max[0] > wantSize - 1 || max[2] > wantSize - 1) warn('有方块落在底板范围之外');

    // showBasePlate() 本质是 showSection(y=0 那一层)，底板方块必须真的存在于 NBT 里
    let floor = 0;
    for (let x = 0; x < wantSize; x++) {
      for (let z = 0; z < wantSize; z++) {
        const b = grid.get([x, 0, z].join(','));
        if (b && b.state.Name !== 'minecraft:air') floor++;
      }
    }
    const total = wantSize * wantSize;
    if (floor === 0) err(`y=0 层没有底板方块：showBasePlate() 只会显示 NBT 里已有的方块，场景会悬空`);
    else if (floor < total) warn(`底板只铺了 ${floor}/${total} 格（缺 ${total - floor} 格），空缺处会透空`);
    else ok(`底板完整覆盖 ${wantSize}×${wantSize}${size[0] !== wantSize || size[2] !== wantSize ? `（但 NBT 尺寸是 ${size[0]}×${size[2]}，两者不一致）` : ''}`);
  }

  // 常见脏数据
  const structBlock = nonAir.filter((b) => b.state.Name.endsWith('structure_block') || b.state.Name.endsWith('jigsaw'));
  if (structBlock.length) err(`包含 ${structBlock.length} 个结构方块/拼图方块，导出前请把它们移出选区`);
  if (nonAir.some((b) => b.state.Name === 'minecraft:barrier')) warn('包含屏障方块（不可见，但会挡住射线/碰撞，确认是有意为之）');

  const bes = records.filter((r) => r.nbt);
  if (bes.length) {
    console.log(`  方块实体 ${bes.length} 个:`);
    for (const b of bes.slice(0, 12)) console.log(`    - ${b.nbt.id ?? '?'} @ ${list(b.pos).join(',')}`);
    if (bes.length > 12) console.log(`    ... 另有 ${bes.length - 12} 个`);
  } else {
    console.log('  方块实体: 无');
  }
  if (entities.length) warn(`包含 ${entities.length} 个实体，Ponder 会一起生成（确认是有意保留）`);

  // 方块 id 汇总
  const counts = new Map();
  for (const b of nonAir) {
    const props = b.state.Properties ? ' ' + JSON.stringify(b.state.Properties) : '';
    const k = b.state.Name + props;
    counts.set(k, (counts.get(k) ?? 0) + 1);
  }
  console.log('  方块构成:');
  [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 12).forEach(([k, v]) => console.log(`    ${String(v).padStart(4)} × ${k}`));

  if (showStates) {
    console.log('  方块明细:');
    const sorted = [...nonAir].sort((a, c) => (a.p[1] - c.p[1]) || (a.p[0] - c.p[0]) || (a.p[2] - c.p[2]));
    for (const b of sorted) {
      const props = b.state.Properties ? ' ' + JSON.stringify(b.state.Properties) : '';
      console.log(`    ${b.p.join(',')}  ${b.state.Name}${props}`);
    }
  }

  if (showMap) {
    const top = max[1];
    for (let y = 0; y <= top; y++) {
      console.log(`\n  --- y=${y} 俯视（北在上，x 向右，z 向下）---`);
      const rows = [];
      for (let z = 0; z < size[2]; z++) {
        let row = '';
        for (let x = 0; x < size[0]; x++) {
          const b = grid.get([x, y, z].join(','));
          const name = b?.state.Name ?? 'minecraft:air';
          row += name === 'minecraft:air' ? '.' : legendOf(name);
        }
        rows.push(row);
      }
      console.log(rows.map((r) => '    ' + r).join('\n'));
    }
  }
}

function legendOf(name) {
  const short = name.replace(/^.*:/, '');
  return short[0].toUpperCase();
}

console.log(`\n结果：${errors} 个错误，${warns} 个警告`);
process.exit(errors ? 1 : 0);
