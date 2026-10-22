package com.soof.nuclearcompat.client;

import com.soof.nuclearcompat.NuclearCompat;
import net.createmod.ponder.foundation.PonderIndex;
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
        event.enqueueWork(() -> PonderIndex.addPlugin(new NuclearPonderPlugin()));
    }
}
