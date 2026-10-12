package com.soof.nuclearcompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.nuclearteam.createnuclear.content.multiblock.alarm.ReactorAlarm;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 让原子核动的「反应堆警报器」对外输出红石信号。
 *
 * <p>警报器本体机制：{@code ReactorAlarmCoordinator} 在反应堆热量达到 {@code DANGER}
 * （失控预警）时，会把反应堆结构上所有报警方块的 {@code POWERED} 置为 {@code true}，
 * 随后 {@code ReactorAlarmEntity.tickAudio} 开始循环响铃。因此 {@code POWERED == true}
 * 就等价于「失控预警中/正在响铃」。这里把该状态映射为强度 15 的红石输出，
 * 便于用原版红石或机械动力自动化联动（自动注入冷却、拉闸、报警灯、紧急停机等）。</p>
 *
 * <p>输出在协调器 {@code setBlock(..., 3)} 时随邻居更新自然传播，无需额外轮询。</p>
 */
@Mixin(ReactorAlarm.class)
public abstract class ReactorAlarmMixin extends Block {

    private ReactorAlarmMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return acnc$isRinging(state) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return acnc$isRinging(state) ? 15 : 0;
    }

    private static boolean acnc$isRinging(BlockState state) {
        return state.hasProperty(ReactorAlarm.POWERED) && state.getValue(ReactorAlarm.POWERED);
    }
}
