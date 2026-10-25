package com.soof.nuclearcompat;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 钙铀云母长矛投掷物的实体类型。 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, NuclearCompat.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AutuniteSpearEntity>> AUTUNITE_SPEAR =
            ENTITY_TYPES.register("autunite_spear", () -> EntityType.Builder
                    .<AutuniteSpearEntity>of(AutuniteSpearEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("autunite_spear"));

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
