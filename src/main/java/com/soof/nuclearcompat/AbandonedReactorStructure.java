package com.soof.nuclearcompat;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * 废弃反应堆的结构入口。
 *
 * <p>只负责“在哪个区块、哪个高度尝试生成”，实际放置仍由
 * {@link AbandonedReactorFeature} 完成（模板变体、基座、战利品标记、酸液避让等），
 * 因此行为与原 feature 版本一致，同时可被 {@code /locate structure} 查询。</p>
 */
public class AbandonedReactorStructure extends Structure {

    public static final MapCodec<AbandonedReactorStructure> CODEC =
            simpleCodec(AbandonedReactorStructure::new);

    /**
     * 群系采样高度。
     *
     * <p>注意：这里必须保持极少量采样。{@code /locate} 会在半径内逐个检查候选区块，
     * 每个候选都调用本方法；之前做 28 次连续采样时，配合 {@code spacing 4} 曾把
     * 单次服务端 tick 卡到 200 秒以上（每个候选约 0.5ms × 28 × 2500）。</p>
     */
    private static final int[] SAMPLE_YS = { -8, -40 };

    public AbandonedReactorStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMinBlockX() + 8;
        int z = chunk.getMinBlockZ() + 8;

        int y = findBiomeY(context, x, z);
        if (y == Integer.MIN_VALUE) {
            return Optional.empty();
        }

        return Optional.of(new GenerationStub(new BlockPos(x, y, z), pieces -> pieces.addPiece(
                new AbandonedReactorPiece(chunk,
                        context.heightAccessor().getMinBuildHeight(),
                        context.heightAccessor().getMaxBuildHeight()))));
    }

    @Override
    public Optional<GenerationStub> findValidGenerationPoint(GenerationContext context) {
        // findGenerationPoint 里已经判断过群系，这里不要再采样一次（/locate 时能省一半开销）。
        return findGenerationPoint(context);
    }

    /** 在毒化洞穴常见的深度里采样群系，命中即返回。 */
    private static int findBiomeY(GenerationContext context, int x, int z) {
        Climate.Sampler sampler = context.randomState().sampler();
        int quartX = QuartPos.fromBlock(x);
        int quartZ = QuartPos.fromBlock(z);
        for (int y : SAMPLE_YS) {
            Holder<Biome> biome = context.biomeSource()
                    .getNoiseBiome(quartX, QuartPos.fromBlock(y), quartZ, sampler);
            if (context.validBiome().test(biome)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ABANDONED_REACTOR.get();
    }
}
