package com.tonic.data;

import lombok.Getter;

@Getter
public enum Tab {
    COMBAT_TAB(4675, 0),
    EXP_TAB(4676, 1),
    QUESTS_TAB(4677, 2),
    INVENTORY_TAB(4678, 3),
    EQUIPMENT_TAB(4679, 4),
    PRAYER_TAB(4680, 5),
    SPELLBOOK_TAB(4682, 6),
    CLAN_TAB(4683, 7),
    FRIENDS_TAB(4684, 9),
    SETTINGS_TAB(4686, 11),
    EMOTES_TAB(4687, 12),
    MUSIC_TAB(4688, 13),
    LOGOUT_TAB(4689, 10),
    ACCOUNT_TAB(6517, 8);

    private final int hotkeyVarbit;
    private final int tabVarbit;

    Tab(int hotkeyVarbit, int tabVarbit)
    {
        this.hotkeyVarbit = hotkeyVarbit;
        this.tabVarbit = tabVarbit;
    }
}