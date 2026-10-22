package com.soof.nuclearcompat.client;

import com.soof.nuclearcompat.NuclearCompat;
import com.soof.nuclearcompat.client.ponder.MagneticQuarryPonderScenes;
import com.soof.nuclearcompat.client.ponder.NuclearFurnacePonderScenes;
import com.soof.nuclearcompat.client.ponder.SubmarinePonderScenes;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * 本模组的思索插件：把场景挂到 Alex's Caves 的物品上。
 *
 * <p>核能熔炉本体没有对应物品（只有 blockstate），玩家实际持有的是
 * {@code alexscaves:nuclear_furnace_component}，所以思索入口挂它；
 * 磁力采石场直接挂 {@code alexscaves:quarry} 方块物品。</p>
 */
public final class NuclearPonderPlugin implements PonderPlugin {

    /** 核能熔炉部件的注册名（Up / Neo 两个移植版都是这个 id）。 */
    public static final ResourceLocation NUCLEAR_FURNACE_COMPONENT =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "nuclear_furnace_component");

    @Override
    public String getModId() {
        return NuclearCompat.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(NUCLEAR_FURNACE_COMPONENT)
                // P1 组装（basic.nbt）
                .addStoryBoard("nuclear_furnace/basic", NuclearFurnacePonderScenes::assembly)
                // P2 输入与输出（basic.nbt，4 个时间点 + 镜头转场 + 淡出）
                .addStoryBoard("nuclear_furnace/basic", NuclearFurnacePonderScenes::io)
                // P3 临界与失控（占位）
                .addStoryBoard("nuclear_furnace/critical", NuclearFurnacePonderScenes::critical);

        // 磁力采石场（2 页）—— 采石场本体与粉碎器都作为触发物品
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("alexscaves", "quarry"))
                .addStoryBoard("quarry/quarry_1", MagneticQuarryPonderScenes::bounds)
                .addStoryBoard("quarry/quarry_2", MagneticQuarryPonderScenes::mining);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("alexscaves", "quarry_smasher"))
                .addStoryBoard("quarry/quarry_1", MagneticQuarryPonderScenes::bounds)
                .addStoryBoard("quarry/quarry_2", MagneticQuarryPonderScenes::mining);

        // 潜水艇（1 页）：奥秘引擎与潜水艇物品都能触发
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("alexscaves", "enigmatic_engine"))
                .addStoryBoard("submarine/submarine", SubmarinePonderScenes::assembly);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("alexscaves", "submarine"))
                .addStoryBoard("submarine/submarine", SubmarinePonderScenes::assembly);
    }
}
