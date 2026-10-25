package com.soof.nuclearcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** 跨 Alex's Caves Up / Neo 两版通用的辐照施加逻辑（只依赖原版注册表）。 */
public final class SpearCompat {

    private static final ResourceLocation SUGAR_RUSH_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "sugar_rush");

    private static final ResourceLocation IRRADIATED_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "irradiated");

    /** 20 秒 = 400 tick，等级 I（amplifier 0）。 */
    private static final int IRRADIATED_DURATION = 400;

    private SpearCompat() {
    }

    public static void applyIrradiated(LivingEntity target) {
        applyIrradiated(target, IRRADIATED_DURATION);
    }

    public static void applyIrradiated(LivingEntity target, int duration) {
        BuiltInRegistries.MOB_EFFECT.getHolder(IRRADIATED_ID).ifPresent(effect ->
                target.addEffect(new MobEffectInstance(effect, duration, 0)));
    }

    /** 甜蜜冲刺，时长与等级与紫色苏打水一致（200 tick，amplifier 0）。 */
    public static void applySugarRush(LivingEntity target) {
        BuiltInRegistries.MOB_EFFECT.getHolder(SUGAR_RUSH_ID).ifPresent(effect ->
                target.addEffect(new MobEffectInstance(effect, 200, 0)));
    }
}
