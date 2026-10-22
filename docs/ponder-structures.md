# 核能熔炉 Ponder 示意图（.nbt）制作规范

本文是「核能熔炉思索」正式结构的制作说明。占位阶段用 `setBlock` 程序化搭的场景，正式版全部改为从 `.nbt` 读取。

---

## 0. 三条铁律（先看这个）

1. **NBT 最小角 = 场景坐标 (0,0,0)，不居中、不偏移。**
   Ponder 加载时执行的是 `StructureTemplate.placeInWorld(level, BlockPos.ZERO, BlockPos.ZERO, ...)`（见 `PonderSceneRegistry`），所以结构方块导出的最小角原封不动落在原点。

2. **底板就是 NBT 里 y=0 那一层的真实方块。**
   `scene.showBasePlate()` 内部等价于
   `world().showSection(cuboid((basePlateOffsetX, 0, basePlateOffsetZ), (size-1, 0, size-1)), Direction.UP)`。
   它只把**已经存在的方块**渐显出来；NBT 的 y=0 层没有方块，场景就是悬空的。
   （Create 官方示意图 y=0 层都是白混凝土/雪块棋盘格，就是这个原因。）

3. **方块实体不会被 tick。**
   `PonderLevel.tick()` 只 tick 粒子和实体。所以：多方块不会自己组装、火不会蔓延、废料不会自己涨、警报器不会自己响。
   一切「会自己变化」的东西，要么**冻结进 NBT**，要么由场景代码手动演（`setBlock` / `modifyBlockEntityNBT` / 粒子）。

---

## 1. 规格表

| 文件 | 尺寸 | `configureBasePlate` | 熔炉位置（2×2×2 最小角） | 内容 |
|---|---|---|---|---|
| `nuclear_furnace/basic.nbt` | 7×5×7 | `(0, 0, 7)` | (2, 1, 2) | 组装好的熔炉 + 铀棒/钍棒 + 金属桶 |
| `nuclear_furnace/automation.nbt` | 9×5×9 | `(0, 0, 9)` | (3, 1, 3) | 熔炉 + 四向传送带/漏斗/箱子 |
| `nuclear_furnace/critical.nbt` | 13×5×13 | `(0, 0, 13)` | (5, 1, 5) | 熔炉 + 核能警报器（状态用代码切） |

> 尺寸必须与 `configureBasePlate` 的 size **完全相等**。结构比底板大 → 悬在底板外；比底板小 → 底板多出一圈空白。

### 关于「4 个部件」——要改成 8 个

AC 官方进度文案写的是：

> Craft **8** Nuclear Furnace Components, which can be placed in a **cube** to create a Nuclear Furnace

`NuclearFurnaceComponentBlock.isCornerForFurnace` 的检查范围也是 **2×2×2 的 8 格**（跳过角上那一格，其余 7 格必须是部件）。

组装完成时：**最后放置的角落方块变成 `alexscaves:nuclear_furnace`（本体，带 BE），其余 7 个变成 `nuclear_furnace_component[active=true]`。**

所以 P1 现在那句「使用 4 个核能熔炉部件」是错的，正式版要改成「使用 8 个核能熔炉部件，搭成 2×2×2 的方块」。

---

## 2. 游戏内搭建（导出之前）

1. 新建一个**创造模式超平坦 / 虚空**存档，专门用来出图。
   - `/time set noon`、`/gamerule doDaylightCycle false`，避免出图时明暗不一致。
2. **先铺地板**（这就是 y=0 层），铺满 `size × size`，一格都别漏：
   - 7×7：`/fill 0 0 0 6 0 6 minecraft:white_concrete`
   - 9×9：`/fill 0 0 0 8 0 8 minecraft:white_concrete`
   - 13×13：`/fill 0 0 0 12 0 12 minecraft:white_concrete`
   - 想要 Create 那种棋盘格：可以全铺 `snow_block` 再手动改一部分，或干脆纯色（视觉影响很小）。
3. 地板之上 y=1 起搭结构。**熔炉 2×2×2 摆好并组装完成**：
   - 8 个部件放成 2×2×2，最后一个放下去会自动成型；
   - 成型后外观完全由 BER（`NuclearFurnaceBlockRenderer`）绘制，本体方块本身没有模型 —— 所以在 Ponder 里「看不见」通常意味着 BE 没建起来，而不是方块没放。
4. 需要特定「份数」外观时，**直接改 BE 的 `Waste`**，不用真的去烧：
   ```
   /data merge block <本体x> <本体y> <本体z> {Waste:400}
   ```
   - 单位是 **mB**，1 份 = 100 mB；
   - `Waste` 只在 `nuclear_furnace` 本体方块上（不是部件）；
   - 对应关系：`400` → 临界 1（底部发红）、`600` → 2、`800` → 3（发黄/剧震）、`1000` → 触发核爆。
   - 依据：`getWasteScale() = currentWaste / 1000`，`getCriticality()` 阈值 0.35 / 0.6 / 0.8。
5. 摆演示物（料斗、桶、箱子、传送带等）。注意这些在 Ponder 里都是**静止**的，别指望它们动。

---

## 3. 结构方块导出

1. `/give @s structure_block`，放一个（**不要放进选区里**，否则它自己会被存进 NBT）。
2. 模式 `SAVE`，`Relative position` 调到选区的**最小角**，`Size` 填规格表里的尺寸。
3. 勾选项：
   - `Include entities` → **关**（Ponder 会一起生成实体，容易穿模/乱飞）；
   - `Detect` / `Show bounding box` 只是显示，无所谓。
4. Name 填英文文件名：`basic` / `automation` / `critical`（不要中文、空格、大写）。
5. 点 `SAVE`，文件落在：
   ```
   .minecraft/saves/<存档名>/generated/minecraft/structures/<name>.nbt
   ```
   这个文件已经是 **gzip 压缩**，不要再压一次，也不要解压。

### 命名与路径对应

`addStoryBoard("nuclear_furnace/basic", ...)` ⇔
`src/main/resources/assets/alexscaves_nuclear_convergence/ponder/nuclear_furnace/basic.nbt`

---

## 4. 拷回项目后先校验

```powershell
node tools/ponder_nbt_tool.mjs <path>\basic.nbt --size 7 --map
```

工具会检查：

- 是否 gzip、根标签是否齐全；
- 坐标是否超出 `size`；
- **y=0 底板是否铺满**（这是最常见的翻车点）；
- y=1 是否有内容（防止整体错层）；
- 是否混进了结构方块/屏障；
- 方块实体列表（确认 `alexscaves:nuclear_furnace` 的 BE 在、`Waste` 在）；
- `--map` 打印每层俯视图，便于确认结构和朝向。

有「错误」会返回退出码 1。

---

## 5. 代码侧要同步改的地方

等图到位后一起改（`NuclearFurnacePonderScenes`）：

1. `configureBasePlate(0, 0, 5)` → `7` / `9`；`critical` 保持 `13`。
2. 删掉 `placePlaceholderFurnace(...)` 以及所有占位 `setBlock`。
3. 所有 `pointAt` / `centerOf` 坐标按新位置重算：
   - 熔炉 2×2×2 的中心 = `util.vector().of(cornerX + 1.0, 1.5, cornerZ + 1.0)`；
   - 顶部坑 = `util.vector().topOf(cornerX, 2, cornerZ)` 附近。
4. P3 建议**不要拆 4 张图**：
   - 一张 `critical.nbt` 就够；
   - 4/6/8 三态用 `world().modifyBlockEntityNBT(selection, NuclearFurnaceBlockEntity.class, tag -> tag.putInt("Waste", 600))` 切；
   - 火焰/黑烟/黄烟/音波继续用 `setBlocks` + `simpleParticleEmitter`；
   - 核爆用 `setBlocks(..., AIR)` + 撒 `unrefined_waste`（现在的 `explodeCrater` 可以留）。
   - 好处：三页结构不变、演出连贯、只需要一张图。
5. **`text_N` 编号跟着 `showText` 调用顺序走**，改顺序 = 语言文件全体错位。

---

## 6. 踩坑清单

| 现象 | 原因 | 处理 |
|---|---|---|
| 进场景一片空白 / 结构悬空 | NBT 的 y=0 没有底板方块 | 导出时把地板框进去 |
| 结构嵌进底板、下半截消失 | 框选从结构本体开始，最小角落在了 y=0 | 选区的**最下一层必须是地板** |
| 熔炉完全看不见 | 只放了部件没组装（`active=false` 是实心柱），或 BE 没被创建 | 导出**组装完成**后的状态 |
| 熔炉是灰的、不发红不发黄 | `Waste` 单位搞错（写了 4 而不是 400） | `Waste` 用 mB |
| 底板只有一半 | `configureBasePlate` 的 size 小于实际 NBT 尺寸 | 两边保持一致 |
| 场景里出现紫黑棋盘方块 | 结构方块自己被框进去了 | 移出选区后重新保存 |
| 翻页文字对不上 | `text_N` 与 `showText` 顺序错位 | 加/删文本时同步改语言文件 |
| 加载即崩 | 手写 NBT 缺 `TAG_End`（只有占位文件会这样） | 用结构方块导出即可避免 |
| 熔炉朝向背对镜头 | 导出时 FACING 朝北 | 统一让正面朝 +Z 导出；或在场景里 `scene.rotateCameraY(...)` |

---

## 7. 交付清单（每个文件都过一遍）

- [ ] 尺寸与规格表一致，且 `configureBasePlate` 已同步
- [ ] y=0 地板铺满，无空缺
- [ ] 结构从 y=1 开始，最高不超过 y=4（5 高）
- [ ] 熔炉是组装完成态（本体 + 7 个 `active=true` 部件）
- [ ] `Waste` 数值符合该页要表达的状态
- [ ] 无结构方块、无实体（或确认保留）
- [ ] `node tools/ponder_nbt_tool.mjs <file> --size N --map` 无错误
- [ ] 游戏内 `/ponder` 或悬停按 W 实际跑一遍
