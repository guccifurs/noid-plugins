package com.tonic.plugins.helperbox;

import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.services.pathfinder.Walker;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.ObjectID;

@Slf4j
public class AgilityAlKharidTask
{
    private final Client client;
    private final HelperBoxConfig config;

    // Approximate ground tile near the Al-Kharid rooftop start
    private static final WorldPoint START_POINT = new WorldPoint(3299, 3194, 0);

    // Number of logical steps
    public static final int TOTAL_STEPS = 8;

    private enum Stage
    {
        WALK_TO_START,
        CLIMB_ROUGH_WALL,
        FIRST_TIGHTROPE,
        CABLE,
        ZIP_LINE,
        TROPICAL_TREE,
        ROOF_TOP_BEAMS,
        SECOND_TIGHTROPE,
        FINAL_GAP
    }

    private Stage stage = Stage.WALK_TO_START;
    private WorldPoint lastPos = null;
    private long lastMoveTime = 0L;
    private long lastActionTime = 0L;
    private long lastCantReachTime = 0L;
    private boolean autoRunDisabledByUser = false;
    private boolean lastRunEnabled = false;

    public AgilityAlKharidTask(Client client, HelperBoxConfig config)
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
        PlayerEx local = PlayerEx.getLocal();
        Stage displayStage = stage;
        if (local != null && local.getWorldPoint() != null && local.getWorldPoint().getPlane() > 0)
        {
            displayStage = inferStageFromPosition(local.getWorldPoint());
        }

        switch (displayStage)
        {
            case CLIMB_ROUGH_WALL: return 1;
            case FIRST_TIGHTROPE:  return 2;
            case CABLE:            return 3;
            case ZIP_LINE:         return 4;
            case TROPICAL_TREE:    return 5;
            case ROOF_TOP_BEAMS:   return 6;
            case SECOND_TIGHTROPE: return 7;
            case FINAL_GAP:        return 8;
            default:               return 0;
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
            case CLIMB_ROUGH_WALL: return "Climb rough wall";
            case FIRST_TIGHTROPE:  return "First tightrope";
            case CABLE:            return "Cable";
            case ZIP_LINE:         return "Zip line";
            case TROPICAL_TREE:    return "Tropical tree";
            case ROOF_TOP_BEAMS:   return "Roof top beams";
            case SECOND_TIGHTROPE: return "Second tightrope";
            case FINAL_GAP:        return "Final gap";
            default:               return "Walking to start";
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
                    log.info("[HelperBox] [Al-Kharid] Resuming course at stage {}", inferred.name());
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
                    log.info("[HelperBox] [Al-Kharid] Picking up Mark of Grace at {}", markWp);
                }
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
                log.info("[HelperBox] [Al-Kharid] Failsafe: no movement after action, resetting to WALK_TO_START");
            }
            resetState();
            return;
        }

        // Basic anti-stuck: if we haven't moved for a while, reset state so mid-course detection can recover
        if (config.antiStuck() && now - lastMoveTime > 10000 && now - lastActionTime > 6000)
        {
            log.debug("[HelperBox] [Al-Kharid] Detected possible stuck state, resetting to WALK_TO_START");
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
                    log.info("[HelperBox] [Al-Kharid] Recovering from can't reach: switching to stage {}", inferred.name());
                }
                stage = inferred;
            }
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
                    log.info("[HelperBox] [Al-Kharid] Idle recovery: switching to stage {}", inferred.name());
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
            case FIRST_TIGHTROPE:
                handleFirstTightrope(pos, now);
                break;
            case CABLE:
                handleCable(pos, now);
                break;
            case ZIP_LINE:
                handleZipLine(pos, now);
                break;
            case TROPICAL_TREE:
                handleTropicalTree(pos, now);
                break;
            case ROOF_TOP_BEAMS:
                handleRoofTopBeams(pos, now);
                break;
            case SECOND_TIGHTROPE:
                handleSecondTightrope(pos, now);
                break;
            case FINAL_GAP:
                handleFinalGap(pos, now);
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
            InventoryAPI.interact("Stamina potion", "Drink");
            Delays.tick(2);
        }
    }

    private void handleWalkToStart(WorldPoint pos, long now)
    {
        TileObjectEx startWall = TileObjectAPI.search()
            .withId(ObjectID.ROUGH_WALL_11633)
            .sortNearest()
            .first();

        if (startWall != null && pos.getPlane() == 0 && pos.distanceTo(startWall.getWorldPoint()) <= 5)
        {
            stage = Stage.CLIMB_ROUGH_WALL;
            return;
        }

        if (config.autoWalkToCourse())
        {
            WorldPoint target = null;
            if (startWall != null)
            {
                target = startWall.getWorldPoint();
            }
            else
            {
                target = START_POINT;
            }

            if (target != null)
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] [Al-Kharid] Walking to rooftop start via world walker at {}", target);
                }

                // Use global world walker (Hybrid BFS) to path from anywhere
                Walker.walkTo(target);
                lastActionTime = now;
            }
        }
    }

    private void handleClimbRoughWall(WorldPoint pos, long now)
    {
        if (pos.getPlane() > 0)
        {
            Stage inferred = inferStageFromPosition(pos);
            if (inferred != Stage.WALK_TO_START)
            {
                stage = inferred;
                return;
            }
            stage = Stage.FIRST_TIGHTROPE;
            return;
        }

        TileObjectEx wall = TileObjectAPI.search()
            .withId(ObjectID.ROUGH_WALL_11633)
            .sortNearest()
            .first();

        if (wall != null && wall.getWorldPoint().getPlane() == 0)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Al-Kharid] Climbing rough wall");
            }
            TileObjectAPI.interact(wall, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.FIRST_TIGHTROPE;
        }
    }

    private void handleFirstTightrope(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx rope = TileObjectAPI.search()
            .withId(ObjectID.TIGHTROPE_14398)
            .sortNearest()
            .first();

        if (rope != null && rope.getWorldPoint().getPlane() == pos.getPlane())
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Al-Kharid] Crossing first tightrope");
            }
            TileObjectAPI.interact(rope, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.CABLE;
        }
    }

    private void handleCable(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx cable = TileObjectAPI.search()
            .withId(ObjectID.CABLE)
            .sortNearest()
            .first();

        if (cable != null && cable.getWorldPoint().getPlane() == pos.getPlane())
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Al-Kharid] Crossing cable");
            }
            TileObjectAPI.interact(cable, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.ZIP_LINE;
        }
    }

    private void handleZipLine(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx zip = TileObjectAPI.search()
            .withId(ObjectID.ZIP_LINE_14403)
            .sortNearest()
            .first();

        if (zip != null && zip.getWorldPoint().getPlane() == pos.getPlane())
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Al-Kharid] Using zip line");
            }
            TileObjectAPI.interact(zip, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.TROPICAL_TREE;
        }
    }

    private void handleTropicalTree(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx tree = TileObjectAPI.search()
            .withId(ObjectID.TROPICAL_TREE_14404)
            .sortNearest()
            .first();

        if (tree != null && tree.getWorldPoint().getPlane() == pos.getPlane())
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Al-Kharid] Using tropical tree");
            }
            TileObjectAPI.interact(tree, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.ROOF_TOP_BEAMS;
        }
    }

    private void handleRoofTopBeams(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx beams = TileObjectAPI.search()
            .withId(ObjectID.ROOF_TOP_BEAMS)
            .sortNearest()
            .first();

        if (beams != null && beams.getWorldPoint().getPlane() == pos.getPlane())
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Al-Kharid] Crossing roof-top beams");
            }
            TileObjectAPI.interact(beams, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.SECOND_TIGHTROPE;
        }
    }

    private void handleSecondTightrope(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx rope = TileObjectAPI.search()
            .withId(ObjectID.TIGHTROPE_14409)
            .sortNearest()
            .first();

        if (rope != null && rope.getWorldPoint().getPlane() == pos.getPlane())
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Al-Kharid] Crossing second tightrope");
            }
            TileObjectAPI.interact(rope, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.FINAL_GAP;
        }
    }

    private void handleFinalGap(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx gap = TileObjectAPI.search()
            .withId(ObjectID.GAP_14399)
            .sortNearest()
            .first();

        if (gap != null && gap.getWorldPoint().getPlane() == pos.getPlane())
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Al-Kharid] Jumping final gap");
            }
            TileObjectAPI.interact(gap, 0);
            lastActionTime = now;
            Delays.tick(2);
            // After final gap we land back on ground near course start
            stage = Stage.WALK_TO_START;
        }
    }

    private Stage inferStageFromPosition(WorldPoint pos)
    {
        // Prefer reachable obstacles from latest to earliest stage
        // 1) Final gap
        TileObjectEx gap = TileObjectAPI.search()
            .withId(ObjectID.GAP_14399)
            .sortNearest()
            .first();
        if (gap != null)
        {
            WorldPoint ip = gap.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.FINAL_GAP;
            }
        }

        // 2) Second tightrope
        TileObjectEx rope2 = TileObjectAPI.search()
            .withId(ObjectID.TIGHTROPE_14409)
            .sortNearest()
            .first();
        if (rope2 != null)
        {
            WorldPoint ip = rope2.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.SECOND_TIGHTROPE;
            }
        }

        // 3) Roof-top beams
        TileObjectEx beams = TileObjectAPI.search()
            .withId(ObjectID.ROOF_TOP_BEAMS)
            .sortNearest()
            .first();
        if (beams != null)
        {
            WorldPoint ip = beams.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.ROOF_TOP_BEAMS;
            }
        }

        // 4) Tropical tree
        TileObjectEx tree = TileObjectAPI.search()
            .withId(ObjectID.TROPICAL_TREE_14404)
            .sortNearest()
            .first();
        if (tree != null)
        {
            WorldPoint ip = tree.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.TROPICAL_TREE;
            }
        }

        // 5) Zip line
        TileObjectEx zip = TileObjectAPI.search()
            .withId(ObjectID.ZIP_LINE_14403)
            .sortNearest()
            .first();
        if (zip != null)
        {
            WorldPoint ip = zip.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.ZIP_LINE;
            }
        }

        // 6) Cable
        TileObjectEx cable = TileObjectAPI.search()
            .withId(ObjectID.CABLE)
            .sortNearest()
            .first();
        if (cable != null)
        {
            WorldPoint ip = cable.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.CABLE;
            }
        }

        // 7) First tightrope
        TileObjectEx rope1 = TileObjectAPI.search()
            .withId(ObjectID.TIGHTROPE_14398)
            .sortNearest()
            .first();
        if (rope1 != null)
        {
            WorldPoint ip = rope1.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                return Stage.FIRST_TIGHTROPE;
            }
        }

        return Stage.WALK_TO_START;
    }
}
