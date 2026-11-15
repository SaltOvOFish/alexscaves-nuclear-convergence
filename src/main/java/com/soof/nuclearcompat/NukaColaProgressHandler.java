package com.soof.nuclearcompat;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 核子可乐饮用计数：用于「你太依赖斯安威斯坦了」这条隐藏挑战进度。
 *
 * <p>计数存在玩家附件里，随存档持久化；每次服务端饮用完成后 +1 并触发
 * {@link NukaColaDrunkTrigger}，进度 JSON 要求 {@code count >= 64}。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID)
public final class NukaColaProgressHandler {

    /** 玩家累计喝下的核子可乐瓶数（随玩家存档持久化，死亡后保留）。 */
    public static final AttachmentType<Integer> NUKA_COLA_DRUNK =
            AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build();

    private static final NukaColaDrunkTrigger TRIGGER = new NukaColaDrunkTrigger();

    private NukaColaProgressHandler() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (event.getRegistry() == NeoForgeRegistries.ATTACHMENT_TYPES) {
            event.register(NeoForgeRegistries.ATTACHMENT_TYPES.key(),
                    ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "nuka_cola_drunk"),
                    () -> NUKA_COLA_DRUNK);
        } else if (event.getRegistry() == BuiltInRegistries.TRIGGER_TYPES) {
            event.register(BuiltInRegistries.TRIGGER_TYPES.key(),
                    ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "drank_nuka_cola"),
                    () -> TRIGGER);
        }
    }

    /** 服务端饮用完成后调用：累计瓶数并触发进度判定。 */
    public static void onDrunk(ServerPlayer player) {
        int count = player.getData(NUKA_COLA_DRUNK) + 1;
        player.setData(NUKA_COLA_DRUNK, count);
        TRIGGER.trigger(player, count);
    }
}
