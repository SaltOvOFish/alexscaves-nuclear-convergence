package com.soof.nuclearcompat.client;

import com.soof.nuclearcompat.ModBlocks;
import com.soof.nuclearcompat.ModEntities;
import com.soof.nuclearcompat.AutuniteSpearEntity;
import com.soof.nuclearcompat.NuclearCompat;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端思索（Ponder）注册入口。
 *
 * <p>必须在 {@code FMLLoadCompleteEvent}（Ponder 的 {@code registerAll}）之前把插件加入，
 * 因此放在 {@code FMLClientSetupEvent} 里。整个类只在客户端加载。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientPonderSetup {

    private ClientPonderSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PonderIndex.addPlugin(new NuclearPonderPlugin());

            // 与 AC 的石灰岩长矛一致：蓄力时把模型切到 _throwing 变体。
            // AC 只给自己的长矛注册了这个属性，我们的物品必须单独注册，
            // 否则蓄力时会套用普通手持变换，长矛前后颠倒。
            ItemProperties.register(ModBlocks.AUTUNITE_SPEAR.get(),
                    ResourceLocation.withDefaultNamespace("throwing"),
                    (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);

            registerSpearEntityRenderer();
        });
    }

    /**
     * 为钙铀云母长矛投掷物注册渲染器，按当前安装的是 Up 还是 Neo 版 AC 选择对应模型。
     * 用类名检测而不是直接引用，避免未安装的那一版类被加载。
     */
    private static void registerSpearEntityRenderer() {
        EntityType<AutuniteSpearEntity> type = ModEntities.AUTUNITE_SPEAR.get();
        if (hasClass("com.alexscaves.client.model.LimestoneSpearModel")) {
            EntityRenderers.register(type, AutuniteSpearEntityRenderer::new);
        } else if (hasClass("com.github.alexmodguy.alexscaves.client.model.LimestoneSpearModel")) {
            EntityRenderers.register(type, AutuniteSpearEntityRendererNeo::new);
        }
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, ClientPonderSetup.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
