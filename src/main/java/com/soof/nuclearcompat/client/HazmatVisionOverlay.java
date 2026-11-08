package com.soof.nuclearcompat.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import com.soof.nuclearcompat.NuclearCompat;

/**
 * 让 Alex's Caves 的防化头盔也拥有「视野被头盔遮住」的第一人称效果。
 *
 * <p>Create Nuclear 的防辐射头盔自带一层全屏遮罩（{@code HelmetOverlay}，按耐久度换贴图）；
 * AC 的防化头盔则完全没有。这里用同一套贴图（已内置到本模组）为防化头盔补上同样的效果，
 * 两套护甲的遮挡强度、花纹与耐久分档保持一致。</p>
 *
 * <p>贴图放在 {@code assets/alexscaves_nuclear_convergence/textures/misc/helmet_vision/}。
 * 本效果没有配置开关：内置资源包 {@code Nuclear Convergence: Clear Helmet Vision}（默认不启用）
 * 会把这些贴图以及 Create Nuclear 的同名贴图一并替换为透明图，启用它即可关闭两套头盔的遮挡。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HazmatVisionOverlay {

    /**
     * 只存 ID、不缓存 {@code Item}。
     *
     * <p>{@code @EventBusSubscriber} 的类会在模组构造阶段就被加载，那时 Alex's Caves 的物品
     * 还没进入注册表，{@code BuiltInRegistries.ITEM.get(...)} 会返回 {@code minecraft:air}；
     * 而空头槽的 {@link ItemStack#getItem()} 恰好也是 AIR，结果就是「不戴头盔时显示遮罩、
     * 戴防化面罩时反而不显示」。所以这里每次都按注册名实时比对。</p>
     */
    private static final ResourceLocation HAZMAT_MASK_ID =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "hazmat_mask");

    private static final String TEXTURE_ROOT = "textures/misc/helmet_vision/";

    /** 与 Create Nuclear 一致的 5 档贴图（按剩余耐久从高到低）。 */
    private static final ResourceLocation[] TEXTURES = {
            texture("helmet_new"),
            texture("helmet_minor_damage"),
            texture("helmet_crack1"),
            texture("helmet_crack2"),
            texture("helmet_almost_broken")
    };

    private HazmatVisionOverlay() {
    }

    /**
     * 这里必须带 {@code .png} 后缀：{@link GuiGraphics#blit} 会把 ResourceLocation 直接交给
     * 纹理管理器按原样查找文件，不会自动补扩展名（漏写就会变成「缺失贴图」的品红黑块）。
     */
    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, TEXTURE_ROOT + name + ".png");
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "hazmat_vision"),
                HazmatVisionOverlay::render);
    }

    private static void render(GuiGraphics gui, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        // 只在第一人称绘制（第三人称看不到自己的头盔遮罩）。
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || !HAZMAT_MASK_ID.equals(BuiltInRegistries.ITEM.getKey(helmet.getItem()))) {
            return;
        }

        ResourceLocation texture = pickTexture(helmet);

        int width = gui.guiWidth();
        int height = gui.guiHeight();
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        // 把整张贴图拉伸铺满整屏（textureWidth/Height 传屏幕尺寸即可覆盖完整 UV）。
        gui.blit(texture, 0, 0, 0, 0, width, height, width, height);
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** 与 Create Nuclear 的 HelmetOverlay 相同的耐久分档。 */
    private static ResourceLocation pickTexture(ItemStack helmet) {
        int maxDamage = helmet.getMaxDamage();
        if (maxDamage <= 0) {
            return TEXTURES[0];
        }
        float remaining = (float) (maxDamage - helmet.getDamageValue()) / (float) maxDamage;
        if (remaining >= 0.95F) {
            return TEXTURES[0];
        }
        if (remaining >= 0.8F) {
            return TEXTURES[1];
        }
        if (remaining >= 0.6F) {
            return TEXTURES[2];
        }
        if (remaining >= 0.25F) {
            return TEXTURES[3];
        }
        return TEXTURES[4];
    }
}
