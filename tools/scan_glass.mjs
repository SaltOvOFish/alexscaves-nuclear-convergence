#!/usr/bin/env node
/**
 * 扫描目录（递归）下所有 .nbt，统计 alexscaves:depth_glass 与
 * createnuclear:reinforced_glass 的出现次数。
 *
 * 用法：node tools/scan_glass.mjs <dir>
 */
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';

const dir = process.argv[2];
if (!dir) {
  console.error('用法: node tools/scan_glass.mjs <dir>');
  process.exit(1);
}

const walk = (d) => {
  const out = [];
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) out.push(...walk(p));
    else if (e.name.endsWith('.nbt')) out.push(p);
  }
  return out;
};

let total = 0, withDepth = 0, withReinforced = 0;

for (const file of walk(dir).sort()) {
  const raw = fs.readFileSync(file);
  let buf = raw;
  if (raw[0] === 0x1f && raw[1] === 0x8b) {
    try { buf = zlib.gunzipSync(raw); } catch (e) { console.log(`${file}: gunzip 失败 ${e.message}`); continue; }
  }
  const text = buf.toString('latin1');
  const depth = (text.match(/alexscaves:depth_glass/g) || []).length;
  const reinforced = (text.match(/createnuclear:reinforced_glass/g) || []).length;
  total++;
  if (depth) withDepth++;
  if (reinforced) withReinforced++;
  if (depth || reinforced) {
    console.log(`${path.relative(dir, file).padEnd(34)} depth_glass=${depth}  reinforced_glass=${reinforced}`);
  }
}

console.log(`\n共扫描 ${total} 个 .nbt：含 depth_glass 的 ${withDepth} 个，含 reinforced_glass 的 ${withReinforced} 个`);
