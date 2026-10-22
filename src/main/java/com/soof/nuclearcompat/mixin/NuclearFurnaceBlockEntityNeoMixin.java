package com.soof.nuclearcompat.mixin;

import com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearFurnaceBlockEntity;
import com.soof.nuclearcompat.NuclearCompatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Alex's Caves Neo（包名 com.github.alexmodguy.alexscaves）专用。
 * 逻辑与 {@code NuclearFurnaceBlockEntityMixin}（Alex's Caves Up）完全一致。
 */
@Mixin(NuclearFurnaceBlockEntity.class)
public abstract class NuclearFurnaceBlockEntityNeoMixin {

    @Shadow
    private int fissionTime;

    @Inject(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/github/alexmodguy/alexscaves/server/block/blockentity/NuclearFurnaceBlockEntity;fissionTime:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private static void acnc$adjustFissionTimeByRod(Level level, BlockPos pos, BlockState state, NuclearFurnaceBlockEntity entity, CallbackInfo ci) {
        ((NuclearFurnaceBlockEntityNeoMixin) (Object) entity).fissionTime =
                NuclearCompatUtil.fissionTimeForRod(entity.getItem(1), NuclearFurnaceBlockEntity.getSpeedReduction());
    }

    /** 烧炼速度翻倍（返回值 × 0.5）。 */
    @Inject(method = "getSpeedReduction", at = @At("RETURN"), cancellable = true)
    private static void acnc$speedUpCooking(CallbackInfoReturnable<Float> cir) {
        double boosted = cir.getReturnValue() * 0.5;
        cir.setReturnValue((float) Math.max(0.02, boosted));
    }
}
