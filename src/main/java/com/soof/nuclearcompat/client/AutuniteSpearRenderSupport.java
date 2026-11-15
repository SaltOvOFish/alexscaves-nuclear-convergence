package com.soof.nuclearcompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.soof.nuclearcompat.AutuniteSpearEntity;
import com.soof.nuclearcompat.ModBlocks;
import com.soof.nuclearcompat.NuclearCompat;
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
 * 钙铀云母长矛渲染的共享实现（Up / Neo 两套渲染类共用）。
 *
 * <p>为什么两套渲染类不能合并：它们引用的 AC 模型类包名不同
 * （{@code com.alexscaves.client.model.LimestoneSpearModel} 与
 * {@code com.github.alexmodguy.alexscaves.client.model.LimestoneSpearModel}），
 * 而 JVM 在类校验阶段就要解析字段类型，同时引用两版会让只装一版的环境直接
 * {@code NoClassDefFoundError}。因此模型相关部分留在各自类里，这里只承载
 * 「变换矩阵 + 反射调用」这类与版本无关的逻辑。</p>
 *
 * <p>行为等价：所有变换数值、绘制顺序与反射参数均与原来两份实现逐行一致。</p>
 */
public final class AutuniteSpearRenderSupport {

    /** 实体贴图（32×32，UV 布局与 AC 石灰岩长矛一致），两版共用。 */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            NuclearCompat.MODID, "textures/entity/autunite_spear.png");

    private AutuniteSpearRenderSupport() {
    }

    public static ResourceLocation texture() {
        return TEXTURE;
    }

    /**
     * 物品渲染的共享主体。
     *
     * <p>保留原来两版的 push/pop 结构：手持分支在内层 push 之后绘制、随之 pop；
     * {@code drawModel} 只负责「把模型画进给定 consumer」，具体版本用反射调用。</p>
     */
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                  MultiBufferSource buffer, int light, int overlay,
                                  ModelDrawer drawModel) {
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
            drawModel.draw(pose, consumer, light, overlay);
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

    /** 投掷物在世界中的渲染（由各版本的实体渲染器调用）。 */
    public static void renderEntity(AutuniteSpearEntity entity, float partialTick, PoseStack pose,
                                   MultiBufferSource buffer, int light, ModelDrawer drawModel) {
        pose.pushPose();
        pose.translate(0.0D, 0.25D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 180.0F));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) + 90.0F));
        pose.translate(0.0D, 0.25D, 0.0D);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        drawModel.draw(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** 把模型画进给定 consumer 的钩子（具体版本用反射实现）。 */
    @FunctionalInterface
    public interface ModelDrawer {
        void draw(PoseStack pose, VertexConsumer consumer, int light, int overlay);
    }

    /**
     * Citadel 的 {@code AdvancedEntityModel} 不在编译类路径里（运行期由 Citadel 提供），
     * 所以统一用反射调用 {@code setupAnim} 与 {@code renderToBuffer}。
     *
     * <p>{@code Method} 按「模型类」缓存，避免每个渲染帧都做一次查找。</p>
     */
    public static final class ReflectiveModel {

        private Method renderMethod;
        private Method setupAnimMethod;

        /** 每帧调用：设置姿态（方法缺失时保持默认姿态）。 */
        public void setupAnim(Object model, Entity entity, float ageInTicks) {
            try {
                if (setupAnimMethod == null) {
                    setupAnimMethod = model.getClass().getMethod("setupAnim",
                            Entity.class, float.class, float.class, float.class, float.class, float.class);
                }
                setupAnimMethod.invoke(model, entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
            } catch (ReflectiveOperationException ignored) {
                // 方法缺失时保持默认姿态
            }
        }

        /** 每帧调用：绘制模型（方法缺失时静默跳过，不影响物品其它部分的渲染）。 */
        public void render(Object model, PoseStack pose, VertexConsumer consumer, int light, int overlay) {
            try {
                if (renderMethod == null) {
                    renderMethod = model.getClass().getMethod("renderToBuffer",
                            PoseStack.class, VertexConsumer.class, int.class, int.class, int.class);
                }
                renderMethod.invoke(model, pose, consumer, light, overlay, -1);
            } catch (ReflectiveOperationException ignored) {
                // 静默跳过
            }
        }
    }
}
