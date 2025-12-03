package com.tonic.plugins.lmsnavigator;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.WorldPointUtil;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.TClient;
import net.runelite.api.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.coords.WorldPoint;

import com.google.inject.Injector;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tonic.plugins.lmsnavigator.FightLogic.FreezeManager;
import com.tonic.plugins.lmsnavigator.FightLogic.AttackTimers;
import com.tonic.plugins.lmsnavigator.FightLogic.FightStateManager;
import com.tonic.plugins.lmsnavigator.FightLogic.BothUnfrozen;
import com.tonic.plugins.lmsnavigator.FightLogic.TargetFrozenWeUnfrozen;
import com.tonic.plugins.lmsnavigator.FightLogic.WeUnfrozenTargetFrozen;
import com.tonic.plugins.lmsnavigator.FightLogic.BothFrozenMelee;
import com.tonic.plugins.lmsnavigator.FightLogic.BothFrozen;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import com.tonic.plugins.lmsnavigator.FightLogic.TaskQueue;
import com.tonic.plugins.lmsnavigator.FightLogic.AiPrayerManager;

@PluginDescriptor(
    name = "LMS Navigator",
    description = "Navigate anywhere in LMS using template-based pathfinding",
    tags = {"lms", "last man standing", "navigation", "pathfinding", "walker"}
)
public class LMSNavigatorPlugin extends Plugin
{
    private static final String VERSION = "2.68";
    private static final BufferedImage PLUGIN_ICON = createDefaultIcon();
    
    // Static reference for tasks to access client
    private static LMSNavigatorPlugin instance;
    
    public static Client getClient()
    {
        return instance != null ? instance.client : null;
    }
    
    public static LMSNavigatorPlugin getPlugin()
    {
        return instance;
    }
    
    public static String getVersion()
    {
        return VERSION;
    }
    
    public boolean isCurrentlyNavigating()
    {
        return isNavigating.get();
    }
    
    @Inject
    private Client client;
    
    @Inject
    private ClientToolbar clientToolbar;
    
    @Inject
    private LMSOverlay overlay;
    
    @Inject
    private LMSOverlayFight fightOverlay;
    
    private SafeZoneOverlay safeZoneOverlay;
    
    @Inject
    private OverlayManager overlayManager;
    
    @Inject
    private Injector injector;
    
    private NavigationButton navigationButton;
    private LMSNavigatorPanel panel;
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isNavigating = new AtomicBoolean(false);
    private Boolean lastInstanceState = null;
    
    @Override
    protected void startUp() throws Exception
    {
        instance = this;
        Logger.norm("[LMS Navigator] Plugin started v" + VERSION);
        
        try {
            // Create and register panel
            panel = injector.getInstance(LMSNavigatorPanel.class);
            panel.setPlugin(this);
                        
            navigationButton = NavigationButton.builder()
                .tooltip("LMS Navigator")
                .icon(PLUGIN_ICON)
                .priority(5)
                .panel(panel)
                .build();
            
            clientToolbar.addNavigation(navigationButton);
            
            // Register overlay
            if (overlay != null)
            {
                overlayManager.add(overlay);
                Logger.norm("[LMS Navigator] Overlay registered");
                
                // Set overlay manager reference for GetMode to refresh overlay
                GetMode.setOverlayManager(overlayManager);
                
                // Set client reference for TargetManagement
                TargetManagement.setClient(client);

                // Set plugin reference for SafeZoneManagement (for auto-navigation)
                SafeZoneManagement.setPlugin(this);

                // Set client reference for FreezeManager (freeze timers)
                FreezeManager.setClient(client);

                // Set client references for fight logic helpers
                AttackTimers.setClient(client);
                FightStateManager.setClient(client);
                BothUnfrozen.setClient(client);
                TargetFrozenWeUnfrozen.setClient(client);
                WeUnfrozenTargetFrozen.setClient(client);
                BothFrozenMelee.setClient(client);
                BothFrozen.setClient(client);
                AiPrayerManager.setClient(client);
            }

            // Register fight overlay separately so it can be toggled independently in the overlay manager
            if (fightOverlay != null)
            {
                overlayManager.add(fightOverlay);
                Logger.norm("[LMS Navigator] Fight overlay registered");
            }

            // Register safe zone overlay
            safeZoneOverlay = new SafeZoneOverlay(client);
            if (safeZoneOverlay != null)
            {
                overlayManager.add(safeZoneOverlay);
                Logger.norm("[LMS Navigator] Safe zone overlay registered");
            }
            Logger.norm("[LMS Navigator] Type ::lms to open panel if toolbar button not visible");
            
        } catch (Exception e) {
            Logger.error("[LMS Navigator] Error creating panel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    protected void shutDown() throws Exception
    {
        instance = null;
        // Cancel navigation and clear state
        cancelNavigation();
        LmsState.setTask(LmsState.LmsTask.IDLE);
        LmsState.setTarget(null);
        LmsState.cleanupExpiredIgnores();
        
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
        }
        
        if (overlay != null)
        {
            overlayManager.remove(overlay);
        }
        
        if (fightOverlay != null)
        {
            overlayManager.remove(fightOverlay);
        }

        if (safeZoneOverlay != null)
        {
            overlayManager.remove(safeZoneOverlay);
        }
        
        // Shutdown executor to prevent resource leaks
        if (executor != null && !executor.isShutdown())
        {
            executor.shutdownNow();
            Logger.norm("[LMS Navigator] Executor shutdown");
        }
        executor = null;
        
        Logger.norm("[LMS Navigator] Plugin stopped");
    }

    /**
     * Lazily obtain the executor used for navigation, recreating it if it
     * was previously shut down (e.g. after plugin disable/enable).
     */
    private synchronized ExecutorService getExecutor()
    {
        if (executor == null || executor.isShutdown() || executor.isTerminated())
        {
            executor = Executors.newSingleThreadExecutor();
            Logger.norm("[LMS Navigator] Navigation executor (re)created");
        }
        return executor;
    }
    
    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        TargetManagement.onInteractingChanged(event);
    }
    
    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Update managers
        TargetManagement.onGameTick(event);
        FreezeManager.onGameTick(event);
        AttackTimers.onGameTick(event);
        AiPrayerManager.onGameTick();

        // Clean up expired ignores each tick
        TargetManagement.cleanupExpiredIgnores();

        // If we are not in the LMS instance, cancel any ongoing navigation
        if (isNavigating.get() && !isInInstance())
        {
            Logger.norm("[LMS Navigator] Not in LMS instance - cancelling navigation");
            cancelNavigation();
        }

        // If target is too far, clear it
        int targetDistance = FreezeManager.getTargetDistance();
        if (TargetManagement.hasTarget() && targetDistance >= 18)
        {
            Logger.norm("[LMS Navigator] Clearing combat target due to distance " + targetDistance + " >= 18 tiles");
            TargetManagement.clearTarget();
            LmsState.setTarget(null);
        }

        // Sync LmsState target with TargetManagement
        if (TargetManagement.hasTarget())
        {
            Player target = null;
            String targetName = TargetManagement.getCurrentTargetName();
            target = findPlayerByName(targetName);

            if (target != null)
            {
                if (target != LmsState.getCurrentTarget())
                {
                    Logger.norm("[LMS Navigator] New combat target: " + target.getName());
                }
                LmsState.setTarget(target);

                if (LmsState.getCurrentTask() != LmsState.LmsTask.ENGAGE_TARGET)
                {
                    TaskQueue.preempt(LmsState.LmsTask.ENGAGE_TARGET);
                }
            }
            else
            {
                // Could not resolve player yet; keep target null so roam/go safe zone continue
                LmsState.setTarget(null);
            }
        }
        else
        {
            LmsState.setTarget(null);
        }

        // Drive the task queue
        TaskQueue.tick();

        // Auto-start INIT_PHASE when we enter an LMS instance and no task is set
        if (isInInstance() && LmsState.getCurrentTask() == LmsState.LmsTask.IDLE)
        {
            Logger.norm("[LMS Navigator] In instance with no task; starting INIT_PHASE.");
            startInitPhase();
        }
    }
    
    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        ChatMessageType type = event.getType();

        // Always allow SafeZoneManagement to inspect broadcast/game messages
        try
        {
            SafeZoneManagement.processChatMessage(event);
        }
        catch (Exception e)
        {
            Logger.error("[LMS Navigator] Error in SafeZoneManagement.processChatMessage: " + e.getMessage());
            e.printStackTrace();
        }

        handleInitPhaseChat(event);

        String messageRaw = event.getMessage();
        if (messageRaw == null)
        {
            return;
        }
        String message = messageRaw.toLowerCase().trim();

        // Handle SPAM messages like "I'm already under attack."
        if (type.equals(ChatMessageType.SPAM))
        {
            if (message.contains("i'm already under attack"))
            {
                Logger.norm("[LMS Navigator] Received 'I'm already under attack.' message; clearing current target and updating to attacker.");
                TargetManagement.clearTarget();
                TargetManagement.updateTargetingEx();
            }
            return; // Skip further processing for SPAM messages
        }

        if (!type.equals(ChatMessageType.GAMEMESSAGE))
        {
            return;
        }

        String originalMessage = messageRaw;

        // Check for LMS panel command
        if (message.equals("::lms"))
        {
            Logger.norm("[LMS Navigator] Opening panel via chat command");
            // Force panel to open
            if (navigationButton != null && panel != null)
            {
                panel.revalidate();
                panel.repaint();
                Logger.norm("[LMS Navigator] Panel refreshed - check toolbar");
            }
            else
            {
                Logger.warn("[LMS Navigator] Panel or button not available");
            }
        }

        // Handle “already fighting” messages
        if (message.contains("already fighting"))
        {
            String plainOriginal = net.runelite.client.util.Text.removeTags(originalMessage).trim();

            String targetName = extractPlayerNameFromAlreadyFighting(plainOriginal);
            Player target = null;

            if (targetName != null && !targetName.isEmpty())
            {
                target = findPlayerByName(targetName);
            }

            // Fallback: use current TargetManagement target name if we couldn't parse one
            if (target == null && TargetManagement.hasTarget())
            {
                String currentTargetName = TargetManagement.getCurrentTargetName();
                if (currentTargetName != null && !currentTargetName.isEmpty())
                {
                    target = findPlayerByName(currentTargetName);
                }
            }

            // If we resolved a target, ignore them for 40 seconds
            if (target != null)
            {
                TargetManagement.ignoreTargetForSeconds(target, 40);
                Logger.norm("[LMS Navigator] Ignoring " + target.getName() + " for 'already fighting' message");
            }

            // Clear current target state and enter WAIT_IGNORE if we had any target at all
            boolean hadTarget = LmsState.getCurrentTarget() != null || TargetManagement.hasTarget();
            TargetManagement.clearTarget();
            LmsState.setTarget(null);

            if (hadTarget)
            {
                TaskQueue.preempt(LmsState.LmsTask.WAIT_IGNORE);
            }

            Logger.norm("[LMS Navigator] Cleared current target due to 'already fighting' message.");
        }
        
        // Process LMS game mode detection (use original message for case-sensitive matching)
        Logger.norm("[LMS Navigator] Calling GetMode.processChatMessage for: " + originalMessage);
        try {
            GetMode.processChatMessage(event);
            Logger.norm("[LMS Navigator] GetMode.processChatMessage completed successfully");
        } catch (Exception e) {
            Logger.error("[LMS Navigator] Error in GetMode.processChatMessage: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Debug: Log all game messages to see what we're receiving
        Logger.norm("[LMS Navigator] Game message: " + originalMessage);
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event)
    {
        FreezeManager.onGraphicChanged(event);
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        AttackTimers.onAnimationChanged(event);
    }
    
    /**
     * Check if currently in an instanced region (like LMS)
     */
    public boolean isInInstance()
    {
        try {
            if (client == null) {
                Logger.warn("[LMS Navigator] Client is null, assuming not in instance");
                return false;
            }
            
            return Static.invoke(() -> {
                WorldView worldView = client.getTopLevelWorldView();
                if (worldView == null) {
                    Logger.warn("[LMS Navigator] WorldView is null, assuming not in instance");
                    return false;
                }
                boolean isInstance = worldView.isInstance();
                if (lastInstanceState == null || lastInstanceState != isInstance)
                {
                    Logger.norm("[LMS Navigator] Instance check result: " + isInstance);
                    lastInstanceState = isInstance;
                }
                return isInstance;
            });
        } catch (Exception e) {
            Logger.error("[LMS Navigator] Error checking instance: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get player's current instance position
     */
    public WorldPoint getPlayerInstanceLocation()
    {
        return Static.invoke(() -> {
            if (client.getLocalPlayer() != null)
            {
                return client.getLocalPlayer().getWorldLocation();
            }
            return null;
        });
    }
    
    /**
     * Get player's current template position (real world coordinates)
     */
    public WorldPoint getPlayerTemplateLocation()
    {
        WorldPoint instancePos = getPlayerInstanceLocation();
        if (instancePos == null) return null;
        
        if (isInInstance())
        {
            return WorldPointUtil.fromInstance(instancePos);
        }
        else
        {
            return instancePos;
        }
    }
    
    /**
     * Convert a template (real-world) LMS coordinate into the current instance's
     * coordinate system, using the player's current instance/template offset.
     */
    public WorldPoint templateToInstance(WorldPoint templatePoint)
    {
        if (templatePoint == null)
        {
            return null;
        }

        WorldPoint currentInstance = getPlayerInstanceLocation();
        WorldPoint currentTemplate = getPlayerTemplateLocation();

        if (currentInstance == null || currentTemplate == null)
        {
            Logger.warn("[LMS Navigator] Cannot get positions for template->instance conversion");
            return null;
        }

        int offsetX = currentInstance.getX() - currentTemplate.getX();
        int offsetY = currentInstance.getY() - currentTemplate.getY();
        int offsetZ = currentInstance.getPlane() - currentTemplate.getPlane();

        return new WorldPoint(
            templatePoint.getX() + offsetX,
            templatePoint.getY() + offsetY,
            templatePoint.getPlane() + offsetZ
        );
    }
    
    /**
     * Navigate to a template coordinate (real world location)
     */
    public boolean navigateToTemplate(WorldPoint target)
    {
        if (target == null)
        {
            return false;
        }

        if (LmsState.isSafeZoneBoxEnforced() && LmsState.hasReachedSafeZone())
        {
            target = LmsState.clampToSafeZoneBox(target);
        }

        final WorldPoint navTarget = target;

        if (isNavigating.get())
        {
            Logger.norm("[LMS Navigator] Already navigating - updating target to: " + target);
            // Just update the target and continue - don't stop existing navigation
            return true;
        }
        
        ExecutorService exec = getExecutor();
        try
        {
            exec.submit(() -> {
            try
            {
                isNavigating.set(true);
                Logger.norm("[LMS Navigator] Starting navigation to: " + navTarget);
                
                // Debug: Check if we're in an instance
                boolean inInstance = isInInstance();
                Logger.norm("[LMS Navigator] In instance: " + inInstance);
                
                if (inInstance)
                {
                    // In LMS instance: convert template to instance and walk directly
                    Logger.norm("[LMS Navigator] Using LMS instance navigation");
                    navigateInInstance(navTarget);
                }
                else
                {
                    // Outside instance: use global pathfinder
                    Logger.norm("[LMS Navigator] Using global pathfinder (not in instance)");
                    Walker.walkTo(navTarget);
                }
                
                // Only set navigation to false if it wasn't cancelled during the process
                if (isNavigating.get()) {
                    Logger.norm("[LMS Navigator] Navigation complete");
                    isNavigating.set(false);
                }
            }
            catch (Exception e)
            {
                Logger.error("[LMS Navigator] Navigation error: " + e.getMessage());
                e.printStackTrace();
                isNavigating.set(false);
            }
            });
        }
        catch (java.util.concurrent.RejectedExecutionException rex)
        {
            Logger.error("[LMS Navigator] Navigation task rejected: executor is shut down");
            isNavigating.set(false);
            return false;
        }
        
        return true;
    }
    
    /**
     * Navigate within an LMS instance using step-by-step approach
     */
    private void navigateInInstance(WorldPoint templateTarget)
    {
        // Get current instance position and convert the template target into
        // instance coordinates using the same offset logic used elsewhere.
        WorldPoint currentInstance = getPlayerInstanceLocation();
        if (currentInstance == null)
        {
            Logger.warn("[LMS Navigator] Cannot get current instance position for navigation");
            return;
        }

        WorldPoint instanceTarget = templateToInstance(templateTarget);
        if (instanceTarget == null)
        {
            Logger.warn("[LMS Navigator] Cannot convert template target to instance: " + templateTarget);
            return;
        }

        int offsetX = instanceTarget.getX() - templateTarget.getX();
        int offsetY = instanceTarget.getY() - templateTarget.getY();
        int offsetZ = instanceTarget.getPlane() - templateTarget.getPlane();

        Logger.norm("[LMS Navigator] Template: " + templateTarget + " -> Instance: " + instanceTarget);
        Logger.norm("[LMS Navigator] Offset: X:" + offsetX + " Y:" + offsetY + " Z:" + offsetZ);

        // Calculate distance to target
        double distance = currentInstance.distanceTo(instanceTarget);
        Logger.norm("[LMS Navigator] Distance to target: " + distance + " tiles");
        
        // For distant targets, walk in steps
        if (distance > 20) {
            Logger.norm("[LMS Navigator] Walking to distant target in steps");
            walkInSteps(currentInstance, instanceTarget);
        } else {
            // For close targets, use direct walking with arrival checking
            Logger.norm("[LMS Navigator] Using direct walking (close target)");
            MovementAPI.walkToWorldPoint(instanceTarget);
            
            // Wait until we reach the target
            if (waitForArrival(instanceTarget, 5000)) {
                Logger.norm("[LMS Navigator] Successfully arrived at close target: " + instanceTarget);
            } else {
                Logger.warn("[LMS Navigator] Timeout waiting for close target arrival at: " + instanceTarget);
            }
        }
    }
    
    /**
     * Walk to target using 35-tile segments
     */
    private void walkInSteps(WorldPoint start, WorldPoint end)
    {
        Logger.norm("[LMS Navigator] Starting 35-tile segment navigation to: " + end);
        
        WorldPoint currentPos = start;
        
        while (isNavigating.get()) {
            // Calculate distance to final target
            double distanceToTarget = currentPos.distanceTo(end);
            Logger.norm("[LMS Navigator] Distance to final target: " + distanceToTarget + " tiles");
            
            // Check if we've arrived
            if (distanceToTarget <= 2) {
                Logger.norm("[LMS Navigator] Successfully arrived at target: " + end);
                break;
            }
            
            // Calculate next segment (max 35 tiles)
            WorldPoint nextSegment = calculateNextSegment(currentPos, end);
            Logger.norm("[LMS Navigator] Next 35-tile segment: " + nextSegment);
            
            // Walk to segment and wait for arrival
            MovementAPI.walkToWorldPoint(nextSegment);
            
            // Wait until we reach the segment
            if (!waitForArrival(nextSegment, 5000)) {
                Logger.warn("[LMS Navigator] Timeout waiting for segment arrival, recalculating from current position");
                // Don't break, just recalculate from current position
            }
            
            // Update current position for next iteration
            WorldPoint newPos = getPlayerInstanceLocation();
            if (newPos != null) {
                currentPos = newPos;
                Logger.norm("[LMS Navigator] Updated current position: " + currentPos);
            }
        }
        
        Logger.norm("[LMS Navigator] 35-tile segment navigation ended");
    }
    
    /**
     * Calculate the next 35-tile segment towards the target
     */
    private WorldPoint calculateNextSegment(WorldPoint current, WorldPoint target)
    {
        final int SEGMENT_SIZE = 35; // Conservative segment size
        
        int dx = target.getX() - current.getX();
        int dy = target.getY() - current.getY();
        int dz = target.getPlane() - current.getPlane();
        
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance <= SEGMENT_SIZE) {
            // Close enough to go directly to target
            return target;
        }
        
        // Calculate segment in the direction of target
        double ratio = SEGMENT_SIZE / distance;
        int segmentX = current.getX() + (int) (dx * ratio);
        int segmentY = current.getY() + (int) (dy * ratio);
        int segmentZ = current.getPlane() + dz;
        
        return new WorldPoint(segmentX, segmentY, segmentZ);
    }
    
    /**
     * Wait until the player arrives at the target position
     */
    private boolean waitForArrival(WorldPoint target, long timeoutMs)
    {
        long startTime = System.currentTimeMillis();
        final int ARRIVAL_THRESHOLD = 2; // Consider arrived within 2 tiles
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!isNavigating.get()) {
                Logger.warn("[LMS Navigator] Navigation cancelled while waiting for arrival");
                return false;
            }
            
            WorldPoint currentPos = getPlayerInstanceLocation();
            if (currentPos != null) {
                double distance = currentPos.distanceTo(target);
                Logger.norm("[LMS Navigator] Distance to target: " + distance + " tiles");
                
                if (distance <= ARRIVAL_THRESHOLD) {
                    Logger.norm("[LMS Navigator] Arrived at target (within " + ARRIVAL_THRESHOLD + " tiles)");
                    return true;
                }
            }
            
            try {
                Thread.sleep(500); // Check every 500ms
            } catch (InterruptedException e) {
                Logger.warn("[LMS Navigator] Arrival check interrupted");
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Navigate to an instance coordinate (current game coords)
     */
    public boolean navigateToInstance(WorldPoint target)
    {
        if (!isInInstance())
        {
            Logger.warn("[LMS Navigator] Not in instance - use template navigation");
            return false;
        }
        
        // Convert to template for pathfinding
        WorldPoint templateTarget = WorldPointUtil.fromInstance(target);
        return navigateToTemplate(templateTarget);
    }
    
    /**
     * Navigate in a direction by specified distance
     */
    public boolean navigateDirection(String direction, int distance)
    {
        WorldPoint current = getPlayerInstanceLocation();
        if (current == null)
        {
            Logger.warn("[LMS Navigator] Cannot get current position");
            return false;
        }
        
        // Calculate target
        WorldPoint target = calculateTarget(current, direction, distance);
        
        // Convert to template if in instance
        if (isInInstance())
        {
            target = WorldPointUtil.fromInstance(target);
        }
        
        return navigateToTemplate(target);
    }
    
    /**
     * Navigate by X/Y offset from current position
     */
    public boolean navigateByOffset(int dx, int dy)
    {
        WorldPoint current = getPlayerInstanceLocation();
        if (current == null)
        {
            Logger.warn("[LMS Navigator] Cannot get current position");
            return false;
        }
        
        WorldPoint target = new WorldPoint(
            current.getX() + dx,
            current.getY() + dy,
            current.getPlane()
        );
        
        // Convert to template if in instance
        if (isInInstance())
        {
            target = WorldPointUtil.fromInstance(target);
        }
        
        return navigateToTemplate(target);
    }
    
    /**
     * Cancel current navigation
     */
    public void cancelNavigation()
    {
        // Stop LMS Navigator's own navigation loop
        isNavigating.set(false);

        // Also cancel any global Walker/BiDirBFS pathfinding so we don't keep
        // pathing outside LMS or after combat starts.
        Walker.cancel();

        Logger.norm("[LMS Navigator] Navigation cancelled");
    }
    
    /**
     * Check if currently navigating
     */
    public boolean isNavigating()
    {
        return isNavigating.get();
    }
    
    /**
     * Debug: Print current position info
     */
    public void debugPosition()
    {
        WorldPoint instancePos = getPlayerInstanceLocation();
        WorldPoint templatePos = getPlayerTemplateLocation();
        
        Logger.norm("[LMS Navigator] === Position Debug ===");
        Logger.norm("[LMS Navigator] In Instance: " + isInInstance());
        Logger.norm("[LMS Navigator] Instance Position: " + instancePos);
        Logger.norm("[LMS Navigator] Template Position: " + templatePos);
        Logger.norm("[LMS Navigator] Is Navigating: " + isNavigating.get());
    }
    
    private void maybeRoamWhenNoTarget()
    {
        // Roaming is now handled by TaskQueue (RoamTask). Do nothing here.
    }
    
    /**
     * Calculate target position based on direction and distance
     */
    private WorldPoint calculateTarget(WorldPoint start, String direction, int distance)
    {
        switch (direction.toLowerCase())
        {
            case "north":
            case "n":
                return new WorldPoint(start.getX(), start.getY() + distance, start.getPlane());
            case "south":
            case "s":
                return new WorldPoint(start.getX(), start.getY() - distance, start.getPlane());
            case "east":
            case "e":
                return new WorldPoint(start.getX() + distance, start.getY(), start.getPlane());
            case "west":
            case "w":
                return new WorldPoint(start.getX() - distance, start.getY(), start.getPlane());
            case "northeast":
            case "ne":
                return new WorldPoint(start.getX() + distance, start.getY() + distance, start.getPlane());
            case "northwest":
            case "nw":
                return new WorldPoint(start.getX() - distance, start.getY() + distance, start.getPlane());
            case "southeast":
            case "se":
                return new WorldPoint(start.getX() + distance, start.getY() - distance, start.getPlane());
            case "southwest":
            case "sw":
                return new WorldPoint(start.getX() - distance, start.getY() - distance, start.getPlane());
            default:
                return start;
        }
    }
    
    /**
     * Create a default icon for the plugin (like GearSwapper)
     */
    private static BufferedImage createDefaultIcon()
    {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Draw a simple compass/north arrow icon
        g.setColor(new Color(100, 150, 200));  // Blue background
        g.fillRect(2, 2, 12, 12);
        
        g.setColor(new Color(255, 255, 255));  // White arrow
        // Arrow pointing north
        int[] xPoints = {8, 11, 8, 5};
        int[] yPoints = {4, 10, 10, 10};
        g.fillPolygon(xPoints, yPoints, 4);
        
        g.setColor(new Color(50, 100, 150));  // Dark blue border
        g.drawRect(2, 2, 12, 12);
        
        g.dispose();
        return img;
    }
    
    private void handleInitPhaseChat(ChatMessage event)
    {
        String message = event.getMessage();
        if (message == null)
        {
            return;
        }

        // Strip any tags like <col=...> before checking the visible text.
        String plain = net.runelite.client.util.Text.removeTags(message).trim();
        if (!plain.startsWith("2"))
        {
            return;
        }

        Logger.norm("[LMS Navigator] Saw '2...' chat, attempting auto-target. type=" + event.getType() + " name=" + event.getName() + " text='" + plain + "'");
        autoTargetNearestPlayerIfNone();
    }

    private void autoTargetNearestPlayerIfNone()
    {
        // Only auto-target if we are in an LMS instance
        if (!isInInstance())
        {
            return;
        }

        if (client == null)
        {
            return;
        }

        // Clear any stale target before selecting a new one for the round start
        TargetManagement.clearTarget();
        LmsState.setTarget(null);

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null)
        {
            return;
        }

        Player best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Player p : client.getPlayers())
        {
            if (p == null || p == local)
            {
                continue;
            }

            WorldPoint pos = p.getWorldLocation();
            if (pos == null || pos.getPlane() != localPos.getPlane())
            {
                continue;
            }

            int dx = Math.abs(localPos.getX() - pos.getX());
            int dy = Math.abs(localPos.getY() - pos.getY());
            int distance = Math.max(dx, dy);

            if (distance < bestDistance)
            {
                bestDistance = distance;
                best = p;
            }
        }

        if (best == null)
        {
            return;
        }

        // Sync target into both TargetManagement and LmsState, then engage
        TargetManagement.setTargetPlayer(best);
        LmsState.setTarget(best);
        TaskQueue.preempt(LmsState.LmsTask.ENGAGE_TARGET);

        TClient tClient = Static.getClient();
        if (tClient == null)
        {
            return;
        }

        int targetId = best.getId();
        Static.invoke(() -> tClient.getPacketWriter().playerActionPacket(1, targetId, false));

        // After issuing the attack packet, TaskQueue/EngageTargetTask will drive combat.
    }

    public static void engagePlayerFromRoam(Player target)
    {
        LMSNavigatorPlugin plugin = instance;
        if (plugin == null || target == null)
        {
            return;
        }

        if (!plugin.isInInstance())
        {
            return;
        }

        if (plugin.client == null)
        {
            return;
        }

        Player local = plugin.client.getLocalPlayer();
        if (local == null || local == target)
        {
            return;
        }

        // Hint TargetManagement/LmsState about the desired target;
        // onGameTick will resolve it to a live Player and switch to
        // ENGAGE_TARGET on the next tick.
        TargetManagement.setTargetPlayer(target);
        LmsState.setTarget(target);

        // Fire the actual attack packet using the VitaLite client API.
        TClient tClient = Static.getClient();
        if (tClient == null)
        {
            return;
        }

        int targetId = target.getId();
        Static.invoke(() -> tClient.getPacketWriter().playerActionPacket(1, targetId, false));
    }

    // === Task Queue helpers ===

    private static String extractPlayerNameFromAlreadyFighting(String message)
    {
        // Example: "You are already fighting PlayerName123."
        int start = message.indexOf("fighting");
        if (start == -1) return null;
        start += "fighting".length();
        // Skip whitespace
        while (start < message.length() && Character.isWhitespace(message.charAt(start)))
        {
            start++;
        }
        // Trim trailing punctuation/whitespace
        int end = message.length();
        while (end > start && (Character.isWhitespace(message.charAt(end - 1)) || message.charAt(end - 1) == '.' || message.charAt(end - 1) == '!'))
        {
            end--;
        }
        return message.substring(start, end);
    }

    private Player findPlayerByName(String name)
    {
        if (client == null || name == null || name.isEmpty())
        {
            return null;
        }
        for (Player p : client.getPlayers())
        {
            if (p != null && name.equalsIgnoreCase(p.getName()))
            {
                return p;
            }
        }
        return null;
    }

    /**
     * Initialize the task queue to INIT_PHASE when a new LMS game starts.
     */
    public void startInitPhase()
    {
        Logger.norm("[LMS Navigator] Starting init phase; queueing INIT_PHASE task.");
        LmsState.setTask(LmsState.LmsTask.INIT_PHASE);
    }
}
