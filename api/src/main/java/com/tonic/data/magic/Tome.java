package com.tonic.data.magic;

import net.runelite.api.gameval.ItemID;

public enum Tome {
    TOME_OF_FIRE(ItemID.TOME_OF_FIRE, Rune.FIRE),
    TOME_OF_WATER(ItemID.TOME_OF_WATER, Rune.WATER);

    private final int tomeId;
    private final Rune rune;

    Tome(int tomeId, Rune rune){
        this.tomeId = tomeId;
        this.rune = rune;
    }

    public static boolean isUnlimitedRuneSource(int itemId, Rune rune){
        for (var tome : Tome.values()) {
            if(tome.tomeId == itemId){
                return tome.rune == rune;
            }
        }

        return false;
    }
}