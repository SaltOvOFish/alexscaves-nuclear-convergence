package com.soof.nuclearcompat;

import com.github.alexmodguy.alexscaves.server.item.SpearItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 钙铀云母长矛（Alex's Caves Neo 移植版）。
 *
 * <p>与 {@link AutuniteSpearItem} 行为完全一致，仅继承自 Neo 版的 {@code SpearItem}。</p>
 */
public class AutuniteSpearItemNeo extends SpearItem {

    /**
     * 投掷音效按注册名解析，不直接引用 {@code ACSoundRegistry}。
     *
     * <p>各移植版把这个字段声明成了不同类型（AC Neo / Unofficial Port 是
     * {@code DeferredHolder}，Continued 是 {@code Supplier}，RENEWED 是自带的
     * {@code RegistryObject}），直接调 {@code .get()} 会在其中两个版本上抛
     * {@code NoSuchFieldError}；按注册名查注册表则三种都能用。</p>
     */
    private static final ResourceLocation LIMESTONE_SPEAR_THROW_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "limestone_spear_throw");

    public AutuniteSpearItemNeo(Item.Properties properties) {
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
        SoundEvent throwSound = BuiltInRegistries.SOUND_EVENT.get(LIMESTONE_SPEAR_THROW_ID);
        if (throwSound != null) {
            level.playSound(null, spear, throwSound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
    }
}
