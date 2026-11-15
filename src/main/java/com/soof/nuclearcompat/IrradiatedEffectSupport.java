package com.soof.nuclearcompat;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

/**
 * 三套辐照混入（Up / Neo / Continued）的共享逻辑。
 *
 * <p>为什么单开一个类而不是继续在混入里复制：混入的 {@code @Mixin} 目标无法参数化，
 * Up 包名是 {@code com.alexscaves}、其余移植版是 {@code com.github.alexmodguy.alexscaves}，
 * 注入点也不同（{@code applyEffectTick} vs Continued 的 {@code tick}），所以混入类必须各留一份；
 * 但注入体里的判断逻辑三份完全一致——抽到此处后，改一处即可作用于全部移植版。</p>
 *
 * <p>行为等价：这里的每个方法都是原混入方法体的逐行搬移，没有新增分支、没有调整顺序。
 * 混入只保留「在哪个方法、什么时机、如何取消」这部分框架代码。</p>
 */
public final class IrradiatedEffectSupport {

    private static final ResourceLocation AC_IRRADIATED =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "irradiated");

    /**
     * 辐照效果的 Holder 缓存。
     *
     * <p>为什么不放在 {@code NuclearCompat.AC_IRRADIATED}：那个字段在 {@code commonSetup}
     * 里解析，而混入的 {@code applyEffectTick} 首次调用一定晚于注册表冻结，
     * 这里惰性解析既安全又能省掉每次结算一次注册表查询 + {@code Optional} 分配。</p>
     */
    private static volatile Holder<MobEffect> irradiatedHolder;

    private IrradiatedEffectSupport() {
    }

    /** 解析（并缓存）辐照效果的 Holder；效果不存在时返回 null。 */
    private static Holder<MobEffect> irradiated() {
        Holder<MobEffect> holder = irradiatedHolder;
        if (holder == null) {
            holder = BuiltInRegistries.MOB_EFFECT.getHolder(AC_IRRADIATED).orElse(null);
            irradiatedHolder = holder;
        }
        return holder;
    }

    /**
     * 穿戴整套防化服或防辐射套装时的免疫判定：移除辐照效果并告知调用方跳过本次结算。
     *
     * @return {@code true} 表示已免疫，调用方应立即取消原方法（返回 false / 不执行 tick）
     */
    public static boolean clearWhenFullyArmored(LivingEntity entity) {
        if (!ArmorCompat.hasFullAntiRadiationArmor(entity) && !ArmorCompat.hasFullHazmatArmor(entity)) {
            return false;
        }
        Holder<MobEffect> irradiated = irradiated();
        if (irradiated != null) {
            entity.removeEffect(irradiated);
        }
        return true;
    }
}
