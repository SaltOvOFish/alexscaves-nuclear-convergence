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

> 结构已注册为真正的 `Structure`（`alexscaves_nuclear_convergence:abandoned_reactor`），
> 可用 `/locate structure` 查询；`structure_set` 使用 `random_spread`，`spacing 6` / `separation 3`。
> 群系采样每候选只取 2 个高度（y = -8 / -40，命中即停），避免 `/locate` 在大范围搜索时单 tick 卡顿。

### 9. 辐射岩矿石
在毒化洞穴的辐射岩（radrock）中生成 4 种矿石：

| 矿石 | 掉落 | 挖掘等级 |
|---|---|---|
| `radrock_iron_ore` 辐射铁矿石 | `minecraft:raw_iron` | 石镐 |
| `radrock_coal_ore` 辐射煤矿石 | `minecraft:coal` | 石镐 |
| `radrock_lead_ore` 辐射铅矿石 | `createnuclear:raw_lead` | 铁镐 |
| `radrock_nitrate_ore` 辐射硝酸盐矿石 | `createnuclear:nitrate` | 铁镐 |

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

在 Alex's Caves 群系中新增 5 种矿石，均支持精准采集 / 时运 / 爆炸衰减。挖掘等级：渊海钍矿石与凝灰岩钍矿石为 **钻石镐**（与原子核动的钍矿石对齐，Alex's Caves 的辐射铀矿石也一并提升为钻石镐）；凝灰岩铜矿石、粪岩硝酸盐矿石为 **石镐**；秽岩铅矿石为 **铁镐**：

| 矿石 | 基底 | 掉落 | 生成 |
|---|---|---|---|
| 渊海钍矿石 `abyssmarine_thorium_ore` | 渊海石 | `createnuclear:raw_thorium` 粗钍 | 渊海陷窟，11 次 × 13 格 |
| 凝灰岩钍矿石 `tuff_thorium_ore` | 凝灰岩 | `createnuclear:raw_thorium` 粗钍 | 渊海陷窟，5 次 × 9 格 |
| 凝灰岩铜矿石 `tuff_copper_ore` | 凝灰岩 | `minecraft:raw_copper` 粗铜 | 渊海陷窟，6 次 × 9 格 |
| 秽岩铅矿石 `coprolith_lead_ore` | 秽岩 | `createnuclear:raw_lead` 粗铅 | 异寂空谷，8 次 × 10 格 |
| 粪岩硝酸盐矿石 `guanostone_nitrate_ore` | 粪岩 | `createnuclear:nitrate` 硝酸盐 | 异寂空谷，6 次 × 10 格 |

同时调整了创造标签栏：
- 「原子核动」主栏：深层硝酸盐矿石 → 凝灰岩钍矿石 → 渊海钍矿石 → 粪岩硝酸盐矿石 → 秽岩铅矿石 → 辐射铀矿石 → 辐射铅矿石 → 辐射硝酸盐矿石；
- 「渊海陷窟」栏：凝灰岩铜矿石 → 凝灰岩钍矿石 → 渊海石 → 渊海石瓦 → 渊海钍矿石；
- 「异寂空谷」栏：粪岩红石矿石 → 粪岩硝酸盐矿石；秽岩煤矿石 → 秽岩铅矿石；
- 「毒化洞穴」栏：隐藏 `uranium_shard`（铀晶碎片）、`uranium`（铀晶）与 `block_of_uranium`（铀块），加入 `createnuclear:uranium_powder`（铀粉）与 `createnuclear:raw_uranium`（粗铀），顺序为 铀粉 → 粗铀 → 硫粉；铀块的原位置换成 `createnuclear:raw_uranium_block`（粗铀块），即 粗铀块 → 核弹 → 未精炼的废料；
- 「原始洞穴」栏：`alexscaves_nuclear_convergence:autunite_spear`（钙铀云母长矛）排在石灰岩长矛之后；
- 「糖果龋洞」栏：`alexscaves_nuclear_convergence:nuka_cola`（核子可乐）排在紫色苏打水瓶之后。

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

### 19. 与井然有矿（Oreganized）的可选联动

仅当安装了 `oreganized` 时生效（通过数据包条件与 `ModList` 判断）：

- **生成**：移除原子核动的铅矿与深板岩铅矿（`createnuclear:lead_ore`），主世界只保留 Oreganized 的铅矿与深板岩铅矿；
- **配方**：原子核动的 18 个铅相关配方（熔炼/高炉/工作台/粉碎/洗涤）全部改用 Oreganized 的粗铅、铅锭、铅块、铅粒；未安装 Oreganized 时保持原配方；
- **秽岩铅矿石**：仍在本模组中生成，但掉落改为 `oreganized:raw_lead`，并加入 `oreganized:creates_lead_cloud` 标签，挖掘时与井然有矿的铅矿一样散出铅尘；
- **银矿石**：异寂空谷新增粪岩银矿石与秽岩银矿石（6 次 × 10 格），掉落 `oreganized:raw_silver`，仅在安装 Oreganized 时生成，并在创造栏排在粪岩红石矿石 / 秽岩煤矿石之前；
- **材质**：内置资源包 `Nuclear Convergence: Oreganized Style`（灰紫色铅矿点），默认启用，可在资源包界面关闭。

### 20. 钙铀云母长矛

新增武器 `alexscaves_nuclear_convergence:autunite_spear`（钙铀云母长矛），行为对齐 Alex's Caves 的石灰岩长矛：

- 攻击伤害 3.0，堆叠 16，可蓄力投掷；
- 投掷物为本模组独立实体 `alexscaves_nuclear_convergence:autunite_spear`（`AbstractArrow`）：
  基础伤害 4，命中后不再重复判定，命中生物施加 **20 秒辐照 I**，落地后可直接拾回；
- 音效与石灰岩长矛一致：投掷 `alexscaves:limestone_spear_throw`，命中 / 落地 `alexscaves:limestone_spear_hit`；
- 手持、GUI、飞行三套模型：GUI 为 16×16 图标（`textures/item/autunite_spear.png`），
  手持与飞行用 32×32 UV 展开的 `LimestoneSpearModel` 结构（`textures/entity/autunite_spear.png`）。
  渲染通过 mixin 注入 AC 的 `ACItemstackRenderer#renderByItem` 头部实现，
  物品类本身不引用任何客户端类（服务端安全，也不依赖 `initializeClient` 时序）；
- 投掷时 `throwing` 物品属性单独注册，避免蓄力时套用普通手持变换导致长矛颠倒。

合成：

| 配方 | 摆法 | 产出 |
|---|---|---|
| `autunite_spear` | 形状与 AC 石灰岩长矛相同：3 个钙铀云母（`createnuclear:autunite`）+ 2 根木棍（`#c:rods/wooden`） | 4 根 |
| `autunite_spear_from_limestone` | 8 个「辐射」标签物品（**不消耗**）+ 中心石灰岩长矛（消耗） | 1 根 |

创造标签栏位于「原始洞穴」，紧跟在石灰岩长矛之后。

### 21. 核子可乐

新增可饮用物品 `alexscaves_nuclear_convergence:nuka_cola`（核子可乐），参数对齐 AC 的紫色苏打水瓶：
堆叠 16、营养 2 / 饱和度 0.1、饮用 40 tick、饮用完毕返还玻璃瓶，并使用 `CONSUME_ITEM` 统计与进度触发器。

饮用后同时获得 **10 秒甜蜜冲刺 I**（`alexscaves:sugar_rush`）与 **10 秒辐照 I**（`alexscaves:irradiated`）。

合成（8 个环位放任意「辐射」标签物品，**只消耗中心**）：

```
RRR
RSR      R = #alexscaves_nuclear_convergence:radiation
RRR      S = alexscaves:purple_soda_bottle
```

创造标签栏位于「糖果龋洞」，紧跟在紫色苏打水瓶之后。

### 22. 「辐射」物品标签与催化剂配方

新增物品标签 `#alexscaves_nuclear_convergence:radiation`（共 19 项），覆盖原子核动的全部铀/钍系物品、
Alex's Caves 的辐射铀矿石与未精炼废料，以及本模组新增的两种钍矿石：

```
createnuclear:yellowcake / enriched_yellowcake / raw_uranium / uranium_powder /
uranium_rod / raw_uranium_block / uranium_ore / deepslate_uranium_ore /
raw_thorium / thorium_dust / thorium_nugget / thorium_ingot / thorium_rod /
thorium_ore / deepslate_thorium_ore

alexscaves:radrock_uranium_ore / alexscaves:unrefined_waste
alexscaves_nuclear_convergence:tuff_thorium_ore / alexscaves_nuclear_convergence:abyssmarine_thorium_ore
```

配套新增配方类型 `alexscaves_nuclear_convergence:catalyst_shaped`：继承 `ShapedRecipe` 并重写
`Recipe#getRemainingItems`，命中 `catalysts` 字段的合成格在合成后原样退还（耐久、附魔、组件全部保留），
其余格按原版逻辑正常消耗。核子可乐与「石灰岩长矛 → 钙铀云母长矛」两条配方均用此类型实现「只消耗中心物品」。

> 实现要点：无需注册自定义 `RecipeType` —— `CraftingRecipe#getType()` 默认返回 `RecipeType.CRAFTING`，
> 而 `RecipeManager` 按 `recipe.getType()` 归档，因此工作台的 `getRemainingItemsFor(RecipeType.CRAFTING, ...)`
> 能正常找到这份配方。

### 23. 物品栏辐射扩展

`alexscaves:radrock_uranium_ore`（辐射铀矿石）与 `alexscaves:unrefined_waste`（未精炼的废料）
通过原子核动的公开 API `RadiationRegistry.register().item(...).value(...)` 接入其「物品栏辐射」体系：
携带时会被当作辐射源，按原子核动服务端配置的阈值换算等级后施加辐照。

| 物品 | 辐射值 | 单件效果 |
|---|---|---|
| 辐射铀矿石 `radrock_uranium_ore` | 4.0（与黄饼同级） | 辐照 I |
| 未精炼的废料 `unrefined_waste` | 50.0 | 辐照 III |

> 堆叠会按数量累加（原子核动原版行为），并受 `createnuclear-server.toml` 的
> `enabled_item_radiation` 与 `radiation_level_1/2/3` 控制；穿齐整套防化服 / 防辐射服时仍完全免疫。

### 24. 钙铀云母矿

在毒化洞穴的辐射岩中生成钙铀云母矿（`createnuclear:autunite`），使用 Create 的
`create:layered_ore`（层状矿脉）：两层图案（权重 2 / 2~4 格、权重 1 / 1~2 格），
目标方块 `alexscaves:radrock`，`discard_chance_on_air_exposure: 0.0`。

生成参数：`size 12`、每区块 **3** 次尝试、`in_square` + 高度范围 `above_bottom 0 ~ absolute 256`。

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
- 烈焰人超级燃料通过 `data/create/data_maps/item/superheated_blaze_burner_fuels.json`（合并语义，不覆盖烈焰蛋糕）注入；
- 钙铀云母长矛的投掷物是独立实体类型（不继承 AC 的 `LimestoneSpearEntity`，因为后者的构造函数会忽略传入的
  `ItemStack` 且不同步拾取物），渲染则在客户端 mixin 中接管 AC 的物品渲染器；
- 供核子可乐与长矛转化使用的「催化剂配方」通过自定义 `RecipeSerializer` + 重写 `getRemainingItems` 实现，
  依靠 `CraftingRecipe#getType()` 默认返回 `RecipeType.CRAFTING` 被工作台正常检索；
- 物品栏辐射通过原子核动的 `RadiationRegistry.Builder` 在 `FMLCommonSetupEvent` 中注册；
- 创造标签栏调整统一在 `BuildCreativeModeTabContentsEvent` 中用 `remove` / `insertBefore` / `insertAfter` 完成。

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
| 物品栏辐射值 | `NuclearCompat.registerAcRadiationSources()` | 辐射铀矿石 4.0 / 未精炼废料 50.0 |
| 核子可乐效果时长 | `NukaColaItem.EFFECT_DURATION` | 默认 200 tick（10 秒） |
| 长矛命中辐照时长 | `SpearCompat.IRRADIATED_DURATION` | 默认 400 tick（20 秒） |
| 催化剂配方内容 | `data/alexscaves_nuclear_convergence/recipe/*.json` | 核子可乐、钙铀云母长矛（两条） |
| 「辐射」标签成员 | `data/alexscaves_nuclear_convergence/tags/item/radiation.json` | 改这里即可增删可用材料 |

## 更新日志

### v1.3.1

**新增**
- 钙铀云母长矛 `autunite_spear`：可蓄力投掷，攻击 3.0、堆叠 16；投掷物为独立实体，命中造成 4 点伤害并施加 20 秒辐照 I，落地可拾回；手持 / GUI / 飞行三套模型，音效对齐 AC 石灰岩长矛；
- 核子可乐 `nuka_cola`：堆叠 16、营养 2 / 饱和度 0.1、饮用 40 tick、返还玻璃瓶，饮后同时获得 10 秒甜蜜冲刺 I 与 10 秒辐照 I；
- 物品标签 `#alexscaves_nuclear_convergence:radiation`（19 项：原子核动铀/钍系、AC 辐射铀矿石与未精炼废料、本模组两种钍矿石）；
- 配方类型 `catalyst_shaped`：催化剂合成后不消耗（耐久 / 附魔 / 组件保留），支撑核子可乐与「石灰岩长矛 → 钙铀云母长矛」两条配方；
- 钙铀云母矿：毒化洞穴辐射岩中层状生成，`size 12`、每区块 3 次尝试；
- AC 辐射铀矿石（4.0）与未精炼废料（50.0）接入原子核动的物品栏辐射体系。

**调整**
- 废弃反应堆改为真正的 `Structure`（可用 `/locate structure` 查询），生成参数调稀为 `spacing 6` / `separation 3`，群系采样每候选只取 2 个高度，修复 `/locate` 单 tick 卡顿；
- 创造标签栏：钙铀云母长矛移入「原始洞穴」（石灰岩长矛之后），核子可乐移入「糖果龋洞」（紫色苏打水瓶之后）；
- 「毒化洞穴」栏隐藏 AC 铀块（`block_of_uranium`）并原位换成原子核动的粗铀块，顺序为 粗铀块 → 核弹 → 未精炼的废料；
- 钙铀云母矿生成频率下调（`size` 16 → 12，`count` 6 → 3）。

**修复**
- 核子可乐只生效甜蜜冲刺、没有辐照：原子核动每 tick 在辐射值为 0 时都会调用一次 `removeEffect(createnuclear:radiation)`，而 NeoForge 的 `MobEffectEvent.Remove` 在目标效果不存在时依旧触发，导致桥接逻辑把独立来源的辐照一并删除；改为只在「确实处于辐射→辐照转译状态」时同步清除（同一修复也让长矛命中的 20 秒辐照真正生效）；
- 长矛投掷物改为本模组实体，避免 AC `LimestoneSpearEntity` 固定使用石灰岩矛、拾取物不同步的问题。

**文档**
- README 补充长矛、核子可乐、「辐射」标签、催化剂配方、物品栏辐射与钙铀云母矿说明，并新增本更新日志；
- `neoforge.mods.toml` 描述同步更新。

**其他**
- 此前已在工作区完成但未纳入版本库的内容一并提交归档：与井然有矿（Oreganized）的可选联动、凝灰岩铜矿石、粪岩/秽岩银矿石、内置资源包 `Nuclear Convergence: Oreganized Style`、废弃反应堆结构化的代码实现。

## 许可证

本项目以 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html)（GPL-3.0）分发，详见 `LICENSE`。
