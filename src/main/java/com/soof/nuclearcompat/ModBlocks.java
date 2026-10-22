package com.soof.nuclearcompat;

import net.minecraft.world.item.BlockItem;
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
    /** 异寂空谷：秽岩基底的铅矿石。 */
    public static final DeferredBlock<Block> COPROLITH_LEAD_ORE = BLOCKS.registerSimpleBlock("coprolith_lead_ore", ORE_PROPERTIES);
    /** 异寂空谷：粪岩基底的硝酸盐矿石。 */
    public static final DeferredBlock<Block> GUANOSTONE_NITRATE_ORE = BLOCKS.registerSimpleBlock("guanostone_nitrate_ore", ORE_PROPERTIES);

    public static final DeferredItem<BlockItem> RADROCK_IRON_ORE_ITEM = ITEMS.registerSimpleBlockItem(RADROCK_IRON_ORE);
    public static final DeferredItem<BlockItem> RADROCK_COAL_ORE_ITEM = ITEMS.registerSimpleBlockItem(RADROCK_COAL_ORE);
    public static final DeferredItem<BlockItem> RADROCK_LEAD_ORE_ITEM = ITEMS.registerSimpleBlockItem(RADROCK_LEAD_ORE);
    public static final DeferredItem<BlockItem> RADROCK_NITRATE_ORE_ITEM = ITEMS.registerSimpleBlockItem(RADROCK_NITRATE_ORE);
    public static final DeferredItem<BlockItem> ABYSSMARINE_THORIUM_ORE_ITEM = ITEMS.registerSimpleBlockItem(ABYSSMARINE_THORIUM_ORE);
    public static final DeferredItem<BlockItem> TUFF_THORIUM_ORE_ITEM = ITEMS.registerSimpleBlockItem(TUFF_THORIUM_ORE);
    public static final DeferredItem<BlockItem> COPROLITH_LEAD_ORE_ITEM = ITEMS.registerSimpleBlockItem(COPROLITH_LEAD_ORE);
    public static final DeferredItem<BlockItem> GUANOSTONE_NITRATE_ORE_ITEM = ITEMS.registerSimpleBlockItem(GUANOSTONE_NITRATE_ORE);

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
