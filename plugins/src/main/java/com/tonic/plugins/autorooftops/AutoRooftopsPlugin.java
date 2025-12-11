package com.tonic.plugins.autorooftops;

import com.google.inject.Provides;
import com.tonic.api.entities.*;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.VitaPlugin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

import javax.inject.Inject;
import java.util.*;

@Slf4j
@PluginDescriptor(
    name = "Auto Rooftops v2.2",
    description = "Advanced rooftop agility automation with mark pickup and stamina management",
    tags = {"agility", "automation", "rooftop"}
)
public class AutoRooftopsPlugin extends VitaPlugin
{
    @Inject
    private Client client;

    @Inject
    private AutoRooftopsConfig config;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoRooftopsOverlay overlay;

    private List<CourseStage> courseStages;
    private int currentStageIndex = 0;
    private boolean running = false;
    private long lastStaminaDrink = 0;
    private int marksCollected = 0;
    private int lapsCompleted = 0;

    public boolean isRunning()
    {
        return running;
    }

    public int getMarksCollected()
    {
        return marksCollected;
    }

    public int getLapsCompleted()
    {
        return lapsCompleted;
    }

    @Override
    protected void startUp()
    {
        log.info("Auto Rooftops v2.2 started");
        initializeCourseStages();
        running = true;
        if (overlayManager != null && overlay != null)
        {
            overlayManager.add(overlay);
        }
    }

    @Override
    protected void shutDown()
    {
        log.info("Auto Rooftops stopped - Completed {} laps, collected {} marks", lapsCompleted, marksCollected);
        running = false;
        if (overlayManager != null && overlay != null)
        {
            overlayManager.remove(overlay);
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (config.stopAtLevel() > 0 && client.getRealSkillLevel(net.runelite.api.Skill.AGILITY) >= config.stopAtLevel())
        {
            log.info("Reached target agility level {}, stopping plugin", client.getRealSkillLevel(net.runelite.api.Skill.AGILITY));
            pluginManager.setPluginEnabled(this, false);
        }
    }

    @Override
    public void loop()
    {
        if (!config.enabled() || !running)
            return;

        try
        {
            PlayerEx local = PlayerEx.getLocal();
            if (local == null || !local.isIdle())
                return;

            if (currentStageIndex >= courseStages.size())
            {
                lapsCompleted++;
                currentStageIndex = 0;
                if (config.logSteps()) log.info("Lap {} completed", lapsCompleted);
            }

            CourseStage stage = courseStages.get(currentStageIndex);

            // Auto-walk to this stage location if we're not close enough
            WorldPoint playerLoc = local.getWorldPoint();
            if (playerLoc.distanceTo(stage.getLocation()) > 2)
            {
                Walker.walkTo(stage.getLocation());
                return;
            }

            // Anti-ban random delay
            if (config.antiBan() && new Random().nextInt(100) < 5)
            {
                Delays.tick(new Random().nextInt(3) + 1);
            }

            // Manage stamina
            if (config.useStaminaPotions() && client.getEnergy() < config.staminaThreshold() &&
                System.currentTimeMillis() - lastStaminaDrink > 3000)
            {
                // Click first "Drink" action on any stamina potion in inventory
                InventoryAPI.interact("Stamina potion", "Drink");
                lastStaminaDrink = System.currentTimeMillis();
                if (config.logSteps()) log.info("Drank stamina potion");
                Delays.tick(3);
            }

            // Pickup marks
            if (config.pickupMarks())
            {
                TileItemEx mark = TileItemAPI.search().withId(ItemID.MARK_OF_GRACE).nearest();
                if (mark != null)
                {
                    // Use first ground-item action (usually "Take")
                    TileItemAPI.interact(mark, 0);
                    marksCollected++;
                    if (config.logSteps()) log.info("Picked up Mark of Grace (total: {})", marksCollected);
                    Delays.tick(2);
                }
            }

            // Interact with obstacle
            TileObjectEx obstacle = TileObjectAPI.search()
                .atLocation(stage.getLocation())
                .withAction(stage.getAction())
                .first();

            // Fallback: if no object exactly at the recorded tile, grab nearest with this action
            if (obstacle == null)
            {
                obstacle = TileObjectAPI.search()
                    .withAction(stage.getAction())
                    .sortNearest()
                    .first();
            }

            if (obstacle != null)
            {
                TileObjectAPI.interact(obstacle, stage.getAction());
                if (config.logSteps()) log.info("Interacting with obstacle: {} at {}", stage.getAction(), obstacle.getWorldPoint());
                Delays.tick(stage.getWaitTicks());
                currentStageIndex++;
            }
            else if (config.logSteps())
            {
                log.warn("No obstacle found for stage {} action {}", currentStageIndex, stage.getAction());
            }

        }
        catch (Exception e)
        {
            log.error("Error in Auto Rooftops loop", e);
        }
    }

    private void initializeCourseStages()
    {
        courseStages = Arrays.asList(
            new CourseStage(new WorldPoint(2474, 3438, 0), "Walk-across", 8, false, 0, new WorldPoint(2474, 3438, 0)),
            new CourseStage(new WorldPoint(2473, 3429, 0), "Climb-over", 8, false, 0, new WorldPoint(2473, 3429, 0)),
            new CourseStage(new WorldPoint(2473, 3426, 0), "Climb-over", 8, false, 0, new WorldPoint(2473, 3426, 0)),
            new CourseStage(new WorldPoint(2485, 3426, 0), "Walk-across", 8, false, 0, new WorldPoint(2485, 3426, 0)),
            new CourseStage(new WorldPoint(2485, 3430, 0), "Climb-over", 8, false, 0, new WorldPoint(2485, 3430, 0)),
            new CourseStage(new WorldPoint(2484, 3437, 0), "Squeeze-through", 8, false, 0, new WorldPoint(2484, 3437, 0))
        );
    }

    @Provides
    AutoRooftopsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoRooftopsConfig.class);
    }
}
