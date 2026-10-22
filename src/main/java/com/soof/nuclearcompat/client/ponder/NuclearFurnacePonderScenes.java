package com.soof.nuclearcompat.client.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 核能熔炉的思索场景。
 *
 * <p>P1「组装」：底层 2×2 → 上层 2×2 → 自动成型（只显示熔炉）。</p>
 *
 * <p>P2「输入与输出」：一个场景里用 4 个时间点串起来：
 * 燃料 → 原料 → 金属桶，每段之间镜头顺时针转 90°；
 * 最后再转 180° 回初始视角，指向熔炉正面右下角格说明下方输出，然后三条传送带一起淡出。</p>
 *
 * <p>P3「临界与失控」：7×7 底板，按 4 / 6 / 8 / 10 份废料演示发红、起火、发黄与核爆。</p>
 *
 * <p>注意：Ponder 的基础世界段是空的，NBT 里的方块必须靠 {@code showSection} 才会渲染；
 * 只有 y=0 的底板会由 {@code showBasePlate()} 显示。</p>
 */
public final class NuclearFurnacePonderScenes {

    /** 每次转场镜头旋转的角度。负值 = 画面里的场景顺时针转（方向反了把符号取反即可）。 */
    private static final float CAMERA_STEP = -90f;

    /** 传送带转速（RPM）。西、南两条实测物品与带面方向相反，取负；东侧正常。 */
    private static final float BELT_RPM = 16f;
    private static final float WEST_BELT_RPM = -BELT_RPM;
    private static final float SOUTH_BELT_RPM = -BELT_RPM;
    private static final float EAST_BELT_RPM = BELT_RPM;

    private NuclearFurnacePonderScenes() {
    }

    // ---------------------------------------------------------------------
    // P1 组装
    // ---------------------------------------------------------------------

    /**
     * P1 组装：底层 2×2 → 上层 2×2 → 自动成型。
     *
     * <p>{@code basic.nbt} 里存的是**已成型**的熔炉（本体在 (2,1,1)，占 x2..3 / y1..2 / z1..2），
     * 动画用 {@code setBlock} 临时摆部件，最后 {@code restoreBlocks} 把带方块实体的本体还原回来。
     * 这一页只显示熔炉区域，三组传送带保持隐藏。</p>
     */
    public static void assembly(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("nuclear_furnace_assembly", "核能熔炉：组装");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        // Ponder 只渲染 WorldSectionElement 覆盖的方块：
        // 先把熔炉区域清空，再对这块区域 showSection，之后 setBlock 放下的部件才会被重绘出来。
        Selection furnace = util.select().fromTo(2, 1, 1, 3, 2, 2);
        scene.world().setBlocks(furnace, Blocks.AIR.defaultBlockState(), false);
        scene.idle(1);
        scene.world().showSection(furnace, Direction.UP);
        scene.idle(5);

        BlockState part = componentState(false);

        scene.overlay().showText(24)
                .text("先用 4 个核能熔炉部件铺出 2×2 的第一层")
                .pointAt(util.vector().of(3.0, 1.0, 2.0))
                .placeNearTarget()
                .attachKeyFrame();
        for (int x = 2; x <= 3; x++) {
            for (int z = 1; z <= 2; z++) {
                scene.world().setBlock(new BlockPos(x, 1, z), part, false);
                scene.idle(8);
            }
        }
        scene.idle(10);

        scene.overlay().showText(24)
                .text("再盖上第二层 2×2")
                .pointAt(util.vector().of(3.0, 2.6, 2.0))
                .placeNearTarget()
                .attachKeyFrame();
        for (int x = 2; x <= 3; x++) {
            for (int z = 1; z <= 2; z++) {
                scene.world().setBlock(new BlockPos(x, 2, z), part, false);
                scene.idle(8);
            }
        }
        scene.idle(10);

        // 成型：临时部件换成带方块实体的真实熔炉（restoreBlocks 会一并还原 BE 并重绘 section）
        scene.world().restoreBlocks(furnace);
        scene.idle(20);

        scene.overlay().showText(24)
                .text("8 个部件到位后会自动组装成一台核能熔炉")
                .pointAt(util.vector().of(3.0, 1.5, 2.0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(20);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // P2 输入与输出（4 个时间点）
    // ---------------------------------------------------------------------

    /**
     * P2：燃料 → 原料 → 金属桶（3 个时间点），结尾把传送带平滑收掉。
     *
     * <p>三个时间点之间镜头各顺时针转 90°，结构按时间点逐个 {@code showSection}。
     * 三块选区互不重叠，避免同一区域被两个 section 重复渲染。</p>
     *
     * <p>第 4 个时间点（下方输出）转回初始视角后用文本指向熔炉正面右下角格。</p>
     */
    public static void io(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("nuclear_furnace_io", "核能熔炉：输入与输出");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(8);

        Selection furnace = util.select().fromTo(2, 1, 1, 3, 2, 2);
        Selection westBelt = util.select().fromTo(0, 1, 2, 1, 2, 2);   // 右侧：燃料
        Selection southBelt = util.select().fromTo(3, 1, 3, 3, 2, 6);  // 背面：原料
        Selection eastBelt = util.select().fromTo(4, 1, 1, 6, 2, 1);   // 左侧：金属桶

        scene.world().showSection(furnace, Direction.DOWN);
        scene.idle(12);

        // 时间点 0：各面功能总览
        scene.overlay().showText(20)
                .text("核能熔炉的输入与输出由不同面分别进行")
                .pointAt(util.vector().centerOf(2, 2, 1))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);

        // 时间点 1：燃料（右侧 / 西面）
        scene.world().showSection(westBelt, Direction.DOWN);
        scene.world().setKineticSpeed(westBelt, WEST_BELT_RPM);
        scene.idle(5);
        scene.overlay().showText(30)
                .text("铀棒或钍棒从右侧输入，作为熔炉的燃料")
                .pointAt(util.vector().centerOf(1, 2, 2))
                .placeNearTarget()
                .attachKeyFrame();
        runBelt(scene, new BlockPos(0, 1, 2), new BlockPos(1, 1, 2), new BlockPos(0, 1, 2),
                new BlockPos(1, 2, 2), "createnuclear", "uranium_rod", 10, 40);
        scene.idle(10);

        // 时间点 2：原料（背面 / 南面）
        scene.rotateCameraY(CAMERA_STEP);
        scene.idle(15);
        scene.world().showSection(southBelt, Direction.DOWN);
        scene.world().setKineticSpeed(southBelt, SOUTH_BELT_RPM);
        scene.idle(10);
        scene.overlay().showText(40)
                .text("原材料从背面输入，在熔炉内完成烧炼")
                .pointAt(util.vector().centerOf(3, 2, 3))
                .placeNearTarget()
                .attachKeyFrame();
        runBelt(scene, new BlockPos(3, 1, 6), new BlockPos(3, 1, 3), new BlockPos(3, 1, 4),
                new BlockPos(3, 2, 3), "minecraft", "raw_iron", 18, 60);
        scene.idle(10);

        // 时间点 3：金属桶（左侧 / 东面）
        scene.rotateCameraY(CAMERA_STEP);
        scene.idle(15);
        scene.world().showSection(eastBelt, Direction.DOWN);
        scene.world().setKineticSpeed(eastBelt, EAST_BELT_RPM);
        scene.idle(5);
        scene.overlay().showText(25)
                .text("金属桶从左侧输入，用来盛装废料以避免熔炉失控")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(4, 2, 1))
                .placeNearTarget()
                .attachKeyFrame();
        runBelt(scene, new BlockPos(6, 1, 1), new BlockPos(4, 1, 1), new BlockPos(5, 1, 1),
                new BlockPos(4, 2, 1), "alexscaves", "metal_barrel", 14, 60);
        scene.idle(10);

        // 再转 180° 回到初始视角
        scene.rotateCameraY(-180f);
        scene.idle(35);

        // 时间点 4：输出（指向熔炉正面右下角格）
        scene.overlay().showText(20)
                .text("废料桶与烧炼成品则通过熔炉底部输出")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(2, 0, 1))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);

        // 三条传送带一起平滑淡出
        scene.world().hideSection(westBelt, Direction.DOWN);
        scene.world().hideSection(southBelt, Direction.DOWN);
        scene.world().hideSection(eastBelt, Direction.DOWN);
        scene.idle(30);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // P3 临界与失控（占位版）
    // ---------------------------------------------------------------------

    /**
     * P3 临界与失控。
     *
     * <p>结构 {@code critical.nbt}（7×5×7）：底板 7×7，熔炉 2×2×2 位于
     * x 2..3 / y 1..2 / z 1..2（本体 (2,1,1)，facing=north）。</p>
     *
     * <p>废料量直接改方块实体的 {@code Waste}（单位 mB，100 = 1 份），
     * 颜色由 BER 按 350 / 600 / 800 三个阈值自己算；火、烟与爆炸用代码摆。</p>
     */
    public static void critical(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("nuclear_furnace_critical", "核能熔炉：失控熔毁");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        // 让底板之上进入渲染（火与粒子都靠这块 section）
        scene.world().showSection(util.select().fromTo(0, 1, 0, 6, 4, 6), Direction.DOWN);
        scene.idle(15);

        Selection furnace = util.select().fromTo(2, 1, 1, 3, 2, 2);
        Vec3 center = util.vector().of(3.0, 1.5, 2.0);

        // 阶段 1：规则
        scene.overlay().showText(20)
                .text("核能熔炉最多可安全存储 9 份核废料，超出则会引发核爆")
                .pointAt(center)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);

        // 阶段 2：4 份，底部发红
        setWaste(scene, furnace, 400);
        scene.overlay().showText(25)
                .text("4 份废料：核能熔炉底部开始泛红")
                .colored(PonderPalette.RED)
                .pointAt(center)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        // 阶段 3：6 份，整体发红 + 黑烟 + 4×4 起火
        setWaste(scene, furnace, 600);
        placeFire(scene, 1, 0, 4, 3, 2, 1, 3, 2);
        emit(scene, ParticleTypes.LARGE_SMOKE, util.vector().of(3.0, 3.2, 2.0));
        scene.overlay().showText(25)
                .text("6 份废料：核能熔炉失控，周边开始起火")
                .colored(PonderPalette.RED)
                .pointAt(center)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        // 阶段 4：8 份，整体发黄 + 12×12 大火
        setWaste(scene, furnace, 800);
        placeFire(scene, 0, 0, 6, 6, 2, 1, 3, 2);
        scene.overlay().showText(25)
                .text("8 份废料：核能熔炉濒临熔毁，四周陷入火海")
                .colored(PonderPalette.RED)
                .pointAt(center)
                .placeNearTarget()
                .attachKeyFrame();
        // 黄烟：从这一阶段开始一直冒到下一阶段爆炸前
        emitYellowSmoke(scene, util.vector().of(3.0, 3.3, 2.0), 12, 5);
        scene.idle(40);

        // 阶段 5：10 份核爆，整个结构被摧毁，画面归零
        scene.overlay().showText(20)
                .text("10 份废料：核能，轻而易——坏了坏了！")
                .colored(PonderPalette.RED)
                .pointAt(center)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(10);

        // 黄烟继续，直到爆炸
        emitYellowSmoke(scene, util.vector().of(3.0, 3.3, 2.0), 6, 5);
        scene.idle(6);

        // 闪光 + 中心爆炸，然后一圈圈向外扩散的爆炸波
        Vec3 blast = util.vector().of(3.0, 2.0, 2.0);
        emit(scene, ParticleTypes.FLASH, blast);
        emit(scene, ParticleTypes.EXPLOSION_EMITTER, blast);
        scene.idle(3);
        for (int ring = 1; ring <= 3; ring++) {
            double radius = ring * 2.2;
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                emitSmall(scene, ParticleTypes.EXPLOSION,
                        blast.add(Math.cos(angle) * radius, 0.6, Math.sin(angle) * radius), 2);
            }
            scene.idle(3);
        }
        scene.idle(8);

        // 所有内容（含底板）清空
        scene.world().setBlocks(util.select().fromTo(0, 0, 0, 6, 4, 6),
                Blocks.AIR.defaultBlockState(), false);
        scene.idle(50);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // 辅助
    // ---------------------------------------------------------------------

    /** 在传送带的指定格放一个会随带子移动的物品（物品不存在时跳过）。 */
    private static void spawnBeltItem(CreateSceneBuilder scene, int x, int y, int z,
                                      String namespace, String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (item == Items.AIR) {
            return;
        }
        scene.world().createItemOnBelt(new BlockPos(x, y, z), Direction.UP, new ItemStack(item));
    }

    /**
     * 在带子起点一次性放 3 个物品（它们会自己排成一串），等它们走到末端后，
     * 让皮带漏斗持续张开并在一个时间窗口内反复清理带尾物品，保证不遗漏。
     *
     * @param travelTicks 物品从 start 走到末端的预计 tick 数
     * @param behind      末端靠近起点的那一格（后半段兜底清理）
     */
    private static void runBelt(CreateSceneBuilder scene, BlockPos start, BlockPos end, BlockPos behind,
                                BlockPos funnel, String namespace, String path, int travelTicks, int totalTicks) {
        int count = 3;
        for (int i = 0; i < count; i++) {
            spawnBeltItem(scene, start.getX(), start.getY(), start.getZ(), namespace, path);
        }
        scene.idle(travelTicks);

        // 翻板张开后直到整段结束都在反复清理带尾；总时长由 totalTicks 决定
        setFunnelShape(scene, funnel, "pulling");
        int cleanTicks = Math.max(10, totalTicks - travelTicks);
        int steps = 10;
        int gap = Math.max(1, cleanTicks / steps);
        int used = 0;
        for (int i = 0; i < steps; i++) {
            scene.world().removeItemsFromBelt(end);
            if (i >= steps / 2) {
                scene.world().removeItemsFromBelt(behind);
            }
            scene.idle(gap);
            used += gap;
        }
        if (used < cleanTicks) {
            scene.idle(cleanTicks - used);
        }
        setFunnelShape(scene, funnel, "retracted");
    }

    /** 按名字设置方块状态里的枚举属性（用于皮带漏斗的 shape）。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setFunnelShape(SceneBuilder scene, BlockPos pos, String shape) {
        scene.world().modifyBlock(pos, state -> {
            for (Property<?> property : state.getProperties()) {
                if (!"shape".equals(property.getName())) {
                    continue;
                }
                for (Object value : property.getPossibleValues()) {
                    if (value.toString().equals(shape)) {
                        return state.setValue((Property) property, (Comparable) value);
                    }
                }
            }
            return state;
        }, false);
    }

    /** 取核能熔炉部件的方块状态，并按需设置 active。 */
    private static BlockState componentState(boolean active) {
        Block component = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("alexscaves", "nuclear_furnace_component"));
        BlockState state = component.defaultBlockState();
        BooleanProperty activeProperty = null;
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty bool && "active".equals(property.getName())) {
                activeProperty = bool;
                break;
            }
        }
        return activeProperty == null ? state : state.setValue(activeProperty, active);
    }

    /** 在 [x1..x2] × [z1..z2] 的 y=1 层铺火，跳过熔炉所在的洞口。 */
    private static void placeFire(SceneBuilder scene, int x1, int z1, int x2, int z2,
                                  int holeX1, int holeZ1, int holeX2, int holeZ2) {
        BlockState fire = Blocks.FIRE.defaultBlockState();
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (x >= holeX1 && x <= holeX2 && z >= holeZ1 && z <= holeZ2) {
                    continue;
                }
                scene.world().setBlock(new BlockPos(x, 1, z), fire, false);
            }
        }
    }

    /** 改熔炉方块实体的废料量（mB，100 = 1 份，1000 即核爆阈值）。 */
    private static void setWaste(SceneBuilder scene, Selection furnace, int milliBuckets) {
        scene.world().modifyBlockEntityNBT(furnace, BlockEntity.class, tag -> {
            if ("alexscaves:nuclear_furnace".equals(tag.getString("id"))) {
                tag.putInt("Waste", milliBuckets);
            }
        });
    }

    /** 单次粒子爆发。 */
    private static void emit(SceneBuilder scene, ParticleOptions particle, Vec3 pos) {
        scene.effects().emitParticles(pos,
                scene.effects().simpleParticleEmitter(particle, Vec3.ZERO), 0.15f, 30);
    }

    /** 少量粒子，用于画环 / 多点效果，避免一次堆太多。 */
    private static void emitSmall(SceneBuilder scene, ParticleOptions particle, Vec3 pos, int max) {
        scene.effects().emitParticles(pos,
                scene.effects().simpleParticleEmitter(particle, Vec3.ZERO), 0.05f, max);
    }

    /** 在给定位置连续喷黄色尘粒，模拟黄烟（每次喷 4 个点，密度更高）。 */
    private static void emitYellowSmoke(SceneBuilder scene, Vec3 pos, int bursts, int gap) {
        DustParticleOptions yellow = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.15f), 3.0f);
        for (int i = 0; i < bursts; i++) {
            emit(scene, yellow, pos);
            emit(scene, yellow, pos.add(0.5, 0.2, 0));
            emit(scene, yellow, pos.add(-0.5, 0.1, 0.35));
            emit(scene, yellow, pos.add(0.1, 0.35, -0.45));
            scene.idle(gap);
        }
    }
}
