package com.tonic.plugins.lmsnavigator;

import net.runelite.api.coords.WorldPoint;

public class FinalPath
{
    
    // Final LMS locations
    public static final WorldPoint MOSER_SETTLEMENT = new WorldPoint(3474, 5788, 0);
    public static final WorldPoint DEBTORS_HIDEOUT = new WorldPoint(3404, 5802, 0);
    public static final WorldPoint THE_MOUNTAIN = new WorldPoint(3430, 5845, 0);
    public static final WorldPoint TRINITY_OUTPOST = new WorldPoint(3500, 5870, 0);
    public static final WorldPoint STONE_CIRCLE = new WorldPoint(3663, 6061, 0);
    public static final WorldPoint PILLARS_OF_SACRIFICE = new WorldPoint(3594, 6164, 0);
    public static final WorldPoint DARK_WARRIORS = new WorldPoint(3546, 6158, 0);
    public static final WorldPoint TRADING_POST = new WorldPoint(3506, 6162, 0);
    public static final WorldPoint HUT = new WorldPoint(3497, 6704, 0);
    public static final WorldPoint BLANK_TOWER = new WorldPoint(3603, 6102, 0);
    public static final WorldPoint TOWN_CITY = new WorldPoint(3551, 6108, 0);
    public static final WorldPoint OLD_MANOR = new WorldPoint(3497, 6117, 0);
    
    public static WorldPoint getLocationByName(String locationName)
    {
        switch (locationName.toLowerCase())
        {
            case "moser settlement":
                return MOSER_SETTLEMENT;
            case "debtors hideout":
                return DEBTORS_HIDEOUT;
            case "the mountain":
                return THE_MOUNTAIN;
            case "trinity outpost":
                return TRINITY_OUTPOST;
            case "stone circle":
                return STONE_CIRCLE;
            case "pillars of sacrafice":
                return PILLARS_OF_SACRIFICE;
            case "dark warrior's":
                return DARK_WARRIORS;
            case "trading post":
                return TRADING_POST;
            case "hut":
                return HUT;
            case "blank tower":
                return BLANK_TOWER;
            case "town center":
                return TOWN_CITY;
            case "old manor":
                return OLD_MANOR;
            default:
                return null;
        }
    }
    
    public static String[] getAllLocationNames()
    {
        return new String[]{
            "Moser Settlement",
            "Debtors Hideout", 
            "The Mountain",
            "Trinity Outpost",
            "Stone Circle",
            "Pillars of Sacrafice",
            "Dark Warrior's",
            "Trading Post",
            "Hut",
            "Blank Tower",
            "Town Center",
            "Old Manor"
        };
    }
}