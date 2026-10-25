package com.soof.nuclearcompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soof.nuclearcompat.ModBlocks;
import com.soof.nuclearcompat.client.AutuniteSpearRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 钙铀云母长矛的物品渲染（Alex's Caves Up 移植版）。
 *
 * <p>AC 的 {@code ACItemstackRenderer.renderByItem} 只处理它自己注册的物品，没有兜底，
 * 我们的长矛放进去什么都不画。这里在方法头部拦截：如果是本模组的长矛，就交给
 * {@link AutuniteSpearRender} 渲染并取消原方法。</p>
 *
 * <p>用 {@code @Pseudo} + 字符串目标名，未安装 Up 版时不报错。</p>
 */
@Pseudo
@Mixin(targets = "com.alexscaves.client.render.item.ACItemstackRenderer")
public class ACItemstackRendererMixin {

    @Inject(
            method = "renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void acnc$renderAutuniteSpear(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                          MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (stack.is(ModBlocks.AUTUNITE_SPEAR.get())) {
            AutuniteSpearRender.render(stack, context, pose, buffer, light, overlay);
            ci.cancel();
        }
    }
}
