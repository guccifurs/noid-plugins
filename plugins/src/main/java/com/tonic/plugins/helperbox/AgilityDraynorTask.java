package com.tonic.plugins.helperbox;

import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.pathfinder.Walker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

@Slf4j
public class AgilityDraynorTask
{
    private final Client client;
    private final HelperBoxConfig config;

    // Draynor rooftop obstacle IDs and key points
    private static final WorldPoint START_POINT = new WorldPoint(3103, 3279, 0); // rough wall
    private static final WorldPoint ROPE1_POINT = new WorldPoint(3098, 3277, 3);
    private static final WorldPoint ROPE2_POINT = new WorldPoint(3092, 3276, 3);
    private static final WorldPoint NARROW_WALL_POINT = new WorldPoint(3089, 3264, 3);
    private static final WorldPoint WALL_SCRAMBLE_POINT = new WorldPoint(3088, 3256, 3);
    private static final WorldPoint GAP_POINT = new WorldPoint(3095, 3255, 3);
    private static final WorldPoint CRATE_POINT = new WorldPoint(3102, 3261, 3);

    private enum Stage
    {
        WALK_TO_START,
        CLIMB_ROUGH_WALL,
        WALK_FIRST_ROPE,
        WALK_SECOND_ROPE,
        BALANCE_WALL,
        JUMP_UP_WALL,
        JUMP_GAP,
        CLIMB_DOWN_CRATE
    }

    // Number of logical steps (excluding WALK_TO_START)
    public static final int TOTAL_STEPS = 7;

    private Stage stage = Stage.WALK_TO_START;
    private WorldPoint lastPos = null;
    private long lastMoveTime = 0L;
    private long lastActionTime = 0L;
    private long lastCantReachTime = 0L;
    private boolean autoRunDisabledByUser = false;
    private boolean lastRunEnabled = false;

    public AgilityDraynorTask(Client client, HelperBoxConfig config)
    {
        this.client = client;
        this.config = config;
    }

    public void resetState()
    {
        stage = Stage.WALK_TO_START;
        lastPos = null;
        lastMoveTime = 0L;
        lastActionTime = 0L;
        lastCantReachTime = 0L;
    }

    public void notifyCantReach()
    {
        lastCantReachTime = System.currentTimeMillis();
    }

    private boolean isLaterStage(Stage candidate, Stage current)
    {
        return candidate.ordinal() > current.ordinal();
    }

    public int getStepIndex()
    {
        // For display, prefer actual player position over internal stage state
        PlayerEx local = PlayerEx.getLocal();
        Stage displayStage = stage;
        if (local != null && local.getWorldPoint() != null && local.getWorldPoint().getPlane() > 0)
        {
            displayStage = inferStageFromPosition(local.getWorldPoint());
        }

        switch (displayStage)
        {
            case CLIMB_ROUGH_WALL:
                return 1;
            case WALK_FIRST_ROPE:
                return 2;
            case WALK_SECOND_ROPE:
                return 3;
            case BALANCE_WALL:
                return 4;
            case JUMP_UP_WALL:
                return 5;
            case JUMP_GAP:
                return 6;
            case CLIMB_DOWN_CRATE:
                return 7;
            default:
                return 0; // WALK_TO_START or unknown
        }
    }

    public String getStepLabel()
    {
        PlayerEx local = PlayerEx.getLocal();
        Stage displayStage = stage;
        if (local != null && local.getWorldPoint() != null && local.getWorldPoint().getPlane() > 0)
        {
            displayStage = inferStageFromPosition(local.getWorldPoint());
        }

        switch (displayStage)
        {
            case CLIMB_ROUGH_WALL:
                return "Climb rough wall";
            case WALK_FIRST_ROPE:
                return "First tightrope";
            case WALK_SECOND_ROPE:
                return "Second tightrope";
            case BALANCE_WALL:
                return "Narrow wall";
            case JUMP_UP_WALL:
                return "Wall scramble";
            case JUMP_GAP:
                return "Leap gap";
            case CLIMB_DOWN_CRATE:
                return "Climb crate";
            default:
                return "Walking to start";
        }
    }

    public void tick()
    {
        PlayerEx local = PlayerEx.getLocal();
        if (local == null)
        {
            return;
        }

        if (!local.isIdle())
        {
            return;
        }

        WorldPoint pos = local.getWorldPoint();
        long now = System.currentTimeMillis();

        // If we just started mid-course on the rooftop, infer the correct stage once
        if (stage == Stage.WALK_TO_START && pos.getPlane() > 0)
        {
            Stage inferred = inferStageFromPosition(pos);
            if (inferred != Stage.WALK_TO_START)
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] Resuming Draynor course at stage {}", inferred.name());
                }
                stage = inferred;
            }
        }

        // Always grab nearby, reachable Marks of Grace first
        TileItemEx mark = TileItemAPI.search().withId(ItemID.MARK_OF_GRACE).nearest();
        if (mark != null)
        {
            WorldPoint markWp = mark.getWorldPoint();
            if (markWp.getPlane() == pos.getPlane()
                && markWp.distanceTo(pos) <= 8
                && SceneAPI.isReachable(pos, markWp))
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] Picking up Mark of Grace at {}", markWp);
                }
                // Use action name instead of raw index for robustness
                TileItemAPI.interact(mark, "Take");
                lastActionTime = now;
                Delays.tick(2);
                return;
            }
        }

        if (lastPos == null || !lastPos.equals(pos))
        {
            lastPos = pos;
            lastMoveTime = now;
        }

        // Failsafe: if a recent action didn't cause any movement, reset state as if freshly started
        if (config.antiStuck() && lastActionTime > 0 && now - lastActionTime > 4000 && lastMoveTime < lastActionTime)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Draynor] Failsafe: no movement after action, resetting to WALK_TO_START");
            }
            resetState();
            return;
        }

        // Basic anti-stuck: if we haven't moved for a while, reset state so mid-course detection can recover
        if (config.antiStuck() && now - lastMoveTime > 10000 && now - lastActionTime > 6000)
        {
            log.debug("[HelperBox] Detected possible stuck state, resetting to WALK_TO_START");
            resetState();
            return;
        }

        // If we recently got "I can't reach that", re-evaluate stage from current position
        if (config.antiStuck() && lastCantReachTime > 0 && now - lastCantReachTime < 5000)
        {
            Stage inferred = inferStageFromPosition(pos);
            if (inferred != Stage.WALK_TO_START && isLaterStage(inferred, stage))
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] Recovering from can't reach: switching to stage {}", inferred.name());
                }
                stage = inferred;
            }
            // Clear flag so we don't keep re-triggering
            lastCantReachTime = 0L;
        }

        // If we've been idle on rooftop for a short while, re-evaluate stage from current position
        if (config.antiStuck() && pos.getPlane() > 0 && now - lastActionTime > 2500)
        {
            Stage inferred = inferStageFromPosition(pos);
            if (inferred != Stage.WALK_TO_START && isLaterStage(inferred, stage))
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] Idle recovery: switching to stage {}", inferred.name());
                }
                stage = inferred;
            }
        }

        // Stamina handling
        handleStamina();

        switch (stage)
        {
            case WALK_TO_START:
                handleWalkToStart(pos, now);
                break;
            case CLIMB_ROUGH_WALL:
                handleClimbRoughWall(pos, now);
                break;
            case WALK_FIRST_ROPE:
                handleFirstRope(pos, now);
                break;
            case WALK_SECOND_ROPE:
                handleSecondRope(pos, now);
                break;
            case BALANCE_WALL:
                handleBalanceWall(pos, now);
                break;
            case JUMP_UP_WALL:
                handleJumpUpWall(pos, now);
                break;
            case JUMP_GAP:
                handleJumpGap(pos, now);
                break;
            case CLIMB_DOWN_CRATE:
                handleClimbDownCrate(pos, now);
                break;
        }
    }

    private void handleStamina()
    {
        int energy = client.getEnergy();
        boolean runEnabled = MovementAPI.isRunEnabled();

        if (lastRunEnabled && !runEnabled && !autoRunDisabledByUser)
        {
            autoRunDisabledByUser = true;
        }
        else if (autoRunDisabledByUser && !lastRunEnabled && runEnabled)
        {
            autoRunDisabledByUser = false;
        }
        lastRunEnabled = runEnabled;

        // Keep run enabled when we have enough energy
        if (!autoRunDisabledByUser && energy > 25 && !runEnabled)
        {
            MovementAPI.toggleRun();
            lastRunEnabled = true;
        }

        if (!config.useStamina())
        {
            return;
        }

        if (energy < config.staminaThreshold() && !MovementAPI.staminaInEffect())
        {
            // Click any stamina potion in inventory
            InventoryAPI.interact("Stamina potion", "Drink");
            Delays.tick(2);
        }
    }

    private void handleWalkToStart(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0 && pos.distanceTo(START_POINT) <= 5)
        {
            stage = Stage.CLIMB_ROUGH_WALL;
            return;
        }

        if (config.autoWalkToCourse())
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] Walking to Draynor rooftop start via world walker");
            }
            // Use global world walker (Hybrid BFS) to path from anywhere
            Walker.walkTo(START_POINT);
            lastActionTime = now;
        }
    }

    private void handleClimbRoughWall(WorldPoint pos, long now)
    {
        if (pos.getPlane() > 0)
        {
            // We are already on the roof; go to correct rooftop stage
            Stage inferred = inferStageFromPosition(pos);
            if (inferred != Stage.WALK_TO_START)
            {
                stage = inferred;
                return;
            }

            stage = Stage.WALK_FIRST_ROPE;
            return;
        }

        TileObjectEx wall = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_WALLCLIMB)
            .within(START_POINT, 10)
            .sortNearest(START_POINT)
            .first();

        if (wall != null)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] Climbing rough wall");
            }
            TileObjectAPI.interact(wall, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.WALK_FIRST_ROPE;
        }
    }

    private void handleFirstRope(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            // We fell; go back to start
            stage = Stage.WALK_TO_START;
            return;
        }

        // If we are near rope 1 area, click tightrope 1
        if (pos.distanceTo(ROPE1_POINT) <= 10)
        {
            TileObjectEx rope = TileObjectAPI.search()
                .withId(ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_1)
                .within(ROPE1_POINT, 10)
                .sortNearest(ROPE1_POINT)
                .first();

            if (rope != null)
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] Crossing first tightrope");
                }
                TileObjectAPI.interact(rope, 0);
                lastActionTime = now;
                Delays.tick(2);
                stage = Stage.WALK_SECOND_ROPE;
            }
        }
    }

    private void handleSecondRope(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        if (pos.distanceTo(ROPE2_POINT) <= 10)
        {
            TileObjectEx rope = TileObjectAPI.search()
                .withId(ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_2)
                .within(ROPE2_POINT, 10)
                .sortNearest(ROPE2_POINT)
                .first();

            if (rope != null)
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] Crossing second tightrope");
                }
                TileObjectAPI.interact(rope, 0);
                lastActionTime = now;
                Delays.tick(2);
                stage = Stage.BALANCE_WALL;
            }
        }
    }

    private void handleBalanceWall(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        // After second rope we often land a bit away from the wall; walk closer first
        if (pos.distanceTo(NARROW_WALL_POINT) > 2)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] Walking to narrow wall");
            }
            // For this short adjustment, use a simple walk click instead of full pathfinder to avoid empty-path errors
            MovementAPI.walkToWorldPoint(NARROW_WALL_POINT);
            lastActionTime = now;
            return;
        }

        TileObjectEx wall = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_WALLCROSSING)
            .within(NARROW_WALL_POINT, 10)
            .sortNearest(NARROW_WALL_POINT)
            .first();

        if (wall != null)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] Balancing across narrow wall");
            }
            TileObjectAPI.interact(wall, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.JUMP_UP_WALL;
        }
    }

    private void handleJumpUpWall(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        // If the gap is already reachable from here, we have finished the scramble
        TileObjectEx gap = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_LEAPDOWN)
            .within(GAP_POINT, 10)
            .sortNearest(GAP_POINT)
            .first();

        boolean gapReachable = gap != null && SceneAPI.isReachable(pos, gap.getInteractionPoint());
        if (config.logSteps())
        {
            log.info("[HelperBox] Debug: JUMP_UP_WALL at {}, gapReachable={}", pos, gapReachable);
        }

        if (gapReachable)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] Finished wall scramble, moving to gap");
            }
            stage = Stage.JUMP_GAP;
            return;
        }

        // Otherwise, normal interaction with wall scramble, only if reachable
        TileObjectEx wall = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_WALLSCRAMBLE)
            .within(WALL_SCRAMBLE_POINT, 10)
            .sortNearest(WALL_SCRAMBLE_POINT)
            .first();

        boolean wallReachable = wall != null && SceneAPI.isReachable(pos, wall.getInteractionPoint());
        if (config.logSteps())
        {
            log.info("[HelperBox] Debug: WALLSCRAMBLE at {}, wallReachable={}", pos, wallReachable);
        }

        if (wallReachable)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] Jumping up wall");
            }
            TileObjectAPI.interact(wall, 0);
            lastActionTime = now;
            Delays.tick(2);
            // Stage will be advanced on next tick once player moves away from scramble tile
        }
    }

    private void handleJumpGap(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx gap = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_LEAPDOWN)
            .within(GAP_POINT, 10)
            .sortNearest(GAP_POINT)
            .first();

        boolean gapReachable = gap != null && SceneAPI.isReachable(pos, gap.getInteractionPoint());
        if (config.logSteps())
        {
            log.info("[HelperBox] Debug: JUMP_GAP at {}, gapReachable={}", pos, gapReachable);
        }

        if (gapReachable)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] Jumping gap");
            }
            TileObjectAPI.interact(gap, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.CLIMB_DOWN_CRATE;
        }
    }

    private void handleClimbDownCrate(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0 && pos.distanceTo(START_POINT) <= 20)
        {
            // We finished the course; start again
            if (config.logSteps())
            {
                log.info("[HelperBox] Completed Draynor course lap; restarting");
            }
            stage = Stage.WALK_TO_START;
            return;
        }

        if (pos.distanceTo(CRATE_POINT) <= 10)
        {
            TileObjectEx crate = TileObjectAPI.search()
                .withId(ObjectID.ROOFTOPS_DRAYNOR_CRATE)
                .within(CRATE_POINT, 10)
                .sortNearest(CRATE_POINT)
                .first();

            if (crate != null)
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] Climbing down crate to finish lap");
                }
                TileObjectAPI.interact(crate, 0);
                lastActionTime = now;
                Delays.tick(2);
            }
        }
    }

    private Stage inferStageFromPosition(WorldPoint pos)
    {
        // Prefer reachable obstacles from latest to earliest stage
        // 1) Crate (end of course)
        TileObjectEx crate = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_CRATE)
            .within(CRATE_POINT, 10)
            .sortNearest(CRATE_POINT)
            .first();
        if (crate != null)
        {
            WorldPoint ip = crate.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.CLIMB_DOWN_CRATE;
            }
        }

        // 2) Gap
        TileObjectEx gap = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_LEAPDOWN)
            .within(GAP_POINT, 10)
            .sortNearest(GAP_POINT)
            .first();
        if (gap != null)
        {
            WorldPoint ip = gap.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.JUMP_GAP;
            }
        }

        // 3) Wall scramble
        TileObjectEx scramble = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_WALLSCRAMBLE)
            .within(WALL_SCRAMBLE_POINT, 10)
            .sortNearest(WALL_SCRAMBLE_POINT)
            .first();
        if (scramble != null)
        {
            WorldPoint ip = scramble.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.JUMP_UP_WALL;
            }
        }

        // 4) Narrow wall
        TileObjectEx narrow = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_WALLCROSSING)
            .within(NARROW_WALL_POINT, 10)
            .sortNearest(NARROW_WALL_POINT)
            .first();
        if (narrow != null)
        {
            WorldPoint ip = narrow.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.BALANCE_WALL;
            }
        }

        // 5) Second rope
        TileObjectEx rope2 = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_2)
            .within(ROPE2_POINT, 10)
            .sortNearest(ROPE2_POINT)
            .first();
        if (rope2 != null)
        {
            WorldPoint ip = rope2.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.WALK_SECOND_ROPE;
            }
        }

        // 6) First rope
        TileObjectEx rope1 = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_1)
            .within(ROPE1_POINT, 10)
            .sortNearest(ROPE1_POINT)
            .first();
        if (rope1 != null)
        {
            WorldPoint ip = rope1.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.WALK_FIRST_ROPE;
            }
        }

        // Fallback to distance heuristic if no reachable obstacle was found
        if (pos.distanceTo(CRATE_POINT) <= 6 && pos.getPlane() >= 2)
        {
            return Stage.CLIMB_DOWN_CRATE;
        }
        if (pos.distanceTo(GAP_POINT) <= 6 && pos.getPlane() >= 2)
        {
            return Stage.JUMP_GAP;
        }
        if (pos.distanceTo(WALL_SCRAMBLE_POINT) <= 6 && pos.getPlane() >= 2)
        {
            return Stage.JUMP_UP_WALL;
        }
        if (pos.distanceTo(NARROW_WALL_POINT) <= 6 && pos.getPlane() >= 2)
        {
            return Stage.BALANCE_WALL;
        }
        if (pos.distanceTo(ROPE2_POINT) <= 6 && pos.getPlane() >= 2)
        {
            return Stage.WALK_SECOND_ROPE;
        }
        if (pos.distanceTo(ROPE1_POINT) <= 6 && pos.getPlane() >= 2)
        {
            return Stage.WALK_FIRST_ROPE;
        }

        // Default: walk to start on ground
        return Stage.WALK_TO_START;
    }
}

