package com.example.hudmenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HudMenuMod implements ClientModInitializer {
    public static final String MOD_ID = "hudmenu";

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        Modules.init();

        // Right Shift opens the menu (change it under Options > Controls).
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hudmenu.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) {
                if (mc.player != null && mc.currentScreen == null) {
                    mc.setScreen(new HudMenuScreen());
                }
            }
            Modules.AutoSprint.tick(mc);
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "modules"), (ctx, tickCounter) -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null) return;
                    Modules.CpsTracker.poll(mc);
                    if (mc.options.hudHidden || mc.currentScreen instanceof HudMenuScreen) return;
                    for (HudModule m : Modules.ALL) m.render(ctx, mc, false);
                });
    }
}
