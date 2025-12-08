package com.tonic.plugins.copilotsmiley;

import com.tonic.Logger;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Copilot Smiley",
        description = "Draws a smiley over Flipping Copilot's sell-item highlight.",
        tags = {"ge", "copilot", "overlay"}
)
public class CopilotSmileyPlugin extends Plugin {

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private CopilotSmileyOverlay overlay;

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        Logger.norm("Copilot Smiley v1.0 - overlaying Flipping Copilot sell highlights with smiley.png");
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
    }
}
