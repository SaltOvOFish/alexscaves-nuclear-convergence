package com.soof.nuclearcompat;

import com.alexscaves.server.item.SpearItem;
import com.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 钙铀云母长矛：行为与 Alex's Caves 的石灰岩长矛一致（近战 + 投掷），
 * 但命中敌人时会额外施加 20 秒的辐照 I。
 */
public class AutuniteSpearItem extends SpearItem {

    private static final ResourceLocation IRRADIATED_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "irradiated");

    /** 20 秒 = 400 tick，等级 I（amplifier 0）。 */
    private static final int IRRADIATED_DURATION = 400;

    public AutuniteSpearItem(Item.Properties properties) {
        super(properties, 3.0);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        SpearCompat.applyIrradiated(target);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int useTime = this.getUseDuration(stack, entity) - timeLeft;
        float power = this.getPowerForTime(useTime);
        if (power <= 0.1F) {
            return;
        }

        AutuniteSpearEntity spear = new AutuniteSpearEntity(level, player, stack);
        spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 2.5F, 1.0F);

        if (player.getAbilities().instabuild) {
            spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        }

        level.addFreshEntity(spear);
        level.playSound(null, spear, ACSoundRegistry.LIMESTONE_SPEAR_THROW.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
    }

}
