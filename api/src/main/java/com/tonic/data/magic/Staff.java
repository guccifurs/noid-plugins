package com.tonic.data.magic;

import net.runelite.api.gameval.ItemID;

import java.util.Arrays;

public enum Staff {
    AIR(ItemID.STAFF_OF_AIR, Rune.AIR),
    WATER(ItemID.STAFF_OF_WATER, Rune.WATER),
    EARTH(ItemID.STAFF_OF_EARTH, Rune.EARTH),
    FIRE(ItemID.STAFF_OF_FIRE, Rune.FIRE),
    AIR_BATTLESTAFF(ItemID.AIR_BATTLESTAFF, Rune.AIR),
    WATER_BATTLESTAFF(ItemID.WATER_BATTLESTAFF, Rune.WATER),
    EARTH_BATTLESTAFF(ItemID.EARTH_BATTLESTAFF, Rune.EARTH),
    FIRE_BATTLESTAFF(ItemID.FIRE_BATTLESTAFF, Rune.FIRE),
    MYSTIC_AIR_STAFF(ItemID.MYSTIC_AIR_STAFF, Rune.AIR),
    MYSTIC_WATER_STAFF(ItemID.MYSTIC_WATER_STAFF, Rune.WATER),
    MYSTIC_EARTH_STAFF(ItemID.MYSTIC_EARTH_STAFF, Rune.EARTH),
    MYSTIC_FIRE_STAFF(ItemID.MYSTIC_FIRE_STAFF, Rune.FIRE),
    LAVA_BATTLESTAFF(ItemID.LAVA_BATTLESTAFF, Rune.FIRE, Rune.EARTH),
    MUD_BATTLESTAFF(ItemID.MUD_BATTLESTAFF, Rune.WATER, Rune.EARTH),
    STEAM_BATTLESTAFF(ItemID.STEAM_BATTLESTAFF, Rune.FIRE, Rune.WATER),
    SMOKE_BATTLESTAFF(ItemID.SMOKE_BATTLESTAFF, Rune.FIRE, Rune.AIR),
    MIST_BATTLESTAFF(ItemID.MIST_BATTLESTAFF, Rune.AIR, Rune.WATER),
    DUST_BATTLESTAFF(ItemID.DUST_BATTLESTAFF, Rune.AIR, Rune.EARTH),
    MYSTIC_LAVA_STAFF(ItemID.MYSTIC_LAVA_STAFF, Rune.FIRE, Rune.EARTH),
    MYSTIC_MUD_STAFF(ItemID.MYSTIC_MUD_STAFF, Rune.WATER, Rune.EARTH),
    MYSTIC_STEAM_STAFF(ItemID.MYSTIC_STEAM_BATTLESTAFF, Rune.FIRE, Rune.WATER),
    MYSTIC_SMOKE_STAFF(ItemID.MYSTIC_SMOKE_BATTLESTAFF, Rune.FIRE, Rune.AIR),
    MYSTIC_MIST_STAFF(ItemID.MYSTIC_MIST_BATTLESTAFF, Rune.AIR, Rune.WATER),
    MYSTIC_DUST_STAFF(ItemID.MYSTIC_DUST_BATTLESTAFF, Rune.AIR, Rune.EARTH),
    BRYOPHYTAS_STAFF(ItemID.NATURE_STAFF_CHARGED, Rune.NATURE);

    private final int staffId;
    private final Rune[] runes;

    Staff(int staffId, Rune... runes){
        this.staffId = staffId;
        this.runes = runes;
    }

    public static boolean isUnlimitedRuneSource(int itemId, Rune rune){
        for (var staff : Staff.values()) {
            if(staff.staffId == itemId){
                return Arrays.stream(staff.runes).anyMatch(p -> p == rune);
            }
        }

        return false;
    }
}