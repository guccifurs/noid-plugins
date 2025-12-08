package com.tonic.plugins.gearswapper;

import java.awt.image.BufferedImage;

/**
 * Data class representing a single button preset configuration
 */
public class ButtonPreset {
    public PresetType type = PresetType.NONE;
    public String value = "";
    public String customIconUrl = null;
    public boolean toggleMode = false; // For prayers: false=activate only, true=toggle on/off

    // Transient - not saved to JSON
    public transient BufferedImage cachedIcon = null;
    public transient boolean iconLoading = false;

    public ButtonPreset() {
    }

    public ButtonPreset(PresetType type, String value) {
        this.type = type;
        this.value = value;
    }

    public boolean isEmpty() {
        return type == PresetType.NONE || value == null || value.isEmpty();
    }

    public void clearCache() {
        cachedIcon = null;
        iconLoading = false;
    }
}
