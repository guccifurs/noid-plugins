package com.tonic.plugins.gearswapper;

/**
 * Orientation for the Inventory Utils overlay layout
 */
public enum InventoryUtilsOrientation {
    VERTICAL("Vertical"),
    HORIZONTAL("Horizontal");

    private final String name;

    InventoryUtilsOrientation(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
