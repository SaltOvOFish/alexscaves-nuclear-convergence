package com.soof.nuclearcompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soof.nuclearcompat.ModBlocks;
import com.soof.nuclearcompat.client.AutuniteSpearRenderNeo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 钙铀云母长矛的物品渲染（Alex's Caves Neo 移植版）。
 *
 * @see ACItemstackRendererMixin
 */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.client.render.item.ACItemstackRenderer")
public class ACItemstackRendererNeoMixin {

    @Inject(
            method = "renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void acnc$renderAutuniteSpear(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                          MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (stack.is(ModBlocks.AUTUNITE_SPEAR.get())) {
            AutuniteSpearRenderNeo.render(stack, context, pose, buffer, light, overlay);
            ci.cancel();
        }
    }
}
