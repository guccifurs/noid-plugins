package com.tonic.plugins.autologin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(AutoLoginConfig.GROUP)
public interface AutoLoginConfig extends Config
{
    String GROUP = "autologin";

    @ConfigItem(
        keyName = "username",
        name = "Username",
        description = "Username to auto-login with"
    )
    default String username()
    {
        return "";
    }

    @ConfigItem(
        keyName = "password",
        name = "Password",
        description = "Password to auto-login with",
        secret = true
    )
    default String password()
    {
        return "";
    }

    @ConfigItem(
        keyName = "autoEat",
        name = "Auto Eat",
        description = "Automatically eat food when HP is at or below the threshold"
    )
    default boolean autoEat()
    {
        return false;
    }

    @Range(min = 1, max = 99)
    @ConfigItem(
        keyName = "autoEatHpThreshold",
        name = "Auto Eat HP threshold",
        description = "Eat when current HP is at or below this value (hitpoints, not %)"
    )
    default int autoEatHpThreshold()
    {
        return 40;
    }
}
