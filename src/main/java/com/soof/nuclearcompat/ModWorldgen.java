package com.soof.nuclearcompat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 世界生成相关的自定义注册项。
 *
 * <p>自定义放置修饰符 {@code alexscaves_nuclear_convergence:config_count}：
 * 与 {@code minecraft:count} 等价，但尝试次数会乘以配置里的密度倍率（支持小数），
 * 倍率为 0 时直接不生成。</p>
 *
 * <p>JSON 写法：</p>
 * <pre>{@code
 * { "type": "alexscaves_nuclear_convergence:config_count", "key": "tuff_thorium_ore", "count": 32 }
 * }</pre>
 */
public final class ModWorldgen {

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, NuclearCompat.MODID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<ConfigCountPlacement>> CONFIG_COUNT =
            PLACEMENT_TYPES.register("config_count", () -> ConfigCountPlacement.TYPE);

    private ModWorldgen() {
    }

    public static void register(IEventBus modEventBus) {
        PLACEMENT_TYPES.register(modEventBus);
    }

    /** 按配置倍率缩放的 count 放置修饰符（必须是普通类：PlacementModifier 是抽象类，record 无法继承）。 */
    public static final class ConfigCountPlacement extends PlacementModifier {

        public static final MapCodec<ConfigCountPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(placement -> placement.key),
                Codec.intRange(0, 1024).fieldOf("count").forGetter(placement -> placement.count)
        ).apply(instance, ConfigCountPlacement::new));

        public static final PlacementModifierType<ConfigCountPlacement> TYPE = () -> CODEC;

        private final String key;
        private final int count;

        public ConfigCountPlacement(String key, int count) {
            this.key = key;
            this.count = count;
        }

        @Override
        public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
            double chance = NuclearCompatConfig.oreChance(this.key);
            if (chance <= 0.0D || this.count <= 0) {
                return Stream.empty();
            }
            double exact = this.count * chance;
            int amount = (int) Math.floor(exact);
            // 小数部分按概率补 1 次，保证 0.5 倍真的是一半
            if (random.nextFloat() < (float) (exact - amount)) {
                amount++;
            }
            return amount <= 0 ? Stream.empty() : IntStream.range(0, amount).mapToObj(i -> pos);
        }

        @Override
        public PlacementModifierType<?> type() {
            return TYPE;
        }
    }
}
