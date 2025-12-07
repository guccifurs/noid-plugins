/**
 * ════════════════════════════════════════════════════════════════════════════════
 * 🎯 GEAR SWAPPER PLUGIN v3.0 - PRODUCTION READY
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * @AI_GOAL: Advanced PvP gear swapping plugin with innovative trigger system
 * @AI_TARGET: Production-ready plugin with flawless functionality
 * @AI_FEATURES: Loadouts, Hotkeys, Triggers, Actions, Real-time Events
 * @AI_ARCHITECTURE: Modular design with clean separation of concerns
 * @AI_SAFETY: Thread-safe, error-resilient, game-compliant execution
 *
 * ════════════════════════════════════════════════════════════════════════════════
 * 🎮 CORE FUNCTIONALITY
 * ════════════════════════════════════════════════════════════════════════════════
 * @AI_CORE_1: 5-slot gear loadout system with instant hotkey swapping
 * @AI_CORE_2: Item name resolution with wildcard support (*)
 * @AI_CORE_3: Spell casting with magic level fallback
 * @AI_CORE_4: Auto-attack functionality for PvP arena
 * @AI_CORE_5: Real-time trigger system based on game events
 *
 * ════════════════════════════════════════════════════════════════════════════════
 * 🎯 TRIGGER SYSTEM ARCHITECTURE
 * ════════════════════════════════════════════════════════════════════════════════
 * @AI_TRIGGER_ENGINE: Core engine processing game events and firing triggers
 * @AI_TRIGGER_TYPES: HP, Animation, Custom event types
 * @AI_TRIGGER_ACTIONS: GearSwap, Prayer, Spell casting actions
 * @AI_TRIGGER_HANDLERS: Event-specific handlers (HpTriggerHandler, etc.)
 *
 * ════════════════════════════════════════════════════════════════════════════════
 * 🔧 EXECUTION FLOW
 * ════════════════════════════════════════════════════════════════════════════════
 * @AI_FLOW_1: Plugin loads → Initialize trigger engine and loadouts
 * @AI_FLOW_2: User configures triggers via UI → Saved to config
 * @AI_FLOW_3: Game events → Trigger engine processes → Actions execute
 * @AI_FLOW_4: Gear swaps → Items equipped instantly → Spells cast → Attack
 *
 * ════════════════════════════════════════════════════════════════════════════════
 * 🛡️ SAFETY & PERFORMANCE
 * ════════════════════════════════════════════════════════════════════════════════
 * @AI_SAFETY_1: Thread-safe trigger processing with read/write locks
 * @AI_SAFETY_2: Error isolation - one failed action doesn't stop others
 * @AI_SAFETY_3: Cooldown system prevents trigger spam
 * @AI_SAFETY_4: Combat state validation before executing actions
 * @AI_PERFORMANCE: Cached item lookups, pre-calculated spell data
 *
 * ════════════════════════════════════════════════════════════════════════════════
 */
package com.tonic.plugins.gearswapper;

import com.google.inject.Injector;
import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.SkillAPI;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.MagicAPI;
import com.tonic.api.widgets.PrayerAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.magic.spellbooks.Ancient;
import com.tonic.data.EquipmentSlot;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.PlayerQuery;
import com.tonic.plugins.gearswapper.triggers.TriggerEngine;
import com.tonic.plugins.gearswapper.triggers.TriggerEngineStats;
import com.tonic.plugins.gearswapper.triggers.TriggerEvent;
import com.tonic.plugins.gearswapper.triggers.TriggerEventType;
import com.tonic.plugins.gearswapper.ui.GearSwapperPanel;
import com.tonic.plugins.gearswapper.ui.overlay.DebugOverlay;
import net.runelite.client.eventbus.Subscribe;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.util.ImageUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.queries.NpcQuery;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.plugins.gearswapper.script.ScriptStopException;

@Slf4j
@PluginDescriptor(name = "<html><font color='#00FF00'>[NP]</font> NoidSwap</html>", description = "NoidSwap - advanced PvP loadouts with trigger engine v3 and debug system", tags = {
        "gear", "swap", "pvp", "pk", "loadout", "triggers", "debug" })
public class GearSwapperPlugin extends Plugin {
    private static final BufferedImage PLUGIN_ICON = createDefaultIcon();

    @Inject
    private Client client;

    @Inject
    private GearSwapperConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private GearSwapperOverlay overlay;

    @Inject
    private DebugOverlay debugOverlay;

    @Inject
    private InventoryUtilsOverlay inventoryUtilsOverlay;

    @Inject
    private InventoryUtilsButtonManager buttonManager;

    @Inject
    private Injector injector;

    @Inject
    private TriggerEngine triggerEngine;

    @Inject
    private PluginManager pluginManager;

    private GearSwapperPanel panel;
    private NavigationButton navigationButton;

    // Fight-awareness state
    private Player currentTarget;
    private String currentTargetName;
    private Player cachedTarget;
    private long cachedTargetTime = 0L;
    private int targetDistance = -1;
    private static final int FREEZE_DURATION_TICKS = 33;
    private static final int FREEZE_IMMUNITY_TICKS = 4;
    private static final long TARGET_CACHE_DURATION_MS = 300_000L; // 5 min cached target
    private static final int[] FREEZE_GFX_IDS = { 369, 360, 361, 363, 358, 359, 143, 144 };
    private int playerFreezeTicks;
    private int targetFreezeTicks;
    private int playerFreezeImmunityTicks;
    private int targetFreezeImmunityTicks;
    private WorldPoint playerFreezeLocation;
    private WorldPoint targetFreezeLocation;
    private int tickCounter;
    private boolean brewClickedThisTick;
    private int brewFlagTick = -1;
    private boolean sanfewClickedThisTick;
    private int sanfewFlagTick = -1;
    private String lastLoadoutName = "None";
    private int lastLoadoutNum = -1;
    private int lastSwapTick = -1;

    // Looper script state
    private volatile boolean looperEnabled;
    private String looperScript = "";

    // DropAll state - tracks ongoing drop operations across ticks
    private String dropAllPattern = null;
    private int dropAllItemsPerTick = 0;

    // Looper pause state - for RandomAfkChance and Wait commands
    private int looperPauseTicks = 0;

    // Pending commands to execute after Wait completes
    // When Wait is encountered, remaining commands in the block are stored here
    private String[] pendingLooperCommands = null;
    private int pendingLooperCommandIndex = 0;

    // Tick timer countdown for looper scripts (SetTickTimer command)
    private int tickTimerValue = 0;

    // Click heatmap state
    private volatile boolean clickHeatmapEnabled = false;
    private int minClickDelay = 100; // ms
    private int maxClickDelay = 500; // ms
    private final java.util.Random clickDelayRandom = new java.util.Random();

    // Force 1 tick state
    private volatile boolean forceOneTickEnabled = false;
    private int forceTickThresholdMs = 500; // within-tick window
    private volatile long lastTickStartMs = 0L;

    // When true, a script is being executed under a single global heatmap delay
    private boolean inScriptHeatmapBlock = false;

    // Heatmap ordered execution queue (ensures script commands execute in order)
    private final Object heatmapLock = new Object();
    private java.util.Queue<Runnable> heatmapQueue = new java.util.ArrayDeque<>();
    private java.util.concurrent.ScheduledExecutorService heatmapScheduler;
    private boolean heatmapRunnerActive = false;

    // Dynamic hotkey listeners for unlocked loadouts (up to 20)
    private final List<KeyListener> dynamicLoadoutListeners = new ArrayList<>();

    // Dedicated hotkey for clearing current target
    private KeyListener clearTargetKeyListener;

    // Tick-based script scheduler for Tick:N delays
    private final List<ScriptTask> scheduledScriptTasks = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * Represents a scheduled script task to run on a specific game tick.
     */
    private static class ScriptTask {
        final int targetTick;
        final Runnable action;
        final String description;

        ScriptTask(int targetTick, Runnable action, String description) {
            this.targetTick = targetTick;
            this.action = action;
            this.description = description;
        }
    }

    private volatile boolean mouseCircleTestRunning = false;
    private long mouseCircleTestStartMs = 0L;
    private long mouseCircleTestDurationMs = 10_000L;
    private int mouseCircleCenterX = -1;
    private int mouseCircleCenterY = -1;
    private int mouseCircleRadius = 80;

    // Recent animation tracking for debug overlay
    private final java.util.Deque<AnimationDebugEntry> recentAnimations = new java.util.ArrayDeque<>();

    public static class AnimationDebugEntry {
        public final int animationId;
        public final boolean ours;
        public final boolean targ;
        public final long timestamp;

        public AnimationDebugEntry(int animationId, boolean ours, boolean targ, long timestamp) {
            this.animationId = animationId;
            this.ours = ours;
            this.targ = targ;
            this.timestamp = timestamp;
        }
    }

    // Helper methods to get loadout data from config manager
    private String getLoadoutName(int num) {
        return configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + num + "Name");
    }

    /**
     * Get all configured loadout names (for Inventory Utils dropdown)
     * Returns list of non-empty loadout names
     */
    public java.util.List<String> getConfiguredLoadoutNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String name = getLoadoutName(i);
            if (name != null && !name.trim().isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    // Public wrapper methods for trigger system
    public String getLoadoutNameForTrigger(int num) {
        return getLoadoutName(num);
    }

    public String[] getLoadoutItemsForTrigger(int num) {
        String itemsString = getLoadoutItems(num);
        if (itemsString != null && !itemsString.trim().isEmpty()) {
            return itemsString.split(",");
        }
        return new String[0];
    }

    public void applyPrayersForTrigger(String[] prayers) {
        applyPrayers(prayers);
    }

    public void castSpellForTrigger(String[] spells) {
        castSpellWithFallback(spells);
    }

    public void attackForTrigger() {
        attackCurrentTarget();
    }

    // Additional wrapper methods for enhanced trigger functionality
    public String getLoadoutSpellForTrigger(int num) {
        return getLoadoutSpell(num);
    }

    public String getLoadoutFrozenSpellForTrigger(int num) {
        return getLoadoutFrozenSpell(num);
    }

    public String getLoadoutPrayerForTrigger(int num) {
        return getLoadoutPrayer(num);
    }

    public String getLoadoutAttackForTrigger(int num) {
        return getLoadoutAttack(num) ? "Enabled" : "Disabled";
    }

    public boolean isMouseCircleTestRunning() {
        return mouseCircleTestRunning;
    }

    /**
     * Execute a loadout by its name (for Inventory Utils overlay)
     * Searches through all loadouts to find one matching the given name
     */
    public void executeLoadoutByName(String loadoutName) {
        if (loadoutName == null || loadoutName.isEmpty()) {
            Logger.warn("[Gear Swapper] executeLoadoutByName called with empty name");
            return;
        }

        // Search through loadouts 1-20 to find matching name
        for (int i = 1; i <= 20; i++) {
            String name = getLoadoutName(i);
            if (name != null && name.equalsIgnoreCase(loadoutName.trim())) {
                Logger.norm("[Gear Swapper] Found loadout '" + loadoutName + "' at slot " + i);

                // Get loadout data and execute
                String items = getLoadoutItems(i);
                boolean attack = getLoadoutAttack(i);

                if (items != null && !items.isEmpty()) {
                    executeCommands(items, attack);
                } else {
                    Logger.warn("[Gear Swapper] Loadout '" + loadoutName + "' has no items configured");
                }
                return;
            }
        }

        // Also check if the name is a number (e.g., "1", "2") to directly select
        // loadout slot
        try {
            int slotNum = Integer.parseInt(loadoutName.trim());
            if (slotNum >= 1 && slotNum <= 20) {
                String items = getLoadoutItems(slotNum);
                boolean attack = getLoadoutAttack(slotNum);

                if (items != null && !items.isEmpty()) {
                    Logger.norm("[Gear Swapper] Executing loadout slot " + slotNum);
                    executeCommands(items, attack);
                } else {
                    Logger.warn("[Gear Swapper] Loadout slot " + slotNum + " has no items configured");
                }
                return;
            }
        } catch (NumberFormatException ignored) {
            // Not a number, that's fine
        }

        Logger.warn("[Gear Swapper] Could not find loadout named '" + loadoutName + "'");
    }

    /**
     * Activate a prayer by name (for Inventory Utils overlay)
     * Supports common prayer names like "Protect from Melee", "Piety", etc.
     */
    public void activatePrayerByName(String prayerName) {
        if (prayerName == null || prayerName.isEmpty()) {
            Logger.warn("[Gear Swapper] activatePrayerByName called with empty name");
            return;
        }

        // Use existing prayer system - wrap in array
        String[] prayers = new String[] { prayerName.trim() };
        applyPrayers(prayers);
    }

    public long getMouseCircleTestStartMs() {
        return mouseCircleTestStartMs;
    }

    public long getMouseCircleTestDurationMs() {
        return mouseCircleTestDurationMs;
    }

    public int getMouseCircleCenterX() {
        return mouseCircleCenterX;
    }

    public int getMouseCircleCenterY() {
        return mouseCircleCenterY;
    }

    public int getMouseCircleRadius() {
        return mouseCircleRadius;
    }

    private String getLoadoutItems(int num) {
        return configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + num + "Items");
    }

    private String getLoadoutSpell(int num) {
        return configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + num + "Spell");
    }

    private String getLoadoutFrozenSpell(int num) {
        return configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + num + "FrozenSpell");
    }

    private String getLoadoutPrayer(int num) {
        return configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + num + "Prayer");
    }

    private boolean getLoadoutAttack(int num) {
        String attackStr = configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + num + "Attack");
        return "true".equals(attackStr);
    }

    private Keybind getLoadoutKeybind(int num) {
        // Load keybind from config manager
        String keybindStr = configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + num + "Keybind");
        if (keybindStr != null && !keybindStr.isEmpty()) {
            try {
                // Parse keybind from string format
                // Format is like "ctrl+alt+1" or "shift+F2"
                String[] parts = keybindStr.toLowerCase().split("\\+");
                int modifiers = 0;
                int keyCode = -1;

                for (String part : parts) {
                    switch (part.trim()) {
                        case "ctrl":
                            modifiers |= KeyEvent.CTRL_DOWN_MASK;
                            break;
                        case "alt":
                            modifiers |= KeyEvent.ALT_DOWN_MASK;
                            break;
                        case "shift":
                            modifiers |= KeyEvent.SHIFT_DOWN_MASK;
                            break;
                        case "meta":
                            modifiers |= KeyEvent.META_DOWN_MASK;
                            break;
                        default:
                            // This should be the key code
                            if (part.length() == 1) {
                                keyCode = KeyEvent.getExtendedKeyCodeForChar(part.charAt(0));
                            } else {
                                // Try to parse as key name
                                try {
                                    keyCode = KeyEvent.class.getField("VK_" + part.toUpperCase()).getInt(null);
                                } catch (Exception e) {
                                    // Fallback: try to get from character
                                    keyCode = KeyEvent.getExtendedKeyCodeForChar(part.charAt(0));
                                }
                            }
                            break;
                    }
                }

                if (keyCode != -1) {
                    return new Keybind(keyCode, modifiers);
                }
            } catch (Exception e) {
                Logger.norm("[Gear Swapper] Failed to parse keybind: " + keybindStr);
            }
        }
        return Keybind.NOT_SET;
    }

    @Override
    protected void startUp() throws Exception {
        // ══════════════════════════════════════════════════════════════
        // 🔒 NOID SUBSCRIPTION CHECK - Plugin requires active Noid subscription
        // Using reflection to avoid classloader issues with separate JARs
        // ══════════════════════════════════════════════════════════════
        boolean authenticated = false;
        String userName = "Unknown";

        for (Plugin plugin : pluginManager.getPlugins()) {
            if (plugin.getClass().getSimpleName().equals("NoidPlugin")) {
                try {
                    // Use reflection to call isAuthenticated()
                    java.lang.reflect.Method authMethod = plugin.getClass().getMethod("isAuthenticated");
                    authenticated = (Boolean) authMethod.invoke(plugin);

                    if (authenticated) {
                        // Get current user name
                        java.lang.reflect.Method userMethod = plugin.getClass().getMethod("getCurrentUser");
                        Object user = userMethod.invoke(plugin);
                        if (user != null) {
                            java.lang.reflect.Method nameMethod = user.getClass().getMethod("getDiscordName");
                            userName = (String) nameMethod.invoke(user);
                        }
                    }
                } catch (Exception e) {
                    Logger.error("[Gear Swapper] Failed to check Noid auth: " + e.getMessage());
                }
                break;
            }
        }

        if (!authenticated) {
            Logger.norm("╔══════════════════════════════════════════════════════════════╗");
            Logger.norm("║  ❌ GEAR SWAPPER REQUIRES ACTIVE NOID SUBSCRIPTION           ║");
            Logger.norm("║                                                              ║");
            Logger.norm("║  Please enable NoidPlugin and authenticate to use this.     ║");
            Logger.norm("╚══════════════════════════════════════════════════════════════╝");
            throw new Exception("Noid subscription required");
        }

        Logger.norm("[Gear Swapper] ✅ Noid subscription verified: " + userName);

        try {
            // Enhanced startup log with version and features
            Logger.norm("╔══════════════════════════════════════════════════════════════╗");
            Logger.norm("║      🛡️ GEAR SWAPPER 3.11 - LOOPER + HEATMAP 🛡️              ║");
            Logger.norm("║                                                              ║");
            Logger.norm("║  ✅ Conditional Loadouts with if / && / || / > / <           ║");
            Logger.norm("║  ✅ Per-Loadout Script Editor + Autocomplete                 ║");
            Logger.norm("║  ✅ Inline Ghost Suggestions for Commands & Conditions       ║");
            Logger.norm("║  ✅ Trigger Debug Overlays                                   ║");
            Logger.norm("║  ✅ NEW: Click Heatmap / Humanized Timing                    ║");
            Logger.norm("║                                                              ║");
            Logger.norm("║  🚀 Plugin Loaded Successfully!                             ║");
            Logger.norm("╚══════════════════════════════════════════════════════════════╝");

            // Explicit version/update line (bump this on every plugin change)
            Logger.norm("[NoidSwap] Reda het is gelukt! WOHOH");
            Logger.norm("[NoidSwap] v1.3.0 - Config with sections!");

            // Test different logging methods
            System.out.println("=== NOID SWAPPER: STARTUP TEST ===");
            System.out.println("If you see this, basic logging works!");

            try {
                com.tonic.Static.invoke(() -> {
                    System.out.println("=== VITALITE INVOKE TEST FROM PLUGIN ===");
                    return null;
                });
            } catch (Exception e) {
                System.out.println("Vitalite invoke failed from plugin: " + e.getMessage());
            }

            // Load looper configuration (script + enabled flag)
            if (configManager != null) {
                try {
                    String savedLooperScript = configManager.getConfiguration(GearSwapperConfig.GROUP, "looperScript");
                    if (savedLooperScript != null) {
                        looperScript = savedLooperScript;
                    }

                    Boolean savedLooperEnabled = configManager.getConfiguration(GearSwapperConfig.GROUP,
                            "looperEnabled", Boolean.class);
                    looperEnabled = savedLooperEnabled != null && savedLooperEnabled;
                } catch (Exception e) {
                    Logger.warn("[Gear Swapper] Failed to load looper config: " + e.getMessage());
                }

                // Load click heatmap configuration
                try {
                    Boolean savedHeatmapEnabled = configManager.getConfiguration(GearSwapperConfig.GROUP,
                            "clickHeatmapEnabled", Boolean.class);
                    clickHeatmapEnabled = savedHeatmapEnabled != null && savedHeatmapEnabled;

                    Integer savedMinDelay = configManager.getConfiguration(GearSwapperConfig.GROUP, "minClickDelay",
                            Integer.class);
                    if (savedMinDelay != null && savedMinDelay >= 0 && savedMinDelay <= 600) {
                        minClickDelay = savedMinDelay;
                    }

                    Integer savedMaxDelay = configManager.getConfiguration(GearSwapperConfig.GROUP, "maxClickDelay",
                            Integer.class);
                    if (savedMaxDelay != null && savedMaxDelay >= 0 && savedMaxDelay <= 600) {
                        maxClickDelay = savedMaxDelay;
                    }

                    Boolean savedForceOneTick = configManager.getConfiguration(GearSwapperConfig.GROUP,
                            "forceOneTickEnabled", Boolean.class);
                    forceOneTickEnabled = savedForceOneTick != null && savedForceOneTick;

                    Integer savedForceThreshold = configManager.getConfiguration(GearSwapperConfig.GROUP,
                            "forceTickThresholdMs", Integer.class);
                    if (savedForceThreshold != null && savedForceThreshold >= 0 && savedForceThreshold <= 600) {
                        forceTickThresholdMs = savedForceThreshold;
                    }
                } catch (Exception e) {
                    Logger.warn("[Gear Swapper] Failed to load click heatmap config: " + e.getMessage());
                }
            }

            registerHotkeys();
            registerClearTargetHotkey();

            panel = injector.getInstance(GearSwapperPanel.class);
            panel.setPlugin(this);
            panel.setConfig(config, configManager);

            navigationButton = NavigationButton.builder()
                    .tooltip("Gear Swapper")
                    .icon(PLUGIN_ICON)
                    .panel(panel)
                    .build();
            clientToolbar.addNavigation(navigationButton);

            overlayManager.add(overlay);

            // Add debug overlay if enabled
            if (config.showDebugOverlay()) {
                overlayManager.add(debugOverlay);
            }

            // Add Inventory Utils overlay if enabled
            if (config.inventoryUtilsEnabled()) {
                overlayManager.add(inventoryUtilsOverlay);
            }

            // Start the trigger engine with error handling
            if (triggerEngine != null) {
                triggerEngine.start();
                Logger.norm("[Gear Swapper 2.0] Trigger engine started successfully");

                // Debug: Check if engine is enabled after start
                Logger.norm("[Gear Swapper 2.0] Trigger engine enabled status: " + triggerEngine.isEnabled());

                if (!triggerEngine.isEnabled()) {
                    Logger.norm("[Gear Swapper 2.0] WARNING: Trigger engine is not enabled after start!");
                }
            } else {
                Logger.error("[Gear Swapper 2.0] Trigger engine is null - trigger system unavailable");
            }
        } catch (Exception e) {
            Logger.error("[Gear Swapper 2.0] Error during plugin startup: " + e.getMessage());
            throw e; // Re-throw to prevent plugin from starting in broken state
        }
    }

    /**
     * Public wrapper so the UI can refresh loadout hotkeys after
     * adding a new loadout or changing a keybind.
     */
    public void refreshHotkeys() {
        registerHotkeys();
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        try {
            if (event == null) {
                return;
            }

            recordAnimationForOverlay(event);

            if (triggerEngine == null || !triggerEngine.isEnabled()) {
                return;
            }

            // Create trigger event and send to trigger engine
            TriggerEvent triggerEvent = new TriggerEvent(TriggerEventType.ANIMATION_CHANGED, event);
            triggerEngine.processEvent(triggerEvent);
        } catch (Exception e) {
            Logger.error("[Gear Swapper 2.0] Error processing animation event for triggers: " + e.getMessage());
        }
    }

    @Subscribe
    public void onFakeXpDrop(FakeXpDrop event) {
        try {
            // Debug: Log event received
            System.out.println("[Gear Swapper] FakeXpDrop received: " + event.getXp() + " XP, skill: " +
                    (event.getSkill() != null ? event.getSkill().getName() : "Unknown"));

            if (event == null || !triggerEngine.isEnabled()) {
                System.out.println("[Gear Swapper] FakeXpDrop ignored: event null or engine disabled");
                return;
            }

            // Create trigger event and send to trigger engine
            TriggerEvent triggerEvent = new TriggerEvent(TriggerEventType.XP_DROPPED, event);
            triggerEngine.processEvent(triggerEvent);

            System.out.println("[Gear Swapper] FakeXpDrop event sent to trigger engine");
        } catch (Exception e) {
            Logger.error("[Gear Swapper 2.0] Error processing XP drop event for triggers: " + e.getMessage());
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        try {
            if (event == null) {
                return;
            }

            int itemId = event.getItemId();
            String option = event.getMenuOption();
            String target = event.getMenuTarget();
            if (option == null || target == null) {
                return;
            }

            // === Brew / Sanfew detection for magic level adjustments ===
            if (!option.equalsIgnoreCase("Drink")) {
                return;
            }

            String lowerTarget = target.toLowerCase();
            boolean isBrewId = isSaradominBrew(itemId);
            boolean isBrewName = lowerTarget.contains("saradomin brew");
            boolean isSanfewId = isSanfewSerum(itemId);
            boolean isSanfewName = lowerTarget.contains("sanfew");

            if (isBrewId || isBrewName) {
                Logger.norm("[Gear Swapper] Detected brew click: itemId=" + itemId + ", option=" + option + ", target="
                        + target + ", action=" + event.getMenuAction());
                brewClickedThisTick = true;
                brewFlagTick = tickCounter;
            }

            if (isSanfewId || isSanfewName) {
                Logger.norm("[Gear Swapper] Detected Sanfew click: itemId=" + itemId + ", option=" + option
                        + ", target=" + target + ", action=" + event.getMenuAction());
                sanfewClickedThisTick = true;
                sanfewFlagTick = tickCounter;
            }
        } catch (Throwable e) {
            Logger.error("[Gear Swapper] Error handling brew click: " + e.getMessage());
        }
    }

    /**
     * Add "Copy Coordinates" option to right-click menu on tiles
     */
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (config == null || !config.copyCoordinates()) {
            return;
        }

        // Only add for WALK option (right-click on tiles)
        if (event.getType() != MenuAction.WALK.getId()) {
            return;
        }

        if (client == null) {
            return;
        }

        // Get the tile location from scene
        WorldView wv = client.getTopLevelWorldView();
        if (wv == null) {
            return;
        }

        Tile tile = wv.getSelectedSceneTile();
        if (tile == null) {
            return;
        }

        WorldPoint wp = tile.getWorldLocation();
        if (wp == null) {
            return;
        }

        final int x = wp.getX();
        final int y = wp.getY();
        final int z = wp.getPlane();

        // Add menu entry
        client.getMenu().createMenuEntry(-1)
                .setOption("<col=00ff00>Copy Coordinates</col>")
                .setTarget("")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> {
                    String coords = x + ":" + y + ":" + z;
                    copyToClipboard(coords);
                    if (client != null) {
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                                "[GearSwapper] Copied: " + coords, "");
                    }
                });
    }

    /**
     * Copy text to system clipboard
     */
    private void copyToClipboard(String text) {
        try {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(selection, selection);
        } catch (Exception e) {
            Logger.error("[Gear Swapper] Failed to copy to clipboard: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isSaradominBrew(int itemId) {
        switch (itemId) {
            case ItemID.SARADOMIN_BREW4:
            case ItemID.SARADOMIN_BREW3:
            case ItemID.SARADOMIN_BREW2:
            case ItemID.SARADOMIN_BREW1:
            case ItemID.SARADOMIN_BREW4_23575:
            case ItemID.SARADOMIN_BREW3_23577:
            case ItemID.SARADOMIN_BREW2_23579:
            case ItemID.SARADOMIN_BREW1_23581:
                return true;
            default:
                return false;
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isSanfewSerum(int itemId) {
        switch (itemId) {
            case ItemID.SANFEW_SERUM4:
            case ItemID.SANFEW_SERUM3:
            case ItemID.SANFEW_SERUM2:
            case ItemID.SANFEW_SERUM1:
                return true;
            default:
                return false;
        }
    }

    private void recordAnimationForOverlay(AnimationChanged event) {
        Actor actor = event.getActor();
        if (actor == null) {
            return;
        }

        int animationId = actor.getAnimation();
        if (animationId <= 0) {
            return;
        }

        Player local = client.getLocalPlayer();
        boolean isOurs = local != null && actor == local;
        boolean isTarget = false;

        if (local != null) {
            Actor currentTarget = local.getInteracting();
            if (currentTarget != null && currentTarget == actor) {
                isTarget = true;
            }
        }

        if (!isOurs && !isTarget) {
            return;
        }

        AnimationDebugEntry entry = new AnimationDebugEntry(animationId, isOurs, isTarget, System.currentTimeMillis());

        synchronized (recentAnimations) {
            if (recentAnimations.size() >= 5) {
                recentAnimations.removeFirst();
            }
            recentAnimations.addLast(entry);
        }
    }

    public java.util.List<AnimationDebugEntry> getRecentAnimations() {
        synchronized (recentAnimations) {
            return new java.util.ArrayList<>(recentAnimations);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        try {
            Logger.norm("[Gear Swapper 2.0] Plugin stopping...");
            unregisterHotkeys();

            // Stop and dispose the trigger engine
            if (triggerEngine != null) {
                try {
                    triggerEngine.stop();
                    Logger.norm("[Gear Swapper 2.0] Trigger engine stopped");

                    // Log final statistics
                    TriggerEngineStats stats = triggerEngine.getStats();
                    Logger.norm("[Gear Swapper 2.0] Final trigger stats: " + stats.toString());
                } catch (Exception e) {
                    Logger.error("[Gear Swapper 2.0] Error stopping trigger engine: " + e.getMessage());
                }
            }

            if (navigationButton != null) {
                try {
                    clientToolbar.removeNavigation(navigationButton);
                } catch (Exception e) {
                    Logger.error("[Gear Swapper 2.0] Error removing navigation button: " + e.getMessage());
                }
            }

            try {
                overlayManager.remove(overlay);
                overlayManager.remove(debugOverlay);
                overlayManager.remove(inventoryUtilsOverlay);
                inventoryUtilsOverlay.shutdown();
                buttonManager.shutdown();
            } catch (Exception e) {
                Logger.error("[Gear Swapper 2.0] Error removing overlay: " + e.getMessage());
            }

            // Clean up state
            currentTarget = null;
            currentTargetName = null;
            targetDistance = -1;
            playerFreezeTicks = 0;
            targetFreezeTicks = 0;
            playerFreezeImmunityTicks = 0;
            targetFreezeImmunityTicks = 0;
            playerFreezeLocation = null;
            targetFreezeLocation = null;

            // Shut down heatmap scheduler and clear queue
            try {
                if (heatmapScheduler != null) {
                    heatmapScheduler.shutdownNow();
                    heatmapScheduler = null;
                }
            } catch (Exception e) {
                Logger.error("[Gear Swapper] Error shutting down heatmap scheduler: " + e.getMessage());
            }

            synchronized (heatmapLock) {
                heatmapQueue.clear();
                heatmapRunnerActive = false;
            }

            Logger.norm("[Gear Swapper 2.0] Plugin stopped successfully");
        } catch (Exception e) {
            Logger.error("[Gear Swapper 2.0] Error during plugin shutdown: " + e.getMessage());
            throw e;
        }
    }

    @Provides
    GearSwapperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GearSwapperConfig.class);
    }

    @Provides
    TriggerEngine provideTriggerEngine(GearSwapperPlugin plugin, Client client, ConfigManager configManager) {
        try {
            TriggerEngine engine = new TriggerEngine(plugin, client, configManager);
            Logger.norm("[Gear Swapper 2.0] Trigger engine provider: Engine created successfully");
            return engine;
        } catch (Exception e) {
            Logger.error("[Gear Swapper 2.0] Failed to create trigger engine: " + e.getMessage());
            throw new RuntimeException("Failed to initialize trigger engine", e);
        }
    }

    private void registerHotkeys() {
        unregisterHotkeys(); // Clear any existing listeners

        // Register hotkeys for all existing loadouts (unlimited)
        for (int i = 1; i <= 50; i++) // Increased from 20 to support more loadouts
        {
            String loadoutName = getLoadoutName(i);
            if (loadoutName != null && !loadoutName.trim().isEmpty()) // Check if loadout actually exists
            {
                final int loadoutNum = i;
                KeyListener listener = new KeyListener() {
                    private volatile boolean consumeNextKeyTyped = false;

                    @Override
                    public void keyTyped(KeyEvent e) {
                        // Consume the typed character if we just handled a hotkey
                        if (consumeNextKeyTyped && config.consumeHotkeyInput()) {
                            e.consume();
                            consumeNextKeyTyped = false;
                        }
                    }

                    @Override
                    public void keyPressed(KeyEvent e) {
                        // Check if this key matches the loadout's keybind
                        Keybind loadoutKeybind = getLoadoutKeybind(loadoutNum);
                        if (loadoutKeybind != null && loadoutKeybind.matches(e)) {
                            // Mark to consume the typed character
                            if (config.consumeHotkeyInput()) {
                                consumeNextKeyTyped = true;
                                e.consume();
                            }

                            Logger.norm("[Gear Swapper DEBUG] Keybind pressed for loadout " + loadoutNum);

                            // Get current data directly from panel
                            String currentName = "Loadout " + loadoutNum;
                            String currentItems = "";
                            boolean currentAttack = false;

                            if (panel != null) {
                                try {
                                    Method getLoadoutDataMethod = panel.getClass().getMethod("getLoadoutData",
                                            int.class);
                                    Object loadoutData = getLoadoutDataMethod.invoke(panel, loadoutNum);

                                    if (loadoutData != null) {
                                        Method getNameMethod = loadoutData.getClass().getMethod("getName");
                                        Method getItemsMethod = loadoutData.getClass().getMethod("getItems");
                                        Method getAttackMethod = loadoutData.getClass().getMethod("getAttack");

                                        currentName = (String) getNameMethod.invoke(loadoutData);
                                        currentItems = (String) getItemsMethod.invoke(loadoutData);
                                        currentAttack = (Boolean) getAttackMethod.invoke(loadoutData);

                                        Logger.norm("[Gear Swapper DEBUG] Got live data: name='" + currentName
                                                + "', items='" + currentItems + "', attack=" + currentAttack);
                                    } else {
                                        Logger.norm(
                                                "[Gear Swapper DEBUG] LoadoutData is null for loadout " + loadoutNum);
                                    }
                                } catch (Exception ex) {
                                    Logger.norm(
                                            "[Gear Swapper DEBUG] Exception getting panel data: " + ex.getMessage());
                                }
                            } else {
                                Logger.norm("[Gear Swapper DEBUG] Panel is null");
                            }

                            executeCommands(currentItems, currentAttack);
                        }
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                        // Also consume on release to be thorough
                        Keybind loadoutKeybind = getLoadoutKeybind(loadoutNum);
                        if (loadoutKeybind != null && loadoutKeybind.matches(e) && config.consumeHotkeyInput()) {
                            e.consume();
                        }
                    }
                };
                keyManager.registerKeyListener(listener);
                dynamicLoadoutListeners.add(listener);
            }
        }
    }

    public boolean isForceOneTickEnabled() {
        return forceOneTickEnabled;
    }

    public void setForceOneTickEnabled(boolean enabled) {
        this.forceOneTickEnabled = enabled;
        if (configManager != null) {
            try {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "forceOneTickEnabled", enabled);
            } catch (Exception e) {
                Logger.warn("[Gear Swapper] Failed to persist forceOneTickEnabled: " + e.getMessage());
            }
        }
    }

    public int getForceTickThresholdMs() {
        return forceTickThresholdMs;
    }

    public void setForceTickThresholdMs(int thresholdMs) {
        this.forceTickThresholdMs = Math.max(0, Math.min(600, thresholdMs));
        if (configManager != null) {
            try {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "forceTickThresholdMs",
                        this.forceTickThresholdMs);
            } catch (Exception e) {
                Logger.warn("[Gear Swapper] Failed to persist forceTickThresholdMs: " + e.getMessage());
            }
        }
    }

    private void unregisterHotkeys() {
        for (KeyListener listener : dynamicLoadoutListeners) {
            keyManager.unregisterKeyListener(listener);
        }
        dynamicLoadoutListeners.clear();

        if (clearTargetKeyListener != null) {
            keyManager.unregisterKeyListener(clearTargetKeyListener);
            clearTargetKeyListener = null;
        }
    }

    private void registerClearTargetHotkey() {
        // Remove previous listener if any
        if (clearTargetKeyListener != null) {
            keyManager.unregisterKeyListener(clearTargetKeyListener);
            clearTargetKeyListener = null;
        }

        if (config == null) {
            return;
        }

        final Keybind hotkey = config.clearTargetHotkey();
        if (hotkey == null || hotkey == Keybind.NOT_SET) {
            return; // no hotkey configured
        }

        clearTargetKeyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (hotkey.matches(e)) {
                    clearCurrentTarget();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        };

        keyManager.registerKeyListener(clearTargetKeyListener);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!GearSwapperConfig.GROUP.equals(event.getGroup())) {
            return;
        }

        String key = event.getKey();

        // Overlay grouping: freeze, animation ID, and target overlays share the same
        // GearSwapperOverlay instance
        if ("showFreezeOverlay".equals(key) || "showAnimationIdOverlay".equals(key)
                || "showTargetOverlay".equals(key)) {
            boolean anyEnabled = config.showFreezeOverlay() || config.showAnimationIdOverlay()
                    || config.showTargetOverlay();
            Logger.norm("[Gear Swapper] Main overlay " + (anyEnabled ? "enabled" : "disabled"));

            if (anyEnabled) {
                overlayManager.add(overlay);
            } else {
                overlayManager.remove(overlay);
            }
        }
        // Check if debug overlay setting was changed
        else if ("showDebugOverlay".equals(key)) {
            boolean enabled = Boolean.parseBoolean(event.getNewValue());
            Logger.norm("[Gear Swapper] Debug overlay " + (enabled ? "enabled" : "disabled"));

            if (enabled) {
                overlayManager.add(debugOverlay);
            } else {
                overlayManager.remove(debugOverlay);
            }
        } else if ("enableMouseCircleTest".equals(key)) {
            boolean enabled = Boolean.parseBoolean(event.getNewValue());
            Logger.norm("[Gear Swapper] Mouse circle test " + (enabled ? "enabled" : "disabled"));
            if (enabled) {
                startMouseCircleTest();
            }
        } else if ("clearTargetHotkey".equals(key)) {
            // Re-register clear-target hotkey when the config changes
            registerClearTargetHotkey();
        } else if ("inventoryUtilsEnabled".equals(key)) {
            // Toggle Inventory Utils overlay
            boolean enabled = Boolean.parseBoolean(event.getNewValue());
            Logger.norm("[Gear Swapper] Inventory Utils overlay " + (enabled ? "enabled" : "disabled"));

            if (enabled) {
                overlayManager.add(inventoryUtilsOverlay);
            } else {
                overlayManager.remove(inventoryUtilsOverlay);
            }
        }
    }

    /**
     * Get the trigger engine (for UI components)
     */
    public TriggerEngine getTriggerEngine() {
        return triggerEngine;
    }

    /**
     * Check if trigger system is healthy
     */
    public boolean isTriggerSystemHealthy() {
        try {
            if (triggerEngine == null) {
                Logger.warn("[Gear Swapper 2.0] Trigger system health check: Engine is null");
                return false;
            }

            TriggerEngineStats stats = triggerEngine.getStats();
            boolean healthy = triggerEngine.isEnabled() && stats.getActiveTriggerCount() >= 0;

            if (!healthy) {
                Logger.warn("[Gear Swapper 2.0] Trigger system health check failed: " + stats.toString());
            }

            return healthy;
        } catch (Exception e) {
            Logger.error("[Gear Swapper 2.0] Error during trigger system health check: " + e.getMessage());
            return false;
        }
    }

    private void startMouseCircleTest() {
        if (mouseCircleTestRunning) {
            Logger.norm("[Gear Swapper] Mouse circle test already running");
            return;
        }

        mouseCircleTestRunning = true;

        new Thread(() -> {
            try {
                Robot robot = new Robot();
                Point start = MouseInfo.getPointerInfo().getLocation();
                int centerX = start.x;
                int centerY = start.y;
                int radius = 80;
                int steps = 300;
                long durationMs = mouseCircleTestDurationMs;

                // Initialise overlay center based on current canvas mouse position
                try {
                    if (client != null) {
                        net.runelite.api.Point canvasPos = client.getMouseCanvasPosition();
                        if (canvasPos != null) {
                            mouseCircleCenterX = canvasPos.getX();
                            mouseCircleCenterY = canvasPos.getY();
                        } else {
                            mouseCircleCenterX = client.getCanvasWidth() / 2;
                            mouseCircleCenterY = client.getCanvasHeight() / 2;
                        }
                    }
                } catch (Exception ignored) {
                }

                mouseCircleRadius = radius;
                long startTime = System.currentTimeMillis();
                mouseCircleTestStartMs = startTime;

                for (int i = 0; i < steps; i++) {
                    if (!mouseCircleTestRunning) {
                        break;
                    }

                    double t = (double) i / (double) steps;
                    double angle = 2.0 * Math.PI * t;

                    int x = centerX + (int) Math.round(radius * Math.cos(angle));
                    int y = centerY + (int) Math.round(radius * Math.sin(angle));

                    robot.mouseMove(x, y);

                    if (System.currentTimeMillis() - startTime >= durationMs) {
                        break;
                    }

                    try {
                        Thread.sleep(30L);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            } catch (Exception e) {
                Logger.error("[Gear Swapper] Error during mouse circle test: " + e.getMessage());
            } finally {
                mouseCircleTestRunning = false;
                try {
                    configManager.setConfiguration(GearSwapperConfig.GROUP, "enableMouseCircleTest", false);
                } catch (Exception e) {
                    Logger.error("[Gear Swapper] Failed to reset mouse circle test config: " + e.getMessage());
                }
            }
        }, "GS-MouseCircleTest").start();
    }

    /**
     * Get current equipped gear as command string
     */
    public String getCurrentGearAsCommands() {
        Logger.norm("[Gear Swapper] Getting current gear...");

        if (client == null || client.getLocalPlayer() == null) {
            Logger.warn("[Gear Swapper] Client or local player is null");
            return "";
        }

        StringBuilder commands = new StringBuilder();

        try {
            // Get equipment from all slots
            ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
            if (equipment != null) {
                Logger.norm("[Gear Swapper] Equipment container found with " + equipment.getItems().length + " slots");
                Item[] items = equipment.getItems();
                for (int i = 0; i < items.length; i++) {
                    Item item = items[i];
                    if (item != null && item.getId() > 0) {
                        String itemName = getItemName(item.getId());
                        if (itemName != null && !itemName.isEmpty()) {
                            if (commands.length() > 0) {
                                commands.append("\n");
                            }
                            commands.append(itemName);
                            Logger.norm("[Gear Swapper] Found equipped item: " + itemName + " (ID: " + item.getId()
                                    + ", Qty: " + item.getQuantity() + ")");
                        }
                    }
                }
            } else {
                Logger.warn("[Gear Swapper] Equipment container is null");
            }
        } catch (Exception e) {
            Logger.error("[Gear Swapper] Error getting current gear: " + e.getMessage());
        }

        Logger.norm("[Gear Swapper] Generated commands: " + commands.length());
        return commands.toString();
    }

    /**
     * Copy current gear to a specific loadout
     */
    public void copyCurrentGearToLoadout(int loadoutNum) {
        Logger.norm("[Gear Swapper] Copying gear to loadout " + loadoutNum);

        Static.invoke(() -> {
            try {
                String gearCommands = getCurrentGearAsCommands();
                Logger.norm("[Gear Swapper] Got gear commands: " + (gearCommands.isEmpty() ? "(empty)"
                        : gearCommands.substring(0, Math.min(100, gearCommands.length())) + "..."));

                if (panel != null) {
                    GearSwapperPanel.LoadoutData data = panel.getLoadoutData(loadoutNum);
                    if (data != null) {
                        String oldItems = data.items;
                        data.items = gearCommands;
                        panel.saveLoadoutToConfig(loadoutNum);
                        panel.refreshLoadoutPanel();

                        Logger.norm("[Gear Swapper] Copied current gear to loadout " + loadoutNum + " (was: "
                                + (oldItems.isEmpty() ? "(empty)" : oldItems.length() + " chars") + ", now: "
                                + gearCommands.length() + " chars)");
                    } else {
                        Logger.error("[Gear Swapper] Loadout data is null for loadout " + loadoutNum);
                    }
                } else {
                    Logger.error("[Gear Swapper] Panel is null, cannot copy gear");
                }
            } catch (Exception e) {
                Logger.error("[Gear Swapper] Error copying gear to loadout " + loadoutNum + ": " + e.getMessage());
            }
        });
    }

    /**
     * Get item name by ID
     */
    private String getItemName(int itemId) {
        try {
            return client.getItemDefinition(itemId).getName();
        } catch (Exception e) {
            return null;
        }
    }

    public void activateGearSetByName(String gearSetName) {
        try {
            Logger.norm("[Gear Swapper Trigger] Activating gear set by name: " + gearSetName);

            // Look for a loadout with this name in the panel
            if (panel != null) {
                // Iterate through all loadout slots (0-9)
                for (int i = 0; i < 10; i++) {
                    try {
                        GearSwapperPanel.LoadoutData loadoutData = panel.getLoadoutData(i);
                        if (loadoutData != null && gearSetName.equalsIgnoreCase(loadoutData.name)) {
                            Logger.norm(
                                    "[Gear Swapper Trigger] Found matching loadout '" + gearSetName + "' at slot " + i);

                            // Execute the loadout commands
                            executeCommands(loadoutData.items, loadoutData.getAttack());
                            Logger.norm("[Gear Swapper Trigger] Successfully activated gear set: " + gearSetName);
                            return;
                        }
                    } catch (Exception e) {
                        Logger.error("[Gear Swapper Trigger] Error checking loadout slot " + i + ": " + e.getMessage());
                        // Continue checking other slots
                    }
                }

                Logger.warn("[Gear Swapper Trigger] No loadout found with name: " + gearSetName);
            } else {
                Logger.error("[Gear Swapper Trigger] Panel is null, cannot activate gear set");
            }
        } catch (Exception e) {
            Logger.error("[Gear Swapper Trigger] Error activating gear set '" + gearSetName + "': " + e.getMessage());
        }
    }

    private void executeCommands(String executeStr, boolean attackFlag) {
        if (executeStr == null || executeStr.isEmpty()) {
            return;
        }

        if (clickHeatmapEnabled) {
            resetHeatmapQueueForNewSequence();
        }

        String lowerExecute = executeStr.toLowerCase();

        // If script contains MeleeRange, use the MeleeRange scheduler.
        // This scheduler will itself delegate to Tick: scheduling if needed.
        if (lowerExecute.contains("meleerange")) {
            scheduleScriptWithMeleeRange(executeStr, attackFlag);
            return;
        }

        // Otherwise, if script contains Tick: commands - use tick-based scheduling
        if (lowerExecute.contains("tick:")) {
            scheduleScriptWithTicks(executeStr, attackFlag);
            return;
        }

        // No Tick: commands - execute in a single script-level block.
        // When heatmap is enabled, apply ONE global delay for the whole script,
        // then run all equips/casts/attack synchronously in order (old behavior).
        final boolean[] shouldAttack = { attackFlag };

        Runnable scriptTask = () -> {
            boolean prev = inScriptHeatmapBlock;
            inScriptHeatmapBlock = true;
            try {
                executeCommandsInternal(executeStr, shouldAttack);

                // Handle attack flag once all commands (including nested blocks) are processed
                if (shouldAttack[0]) {
                    attackTarget();
                }
            } catch (Exception ex) {
                Logger.norm("[Gear Swapper] Error executing commands: " + ex.getMessage());
            } finally {
                inScriptHeatmapBlock = prev;
            }
        };

        if (clickHeatmapEnabled) {
            executeCommandWithDelay(scriptTask);
        } else {
            Static.invoke(scriptTask);
        }
    }

    /**
     * Schedule script commands across game ticks when Tick:N is used.
     * Commands before first Tick: run immediately.
     * Commands after Tick:N run N ticks later, etc.
     */
    private void scheduleScriptWithTicks(String executeStr, boolean attackFlag) {
        int baseTick = tickCounter;
        int tickOffset = 0;
        boolean isFirstBatch = true;

        String[] lines = executeStr.split("\n");
        StringBuilder currentBatch = new StringBuilder();

        Logger.norm("[Gear Swapper] Scheduling script with Tick: delays, baseTick=" + baseTick);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String lower = trimmed.toLowerCase();

            // Check if this is a Tick:N command
            if (lower.startsWith("tick:")) {
                // Execute or schedule current batch
                if (currentBatch.length() > 0) {
                    final String batchCommands = currentBatch.toString();

                    if (isFirstBatch) {
                        // First batch runs immediately
                        Logger.norm("[Gear Swapper] Executing first batch immediately: "
                                + batchCommands.replace("\n", "; "));
                        Static.invoke(() -> {
                            boolean[] shouldAttack = { false };
                            executeCommandsInternal(batchCommands, shouldAttack);
                        });
                        isFirstBatch = false;
                    } else {
                        // Later batches are scheduled
                        final int targetTick = baseTick + tickOffset;
                        scheduledScriptTasks.add(new ScriptTask(targetTick, () -> {
                            Static.invoke(() -> {
                                boolean[] shouldAttack = { false };
                                executeCommandsInternal(batchCommands, shouldAttack);
                            });
                        }, "Batch at tick+" + tickOffset));
                        Logger.norm("[Gear Swapper] Scheduled batch for tick " + targetTick + ": "
                                + batchCommands.replace("\n", "; "));
                    }

                    currentBatch = new StringBuilder();
                }

                // Parse tick delay and add to offset
                try {
                    String tickValue = trimmed.substring(5).trim();
                    int ticks = Integer.parseInt(tickValue);
                    if (ticks > 0 && ticks <= 50) {
                        tickOffset += ticks;
                        Logger.norm("[Gear Swapper] Tick:" + ticks + " - offset now " + tickOffset);
                        isFirstBatch = false; // After any Tick:, we're no longer on first batch
                    } else {
                        Logger.warn("[Gear Swapper] Tick value must be between 1 and 50, got: " + ticks);
                    }
                } catch (NumberFormatException e) {
                    Logger.warn("[Gear Swapper] Invalid Tick format: " + trimmed);
                }
            } else {
                // Add command to current batch
                if (currentBatch.length() > 0) {
                    currentBatch.append("\n");
                }
                currentBatch.append(trimmed);
            }
        }

        // Handle final batch (including attack if needed)
        if (currentBatch.length() > 0 || attackFlag) {
            final String batchCommands = currentBatch.toString();
            final boolean doAttack = attackFlag;

            if (isFirstBatch && tickOffset == 0) {
                // No Tick: commands were encountered, run everything immediately
                Logger.norm("[Gear Swapper] Executing final batch immediately: " +
                        (batchCommands.isEmpty() ? "(attack only)" : batchCommands.replace("\n", "; ")) +
                        (doAttack ? " +attack" : ""));
                Static.invoke(() -> {
                    boolean[] shouldAttack = { doAttack };
                    if (!batchCommands.isEmpty()) {
                        executeCommandsInternal(batchCommands, shouldAttack);
                    }
                    if (shouldAttack[0]) {
                        attackTarget();
                    }
                });
            } else {
                // Schedule final batch for later tick
                final int targetTick = baseTick + tickOffset;
                scheduledScriptTasks.add(new ScriptTask(targetTick, () -> {
                    Static.invoke(() -> {
                        boolean[] shouldAttack = { doAttack };
                        if (!batchCommands.isEmpty()) {
                            executeCommandsInternal(batchCommands, shouldAttack);
                        }
                        if (shouldAttack[0]) {
                            attackTarget();
                        }
                    });
                }, "Final batch at tick+" + tickOffset + (doAttack ? " +attack" : "")));

                Logger.norm("[Gear Swapper] Scheduled final batch for tick " + targetTick + ": " +
                        (batchCommands.isEmpty() ? "(attack only)" : batchCommands.replace("\n", "; ")) +
                        (doAttack ? " +attack" : ""));
            }
        }
    }

    private void scheduleScriptWithMeleeRange(String executeStr, boolean attackFlag) {
        String[] lines = executeStr.split("\n");
        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();
        boolean seenMeleeRange = false;

        for (String line : lines) {
            if (line == null) {
                continue;
            }

            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (!seenMeleeRange && trimmed.equalsIgnoreCase("MeleeRange")) {
                seenMeleeRange = true;
                continue;
            }

            if (!seenMeleeRange) {
                if (before.length() > 0) {
                    before.append("\n");
                }
                before.append(trimmed);
            } else {
                if (after.length() > 0) {
                    after.append("\n");
                }
                after.append(trimmed);
            }
        }

        final boolean[] shouldAttack = { attackFlag };

        if (!seenMeleeRange) {
            Static.invoke(() -> {
                try {
                    executeCommandsInternal(executeStr, shouldAttack);
                    if (shouldAttack[0]) {
                        attackTarget();
                    }
                } catch (Exception ex) {
                    Logger.norm("[Gear Swapper] Error executing commands (no MeleeRange found): " + ex.getMessage());
                }
            });
            return;
        }

        if (before.length() > 0) {
            final String beforeScript = before.toString();
            Static.invoke(() -> {
                try {
                    executeCommandsInternal(beforeScript, shouldAttack);
                } catch (Exception ex) {
                    Logger.norm("[Gear Swapper] Error executing MeleeRange prefix: " + ex.getMessage());
                }
            });
        }

        if (after.length() == 0) {
            if (shouldAttack[0]) {
                Static.invoke(this::attackTarget);
            }
            return;
        }

        if (client == null || client.getLocalPlayer() == null) {
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        Actor targetActor = localPlayer.getInteracting();
        if (targetActor == null && cachedTarget != null) {
            long now = System.currentTimeMillis();
            if (cachedTargetTime > 0 && now - cachedTargetTime < TARGET_CACHE_DURATION_MS) {
                targetActor = cachedTarget;
            }
        }

        if (!(targetActor instanceof Player)) {
            return;
        }

        WorldPoint localPos = localPlayer.getWorldLocation();
        WorldPoint targetPos = targetActor.getWorldLocation();

        final String afterScript = after.toString();
        final boolean afterHasTick = afterScript.toLowerCase().contains("tick:");

        if (localPos != null && targetPos != null) {
            int dist = localPos.distanceTo(targetPos);
            if (dist <= 3) {
                if (!afterHasTick) {
                    final String afterScriptNow = afterScript;
                    Static.invoke(() -> {
                        try {
                            executeCommandsInternal(afterScriptNow, shouldAttack);
                            if (shouldAttack[0]) {
                                attackTarget();
                            }
                        } catch (Exception ex) {
                            Logger.norm("[Gear Swapper] Error executing MeleeRange continuation: " + ex.getMessage());
                        }
                    });
                } else {
                    // Already in melee range: now schedule the rest of the script using Tick:
                    // semantics
                    scheduleScriptWithTicks(afterScript, attackFlag);
                }
                return;
            }
        }

        // Not yet in range: start walking towards target (3 tiles) and gate for up to 2
        // ticks
        executeMoveCommand(3);

        final int startTick = tickCounter;

        if (!afterHasTick) {
            scheduledScriptTasks.add(new ScriptTask(startTick + 1, () -> {
                handleMeleeRangeGate(afterScript, shouldAttack, startTick);
            }, "MeleeRange gate"));
        } else {
            scheduledScriptTasks.add(new ScriptTask(startTick + 1, () -> {
                handleMeleeRangeGateWithTicks(afterScript, attackFlag, startTick);
            }, "MeleeRange gate (Tick)"));
        }
    }

    private void handleMeleeRangeGate(String script, boolean[] shouldAttack, int startTick) {
        if (client == null || client.getLocalPlayer() == null) {
            return;
        }

        if (currentTarget == null) {
            return;
        }

        int dist = targetDistance;
        if (dist < 0) {
            WorldPoint localPos = client.getLocalPlayer().getWorldLocation();
            WorldPoint targetPos = currentTarget.getWorldLocation();
            if (localPos != null && targetPos != null) {
                dist = localPos.distanceTo(targetPos);
            }
        }

        int ticksElapsed = tickCounter - startTick;

        if (dist >= 0 && dist <= 3) {
            Static.invoke(() -> {
                try {
                    executeCommandsInternal(script, shouldAttack);
                    if (shouldAttack[0]) {
                        attackTarget();
                    }
                } catch (Exception ex) {
                    Logger.norm("[Gear Swapper] Error executing MeleeRange completion: " + ex.getMessage());
                }
            });
            return;
        }

        if (ticksElapsed >= 2) {
            return;
        }

        final String scriptCopy = script;
        final boolean[] flagRef = shouldAttack;
        final int startTickRef = startTick;
        final int nextTick = tickCounter + 1;

        scheduledScriptTasks.add(new ScriptTask(nextTick, () -> {
            handleMeleeRangeGate(scriptCopy, flagRef, startTickRef);
        }, "MeleeRange gate retry"));
    }

    private void handleMeleeRangeGateWithTicks(String script, boolean attackFlag, int startTick) {
        if (client == null || client.getLocalPlayer() == null) {
            return;
        }

        if (currentTarget == null) {
            return;
        }

        int dist = targetDistance;
        if (dist < 0) {
            WorldPoint localPos = client.getLocalPlayer().getWorldLocation();
            WorldPoint targetPos = currentTarget.getWorldLocation();
            if (localPos != null && targetPos != null) {
                dist = localPos.distanceTo(targetPos);
            }
        }

        int ticksElapsed = tickCounter - startTick;

        if (dist >= 0 && dist <= 3) {
            // Now that we're in melee range, schedule the gated script using Tick: handling
            scheduleScriptWithTicks(script, attackFlag);
            return;
        }

        if (ticksElapsed >= 2) {
            // Timed out: do not execute the rest of the script
            return;
        }

        final String scriptCopy = script;
        final int startTickRef = startTick;
        final int nextTick = tickCounter + 1;

        scheduledScriptTasks.add(new ScriptTask(nextTick, () -> {
            handleMeleeRangeGateWithTicks(scriptCopy, attackFlag, startTickRef);
        }, "MeleeRange gate retry (Tick)"));
    }

    /**
     * Internal implementation for executing a block of commands.
     * This method may be called recursively for multi-line if/else blocks.
     */
    private void executeCommandsInternal(String executeStr, boolean[] shouldAttack) {
        if (executeStr == null || executeStr.isEmpty()) {
            return;
        }

        String[] lines = executeStr.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String lower = trimmed.toLowerCase();

            if (lower.startsWith("if ")) {
                // Multi-line aware conditional handler will advance the index
                i = handleConditionalBlock(lines, i, shouldAttack);
            } else {
                executeSingleCommand(trimmed, shouldAttack);
            }
        }
    }

    /**
     * Handle conditional blocks including multi-line if/else structures.
     * Returns the index of the last line consumed by this block.
     */
    private int handleConditionalBlock(String[] lines, int startIndex, boolean[] shouldAttack) {
        if (lines == null || startIndex < 0 || startIndex >= lines.length) {
            return startIndex;
        }

        String header = lines[startIndex] != null ? lines[startIndex] : "";
        String trimmedHeader = header.trim();
        String lowerHeader = trimmedHeader.toLowerCase();

        if (!lowerHeader.startsWith("if ")) {
            return startIndex;
        }

        // Extract condition part from header (between 'if' and '{' if present)
        int bracePosInHeader = trimmedHeader.indexOf('{');
        String conditionPart;
        if (bracePosInHeader != -1) {
            conditionPart = trimmedHeader.substring(2, bracePosInHeader).trim();
        } else {
            conditionPart = trimmedHeader.length() > 2 ? trimmedHeader.substring(2).trim() : "";
        }

        if (conditionPart.isEmpty()) {
            Logger.warn("[Gear Swapper] Empty condition in if-statement: " + trimmedHeader);
        }

        // Locate opening brace for THEN block (may be on same or later line)
        int openLine = -1;
        int openCol = -1;
        outerOpen: for (int i = startIndex; i < lines.length; i++) {
            String s = lines[i];
            if (s == null) {
                continue;
            }
            int idx = s.indexOf('{');
            if (idx != -1) {
                openLine = i;
                openCol = idx;
                break outerOpen;
            }
        }

        if (openLine == -1) {
            Logger.warn("[Gear Swapper] Invalid if-statement syntax (no '{'): " + trimmedHeader);
            return startIndex;
        }

        // Collect THEN block text between matching braces, supporting nested blocks
        StringBuilder thenBlock = new StringBuilder();
        int thenEndLine = -1;
        String remainderAfterThen = "";

        int depth = 1;
        int lineIndex = openLine;
        int colIndex = openCol + 1; // start after '{'

        blockThen: for (; lineIndex < lines.length; lineIndex++) {
            String s = lines[lineIndex];
            if (s == null) {
                s = "";
            }

            int len = s.length();
            for (int j = colIndex; j < len; j++) {
                char c = s.charAt(j);
                if (c == '{') {
                    depth++;
                    // Keep nested braces in the block text
                    if (depth >= 1) {
                        thenBlock.append(c);
                    }
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        // End of THEN block; capture remainder on this line
                        if (j + 1 < len) {
                            remainderAfterThen = s.substring(j + 1).trim();
                        }
                        thenEndLine = lineIndex;
                        break blockThen;
                    } else {
                        if (depth >= 1) {
                            thenBlock.append(c);
                        }
                    }
                } else {
                    if (depth >= 1) {
                        thenBlock.append(c);
                    }
                }
            }

            if (depth >= 1) {
                thenBlock.append('\n');
            }

            colIndex = 0; // reset for next line
        }

        if (thenEndLine == -1) {
            Logger.warn("[Gear Swapper] Invalid if-statement syntax (no closing '}'): " + trimmedHeader);
            return startIndex;
        }

        String thenBlockStr = thenBlock.toString().trim();

        // Optional ELSE block detection
        String elseBlockStr = null;
        int finalEndLine = thenEndLine;

        String after = remainderAfterThen;
        int searchLine = thenEndLine;

        if (after.isEmpty()) {
            // Look ahead for an 'else' line
            for (int i = thenEndLine + 1; i < lines.length; i++) {
                String s = lines[i];
                if (s == null) {
                    continue;
                }
                String t = s.trim();
                if (t.isEmpty()) {
                    continue;
                }
                after = t;
                searchLine = i;
                break;
            }
        }

        if (!after.isEmpty()) {
            String lowerAfter = after.toLowerCase();
            if (lowerAfter.startsWith("else")) {
                // Find opening brace for ELSE block
                int elseOpenLine = -1;
                int elseOpenCol = -1;
                outerElseOpen: for (int i = searchLine; i < lines.length; i++) {
                    String s = lines[i];
                    if (s == null) {
                        continue;
                    }
                    int idx = s.indexOf('{');
                    if (idx != -1) {
                        elseOpenLine = i;
                        elseOpenCol = idx;
                        break outerElseOpen;
                    }
                }

                if (elseOpenLine == -1) {
                    Logger.warn("[Gear Swapper] Invalid else-block syntax (no '{'): " + after);
                } else {
                    StringBuilder elseBlock = new StringBuilder();
                    int elseEndLine = -1;
                    int elseDepth = 1;
                    int li = elseOpenLine;
                    int ci = elseOpenCol + 1;

                    blockElse: for (; li < lines.length; li++) {
                        String s = lines[li];
                        if (s == null) {
                            s = "";
                        }
                        int len = s.length();
                        for (int j = ci; j < len; j++) {
                            char c = s.charAt(j);
                            if (c == '{') {
                                elseDepth++;
                                if (elseDepth >= 1) {
                                    elseBlock.append(c);
                                }
                            } else if (c == '}') {
                                elseDepth--;
                                if (elseDepth == 0) {
                                    elseEndLine = li;
                                    break blockElse;
                                } else {
                                    if (elseDepth >= 1) {
                                        elseBlock.append(c);
                                    }
                                }
                            } else {
                                if (elseDepth >= 1) {
                                    elseBlock.append(c);
                                }
                            }
                        }

                        if (elseDepth >= 1) {
                            elseBlock.append('\n');
                        }

                        ci = 0;
                    }

                    if (elseEndLine == -1) {
                        Logger.warn("[Gear Swapper] Invalid else-block syntax (no closing '}'): " + after);
                    } else {
                        elseBlockStr = elseBlock.toString().trim();
                        finalEndLine = elseEndLine;
                    }
                }
            }
        }

        boolean conditionResult = evaluateCondition(conditionPart);
        if (conditionResult) {
            if (!thenBlockStr.isEmpty()) {
                executeCommandsInternal(thenBlockStr, shouldAttack);
            }
        } else if (elseBlockStr != null && !elseBlockStr.isEmpty()) {
            executeCommandsInternal(elseBlockStr, shouldAttack);
        }

        return finalEndLine;
    }

    /**
     * Execute a single loadout line (item, cast, prayer, move, etc.)
     */
    private void executeSingleCommand(String trimmed, boolean[] shouldAttack) {
        // Check if it's a command with colon (old format)
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            if (parts.length >= 2) {
                String command = parts[0].trim();
                String value = parts[1].trim();

                if (command.equalsIgnoreCase("Item")) {
                    equipItem(value);
                } else if (command.equalsIgnoreCase("Cast")) {
                    // Cast:... lines. When heatmap is enabled, route through the
                    // queued heatmap executor (castSpell) to preserve script order.
                    if (clickHeatmapEnabled) {
                        String[] spells = value.split(":");
                        for (String spell : spells) {
                            spell = spell.trim();
                            if (!spell.isEmpty()) {
                                // Use the heatmap-aware castSpell; we only need
                                // the first non-empty candidate here.
                                castSpell(spell);
                                break;
                            }
                        }
                    } else {
                        // Original fallback behavior without heatmap: try
                        // multiple spells until one can be cast.
                        String[] spells = value.split(":");
                        for (String spell : spells) {
                            spell = spell.trim();
                            if (!spell.isEmpty()) {
                                if (castSpellWithReturn(spell)) {
                                    // Spell was cast successfully, stop trying fallbacks
                                    break;
                                }
                            }
                        }
                    }
                } else if (command.equalsIgnoreCase("Prayer")) {
                    String[] prayers = { value };
                    applyPrayersForTrigger(prayers);
                } else if (command.equalsIgnoreCase("Special")) {
                    enableSpecialAttack();
                } else if (command.equalsIgnoreCase("Move")) {
                    try {
                        int tilesToMove = Integer.parseInt(value);
                        if (tilesToMove >= 0 && tilesToMove <= 10) {
                            // Move:0 = walk directly onto target (underneath)
                            executeMoveCommand(tilesToMove);
                        } else {
                            Logger.warn("[Gear Swapper] Move tiles must be between 0 and 10, got: " + tilesToMove);
                        }
                    } catch (NumberFormatException e) {
                        Logger.warn("[Gear Swapper] Invalid move tiles format: " + value);
                    }
                } else if (command.equalsIgnoreCase("MoveDiag")) {
                    try {
                        int tilesToMove = Integer.parseInt(value);
                        if (tilesToMove >= 0 && tilesToMove <= 10) {
                            executeMoveDiagCommand(tilesToMove);
                        } else {
                            Logger.warn("[Gear Swapper] MoveDiag tiles must be between 0 and 10, got: " + tilesToMove);
                        }
                    } catch (NumberFormatException e) {
                        Logger.warn("[Gear Swapper] Invalid MoveDiag tiles format: " + value);
                    }
                } else if (command.equalsIgnoreCase("Log")) {
                    // Send message to in-game chat log
                    if (client != null && client.getLocalPlayer() != null) {
                        String message = "[GS] " + value;
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
                    }
                } else if (command.equalsIgnoreCase("Tick")) {
                    // Tick:N is handled by scheduleScriptWithTicks() at the executeCommands level.
                    // If we reach here, it means the script was executed without the scheduler
                    // (e.g., from executeCommandsInternal directly). Just log and skip.
                    Logger.norm("[Gear Swapper] Tick:" + value
                            + " encountered in direct execution (no-op here, handled by scheduler)");
                } else if (command.equalsIgnoreCase("TogglePray")) {
                    togglePrayer(value);
                } else if (command.equalsIgnoreCase("DropAll")) {
                    // DropAll:pattern - Drop items matching pattern (wildcard support)
                    // Drop 2-6 random items per tick until all matching items are dropped
                    scheduleDropAllTask(value);
                } else if (command.equalsIgnoreCase("Npc")) {
                    // Npc:*name*:action - Interact with NPC (wildcard support)
                    String[] npcParts = value.split(":", 2);
                    String npcPattern = npcParts[0].trim();
                    String action = npcParts.length > 1 ? npcParts[1].trim() : null;
                    interactWithNpc(npcPattern, action);
                } else if (command.equalsIgnoreCase("Wait")) {
                    // Wait:min:max - Random delay in milliseconds (converted to ticks)
                    try {
                        String[] waitParts = value.split(":");
                        int minMs = Integer.parseInt(waitParts[0].trim());
                        int maxMs = waitParts.length > 1 ? Integer.parseInt(waitParts[1].trim()) : minMs;
                        int delayMs = minMs + (int) (Math.random() * (maxMs - minMs + 1));
                        int delayTicks = Math.max(1, delayMs / 600); // Convert to ticks (~600ms per tick)
                        if (delayTicks > 0) {
                            scheduleLooperPause(delayTicks);
                            throw new ScriptStopException("Wait:" + delayMs + "ms (~" + delayTicks + " ticks)");
                        }
                    } catch (NumberFormatException e) {
                        Logger.warn("[Gear Swapper] Invalid Wait format: " + value);
                    }
                } else if (command.equalsIgnoreCase("Walk")) {
                    // Walk:X:Y:Z - Walk towards coordinates
                    try {
                        String[] coords = value.split(":");
                        if (coords.length >= 2) {
                            int x = Integer.parseInt(coords[0].trim());
                            int y = Integer.parseInt(coords[1].trim());
                            int z = coords.length >= 3 ? Integer.parseInt(coords[2].trim()) : 0;
                            WorldPoint target = new WorldPoint(x, y, z);
                            if (client != null) {
                                com.tonic.api.game.MovementAPI.walkToWorldPoint(target);
                                Logger.norm("[Gear Swapper] Walking to " + x + ":" + y + ":" + z);
                            }
                        } else {
                            Logger.warn("[Gear Swapper] Invalid Walk format: " + value
                                    + " (expected Walk:X:Y or Walk:X:Y:Z)");
                        }
                    } catch (NumberFormatException e) {
                        Logger.warn("[Gear Swapper] Invalid Walk coordinates: " + value);
                    }
                }
            }
        }
        // Check for single-word commands (new format)
        else if (trimmed.equalsIgnoreCase("Special")) {
            enableSpecialAttack();
        } else if (trimmed.equalsIgnoreCase("Attack")) {
            // Inline Attack command: immediately attack current target.
            // This allows scripts like: D scimitar -> Tick:1 -> Attack -> Tick:1 -> Attack
            // to perform multiple attacks separated by Tick:N delays.
            attackTarget();
        } else if (trimmed.equalsIgnoreCase("Stop")) {
            // Stop command: halt script execution for this tick
            shouldAttack[0] = false;
            throw new ScriptStopException("Script stopped by Stop command");
        } else if (trimmed.equalsIgnoreCase("RandomAfkChance")) {
            // RandomAfkChance: 15% chance to pause for 1-5 ticks
            if (Math.random() < 0.15) {
                int waitTicks = 1 + (int) (Math.random() * 5); // 1-5 ticks
                scheduleLooperPause(waitTicks);
                throw new ScriptStopException("RandomAfkChance: pausing for " + waitTicks + " ticks");
            }
        } else {
            // Plain item name (new format from copy feature)
            // Remove any trailing attack=false
            String itemName = trimmed;
            if (itemName.endsWith(", attack=false")) {
                itemName = itemName.substring(0, itemName.length() - ", attack=false".length()).trim();
            } else if (itemName.endsWith(", attack=true")) {
                itemName = itemName.substring(0, itemName.length() - ", attack=true".length()).trim();
                shouldAttack[0] = true;
            }

            String lowerItem = itemName.toLowerCase();

            // Check if it's a spell (common spell keywords)
            if (lowerItem.contains("barrage") ||
                    lowerItem.contains("burst") ||
                    lowerItem.contains("blast") ||
                    lowerItem.contains("strike") ||
                    lowerItem.contains("bolt") ||
                    lowerItem.contains("surge") ||
                    lowerItem.contains("wave") ||
                    lowerItem.contains("dart") ||
                    lowerItem.contains("teleport") ||
                    lowerItem.contains("vengeance") ||
                    lowerItem.contains("entangle") ||
                    lowerItem.contains("bind") ||
                    lowerItem.contains("snare") ||
                    lowerItem.contains("tele block") ||
                    lowerItem.contains("tb")) {
                // It's a spell, cast it
                castSpell(itemName);
            } else {
                // It's an item, equip it
                equipItem(itemName);
            }
        }
    }

    /**
     * Handle conditional loadout command lines like: if frozen { Armadyl godsword }
     */
    private void handleConditionalCommand(String line, boolean[] shouldAttack) {
        if (line == null) {
            return;
        }

        String trimmed = line.trim();
        int braceStart = trimmed.indexOf('{');
        if (braceStart == -1) {
            Logger.warn("[Gear Swapper] Invalid if-statement syntax: " + trimmed);
            return;
        }

        int braceEnd = trimmed.indexOf('}', braceStart + 1);
        if (braceEnd == -1) {
            Logger.warn("[Gear Swapper] Invalid if-statement syntax: " + trimmed);
            return;
        }

        // Extract condition between "if" and first "{"
        String conditionPart = trimmed.substring(2, braceStart).trim();
        // Extract main body inside first braces
        String body = trimmed.substring(braceStart + 1, braceEnd).trim();

        // Optional else { ... } block after first }
        String elseBody = null;
        String afterThen = trimmed.substring(braceEnd + 1).trim();
        if (!afterThen.isEmpty()) {
            String lowerAfter = afterThen.toLowerCase();
            if (lowerAfter.startsWith("else")) {
                int elseBraceStart = afterThen.indexOf('{');
                int elseBraceEnd = elseBraceStart == -1 ? -1 : afterThen.indexOf('}', elseBraceStart + 1);
                if (elseBraceStart == -1 || elseBraceEnd == -1) {
                    Logger.warn("[Gear Swapper] Invalid else-block syntax in if-statement: " + trimmed);
                } else {
                    elseBody = afterThen.substring(elseBraceStart + 1, elseBraceEnd).trim();
                }
            }
        }

        if (body.isEmpty() && (elseBody == null || elseBody.isEmpty())) {
            return;
        }

        boolean conditionResult = evaluateCondition(conditionPart);
        if (conditionResult) {
            if (!body.isEmpty()) {
                executeSingleCommand(body, shouldAttack);
            }
        } else if (elseBody != null && !elseBody.isEmpty()) {
            executeSingleCommand(elseBody, shouldAttack);
        }
    }

    /**
     * Evaluate simple conditions for loadout if-statements.
     * Currently supports:
     * - "frozen" / "target_frozen" / "target frozen" -> targetFreezeTicks > 0
     * - "self_frozen" / "player_frozen" / "me_frozen" / variants ->
     * playerFreezeTicks > 0
     */
    private boolean evaluateCondition(String condition) {
        if (condition == null) {
            return false;
        }

        String expr = condition.trim();
        if (expr.isEmpty()) {
            return false;
        }

        try {
            return evalConditionExpression(expr);
        } catch (Exception e) {
            Logger.warn("[Gear Swapper] Error evaluating condition '" + condition + "': " + e.getMessage());
            return false;
        }
    }

    private boolean evalConditionExpression(String expr) {
        // OR has lowest precedence: split by ||, evaluate each as an AND-expression
        String[] orParts = expr.split("\\|\\|");
        boolean result = false;
        for (String part : orParts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (evalAndExpression(trimmed)) {
                result = true;
                break; // short-circuit on first true
            }
        }
        return result;
    }

    private boolean evalAndExpression(String expr) {
        String[] andParts = expr.split("&&");
        boolean result = true;
        for (String part : andParts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (!evalSimpleCondition(trimmed)) {
                result = false;
                break; // short-circuit on first false
            }
        }
        return result;
    }

    private boolean evalSimpleCondition(String expr) {
        if (expr == null) {
            return false;
        }

        String cond = expr.trim();
        if (cond.isEmpty()) {
            return false;
        }

        // Strip outer parentheses if present: (world == 578) -> world == 578
        while (cond.startsWith("(") && cond.endsWith(")")) {
            cond = cond.substring(1, cond.length() - 1).trim();
        }

        // Support ! negation, including multiple !!
        boolean negate = false;
        while (cond.startsWith("!")) {
            negate = !negate;
            cond = cond.substring(1).trim();
        }

        String lowerCond = cond.toLowerCase();
        boolean value = evalAtomicCondition(lowerCond, cond);
        return negate ? !value : value;
    }

    private boolean evalAtomicCondition(String lowerCond, String originalCond) {
        // First, handle numeric comparisons like: hp<40, spec>=50, distance<=3
        String[] operators = new String[] { "<=", ">=", "==", "!=", "<", ">" };
        for (String op : operators) {
            int idx = lowerCond.indexOf(op);
            if (idx > 0) {
                String left = lowerCond.substring(0, idx).trim();
                String rightRaw = originalCond.substring(idx + op.length()).trim();
                if (left.isEmpty() || rightRaw.isEmpty()) {
                    Logger.warn("[Gear Swapper] Invalid comparison in condition: " + originalCond);
                    return false;
                }

                try {
                    int right = Integer.parseInt(rightRaw);
                    int leftValue = resolveNumericValue(left);
                    boolean result = compareInts(leftValue, right, op);
                    Logger.norm("[Gear Swapper] Condition: " + left + " " + op + " " + right + " => " + leftValue + " "
                            + op + " " + right + " = " + result);
                    return result;
                } catch (NumberFormatException e) {
                    Logger.warn("[Gear Swapper] Invalid numeric value in condition: " + originalCond);
                    return false;
                }
            }
        }

        // ===== REGION CHECKS =====
        // if(RegionId == 13363) or if(region == 13363)
        if (lowerCond.startsWith("regionid_") || lowerCond.startsWith("region_")) {
            try {
                String regionStr = lowerCond.substring(lowerCond.indexOf('_') + 1);
                int expectedRegion = Integer.parseInt(regionStr);
                int currentRegion = client != null && client.getLocalPlayer() != null
                        ? client.getLocalPlayer().getWorldLocation().getRegionID()
                        : -1;
                return currentRegion == expectedRegion;
            } catch (Exception e) {
                return false;
            }
        }

        // ===== VARBIT CHECKS =====
        // if(varbit_8121_1) means Varbit 8121 == 1
        if (lowerCond.startsWith("varbit_")) {
            try {
                String[] parts = lowerCond.substring(7).split("_");
                if (parts.length >= 2) {
                    int varbitId = Integer.parseInt(parts[0]);
                    int expectedValue = Integer.parseInt(parts[1]);
                    int actualValue = client != null ? client.getVarbitValue(varbitId) : -1;
                    return actualValue == expectedValue;
                }
            } catch (Exception e) {
                return false;
            }
        }

        // ===== SKILL CHECKS =====
        // if(SkillBelow_Hitpoints_69) or if(SkillAbove_Magic_93)
        if (lowerCond.startsWith("skillbelow_")) {
            try {
                String remainder = lowerCond.substring(11); // after "skillbelow_"
                int lastUnderscore = remainder.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String skillName = remainder.substring(0, lastUnderscore);
                    int threshold = Integer.parseInt(remainder.substring(lastUnderscore + 1));
                    int currentLevel = getSkillLevel(skillName);
                    return currentLevel < threshold;
                }
            } catch (Exception e) {
                return false;
            }
        }

        if (lowerCond.startsWith("skillabove_")) {
            try {
                String remainder = lowerCond.substring(11); // after "skillabove_"
                int lastUnderscore = remainder.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String skillName = remainder.substring(0, lastUnderscore);
                    int threshold = Integer.parseInt(remainder.substring(lastUnderscore + 1));
                    int currentLevel = getSkillLevel(skillName);
                    return currentLevel > threshold;
                }
            } catch (Exception e) {
                return false;
            }
        }

        // ===== ITEM CHECKS =====
        // if(HasItem_Anglerfish) or if(HasItem_Saradomin brew(*))
        if (lowerCond.startsWith("hasitem_")) {
            String itemPattern = originalCond.substring(8); // preserve case
            return hasItemInInventory(itemPattern);
        }

        // if(HasItemAmount_Saradomin brew(*)_2) - has at least 2
        if (lowerCond.startsWith("hasitemamount_")) {
            try {
                String remainder = originalCond.substring(14);
                int lastUnderscore = remainder.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String itemPattern = remainder.substring(0, lastUnderscore);
                    int requiredAmount = Integer.parseInt(remainder.substring(lastUnderscore + 1));
                    int actualAmount = countItemsInInventory(itemPattern);
                    return actualAmount >= requiredAmount;
                }
            } catch (Exception e) {
                return false;
            }
        }

        // ===== TARGET CHECKS =====
        // if(TargetHP > 60) - handled by numeric comparison
        // if(TargetDistance < 3) - handled by numeric comparison
        // if(TargetFrozenTicks > 5) - handled by numeric comparison

        // if(TargetIsDiagonal)
        if (lowerCond.equals("targetisdiagonal") || lowerCond.equals("target_is_diagonal")) {
            return isTargetDiagonal();
        }

        // if(TargetAnimation_1979) - target is playing animation 1979
        if (lowerCond.startsWith("targetanimation_")) {
            try {
                int expectedAnim = Integer.parseInt(lowerCond.substring(16));
                if (currentTarget != null) {
                    return currentTarget.getAnimation() == expectedAnim;
                }
            } catch (Exception e) {
            }
            return false;
        }

        // if(TargetPraying_Magic) / if(TargetPraying_Ranged) / if(TargetPraying_Melee)
        if (lowerCond.startsWith("targetpraying_")) {
            String prayer = lowerCond.substring(14);
            return isTargetPraying(prayer);
        }

        // if(TargetOverheadText_X) - target has overhead text containing X
        if (lowerCond.startsWith("targetoverheadtext_")) {
            String textToFind = originalCond.substring(19);
            if (currentTarget != null) {
                String overhead = currentTarget.getOverheadText();
                return overhead != null && overhead.toLowerCase().contains(textToFind.toLowerCase());
            }
            return false;
        }

        // ===== ANIMATION CHECKS =====
        // if(animation == 1979) - local player animation
        if (lowerCond.startsWith("animation_")) {
            try {
                int expectedAnim = Integer.parseInt(lowerCond.substring(10));
                if (client != null && client.getLocalPlayer() != null) {
                    return client.getLocalPlayer().getAnimation() == expectedAnim;
                }
            } catch (Exception e) {
            }
            return false;
        }

        // ===== RANDOM CHANCE =====
        // if(RandomChancePercent_65) - 65% chance to be true
        if (lowerCond.startsWith("randomchancepercent_")) {
            try {
                int percent = Integer.parseInt(lowerCond.substring(20));
                return Math.random() * 100 < percent;
            } catch (Exception e) {
                return false;
            }
        }

        // ===== MEMORY/STATE CHECKS =====
        // if(getmemory_World_Uk) - check stored memory value
        if (lowerCond.startsWith("getmemory_")) {
            String memKey = lowerCond.substring(10);
            return checkMemory(memKey);
        }

        // ===== IDLE CHECKS =====
        // if(IdleTicks >= 100) - handled by numeric comparison via resolveNumericValue

        // ===== ATTACK TIMER CHECKS =====
        // if(AttackTimerCount == 2) - handled by numeric comparison
        // if(TickTimer > 2) - handled by numeric comparison
        // if(TickTimer_MyAttack < 3) - handled by numeric comparison

        // ===== BOOLEAN FLAGS =====
        // "frozen" (and self_* variants) refer to the LOCAL PLAYER being frozen
        if (lowerCond.equals("frozen") ||
                lowerCond.equals("self_frozen") || lowerCond.equals("player_frozen") || lowerCond.equals("me_frozen") ||
                lowerCond.equals("self frozen") || lowerCond.equals("player frozen") || lowerCond.equals("me frozen")) {
            return playerFreezeTicks > 0;
        }

        // Target-only frozen checks
        if (lowerCond.equals("target_frozen") || lowerCond.equals("target frozen")
                || lowerCond.equals("targetfrozen")) {
            return targetFreezeTicks > 0;
        }

        if (lowerCond.equals("has_target") || lowerCond.equals("target_exists") || lowerCond.equals("has target")
                || lowerCond.equals("hastarget")) {
            return currentTarget != null;
        }

        if (lowerCond.equals("has_cached_target") || lowerCond.equals("cached_target")) {
            return cachedTarget != null;
        }

        // Inventory full check
        if (lowerCond.equals("inventoryfull") || lowerCond.equals("inventory_full")) {
            return InventoryAPI.isFull();
        }

        // Run energy check
        if (lowerCond.equals("run_enabled") || lowerCond.equals("running")) {
            return client != null && client.getVarpValue(173) == 1;
        }

        // ===== COMBAT CHECK =====
        if (lowerCond.equals("incombat") || lowerCond.equals("in_combat") || lowerCond.equals("combat")) {
            return isPlayerInCombat();
        }

        // ===== MOVING CHECK =====
        if (lowerCond.equals("moving") || lowerCond.equals("walking") || lowerCond.equals("is_moving")) {
            return isPlayerMoving();
        }

        Logger.warn("[Gear Swapper] Unknown condition in if-statement: " + originalCond);
        return false;
    }

    private int resolveNumericValue(String name) {
        String key = name == null ? "" : name.trim().toLowerCase();
        try {
            switch (key) {
                case "spec":
                case "spec_energy":
                case "special":
                case "special_energy":
                case "specialattack":
                    return CombatAPI.getSpecEnergy();

                case "hp":
                case "health":
                case "hitpoints":
                    return SkillAPI.getBoostedLevel(Skill.HITPOINTS);

                case "prayer":
                case "pray":
                    return SkillAPI.getBoostedLevel(Skill.PRAYER);

                case "distance":
                case "target_distance":
                case "targetdistance":
                    return targetDistance >= 0 ? targetDistance : 0;

                case "player_frozen_ticks":
                case "self_frozen_ticks":
                case "frozenticks":
                    return playerFreezeTicks;

                case "target_frozen_ticks":
                case "targetfrozenticks":
                    return targetFreezeTicks;

                case "ticks_since_swap":
                case "swap_ticks":
                    return getTicksSinceLastSwap();

                case "idleticks":
                case "idle_ticks":
                    return getIdleTicks();

                case "ticktimer":
                case "tick_timer":
                    return tickTimerValue;

                case "attacktimer":
                case "attack_timer":
                    return getAttackTimer();

                case "ticktimer_myattack":
                case "myattacktimer":
                    return getMyAttackTimer();

                case "attacktimercount":
                case "attack_timer_count":
                    return getAttackTimerCount();

                case "targethp":
                case "target_hp":
                    return getTargetHpPercent();

                case "targetspec":
                case "target_spec":
                    return getTargetSpecPercent();

                case "regionid":
                case "region":
                    return client != null && client.getLocalPlayer() != null
                            ? client.getLocalPlayer().getWorldLocation().getRegionID()
                            : 0;

                case "run_energy":
                case "runenergy":
                    return client != null ? client.getEnergy() / 100 : 0;

                case "world":
                    return client != null ? client.getWorld() : 0;

                case "animation":
                    return client != null && client.getLocalPlayer() != null
                            ? client.getLocalPlayer().getAnimation()
                            : -1;

                default:
                    // Check for skill-specific lookups like "magic", "ranged", "strength"
                    int skillLevel = getSkillLevel(key);
                    if (skillLevel >= 0) {
                        return skillLevel;
                    }
                    Logger.warn("[Gear Swapper] Unknown numeric variable in condition: " + name);
                    return 0;
            }
        } catch (Exception e) {
            Logger.warn("[Gear Swapper] Error resolving numeric variable '" + name + "': " + e.getMessage());
            return 0;
        }
    }

    private boolean compareInts(int left, int right, String op) {
        switch (op) {
            case "<":
                return left < right;
            case ">":
                return left > right;
            case "<=":
                return left <= right;
            case ">=":
                return left >= right;
            case "==":
                return left == right;
            case "!=":
                return left != right;
            default:
                return false;
        }
    }

    private void equipItem(String itemName) {
        equipItem(itemName, false);
    }

    private void equipItem(String itemName, boolean isRetry) {
        try {
            Runnable equipTask = () -> {
                Logger.norm("[Gear Swapper] equipItem pattern: " + itemName + (isRetry ? " (retry)" : ""));
                boolean multiMatch = itemName != null && itemName.trim().endsWith("*");
                boolean anyMatched = false;

                for (ItemEx item : InventoryAPI.getItems()) {
                    if (item == null || item.getName() == null) {
                        continue;
                    }

                    if (!matchesItemPattern(item.getName(), itemName)) {
                        continue;
                    }

                    anyMatched = true;

                    String[] actions = item.getActions();
                    if (actions == null) {
                        continue;
                    }

                    boolean equippedThisItem = false;

                    for (String actionName : new String[] { "Wear", "Equip", "Wield" }) {
                        for (String action : actions) {
                            if (action != null && action.equalsIgnoreCase(actionName)) {
                                InventoryAPI.interact(item, actionName);
                                Logger.norm("[Gear Swapper] Equipped: " + item.getName()
                                        + (clickHeatmapEnabled ? " (with heatmap delay)" : ""));
                                equippedThisItem = true;
                                break;
                            }
                        }

                        if (equippedThisItem) {
                            break;
                        }
                    }

                    // For exact-name patterns, keep old behavior: stop after first match.
                    // For wildcard patterns (ending with '*'), continue to equip all matches.
                    if (equippedThisItem && !multiMatch) {
                        return;
                    }
                }

                if (!anyMatched) {
                    Logger.norm("[Gear Swapper] No inventory items matched pattern: " + itemName);

                    // Timing guard: if this is the first attempt, retry once on the next tick
                    if (!isRetry) {
                        int targetTick = tickCounter + 1;
                        Logger.norm("[Gear Swapper] Scheduling one-tick retry for pattern: " + itemName + " at tick "
                                + targetTick);
                        scheduledScriptTasks.add(
                                new ScriptTask(targetTick, () -> equipItem(itemName, true), "Retry equip " + itemName));
                    }
                }
            };

            if (clickHeatmapEnabled && !inScriptHeatmapBlock) {
                executeCommandWithDelay(equipTask);
            } else {
                Static.invoke(equipTask);
            }
        } catch (Throwable e) {
            Logger.error("[Gear Swapper] Error equipping item: " + itemName + " - " + e.getMessage());
        }
    }

    private boolean castSpellWithReturn(String spellName) {
        try {
            String lower = spellName.trim().toLowerCase();
            if (lower.isEmpty()) {
                return false;
            }

            // Use a boolean array to return success from lambda
            final boolean[] success = { false };

            Static.invoke(() -> {
                int boostedMagic = SkillAPI.getBoostedLevel(Skill.MAGIC);
                int effectiveMagic = boostedMagic;

                if (sanfewClickedThisTick) {
                    effectiveMagic = 99;
                    Logger.norm("[Gear Swapper] Sanfew flag active this tick - boosted=" + boostedMagic + ", effective="
                            + effectiveMagic);
                } else if (brewClickedThisTick) {
                    effectiveMagic = Math.max(1, boostedMagic - 11);
                    Logger.norm("[Gear Swapper] Brew flag active this tick - boosted=" + boostedMagic + ", effective="
                            + effectiveMagic);
                }

                Logger.norm(
                        "[Gear Swapper] Attempting to cast '" + spellName + "' with effective magic " + effectiveMagic);

                if (!canCastSpell(lower, effectiveMagic)) {
                    Logger.norm("[Gear Swapper] Cannot cast '" + spellName + "' at effective level " + effectiveMagic
                            + " (boosted " + boostedMagic + "), skipping.");
                    return;
                }

                Ancient spell = getAncientSpell(lower);
                if (spell == null) {
                    Logger.norm("[Gear Swapper] Unknown spell '" + spellName + "', skipping.");
                    return;
                }

                Logger.norm("[Gear Swapper] Found spell '" + spellName + "', casting...");

                // Check if spell can be cast
                if (!spell.canCast()) {
                    Logger.norm(
                            "[Gear Swapper] Spell cannot be cast for '" + spellName + "' (runes/gear/book), skipping.");
                    return;
                }

                // Get target for combat spells
                Player target = cachedTarget;
                if (target == null && client.getLocalPlayer() != null) {
                    Actor interacting = client.getLocalPlayer().getInteracting();
                    if (interacting instanceof Player) {
                        target = (Player) interacting;
                    }
                }

                if (target != null) {
                    PlayerEx targetEx = new PlayerEx(target);
                    spell.castOn(targetEx);
                } else {
                    spell.cast();
                }

                Logger.norm("[Gear Swapper] Cast spell: " + spellName);
                success[0] = true;
            });

            return success[0];
        } catch (Exception ex) {
            Logger.error("[Gear Swapper] Error casting spell: " + spellName + " - " + ex.getMessage());
            return false;
        }
    }

    private void castSpell(String spellName) {
        try {
            String lower = spellName.trim().toLowerCase();
            if (lower.isEmpty()) {
                return;
            }

            Runnable castTask = () -> {
                int boostedMagic = SkillAPI.getBoostedLevel(Skill.MAGIC);
                int effectiveMagic = boostedMagic;

                if (sanfewClickedThisTick) {
                    effectiveMagic = 99;
                    Logger.norm("[Gear Swapper] Sanfew flag active this tick - boosted=" + boostedMagic + ", effective="
                            + effectiveMagic);
                } else if (brewClickedThisTick) {
                    effectiveMagic = Math.max(1, boostedMagic - 11);
                    Logger.norm("[Gear Swapper] Brew flag active this tick - boosted=" + boostedMagic + ", effective="
                            + effectiveMagic);
                }

                Logger.norm(
                        "[Gear Swapper] Attempting to cast '" + spellName + "' with effective magic " + effectiveMagic);

                if (!canCastSpell(lower, effectiveMagic)) {
                    Logger.norm("[Gear Swapper] Cannot cast '" + spellName + "' at effective level " + effectiveMagic
                            + " (boosted " + boostedMagic + "), skipping.");
                    return;
                }

                Ancient spell = getAncientSpell(lower);
                if (spell == null) {
                    Logger.norm("[Gear Swapper] Unknown spell '" + spellName + "', skipping.");
                    return;
                }

                Logger.norm("[Gear Swapper] Found spell '" + spellName + "', casting...");

                // Check if spell can be cast
                if (!spell.canCast()) {
                    Logger.norm(
                            "[Gear Swapper] Spell cannot be cast for '" + spellName + "' (runes/gear/book), skipping.");
                    return;
                }

                // Get target for combat spells
                Player target = cachedTarget;
                if (target == null && client.getLocalPlayer() != null) {
                    Actor interacting = client.getLocalPlayer().getInteracting();
                    if (interacting instanceof Player) {
                        target = (Player) interacting;
                    }
                }

                if (target != null) {
                    PlayerEx targetEx = new PlayerEx(target);
                    spell.castOn(targetEx);
                } else {
                    spell.cast();
                }

                Logger.norm("[Gear Swapper] Cast spell: " + spellName
                        + (clickHeatmapEnabled ? " (with heatmap delay)" : ""));
            };

            if (clickHeatmapEnabled && !inScriptHeatmapBlock) {
                executeCommandWithDelay(castTask);
            } else {
                Static.invoke(castTask);
            }
        } catch (Exception ex) {
            Logger.error("[Gear Swapper] Error casting spell: " + spellName + " - " + ex.getMessage());
        }
    }

    private void activatePrayer(String prayerName) {
        try {
            String lower = prayerName.trim().toLowerCase();
            if (lower.isEmpty()) {
                return;
            }

            PrayerAPI prayer = getPrayerByName(lower);
            if (prayer == null) {
                Logger.norm("[Gear Swapper] Unknown prayer '" + prayerName + "', skipping.");
                return;
            }

            Static.invoke(() -> {
                prayer.turnOn();
                Logger.norm("[Gear Swapper] Enabled prayer: " + prayerName);
            });
        } catch (Exception ex) {
            Logger.error("[Gear Swapper] Error activating prayer: " + prayerName + " - " + ex.getMessage());
        }
    }

    private void togglePrayer(String prayerName) {
        try {
            String lower = prayerName.trim().toLowerCase();
            if (lower.isEmpty()) {
                return;
            }

            PrayerAPI prayer = getPrayerByName(lower);
            if (prayer == null) {
                Logger.norm("[Gear Swapper] Unknown prayer '" + prayerName + "', skipping.");
                return;
            }

            Static.invoke(prayer::toggle);
        } catch (Exception ex) {
            Logger.error("[Gear Swapper] Error toggling prayer: " + prayerName + " - " + ex.getMessage());
        }
    }

    private void attackTarget() {
        Logger.norm("[Gear Swapper] Attack flag enabled - attacking current target");

        Runnable attackTask = this::attackCurrentTarget;

        if (clickHeatmapEnabled && !inScriptHeatmapBlock) {
            executeCommandWithDelay(attackTask);
        } else {
            attackCurrentTarget();
        }
    }

    private void applyPrayers(String[] prayerOptions) {
        try {
            if (prayerOptions == null || prayerOptions.length == 0) {
                return;
            }

            Static.invoke(() -> {
                Client client = Static.getClient();

                for (String prayerName : prayerOptions) {
                    if (prayerName == null) {
                        continue;
                    }

                    String lower = prayerName.trim().toLowerCase();
                    if (lower.isEmpty()) {
                        continue;
                    }

                    PrayerAPI prayer = getPrayerByName(lower);
                    if (prayer == null) {
                        Logger.norm("[Gear Swapper] Unknown prayer '" + prayerName + "', skipping.");
                        continue;
                    }

                    // First try normal PrayerAPI.turnOn() behaviour
                    prayer.turnOn();

                    boolean active = prayer.isActive();

                    // If it didn't activate and we're in an LMS-style environment (boosted 99, real
                    // below requirement),
                    // bypass the PrayerAPI.hasLevelFor gating and click the widget directly using
                    // VitaLite's WidgetAPI.
                    if (!active && client != null) {
                        int boostedLevel = client.getBoostedSkillLevel(Skill.PRAYER);
                        int realLevel = client.getRealSkillLevel(Skill.PRAYER);

                        if (boostedLevel == 99 && realLevel < prayer.getLevel() && boostedLevel > 0) {
                            WidgetAPI.interact(1, prayer.getInterfaceId(), -1, -1);
                            active = prayer.isActive();
                        }
                    }

                    if (active) {
                        Logger.norm("[Gear Swapper] Enabled prayer: " + prayerName);
                    } else {
                        Logger.norm("[Gear Swapper] Failed to enable prayer: " + prayerName);
                    }
                }
            });
        } catch (Throwable e) {
            Logger.error("[Gear Swapper] Error applying prayers: " + e.getMessage());
        }
    }

    private void enableSpecialAttack() {
        try {
            Static.invoke(() -> {
                if (client == null || client.getLocalPlayer() == null) {
                    Logger.norm("[Gear Swapper] Cannot enable special attack - client or local player is null");
                    return;
                }

                Logger.norm("[Gear Swapper] Enabling special attack");

                try {
                    // Check current special attack state
                    boolean currentlyEnabled = CombatAPI.isSpecEnabled();
                    int specEnergy = CombatAPI.getSpecEnergy();

                    if (currentlyEnabled) {
                        Logger.norm("[Gear Swapper] Special attack already enabled");
                        return;
                    }

                    if (specEnergy < 50) {
                        Logger.norm(
                                "[Gear Swapper] Cannot enable special attack - not enough energy: " + specEnergy + "%");
                        return;
                    }

                    // Use VitaLite API to toggle special attack
                    CombatAPI.toggleSpec();
                    Logger.norm("[Gear Swapper] Special attack enabled");

                } catch (Exception e) {
                    Logger.error("[Gear Swapper] Error enabling special attack: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Logger.error("[Gear Swapper] Error enabling special attack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private PrayerAPI getPrayerByName(String name) {
        switch (name) {
            case "thick skin":
                return PrayerAPI.THICK_SKIN;
            case "burst of strength":
                return PrayerAPI.BURST_OF_STRENGTH;
            case "clarity of thought":
                return PrayerAPI.CLARITY_OF_THOUGHT;
            case "sharp eye":
                return PrayerAPI.SHARP_EYE;
            case "mystic will":
                return PrayerAPI.MYSTIC_WILL;
            case "rock skin":
                return PrayerAPI.ROCK_SKIN;
            case "superhuman strength":
                return PrayerAPI.SUPERHUMAN_STRENGTH;
            case "improved reflexes":
                return PrayerAPI.IMPROVED_REFLEXES;
            case "rapid restore":
                return PrayerAPI.RAPID_RESTORE;
            case "rapid heal":
                return PrayerAPI.RAPID_HEAL;
            case "protect item":
                return PrayerAPI.PROTECT_ITEM;
            case "hawk eye":
                return PrayerAPI.HAWK_EYE;
            case "mystic lore":
                return PrayerAPI.MYSTIC_LORE;
            case "steel skin":
                return PrayerAPI.STEEL_SKIN;
            case "ultimate strength":
                return PrayerAPI.ULTIMATE_STRENGTH;
            case "incredible reflexes":
                return PrayerAPI.INCREDIBLE_REFLEXES;
            case "protect from magic":
                return PrayerAPI.PROTECT_FROM_MAGIC;
            case "protect from missiles":
                return PrayerAPI.PROTECT_FROM_MISSILES;
            case "protect from melee":
                return PrayerAPI.PROTECT_FROM_MELEE;
            case "eagle eye":
                return PrayerAPI.EAGLE_EYE;
            case "mystic might":
                return PrayerAPI.MYSTIC_MIGHT;
            case "retribution":
                return PrayerAPI.RETRIBUTION;
            case "redemption":
                return PrayerAPI.REDEMPTION;
            case "smite":
                return PrayerAPI.SMITE;
            case "preserve":
                return PrayerAPI.PRESERVE;
            case "chivalry":
                return PrayerAPI.CHIVALRY;
            case "piety":
                return PrayerAPI.PIETY;
            case "rigour":
                return PrayerAPI.RIGOUR;
            case "augury":
                return PrayerAPI.AUGURY;
            default:
                return null;
        }
    }

    private boolean matchesItemPattern(String itemName, String pattern) {
        if (itemName == null || pattern == null) {
            return false;
        }

        String normItem = normalizeItemName(itemName);
        String normPattern = normalizeItemName(pattern);

        if (normItem.isEmpty() || normPattern.isEmpty()) {
            return false;
        }

        // Exact match after normalization
        if (normItem.equals(normPattern)) {
            return true;
        }

        // Wildcard support: pattern ending with * means "contains" match
        if (pattern.trim().endsWith("*")) {
            String rawPrefix = pattern.trim().substring(0, pattern.trim().length() - 1);
            String normPrefix = normalizeItemName(rawPrefix);
            return !normPrefix.isEmpty() && normItem.contains(normPrefix);
        }

        // Fallback: allow partial match in either direction after normalization
        return normItem.contains(normPattern) || normPattern.contains(normItem);
    }

    private String normalizeItemName(String name) {
        String s = name.toLowerCase().trim();
        // Strip common punctuation and brackets so "Helm of neitiznot" and variants
        // match
        s = s.replaceAll("[\\[\\]()'`-]", " ");
        // Collapse multiple spaces
        s = s.replaceAll("\\s+", " ");
        return s.trim();
    }

    private void castSpellWithFallback(String[] spellOptions) {
        try {
            if (spellOptions == null || spellOptions.length == 0 || client == null) {
                return;
            }

            Static.invoke(() -> {
                int boostedMagic = SkillAPI.getBoostedLevel(Skill.MAGIC);
                int effectiveMagic = boostedMagic;

                if (sanfewClickedThisTick) {
                    effectiveMagic = 99;
                    Logger.norm("[Gear Swapper] Sanfew flag active this tick - boosted=" + boostedMagic + ", effective="
                            + effectiveMagic);
                } else if (brewClickedThisTick) {
                    effectiveMagic = Math.max(1, boostedMagic - 11);
                    Logger.norm("[Gear Swapper] Brew flag active this tick - boosted=" + boostedMagic + ", effective="
                            + effectiveMagic);
                }

                for (String spellName : spellOptions) {
                    if (spellName == null) {
                        continue;
                    }

                    spellName = spellName.trim().toLowerCase();

                    if (!canCastSpell(spellName, effectiveMagic)) {
                        Logger.norm("[Gear Swapper] Cannot cast '" + spellName + "' at effective level "
                                + effectiveMagic + " (boosted " + boostedMagic + "), skipping.");
                        continue;
                    }

                    Ancient spell = getAncientSpell(spellName);
                    if (spell == null) {
                        Logger.norm("[Gear Swapper] Unknown spell '" + spellName + "', skipping.");
                        continue;
                    }

                    if (!spell.canCast()) {
                        Logger.norm("[Gear Swapper] Spell cannot be cast for '" + spellName
                                + "' (runes/gear/book), skipping.");
                        continue;
                    }

                    Player target = cachedTarget;
                    if (target == null && client.getLocalPlayer() != null) {
                        Actor interacting = client.getLocalPlayer().getInteracting();
                        if (interacting instanceof Player) {
                            target = (Player) interacting;
                        }
                    }

                    if (target != null) {
                        PlayerEx targetEx = new PlayerEx(target);
                        spell.castOn(targetEx);
                        Logger.norm(
                                "[Gear Swapper] Cast on target '" + target.getName() + "' using spell: " + spellName);
                    } else {
                        MagicAPI.cast(spell);
                        Logger.norm("[Gear Swapper] Cast (no target) using spell: " + spellName);
                    }

                    return;
                }

                Logger.norm("[Gear Swapper] No castable spell found for options: " + String.join(", ", spellOptions));
            });
        } catch (Throwable e) {
            Logger.error("[Gear Swapper] Error casting spell: " + e.getMessage());
        }
    }

    private Ancient getAncientSpell(String spellName) {
        switch (spellName) {
            case "ice barrage":
                return Ancient.ICE_BARRAGE;
            case "ice blitz":
                return Ancient.ICE_BLITZ;
            case "ice burst":
                return Ancient.ICE_BURST;
            case "ice rush":
                return Ancient.ICE_RUSH;
            case "blood barrage":
                return Ancient.BLOOD_BARRAGE;
            case "blood blitz":
                return Ancient.BLOOD_BLITZ;
            case "blood burst":
                return Ancient.BLOOD_BURST;
            case "blood rush":
                return Ancient.BLOOD_RUSH;
            case "shadow barrage":
                return Ancient.SHADOW_BARRAGE;
            case "shadow blitz":
                return Ancient.SHADOW_BLITZ;
            case "shadow burst":
                return Ancient.SHADOW_BURST;
            case "shadow rush":
                return Ancient.SHADOW_RUSH;
            case "smoke barrage":
                return Ancient.SMOKE_BARRAGE;
            case "smoke blitz":
                return Ancient.SMOKE_BLITZ;
            case "smoke burst":
                return Ancient.SMOKE_BURST;
            case "smoke rush":
                return Ancient.SMOKE_RUSH;
            default:
                return null;
        }
    }

    private boolean canCastSpell(String spellName, int magicLevel) {
        switch (spellName) {
            case "ice barrage":
                return magicLevel >= 94;
            case "blood barrage":
                return magicLevel >= 92;
            case "shadow barrage":
                return magicLevel >= 88;
            case "smoke barrage":
                return magicLevel >= 86;
            case "ice blitz":
                return magicLevel >= 82;
            case "blood blitz":
                return magicLevel >= 80;
            case "shadow blitz":
                return magicLevel >= 76;
            case "smoke blitz":
                return magicLevel >= 74;
            case "ice burst":
                return magicLevel >= 70;
            case "blood burst":
                return magicLevel >= 68;
            case "shadow burst":
                return magicLevel >= 64;
            case "smoke burst":
                return magicLevel >= 62;
            case "ice rush":
                return magicLevel >= 58;
            case "blood rush":
                return magicLevel >= 56;
            case "shadow rush":
                return magicLevel >= 52;
            case "smoke rush":
                return magicLevel >= 50;
            default:
                return false;
        }
    }

    private void attackCurrentTarget() {
        try {
            if (client == null || client.getLocalPlayer() == null) {
                return;
            }

            Actor targetActor = client.getLocalPlayer().getInteracting();
            Player targetPlayer = null;

            if (targetActor instanceof Player) {
                targetPlayer = (Player) targetActor;
            } else if (cachedTarget != null) {
                targetPlayer = cachedTarget;
            }

            if (targetPlayer == null) {
                return;
            }

            final Player finalTarget = targetPlayer;

            Static.invoke(() -> {
                PlayerEx targetEx = new PlayerEx(finalTarget);
                String[] actions = targetEx.getActions();
                int fightIndex = -1;

                if (actions != null) {
                    // Prefer exact 'Fight', then any action containing 'fight', then 'attack'
                    for (int i = 0; i < actions.length; i++) {
                        String action = actions[i];
                        if (action == null) {
                            continue;
                        }

                        String lower = action.toLowerCase();
                        if (action.equalsIgnoreCase("Fight") || lower.contains("fight")) {
                            fightIndex = i;
                            break;
                        }
                    }

                    if (fightIndex == -1) {
                        for (int i = 0; i < actions.length; i++) {
                            String action = actions[i];
                            if (action == null) {
                                continue;
                            }

                            String lower = action.toLowerCase();
                            if (lower.contains("attack")) {
                                fightIndex = i;
                                break;
                            }
                        }
                    }
                }

                if (fightIndex != -1) {
                    targetEx.interact(fightIndex);
                }
            });
            Logger.norm("[Gear Swapper] Attacking target using 'Fight' option: " + targetPlayer.getName());
        } catch (Throwable e) {
            Logger.error("[Gear Swapper] Error attacking target: " + e.getMessage());
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        tickCounter++;
        lastTickStartMs = System.currentTimeMillis();

        // Decrement tick timer if active (for looper SetTickTimer command)
        if (tickTimerValue > 0) {
            tickTimerValue--;
        }

        if (brewClickedThisTick && brewFlagTick != tickCounter) {
            brewClickedThisTick = false;
            brewFlagTick = -1;
        }

        if (sanfewClickedThisTick && sanfewFlagTick != tickCounter) {
            sanfewClickedThisTick = false;
            sanfewFlagTick = -1;
        }

        // If player or target moved while considered frozen, clear freeze state
        if (client != null) {
            if (playerFreezeTicks > 0 && client.getLocalPlayer() != null) {
                WorldPoint currentPlayerLoc = client.getLocalPlayer().getWorldLocation();
                if (currentPlayerLoc != null && playerFreezeLocation != null
                        && !currentPlayerLoc.equals(playerFreezeLocation)) {
                    playerFreezeTicks = 0;
                    playerFreezeImmunityTicks = 0;
                    playerFreezeLocation = null;
                }
            }

            if (targetFreezeTicks > 0 && currentTarget != null) {
                WorldPoint currentTargetLoc = currentTarget.getWorldLocation();
                if (currentTargetLoc != null && targetFreezeLocation != null
                        && !currentTargetLoc.equals(targetFreezeLocation)) {
                    targetFreezeTicks = 0;
                    targetFreezeImmunityTicks = 0;
                    targetFreezeLocation = null;
                }
            }
        }

        boolean playerWasFrozen = playerFreezeTicks > 0;
        boolean targetWasFrozen = targetFreezeTicks > 0;

        if (playerFreezeTicks > 0) {
            playerFreezeTicks--;
        }
        if (targetFreezeTicks > 0) {
            targetFreezeTicks--;
        }

        if (playerWasFrozen && playerFreezeTicks == 0) {
            playerFreezeImmunityTicks = FREEZE_IMMUNITY_TICKS;
            playerFreezeLocation = null;
        }
        if (targetWasFrozen && targetFreezeTicks == 0) {
            targetFreezeImmunityTicks = FREEZE_IMMUNITY_TICKS;
            targetFreezeLocation = null;
        }

        if (playerFreezeImmunityTicks > 0) {
            playerFreezeImmunityTicks--;
        }
        if (targetFreezeImmunityTicks > 0) {
            targetFreezeImmunityTicks--;
        }

        if (client != null && client.getLocalPlayer() != null) {
            Actor interacting = client.getLocalPlayer().getInteracting();
            if (interacting instanceof Player) {
                // Live target: update current and cached target
                currentTarget = (Player) interacting;
                currentTargetName = currentTarget.getName();
                cachedTarget = currentTarget;
                cachedTargetTime = System.currentTimeMillis();

                WorldPoint localPos = client.getLocalPlayer().getWorldLocation();
                WorldPoint targetPos = currentTarget.getWorldLocation();
                if (localPos != null && targetPos != null) {
                    targetDistance = localPos.distanceTo(targetPos);
                }
            } else if (cachedTarget != null) {
                // Use cached target only if it is still fresh
                long now = System.currentTimeMillis();
                if (cachedTargetTime > 0 && now - cachedTargetTime < TARGET_CACHE_DURATION_MS) {
                    currentTarget = cachedTarget;
                    currentTargetName = currentTarget.getName();

                    WorldPoint localPos = client.getLocalPlayer().getWorldLocation();
                    WorldPoint targetPos = currentTarget.getWorldLocation();
                    if (localPos != null && targetPos != null) {
                        targetDistance = localPos.distanceTo(targetPos);
                    }
                } else {
                    // Cached target expired
                    cachedTarget = null;
                    cachedTargetTime = 0L;
                    currentTarget = null;
                    currentTargetName = null;
                    targetDistance = -1;
                }
            } else {
                currentTarget = null;
                currentTargetName = null;
                targetDistance = -1;
            }
        }

        if (panel != null) {
            panel.updateFromPlugin(this);
        }

        // Process scheduled script tasks for this tick (Tick:N feature)
        if (!scheduledScriptTasks.isEmpty()) {
            List<ScriptTask> tasksToRemove = new ArrayList<>();
            for (ScriptTask task : scheduledScriptTasks) {
                if (task.targetTick == tickCounter) {
                    Logger.norm("[Gear Swapper] Executing scheduled task: " + task.description + " (tick " + tickCounter
                            + ")");
                    try {
                        task.action.run();
                    } catch (Exception e) {
                        Logger.error("[Gear Swapper] Error executing scheduled task: " + e.getMessage());
                    }
                    tasksToRemove.add(task);
                } else if (task.targetTick < tickCounter) {
                    // Task missed its tick (shouldn't happen normally), run it now
                    Logger.warn("[Gear Swapper] Running late scheduled task: " + task.description + " (was for tick "
                            + task.targetTick + ", now " + tickCounter + ")");
                    try {
                        task.action.run();
                    } catch (Exception e) {
                        Logger.error("[Gear Swapper] Error executing late scheduled task: " + e.getMessage());
                    }
                    tasksToRemove.add(task);
                }
            }
            scheduledScriptTasks.removeAll(tasksToRemove);
        }

        // Show animation overhead if enabled (independent of looper)
        if (config != null && config.showAnimationOverhead() && client != null && client.getLocalPlayer() != null) {
            int currentAnim = client.getLocalPlayer().getAnimation();
            client.getLocalPlayer().setOverheadText("Anim: " + currentAnim);
            client.getLocalPlayer().setOverheadCycle(100);
        }

        // Global looper script: execute once per tick when enabled
        if (looperEnabled && looperScript != null && !looperScript.trim().isEmpty()) {

            // Check if paused by RandomAfkChance or Wait command
            if (isLooperPaused()) {
                return;
            }

            // Process any ongoing DropAll operation
            if (processDropAllTick()) {
                return; // Still dropping, skip script execution this tick
            }

            // Execute looper script directly (bypasses scheduling - evaluates every tick)
            // This is different from loadout scripts which may use Tick:N delays
            try {
                executeLooperScriptDirect(looperScript);
            } catch (ScriptStopException e) {
                // Script stopped intentionally - this is expected behavior
                // Don't spam logs for normal Stop commands
            }
        }

        // Process trigger system events
        try {
            if (triggerEngine != null && triggerEngine.isEnabled()) {
                TriggerEvent triggerEvent = new TriggerEvent(TriggerEventType.GAME_TICK, event);
                triggerEngine.processEvent(triggerEvent);
            }
        } catch (Exception e) {
            Logger.error("[Gear Swapper 2.0] Error processing game tick for triggers: " + e.getMessage());
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event) {
        if (event.getActor() == null) {
            return;
        }

        @SuppressWarnings("deprecation")
        int graphicId = event.getActor().getGraphic();
        if (Arrays.stream(FREEZE_GFX_IDS).anyMatch(id -> id == graphicId)) {
            if (client != null && event.getActor() == client.getLocalPlayer()) {
                if (playerFreezeImmunityTicks <= 0 && playerFreezeTicks <= 0) {
                    playerFreezeTicks = FREEZE_DURATION_TICKS;
                    if (client.getLocalPlayer() != null) {
                        playerFreezeLocation = client.getLocalPlayer().getWorldLocation();
                    }
                }
            } else if (event.getActor() == currentTarget) {
                if (targetFreezeImmunityTicks <= 0 && targetFreezeTicks <= 0) {
                    targetFreezeTicks = FREEZE_DURATION_TICKS;
                    if (currentTarget != null) {
                        targetFreezeLocation = currentTarget.getWorldLocation();
                    }
                }
            }
        }
    }

    public Player getCurrentTarget() {
        return currentTarget;
    }

    public String getCurrentTargetName() {
        return currentTargetName;
    }

    public int getTargetDistance() {
        return targetDistance;
    }

    public void clearCurrentTarget() {
        currentTarget = null;
        currentTargetName = null;
        cachedTarget = null;
        cachedTargetTime = 0L;
        targetDistance = -1;
        targetFreezeTicks = 0;
        targetFreezeImmunityTicks = 0;
        targetFreezeLocation = null;
    }

    public int getPlayerFreezeTicks() {
        return playerFreezeTicks;
    }

    public int getTargetFreezeTicks() {
        return targetFreezeTicks;
    }

    public int getPlayerFreezeImmunityTicks() {
        return playerFreezeImmunityTicks;
    }

    public int getTargetFreezeImmunityTicks() {
        return targetFreezeImmunityTicks;
    }

    public String getLastLoadoutName() {
        return lastLoadoutName;
    }

    public int getLastLoadoutNum() {
        return lastLoadoutNum;
    }

    public int getTicksSinceLastSwap() {
        if (lastSwapTick < 0) {
            return -1;
        }
        return tickCounter - lastSwapTick;
    }

    // Looper script accessors
    public boolean isLooperEnabled() {
        return looperEnabled;
    }

    public void setLooperEnabled(boolean enabled) {
        this.looperEnabled = enabled;
        if (configManager != null) {
            try {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "looperEnabled", enabled);
            } catch (Exception e) {
                Logger.warn("[Gear Swapper] Failed to persist looperEnabled: " + e.getMessage());
            }
        }
    }

    public String getLooperScript() {
        return looperScript != null ? looperScript : "";
    }

    public void setLooperScript(String script) {
        this.looperScript = script != null ? script : "";
        if (configManager != null) {
            try {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "looperScript", this.looperScript);
            } catch (Exception e) {
                Logger.warn("[Gear Swapper] Failed to persist looperScript: " + e.getMessage());
            }
        }
    }

    private static BufferedImage createDefaultIcon() {
        try {
            return ImageUtil.loadImageResource(GearSwapperPlugin.class, "icon.png");
        } catch (Exception e) {
            // Fallback to simple icon if resource not found
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(0, 255, 0)); // Green for NoidSwap
            g.fillRect(2, 2, 12, 12);
            g.dispose();
            return img;
        }
    }

    /**
     * Execute move command to move tiles away from current target (live or cached).
     * tilesToMove=0 will walk directly onto the target's tile ("under" target).
     */
    public void executeMoveCommand(int tilesToMove) {
        if (client == null) {
            Logger.warn("[Gear Swapper] Client is null, cannot execute move command");
            return;
        }

        try {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null) {
                Logger.warn("[Gear Swapper] Local player is null, cannot execute move command");
                return;
            }

            // Get current target (live or cached with TTL)
            net.runelite.api.Actor target = localPlayer.getInteracting();
            if (target == null && cachedTarget != null) {
                long now = System.currentTimeMillis();
                if (cachedTargetTime > 0 && now - cachedTargetTime < TARGET_CACHE_DURATION_MS) {
                    target = cachedTarget;
                } else {
                    cachedTarget = null;
                    cachedTargetTime = 0L;
                }
            }
            if (target == null) {
                Logger.warn("[Gear Swapper] No target found (live or cached), cannot execute move command");
                return;
            }

            WorldPoint playerPos = localPlayer.getWorldLocation();
            WorldPoint targetPos = target.getWorldLocation();

            if (playerPos == null || targetPos == null) {
                Logger.warn("[Gear Swapper] Invalid positions, cannot execute move command");
                return;
            }

            WorldPoint moveTarget;

            if (tilesToMove == 0) {
                // Move:0 - stand directly on the target tile
                moveTarget = targetPos;
            } else {
                // Vector from target -> player
                int dx = playerPos.getX() - targetPos.getX();
                int dy = playerPos.getY() - targetPos.getY();

                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance == 0) {
                    // On same tile: choose simple direction east from target
                    moveTarget = new WorldPoint(targetPos.getX() + tilesToMove, targetPos.getY(), targetPos.getPlane());
                } else {
                    // Target position that is approximately tilesToMove away from target,
                    // in the same general direction as the current player.
                    double scale = tilesToMove / distance;
                    double rawX = targetPos.getX() + dx * scale;
                    double rawY = targetPos.getY() + dy * scale;

                    int destX = (int) Math.round(rawX);
                    int destY = (int) Math.round(rawY);

                    WorldPoint candidate = new WorldPoint(destX, destY, targetPos.getPlane());

                    // If rounding would keep us on the exact same tile, force at least
                    // a 1-tile move towards or away from the target depending on current distance.
                    if (candidate.equals(playerPos)) {
                        double desired = tilesToMove;
                        if (distance > desired + 0.1) {
                            // We should move closer: step 1 tile towards target
                            int stepX = Integer.compare(targetPos.getX() - playerPos.getX(), 0);
                            int stepY = Integer.compare(targetPos.getY() - playerPos.getY(), 0);
                            candidate = new WorldPoint(playerPos.getX() + stepX, playerPos.getY() + stepY,
                                    targetPos.getPlane());
                        } else if (distance < desired - 0.1) {
                            // We should move further: step 1 tile away from target
                            int stepX = Integer.compare(playerPos.getX() - targetPos.getX(), 0);
                            int stepY = Integer.compare(playerPos.getY() - targetPos.getY(), 0);
                            candidate = new WorldPoint(playerPos.getX() + stepX, playerPos.getY() + stepY,
                                    targetPos.getPlane());
                        }
                    }

                    moveTarget = candidate;
                }
            }

            // Use VitaLite MovementAPI (packets) instead of mouse simulation
            Static.invoke(() -> MovementAPI.walkToWorldPoint(moveTarget));

            Logger.norm("[Gear Swapper] Executed move command: tilesToMove=" + tilesToMove
                    + ", playerPos=" + playerPos
                    + ", targetPos=" + targetPos
                    + ", moveTarget=" + moveTarget);
        } catch (Exception e) {
            Logger.error("[Gear Swapper] Error executing move command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute diagonal move command relative to the current target.
     * Uses the quadrant from target->player to choose a diagonal direction
     * (NW/NE/SW/SE). tilesToMove=0 walks directly onto the target tile.
     */
    public void executeMoveDiagCommand(int tilesToMove) {
        if (client == null) {
            Logger.warn("[Gear Swapper] Client is null, cannot execute MoveDiag command");
            return;
        }

        try {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null) {
                Logger.warn("[Gear Swapper] Local player is null, cannot execute MoveDiag command");
                return;
            }

            // Get current target (live or cached with TTL)
            net.runelite.api.Actor target = localPlayer.getInteracting();
            if (target == null && cachedTarget != null) {
                long now = System.currentTimeMillis();
                if (cachedTargetTime > 0 && now - cachedTargetTime < TARGET_CACHE_DURATION_MS) {
                    target = cachedTarget;
                } else {
                    cachedTarget = null;
                    cachedTargetTime = 0L;
                }
            }
            if (target == null) {
                Logger.warn("[Gear Swapper] No target found (live or cached), cannot execute MoveDiag command");
                return;
            }

            WorldPoint playerPos = localPlayer.getWorldLocation();
            WorldPoint targetPos = target.getWorldLocation();

            if (playerPos == null || targetPos == null) {
                Logger.warn("[Gear Swapper] Invalid positions, cannot execute MoveDiag command");
                return;
            }

            WorldPoint moveTarget;

            if (tilesToMove == 0) {
                // MoveDiag:0 behaves like Move:0 -> stand under target
                moveTarget = targetPos;
            } else {
                // Vector from target -> player to determine quadrant
                int dx = playerPos.getX() - targetPos.getX();
                int dy = playerPos.getY() - targetPos.getY();

                int signX = Integer.compare(dx, 0); // -1, 0, or 1
                int signY = Integer.compare(dy, 0);

                // If player is perfectly horizontal/vertical to target,
                // pick a diagonal by defaulting the missing component.
                if (signX == 0 && signY == 0) {
                    // On same tile already - step north-east by default
                    signX = 1;
                    signY = 1;
                } else if (signX == 0) {
                    signX = 1; // default east
                } else if (signY == 0) {
                    signY = 1; // default north
                }

                int destX = targetPos.getX() + signX * tilesToMove;
                int destY = targetPos.getY() + signY * tilesToMove;

                moveTarget = new WorldPoint(destX, destY, targetPos.getPlane());
            }

            Static.invoke(() -> MovementAPI.walkToWorldPoint(moveTarget));

            Logger.norm("[Gear Swapper] Executed MoveDiag command: tilesToMove=" + tilesToMove
                    + ", playerPos=" + playerPos
                    + ", targetPos=" + targetPos
                    + ", moveTarget=" + moveTarget);
        } catch (Exception e) {
            Logger.error("[Gear Swapper] Error executing MoveDiag command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Click heatmap methods
    public boolean isClickHeatmapEnabled() {
        return clickHeatmapEnabled;
    }

    public void setClickHeatmapEnabled(boolean enabled) {
        this.clickHeatmapEnabled = enabled;
        if (configManager != null) {
            try {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "clickHeatmapEnabled", enabled);
            } catch (Exception e) {
                Logger.warn("[Gear Swapper] Failed to persist clickHeatmapEnabled: " + e.getMessage());
            }
        }
    }

    public int getMinClickDelay() {
        return minClickDelay;
    }

    public void setMinClickDelay(int minDelay) {
        this.minClickDelay = Math.max(0, Math.min(600, minDelay));
        if (configManager != null) {
            try {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "minClickDelay", this.minClickDelay);
            } catch (Exception e) {
                Logger.warn("[Gear Swapper] Failed to persist minClickDelay: " + e.getMessage());
            }
        }
    }

    public int getMaxClickDelay() {
        return maxClickDelay;
    }

    public void setMaxClickDelay(int maxDelay) {
        this.maxClickDelay = Math.max(0, Math.min(600, maxDelay));
        if (configManager != null) {
            try {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "maxClickDelay", this.maxClickDelay);
            } catch (Exception e) {
                Logger.warn("[Gear Swapper] Failed to persist maxClickDelay: " + e.getMessage());
            }
        }
    }

    /**
     * Get a randomized click delay within the heatmap range
     * Returns 0 if heatmap is disabled
     */
    public int getRandomizedClickDelay() {
        if (!clickHeatmapEnabled) {
            return 0;
        }

        // Ensure min < max
        int min = Math.min(minClickDelay, maxClickDelay);
        int max = Math.max(minClickDelay, maxClickDelay);

        // Generate random delay in the range
        return min + clickDelayRandom.nextInt(max - min + 1);
    }

    /**
     * Execute a command with heatmap delay if enabled.
     * Logs the chosen delay and ensures commands are executed in the
     * same order as the script (sequential queue).
     */
    private void executeCommandWithDelay(Runnable command) {
        if (!clickHeatmapEnabled) {
            // Execute immediately if heatmap is disabled
            Static.invoke(command);
            return;
        }

        ensureHeatmapScheduler();

        synchronized (heatmapLock) {
            heatmapQueue.add(command);
            if (!heatmapRunnerActive) {
                heatmapRunnerActive = true;
                scheduleNextHeatmapCommand();
            }
        }
    }

    /**
     * Ensure the heatmap scheduler exists and is ready.
     */
    private void ensureHeatmapScheduler() {
        if (heatmapScheduler == null || heatmapScheduler.isShutdown()) {
            heatmapScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        }
    }

    private void resetHeatmapQueueForNewSequence() {
        synchronized (heatmapLock) {
            heatmapQueue.clear();
            heatmapRunnerActive = false;
        }

        try {
            if (heatmapScheduler != null) {
                heatmapScheduler.shutdownNow();
                heatmapScheduler = null;
            }
        } catch (Exception e) {
            Logger.error("[Gear Swapper] Error resetting heatmap scheduler: " + e.getMessage());
        }
    }

    private void scheduleNextHeatmapCommand() {
        Runnable nextCommand;
        synchronized (heatmapLock) {
            nextCommand = heatmapQueue.poll();
            if (nextCommand == null) {
                heatmapRunnerActive = false;
                return;
            }
        }

        int delay = getRandomizedClickDelay();
        if (clickHeatmapEnabled && delay > 0) {
            // If Force 1 tick is enabled, clamp delay so execution stays within the
            // threshold window of the current tick.
            if (forceOneTickEnabled && lastTickStartMs > 0L) {
                long now = System.currentTimeMillis();
                long sinceTick = now - lastTickStartMs;
                if (sinceTick < 0L || sinceTick > 2000L) {
                    sinceTick = 0L;
                }

                int threshold = forceTickThresholdMs;
                long allowedRemaining = threshold - sinceTick;
                if (allowedRemaining <= 0L) {
                    Logger.norm("[Heatmap][Force1] Past threshold (elapsed=" + sinceTick
                            + "ms, threshold=" + threshold + "ms) - forcing immediate execution");
                    delay = 0;
                } else if (delay > allowedRemaining) {
                    Logger.norm("[Heatmap][Force1] Clamping delay from " + delay + "ms to "
                            + allowedRemaining + "ms (elapsed=" + sinceTick
                            + "ms, threshold=" + threshold + "ms)");
                    delay = (int) allowedRemaining;
                }
            }

            final int scheduledDelay = delay;
            final long scheduledAt = System.currentTimeMillis();
            Logger.norm("[Heatmap] Scheduling command with delay=" + scheduledDelay + "ms (range="
                    + minClickDelay + "-" + maxClickDelay + "ms, force1=" + forceOneTickEnabled
                    + ", threshold=" + forceTickThresholdMs + "ms) at t=" + scheduledAt + "ms");

            heatmapScheduler.schedule(() -> {
                long executedAt = System.currentTimeMillis();
                long elapsed = executedAt - scheduledAt;
                Logger.norm("[Heatmap] Executing command after " + elapsed
                        + "ms (scheduled delay=" + scheduledDelay + "ms)");

                Static.invoke(nextCommand);

                boolean hasMore;
                synchronized (heatmapLock) {
                    hasMore = !heatmapQueue.isEmpty();
                    if (!hasMore) {
                        heatmapRunnerActive = false;
                    }
                }

                if (hasMore) {
                    scheduleNextHeatmapCommand();
                }
            }, scheduledDelay, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            if (clickHeatmapEnabled) {
                Logger.norm("[Heatmap] Heatmap enabled but delay=0ms (min=" + minClickDelay
                        + ", max=" + maxClickDelay + ") - executing immediately");
            }

            Static.invoke(nextCommand);

            boolean hasMore;
            synchronized (heatmapLock) {
                hasMore = !heatmapQueue.isEmpty();
                if (!hasMore) {
                    heatmapRunnerActive = false;
                }
            }

            if (hasMore) {
                scheduleNextHeatmapCommand();
            }
        }
    }

    // ===== HELPER METHODS FOR CONDITIONS =====

    /**
     * Check if player is in combat.
     * True if we are interacting with something, or if something is interacting
     * with us.
     */
    private boolean isPlayerInCombat() {
        if (client == null || client.getLocalPlayer() == null) {
            return false;
        }

        Player player = client.getLocalPlayer();

        // Check if we are attacking/interacting with something
        if (player.getInteracting() != null) {
            return true;
        }

        // Check if any NPC is attacking us
        for (NPC npc : client.getNpcs()) {
            if (npc != null && npc.getInteracting() == player) {
                return true;
            }
        }

        // Check if any player is attacking us
        for (Player p : client.getPlayers()) {
            if (p != null && p != player && p.getInteracting() == player) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if player is currently moving/walking.
     * Uses pose animation comparison - if not idle pose, player is moving.
     */
    private boolean isPlayerMoving() {
        if (client == null || client.getLocalPlayer() == null) {
            return false;
        }

        Player player = client.getLocalPlayer();
        // If pose animation differs from idle pose, player is moving
        return player.getIdlePoseAnimation() != player.getPoseAnimation();
    }

    /**
     * Get skill level by name (boosted level)
     */
    private int getSkillLevel(String skillName) {
        if (skillName == null)
            return -1;
        String lower = skillName.toLowerCase().trim();

        try {
            switch (lower) {
                case "attack":
                    return SkillAPI.getBoostedLevel(Skill.ATTACK);
                case "strength":
                    return SkillAPI.getBoostedLevel(Skill.STRENGTH);
                case "defence":
                case "defense":
                    return SkillAPI.getBoostedLevel(Skill.DEFENCE);
                case "ranged":
                case "range":
                    return SkillAPI.getBoostedLevel(Skill.RANGED);
                case "prayer":
                    return SkillAPI.getBoostedLevel(Skill.PRAYER);
                case "magic":
                case "mage":
                    return SkillAPI.getBoostedLevel(Skill.MAGIC);
                case "hitpoints":
                case "hp":
                case "health":
                    return SkillAPI.getBoostedLevel(Skill.HITPOINTS);
                case "agility":
                    return SkillAPI.getBoostedLevel(Skill.AGILITY);
                case "herblore":
                    return SkillAPI.getBoostedLevel(Skill.HERBLORE);
                case "thieving":
                    return SkillAPI.getBoostedLevel(Skill.THIEVING);
                case "crafting":
                    return SkillAPI.getBoostedLevel(Skill.CRAFTING);
                case "fletching":
                    return SkillAPI.getBoostedLevel(Skill.FLETCHING);
                case "slayer":
                    return SkillAPI.getBoostedLevel(Skill.SLAYER);
                case "hunter":
                    return SkillAPI.getBoostedLevel(Skill.HUNTER);
                case "mining":
                    return SkillAPI.getBoostedLevel(Skill.MINING);
                case "smithing":
                    return SkillAPI.getBoostedLevel(Skill.SMITHING);
                case "fishing":
                    return SkillAPI.getBoostedLevel(Skill.FISHING);
                case "cooking":
                    return SkillAPI.getBoostedLevel(Skill.COOKING);
                case "firemaking":
                    return SkillAPI.getBoostedLevel(Skill.FIREMAKING);
                case "woodcutting":
                    return SkillAPI.getBoostedLevel(Skill.WOODCUTTING);
                case "runecraft":
                case "runecrafting":
                    return SkillAPI.getBoostedLevel(Skill.RUNECRAFT);
                case "construction":
                    return SkillAPI.getBoostedLevel(Skill.CONSTRUCTION);
                case "farming":
                    return SkillAPI.getBoostedLevel(Skill.FARMING);
                default:
                    return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Check if inventory contains item matching pattern (supports * wildcard)
     */
    private boolean hasItemInInventory(String itemPattern) {
        if (itemPattern == null || itemPattern.isEmpty())
            return false;

        try {
            for (ItemEx item : InventoryAPI.getItems()) {
                if (item != null && item.getName() != null) {
                    if (matchesItemPattern(item.getName(), itemPattern)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Logger.warn("[Gear Swapper] Error checking inventory for item: " + e.getMessage());
        }
        return false;
    }

    /**
     * Count items in inventory matching pattern
     */
    private int countItemsInInventory(String itemPattern) {
        if (itemPattern == null || itemPattern.isEmpty())
            return 0;

        int count = 0;
        try {
            for (ItemEx item : InventoryAPI.getItems()) {
                if (item != null && item.getName() != null) {
                    if (matchesItemPattern(item.getName(), itemPattern)) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            Logger.warn("[Gear Swapper] Error counting inventory items: " + e.getMessage());
        }
        return count;
    }

    /**
     * Check if target is diagonal to player
     */
    private boolean isTargetDiagonal() {
        if (client == null || client.getLocalPlayer() == null || currentTarget == null) {
            return false;
        }

        try {
            WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
            WorldPoint targetPos = currentTarget.getWorldLocation();

            if (playerPos == null || targetPos == null)
                return false;

            int dx = Math.abs(playerPos.getX() - targetPos.getX());
            int dy = Math.abs(playerPos.getY() - targetPos.getY());

            // Diagonal if both dx and dy are non-zero
            return dx > 0 && dy > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if target is praying a specific prayer
     */
    private boolean isTargetPraying(String prayerType) {
        if (currentTarget == null || prayerType == null)
            return false;

        try {
            // Check overhead icon
            HeadIcon headIcon = currentTarget.getOverheadIcon();
            if (headIcon == null)
                return false;

            String lower = prayerType.toLowerCase();
            switch (lower) {
                case "magic":
                case "mage":
                    return headIcon == HeadIcon.MAGIC;
                case "ranged":
                case "range":
                case "missiles":
                    return headIcon == HeadIcon.RANGED;
                case "melee":
                    return headIcon == HeadIcon.MELEE;
                case "smite":
                    return headIcon == HeadIcon.SMITE;
                case "redemption":
                    return headIcon == HeadIcon.REDEMPTION;
                case "retribution":
                    return headIcon == HeadIcon.RETRIBUTION;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get idle ticks (ticks since last action)
     */
    private int idleTickCounter = 0;
    private int lastPlayerAnimation = -1;

    private int getIdleTicks() {
        if (client == null || client.getLocalPlayer() == null)
            return 0;

        int currentAnim = client.getLocalPlayer().getAnimation();
        if (currentAnim != -1 && currentAnim != lastPlayerAnimation) {
            idleTickCounter = 0;
            lastPlayerAnimation = currentAnim;
        } else if (currentAnim == -1) {
            idleTickCounter++;
        }

        return idleTickCounter;
    }

    /**
     * Get attack timer (ticks until next attack)
     */
    private int getAttackTimer() {
        // This would need integration with AttackTimer plugin
        // For now, return a basic estimate
        return 0;
    }

    /**
     * Get my attack timer
     */
    private int getMyAttackTimer() {
        return getAttackTimer();
    }

    /**
     * Get attack timer count
     */
    private int getAttackTimerCount() {
        return getAttackTimer();
    }

    /**
     * Get target HP percentage
     */
    private int getTargetHpPercent() {
        if (currentTarget == null)
            return 0;

        try {
            int healthRatio = currentTarget.getHealthRatio();
            int healthScale = currentTarget.getHealthScale();

            if (healthRatio >= 0 && healthScale > 0) {
                return (healthRatio * 100) / healthScale;
            }
        } catch (Exception e) {
        }

        return 0;
    }

    /**
     * Get target spec percentage (estimate based on weapon)
     */
    private int getTargetSpecPercent() {
        // Cannot directly read target's spec - would need heuristics
        return 0;
    }

    // ===== MEMORY SYSTEM =====
    private final java.util.Map<String, String> memoryStore = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Check memory value - supports key_value format
     * e.g., getmemory_World_Uk checks if memory["world"] == "uk"
     */
    private boolean checkMemory(String memKey) {
        if (memKey == null || memKey.isEmpty())
            return false;

        // Parse key_value format
        int underscoreIdx = memKey.indexOf('_');
        if (underscoreIdx > 0) {
            String key = memKey.substring(0, underscoreIdx).toLowerCase();
            String expectedValue = memKey.substring(underscoreIdx + 1).toLowerCase();
            String actualValue = memoryStore.getOrDefault(key, "");
            return actualValue.equalsIgnoreCase(expectedValue);
        }

        // Just check if key exists and is truthy
        String value = memoryStore.getOrDefault(memKey.toLowerCase(), "");
        return !value.isEmpty() && !value.equalsIgnoreCase("false") && !value.equals("0")
                && !value.equalsIgnoreCase("off");
    }

    /**
     * Set memory value
     */
    public void setMemory(String key, String value) {
        if (key != null && !key.isEmpty()) {
            memoryStore.put(key.toLowerCase(), value != null ? value : "");
            Logger.norm("[Gear Swapper] Memory set: " + key + " = " + value);
        }
    }

    /**
     * Clear memory value
     */
    public void clearMemory(String key) {
        if (key != null) {
            memoryStore.remove(key.toLowerCase());
        }
    }

    // ===== LOOPER SCRIPT HELPER METHODS =====

    /**
     * Execute looper script directly without scheduling.
     * This method is designed for per-tick evaluation where every condition
     * is checked and matching actions are executed immediately.
     * 
     * Unlike executeCommands which can schedule tasks across ticks,
     * this method processes the entire script synchronously each tick.
     */
    private void executeLooperScriptDirect(String script) {
        if (script == null || script.isEmpty()) {
            return;
        }

        // First, check if we have pending commands from a previous Wait
        // These should be executed before re-evaluating the script
        if (pendingLooperCommands != null && pendingLooperCommandIndex < pendingLooperCommands.length) {
            boolean[] shouldAttack = { false };
            try {
                executePendingLooperCommands(shouldAttack);
            } catch (ScriptStopException e) {
                // Pending commands hit another Wait or stop - don't continue to main script
                return;
            }
            // Clear pending commands after successful execution
            pendingLooperCommands = null;
            pendingLooperCommandIndex = 0;
            return; // Don't re-evaluate the full script this tick
        }

        // Remove the REPEATER header if present (it's just a marker)
        String cleanScript = script.trim();
        if (cleanScript.toUpperCase().startsWith("REPEATER")) {
            int newlineIndex = cleanScript.indexOf('\n');
            if (newlineIndex != -1) {
                cleanScript = cleanScript.substring(newlineIndex + 1).trim();
            } else {
                return; // Just "REPEATER" with no commands
            }
        }

        // Process script using the internal command executor which handles if/else
        // blocks
        boolean[] shouldAttack = { false };
        executeLooperLines(cleanScript.split("\n"), 0, shouldAttack);
    }

    /**
     * Execute pending commands from a previous Wait.
     */
    private void executePendingLooperCommands(boolean[] shouldAttack) {
        for (int i = pendingLooperCommandIndex; i < pendingLooperCommands.length; i++) {
            String line = pendingLooperCommands[i];
            if (line == null)
                continue;

            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.equals("{") || trimmed.equals("}")) {
                continue;
            }

            // Remove inline comments
            int commentIndex = trimmed.indexOf("//");
            if (commentIndex > 0) {
                trimmed = trimmed.substring(0, commentIndex).trim();
            }

            // Update index before execution (in case of Wait)
            pendingLooperCommandIndex = i + 1;

            // Execute the command (may throw ScriptStopException for Wait)
            executeLooperCommand(trimmed, shouldAttack);
        }
    }

    /**
     * Process looper script lines, handling if/else blocks and commands.
     * This is a simplified version of executeCommandsInternal optimized for
     * per-tick looper execution without scheduling.
     */
    private void executeLooperLines(String[] lines, int startLine, boolean[] shouldAttack) {
        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i];
            if (line == null) {
                continue;
            }

            String trimmed = line.trim();

            // Skip empty lines and comments
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                continue;
            }

            // Remove inline comments
            int commentIndex = trimmed.indexOf("//");
            if (commentIndex > 0) {
                trimmed = trimmed.substring(0, commentIndex).trim();
            }

            // Skip pure braces - they are structural, not commands
            if (trimmed.equals("{") || trimmed.equals("}")) {
                continue;
            }

            String lower = trimmed.toLowerCase();

            // Handle if statements with multi-line blocks
            // Each top-level if block is evaluated independently - ScriptStopException
            // from one block should not prevent other blocks from being evaluated
            if (lower.startsWith("if") && (lower.startsWith("if(") || lower.startsWith("if "))) {
                try {
                    i = handleLooperConditionalBlock(lines, i, shouldAttack);
                } catch (ScriptStopException e) {
                    // Check if this exception has remaining commands to execute later (Wait
                    // command)
                    if (e.hasRemainingCommands()) {
                        pendingLooperCommands = e.getRemainingCommands();
                        pendingLooperCommandIndex = e.getStartIndex();
                        Logger.norm("[Gear Swapper] Wait: stored " +
                                (pendingLooperCommands.length - pendingLooperCommandIndex) +
                                " commands to execute after delay");
                    }

                    // This block triggered a stop (Wait, WaitIfWalking, etc.)
                    // Continue to evaluate remaining top-level if blocks
                    // Find the end of this if block to continue from there
                    int depth = 0;
                    boolean foundOpen = false;
                    for (int j = i; j < lines.length; j++) {
                        String s = lines[j].trim();
                        for (char c : s.toCharArray()) {
                            if (c == '{') {
                                depth++;
                                foundOpen = true;
                            }
                            if (c == '}') {
                                depth--;
                            }
                        }
                        if (foundOpen && depth == 0) {
                            i = j;
                            break;
                        }
                    }
                    // Continue to next if block
                }
            }
            // Skip else on its own line (handled by if block)
            else if (lower.equals("else") || lower.startsWith("else ") || lower.startsWith("else{")) {
                // Skip - this is handled by the if block above
                // Find matching closing brace
                int depth = 0;
                for (int j = i; j < lines.length; j++) {
                    String s = lines[j].trim();
                    for (char c : s.toCharArray()) {
                        if (c == '{')
                            depth++;
                        if (c == '}')
                            depth--;
                    }
                    if (depth == 0 && s.contains("}")) {
                        i = j;
                        break;
                    }
                }
            }
            // Handle regular commands
            else {
                executeLooperCommand(trimmed, shouldAttack);
            }
        }
    }

    /**
     * Handle a conditional block (if/else) in the looper script.
     * Returns the line index after the entire if/else block.
     */
    private int handleLooperConditionalBlock(String[] lines, int startIndex, boolean[] shouldAttack) {
        if (lines == null || startIndex < 0 || startIndex >= lines.length) {
            return startIndex;
        }

        String header = lines[startIndex].trim();

        // Remove inline comments from header
        int commentIdx = header.indexOf("//");
        if (commentIdx > 0) {
            header = header.substring(0, commentIdx).trim();
        }

        String lower = header.toLowerCase();

        // Extract condition - between if( and ) or if and {
        String condition = "";
        if (lower.startsWith("if(")) {
            int closeParenIdx = header.indexOf(')');
            if (closeParenIdx > 3) {
                condition = header.substring(3, closeParenIdx).trim();
            }
        } else if (lower.startsWith("if ")) {
            int braceIdx = header.indexOf('{');
            if (braceIdx > 2) {
                condition = header.substring(2, braceIdx).trim();
            } else {
                condition = header.substring(2).trim();
            }
            // Remove parentheses if present
            if (condition.startsWith("(") && condition.endsWith(")")) {
                condition = condition.substring(1, condition.length() - 1).trim();
            }
        }

        // Find the THEN block content and ELSE block content
        StringBuilder thenBlock = new StringBuilder();
        StringBuilder elseBlock = new StringBuilder();
        int endIndex = startIndex;
        int depth = 0;
        boolean inElse = false;
        boolean foundThenOpen = false;

        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            if (line == null)
                continue;

            String trimmed = line.trim();

            // Remove inline comments for parsing
            int cIdx = trimmed.indexOf("//");
            if (cIdx > 0) {
                trimmed = trimmed.substring(0, cIdx).trim();
            }

            for (int j = 0; j < trimmed.length(); j++) {
                char c = trimmed.charAt(j);

                if (c == '{') {
                    depth++;
                    if (!foundThenOpen) {
                        foundThenOpen = true;
                        continue; // Don't add opening brace
                    }
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        endIndex = i;

                        // Check for else after this closing brace
                        String remainder = trimmed.substring(j + 1).trim().toLowerCase();
                        if (remainder.startsWith("else")) {
                            inElse = true;
                            // Look for opening brace of else
                            int elseBraceIdx = remainder.indexOf('{');
                            if (elseBraceIdx != -1) {
                                depth = 1;
                                foundThenOpen = true;
                            }
                        }

                        // Check subsequent lines for else
                        if (!inElse) {
                            for (int k = i + 1; k < lines.length; k++) {
                                String nextLine = lines[k].trim().toLowerCase();
                                if (nextLine.isEmpty())
                                    continue;
                                if (nextLine.startsWith("else")) {
                                    inElse = true;
                                    endIndex = k;
                                    // Find opening brace
                                    for (int m = k; m < lines.length; m++) {
                                        if (lines[m].contains("{")) {
                                            depth = 1;
                                            i = m;
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }

                        if (!inElse && depth == 0) {
                            // No else - we're done
                            break;
                        }
                        continue;
                    }
                }

                if (foundThenOpen && depth > 0) {
                    if (inElse) {
                        elseBlock.append(c);
                    } else {
                        thenBlock.append(c);
                    }
                }
            }

            if (foundThenOpen && depth > 0) {
                if (inElse) {
                    elseBlock.append('\n');
                } else {
                    thenBlock.append('\n');
                }
            }

            if (depth == 0 && foundThenOpen) {
                break;
            }
        }

        // Evaluate condition and execute the appropriate block
        boolean conditionResult = evaluateCondition(condition);

        // Debug logging to trace condition evaluation
        if (client != null && client.getLocalPlayer() != null) {
            int anim = client.getLocalPlayer().getAnimation();
            Logger.norm("[GS Looper Debug] Condition: '" + condition + "' = " + conditionResult
                    + " (current animation: " + anim + ")");
        }

        if (conditionResult) {
            String thenContent = thenBlock.toString().trim();
            if (!thenContent.isEmpty()) {
                String[] blockLines = thenContent.split("\n");
                executeBlockWithWaitSupport(blockLines, shouldAttack);
            }
        } else {
            String elseContent = elseBlock.toString().trim();
            if (!elseContent.isEmpty()) {
                String[] blockLines = elseContent.split("\n");
                executeBlockWithWaitSupport(blockLines, shouldAttack);
            }
        }

        return endIndex;
    }

    /**
     * Execute block lines with Wait command support.
     * When Wait is encountered, stores remaining commands for later execution.
     */
    private void executeBlockWithWaitSupport(String[] blockLines, boolean[] shouldAttack) {
        for (int i = 0; i < blockLines.length; i++) {
            String line = blockLines[i];
            if (line == null)
                continue;

            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.equals("{") || trimmed.equals("}")) {
                continue;
            }

            // Remove inline comments
            int commentIndex = trimmed.indexOf("//");
            if (commentIndex > 0) {
                trimmed = trimmed.substring(0, commentIndex).trim();
            }

            String lower = trimmed.toLowerCase();

            // Special handling for Wait command - need to capture remaining lines
            if (lower.startsWith("wait:")) {
                try {
                    String[] parts = trimmed.split(":", 2);
                    if (parts.length >= 2) {
                        String value = parts[1].trim();
                        String[] waitParts = value.split(":");
                        int minMs = Integer.parseInt(waitParts[0].trim());
                        int maxMs = waitParts.length > 1 ? Integer.parseInt(waitParts[1].trim()) : minMs;
                        int delayMs = minMs + (int) (Math.random() * (maxMs - minMs + 1));
                        int delayTicks = Math.max(1, delayMs / 600);
                        scheduleLooperPause(delayTicks);

                        // Throw exception with remaining commands to execute after wait
                        throw new ScriptStopException("Wait:" + delayMs + "ms", blockLines, i + 1);
                    }
                } catch (NumberFormatException e) {
                    Logger.warn("[Gear Swapper] Invalid Wait format: " + trimmed);
                }
                continue;
            }

            // Execute non-wait command
            executeLooperCommand(trimmed, shouldAttack);
        }
    }

    /**
     * Execute a single looper command (not an if/else block).
     * This handles the new looper-specific commands like Stop, RandomAfkChance,
     * etc.
     */
    private void executeLooperCommand(String command, boolean[] shouldAttack) {
        if (command == null || command.isEmpty()) {
            return;
        }

        String trimmed = command.trim();
        String lower = trimmed.toLowerCase();

        // Handle colon-separated commands
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            if (parts.length >= 2) {
                String cmd = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                switch (cmd) {
                    case "log":
                        if (client != null) {
                            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[GS] " + value, "");
                        }
                        return;
                    case "dropall":
                        scheduleDropAllTask(value);
                        return;
                    case "npc":
                        String[] npcParts = value.split(":", 2);
                        String npcPattern = npcParts[0].trim();
                        String action = npcParts.length > 1 ? npcParts[1].trim() : null;
                        interactWithNpc(npcPattern, action);
                        return;
                    case "wait":
                        try {
                            String[] waitParts = value.split(":");
                            int minMs = Integer.parseInt(waitParts[0].trim());
                            int maxMs = waitParts.length > 1 ? Integer.parseInt(waitParts[1].trim()) : minMs;
                            int delayMs = minMs + (int) (Math.random() * (maxMs - minMs + 1));
                            int delayTicks = Math.max(1, delayMs / 600);
                            scheduleLooperPause(delayTicks);
                            throw new ScriptStopException("Wait:" + delayMs + "ms");
                        } catch (NumberFormatException e) {
                            Logger.warn("[Gear Swapper] Invalid Wait format: " + value);
                        }
                        return;
                    case "tick":
                        // Tick commands are ignored in looper - we evaluate every tick anyway
                        return;
                    case "item":
                        // Support item:Name:Action format
                        String[] itemParts = value.split(":", 2);
                        String itemPattern = itemParts[0].trim();
                        String itemAction = itemParts.length > 1 ? itemParts[1].trim() : null;
                        if (itemAction != null && !itemAction.isEmpty()) {
                            useItemWithAction(itemPattern, itemAction);
                        } else {
                            equipItem(itemPattern);
                        }
                        return;
                    case "settickimer":
                    case "setticktimer":
                        try {
                            tickTimerValue = Integer.parseInt(value);
                            Logger.norm("[Gear Swapper] TickTimer set to: " + tickTimerValue);
                        } catch (NumberFormatException e) {
                            Logger.warn("[Gear Swapper] Invalid SetTickTimer value: " + value);
                        }
                        return;
                    case "object":
                        // Support object:Name:Action format
                        String[] objectParts = value.split(":", 2);
                        String objectPattern = objectParts[0].trim();
                        String objectAction = objectParts.length > 1 ? objectParts[1].trim() : null;
                        interactWithObject(objectPattern, objectAction);
                        return;
                    case "prayer":
                        String[] prayers = { value };
                        applyPrayersForTrigger(prayers);
                        return;
                    case "special":
                        enableSpecialAttack();
                        return;
                    case "walk":
                        // Walk:X:Y:Z - Walk towards coordinates
                        try {
                            String[] coords = value.split(":");
                            if (coords.length >= 2) {
                                int x = Integer.parseInt(coords[0].trim());
                                int y = Integer.parseInt(coords[1].trim());
                                int z = coords.length >= 3 ? Integer.parseInt(coords[2].trim()) : 0;
                                WorldPoint target = new WorldPoint(x, y, z);
                                if (client != null) {
                                    com.tonic.api.game.MovementAPI.walkToWorldPoint(target);
                                    Logger.norm("[Gear Swapper] Walking to " + x + ":" + y + ":" + z);
                                }
                            } else {
                                Logger.warn("[Gear Swapper] Invalid Walk format: " + value
                                        + " (expected Walk:X:Y or Walk:X:Y:Z)");
                            }
                        } catch (NumberFormatException e) {
                            Logger.warn("[Gear Swapper] Invalid Walk coordinates: " + value);
                        }
                        return;
                }
            }
        }

        // Handle single-word commands
        if (lower.equals("stop")) {
            throw new ScriptStopException("Script stopped by Stop command");
        }
        if (lower.equals("waitifwalking") || lower.equals("wait_if_walking")) {
            if (isPlayerMoving()) {
                throw new ScriptStopException("WaitIfWalking: player is moving");
            }
            return;
        }
        if (lower.equals("randomafkchance")) {
            if (Math.random() < 0.15) {
                int waitTicks = 1 + (int) (Math.random() * 5);
                scheduleLooperPause(waitTicks);
                throw new ScriptStopException("RandomAfkChance: pausing for " + waitTicks + " ticks");
            }
            return;
        }
        if (lower.equals("special")) {
            enableSpecialAttack();
            return;
        }
        if (lower.equals("attack")) {
            attackTarget();
            return;
        }

        // Not a recognized command - could be an item name, but log a warning for
        // debugging
        // Don't try to equip things that look like code
        if (trimmed.contains("(") || trimmed.contains(")") ||
                trimmed.contains("{") || trimmed.contains("}") ||
                lower.startsWith("if") || lower.equals("else")) {
            Logger.warn("[Gear Swapper Looper] Unrecognized syntax (ignoring): " + trimmed);
            return;
        }

        // Try to equip as item
        equipItem(trimmed);
    }

    /**
     * Schedule a DropAll operation across multiple ticks.
     * Drops 2-6 matching items per tick until all are dropped.
     */
    private void scheduleDropAllTask(String pattern) {
        this.dropAllPattern = pattern;
        this.dropAllItemsPerTick = 2 + (int) (Math.random() * 5); // 2-6 items per tick
        Logger.norm("[Gear Swapper] DropAll scheduled: pattern='" + pattern + "', items/tick=" + dropAllItemsPerTick);
    }

    /**
     * Process one tick of DropAll operation.
     * 
     * @return true if still dropping, false if done
     */
    private boolean processDropAllTick() {
        if (dropAllPattern == null) {
            return false;
        }

        try {
            List<ItemEx> items = InventoryAPI.getItems().stream()
                    .filter(item -> item != null && item.getName() != null
                            && matchesWildcard(item.getName(), dropAllPattern))
                    .limit(dropAllItemsPerTick)
                    .collect(Collectors.toList());

            if (items.isEmpty()) {
                Logger.norm("[Gear Swapper] DropAll complete: no more items matching '" + dropAllPattern + "'");
                dropAllPattern = null;
                return false;
            }

            for (ItemEx item : items) {
                InventoryAPI.interact(item, "Drop");
                Logger.norm("[Gear Swapper] DropAll: dropped " + item.getName());
            }

            // Refresh items per tick for next round
            dropAllItemsPerTick = 2 + (int) (Math.random() * 5);
            return true; // Still dropping
        } catch (Exception e) {
            Logger.error("[Gear Swapper] DropAll error: " + e.getMessage());
            dropAllPattern = null;
            return false;
        }
    }

    /**
     * Interact with an NPC by name pattern and action.
     * Supports wildcard patterns like *Fishing spot*
     */
    private void interactWithNpc(String namePattern, String action) {
        try {
            NpcEx npc = NpcAPI.search()
                    .keepIf(n -> n != null && n.getName() != null && matchesWildcard(n.getName(), namePattern))
                    .first();

            if (npc != null) {
                if (action != null && !action.isEmpty()) {
                    NpcAPI.interact(npc, action);
                    Logger.norm("[Gear Swapper] NPC interaction: " + npc.getName() + " -> " + action);
                } else {
                    // Default to first action if none specified
                    NpcAPI.interact(npc, 0);
                    Logger.norm("[Gear Swapper] NPC interaction: " + npc.getName() + " (first action)");
                }
            } else {
                Logger.norm("[Gear Swapper] NPC not found: " + namePattern);
            }
        } catch (Exception e) {
            Logger.error("[Gear Swapper] NPC interaction error: " + e.getMessage());
        }
    }

    /**
     * Use an item with a specific menu action.
     * Example: useItemWithAction("Divine super strength potion(*)", "Drink")
     */
    private void useItemWithAction(String itemPattern, String action) {
        try {
            for (ItemEx item : InventoryAPI.getItems()) {
                if (item == null || item.getName() == null) {
                    continue;
                }

                // Use matchesWildcard for proper wildcard support like "Divine super strength
                // potion(*)"
                if (!matchesWildcard(item.getName(), itemPattern)) {
                    continue;
                }

                // Found matching item - use the specified action
                InventoryAPI.interact(item, action);
                Logger.norm("[Gear Swapper] Item action: " + item.getName() + " -> " + action);
                return;
            }
            Logger.norm("[Gear Swapper] Item not found for action: " + itemPattern);
        } catch (Exception e) {
            Logger.error("[Gear Swapper] Item action error: " + e.getMessage());
        }
    }

    /**
     * Interact with a game object by name pattern and action.
     * Example: interactWithObject("Cave", "Crawl-through")
     */
    private void interactWithObject(String namePattern, String action) {
        try {
            TileObjectEx obj = TileObjectAPI.search()
                    .keepIf(o -> o != null && o.getName() != null && matchesWildcard(o.getName(), namePattern))
                    .sortNearest()
                    .first();

            if (obj != null) {
                if (action != null && !action.isEmpty()) {
                    TileObjectAPI.interact(obj, action);
                    Logger.norm("[Gear Swapper] Object interaction: " + obj.getName() + " -> " + action);
                } else {
                    TileObjectAPI.interact(obj, 0);
                    Logger.norm("[Gear Swapper] Object interaction: " + obj.getName() + " (first action)");
                }
            } else {
                Logger.norm("[Gear Swapper] Object not found: " + namePattern);
            }
        } catch (Exception e) {
            Logger.error("[Gear Swapper] Object interaction error: " + e.getMessage());
        }
    }

    /**
     * Schedule a looper pause for the specified number of ticks.
     */
    private void scheduleLooperPause(int ticks) {
        this.looperPauseTicks = ticks;
        Logger.norm("[Gear Swapper] Looper paused for " + ticks + " ticks");
    }

    /**
     * Check if looper is currently paused.
     * Also decrements the pause counter if paused.
     * 
     * @return true if paused (should skip script execution), false if ready to run
     */
    private boolean isLooperPaused() {
        if (looperPauseTicks > 0) {
            looperPauseTicks--;
            return true;
        }
        return false;
    }

    /**
     * Match a string against a wildcard pattern.
     * Supports * as wildcard for any characters.
     * Example: "*Fishing spot*" matches "Rod Fishing spot" and "Fishing spot
     * (hard)"
     */
    private boolean matchesWildcard(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }

        String lowerText = text.toLowerCase();
        String lowerPattern = pattern.toLowerCase().trim();

        // If pattern doesn't contain wildcard, try contains match
        if (!lowerPattern.contains("*")) {
            return lowerText.contains(lowerPattern);
        }

        // Convert wildcard pattern to regex
        // Escape all regex special characters EXCEPT *, then convert * to .*
        String regex = lowerPattern
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("|", "\\|")
                .replace("*", ".*");

        try {
            // Use partial match (contains) rather than full string match
            return java.util.regex.Pattern.compile(regex).matcher(lowerText).find();
        } catch (Exception e) {
            // Fallback to simple contains without wildcards
            return lowerText.contains(lowerPattern.replace("*", ""));
        }
    }
}
