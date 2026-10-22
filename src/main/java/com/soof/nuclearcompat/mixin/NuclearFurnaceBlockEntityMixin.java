package com.soof.nuclearcompat.mixin;

import com.alexscaves.server.block.blockentity.NuclearFurnaceBlockEntity;
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
 * 让 Alex's Caves Up 的核能熔炉（Nuclear Furnace）支持不同燃料棒的不同烧制量：
 * - createnuclear:uranium_rod  → 51200 裂变时间（约 512 个物品）
 * - createnuclear:thorium_rod  → 12800 裂变时间（约 128 个物品）
 *
 * 原版 {@code getMaxFissionTime()} 恒为 {@code 6400 * getSpeedReduction()}（不区分燃料棒），
 * 这里在 {@code fissionTime = getMaxFissionTime()} 赋值完成后，按燃料棒类型改写裂变时间。
 *
 * <p>另外把 {@code getSpeedReduction()} 的返回值减半，使核能熔炉烧炼速度翻倍；
 * 燃料棒时长不受影响，因为已在下面的注入中直接覆盖。</p>
 */
@Mixin(NuclearFurnaceBlockEntity.class)
public abstract class NuclearFurnaceBlockEntityMixin {

    @Shadow
    private int fissionTime;

    @Inject(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/alexscaves/server/block/blockentity/NuclearFurnaceBlockEntity;fissionTime:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private static void acnc$adjustFissionTimeByRod(Level level, BlockPos pos, BlockState state, NuclearFurnaceBlockEntity entity, CallbackInfo ci) {
        ((NuclearFurnaceBlockEntityMixin) (Object) entity).fissionTime =
                NuclearCompatUtil.fissionTimeForRod(entity.getItem(1), NuclearFurnaceBlockEntity.getSpeedReduction());
    }

    /**
     * 烧炼速度：把 {@code getSpeedReduction()} 的返回值乘上 0.5，即烧炼速度翻倍。
     *
     * <p>该值同时用于 {@code maxCookTime}（烧炼时长）和 {@code getMaxFissionTime()}（燃料棒时长），
     * 但燃料棒时长已被上面的注入直接覆盖，所以这里只需要考虑烧炼速度。</p>
     */
    @Inject(method = "getSpeedReduction", at = @At("RETURN"), cancellable = true)
    private static void acnc$speedUpCooking(CallbackInfoReturnable<Float> cir) {
        double boosted = cir.getReturnValue() * 0.5;
        cir.setReturnValue((float) Math.max(0.02, boosted));
    }
}
