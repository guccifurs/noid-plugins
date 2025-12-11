package com.tonic.plugins.gearswapper;

/**
 * Type of preset action for Inventory Utils overlay
 */
public enum PresetType {
    NONE("None"),
    LOADOUT("Loadout"),
    PRAYER("Prayer");

    private final String name;

    PresetType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
