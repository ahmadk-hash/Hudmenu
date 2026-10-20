package com.example.hudmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Base class for every HUD element. Position, size and colors come from its ModuleSettings. */
abstract class HudModule {
    static final int PAD = 3;

    final String id;
    final String name;
    final int defX;
    final int defY;
    final Config.ModuleSettings s;

    HudModule(String id, String name, int defX, int defY) {
        this.id = id;
        this.name = name;
        this.defX = defX;
        this.defY = defY;
        this.s = Config.get(id, defX, defY);
    }

    abstract int contentWidth(MinecraftClient mc, boolean preview);

    abstract int contentHeight(MinecraftClient mc, boolean preview);

    abstract void drawContent(DrawContext ctx, MinecraftClient mc, boolean preview);

    /** Modules that draw their own boxes (keystrokes) skip the shared background. */
    boolean ownBackground() {
        return false;
    }

    static int argb(int alpha, int rgb) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    void text(DrawContext ctx, MinecraftClient mc, String str, int x, int y) {
        ctx.drawText(mc.textRenderer, str, x, y, argb(255, s.textColor), s.shadow);
    }

    /** {x, y, width, height} on screen, after scaling. Used for drag and drop. */
    int[] bounds(MinecraftClient mc, boolean preview) {
        int w = Math.round((contentWidth(mc, preview) + PAD * 2) * s.scale);
        int h = Math.round((contentHeight(mc, preview) + PAD * 2) * s.scale);
        return new int[]{s.x, s.y, w, h};
    }

    void render(DrawContext ctx, MinecraftClient mc, boolean preview) {
        if (!s.enabled) return;
        int cw = contentWidth(mc, preview);
        int ch = contentHeight(mc, preview);
        if (cw <= 0 || ch <= 0) return;

        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) s.x, (float) s.y);
        m.scale(s.scale, s.scale);
        if (s.showBackground && !ownBackground()) {
            ctx.fill(0, 0, cw + PAD * 2, ch + PAD * 2, argb(s.bgOpacity, s.bgColor));
        }
        m.translate((float) PAD, (float) PAD);
        drawContent(ctx, mc, preview);
        m.popMatrix();
    }
}
