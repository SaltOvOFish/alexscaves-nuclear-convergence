package com.soof.nuclearcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 钙铀云母长矛的投掷物。
 *
 * <p>行为参照 AC 的 {@code LimestoneSpearEntity}：伤害 4、命中后不再重复判定、
 * 命中生物时施加辐照，拾取物是钙铀云母长矛本体。</p>
 *
 * <p>使用独立实体类型（而不是复用 AC 的石灰岩长矛实体）的原因：
 * AC 的实体构造函数会丢弃传入的物品栈，且物品栈不同步到客户端；
 * 独立类型可以让渲染、拾取、命中判定都直接依赖自身，无需同步 hack。</p>
 */
public class AutuniteSpearEntity extends AbstractArrow {

    /** AC 石灰岩长矛的命中音效。按注册名解析，避开 Up / Neo 两版不同的 Java 包名。 */
    private static final ResourceLocation LIMESTONE_SPEAR_HIT_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "limestone_spear_hit");

    private boolean dealtDamage;

    public AutuniteSpearEntity(EntityType<? extends AutuniteSpearEntity> type, Level level) {
        super(type, level);
    }

    public AutuniteSpearEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.AUTUNITE_SPEAR.get(), owner, level, stack.copy(), null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModBlocks.AUTUNITE_SPEAR.get());
    }

    @Override
    protected EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return this.dealtDamage ? null : super.findHitEntity(from, to);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
        this.dealtDamage = true;

        if (target.hurt(source, 4.0F)) {
            if (target.getType() == EntityType.ENDERMAN) {
                return;
            }
            if (target instanceof LivingEntity living) {
                this.doPostHurtEffects(living);
            }
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        this.playSound(hitSound(), 1.0F, 1.0F);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        // 命中施加 20 秒辐照 I（与之前长矛一致）。
        SpearCompat.applyIrradiated(target);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        // 与 AC 石灰岩长矛一致：扎地面和扎中实体用同一个音效。
        return hitSound();
    }

    /**
     * 石灰岩长矛命中音效。AC 未安装（或声音注册名变动）时退回三叉戟音效，避免静音或空指针。
     */
    private static SoundEvent hitSound() {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(LIMESTONE_SPEAR_HIT_ID);
        return sound != null ? sound : SoundEvents.TRIDENT_HIT;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }
}
