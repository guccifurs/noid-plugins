package com.tonic.plugins.helperbox;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.Static;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import java.util.List;
import java.util.ArrayList;

@Slf4j
public class WitchsHouseTask
{
    private final Client client;
    private final HelperBoxConfig config;

    // Boy outside Witch's House (user-confirmed pathable tile)
    private static final WorldPoint BOY_TILE = new WorldPoint(2929, 3454, 0);
    private static final WorldPoint POT_TILE = new WorldPoint(2900, 3474, 0);
    private static final WorldPoint HOUSE_DOOR_TILE = new WorldPoint(2900, 3473, 0);
    private static final WorldArea HOUSE_AREA = new WorldArea(2901, 3466, 7, 11, 0);

    public static final int TOTAL_STEPS = 10; // placeholder, will adjust as we port full quest

    private enum Step
    {
        IDLE,
        TALK_TO_BOY,
        ENTER_GARDEN,
        ENTER_HOUSE,
        SEARCH_FOR_KEY,
        DESCEND_BASEMENT,
        FIGHT_FORMS,
        RETRIEVE_BALL,
        RETURN_TO_BOY,
        COMPLETE
    }

    private Step step = Step.IDLE;
    private boolean walkingToBoy = false;
    private WorldPoint lastBoyTarget = null;

    private void handleBoyDialogue()
    {
        int safety = 0;

        while (safety++ < 200)
        {
            // Always advance any "Click here to continue" prompts
            if (DialogueAPI.continueDialogue())
            {
                Delays.tick();
                continue;
            }

            if (!DialogueAPI.dialoguePresent())
            {
                // Dialogue fully closed
                break;
            }

            // Inspect current options if any
            java.util.List<String> options = DialogueAPI.getOptions();
            boolean clicked = false;
            for (String opt : options)
            {
                String lower = opt.toLowerCase();

                // Quest Helper path: "What's the matter?" -> "Ok, I'll see what I can do." -> "Yes"
                if (lower.contains("what's the matter"))
                {
                    DialogueAPI.selectOption(opt);
                    clicked = true;
                    break;
                }
                if (lower.contains("i'll see what i can do"))
                {
                    DialogueAPI.selectOption(opt);
                    clicked = true;
                    break;
                }
                // Final confirmation: any Yes-like option (covers "Yes" / "Yes." / "Yes, please" etc., even with color tags)
                if (lower.contains("yes") && !lower.contains("no"))
                {
                    DialogueAPI.selectOption(opt);
                    clicked = true;
                    break;
                }
            }

            if (clicked)
            {
                Delays.tick();
                continue;
            }

            // Fallback: let Quest Helper highlighting or museum quiz logic drive option selection
            if (!DialogueAPI.continueQuestHelper() && !DialogueAPI.continueMuseumQuiz())
            {
                Delays.tick();
            }
        }
    }

    private WorldPoint findReachableAround(WorldPoint desired)
    {
        if (desired == null)
        {
            return null;
        }

        // If direct path is fine, use it
        if (MovementAPI.canPathTo(desired))
        {
            return desired;
        }

        int baseX = desired.getX();
        int baseY = desired.getY();
        int plane = desired.getPlane();

        // Search a small ring of tiles around the desired point for a reachable alternative
        for (int radius = 1; radius <= 4; radius++)
        {
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dy = -radius; dy <= radius; dy++)
                {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius)
                    {
                        continue; // only outer ring
                    }
                    WorldPoint cand = new WorldPoint(baseX + dx, baseY + dy, plane);
                    if (MovementAPI.canPathTo(cand))
                    {
                        return cand;
                    }
                }
            }
        }

        // Fallback: return original even if blocked (will behave as before)
        return desired;
    }

    public WitchsHouseTask(Client client, HelperBoxConfig config)
    {
        this.client = client;
        this.config = config;
    }

    public void resetState()
    {
        step = Step.IDLE;
        walkingToBoy = false;
        lastBoyTarget = null;
    }

    public int getStepIndex()
    {
        switch (step)
        {
            case TALK_TO_BOY:     return 1;
            case ENTER_GARDEN:    return 2;
            case ENTER_HOUSE:     return 3;
            case SEARCH_FOR_KEY:  return 4;
            case DESCEND_BASEMENT:return 5;
            case FIGHT_FORMS:     return 6;
            case RETRIEVE_BALL:   return 7;
            case RETURN_TO_BOY:   return 8;
            case COMPLETE:        return 9;
            default:              return 0;
        }
    }

    public String getStepLabel()
    {
        switch (step)
        {
            case TALK_TO_BOY:      return "Talk to the Boy";
            case ENTER_GARDEN:     return "Enter the garden";
            case ENTER_HOUSE:      return "Enter the house";
            case SEARCH_FOR_KEY:   return "Search for key";
            case DESCEND_BASEMENT: return "Descend to basement";
            case FIGHT_FORMS:      return "Defeat experiment forms";
            case RETRIEVE_BALL:    return "Retrieve the ball";
            case RETURN_TO_BOY:    return "Return ball to Boy";
            case COMPLETE:         return "Quest complete";
            default:               return "Idle";
        }
    }

    public void tick()
    {
        PlayerEx local = PlayerEx.getLocal();
        if (local == null || !config.enabled())
        {
            return;
        }

        WorldPoint pos = local.getWorldPoint();
        if (pos == null)
        {
            return;
        }

        switch (step)
        {
            case IDLE:
                // Initial step: set up TALK_TO_BOY, but let pathfinder run on its own
                if (config.logSteps())
                {
                    log.info("[HelperBox] [WitchsHouse] Starting quest helper, targeting Boy at {}", BOY_TILE);
                }
                step = Step.TALK_TO_BOY;
                walkingToBoy = false;
                lastBoyTarget = null;
                break;

            case TALK_TO_BOY:
                // While in TALK_TO_BOY, path once toward the Boy and let Walker handle the walking
                WorldPoint target = BOY_TILE;
                try
                {
                    NpcEx boy = NpcAPI.search().withNameContains("Boy").shortestPath();
                    if (boy != null && boy.getWorldPoint() != null)
                    {
                        target = boy.getWorldPoint();
                    }
                }
                catch (Exception ignored)
                {
                }

                // Adjust target to the nearest reachable tile around the Boy, to avoid blocked tiles
                target = findReachableAround(target);

                if (pos.distanceTo(target) > 3)
                {
                    // Only start or restart pathing if we haven't already or the target changed
                    if (!walkingToBoy || lastBoyTarget == null || !lastBoyTarget.equals(target))
                    {
                        if (config.logSteps())
                        {
                            log.info("[HelperBox] [WitchsHouse] Starting Hybrid BFS pathfinder to {} (current={})", target, pos);
                        }
                        
                        // Use Hybrid BFS pathfinder
                        try {
                            // Get the Hybrid BFS pathfinder and find path
                            IPathfinder engine = Static.getVitaConfig().getPathfinderImpl().newInstance();
                            List<? extends IStep> path = engine.find(target);
                            
                            if (path != null && !path.isEmpty()) {
                                // Convert path to WorldPoints for visualization
                                List<WorldPoint> worldPoints = new ArrayList<>();
                                for (IStep step : path) {
                                    worldPoints.add(step.getPosition());
                                }
                                
                                // Set the path points for visualization
                                GameManager.setPathPoints(worldPoints);
                                log.info("[HelperBox] [WitchsHouse] Hybrid BFS path found with {} steps", path.size());
                                
                                // Start walking using the regular Walker (which will follow the path)
                                Walker.walkTo(target);
                            } else {
                                log.warn("[HelperBox] [WitchsHouse] No path found with Hybrid BFS, falling back to Walker.walkTo");
                                Walker.walkTo(target);
                            }
                        } catch (Exception e) {
                            log.error("[HelperBox] [WitchsHouse] Hybrid BFS failed, falling back to Walker.walkTo", e);
                            Walker.walkTo(target);
                        }
                        
                        walkingToBoy = true;
                        lastBoyTarget = target;
                    }
                }
                else
                {
                    // We've arrived near the Boy
                    walkingToBoy = false;
                    lastBoyTarget = null;

                    try
                    {
                        NpcEx boy = NpcAPI.search().withNameContains("Boy").shortestPath();
                        if (boy != null)
                        {
                            if (config.logSteps())
                            {
                                log.info("[HelperBox] [WitchsHouse] Talking to Boy to start quest");
                            }
                            NpcAPI.interact(boy, "Talk-to");

                            // Handle the full boy dialogue, including extra continues/options
                            handleBoyDialogue();
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("[HelperBox] [WitchsHouse] Failed to talk to Boy", e);
                    }

                    // Move on to the next step (entering garden / house logic will be wired next)
                    step = Step.ENTER_GARDEN;
                }
                break;

            case ENTER_GARDEN:
                boolean hasHouseKey = InventoryAPI.getItem(ItemID.WITCHES_DOORKEY) != null;
                if (!hasHouseKey)
                {
                    if (pos.distanceTo(POT_TILE) > 3)
                    {
                        Walker.walkTo(POT_TILE);
                    }
                    else
                    {
                        TileObjectEx pot = TileObjectAPI.search()
                            .withId(ObjectID.WITCHPOT)
                            .sortNearest()
                            .first();
                        if (pot != null)
                        {
                            TileObjectAPI.interact(pot, "Search");
                        }
                    }
                    break;
                }
                step = Step.ENTER_HOUSE;
                break;

            case ENTER_HOUSE:
                if (HOUSE_AREA.contains(pos))
                {
                    step = Step.SEARCH_FOR_KEY;
                    break;
                }
                if (pos.distanceTo(HOUSE_DOOR_TILE) > 3)
                {
                    Walker.walkTo(HOUSE_DOOR_TILE);
                }
                else
                {
                    TileObjectEx door = TileObjectAPI.search()
                        .withId(ObjectID.WITCHHOUSEDOOR)
                        .sortNearest()
                        .first();
                    if (door != null)
                    {
                        TileObjectAPI.interact(door, "Open", "Unlock");
                    }
                }
                break;

            default:
                // Further steps will be implemented in later phases
                break;
        }
    }
}
