package com.soof.nuclearcompat.client;

import com.alexscaves.client.model.LimestoneSpearModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.soof.nuclearcompat.AutuniteSpearEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 钙铀云母长矛的物品渲染（Alex's Caves Up 移植版）。
 *
 * <p>AC 的 {@code ACItemstackRenderer} 只对自己注册的长矛物品分支渲染，且方法内没有兜底，
 * 我们的物品走进去会什么都不画。这里由 {@code ACItemstackRendererMixin} 在
 * {@code renderByItem} 头部拦截，交给我们渲染：</p>
 * <ul>
 *   <li>手持/蓄力：用 {@code LimestoneSpearModel} + 本模组的 32×32 实体贴图（UV 布局与 AC 相同）；</li>
 *   <li>GUI/地面/展示框等：交给隐藏的 {@code autunite_spear_sprite} 物品渲染 16×16 图标。</li>
 * </ul>
 *
 * <p>变换矩阵、绘制顺序与反射调用在 {@link AutuniteSpearRenderSupport}（与 Neo 版共用）；
 * 本类只保留 Up 版模型类与其 {@code Method} 缓存，因为两版模型类包名不同、不能同时引用。</p>
 */
public final class AutuniteSpearRender {

    private static LimestoneSpearModel model;
    private static final AutuniteSpearRenderSupport.ReflectiveModel REFLECTION =
            new AutuniteSpearRenderSupport.ReflectiveModel();

    private AutuniteSpearRender() {
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

    /** 投掷物在世界中的渲染（由 {@link AutuniteSpearEntityRenderer} 调用）。 */
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
