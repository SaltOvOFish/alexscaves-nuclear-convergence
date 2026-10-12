package com.soof.nuclearcompat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * 废弃反应堆结构 feature。
 *
 * 不依赖 Alex's Caves 的 feature 类型注册名（Up 为 {@code alexscaves:toxic_ruins}、
 * Neo 为 {@code alexscaves:underground_ruins}），从而同时兼容两个移植版。
 * 放置逻辑参考 Alex's Caves 的 {@code UndergroundRuinsFeature}。
 */
public class AbandonedReactorFeature extends Feature<AbandonedReactorFeature.Config> {

    /** 结构上方需要保持可替换（空气/可替换方块）的净空高度（格）。 */
    private static final int CLEARANCE_ABOVE = 9;

    /**
     * 结构底部到下方实心方块的最大允许距离（格）。下方 9 格内必须有实心方块支撑，
     * 否则视为高空悬空、放弃该列。仅空气与可替换方块计入该悬空距离，酸液不计入（可穿透）。
     */
    private static final int MAX_AIR_GAP = 9;

    /** 基座核心区在结构水平投影基础上外扩的格数。 */
    private static final int BASE_CORE_MARGIN = 1;

    /**
     * 基座斜坡裙边向外延伸的最大格数。实际只在地形有落差的地方延伸（按落差自适应），
     * 此值为兜底上限。
     */
    private static final int BASE_SKIRT_MAX = 16;

    /** 基座坡度：每向外 1 格，填充起点下降 BASE_SLOPE 格（2 即 2:1，比 1:1 更窄更陡）。 */
    private static final int BASE_SLOPE = 2;

    /** 基座中随机夹带的辐射铀矿石占比。 */
    private static final float BASE_ORE_CHANCE = 0.05f;

    /** 结构在生成区块内需保留的最小水平边距（格），避免贴住区块边界。 */
    private static final int CHUNK_MARGIN = 2;

    /** AC 酸液方块注册名。 */
    private static final ResourceLocation ACID_ID = ResourceLocation.fromNamespaceAndPath("alexscaves", "acid");

    public AbandonedReactorFeature(Codec<Config> codec) {
        super(codec);
    }

    public record Config(
            List<ResourceLocation> structures,
            ResourceKey<LootTable> chestLoot,
            int sinkBy
    ) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("structures").forGetter(Config::structures),
                ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("chest_loot").forGetter(Config::chestLoot),
                Codec.INT.fieldOf("sink_by").forGetter(Config::sinkBy)
        ).apply(instance, Config::new));
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        Config config = context.config();

        // 先确定变体与旋转；旋转会影响结构在水平面上的实际偏移，必须据此计算包围盒。
        Rotation rotation = Rotation.getRandom(random);
        StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();
        StructureTemplate template = manager.getOrCreate(
                config.structures().get(random.nextInt(config.structures().size())));

        // 「正在生成的区块」。FEATURES 阶段写入半径只有 ±1 区块，而原点是区块角（SectionPos.origin()）
        // 叠加 random_offset 后可能落在相邻区块；跨区块写入会在相邻区块后续生成时被覆盖/丢弃，
        // 表现为结构沿区块边界被整齐切掉。因此取 WorldGenRegion 的中心区块作为目标区块。
        ChunkPos genChunk = level instanceof WorldGenRegion region ? region.getCenter() : new ChunkPos(origin);
        int chunkSpanX = genChunk.getMaxBlockX() - genChunk.getMinBlockX() + 1;
        int chunkSpanZ = genChunk.getMaxBlockZ() - genChunk.getMinBlockZ() + 1;

        // 用一次「探针」求出当前旋转下，结构包围盒相对放置点的固定偏移与整体尺寸。
        // 关键：旋转 90/180/270 会把结构整体在水平面平移 (size-1) 格；
        // 若仍只按「半宽」把放置点内缩，本体就会被推出区块边界（表现为被切／贴边）。
        StructurePlaceSettings probeSettings = new StructurePlaceSettings().setRotation(rotation);
        BoundingBox probeBox = template.getBoundingBox(probeSettings, BlockPos.ZERO);
        int rotOffsetX = probeBox.minX();
        int rotOffsetY = probeBox.minY();
        int rotOffsetZ = probeBox.minZ();
        int bodyW = probeBox.getXSpan();
        int bodyD = probeBox.getZSpan();

        // 需求：结构本体严格居中于生成区块内，四周保留 CHUNK_MARGIN 边距
        // （尺寸过大无法留边距时退化为贴边居中）。
        int targetMinX = genChunk.getMinBlockX() + (chunkSpanX - bodyW) / 2;
        int targetMinZ = genChunk.getMinBlockZ() + (chunkSpanZ - bodyD) / 2;
        targetMinX = Math.max(genChunk.getMinBlockX() + CHUNK_MARGIN,
                Math.min(targetMinX, genChunk.getMaxBlockX() - bodyW + 1 - CHUNK_MARGIN));
        targetMinZ = Math.max(genChunk.getMinBlockZ() + CHUNK_MARGIN,
                Math.min(targetMinZ, genChunk.getMaxBlockZ() - bodyD + 1 - CHUNK_MARGIN));

        // 结构本体中心所在的竖直列，用于净空／支撑检查。
        int centerX = targetMinX + bodyW / 2;
        int centerZ = targetMinZ + bodyD / 2;

        // 放置点 = 目标最小角 − 旋转偏移，这样本体才会正好落在目标包围盒内。
        int placeX = targetMinX - rotOffsetX;
        int placeZ = targetMinZ - rotOffsetZ;

        // 在中心列上从下往上寻找可放置高度：本体所在的整列（CLEARANCE_ABOVE 格）可替换且**不含酸液**，
        // 且下方 MAX_AIR_GAP 格内有实心方块支撑（酸液不计入悬空距离）。
        // 若该竖直段内有酸液，则该高度作废、扫描继续向上，使结构至少生成在酸液顶部上方一格
        // （不会浸没在／被酸液盖住）。
        BlockPos pos = new BlockPos(centerX, level.getMinBuildHeight() + 3, centerZ);
        List<BlockPos> candidates = new ArrayList<>();
        int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, centerX, centerZ) - 5;
        while (pos.getY() < surfaceY) {
            if (isReplaceableColumn(level, pos.above(), CLEARANCE_ABOVE)
                    && !containsAcid(level, pos.above(), CLEARANCE_ABOVE)
                    && hasSolidBelow(level, pos, MAX_AIR_GAP)) {
                candidates.add(pos);
            }
            pos = pos.above();
        }
        if (candidates.isEmpty()) {
            return false;
        }
        BlockPos chosen = candidates.get(random.nextInt(candidates.size()));

        BoundingBox boundingBox = new BoundingBox(
                genChunk.getMinBlockX(), level.getMinBuildHeight(), genChunk.getMinBlockZ(),
                genChunk.getMaxBlockX(), level.getMaxBuildHeight(), genChunk.getMaxBlockZ());

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setBoundingBox(boundingBox)
                .setRandom(random)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);

        BlockPos placePos = new BlockPos(placeX, chosen.getY() + config.sinkBy() - rotOffsetY, placeZ);

        // 保险：按实际包围盒把本体推回生成区块内（防止旋转偏移估算与真实放置有出入）。
        BoundingBox bodyBox = template.getBoundingBox(settings, placePos);
        int corrX = 0;
        int corrZ = 0;
        if (bodyBox.minX() < genChunk.getMinBlockX()) {
            corrX = genChunk.getMinBlockX() - bodyBox.minX();
        } else if (bodyBox.maxX() > genChunk.getMaxBlockX()) {
            corrX = genChunk.getMaxBlockX() - bodyBox.maxX();
        }
        if (bodyBox.minZ() < genChunk.getMinBlockZ()) {
            corrZ = genChunk.getMinBlockZ() - bodyBox.minZ();
        } else if (bodyBox.maxZ() > genChunk.getMaxBlockZ()) {
            corrZ = genChunk.getMaxBlockZ() - bodyBox.maxZ();
        }
        if (corrX != 0 || corrZ != 0) {
            placePos = placePos.offset(corrX, 0, corrZ);
        }

        if (!template.placeInWorld(level, placePos, placePos, settings, random, 2)) {
            return false;
        }

        // 诊断：若结构超出生成区块，说明限制失败（理论上不会发生），打日志便于排查。
        BoundingBox placedBox = template.getBoundingBox(settings, placePos);
        if (placedBox.minX() < genChunk.getMinBlockX() || placedBox.maxX() > genChunk.getMaxBlockX()
                || placedBox.minZ() < genChunk.getMinBlockZ() || placedBox.maxZ() > genChunk.getMaxBlockZ()) {
            NuclearCompat.LOGGER.warn("[abandoned_reactor] structure box {} escapes generating chunk {} (origin {})",
                    placedBox, genChunk, origin);
        }

        // 处理 data 标记：loot_chest 标记下方的容器设置为战利品箱。
        for (StructureTemplate.StructureBlockInfo info : getDataMarkers(template, placePos, rotation)) {
            if (info.nbt() == null) {
                continue;
            }
            String metadata = info.nbt().getString("metadata");
            if ("loot_chest".equals(metadata)) {
                level.setBlock(info.pos(), Blocks.CAVE_AIR.defaultBlockState(), 3);
                if (level.getBlockEntity(info.pos().below()) instanceof RandomizableContainerBlockEntity container) {
                    container.setLootTable(config.chestLoot(), random.nextLong());
                }
            }
        }

        // 生成基座：核心区（结构水平投影外扩 BASE_CORE_MARGIN 格）垂直填到地形，
        // 外围再铺一圈带坡度的裙边（每向外 1 格、填充起点下降 BASE_SLOPE 格）：
        // 只在有落差处延伸，与地形平滑衔接，整体比 1:1 更窄。填充辐射岩并随机夹带辐射铀矿石，
        // 坡面加小幅抖动使边缘不那么规整。
        Block baseBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("alexscaves", "radrock"));
        if (baseBlock == null) {
            baseBlock = Blocks.STONE;
        }
        BlockState baseState = baseBlock.defaultBlockState();
        Block oreBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("alexscaves", "radrock_uranium_ore"));
        BlockState oreState = oreBlock != null ? oreBlock.defaultBlockState() : baseState;
        BoundingBox box = template.getBoundingBox(settings, placePos);

        int coreMinX = box.minX() - BASE_CORE_MARGIN;
        int coreMaxX = box.maxX() + BASE_CORE_MARGIN;
        int coreMinZ = box.minZ() - BASE_CORE_MARGIN;
        int coreMaxZ = box.maxZ() + BASE_CORE_MARGIN;
        int baseTopY = box.minY() - 1;

        // 结构本体已严格限制在生成区块内；基座按需求允许跨区块延伸
        // （外围裙边可自然铺出区块边界，代价是边界处可能被相邻区块生成覆盖，可接受）。
        for (int x = coreMinX - BASE_SKIRT_MAX; x <= coreMaxX + BASE_SKIRT_MAX; x++) {
            for (int z = coreMinZ - BASE_SKIRT_MAX; z <= coreMaxZ + BASE_SKIRT_MAX; z++) {
                // 到核心区的切比雪夫水平距离：核心内为 0，向外每 1 格 +1。
                int edgeDist = Math.max(0, Math.max(
                        Math.max(coreMinX - x, x - coreMaxX),
                        Math.max(coreMinZ - z, z - coreMaxZ)));
                // 2:1 坡度；坡面加 ±1 抖动，使外缘更自然。
                int jitter = edgeDist > 0 ? random.nextInt(3) - 1 : 0;
                BlockPos below = new BlockPos(x, baseTopY - BASE_SLOPE * edgeDist + jitter, z);
                while (below.getY() > level.getMinBuildHeight()) {
                    BlockState s = level.getBlockState(below);
                    if (!s.isAir() && !s.canBeReplaced()) {
                        break;
                    }
                    BlockState fill = random.nextFloat() < BASE_ORE_CHANCE ? oreState : baseState;
                    level.setBlock(below, fill, 3);
                    below = below.below();
                }
            }
        }

        return true;
    }

    /** 检查从 pos 起、向上 height 格是否都可替换（避免结构生成在封闭岩石内）。 */
    private static boolean isReplaceableColumn(WorldGenLevel level, BlockPos pos, int height) {
        for (int i = 0; i < height; i++) {
            BlockPos p = pos.above(i);
            if (p.getY() >= level.getMaxBuildHeight()) {
                return false;
            }
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && !s.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查从 pos 起、向上 height 格内是否存在酸液。
     * 结构本体不应浸没在酸液里：若该竖直段内有酸液，则放弃当前高度，
     * 让扫描继续向上，使结构至少落在酸液顶部上方一格。
     */
    private static boolean containsAcid(WorldGenLevel level, BlockPos pos, int height) {
        for (int i = 0; i < height; i++) {
            BlockPos p = pos.above(i);
            if (p.getY() >= level.getMaxBuildHeight()) {
                return false;
            }
            if (isAcid(level.getBlockState(p))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查 pos 下方 maxGap 格内是否存在实心方块（避免高空悬空）。仅空气与可替换方块
     * 计入悬空距离，酸液不计入（可穿透）；超过 maxGap 仍未触及实心方块则判定为无支撑。
     */
    private static boolean hasSolidBelow(WorldGenLevel level, BlockPos pos, int maxGap) {
        BlockPos below = pos.below();
        int airGap = 0;
        while (below.getY() > level.getMinBuildHeight()) {
            BlockState s = level.getBlockState(below);
            if (!s.isAir() && !s.canBeReplaced()) {
                return true; // 实心方块（岩石），有支撑
            }
            if (s.isAir() || !isAcid(s)) {
                airGap++; // 仅空气/可替换方块累计悬空距离
                if (airGap > maxGap) {
                    return false;
                }
            }
            // 酸液：不累计悬空距离，继续向下穿透
            below = below.below();
        }
        return false; // 探到世界底部仍无实心方块支撑
    }

    private static boolean isAcid(BlockState state) {
        return ACID_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static List<StructureTemplate.StructureBlockInfo> getDataMarkers(
            StructureTemplate template, BlockPos pos, Rotation rotation) {
        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(pos, settings, Blocks.STRUCTURE_BLOCK, true)) {
            if (info.nbt() != null && "DATA".equals(info.nbt().getString("mode"))) {
                result.add(info);
            }
        }
        return result;
    }
}
