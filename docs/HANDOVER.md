# 交接日志 — Alex's Caves: Nuclear Convergence

> 面向：接手本项目的开发者 / AI
> 编写时间：2026-11-15
> 覆盖范围：v1.4.1 → 当前 HEAD（v1.5.0 基线 + 12 个审校/优化 commit）
> 本文取代旧版 `HANDOVER.md`（v1.5.0 时代编写），差异见 §7「对旧交接文档的更正」

---

## 1. 当前状态速览

| 项 | 值 |
|---|---|
| 分支 | `main`，**领先 `origin/main` 12 个 commit** |
| 最近 commit | `d8d3b30 维护性：收敛散落的魔法数字为具名常量` |
| 版本号 | `1.5.0`（`gradle.properties` 未改；审校内容已写入 README 的 v1.5.1 段） |
| 最新 tag | `v1.4.1`（**本次未打任何 tag**） |
| 构建 | `.\gradlew.bat clean build --console=plain` 通过，**0 警告** |
| 产物 | `build\libs\alexscaves_nuclear_convergence-1.5.0.jar`，733,086 字节，408 条目 |
| 规模 | Java 60 个文件 / 4,894 行；资源 227 个文件；混入 19 个（17 通用 + 2 客户端） |

**未做**（按作者要求）：未 push、未打 tag、未部署到任何实例、未创建 Release。

### 部署前必读

如果要把当前 jar 部署到整合包：

1. 先删掉 `mods` 里另一个版本号的同名 mod jar，否则同 modid 双加载直接崩；
2. **部署时游戏不能正在运行**（运行中替换会让已加载的 zip 变成撕裂读，表现为资源随机缺失、日志刷 `ZipException`）；
3. 两处部署路径需同步：
   - `E:\Minecraft\.minecraft\versions\1.21.1-NeoForge_21.1.248\mods\`
   - `E:\Minecraft\动力之旅\mods\`

---

## 2. 本次交付的 12 个 commit

按时间顺序，每个都可独立回滚。

| # | commit | 主题 | 类型 |
|---|---|---|---|
| 1 | `0b8e932` | v1.5.0 基线快照 | 基线 |
| 2 | `0c089b2` | AC 缺失时的两处启动/运行期崩溃 | **P0** |
| 3 | `2c9f1ba` | DefaultedRegistry 当可空用的 5 处隐患 | **P1** |
| 4 | `8b101b8` | 移除 3 处弃用 `EventBusSubscriber.bus` 参数 | P1 |
| 5 | `b9f6902` | 52 个 JSON 去 UTF-8 BOM | P2 |
| 6 | `c320523` | 日志前缀与过时单移植版措辞 | P1 |
| 7 | `e982174` | 三套辐照混入 → `IrradiatedEffectSupport` | 重构 |
| 8 | `ee74d82` | 警报器/警示灯 → `SirenSupport` | 重构 |
| 9 | `d254f1f` | 长矛投掷 → `SpearCompat.throwSpear` | 重构 |
| 10 | `59ffc35` | 长矛渲染 → `AutuniteSpearRenderSupport` | 重构 |
| 11 | `7f6160d` | 面罩判定每帧查表 → 引用比较 | 性能 |
| 12 | `d8d3b30` | 魔法数字收敛为具名常量 | 维护性 |

净变化：+约 500 / −约 350 行，新增 4 个支撑类，删除 0 个类。

---

## 3. 两个 P0 的细节（重要）

两处问题同源：**把可选依赖的缺失当成不可恢复错误**。都只在「未安装 Alex's Caves」时触发，
但只要 `mods.toml` 里 `alexscaves` 仍是 `optional`，这就是必须修的契约违反。

### P0-1 长矛注册抛异常 → 整个游戏起不来

`ModBlocks.createAutuniteSpear()` 原本在两版 AC 都不可用时 `throw new IllegalStateException`。
该 supplier 在 `RegisterEvent` 分发物品注册时执行，异常会中断模组加载。

**修复**：降级注册为普通 `Item` 占位并 `LOGGER.warn`。注册名保持不变，
所以数据包/配方引用不失效，只是失去投掷与命中施毒行为。

**为什么不用「改成 required」**：作者选择保持 `optional` 语义。

### P0-2 能力注册到 `minecraft:air`

`ModCapabilities.registerNuclearFurnaceComponent()` 用 `if (component == null)` 判空，
但 `BuiltInRegistries.BLOCK` 是 `DefaultedRegistry`，**缺失时返回 `minecraft:air` 而非 null**。
于是 AC 缺失时会把 `ItemHandler` 能力注册到空气方块上，而 provider 里
`(WorldlyContainerHolder) state.getBlock()` 的强转会在任何模组查询空气位时抛 `ClassCastException`。

**修复**：新增 `blockOrNull()`，解析后回查 `getKey(...)` 是否等于目标 id。

---

## 4. 本次新增的可复用约定：`DefaultedRegistry` 三件套

这是本次审校最核心的收获，**后续任何「按注册名取注册表对象」的代码都必须遵守**。

`BuiltInRegistries` 下这几个注册表是 `DefaultedRegistry`——查不到时返回默认值，**永不返回 null**：

| 注册表 | 缺失时返回 |
|---|---|
| `BLOCK` | `minecraft:air` |
| `ITEM` | `minecraft:air` |
| `ENTITY_TYPE` | `minecraft:pig` |
| `MOB_EFFECT` | 同理（用 `getHolder()` 才可得 `Optional`） |

因此 `xxx != null` 型判空**全是死代码**。正确写法：

```java
ResourceLocation id = ResourceLocation.fromNamespaceAndPath("alexscaves", "tremorzilla");
EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
if (!id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type))) {
    return;   // 真·缺失
}
```

本次修复的 5 处（commit `2c9f1ba`）分别会造成：
猫→猪转化、核爆伤害误过滤猪、熔毁孵蛋对每个空气方块做反射、结构基座填空气、机械臂认不出祭坛。

**反例提醒**：`NuclearBombBiomeHandler` 原本就写对了（回查 + 懒加载），可作参考实现。

---

## 5. 设计红线（沿用旧文档，逐条复核过，仍然有效）

1. **辐射桥接用 `MobEffectEvent.Applicable`，不用 `Added`** —— `Added` 不可取消且效果已写入实体。
2. **注册表对象一律运行时按名解析，禁止静态初始化缓存** —— 曾经因此导致「不戴头盔反而显示遮罩」。
   本次 `AbyssalAltarInteractionPointType` 就是照此修正的（改为首次使用时惰性解析）。
3. **CN 模式必须用 `RadiationCapability.applyContagion`**，不能直接 `setRadiation`（会被每 tick 重算覆盖）。
4. **护甲属性修饰符 ID 必须按部件区分** —— 实体属性表以 ID 为键，共用会互相覆盖。
5. **混入按包名分三套（Up / Neo / Continued），`required: false` 是有意为之。**
6. **`mods.toml` 里 `alexscaves` 故意不写版本区间** —— 五个移植版版本号体系互不相干。
7. **`CONVERTED` 用 `WeakHashMap`** —— CN 每 tick 在辐射值为 0 时也调 `removeEffect`，不区分会误删核子可乐等独立来源。
8. **AC 模式下不刷新辐照时长** —— 否则倒计时永不递减、周期伤害永不触发。

---

## 6. 重构后的代码结构变化

新增 4 个共享支撑类，全部在 `com.soof.nuclearcompat` 内部，**不对外暴露**：

| 类 | 职责 | 调用方 |
|---|---|---|
| `IrradiatedEffectSupport` | 辐照 Holder 惰性缓存；整套护甲免疫判定 | 3 套 `IrradiatedEffect*Mixin` |
| `SirenSupport` | 扫描周期、相位打散、点亮条件 | 警报器/警示灯 4 个混入 |
| `AutuniteSpearRenderSupport` | 变换矩阵、push/pop、反射 Method 缓存 | `AutuniteSpearRender` / `...Neo` |
| `SpearCompat`（扩充） | 新增 `throwSpear()`，两版长矛共用 | `AutuniteSpearItem` / `...Neo` |

**混入类一个都没合并**：`@Mixin` 目标无法参数化。现在的分工是
「混入保留注入框架（`@Inject` / `@Redirect` / `@Unique` / NBT），判断逻辑在支撑类」。

重构纪律：采用**纯搬移**（不做参数化、不按 flavour 加分支）。日后若某个移植版需要行为分叉，
应把该段逻辑从支撑类搬回对应混入，而不是在支撑类里加 `if`。

**时钟序保证**：`SpearCompat.throwSpear` 的音效参数用 `Supplier<SoundEvent>` 而非直接传值，
目的是保持「先 `addFreshEntity`、后取音效」的原始求值顺序。改回直接传值会改变异常路径下的行为。

---

## 7. 对旧交接文档的更正

| 旧文档说法 | 实际情况 |
|---|---|
| §7.10「`useCreateNuclearRadiation()` 等在高频路径被调用，可缓存到 volatile」 | **误判**。`ModConfigSpec.ConfigValue#get()` 内部已有 `cachedValue` 字段（源码注释原文 "This getter is cached"），`afterReload()` 会自动 `clearCache()`。开销只是一次字段读 + 判空。**不要加缓存层**，加了反而可能引入「改配置不生效」。本次曾实施后已撤回。 |
| §7.11「`HazmatVisionOverlay` 每帧 `ITEM.getKey`」 | 属实，已修：惰性缓存 `Item` + 引用比较，且**只缓存命中结果**（解析失败返回 AIR 但不写缓存）。 |
| §1「57 个 Java 文件 / 4,729 行」 | 审校前实为 58 个文件；本次新增 2 个支撑类后为 **60 个文件 / 4,894 行**。 |
| §7.16「预计 4 条 `EventBusSubscriber.bus` 弃用警告」 | 实际 **6 条**（3 个文件 × `bus()` 与 `Bus` 各一条）。已清零。 |
| §9「CI 必然失败」 | 属实，**本次未修**（见 §9 待办）。 |
| §7.6「钍矿标签冗余」 | 属实（同时出现在 `needs_iron_tool` 与 `needs_diamond_tool`，后者生效），**本次未动**——属数据包内容，按约定不擅自改。 |

---

## 8. 构建链备忘

- 构建：`.\gradlew.bat clean build --console=plain`
- **复核编译警告必须加 `--rerun-tasks --no-build-cache`**，否则 `compileJava` 命中缓存时不会打印任何警告，会误以为「零警告」。
- `libs/` 下 10 个编译期 jar 被 `.gitignore` 忽略，不入库。
- 验证 jar：用 `[IO.Compression.ZipFile]::OpenRead(...)` 列条目、读 `mixins.json`。
- `javap` 路径：`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\javap.exe`；
  MC 源码：`build\moddev\artifacts\neoforge-21.1.248-sources.jar`。

### 环境坑（PowerShell）

1. **写 Java 文件会加 BOM** → 编译报「非法字符」。用 `New-Object System.Text.UTF8Encoding($false)`。
2. **脚本文件里的中文路径**读取时会被按 ANSI 解释而损坏。用通配符解析目录，别在脚本里写死中文路径字面量。
3. **不支持 heredoc**（`<<EOF`）。多行 commit message 先写文件，再 `git commit -F 文件`。
4. **commit message 里带全角引号/中文**经命令行传入会被 PowerShell 拆碎，同上用 `-F`。
5. `cmd /c ... 2>nul` 会炸，用 `-ErrorAction SilentlyContinue`。

---

## 9. 仍待处理

| 项 | 优先级 | 说明 |
|---|---|---|
| **五个移植版实测** | **P0** | 本次所有重构都只做了静态核对（javap 已逐一比对方法表），**必须进游戏确认**。见 §10 |
| CI 修复 | P1 | `.github/workflows/build.yml` 仍必然失败（`libs/` 未入库）。方向：workflow 里下载并校验 SHA256 |
| 混入日志噪音 | P2 | 每个移植版都会有若干「目标方法不存在」ERROR，**这是设计使然**（`required: false`）。可考虑拆 mixin 配置文件按 flavour 激活，但**不要**改成 `required: true` |
| 钍矿标签冗余 | P2 | 需作者授权后才能动数据包 |
| README §20 表述 | P2 | 声称音效「按注册名查询」，但 Up 版仍直接读 `ACSoundRegistry`（Up 用 `DeferredHolder`，本身没问题）。属文档措辞 |

---

## 10. 待人工验证清单（我无法启动 Minecraft 客户端）

### 必查：五个 AC 变体各开一次实例

| 检查项 | 预期 |
|---|---|
| 启动日志 | 有 `Alex's Caves flavour: ...`，识别正确 |
| 混入日志 | 只有「目标不存在」类预期错误，**无 `InjectionError` 连锁** |
| 护甲 | 整套防辐射服 15 护甲 / 0.4 击退抗性 |
| 辐照免疫 | 穿整套防辐射服或整套防化服时辐照不生效（**Continued 走 `tick` 注入那条最易出问题**） |
| 部分穿戴 | 每件 -25% 辐照伤害减免 |
| 酸液 | 防辐射服不掉耐久、每件 -25% 减伤 |
| 铁砧修复 | `oreganized:lead_ingot` 可修防辐射服 |
| 长矛 | 投掷有音效、命中施辐照、可拾回；手持/投掷/GUI 三种渲染姿态正确 |
| 核能熔炉 | 烧炼速度 ×2；铀棒 51200 / 钍棒 12800 |
| 警报联动 | 反应堆失控时 AC 警报器响、警示灯亮、CN 警报器输出红石信号 |
| 熔毁孵蛋 | 36 格内撼地斯拉蛋孵化，孵出的撼地斯拉不被这次爆炸杀死 |
| 射线猫 | 猫受辐照 III 满 45 秒转化（**AC 缺失环境下不应变成猪**） |
| 视野遮罩 | 空头不遮挡、防化面罩遮挡；勾选 `Clear Helmet Vision` 后都不遮挡 |
| 机械臂 | 能对渊海祭坛取放物品；放珍珠触发 `love_deep_and_robots` 进度 |
| 世界生成 | 四种毒化洞穴矿石、渊海钍/铜、异寂空谷三矿（银矿需 Oreganized） |
| 核弹 | AC 核弹把 90 格内转成 `createnuclear:irradiated_land` |

### 新增测试点：无 AC 环境

这是本次 P0 修复的重点，**以前必崩**：

| 检查项 | 预期 |
|---|---|
| 启动 | 游戏能正常进入 |
| 日志 | 出现 `'autunite_spear' is registered as an inert placeholder item` |
| 物品 | 长矛物品存在（创造栏可见），但无投掷/命中行为 |
| 物品栏操作 | 漏斗/溜槽/机械臂操作普通空气位不崩溃（P0-2 的验证点） |

---

## 11. 风格约定

- 注释、README、commit 信息用**中文**；标识符用英文。
- 类级 Javadoc 写「为什么」而不是「做什么」；关键坑（如 §4、§5）必须在代码注释里留痕。
- 配置注释**中英双语**（中文在上、英文在下）。
- 一个主题一个 commit；重构与行为修复分开；**不要 push**。
- 版本号 semver：修复 `x.y.z+1`，功能 `x.y+1.0`。
