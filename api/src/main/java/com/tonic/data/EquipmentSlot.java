package com.tonic.data;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of equipment slots in the inventory.
 */
@Getter
public enum EquipmentSlot
{
    HEAD(0, WidgetInfoExtended.EQUIPMENT_HELMET),
    CAPE(1, WidgetInfoExtended.EQUIPMENT_CAPE),
    AMULET(2, WidgetInfoExtended.EQUIPMENT_AMULET),
    WEAPON(3, WidgetInfoExtended.EQUIPMENT_WEAPON),
    BODY(4, WidgetInfoExtended.EQUIPMENT_BODY),
    SHIELD(5, WidgetInfoExtended.EQUIPMENT_SHIELD),
    LEGS(7, WidgetInfoExtended.EQUIPMENT_LEGS),
    GLOVES(9, WidgetInfoExtended.EQUIPMENT_GLOVES),
    BOOTS(10, WidgetInfoExtended.EQUIPMENT_BOOTS),
    RING(12, WidgetInfoExtended.EQUIPMENT_RING),
    AMMO(13, WidgetInfoExtended.EQUIPMENT_AMMO);

    private final int slotIdx;
    private final WidgetInfoExtended widgetInfo;

    EquipmentSlot(int slotIdx, WidgetInfoExtended widgetInfo)
    {
        this.slotIdx = slotIdx;
        this.widgetInfo = widgetInfo;
    }

    private static final Map<Integer, EquipmentSlot> map;

    static {
        map = new HashMap<>();
        for (EquipmentSlot v : EquipmentSlot.values()) {
            map.put(v.slotIdx, v);
        }
    }

    public static EquipmentSlot findBySlot(int i) {
        return map.get(i);
    }
}