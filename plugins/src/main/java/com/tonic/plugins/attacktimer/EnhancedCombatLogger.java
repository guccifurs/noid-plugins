/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.google.gson.Gson;
/*     */ import com.google.gson.GsonBuilder;
/*     */ import com.google.gson.reflect.TypeToken;
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import com.tonic.api.game.SkillAPI;
/*     */ import com.tonic.services.GameManager;
/*     */ import java.io.File;
/*     */ import java.io.FileWriter;
/*     */ import java.io.IOException;
/*     */ import java.lang.reflect.Type;
/*     */ import java.nio.file.Files;
/*     */ import java.nio.file.Path;
/*     */ import java.util.HashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Objects;
/*     */ import java.util.concurrent.Executors;
/*     */ import java.util.concurrent.ScheduledExecutorService;
/*     */ import java.util.concurrent.ScheduledFuture;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import java.util.concurrent.locks.ReentrantReadWriteLock;
/*     */ import net.runelite.api.Player;
/*     */ import net.runelite.api.Skill;
/*     */ import net.runelite.api.coords.WorldPoint;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class EnhancedCombatLogger
/*     */ {
/*     */   private static final String LOG_FILE_NAME = "enhanced-combat-attack-log.json";
/*  35 */   private static final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
/*     */   
/*     */   private static final long SAVE_DEBOUNCE_MS = 5000L;
/*     */   
/*     */   private final Map<String, EnhancedCombatLogData> targetLogs;
/*     */   
/*     */   private final File logFile;
/*     */   
/*     */   private final AttackTimerConfig config;
/*     */   
/*     */   private final Map<String, CombatStateTracker> stateTrackers;
/*     */   
/*     */   private String currentTargetRSN;
/*     */   
/*     */   private CombatStateTracker currentStateTracker;
/*     */   
/*     */   private final ScheduledExecutorService saveExecutor;
/*  52 */   private ScheduledFuture<?> pendingSave = null;
/*  53 */   private final ReentrantReadWriteLock saveLock = new ReentrantReadWriteLock();
/*     */   
/*     */   private volatile boolean shutdown = false;
/*     */   
/*     */   public EnhancedCombatLogger(AttackTimerConfig config) {
/*  58 */     this.config = config;
/*     */     
/*  60 */     Path vitaDir = Static.VITA_DIR;
/*  61 */     this.logFile = new File(vitaDir.toFile(), "enhanced-combat-attack-log.json");
/*     */     
/*  63 */     Map<String, EnhancedCombatLogData> loadedLogs = null;
/*  64 */     if (this.logFile.exists()) {
/*     */       
/*     */       try {
/*     */         
/*  68 */         String json = new String(Files.readAllBytes(this.logFile.toPath()));
/*  69 */         Type type = (new TypeToken<Map<String, EnhancedCombatLogData>>() {  }).getType();
/*  70 */         loadedLogs = (Map<String, EnhancedCombatLogData>)gson.fromJson(json, type);
/*  71 */         Logger.norm("[EnhancedCombatLogger] Loaded existing enhanced combat log from: " + this.logFile.getAbsolutePath());
/*     */       }
/*  73 */       catch (Exception e) {
/*     */         
/*  75 */         Logger.norm("[EnhancedCombatLogger] Failed to load existing log, creating new: " + e.getMessage());
/*     */       } 
/*     */     }
/*     */ 
/*     */     
/*  80 */     this.targetLogs = (loadedLogs != null) ? loadedLogs : new HashMap<>();
/*  81 */     this.stateTrackers = new HashMap<>();
/*     */ 
/*     */     
/*  84 */     this.saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
/*     */           Thread t = new Thread(r, "EnhancedCombatLogger-Save");
/*     */           t.setDaemon(true);
/*     */           return t;
/*     */         });
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private CombatStateTracker getOrCreateStateTracker(String targetRSN) {
/*  96 */     if (targetRSN == null || targetRSN.trim().isEmpty())
/*     */     {
/*  98 */       targetRSN = "Unknown";
/*     */     }
/*     */     
/* 101 */     if (!this.stateTrackers.containsKey(targetRSN))
/*     */     {
/* 103 */       this.stateTrackers.put(targetRSN, new CombatStateTracker());
/*     */     }
/*     */     
/* 106 */     return this.stateTrackers.get(targetRSN);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void setCurrentTarget(String targetRSN) {
/* 114 */     if (targetRSN == null || targetRSN.trim().isEmpty())
/*     */     {
/* 116 */       targetRSN = "Unknown";
/*     */     }
/*     */ 
/*     */     
/* 120 */     if (this.currentStateTracker != null && this.currentStateTracker.isFightActive())
/*     */     {
/* 122 */       endFight(this.currentTargetRSN);
/*     */     }
/*     */     
/* 125 */     this.currentTargetRSN = targetRSN;
/* 126 */     this.currentStateTracker = getOrCreateStateTracker(targetRSN);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void startFight(String targetRSN, String playerRSN) {
/* 134 */     if (targetRSN == null || targetRSN.trim().isEmpty())
/*     */     {
/* 136 */       targetRSN = "Unknown";
/*     */     }
/*     */     
/* 139 */     CombatStateTracker tracker = getOrCreateStateTracker(targetRSN);
/* 140 */     long currentTick = GameManager.getTickCount();
/*     */     
/* 142 */     tracker.startFight(currentTick);
/*     */ 
/*     */     
/* 145 */     EnhancedCombatLogData logData = this.targetLogs.computeIfAbsent(targetRSN, k -> new EnhancedCombatLogData());
/* 146 */     logData.startFight(currentTick, targetRSN, playerRSN);
/*     */     
/* 148 */     if (this.config.debug())
/*     */     {
/* 150 */       Logger.norm("[EnhancedCombatLogger] Started fight tracking for: " + targetRSN);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void endFight(String targetRSN) {
/* 159 */     if (targetRSN == null || targetRSN.trim().isEmpty())
/*     */     {
/* 161 */       targetRSN = "Unknown";
/*     */     }
/*     */     
/* 164 */     CombatStateTracker tracker = this.stateTrackers.get(targetRSN);
/* 165 */     if (tracker == null || !tracker.isFightActive()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 170 */     tracker.endFight();
/*     */ 
/*     */     
/* 173 */     EnhancedCombatLogData logData = this.targetLogs.get(targetRSN);
/* 174 */     if (logData != null && logData.getCurrentFight() != null) {
/*     */       
/* 176 */       long currentTick = GameManager.getTickCount();
/* 177 */       String winner = "unknown";
/* 178 */       logData.endFight(currentTick, winner, tracker
/* 179 */           .getKillCount(), tracker.getDeathCount(), tracker
/* 180 */           .getOpponentKillCount(), tracker.getOpponentDeathCount());
/*     */     } 
/*     */     
/* 183 */     if (this.config.debug())
/*     */     {
/* 185 */       Logger.norm("[EnhancedCombatLogger] Ended fight tracking for: " + targetRSN);
/*     */     }
/*     */ 
/*     */     
/* 189 */     scheduleAsyncSave();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void logExtendedAttack(String targetRSN, ExtendedAttackContext context) {
/* 197 */     if (targetRSN == null || targetRSN.trim().isEmpty())
/*     */     {
/* 199 */       targetRSN = "Unknown";
/*     */     }
/*     */     
/* 202 */     if (context == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 208 */     EnhancedCombatLogData logData = this.targetLogs.computeIfAbsent(targetRSN, k -> new EnhancedCombatLogData());
/*     */ 
/*     */     
/* 211 */     int maxHistorySize = (this.config != null) ? this.config.predictionHistorySize() : 5000;
/* 212 */     logData.addExtendedAttackToHistory(context, maxHistorySize);
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 217 */     boolean playerFrozen = (context.getFreezeState() != null && (context.getFreezeState().equals("bothFrozen") || context.getFreezeState().equals("weFrozenTargetUnfrozen")));
/*     */ 
/*     */     
/* 220 */     boolean opponentFrozen = (context.getFreezeState() != null && (context.getFreezeState().equals("bothFrozen") || context.getFreezeState().equals("targetFrozenWeUnfrozen")));
/*     */     
/* 222 */     String attackStyle = context.getAttackStyle();
/* 223 */     if (attackStyle != null) {
/*     */       
/* 225 */       CombatAttackStats targetStats = null;
/* 226 */       if (playerFrozen && opponentFrozen) {
/*     */         
/* 228 */         targetStats = logData.getBothFrozen();
/*     */       }
/* 230 */       else if (!playerFrozen && !opponentFrozen) {
/*     */         
/* 232 */         targetStats = logData.getBothUnfrozen();
/*     */       }
/* 234 */       else if (!playerFrozen && opponentFrozen) {
/*     */         
/* 236 */         targetStats = logData.getTargetFrozenWeUnfrozen();
/*     */       }
/* 238 */       else if (playerFrozen && !opponentFrozen) {
/*     */         
/* 240 */         targetStats = logData.getWeFrozenTargetUnfrozen();
/*     */       } 
/*     */       
/* 243 */       if (targetStats != null)
/*     */       {
/* 245 */         switch (attackStyle.toLowerCase()) {
/*     */           
/*     */           case "melee":
/* 248 */             targetStats.incrementMelee();
/*     */             break;
/*     */           case "ranged":
/* 251 */             targetStats.incrementRanged();
/*     */             break;
/*     */           case "magic":
/* 254 */             targetStats.incrementMagic();
/*     */             break;
/*     */         } 
/*     */ 
/*     */       
/*     */       }
/*     */     } 
/*     */ 
/*     */     String freezeStateKey;
/*     */     if (playerFrozen && opponentFrozen) {
/*     */       freezeStateKey = "bothFrozen";
/*     */     }
/*     */     else if (!playerFrozen && !opponentFrozen) {
/*     */       freezeStateKey = "bothUnfrozen";
/*     */     }
/*     */     else if (!playerFrozen && opponentFrozen) {
/*     */       freezeStateKey = "targetFrozenWeUnfrozen";
/*     */     }
/*     */     else {
/*     */       freezeStateKey = "weFrozenTargetUnfrozen";
/*     */     }
/*     */ 
/*     */     int distance = context.getDistance();
/*     */     if (distance > 0) {
/*     */       String distanceBand;
/*     */       if (distance <= 3) {
/*     */         distanceBand = "1-3";
/*     */       }
/*     */       else if (distance <= 7) {
/*     */         distanceBand = "4-7";
/*     */       }
/*     */       else if (distance <= 10) {
/*     */         distanceBand = "8-10";
/*     */       }
/*     */       else {
/*     */         distanceBand = ">10";
/*     */       }
/*     */ 
/*     */       int timerTicks = context.getPlayerAttackTimerTicks();
/*     */       if (timerTicks >= 0) {
/*     */         String timerBucket;
/*     */         if (timerTicks == 0) {
/*     */           timerBucket = "0";
/*     */         }
/*     */         else if (timerTicks == 1) {
/*     */           timerBucket = "1";
/*     */         }
/*     */         else if (timerTicks <= 3) {
/*     */           timerBucket = "2-3";
/*     */         }
/*     */         else if (timerTicks <= 6) {
/*     */           timerBucket = "4-6";
/*     */         }
/*     */         else if (timerTicks <= 8) {
/*     */           timerBucket = "7-8";
/*     */         }
/*     */         else {
/*     */           timerBucket = ">8";
/*     */         }
/*     */ 
/*     */         Map<String, Map<String, Map<String, CombatAttackStats>>> globalMap = logData.getFreezeStateDistanceTimerStats();
/*     */         Map<String, Map<String, CombatAttackStats>> distanceMap = globalMap.computeIfAbsent(freezeStateKey, k -> new HashMap<>());
/*     */         Map<String, CombatAttackStats> timerMap = distanceMap.computeIfAbsent(distanceBand, k -> new HashMap<>());
/*     */         CombatAttackStats bucketStats = timerMap.computeIfAbsent(timerBucket, k -> new CombatAttackStats());
/*     */ 
/*     */         switch (attackStyle.toLowerCase()) {
/*     */           case "melee":
/*     */             bucketStats.incrementMelee();
/*     */             break;
/*     */           case "ranged":
/*     */             bucketStats.incrementRanged();
/*     */             break;
/*     */           case "magic":
/*     */             bucketStats.incrementMagic();
/*     */             break;
/*     */         }
/*     */       }
/*     */     }
/* 261 */     scheduleAsyncSave();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public ExtendedAttackContext createExtendedContext(Player opponent, Player localPlayer, String attackStyle, boolean playerFrozen, boolean opponentFrozen, AttackContext baseContext) {
/* 271 */     if (opponent == null || baseContext == null)
/*     */     {
/* 273 */       return null;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/* 279 */       ExtendedAttackContext extendedContext = new ExtendedAttackContext(baseContext);
/*     */ 
/*     */       
/* 282 */       CombatStateTracker tracker = this.currentStateTracker;
/* 283 */       if (tracker == null)
/*     */       {
/* 285 */         tracker = getOrCreateStateTracker(this.currentTargetRSN);
/*     */       }
/*     */ 
/*     */       
/* 289 */       extendedContext.setPlayerLastAttack(tracker.getPlayerLastAttack());
/* 290 */       extendedContext.setPlayerLastWeapon(tracker.getPlayerLastWeapon());
/* 291 */       extendedContext.setPlayerLastPrayer(tracker.getPlayerLastPrayer());
/* 292 */       extendedContext.setPlayerLastMovement(tracker.isPlayerLastMovement());
/* 293 */       extendedContext.setPlayerLastFood(tracker.isPlayerLastFood());
/* 294 */       extendedContext.setPlayerLastBrew(tracker.isPlayerLastBrew());
/* 295 */       extendedContext.setPlayerLastSpecial(tracker.isPlayerLastSpecial());
/* 296 */       extendedContext.setPlayerLastFreeze(tracker.isPlayerLastFreeze());
/* 297 */       extendedContext.setPlayerLastDamageDealt(tracker.getPlayerLastDamageDealt());
/* 298 */       extendedContext.setPlayerLastDamageReceived(tracker.getPlayerLastDamageReceived());
/*     */ 
/*     */       
/* 301 */       extendedContext.setOpponentLastAttack(tracker.getOpponentLastAttack());
/* 302 */       extendedContext.setOpponentLastWeapon(tracker.getOpponentLastWeapon());
/* 303 */       extendedContext.setOpponentLastPrayer(tracker.getOpponentLastPrayer());
/* 304 */       extendedContext.setOpponentLastMovement(tracker.isOpponentLastMovement());
/* 305 */       extendedContext.setOpponentLastFood(tracker.isOpponentLastFood());
/* 306 */       extendedContext.setOpponentLastBrew(tracker.isOpponentLastBrew());
/* 307 */       extendedContext.setOpponentLastSpecial(tracker.isOpponentLastSpecial());
/* 308 */       extendedContext.setOpponentLastFreeze(tracker.isOpponentLastFreeze());
/* 309 */       extendedContext.setOpponentLastDamageDealt(tracker.getOpponentLastDamageDealt());
/* 310 */       extendedContext.setOpponentLastDamageReceived(tracker.getOpponentLastDamageReceived());
/*     */ 
/*     */ 
/*     */       
/* 314 */       extendedContext.getAttackSequence().addAll(tracker.getAttackSequence());
/* 315 */       extendedContext.getAttackIntervals().addAll(tracker.getAttackIntervals());
/* 316 */       extendedContext.getFreezeHistory().addAll(tracker.getFreezeHistory());
/* 317 */       extendedContext.getSpecHistory().addAll(tracker.getSpecHistory());
/* 318 */       extendedContext.getWeaponSwitchHistory().addAll(tracker.getWeaponSwitchHistory());
/* 319 */       extendedContext.getPrayerSwitchHistory().addAll(tracker.getPrayerSwitchHistory());
/* 320 */       extendedContext.getMovementHistory().addAll(tracker.getMovementHistory());
/*     */ 
/*     */       
/* 323 */       extendedContext.setCombatDuration(tracker.getCombatDuration());
/* 324 */       int playerHP = (localPlayer != null) ? SkillAPI.getBoostedLevel(Skill.HITPOINTS) : -1;
/* 325 */       int opponentHP = getTargetHPPercentage(opponent);
/* 326 */       extendedContext.setFightPhase(ExtendedAttackContext.determineFightPhase(tracker
/* 327 */             .getCombatDuration(), playerHP, opponentHP));
/* 328 */       extendedContext.setDamageDealtTotal(tracker.getDamageDealtTotal());
/* 329 */       extendedContext.setDamageReceivedTotal(tracker.getDamageReceivedTotal());
/* 330 */       extendedContext.setKillCount(tracker.getKillCount());
/* 331 */       extendedContext.setDeathCount(tracker.getDeathCount());
/* 332 */       extendedContext.setOpponentKillCount(tracker.getOpponentKillCount());
/* 333 */       extendedContext.setOpponentDeathCount(tracker.getOpponentDeathCount());
/*     */ 
/*     */       
/* 336 */       if (localPlayer != null) {
/*     */         
/* 338 */         Objects.requireNonNull(localPlayer); WorldPoint playerTile = (WorldPoint)Static.invoke(localPlayer::getWorldLocation);
/* 339 */         Objects.requireNonNull(opponent); WorldPoint opponentTile = (WorldPoint)Static.invoke(opponent::getWorldLocation);
/*     */         
/* 341 */         extendedContext.setPlayerTile(playerTile);
/* 342 */         extendedContext.setOpponentTile(opponentTile);
/* 343 */         extendedContext.setDistanceChange(tracker.calculateDistanceChange());
/* 344 */         extendedContext.setMovementDirection(ExtendedAttackContext.calculateMovementDirection(tracker
/* 345 */               .getPlayerLastTile(), tracker.getPlayerCurrentTile()));
/* 346 */         extendedContext.setUnderTarget(tracker.isPlayerUnderTarget(localPlayer, opponent));
/* 347 */         extendedContext.setTargetUnderUs(tracker.isTargetUnderPlayer(localPlayer, opponent));
/* 348 */         extendedContext.setLastMovementTick(tracker.getPlayerLastMovementTick());
/*     */       } 
/*     */ 
/*     */       
/* 352 */       extendedContext.setOpponentFoodCount(tracker.getOpponentFoodCount());
/* 353 */       extendedContext.setOpponentBrewCount(tracker.getOpponentBrewCount());
/* 354 */       extendedContext.setOpponentPrayerPoints(tracker.getOpponentPrayerPoints());
/* 355 */       extendedContext.setPlayerFoodCount(tracker.getPlayerFoodCount());
/* 356 */       extendedContext.setPlayerBrewCount(tracker.getPlayerBrewCount());
/* 357 */       extendedContext.setPlayerPrayerPoints(SkillAPI.getBoostedLevel(Skill.PRAYER));
/*     */ 
/*     */ 
/*     */       
/* 361 */       if (!tracker.getAttackSequence().isEmpty()) {
/*     */         
/* 363 */         StringBuilder patternBuilder = new StringBuilder();
/* 364 */         List<String> sequence = tracker.getAttackSequence();
/* 365 */         int start = Math.max(0, sequence.size() - 5);
/* 366 */         for (int i = start; i < sequence.size(); i++) {
/*     */           
/* 368 */           if (i > start)
/*     */           {
/* 370 */             patternBuilder.append("->");
/*     */           }
/* 372 */           patternBuilder.append(sequence.get(i));
/*     */         } 
/* 374 */         extendedContext.setRecentPattern(patternBuilder.toString());
/*     */       } 
/*     */ 
/*     */ 
/*     */ 
/*     */       
/* 380 */       return extendedContext;
/*     */     }
/* 382 */     catch (Exception e) {
/*     */       
/* 384 */       Logger.norm("[EnhancedCombatLogger] Failed to create extended context: " + e.getMessage());
/* 385 */       return null;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private int getTargetHPPercentage(Player target) {
/* 395 */     if (target == null)
/*     */     {
/* 397 */       return -1;
/*     */     }
/*     */     
/* 400 */     return ((Integer)Static.invoke(() -> {
/*     */           Objects.requireNonNull(target); int healthRatio = ((Integer)Static.invoke(target::getHealthRatio)).intValue();
/*     */           
/*     */           Objects.requireNonNull(target); int healthScale = ((Integer)Static.invoke(target::getHealthScale)).intValue();
/* 404 */           return (healthRatio == -1 || healthScale == -1 || healthScale == 0) ? Integer.valueOf(-1) : Integer.valueOf(healthRatio * 100 / healthScale);
/*     */         })).intValue();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerAction(String actionType, String value, long currentTick) {
/* 419 */     if (this.currentStateTracker == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 424 */     switch (actionType) {
/*     */       
/*     */       case "attack":
/* 427 */         this.currentStateTracker.updatePlayerAttack(value, null, currentTick);
/*     */         break;
/*     */       case "weapon":
/* 430 */         this.currentStateTracker.updatePlayerWeaponSwitch(value, currentTick);
/*     */         break;
/*     */       case "prayer":
/* 433 */         this.currentStateTracker.updatePlayerPrayer(value, currentTick);
/*     */         break;
/*     */ 
/*     */ 
/*     */ 
/*     */       
/*     */       case "food":
/* 440 */         this.currentStateTracker.updatePlayerFood(currentTick);
/*     */         break;
/*     */       case "brew":
/* 443 */         this.currentStateTracker.updatePlayerBrew(currentTick);
/*     */         break;
/*     */       case "special":
/* 446 */         this.currentStateTracker.updatePlayerSpecial(currentTick);
/*     */         break;
/*     */       case "freeze":
/* 449 */         this.currentStateTracker.updatePlayerFreeze(currentTick);
/*     */         break;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentAction(String actionType, String value, long currentTick) {
/* 459 */     if (this.currentStateTracker == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 464 */     switch (actionType) {
/*     */       
/*     */       case "attack":
/* 467 */         this.currentStateTracker.updateOpponentAttack(value, null, currentTick);
/*     */         break;
/*     */ 
/*     */ 
/*     */       
/*     */       case "prayer":
/* 473 */         this.currentStateTracker.updateOpponentPrayer(value);
/*     */         break;
/*     */ 
/*     */ 
/*     */       
/*     */       case "food":
/* 479 */         this.currentStateTracker.updateOpponentFood(currentTick);
/*     */         break;
/*     */       case "brew":
/* 482 */         this.currentStateTracker.updateOpponentBrew(currentTick);
/*     */         break;
/*     */       case "special":
/* 485 */         this.currentStateTracker.updateOpponentSpecial(currentTick);
/*     */         break;
/*     */       case "freeze":
/* 488 */         this.currentStateTracker.updateOpponentFreeze(currentTick);
/*     */         break;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateDamage(int damageDealt, int damageReceived) {
/* 498 */     if (this.currentStateTracker == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 503 */     if (damageDealt > 0)
/*     */     {
/* 505 */       this.currentStateTracker.updateDamageDealt(damageDealt);
/*     */     }
/* 507 */     if (damageReceived > 0)
/*     */     {
/* 509 */       this.currentStateTracker.updateDamageReceived(damageReceived);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentDamage(int damageDealt, int damageReceived) {
/* 518 */     if (this.currentStateTracker == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 523 */     if (damageDealt > 0)
/*     */     {
/* 525 */       this.currentStateTracker.updateOpponentDamageDealt(damageDealt);
/*     */     }
/* 527 */     if (damageReceived > 0)
/*     */     {
/* 529 */       this.currentStateTracker.updateOpponentDamageReceived(damageReceived);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerMovement(WorldPoint currentTile, long currentTick) {
/* 538 */     if (this.currentStateTracker == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 543 */     this.currentStateTracker.updatePlayerMovement(currentTile, currentTick);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentMovement(WorldPoint currentTile, long currentTick) {
/* 551 */     if (this.currentStateTracker == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 556 */     this.currentStateTracker.updateOpponentMovement(currentTile, currentTick);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void clearActionFlags() {
/* 564 */     if (this.currentStateTracker != null)
/*     */     {
/* 566 */       this.currentStateTracker.clearActionFlags();
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void scheduleAsyncSave() {
/* 575 */     if (this.shutdown) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 580 */     this.saveLock.writeLock().lock();
/*     */     
/*     */     try {
/* 583 */       if (this.pendingSave != null && !this.pendingSave.isDone())
/*     */       {
/* 585 */         this.pendingSave.cancel(false);
/*     */       }
/*     */       
/* 588 */       this.pendingSave = this.saveExecutor.schedule(() -> saveToFileAsync(), 5000L, TimeUnit.MILLISECONDS);
/*     */     
/*     */     }
/*     */     finally {
/*     */ 
/*     */       
/* 594 */       this.saveLock.writeLock().unlock();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void saveToFileAsync() {
/*     */     Map<String, EnhancedCombatLogData> dataToSave;
/* 604 */     this.saveLock.readLock().lock();
/*     */     
/*     */     try {
/* 607 */       dataToSave = new HashMap<>(this.targetLogs);
/*     */     }
/*     */     finally {
/*     */       
/* 611 */       this.saveLock.readLock().unlock();
/*     */     } 
/*     */ 
/*     */     
/*     */     try {
/* 616 */       File parentDir = this.logFile.getParentFile();
/* 617 */       if (parentDir != null && !parentDir.exists())
/*     */       {
/* 619 */         parentDir.mkdirs();
/*     */       }
/*     */       
/* 622 */       FileWriter writer = new FileWriter(this.logFile, false);
/*     */       
/* 624 */       try { gson.toJson(dataToSave, writer);
/* 625 */         writer.flush();
/* 626 */         writer.close(); } catch (Throwable throwable) { try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; }
/*     */     
/* 628 */     } catch (IOException e) {
/*     */       
/* 630 */       Logger.norm("[EnhancedCombatLogger] Failed to save enhanced combat log: " + e.getMessage());
/*     */     }
/* 632 */     catch (Exception e) {
/*     */       
/* 634 */       Logger.norm("[EnhancedCombatLogger] Error in async save: " + e.getMessage());
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public EnhancedCombatLogData getLogDataForTarget(String targetRSN) {
/* 643 */     return this.targetLogs.get(targetRSN);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public Map<String, EnhancedCombatLogData> getAllLogs() {
/* 651 */     return new HashMap<>(this.targetLogs);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public CombatStateTracker getCurrentStateTracker() {
/* 659 */     return this.currentStateTracker;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void reset() {
/* 667 */     this.saveLock.writeLock().lock();
/*     */     
/*     */     try {
/* 670 */       this.targetLogs.clear();
/* 671 */       this.stateTrackers.clear();
/* 672 */       this.currentTargetRSN = null;
/* 673 */       this.currentStateTracker = null;
/*     */ 
/*     */       
/* 676 */       File parentDir = this.logFile.getParentFile();
/* 677 */       if (parentDir != null && !parentDir.exists())
/*     */       {
/* 679 */         parentDir.mkdirs();
/*     */       }
/*     */       
/* 682 */       FileWriter writer = new FileWriter(this.logFile, false);
/*     */       
/* 684 */       try { gson.toJson(this.targetLogs, writer);
/* 685 */         writer.flush();
/* 686 */         writer.close(); } catch (Throwable throwable) { try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }
/*     */          throw throwable; }
/* 688 */        Logger.norm("[EnhancedCombatLogger] Reset all enhanced combat statistics");
/*     */     }
/* 690 */     catch (IOException e) {
/*     */       
/* 692 */       Logger.norm("[EnhancedCombatLogger] Failed to save after reset: " + e.getMessage());
/*     */     }
/*     */     finally {
/*     */       
/* 696 */       this.saveLock.writeLock().unlock();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void resetTarget(String targetRSN) {
/* 705 */     if (targetRSN == null || targetRSN.trim().isEmpty())
/*     */     {
/* 707 */       targetRSN = "Unknown";
/*     */     }
/*     */     
/* 710 */     this.saveLock.writeLock().lock();
/*     */     
/*     */     try {
/* 713 */       if (this.targetLogs.containsKey(targetRSN))
/*     */       {
/* 715 */         this.targetLogs.remove(targetRSN);
/* 716 */         this.stateTrackers.remove(targetRSN);
/*     */         
/* 718 */         if (targetRSN.equals(this.currentTargetRSN)) {
/*     */           
/* 720 */           this.currentTargetRSN = null;
/* 721 */           this.currentStateTracker = null;
/*     */         } 
/*     */ 
/*     */         
/* 725 */         File parentDir = this.logFile.getParentFile();
/* 726 */         if (parentDir != null && !parentDir.exists())
/*     */         {
/* 728 */           parentDir.mkdirs();
/*     */         }
/*     */         
/* 731 */         FileWriter writer = new FileWriter(this.logFile, false);
/*     */         
/* 733 */         try { gson.toJson(this.targetLogs, writer);
/* 734 */           writer.flush();
/* 735 */           writer.close(); } catch (Throwable throwable) { try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }
/*     */            throw throwable; }
/* 737 */          Logger.norm("[EnhancedCombatLogger] Reset enhanced combat statistics for: " + targetRSN);
/*     */       }
/*     */     
/* 740 */     } catch (IOException e) {
/*     */       
/* 742 */       Logger.norm("[EnhancedCombatLogger] Failed to save after reset: " + e.getMessage());
/*     */     }
/*     */     finally {
/*     */       
/* 746 */       this.saveLock.writeLock().unlock();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void shutdown() {
/* 755 */     this.shutdown = true;
/*     */     
/* 757 */     this.saveLock.writeLock().lock();
/*     */     
/*     */     try {
/* 760 */       if (this.pendingSave != null && !this.pendingSave.isDone())
/*     */       {
/* 762 */         this.pendingSave.cancel(false);
/*     */       }
/*     */     }
/*     */     finally {
/*     */       
/* 767 */       this.saveLock.writeLock().unlock();
/*     */     } 
/*     */ 
/*     */     
/* 771 */     this.saveLock.readLock().lock();
/*     */     
/*     */     try {
/* 774 */       File parentDir = this.logFile.getParentFile();
/* 775 */       if (parentDir != null && !parentDir.exists())
/*     */       {
/* 777 */         parentDir.mkdirs();
/*     */       }
/*     */       
/* 780 */       FileWriter writer = new FileWriter(this.logFile, false);
/*     */       
/* 782 */       try { gson.toJson(this.targetLogs, writer);
/* 783 */         writer.flush();
/* 784 */         writer.close(); } catch (Throwable throwable) { try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; }
/*     */     
/* 786 */     } catch (IOException e) {
/*     */       
/* 788 */       Logger.norm("[EnhancedCombatLogger] Failed to save on shutdown: " + e.getMessage());
/*     */     }
/*     */     finally {
/*     */       
/* 792 */       this.saveLock.readLock().unlock();
/*     */     } 
/*     */     
/* 795 */     if (this.saveExecutor != null) {
/*     */       
/* 797 */       this.saveExecutor.shutdown();
/*     */       
/*     */       try {
/* 800 */         if (!this.saveExecutor.awaitTermination(2L, TimeUnit.SECONDS))
/*     */         {
/* 802 */           this.saveExecutor.shutdownNow();
/*     */         }
/*     */       }
/* 805 */       catch (InterruptedException e) {
/*     */         
/* 807 */         this.saveExecutor.shutdownNow();
/* 808 */         Thread.currentThread().interrupt();
/*     */       } 
/*     */     } 
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/EnhancedCombatLogger.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */