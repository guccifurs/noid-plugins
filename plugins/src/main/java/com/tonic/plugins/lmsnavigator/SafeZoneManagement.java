package com.tonic.plugins.lmsnavigator;

import com.tonic.Logger;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import com.tonic.plugins.lmsnavigator.FightLogic.TaskQueue;
import net.runelite.api.ChatMessageType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;

public class SafeZoneManagement
{
    private static String currentSafeZone = "Unknown";
    private static LMSNavigatorPlugin plugin;
    private static boolean autoNavDoneForCurrentZone = false;

    /**
     * Process chat messages to detect safe zone broadcasts
     */
    public static void processChatMessage(ChatMessage event)
    {
        ChatMessageType type = event.getType();
        String message = event.getMessage();
        Logger.norm("[SafeZone] type=" + type + " message=" + message);

        // Look for safe zone phrase in ANY chat line, regardless of type
        String lower = message.toLowerCase();

        String phrase = "safe zone is";
        int idx = lower.indexOf(phrase);
        if (idx == -1)
        {
            phrase = "save zone is"; // handle possible typo variant
            idx = lower.indexOf(phrase);
        }
        if (idx == -1)
        {
            phrase = "safezone is"; // handle compact form
            idx = lower.indexOf(phrase);
        }
        if (idx == -1)
        {
            // Not a safe zone message
            return;
        }

        // Try to extract the zone name in quotes after the phrase
        int firstQuote = message.indexOf('"', idx);
        String zoneName = null;
        if (firstQuote != -1)
        {
            int secondQuote = message.indexOf('"', firstQuote + 1);
            if (secondQuote != -1)
            {
                zoneName = message.substring(firstQuote + 1, secondQuote);
            }
        }

        // Fallback: take everything after the phrase if quotes are missing
        if (zoneName == null)
        {
            int start = idx + phrase.length();
            if (start < message.length())
            {
                String after = message.substring(start).trim();
                zoneName = after;
            }
        }

        if (zoneName == null)
        {
            Logger.warn("[SafeZone] zoneName is null after parsing, skipping update");
            return;
        }

        // Strip any colour tags
        String cleanZone = zoneName.replaceAll("<col=[0-9a-fA-F]+>", "")
                                   .replaceAll("</col>", "")
                                   .trim();

        // Detect if this is the FINAL safe zone announcement
        boolean isFinal = lower.contains("final safe zone");

        if (!cleanZone.isEmpty())
        {
            // Normalize leading 'the ' so it matches our path names
            String normalized = cleanZone;
            String lowerZone = normalized.toLowerCase();
            if (lowerZone.startsWith("the "))
            {
                normalized = normalized.substring(4).trim();
            }

            // Remove trailing '.' or '!' characters
            normalized = normalized.replaceAll("[.!]+$", "").trim();

            currentSafeZone = normalized;
            autoNavDoneForCurrentZone = false;
            Logger.norm("[SafeZone] Detected safe zone: " + currentSafeZone + (isFinal ? " (FINAL)" : ""));

            // Resolve a world destination for this safe zone; required for navigation and boxing
            WorldPoint dest = getDestinationForSafeZone(normalized);
            if (dest == null)
            {
                Logger.warn("[SafeZone] No destination mapped for safe zone: " + normalized);
                return;
            }

            // Always update LmsState safe zone and head there. Mark final zone as such.
            LmsState.setSafeZone(dest);
            if (isFinal)
            {
                LmsState.setFinalSafeZoneAnnounced(true);
            }

            TaskQueue.preempt(LmsState.LmsTask.GO_TO_SAFE_ZONE);
        }
    }

    public static String getCurrentSafeZone()
    {
        return currentSafeZone;
    }

    public static void setPlugin(LMSNavigatorPlugin navigatorPlugin)
    {
        plugin = navigatorPlugin;
        Logger.norm("[SafeZone] Plugin reference set");
    }

    /**
     * Called each game tick to decide if we should auto-navigate to the current safe zone.
     * This allows navigation to begin once conditions become true (e.g. target cleared) even
     * if they were not met at the exact moment of detection.
     */
    public static void onGameTick()
    {
        // Only attempt auto-navigation when the LMS Navigator plugin is available
        // and we are actually in the LMS instance.
        if (plugin == null)
        {
            return;
        }

        if (!plugin.isInInstance())
        {
            return;
        }

        if (!autoNavDoneForCurrentZone)
        {
            autoNavigateToSafeZone();
        }
    }

    private static void autoNavigateToSafeZone()
    {
        if (plugin == null)
        {
            Logger.warn("[SafeZone] Plugin reference is null, cannot auto-navigate");
            return;
        }

        // Conditions: No key, No target, Safe zone is known
        if (KeyManagement.hasBloodKey())
        {
            Logger.norm("[SafeZone] Skipping auto-nav: Bloody key present");
            return;
        }

        if (TargetManagement.hasTarget())
        {
            Logger.norm("[SafeZone] Skipping auto-nav: Target present");
            return;
        }

        if (currentSafeZone == null || currentSafeZone.equals("Unknown"))
        {
            Logger.norm("[SafeZone] Skipping auto-nav: Safe zone unknown");
            return;
        }

        WorldPoint destination = getDestinationForSafeZone(currentSafeZone);
        if (destination == null)
        {
            Logger.warn("[SafeZone] No destination mapped for safe zone: " + currentSafeZone);
            return;
        }

        Logger.norm("[SafeZone] Auto-navigating to safe zone '" + currentSafeZone + "' at " + destination);
        plugin.navigateToTemplate(destination);
        autoNavDoneForCurrentZone = true;
    }

    private static WorldPoint getDestinationForSafeZone(String safeZoneName)
    {
        if (safeZoneName == null)
        {
            return null;
        }

        switch (safeZoneName)
        {
            case "Moser Settlement":
                return new WorldPoint(3474, 5788, 0);
            case "Debtors Hideout":
            case "Debtor Hideout":
                return new WorldPoint(3404, 5802, 0);
            case "Mountain":
            case "The Mountain":
                return new WorldPoint(3430, 5845, 0);
            case "Trinity Outpost":
                return new WorldPoint(3500, 5870, 0);
            case "Stone Circle":
                return new WorldPoint(3663, 6061, 0);
            case "Pillars of Sacrifice":
            case "Pillars of sacrifice":
                return new WorldPoint(3594, 6164, 0);
            case "Dark Warriors' Palace":
            case "Dark Warrior's":
                return new WorldPoint(3546, 6158, 0);
            case "Trading Post":
                return new WorldPoint(3506, 6162, 0);
            case "Hut":
                return new WorldPoint(3498, 6073, 0);
            case "Blank Tower":
                return new WorldPoint(3603, 6102, 0);
            case "Town Center":
                return new WorldPoint(3551, 6108, 0);
            default:
                return null;
        }
    }
}
