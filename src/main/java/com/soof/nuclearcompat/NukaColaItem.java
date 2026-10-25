package com.soof.nuclearcompat;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 核子可乐。
 *
 * <p>行为复制自 Alex's Caves 的紫色苏打水瓶（{@code purple_soda_bottle}）：
 * 堆叠 16、营养 2 / 饱和度 0.1、饮用 40 tick、返还玻璃瓶。</p>
 *
 * <p>额外效果：饮用后获得 10 秒甜蜜冲刺（与紫色苏打水同级，amplifier 0）
 * 与 10 秒辐照 I。</p>
 */
public class NukaColaItem extends Item {

    /** 与紫色苏打水瓶一致的饮用时长（40 tick）。 */
    private static final int DRINK_DURATION = 40;

    /** 10 秒 = 200 tick。 */
    private static final int EFFECT_DURATION = 200;

    public NukaColaItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // 与原版食物一样结算饱和度；返回值忽略（与 AC 的 DrinkableBottledItem 一致）。
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide) {
            SpearCompat.applySugarRush(entity);
            SpearCompat.applyIrradiated(entity, EFFECT_DURATION);
        }

        if (entity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }

        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }

        return stack;
    }
}
