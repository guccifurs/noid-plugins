package com.tonic.plugins.clicktest;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

import java.awt.event.KeyEvent;

@ConfigGroup("clicktest")
public interface ClickTestConfig extends Config {

    enum ClickType {
        ROBOT("Robot (Real Mouse)"),
        CANVAS_EVENT("Canvas Events"),
        PACKET_ONLY("Packet Only"),
        HYBRID("Hybrid (Event + Packet)"),
        TRAJECTORY("VitaLite Trajectory");

        private final String label;

        ClickType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @ConfigItem(keyName = "clickType", name = "Click Type", description = "Type of click to use when testing")
    default ClickType clickType() {
        return ClickType.CANVAS_EVENT;
    }

    @ConfigItem(keyName = "captureHotkey", name = "Capture Position Hotkey", description = "Hotkey to capture current mouse position")
    default Keybind captureHotkey() {
        return new Keybind(KeyEvent.VK_F9, 0);
    }

    @ConfigItem(keyName = "targetX", name = "Target X", description = "Canvas X coordinate to click")
    default int targetX() {
        return 400;
    }

    @ConfigItem(keyName = "targetY", name = "Target Y", description = "Canvas Y coordinate to click")
    default int targetY() {
        return 300;
    }
}
