package com.soof.nuclearcompat;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 让机械动力（Create）的机械臂把渊海祭坛识别为可交互目标（类似置物台）。
 *
 * <p>机械臂通过「交互点类型」注册表识别可交互方块；渊海祭坛原本不在其中。
 * 注册此类型后，机械臂即可从上方像操作置物台一样拿取/放置渊海祭坛里的物品。</p>
 *
 * <p>另外在这里挂了一个钩子：机械臂把<b>珍珠</b>放进祭坛时，给最近的玩家授予
 * 隐藏进度「爱，深潜，与机器人」（{@code love_deep_and_robots}）。
 * 之所以要在这里做，是因为机械臂插入不经过玩家，原版的
 * {@code item_used_on_block} 之类的触发器根本不会触发。</p>
 */
public final class AbyssalAltarInteractionPointType extends ArmInteractionPointType {

    private static final ResourceLocation ABYSSAL_ALTAR_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "abyssal_altar");
    private static final ResourceLocation PEARL_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "pearl");

    /**
     * 解析结果缓存（可为 null 表示 Alex's Caves 缺失）。
     *
     * <p><b>不能在静态初始化时解析</b>：本类在 {@code RegisterEvent} 里被实例化，那时方块注册表
     * 可能还没填充完毕，{@code BuiltInRegistries.BLOCK.get(...)} 会返回 {@code minecraft:air}
     * 并被永久缓存下来，导致机械臂永远认不出祭坛、珍珠进度永不触发。
     * 因此推迟到首次实际使用时（一定是注册表冻结后的游戏运行期）再解析。</p>
     */
    private static Block abyssalAltar;
    private static Item pearl;
    private static boolean resolved = false;

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        Block block = BuiltInRegistries.BLOCK.get(ABYSSAL_ALTAR_ID);
        // BuiltInRegistries.BLOCK 是 DefaultedRegistry：缺失时返回 minecraft:air 而非 null。
        abyssalAltar = ABYSSAL_ALTAR_ID.equals(BuiltInRegistries.BLOCK.getKey(block)) ? block : null;
        // BuiltInRegistries.ITEM 同理，缺失时返回 minecraft:air。
        Item item = BuiltInRegistries.ITEM.get(PEARL_ID);
        pearl = PEARL_ID.equals(BuiltInRegistries.ITEM.getKey(item)) ? item : null;
    }

    private static final ResourceLocation LOVE_DEEP_AND_ROBOTS =
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "love_deep_and_robots");

    /** 进度 JSON 里的条件名。 */
    private static final String CRITERION = "arm_offering";

    /** 授予半径（格）：机械臂本身只够得到 3 格，取 16 格足够宽松。 */
    private static final double AWARD_RADIUS = 16.0D;

    @Override
    public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
        resolve();
        return abyssalAltar != null && state.getBlock() == abyssalAltar;
    }

    @Override
    public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
        return new AltarPoint(this, level, pos, state);
    }

    /** 在插入路径上检测「机械臂放珍珠」。 */
    private static final class AltarPoint extends ArmInteractionPoint {

        private AltarPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
            super(type, level, pos, state);
        }

        @Override
        public ItemStack insert(ArmBlockEntity arm, ItemStack stack, boolean simulate) {
            int before = stack.getCount();
            ItemStack leftover = super.insert(arm, stack, simulate);

            // simulate 为 true 是机械臂的预演（真插入之前先试一次），不能据此发进度；
            // leftover 变少才说明真的放进去了（祭坛每格上限 1，已占用时不会成功）。
            if (!simulate && leftover.getCount() < before && isOfferingPearl(stack)) {
                awardNearestPlayer(this.level, this.pos);
            }
            return leftover;
        }
    }

    private static boolean isOfferingPearl(ItemStack stack) {
        resolve();
        return pearl != null && stack.is(pearl);
    }

    /**
     * 授予最近的玩家。机械臂不保存主人信息，因此只能就近选择：
     * 取 {@link #AWARD_RADIUS} 格内距离最近的在线玩家。
     */
    private static void awardNearestPlayer(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        ServerPlayer nearest = null;
        double bestDistanceSqr = AWARD_RADIUS * AWARD_RADIUS;
        for (ServerPlayer player : serverLevel.players()) {
            double distanceSqr = player.distanceToSqr(x, y, z);
            if (distanceSqr <= bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                nearest = player;
            }
        }
        if (nearest == null) {
            return;
        }

        AdvancementHolder holder = serverLevel.getServer().getAdvancements().get(LOVE_DEEP_AND_ROBOTS);
        if (holder != null) {
            nearest.getAdvancements().award(holder, CRITERION);
        }
    }
}
