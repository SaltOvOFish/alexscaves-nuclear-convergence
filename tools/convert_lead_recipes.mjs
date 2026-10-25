#!/usr/bin/env node
/**
 * 把原子核动的铅相关配方整体替换为「井然有矿」的铅物品，
 * 并加上 `neoforge:mod_loaded oreganized` 条件（未安装时原配方照常生效）。
 *
 * 用法：node tools/convert_lead_recipes.mjs <源目录> <输出目录>
 */
import fs from 'node:fs';
import path from 'node:path';

const srcRoot = process.argv[2];
const outRoot = process.argv[3];

const REPLACEMENTS = [
  ['createnuclear:raw_lead_block', 'oreganized:raw_lead_block'],
  ['createnuclear:lead_ingot', 'oreganized:lead_ingot'],
  ['createnuclear:lead_block', 'oreganized:lead_block'],
  ['createnuclear:lead_nugget', 'oreganized:lead_nugget'],
  ['createnuclear:raw_lead', 'oreganized:raw_lead'],
];

const walk = (dir) =>
  fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) =>
    entry.isDirectory()
      ? walk(path.join(dir, entry.name))
      : [path.join(dir, entry.name)]);

for (const file of walk(srcRoot)) {
  if (!file.endsWith('.json')) continue;

  const text = fs.readFileSync(file, 'utf8');
  let replaced = text;
  for (const [from, to] of REPLACEMENTS) {
    replaced = replaced.split(`"${from}"`).join(`"${to}"`);
  }

  const json = JSON.parse(replaced);
  json['neoforge:conditions'] = [{ type: 'neoforge:mod_loaded', modid: 'oreganized' }];

  const relative = path.relative(srcRoot, file);
  const outPath = path.join(outRoot, relative);
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, `${JSON.stringify(json, null, 2)}\n`, 'utf8');
  console.log(`已转换 ${relative}`);
}
