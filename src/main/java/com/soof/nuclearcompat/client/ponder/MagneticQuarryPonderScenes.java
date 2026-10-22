package com.soof.nuclearcompat.client.ponder;

import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 磁力采石场（{@code alexscaves:quarry}）的思索，共 2 页。
 *
 * <p>机制：需要 4 盏磁力灯（{@code alexscaves:magnetic_light}）在同一高度围出矩形矿区，
 * 再把磁力粉碎器放进矿区中心，采石场才会开始工作。</p>
 *
 * <p>结构：{@code quarry_1.nbt} / {@code quarry_2.nbt}，7×5×7，内容一致
 * （y=0 石头底板、y=1 石头与矿、y=2 草地、y=3 采石场与四灯）。</p>
 */
public final class MagneticQuarryPonderScenes {

    private static final BlockPos QUARRY = new BlockPos(5, 3, 0);
    /** 四灯围出的矩形：x 0.5..6.5，z 1.5..6.5；灯方块在 y=3。 */
    private static final double BORDER_Y = 3.5;
    /** 矿区中心（粉碎器悬停位置）。 */
    private static final Vec3 AREA_CENTER = new Vec3(3.5, 4.2, 4.0);
    private static final ResourceLocation SMASHER_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "quarry_smasher");

    private MagneticQuarryPonderScenes() {
    }

    /** P1：划定矿区。 */
    public static void bounds(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("magnetic_quarry_bounds", "磁力采石场：划定矿区");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        // 地形整体降入：石头与矿（y=1）+ 草地（y=2）
        scene.world().showSection(util.select().fromTo(0, 1, 0, 6, 2, 6), Direction.DOWN);
        scene.idle(15);

        // 采石场本体单独降入
        drop(scene, util, QUARRY.getX(), QUARRY.getY(), QUARRY.getZ(),
                acBlock("quarry"), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(30)
                .text("磁力采石场需要用 4 盏磁力灯标出开采范围")
                .pointAt(util.vector().centerOf(QUARRY))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(5);

        // 四盏磁力灯一个个降入
        int[][] lamps = {{0, 1}, {6, 1}, {0, 6}, {6, 6}};
        for (int[] lamp : lamps) {
            drop(scene, util, lamp[0], 3, lamp[1], acBlock("magnetic_light"), Direction.DOWN);
            scene.idle(6);
        }
        scene.idle(20);

        // 白色边框线常驻
        borderLines(scene, 220);

        scene.overlay().showText(20)
                .text("四盏灯围出的矩形就是矿区，最大 20 格")
                .colored(PonderPalette.GREEN)
                .pointAt(new Vec3(3.5, 3.5, 4.0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);

        // 中心上方一格生成粉碎器实体
        spawnSmasher(scene);

        scene.overlay().showText(20)
                .text("四角齐全后，在矿区中心放入粉碎器")
                .pointAt(AREA_CENTER)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(20);

        scene.markAsFinished();
    }

    /** P2：自动开采。 */
    public static void mining(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("magnetic_quarry_mining", "磁力采石场：自动开采");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(0, 1, 0, 6, 4, 6), Direction.DOWN);
        scene.idle(15);

        // 边界线 + 中心粉碎器
        borderLines(scene, 700);
        spawnSmasher(scene);
        scene.idle(10);

        scene.overlay().showText(40)
                .text("粉碎器会在矿区内往返，砸碎下方的方块")
                .pointAt(AREA_CENTER)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(10);

        // 阶段 1：中心 9 格逐块挖掉（每格 6 tick）
        int[][] center9 = {
                {2, 2}, {3, 2}, {4, 2},
                {2, 3}, {4, 3},
                {2, 4}, {3, 4}, {4, 4},
                {3, 3}
        };
        boolean[][] mined = new boolean[7][7];
        for (int[] spot : center9) {
            mineColumn(scene, spot[0], spot[1]);
            mined[spot[0]][spot[1]] = true;
            scene.idle(4);
        }
        scene.idle(10);

        scene.overlay().showText(40)
                .text("砸出的资源会被磁力吸回采石场")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(QUARRY))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(20);

        // 阶段 2：只挖磁力灯范围中间，外圈（x=0/6、z=1/6）保留；每列 6 tick
        for (int z = 2; z <= 5; z++) {
            for (int x = 1; x <= 5; x++) {
                if (mined[x][z]) {
                    continue;
                }
                mineColumn(scene, x, z);
                scene.idle(6);
            }
        }

        scene.overlay().showText(40)
                .text("矿区内再无可挖方块时，采石场停止工作")
                .pointAt(AREA_CENTER)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        scene.markAsFinished();
    }

    /** 挖掉一列：草地层 + 石头层，配中心爆炸烟幕。 */
    private static void mineColumn(SceneBuilder scene, int x, int z) {
        scene.world().setBlock(new BlockPos(x, 2, z), Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(x, 1, z), Blocks.AIR.defaultBlockState(), false);
        smokescreen(scene);
    }

    /** 单格方块从指定方向落到位。 */
    private static void drop(SceneBuilder scene, SceneBuildingUtil util, int x, int y, int z,
                             BlockState state, Direction direction) {
        BlockPos pos = new BlockPos(x, y, z);
        scene.world().setBlock(pos, state, false);
        scene.world().showSection(util.select().position(pos), direction);
        scene.idle(2);
    }

    /**
     * 在四盏灯围成的中心生成一只静态停放的粉碎器实体。
     *
     * <p>只展示模型：关重力、清速度，并用反射把 quarryPos 指向采石场、
     * 置为 inactive，避免它自己的 tick 去找矿或算锤头时报错。</p>
     */
    private static void spawnSmasher(SceneBuilder scene) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(SMASHER_ID)) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(SMASHER_ID);
        ElementLink<EntityElement> link = scene.world().createEntity(level -> {
            Entity entity = type.create(level);
            if (entity != null) {
                entity.setPos(AREA_CENTER.x, AREA_CENTER.y, AREA_CENTER.z);
                entity.setNoGravity(true);
                entity.setDeltaMovement(Vec3.ZERO);
            }
            return entity;
        });
        scene.world().modifyEntity(link, entity -> {
            if (entity == null) {
                return;
            }
            try {
                entity.getClass().getMethod("setQuarryPos", BlockPos.class).invoke(entity, QUARRY);
            } catch (ReflectiveOperationException ignored) {
                // 版本差异时跳过
            }
            try {
                entity.getClass().getMethod("setInactive", boolean.class).invoke(entity, true);
            } catch (ReflectiveOperationException ignored) {
                // 版本差异时跳过
            }
        });
    }

    /** 沿矿区矩形四边画白色粗线，并补一层白色粒子。 */
    private static void borderLines(SceneBuilder scene, int ticks) {
        double x0 = 0.5, x1 = 6.5, z0 = 1.5, z1 = 6.5;
        Vec3 a = new Vec3(x0, BORDER_Y, z0);
        Vec3 b = new Vec3(x1, BORDER_Y, z0);
        Vec3 c = new Vec3(x1, BORDER_Y, z1);
        Vec3 d = new Vec3(x0, BORDER_Y, z1);
        scene.overlay().showBigLine(PonderPalette.WHITE, a, b, ticks);
        scene.overlay().showBigLine(PonderPalette.WHITE, b, c, ticks);
        scene.overlay().showBigLine(PonderPalette.WHITE, c, d, ticks);
        scene.overlay().showBigLine(PonderPalette.WHITE, d, a, ticks);

        double py = 4.06;
        ParticleEmitter emitter = scene.effects().simpleParticleEmitter(ParticleTypes.END_ROD, Vec3.ZERO);
        for (double x = x0; x <= x1 + 1e-6; x += 0.5) {
            scene.effects().emitParticles(new Vec3(x, py, z0), emitter, 0.06f, ticks);
            scene.effects().emitParticles(new Vec3(x, py, z1), emitter, 0.06f, ticks);
        }
        for (double z = z0 + 0.5; z < z1 - 1e-6; z += 0.5) {
            scene.effects().emitParticles(new Vec3(x0, py, z), emitter, 0.06f, ticks);
            scene.effects().emitParticles(new Vec3(x1, py, z), emitter, 0.06f, ticks);
        }
    }

    /** 矿区中心的一团爆炸 + 浓烟。 */
    private static void smokescreen(SceneBuilder scene) {
        Vec3 center = new Vec3(3.5, 3.05, 4.0);
        scene.effects().emitParticles(center,
                scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION, Vec3.ZERO), 1f, 1);
        scene.effects().emitParticles(center,
                scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, new Vec3(0, 0.05, 0)),
                0.8f, 24);
        scene.effects().emitParticles(center,
                scene.effects().simpleParticleEmitter(ParticleTypes.POOF, Vec3.ZERO), 0.5f, 8);
    }

    private static BlockState acBlock(String path) {
        return BuiltInRegistries.BLOCK
                .get(ResourceLocation.fromNamespaceAndPath("alexscaves", path))
                .defaultBlockState();
    }
}
