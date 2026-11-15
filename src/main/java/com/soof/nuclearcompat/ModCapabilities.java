package com.soof.nuclearcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/**
 * 兼容修复：Alex's Caves Up 的部分容器方块没有暴露 NeoForge 的 ItemHandler capability，
 * 导致机械动力（Create）的漏斗、溜槽、动力机械臂等无法输入输出。这里统一注册：
 *
 * - 核能熔炉主体：SidedInvWrapper（按面）
 * - 核能熔炉组件（4x4 多方块的外围）：组件方块无 BlockEntity，但实现了 WorldlyContainerHolder，
 *   通过 registerBlock 转发到主体 BlockEntity，让漏斗对着组件方块也能工作
 * - 金属桶 / 锈蚀金属桶：InvWrapper（共用 MetalBarrelBlockEntity）
 * - 姜饼桶：InvWrapper（GingerbarrelBlockEntity）
 * - 渊海祭坛：InvWrapper（类似置物台，机械臂可放入/取出，槽位上限 1）
 */
@EventBusSubscriber(modid = NuclearCompat.MODID)
public final class ModCapabilities {

    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerSided(event, "nuclear_furnace");
        registerSimple(event, "metal_barrel");
        registerSimple(event, "gingerbarrel");
        registerAbyssalAltar(event);
        registerNuclearFurnaceComponent(event);
    }

    /** 用 SidedInvWrapper 包装 WorldlyContainer，保留按面的输入输出逻辑。 */
    private static void registerSided(RegisterCapabilitiesEvent event, String path) {
        BlockEntityType<?> type = blockEntityType(path);
        if (type == null) {
            return;
        }
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (be, side) -> new SidedInvWrapper((WorldlyContainer) be, side)
        );
    }

    /** 用 InvWrapper 包装 Container，绕过面限制，让机械臂/漏斗/溜槽能自由输入输出。 */
    private static void registerSimple(RegisterCapabilitiesEvent event, String path) {
        BlockEntityType<?> type = blockEntityType(path);
        if (type == null) {
            return;
        }
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (be, side) -> new InvWrapper((Container) be)
        );
    }

    /** 渊海祭坛：槽位上限固定为 1，避免机械臂一次性堆入多颗珍珠。 */
    private static void registerAbyssalAltar(RegisterCapabilitiesEvent event) {
        BlockEntityType<?> type = blockEntityType("abyssal_altar");
        if (type == null) {
            return;
        }
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (be, side) -> new SingleItemInvWrapper((Container) be)
        );
    }

    /** 槽位上限固定为 1 的容器包装：机械臂/漏斗每次最多只能放入 1 个物品。 */
    private static class SingleItemInvWrapper extends InvWrapper {
        SingleItemInvWrapper(Container inv) {
            super(inv);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return stack;
            }
            if (stack.getCount() <= 1) {
                return super.insertItem(slot, stack, simulate);
            }
            ItemStack single = stack.copyWithCount(1);
            ItemStack rejected = super.insertItem(slot, single, simulate);
            return rejected.isEmpty() ? stack.copyWithCount(stack.getCount() - 1) : stack;
        }
    }

    /** 核能熔炉组件方块：无 BlockEntity，通过 WorldlyContainerHolder 转发到主体。 */
    private static void registerNuclearFurnaceComponent(RegisterCapabilitiesEvent event) {
        // BuiltInRegistries.BLOCK 是 DefaultedRegistry：缺失时返回 minecraft:air 而不是 null，
        // 所以必须回查注册名确认。否则 Alex's Caves 缺失时会把这个能力注册到空气上，
        // provider 里的 (WorldlyContainerHolder) 强转会在任何模组查询空气位时抛 ClassCastException。
        Block component = blockOrNull(
                ResourceLocation.fromNamespaceAndPath("alexscaves", "nuclear_furnace_component"));
        if (component == null) {
            return;
        }
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> {
                    WorldlyContainer container = ((WorldlyContainerHolder) state.getBlock())
                            .getContainer(state, level, pos);
                    return container == null ? null : new SidedInvWrapper(container, side);
                },
                component
        );
    }

    private static BlockEntityType<?> blockEntityType(String path) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath("alexscaves", path));
    }

    /**
     * 按注册名取方块，缺失时返回 null。
     *
     * <p>{@link BuiltInRegistries#BLOCK} 是 {@code DefaultedRegistry}，{@code get} 对不存在的
     * 注册名返回 {@code minecraft:air} 而非 null，直接判空会失效（会把能力注册到空气上）。</p>
     */
    private static Block blockOrNull(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        return id.equals(BuiltInRegistries.BLOCK.getKey(block)) ? block : null;
    }
}
