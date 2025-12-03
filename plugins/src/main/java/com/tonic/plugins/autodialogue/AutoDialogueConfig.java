package com.tonic.plugins.autodialogue;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(AutoDialogueConfig.GROUP)
public interface AutoDialogueConfig extends Config
{
    String GROUP = "autodialogue";

    @ConfigItem(
        keyName = "enabled",
        name = "Enable",
        description = "Enable Auto Dialogue Navigator"
    )
    default boolean enabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "autoContinue",
        name = "Auto-continue",
        description = "Automatically click through 'Click here to continue' style dialogues."
    )
    default boolean autoContinue()
    {
        return true;
    }

    @ConfigItem(
        keyName = "logContinues",
        name = "Log continues",
        description = "Log a message whenever a dialogue is auto-continued."
    )
    default boolean logContinues()
    {
        return false;
    }

    @ConfigItem(
        keyName = "autoSelectOptions",
        name = "Auto-select options",
        description = "Automatically select dialogue options based on rules."
    )
    default boolean autoSelectOptions()
    {
        return true;
    }

    @ConfigItem(
        keyName = "useQuestHelperHighlight",
        name = "Use Quest Helper highlight",
        description = "If Quest Helper is highlighting an option, select that option first."
    )
    default boolean useQuestHelperHighlight()
    {
        return true;
    }

    @ConfigItem(
        keyName = "preferredOptionKeywords",
        name = "Preferred keywords",
        description = "Keywords for options to auto-select (one per line or comma-separated)."
    )
    default String preferredOptionKeywords()
    {
        return "";
    }

    @ConfigItem(
        keyName = "selectFirstIfNoMatch",
        name = "Select first if no match",
        description = "If no keyword matches, select the first option."
    )
    default boolean selectFirstIfNoMatch()
    {
        return false;
    }

    @ConfigItem(
        keyName = "blockIfContains",
        name = "Block if text contains",
        description = "If dialogue text contains any of these substrings, do nothing (one per line or comma-separated)."
    )
    default String blockIfContains()
    {
        return "are you sure\nwarning\nthis action cannot be undone";
    }
}
