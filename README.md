# Alex's Caves: Nuclear Convergence

一个 NeoForge 1.21.1 桥接模组，把「原子核动（Create Nuclear）」与「Alex's Caves」的铀/辐射体系打通，
统一为一套以 Create Nuclear 为中心的机制。

## 功能

### 1. 辐射效果统一
- 拦截 `createnuclear:radiation`（Create Nuclear 的辐射效果），改为施加 `alexscaves:irradiated`
  （Alex's Caves 的辐照效果），等级 / 时长 / 氛围 / 可见性保持一致；
- 当 Create Nuclear 移除辐射效果时，同步移除辐照效果。

### 2. 铀物品映射（配方 + 掉落）
| Alex's Caves | Create Nuclear |
|---|---|
| `alexscaves:uranium` | `createnuclear:raw_uranium` |
| `alexscaves:uranium_shard` | `createnuclear:uranium_powder` |
| `alexscaves:uranium_rod` | `createnuclear:uranium_rod` |
| `alexscaves:block_of_uranium` | `createnuclear:raw_uranium_block` |

- `radrock_uranium_ore` 掉落改为 **基础 2 个 `raw_uranium`**（受时运、爆炸衰减影响）；
- 生物掉落（tremorzilla）中的 uranium / shard 替换为 raw_uranium / uranium_powder；
- 禁用 Alex's Caves 产出 `uranium` / `uranium_shard` / `block_of_uranium` 的全部配方；
- `nuclear_siren`、`nuclear_furnace_component` 配方中的铀材料改为 `raw_uranium`；
- `raygun`、`nuclear_bomb` 配方中的 `uranium_rod` 改为 `createnuclear:uranium_rod`（核弹中心格改为 `raw_uranium_block`）；
- `unrefined_waste` 熔炉/高炉烧炼改为产出 1 个 `createnuclear:uranium_powder`。

### 3. 铀烛配方
- 新增 `alexscaves:uranium_rod` 合成配方（工作台竖列：铁锭 + `createnuclear:uranium_rod` + 铁锭 → 1 个）；
- 语言文件将其显示名重命名为「铀烛」。

### 4. 核能熔炉燃料
核能熔炉（Nuclear Furnace）燃料棒改为：
- `createnuclear:uranium_rod` → 51200 裂变时间（约 512 个物品）；
- `createnuclear:thorium_rod` → 12800 裂变时间（约 128 个物品）。

同时将核能熔炉的烧炼速度提高到 **2 倍**（`getSpeedReduction()` 返回值 × 0.5）。

### 5. 核弹群系转化
`alexscaves:nuclear_bomb` 爆炸时，把爆炸区域转化为 Create Nuclear 的辐照之地群系
（`createnuclear:irradiated_land`），复刻反应堆失控爆炸的群系转化效果，且可被
`createnuclear:biome_irradiation_extractor` 正常消除。

### 6. 护甲兼容
防辐射套装（`createnuclear:default_anti_radiation_*`）与防化套装（`alexscaves:hazmat_*`）能力互通：

| 能力 | 防辐射套装 | 防化套装 |
|---|---|---|
| 辐照伤害减免（每件 -25%） | ✅ | ✅ |
| 穿齐 4 件免疫辐照（清除效果） | ✅ | ✅ |
| 酸液伤害减免（每件 -25%） | ✅ | ✅ |
| 穿齐 4 件免疫酸液 | ✅ | ✅ |

### 7. 剧毒遗迹箱子战利品
`toxic_ruins` 箱子中：
- `alexscaves:uranium_rod` 改为 `createnuclear:uranium_rod`；
- 新增 `createnuclear:reactor_blueprint_item`（数量 1，权重 2）。

### 8. 废弃反应堆结构（自然生成）
在毒化洞穴新增「废弃反应堆」结构（4 种变体，`abandoned_reactor_0~3`），参考 Alex's Caves 的
遗迹生成逻辑自实现，以**锈蚀金属桶**作为战利品箱（通过 `loot_chest` data 标记定位）。

> 生成细节：结构本体严格居中于单个区块内生成（避免被区块边界切断），基座裙边按地形自适应
> 坡度铺设（可跨区块延伸）；结构不生成在酸液内（若其竖段含酸液则抬升到酸液顶面以上）。

战利品表（`alexscaves_nuclear_convergence:chests/abandoned_reactor`）包含：
- 原子核动：铀棒、液氮桶、石墨棒、钢锭、反应堆蓝图、铅锭；
- Alex's Caves Up：洞穴石板、氡瓶、铀烛、绿色豆粮、废料桶、防化套装、唱片 11。

### 9. 辐射岩矿石
在毒化洞穴的辐射岩（radrock）中生成 4 种矿石：

| 矿石 | 掉落 |
|---|---|
| `radrock_iron_ore` 辐射铁矿石 | `minecraft:raw_iron` |
| `radrock_coal_ore` 辐射煤矿石 | `minecraft:coal` |
| `radrock_lead_ore` 辐射铅矿石 | `createnuclear:raw_lead` |
| `radrock_nitrate_ore` 辐射硝酸盐矿石 | `createnuclear:nitrate` |

### 10. 机械动力自动化兼容
- 为 Alex's Caves 的核能熔炉（含 4×4 组件方块）、金属桶/锈蚀金属桶、渊海祭坛补充
  NeoForge 的 `ItemHandler` capability，使 Create 的漏斗、溜槽、动力机械臂等能正常输入输出；
- 渊海祭坛注册为机械臂交互点类型（类似置物台），机械臂可从中拿取/放置物品；
- 手持 Create 扳手或机械臂物品右键渊海祭坛时放行，避免误把工具放进祭坛。

### 11. 反应堆熔毁与警报器 / 撼地斯拉孵化联动
- 当原子核动的反应堆热量达到 DANGER（进入失控爆炸倒计时）时，附近的 Alex's Caves 核能警报器提前响起；
- 反应堆失控爆炸时，会孵化爆炸范围内（36 格）的 `alexscaves:tremorzilla_egg`（撼地斯拉蛋），
  且孵出的撼地斯拉对本次核爆免疫（不会被秒杀或击飞），复刻 Alex's Caves 核弹孵化 Boss 的行为。

### 12. 辐射生物转化
- 原版的猫（Cat）与豹猫（Ocelot）在持续受到辐照 III 及以上效果 45 秒后，转化为
  Alex's Caves 的辐射猫（`raycat`）。

### 13. 反应堆警报器红石输出
原子核动的**反应堆警报器**在反应堆热量达到 DANGER（失控预警、警报响起）时，除响铃外还会
**对外输出 15 级红石信号**（六面全向），便于用原版红石或机械动力自动化联动（自动注入冷却、
拉闸、报警灯、紧急停机等）。

> 说明：警报器的 `POWERED` 状态同时可由红石输入驱动，因此手动为其供电时也会输出。

### 14. 核能警示灯环境侦测
Alex's Caves 的**核能警示灯**（`alexscaves:siren_light`）原先只响应红石信号；现在会像核能警报器
一样侦测周围核危险环境，并在有危险时**自动点亮、旋转、发光**：
- 原子核动反应堆失控（热量 DANGER，半径 32 格）；
- Alex's Caves 核弹（`alexscaves:nuclear_bomb`，半径 256 格）；
- 撼地斯拉吐息、核爆怪 Nucleeper（实现 `ActivatesSirens` 接口，半径 256 格）；
- Alex's Caves 临界核熔炉（POI 查找，半径 128 格）。

无危险时回退为由红石输入控制（原行为不变）。

### 15. 思索（Ponder）教程

为 Alex's Caves 的核能熔炉、磁力采石场与潜水艇提供 Create 风格的思索教程，共 **6 页**：

| 触发物品 | 页面 |
|---|---|
| `alexscaves:nuclear_furnace_component` | ① 组装 ② 输入与输出 ③ 失控熔毁 |
| `alexscaves:quarry`、`alexscaves:quarry_smasher` | ① 划定矿区 ② 自动开采 |
| `alexscaves:enigmatic_engine`、`alexscaves:submarine` | ① 搭建与下水 |

- 场景结构（7×5×7）随模组内置，包含逐块落下、白色矿区边界线、爆炸烟幕、镜头转场等动画；
- 磁力采石场两页共用同一份结构（底座为基岩），翻页时地面不会跳动；
- 核能熔炉熔毁页用 `Waste` 方块实体数据演示 4 / 6 / 8 / 10 份废料的递进状态。

### 16. 群系矿石扩展

在 Alex's Caves 群系中新增 4 种矿石，均支持精准采集 / 时运 / 爆炸衰减，挖掘等级为铁镐：

| 矿石 | 基底 | 掉落 | 生成 |
|---|---|---|---|
| 渊海钍矿石 `abyssmarine_thorium_ore` | 渊海石 | `createnuclear:raw_thorium` 粗钍 | 渊海陷窟，9 次 × 11 格 |
| 凝灰岩钍矿石 `tuff_thorium_ore` | 凝灰岩 | `createnuclear:raw_thorium` 粗钍 | 渊海陷窟，5 次 × 9 格 |
| 秽岩铅矿石 `coprolith_lead_ore` | 秽岩 | `createnuclear:raw_lead` 粗铅 | 异寂空谷，8 次 × 10 格 |
| 粪岩硝酸盐矿石 `guanostone_nitrate_ore` | 粪岩 | `createnuclear:nitrate` 硝酸盐 | 异寂空谷，6 次 × 10 格 |

同时调整了创造标签栏：
- 「原子核动」主栏：深层硝酸盐矿石 → 凝灰岩钍矿石 → 渊海钍矿石 → 粪岩硝酸盐矿石 → 秽岩铅矿石 → 辐射铀矿石 → 辐射铅矿石 → 辐射硝酸盐矿石；
- 「渊海陷窟」栏：凝灰岩钍矿石 → 渊海石 → 渊海石瓦 → 渊海钍矿石；
- 「异寂空谷」栏：粪岩红石矿石 → 粪岩硝酸盐矿石；秽岩煤矿石 → 秽岩铅矿石；
- 「毒化洞穴」栏：隐藏 `uranium_shard`（铀晶碎片）与 `uranium`（铀晶），加入 `createnuclear:uranium_powder`（铀粉）与 `createnuclear:raw_uranium`（粗铀），顺序为 铀粉 → 粗铀 → 硫粉。

### 17. 淤泥烧炼配方

新增烧炼配方：`alexscaves:muck`（淤泥）→ `alexscaves:sea_glass_shards`（海洋玻璃碎片）。

- 熔炉：200 tick（10 秒），经验 0.15；
- 高炉：100 tick（5 秒），经验 0.15。

### 18. 烈焰人燃烧室超级燃料

原子核动的钍棒与铀棒可作为 Create 烈焰人燃烧室的超级燃料（通过 `create:superheated_blaze_burner_fuels` 数据图注入，不影响烈焰蛋糕）：

| 燃料 | 超级燃烧时长 |
|---|---|
| `createnuclear:thorium_rod` 钍棒 | 1600 tick（80 秒） |
| `createnuclear:uranium_rod` 铀棒 | 6400 tick（320 秒） |

## 前置

- Minecraft 1.21.1 + NeoForge 21.1.x
- Create（机械动力，`create`）—— **硬依赖**（Create Nuclear 的父模组）
- Create Nuclear（原子核动，`createnuclear`）—— **硬依赖**（直接调用其 `BiomeIrradiationService`）
- Alex's Caves（`alexscaves`）—— **可选依赖**（缺失时 mixin 自动停用）。兼容两个 1.21.1 移植版：
  - Alex's Caves Up（PixellCubed）
  - Alex's Caves Neo（TysonTheEmber）

### 依赖下载（构建前自备，不随仓库提交）

本项目是 Create Nuclear 与 Alex's Caves 的桥接。构建前请自行下载下列前置 jar 放入 `libs/` 目录：

| 模组 | 放入 `libs/` 的文件名 | 出处 |
|---|---|---|
| Create（机械动力） | `create-6.0.10+mc1.21.1.jar` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/create) · [Modrinth](https://modrinth.com/mod/create) · [GitHub 源码](https://github.com/Creators-of-Create/Create) |
| Create Nuclear（原子核动） | `createnuclear-2.0.0-neoforge.jar` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/createnuclear) · [Modrinth](https://modrinth.com/project/z611fdf7) · [GitHub 源码](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge) |
| Alex's Caves Up | `alexscaves-up-0.1.4.jar` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/alexs-caves-up) |
| Alex's Caves Neo | `alexscaves-neo-2.0.3.jar` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/alexs-caves-neo) · [Modrinth](https://modrinth.com/mod/alexscavesneo) |
| Ponder（思索库，Create 内嵌） | `ponder-neoforge-1.0.82+mc1.21.1.jar` | 从 Create 的 jar-in-jar 中提取，或 [Maven](https://maven.createmod.net/) |

> 许可证说明：本项目硬依赖 Create Nuclear 并直接调用其 `BiomeIrradiationService` 等类，
> 而 Create Nuclear 采用 GPL-3.0，故本项目同样以 GPL-3.0 分发。Alex's Caves（Up / Neo 移植版）
> 为可选依赖，本项目仅通过 mixin 在运行时与之交互，不并入其代码。

## 实现要点

- 辐射效果替换在 `MobEffectEvent.Applicable` 中拦截（可取消、且在效果写入实体之前）；
- 核弹群系转化监听 `EntityJoinLevelEvent`（`alexscaves:nuclear_explosion`），延迟一 tick 后
  直接调用 Create Nuclear 的 `BiomeIrradiationService.circularArea`（同步登记 `PersistentIrradiatedZones`）；
- 护甲能力互通通过 mixin 修改 `IrradiatedEffect`（辐照）与 `AcidBlock`（酸液）实现；
- 核能熔炉燃料数量差异通过 mixin 修改 `NuclearFurnaceBlockEntity` 的裂变时间实现；
- 反应堆熔毁联动通过 mixin 修改 `NuclearSirenBlockEntity`（警报器提前响起）、
  `ReactorMeltdownExecutor`（爆炸孵化蛋）与 `NuclearExplosionEntity`（撼地斯拉免疫）实现；
- 反应堆警报器红石输出通过 mixin 修改 `ReactorAlarm`（`isSignalSource` / `getSignal` / `getDirectSignal`）实现；
- 核能警示灯环境侦测通过 mixin 修改 `SirenLightBlockEntity`（每 2 秒侦测一次核危险环境并驱动 `POWERED`）实现；
- 猫/豹猫变 raycat 通过 `EntityTickEvent` + NeoForge data attachment 累计辐照时间实现；
- 上述 mixin 各提供两套（Alex's Caves Up 的 `com.alexscaves` 包名 + Alex's Caves Neo 的
  `com.github.alexmodguy.alexscaves` 包名），`required: false`，运行时按实际加载的版本自动生效；
  两套 mixin 中不依赖 Alex's Caves 具体类的共享逻辑集中在 `NuclearCompatUtil` / `ArmorCompat`；
- 配方 / 掉落修改通过内置数据包覆盖 `data/alexscaves/...` 实现（声明 AFTER alexscaves 保证覆盖优先级）；
- 思索场景在 `FMLClientSetupEvent` 中一次性注册（`PonderIndex.addPlugin`），语言键为 `<modid>.ponder.<sceneId>.header` / `.text_N`；
- 思索结构为 7×5×7 的 `.nbt`，放在 `assets/alexscaves_nuclear_convergence/ponder/<组>/`，场景代码通过 `showSection` / `setBlock` 逐块驱动；
- 新矿石的掉落通过 `loot_table/blocks/*.json` 定义，生成通过 `worldgen/configured_feature` + `placed_feature` + `neoforge:add_features` 组合实现；
- 烈焰人超级燃料通过 `data/create/data_maps/item/superheated_blaze_burner_fuels.json`（合并语义，不覆盖烈焰蛋糕）注入。

## 构建

1. 按上文「依赖下载」把下列前置 jar 放入 `libs/`（该目录已被 `.gitignore` 忽略，不提交）；
2. 运行构建：

```bash
./gradlew build
```

产物位于 `build/libs/`。`libs/` 内的前置 jar 仅用于编译期（compileOnly），不会打进产物。

## 可调项

| 常量 | 位置 | 说明 |
|---|---|---|
| `CN_RADIATION_ID` / `AC_IRRADIATED_ID` | `NuclearCompat.java` | 两个辐射效果的注册名 |
| `IRRADIATION_RADIUS` | `NuclearBombBiomeHandler.java` | 核弹群系转化半径（默认 90，对应核弹默认规模 3.0 × 30） |
| 燃料烧制量 | `NuclearCompatUtil.java` | 铀棒 51200 / 钍棒 12800 裂变时间系数 |
| 核能熔炉烧炼速度 | `mixin/NuclearFurnaceBlockEntity*.java` | `getSpeedReduction()` 返回值 × 0.5（2 倍速） |
| `HAZARD_ENTITY_RADIUS` / `NUCLEAR_FURNACE_RADIUS` | `NuclearCompatUtil.java` | 核危险环境侦测半径（危险源实体 256 / 临界核熔炉 POI 128） |
| 反应堆侦测半径 | `NuclearCompatUtil.findMeltdownReactor` | 默认 32 格（每轴 ±32，立方体，按区块方块实体扫描） |
| 矿石生成频率 | `data/alexscaves_nuclear_convergence/worldgen/placed_feature/*.json` | 各矿石的 `count`（每区块尝试次数） |
| 矿石矿脉大小 | `data/alexscaves_nuclear_convergence/worldgen/configured_feature/*.json` | 各矿石的 `size`（每次矿脉格数） |
| 思索文本时长 | `client/ponder/*PonderScenes.java` | `showText(n)` 的 n（tick），页面节奏由 `idle(n)` 控制 |
| 超级燃料时长 | `data/create/data_maps/item/superheated_blaze_burner_fuels.json` | 钍棒 1600 / 铀棒 6400 tick |

## 许可证

本项目以 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html)（GPL-3.0）分发，详见 `LICENSE`。
