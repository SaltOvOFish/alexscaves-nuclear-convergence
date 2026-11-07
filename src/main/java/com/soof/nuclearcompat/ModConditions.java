package com.soof.nuclearcompat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 数据包条件：让数据文件（配方 / 生物群系修改器 / 结构）跟随配置开关。
 *
 * <p>用法示例：</p>
 * <pre>{@code
 * "neoforge:conditions": [
 *   { "type": "alexscaves_nuclear_convergence:config", "key": "oreganized.full_lead_integration" }
 * ]
 * }</pre>
 *
 * <p>条件在数据包重载时求值，因此修改配置后需要 {@code /reload} 或重启。</p>
 */
public final class ModConditions {

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, NuclearCompat.MODID);

    private ModConditions() {
    }

    public static void register(IEventBus modEventBus) {
        CONDITION_CODECS.register("config", () -> ConfigCondition.CODEC);
        CONDITION_CODECS.register(modEventBus);
    }

    /** 读取 {@link NuclearCompatConfig#isEnabled(String)} 的条件。 */
    public record ConfigCondition(String key) implements ICondition {

        public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(ConfigCondition::key)
        ).apply(instance, ConfigCondition::new));

        @Override
        public boolean test(IContext context) {
            return NuclearCompatConfig.isEnabled(this.key);
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }
}
