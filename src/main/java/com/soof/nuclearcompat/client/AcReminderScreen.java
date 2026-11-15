package com.soof.nuclearcompat.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 未安装 Alex's Caves 时，在标题界面弹出的一次性提示。
 *
 * <p>每个移植版一行，后面直接给出可用的 CurseForge / Modrinth 入口；需要前置的移植版
 * 在次行列出前置及其入口。没有在某个平台上架的项目，按钮置灰并给出提示，避免死链接。</p>
 *
 * <p>尺寸按常见的 GUI 缩放反推：面板最大 410×232，可在 426×240 的 GUI 空间（1280×720、缩放 3）
 * 内完整显示，不会像早期版本那样在 1080p/缩放 4（480×270）下被裁掉。</p>
 */
public final class AcReminderScreen extends Screen {

    private static final int PANEL_WIDTH = 410;
    private static final int PANEL_HEIGHT = 224;

    private static final int NAME_X = 12;
    private static final int PREREQ_X = 20;
    private static final int CF_X = 214;
    private static final int MR_X = 298;
    private static final int BTN_WIDTH = 76;
    private static final int BTN_HEIGHT = 18;
    private static final int SUB_BTN_HEIGHT = 16;

    private static final String CF_ROOT = "https://www.curseforge.com/minecraft/mc-mods/";
    private static final String MR_ROOT = "https://modrinth.com/mod/";

    private static final String KEY_PREFIX = "alexscaves_nuclear_convergence.reminder.";

    /**
     * 五个移植版及其入口。cf / modrinth 为 {@code null} 表示该项目未在该平台发布，
     * 界面会把对应按钮置灰。前置按各移植版实际声明的依赖区分（不是同一个 Citadel）。
     */
    private static final Port[] PORTS = {
            new Port(KEY_PREFIX + "port.up",
                    CF_ROOT + "alexs-caves-up", null,
                    KEY_PREFIX + "prereq.citadel_up", CF_ROOT + "citadel-up", null),
            new Port(KEY_PREFIX + "port.neo",
                    CF_ROOT + "alexs-caves-neo", MR_ROOT + "alexscavesneo",
                    KEY_PREFIX + "prereq.citadel", CF_ROOT + "citadel", MR_ROOT + "citadel"),
            new Port(KEY_PREFIX + "port.continued",
                    CF_ROOT + "alexs-caves-continued", MR_ROOT + "alexs-caves-continued",
                    KEY_PREFIX + "prereq.codxlib", CF_ROOT + "codxlib", MR_ROOT + "codxlib"),
            new Port(KEY_PREFIX + "port.unofficial",
                    CF_ROOT + "alexs-caves-unofficial-port", MR_ROOT + "alexs-caves-unofficial-port",
                    KEY_PREFIX + "prereq.citadel_unofficial",
                    CF_ROOT + "citadel-1-21-1-port", MR_ROOT + "citadel-1.21.1-port"),
            new Port(KEY_PREFIX + "port.renewed",
                    CF_ROOT + "alexs-caves-renewed", MR_ROOT + "alexs-caves-renewed",
                    null, null, null)
    };

    private record Port(String nameKey, String cf, String modrinth,
                        String prereqKey, String prereqCf, String prereqModrinth) {
    }

    private record Row(Component name, int nameY, Component prereq, int prereqY) {
    }

    private final List<Row> rows = new ArrayList<>();

    private int left;
    private int top;

    public AcReminderScreen() {
        super(Component.translatable(KEY_PREFIX + "title"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        rows.clear();

        int y = top + 30;
        for (Port port : PORTS) {
            rows.add(new Row(Component.translatable(port.nameKey()), y, null, 0));
            addLink(left + CF_X, y - 2, port.cf(), platformKey("curseforge"), BTN_HEIGHT);
            addLink(left + MR_X, y - 2, port.modrinth(), platformKey("modrinth"), BTN_HEIGHT);

            int rowHeight = 22;
            if (port.prereqKey() != null) {
                int prereqY = y + 17;
                rows.add(new Row(null, 0,
                        Component.translatable(KEY_PREFIX + "requires").append(Component.translatable(port.prereqKey())),
                        prereqY));
                addLink(left + CF_X, prereqY - 3, port.prereqCf(), platformKey("curseforge"), SUB_BTN_HEIGHT);
                addLink(left + MR_X, prereqY - 3, port.prereqModrinth(), platformKey("modrinth"), SUB_BTN_HEIGHT);
                rowHeight = 34;
            }
            y += rowHeight;
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> close())
                .bounds(left + (PANEL_WIDTH - 100) / 2, top + 204, 100, 18)
                .build());
    }

    private static Component platformKey(String platform) {
        return Component.translatable(KEY_PREFIX + "platform." + platform);
    }

    /** 入口为 null 时按钮置灰并给出「未上架」提示，绝不跳 404。 */
    private void addLink(int x, int y, String url, Component label, int height) {
        Button button = Button.builder(label, pressed -> openLink(url))
                .bounds(x, y, BTN_WIDTH, height)
                .build();
        if (url == null) {
            button.active = false;
            button.setTooltip(Tooltip.create(Component.translatable(KEY_PREFIX + "not_listed")));
        }
        addRenderableWidget(button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // setScreen 打开时标题界面已被替换，这里自己补上全景背景，避免变成纯黑屏。
        this.renderPanorama(guiGraphics, partialTick);
        guiGraphics.fill(0, 0, width, height, 0x99000000);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF00E0E0E);
        guiGraphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top, 0xFF6E6E6E);
        guiGraphics.fill(left - 1, top + PANEL_HEIGHT, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, 0xFF6E6E6E);
        guiGraphics.fill(left - 1, top, left, top + PANEL_HEIGHT, 0xFF6E6E6E);
        guiGraphics.fill(left + PANEL_WIDTH, top, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT, 0xFF6E6E6E);

        guiGraphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 6, 0xFFFFFF);
        guiGraphics.drawCenteredString(font,
                Component.translatable(KEY_PREFIX + "intro"),
                left + PANEL_WIDTH / 2, top + 17, 0xD0D0D0);

        for (Row row : rows) {
            if (row.name() != null) {
                guiGraphics.drawString(font, row.name(), left + NAME_X, row.nameY(), 0xFFFFFF, false);
            }
            if (row.prereq() != null) {
                guiGraphics.drawString(font, row.prereq(), left + PREREQ_X, row.prereqY(), 0xA8A8A8, false);
            }
        }

        // 说明文字可能较长（中英文长度差异大），按面板宽度自动换行并居中。
        int noteY = top + 190;
        for (var line : font.split(Component.translatable(KEY_PREFIX + "note"), PANEL_WIDTH - 24)) {
            int lineWidth = font.width(line);
            guiGraphics.drawString(font, line, left + (PANEL_WIDTH - lineWidth) / 2, noteY, 0x9A9A9A, false);
            noteY += 9;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /** 不要模糊背景：标题界面的全景图保留原样，只加一层暗色。 */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        close();
    }

    private void close() {
        if (minecraft != null) {
            // 用 setScreen 回到标题界面（同时释放鼠标、重置输入状态）；不要用 popGuiLayer，
            // 因为提示界面是通过 setScreen 打开的，不在 GUI 层栈里。
            minecraft.setScreen(new TitleScreen());
        }
    }

    /** 与 Create: AFN 相同：先弹原版的外链确认页，确认后才真正打开浏览器。 */
    private static void openLink(String url) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.pushGuiLayer(new ConfirmLinkScreen(result -> {
            if (result) {
                Util.getPlatform().openUri(url);
            }
            minecraft.popGuiLayer();
        }, url, true));
    }
}
