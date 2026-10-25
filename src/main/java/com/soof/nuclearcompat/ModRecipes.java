package com.soof.nuclearcompat;

import com.soof.nuclearcompat.recipe.CatalystShapedRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 配方序列化器注册。 */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, NuclearCompat.MODID);

    /** 带催化剂（合成后保留）的有序配方，见 {@link CatalystShapedRecipe}。 */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CatalystShapedRecipe>> CATALYST_SHAPED =
            SERIALIZERS.register("catalyst_shaped", CatalystShapedRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
