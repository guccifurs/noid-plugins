package com.tonic.plugins.autoquester;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.threaded.Dialogues;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.WidgetQuery;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.VitaPlugin;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@PluginDescriptor(
    name = "Auto Quester",
    description = "Scriptable automatic questing using VitaLite pathfinder, dialogue, and item APIs.",
    tags = {"quest", "automation", "pathfinding", "dialogue"}
)
public class AutoQuesterPlugin extends VitaPlugin
{
    private static final String VERSION = "1.6";

    @Inject
    private AutoQuesterConfig config;

    private volatile boolean questRunning = false;
    private volatile boolean questCompleted = false;

    @Provides
    AutoQuesterConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoQuesterConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        questRunning = false;
        questCompleted = false;
        Logger.norm("[AutoQuester] Auto Quester v" + VERSION + " - Cook's Assistant + Sheep Shearer auto-shearing & auto-spinning");
    }

    @Override
    protected void shutDown() throws Exception
    {
        questRunning = false;
        questCompleted = false;
        Logger.norm("[AutoQuester] Auto Quester stopped");
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!AutoQuesterConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }

        questCompleted = false;
    }

    @Override
    public void loop() throws Exception
    {
        if (!config.enabled())
        {
            Delays.tick(1);
            return;
        }

        if (questRunning || questCompleted)
        {
            Delays.tick(1);
            return;
        }

        String script = getActiveScript();
        if (script == null || script.trim().isEmpty())
        {
            Delays.tick(1);
            return;
        }

        questRunning = true;
        try
        {
            log("Starting quest script");
            runScript(script);
            log("Quest script finished");
            questCompleted = true;
        }
        catch (RuntimeException e)
        {
            Logger.error(e, "[AutoQuester] Quest script aborted: %e");
        }
        finally
        {
            questRunning = false;
        }
    }

    private void runScript(String script)
    {
        String[] rawLines = script.split("\\r?\\n");
        List<String> lines = new ArrayList<>();
        for (String raw : rawLines)
        {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#"))
            {
                continue;
            }
            lines.add(line);
        }

        for (int i = 0; i < lines.size(); i++)
        {
            String line = lines.get(i);
            log("Step " + (i + 1) + ": " + line);
            executeStep(line, i + 1);
        }
    }

    private void executeStep(String line, int lineNumber)
    {
        String[] parts = line.split("\\|");
        if (parts.length == 0)
        {
            return;
        }

        String op = parts[0].trim().toUpperCase(Locale.ROOT);

        switch (op)
        {
            case "SLEEP_TICKS":
                handleSleepTicks(parts, lineNumber);
                break;
            case "DIALOGUE_ALL":
                Dialogues.continueAllDialogue();
                break;
            case "WAIT_DIALOGUE":
                Delays.waitUntil(DialogueAPI::dialoguePresent, 50);
                break;
            case "WALK_NPC":
                handleWalkNpc(parts, lineNumber);
                break;
            case "INTERACT_NPC":
                handleInteractNpc(parts, lineNumber);
                break;
            case "WALK_OBJECT":
                handleWalkObject(parts, lineNumber);
                break;
            case "INTERACT_OBJECT":
                handleInteractObject(parts, lineNumber);
                break;
            case "PICKUP_ITEM":
                handlePickupItem(parts, lineNumber);
                break;
            case "USE_ITEM_ON_NPC":
                handleUseItemOnNpc(parts, lineNumber);
                break;
            case "USE_ITEM_ON_OBJECT":
                handleUseItemOnObject(parts, lineNumber);
                break;
            case "WAIT_INVENTORY_CONTAINS":
                handleWaitInventoryContains(parts, lineNumber);
                break;
            case "ENSURE_SHEARS":
                handleEnsureShears(lineNumber);
                break;
            case "SHEAR_WOOL":
                handleShearWool(parts, lineNumber);
                break;
            case "WALK_SPINNING_WHEEL":
                handleWalkSpinningWheel(lineNumber);
                break;
            case "SPIN_BALL_OF_WOOL":
                handleSpinBallOfWool(lineNumber);
                break;
            case "END":
                throw new RuntimeException("Script terminated by END");
            default:
                Logger.warn("[AutoQuester] Unknown opcode '" + op + "' at line " + lineNumber);
        }
    }

    private void handleSleepTicks(String[] parts, int lineNumber)
    {
        if (parts.length < 2)
        {
            Logger.warn("[AutoQuester] SLEEP_TICKS missing argument at line " + lineNumber);
            return;
        }

        try
        {
            int ticks = Integer.parseInt(parts[1].trim());
            if (ticks > 0 && ticks < 1000)
            {
                Delays.tick(ticks);
            }
        }
        catch (NumberFormatException e)
        {
            Logger.warn("[AutoQuester] Invalid SLEEP_TICKS value at line " + lineNumber);
        }
    }

    private void handleWalkNpc(String[] parts, int lineNumber)
    {
        if (parts.length < 2)
        {
            Logger.warn("[AutoQuester] WALK_NPC missing npc name at line " + lineNumber);
            return;
        }

        String name = parts[1].trim();
        if (name.isEmpty())
        {
            Logger.warn("[AutoQuester] WALK_NPC empty npc name at line " + lineNumber);
            return;
        }

        NpcEx npc = NpcAPI.search().withNameContains(name).shortestPath();
        if (npc == null)
        {
            Logger.warn("[AutoQuester] WALK_NPC could not find npc '" + name + "' at line " + lineNumber);
            return;
        }

        WorldPoint wp = npc.getWorldPoint();
        log("Walking to NPC '" + npc.getName() + "' at " + wp);
        Walker.walkTo(wp);
    }

    private void handleInteractNpc(String[] parts, int lineNumber)
    {
        if (parts.length < 3)
        {
            Logger.warn("[AutoQuester] INTERACT_NPC requires npc name and action at line " + lineNumber);
            return;
        }

        String name = parts[1].trim();
        String action = parts[2].trim();
        if (name.isEmpty() || action.isEmpty())
        {
            Logger.warn("[AutoQuester] INTERACT_NPC missing npc name or action at line " + lineNumber);
            return;
        }

        NpcEx npc = NpcAPI.search().withNameContains(name).shortestPath();
        if (npc == null)
        {
            Logger.warn("[AutoQuester] INTERACT_NPC could not find npc '" + name + "' at line " + lineNumber);
            return;
        }

        WorldPoint wp = npc.getWorldPoint();
        log("Walking to and interacting with NPC '" + npc.getName() + "' using action '" + action + "'");
        Walker.walkTo(wp);
        NpcAPI.interact(npc, action);
        Delays.tick(1);
    }

    private void handleWalkObject(String[] parts, int lineNumber)
    {
        if (parts.length < 2)
        {
            Logger.warn("[AutoQuester] WALK_OBJECT missing object name at line " + lineNumber);
            return;
        }

        String name = parts[1].trim();
        if (name.isEmpty())
        {
            Logger.warn("[AutoQuester] WALK_OBJECT empty object name at line " + lineNumber);
            return;
        }

        TileObjectEx object = TileObjectAPI.search().withNameContains(name).shortestPath();
        if (object == null)
        {
            Logger.warn("[AutoQuester] WALK_OBJECT could not find object '" + name + "' at line " + lineNumber);
            return;
        }

        WorldPoint wp = object.getWorldPoint();
        log("Walking to object '" + object.getName() + "' at " + wp);
        Walker.walkTo(wp);
    }

    private void handleInteractObject(String[] parts, int lineNumber)
    {
        if (parts.length < 3)
        {
            Logger.warn("[AutoQuester] INTERACT_OBJECT requires object name and action at line " + lineNumber);
            return;
        }

        String name = parts[1].trim();
        String action = parts[2].trim();
        if (name.isEmpty() || action.isEmpty())
        {
            Logger.warn("[AutoQuester] INTERACT_OBJECT missing object name or action at line " + lineNumber);
            return;
        }

        TileObjectEx object = TileObjectAPI.search().withNameContains(name).shortestPath();
        if (object == null)
        {
            Logger.warn("[AutoQuester] INTERACT_OBJECT could not find object '" + name + "' at line " + lineNumber);
            return;
        }

        WorldPoint wp = object.getWorldPoint();
        log("Walking to and interacting with object '" + object.getName() + "' using action '" + action + "'");
        Walker.walkTo(wp);
        TileObjectAPI.interact(object, action);
        Delays.tick(1);
    }

    private void handlePickupItem(String[] parts, int lineNumber)
    {
        if (parts.length < 2)
        {
            Logger.warn("[AutoQuester] PICKUP_ITEM missing item name at line " + lineNumber);
            return;
        }

        String name = parts[1].trim();
        if (name.isEmpty())
        {
            Logger.warn("[AutoQuester] PICKUP_ITEM empty item name at line " + lineNumber);
            return;
        }

        TileItemEx item = TileItemAPI.search().withNameContains(name).shortestPath();
        if (item == null)
        {
            Logger.warn("[AutoQuester] PICKUP_ITEM could not find item '" + name + "' at line " + lineNumber);
            return;
        }

        WorldPoint wp = item.getWorldPoint();
        log("Walking to and picking up item '" + item.getName() + "' at " + wp);
        Walker.walkTo(wp);
        TileItemAPI.interact(item, "take");
        Delays.tick(1);
    }

    private void handleUseItemOnNpc(String[] parts, int lineNumber)
    {
        if (parts.length < 3)
        {
            Logger.warn("[AutoQuester] USE_ITEM_ON_NPC requires item name and npc name at line " + lineNumber);
            return;
        }

        String itemName = parts[1].trim();
        String npcName = parts[2].trim();
        if (itemName.isEmpty() || npcName.isEmpty())
        {
            Logger.warn("[AutoQuester] USE_ITEM_ON_NPC missing item or npc name at line " + lineNumber);
            return;
        }

        ItemEx item = InventoryAPI.getItem(itemName);
        if (item == null)
        {
            Logger.warn("[AutoQuester] USE_ITEM_ON_NPC could not find inventory item '" + itemName + "' at line " + lineNumber);
            return;
        }

        NpcEx npc = NpcAPI.search().withNameContains(npcName).shortestPath();
        if (npc == null)
        {
            Logger.warn("[AutoQuester] USE_ITEM_ON_NPC could not find npc '" + npcName + "' at line " + lineNumber);
            return;
        }

        WorldPoint wp = npc.getWorldPoint();
        log("Walking to NPC '" + npc.getName() + "' and using item '" + item.getName() + "'");
        Walker.walkTo(wp);
        InventoryAPI.useOn(item, npc);
        Delays.tick(1);
    }

    private void handleUseItemOnObject(String[] parts, int lineNumber)
    {
        if (parts.length < 3)
        {
            Logger.warn("[AutoQuester] USE_ITEM_ON_OBJECT requires item name and object name at line " + lineNumber);
            return;
        }

        String itemName = parts[1].trim();
        String objectName = parts[2].trim();
        if (itemName.isEmpty() || objectName.isEmpty())
        {
            Logger.warn("[AutoQuester] USE_ITEM_ON_OBJECT missing item or object name at line " + lineNumber);
            return;
        }

        ItemEx item = InventoryAPI.getItem(itemName);
        if (item == null)
        {
            Logger.warn("[AutoQuester] USE_ITEM_ON_OBJECT could not find inventory item '" + itemName + "' at line " + lineNumber);
            return;
        }

        TileObjectEx object = TileObjectAPI.search().withNameContains(objectName).shortestPath();
        if (object == null)
        {
            Logger.warn("[AutoQuester] USE_ITEM_ON_OBJECT could not find object '" + objectName + "' at line " + lineNumber);
            return;
        }

        WorldPoint wp = object.getWorldPoint();
        log("Walking to object '" + object.getName() + "' and using item '" + item.getName() + "'");
        Walker.walkTo(wp);
        InventoryAPI.useOn(item, object);
        Delays.tick(1);
    }

    private void handleWaitInventoryContains(String[] parts, int lineNumber)
    {
        if (parts.length < 2)
        {
            Logger.warn("[AutoQuester] WAIT_INVENTORY_CONTAINS missing item name at line " + lineNumber);
            return;
        }

        String name = parts[1].trim();
        if (name.isEmpty())
        {
            Logger.warn("[AutoQuester] WAIT_INVENTORY_CONTAINS empty item name at line " + lineNumber);
            return;
        }

        int timeoutTicks = 200;
        if (parts.length >= 3)
        {
            try
            {
                timeoutTicks = Integer.parseInt(parts[2].trim());
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        log("Waiting for inventory to contain '" + name + "' (up to " + timeoutTicks + " ticks)");
        Delays.waitUntil(() -> InventoryAPI.contains(name), timeoutTicks);
    }

    private void log(String message)
    {
        if (config.logSteps())
        {
            Logger.norm("[AutoQuester] " + message);
        }
    }

    private String getActiveScript()
    {
        AutoQuesterQuest profile = config.questProfile();
        if (profile != null)
        {
            switch (profile)
            {
                case COOKS_ASSISTANT:
                    return getCooksAssistantScript();
                case SHEEP_SHEARER:
                    return getSheepShearerScript();
                case NONE:
                default:
                    break;
            }
        }

        return config.questScript();
    }

    private String getCooksAssistantScript()
    {
        return String.join("\n",
            "# Cook's Assistant – AutoQuester script",
            "# Assumptions:",
            "# - Start in Lumbridge Castle kitchen near the Cook",
            "# - Have 1x Bucket and 1x Pot in inventory",
            "",
            "# 1) Talk to Cook to start/advance the quest",
            "WALK_NPC|Cook",
            "INTERACT_NPC|Cook|Talk-to",
            "WAIT_DIALOGUE",
            "DIALOGUE_ALL",
            "SLEEP_TICKS|2",
            "",
            "# 2) Get a bucket of milk from nearest Dairy cow",
            "USE_ITEM_ON_NPC|bucket|Dairy cow",
            "WAIT_INVENTORY_CONTAINS|bucket of milk|200",
            "SLEEP_TICKS|2",
            "",
            "# 3) Get an egg from nearest chicken coop",
            "PICKUP_ITEM|egg",
            "WAIT_INVENTORY_CONTAINS|egg|200",
            "SLEEP_TICKS|2",
            "",
            "# 4) Make flour from wheat using windmill",
            "",
            "# Pick wheat",
            "WALK_OBJECT|Wheat",
            "INTERACT_OBJECT|Wheat|Pick",
            "WAIT_INVENTORY_CONTAINS|grain|200",
            "SLEEP_TICKS|2",
            "",
            "# Put grain in hopper",
            "WALK_OBJECT|Hopper",
            "USE_ITEM_ON_OBJECT|grain|Hopper",
            "SLEEP_TICKS|2",
            "",
            "# Operate hopper controls to fill bin",
            "INTERACT_OBJECT|Hopper controls|Operate",
            "SLEEP_TICKS|4",
            "",
            "# Take flour from flour bin into pot",
            "WALK_OBJECT|Flour bin",
            "INTERACT_OBJECT|Flour bin|Empty",
            "WAIT_INVENTORY_CONTAINS|pot of flour|200",
            "SLEEP_TICKS|2",
            "",
            "# 5) Return to Cook and hand everything in",
            "WALK_NPC|Cook",
            "INTERACT_NPC|Cook|Talk-to",
            "WAIT_DIALOGUE",
            "DIALOGUE_ALL",
            "SLEEP_TICKS|2",
            "",
            "END"
        );
    }

    private String getSheepShearerScript()
    {
        return String.join("\n",
            "# Sheep Shearer – AutoQuester script",
            "# Assumptions:",
            "# - Start near Farmer Fred north-west of Lumbridge",
            "# - Plugin will auto-obtain shears from Lumbridge farm spawn, shear sheep, walk to a spinning wheel, and auto-spin wool.",
            "",
            "# Talk to Farmer Fred to start/advance the quest",
            "WALK_NPC|Fred",
            "INTERACT_NPC|Fred|Talk-to",
            "WAIT_DIALOGUE",
            "DIALOGUE_ALL",
            "SLEEP_TICKS|2",
            "",
            "# Ensure we have shears (from Lumbridge farm north-west spawn if needed)",
            "ENSURE_SHEARS",
            "",
            "# Shear sheep until we have at least 20 wool in inventory",
            "SHEAR_WOOL|20",
            "SLEEP_TICKS|2",
            "",
            "# Walk to nearest spinning wheel and open it, then auto-spin all wool into balls of wool",
            "WALK_SPINNING_WHEEL",
            "SLEEP_TICKS|2",
            "SPIN_BALL_OF_WOOL",
            "SLEEP_TICKS|2",
            "",
            "# Wait until balls of wool are produced",
            "WAIT_INVENTORY_CONTAINS|ball of wool|400",
            "SLEEP_TICKS|2",
            "",
            "# Return to Fred and hand in wool",
            "WALK_NPC|Fred",
            "INTERACT_NPC|Fred|Talk-to",
            "WAIT_DIALOGUE",
            "DIALOGUE_ALL",
            "SLEEP_TICKS|2",
            "",
            "END"
        );
    }

    private void handleEnsureShears(int lineNumber)
    {
        // If we already have shears, nothing to do
        if (InventoryAPI.contains("shears"))
        {
            log("ENSURE_SHEARS: Shears already in inventory");
            return;
        }

        // Walk to known Lumbridge shears spawn at farm north-west of Lumbridge
        WorldPoint shearsSpawn = new WorldPoint(3192, 3272, 0);
        log("ENSURE_SHEARS: Walking to Lumbridge farm shears spawn at " + shearsSpawn);
        Walker.walkTo(shearsSpawn);

        // After arriving, look for ground shears nearby
        TileItemEx shears = TileItemAPI.search().withNameContains("Shears").shortestPath();
        if (shears == null)
        {
            Logger.warn("[AutoQuester] ENSURE_SHEARS: No ground shears found near spawn at line " + lineNumber);
            return;
        }

        WorldPoint wp = shears.getWorldPoint();
        log("ENSURE_SHEARS: Picking up shears at " + wp);
        Walker.walkTo(wp);
        TileItemAPI.interact(shears, "take");
        Delays.tick(1);
    }

    private void handleShearWool(String[] parts, int lineNumber)
    {
        int target = 20;
        if (parts.length >= 2)
        {
            try
            {
                target = Integer.parseInt(parts[1].trim());
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        log("SHEAR_WOOL: Target wool count = " + target);
        int attempts = 0;
        int maxAttempts = Math.max(20, target * 5);

        while (getWoolCount() < target && attempts < maxAttempts)
        {
            if (InventoryAPI.isFull() && !InventoryAPI.contains("wool"))
            {
                Logger.warn("[AutoQuester] SHEAR_WOOL: Inventory is full and contains no wool at line " + lineNumber);
                break;
            }

            NpcEx sheep = NpcAPI.search().withNameContains("sheep").shortestPath();
            if (sheep == null)
            {
                Logger.warn("[AutoQuester] SHEAR_WOOL: No sheep found at line " + lineNumber);
                break;
            }

            WorldPoint wp = sheep.getWorldPoint();
            log("SHEAR_WOOL: Walking to sheep at " + wp);
            Walker.walkTo(wp);

            String sheepName = sheep.getName();
            if (sheepName == null || sheepName.isEmpty())
            {
                sheepName = "sheep";
            }

            log("SHEAR_WOOL: Shearing sheep " + sheepName);

            NpcEx targetSheep = NpcAPI.search().withNameContains("sheep").shortestPath();
            if (targetSheep != null)
            {
                NpcAPI.interact(targetSheep, "Shear");
            }
            else
            {
                Logger.warn("[AutoQuester] SHEAR_WOOL: Sheep disappeared before interaction at line " + lineNumber);
                break;
            }

            Delays.tick(1);
            attempts++;
        }

        if (getWoolCount() < target)
        {
            Logger.warn("[AutoQuester] SHEAR_WOOL: Stopped with wool=" + getWoolCount() + " target=" + target + " at line " + lineNumber);
        }
    }

    private int getWoolCount()
    {
        // Use case-insensitive name matching via InventoryAPI.search().withNameContains
        // so both "Wool" and possible variants are counted.
        return InventoryAPI.search().withNameContains("wool").count();
    }

    private void handleWalkSpinningWheel(int lineNumber)
    {
        TileObjectEx wheel = TileObjectAPI.search().withNameContains("Spinning wheel").shortestPath();
        if (wheel == null)
        {
            // Fallback: walk to known Lumbridge castle spinning wheel location first,
            // then search again once the scene has loaded that area.
            WorldPoint lumbridgeSpinner = new WorldPoint(3209, 3213, 1);
            log("WALK_SPINNING_WHEEL: No local spinning wheel, walking to Lumbridge spinner at " + lumbridgeSpinner);
            Walker.walkTo(lumbridgeSpinner);

            wheel = TileObjectAPI.search().withNameContains("Spinning wheel").shortestPath();
            if (wheel == null)
            {
                Logger.warn("[AutoQuester] WALK_SPINNING_WHEEL: No spinning wheel found near Lumbridge at line " + lineNumber);
                return;
            }
        }

        WorldPoint wp = wheel.getWorldPoint();
        log("WALK_SPINNING_WHEEL: Interacting with spinning wheel at " + wp);
        // We are already in the Lumbridge spinner area if needed; object click will handle final pathing.
        TileObjectAPI.interact(wheel, "Spin");
        Delays.tick(1);
    }

    private void handleSpinBallOfWool(int lineNumber)
    {
        if (getWoolCount() == 0)
        {
            Logger.warn("[AutoQuester] SPIN_BALL_OF_WOOL: No wool in inventory at line " + lineNumber);
            return;
        }

        // Wait for the spinning interface with Ball of wool option to appear
        boolean interfaceOpened = Delays.waitUntil(() -> findBallOfWoolSpinWidget() != null, 50);
        if (!interfaceOpened)
        {
            Logger.warn("[AutoQuester] SPIN_BALL_OF_WOOL: Spinning interface did not appear at line " + lineNumber);
            return;
        }

        Widget spinWidget = findBallOfWoolSpinWidget();
        if (spinWidget == null)
        {
            Logger.warn("[AutoQuester] SPIN_BALL_OF_WOOL: Could not find Ball of wool spin option at line " + lineNumber);
            return;
        }

        log("SPIN_BALL_OF_WOOL: Clicking Make All / Spin All for Ball of wool");
        // Try common action names used by spinning/crafting interfaces
        WidgetAPI.interact(spinWidget,
            "Make all",
            "Make x",
            "Make",
            "Spin all",
            "Spin");
        Delays.tick(1);
    }

    private Widget findBallOfWoolSpinWidget()
    {
        // Prefer matching by BALL_OF_WOOL item id, fall back to text search if needed.
        WidgetQuery byItem = WidgetAPI.search()
            .isSelfVisible()
            .withItemId(ItemID.BALL_OF_WOOL);
        Widget widget = byItem.first();
        if (widget != null)
        {
            return widget;
        }

        return WidgetAPI.search()
            .isSelfVisible()
            .withTextContains("Ball of wool")
            .first();
    }
}
