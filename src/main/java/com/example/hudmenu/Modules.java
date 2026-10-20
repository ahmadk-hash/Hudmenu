package com.example.hudmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All HUD modules live here. To add one, write a class and add it in init(). */
final class Modules {
    static final List<HudModule> ALL = new ArrayList<>();
    static HudModule autoSprint;

    private Modules() {}

    static void init() {
        ALL.clear();
        ALL.add(new Fps());
        ALL.add(new Cps());
        ALL.add(new Ping());
        autoSprint = new AutoSprintHud();
        ALL.add(autoSprint);
        ALL.add(new Coordinates());
        ALL.add(new Keystrokes());
        ALL.add(new Potions());
    }

    // ------------------------------------------------------------------ helpers

    abstract static class TextModule extends HudModule {
        TextModule(String id, String name, int x, int y) {
            super(id, name, x, y);
        }

        abstract List<String> lines(MinecraftClient mc, boolean preview);

        @Override
        int contentWidth(MinecraftClient mc, boolean preview) {
            int w = 0;
            for (String l : lines(mc, preview)) w = Math.max(w, mc.textRenderer.getWidth(l));
            return w;
        }

        @Override
        int contentHeight(MinecraftClient mc, boolean preview) {
            int n = lines(mc, preview).size();
            return n == 0 ? 0 : n * 10 - 1;
        }

        @Override
        void drawContent(DrawContext ctx, MinecraftClient mc, boolean preview) {
            int i = 0;
            for (String l : lines(mc, preview)) text(ctx, mc, l, 0, 10 * i++);
        }
    }

    // ------------------------------------------------------------------ modules

    /** "Status": FPS counter. */
    static final class Fps extends TextModule {
        Fps() {
            super("fps", "FPS", 4, 4);
        }

        @Override
        List<String> lines(MinecraftClient mc, boolean preview) {
            return List.of("FPS: " + mc.getCurrentFps());
        }
    }

    static final class Cps extends TextModule {
        Cps() {
            super("cps", "CPS", 4, 22);
        }

        @Override
        List<String> lines(MinecraftClient mc, boolean preview) {
            return List.of("CPS: " + CpsTracker.left() + " | " + CpsTracker.right());
        }
    }

    static final class Ping extends TextModule {
        Ping() {
            super("ping", "Ping", 4, 40);
        }

        @Override
        List<String> lines(MinecraftClient mc, boolean preview) {
            int ms = 0;
            if (mc.player != null && mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) ms = e.getLatency();
            }
            return List.of("Ping: " + ms + " ms");
        }
    }

    /** Shows whether Auto Sprint is on. The sprinting itself is done by AutoSprint.tick(). */
    static final class AutoSprintHud extends TextModule {
        AutoSprintHud() {
            super("autosprint", "Auto Sprint", 4, 58);
        }

        @Override
        List<String> lines(MinecraftClient mc, boolean preview) {
            boolean sprinting = mc.player != null && mc.player.isSprinting();
            return List.of(sprinting ? "[Sprinting (Auto)]" : "[Auto Sprint]");
        }
    }

    static final class Coordinates extends TextModule {
        Coordinates() {
            super("coords", "Coordinates", 4, 76);
        }

        @Override
        List<String> lines(MinecraftClient mc, boolean preview) {
            if (mc.player == null) return List.of("X: 0", "Y: 0", "Z: 0");
            String f = mc.player.getHorizontalFacing().asString();
            f = Character.toUpperCase(f.charAt(0)) + f.substring(1);
            return List.of(
                    String.format(Locale.ROOT, "X: %.1f", mc.player.getX()),
                    String.format(Locale.ROOT, "Y: %.1f", mc.player.getY()),
                    String.format(Locale.ROOT, "Z: %.1f", mc.player.getZ()),
                    "Facing: " + f);
        }
    }

    static final class Potions extends TextModule {
        Potions() {
            super("potions", "Potion Effects", 110, 4);
        }

        private static String roman(int n) {
            String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
            return n > 0 && n < r.length ? r[n] : String.valueOf(n);
        }

        @Override
        List<String> lines(MinecraftClient mc, boolean preview) {
            List<String> out = new ArrayList<>();
            if (mc.player != null) {
                for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                    String name = e.getEffectType().value().getName().getString();
                    String time;
                    if (e.isInfinite()) {
                        time = "Inf";
                    } else {
                        int sec = e.getDuration() / 20;
                        time = String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
                    }
                    out.add(name + " " + roman(e.getAmplifier() + 1) + "  " + time);
                }
            }
            // While the menu is open, show sample lines so you can position it.
            if (out.isEmpty() && preview) {
                out.add("Speed II  1:30");
                out.add("Strength I  0:20");
            }
            return out;
        }
    }

    static final class Keystrokes extends HudModule {
        Keystrokes() {
            super("keystrokes", "Keystrokes", 4, 120);
        }

        @Override
        int contentWidth(MinecraftClient mc, boolean preview) {
            return 58;
        }

        @Override
        int contentHeight(MinecraftClient mc, boolean preview) {
            return 70;
        }

        @Override
        boolean ownBackground() {
            return true;
        }

        private void key(DrawContext ctx, MinecraftClient mc, String label, int x, int y, int w, int h, boolean down) {
            int bg = down ? argb(190, s.textColor) : argb(s.showBackground ? s.bgOpacity : 0, s.bgColor);
            ctx.fill(x, y, x + w, y + h, bg);
            int fg = down ? 0xFF000000 : argb(255, s.textColor);
            if (label.isEmpty()) {
                ctx.fill(x + 16, y + h / 2 - 1, x + w - 16, y + h / 2, fg);
            } else {
                int tw = mc.textRenderer.getWidth(label);
                ctx.drawText(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, fg, s.shadow && !down);
            }
        }

        @Override
        void drawContent(DrawContext ctx, MinecraftClient mc, boolean preview) {
            var o = mc.options;
            key(ctx, mc, "W", 20, 0, 18, 18, o.forwardKey.isPressed());
            key(ctx, mc, "A", 0, 20, 18, 18, o.leftKey.isPressed());
            key(ctx, mc, "S", 20, 20, 18, 18, o.backKey.isPressed());
            key(ctx, mc, "D", 40, 20, 18, 18, o.rightKey.isPressed());
            key(ctx, mc, "LMB", 0, 40, 28, 18, CpsTracker.leftDown);
            key(ctx, mc, "RMB", 30, 40, 28, 18, CpsTracker.rightDown);
            key(ctx, mc, "", 0, 60, 58, 10, o.jumpKey.isPressed());
        }
    }

    // ------------------------------------------------------------------ behaviour

    /** Counts mouse clicks. Polled every frame so fast clicking is not missed. */
    static final class CpsTracker {
        private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
        private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
        private static boolean prevL;
        private static boolean prevR;
        static boolean leftDown;
        static boolean rightDown;

        private CpsTracker() {}

        static void poll(MinecraftClient mc) {
            long h = mc.getWindow().getHandle();
            leftDown = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            rightDown = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            long now = System.currentTimeMillis();
            boolean count = mc.currentScreen == null;
            if (count && leftDown && !prevL) LEFT.addLast(now);
            if (count && rightDown && !prevR) RIGHT.addLast(now);
            prevL = leftDown;
            prevR = rightDown;
            trim(LEFT, now);
            trim(RIGHT, now);
        }

        private static void trim(ArrayDeque<Long> q, long now) {
            while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        }

        static int left() {
            return LEFT.size();
        }

        static int right() {
            return RIGHT.size();
        }
    }

    /** Holds the sprint key while you walk forward. Normal sprint rules (hunger, etc.) still apply. */
    static final class AutoSprint {
        private static boolean forcing;

        private AutoSprint() {}

        static void tick(MinecraftClient mc) {
            if (mc.player == null || autoSprint == null) return;
            boolean on = autoSprint.s.enabled && mc.currentScreen == null;
            boolean walking = mc.options.forwardKey.isPressed() && !mc.options.sneakKey.isPressed();
            if (on && walking) {
                mc.options.sprintKey.setPressed(true);
                forcing = true;
            } else if (forcing) {
                mc.options.sprintKey.setPressed(false);
                forcing = false;
            }
        }
    }
}
