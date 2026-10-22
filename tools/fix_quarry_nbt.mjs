#!/usr/bin/env node
/**
 * 磁力采石场结构对齐工具
 *
 * 用途：把 quarry_2.nbt（3 层：石 y=0..1、草 y=2、采石场与灯 y=3）
 *       补齐 y=0 底板，并复制出一份完全同构的 quarry_1.nbt，
 *       让思索翻页时地面高度一致（不会「跳上去」）。
 *
 * 用法：node tools/fix_quarry_nbt.mjs <quarry_2.nbt> <quarry_1.nbt>
 */
import fs from 'node:fs';
import zlib from 'node:zlib';

/* ---------------- NBT 读取（保留 tag 类型，便于原样写回） ---------------- */

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
  payload(t) {
    switch (t) {
      case 1: return { t, v: this.i1() };
      case 2: return { t, v: this.i2() };
      case 3: return { t, v: this.i4() };
      case 4: return { t, v: this.i8() };
      case 5: return { t, v: this.f4() };
      case 6: return { t, v: this.f8() };
      case 7: { const n = this.i4(); const v = this.b.subarray(this.p, this.p + n); this.p += n; return { t, v: Buffer.from(v) }; }
      case 8: return { t, v: this.str() };
      case 9: {
        const et = this.u1(); const n = this.i4(); const items = [];
        for (let i = 0; i < n; i++) items.push(this.payload(et));
        return { t, elemType: et, v: items };
      }
      case 10: {
        const obj = {};
        for (;;) {
          const ct = this.u1();
          if (ct === 0) break;
          const name = this.str();
          obj[name] = this.payload(ct);
        }
        return { t, v: obj };
      }
      case 11: { const n = this.i4(); const a = []; for (let i = 0; i < n; i++) a.push(this.i4()); return { t, v: a }; }
      case 12: { const n = this.i4(); const a = []; for (let i = 0; i < n; i++) a.push(this.i8()); return { t, v: a }; }
      default: throw new Error(`未知 tag 类型 ${t}（偏移 ${this.p}）`);
    }
  }
}

class Writer {
  constructor() { this.chunks = []; }
  push(b) { this.chunks.push(b); }
  u1(v) { const b = Buffer.alloc(1); b.writeUInt8(v); this.push(b); }
  i1(v) { const b = Buffer.alloc(1); b.writeInt8(v); this.push(b); }
  i2(v) { const b = Buffer.alloc(2); b.writeInt16BE(v); this.push(b); }
  u2(v) { const b = Buffer.alloc(2); b.writeUInt16BE(v); this.push(b); }
  i4(v) { const b = Buffer.alloc(4); b.writeInt32BE(v); this.push(b); }
  i8(v) { const b = Buffer.alloc(8); b.writeBigInt64BE(BigInt(v)); this.push(b); }
  f4(v) { const b = Buffer.alloc(4); b.writeFloatBE(v); this.push(b); }
  f8(v) { const b = Buffer.alloc(8); b.writeDoubleBE(v); this.push(b); }
  str(s) { const b = Buffer.from(s, 'utf8'); this.u2(b.length); this.push(b); }
  payload(tag) {
    const { t, v } = tag;
    switch (t) {
      case 1: return this.i1(v);
      case 2: return this.i2(v);
      case 3: return this.i4(v);
      case 4: return this.i8(v);
      case 5: return this.f4(v);
      case 6: return this.f8(v);
      case 7: this.i4(v.length); return this.push(v);
      case 8: return this.str(v);
      case 9: {
        this.u1(tag.elemType);
        this.i4(v.length);
        for (const item of v) this.payload(item);
        return;
      }
      case 10: {
        for (const [name, child] of Object.entries(v)) {
          this.u1(child.t);
          this.str(name);
          this.payload(child);
        }
        this.u1(0);
        return;
      }
      case 11: { this.i4(v.length); for (const n of v) this.i4(n); return; }
      case 12: { this.i4(v.length); for (const n of v) this.i8(n); return; }
      default: throw new Error(`无法写出 tag 类型 ${t}`);
    }
  }
  toBuffer() { return Buffer.concat(this.chunks); }
}

function parseNbt(raw) {
  const gz = raw[0] === 0x1f && raw[1] === 0x8b;
  const buf = gz ? zlib.gunzipSync(raw) : raw;
  const r = new Reader(buf);
  const rt = r.u1();
  if (rt !== 10) throw new Error('根标签不是 compound');
  const rootName = r.str();
  const root = r.payload(10);
  return { root, rootName, gz };
}

function writeNbt(root, rootName) {
  const w = new Writer();
  w.u1(10);
  w.str(rootName);
  w.payload(root);
  return zlib.gzipSync(w.toBuffer());
}

const listItems = (tag) => (tag && tag.t === 9 ? tag.v : []);
const strOf = (tag) => tag.v;
const intsOf = (tag) => (tag && tag.t === 9 ? tag.v.map((x) => x.v) : tag && tag.t === 11 ? tag.v : []);

/* ---------------- 主流程 ---------------- */

const [src, dst] = process.argv.slice(2);
if (!src || !dst) {
  console.log('用法: node tools/fix_quarry_nbt.mjs <quarry_2.nbt> <quarry_1.nbt>');
  process.exit(1);
}

const { root, rootName } = parseNbt(fs.readFileSync(src));
const size = intsOf(root.v.size);
const SX = size[0], SY = size[1], SZ = size[2];
const palette = listItems(root.v.palette);
const blocks = listItems(root.v.blocks);

const paletteIndex = (name) => {
  const i = palette.findIndex((p) => strOf(p.v.Name) === name);
  if (i < 0) throw new Error('palette 里没有 ' + name);
  return i;
};
const STONE = paletteIndex('minecraft:stone');
const GRASS = paletteIndex('minecraft:grass_block');

const cellKey = (p) => p.join(',');
const grid = new Map();
for (const rec of blocks) grid.set(cellKey(intsOf(rec.v.pos)), rec);

const makePos = (x, y, z) => ({ t: 9, elemType: 3, v: [x, y, z].map((n) => ({ t: 3, v: n })) });

let added = 0, fixed = 0;

// 1) 补满 y=0 底板（用石头，与周围一致）
for (let x = 0; x < 7; x++) {
  for (let z = 0; z < 7; z++) {
    const k = cellKey([x, 0, z]);
    const cur = grid.get(k);
    if (!cur) {
      const rec = { t: 10, v: { pos: makePos(x, 0, z), state: { t: 3, v: STONE } } };
      blocks.push(rec);
      grid.set(k, rec);
      added++;
    } else if (strOf(palette[cur.v.state.v].v.Name) === 'minecraft:air') {
      cur.v.state.v = STONE;
      added++;
    }
  }
}

// 2) 采石场脚下的泥土统一成草地
const dirtPos = cellKey([5, 2, 0]);
const dirtRec = grid.get(dirtPos);
if (dirtRec && strOf(palette[dirtRec.v.state.v].v.Name) === 'minecraft:dirt') {
  dirtRec.v.state.v = GRASS;
  fixed++;
}

root.v.blocks.v = blocks;
fs.writeFileSync(src, writeNbt(root, rootName));
fs.writeFileSync(dst, writeNbt(root, rootName));

console.log(`已在 ${SX}×${SY}×${SZ} 结构上补齐 y=0 底板 ${added} 格，并统一 ${fixed} 格泥土为草地。`);
console.log(`已写入：${src}、${dst}（两份内容完全一致）`);
