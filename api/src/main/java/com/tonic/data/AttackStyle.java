package com.tonic.data;

import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

import java.util.Arrays;

@Getter
public enum AttackStyle
{
    FIRST(0, InterfaceID.CombatInterface._0),
    SECOND(1, InterfaceID.CombatInterface._1),
    THIRD(2, InterfaceID.CombatInterface._2),
    FOURTH(3, InterfaceID.CombatInterface._3),
    SPELLS(4, InterfaceID.CombatInterface.AUTOCAST_NORMAL),
    SPELLS_DEFENSIVE(4, InterfaceID.CombatInterface.AUTOCAST_DEFENSIVE),
    UNKNOWN(-1, -1);

    private final int index;
    private final int interfaceId;

    AttackStyle(int index, int interfaceId)
    {
        this.index = index;
        this.interfaceId = interfaceId;
    }

    public static AttackStyle fromIndex(int index)
    {
        return Arrays.stream(values()).filter(x -> x.index == index)
                .findFirst()
                .orElse(UNKNOWN);
    }
}