package com.tonic.plugins.gearswapper.triggers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles JSON serialization and deserialization of triggers
 */
public class TriggerSerializer {
    private static final Logger logger = LoggerFactory.getLogger(TriggerSerializer.class);

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * Serialize a list of triggers to JSON
     */
    public static String serializeTriggers(List<Trigger> triggers) {
        try {
            JsonArray triggersArray = new JsonArray();

            for (Trigger trigger : triggers) {
                JsonObject triggerJson = serializeTrigger(trigger);
                triggersArray.add(triggerJson);
            }

            JsonObject root = new JsonObject();
            root.add("triggers", triggersArray);
            root.addProperty("version", "1.0");
            root.addProperty("exported", System.currentTimeMillis());

            return gson.toJson(root);
        } catch (Exception e) {
            logger.error("[Trigger Serializer] Error serializing triggers: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Deserialize triggers from JSON
     */
    public static List<Trigger> deserializeTriggers(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new ArrayList<>();
            }

            JsonElement rootElement = JsonParser.parseString(json);
            if (!rootElement.isJsonObject()) {
                logger.error("[Trigger Serializer] Invalid JSON format");
                return new ArrayList<>();
            }

            JsonObject root = rootElement.getAsJsonObject();
            JsonArray triggersArray = root.getAsJsonArray("triggers");

            List<Trigger> triggers = new ArrayList<>();

            for (JsonElement triggerElement : triggersArray) {
                if (triggerElement.isJsonObject()) {
                    Trigger trigger = deserializeTrigger(triggerElement.getAsJsonObject());
                    if (trigger != null) {
                        triggers.add(trigger);
                    }
                }
            }

            logger.info("[Trigger Serializer] Loaded {} triggers from JSON", triggers.size());
            return triggers;
        } catch (Exception e) {
            logger.error("[Trigger Serializer] Error deserializing triggers: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Serialize a single trigger to JSON
     */
    private static JsonObject serializeTrigger(Trigger trigger) {
        JsonObject triggerJson = new JsonObject();

        // Basic info
        triggerJson.addProperty("id", trigger.getId());
        triggerJson.addProperty("name", trigger.getName());
        triggerJson.addProperty("type", trigger.getType().name());
        triggerJson.addProperty("enabled", trigger.isEnabled());
        triggerJson.addProperty("lastFired", trigger.getLastFired());
        triggerJson.addProperty("fireCount", trigger.getFireCount());
        triggerJson.addProperty("createdTime", trigger.getCreatedTime());

        // Configuration
        JsonObject configJson = serializeConfig(trigger.getConfig());
        triggerJson.add("config", configJson);

        // Actions
        JsonArray actionsArray = new JsonArray();
        for (TriggerAction action : trigger.getActions()) {
            JsonObject actionJson = serializeAction(action);
            actionsArray.add(actionJson);
        }
        triggerJson.add("actions", actionsArray);

        // Statistics
        JsonObject statsJson = serializeStats(trigger.getStats());
        triggerJson.add("stats", statsJson);

        return triggerJson;
    }

    /**
     * Deserialize a single trigger from JSON
     */
    private static Trigger deserializeTrigger(JsonObject triggerJson) {
        try {
            String id = triggerJson.get("id").getAsString();
            String name = triggerJson.get("name").getAsString();
            String typeName = triggerJson.get("type").getAsString();

            TriggerType type;
            try {
                type = TriggerType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                logger.error("[Trigger Serializer] Unknown trigger type: {}", typeName);
                return null;
            }

            Trigger trigger = new Trigger(id, name, type);

            // Basic properties
            trigger.setEnabled(triggerJson.get("enabled").getAsBoolean());
            trigger.setLastFired(triggerJson.get("lastFired").getAsLong());

            if (triggerJson.has("fireCount")) {
                trigger.setFireCount(triggerJson.get("fireCount").getAsInt());
            }

            if (triggerJson.has("createdTime")) {
                // Set created time via reflection since there's no setter
                try {
                    java.lang.reflect.Field createdTimeField = Trigger.class.getDeclaredField("createdTime");
                    createdTimeField.setAccessible(true);
                    createdTimeField.setLong(trigger, triggerJson.get("createdTime").getAsLong());
                } catch (Exception e) {
                    // Ignore if we can't set created time
                }
            }

            // Configuration
            if (triggerJson.has("config") && triggerJson.get("config").isJsonObject()) {
                TriggerConfig config = deserializeConfig(triggerJson.getAsJsonObject("config"));
                trigger.updateConfig(config);
            }

            // Actions
            if (triggerJson.has("actions") && triggerJson.get("actions").isJsonArray()) {
                JsonArray actionsArray = triggerJson.getAsJsonArray("actions");
                for (JsonElement actionElement : actionsArray) {
                    if (actionElement.isJsonObject()) {
                        TriggerAction action = deserializeAction(actionElement.getAsJsonObject());
                        if (action != null) {
                            trigger.addAction(action);
                        }
                    }
                }
            }

            // Statistics
            if (triggerJson.has("stats") && triggerJson.get("stats").isJsonObject()) {
                TriggerStats stats = deserializeStats(triggerJson.getAsJsonObject("stats"));
                trigger.setStats(stats);
            }

            return trigger;
        } catch (Exception e) {
            logger.error("[Trigger Serializer] Error deserializing trigger: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Serialize trigger configuration
     */
    private static JsonObject serializeConfig(TriggerConfig config) {
        JsonObject configJson = new JsonObject();

        configJson.addProperty("cooldownMs", config.getCooldownMs());
        configJson.addProperty("onlyInCombat", config.isOnlyInCombat());
        configJson.addProperty("onlyWhenLoggedIn", config.isOnlyWhenLoggedIn());
        configJson.addProperty("testMode", config.isTestMode());
        configJson.addProperty("debugEnabled", config.isDebugEnabled());

        // HP-specific
        configJson.addProperty("hpThreshold", config.getHpThreshold());
        configJson.addProperty("hpIsPercentage", config.isHpIsPercentage());
        configJson.addProperty("hpTargetType", config.getHpTargetType().name());
        configJson.addProperty("hpThresholdType", config.getHpThresholdType().name());
        configJson.addProperty("minConsecutiveTicks", config.getMinConsecutiveTicks());

        // Special attack
        configJson.addProperty("requireSpecialAttack", config.isRequireSpecialAttack());
        configJson.addProperty("specialAttackThreshold", config.getSpecialAttackThreshold());

        // Distance
        configJson.addProperty("maxDistance", config.getMaxDistance());

        // Animation-specific
        configJson.addProperty("animationId", config.getAnimationId());
        configJson.addProperty("targetFilter", config.getTargetFilterValue());

        // XP-specific
        configJson.addProperty("xpThreshold", config.getXpThreshold());
        configJson.addProperty("maxXpThreshold", config.getMaxXpThreshold());
        configJson.addProperty("skillFilter", config.getSkillFilter());

        // Damage-specific
        configJson.addProperty("damageThreshold", config.getDamageThreshold());
        configJson.addProperty("damageType", config.getDamageType());

        // Status-specific
        configJson.addProperty("statusThreshold", config.getStatusThreshold());

        configJson.addProperty("areaName", config.getAreaName());
        configJson.addProperty("x1", config.getX1());
        configJson.addProperty("y1", config.getY1());
        configJson.addProperty("x2", config.getX2());
        configJson.addProperty("y2", config.getY2());

        // Player Spawned-specific
        configJson.addProperty("playerSpawnedRadius", config.getPlayerSpawnedRadius());
        configJson.addProperty("playerSpawnedNoTarget", config.isPlayerSpawnedNoTarget());
        configJson.addProperty("playerSpawnedSetTarget", config.isPlayerSpawnedSetTarget());
        configJson.addProperty("playerSpawnedIgnoreFriends", config.isPlayerSpawnedIgnoreFriends());
        configJson.addProperty("playerSpawnedAttackableOnly", config.isPlayerSpawnedAttackableOnly());

        return configJson;
    }

    /**
     * Deserialize trigger configuration
     */
    private static TriggerConfig deserializeConfig(JsonObject configJson) {
        TriggerConfig config = new TriggerConfig();

        if (configJson.has("cooldownMs"))
            config.setCooldownMs(configJson.get("cooldownMs").getAsLong());

        if (configJson.has("onlyInCombat"))
            config.setOnlyInCombat(configJson.get("onlyInCombat").getAsBoolean());

        if (configJson.has("onlyWhenLoggedIn"))
            config.setOnlyWhenLoggedIn(configJson.get("onlyWhenLoggedIn").getAsBoolean());

        if (configJson.has("testMode"))
            config.setTestMode(configJson.get("testMode").getAsBoolean());

        if (configJson.has("debugEnabled"))
            config.setDebugEnabled(configJson.get("debugEnabled").getAsBoolean());

        // Animation-specific
        if (configJson.has("animationId"))
            config.setAnimationId(configJson.get("animationId").getAsInt());

        if (configJson.has("targetFilter"))
            config.setTargetFilterByValue(configJson.get("targetFilter").getAsString());

        // HP-specific
        if (configJson.has("hpThreshold"))
            config.setHpThreshold(configJson.get("hpThreshold").getAsDouble());

        if (configJson.has("hpIsPercentage"))
            config.setHpIsPercentage(configJson.get("hpIsPercentage").getAsBoolean());

        if (configJson.has("hpTargetType")) {
            try {
                config.setHpTargetType(
                        TriggerConfig.HpTargetType.valueOf(configJson.get("hpTargetType").getAsString()));
            } catch (Exception e) {
                config.setHpTargetType(TriggerConfig.HpTargetType.TARGET);
            }
        }

        if (configJson.has("hpThresholdType")) {
            try {
                config.setHpThresholdType(
                        TriggerConfig.HpThresholdType.valueOf(configJson.get("hpThresholdType").getAsString()));
            } catch (Exception e) {
                config.setHpThresholdType(TriggerConfig.HpThresholdType.BELOW);
            }
        }

        if (configJson.has("minConsecutiveTicks"))
            config.setMinConsecutiveTicks(configJson.get("minConsecutiveTicks").getAsInt());

        // Special attack
        if (configJson.has("requireSpecialAttack"))
            config.setRequireSpecialAttack(configJson.get("requireSpecialAttack").getAsBoolean());

        if (configJson.has("specialAttackThreshold"))
            config.setSpecialAttackThreshold(configJson.get("specialAttackThreshold").getAsInt());

        // Distance
        if (configJson.has("maxDistance"))
            config.setMaxDistance(configJson.get("maxDistance").getAsInt());

        // XP-specific
        if (configJson.has("xpThreshold"))
            config.setXpThreshold(configJson.get("xpThreshold").getAsInt());

        if (configJson.has("maxXpThreshold"))
            config.setMaxXpThreshold(configJson.get("maxXpThreshold").getAsInt());

        if (configJson.has("skillFilter"))
            config.setSkillFilter(configJson.get("skillFilter").getAsString());

        // Damage-specific
        if (configJson.has("damageThreshold"))
            config.setDamageThreshold(configJson.get("damageThreshold").getAsInt());

        if (configJson.has("damageType"))
            config.setDamageType(configJson.get("damageType").getAsString());

        // Status-specific
        if (configJson.has("statusThreshold"))
            config.setStatusThreshold(configJson.get("statusThreshold").getAsDouble());

        // Location-specific
        if (configJson.has("areaName"))
            config.setAreaName(configJson.get("areaName").getAsString());

        if (configJson.has("x1"))
            config.setX1(configJson.get("x1").getAsInt());

        if (configJson.has("y1"))
            config.setY1(configJson.get("y1").getAsInt());

        if (configJson.has("x2"))
            config.setX2(configJson.get("x2").getAsInt());

        if (configJson.has("y2"))
            config.setY2(configJson.get("y2").getAsInt());

        // Player Spawned-specific
        if (configJson.has("playerSpawnedRadius"))
            config.setPlayerSpawnedRadius(configJson.get("playerSpawnedRadius").getAsInt());

        if (configJson.has("playerSpawnedNoTarget"))
            config.setPlayerSpawnedNoTarget(configJson.get("playerSpawnedNoTarget").getAsBoolean());

        if (configJson.has("playerSpawnedSetTarget"))
            config.setPlayerSpawnedSetTarget(configJson.get("playerSpawnedSetTarget").getAsBoolean());

        if (configJson.has("playerSpawnedIgnoreFriends"))
            config.setPlayerSpawnedIgnoreFriends(configJson.get("playerSpawnedIgnoreFriends").getAsBoolean());

        if (configJson.has("playerSpawnedAttackableOnly"))
            config.setPlayerSpawnedAttackableOnly(configJson.get("playerSpawnedAttackableOnly").getAsBoolean());

        return config;
    }

    /**
     * Serialize trigger action
     */
    private static JsonObject serializeAction(TriggerAction action) {
        JsonObject actionJson = new JsonObject();

        actionJson.addProperty("id", action.getId());
        actionJson.addProperty("type", action.getType().name());
        actionJson.addProperty("description", action.getDescription());
        actionJson.addProperty("enabled", action.isEnabled());

        // Action-specific data
        if (action instanceof GearSwapAction) {
            GearSwapAction gearAction = (GearSwapAction) action;
            actionJson.addProperty("gearSetName", gearAction.getGearSetName());
        } else if (action instanceof CustomAction) {
            CustomAction customAction = (CustomAction) action;
            actionJson.addProperty("customCommand", customAction.getCustomCommand());
        } else if (action instanceof com.tonic.plugins.gearswapper.triggers.actions.MoveAction) {
            com.tonic.plugins.gearswapper.triggers.actions.MoveAction moveAction = (com.tonic.plugins.gearswapper.triggers.actions.MoveAction) action;
            actionJson.addProperty("tilesToMove", moveAction.getTilesToMove());
        }

        return actionJson;
    }

    /**
     * Deserialize trigger action
     */
    private static TriggerAction deserializeAction(JsonObject actionJson) {
        try {
            String typeName = actionJson.get("type").getAsString();

            ActionType type;
            try {
                type = ActionType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                logger.error("[Trigger Serializer] Unknown action type: {}", typeName);
                return null;
            }

            TriggerAction action;

            switch (type) {
                case GEAR_SWAP:
                    String gearSetName = actionJson.get("gearSetName").getAsString();
                    action = new GearSwapAction(gearSetName);
                    break;
                case EXECUTE_COMMAND:
                    String customCommand;
                    if (actionJson.has("customCommand")) {
                        customCommand = actionJson.get("customCommand").getAsString();
                    } else {
                        // Fallback: derive from description if needed
                        String desc = actionJson.has("description") ? actionJson.get("description").getAsString() : "";
                        customCommand = desc.replaceFirst("^Execute:\\s*", "");
                    }
                    action = new CustomAction(customCommand);
                    break;
                case MOVE:
                    int tilesToMove = actionJson.has("tilesToMove") ? actionJson.get("tilesToMove").getAsInt() : 3; // Default
                                                                                                                    // to
                                                                                                                    // 3
                                                                                                                    // tiles
                    action = new com.tonic.plugins.gearswapper.triggers.actions.MoveAction(tilesToMove);
                    break;
                default:
                    logger.warn("[Trigger Serializer] Action type {} not yet implemented for deserialization", type);
                    return null;
            }

            if (actionJson.has("enabled")) {
                action.setEnabled(actionJson.get("enabled").getAsBoolean());
            }

            return action;
        } catch (Exception e) {
            logger.error("[Trigger Serializer] Error deserializing action: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Serialize trigger statistics
     */
    private static JsonObject serializeStats(TriggerStats stats) {
        JsonObject statsJson = new JsonObject();

        statsJson.addProperty("totalFired", stats.getTotalFired());
        statsJson.addProperty("lastFired", stats.getLastFired());
        statsJson.addProperty("averageFireInterval", stats.getAverageFireInterval());
        statsJson.addProperty("minFireInterval", stats.getMinFireInterval());
        statsJson.addProperty("maxFireInterval", stats.getMaxFireInterval());
        statsJson.addProperty("hasFired", stats.hasFired());

        return statsJson;
    }

    /**
     * Deserialize trigger statistics
     */
    private static TriggerStats deserializeStats(JsonObject statsJson) {
        TriggerStats stats = new TriggerStats();

        if (statsJson.has("totalFired"))
            stats.setTotalFired(statsJson.get("totalFired").getAsLong());

        if (statsJson.has("lastFired"))
            stats.setLastFired(statsJson.get("lastFired").getAsLong());

        if (statsJson.has("averageFireInterval"))
            stats.setAverageFireInterval(statsJson.get("averageFireInterval").getAsLong());

        if (statsJson.has("minFireInterval"))
            stats.setMinFireInterval(statsJson.get("minFireInterval").getAsLong());

        if (statsJson.has("maxFireInterval"))
            stats.setMaxFireInterval(statsJson.get("maxFireInterval").getAsLong());

        if (statsJson.has("hasFired"))
            stats.setHasFired(statsJson.get("hasFired").getAsBoolean());

        return stats;
    }
}
