package com.soof.nuclearcompat.mixin;

import com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearSirenBlockEntity;
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
 * Alex's Caves Neo（包名 com.github.alexmodguy.alexscaves）专用。
 * 逻辑与 {@code NuclearSirenBlockEntityMixin}（Alex's Caves Up）完全一致。
 */
@Mixin(NuclearSirenBlockEntity.class)
public abstract class NuclearSirenBlockEntityNeoMixin {

    @Unique
    private boolean acnc$reactorDanger = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private static void acnc$detectCriticalReactor(
            Level level, BlockPos pos, BlockState state, NuclearSirenBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        NuclearSirenBlockEntityNeoMixin self = (NuclearSirenBlockEntityNeoMixin) (Object) be;
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

    @Inject(method = "isActivated", at = @At("RETURN"), cancellable = true)
    private void acnc$activateForReactor(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (((NuclearSirenBlockEntityNeoMixin) (Object) this).acnc$reactorDanger) {
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
