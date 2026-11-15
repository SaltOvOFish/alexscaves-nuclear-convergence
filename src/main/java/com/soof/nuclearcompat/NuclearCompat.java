package com.soof.nuclearcompat;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.nuclearteam.createnuclear.api.radiation.RadiationRegistry;
import org.slf4j.Logger;

/**
 * Alex's Caves: Nuclear Convergence
 *
 * 一个轻量级桥接模组：把「原子核动（Create Nuclear）」与「Alex's Caves」
 * 两套辐射机制（createnuclear:radiation / alexscaves:irradiated）按配置统一成其中一套，
 * 让两个模组共用同一个辐射体系。
 *
 * 实现方式：运行时通过注册表动态解析两个效果（效果缺失时对应桥接自动停用），
 * 在 NeoForge 的 {@code MobEffectEvent.Applicable} 事件里拦截
 * {@code createnuclear:radiation} 的施加，改为施加 {@code alexscaves:irradiated}。
 */
@Mod(NuclearCompat.MODID)
public class NuclearCompat {

    /** 本模组的 Mod ID，必须与 META-INF/neoforge.mods.toml 及 gradle.properties 一致。 */
    public static final String MODID = "alexscaves_nuclear_convergence";

    public static final Logger LOGGER = LogUtils.getLogger();

    /** 原子核动（Create Nuclear）的辐射效果注册名。 */
    public static final ResourceLocation CN_RADIATION_ID = ResourceLocation.fromNamespaceAndPath("createnuclear", "radiation");

    /** Alex's Caves 的辐照效果注册名（五个移植版通用）。 */
    public static final ResourceLocation AC_IRRADIATED_ID = ResourceLocation.fromNamespaceAndPath("alexscaves", "irradiated");

    /** 在 commonSetup 阶段解析得到的两个效果 Holder（未找到时为 null，桥接自动停用）。 */
    public static Holder<MobEffect> CN_RADIATION = null;
    public static Holder<MobEffect> AC_IRRADIATED = null;

    /** 废弃反应堆 feature 类型（不依赖 AC 的 feature 注册名，兼容 Up 与 Neo）。 */
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);
    public static final DeferredHolder<Feature<?>, AbandonedReactorFeature> ABANDONED_REACTOR_FEATURE =
            FEATURES.register("abandoned_reactor", () -> new AbandonedReactorFeature(AbandonedReactorFeature.Config.CODEC));

    public NuclearCompat(IEventBus modEventBus, ModContainer modContainer) {
        // 配置文件：config/alexscaves_nuclear_convergence-common.toml
        modContainer.registerConfig(ModConfig.Type.COMMON, NuclearCompatConfig.SPEC);
        // 数据包条件（让配方/群系修改器跟随配置开关）。
        ModConditions.register(modEventBus);
        // 自定义矿石密度修饰符。
        ModWorldgen.register(modEventBus);

        // 注册本模组新增的方块（四种辐射岩矿石）及对应的方块物品。
        ModBlocks.register(modEventBus);
        // 注册钙铀云母长矛的投掷物实体类型。
        ModEntities.register(modEventBus);
        // 注册废弃反应堆的 Structure 类型与 StructurePiece 类型（供 /locate structure 查询）。
        ModStructures.register(modEventBus);
        // 注册废弃反应堆 feature 类型。
        FEATURES.register(modEventBus);
        // 注册「带催化剂的合成」序列化器（核子可乐配方用）。
        ModRecipes.register(modEventBus);

        // FMLCommonSetupEvent 在所有模组的 RegisterEvent 之后触发，
        // 此时两个目标模组的效果都已注册完毕，可以安全查询。
        modEventBus.addListener(this::commonSetup);
        // RegisterEvent 在注册表冻结前触发，用于注册 Create 的机械臂交互点类型。
        modEventBus.addListener(this::registerArmInteractionPoint);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        CN_RADIATION = BuiltInRegistries.MOB_EFFECT.getHolder(CN_RADIATION_ID).orElse(null);
        AC_IRRADIATED = BuiltInRegistries.MOB_EFFECT.getHolder(AC_IRRADIATED_ID).orElse(null);

        if (CN_RADIATION == null) {
            LOGGER.warn("[Nuclear Convergence] Effect '{}' not found. Create Nuclear missing? Radiation replacement disabled.", CN_RADIATION_ID);
        }
        if (AC_IRRADIATED == null) {
            LOGGER.warn("[Nuclear Convergence] Effect '{}' not found. Alex's Caves missing? Radiation replacement disabled.", AC_IRRADIATED_ID);
        }
        if (CN_RADIATION != null && AC_IRRADIATED != null) {
            LOGGER.info("[Nuclear Convergence] Radiation unified: {} -> {}", CN_RADIATION_ID, AC_IRRADIATED_ID);
        }

        logAcFlavour();
        registerAcRadiationSources();
    }

    /**
     * 输出检测到的 Alex's Caves 移植版信息，方便用户反馈兼容性问题时定位。
     *
     * <p>目前已知的 1.21.1 移植版都共用 {@code alexscaves} 这个 mod id，仅靠依赖声明区分不出版本；
     * 包名与关键方法才是真正的差异点：</p>
     * <ul>
     *     <li>{@code com.alexscaves} → Alex's Caves Up；</li>
     *     <li>{@code com.github.alexmodguy.alexscaves} → AC Neo / Unofficial Port / RENEWED；</li>
     *     <li>同一包名但辐照效果覆写 {@code tick(LivingEntity, int)} 的 → Alex's Caves Continued，
     *         走 {@code IrradiatedEffectContinuedMixin}。</li>
     * </ul>
     */
    private static void logAcFlavour() {
        boolean up = hasClass("com.alexscaves.server.potion.IrradiatedEffect");
        boolean neo = hasClass("com.github.alexmodguy.alexscaves.server.potion.IrradiatedEffect");

        String flavour;
        if (up) {
            flavour = "Alex's Caves Up (com.alexscaves)";
        } else if (neo) {
            flavour = hasClassMethod("com.github.alexmodguy.alexscaves.server.potion.IrradiatedEffect",
                    "tick", LivingEntity.class, int.class)
                    ? "Alex's Caves Continued (com.github.alexmodguy.alexscaves, ACMobEffect)"
                    : "Alex's Caves Neo / Unofficial Port / RENEWED (com.github.alexmodguy.alexscaves)";
        } else {
            flavour = "not detected";
        }
        LOGGER.info("[Nuclear Convergence] Alex's Caves flavour: {}", flavour);
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, NuclearCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static boolean hasClassMethod(String className, String method, Class<?>... params) {
        try {
            Class<?> type = Class.forName(className, false, NuclearCompat.class.getClassLoader());
            type.getMethod(method, params);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
            return false;
        }
    }

    /** 辐射铀矿石在 CN 物品栏辐射体系里的剂量（与黄饼同级）。 */
    private static final String RADROCK_URANIUM_ORE = "radrock_uranium_ore";
    private static final double RADROCK_URANIUM_ORE_RADIATION = 4.0D;
    /** 未精炼废料的剂量（单个即「辐照 III」）。 */
    private static final String UNREFINED_WASTE = "unrefined_waste";
    private static final double UNREFINED_WASTE_RADIATION = 50.0D;

    /**
     * 把 AC 的两个放射性物品接入原子核动的「物品栏辐射」体系。
     *
     * <p>原子核动会在每个 tick 遍历玩家背包（主背包 + 副手），累加每个物品的辐射值
     * （{@code RadiationRegistry.getRadiation(stack)} = 单件值 × 堆叠数），再按服务端配置的
     * 阈值 {@code radiation_level_1/2/3}（默认 10 / 25 / 50）换算成辐射等级并施加
     * {@code createnuclear:radiation}——而这个效果随后会被本模组的桥接换为
     * {@code alexscaves:irradiated}。</p>
     *
     * <p>数值参照原子核动自己的物品：黄饼 4.0、粗铀 3.0、铀粉 2.0、铀棒 100.0。
     * 因此：</p>
     * <ul>
     *     <li>辐射铀矿石 = 4.0（与黄饼同级，单个约等于「辐照 I」，不造成伤害）；</li>
     *     <li>未精炼的废料 = 50.0（单个即「辐照 III」，携带即开始周期性受伤）。</li>
     * </ul>
     *
     * <p>注意：堆叠会按数量累加，一叠 16 个辐射铀矿石相当于 64。</p>
     */
    private static void registerAcRadiationSources() {
        registerAcRadiationSource(RADROCK_URANIUM_ORE, RADROCK_URANIUM_ORE_RADIATION);
        registerAcRadiationSource(UNREFINED_WASTE, UNREFINED_WASTE_RADIATION);
    }

    /** 按注册名取 AC 物品，存在则登记辐射值；AC 未安装时静默跳过。 */
    private static void registerAcRadiationSource(String path, double value) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("alexscaves", path);
        Item item = BuiltInRegistries.ITEM.get(id);
        // BuiltInRegistries.ITEM 是 DefaultedRegistry：缺失时会返回 AIR 而不是 null，
        // 所以必须再比对一次注册名，避免把 minecraft:air 当成辐射源。
        if (item == Items.AIR || !id.equals(BuiltInRegistries.ITEM.getKey(item))) {
            LOGGER.warn("[Nuclear Convergence] Item '{}' not found, skipped as radiation source.", id);
            return;
        }
        RadiationRegistry.register().item(item).value(value).build();
        LOGGER.info("[Nuclear Convergence] Registered radiation source: {} = {}", id, value);
    }

    /** 注册渊海祭坛为机械臂交互点，让机械臂能像置物台一样拿取/放置其中的物品。 */
    private void registerArmInteractionPoint(RegisterEvent event) {
        if (event.getRegistry() != CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE) {
            return;
        }
        Registry.register(
                CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE,
                ResourceLocation.fromNamespaceAndPath(MODID, "abyssal_altar"),
                new AbyssalAltarInteractionPointType()
        );
        ArmInteractionPointType.init();
        LOGGER.info("[Nuclear Convergence] Registered abyssal altar as a mechanical arm interaction point.");
    }
}
