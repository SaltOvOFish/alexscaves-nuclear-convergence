package com.soof.nuclearcompat.mixin;

import com.alexscaves.server.block.blockentity.NuclearSirenBlockEntity;
import com.soof.nuclearcompat.NuclearCompatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Alex's Caves Up（包名 com.alexscaves）专用：让核能警报器在原子核动反应堆
 * 进入「失控爆炸倒计时」（热量达到 DANGER）时提前响起。
 *
 * <p>为什么不能直接复用 AC 的 {@code nearestMeltdownFurnace} 字段：AC 自己的
 * {@code tick()} 每次都会调用 {@code isTrackedFurnaceCritical()}，而 CN 的反应堆
 * 控制器并不是 {@code NuclearFurnaceBlockEntity}，恒返回 false，于是 AC 会在下一
 * tick 把 {@code nearestMeltdownFurnace} 清空，导致警报永远不响。</p>
 *
 * <p>因此这里改用独立的 {@code @Unique} 布尔标记，通过 NBT 同步到客户端，
 * 并覆盖 {@code isActivated} 让警报器在标记为真时激活。</p>
 */
@Mixin(NuclearSirenBlockEntity.class)
public abstract class NuclearSirenBlockEntityMixin {

    /** 附近是否存在进入失控倒计时的 CN 反应堆（同步到客户端用）。 */
    @Unique
    private boolean acnc$reactorDanger = false;

    /**
     * 每 20 tick 在服务端检测一次附近是否有进入倒计时的 CN 反应堆。
     * 状态变化时通过 {@code sendBlockUpdated} 同步到客户端。
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private static void acnc$detectCriticalReactor(
            Level level, BlockPos pos, BlockState state, NuclearSirenBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        NuclearSirenBlockEntityMixin self = (NuclearSirenBlockEntityMixin) (Object) be;
        // 性能优化：64 格内没有玩家、且当前未在报警时直接跳过扫描；
        // 同时用坐标打散相位，避免多台警报器在同一 tick 集中扫描。
        if (!self.acnc$reactorDanger
                && !level.hasNearbyAlivePlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 64.0D)) {
            return;
        }
        if (Math.floorMod(be.age + pos.asLong(), 20L) != 0L) {
            return;
        }
        boolean danger = NuclearCompatUtil.findMeltdownReactor(level, pos) != null;
        if (self.acnc$reactorDanger != danger) {
            self.acnc$reactorDanger = danger;
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    /** 覆盖激活判定：附近有进入倒计时的 CN 反应堆时也激活警报器。 */
    @Inject(method = "isActivated", at = @At("RETURN"), cancellable = true)
    private void acnc$activateForReactor(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (((NuclearSirenBlockEntityMixin) (Object) this).acnc$reactorDanger) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void acnc$saveReactorDanger(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.putBoolean("AcncReactorDanger", this.acnc$reactorDanger);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void acnc$loadReactorDanger(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.acnc$reactorDanger = tag.getBoolean("AcncReactorDanger");
    }
}
