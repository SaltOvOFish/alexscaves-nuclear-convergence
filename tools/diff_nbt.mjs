#!/usr/bin/env node
/**
 * 比较两个结构文件（.nbt），列出方块差异。
 *
 * 用法：node tools/diff_nbt.mjs <old.nbt> <new.nbt>
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
      case 9: { const et = this.u1(); const n = this.i4(); const items = []; for (let i = 0; i < n; i++) items.push(this.payload(et)); return { t, elemType: et, v: items }; }
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

const parse = (file) => {
  const raw = fs.readFileSync(file);
  const buf = raw[0] === 0x1f && raw[1] === 0x8b ? zlib.gunzipSync(raw) : raw;
  const r = new Reader(buf);
  r.u1();
  r.str();
  const root = r.payload(10).v;
  const size = root.size.v.map((x) => x.v);
  const palette = root.palette.v.map((p) => {
    const name = p.v.Name.v;
    const props = p.v.Properties ? Object.fromEntries(Object.entries(p.v.Properties.v).map(([k, v]) => [k, v.v])) : null;
    if (!props) return name;
    const keys = Object.keys(props).sort();
    return `${name}[${keys.map((k) => `${k}=${props[k]}`).join(',')}]`;
  });
  const map = new Map();
  for (const b of root.blocks.v) {
    const pos = b.v.pos.v.map((x) => x.v).join(',');
    map.set(pos, palette[b.v.state.v]);
  }
  return { size, map };
};

const [oldFile, newFile] = process.argv.slice(2);
if (!oldFile || !newFile) {
  console.log('用法: node tools/diff_nbt.mjs <old.nbt> <new.nbt>');
  process.exit(1);
}

const a = parse(oldFile);
const b = parse(newFile);

console.log(`尺寸: ${a.size.join('×')} -> ${b.size.join('×')}`);
console.log(`方块数: ${a.map.size} -> ${b.map.size}\n`);

const onlyNew = [];
const onlyOld = [];
const changed = [];

for (const [pos, state] of b.map) {
  if (!a.map.has(pos)) { onlyNew.push([pos, state]); continue; }
  const prev = a.map.get(pos);
  if (prev !== state) changed.push([pos, prev, state]);
}
for (const [pos, state] of a.map) {
  if (!b.map.has(pos)) onlyOld.push([pos, state]);
}

console.log(`=== 方块类型/属性变化（${changed.length}）===`);
for (const [pos, o, n] of changed) console.log(`  ${pos}: ${o}  ->  ${n}`);
console.log(`\n=== 新增方块（${onlyNew.length}）===`);
for (const [pos, s] of onlyNew) console.log(`  ${pos}: ${s}`);
console.log(`\n=== 移除方块（${onlyOld.length}）===`);
for (const [pos, s] of onlyOld) console.log(`  ${pos}: ${s}`);
