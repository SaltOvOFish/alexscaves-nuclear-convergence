package com.soof.nuclearcompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nuclearteam.createnuclear.content.multiblock.IHeat;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跨 mixin 共享的核能相关工具方法。
 *
 * <p>Alex's Caves 的 Up / Neo 两个移植版包名不同，对应的 mixin 必须各写一份，
 * 但其中的核心判断逻辑完全相同。这里把不依赖 Alex's Caves 具体类的纯逻辑集中
 * 到一处，避免双份维护、改一处漏一处。</p>
 */
public final class NuclearCompatUtil {

    private static final ResourceLocation REACTOR_CONTROLLER_ID =
            ResourceLocation.fromNamespaceAndPath("createnuclear", "reactor_controller");
    private static final ResourceLocation THORIUM_ROD_ID =
            ResourceLocation.fromNamespaceAndPath("createnuclear", "thorium_rod");

    /** 铀棒（默认）裂变时间系数，约 256 个物品。 */
    private static final float URANIUM_FISSION_FACTOR = 51200.0f;
    /** 钍棒裂变时间系数，约 64 个物品。 */
    private static final float THORIUM_FISSION_FACTOR = 12800.0f;

    private NuclearCompatUtil() {
    }

    /**
     * 在中心点附近（默认 32 格）查找热量达到 DANGER、即已进入失控倒计时的
     * 原子核动反应堆控制器，返回其坐标；未找到返回 null。
     *
     * <p>与 CN 自身的 {@code ReactorAlarmCoordinator#computeDanger} 判定完全等价：
     * {@code IHeat.HeatLevel.of(heat, size) == DANGER}。通过反射读取
     * {@code getConfiguredPatternHeat()} / {@code getMultiblockSize()}，
     * 避免对 CN 反应堆控制器具体类的编译期依赖。</p>
     */
    public static BlockPos findMeltdownReactor(Level level, BlockPos center) {
        return findMeltdownReactor(level, center, 32);
    }

    public static BlockPos findMeltdownReactor(Level level, BlockPos center, int radius) {
        // 反应堆控制器是方块实体：改为只遍历「半径内已加载区块」的方块实体表，
        // 只需访问约 (2r/16+1)^2 个区块（半径 32 约 25 个），
        // 远优于原实现逐方块遍历 65^3 ≈ 27.5 万个坐标。
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        Block controller = BuiltInRegistries.BLOCK.get(REACTOR_CONTROLLER_ID);
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minY = center.getY() - radius;
        int maxY = center.getY() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        for (int cx = minX >> 4; cx <= (maxX >> 4); cx++) {
            for (int cz = minZ >> 4; cz <= (maxZ >> 4); cz++) {
                LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (p.getX() < minX || p.getX() > maxX
                            || p.getY() < minY || p.getY() > maxY
                            || p.getZ() < minZ || p.getZ() > maxZ
                            || !be.getBlockState().is(controller)) {
                        continue;
                    }
                    try {
                        int heat = (Integer) be.getClass().getMethod("getConfiguredPatternHeat").invoke(be);
                        int size = (Integer) be.getClass().getMethod("getMultiblockSize").invoke(be);
                        if (!IHeat.HeatLevel.isNotDanger(heat, size)) {
                            return p.immutable();
                        }
                    } catch (Exception ignored) {
                        // 反射失败时忽略（类结构差异或加载时序）。
                    }
                }
            }
        }
        return null;
    }

    /**
     * 根据燃料棒类型返回裂变时间：钍棒 6400、其余（铀棒等）25600，再乘以核能熔炉的
     * 速度系数（{@code getSpeedReduction()}）。
     */
    public static int fissionTimeForRod(ItemStack rod, float speedReduction) {
        Item thoriumRod = BuiltInRegistries.ITEM.get(THORIUM_ROD_ID);
        float factor = (thoriumRod != null && rod.is(thoriumRod))
                ? THORIUM_FISSION_FACTOR
                : URANIUM_FISSION_FACTOR;
        return (int) Math.ceil(factor * speedReduction);
    }

    // ==================== 核危险环境侦测（供核能警报器 / 核能警示灯等使用）====================

    /** Alex's Caves 核弹实体的注册名。 */
    private static final ResourceLocation NUCLEAR_BOMB_ENTITY_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "nuclear_bomb");
    /** Alex's Caves 核熔炉的 POI 类型（用于找到临界核熔炉）。 */
    private static final ResourceKey<PoiType> NUCLEAR_FURNACE_POI = ResourceKey.create(
            Registries.POINT_OF_INTEREST_TYPE,
            ResourceLocation.fromNamespaceAndPath("alexscaves", "nuclear_furnace"));
    /** Alex's Caves「会让警报器鸣响」的实体接口简单名（Up / Neo 包名不同，用简单名 + 反射匹配）。 */
    private static final String ACTIVATES_SIRENS_INTERFACE = "ActivatesSirens";

    /** 危险源实体侦测半径，与 Alex's Caves 核能警报器一致（核弹 / 撼地斯拉在 256 格内触发警报）。 */
    private static final int HAZARD_ENTITY_RADIUS = 256;
    /** 临界核熔炉 POI 侦测半径，与 Alex's Caves 核能警报器一致。 */
    private static final int NUCLEAR_FURNACE_RADIUS = 128;

    /**
     * 中心点附近是否存在「核危险环境」。收录：
     * <ul>
     *     <li>原子核动反应堆失控（热量达到 DANGER）</li>
     *     <li>Alex's Caves 核弹（nuclear_bomb 实体）</li>
     *     <li>撼地斯拉吐息、核爆怪 Nucleeper（实现 {@code ActivatesSirens} 且未要求停止鸣响）</li>
     *     <li>Alex's Caves 临界核熔炉（自身核熔毁）</li>
     * </ul>
     * 判定口径与 Alex's Caves 核能警报器保持一致。
     */
    public static boolean isNuclearHazardNearby(Level level, BlockPos center) {
        if (level.isClientSide) {
            return false;
        }
        // 原子核动反应堆失控
        if (findMeltdownReactor(level, center) != null) {
            return true;
        }
        // Alex's Caves 核危险源实体
        double r = HAZARD_ENTITY_RADIUS;
        Vec3 v = Vec3.atCenterOf(center);
        AABB box = new AABB(v.x - r, v.y - r, v.z - r, v.x + r, v.y + r, v.z + r);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box)) {
            if (isSirenDangerEntity(e)) {
                return true;
            }
        }
        // Alex's Caves 临界核熔炉
        return hasCriticalNuclearFurnaceNearby(level, center);
    }

    /** 每种实体类型的「危险源」判定规则缓存，避免每次扫描都对每个实体做反射。 */
    private static final Map<EntityType<?>, SirenRule> SIREN_RULES = new ConcurrentHashMap<>();

    /** 规则：核弹类恒为危险；实现 {@code ActivatesSirens} 的类型则缓存其 shouldStopBlaringSirens 方法。 */
    private record SirenRule(boolean alwaysDanger, Method shouldStopMethod) {
    }

    /** 让 Alex's Caves 核能警报器鸣响的实体：核弹、撼地斯拉吐息、核爆怪。 */
    private static boolean isSirenDangerEntity(Entity entity) {
        SirenRule rule = SIREN_RULES.computeIfAbsent(entity.getType(), NuclearCompatUtil::buildSirenRule);
        if (rule.alwaysDanger()) {
            return true;
        }
        Method method = rule.shouldStopMethod();
        if (method == null) {
            return false;
        }
        try {
            Object r = method.invoke(entity);
            return r instanceof Boolean b && !b;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** 构建（并缓存）某一实体类型的危险源判定规则，仅首次扫描该类型时做反射。 */
    private static SirenRule buildSirenRule(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (NUCLEAR_BOMB_ENTITY_ID.equals(id)) {
            return new SirenRule(true, null);
        }
        // ActivatesSirens 接口在 Up / Neo 两个移植版中包名不同，用简单名 + 反射匹配，避免编译期依赖。
        Class<?> c = type.getBaseClass();
        while (c != null) {
            for (Class<?> itf : c.getInterfaces()) {
                if (ACTIVATES_SIRENS_INTERFACE.equals(itf.getSimpleName())) {
                    try {
                        return new SirenRule(false, itf.getMethod("shouldStopBlaringSirens"));
                    } catch (Exception ignored) {
                        return new SirenRule(false, null);
                    }
                }
            }
            c = c.getSuperclass();
        }
        return new SirenRule(false, null);
    }

    /** 中心点附近是否存在 Alex's Caves 的临界核熔炉（通过 POI 查找）。 */
    private static boolean hasCriticalNuclearFurnaceNearby(Level level, BlockPos center) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        try {
            PoiManager poi = serverLevel.getPoiManager();
            return poi.findAll(
                    holder -> holder.unwrapKey().map(NUCLEAR_FURNACE_POI::equals).orElse(false),
                    pos -> isCriticalFurnace(level, pos),
                    center, NUCLEAR_FURNACE_RADIUS, PoiManager.Occupancy.ANY)
                    .findAny().isPresent();
        } catch (Exception ignored) {
            // Alex's Caves 缺失或 POI 未注册时静默忽略。
            return false;
        }
    }

    /** 反射读取核熔炉方块实体的临界值，判断是否已达临界（criticality >= 2）。 */
    private static boolean isCriticalFurnace(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return false;
        }
        try {
            Object r = be.getClass().getMethod("getCriticality").invoke(be);
            return r instanceof Integer i && i >= 2;
        } catch (Exception ignored) {
            return false;
        }
    }
}
