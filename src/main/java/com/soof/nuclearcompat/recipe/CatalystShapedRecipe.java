package com.soof.nuclearcompat.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.soof.nuclearcompat.ModRecipes;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * 带「催化剂」的有序配方。
 *
 * <p>匹配 {@link #catalysts} 的合成格在合成后<b>原样保留</b>（连耐久、附魔、组件一起），
 * 其余格子按原版逻辑正常消耗。核子可乐用的就是「8 个辐射类材料围一圈 +
 * 中心紫色苏打水瓶」，只有中心那瓶被消耗。</p>
 *
 * <p>实现要点：工作台的结算在 {@code ResultSlot#onTake}，它对每个格子
 * {@code removeItem(i, 1)} 之后，会把 {@code Recipe#getRemainingItems} 返回的
 * 物品放回该格；同物品同组件时直接并回格子，视觉上就是「没被消耗」。
 * 因此只要重写 {@code getRemainingItems} 即可。</p>
 *
 * <p>注意：不需要注册自定义 {@code RecipeType}。{@code CraftingRecipe#getType()}
 * 默认返回 {@code RecipeType.CRAFTING}，而 {@code RecipeManager} 是按
 * {@code recipe.getType()} 归档的，所以这份配方仍会被工作台的
 * {@code getRemainingItemsFor(RecipeType.CRAFTING, ...)} 找到。</p>
 */
public class CatalystShapedRecipe extends ShapedRecipe {

    private final List<Ingredient> catalysts;

    public CatalystShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
                                ItemStack result, boolean showNotification, List<Ingredient> catalysts) {
        super(group, category, pattern, result, showNotification);
        this.catalysts = List.copyOf(catalysts);
    }

    public List<Ingredient> catalysts() {
        return this.catalysts;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        // 先走原版逻辑，保留「物品自带 craftRemainder」之类的行为（例如桶）。
        NonNullList<ItemStack> remainder = super.getRemainingItems(input);

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty() || !isCatalyst(stack)) {
                continue; // 空槽或普通材料 → 正常消耗
            }
            // 退还这一格里的一个：原样复制，耐久/附魔/组件全部保留，净消耗为 0。
            remainder.set(i, stack.copyWithCount(1));
        }
        return remainder;
    }

    private boolean isCatalyst(ItemStack stack) {
        for (Ingredient ingredient : this.catalysts) {
            if (ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CATALYST_SHAPED.get();
    }

    /** 序列化器：JSON 用 MapCodec，联机同步用 StreamCodec（6 个字段正好是 composite 的上限）。 */
    public static final class Serializer implements RecipeSerializer<CatalystShapedRecipe> {

        public static final MapCodec<CatalystShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
                        .forGetter(ShapedRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.getResultItem(null)),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification),
                Ingredient.CODEC.listOf().optionalFieldOf("catalysts", List.of())
                        .forGetter(CatalystShapedRecipe::catalysts)
        ).apply(inst, CatalystShapedRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CatalystShapedRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
                        CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
                        ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.pattern,
                        ItemStack.STREAM_CODEC, recipe -> recipe.getResultItem(null),
                        ByteBufCodecs.BOOL, ShapedRecipe::showNotification,
                        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
                        CatalystShapedRecipe::catalysts,
                        CatalystShapedRecipe::new);

        @Override
        public MapCodec<CatalystShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CatalystShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
