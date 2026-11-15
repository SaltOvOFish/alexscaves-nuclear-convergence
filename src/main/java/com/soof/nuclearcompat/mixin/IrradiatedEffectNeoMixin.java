package com.soof.nuclearcompat.mixin;

import com.github.alexmodguy.alexscaves.server.item.HazmatArmorItem;
import com.github.alexmodguy.alexscaves.server.potion.IrradiatedEffect;
import com.soof.nuclearcompat.ArmorCompat;
import com.soof.nuclearcompat.IrradiatedEffectSupport;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Alex's Caves Neo（包名 com.github.alexmodguy.alexscaves）专用版。
 * 逻辑与 {@code IrradiatedEffectMixin}（Alex's Caves Up）完全一致，
 * 共享部分见 {@link IrradiatedEffectSupport}。
 */
@Mixin(IrradiatedEffect.class)
public abstract class IrradiatedEffectNeoMixin {

    @Inject(
            method = "applyEffectTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void acnc$immuneWhenFullArmor(LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        if (IrradiatedEffectSupport.clearWhenFullyArmored(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "applyEffectTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexmodguy/alexscaves/server/item/HazmatArmorItem;getWornAmount(Lnet/minecraft/world/entity/LivingEntity;)I"
            )
    )
    private static int acnc$countAntiRadiationAsHazmat(LivingEntity entity) {
        return ArmorCompat.countHazmatLikeArmor(HazmatArmorItem.getWornAmount(entity), entity);
    }
}
