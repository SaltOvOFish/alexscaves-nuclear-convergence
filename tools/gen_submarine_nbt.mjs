#!/usr/bin/env node
/**
 * 生成潜水艇（Submarine）多方块结构的 Ponder 示意图。
 *
 * 结构规则（来自 EnigmaticEngineBlockEntity#isAssembledInDirection）：
 *   - 3×3×3 外壳
 *   - 正面 3×2 = 6 格深海玻璃（alexscaves:depth_glass）
 *   - 其余 20 格铜块
 *   - 中心 1 个奥秘引擎（alexscaves:enigmatic_engine）
 *
 * 用法：node tools/gen_submarine_nbt.mjs <out.nbt>
 */
import fs from 'node:fs';
import zlib from 'node:zlib';

class Writer {
  constructor() { this.chunks = []; }
  push(b) { this.chunks.push(b); }
  u1(v) { const b = Buffer.alloc(1); b.writeUInt8(v); this.push(b); }
  i1(v) { const b = Buffer.alloc(1); b.writeInt8(v); this.push(b); }
  i2(v) { const b = Buffer.alloc(2); b.writeInt16BE(v); this.push(b); }
  i4(v) { const b = Buffer.alloc(4); b.writeInt32BE(v); this.push(b); }
  str(s) { const b = Buffer.from(s, 'utf8'); this.i2(b.length); this.push(b); }
  payload(tag) {
    const { t, v } = tag;
    switch (t) {
      case 1: return this.i1(v);
      case 3: return this.i4(v);
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
      default: throw new Error('unsupported tag ' + t);
    }
  }
  toBuffer() { return Buffer.concat(this.chunks); }
}

const T = {
  int: (n) => ({ t: 3, v: n }),
  str: (s) => ({ t: 8, v: s }),
  list: (elemType, items) => ({ t: 9, elemType, v: items }),
  compound: (obj) => ({ t: 10, v: obj }),
};

const WHITE = 'minecraft:white_concrete';
const SNOW = 'minecraft:snow_block';
const COPPER = 'minecraft:copper_block';
const GLASS = 'alexscaves:depth_glass';
const ENGINE = 'alexscaves:enigmatic_engine';

const palette = [WHITE, SNOW, COPPER, GLASS, ENGINE];
const idx = (name) => palette.indexOf(name);

const blocks = [];
const put = (x, y, z, name) => {
  blocks.push(T.compound({
    pos: T.list(3, [T.int(x), T.int(y), T.int(z)]),
    state: T.int(idx(name)),
  }));
};

// y=0 底板：7×7 棋盘
for (let x = 0; x < 7; x++) {
  for (let z = 0; z < 7; z++) put(x, 0, z, (x + z) % 2 === 0 ? WHITE : SNOW);
}

// 3×3×3 潜水艇，x/z 2..4，y 1..3，正面朝 z+（z=4）
for (let x = 2; x <= 4; x++) {
  for (let y = 1; y <= 3; y++) {
    for (let z = 2; z <= 4; z++) {
      if (x === 3 && y === 2 && z === 3) put(x, y, z, ENGINE);
      else if (z === 4 && y >= 2) put(x, y, z, GLASS);
      else put(x, y, z, COPPER);
    }
  }
}

const size = [7, 5, 7];
const root = T.compound({
  DataVersion: T.int(3955),
  size: T.list(3, size.map(T.int)),
  palette: T.list(10, palette.map((name) => T.compound({ Name: T.str(name) }))),
  blocks: T.list(10, blocks),
  entities: T.list(10, []),
});

const w = new Writer();
w.u1(10);
w.str('');
w.payload(root);
const out = zlib.gzipSync(w.toBuffer());

const dst = process.argv[2];
if (!dst) {
  console.error('用法: node tools/gen_submarine_nbt.mjs <out.nbt>');
  process.exit(1);
}
fs.writeFileSync(dst, out);

const counts = {};
for (const b of blocks) {
  const name = palette[b.v.state.v];
  counts[name] = (counts[name] || 0) + 1;
}
console.log(`已生成 ${dst}（${out.length} B）`);
console.log(counts);
