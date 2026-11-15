package com.soof.nuclearcompat;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.nuclearteam.createnuclear.content.radiation.capability.RadiationCapability;

/**
 * 核心桥接逻辑：把两套模组的辐射统一成同一套机制。
 *
 * <p>方向由配置 {@code general.radiation_system} 决定：</p>
 * <ul>
 *     <li><b>AC（默认）</b>：拦截原子核动的 {@code createnuclear:radiation}，改为施加
 *         Alex's Caves 的 {@code alexscaves:irradiated}；</li>
 *     <li><b>CN</b>：反过来——原子核动的辐射原样生效；所有 {@code alexscaves:irradiated}
 *         的施加（长矛、核子可乐、AC 自己的放射性物品 / 废料方块 / 伽马蟑螂 / 核爆）都被拦下，
 *         按等级折算成原子核动的辐射剂量（{@link RadiationCapability#applyContagion}）。</li>
 * </ul>
 *
 * <p>为什么用 {@link MobEffectEvent.Applicable} 而不是 {@link MobEffectEvent.Added}：
 * {@code Added} 不可取消，且事件触发时效果尚未写入实体，无法真正阻止原效果生效；
 * {@code Applicable} 在 {@code LivingEntity#canBeAffected} 阶段触发，可通过
 * {@code setResult(Result.DO_NOT_APPLY)} 在效果生效前彻底拦截，是最干净的替换点。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID)
public final class RadiationCompatHandler {

    /**
     * 原子核动默认的辐射等级阈值（{@code radiation_level_1/2/3}），用于把 AC 辐照等级
     * 折算成辐射剂量：辐照 I / II / III → 10 / 25 / 50。若在原子核动配置里改过阈值，
     * 这里的比例关系会随之偏移，需要同步调整。
     */
    private static final double CN_DOSE_LEVEL_1 = 10.0D;
    private static final double CN_DOSE_LEVEL_2 = 25.0D;
    private static final double CN_DOSE_LEVEL_3 = 50.0D;

    /**
     * 撼地斯拉（Tremorzilla）的实体注册名。
     *
     * <p>AC 原版就对辐照免疫（{@code canBeAffected} 直接返回 false），但原子核动的
     * {@code createnuclear:radiation} 是独立效果，会正常施加并造成伤害；本模组把它当成
     * 同一套辐射体系后，需要在这里显式拦下，保持「核弹炸不死、辐射也伤不到」的一致行为。</p>
     */
    private static final ResourceLocation TREMORZILLA_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "tremorzilla");

    /** 无限时长的辐照（{@code /effect give ... infinite}）折算成多少 tick 的剂量。 */
    private static final int INFINITE_FALLBACK_TICKS = 6000;

    /** 至少保留多久，避免时长极短的辐照折算后完全不生效。 */
    private static final int MIN_CONTAGION_TICKS = 20;

    /**
     * 当前处于「辐射 → 辐照」转译状态的实体（仅 AC 模式使用）。
     *
     * <p>为什么必须记录：Create Nuclear 的 {@code RadiationCapability#applyEffects}
     * 每 tick 都会在辐射值 &lt;= 0 时调用 {@code entity.removeEffect(createnuclear:radiation)}，
     * 哪怕实体身上根本没有该效果；而 NeoForge 的 {@code MobEffectEvent.Remove}
     * 在 {@code LivingEntity#removeEffect} 里是在 <b>移除之前</b> 无条件触发的，
     * 并不会先判断效果是否真的存在。若不加区分地跟着删 {@code alexscaves:irradiated}，
     * 就会把与辐射无关的辐照来源（例如核子可乐）在下一个 tick 直接抹掉。</p>
     *
     * <p>用 {@link WeakHashMap} 作为键集，实体卸载后条目自动回收，不会泄漏。</p>
     */
    private static final Set<LivingEntity> CONVERTED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private RadiationCompatHandler() {
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) {
            return;
        }

        LivingEntity entity = event.getEntity();

        // 注意：不能直接 == 比较 Holder。Create Nuclear 传入的是 DeferredHolder，
        // 而 BuiltInRegistries 返回的是 Holder.Reference，两者是不同对象。
        // 用 Holder.is(ResourceLocation) 按注册名比较才可靠。
        boolean isCnRadiation = instance.getEffect().is(NuclearCompat.CN_RADIATION_ID);
        boolean isAcIrradiated = instance.getEffect().is(NuclearCompat.AC_IRRADIATED_ID);

        // 只处理两个模组的辐射/辐照效果，其余效果一律不干预。
        if (!isCnRadiation && !isAcIrradiated) {
            return;
        }

        // 撼地斯拉免疫原子核动的辐射（AC 侧本来就免疫辐照，这里把 CN 的效果也一并拦下）。
        if (isTremorzilla(entity)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }

        // 穿戴整套防化服或防辐射服：彻底免疫，不施加任何辐射/辐照效果。
        // 这样防化服也能像防辐射服一样，在携带铀棒、站在富集营火上时不受辐射。
        // 两种辐射体系都适用。
        if (ArmorCompat.hasFullHazmatArmor(entity) || ArmorCompat.hasFullAntiRadiationArmor(entity)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }

        if (NuclearCompatConfig.useCreateNuclearRadiation()) {
            // ---- CN 模式：AC 辐照折算成原子核动的辐射剂量 ----
            // 原子核动自己的辐射效果不做任何处理，让它原样生效。
            if (isAcIrradiated) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                applyCnContagion(entity, instance);
            }
            return;
        }

        // ---- AC 模式：把原子核动的辐射效果统一替换为辐照效果 ----
        if (isCnRadiation && NuclearCompat.AC_IRRADIATED != null) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);

            // 标记为「正在转译」，供 onEffectRemoved 区分真正的离开辐射区与 CN 每 tick 的空调用。
            CONVERTED.add(entity);

            // CN 的 applyEffects 每个 tick 都会重新施加辐射效果，若每次都刷新 duration，
            // 会导致辐照效果的倒计时永不递减、缓慢扣血（applyEffectTick）永不触发。
            // 因此：仅当实体没有辐照效果、或辐射等级发生变化时才重新施加，
            // 其余情况保持现有效果，让 duration 正常流转。
            MobEffectInstance existing = entity.getEffect(NuclearCompat.AC_IRRADIATED);
            if (existing != null && existing.getAmplifier() == instance.getAmplifier()) {
                return;
            }

            entity.addEffect(new MobEffectInstance(
                    NuclearCompat.AC_IRRADIATED,
                    instance.getDuration(),
                    instance.getAmplifier(),
                    instance.isAmbient(),
                    instance.isVisible()
            ));
        }
    }

    /** 按实体注册名判断是不是撼地斯拉（不依赖任何一版 AC 的实体类，兼容全部移植版）。 */
    private static boolean isTremorzilla(LivingEntity entity) {
        return TREMORZILLA_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    /**
     * 把一次 AC 辐照折算成原子核动的辐射剂量。
     *
     * <p>原子核动的辐射值是「按来源求和」的（物品栏 + 群系 + 传染），不能直接改
     * {@code radiation} 字段——下一 tick 就会被重算覆盖。{@code applyContagion} 写入的是
     * 会逐 tick 递减的「外部剂量」，正好用来承载带时长的辐照：剂量决定等级，tick 数决定残留时间。</p>
     */
    private static void applyCnContagion(LivingEntity entity, MobEffectInstance instance) {
        int amplifier = Math.max(0, instance.getAmplifier());
        double dose = switch (amplifier) {
            case 0 -> CN_DOSE_LEVEL_1;
            case 1 -> CN_DOSE_LEVEL_2;
            // 超过 III 级时按每级 +50 继续线性放大，保证高等级辐照仍然有明显效果。
            default -> CN_DOSE_LEVEL_3 + (amplifier - 2) * CN_DOSE_LEVEL_3;
        };

        int duration = instance.getDuration();
        int ticks = duration == MobEffectInstance.INFINITE_DURATION
                ? INFINITE_FALLBACK_TICKS
                : Math.max(MIN_CONTAGION_TICKS, duration);

        RadiationCapability.applyContagion(entity, dose, ticks);
    }

    /**
     * 同步清除（仅 AC 模式）：当原子核动试图移除自己的辐射效果（辐射源消失或等级变化）时，
     * 一并移除 Alex's Caves Up 的辐照效果，保持与原版「离开辐射区立即解除」一致。
     */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (NuclearCompat.CN_RADIATION == null || NuclearCompat.AC_IRRADIATED == null) {
            return;
        }
        // CN 模式不做转译，也就不需要同步清除。
        if (NuclearCompatConfig.useCreateNuclearRadiation()) {
            return;
        }

        if (event.getEffect().is(NuclearCompat.CN_RADIATION_ID)) {
            // 只有该实体确实处于「辐射 → 辐照」转译状态时，才同步清除辐照。
            // 否则这只是 CN 在辐射值为 0 时每 tick 发出的空移除调用
            // （NeoForge 在效果不存在时同样会触发 Remove），必须忽略，
            // 不然核子可乐之类独立来源的辐照会立刻被删掉。
            //
            // 性能：CN 每 tick 会对每个生物调用一次 removeEffect，这里先用 isEmpty()
            // 短路（O(1)、无哈希），否则每次都要在 WeakHashMap 里查一遍。
            if (!CONVERTED.isEmpty() && CONVERTED.remove(event.getEntity())) {
                event.getEntity().removeEffect(NuclearCompat.AC_IRRADIATED);
            }
        }
    }
}
