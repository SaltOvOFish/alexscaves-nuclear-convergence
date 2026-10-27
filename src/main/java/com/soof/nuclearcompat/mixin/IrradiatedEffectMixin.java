package com.soof.nuclearcompat.mixin;

import com.alexscaves.server.item.HazmatArmorItem;
import com.alexscaves.server.potion.IrradiatedEffect;
import com.soof.nuclearcompat.ArmorCompat;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让「防辐射套装」与「防化套装」在 Alex's Caves Up 的辐照效果上能力对等：
 *
 * 1. 部分穿戴（1~3 件）：把防辐射护甲计入防化服数量，获得每件 -25% 的辐照伤害减免。
 * 2. 穿戴整套（4 件防辐射套装 或 4 件防化套装）：免疫辐照效果——在结算前直接移除
 *    Irradiated 效果并跳过本次伤害（不受伤、不饥饿、效果被清除）。
 */
@Mixin(IrradiatedEffect.class)
public abstract class IrradiatedEffectMixin {

    private static final ResourceLocation AC_IRRADIATED = ResourceLocation.fromNamespaceAndPath("alexscaves", "irradiated");

    /** 辐照效果的 Holder 缓存：避免每次结算都做一次注册表查询 + Optional 分配。 */
    private static Holder<MobEffect> acIrradiated;

    private static Holder<MobEffect> acIrradiated() {
        Holder<MobEffect> holder = acIrradiated;
        if (holder == null) {
            holder = BuiltInRegistries.MOB_EFFECT.getHolder(AC_IRRADIATED).orElse(null);
            acIrradiated = holder;
        }
        return holder;
    }

    @Inject(
            method = "applyEffectTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void acnc$immuneWhenFullArmor(LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        if (ArmorCompat.hasFullAntiRadiationArmor(entity) || ArmorCompat.hasFullHazmatArmor(entity)) {
            Holder<MobEffect> irradiated = acIrradiated();
            if (irradiated != null) {
                entity.removeEffect(irradiated);
            }
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "applyEffectTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/alexscaves/server/item/HazmatArmorItem;getWornAmount(Lnet/minecraft/world/entity/LivingEntity;)I"
            )
    )
    private static int acnc$countAntiRadiationAsHazmat(LivingEntity entity) {
        return ArmorCompat.countHazmatLikeArmor(HazmatArmorItem.getWornAmount(entity), entity);
    }
}
