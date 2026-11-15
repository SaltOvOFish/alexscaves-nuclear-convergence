package com.soof.nuclearcompat.mixin;

import com.github.alexmodguy.alexscaves.server.item.HazmatArmorItem;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 适用于「Alex's Caves Continued」及同类移植版（包名同为
 * {@code com.github.alexmodguy.alexscaves}，但辐照效果走自己的 {@code ACMobEffect} 抽象）。
 *
 * <p>这些版本没有覆写原版的 {@code applyEffectTick}，而是覆写自建的
 * {@code tick(LivingEntity, int)}；因此 {@link IrradiatedEffectNeoMixin} 里的注入点找不到，
 * 需要单独一套挂到 {@code tick} 上。逻辑与另外两套完全一致：</p>
 * <ul>
 *     <li>穿戴整套防化服 / 防辐射套装时清除辐照并跳过本次结算；</li>
 *     <li>把防辐射护甲计入防化服数量，获得每件 -25% 的辐照伤害减免。</li>
 * </ul>
 *
 * <p>目标类用字符串指定，方便在缺少该类的移植版上安全跳过（混入配置为 {@code required: false}）。</p>
 */
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.potion.IrradiatedEffect")
public abstract class IrradiatedEffectContinuedMixin {

    private static final ResourceLocation AC_IRRADIATED =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "irradiated");

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
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void acnc$immuneWhenFullArmor(LivingEntity entity, int amplifier, CallbackInfo ci) {
        if (ArmorCompat.hasFullAntiRadiationArmor(entity) || ArmorCompat.hasFullHazmatArmor(entity)) {
            Holder<MobEffect> irradiated = acIrradiated();
            if (irradiated != null) {
                entity.removeEffect(irradiated);
            }
            ci.cancel();
        }
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexmodguy/alexscaves/server/item/HazmatArmorItem;getWornAmount(Lnet/minecraft/world/entity/LivingEntity;)I"
            )
    )
    private static int acnc$countAntiRadiationAsHazmat(LivingEntity entity) {
        return ArmorCompat.countHazmatLikeArmor(HazmatArmorItem.getWornAmount(entity), entity);
    }
}
