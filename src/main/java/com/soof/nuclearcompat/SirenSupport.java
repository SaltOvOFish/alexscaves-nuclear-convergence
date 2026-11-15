package com.soof.nuclearcompat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 核能警报器 / 核能警示灯混入的共享侦测逻辑。
 *
 * <p>Up 与 Neo 两套混入的 {@code @Mixin} 目标包名不同，必须各留一份；
 * 但每 tick 的「要不要扫描」「扫什么」「点亮条件」完全一致，抽到这里避免双份维护。</p>
 *
 * <p>行为等价：所有判断的顺序与短路方式都与原实现逐行一致。</p>
 */
public final class SirenSupport {

    /** 附近没有玩家时的扫描跳过半径（格）。警示灯/警报器只是视听反馈，远处无人时无需计算。 */
    private static final double NEARBY_PLAYER_RADIUS = 64.0D;

    /** 警报器扫描周期（tick）。 */
    private static final long SIREN_SCAN_PERIOD = 20L;

    /** 警示灯扫描周期（tick）。 */
    private static final long LIGHT_SCAN_PERIOD = 40L;

    private SirenSupport() {
    }

    /**
     * 核能警报器：本 tick 是否应扫描附近的反核动反应堆。
     *
     * <p>两个早期退出条件：①当前未在报警且 64 格内没有玩家；②用「方块实体的 age + 坐标」
     * 打散相位，避免多台警报器在同一 tick 集中扫描。</p>
     *
     * @param beAge 方块实体的 {@code age} 字段（警报器原版就有这个公开字段）
     */
    public static boolean shouldScanForReactor(Level level, BlockPos pos, boolean currentlyDanger, int beAge) {
        if (level.isClientSide) {
            return false;
        }
        if (!currentlyDanger
                && !level.hasNearbyAlivePlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, NEARBY_PLAYER_RADIUS)) {
            return false;
        }
        return Math.floorMod(beAge + pos.asLong(), SIREN_SCAN_PERIOD) == 0L;
    }

    /** 核能警报器：附近是否存在进入失控倒计时的原子核动反应堆。 */
    public static boolean detectReactorDanger(Level level, BlockPos pos) {
        return NuclearCompatUtil.findMeltdownReactor(level, pos) != null;
    }

    /**
     * 核能警示灯：本 tick 是否应重新侦测核危险环境。
     *
     * <p>两个条件：①64 格内有玩家（警示灯只是视听反馈）；②用「游戏时间 + 坐标」打散相位。</p>
     */
    public static boolean shouldScanHazard(Level level, BlockPos pos) {
        return level.hasNearbyAlivePlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, NEARBY_PLAYER_RADIUS)
                && Math.floorMod(level.getGameTime() + pos.asLong(), LIGHT_SCAN_PERIOD) == 0L;
    }

    /** 核能警示灯：应点亮的状态（危险环境，或由红石信号驱动）。 */
    public static boolean wantPowered(boolean hazard, Level level, BlockPos pos) {
        return hazard || level.hasNeighborSignal(pos);
    }
}
