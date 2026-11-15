package com.soof.nuclearcompat.mixin;

import com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearSirenBlockEntity;
import com.soof.nuclearcompat.SirenSupport;
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
        NuclearSirenBlockEntityNeoMixin self = (NuclearSirenBlockEntityNeoMixin) (Object) be;
        // 扫描条件与侦测本体在 SirenSupport，与 Up 版共用。
        if (!SirenSupport.shouldScanForReactor(level, pos, self.acnc$reactorDanger, be.age)) {
            return;
        }
        boolean danger = SirenSupport.detectReactorDanger(level, pos);
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
