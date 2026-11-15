package com.soof.nuclearcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/** 跨 Alex's Caves Up / Neo 两版通用的辐照施加与长矛投掷逻辑（只依赖原版注册表）。 */
public final class SpearCompat {

    private static final ResourceLocation SUGAR_RUSH_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "sugar_rush");

    private static final ResourceLocation IRRADIATED_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "irradiated");

    /** 长矛命中施加的辐照时长：20 秒 = 400 tick。 */
    private static final int IRRADIATED_DURATION = 400;
    /** 甜蜜冲刺时长：与紫色苏打水一致，10 秒 = 200 tick。 */
    private static final int SUGAR_RUSH_DURATION = 200;

    private SpearCompat() {
    }

    public static void applyIrradiated(LivingEntity target) {
        applyIrradiated(target, IRRADIATED_DURATION);
    }

    public static void applyIrradiated(LivingEntity target, int duration) {
        BuiltInRegistries.MOB_EFFECT.getHolder(IRRADIATED_ID).ifPresent(effect ->
                target.addEffect(new MobEffectInstance(effect, duration, 0)));
    }

    /** 甜蜜冲刺，默认时长与紫色苏打水一致（{@value #SUGAR_RUSH_DURATION} tick，amplifier 0）。 */
    public static void applySugarRush(LivingEntity target) {
        applySugarRush(target, SUGAR_RUSH_DURATION);
    }

    /** 甜蜜冲刺，指定时长（tick），等级与紫色苏打水一致（amplifier 0）。 */
    public static void applySugarRush(LivingEntity target, int duration) {
        BuiltInRegistries.MOB_EFFECT.getHolder(SUGAR_RUSH_ID).ifPresent(effect ->
                target.addEffect(new MobEffectInstance(effect, duration, 0)));
    }

    /**
     * 长矛投掷的共享实现：两版 {@code releaseUsing} 逐行相同，差别只在音效的获取方式
     * （Up 版能直接拿 {@code ACSoundRegistry} 字段，其余移植版必须按注册名查）。
     *
     * <p>纯搬移：生成投掷物 → 赋速 → 创造模式只拾取 → 播放音效 → 非创造模式消耗 1 根 → 计统计，
     * 顺序与判断条件与原两版完全一致。{@code power} 由调用方用本版
     * {@code SpearItem} 的 {@code getPowerForTime} 算好传入，避免这里依赖具体版本的方法。</p>
     */
    public static void throwSpear(Item item, ItemStack stack, Level level, Player player,
                                  float power, Supplier<SoundEvent> throwSound) {
        AutuniteSpearEntity spear = new AutuniteSpearEntity(level, player, stack);
        spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 2.5F, 1.0F);

        if (player.getAbilities().instabuild) {
            spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        }

        level.addFreshEntity(spear);
        // 用 Supplier 而不是直接传值：保持「先生成投掷物、再取音效」的原求值顺序。
        SoundEvent sound = throwSound.get();
        if (sound != null) {
            level.playSound(null, spear, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.awardStat(Stats.ITEM_USED.get(item));
    }
}
