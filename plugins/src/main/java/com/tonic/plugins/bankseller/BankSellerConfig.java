package com.tonic.plugins.bankseller;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("bankseller")
public interface BankSellerConfig extends Config {

    @ConfigSection(name = "Settings", description = "Plugin settings", position = 0)
    String settingsSection = "settings";

    @ConfigItem(keyName = "enabled", name = "Enable Selling", description = "Start/stop the bank seller", position = 0, section = settingsSection)
    default boolean enabled() {
        return false;
    }

    @ConfigItem(keyName = "minPrice", name = "Min Price", description = "Minimum item value to sell (items below this are skipped)", position = 1, section = settingsSection)
    default int minPrice() {
        return 100;
    }

    @ConfigItem(keyName = "pricePercentage", name = "Price %", description = "Percentage of guide price to sell at (e.g. 95 = 5% under)", position = 2, section = settingsSection)
    default int pricePercentage() {
        return 90;
    }

    @ConfigItem(keyName = "excludedItems", name = "Excluded Items", description = "Comma-separated list of item names to never sell", position = 3, section = settingsSection)
    default String excludedItems() {
        return "Coins,Bond";
    }

    @ConfigItem(keyName = "tickDelay", name = "Tick Delay", description = "Delay between actions (0 = 1-tick fast mode, 3 = normal)", position = 4, section = settingsSection)
    default int tickDelay() {
        return 2;
    }
}
