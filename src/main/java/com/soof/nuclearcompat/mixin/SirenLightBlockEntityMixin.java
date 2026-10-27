package com.soof.nuclearcompat.mixin;

import com.alexscaves.server.block.SirenLightBlock;
import com.alexscaves.server.block.blockentity.SirenLightBlockEntity;
import com.soof.nuclearcompat.NuclearCompatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Alex's Caves Up（包名 {@code com.alexscaves}）专用。
 * 逻辑与 {@code SirenLightBlockEntityNeoMixin}（Alex's Caves Neo）完全一致。
 *
 * <p>让「核能警示灯」像核能警报器一样检测周围核危险环境（原子核动反应堆失控、
 * Alex's Caves 核弹、撼地斯拉吐息、核爆怪、临界核熔炉），并在有危险时点亮/旋转，
 * 无危险时恢复为由红石输入控制。</p>
 */
@Mixin(SirenLightBlockEntity.class)
public abstract class SirenLightBlockEntityMixin {

    /** 当前是否处于「核危险环境」点亮状态（服务端计算，驱动 POWERED 方块状态）。 */
    @Unique
    private boolean acnc$hazard = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private static void acnc$detectNuclearHazard(
            Level level, BlockPos pos, BlockState state, SirenLightBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        SirenLightBlockEntityMixin self = (SirenLightBlockEntityMixin) (Object) be;
        // 性能优化：
        // 1) 附近 64 格内没有玩家时完全不扫描（警示灯只是视觉/听觉反馈）；
        // 2) 用坐标打散相位，避免大量警示灯在同一 tick 集中扫描（原先都命中 getGameTime()%40==0）。
        if (level.hasNearbyAlivePlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 64.0D)
                && Math.floorMod(level.getGameTime() + pos.asLong(), 40L) == 0L) {
            self.acnc$hazard = NuclearCompatUtil.isNuclearHazardNearby(level, pos);
        }
        // 危险时点亮；无危险时回退为红石输入控制（与警示灯原本的逻辑一致）。
        boolean want = self.acnc$hazard || level.hasNeighborSignal(pos);
        if (state.getValue(SirenLightBlock.POWERED) != want) {
            level.setBlock(pos, state.setValue(SirenLightBlock.POWERED, want), 3);
        }
    }
}
