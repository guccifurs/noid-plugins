package com.tonic.plugins.hellowebhook;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("hellowebhook")
public interface HelloWebhookConfig extends Config
{
    @ConfigItem(
            name = "Enabled",
            keyName = "enabled",
            description = "Enable the hello.txt Discord webhook on first startup",
            position = 0
    )
    default boolean enabled()
    {
        return true;
    }

    @ConfigItem(
            name = "Has Sent Welcome",
            keyName = "hasSentWelcome",
            description = "Internal flag to ensure the welcome message is only sent once",
            position = 1
    )
    default boolean hasSentWelcome()
    {
        return false;
    }

    @ConfigItem(
            name = "",
            keyName = "hasSentWelcome",
            description = "",
            position = 2
    )
    default void setHasSentWelcome(boolean value)
    {
    }
}
