package com.tonic.plugins.noidbets;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("noidbets")
public interface NoidBetsConfig extends Config
{
    @ConfigItem(
            name = "Enabled",
            keyName = "enabled",
            description = "Enable Noid Bets duel reporting and linking",
            position = 0
    )
    default boolean enabled()
    {
        return false;
    }

    @ConfigItem(
            name = "Discord Bot API URL",
            keyName = "discordBotApiUrl",
            description = "URL of the Noid Bets Discord bot HTTP server (e.g. http://localhost:8081)",
            position = 1
    )
    default String discordBotApiUrl()
    {
        return "http://localhost:8081";
    }

    @ConfigItem(
            name = "Target RSN",
            keyName = "targetRsn",
            description = "RSN of the opponent this client should challenge in the arena (per-client)",
            position = 2
    )
    default String targetRsn()
    {
        return "";
    }

    @ConfigItem(
            name = "Host Reporter",
            keyName = "hostReporter",
            description = "If enabled, this client will act as the host and report duel rounds/results to Discord.",
            position = 3
    )
    default boolean hostReporter()
    {
        return false;
    }

    @ConfigItem(
            name = "Deposit/withdraw bot mode",
            keyName = "depositWithdrawMode",
            description = "If enabled, this client will act as a GP deposit/withdraw bot for linked users via trades.",
            position = 4
    )
    default boolean depositWithdrawMode()
    {
        return false;
    }
}
