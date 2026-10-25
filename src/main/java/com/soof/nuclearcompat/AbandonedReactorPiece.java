package com.soof.nuclearcompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.Optional;

/**
 * 废弃反应堆的 StructurePiece。
 *
 * <p>本身只是“占位框”：包围盒正好覆盖起始区块，{@code postProcess} 在且仅在该区块调用一次，
 * 然后把放置工作交给 {@link AbandonedReactorFeature}，从而保留原有的模板变体、居中、
 * 基座、战利品与酸液避让逻辑。</p>
 */
public class AbandonedReactorPiece extends StructurePiece {

    private static final List<ResourceLocation> TEMPLATES = List.of(
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "abandoned_reactor_0"),
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "abandoned_reactor_1"),
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "abandoned_reactor_2"),
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "abandoned_reactor_3"));

    private static final ResourceKey<LootTable> CHEST_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "chests/abandoned_reactor"));

    private static final AbandonedReactorFeature.Config CONFIG =
            new AbandonedReactorFeature.Config(TEMPLATES, CHEST_LOOT, 1);

    private static final AbandonedReactorFeature FEATURE =
            new AbandonedReactorFeature(AbandonedReactorFeature.Config.CODEC);

    public AbandonedReactorPiece(ChunkPos chunk, int minY, int maxY) {
        super(ModStructures.REACTOR_PIECE.get(), 0, new BoundingBox(
                chunk.getMinBlockX(), minY, chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), maxY - 1, chunk.getMaxBlockZ()));
    }

    public AbandonedReactorPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.REACTOR_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        // 起始区块可由 boundingBox 推导，无需额外数据。
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // 包围盒只有一个区块；这个判断防止 piece 被重复处理。
        if (chunkPos.getMinBlockX() != this.boundingBox.minX()
                || chunkPos.getMinBlockZ() != this.boundingBox.minZ()) {
            return;
        }

        BlockPos origin = new BlockPos(
                this.boundingBox.minX() + 8,
                level.getMinBuildHeight() + 3,
                this.boundingBox.minZ() + 8);

        FEATURE.place(new FeaturePlaceContext<>(
                Optional.empty(), level, generator, random, origin, CONFIG));
    }
}
