package com.soof.nuclearcompat;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 废弃反应堆的 Structure 注册。
 *
 * <p>注册成真正的 {@code Structure} 之后，{@code /locate structure
 * alexscaves_nuclear_convergence:abandoned_reactor} 才能查询到它。</p>
 */
public final class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, NuclearCompat.MODID);

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, NuclearCompat.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<AbandonedReactorStructure>> ABANDONED_REACTOR =
            STRUCTURE_TYPES.register("abandoned_reactor", () -> () -> AbandonedReactorStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> REACTOR_PIECE =
            PIECE_TYPES.register("abandoned_reactor", () -> AbandonedReactorPiece::new);

    private ModStructures() {
    }

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        PIECE_TYPES.register(bus);
    }
}
