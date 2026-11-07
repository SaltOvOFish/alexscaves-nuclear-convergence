package com.soof.nuclearcompat;

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

    /** 所有可调的矿石键（与 placed_feature 里的 key 一一对应）。 */
    public static final String[] ORE_KEYS = {
            "radrock_iron_ore",
            "radrock_coal_ore",
            "radrock_lead_ore",
            "radrock_nitrate_ore",
            "abyssmarine_thorium_ore",
            "tuff_thorium_ore",
            "tuff_copper_ore",
            "coprolith_lead_ore",
            "guanostone_nitrate_ore",
            "coprolith_silver_ore",
            "guanostone_silver_ore",
            "autunite_striated_toxic_caves"
    };

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

        private Common(ModConfigSpec.Builder builder) {
            builder.comment("世界生成").push("worldgen");

            reactorChance = builder
                    .comment("废弃反应堆的生成概率（0 = 不生成，1 = 正常）")
                    .defineInRange("reactor_chance", 1.0D, 0.0D, 1.0D);

            builder.comment("每种矿石的密度倍率（0 = 不生成，1 = 原本密度，可填小数）").push("ores");
            for (String key : ORE_KEYS) {
                ores.put(key, builder
                        .comment("矿石：" + key + "（0 = 不生成）")
                        .defineInRange(key, 1.0D, 0.0D, 10.0D));
            }
            builder.pop();
            builder.pop();

            builder.comment("井然有矿（Oreganized）联动").push("oreganized");

            fullLeadIntegration = builder
                    .comment("完全使用 Oreganized 的铅相关内容：",
                            "开启时禁用原子核动本身的铅矿生成，并把铅相关配方改用 Oreganized 的铅材料；",
                            "同时本模组的铅矿石掉落 oreganized:raw_lead。关闭则保留原子核动原本的铅体系。")
                    .define("full_lead_integration", true);

            leadStylePack = builder
                    .comment("启用 Oreganized 铅风格资源包：",
                            "辐射铅矿石 / 秽岩铅矿石使用 Oreganized 风格纹理，",
                            "防辐射套装与反应堆强化玻璃更接近 Oreganized 铅材料的暗紫色。",
                            "（资源包本体在资源包界面里，默认不勾选；此项只控制是否注册它）")
                    .define("lead_style_pack", true);

            builder.pop();

            builder.comment("其它").push("general");

            enhancedAntiRadiationArmor = builder
                    .comment("增强防辐射套装：头盔 2 / 胸甲 6 / 护腿 5 / 靴子 2，并每件 +0.1 击退抗性")
                    .define("enhanced_anti_radiation_armor", true);

            builder.pop();
        }
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
