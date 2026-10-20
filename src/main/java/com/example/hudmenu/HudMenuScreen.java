package com.example.hudmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * The mod menu. Left: list of modules. Right: settings for the selected module.
 * Drag any HUD element on screen to move it.
 *
 * Mouse input is read straight from GLFW inside render(), which keeps this class
 * independent of Minecraft's changing mouse-event method signatures.
 */
public class HudMenuScreen extends Screen {
    private static final int PW = 340;
    private static final int PH = 240;
    private static final int ROW = 13;

    private static final int BG = 0xF0141420;
    private static final int BORDER = 0xFF3A3A55;
    private static final int ACCENT = 0xFF4F8CFF;
    private static final int GREEN = 0xFF3DDC84;
    private static final int RED = 0xFFE5534B;
    private static final int DIM = 0xFFAAAAC0;
    private static final int WHITE = 0xFFFFFFFF;

    private int px;
    private int py;
    private int mx;
    private int my;
    private int selected = 0;

    private boolean down;
    private boolean pressed;
    private boolean prevDown;
    private boolean mouseOnPanel;

    private HudModule dragging;
    private int dragOffX;
    private int dragOffY;
    private String activeSlider;

    public HudMenuScreen() {
        super(Text.literal("HUD Menu"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        px = (width - PW) / 2;
        py = (height - PH) / 2;
        mx = mouseX;
        my = mouseY;

        Modules.CpsTracker.poll(client);
        down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        pressed = down && !prevDown;
        prevDown = down;
        if (!down) activeSlider = null;
        mouseOnPanel = inside(mx, my, px, py, PW, PH);

        // Plain dim instead of the blurred vanilla background, so HUD elements stay readable.
        ctx.fill(0, 0, width, height, 0x70000000);

        updateDrag();
        for (HudModule m : Modules.ALL) m.render(ctx, client, true);

        HudModule sel = Modules.ALL.get(selected);
        if (sel.s.enabled) {
            int[] b = sel.bounds(client, true);
            outline(ctx, b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, ACCENT);
        }

        drawPanel(ctx);
    }

    // ------------------------------------------------------------------ dragging

    private void updateDrag() {
        if (pressed && !mouseOnPanel) {
            for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
                HudModule m = Modules.ALL.get(i);
                if (!m.s.enabled) continue;
                int[] b = m.bounds(client, true);
                if (inside(mx, my, b[0], b[1], b[2], b[3])) {
                    dragging = m;
                    selected = i;
                    dragOffX = mx - b[0];
                    dragOffY = my - b[1];
                    break;
                }
            }
        }
        if (dragging != null) {
            if (down) {
                int[] b = dragging.bounds(client, true);
                dragging.s.x = Math.max(0, Math.min(mx - dragOffX, Math.max(0, width - b[2])));
                dragging.s.y = Math.max(0, Math.min(my - dragOffY, Math.max(0, height - b[3])));
            } else {
                dragging = null;
                Config.save();
            }
        }
    }

    // ------------------------------------------------------------------ panel

    private void drawPanel(DrawContext ctx) {
        ctx.fill(px, py, px + PW, py + PH, BG);
        outline(ctx, px, py, PW, PH, BORDER);

        ctx.drawText(textRenderer, "HUD Menu", px + 8, py + 8, WHITE, true);
        String hint = "Drag HUD elements to move them  |  ESC to close";
        ctx.drawText(textRenderer, hint, px + PW - 8 - textRenderer.getWidth(hint), py + 8, DIM, false);

        // Module list (click a name to select, click the dot to turn it on/off)
        for (int i = 0; i < Modules.ALL.size(); i++) {
            HudModule m = Modules.ALL.get(i);
            int rx = px + 8;
            int ry = py + 24 + i * 22;
            int rw = 104;
            boolean hover = inside(mx, my, rx, ry, rw, 20);
            ctx.fill(rx, ry, rx + rw, ry + 20, i == selected ? 0xFF2A3A66 : (hover ? 0xFF222233 : 0xFF1A1A28));
            ctx.drawText(textRenderer, m.name, rx + 6, ry + 6, WHITE, false);
            int dx = rx + rw - 14;
            ctx.fill(dx, ry + 6, dx + 8, ry + 14, m.s.enabled ? GREEN : RED);
            if (pressed && hover) {
                if (mx >= dx - 2) m.s.enabled = !m.s.enabled;
                else selected = i;
            }
        }

        // Settings for the selected module
        HudModule m = Modules.ALL.get(selected);
        Config.ModuleSettings s = m.s;
        int x = px + 124;
        int w = PW - 124 - 8;
        int y = py + 24;

        ctx.drawText(textRenderer, m.name + " settings", x, y, ACCENT, false);
        y += ROW;

        s.enabled = toggle(ctx, "Enabled", x, y, w, s.enabled);
        y += ROW;

        float sc = slider(ctx, "scale", "Size", x, y, w, s.scale, 0.5f, 3.0f,
                String.format("%.2fx", s.scale), ACCENT);
        s.scale = Math.round(sc * 20f) / 20f;
        y += ROW;

        s.shadow = toggle(ctx, "Text shadow", x, y, w, s.shadow);
        y += ROW;

        heading(ctx, "Text color", x, y, w, s.textColor);
        y += ROW;
        s.textColor = colorSliders(ctx, "t", x, y, w, s.textColor);
        y += ROW * 3;

        s.showBackground = toggle(ctx, "Background", x, y, w, s.showBackground);
        y += ROW;

        heading(ctx, "Background color", x, y, w, s.bgColor);
        y += ROW;
        s.bgColor = colorSliders(ctx, "b", x, y, w, s.bgColor);
        y += ROW * 3;

        float op = slider(ctx, "opacity", "Opacity", x, y, w, s.bgOpacity, 0, 255,
                Math.round(s.bgOpacity / 255f * 100f) + "%", 0xFF8888AA);
        s.bgOpacity = Math.round(op);
        y += ROW;

        if (button(ctx, "Reset position", x, y + 2, 100)) {
            s.x = m.defX;
            s.y = m.defY;
        }
    }

    // ------------------------------------------------------------------ widgets

    private void heading(DrawContext ctx, String label, int x, int y, int w, int rgb) {
        ctx.drawText(textRenderer, label, x, y + 1, DIM, false);
        ctx.fill(x + w - 30, y, x + w, y + 10, 0xFF000000 | rgb);
        outline(ctx, x + w - 30, y, 30, 10, BORDER);
    }

    private boolean toggle(DrawContext ctx, String label, int x, int y, int w, boolean value) {
        boolean hover = inside(mx, my, x, y, w, ROW - 1);
        ctx.drawText(textRenderer, label, x, y + 1, WHITE, false);
        int bx = x + w - 30;
        ctx.fill(bx, y, bx + 30, y + 10, value ? GREEN : RED);
        String t = value ? "ON" : "OFF";
        ctx.drawText(textRenderer, t, bx + (30 - textRenderer.getWidth(t)) / 2, y + 1, 0xFF000000, false);
        if (pressed && hover) value = !value;
        return value;
    }

    private boolean button(DrawContext ctx, String label, int x, int y, int w) {
        boolean hover = inside(mx, my, x, y, w, 12);
        ctx.fill(x, y, x + w, y + 12, hover ? 0xFF33334D : 0xFF26263A);
        outline(ctx, x, y, w, 12, BORDER);
        ctx.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + 2, WHITE, false);
        return pressed && hover;
    }

    private float slider(DrawContext ctx, String id, String label, int x, int y, int w,
                         float value, float min, float max, String valueText, int fill) {
        int tx = x + 56;
        int tw = w - 56;
        int th = ROW - 2;
        boolean hover = inside(mx, my, tx, y, tw, th);
        if (pressed && hover && dragging == null) activeSlider = id;
        if (id.equals(activeSlider)) {
            float t = Math.max(0f, Math.min(1f, (mx - tx) / (float) tw));
            value = min + t * (max - min);
        }
        float t = (value - min) / (max - min);
        ctx.fill(tx, y, tx + tw, y + th, 0xFF22222E);
        ctx.fill(tx, y, tx + Math.round(tw * t), y + th, fill);
        ctx.drawText(textRenderer, label, x, y + 1, WHITE, false);
        ctx.drawText(textRenderer, valueText, tx + (tw - textRenderer.getWidth(valueText)) / 2, y + 1, WHITE, true);
        return value;
    }

    private int colorSliders(DrawContext ctx, String id, int x, int y, int w, int color) {
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        r = Math.round(slider(ctx, id + "r", "Red", x, y, w, r, 0, 255, String.valueOf(r), 0xFFE05555));
        g = Math.round(slider(ctx, id + "g", "Green", x, y + ROW, w, g, 0, 255, String.valueOf(g), 0xFF4CAF50));
        b = Math.round(slider(ctx, id + "b", "Blue", x, y + ROW * 2, w, b, 0, 255, String.valueOf(b), 0xFF4F8CFF));
        return (r << 16) | (g << 8) | b;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col);
        c.fill(x + w - 1, y, x + w, y + h, col);
    }
}
