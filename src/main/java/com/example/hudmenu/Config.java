package com.example.hudmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Saves every module's settings to config/hudmenu.json. */
public final class Config {
    public static class ModuleSettings {
        public boolean enabled = true;
        public int x;
        public int y;
        public float scale = 1.0f;
        public int textColor = 0xFFFFFF;   // RGB
        public boolean shadow = true;
        public boolean showBackground = true;
        public int bgColor = 0x000000;     // RGB
        public int bgOpacity = 110;        // 0-255
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, ModuleSettings> map = new LinkedHashMap<>();

    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("hudmenu.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                Map<String, ModuleSettings> loaded = GSON.fromJson(
                        Files.readString(f),
                        new TypeToken<Map<String, ModuleSettings>>() {}.getType());
                if (loaded != null) map = loaded;
            }
        } catch (Exception e) {
            System.err.println("[hudmenu] Could not read config, using defaults: " + e);
        }
    }

    public static ModuleSettings get(String id, int defX, int defY) {
        return map.computeIfAbsent(id, k -> {
            ModuleSettings s = new ModuleSettings();
            s.x = defX;
            s.y = defY;
            return s;
        });
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(map));
        } catch (Exception e) {
            System.err.println("[hudmenu] Could not save config: " + e);
        }
    }
}
