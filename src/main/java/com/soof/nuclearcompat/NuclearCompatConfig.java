package com.soof.nuclearcompat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置（{@code config/alexscaves_nuclear_convergence-common.toml}）。
 *
 * <p>所有世界生成相关项都是 {@code 0~10} 的倍率：0 = 不生成，1 = 原本密度，
 * 0.5 = 一半密度（矿石支持小数，结构按概率生效）。</p>
 */
public final class NuclearCompatConfig {

    /** 一条矿石配置：键 + 中/英文说明（顺序即文件中的排列顺序，注释里中英各占一行）。 */
    private record OreEntry(String key, String commentZh, String commentEn) {
    }

    /**
     * 所有可调的矿石（与 placed_feature 里的 key 一一对应）。
     *
     * <p>按群系分组排列：毒化洞穴 → 渊海陷窟 → 异寂空谷，方便玩家对照游戏内位置调整。</p>
     */
    private static final OreEntry[] ORES = {
            new OreEntry("autunite_striated_toxic_caves",
                    "钙铀云母（毒化洞穴）：autunite_striated_toxic_caves（0 = 不生成）",
                    "Autunite (Toxic Caves): autunite_striated_toxic_caves (0 = none)"),
            new OreEntry("radrock_iron_ore",
                    "辐射铁矿石（毒化洞穴）：radrock_iron_ore（0 = 不生成）",
                    "Radiated Iron Ore (Toxic Caves): radrock_iron_ore (0 = none)"),
            new OreEntry("radrock_coal_ore",
                    "辐射煤矿石（毒化洞穴）：radrock_coal_ore（0 = 不生成）",
                    "Radiated Coal Ore (Toxic Caves): radrock_coal_ore (0 = none)"),
            new OreEntry("radrock_lead_ore",
                    "辐射铅矿石（毒化洞穴）：radrock_lead_ore（0 = 不生成）",
                    "Radiated Lead Ore (Toxic Caves): radrock_lead_ore (0 = none)"),
            new OreEntry("radrock_nitrate_ore",
                    "辐射硝酸盐矿石（毒化洞穴）：radrock_nitrate_ore（0 = 不生成）",
                    "Radiated Nitrate Ore (Toxic Caves): radrock_nitrate_ore (0 = none)"),
            new OreEntry("abyssmarine_thorium_ore",
                    "渊海钍矿石（渊海陷窟）：abyssmarine_thorium_ore（0 = 不生成）",
                    "Abyssal Thorium Ore (Abyssal Chasm): abyssmarine_thorium_ore (0 = none)"),
            new OreEntry("tuff_thorium_ore",
                    "凝灰岩钍矿石（渊海陷窟）：tuff_thorium_ore（0 = 不生成）",
                    "Tuff Thorium Ore (Abyssal Chasm): tuff_thorium_ore (0 = none)"),
            new OreEntry("tuff_copper_ore",
                    "凝灰岩铜矿石（渊海陷窟）：tuff_copper_ore（0 = 不生成）",
                    "Tuff Copper Ore (Abyssal Chasm): tuff_copper_ore (0 = none)"),
            new OreEntry("coprolith_lead_ore",
                    "秽岩铅矿石（异寂空谷）：coprolith_lead_ore（0 = 不生成）",
                    "Coprolith Lead Ore (Forlorn Hollows): coprolith_lead_ore (0 = none)"),
            new OreEntry("guanostone_nitrate_ore",
                    "粪岩硝酸盐矿石（异寂空谷）：guanostone_nitrate_ore（0 = 不生成）",
                    "Guanostone Nitrate Ore (Forlorn Hollows): guanostone_nitrate_ore (0 = none)"),
            new OreEntry("coprolith_silver_ore",
                    "秽岩银矿石（异寂空谷）：coprolith_silver_ore（0 = 不生成）",
                    "Coprolith Silver Ore (Forlorn Hollows): coprolith_silver_ore (0 = none)"),
            new OreEntry("guanostone_silver_ore",
                    "粪岩银矿石（异寂空谷）：guanostone_silver_ore（0 = 不生成）",
                    "Guanostone Silver Ore (Forlorn Hollows): guanostone_silver_ore (0 = none)")
    };

    /** 所有可调的矿石键（与 placed_feature 里的 key 一一对应）。 */
    public static final String[] ORE_KEYS = Arrays.stream(ORES).map(OreEntry::key).toArray(String[]::new);

    /**
     * 辐射体系：两套模组的辐射模型不同，只能二选一。
     *
     * <ul>
     *     <li>{@link #AC}：统一为 Alex's Caves 的辐照效果（默认）。原子核动的辐射被拦下，
     *         换成 {@code alexscaves:irradiated}；</li>
     *     <li>{@link #CN}：统一为原子核动的辐射效果。AC 的辐照（含长矛、核子可乐、AC 自己的
     *         放射性物品 / 方块 / 生物）会被折算成原子核动的辐射剂量，HUD 显示辐射条。</li>
     * </ul>
     */
    public enum RadiationSystem {
        AC,
        CN
    }

    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COMMON = new Common(builder);
        SPEC = builder.build();
    }

    private NuclearCompatConfig() {
    }

    public static final class Common {

        /** 废弃反应堆生成概率：0 = 不生成。 */
        public final ModConfigSpec.DoubleValue reactorChance;

        /** 每种矿石的密度倍率：0 = 不生成。 */
        public final Map<String, ModConfigSpec.DoubleValue> ores = new LinkedHashMap<>();

        // ---- 井然有矿 ----
        public final ModConfigSpec.BooleanValue fullLeadIntegration;
        public final ModConfigSpec.BooleanValue leadStylePack;

        // ---- 其它 ----
        public final ModConfigSpec.BooleanValue enhancedAntiRadiationArmor;

        /** 辐射体系：{@code AC}（默认）或 {@code CN}。 */
        public final ModConfigSpec.EnumValue<RadiationSystem> radiationSystem;

        private Common(ModConfigSpec.Builder builder) {
            builder.comment("世界生成", "World Generation").push("worldgen");

            reactorChance = builder
                    .comment("废弃反应堆在毒化洞穴中的生成概率（0 = 不生成，1 = 正常）",
                            "Chance for the Abandoned Reactor to generate in the Toxic Caves (0 = never, 1 = normal)")
                    .defineInRange("reactor_chance", 1.0D, 0.0D, 1.0D);

            builder.comment(
                            "每种矿石的密度倍率（0 = 不生成，1 = 原本密度，可填小数）",
                            "Density multiplier for each ore (0 = none, 1 = default density, decimals allowed)")
                    .push("ores");
            for (OreEntry entry : ORES) {
                ores.put(entry.key(), builder
                        .comment(entry.commentZh(), entry.commentEn())
                        .defineInRange(entry.key(), 1.0D, 0.0D, 10.0D));
            }
            builder.pop();
            builder.pop();

            builder.comment("井然有矿（Oreganized）联动", "Oreganized Integration").push("oreganized");

            fullLeadIntegration = builder
                    .comment("完全使用 Oreganized 的铅相关内容：",
                            "开启时禁用原子核动本身的铅矿生成，并把铅相关配方改用 Oreganized 的铅材料；",
                            "同时本模组的铅矿石掉落 oreganized:raw_lead。关闭则保留原子核动原本的铅体系。",
                            "Fully use Oreganized's lead content:",
                            "when enabled, disables Create Nuclear's own lead ore generation and switches all lead recipes to Oreganized materials;",
                            "our lead ores then drop oreganized:raw_lead. When disabled, Create Nuclear's original lead system is kept.")
                    .define("full_lead_integration", true);

            leadStylePack = builder
                    .comment("铅制物品使用 Oreganized 铅风格纹理：",
                            "辐射铅矿石 / 秽岩铅矿石使用 Oreganized 风格纹理，",
                            "防辐射套装与反应堆强化玻璃更接近 Oreganized 铅材料的暗紫色。",
                            "（资源包本体在资源包界面里，默认不勾选；此项只控制是否注册它）",
                            "Use Oreganized lead-style textures for lead items:",
                            "radrock and coprolith lead ores use Oreganized-style sprites;",
                            "the anti-radiation suit and reinforced reactor glass move closer to Oreganized lead's dark purple;",
                            "(the pack itself is listed in the resource pack screen and is not selected by default; this option only controls whether it is registered)")
                    .define("lead_style_pack", true);

            builder.pop();

            builder.comment("其它", "Miscellaneous").push("general");

            enhancedAntiRadiationArmor = builder
                    .comment("增强防辐射套装：头盔 2 / 胸甲 6 / 护腿 5 / 靴子 2，并每件 +0.1 击退抗性",
                            "Enhanced anti-radiation suit: helmet 2 / chestplate 6 / leggings 5 / boots 2, plus +0.1 knockback resistance per piece")
                    .define("enhanced_anti_radiation_armor", true);

            radiationSystem = builder
                    .comment("辐射体系统一（两套只能选一）：",
                            "AC = 统一为 Alex's Caves 的辐照效果（默认）；",
                            "CN = 统一为原子核动的辐射效果（带 HUD 辐射条，辐照来源会折算成辐射剂量）。",
                            "改完需要重启游戏 / 重进世界。",
                            "Unified radiation system (pick exactly one):",
                            "AC = use Alex's Caves' Irradiated effect (default);",
                            "CN = use Create Nuclear's Radiation effect (with the HUD radiation bar; Irradiated sources are converted to radiation dose);",
                            "Requires a game restart / re-entering the world.")
                    .defineEnum("radiation_system", RadiationSystem.AC);

            builder.pop();
        }
    }

    /** 是否使用原子核动的辐射效果（{@code radiation_system = CN}）。 */
    public static boolean useCreateNuclearRadiation() {
        return COMMON.radiationSystem.get() == RadiationSystem.CN;
    }

    /** 某条矿石配置的倍率。 */
    public static double oreChance(String key) {
        ModConfigSpec.DoubleValue value = COMMON.ores.get(key);
        return value == null ? 1.0D : value.get();
    }

    /**
     * 供数据包条件使用：把配置键映射成布尔值。
     *
     * <p>数字型配置取 {@code > 0} 作为真，布尔型直接返回其值。</p>
     */
    public static boolean isEnabled(String key) {
        if (key == null) {
            return true;
        }
        return switch (key) {
            case "worldgen.reactor_chance" -> COMMON.reactorChance.get() > 0.0D;
            case "oreganized.full_lead_integration" -> COMMON.fullLeadIntegration.get();
            case "oreganized.lead_style_pack" -> COMMON.leadStylePack.get();
            case "general.enhanced_anti_radiation_armor" -> COMMON.enhancedAntiRadiationArmor.get();
            default -> {
                // worldgen.ores.<key>
                String prefix = "worldgen.ores.";
                if (key.startsWith(prefix)) {
                    yield oreChance(key.substring(prefix.length())) > 0.0D;
                }
                yield true;
            }
        };
    }
}
