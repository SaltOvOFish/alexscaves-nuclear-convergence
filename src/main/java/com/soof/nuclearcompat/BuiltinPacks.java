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
 * 本模组内置的资源包（都在资源包界面可选，默认不启用）。
 *
 * <ul>
 *     <li>{@code oreganized_style} — 把铅矿石的矿点换成「井然有矿」风格；
 *         另外还覆盖了原子核动的一些贴图（染色防辐射服、反应堆强化玻璃）。</li>
 *     <li>{@code clear_vision} — 隐藏防辐射头盔与 AC 防化头盔的第一人称视野遮罩。</li>
 * </ul>
 *
 * <p>名称用 {@link Component#translatable} 传入，描述写在各自 {@code pack.mcmeta} 里、
 * 同样是可翻译组件（{@code {"translate": "..."}}），两者都靠模组的语言文件翻译
 * （{@code assets/alexscaves_nuclear_convergence/lang/*.json}）。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID, value = Dist.CLIENT)
public final class BuiltinPacks {

    private static final ResourceLocation OREGANIZED_STYLE =
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "resourcepacks/oreganized_style");

    private static final ResourceLocation CLEAR_VISION =
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "resourcepacks/clear_vision");

    private BuiltinPacks() {
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        if (NuclearCompatConfig.COMMON.leadStylePack.get()) {
            event.addPackFinders(
                    OREGANIZED_STYLE,
                    PackType.CLIENT_RESOURCES,
                    Component.translatable("resourcepack.alexscaves_nuclear_convergence.oreganized_style.name"),
                    PackSource.BUILT_IN,
                    false,
                    Pack.Position.TOP);
        }
        event.addPackFinders(
                CLEAR_VISION,
                PackType.CLIENT_RESOURCES,
                Component.translatable("resourcepack.alexscaves_nuclear_convergence.clear_vision.name"),
                PackSource.BUILT_IN,
                false,
                Pack.Position.TOP);
    }
}
