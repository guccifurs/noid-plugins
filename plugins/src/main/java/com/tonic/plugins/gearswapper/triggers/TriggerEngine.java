package com.tonic.plugins.gearswapper.triggers;

import com.tonic.plugins.gearswapper.GearSwapperPlugin;
import com.tonic.plugins.gearswapper.GearSwapperConfig;
import net.runelite.api.Client;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.CompletableFuture;

/**
 * Core trigger engine that manages all triggers and processes game events
 */
@Singleton
public class TriggerEngine
{
    private static final Logger logger = LoggerFactory.getLogger(TriggerEngine.class);
    
    private final GearSwapperPlugin plugin;
    private final Client client;
    private final ConfigManager configManager;
    
    // Active triggers
    private final ConcurrentHashMap<String, Trigger> activeTriggers = new ConcurrentHashMap<>();
    private final List<TriggerHandler> handlers = new ArrayList<>();
    
    // Engine state
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final ReentrantReadWriteLock triggerLock = new ReentrantReadWriteLock();
    
    // Statistics
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalTriggersFired = new AtomicLong(0);
    private long engineStartTime = 0;

    @Inject
    public TriggerEngine(GearSwapperPlugin plugin, Client client, ConfigManager configManager)
    {
        this.plugin = plugin;
        this.client = client;
        this.configManager = configManager;
        
        initializeHandlers();
        loadTriggersFromConfig();
    }

    /**
     * Initialize all trigger handlers
     */
    private void initializeHandlers()
    {
        // Register animation trigger handler
        handlers.add(new AnimationTriggerHandler(this));
        
        // Register HP trigger handler
        handlers.add(new HpTriggerHandler(this));
        
        // Register XP trigger handler
        handlers.add(new XpTriggerHandler(this));
        
        logger.info("[Trigger Engine] Initialized {} trigger handlers", handlers.size());
    }

    /**
     * Start the trigger engine
     */
    public void start()
    {
        if (enabled.compareAndSet(false, true))
        {
            engineStartTime = System.currentTimeMillis();
            logger.info("[Trigger Engine] Started with {} active triggers", activeTriggers.size());
        }
    }

    /**
     * Stop the trigger engine
     */
    public void stop()
    {
        if (enabled.compareAndSet(true, false))
        {
            logger.info("[Trigger Engine] Stopped. Processed {} events, fired {} triggers", 
                       totalEventsProcessed, totalTriggersFired);
        }
    }

    /**
     * Check if engine is enabled
     */
    public boolean isEnabled()
    {
        return enabled.get();
    }

    /**
     * Add a new trigger with thread safety
     */
    public boolean addTrigger(Trigger trigger)
    {
        if (trigger == null || trigger.getId() == null)
        {
            logger.warn("[Trigger Engine] Cannot add trigger - null trigger or ID");
            return false;
        }
        
        triggerLock.writeLock().lock();
        try
        {
            // Check if trigger already exists
            if (activeTriggers.containsKey(trigger.getId()))
            {
                logger.warn("[Trigger Engine] Trigger with ID {} already exists", trigger.getId());
                return false;
            }
            
            activeTriggers.put(trigger.getId(), trigger);
            saveTriggerToConfig(trigger);
            
            logger.info("[Trigger Engine] Added trigger: {} ({})", trigger.getName(), trigger.getType());
            return true;
        }
        catch (Exception e)
        {
            logger.error("[Trigger Engine] Error adding trigger {}: {}", trigger.getName(), e.getMessage(), e);
            return false;
        }
        finally
        {
            triggerLock.writeLock().unlock();
        }
    }

    /**
     * Remove a trigger with thread safety
     */
    public boolean removeTrigger(String triggerId)
    {
        if (triggerId == null || triggerId.trim().isEmpty())
        {
            logger.warn("[Trigger Engine] Cannot remove trigger - null or empty ID");
            return false;
        }
        
        triggerLock.writeLock().lock();
        try
        {
            Trigger removed = activeTriggers.remove(triggerId);
            if (removed != null)
            {
                removeTriggerFromConfig(triggerId);
                logger.info("[Trigger Engine] Removed trigger: {}", removed.getName());
                return true;
            }
            else
            {
                logger.warn("[Trigger Engine] Trigger with ID {} not found", triggerId);
                return false;
            }
        }
        catch (Exception e)
        {
            logger.error("[Trigger Engine] Error removing trigger {}: {}", triggerId, e.getMessage(), e);
            return false;
        }
        finally
        {
            triggerLock.writeLock().unlock();
        }
    }

    /**
     * Get all active triggers with thread safety
     */
    public List<Trigger> getActiveTriggers()
    {
        triggerLock.readLock().lock();
        try
        {
            return new ArrayList<>(activeTriggers.values());
        }
        finally
        {
            triggerLock.readLock().unlock();
        }
    }

    /**
     * Get trigger by ID with thread safety
     */
    public Trigger getTrigger(String triggerId)
    {
        if (triggerId == null)
        {
            return null;
        }
        
        triggerLock.readLock().lock();
        try
        {
            return activeTriggers.get(triggerId);
        }
        finally
        {
            triggerLock.readLock().unlock();
        }
    }

    /**
     * Enable/disable a trigger
     */
    public boolean setTriggerEnabled(String triggerId, boolean enabled)
    {
        triggerLock.writeLock().lock();
        try
        {
            Trigger trigger = activeTriggers.get(triggerId);
            if (trigger != null)
            {
                trigger.setEnabled(enabled);
                saveTriggerToConfig(trigger);
                logger.info("[Trigger Engine] {} trigger: {}", enabled ? "Enabled" : "Disabled", trigger.getName());
                return true;
            }
            return false;
        }
        finally
        {
            triggerLock.writeLock().unlock();
        }
    }

    /**
     * Process a game event through all relevant triggers
     */
    public void processEvent(TriggerEvent event)
    {
        if (!enabled.get() || processing.get())
        {
            return;
        }

        try
        {
            processing.set(true);
            totalEventsProcessed.incrementAndGet();
            
            // Process event through all handlers
            for (TriggerHandler handler : handlers)
            {
                try
                {
                    // Fix: handleEvent needs both event and triggers
                    handler.handleEvent(event, getActiveTriggers());
                }
                catch (Exception e)
                {
                    logger.error("[Trigger Engine] Error in handler {}: {}", 
                               handler.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        }
        finally
        {
            processing.set(false);
        }
    }

    /**
     * Fire a trigger (execute its actions)
     */
    public void fireTrigger(Trigger trigger, TriggerEvent event)
    {
        if (!trigger.isEnabled())
        {
            return;
        }

        // Check cooldown
        long currentTime = System.currentTimeMillis();
        if (currentTime - trigger.getLastFired() < trigger.getCooldownMs())
        {
            return;
        }

        // Use read lock for thread-safe trigger access
        triggerLock.readLock().lock();
        try
        {
            logger.info("[Trigger Engine] Firing trigger: {} ({})", trigger.getName(), trigger.getType());
            
            // Execute all actions with proper error handling
            boolean actionFailed = false;
            java.util.List<TriggerAction> actions = trigger.getActions();
            logger.info("[Trigger Engine] Trigger '{}' has {} actions", trigger.getName(), actions.size());
            
            for (int i = 0; i < actions.size(); i++)
            {
                TriggerAction action = actions.get(i);
                try
                {
                    logger.info("[Trigger Engine] Executing action {}/{}: {} (enabled: {})", 
                               i + 1, actions.size(), action.getClass().getSimpleName(), action.isEnabled());
                    
                    if (action.isEnabled())
                    {
                        action.execute(plugin, client, event);
                        logger.info("[Trigger Engine] Action {} executed successfully", action.getClass().getSimpleName());
                    }
                    else
                    {
                        logger.info("[Trigger Engine] Skipping disabled action: {}", action.getClass().getSimpleName());
                    }
                }
                catch (Exception e)
                {
                    actionFailed = true;
                    logger.error("[Trigger Engine] Error executing action {} for trigger {}: {}", 
                               action.getClass().getSimpleName(), trigger.getName(), e.getMessage(), e);
                    // Continue with other actions, don't let one failure stop everything
                }
            }
            
            // Update trigger statistics only if at least one action executed
            if (!actionFailed)
            {
                trigger.setLastFired(currentTime);
                trigger.incrementFireCount();
                totalTriggersFired.incrementAndGet();
                
                // Save updated statistics asynchronously to avoid blocking
                saveTriggerToConfigAsync(trigger);
            }
            
        }
        catch (Exception e)
        {
            logger.error("[Trigger Engine] Critical error firing trigger {}: {}", trigger.getName(), e.getMessage(), e);
        }
        finally
        {
            triggerLock.readLock().unlock();
        }
    }

    /**
     * Get engine statistics
     */
    public TriggerEngineStats getStats()
    {
        long uptime = enabled.get() ? System.currentTimeMillis() - engineStartTime : 0;
        
        return new TriggerEngineStats(
            enabled.get(),
            activeTriggers.size(),
            totalEventsProcessed.get(),
            totalTriggersFired.get(),
            uptime,
            processing.get()
        );
    }

    /**
     * Load triggers from configuration
     */
    private void loadTriggersFromConfig()
    {
        try
        {
            String triggersJson = configManager.getConfiguration(GearSwapperConfig.GROUP, "triggers");
            if (triggersJson != null && !triggersJson.trim().isEmpty())
            {
                List<Trigger> loadedTriggers = TriggerSerializer.deserializeTriggers(triggersJson);
                
                // Filter out any test triggers that might have been accidentally saved
                List<Trigger> validTriggers = new ArrayList<>();
                for (Trigger trigger : loadedTriggers)
                {
                    // Don't load test triggers from configuration
                    if (!trigger.getConfig().isTestMode())
                    {
                        activeTriggers.put(trigger.getId(), trigger);
                        validTriggers.add(trigger);
                    }
                    else
                    {
                        logger.info("[Trigger Engine] Skipping test trigger during load: {}", trigger.getId());
                    }
                }
                
                logger.info("[Trigger Engine] Loaded {} triggers from configuration (skipped test triggers)", validTriggers.size());
                
                // If we skipped any test triggers, resave the config to clean it up
                if (validTriggers.size() < loadedTriggers.size())
                {
                    saveTriggersToConfig();
                }
            }
        }
        catch (Exception e)
        {
            logger.error("[Trigger Engine] Error loading triggers from config: {}", e.getMessage(), e);
        }
    }

    /**
     * Save trigger to configuration
     */
    private void saveTriggerToConfig(Trigger trigger)
    {
        try
        {
            // Save all triggers except test triggers
            List<Trigger> allTriggers = new ArrayList<>();
            for (Trigger t : activeTriggers.values())
            {
                // Don't save test triggers to configuration
                if (!t.getConfig().isTestMode())
                {
                    allTriggers.add(t);
                }
            }
            
            String triggersJson = TriggerSerializer.serializeTriggers(allTriggers);
            
            if (triggersJson != null)
            {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "triggers", triggersJson);
                logger.debug("[Trigger Engine] Saved {} triggers (excluded test triggers)", allTriggers.size());
            }
        }
        catch (Exception e)
        {
            logger.error("[Trigger Engine] Error saving trigger to config: {}", e.getMessage(), e);
        }
    }

    /**
     * Save trigger to configuration asynchronously (non-blocking)
     */
    private void saveTriggerToConfigAsync(Trigger trigger)
    {
        // Run in background thread to avoid blocking trigger execution
        CompletableFuture.runAsync(() -> {
            try
            {
                triggerLock.readLock().lock();
                try
                {
                    List<Trigger> allTriggers = new ArrayList<>(activeTriggers.values());
                    String triggersJson = TriggerSerializer.serializeTriggers(allTriggers);
                    
                    if (triggersJson != null)
                    {
                        configManager.setConfiguration(GearSwapperConfig.GROUP, "triggers", triggersJson);
                        logger.debug("[Trigger Engine] Async save completed for trigger: {}", trigger.getName());
                    }
                }
                finally
                {
                    triggerLock.readLock().unlock();
                }
            }
            catch (Exception e)
            {
                logger.error("[Trigger Engine] Error in async save for trigger {}: {}", trigger.getName(), e.getMessage(), e);
            }
        }).exceptionally(throwable -> {
            logger.error("[Trigger Engine] Async save failed for trigger: {}", trigger.getName(), throwable);
            return null;
        });
    }

    /**
     * Save all triggers to configuration
     */
    public void saveTriggersToConfig()
    {
        try
        {
            // Save all triggers except test triggers
            List<Trigger> allTriggers = new ArrayList<>();
            for (Trigger t : activeTriggers.values())
            {
                // Don't save test triggers to configuration
                if (!t.getConfig().isTestMode())
                {
                    allTriggers.add(t);
                }
            }
            
            String triggersJson = TriggerSerializer.serializeTriggers(allTriggers);
            
            if (triggersJson != null)
            {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "triggers", triggersJson);
                logger.info("[Trigger Engine] Saved {} triggers to configuration (excluded test triggers)", allTriggers.size());
            }
        }
        catch (Exception e)
        {
            logger.error("[Trigger Engine] Error saving triggers to config: {}", e.getMessage(), e);
        }
    }

    /**
     * Remove trigger from configuration
     */
    private void removeTriggerFromConfig(String triggerId)
    {
        try
        {
            // Save all triggers (excluding the removed one)
            List<Trigger> remainingTriggers = new ArrayList<>(activeTriggers.values());
            String triggersJson = TriggerSerializer.serializeTriggers(remainingTriggers);
            
            if (triggersJson != null)
            {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "triggers", triggersJson);
            }
        }
        catch (Exception e)
        {
            logger.error("[Trigger Engine] Error removing trigger from config: {}", e.getMessage(), e);
        }
    }

    /**
     * Create sample triggers for testing
     */
    public void createSampleTriggers()
    {
        try
        {
            // Sample 1: Dragon Fire Breath Response
            Trigger dragonFireTrigger = new Trigger("dragon_fire_sample", "Dragon Fire Response", TriggerType.ANIMATION);
            dragonFireTrigger.getConfig().setAnimationId(3242); // Dragon fire breath animation
            dragonFireTrigger.getConfig().setCooldownMs(5000); // 5 second cooldown
            dragonFireTrigger.getConfig().setOnlyInCombat(true);
            
            GearSwapAction antiFireAction = new GearSwapAction("Anti-Fire Gear");
            dragonFireTrigger.addAction(antiFireAction);
            
            addTrigger(dragonFireTrigger);
            
            // Sample 2: Eating Animation Response
            Trigger eatingTrigger = new Trigger("eating_sample", "Eating Response", TriggerType.ANIMATION);
            eatingTrigger.getConfig().setAnimationId(829); // Eating animation
            eatingTrigger.getConfig().setCooldownMs(3000); // 3 second cooldown
            
            GearSwapAction defensiveAction = new GearSwapAction("Defensive Gear");
            eatingTrigger.addAction(defensiveAction);
            
            addTrigger(eatingTrigger);
            
            // Sample 3: Magic Attack Response
            Trigger magicTrigger = new Trigger("magic_attack_sample", "Magic Attack Response", TriggerType.ANIMATION);
            magicTrigger.getConfig().setAnimationId(1161); // Magic attack animation
            magicTrigger.getConfig().setCooldownMs(4000); // 4 second cooldown
            magicTrigger.getConfig().setOnlyInCombat(true);
            
            GearSwapAction magicResistAction = new GearSwapAction("Magic Resist Gear");
            magicTrigger.addAction(magicResistAction);
            
            addTrigger(magicTrigger);
            
            // Sample 4: Low HP Response (HP Trigger - Now Fixed)
            Trigger lowHpTrigger = new Trigger("low_hp_sample", "Low HP Response", TriggerType.HP);
            lowHpTrigger.getConfig().setHpThreshold(40); // Trigger when HP below 40%
            lowHpTrigger.getConfig().setHpThresholdType(TriggerConfig.HpThresholdType.BELOW);
            lowHpTrigger.getConfig().setHpTargetType(TriggerConfig.HpTargetType.PLAYER);
            lowHpTrigger.getConfig().setCooldownMs(3000); // 3 second cooldown
            lowHpTrigger.getConfig().setMinConsecutiveTicks(2); // Require 2 consecutive ticks
            
            GearSwapAction defensiveHpAction = new GearSwapAction("Defensive Gear");
            lowHpTrigger.addAction(defensiveHpAction);
            
            addTrigger(lowHpTrigger);
            
            // Sample 5: Target Low HP Response
            Trigger targetLowHpTrigger = new Trigger("target_low_hp_sample", "Target Low HP Response", TriggerType.HP);
            targetLowHpTrigger.getConfig().setHpThreshold(30); // Trigger when target HP below 30%
            targetLowHpTrigger.getConfig().setHpThresholdType(TriggerConfig.HpThresholdType.BELOW);
            targetLowHpTrigger.getConfig().setHpTargetType(TriggerConfig.HpTargetType.TARGET);
            targetLowHpTrigger.getConfig().setTargetFilterByValue("current");
            targetLowHpTrigger.getConfig().setCooldownMs(2000); // 2 second cooldown
            targetLowHpTrigger.getConfig().setOnlyInCombat(true);
            
            GearSwapAction finishingAction = new GearSwapAction("Finishing Gear");
            targetLowHpTrigger.addAction(finishingAction);
            
            addTrigger(targetLowHpTrigger);
            
            logger.info("[Trigger Engine] Created {} sample triggers for testing", 5);
        }
        catch (Exception e)
        {
            logger.error("[Trigger Engine] Error creating sample triggers: {}", e.getMessage(), e);
        }
    }

    /**
     * Get the game client
     */
    public Client getClient()
    {
        return client;
    }

    /**
     * Get the plugin
     */
    public GearSwapperPlugin getPlugin()
    {
        return plugin;
    }

    // Event listeners (will be expanded in future phases)
    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (event.getActor() != null)
        {
            TriggerEvent triggerEvent = new TriggerEvent(TriggerEventType.ANIMATION_CHANGED, event);
            processEvent(triggerEvent);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!enabled.get())
        {
            return;
        }

        // Process HP triggers via the event system
        TriggerEvent triggerEvent = new TriggerEvent(TriggerEventType.GAME_TICK, event);
        processEvent(triggerEvent);
    }
}
