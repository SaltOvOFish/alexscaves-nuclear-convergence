package com.soof.nuclearcompat.mixin;

import com.github.alexmodguy.alexscaves.server.item.HazmatArmorItem;
import com.github.alexmodguy.alexscaves.server.potion.IrradiatedEffect;
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
 * Alex's Caves Neo（包名 com.github.alexmodguy.alexscaves）专用版。
 * 逻辑与 {@code IrradiatedEffectMixin}（Alex's Caves Up）完全一致。
 */
@Mixin(IrradiatedEffect.class)
public abstract class IrradiatedEffectNeoMixin {

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
                    target = "Lcom/github/alexmodguy/alexscaves/server/item/HazmatArmorItem;getWornAmount(Lnet/minecraft/world/entity/LivingEntity;)I"
            )
    )
    private static int acnc$countAntiRadiationAsHazmat(LivingEntity entity) {
        return ArmorCompat.countHazmatLikeArmor(HazmatArmorItem.getWornAmount(entity), entity);
    }
}
