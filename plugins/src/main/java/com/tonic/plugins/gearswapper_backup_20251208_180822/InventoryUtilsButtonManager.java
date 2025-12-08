package com.tonic.plugins.gearswapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tonic.Logger;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages button presets for Inventory Utils overlay.
 * Handles JSON persistence and wiki icon loading.
 */
@Singleton
public class InventoryUtilsButtonManager {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.runelite/gearswapper";
    private static final String CONFIG_FILE = CONFIG_DIR + "/inventory_utils_buttons.json";
    private static final String WIKI_BASE_URL = "https://oldschool.runescape.wiki/images/";
    private static final int MAX_BUTTONS = 10;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ExecutorService iconLoader = Executors.newSingleThreadExecutor();
    private final Map<String, BufferedImage> iconCache = new HashMap<>();

    private List<ButtonPreset> buttons = new ArrayList<>();

    @Inject
    public InventoryUtilsButtonManager() {
        // Initialize with empty buttons
        for (int i = 0; i < MAX_BUTTONS; i++) {
            buttons.add(new ButtonPreset());
        }
        load();
    }

    public ButtonPreset getButton(int index) {
        if (index < 0 || index >= buttons.size()) {
            return new ButtonPreset();
        }
        return buttons.get(index);
    }

    public void setButton(int index, ButtonPreset preset) {
        if (index < 0 || index >= MAX_BUTTONS)
            return;

        while (buttons.size() <= index) {
            buttons.add(new ButtonPreset());
        }

        buttons.set(index, preset);
        preset.clearCache();
        save();
        loadIconAsync(preset);
    }

    public void clearButton(int index) {
        setButton(index, new ButtonPreset());
    }

    public void load() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            Logger.norm("[InventoryUtils] No config file found, using defaults");
            return;
        }

        try (Reader reader = new FileReader(configFile)) {
            Type listType = new TypeToken<List<ButtonPreset>>() {
            }.getType();
            List<ButtonPreset> loaded = gson.fromJson(reader, listType);

            if (loaded != null) {
                buttons.clear();
                buttons.addAll(loaded);

                while (buttons.size() < MAX_BUTTONS) {
                    buttons.add(new ButtonPreset());
                }

                for (ButtonPreset button : buttons) {
                    if (!button.isEmpty()) {
                        loadIconAsync(button);
                    }
                }

                Logger.norm("[InventoryUtils] Loaded " + buttons.size() + " button configs");
            }
        } catch (Exception e) {
            Logger.error("[InventoryUtils] Error loading config: " + e.getMessage());
        }
    }

    public void save() {
        try {
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            try (Writer writer = new FileWriter(CONFIG_FILE)) {
                gson.toJson(buttons, writer);
            }

            Logger.norm("[InventoryUtils] Saved button configs");
        } catch (Exception e) {
            Logger.error("[InventoryUtils] Error saving config: " + e.getMessage());
        }
    }

    public void shutdown() {
        iconLoader.shutdown();
    }

    public void loadIconAsync(ButtonPreset preset) {
        if (preset.isEmpty() || preset.iconLoading || preset.cachedIcon != null) {
            return;
        }

        preset.iconLoading = true;

        iconLoader.submit(() -> {
            try {
                String iconUrl = getIconUrl(preset);
                if (iconUrl == null) {
                    preset.iconLoading = false;
                    return;
                }

                if (iconCache.containsKey(iconUrl)) {
                    preset.cachedIcon = iconCache.get(iconUrl);
                    preset.iconLoading = false;
                    return;
                }

                URL url = new URL(iconUrl);
                BufferedImage image = ImageIO.read(url);

                if (image != null) {
                    iconCache.put(iconUrl, image);
                    preset.cachedIcon = image;
                    Logger.norm("[InventoryUtils] Loaded icon: " + iconUrl);
                }
            } catch (Exception e) {
                Logger.warn("[InventoryUtils] Failed to load icon for " + preset.value + ": " + e.getMessage());
            } finally {
                preset.iconLoading = false;
            }
        });
    }

    private String getIconUrl(ButtonPreset preset) {
        if (preset.customIconUrl != null && !preset.customIconUrl.isEmpty()) {
            return preset.customIconUrl;
        }

        if (preset.type == PresetType.PRAYER) {
            return getPrayerIconUrl(preset.value);
        }

        return null;
    }

    private String getPrayerIconUrl(String prayerName) {
        if (prayerName == null || prayerName.isEmpty())
            return null;

        String formatted = prayerName.trim()
                .replace(" ", "_")
                .replace("'", "%27");

        return WIKI_BASE_URL + formatted + ".png";
    }

    public static String[] getAllPrayerNames() {
        return new String[] {
                "Thick Skin", "Burst of Strength", "Clarity of Thought", "Sharp Eye", "Mystic Will",
                "Rock Skin", "Superhuman Strength", "Improved Reflexes", "Rapid Restore", "Rapid Heal",
                "Protect Item", "Hawk Eye", "Mystic Lore", "Steel Skin", "Ultimate Strength",
                "Incredible Reflexes", "Protect from Magic", "Protect from Missiles", "Protect from Melee",
                "Eagle Eye", "Mystic Might", "Retribution", "Redemption", "Smite",
                "Preserve", "Chivalry", "Piety", "Rigour", "Augury"
        };
    }
}
