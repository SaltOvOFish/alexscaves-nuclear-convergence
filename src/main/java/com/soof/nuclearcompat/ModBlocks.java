package com.soof.nuclearcompat;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 四种「辐射岩」矿石方块，只在毒化洞穴生成：
 * - radrock_iron_ore    辐射铁矿石   → 掉落 minecraft:raw_iron（粗铁）
 * - radrock_coal_ore    辐射煤矿石   → 掉落 minecraft:coal（煤炭）
 * - radrock_lead_ore    辐射铅矿石   → 掉落 createnuclear:raw_lead（粗铅）
 * - radrock_nitrate_ore 辐射硝酸盐矿石 → 掉落 createnuclear:nitrate（硝酸盐）
 * - abyssmarine_thorium_ore 渊海钍矿石（渊海石基底）→ 掉落 createnuclear:raw_thorium（粗钍）
 * - tuff_thorium_ore 凝灰岩钍矿石（凝灰岩基底）→ 掉落 createnuclear:raw_thorium（粗钍）
 * - coprolith_lead_ore 秽岩铅矿石（秽岩基底）→ 掉落 createnuclear:raw_lead（粗铅）
 * - guanostone_nitrate_ore 粪岩硝酸盐矿石（粪岩基底）→ 掉落 createnuclear:nitrate（硝酸盐）
 *
 * 方块本身是普通矿石方块（无特殊行为），挖掘等级 / 掉落 / 生成均由数据包资源控制。
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NuclearCompat.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NuclearCompat.MODID);

    private static final BlockBehaviour.Properties ORE_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(3.0f, 3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE);

    public static final DeferredBlock<Block> RADROCK_IRON_ORE = BLOCKS.registerSimpleBlock("radrock_iron_ore", ORE_PROPERTIES);
    public static final DeferredBlock<Block> RADROCK_COAL_ORE = BLOCKS.registerSimpleBlock("radrock_coal_ore", ORE_PROPERTIES);
    public static final DeferredBlock<Block> RADROCK_LEAD_ORE = BLOCKS.registerSimpleBlock("radrock_lead_ore", ORE_PROPERTIES);
    public static final DeferredBlock<Block> RADROCK_NITRATE_ORE = BLOCKS.registerSimpleBlock("radrock_nitrate_ore", ORE_PROPERTIES);

    /** 渊海陷窟：渊海石基底的钍矿石。 */
    public static final DeferredBlock<Block> ABYSSMARINE_THORIUM_ORE = BLOCKS.registerSimpleBlock("abyssmarine_thorium_ore", ORE_PROPERTIES);
    /** 主世界深层：凝灰岩基底的钍矿石。 */
    public static final DeferredBlock<Block> TUFF_THORIUM_ORE = BLOCKS.registerSimpleBlock("tuff_thorium_ore", ORE_PROPERTIES);
    /** 渊海陷窟：凝灰岩基底的铜矿石。 */
    public static final DeferredBlock<Block> TUFF_COPPER_ORE = BLOCKS.registerSimpleBlock("tuff_copper_ore", ORE_PROPERTIES);
    /** 异寂空谷：秽岩基底的铅矿石。 */
    public static final DeferredBlock<Block> COPROLITH_LEAD_ORE = BLOCKS.registerSimpleBlock("coprolith_lead_ore", ORE_PROPERTIES);
    /** 异寂空谷：粪岩基底的银矿石（需安装井然有矿）。 */
    public static final DeferredBlock<Block> GUANOSTONE_SILVER_ORE = BLOCKS.registerSimpleBlock("guanostone_silver_ore", ORE_PROPERTIES);
    /** 异寂空谷：秽岩基底的银矿石（需安装井然有矿）。 */
    public static final DeferredBlock<Block> COPROLITH_SILVER_ORE = BLOCKS.registerSimpleBlock("coprolith_silver_ore", ORE_PROPERTIES);
    /** 异寂空谷：粪岩基底的硝酸盐矿石。 */
    public static final DeferredBlock<Block> GUANOSTONE_NITRATE_ORE = BLOCKS.registerSimpleBlock("guanostone_nitrate_ore", ORE_PROPERTIES);

    public static final DeferredItem<BlockItem> RADROCK_IRON_ORE_ITEM = ITEMS.registerSimpleBlockItem(RADROCK_IRON_ORE);
    public static final DeferredItem<BlockItem> RADROCK_COAL_ORE_ITEM = ITEMS.registerSimpleBlockItem(RADROCK_COAL_ORE);
    public static final DeferredItem<BlockItem> RADROCK_LEAD_ORE_ITEM = ITEMS.registerSimpleBlockItem(RADROCK_LEAD_ORE);
    public static final DeferredItem<BlockItem> RADROCK_NITRATE_ORE_ITEM = ITEMS.registerSimpleBlockItem(RADROCK_NITRATE_ORE);

    /** 钙铀云母长矛：石灰岩长矛的复制品，命中施加 20 秒辐照 I（Up / Neo 两版自动选）。 */
    public static final DeferredItem<Item> AUTUNITE_SPEAR =
            ITEMS.register("autunite_spear", () -> createAutuniteSpear(new Item.Properties().stacksTo(16)));

    /** 核子可乐：饮用后获得 10 秒甜蜜冲刺 + 10 秒辐照 I。 */
    public static final DeferredItem<Item> NUKA_COLA = ITEMS.register("nuka_cola", () -> new NukaColaItem(
            new Item.Properties()
                    .stacksTo(16)
                    .food(new FoodProperties.Builder()
                            .nutrition(2)
                            .saturationModifier(0.1F)
                            .build())));

    /**
     * 反射构造长矛物品。
     *
     * <p>Up / Neo 两版物品类分别继承不同包名的 AC {@code SpearItem}。如果在字节码里直接
     * {@code new} 它们，JVM 在<b>校验 ModBlocks 类时</b>就会去解析两个类及其父类；
     * 只装了一版 AC 时另一版的父类不存在，直接抛 {@code NoClassDefFoundError}，模组构造失败。
     * 用类名反射加载可以做到「用哪个版本加载哪个」，缺席的版本完全不碰。</p>
     */
    private static Item createAutuniteSpear(Item.Properties properties) {
        try {
            return (Item) Class.forName("com.soof.nuclearcompat.AutuniteSpearItem")
                    .getConstructor(Item.Properties.class)
                    .newInstance(properties);
        } catch (Throwable upUnavailable) {
            try {
                return (Item) Class.forName("com.soof.nuclearcompat.AutuniteSpearItemNeo")
                        .getConstructor(Item.Properties.class)
                        .newInstance(properties);
            } catch (Throwable neoUnavailable) {
                upUnavailable.addSuppressed(neoUnavailable);
                // Alex's Caves 是可选依赖：两版都不可用时不能抛异常。
                // 这个 supplier 在 RegisterEvent 分发物品注册时执行，抛异常会直接让模组加载失败、
                // 游戏起不来——那与 mods.toml 里 alexscaves=optional 的声明矛盾。
                // 因此降级注册为普通物品占位：注册名保持不变，数据包引用不会失效，
                // 只是失去投掷/命中施毒等长矛行为。
                NuclearCompat.LOGGER.warn(
                        "[Nuclear Convergence] Neither Alex's Caves Up nor Neo is available; "
                                + "'{}' is registered as an inert placeholder item.",
                        "autunite_spear");
                return new Item(properties);
            }
        }
    }
    /** 隐藏的图标物品：供 AutuniteSpearRender 在 GUI / 地面等场景渲染 16×16 图标（不进创造栏）。 */
    public static final DeferredItem<Item> AUTUNITE_SPEAR_SPRITE = ITEMS.register("autunite_spear_sprite", () -> new Item(new Item.Properties()));

    /**
     * 隐藏的进度图标物品：直接使用 Alex's Caves 的「甜蜜冲刺」效果贴图。
     *
     * <p>各 AC 移植版显示效果图标的方式不同（Up/Neo/Unofficial 用 {@code citadel:effect_item}，
     * Continued/RENEWED 用 {@code alexscaves:effect_item} 且组件键还不一样），无法用同一个物品 id
     * 覆盖五种版本；这里自带一个只改了贴图的物品，所有版本都能渲染成同一张效果图标。</p>
     */
    public static final DeferredItem<Item> EFFECT_ICON_SUGAR_RUSH =
            ITEMS.register("effect_icon_sugar_rush", () -> new Item(new Item.Properties()));

    public static final DeferredItem<BlockItem> ABYSSMARINE_THORIUM_ORE_ITEM = ITEMS.registerSimpleBlockItem(ABYSSMARINE_THORIUM_ORE);
    public static final DeferredItem<BlockItem> TUFF_THORIUM_ORE_ITEM = ITEMS.registerSimpleBlockItem(TUFF_THORIUM_ORE);
    public static final DeferredItem<BlockItem> TUFF_COPPER_ORE_ITEM = ITEMS.registerSimpleBlockItem(TUFF_COPPER_ORE);
    public static final DeferredItem<BlockItem> COPROLITH_LEAD_ORE_ITEM = ITEMS.registerSimpleBlockItem(COPROLITH_LEAD_ORE);
    public static final DeferredItem<BlockItem> GUANOSTONE_SILVER_ORE_ITEM = ITEMS.registerSimpleBlockItem(GUANOSTONE_SILVER_ORE);
    public static final DeferredItem<BlockItem> COPROLITH_SILVER_ORE_ITEM = ITEMS.registerSimpleBlockItem(COPROLITH_SILVER_ORE);
    public static final DeferredItem<BlockItem> GUANOSTONE_NITRATE_ORE_ITEM = ITEMS.registerSimpleBlockItem(GUANOSTONE_NITRATE_ORE);

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
