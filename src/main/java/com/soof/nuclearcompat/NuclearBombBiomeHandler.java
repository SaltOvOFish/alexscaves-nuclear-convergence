package com.soof.nuclearcompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.nuclearteam.createnuclear.infrastructure.worldgen.biome.BiomeIrradiationService;
import net.nuclearteam.createnuclear.infrastructure.worldgen.biome.CNBiomes;

/**
 * 当 Alex's Caves Up 的核弹（nuclear_bomb）爆炸时，把爆炸区域转化为
 * 原子核动（Create Nuclear）的辐照之地群系（createnuclear:irradiated_land）。
 *
 * 直接复用 Create Nuclear 的 {@link BiomeIrradiationService#circularArea}：
 * 它会同步把受影响的区块登记进 {@code PersistentIrradiatedZones}，从而让
 * {@code createnuclear:biome_irradiation_extractor} 能够正常消除这些群系。
 */
@EventBusSubscriber(modid = NuclearCompat.MODID)
public final class NuclearBombBiomeHandler {

    private NuclearBombBiomeHandler() {
    }

    /** Alex's Caves Up 的核爆实体注册名。 */
    private static final ResourceLocation NUCLEAR_EXPLOSION = ResourceLocation.fromNamespaceAndPath("alexscaves", "nuclear_explosion");

    /** 惰性解析并缓存的核爆实体类型（null 表示 Alex's Caves 未加载），避免每次实体加入都做注册表查询。 */
    private static boolean explosionTypeResolved = false;
    private static EntityType<?> nuclearExplosionType = null;

    private static EntityType<?> nuclearExplosionType() {
        if (!explosionTypeResolved) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(NUCLEAR_EXPLOSION);
            // get() 对缺失的注册名会返回默认值（猪），因此必须回查注册名确认。
            nuclearExplosionType = NUCLEAR_EXPLOSION.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type))
                    ? type : null;
            explosionTypeResolved = true;
        }
        return nuclearExplosionType;
    }

    /**
     * 群系转化半径（方块）。Create Nuclear 的反应堆失控爆炸使用 {@code size * 30}；
     * Alex's Caves 核弹的默认 {@code nukeExplosionSizeModifier} 为 3.0，故取 3.0 * 30 = 90。
     * 若调整了 Alex's Caves Up 的核弹爆炸规模配置，请同步修改此处。
     */
    private static final int IRRADIATION_RADIUS = 90;

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity.getType() != nuclearExplosionType()) {
            return;
        }

        BlockPos center = entity.blockPosition();

        // 延迟到下一 tick：EntityJoinLevelEvent 触发时目标区块可能尚未推进到 FULL，
        // 直接做区块交互会引发区块加载死锁（见该事件 Javadoc 警告）。
        serverLevel.getServer().execute(() ->
                BiomeIrradiationService.circularArea(serverLevel, center, CNBiomes.Irradiated.PLAIN, IRRADIATION_RADIUS));
    }
}
