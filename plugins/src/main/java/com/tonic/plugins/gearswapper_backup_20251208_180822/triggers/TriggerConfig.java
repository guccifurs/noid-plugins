package com.tonic.plugins.gearswapper.triggers;

/**
 * Configuration for a trigger with validation and type safety
 */
public class TriggerConfig {
    // HP Target Type enum
    public enum HpTargetType {
        TARGET("Target"),
        PLAYER("Player");

        private final String displayName;

        HpTargetType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // HP Threshold Type enum
    public enum HpThresholdType {
        ABOVE("Above"),
        BELOW("Below");

        private final String displayName;

        HpThresholdType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Core configuration with safe defaults
    private long cooldownMs = 1000; // 1 second default cooldown
    private boolean onlyInCombat = false;
    private boolean onlyWhenLoggedIn = true;
    private boolean testMode = false;

    // Animation-specific config
    private int animationId = -1;
    private TargetFilter targetFilter = TargetFilter.CURRENT;

    // HP-specific config
    private double hpThreshold = 50.0; // 50% default
    private boolean hpIsPercentage = false; // Changed to false for whole numbers
    private HpTargetType hpTargetType = HpTargetType.TARGET; // Target vs Player
    private HpThresholdType hpThresholdType = HpThresholdType.BELOW; // Above vs Below
    private int minConsecutiveTicks = 1;

    // Special attack requirement config
    private boolean requireSpecialAttack = false;
    private int specialAttackThreshold = 50; // 50% default

    // Distance check config
    private int maxDistance = 10; // 10 tiles default
    private boolean debugEnabled = true;

    // XP-specific config (future)
    private int xpThreshold = 0;
    private int maxXpThreshold = Integer.MAX_VALUE;
    private String skillFilter = "any";

    // Damage-specific config (future)
    private int damageThreshold = 0;
    private String damageType = "any";

    // Status-specific config (future)
    private double statusThreshold = 0.0;

    // Location-specific config (future)
    private String areaName = "";
    private int x1 = -1, y1 = -1, x2 = -1, y2 = -1;

    // Player Spawned Config
    private int playerSpawnedRadius = 0; // 0-15, 0=anywhere
    private boolean playerSpawnedNoTarget = false; // Only trigger if no current target
    private boolean playerSpawnedSetTarget = false; // Set spawned player as target
    private boolean playerSpawnedIgnoreFriends = false; // Ignore friends/clan
    private boolean playerSpawnedAttackableOnly = false; // Only attackable players

    /**
     * Target filter enum for type safety
     */
    public enum TargetFilter {
        CURRENT("current"),
        ANY("any"),
        LOCAL("local"),
        SPECIFIC("specific");

        private final String value;

        TargetFilter(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static TargetFilter fromValue(String value) {
            for (TargetFilter filter : values()) {
                if (filter.value.equals(value)) {
                    return filter;
                }
            }
            return CURRENT; // Safe default
        }
    }

    // Getters and setters with validation
    public long getCooldownMs() {
        return cooldownMs;
    }

    public void setCooldownMs(long cooldownMs) {
        this.cooldownMs = Math.max(0, Math.min(cooldownMs, 60000)); // 0-60 seconds
    }

    public boolean isOnlyInCombat() {
        return onlyInCombat;
    }

    public void setOnlyInCombat(boolean onlyInCombat) {
        this.onlyInCombat = onlyInCombat;
    }

    public boolean isOnlyWhenLoggedIn() {
        return onlyWhenLoggedIn;
    }

    public void setOnlyWhenLoggedIn(boolean onlyWhenLoggedIn) {
        this.onlyWhenLoggedIn = onlyWhenLoggedIn;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public int getAnimationId() {
        return animationId;
    }

    public void setAnimationId(int animationId) {
        this.animationId = Math.max(-1, animationId); // -1 = disabled
    }

    public TargetFilter getTargetFilter() {
        return targetFilter;
    }

    public void setTargetFilter(TargetFilter targetFilter) {
        this.targetFilter = targetFilter != null ? targetFilter : TargetFilter.CURRENT;
    }

    public String getTargetFilterValue() {
        return targetFilter.getValue();
    }

    public void setTargetFilterByValue(String value) {
        this.targetFilter = TargetFilter.fromValue(value);
    }

    public double getHpThreshold() {
        return hpThreshold;
    }

    public void setHpThreshold(double hpThreshold) {
        if (hpIsPercentage) {
            this.hpThreshold = Math.max(0.0, Math.min(hpThreshold, 100.0)); // 0-100%
        } else {
            this.hpThreshold = Math.max(0.0, hpThreshold); // >= 0 HP
        }
    }

    public boolean isHpIsPercentage() {
        return hpIsPercentage;
    }

    public void setHpIsPercentage(boolean hpIsPercentage) {
        this.hpIsPercentage = hpIsPercentage;
    }

    public HpTargetType getHpTargetType() {
        return hpTargetType;
    }

    public void setHpTargetType(HpTargetType hpTargetType) {
        this.hpTargetType = hpTargetType != null ? hpTargetType : HpTargetType.TARGET;
    }

    public HpThresholdType getHpThresholdType() {
        return hpThresholdType;
    }

    public void setHpThresholdType(HpThresholdType hpThresholdType) {
        this.hpThresholdType = hpThresholdType != null ? hpThresholdType : HpThresholdType.BELOW;
    }

    public int getMinConsecutiveTicks() {
        return minConsecutiveTicks;
    }

    public void setMinConsecutiveTicks(int minConsecutiveTicks) {
        this.minConsecutiveTicks = Math.max(1, minConsecutiveTicks);
    }

    // Special attack requirement getters/setters
    public boolean isRequireSpecialAttack() {
        return requireSpecialAttack;
    }

    public void setRequireSpecialAttack(boolean requireSpecialAttack) {
        this.requireSpecialAttack = requireSpecialAttack;
    }

    public int getSpecialAttackThreshold() {
        return specialAttackThreshold;
    }

    public void setSpecialAttackThreshold(int specialAttackThreshold) {
        this.specialAttackThreshold = Math.max(0, Math.min(100, specialAttackThreshold)); // 0-100%
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = Math.max(0, maxDistance); // Non-negative distance
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public int getXpThreshold() {
        return xpThreshold;
    }

    public void setXpThreshold(int xpThreshold) {
        this.xpThreshold = Math.max(0, xpThreshold);
    }

    public int getMaxXpThreshold() {
        return maxXpThreshold;
    }

    public void setMaxXpThreshold(int maxXpThreshold) {
        // 0 or negative means "no upper limit"
        this.maxXpThreshold = maxXpThreshold <= 0 ? Integer.MAX_VALUE : Math.max(0, maxXpThreshold);
    }

    public String getSkillFilter() {
        return skillFilter;
    }

    public void setSkillFilter(String skillFilter) {
        this.skillFilter = skillFilter != null ? skillFilter : "any";
    }

    public int getDamageThreshold() {
        return damageThreshold;
    }

    public void setDamageThreshold(int damageThreshold) {
        this.damageThreshold = Math.max(0, damageThreshold);
    }

    public String getDamageType() {
        return damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType != null ? damageType : "any";
    }

    public double getStatusThreshold() {
        return statusThreshold;
    }

    public void setStatusThreshold(double statusThreshold) {
        this.statusThreshold = Math.max(0.0, statusThreshold);
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName != null ? areaName : "";
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = Math.max(-1, x1);
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = Math.max(-1, y1);
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = Math.max(-1, x2);
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = Math.max(-1, y2);
    }

    // Player Spawned getters/setters
    public int getPlayerSpawnedRadius() {
        return playerSpawnedRadius;
    }

    public void setPlayerSpawnedRadius(int radius) {
        this.playerSpawnedRadius = Math.max(0, Math.min(15, radius));
    }

    public boolean isPlayerSpawnedNoTarget() {
        return playerSpawnedNoTarget;
    }

    public void setPlayerSpawnedNoTarget(boolean noTarget) {
        this.playerSpawnedNoTarget = noTarget;
    }

    public boolean isPlayerSpawnedSetTarget() {
        return playerSpawnedSetTarget;
    }

    public void setPlayerSpawnedSetTarget(boolean setTarget) {
        this.playerSpawnedSetTarget = setTarget;
    }

    public boolean isPlayerSpawnedIgnoreFriends() {
        return playerSpawnedIgnoreFriends;
    }

    public void setPlayerSpawnedIgnoreFriends(boolean ignoreFriends) {
        this.playerSpawnedIgnoreFriends = ignoreFriends;
    }

    public boolean isPlayerSpawnedAttackableOnly() {
        return playerSpawnedAttackableOnly;
    }

    public void setPlayerSpawnedAttackableOnly(boolean attackableOnly) {
        this.playerSpawnedAttackableOnly = attackableOnly;
    }

    /**
     * Validate configuration consistency
     */
    public boolean isValid() {
        // Core validation
        if (cooldownMs < 0 || cooldownMs > 60000)
            return false;

        // Animation validation
        if (animationId < -1)
            return false;

        // HP validation
        if (hpIsPercentage && (hpThreshold < 0.0 || hpThreshold > 100.0))
            return false;
        if (!hpIsPercentage && hpThreshold < 0.0)
            return false;

        // Location validation (if specified)
        if (x1 != -1 && y1 != -1 && x2 != -1 && y2 != -1) {
            if (x1 > x2 || y1 > y2)
                return false; // Invalid rectangle
        }

        return true;
    }

    /**
     * Get validation error message
     */
    public String getValidationError() {
        if (cooldownMs < 0 || cooldownMs > 60000)
            return "Cooldown must be between 0 and 60000 milliseconds";

        if (animationId < -1)
            return "Animation ID must be -1 (disabled) or positive";

        if (hpIsPercentage && (hpThreshold < 0.0 || hpThreshold > 100.0))
            return "HP percentage must be between 0 and 100";

        if (!hpIsPercentage && hpThreshold < 0.0)
            return "HP threshold must be positive";

        if (x1 != -1 && y1 != -1 && x2 != -1 && y2 != -1) {
            if (x1 > x2 || y1 > y2)
                return "Invalid area coordinates (x1 <= x2 and y1 <= y2 required)";
        }

        return null; // No error
    }
}
