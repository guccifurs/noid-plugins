package com.tonic.plugins.humanequipper;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.PrayerAPI;
import com.tonic.util.ClickManagerUtil;
import com.tonic.data.magic.Spell;
import com.tonic.data.magic.SpellBook;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.services.mouserecorder.MouseDataPoint;
import com.tonic.services.mouserecorder.MouseMovementSequence;
import com.tonic.services.mouserecorder.trajectory.TrajectoryGenerator;
import com.tonic.services.mouserecorder.trajectory.TrajectoryService;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@PluginDescriptor(
    name = "Human Equipper",
    description = "Equip inventory items using OS mouse movement based on trained trajectories.",
    tags = {"equip", "mouse", "trajectory", "robot"}
)
public class HumanEquipperPlugin extends Plugin
{
    private static final String VERSION = "Human Equipper v2.3";

    @Inject
    private Client client;

    @Inject
    private HumanEquipperConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HumanEquipperOverlay overlay;

    private volatile boolean isRunning = false;

    private final Random random = new Random();

    private static final long TARGET_CACHE_DURATION_MS = 30000L;

    private volatile ActorEx<?> cachedTarget;

    private volatile long cachedTargetTime = 0L;

    private long currentActionTargetMillis = -1L;

    private final HotkeyListener equipHotkeyListener = new HotkeyListener(() -> config.equipHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            startEquipThread(config.itemNames(), "HumanEquipper-Runner-1");
        }
    };

    private final HotkeyListener equipHotkeyListener2 = new HotkeyListener(() -> config.equipHotkey2())
    {
        @Override
        public void hotkeyPressed()
        {
            startEquipThread(config.itemNames2(), "HumanEquipper-Runner-2");
        }
    };

    private final HotkeyListener equipHotkeyListener3 = new HotkeyListener(() -> config.equipHotkey3())
    {
        @Override
        public void hotkeyPressed()
        {
            startEquipThread(config.itemNames3(), "HumanEquipper-Runner-3");
        }
    };

    private final HotkeyListener equipHotkeyListener4 = new HotkeyListener(() -> config.equipHotkey4())
    {
        @Override
        public void hotkeyPressed()
        {
            startEquipThread(config.itemNames4(), "HumanEquipper-Runner-4");
        }
    };

    private final HotkeyListener equipHotkeyListener5 = new HotkeyListener(() -> config.equipHotkey5())
    {
        @Override
        public void hotkeyPressed()
        {
            startEquipThread(config.itemNames5(), "HumanEquipper-Runner-5");
        }
    };

    @Provides
    HumanEquipperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HumanEquipperConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        Logger.norm("[" + VERSION + "] Plugin started.");
        keyManager.registerKeyListener(equipHotkeyListener);
        keyManager.registerKeyListener(equipHotkeyListener2);
        keyManager.registerKeyListener(equipHotkeyListener3);
        keyManager.registerKeyListener(equipHotkeyListener4);
        keyManager.registerKeyListener(equipHotkeyListener5);
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        isRunning = false;
        Logger.norm("[Human Equipper] Plugin stopped.");
        keyManager.unregisterKeyListener(equipHotkeyListener);
        keyManager.unregisterKeyListener(equipHotkeyListener2);
        keyManager.unregisterKeyListener(equipHotkeyListener3);
        keyManager.unregisterKeyListener(equipHotkeyListener4);
        keyManager.unregisterKeyListener(equipHotkeyListener5);
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            cachedTarget = null;
            cachedTargetTime = 0L;
            return;
        }

        Player local = client.getLocalPlayer();
        Actor interacting = local.getInteracting();
        long now = System.currentTimeMillis();

        if (interacting != null)
        {
            cachedTarget = ActorEx.fromActor(interacting);
            cachedTargetTime = now;
        }
        else
        {
            // Try to find anyone currently attacking us as a fallback target
            Actor best = null;
            int bestDist = Integer.MAX_VALUE;

            for (Player p : client.getPlayers())
            {
                if (p == null)
                {
                    continue;
                }

                Actor theirTarget = p.getInteracting();
                if (theirTarget == local)
                {
                    WorldPoint lp = local.getWorldLocation();
                    WorldPoint tp = p.getWorldLocation();
                    if (lp != null && tp != null)
                    {
                        int dist = lp.distanceTo(tp);
                        if (dist < bestDist)
                        {
                            bestDist = dist;
                            best = p;
                        }
                    }
                    else
                    {
                        best = p;
                        break;
                    }
                }
            }

            for (NPC n : client.getNpcs())
            {
                if (n == null)
                {
                    continue;
                }

                Actor theirTarget = n.getInteracting();
                if (theirTarget == local)
                {
                    WorldPoint lp = local.getWorldLocation();
                    WorldPoint tp = n.getWorldLocation();
                    if (lp != null && tp != null)
                    {
                        int dist = lp.distanceTo(tp);
                        if (dist < bestDist)
                        {
                            bestDist = dist;
                            best = n;
                        }
                    }
                    else
                    {
                        best = n;
                        break;
                    }
                }
            }

            if (best != null)
            {
                cachedTarget = ActorEx.fromActor(best);
                cachedTargetTime = now;
            }
            else if (cachedTarget != null)
            {
                if (cachedTargetTime <= 0L || now - cachedTargetTime > TARGET_CACHE_DURATION_MS)
                {
                    cachedTarget = null;
                    cachedTargetTime = 0L;
                }
            }
        }
    }

    private void startEquipThread(String rawScript, String threadName)
    {
        if (isRunning)
        {
            Logger.warn("[Human Equipper] Equip sequence already running.");
            return;
        }

        new Thread(() -> runEquipThreadForScript(rawScript), threadName).start();
    }

    private void runEquipThreadForScript(String raw)
    {
        if (isRunning)
        {
            Logger.warn("[Human Equipper] Equip sequence already running.");
            return;
        }

        isRunning = true;
        try
        {
            if (raw == null)
            {
                Logger.warn("[Human Equipper] No item names configured.");
                return;
            }

            String[] lines = raw.split("\\r?\\n");
            List<String> scriptLines = new ArrayList<>();
            for (String line : lines)
            {
                if (line == null)
                {
                    continue;
                }
                String trimmed = line.trim();
                if (!trimmed.isEmpty())
                {
                    scriptLines.add(trimmed);
                }
            }

            if (scriptLines.isEmpty())
            {
                Logger.warn("[Human Equipper] No non-empty item names found.");
                return;
            }

            // Allow up to 5 seconds from config so timing changes are clearly visible.
            int totalTimeMs = Math.max(1, Math.min(5000, config.totalTimeMillis()));
            int randomnessPercent = Math.max(0, Math.min(100, config.perItemDelayRandomness()));

            Logger.norm("[Human Equipper] Executing " + scriptLines.size() + " actions with combined path targeting ~" + totalTimeMs + "ms total (randomness=" + randomnessPercent + "%).");

            // First, execute all Prayer: lines individually using Robot-based clicks,
            // while collecting Item:/Cast: actions to run in a single combined mouse path.
            // "Attack" lines are treated as a request to click the current target at the end.
            List<String> nonPrayerCommands = new ArrayList<>();
            boolean hasAttackCommand = false;
            for (String line : scriptLines)
            {
                String trimmed = line.trim();
                if (trimmed.isEmpty())
                {
                    continue;
                }
                String lower = trimmed.toLowerCase(Locale.ROOT);
                if (lower.startsWith("prayer:"))
                {
                    String name = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                    if (!name.isEmpty())
                    {
                        try
                        {
                            handlePrayerCommand(name);
                        }
                        catch (Exception e)
                        {
                            Logger.error("[Human Equipper] Error executing prayer '" + trimmed + "': " + e.getMessage());
                        }
                    }
                }
                else if (lower.equals("attack"))
                {
                    // Remember to attack the current target after the sequence.
                    hasAttackCommand = true;
                }
                else
                {
                    nonPrayerCommands.add(trimmed);
                }
            }

            if (nonPrayerCommands.isEmpty())
            {
                return;
            }

            // Normalize Item:/Cast:/plain lines into a sequence we will drive with one combined path.
            List<String> combinedSequence = new ArrayList<>();
            boolean hasCast = false;
            boolean hasItem = false;
            for (String cmd : nonPrayerCommands)
            {
                String lower = cmd.toLowerCase(Locale.ROOT);

                // "Attack" was already handled as a flag; do not treat it as an item name.
                if (lower.equals("attack"))
                {
                    continue;
                }

                if (lower.startsWith("item:"))
                {
                    String name = cmd.substring(cmd.indexOf(':') + 1).trim();
                    if (!name.isEmpty())
                    {
                        combinedSequence.add("item:" + name);
                        hasItem = true;
                    }
                }
                else if (lower.startsWith("cast:"))
                {
                    String name = cmd.substring(cmd.indexOf(':') + 1).trim();
                    if (!name.isEmpty())
                    {
                        combinedSequence.add("cast:" + name);
                        hasCast = true;
                    }
                }
                else
                {
                    // Default: treat as Item for backwards compatibility
                    combinedSequence.add("item:" + cmd);
                    hasItem = true;
                }
            }

            if (combinedSequence.isEmpty())
            {
                return;
            }

            List<CombinedClickAction> clickActions = new ArrayList<>();
            for (String cmd : combinedSequence)
            {
                if (!isRunning)
                {
                    return;
                }

                String lowerCmd = cmd.toLowerCase(Locale.ROOT);
                if (lowerCmd.startsWith("item:"))
                {
                    String name = cmd.substring(cmd.indexOf(':') + 1).trim();
                    CombinedClickAction action = buildItemClickAction(name);
                    if (action != null)
                    {
                        clickActions.add(action);
                    }
                }
                else if (lowerCmd.startsWith("cast:"))
                {
                    String name = cmd.substring(cmd.indexOf(':') + 1).trim();
                    CombinedClickAction action = buildSpellClickAction(name);
                    if (action != null)
                    {
                        clickActions.add(action);
                        hasCast = true;
                    }
                }
            }

            if (clickActions.isEmpty())
            {
                return;
            }

            // Use the fast Robot-based sequential path for all actions.
            runSequentialRobotPath(clickActions, totalTimeMs);

            // After casting spells or if an explicit Attack command was present,
            // click the current target once using the smoother tracking logic.
            if ((hasCast || hasAttackCommand) && isRunning)
            {
                clickCurrentTargetWithRobot();
            }
        }
        finally
        {
            isRunning = false;
        }
    }

    private boolean handlePrayerCommand(String prayerName)
    {
        if (prayerName == null)
        {
            return false;
        }

        String trimmed = prayerName.trim();
        if (trimmed.isEmpty())
        {
            return false;
        }

        // Open prayer tab using user's keybind
        pressKeybind(config.prayerTabKey());

        String normalized = trimmed.toUpperCase(Locale.ROOT).replace(' ', '_');
        PrayerAPI prayer;
        try
        {
            prayer = PrayerAPI.valueOf(normalized);
        }
        catch (IllegalArgumentException e)
        {
            Logger.warn("[Human Equipper] Unknown prayer '" + trimmed + "'. Expected PrayerAPI enum-style name.");
            return false;
        }

        int widgetId = prayer.getInterfaceId();
        Widget widget = Static.invoke(() -> client.getWidget(widgetId));
        if (widget == null)
        {
            Logger.warn("[Human Equipper] Prayer widget not found for '" + trimmed + "' (id=" + widgetId + ").");
            return false;
        }

        net.runelite.api.Point loc = Static.invoke(widget::getCanvasLocation);
        int width = Static.invoke(widget::getWidth);
        int height = Static.invoke(widget::getHeight);

        if (loc == null || width <= 0 || height <= 0)
        {
            Logger.warn("[Human Equipper] Invalid prayer widget bounds for '" + trimmed + "'.");
            return false;
        }

        int centerX = loc.getX() + width / 2;
        int centerY = loc.getY() + height / 2;

        int targetX = centerX;
        int targetY = centerY;

        int offsetRadius = Math.max(0, config.randomTargetOffsetRadius());
        if (offsetRadius > 0)
        {
            int maxOffsetX = Math.min(offsetRadius, width / 2);
            int maxOffsetY = Math.min(offsetRadius, height / 2);

            if (maxOffsetX > 0 || maxOffsetY > 0)
            {
                int offsetX = maxOffsetX > 0 ? random.nextInt(maxOffsetX * 2 + 1) - maxOffsetX : 0;
                int offsetY = maxOffsetY > 0 ? random.nextInt(maxOffsetY * 2 + 1) - maxOffsetY : 0;

                targetX = Math.max(loc.getX() + 1, Math.min(loc.getX() + width - 1, centerX + offsetX));
                targetY = Math.max(loc.getY() + 1, Math.min(loc.getY() + height - 1, centerY + offsetY));
            }
        }

        moveMouseAndClickCanvasPoint(targetX, targetY, "prayer: " + trimmed);
        return true;
    }

    private boolean clickItemByName(String itemName)
    {
        if (itemName == null)
        {
            return false;
        }

        String trimmed = itemName.trim();
        if (trimmed.isEmpty())
        {
            return false;
        }

        ItemEx item = InventoryAPI.getItem(trimmed);
        if (item == null)
        {
            Logger.warn("[Human Equipper] Item not found in inventory: " + trimmed);
            return false;
        }

        ClickManagerUtil.queueClickBox(item);
        return true;
    }

    private boolean clickSpellByName(String spellName)
    {
        if (spellName == null)
        {
            return false;
        }

        String trimmed = spellName.trim();
        if (trimmed.isEmpty())
        {
            return false;
        }

        String normalized = trimmed.toUpperCase(Locale.ROOT).replace(' ', '_');

        Spell targetSpell = null;
        for (Spell spell : SpellBook.getCurrentOffensiveSpells())
        {
            if (spell instanceof Enum)
            {
                String enumName = ((Enum<?>) spell).name();
                if (enumName.equalsIgnoreCase(normalized))
                {
                    targetSpell = spell;
                    break;
                }
            }
        }

        if (targetSpell == null)
        {
            Logger.warn("[Human Equipper] Unknown spell '" + trimmed + "' for current spellbook.");
            return false;
        }

        int widgetId = targetSpell.getWidget();
        Widget widget = Static.invoke(() -> client.getWidget(widgetId));
        if (widget == null)
        {
            Logger.warn("[Human Equipper] Spell widget not found for '" + trimmed + "' (id=" + widgetId + ").");
            return false;
        }

        ClickManagerUtil.queueClickBox(widget);
        return true;
    }

    private void moveMouseAndClickCanvasPoint(int targetX, int targetY, String contextLabel)
    {
        Component canvas = client != null ? client.getCanvas() : null;
        if (canvas == null)
        {
            Logger.warn("[Human Equipper] Client canvas is null; cannot compute screen coordinates for " + contextLabel + ".");
            return;
        }

        Point canvasOnScreen;
        try
        {
            canvasOnScreen = canvas.getLocationOnScreen();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to get canvas location on screen for " + contextLabel + ": " + e.getMessage());
            return;
        }

        if (canvasOnScreen == null)
        {
            Logger.warn("[Human Equipper] Canvas on-screen location is null for " + contextLabel + ".");
            return;
        }

        Point mouseOnScreen = MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : null;
        if (mouseOnScreen == null)
        {
            Logger.warn("[Human Equipper] Mouse pointer location is null; cannot move to " + contextLabel + ".");
            return;
        }

        int startX = mouseOnScreen.x - canvasOnScreen.x;
        int startY = mouseOnScreen.y - canvasOnScreen.y;

        int endX = targetX;
        int endY = targetY;

        int steps = 30;
        long perStepMs = 5L;

        Robot robot;
        try
        {
            robot = new Robot();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to create Robot for " + contextLabel + ": " + e.getMessage());
            return;
        }

        for (int i = 1; i <= steps; i++)
        {
            if (!isRunning)
            {
                return;
            }

            double t = (double) i / (double) steps;
            int x = (int) Math.round(startX + (endX - startX) * t);
            int y = (int) Math.round(startY + (endY - startY) * t);

            int screenX = canvasOnScreen.x + x;
            int screenY = canvasOnScreen.y + y;
            robot.mouseMove(screenX, screenY);

            try
            {
                Thread.sleep(perStepMs);
            }
            catch (InterruptedException ignored)
            {
                return;
            }
        }

        int holdMs = Math.max(0, Math.min(120, config.clickHoldMillis()));

        try
        {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            if (holdMs > 0)
            {
                Thread.sleep(holdMs);
            }
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
        catch (InterruptedException ignored)
        {
            // Ignore
        }
    }

    private CombinedClickAction buildItemClickAction(String itemName)
    {
        if (itemName == null)
        {
            return null;
        }

        String trimmed = itemName.trim();
        if (trimmed.isEmpty())
        {
            return null;
        }

        ItemEx item = InventoryAPI.getItem(trimmed);
        if (item == null)
        {
            Logger.warn("[Human Equipper] Item not found in inventory: " + trimmed);
            return null;
        }

        Widget widget = item.getWidget();
        if (widget == null)
        {
            Logger.warn("[Human Equipper] Inventory widget not found for item: " + item.getName());
            return null;
        }

        net.runelite.api.Point loc = Static.invoke(widget::getCanvasLocation);
        int width = Static.invoke(widget::getWidth);
        int height = Static.invoke(widget::getHeight);

        if (loc == null || width <= 0 || height <= 0)
        {
            Logger.warn("[Human Equipper] Invalid inventory widget bounds for item: " + item.getName());
            return null;
        }

        int centerX = loc.getX() + width / 2;
        int centerY = loc.getY() + height / 2;

        // For items, aim directly at the slot center with no extra randomness for reliability.
        int targetX = centerX;
        int targetY = centerY;

        return new CombinedClickAction(targetX, targetY, "item: " + item.getName(), true, false);
    }

    private CombinedClickAction buildSpellClickAction(String spellName)
    {
        if (spellName == null)
        {
            return null;
        }

        String trimmed = spellName.trim();
        if (trimmed.isEmpty())
        {
            return null;
        }

        String normalized = trimmed.toUpperCase(Locale.ROOT).replace(' ', '_');

        Spell targetSpell = null;
        for (Spell spell : SpellBook.getCurrentOffensiveSpells())
        {
            if (spell instanceof Enum)
            {
                String enumName = ((Enum<?>) spell).name();
                if (enumName.equalsIgnoreCase(normalized))
                {
                    targetSpell = spell;
                    break;
                }
            }
        }

        if (targetSpell == null)
        {
            Logger.warn("[Human Equipper] Unknown spell '" + trimmed + "' for current spellbook.");
            return null;
        }

        int widgetId = targetSpell.getWidget();
        Widget widget = Static.invoke(() -> client.getWidget(widgetId));
        if (widget == null)
        {
            Logger.warn("[Human Equipper] Spell widget not found for '" + trimmed + "' (id=" + widgetId + ").");
            return null;
        }

        net.runelite.api.Point loc = Static.invoke(widget::getCanvasLocation);
        int width = Static.invoke(widget::getWidth);
        int height = Static.invoke(widget::getHeight);

        if (loc == null || width <= 0 || height <= 0)
        {
            Logger.warn("[Human Equipper] Invalid spell widget bounds for '" + trimmed + "'.");
            return null;
        }

        int centerX = loc.getX() + width / 2;
        int centerY = loc.getY() + height / 2;

        int targetX = centerX;
        int targetY = centerY;

        int offsetRadius = Math.max(0, config.randomTargetOffsetRadius());
        if (offsetRadius > 0)
        {
            int maxOffsetX = Math.min(offsetRadius, width / 2);
            int maxOffsetY = Math.min(offsetRadius, height / 2);

            if (maxOffsetX > 0 || maxOffsetY > 0)
            {
                int offsetX = maxOffsetX > 0 ? random.nextInt(maxOffsetX * 2 + 1) - maxOffsetX : 0;
                int offsetY = maxOffsetY > 0 ? random.nextInt(maxOffsetY * 2 + 1) - maxOffsetY : 0;

                targetX = Math.max(loc.getX() + 1, Math.min(loc.getX() + width - 1, centerX + offsetX));
                targetY = Math.max(loc.getY() + 1, Math.min(loc.getY() + height - 1, centerY + offsetY));
            }
        }

        return new CombinedClickAction(targetX, targetY, "spell: " + trimmed, false, true);
    }

    private void runSequentialRobotPath(List<CombinedClickAction> actions, int totalTimeMs)
    {
        if (actions == null || actions.isEmpty())
        {
            return;
        }

        Component canvas = client != null ? client.getCanvas() : null;
        if (canvas == null)
        {
            Logger.warn("[Human Equipper] Client canvas is null; cannot run sequential robot path.");
            return;
        }

        Point canvasOnScreen;
        try
        {
            canvasOnScreen = canvas.getLocationOnScreen();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to get canvas location on screen for sequential path: " + e.getMessage());
            return;
        }

        if (canvasOnScreen == null)
        {
            Logger.warn("[Human Equipper] Canvas on-screen location is null for sequential path.");
            return;
        }

        Robot robot;
        try
        {
            robot = new Robot();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to create Robot for sequential path: " + e.getMessage());
            return;
        }

        int clampedTotal = Math.max(1, Math.min(5000, totalTimeMs));
        double speedFactor = Math.max(0.1, config.speedPercent() / 100.0);
        long effectiveTotalMs = (long) Math.max(1.0, clampedTotal / speedFactor);

        long perActionBudget = Math.max(40L, effectiveTotalMs / actions.size());
        int stepSleepMs = Math.max(5, (int) config.smoothingIntervalMs());
        double baseStepFactor = 0.4;

        for (CombinedClickAction action : actions)
        {
            if (!isRunning)
            {
                return;
            }

            ensureTabForAction(action);

            long start = System.currentTimeMillis();

            int targetAbsX = canvasOnScreen.x + action.getTargetX();
            int targetAbsY = canvasOnScreen.y + action.getTargetY();
            while (System.currentTimeMillis() - start < perActionBudget && isRunning)
            {
                Point mouseOnScreen = MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : null;
                if (mouseOnScreen == null)
                {
                    Logger.warn("[Human Equipper] Mouse pointer location is null during sequential path.");
                    return;
                }

                int dx = targetAbsX - mouseOnScreen.x;
                int dy = targetAbsY - mouseOnScreen.y;

                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1)
                {
                    break;
                }

                double stepFactor = baseStepFactor;

                // Simple straight-line step towards target.
                int stepX = mouseOnScreen.x + (int) Math.round(dx * stepFactor);
                int stepY = mouseOnScreen.y + (int) Math.round(dy * stepFactor);

                robot.mouseMove(stepX, stepY);

                try
                {
                    Thread.sleep(stepSleepMs);
                }
                catch (InterruptedException ignored)
                {
                    return;
                }
            }

            int clickX = canvasOnScreen.x + action.getTargetX();
            int clickY = canvasOnScreen.y + action.getTargetY();
            robot.mouseMove(clickX, clickY);

            int holdMs = Math.max(0, Math.min(120, config.clickHoldMillis()));
            try
            {
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                if (holdMs > 0)
                {
                    Thread.sleep(holdMs);
                }
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            }
            catch (InterruptedException ignored)
            {
                return;
            }
        }
    }

    private void runCombinedSequence(List<CombinedClickAction> clickActions, int totalTimeMs)
    {
        if (clickActions == null || clickActions.isEmpty())
        {
            return;
        }

        Component canvas = client != null ? client.getCanvas() : null;
        if (canvas == null)
        {
            Logger.warn("[Human Equipper] Client canvas is null; cannot compute screen coordinates for combined path.");
            return;
        }

        Point canvasOnScreen;
        try
        {
            canvasOnScreen = canvas.getLocationOnScreen();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to get canvas location on screen for combined path: " + e.getMessage());
            return;
        }

        if (canvasOnScreen == null)
        {
            Logger.warn("[Human Equipper] Canvas on-screen location is null for combined path.");
            return;
        }

        Point mouseOnScreen = MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : null;
        if (mouseOnScreen == null)
        {
            Logger.warn("[Human Equipper] Mouse pointer location is null; cannot move to combined path.");
            return;
        }

        int startX = mouseOnScreen.x - canvasOnScreen.x;
        int startY = mouseOnScreen.y - canvasOnScreen.y;

        TrajectoryGenerator generator = TrajectoryService.createGenerator();

        long baseStepMs = Math.max(1L, (long) config.smoothingIntervalMs());
        long syntheticTime = 0L;
        List<MouseDataPoint> points = new ArrayList<>();
        List<Integer> clickIndices = new ArrayList<>();

        int currentX = startX;
        int currentY = startY;

        for (int i = 0; i < clickActions.size(); i++)
        {
            if (!isRunning)
            {
                return;
            }

            CombinedClickAction action = clickActions.get(i);
            int targetX = action.getTargetX();
            int targetY = action.getTargetY();

            MouseMovementSequence segmentSeq = generator.generate(currentX, currentY, targetX, targetY, System.currentTimeMillis());
            List<MouseDataPoint> segmentPoints = segmentSeq.getPoints();
            if (segmentPoints == null || segmentPoints.isEmpty())
            {
                continue;
            }

            for (int s = 0; s < segmentPoints.size(); s++)
            {
                if (i > 0 && s == 0)
                {
                    continue;
                }

                MouseDataPoint p = segmentPoints.get(s);
                points.add(new MouseDataPoint(p.getX(), p.getY(), syntheticTime));
                syntheticTime += baseStepMs;
            }

            // Force the end of this segment to land exactly on the intended target
            if (!points.isEmpty())
            {
                int lastIdx = points.size() - 1;
                MouseDataPoint last = points.get(lastIdx);
                points.set(lastIdx, new MouseDataPoint(targetX, targetY, last.getTimestampMillis()));
                clickIndices.add(lastIdx);
            }

            currentX = targetX;
            currentY = targetY;
        }

        if (points.isEmpty())
        {
            Logger.warn("[Human Equipper] Combined stitched path has no points; skipping combined path.");
            return;
        }

        Robot robot;
        try
        {
            robot = new Robot();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to create Robot for combined path: " + e.getMessage());
            return;
        }

        // Allow up to 5 seconds for the combined path so timing adjustments are clearly visible.
        int clampedTotal = Math.max(1, Math.min(5000, totalTimeMs));
        double speedFactor = Math.max(0.1, config.speedPercent() / 100.0);
        long effectiveTotalMs = (long) Math.max(1.0, clampedTotal / speedFactor);

        int sampleCount = points.size();
        double perSampleMs = sampleCount > 1 ? (double) effectiveTotalMs / (double) (sampleCount - 1) : (double) effectiveTotalMs;

        int holdMs = Math.max(0, Math.min(120, config.clickHoldMillis()));

        long wallStart = System.currentTimeMillis();
        int nextClickIdx = 0;
        int nextClickSample = clickIndices.get(0);

        ensureTabForAction(clickActions.get(0));

        for (int i = 0; i < points.size(); i++)
        {
            if (!isRunning)
            {
                return;
            }

            MouseDataPoint point = points.get(i);
            long targetWall = wallStart + (long) Math.round(i * perSampleMs);
            long sleepMs = targetWall - System.currentTimeMillis();
            if (sleepMs > 0)
            {
                try
                {
                    Thread.sleep(sleepMs);
                }
                catch (InterruptedException ignored)
                {
                    return;
                }
            }

            int screenX = canvasOnScreen.x + point.getX();
            int screenY = canvasOnScreen.y + point.getY();
            robot.mouseMove(screenX, screenY);

            if (nextClickIdx < clickIndices.size() && i == nextClickSample)
            {
                try
                {
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    if (holdMs > 0)
                    {
                        Thread.sleep(holdMs);
                    }
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                }
                catch (InterruptedException ignored)
                {
                    return;
                }

                nextClickIdx++;
                if (nextClickIdx < clickIndices.size())
                {
                    nextClickSample = clickIndices.get(nextClickIdx);
                    ensureTabForAction(clickActions.get(nextClickIdx));
                }
            }
        }
    }

    /* Legacy combined-path implementation kept for reference only.
    private void runCombinedPathLegacy(List<CombinedClickAction> clickActions, int totalTimeMs)
    {
        if (clickActions == null || clickActions.isEmpty())
        {
            return;
        }

        Component canvas = client != null ? client.getCanvas() : null;
        if (canvas == null)
        {
            Logger.warn("[Human Equipper] Client canvas is null; cannot compute screen coordinates for combined path.");
            return;
        }

        Point canvasOnScreen;
        try
        {
            canvasOnScreen = canvas.getLocationOnScreen();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to get canvas location on screen for combined path: " + e.getMessage());
            return;
        }

        if (canvasOnScreen == null)
        {
            Logger.warn("[Human Equipper] Canvas on-screen location is null for combined path.");
            return;
        }

        Point mouseOnScreen = MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : null;
        if (mouseOnScreen == null)
        {
            Logger.warn("[Human Equipper] Mouse pointer location is null; cannot move to combined path.");
            return;
        }

        int startX = mouseOnScreen.x - canvasOnScreen.x;
        int startY = mouseOnScreen.y - canvasOnScreen.y;

        TrajectoryGenerator generator = TrajectoryService.createGenerator();

        // Build a single stitched list of points across all segments, and remember
        // the index of the last point for each click target.
        long baseStepMs = Math.max(1L, (long) config.smoothingIntervalMs());
        long syntheticTime = 0L;
        List<MouseDataPoint> points = new ArrayList<>();
        List<Integer> clickIndices = new ArrayList<>();

        int currentX = startX;
        int currentY = startY;

        for (int i = 0; i < clickActions.size(); i++)
        {
            if (!isRunning)
            {
                return;
            }

            CombinedClickAction action = clickActions.get(i);
            int targetX = action.getTargetX();
            int targetY = action.getTargetY();

            MouseMovementSequence segmentSeq = generator.generate(currentX, currentY, targetX, targetY, System.currentTimeMillis());
            List<MouseDataPoint> segmentPoints = segmentSeq.getPoints();
            if (segmentPoints == null || segmentPoints.isEmpty())
            {
                continue;
            }

            for (int s = 0; s < segmentPoints.size(); s++)
            {
                // Skip the first point of subsequent segments to avoid duplicates
                if (i > 0 && s == 0)
                {
                    continue;
                }

                MouseDataPoint p = segmentPoints.get(s);
                points.add(new MouseDataPoint(p.getX(), p.getY(), syntheticTime));
                syntheticTime += baseStepMs;
            }

            // Click when we reach the last point of this segment
            clickIndices.add(points.size() - 1);
            currentX = targetX;
            currentY = targetY;
        }

        if (points.isEmpty())
        {
            Logger.warn("[Human Equipper] Combined stitched path has no points; skipping combined path.");
            return;
        }

        Robot robot;
        try
        {
            robot = new Robot();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to create Robot for combined path: " + e.getMessage());
            return;
        }

        // Use totalTimeMs as the primary control for total duration, scaled by speedPercent.
        int clampedTotal = Math.max(1, Math.min(2400, totalTimeMs));
        double speedFactor = Math.max(0.1, config.speedPercent() / 100.0);
        long effectiveTotalMs = (long) Math.max(1.0, clampedTotal / speedFactor);

        // Spread the time evenly across all samples so the whole motion takes ~effectiveTotalMs.
        int sampleCount = points.size();
        double perSampleMs = sampleCount > 1 ? (double) effectiveTotalMs / (double) (sampleCount - 1) : (double) effectiveTotalMs;

        int holdMs = Math.max(0, Math.min(120, config.clickHoldMillis()));

        long wallStart = System.currentTimeMillis();
        int nextClickIdx = 0;
        int nextClickSample = clickIndices.get(0);

        // Ensure the correct tab is active for the first segment before we start moving.
        ensureTabForAction(clickActions.get(0));

        for (int i = 0; i < points.size(); i++)
        {
            if (!isRunning)
            {
                return;
            }

            MouseDataPoint point = points.get(i);
            long targetWall = wallStart + (long) Math.round(i * perSampleMs);
            long sleepMs = targetWall - System.currentTimeMillis();
            if (sleepMs > 0)
            {
                try
                {
                    Thread.sleep(sleepMs);
                }
                catch (InterruptedException ignored)
                {
                    return;
                }
            }

            int screenX = canvasOnScreen.x + point.getX();
            int screenY = canvasOnScreen.y + point.getY();
            robot.mouseMove(screenX, screenY);

            if (nextClickIdx < clickIndices.size() && i == nextClickSample)
            {
                try
                {
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    Thread.sleep(holdMs);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                }
                catch (InterruptedException ignored)
                {
                    return;
                }

                nextClickIdx++;
                if (nextClickIdx < clickIndices.size())
                {
                    nextClickSample = clickIndices.get(nextClickIdx);
                    ensureTabForAction(clickActions.get(nextClickIdx));
                }
            }
        }
    }
    */

    private static final class CombinedClickAction
    {
        private final int targetX;
        private final int targetY;
        private final String contextLabel;
        private final boolean inventoryTab;
        private final boolean magicTab;

        private CombinedClickAction(int targetX, int targetY, String contextLabel, boolean inventoryTab, boolean magicTab)
        {
            this.targetX = targetX;
            this.targetY = targetY;
            this.contextLabel = contextLabel;
            this.inventoryTab = inventoryTab;
            this.magicTab = magicTab;
        }

        int getTargetX()
        {
            return targetX;
        }

        int getTargetY()
        {
            return targetY;
        }

        String getContextLabel()
        {
            return contextLabel;
        }

        boolean isInventoryTab()
        {
            return inventoryTab;
        }

        boolean isMagicTab()
        {
            return magicTab;
        }
    }

    private void ensureTabForAction(CombinedClickAction action)
    {
        if (action == null)
        {
            return;
        }

        if (action.isInventoryTab())
        {
            pressKeybind(config.inventoryTabKey());
        }
        else if (action.isMagicTab())
        {
            pressKeybind(config.magicTabKey());
        }
    }

    private List<MouseDataPoint> smoothPoints(List<MouseDataPoint> points, long targetIntervalMs)
    {
        if (points == null || points.size() < 2 || targetIntervalMs <= 0L)
        {
            return points;
        }

        List<MouseDataPoint> result = new ArrayList<>();
        MouseDataPoint prev = points.get(0);
        result.add(prev);

        for (int i = 1; i < points.size(); i++)
        {
            MouseDataPoint next = points.get(i);
            long prevTime = prev.getTimestampMillis();
            long nextTime = next.getTimestampMillis();
            long dt = nextTime - prevTime;

            if (dt > targetIntervalMs)
            {
                int steps = (int) (dt / targetIntervalMs);

                int segDx = next.getX() - prev.getX();
                int segDy = next.getY() - prev.getY();
                double segLen = Math.sqrt(segDx * segDx + segDy * segDy);

                int curveStrength = Math.max(0, Math.min(100, config.curveStrengthPercent()));
                double curveAmount = curveStrength / 100.0;
                double orthoX = 0.0;
                double orthoY = 0.0;
                double maxCurvePixels = 0.0;

                if (curveAmount > 0.0 && segLen > 0.0)
                {
                    orthoX = -segDy / segLen;
                    orthoY = segDx / segLen;
                    maxCurvePixels = Math.min(25.0, segLen * 0.3);
                }

                for (int s = 1; s <= steps; s++)
                {
                    long t = prevTime + s * targetIntervalMs;
                    if (t >= nextTime)
                    {
                        break;
                    }

                    double alpha = (double) (t - prevTime) / (double) dt;
                    double baseX = prev.getX() + (next.getX() - prev.getX()) * alpha;
                    double baseY = prev.getY() + (next.getY() - prev.getY()) * alpha;

                    double curveX = baseX;
                    double curveY = baseY;

                    if (curveAmount > 0.0 && maxCurvePixels > 0.0)
                    {
                        double sinFactor = Math.sin(Math.PI * alpha);
                        double offsetMag = curveAmount * maxCurvePixels * sinFactor;
                        curveX += orthoX * offsetMag;
                        curveY += orthoY * offsetMag;
                    }

                    int x = (int) Math.round(curveX);
                    int y = (int) Math.round(curveY);
                    int noiseStrength = Math.max(0, Math.min(100, config.pathNoiseStrength()));
                    if (noiseStrength > 0)
                    {
                        double strength = noiseStrength / 100.0;
                        int maxJitter = 3;
                        int jitterRange = (int) Math.round(maxJitter * strength);
                        if (jitterRange > 0)
                        {
                            int jitterX = random.nextInt(jitterRange * 2 + 1) - jitterRange;
                            int jitterY = random.nextInt(jitterRange * 2 + 1) - jitterRange;
                            x += jitterX;
                            y += jitterY;
                        }
                    }

                    result.add(new MouseDataPoint(x, y, t));
                }
            }

            result.add(next);
            prev = next;
        }

        return result;
    }

    private void clickCurrentTargetWithRobot()
    {
        ActorEx<?> target = cachedTarget;
        long now = System.currentTimeMillis();

        if (target == null || cachedTargetTime <= 0L || now - cachedTargetTime > TARGET_CACHE_DURATION_MS)
        {
            Logger.warn("[Human Equipper] No recent cached target to click.");
            return;
        }

        Component canvas = client != null ? client.getCanvas() : null;
        if (canvas == null)
        {
            Logger.warn("[Human Equipper] Client canvas is null; cannot click target.");
            return;
        }

        Point canvasOnScreen;
        try
        {
            canvasOnScreen = canvas.getLocationOnScreen();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to get canvas location on screen for target: " + e.getMessage());
            return;
        }

        if (canvasOnScreen == null)
        {
            Logger.warn("[Human Equipper] Canvas on-screen location is null for target.");
            return;
        }

        Robot robot;
        try
        {
            robot = new Robot();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to create Robot for target: " + e.getMessage());
            return;
        }

        long start = System.currentTimeMillis();
        long maxTrackDuration = 220L; // Track target position for up to ~0.2s

        while (System.currentTimeMillis() - start < maxTrackDuration && isRunning)
        {
            Shape clickBox = target.getShape();
            if (clickBox == null)
            {
                Logger.warn("[Human Equipper] No clickbox for target actor during tracking.");
                return;
            }

            Rectangle bounds = clickBox.getBounds();
            if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
            {
                Logger.warn("[Human Equipper] Invalid bounds for target actor during tracking.");
                return;
            }

            int centerX = bounds.x + bounds.width / 2;
            int centerY = bounds.y + bounds.height / 2;
            int targetX = centerX;
            int targetY = centerY;

            Point mouseOnScreen = MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : null;
            if (mouseOnScreen == null)
            {
                Logger.warn("[Human Equipper] Mouse pointer location is null during target tracking.");
                return;
            }

            int screenTargetX = canvasOnScreen.x + targetX;
            int screenTargetY = canvasOnScreen.y + targetY;

            int dx = screenTargetX - mouseOnScreen.x;
            int dy = screenTargetY - mouseOnScreen.y;

            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1)
            {
                break; // Close enough to click
            }

            // Move a fraction towards the current target position for smoother tracking
            double stepFactor = 0.4; // lower = smoother, still fast enough to keep up
            int stepX = mouseOnScreen.x + (int) Math.round(dx * stepFactor);
            int stepY = mouseOnScreen.y + (int) Math.round(dy * stepFactor);

            robot.mouseMove(stepX, stepY);

            try
            {
                Thread.sleep(10L);
            }
            catch (InterruptedException ignored)
            {
                return;
            }
        }

        // Final recalc and click at the latest known position
        Shape finalBox = target.getShape();
        if (finalBox == null)
        {
            Logger.warn("[Human Equipper] No final clickbox for target actor.");
            return;
        }

        Rectangle finalBounds = finalBox.getBounds();
        if (finalBounds == null || finalBounds.width <= 0 || finalBounds.height <= 0)
        {
            Logger.warn("[Human Equipper] Invalid final bounds for target actor.");
            return;
        }

        int finalCenterX = finalBounds.x + finalBounds.width / 2;
        int finalCenterY = finalBounds.y + finalBounds.height / 2;

        int finalTargetX = finalCenterX;
        int finalTargetY = finalCenterY;

        int finalOffsetRadius = Math.max(0, config.randomTargetOffsetRadius());
        if (finalOffsetRadius > 0)
        {
            int maxOffsetX = Math.min(finalOffsetRadius, finalBounds.width / 2);
            int maxOffsetY = Math.min(finalOffsetRadius, finalBounds.height / 2);

            if (maxOffsetX > 0 || maxOffsetY > 0)
            {
                int offsetX = maxOffsetX > 0 ? random.nextInt(maxOffsetX * 2 + 1) - maxOffsetX : 0;
                int offsetY = maxOffsetY > 0 ? random.nextInt(maxOffsetY * 2 + 1) - maxOffsetY : 0;

                finalTargetX = Math.max(finalBounds.x + 1, Math.min(finalBounds.x + finalBounds.width - 1, finalCenterX + offsetX));
                finalTargetY = Math.max(finalBounds.y + 1, Math.min(finalBounds.y + finalBounds.height - 1, finalCenterY + offsetY));
            }
        }

        int finalScreenX = canvasOnScreen.x + finalTargetX;
        int finalScreenY = canvasOnScreen.y + finalTargetY;
        robot.mouseMove(finalScreenX, finalScreenY);

        int holdMs = Math.max(0, Math.min(120, config.clickHoldMillis()));

        try
        {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(holdMs);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
        catch (InterruptedException ignored)
        {
            // Ignore
        }
    }

    private void pressKeybind(Keybind keybind)
    {
        if (keybind == null || keybind == Keybind.NOT_SET)
        {
            return;
        }

        int keyCode;
        int modifiers;
        try
        {
            keyCode = keybind.getKeyCode();
            modifiers = keybind.getModifiers();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to read keybind: " + e.getMessage());
            return;
        }

        if (keyCode <= 0)
        {
            return;
        }

        Robot robot;
        try
        {
            robot = new Robot();
        }
        catch (Exception e)
        {
            Logger.error("[Human Equipper] Failed to create Robot for keybind: " + e.getMessage());
            return;
        }

        try
        {
            // Press modifiers first
            if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0)
            {
                robot.keyPress(KeyEvent.VK_SHIFT);
            }
            if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0)
            {
                robot.keyPress(KeyEvent.VK_CONTROL);
            }
            if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0)
            {
                robot.keyPress(KeyEvent.VK_ALT);
            }
            if ((modifiers & InputEvent.META_DOWN_MASK) != 0)
            {
                robot.keyPress(KeyEvent.VK_META);
            }

            robot.keyPress(keyCode);
            Thread.sleep(5L);
            robot.keyRelease(keyCode);

            // Release modifiers
            if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0)
            {
                robot.keyRelease(KeyEvent.VK_SHIFT);
            }
            if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0)
            {
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }
            if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0)
            {
                robot.keyRelease(KeyEvent.VK_ALT);
            }
            if ((modifiers & InputEvent.META_DOWN_MASK) != 0)
            {
                robot.keyRelease(KeyEvent.VK_META);
            }
        }
        catch (InterruptedException ignored)
        {
            // Ignore
        }
    }

    ActorEx<?> getCachedTarget()
    {
        return cachedTarget;
    }
}
