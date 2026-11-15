# Alex's Caves: Nuclear Convergence

一个 NeoForge 1.21.1 桥接模组，把「原子核动（Create Nuclear）」与「Alex's Caves」的铀/辐射体系打通，
统一成一套辐射机制（可配置统一为 AC 的辐照或 CN 的辐射）。

## 功能

### 1. 辐射效果统一
两种辐射体系二选一，由配置项 `general.radiation_system` 决定（默认 `AC`）：

**`AC`（默认）—— 统一为 Alex's Caves 的辐照**
- 拦截 `createnuclear:radiation`（Create Nuclear 的辐射效果），改为施加 `alexscaves:irradiated`
  （Alex's Caves 的辐照效果），等级 / 时长 / 氛围 / 可见性保持一致；
- 当 Create Nuclear 移除辐射效果时，同步移除辐照效果。

**`CN` —— 统一为原子核动的辐射**
- 原子核动的辐射原样生效（HUD 显示辐射条/心跳遮罩）；
- 所有 `alexscaves:irradiated` 的施加（长矛、核子可乐，以及 AC 自己的放射性物品、未精炼废料方块、
  伽马蟑螂、核爆）都会被拦下，按等级折算成辐射剂量：辐照 I / II / III → 剂量 10 / 25 / 50，
  残留时间 = 原效果剩余时长；
- AC 防化服四个部件各 +0.25 `createnuclear:irradiated_resistance`（与原子核动防辐射套装对等）。

切换辐射体系需要重启游戏或重进世界；两种体系下，穿齐整套防化服 / 防辐射套装都免疫辐射（详见第 6 节）。

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

### 9. 矿石与世界生成

本模组新增 11 种矿石（毒化洞穴 4 + 渊海陷窟 3 + 异寂空谷 4），并复用 Create 的层状生成放入钙铀云母矿：

| 矿石 | 基底 | 掉落 | 挖掘等级 | 生成 |
|---|---|---|---|---|
| `radrock_iron_ore` 辐射铁矿石 | 辐射岩 | `minecraft:raw_iron` | 石镐 | 毒化洞穴，6 次 × 9 格 |
| `radrock_coal_ore` 辐射煤矿石 | 辐射岩 | `minecraft:coal` | 石镐 | 毒化洞穴，5 次 × 17 格 |
| `radrock_lead_ore` 辐射铅矿石 | 辐射岩 | `createnuclear:raw_lead`¹ | 铁镐 | 毒化洞穴，5 次 × 12 格 |
| `radrock_nitrate_ore` 辐射硝酸盐矿石 | 辐射岩 | `createnuclear:nitrate` | 铁镐 | 毒化洞穴，4 次 × 10 格 |
| `abyssmarine_thorium_ore` 渊海钍矿石 | 渊海石 | `createnuclear:raw_thorium` | 钻石镐 | 渊海陷窟，16 次 × 13 格 |
| `tuff_thorium_ore` 凝灰岩钍矿石 | 凝灰岩 | `createnuclear:raw_thorium` | 钻石镐 | 渊海陷窟，32 次 × 9 格 |
| `tuff_copper_ore` 凝灰岩铜矿石 | 凝灰岩 | `minecraft:raw_copper` | 石镐 | 渊海陷窟，40 次 × 9 格 |
| `coprolith_lead_ore` 秽岩铅矿石 | 秽岩 | `createnuclear:raw_lead`¹ | 铁镐 | 异寂空谷，8 次 × 10 格 |
| `guanostone_nitrate_ore` 粪岩硝酸盐矿石 | 粪岩 | `createnuclear:nitrate` | 石镐 | 异寂空谷，6 次 × 10 格 |
| `coprolith_silver_ore` 秽岩银矿石 | 秽岩 | `oreganized:raw_silver` | 铁镐 | 异寂空谷，6 次 × 10 格（仅井然有矿） |
| `guanostone_silver_ore` 粪岩银矿石 | 粪岩 | `oreganized:raw_silver` | 铁镐 | 异寂空谷，6 次 × 10 格（仅井然有矿） |
| 钙铀云母矿 `createnuclear:autunite` | 辐射岩 | `createnuclear:autunite` | — | 毒化洞穴，`create:layered_ore` 层状，3 次 × 12 格 |

¹ 安装井然有矿并开启 `full_lead_integration` 时改掉 `oreganized:raw_lead`（见第 19 节）。

补充说明：
- 普通矿石均支持精准采集 / 时运 / 爆炸衰减；钙铀云母矿使用 Create 的层状生成（高度 `above_bottom 0 ~ absolute 256`，暴露空气不消失）；
- 顺带把 AC 的辐射铀矿石也提升为**钻石镐**；其余镐级见上表；
- 生成参数可在 `data/alexscaves_nuclear_convergence/worldgen/` 下调整（见「可调项」）；
- 全部矿石已登记进 `c:ores` 与 `c:ores/<材质>`（方块 + 物品），铁/煤/铜矿同时登记进
  `minecraft:{iron,coal,copper}_ores`，便于其它模组按材质识别与处理。

### 10. 机械动力自动化兼容
- 为 Alex's Caves 的核能熔炉（含 4×4 组件方块）、金属桶/锈蚀金属桶、渊海祭坛补充
  NeoForge 的 `ItemHandler` capability，使 Create 的漏斗、溜槽、动力机械臂等能正常输入输出；
- 渊海祭坛注册为机械臂交互点类型（类似置物台），机械臂可从中拿取/放置物品；
- 手持 Create 扳手或机械臂物品右键渊海祭坛时放行，避免误把工具放进祭坛。

### 11. 反应堆熔毁与警报器 / 撼地斯拉孵化联动
- 当原子核动的反应堆热量达到 DANGER（进入失控爆炸倒计时）时，附近的 Alex's Caves 核能警报器提前响起；
- 反应堆失控爆炸时，会孵化爆炸范围内（36 格）的 `alexscaves:tremorzilla_egg`（撼地斯拉蛋），
  且孵出的撼地斯拉对本次核爆免疫（不会被秒杀或击飞），复刻 Alex's Caves 核弹孵化 Boss 的行为；
- 撼地斯拉对原子核动的辐射效果（`createnuclear:radiation`）**整体免疫**：站在辐照之地、被反应堆
  泄漏波及，或直接用 `/effect` 施加都不会生效（AC 本身已对辐照免疫，这里把 CN 的效果一并挡住，
  两种辐射体系下行为一致）。

### 12. 辐射生物转化
- 原版的猫（Cat）与豹猫（Ocelot）在持续受到辐照 III（`CN` 模式下为辐射 III）及以上效果
  45 秒后，转化为 Alex's Caves 的辐射猫（`raycat`）。

### 13. 反应堆警报器红石输出
原子核动的**反应堆警报器**在反应堆热量达到 DANGER（失控预警、警报响起）时，除响铃外还会
**对外输出 15 级红石信号**（六面全向），便于用原版红石或机械动力自动化联动（自动注入冷却、
拉闸、报警灯、紧急停机等）。

> 说明：警报器的 `POWERED` 状态同时可由红石输入驱动，因此手动为其供电时也会输出。

### 14. 核能警示灯环境侦测
Alex's Caves 的**核能警示灯**（`alexscaves:siren_light`）除了响应红石信号，还会像核能警报器
一样侦测周围核危险环境，并在有危险时**自动点亮、旋转、发光**：
- 原子核动反应堆失控（热量 DANGER，半径 32 格）；
- Alex's Caves 核弹（`alexscaves:nuclear_bomb`，半径 256 格）；
- 撼地斯拉吐息、核爆怪 Nucleeper（实现 `ActivatesSirens` 接口，半径 256 格）；
- Alex's Caves 临界核熔炉（POI 查找，半径 128 格）。

无危险时回退为由红石输入控制（原行为不变）。

> 性能：64 格内没有玩家时直接跳过侦测；多台设备按坐标错开相位、并按区块缓存结果 10 tick，避免同一 tick 集体扫描。

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

### 16. 创造标签栏调整

- 「原子核动」主栏：深层硝酸盐矿石 → 凝灰岩钍矿石 → 渊海钍矿石 → 粪岩硝酸盐矿石 → 秽岩铅矿石 → 辐射铀矿石 → 辐射铅矿石 → 辐射硝酸盐矿石；
- 「渊海陷窟」栏：凝灰岩铜矿石 → 凝灰岩钍矿石 → 渊海石 → 渊海石瓦 → 渊海钍矿石；
- 「异寂空谷」栏：粪岩红石矿石 → 粪岩硝酸盐矿石；秽岩煤矿石 → 秽岩铅矿石；
- 「毒化洞穴」栏：隐藏 `uranium_shard` / `uranium` / `block_of_uranium`，加入铀粉与粗铀（铀粉 → 粗铀 → 硫粉）；铀块原位换成原子核动的粗铀块（粗铀块 → 核弹 → 未精炼的废料）；
- 「原始洞穴」栏：钙铀云母长矛排在石灰岩长矛之后；
- 「糖果龋洞」栏：核子可乐排在紫色苏打水瓶之后。

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
- **铅矿石**：秽岩铅矿石与辐射铅矿石仍在本模组中生成，但掉落改为 `oreganized:raw_lead`，并加入 `oreganized:creates_lead_cloud` 标签，挖掘时与井然有矿的铅矿一样散出铅尘；
- **银矿石**：异寂空谷新增两种银矿石（生成参数见第 9 节），掉落 `oreganized:raw_silver`，仅在安装 Oreganized 时生成；
- **材质**：内置资源包 `Nuclear Convergence: Oreganized Style` 把**秽岩铅矿石**与**辐射铅矿石**的矿点统一为井然有矿的灰紫色铅矿点（基底贴图不变），并包含防辐射服模型贴图（默认 + 16 种颜色）与反应堆强化玻璃贴图，界面图标为辐射铅矿石纹理；该资源包默认不勾选，需在资源包界面启用（`lead_style_pack` 只控制是否注册它）；
- **铅中毒免疫**：AC 防化服四件套与原子核动防辐射四件套登记进 `oreganized:lead_protection/armor_set`，四件齐全即免疫铅尘云的毒素效果（毒/眩晕）。

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

已登记进 `minecraft:swords`，与 AC 的石灰岩/灭绝长矛一致，可正常获得武器类附魔。

### 21. 核子可乐

新增可饮用物品 `alexscaves_nuclear_convergence:nuka_cola`（核子可乐），参数对齐 AC 的紫色苏打水瓶：
堆叠 16、营养 2 / 饱和度 0.1、饮用 40 tick、饮用完毕返还玻璃瓶；使用 `CONSUME_ITEM` 统计，
并已登记进 `c:foods` 与 `c:drinks`。

饮用后同时获得 **20 秒甜蜜冲刺 I**（`alexscaves:sugar_rush`）与 **20 秒辐照 I**（`alexscaves:irradiated`）。

合成（8 个环位放任意「辐射」标签物品，**只消耗中心**）：

```
RRR
RSR      R = #alexscaves_nuclear_convergence:radiation
RRR      S = alexscaves:purple_soda_bottle
```

累计喝下 64 瓶可解锁隐藏挑战进度（详见第 24 节）；计数通过自定义触发器 `drank_nuka_cola`
与玩家附件累计，随存档保留，不要求连续或同一次登录喝完。

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
> `enabled_item_radiation` 与 `radiation_level_1/2/3` 控制。

### 24. 进度（Advancements）

新增 5 条隐藏进度，均位于 Alex's Caves 进度树内：

| 进度 | 挂载点 | 解锁条件 |
|---|---|---|
| 哇，宾周在发光！ `nuka_cola_glow` | 碳酸浴 | 饮用核子可乐 |
| 你太依赖斯安威斯坦了 `sandevistan` | 哇，宾周在发光！ | 累计喝下 64 瓶核子可乐（挑战框，图标为 AC 甜蜜冲刺效果） |
| 原始人的核武器 `primitive_nuke` | 基岩小镇 | 获得钙铀云母长矛 |
| 切尔诺贝利 `chernobyl` | 警告：检测到大量辐射 | 进入废弃反应堆结构范围 |
| 爱，深潜，与机器人 `love_deep_and_robots` | 友好协定 | 机械臂向渊海祭坛放入珍珠（授予 16 格内最近玩家）|

同时覆盖 AC 的两条进度，让它们检测原子核动物品：淘铀热 → `createnuclear:raw_uranium`，
热棒 → `createnuclear:uranium_rod`（图标同步替换）。

### 25. 头盔视野遮罩

- 原子核动防辐射头盔自带第一人称全屏遮罩（按剩余耐久分 5 档换贴图）；本模组把这套贴图**内置**，
  并给 AC 防化头盔（`alexscaves:hazmat_mask`）补上完全一致的效果（同样 5 档、同样贴图）；
- 两套遮罩都**没有配置开关**，默认按原样渲染；内置资源包
  **`Nuclear Convergence: Clear Helmet Vision`**（默认不启用）会把两套遮罩贴图替换为透明，
  勾选它即可关掉视野遮挡（想恢复就取消勾选）；
- 两个内置资源包（`Oreganized Style` / `Clear Helmet Vision`）的**名称与说明都走语言文件**
  （`resourcepack.alexscaves_nuclear_convergence.<包名>.name` / `.description`），
  中文环境显示为「核力汇聚：井然有矿风格」「核力汇聚：清晰头盔视野」，英文环境保持原英文名。

## 前置

- Minecraft 1.21.1 + NeoForge 21.1.x
- Create（机械动力，`create`）—— **硬依赖**（Create Nuclear 的父模组）
- Create Nuclear（原子核动，`createnuclear`）—— **硬依赖**（直接调用其 `BiomeIrradiationService`）
- Alex's Caves（`alexscaves`）—— **可选依赖**（缺失时 mixin 自动停用）。兼容以下 1.21.1 移植版：
  - Alex's Caves Up（PixellCubed）—— 包名 `com.alexscaves`，需要前置 **Citadel Up**
  - Alex's Caves Neo（TysonTheEmber）—— 包名 `com.github.alexmodguy.alexscaves`，需要前置 **Citadel**
  - Alex's Caves Continued（Codx-org）—— 同包名，辐照效果走自建 `ACMobEffect#tick`，需要前置 **CodxLib**
  - Alex's Caves (Unofficial Port)（Raguto）—— 同包名，需要前置 **Citadel (Unofficial Port)**
  - Alex's Caves RENEWED（fmpapierz）—— 同包名，无前置

  这些移植版共用 `alexscaves` 这个 mod id，数据包无法区分，所以兼容处理全部在代码层：
  启动时日志会打印 `Alex's Caves flavour: ...`，反馈问题时可以直接附上。

  | 移植版 | 包名 | 辐照效果注入点 |
  |---|---|---|
  | AC Up | `com.alexscaves` | `applyEffectTick` |
  | AC Neo | `com.github.alexmodguy.alexscaves` | `applyEffectTick` |
  | Continued | 同上 | **`tick`**（自建 `ACMobEffect` 抽象） |
  | Unofficial Port | 同上 | `applyEffectTick` |
  | RENEWED | 同上 | `applyEffectTick` |

  Neo 系移植版（Neo / Unofficial / RENEWED / Continued）的长矛投掷音效按注册名查询
  （`alexscaves:limestone_spear_throw`），不依赖各版本类型不一的 `ACSoundRegistry` 字段；
  AC Up 仍使用其自带的 `DeferredHolder` 字段。

  **未安装 Alex's Caves 时**模组仍可正常启动：世界生成 / 配方 / 进度 / 结构等数据包内容都带了
  `neoforge:mod_loaded` 条件，会自动停用，**创建世界不会因缺少注册项而报错**；但每次进入标题界面都会弹出安装提示；
  安装任意一款移植版（它们共用 `alexscaves`）后不再出现。提示内容：五个移植版各占一行，
  后面直接给出可用的 CurseForge / Modrinth 入口（未在该平台上架的项目按钮置灰）；
  需要前置的移植版在次行列出前置及其入口（Up 需 Citadel Up；Neo 需 Citadel；Unofficial 需
  Citadel (Unofficial Port)；Continued 需 CodxLib；RENEWED 无前置）。提示不写配置文件，关闭后本次启动不再弹出；
下次启动仍缺 AC 时会再次提示。

### 依赖下载（构建前自备，不随仓库提交）

本项目是 Create Nuclear 与 Alex's Caves 的桥接。构建前请自行下载下列前置 jar 放入 `libs/` 目录：

| 模组 | 放入 `libs/` 的文件名 | 出处 |
|---|---|---|
| Create（机械动力） | `create-6.0.10+mc1.21.1.jar` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/create) · [Modrinth](https://modrinth.com/mod/create) · [GitHub 源码](https://github.com/Creators-of-Create/Create) |
| Create Nuclear（原子核动） | `createnuclear-2.0.1-neoforge.jar` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/createnuclear) · [Modrinth](https://modrinth.com/project/z611fdf7) · [GitHub 源码](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge) |
| Alex's Caves Up | `alexscaves-up-0.1.4.jar` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/alexs-caves-up) |
| Alex's Caves Neo | `alexscaves-neo-2.0.3.jar` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/alexs-caves-neo) · [Modrinth](https://modrinth.com/mod/alexscavesneo) |
| Ponder（思索库，Create 内嵌） | `ponder-neoforge-1.0.82+mc1.21.1.jar` | 从 Create 的 jar-in-jar 中提取，或 [Maven](https://maven.createmod.net/) |

> 上表只列 `build.gradle` 实际引用的编译期依赖。Continued / Unofficial Port / RENEWED 的 jar 只用于兼容性核对，无需放入 `libs/`。
>
> 许可证说明：本项目硬依赖 Create Nuclear 并直接调用其 `BiomeIrradiationService` 等类，
> 而 Create Nuclear 采用 GPL-3.0，故本项目同样以 GPL-3.0 分发。Alex's Caves（Up / Neo 移植版）
> 为可选依赖，本项目仅通过 mixin 在运行时与之交互，不并入其代码。

## 实现要点

- 辐射效果替换在 `MobEffectEvent.Applicable` 中拦截（可取消、且在效果写入实体之前）；
- 核弹群系转化监听 `EntityJoinLevelEvent`（`alexscaves:nuclear_explosion`），延迟一 tick 后
  直接调用 Create Nuclear 的 `BiomeIrradiationService.circularArea`（同步登记 `PersistentIrradiatedZones`）；
- 护甲互通、熔炉燃料、熔毁联动、警报器红石与警示灯侦测均通过 mixin 实现，目标类分别是
  `IrradiatedEffect` / `AcidBlock`、`NuclearFurnaceBlockEntity`、
  `NuclearSirenBlockEntity` / `ReactorMeltdownExecutor` / `NuclearExplosionEntity`、
  `ReactorAlarm`（`isSignalSource` / `getSignal` / `getDirectSignal`）、
  `SirenLightBlockEntity`（40 tick 周期、按坐标错开相位，仅附近有玩家时侦测，结果按区块缓存 10 tick）；
- 猫/豹猫变 raycat 通过 `EntityTickEvent` + NeoForge data attachment 累计辐照时间实现；
- mixin 按移植版分套（AC Up 的 `com.alexscaves`、Neo 系移植版的 `com.github.alexmodguy.alexscaves`、
  以及 Continued 的 `tick` 注入），`required: false`，运行时按实际加载的版本自动生效；
  不依赖 AC 具体类的共享逻辑集中在 `NuclearCompatUtil` / `ArmorCompat` / `IrradiatedEffectSupport` / `SirenSupport`；
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
./gradlew build          # Linux / macOS
.\gradlew.bat build      # Windows
```

产物位于 `build/libs/`。`libs/` 内的前置 jar 仅用于编译期（compileOnly），不会打进产物。

## 可调项

| 常量 | 位置 | 说明 |
|---|---|---|
| `CN_RADIATION_ID` / `AC_IRRADIATED_ID` | `NuclearCompat.java` | 两个辐射效果的注册名 |
| `IRRADIATION_RADIUS` | `NuclearBombBiomeHandler.java` | 核弹群系转化半径（默认 90，对应核弹默认规模 3.0 × 30） |
| 燃料烧制量 | `NuclearCompatUtil.java` | 铀棒 51200 / 钍棒 12800 裂变时间系数 |
| 核能熔炉烧炼速度 | `NuclearCompatUtil.boostedSpeedReduction()` | `getSpeedReduction()` 返回值 × 0.5（2 倍速），由 `NuclearFurnaceBlockEntity*Mixin` 调用 |
| `HAZARD_ENTITY_RADIUS` / `NUCLEAR_FURNACE_RADIUS` | `NuclearCompatUtil.java` | 核危险环境侦测半径（危险源实体 256 / 临界核熔炉 POI 128） |
| `HAZARD_CACHE_TTL` | `NuclearCompatUtil.java` | 核危险侦测结果按区块缓存时长（默认 10 tick）；扫描周期 / 相位见 `SirenSupport` |
| 反应堆侦测半径 | `NuclearCompatUtil.findMeltdownReactor` | 默认 32 格（每轴 ±32，立方体，按区块方块实体扫描） |
| 矿石生成频率 | `data/alexscaves_nuclear_convergence/worldgen/placed_feature/*.json` | 各矿石的 `count`（每区块尝试次数） |
| 矿石矿脉大小 | `data/alexscaves_nuclear_convergence/worldgen/configured_feature/*.json` | 各矿石的 `size`（每次矿脉格数） |
| 思索文本时长 | `client/ponder/*PonderScenes.java` | `showText(n)` 的 n（tick），页面节奏由 `idle(n)` 控制 |
| 超级燃料时长 | `data/create/data_maps/item/superheated_blaze_burner_fuels.json` | 钍棒 1600 / 铀棒 6400 tick |
| 物品栏辐射值 | `NuclearCompat.registerAcRadiationSources()` | 辐射铀矿石 4.0 / 未精炼废料 50.0 |
| 核子可乐效果时长 | `NukaColaItem.EFFECT_DURATION` | 默认 400 tick（20 秒，甜蜜冲刺与辐照共用） |
| 长矛命中辐照时长 | `SpearCompat.IRRADIATED_DURATION` | 默认 400 tick（20 秒） |
| 催化剂配方内容 | `data/alexscaves_nuclear_convergence/recipe/*.json` | 核子可乐、钙铀云母长矛（两条） |
| 「辐射」标签成员 | `data/alexscaves_nuclear_convergence/tags/item/radiation.json` | 改这里即可增删可用材料 |

## 配置

配置文件：`config/alexscaves_nuclear_convergence-common.toml`（COMMON，首次启动后生成）。

### 一、世界生成 `[worldgen]`

| 项 | 默认 | 说明 |
|---|---|---|
| `reactor_chance` | 1.0 | 废弃反应堆生成概率：**0 = 不生成**，1 = 正常；0~1 之间按概率跳过候选区块 |
| `[worldgen.ores]` 下的 12 项 | 全部 1.0 | 每种矿石的密度倍率：**0 = 不生成**，1 = 原本密度，可填小数（0.5 = 一半） |

矿石项按群系分组排列（与配置文件里的顺序一致）：

- 毒化洞穴：`autunite_striated_toxic_caves`（钙铀云母）、`radrock_iron_ore`、`radrock_coal_ore`、
  `radrock_lead_ore`、`radrock_nitrate_ore`；
- 渊海陷窟：`abyssmarine_thorium_ore`、`tuff_thorium_ore`、`tuff_copper_ore`；
- 异寂空谷：`coprolith_lead_ore`、`guanostone_nitrate_ore`、`coprolith_silver_ore`、`guanostone_silver_ore`。

### 二、井然有矿 `[oreganized]`

| 项 | 默认 | 说明 |
|---|---|---|
| `full_lead_integration` | true | 完全使用 Oreganized 的铅体系：开启时禁用原子核动自身的铅矿生成、18 个铅相关配方改用 Oreganized 材料，本模组铅矿也掉落 `oreganized:raw_lead` |
| `lead_style_pack` | true | 是否注册内置资源包 `Nuclear Convergence: Oreganized Style`（铅矿点、防辐射服、反应堆强化玻璃的暗紫配色；仍需在资源包界面勾选） |

### 三、其它 `[general]`

| 项 | 默认 | 说明 |
|---|---|---|
| `enhanced_anti_radiation_armor` | true | 增强防辐射套装：头盔 2 / 胸甲 6 / 护腿 5 / 靴子 2，每件 +0.1 击退抗性 |
| `radiation_system` | `AC` | 辐射体系：`AC` = 统一为 Alex's Caves 的辐照；`CN` = 统一为原子核动的辐射（带 HUD 辐射条，AC 辐照来源折算成辐射剂量） |

> 世界生成与数据包条件在**数据包加载时**求值：改完配置后 `/reload` 即可让矿石 / 反应堆 / 铅整合开关生效（已生成的区块不受影响）；护甲增强、辐射体系切换需要在退出重进世界（或重启）后完全生效。
>
> 头盔视野遮罩不占用配置项，统一由内置资源包 `Nuclear Convergence: Clear Helmet Vision` 控制：勾选 = 不遮挡，取消勾选 = 遮挡（默认）。
>
> 配置文件里的每条注释都是**中英双语**（中文在上、英文在下），英文环境也能直接阅读。

## 许可证

本项目以 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html)（GPL-3.0）分发，详见 `LICENSE`。
