package com.soof.nuclearcompat.client.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 潜水艇（{@code alexscaves:submarine}）的思索，绑定在奥秘引擎上。
 *
 * <p>搭建：第一层 3×3 铜块；第二、三层各 2×3 铜块（前方留一排），
 * 第二层中心放奥秘引擎；留空的一排用深海玻璃补上，结构补齐后自动组装成潜水艇。</p>
 *
 * <p>结构：{@code submarine/submarine.nbt}，7×5×7。</p>
 */
public final class SubmarinePonderScenes {

    private static final BlockPos ENGINE = new BlockPos(3, 2, 3);
    private static final ResourceLocation SUBMARINE_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "submarine");

    private SubmarinePonderScenes() {
    }

    /** P1：搭建与组装。 */
    public static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("submarine_assembly", "潜水艇：搭建与下水");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        scene.overlay().showText(15)
                .text("先用 3×3 铜块铺出底层")
                .pointAt(util.vector().centerOf(3, 1, 3))
                .placeNearTarget()
                .attachKeyFrame();

        // 第一层：3×3 铜块，逐块从上方落下
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                drop(scene, util, x, 1, z, Blocks.COPPER_BLOCK.defaultBlockState());
            }
        }
        scene.idle(10);

        scene.overlay().showText(25)
                .text("第二、三层使用铜块和奥秘引擎按此搭建，前方留出一排")
                .pointAt(util.vector().centerOf(3, 2, 2))
                .placeNearTarget()
                .attachKeyFrame();

        // 第二层：靠后的 2×3，中心那块是奥秘引擎
        for (int x = 2; x <= 4; x++) {
            for (int z = 3; z <= 4; z++) {
                if (x == 3 && z == 3) {
                    continue;
                }
                drop(scene, util, x, 2, z, Blocks.COPPER_BLOCK.defaultBlockState());
            }
        }
        drop(scene, util, 3, 2, 3, acBlock("alexscaves", "enigmatic_engine"));
        scene.idle(10);

        // 第三层：靠后的 2×3
        for (int x = 2; x <= 4; x++) {
            for (int z = 3; z <= 4; z++) {
                drop(scene, util, x, 3, z, Blocks.COPPER_BLOCK.defaultBlockState());
            }
        }
        scene.idle(10);

        scene.overlay().showText(10)
                .text("用深海玻璃补上留空的一排")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(3, 2, 2))
                .placeNearTarget()
                .attachKeyFrame();

        // 留空的一排用深海玻璃补上
        for (int x = 2; x <= 4; x++) {
            for (int y = 2; y <= 3; y++) {
                drop(scene, util, x, y, 2, acBlock("alexscaves", "depth_glass"));
            }
        }
        scene.idle(20);

        // 方块被实体取代：整体清空，同时生成潜水艇实体
        scene.world().setBlocks(util.select().fromTo(2, 1, 2, 4, 3, 4), Blocks.AIR.defaultBlockState(), false);
        spawnSubmarine(scene);
        scene.effects().emitParticles(util.vector().centerOf(3, 2, 3),
                scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION, Vec3.ZERO), 1f, 1);
        scene.effects().emitParticles(util.vector().centerOf(3, 2, 3),
                scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, new Vec3(0, 0.05, 0)), 0.8f, 20);
        scene.idle(10);

        scene.overlay().showText(15)
                .text("外壳补齐后，会在原地自动组装出潜水艇")
                .pointAt(util.vector().centerOf(3, 2, 3))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(15);

        // 镜头顺时针转 90°
        scene.rotateCameraY(-90f);
        scene.idle(20);

        scene.overlay().showText(20)
                .text("右键乘坐，用 WASD 控制潜水艇前进转向，空格 / Ctrl 控制升降")
                .pointAt(util.vector().centerOf(3, 2, 3))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);

        scene.markAsFinished();
    }

    /** 让一格方块从上方自然落到位。 */
    private static void drop(SceneBuilder scene, SceneBuildingUtil util, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        scene.world().setBlock(pos, state, false);
        scene.world().showSection(util.select().position(pos), Direction.UP);
        scene.idle(3);
    }

    /** 生成一只静态停放的潜水艇实体，代替原来的方块外壳。 */
    private static void spawnSubmarine(SceneBuilder scene) {
        try {
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(SUBMARINE_ID)) {
                return;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(SUBMARINE_ID);
            scene.world().createEntity(level -> {
                Entity entity = type.create(level);
                if (entity != null) {
                    entity.setPos(3.5, 1.0, 3.5);
                    entity.setYRot(180.0F);
                    entity.setNoGravity(true);
                    entity.setDeltaMovement(Vec3.ZERO);
                }
                return entity;
            });
        } catch (Throwable ignored) {
            // 实体显示失败时不影响整页场景
        }
    }

    private static BlockState acBlock(String namespace, String path) {
        return BuiltInRegistries.BLOCK
                .get(ResourceLocation.fromNamespaceAndPath(namespace, path))
                .defaultBlockState();
    }
}
