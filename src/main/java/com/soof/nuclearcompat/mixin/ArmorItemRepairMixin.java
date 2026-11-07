package com.soof.nuclearcompat.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让原子核动防辐射套装可以用任意「铅锭」修复。
 *
 * <p>Create Nuclear 的 {@code ArmorMaterial} 把修复材料写死为 {@code createnuclear:lead_ingot}，
 * 装了井然有矿（Oreganized）后玩家手上的铅锭是 {@code oreganized:lead_ingot}，无法修复。
 * 这里在 {@link ArmorItem#isValidRepairItem} 头部补一条判定：只要待修复物品是本模组
 * 认可的防辐射部件，且材料在 {@code #c:ingots/lead} 标签内，就视为可修复。</p>
 */
@Mixin(ArmorItem.class)
public abstract class ArmorItemRepairMixin {

    /** 通用铅锭标签：Create Nuclear 与 Oreganized 的铅锭都会登记在里面。 */
    private static final TagKey<Item> LEAD_INGOTS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/lead"));

    @Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
    private void acnc$allowAnyLeadIngot(ItemStack toRepair, ItemStack repair, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(toRepair.getItem());
        if (!"createnuclear".equals(id.getNamespace()) || !id.getPath().contains("anti_radiation")) {
            return;
        }
        if (repair.is(LEAD_INGOTS)) {
            cir.setReturnValue(true);
        }
    }
}
