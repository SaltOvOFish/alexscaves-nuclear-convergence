package com.soof.nuclearcompat;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * 自定义进度触发器：累计喝下指定数量的核子可乐。
 *
 * <p>原版 {@code minecraft:consume_item} 只能判断「喝了一口」，无法累计次数，
 * 因此这里用玩家附件记录总瓶数，并在每次饮用后带着当前计数触发；
 * 进度 JSON 里写 {@code "count": 64} 即可要求累计到 64 瓶。</p>
 */
public class NukaColaDrunkTrigger extends SimpleCriterionTrigger<NukaColaDrunkTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** 当前累计瓶数达到或超过进度要求时触发。 */
    public void trigger(ServerPlayer player, int count) {
        this.trigger(player, instance -> count >= instance.count());
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, int count)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        Codec.INT.fieldOf("count").forGetter(TriggerInstance::count)
                )
                .apply(instance, TriggerInstance::new));
    }
}
