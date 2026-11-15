package com.soof.nuclearcompat.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.nuclearteam.createnuclear.content.explosion.NuclearExplosionEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/**
 * 让原子核动的反应堆熔毁核爆不伤害刚孵化的撼地斯拉（Tremorzilla）。
 *
 * <p>原子核动的 {@code NuclearExplosionEntity} 与 Alex's Caves 的同名实体
 * 逻辑几乎完全相同（复制而来），但 CN 版删掉了三处关键逻辑：蛋孵化、爆炸波及
 * 蛋时的「孵化而非摧毁」、以及对撼地斯拉的伤害免疫。其中伤害免疫缺失导致：</p>
 *
 * <ol>
 *   <li>我们的 {@code ReactorMeltdownExecutorMixin} 在爆炸前把蛋孵化出撼地斯拉；</li>
 *   <li>CN 核爆的伤害循环每 tick 对撼地斯拉造成数百点辐射伤害（无 AC 的
 *       {@code instanceof TremorzillaEntity -> damage = 0} 免疫），撼地斯拉 500 血
 *       约 2 tick 内死亡，于是「既没有蛋也没有撼地斯拉」。</li>
 * </ol>
 *
 * <p>这里通过 {@code @Redirect} 把伤害循环里的实体列表过滤掉撼地斯拉（按实体类型
 * 注册名 {@code alexscaves:tremorzilla} 识别，避免对 AC 具体类的编译期依赖，从而
 * 同时兼容 Up 与 Neo 两个移植版）。过滤后撼地斯拉既不受伤、也不会被击飞。</p>
 */
@Mixin(NuclearExplosionEntity.class)
public abstract class NuclearExplosionEntityMixin {

    private static final ResourceLocation TREMORZILLA_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "tremorzilla");

    /** 懒加载的撼地斯拉实体类型（null 表示 Alex's Caves 未加载）。 */
    @Unique
    private static EntityType<?> acnc$TREMORZILLA = null;

    /** 是否已完成解析（结果可能是 null，所以不能只靠 acnc$TREMORZILLA 判断）。 */
    @Unique
    private static boolean acnc$TREMORZILLA_RESOLVED = false;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
            )
    )
    private List<LivingEntity> acnc$filterTremorzilla(Level level, Class<LivingEntity> clazz, AABB box) {
        List<LivingEntity> entities = new ArrayList<>(level.getEntitiesOfClass(clazz, box));
        entities.removeIf(NuclearExplosionEntityMixin::acnc$isTremorzilla);
        return entities;
    }

    private static boolean acnc$isTremorzilla(Entity entity) {
        EntityType<?> tremorzilla = acnc$tremorzillaType();
        return tremorzilla != null && entity.getType() == tremorzilla;
    }

    private static EntityType<?> acnc$tremorzillaType() {
        if (!acnc$TREMORZILLA_RESOLVED) {
            // BuiltInRegistries.ENTITY_TYPE 是 DefaultedRegistry：缺失时返回 minecraft:pig 而不是 null，
            // 不查注册名的话，AC 缺失时所有猪都会变成“撼地斯拉”被核爆滤除。
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(TREMORZILLA_ID);
            acnc$TREMORZILLA = TREMORZILLA_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)) ? type : null;
            acnc$TREMORZILLA_RESOLVED = true;
        }
        return acnc$TREMORZILLA;
    }
}
