#!/usr/bin/env node
/**
 * 批量把结构文件里的深海玻璃（alexscaves:depth_glass）替换成
 * 强化玻璃（createnuclear:reinforced_glass）。
 *
 * 强化玻璃没有 east/west/up/down 这些连接属性，所以替换时要一并删掉 Properties，
 * 否则结构方块加载时会因为未知属性而报错。
 *
 * 用法：node tools/replace_glass.mjs <dir>
 */
import fs from 'node:fs';
import path from 'node:path';
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

const DEPTH = 'alexscaves:depth_glass';
const REINFORCED = 'createnuclear:reinforced_glass';

const dir = process.argv[2];
if (!dir) {
  console.error('用法: node tools/replace_glass.mjs <dir>');
  process.exit(1);
}

let filesChanged = 0, paletteChanged = 0;

for (const file of fs.readdirSync(dir).filter((f) => f.endsWith('.nbt')).sort()) {
  const full = path.join(dir, file);
  const raw = fs.readFileSync(full);
  const gz = raw[0] === 0x1f && raw[1] === 0x8b;
  const buf = gz ? zlib.gunzipSync(raw) : raw;
  const r = new Reader(buf);
  if (r.u1() !== 10) { console.log(`${file}: 根标签不是 compound，跳过`); continue; }
  const rootName = r.str();
  const root = r.payload(10);

  let changed = 0;
  for (const entry of root.v.palette.v) {
    const p = entry.v;
    if (p.Name && p.Name.v === DEPTH) {
      p.Name.v = REINFORCED;
      if (p.Properties) delete p.Properties;
      changed++;
    }
  }

  if (!changed) {
    console.log(`${file}: 没有深海玻璃，跳过`);
    continue;
  }

  const w = new Writer();
  w.u1(10); w.str(rootName); w.payload(root);
  const out = gz ? zlib.gzipSync(w.toBuffer()) : w.toBuffer();
  fs.writeFileSync(full, out);
  filesChanged++;
  paletteChanged += changed;
  console.log(`${file}: 替换 ${changed} 种调色板条目`);
}

console.log(`\n完成：${filesChanged} 个文件，共 ${paletteChanged} 个调色板条目`);
