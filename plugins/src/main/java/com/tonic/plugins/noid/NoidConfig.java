package com.tonic.plugins.noid;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;

/**
 * Empty config - NoidPlugin uses only UI panel, no config options
 */
@ConfigGroup("noid")
public interface NoidConfig extends Config {
    // No config options - all settings are internal
}
