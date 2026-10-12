package com.soof.nuclearcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 原版的猫（Cat）与豹猫（Ocelot）在持续受到「辐照 III 及以上」效果 45 秒后，
 * 转化为 Alex's Caves 的辐射猫（raycat）。
 *
 * <p>「状态刷新也算」：只要辐照效果持续存在且等级达到 III 及以上，累计时间就继续，
 * 不关心效果的倒计时是否被刷新。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID)
public final class RaycatConversionHandler {

    /** 累计受辐照 III+ 的时间（tick），达到阈值即转化。 */
    public static final AttachmentType<Integer> IRRADIATION_TIME =
            AttachmentType.builder(() -> 0).build();

    /** 需要持续 45 秒（900 tick）。 */
    private static final int CONVERSION_TICKS = 45 * 20;
    /** 辐照 III 对应 amplifier = 2。 */
    private static final int REQUIRED_AMPLIFIER = 2;
    private static final ResourceLocation RAYCAT_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "raycat");

    private RaycatConversionHandler() {
    }

    @SubscribeEvent
    public static void registerAttachment(RegisterEvent event) {
        if (event.getRegistry() != NeoForgeRegistries.ATTACHMENT_TYPES) {
            return;
        }
        event.register(NeoForgeRegistries.ATTACHMENT_TYPES.key(),
                ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "irradiation_time"),
                () -> IRRADIATION_TIME);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Cat) && !(entity instanceof Ocelot)) {
            return;
        }
        LivingEntity living = (LivingEntity) entity;

        if (NuclearCompat.AC_IRRADIATED == null) {
            return;
        }

        MobEffectInstance effect = living.getEffect(NuclearCompat.AC_IRRADIATED);
        if (effect == null || effect.getAmplifier() < REQUIRED_AMPLIFIER) {
            // 已经是 0 就不必每 tick 重复写入附件数据。
            if (living.getData(IRRADIATION_TIME) != 0) {
                living.setData(IRRADIATION_TIME, 0);
            }
            return;
        }

        if (living.level().isClientSide()) {
            return;
        }

        int time = living.getData(IRRADIATION_TIME) + 1;
        if (time >= CONVERSION_TICKS) {
            convertToRaycat(living);
        } else {
            living.setData(IRRADIATION_TIME, time);
        }
    }

    private static void convertToRaycat(LivingEntity living) {
        if (!(living instanceof Mob mob)) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(RAYCAT_ID);
        if (type == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        EntityType<? extends Mob> raycatType = (EntityType<? extends Mob>) type;
        mob.convertTo(raycatType, true);
    }
}
