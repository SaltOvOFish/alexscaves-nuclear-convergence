package com.soof.nuclearcompat.client;

import com.github.alexmodguy.alexscaves.client.model.LimestoneSpearModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.soof.nuclearcompat.ModBlocks;
import com.soof.nuclearcompat.NuclearCompat;
import com.soof.nuclearcompat.AutuniteSpearEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * 钙铀云母长矛的物品渲染（Alex's Caves Neo 移植版）。
 *
 * @see AutuniteSpearRender
 */
public final class AutuniteSpearRenderNeo {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            NuclearCompat.MODID, "textures/entity/autunite_spear.png");

    private static LimestoneSpearModel model;
    private static Method renderMethod;
    private static Method setupAnimMethod;

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
        // 与 AC 的石灰岩长矛分支保持完全相同的变换
        pose.translate(0.5F, 0.5F, 0.5F);

        boolean hand = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        if (hand) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-180.0F));
            pose.translate(0.0F, -0.85F, -0.1F);
            if (context.firstPerson()) {
                pose.translate(0.0F, 0.5F, 0.0F);
                pose.scale(0.75F, 0.75F, 0.75F);
            }
            VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(
                    buffer, RenderType.armorCutoutNoCull(TEXTURE), stack.hasFoil());
            renderModel(pose, consumer, light, overlay);
            pose.popPose();
        } else {
            ItemStack sprite = new ItemStack(ModBlocks.AUTUNITE_SPEAR_SPRITE.get());
            sprite.applyComponents(stack.getComponents());
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.getItemRenderer().renderStatic(sprite, context,
                    context == ItemDisplayContext.GROUND ? light : 240,
                    overlay, pose, buffer, minecraft.level, 0);
        }
    }

    public static ResourceLocation texture() {
        return TEXTURE;
    }

    /** 投掷物在世界中的渲染（由 {@link AutuniteSpearEntityRendererNeo} 调用）。 */
    public static void renderEntity(AutuniteSpearEntity entity, float partialTick, PoseStack pose,
                                    MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.translate(0.0D, 0.25D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 180.0F));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) + 90.0F));
        pose.translate(0.0D, 0.25D, 0.0D);
        setupAnim(entity, entity.tickCount + partialTick);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        renderModel(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** Citadel 的 {@code setupAnim} 同样不在编译类路径，用反射调用。 */
    private static void setupAnim(Entity entity, float ageInTicks) {
        try {
            if (setupAnimMethod == null) {
                setupAnimMethod = LimestoneSpearModel.class.getMethod("setupAnim",
                        Entity.class, float.class, float.class, float.class, float.class, float.class);
            }
            setupAnimMethod.invoke(model(), entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        } catch (ReflectiveOperationException ignored) {
            // 方法缺失时保持默认姿态
        }
    }

    /**
     * Citadel 的 {@code AdvancedEntityModel} 不在编译类路径里（运行期由 Citadel 提供），
     * 所以用反射调用它上面的 {@code renderToBuffer}。
     */
    private static void renderModel(PoseStack pose, VertexConsumer consumer, int light, int overlay) {
        try {
            if (renderMethod == null) {
                renderMethod = LimestoneSpearModel.class.getMethod("renderToBuffer",
                        PoseStack.class, VertexConsumer.class, int.class, int.class, int.class);
            }
            renderMethod.invoke(model(), pose, consumer, light, overlay, -1);
        } catch (ReflectiveOperationException ignored) {
            // 方法缺失时静默跳过，不影响物品其它部分的渲染
        }
    }
}
