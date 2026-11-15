package com.soof.nuclearcompat.client;

import com.github.alexmodguy.alexscaves.client.model.LimestoneSpearModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.soof.nuclearcompat.AutuniteSpearEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 钙铀云母长矛的物品渲染（Alex's Caves Neo 移植版）。
 *
 * <p>与 {@link AutuniteSpearRender} 行为完全一致，仅使用 Neo 包名下的
 * {@code LimestoneSpearModel}。共享逻辑见 {@link AutuniteSpearRenderSupport}。</p>
 */
public final class AutuniteSpearRenderNeo {

    private static LimestoneSpearModel model;
    private static final AutuniteSpearRenderSupport.ReflectiveModel REFLECTION =
            new AutuniteSpearRenderSupport.ReflectiveModel();

    private AutuniteSpearRenderNeo() {
    }

    private static LimestoneSpearModel model() {
        if (model == null) {
            model = new LimestoneSpearModel();
        }
        return model;
    }

    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffer, int light, int overlay) {
        AutuniteSpearRenderSupport.renderItem(stack, context, pose, buffer, light, overlay,
                (p, consumer, l, o) -> REFLECTION.render(model(), p, consumer, l, o));
    }

    public static ResourceLocation texture() {
        return AutuniteSpearRenderSupport.texture();
    }

    /** 投掷物在世界中的渲染（由 {@link AutuniteSpearEntityRendererNeo} 调用）。 */
    public static void renderEntity(AutuniteSpearEntity entity, float partialTick, PoseStack pose,
                                    MultiBufferSource buffer, int light) {
        AutuniteSpearRenderSupport.renderEntity(entity, partialTick, pose, buffer, light,
                (p, consumer, l, o) -> {
                    setupAnim(entity, entity.tickCount + partialTick);
                    REFLECTION.render(model(), p, consumer, l, o);
                });
    }

    /** Citadel 的 {@code setupAnim} 不在编译类路径，用反射调用（缺方法时保持默认姿态）。 */
    private static void setupAnim(Entity entity, float ageInTicks) {
        REFLECTION.setupAnim(model(), entity, ageInTicks);
    }
}
