package com.tonic.data;

import net.runelite.api.widgets.WidgetID;
import java.util.Arrays;

public enum WidgetInfoExtended {
    DIALOG_OPTION_OPTION1(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS),
    DIALOG2_SPRITE_TEXT(11, 2),
    MINIGAME_DIALOG_TEXT(229, MinigameDialog.TEXT),
    MINIGAME_DIALOG_CONTINUE(229, MinigameDialog.CONTINUE),
    DIALOG_NOTIFICATION_TEXT(229, DialogNotification.TEXT),
    DIALOG_NPC_CONTINUE(WidgetID.DIALOG_NPC_GROUP_ID, DialogNPC.CONTINUE),
    DIALOG_PLAYER_CONTINUE(WidgetID.DIALOG_PLAYER_GROUP_ID, DialogPlayer.CONTINUE),
    DIALOG2_SPRITE(11, 0),
    DIALOG2_SPRITE_CONTINUE(11, DialogSprite2.CONTINUE),
    DIALOG_NOTIFICATION_CONTINUE(229, DialogNotification.CONTINUE),
    LEVEL_UP_CONTINUE(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE),


    EQUIPMENT_HELMET(WidgetID.EQUIPMENT_GROUP_ID, Equipment.HELMET),
    EQUIPMENT_CAPE(WidgetID.EQUIPMENT_GROUP_ID, Equipment.CAPE),
    EQUIPMENT_AMULET(WidgetID.EQUIPMENT_GROUP_ID, Equipment.AMULET),
    EQUIPMENT_WEAPON(WidgetID.EQUIPMENT_GROUP_ID, Equipment.WEAPON),
    EQUIPMENT_BODY(WidgetID.EQUIPMENT_GROUP_ID, Equipment.BODY),
    EQUIPMENT_SHIELD(WidgetID.EQUIPMENT_GROUP_ID, Equipment.SHIELD),
    EQUIPMENT_LEGS(WidgetID.EQUIPMENT_GROUP_ID, Equipment.LEGS),
    EQUIPMENT_GLOVES(WidgetID.EQUIPMENT_GROUP_ID, Equipment.GLOVES),
    EQUIPMENT_BOOTS(WidgetID.EQUIPMENT_GROUP_ID, Equipment.BOOTS),
    EQUIPMENT_RING(WidgetID.EQUIPMENT_GROUP_ID, Equipment.RING),
    EQUIPMENT_AMMO(WidgetID.EQUIPMENT_GROUP_ID, Equipment.AMMO),

    WORLD_SWITCHER_LOGOUT_BUTTON(4522009),
    ;

    private final int id;

    WidgetInfoExtended(int id)
    {
        this.id = id;
    }

    WidgetInfoExtended(int groupId, int childId)
    {
        this.id = (groupId << 16) | childId;
    }

    /**
     * Gets the ID of the group-child pairing.
     *
     * @return the ID
     */
    public int getId()
    {
        return id;
    }

    /**
     * Gets the group ID of the pair.
     *
     * @return the group ID
     */
    public int getGroupId()
    {
        return id >> 16;
    }

    /**
     * Gets the ID of the child in the group.
     *
     * @return the child ID
     */
    public int getChildId()
    {
        return id & 0xffff;
    }

    /**
     * Gets the packed widget ID.
     *
     * @return the packed ID
     */
    public int getPackedId()
    {
        return id;
    }

    /**
     * Utility method that converts an ID returned by {@link #getId()} back
     * to its group ID.
     *
     * @param id passed group-child ID
     * @return the group ID
     */
    public static int TO_GROUP(int id)
    {
        return id >>> 16;
    }

    /**
     * Utility method that converts an ID returned by {@link #getId()} back
     * to its child ID.
     *
     * @param id passed group-child ID
     * @return the child ID
     */
    public static int TO_CHILD(int id)
    {
        return id & 0xFFFF;
    }

    /**
     * Packs the group and child IDs into a single integer.
     *
     * @param groupId the group ID
     * @param childId the child ID
     * @return the packed ID
     */
    public static int PACK(int groupId, int childId)
    {
        return groupId << 16 | childId;
    }

    public static WidgetInfoExtended ofId(int packed)
    {
        int childId = packed & 0xFFFF;
        int groupId = packed >>> 16;
        return of(groupId, childId);
    }

    public static WidgetInfoExtended of(int groupId, int childId)
    {
        return Arrays.stream(WidgetInfoExtended.values()).filter(w -> w.getChildId() == childId && w.getGroupId() == groupId).findFirst().orElse(null);
    }

    static class DialogOption
    {
        static final int OPTIONS = 1;
    }

    static class DialogNPC
    {
        static final int HEAD_MODEL = 2;
        static final int NAME = 4;
        static final int CONTINUE = 5;
        static final int TEXT = 6;
    }

    static class DialogPlayer
    {
        static final int HEAD_MODEL = 2;
        static final int NAME = 4;
        static final int CONTINUE = 5;
        static final int TEXT = 6;
    }

    static class DialogSprite2
    {
        static final int SPRITE1 = 1;
        static final int TEXT = 2;
        static final int SPRITE2 = 3;
        static final int CONTINUE = 4;
    }

    static class MinigameDialog
    {
        static final int TEXT = 1;
        static final int CONTINUE = 2;
    }

    static class DialogNotification
    {
        static final int TEXT = 0;
        static final int CONTINUE = 1;
    }

    static class LevelUp
    {
        static final int SKILL = 1;
        static final int LEVEL = 2;
        static final int CONTINUE = 3;
    }

    static class Equipment
    {
        static final int STATS = 1;
        static final int PRICES = 3;
        static final int DEATH = 5;
        static final int FOLLOWER = 7;
        static final int HELMET = 15;
        static final int CAPE = 16;
        static final int AMULET = 17;
        static final int WEAPON = 18;
        static final int BODY = 19;
        static final int SHIELD = 20;
        static final int LEGS = 21;
        static final int GLOVES = 22;
        static final int BOOTS = 23;
        static final int RING = 24;
        static final int AMMO = 25;

        static final int INVENTORY_ITEM_CONTAINER = 0;
    }
}
