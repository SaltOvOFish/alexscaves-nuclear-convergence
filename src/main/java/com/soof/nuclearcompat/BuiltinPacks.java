package com.soof.nuclearcompat;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * 内置资源包：把秽岩铅矿石的贴图切换为「井然有矿」风格。
 *
 * <p>资源包内容放在 {@code resourcepacks/oreganized_style/}，默认位于资源包列表顶部并启用；
 * 不想用的话可以在游戏内的资源包界面取消勾选。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BuiltinPacks {

    private static final ResourceLocation OREGANIZED_STYLE =
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "resourcepacks/oreganized_style");

    private BuiltinPacks() {
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        event.addPackFinders(
                OREGANIZED_STYLE,
                PackType.CLIENT_RESOURCES,
                Component.literal("Nuclear Convergence: Oreganized Style"),
                PackSource.BUILT_IN,
                false,
                Pack.Position.TOP);
    }
}
