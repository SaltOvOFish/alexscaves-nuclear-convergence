package com.soof.nuclearcompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soof.nuclearcompat.AutuniteSpearEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 钙铀云母长矛投掷物的渲染器（Up 版 AC 模型）。 */
public class AutuniteSpearEntityRenderer extends EntityRenderer<AutuniteSpearEntity> {

    public AutuniteSpearEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AutuniteSpearEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        AutuniteSpearRender.renderEntity(entity, partialTick, pose, buffer, light);
        super.render(entity, entityYaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(AutuniteSpearEntity entity) {
        return AutuniteSpearRender.texture();
    }
}
