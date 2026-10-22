#!/usr/bin/env node
/**
 * 把潜水艇结构里的强化玻璃换回深海玻璃（带连接属性）。
 *
 * 用法：node tools/restore_depth_glass.mjs <in.nbt> <out.nbt>
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
        for (;;) { const ct = this.u1(); if (ct === 0) break; const name = this.str(); obj[name] = this.payload(ct); }
        return { t, v: obj };
      }
      case 11: { const n = this.i4(); const a = []; for (let i = 0; i < n; i++) a.push(this.i4()); return { t, v: a }; }
      case 12: { const n = this.i4(); const a = []; for (let i = 0; i < n; i++) a.push(this.i8()); return { t, v: a }; }
      default: throw new Error('未知 tag 类型 ' + t);
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
      case 9: { this.u1(tag.elemType); this.i4(v.length); for (const item of v) this.payload(item); return; }
      case 10: {
        for (const [name, child] of Object.entries(v)) { this.u1(child.t); this.str(name); this.payload(child); }
        this.u1(0); return;
      }
      case 11: { this.i4(v.length); for (const n of v) this.i4(n); return; }
      case 12: { this.i4(v.length); for (const n of v) this.i8(n); return; }
      default: throw new Error('无法写出 tag 类型 ' + t);
    }
  }
  toBuffer() { return Buffer.concat(this.chunks); }
}

const [src, dst] = process.argv.slice(2);
if (!src || !dst) {
  console.log('用法: node tools/restore_depth_glass.mjs <in.nbt> <out.nbt>');
  process.exit(1);
}

const raw = fs.readFileSync(src);
const gz = raw[0] === 0x1f && raw[1] === 0x8b;
const buf = gz ? zlib.gunzipSync(raw) : raw;
const r = new Reader(buf);
if (r.u1() !== 10) throw new Error('根标签不是 compound');
const rootName = r.str();
const root = r.payload(10);

const palette = root.v.palette.v;
const blocks = root.v.blocks.v;
const nameOf = (i) => palette[i].v.Name.v;

const glassKeys = new Set(
  blocks
    .filter((b) => nameOf(b.v.state.v) === 'createnuclear:reinforced_glass')
    .map((b) => b.v.pos.v.map((x) => x.v).join(','))
);

if (!glassKeys.size) {
  console.log('没有强化玻璃，直接复制。');
  fs.writeFileSync(dst, raw);
  process.exit(0);
}

const DIRS = [
  ['east', [1, 0, 0]],
  ['west', [-1, 0, 0]],
  ['up', [0, 1, 0]],
  ['down', [0, -1, 0]],
  ['north', [0, 0, -1]],
  ['south', [0, 0, 1]],
];

const paletteFor = (props) => {
  const key = JSON.stringify(props);
  const found = palette.findIndex((p) => p.v.Name.v === 'alexscaves:depth_glass' && JSON.stringify(propsOf(p)) === key);
  if (found >= 0) return found;
  palette.push({
    t: 10,
    v: {
      Name: { t: 8, v: 'alexscaves:depth_glass' },
      Properties: { t: 10, v: Object.fromEntries(Object.entries(props).map(([k, v]) => [k, { t: 8, v }])) },
    },
  });
  return palette.length - 1;
};

function propsOf(p) {
  const out = {};
  if (p.v.Properties) for (const [k, v] of Object.entries(p.v.Properties.v)) out[k] = v.v;
  return out;
}

let converted = 0;
for (const b of blocks) {
  if (nameOf(b.v.state.v) !== 'createnuclear:reinforced_glass') continue;
  const [x, y, z] = b.v.pos.v.map((n) => n.v);
  const props = {};
  for (const [name, [dx, dy, dz]] of DIRS) {
    props[name] = glassKeys.has(`${x + dx},${y + dy},${z + dz}`) ? 'true' : 'false';
  }
  b.v.state.v = paletteFor(props);
  converted++;
}

const w = new Writer();
w.u1(10); w.str(rootName); w.payload(root);
const out = gz ? zlib.gzipSync(w.toBuffer()) : w.toBuffer();
fs.writeFileSync(dst, out);
console.log(`已转换 ${converted} 格强化玻璃为深海玻璃 → ${dst}`);
