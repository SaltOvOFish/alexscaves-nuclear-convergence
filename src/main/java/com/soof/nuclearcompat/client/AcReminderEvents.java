package com.soof.nuclearcompat.client;

import com.soof.nuclearcompat.NuclearCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 未安装 Alex's Caves 时，在标题界面弹出一次提醒（可跳转下载页）。
 *
 * <p>触发条件：客户端、标题界面、无加载遮罩、已进入世界之外；标题界面稳定
 * {@value #TITLE_SETTLE_TICKS} tick 后弹出，避免在初始化/窗口缩放过程中改动 screen 栈。
 * 装了任意一款移植版（它们共用 modid {@code alexscaves}）就不显示。</p>
 *
 * <p>用 {@link Minecraft#setScreen} 而不是 {@code pushGuiLayer}：前者会同时释放鼠标、
 * 清理 GUI 层并调用 {@code Screen#added()}，避免出现「界面可见但输入仍归底层标题界面」的状态。</p>
 *
 * <p>不依赖任何 Alex's Caves 类，所以缺少前置时也能正常加载。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID, value = Dist.CLIENT)
public final class AcReminderEvents {

    /** 标题界面出现后等待多少 tick 再弹出，避开加载遮罩消失/窗口缩放的时序。 */
    private static final int TITLE_SETTLE_TICKS = 10;

    /** 本次启动是否已经检查/弹过；无论是否弹出，都只判定一次。 */
    private static boolean checkedThisLaunch;

    /** 标题界面已稳定存在的 tick 数。 */
    private static int titleTicks;

    private AcReminderEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (checkedThisLaunch) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        // 还在加载界面 / 已进入世界 / 不是标题界面时都不处理，并重置稳定计数。
        if (minecraft.level != null || minecraft.getOverlay() != null || !(minecraft.screen instanceof TitleScreen)) {
            titleTicks = 0;
            return;
        }

        if (++titleTicks < TITLE_SETTLE_TICKS) {
            return;
        }

        checkedThisLaunch = true;

        // 五个移植版共用 modid=alexscaves：装了任意一款就不提示，未安装时每次启动显示一次。
        if (ModList.get().isLoaded("alexscaves")) {
            return;
        }

        NuclearCompat.LOGGER.info(
                "[Nuclear Convergence] Alex's Caves is not installed; showing the installation reminder");
        minecraft.setScreen(new AcReminderScreen());
    }
}
