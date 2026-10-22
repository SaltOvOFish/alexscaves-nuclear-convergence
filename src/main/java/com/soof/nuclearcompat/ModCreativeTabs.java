package com.soof.nuclearcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * 把本模组的矿石按指定顺序插入到前置模组的创造物品栏中。
 *
 * <p>原子核动主 tab：深层硝酸盐矿石 → 凝灰岩钍矿石 → 渊海钍矿石 → 粪岩硝酸盐矿石
 * → 秽岩铅矿石 → 辐射铀矿石 → 辐射铅矿石 → 辐射硝酸盐矿石。</p>
 *
 * <p>AC 渊海陷窟 tab：凝灰岩钍矿石 → 渊海石 → 渊海石瓦 → 渊海钍矿石。</p>
 *
 * <p>AC 异寂空谷 tab：粪岩红石矿石 → 粪岩硝酸盐矿石；秽岩煤矿石 → 秽岩铅矿石。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID)
public final class ModCreativeTabs {

    private static final ResourceKey<CreativeModeTab> AC_TOXIC_CAVES =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("alexscaves", "toxic_caves"));
    private static final ResourceKey<CreativeModeTab> CN_MAIN =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("createnuclear", "main"));
    private static final ResourceKey<CreativeModeTab> AC_ABYSSAL_CHASM =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("alexscaves", "abyssal_chasm"));
    private static final ResourceKey<CreativeModeTab> AC_FORLORN_HOLLOWS =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("alexscaves", "forlorn_hollows"));

    private ModCreativeTabs() {
    }

    @SubscribeEvent
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        CreativeModeTab.TabVisibility vis = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;

        if (event.getTabKey().equals(AC_TOXIC_CAVES)) {
            // 隐藏 AC 自己的铀晶碎片与铀晶
            event.remove(itemStack("alexscaves", "uranium_shard"), vis);
            event.remove(itemStack("alexscaves", "uranium"), vis);

            // 铀粉 → 粗铀 → 硫粉
            ItemStack sulfur = itemStack("alexscaves", "sulfur_dust");
            ItemStack rawUranium = itemStack("createnuclear", "raw_uranium");
            ItemStack uraniumPowder = itemStack("createnuclear", "uranium_powder");
            event.insertBefore(sulfur, rawUranium, vis);
            event.insertBefore(rawUranium, uraniumPowder, vis);

            // 辐射铀矿石 → 铁 → 煤 → 铅 → 硝酸盐
            ItemStack anchor = itemStack("alexscaves", "radrock_uranium_ore");
            ItemStack iron = new ItemStack(ModBlocks.RADROCK_IRON_ORE.get());
            ItemStack coal = new ItemStack(ModBlocks.RADROCK_COAL_ORE.get());
            ItemStack lead = new ItemStack(ModBlocks.RADROCK_LEAD_ORE.get());
            ItemStack nitrate = new ItemStack(ModBlocks.RADROCK_NITRATE_ORE.get());
            event.insertAfter(anchor, iron, vis);
            event.insertAfter(iron, coal, vis);
            event.insertAfter(coal, lead, vis);
            event.insertAfter(lead, nitrate, vis);
        }

        if (event.getTabKey().equals(CN_MAIN)) {
            // 深层硝酸盐矿石 → 凝灰岩钍 → 渊海钍 → 粪岩硝酸盐 → 秽岩铅
            // → 辐射铀 → 辐射铅 → 辐射硝酸盐
            ItemStack anchor = itemStack("createnuclear", "deepslate_nitrate_ore");
            ItemStack tuffThorium = new ItemStack(ModBlocks.TUFF_THORIUM_ORE.get());
            ItemStack abyssThorium = new ItemStack(ModBlocks.ABYSSMARINE_THORIUM_ORE.get());
            ItemStack guanoNitrate = new ItemStack(ModBlocks.GUANOSTONE_NITRATE_ORE.get());
            ItemStack coproLead = new ItemStack(ModBlocks.COPROLITH_LEAD_ORE.get());
            ItemStack uranium = itemStack("alexscaves", "radrock_uranium_ore");
            ItemStack lead = new ItemStack(ModBlocks.RADROCK_LEAD_ORE.get());
            ItemStack nitrate = new ItemStack(ModBlocks.RADROCK_NITRATE_ORE.get());

            event.insertAfter(anchor, tuffThorium, vis);
            event.insertAfter(tuffThorium, abyssThorium, vis);
            event.insertAfter(abyssThorium, guanoNitrate, vis);
            event.insertAfter(guanoNitrate, coproLead, vis);
            event.insertAfter(coproLead, uranium, vis);
            event.insertAfter(uranium, lead, vis);
            event.insertAfter(lead, nitrate, vis);
        }

        if (event.getTabKey().equals(AC_ABYSSAL_CHASM)) {
            // 凝灰岩钍矿石 → 渊海石 → 渊海石瓦 → 渊海钍矿石
            ItemStack abyssmarine = itemStack("alexscaves", "abyssmarine");
            ItemStack tiles = itemStack("alexscaves", "abyssmarine_tiles");
            ItemStack tuffThorium = new ItemStack(ModBlocks.TUFF_THORIUM_ORE.get());
            ItemStack abyssThorium = new ItemStack(ModBlocks.ABYSSMARINE_THORIUM_ORE.get());

            event.insertBefore(abyssmarine, tuffThorium, vis);
            event.insertAfter(tiles, abyssThorium, vis);
        }

        if (event.getTabKey().equals(AC_FORLORN_HOLLOWS)) {
            // 粪岩红石矿石 → 粪岩硝酸盐矿石；秽岩煤矿石 → 秽岩铅矿石
            ItemStack guanostoneRedstone = itemStack("alexscaves", "guanostone_redstone_ore");
            ItemStack coprolithCoal = itemStack("alexscaves", "coprolith_coal_ore");
            ItemStack guanoNitrate = new ItemStack(ModBlocks.GUANOSTONE_NITRATE_ORE.get());
            ItemStack coproLead = new ItemStack(ModBlocks.COPROLITH_LEAD_ORE.get());

            event.insertAfter(guanostoneRedstone, guanoNitrate, vis);
            event.insertAfter(coprolithCoal, coproLead, vis);
        }
    }

    private static ItemStack itemStack(String namespace, String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        return new ItemStack(item);
    }
}
