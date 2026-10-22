#!/usr/bin/env node
/**
 * 批量列出结构文件（.nbt）的尺寸与方块数量。
 * 用法：node tools/list_structures.mjs <dir>
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

const parseNbt = (raw) => {
  const gz = raw[0] === 0x1f && raw[1] === 0x8b;
  const buf = gz ? zlib.gunzipSync(raw) : raw;
  const r = new Reader(buf);
  if (r.u1() !== 10) throw new Error('根标签不是 compound');
  r.str();
  return r.payload(10);
};

const dir = process.argv[2];
if (!dir) {
  console.error('用法: node tools/list_structures.mjs <dir>');
  process.exit(1);
}

const rows = [];
for (const file of fs.readdirSync(dir).filter((f) => f.endsWith('.nbt')).sort()) {
  try {
    const root = parseNbt(fs.readFileSync(path.join(dir, file))).v;
    const size = root.size.v.map((x) => x.v);
    const palette = root.palette.v.map((p) => p.v.Name.v);
    const blocks = root.blocks.v;
    const nonAir = blocks.filter((b) => palette[b.v.state.v] !== 'minecraft:air').length;
    const bes = blocks.filter((b) => b.v.nbt).length;
    rows.push({ file, size, nonAir, bes });
  } catch (e) {
    rows.push({ file, error: e.message });
  }
}

console.log('文件名'.padEnd(26) + '尺寸'.padEnd(16) + '方块数'.padEnd(8) + '方块实体');
for (const r of rows) {
  if (r.error) {
    console.log(r.file.padEnd(26) + '解析失败: ' + r.error);
    continue;
  }
  console.log(r.file.padEnd(26) + r.size.join('×').padEnd(16) + String(r.nonAir).padEnd(8) + r.bes);
}
console.log(`\n共 ${rows.length} 个结构`);
