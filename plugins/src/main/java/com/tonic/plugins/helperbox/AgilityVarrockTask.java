package com.tonic.plugins.helperbox;

import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.services.pathfinder.Walker;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

@Slf4j
public class AgilityVarrockTask
{
    private final Client client;
    private final HelperBoxConfig config;

    private static final WorldPoint START_POINT = new WorldPoint(3221, 3414, 0);

    // Define course bounding boxes / regions (as per user-provided coordinates)
    private static final WorldPoint COURSE_GROUND_A = new WorldPoint(3184, 3386, 0);
    private static final WorldPoint COURSE_GROUND_B = new WorldPoint(3258, 3428, 0);

    private static final WorldPoint ROOFTOP_ONE_A = new WorldPoint(3213, 3409, 3);
    private static final WorldPoint ROOFTOP_ONE_B = new WorldPoint(3220, 3420, 3);

    private static final WorldPoint ROOFTOP_TWO_A = new WorldPoint(3200, 3412, 3);
    private static final WorldPoint ROOFTOP_TWO_B = new WorldPoint(3209, 3420, 3);

    private static final WorldPoint CROSSWALK_A = new WorldPoint(3192, 3415, 1);
    private static final WorldPoint CROSSWALK_B = new WorldPoint(3198, 3417, 1);

    private static final WorldPoint ROOFTOP_THREE_A = new WorldPoint(3191, 3401, 3);
    private static final WorldPoint ROOFTOP_THREE_B = new WorldPoint(3198, 3407, 3);

    private static final WorldPoint ROOFTOP_FOUR_A = new WorldPoint(3181, 3393, 3);
    private static final WorldPoint ROOFTOP_FOUR_B = new WorldPoint(3209, 3401, 3);

    private static final WorldPoint ROOFTOP_FIVE_A = new WorldPoint(3217, 3392, 3);
    private static final WorldPoint ROOFTOP_FIVE_B = new WorldPoint(3233, 3404, 3);

    private static final WorldPoint ROOFTOP_SIX_A = new WorldPoint(3235, 3402, 3);
    private static final WorldPoint ROOFTOP_SIX_B = new WorldPoint(3240, 3409, 3);

    private static final WorldPoint ROOFTOP_SEVEN_A = new WorldPoint(3235, 3410, 3);
    private static final WorldPoint ROOFTOP_SEVEN_B = new WorldPoint(3240, 3416, 3);

    // Object ID mappings for Varrock rooftop course
    private static final int ROUGH_WALL_14412 = 14412;
    private static final int GAP_14414 = 14414;
    private static final int WALL_14832 = 14832;
    private static final int GAP_14833 = 14833;
    private static final int GAP_14834 = 14834;
    private static final int GAP_14835 = 14835;
    private static final int LEDGE_14836 = 14836;

    public static final int TOTAL_STEPS = 9;

    private enum Stage
    {
        WALK_TO_START,
        COURSE_GROUND,
        ROOFTOP_ONE,
        ROOFTOP_TWO,
        CROSSWALK,
        ROOFTOP_THREE,
        ROOFTOP_FOUR,
        ROOFTOP_FIVE,
        ROOFTOP_SIX,
        ROOFTOP_SEVEN
    }

    private Stage stage = Stage.WALK_TO_START;
    private WorldPoint lastPos = null;
    private long lastMoveTime = 0L;
    private long lastActionTime = 0L;
    private long lastCantReachTime = 0L;
    private boolean autoRunDisabledByUser = false;
    private boolean lastRunEnabled = false;
    // Tracking for last object we interacted with (to re-try if we appear to not have moved)
    private int lastInteractedObjectId = -1;
    private long lastInteractTime = 0L;
    private WorldPoint lastInteractPos = null;
    private int reInteractAttempts = 0;
    private Stage lastInteractedNextStage = null;
    private int lastInteractAnim = -1;

    public AgilityVarrockTask(Client client, HelperBoxConfig config)
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
        clearInteractionTrackers();
    }

    public void notifyCantReach()
    {
        lastCantReachTime = System.currentTimeMillis();
    }

    /**
     * Called when a game object action packet is sent. Used to advance stage immediately
     * when the player performs the object interaction (click).
     */
    public void onObjectActionPacket(int objectId, int worldX, int worldY, int plane)
    {
        if (!config.logSteps())
        {
            // keep this quiet unless debugging
        }
        if (config.logSteps())
        {
            log.info("[HelperBox] [Varrock] Packet object action detected id={} worldX={} worldY={} at stage={}", objectId, worldX, worldY, stage.name());
        }

        // Try to find the scene's object for this packet coordinate: sometimes the server
        // object IDs differ from the client id in the packet due to impostors, or the
        // worldX/worldY are what actually maps to the object. We'll prefer the tile object
        // to reduce coupling to raw ids.
        WorldPoint packetPoint = new WorldPoint(worldX, worldY, plane);
        TileObjectEx obj = findObjectFromPacket(packetPoint);

        if (obj != null)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] Packet mapped to object {} at {} (id pkt={} objid={})", obj.getName(), obj.getWorldPoint(), objectId, obj.getId());
            }

            int objId = obj.getId();
            // Stage mapping based on user-defined layout
            if (objId == ROUGH_WALL_14412)
            {
                if (stage == Stage.WALK_TO_START || stage == Stage.COURSE_GROUND)
                {
                    if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> COURSE_GROUND (rough wall)");
                    stage = Stage.COURSE_GROUND;
                    lastActionTime = System.currentTimeMillis();
                    clearInteractionTrackers();
                    return;
                }
            }
            if (objId == ObjectID.ROOFTOPS_VARROCK_CLOTHESLINE)
            {
                if (stage == Stage.COURSE_GROUND || stage == Stage.ROOFTOP_ONE)
                {
                    if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> ROOFTOP_TWO (clothes line)");
                    stage = Stage.ROOFTOP_TWO;
                    lastActionTime = System.currentTimeMillis();
                    clearInteractionTrackers();
                    return;
                }
            }
            if (objId == GAP_14414)
            {
                if (stage == Stage.ROOFTOP_TWO)
                {
                    if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> CROSSWALK (gap 14414)");
                    stage = Stage.CROSSWALK;
                    lastActionTime = System.currentTimeMillis();
                    clearInteractionTrackers();
                    return;
                }
            }
            if (objId == WALL_14832)
            {
                if (stage == Stage.CROSSWALK)
                {
                    if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> ROOFTOP_THREE (wall 14832)");
                    stage = Stage.ROOFTOP_THREE;
                    lastActionTime = System.currentTimeMillis();
                    clearInteractionTrackers();
                    return;
                }
            }
            if (objId == GAP_14833)
            {
                if (stage == Stage.ROOFTOP_THREE)
                {
                    if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> ROOFTOP_FOUR (gap 14833)");
                    stage = Stage.ROOFTOP_FOUR;
                    lastActionTime = System.currentTimeMillis();
                    clearInteractionTrackers();
                    return;
                }
            }
            if (objId == GAP_14834)
            {
                if (stage == Stage.ROOFTOP_FOUR)
                {
                    if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> ROOFTOP_FIVE (gap 14834)");
                    stage = Stage.ROOFTOP_FIVE;
                    lastActionTime = System.currentTimeMillis();
                    clearInteractionTrackers();
                    return;
                }
            }
            if (objId == GAP_14835)
            {
                if (stage == Stage.ROOFTOP_FIVE)
                {
                    if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> ROOFTOP_SIX (gap 14835)");
                    stage = Stage.ROOFTOP_SIX;
                    lastActionTime = System.currentTimeMillis();
                    clearInteractionTrackers();
                    return;
                }
            }
            if (objId == LEDGE_14836)
            {
                if (stage == Stage.ROOFTOP_SIX)
                {
                    if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> ROOFTOP_SEVEN (ledge 14836)");
                    stage = Stage.ROOFTOP_SEVEN;
                    lastActionTime = System.currentTimeMillis();
                    clearInteractionTrackers();
                    return;
                }
            }
            if (objId == ObjectID.ROOFTOPS_VARROCK_FINISH)
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> FINISH -> WALK_TO_START");
                stage = Stage.WALK_TO_START;
                lastActionTime = System.currentTimeMillis();
                clearInteractionTrackers();
                return;
            }
            if (objId == ObjectID.ROOFTOPS_VARROCK_FINISH)
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] Packet-> FINISH -> WALK_TO_START");
                stage = Stage.WALK_TO_START;
                lastActionTime = System.currentTimeMillis();
                clearInteractionTrackers();
                return;
            }
        }

        // Fallback: try to infer stage by raw id if object mapping failed
        if (objectId == ROUGH_WALL_14412)
        {
            if (stage.ordinal() <= Stage.COURSE_GROUND.ordinal())
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] PacketId-> ROUGH_WALL -> COURSE_GROUND (fallback)");
                stage = Stage.COURSE_GROUND;
                clearInteractionTrackers();
                return;
            }
        }
        if (objectId == ObjectID.ROOFTOPS_VARROCK_CLOTHESLINE)
        {
            if (stage.ordinal() <= Stage.ROOFTOP_TWO.ordinal())
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] PacketId-> CLOTHES_LINE -> ROOFTOP_TWO (fallback)");
                stage = Stage.ROOFTOP_TWO;
                clearInteractionTrackers();
                return;
            }
        }
        if (objectId == GAP_14414)
        {
            if (stage.ordinal() <= Stage.ROOFTOP_TWO.ordinal())
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] PacketId-> GAP_14414 -> CROSSWALK (fallback)");
                stage = Stage.CROSSWALK;
                clearInteractionTrackers();
                return;
            }
        }
        if (objectId == WALL_14832)
        {
            if (stage.ordinal() <= Stage.CROSSWALK.ordinal())
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] PacketId-> WALL_14832 -> ROOFTOP_THREE (fallback)");
                stage = Stage.ROOFTOP_THREE;
                clearInteractionTrackers();
                return;
            }
        }
        if (objectId == GAP_14833)
        {
            if (stage.ordinal() <= Stage.ROOFTOP_THREE.ordinal())
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] PacketId-> GAP_14833 -> ROOFTOP_FOUR (fallback)");
                stage = Stage.ROOFTOP_FOUR;
                clearInteractionTrackers();
                return;
            }
        }
        if (objectId == GAP_14834)
        {
            if (stage.ordinal() <= Stage.ROOFTOP_FOUR.ordinal())
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] PacketId-> GAP_14834 -> ROOFTOP_FIVE (fallback)");
                stage = Stage.ROOFTOP_FIVE;
                clearInteractionTrackers();
                return;
            }
        }
        if (objectId == GAP_14835)
        {
            if (stage.ordinal() <= Stage.ROOFTOP_FIVE.ordinal())
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] PacketId-> GAP_14835 -> ROOFTOP_SIX (fallback)");
                stage = Stage.ROOFTOP_SIX;
                clearInteractionTrackers();
                return;
            }
        }
        // If we reach here and haven't advanced, log the packet for debugging
        if (config.logSteps())
        {
            log.info("[HelperBox] [Varrock] Packet fallback did not match stage; id={} pktWx={}, pktWy={}, plane={}, current={} ", objectId, worldX, worldY, plane, stage.name());
        }
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
            Stage inferred = inferStageFromPosition(local.getWorldPoint());
            if (inferred != Stage.WALK_TO_START && isLaterStage(inferred, stage))
            {
                displayStage = inferred;
            }
        }

        switch (displayStage)
        {
            case COURSE_GROUND:   return 1;
            case ROOFTOP_ONE:     return 2;
            case ROOFTOP_TWO:     return 3;
            case CROSSWALK:       return 4;
            case ROOFTOP_THREE:   return 5;
            case ROOFTOP_FOUR:    return 6;
            case ROOFTOP_FIVE:    return 7;
            case ROOFTOP_SIX:     return 8;
            case ROOFTOP_SEVEN:   return 9; // final step
            default:              return 0;
        }
    }

    public String getStepLabel()
    {
        PlayerEx local = PlayerEx.getLocal();
        Stage displayStage = stage;
        if (local != null && local.getWorldPoint() != null && local.getWorldPoint().getPlane() > 0)
        {
            Stage inferred = inferStageFromPosition(local.getWorldPoint());
            if (inferred != Stage.WALK_TO_START && isLaterStage(inferred, stage))
            {
                displayStage = inferred;
            }
        }

        switch (displayStage)
        {
            case COURSE_GROUND:   return "Course ground";
            case ROOFTOP_ONE:     return "Rooftop 1 (rough wall / bank)";
            case ROOFTOP_TWO:     return "Rooftop 2 (clothes line / gap)";
            case CROSSWALK:       return "Crosswalk";
            case ROOFTOP_THREE:   return "Rooftop 3 (gap 14833)";
            case ROOFTOP_FOUR:    return "Rooftop 4 (gap 14834)";
            case ROOFTOP_FIVE:    return "Rooftop 5 (gap 14835)";
            case ROOFTOP_SIX:     return "Rooftop 6 (ledge 14836)";
            case ROOFTOP_SEVEN:   return "Rooftop 7 (final edge)";
            default:              return "Walking to start";
        }
    }

    public void tick()
    {
        PlayerEx local = PlayerEx.getLocal();
        if (local == null || !local.isIdle())
        {
            return;
        }

        WorldPoint pos = local.getWorldPoint();
        long now = System.currentTimeMillis();

        // Resume mid-course if detected on rooftop
        if (stage == Stage.WALK_TO_START && pos.getPlane() > 0)
        {
            Stage inferred = inferStageFromPosition(pos);
            if (inferred != Stage.WALK_TO_START)
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] [Varrock] Resuming at stage {}", inferred.name());
                }
                stage = inferred;
            }
        }

        // Pick up nearby marks of grace
        TileItemEx mark = TileItemAPI.search().withId(ItemID.MARK_OF_GRACE).nearest();
        if (mark != null && mark.getWorldPoint().getPlane() == pos.getPlane() 
            && mark.getWorldPoint().distanceTo(pos) <= 8 
            && SceneAPI.isReachable(pos, mark.getWorldPoint()))
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] Picking up mark of grace");
            }
            TileItemAPI.interact(mark, "Take");
            lastActionTime = now;
            Delays.tick(2);
            return;
        }

        // If we interacted with an object a short time ago and the player didn't move,
        // try re-interacting to recover from failed interactions
        if (lastInteractedObjectId != -1 && now - lastInteractTime > 2500 && now - lastMoveTime > 2000 && reInteractAttempts < 3)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] Re-attempting interaction with object {} (attempt {})", lastInteractedObjectId, reInteractAttempts + 1);
            }
            TileObjectEx retryObj = TileObjectAPI.search().withId(lastInteractedObjectId).within(pos, 6).sortNearest(pos).first();
            if (retryObj != null)
            {
                WorldPoint ip = retryObj.getInteractionPoint();
                if (ip != null && SceneAPI.isReachable(pos, ip))
                {
                    int action = 0;
                    // For known tricky objects, try alternate actions on subsequent retries
                            if (lastInteractedObjectId == GAP_14833)
                    {
                        action = Math.min(reInteractAttempts, 2);
                    }
                    if (config.logSteps()) log.info("[HelperBox] [Varrock] Re-trying interact with {} at {} (action idx {})", retryObj.getId(), ip, action);
                    TileObjectAPI.interact(retryObj, action);
                    lastActionTime = now;
                    Delays.tick(2);
                    reInteractAttempts++;
                    lastInteractTime = now;
                }
                else
                {
                    // Walk to the object if not reachable
                    MovementAPI.walkToWorldPoint(retryObj.getWorldPoint());
                    lastActionTime = now;
                }
            }
            else
            {
                // Couldn't find object to retry, reset
                lastInteractedObjectId = -1;
                reInteractAttempts = 0;
            }
        }


        // If we have interacted recently and the player moved from the interaction tile,
        // assume the interaction succeeded and advance the stored next stage.
        if (lastInteractedObjectId != -1 && lastInteractedNextStage != null && lastInteractPos != null)
        {
            // If player moved beyond the interaction point or plane changed or animation changed,
            // treat as success and advance to the next stage.
            boolean moved = !lastInteractPos.equals(pos) && lastInteractPos.distanceTo(pos) > 0;
            boolean planeChanged = lastInteractPos.getPlane() != pos.getPlane();
            boolean animChanged = false;
            try {
                com.tonic.data.wrappers.PlayerEx localEx = com.tonic.data.wrappers.PlayerEx.getLocal();
                if (localEx != null && localEx.getPlayer() != null && localEx.getPlayer().getAnimation() != -1 && localEx.getPlayer().getAnimation() != lastInteractAnim) {
                    animChanged = true;
                }
            } catch (Exception ignored) {}

            if (moved || planeChanged || animChanged)
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] [Varrock] Player moved after interacting with object {} - advancing to {}", lastInteractedObjectId, lastInteractedNextStage.name());
                }
                stage = lastInteractedNextStage;
                clearInteractionTrackers();
            }
        }
        // If we've tried re-interacting multiple times and still haven't moved, force-advance (fail-safe)
        if (lastInteractedObjectId != -1 && reInteractAttempts >= 3 && now - lastInteractTime > 6000)
        {
            if (lastInteractedNextStage != null)
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] [Varrock] Force-advancing stage to {} after {} failed re-interacts for object {}", lastInteractedNextStage.name(), reInteractAttempts, lastInteractedObjectId);
                }
                stage = lastInteractedNextStage;
            }
            lastInteractedObjectId = -1;
            lastInteractedNextStage = null;
            reInteractAttempts = 0;
        }

        // Update position tracking
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
                log.info("[HelperBox] [Varrock] Failsafe: no movement after action, resetting to WALK_TO_START");
            }
            resetState();
            return;
        }

        // Anti-stuck: reset if stuck for too long (let mid-course detection handle where we are)
        if (config.antiStuck() && now - lastMoveTime > 10000 && now - lastActionTime > 6000)
        {
            log.debug("[HelperBox] [Varrock] Anti-stuck triggered, resetting to WALK_TO_START");
            resetState();
            return;
        }

        // Recovery: try to advance if stuck
        if (config.antiStuck() && pos.getPlane() > 0 && now - lastActionTime > 2500)
        {
            Stage inferred = inferStageFromPosition(pos);
            if (inferred != Stage.WALK_TO_START && isLaterStage(inferred, stage))
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] [Varrock] Recovery: advancing to stage {}", inferred.name());
                }
                stage = inferred;
            }
        }

        handleStamina();

        switch (stage)
        {
            case WALK_TO_START:
                handleWalkToStart(pos, now);
                break;
            case COURSE_GROUND:
                handleRoughWall(pos, now);
                break;
            case ROOFTOP_ONE:
                handleClothesLine(pos, now);
                break;
            case ROOFTOP_TWO:
                handleLeapGap1(pos, now);
                break;
            case CROSSWALK:
                handleBalanceWall(pos, now);
                break;
            case ROOFTOP_THREE:
                handleLeapGap2(pos, now);
                break;
            case ROOFTOP_FOUR:
                handleLeapGap3(pos, now);
                break;
            case ROOFTOP_FIVE:
                handleLeapGap4(pos, now);
                break;
            case ROOFTOP_SIX:
                handleHurdleLedge(pos, now);
                break;
            case ROOFTOP_SEVEN:
                handleJumpOffEdge(pos, now);
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
        TileObjectEx wall = TileObjectAPI.search()
            .withId(ROUGH_WALL_14412)
            .sortNearest()
            .first();

        if (wall != null && pos.getPlane() == 0 && pos.distanceTo(wall.getWorldPoint()) <= 5)
        {
            stage = Stage.COURSE_GROUND;
            return;
        }

        if (config.autoWalkToCourse() && wall != null)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] Walking to start via world walker at {}", wall.getWorldPoint());
            }
            // Use global world walker (Hybrid BFS) to path from anywhere
            Walker.walkTo(wall.getWorldPoint());
            lastActionTime = now;
        }
    }

    private void handleRoughWall(WorldPoint pos, long now)
    {
        if (pos.getPlane() > 0)
        {
            Stage inferred = inferStageFromPosition(pos);
            if (inferred != Stage.WALK_TO_START && inferred != Stage.COURSE_GROUND)
            {
                stage = inferred;
                return;
            }
            stage = Stage.ROOFTOP_ONE;
            return;
        }

        TileObjectEx wall = TileObjectAPI.search()
            .withId(ROUGH_WALL_14412)
            .sortNearest()
            .first();

        if (wall != null)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] Climbing rough wall");
            }
            TileObjectAPI.interact(wall, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.ROOFTOP_ONE;
        }
    }

    private void handleClothesLine(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx clothes = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_VARROCK_CLOTHESLINE)
            .sortNearest()
            .first();

        if (clothes != null && clothes.getWorldPoint().getPlane() == pos.getPlane())
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] Clothes line");
            }
            TileObjectAPI.interact(clothes, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.ROOFTOP_TWO;
        }
    }

    private void handleLeapGap1(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }
        TileObjectEx gap = TileObjectAPI.search()
            .withId(GAP_14414)
            .sortNearest()
            .first();

        interactWithGap(gap, pos, now, Stage.CROSSWALK, "Leap gap");
    }

    private void handleBalanceWall(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }
        TileObjectEx balance = TileObjectAPI.search()
            .withId(WALL_14832)
            .sortNearest()
            .first();

        if (balance != null && balance.getWorldPoint().getPlane() == pos.getPlane())
        {
            if (config.logSteps()) log.info("[HelperBox] [Varrock] Crosswalk wall (wall 14832)");
            TileObjectAPI.interact(balance, 0);
            lastActionTime = now;
            Delays.tick(2);
            stage = Stage.ROOFTOP_THREE;
        }
    }

    private void handleLeapGap2(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }
        TileObjectEx gap = TileObjectAPI.search()
            .withId(GAP_14833)
            .sortNearest()
            .first();

        interactWithGap(gap, pos, now, Stage.ROOFTOP_FOUR, "Leap gap");
    }

    private void handleLeapGap3(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx gap = TileObjectAPI.search()
            .withId(GAP_14834)
            .sortNearest()
            .first();

        interactWithGap(gap, pos, now, Stage.ROOFTOP_FIVE, "Leap gap");
    }

    private void handleLeapGap4(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx gap = TileObjectAPI.search()
            .withId(GAP_14835)
            .sortNearest()
            .first();

        interactWithGap(gap, pos, now, Stage.ROOFTOP_SIX, "Leap gap");
    }

    private void handleHurdleLedge(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        // Try multiple potential hurdle object IDs (ledge)
        TileObjectEx hurdle = TileObjectAPI.search()
            .withId(LEDGE_14836)
            .sortNearest()
            .first();

        if (hurdle == null)
        {
            // Try alternate ID
            hurdle = TileObjectAPI.search()
                .withId(ObjectID.ROOFTOPS_VARROCK_STEPUPROOF)
                .sortNearest()
                .first();
        }

        if (hurdle != null && hurdle.getWorldPoint().getPlane() == pos.getPlane())
        {
            WorldPoint ip = hurdle.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                if (config.logSteps()) log.info("[HelperBox] [Varrock] Hurdle ledge (ledge 14836)");
                TileObjectAPI.interact(hurdle, 0);
                lastActionTime = now;
                Delays.tick(2);
                stage = Stage.ROOFTOP_SEVEN;
            }
            else
            {
                MovementAPI.walkToWorldPoint(hurdle.getWorldPoint());
                lastActionTime = now;
            }
        }
        else
        {
            // Auto-advance if not found
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] Hurdle not found, advancing");
            }
            stage = Stage.ROOFTOP_SEVEN;
        }
    }

    private void handleJumpOffEdge(WorldPoint pos, long now)
    {
        if (pos.getPlane() == 0)
        {
            stage = Stage.WALK_TO_START;
            return;
        }

        TileObjectEx edge = TileObjectAPI.search()
            .withId(ObjectID.ROOFTOPS_VARROCK_FINISH)
            .sortNearest()
            .first();

        if (edge != null && edge.getWorldPoint().getPlane() == pos.getPlane())
        {
            WorldPoint ip = edge.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] [Varrock] Jump-off edge");
                }
                TileObjectAPI.interact(edge, 0);
                lastActionTime = now;
                Delays.tick(2);
                stage = Stage.WALK_TO_START;
            }
            else
            {
                MovementAPI.walkToWorldPoint(edge.getWorldPoint());
                lastActionTime = now;
            }
        }
    }

    private void interactWithGap(TileObjectEx gap, WorldPoint pos, long now, Stage nextStage, String label)
    {
        if (gap != null)
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] Found object {} at {}, player at {}", gap.getId(), gap.getWorldPoint(), pos);
            }
            if (gap.getWorldPoint().getPlane() != pos.getPlane())
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] [Varrock] object {} is on a different plane ({}), player plane {}", gap.getId(), gap.getWorldPoint().getPlane(), pos.getPlane());
                }
                return;
            }
            WorldPoint ip = gap.getInteractionPoint();
            if (ip != null && SceneAPI.isReachable(pos, ip))
            {
                if (config.logSteps())
                {
                    log.info("[HelperBox] [Varrock] {}", label);
                }
                TileObjectAPI.interact(gap, 0);
                lastActionTime = now;
                // Track the last object we interacted with so we can re-try if we didn't move
                lastInteractedObjectId = gap.getId();
                lastInteractTime = now;
                lastInteractPos = ip != null ? ip : pos;
                try {
                    com.tonic.data.wrappers.PlayerEx localEx = com.tonic.data.wrappers.PlayerEx.getLocal();
                    if (localEx != null && localEx.getPlayer() != null)
                        lastInteractAnim = localEx.getPlayer().getAnimation();
                } catch (Exception ignored) {}
                reInteractAttempts = 0;
                if (config.logSteps()) log.info("[HelperBox] [Varrock] Interaction recorded: obj={} ip={} anim={} next={}", lastInteractedObjectId, lastInteractPos, lastInteractAnim, nextStage.name());
                lastInteractedNextStage = nextStage; // Set the next stage to be advanced later
                Delays.tick(2); // Delay after interaction
            }
            else
            {
                MovementAPI.walkToWorldPoint(gap.getWorldPoint());
                lastActionTime = now;
            }
        }
        else
        {
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] {}: gap not found (expected id or query)", label);
            }
        }
    }

    private Stage inferStageFromPosition(WorldPoint pos)
    {
        // Prefer region bounding boxes first
        if (isWithin(pos, ROOFTOP_SEVEN_A, ROOFTOP_SEVEN_B)) return Stage.ROOFTOP_SEVEN;
        if (isWithin(pos, ROOFTOP_SIX_A, ROOFTOP_SIX_B)) return Stage.ROOFTOP_SIX;
        if (isWithin(pos, ROOFTOP_FIVE_A, ROOFTOP_FIVE_B)) return Stage.ROOFTOP_FIVE;
        if (isWithin(pos, ROOFTOP_FOUR_A, ROOFTOP_FOUR_B)) return Stage.ROOFTOP_FOUR;
        if (isWithin(pos, ROOFTOP_THREE_A, ROOFTOP_THREE_B)) return Stage.ROOFTOP_THREE;
        if (isWithin(pos, CROSSWALK_A, CROSSWALK_B)) return Stage.CROSSWALK;
        if (isWithin(pos, ROOFTOP_TWO_A, ROOFTOP_TWO_B)) return Stage.ROOFTOP_TWO;
        if (isWithin(pos, ROOFTOP_ONE_A, ROOFTOP_ONE_B)) return Stage.ROOFTOP_ONE;
        if (isWithin(pos, COURSE_GROUND_A, COURSE_GROUND_B) && pos.getPlane() == 0) return Stage.COURSE_GROUND;

        // Next fallback: check nearest objects if region not identified
        TileObjectEx finish = TileObjectAPI.search().withId(ObjectID.ROOFTOPS_VARROCK_FINISH).sortNearest().first();
        if (finish != null && isReachable(pos, finish)) return Stage.ROOFTOP_SEVEN;

        TileObjectEx ledge6 = TileObjectAPI.search().withId(LEDGE_14836).sortNearest().first();
        if (ledge6 != null && isReachable(pos, ledge6)) return Stage.ROOFTOP_SIX;

        TileObjectEx gap5 = TileObjectAPI.search().withId(GAP_14835).sortNearest().first();
        if (gap5 != null && isReachable(pos, gap5)) return Stage.ROOFTOP_FIVE;

        TileObjectEx gap4 = TileObjectAPI.search().withId(GAP_14834).sortNearest().first();
        if (gap4 != null && isReachable(pos, gap4)) return Stage.ROOFTOP_FOUR;

        TileObjectEx gap3 = TileObjectAPI.search().withId(GAP_14833).sortNearest().first();
        if (gap3 != null && isReachable(pos, gap3)) return Stage.ROOFTOP_THREE;

        TileObjectEx wall = TileObjectAPI.search().withId(WALL_14832).sortNearest().first();
        if (wall != null && isReachable(pos, wall)) return Stage.CROSSWALK;

        TileObjectEx gap2 = TileObjectAPI.search().withId(GAP_14414).sortNearest().first();
        if (gap2 != null && isReachable(pos, gap2)) return Stage.ROOFTOP_TWO;

        TileObjectEx rough = TileObjectAPI.search().withId(ROUGH_WALL_14412).sortNearest().first();
        if (rough != null && isReachable(pos, rough)) return Stage.ROOFTOP_ONE;

        return Stage.WALK_TO_START;
    }

    private boolean isReachable(WorldPoint pos, TileObjectEx obj)
    {
        if (obj == null || obj.getWorldPoint() == null || obj.getWorldPoint().getPlane() != pos.getPlane())
        {
            return false;
        }
        WorldPoint ip = obj.getInteractionPoint();
        return ip != null && SceneAPI.isReachable(pos, ip);
    }

    private boolean isWithin(WorldPoint p, WorldPoint a, WorldPoint b)
    {
        if (p == null || a == null || b == null) return false;
        if (p.getPlane() != a.getPlane() || a.getPlane() != b.getPlane()) return false;
        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.max(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int maxY = Math.max(a.getY(), b.getY());
        return p.getX() >= minX && p.getX() <= maxX && p.getY() >= minY && p.getY() <= maxY;
    }

    private TileObjectEx findObjectFromPacket(WorldPoint packetPoint)
    {
        // Try to find by exact location first
        TileObjectEx obj = TileObjectAPI.search().within(packetPoint, 1).sortNearest(packetPoint).first();
        if (obj != null)
        {
            return obj;
        }

        // Expand search radius
        obj = TileObjectAPI.search().within(packetPoint, 2).sortNearest(packetPoint).first();
        if (obj != null)
        {
            return obj;
        }

        // Try common obstacle ids near that region
        List<Integer> ids = Arrays.asList(
            ROUGH_WALL_14412,
            ObjectID.ROOFTOPS_VARROCK_CLOTHESLINE,
            GAP_14414,
            WALL_14832,
            GAP_14833,
            GAP_14834,
            GAP_14835,
            LEDGE_14836,
            ObjectID.ROOFTOPS_VARROCK_STEPUPROOF,
            ObjectID.ROOFTOPS_VARROCK_WALLSCRAMBLE,
            ObjectID.ROOFTOPS_VARROCK_FINISH
        );

        for (int id : ids)
        {
            TileObjectEx t = TileObjectAPI.search().withId(id).within(packetPoint, 2).sortNearest(packetPoint).first();
            if (t != null)
            {
                return t;
            }
        }

        return null;
    }

    private void clearInteractionTrackers()
    {
        if (config.logSteps())
        {
            log.info("[HelperBox] [Varrock] Clearing interaction trackers (obj={}, next={})", lastInteractedObjectId, lastInteractedNextStage == null ? "null" : lastInteractedNextStage.name());
        }
        lastInteractedObjectId = -1;
        lastInteractedNextStage = null;
        lastInteractPos = null;
        reInteractAttempts = 0;
        lastInteractTime = 0L;
        lastInteractAnim = -1;
    }
}

