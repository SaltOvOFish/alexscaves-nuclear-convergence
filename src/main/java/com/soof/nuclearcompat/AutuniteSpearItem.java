package com.soof.nuclearcompat;

import com.alexscaves.server.item.SpearItem;
import com.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 钙铀云母长矛：行为与 Alex's Caves 的石灰岩长矛一致（近战 + 投掷），
 * 但命中敌人时会额外施加 20 秒的辐照 I。
 */
public class AutuniteSpearItem extends SpearItem {

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
        // 投掷流程在 SpearCompat，与 Neo 版共用；音效来源两版不同，故在这里取。
        SpearCompat.throwSpear(this, stack, level, player, power,
                () -> ACSoundRegistry.LIMESTONE_SPEAR_THROW.get());
    }
}
