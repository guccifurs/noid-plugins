/*     */ package com.tonic.plugins.attacktimer;
/*     */ import com.google.gson.Gson;
/*     */ import com.google.gson.GsonBuilder;
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import com.tonic.api.game.SkillAPI;
/*     */ import com.tonic.api.game.VarAPI;
/*     */ import com.tonic.services.GameManager;
/*     */ import java.io.File;
/*     */ import java.io.FileWriter;
/*     */ import java.io.IOException;
/*     */ import java.nio.file.Path;
/*     */ import java.util.ArrayList;
/*     */ import java.util.HashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Objects;
/*     */ import net.runelite.api.Actor;
/*     */ import net.runelite.api.Client;
/*     */ import net.runelite.api.HeadIcon;
/*     */ import net.runelite.api.Player;
/*     */ import net.runelite.api.PlayerComposition;
/*     */ import net.runelite.api.Skill;
/*     */ import net.runelite.api.coords.WorldPoint;
/*     */ import net.runelite.api.kit.KitType;
/*     */ 
/*     */ public class TickDataLogger {
/*     */   private static final String COMBAT_LOGS_FOLDER = "CombatLogs";
/*  28 */   private static final Gson gson = (new GsonBuilder())
/*  29 */     .setPrettyPrinting()
/*  30 */     .serializeNulls()
/*  31 */     .create();
/*     */ 
/*     */   
/*     */   private final AttackTimerConfig config;
/*     */ 
/*     */   
/*     */   private final File combatLogsDir;
/*     */   
/*     */   private String currentTargetRSN;
/*     */   
/*     */   private final Map<String, List<TickData>> tickDataMap;
/*     */   
/*     */   private TickData lastTickData;
/*     */ 
/*     */   
/*     */   public TickDataLogger(AttackTimerConfig config) {
/*  47 */     this.config = config;
/*  48 */     Path vitaDir = Static.VITA_DIR;
/*  49 */     this.combatLogsDir = new File(vitaDir.toFile(), "CombatLogs");
/*     */ 
/*     */     
/*  52 */     if (!this.combatLogsDir.exists()) {
/*     */       
/*  54 */       this.combatLogsDir.mkdirs();
/*  55 */       if (config.debug())
/*     */       {
/*  57 */         Logger.norm("[TickDataLogger] Created CombatLogs directory: " + this.combatLogsDir.getAbsolutePath());
/*     */       }
/*     */     } 
/*     */     
/*  61 */     this.tickDataMap = new HashMap<>();
/*  62 */     this.lastTickData = null;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void setCurrentTarget(String targetRSN) {
/*  70 */     this.currentTargetRSN = (targetRSN != null && !targetRSN.trim().isEmpty()) ? targetRSN : "Unknown";
/*  71 */     this.lastTickData = null;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void logTickData(Client client, Player target) {
/*  79 */     if (this.currentTargetRSN == null || !this.config.combatAutomationLogging()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/*  86 */       long currentTick = GameManager.getTickCount();
/*  87 */       TickData tickData = new TickData();
/*  88 */       tickData.tick = currentTick;
/*  89 */       tickData.targetRSN = this.currentTargetRSN;
/*     */       
/*  91 */       Objects.requireNonNull(client); Player localPlayer = (Player)Static.invoke(client::getLocalPlayer);
/*  92 */       if (target == null)
/*     */       {
/*  94 */         target = getCurrentTarget(client);
/*     */       }
/*     */ 
/*     */       
/*  98 */       if (localPlayer != null) {
/*     */         
/* 100 */         tickData.player = new PlayerTickData();
/* 101 */         Objects.requireNonNull(localPlayer); tickData.player.animationId = ((Integer)Static.invoke(localPlayer::getAnimation)).intValue();
/* 102 */         Objects.requireNonNull(localPlayer); tickData.player.graphicId = ((Integer)Static.invoke(localPlayer::getGraphic)).intValue();
/* 103 */         tickData.player.spotAnim = -1;
/*     */         
/* 105 */         Objects.requireNonNull(localPlayer); WorldPoint playerTile = (WorldPoint)Static.invoke(localPlayer::getWorldLocation);
/* 106 */         tickData.player.tileX = playerTile.getX();
/* 107 */         tickData.player.tileY = playerTile.getY();
/* 108 */         tickData.player.tilePlane = playerTile.getPlane();
/*     */ 
/*     */         
/* 111 */         tickData.player.hitpoints = SkillAPI.getBoostedLevel(Skill.HITPOINTS);
/* 112 */         tickData.player.maxHitpoints = 99;
/* 113 */         tickData.player.prayerPoints = SkillAPI.getBoostedLevel(Skill.PRAYER);
/* 114 */         tickData.player.maxPrayer = 99;
/* 115 */         tickData.player.runEnergy = client.getEnergy();
/* 116 */         tickData.player.specEnergy = VarAPI.getVar(300);
/*     */ 
/*     */         
/* 119 */         tickData.player.weaponId = getEquippedItemId(client, KitType.WEAPON);
/* 120 */         tickData.player.offhandId = getEquippedItemId(client, KitType.SHIELD);
/* 121 */         tickData.player.helmetId = getEquippedItemId(client, KitType.HEAD);
/* 122 */         tickData.player.bodyId = getEquippedItemId(client, KitType.TORSO);
/* 123 */         tickData.player.legsId = getEquippedItemId(client, KitType.LEGS);
/* 124 */         tickData.player.glovesId = getEquippedItemId(client, KitType.HANDS);
/* 125 */         tickData.player.bootsId = -1;
/* 126 */         tickData.player.ammoId = -1;
/*     */ 
/*     */         
/* 129 */         if (this.lastTickData != null && this.lastTickData.player != null) {
/*     */           
/* 131 */           tickData.player.moved = !tilesEqual(playerTile, this.lastTickData.player.tileX, this.lastTickData.player.tileY, this.lastTickData.player.tilePlane);
/* 132 */           tickData.player.animationChanged = (tickData.player.animationId != this.lastTickData.player.animationId);
/* 133 */           tickData.player.graphicChanged = (tickData.player.graphicId != this.lastTickData.player.graphicId);
/* 134 */           tickData.player.hpChanged = (tickData.player.hitpoints != this.lastTickData.player.hitpoints);
/* 135 */           tickData.player.prayerChanged = (tickData.player.prayerPoints != this.lastTickData.player.prayerPoints);
/* 136 */           tickData.player.energyChanged = (tickData.player.runEnergy != this.lastTickData.player.runEnergy);
/* 137 */           tickData.player.specChanged = (tickData.player.specEnergy != this.lastTickData.player.specEnergy);
/*     */ 
/*     */           
/* 140 */           tickData.player.attacked = (tickData.player.animationChanged && isAttackAnimation(tickData.player.animationId));
/* 141 */           tickData.player.ate = (tickData.player.hpChanged && tickData.player.hitpoints > this.lastTickData.player.hitpoints);
/* 142 */           tickData.player.brewed = (tickData.player.hpChanged && tickData.player.hitpoints > this.lastTickData.player.hitpoints && tickData.player.hitpoints - this.lastTickData.player.hitpoints < 22);
/*     */           
/* 144 */           tickData.player.prayed = (tickData.player.prayerChanged && tickData.player.prayerPoints > this.lastTickData.player.prayerPoints);
/* 145 */           tickData.player.speced = (tickData.player.specChanged && tickData.player.specEnergy < this.lastTickData.player.specEnergy);
/* 146 */           tickData.player.froze = (tickData.player.graphicChanged && isFreezeGraphic(tickData.player.graphicId));
/*     */         } 
/*     */ 
/*     */         
/* 150 */         tickData.player.overheadPrayer = getOverheadPrayer(client);
/*     */       } 
/*     */ 
/*     */       
/* 154 */       if (target != null) {
/*     */         
/* 156 */         tickData.opponent = new OpponentTickData();
/* 157 */         Objects.requireNonNull(target); tickData.opponent.animationId = ((Integer)Static.invoke(target::getAnimation)).intValue();
/* 158 */         Objects.requireNonNull(target); tickData.opponent.graphicId = ((Integer)Static.invoke(target::getGraphic)).intValue();
/* 159 */         tickData.opponent.spotAnim = -1;
/*     */         
/* 161 */         Objects.requireNonNull(target); WorldPoint opponentTile = (WorldPoint)Static.invoke(target::getWorldLocation);
/* 162 */         tickData.opponent.tileX = opponentTile.getX();
/* 163 */         tickData.opponent.tileY = opponentTile.getY();
/* 164 */         tickData.opponent.tilePlane = opponentTile.getPlane();
/*     */ 
/*     */         
/* 167 */         Objects.requireNonNull(target); tickData.opponent.hitpoints = (((Integer)Static.invoke(target::getHealthRatio)).intValue() >= 0) ? ((Integer)Static.invoke(target::getHealthRatio)).intValue() : -1;
/* 168 */         tickData.opponent.maxHitpoints = -1;
/*     */ 
/*     */         
/* 171 */         tickData.opponent.weaponId = getTargetEquippedItemId(target, KitType.WEAPON);
/* 172 */         tickData.opponent.offhandId = getTargetEquippedItemId(target, KitType.SHIELD);
/* 173 */         tickData.opponent.helmetId = getTargetEquippedItemId(target, KitType.HEAD);
/* 174 */         tickData.opponent.bodyId = getTargetEquippedItemId(target, KitType.TORSO);
/* 175 */         tickData.opponent.legsId = getTargetEquippedItemId(target, KitType.LEGS);
/*     */ 
/*     */         
/* 178 */         tickData.opponent.attackType = null;
/*     */ 
/*     */         
/* 181 */         if (this.lastTickData != null && this.lastTickData.opponent != null) {
/*     */           
/* 183 */           tickData.opponent.moved = !tilesEqual(opponentTile, this.lastTickData.opponent.tileX, this.lastTickData.opponent.tileY, this.lastTickData.opponent.tilePlane);
/* 184 */           tickData.opponent.animationChanged = (tickData.opponent.animationId != this.lastTickData.opponent.animationId);
/* 185 */           tickData.opponent.graphicChanged = (tickData.opponent.graphicId != this.lastTickData.opponent.graphicId);
/* 186 */           tickData.opponent.hpChanged = (tickData.opponent.hitpoints != this.lastTickData.opponent.hitpoints);
/*     */ 
/*     */           
/* 189 */           tickData.opponent.attacked = (tickData.opponent.animationChanged && isAttackAnimation(tickData.opponent.animationId));
/* 190 */           tickData.opponent.ate = (tickData.opponent.hpChanged && tickData.opponent.hitpoints > this.lastTickData.opponent.hitpoints);
/* 191 */           tickData.opponent.brewed = (tickData.opponent.hpChanged && tickData.opponent.hitpoints > this.lastTickData.opponent.hitpoints && tickData.opponent.hitpoints - this.lastTickData.opponent.hitpoints < 22);
/*     */           
/* 193 */           tickData.opponent.froze = (tickData.opponent.graphicChanged && isFreezeGraphic(tickData.opponent.graphicId));
/* 194 */           tickData.opponent.speced = (tickData.opponent.animationChanged && isSpecAnimation(tickData.opponent.animationId));
/*     */         } 
/*     */ 
/*     */         
/* 198 */         tickData.opponent.overheadPrayer = getTargetOverheadPrayer(target);
/*     */ 
/*     */         
/* 201 */         if (tickData.opponent.attacked)
/*     */         {
/* 203 */           tickData.opponent.attackType = categorizeAttackType(tickData.opponent.animationId, tickData.opponent.weaponId);
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/* 208 */       tickData.combatState = new CombatStateData();
/* 209 */       tickData.combatState.inCombat = (VarAPI.getVar(8121) == 1);
/*     */       
/* 211 */       if (localPlayer != null && target != null) {
/*     */         
/* 213 */         Objects.requireNonNull(localPlayer); WorldPoint playerTile = (WorldPoint)Static.invoke(localPlayer::getWorldLocation);
/* 214 */         Objects.requireNonNull(target); WorldPoint opponentTile = (WorldPoint)Static.invoke(target::getWorldLocation);
/*     */ 
/*     */         
/* 217 */         tickData.combatState.distance = calculateDistance(playerTile, opponentTile);
/*     */ 
/*     */         
/* 220 */         Objects.requireNonNull(localPlayer); int playerGraphic = ((Integer)Static.invoke(localPlayer::getGraphic)).intValue();
/* 221 */         int opponentGraphic = target.getGraphic();
/* 222 */         tickData.combatState.playerFrozen = isFreezeGraphic(playerGraphic);
/* 223 */         tickData.combatState.opponentFrozen = isFreezeGraphic(opponentGraphic);
/* 224 */         tickData.combatState.freezeState = getFreezeStateString(tickData.combatState.playerFrozen, tickData.combatState.opponentFrozen);
/*     */ 
/*     */         
/* 227 */         tickData.combatState.underTarget = playerTile.equals(opponentTile);
/* 228 */         tickData.combatState.targetUnderUs = playerTile.equals(opponentTile);
/*     */       } 
/*     */ 
/*     */       
/* 232 */       tickData.temporal = new TemporalData();
/* 233 */       if (this.lastTickData != null) {
/*     */         
/* 235 */         tickData.temporal.ticksSinceLastPlayerAction = currentTick - this.lastTickData.tick;
/* 236 */         if (this.lastTickData.player != null && this.lastTickData.player.attacked)
/*     */         {
/* 238 */           tickData.temporal.ticksSincePlayerAttack = currentTick - this.lastTickData.tick;
/*     */         }
/* 240 */         if (this.lastTickData.opponent != null && this.lastTickData.opponent.attacked)
/*     */         {
/* 242 */           tickData.temporal.ticksSinceOpponentAttack = (int)(currentTick - this.lastTickData.tick);
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/* 247 */       tickData.features = categorizeFeatures(tickData);
/*     */ 
/*     */       
/* 250 */       List<TickData> targetTicks = this.tickDataMap.computeIfAbsent(this.currentTargetRSN, k -> new ArrayList());
/* 251 */       targetTicks.add(tickData);
/*     */ 
/*     */       
/* 254 */       if (targetTicks.size() > 10000)
/*     */       {
/* 256 */         targetTicks.remove(0);
/*     */       }
/*     */ 
/*     */       
/* 260 */       this.lastTickData = tickData;
/*     */ 
/*     */       
/* 263 */       if (targetTicks.size() % 100 == 0)
/*     */       {
/* 265 */         saveCurrentTarget();
/*     */       }
/*     */     }
/* 268 */     catch (Exception e) {
/*     */       
/* 270 */       Logger.norm("[TickDataLogger] Error logging tick data: " + e.getMessage());
/* 271 */       e.printStackTrace();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public TickData getLastTickData() {
/* 281 */     return this.lastTickData;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private Map<String, Object> categorizeFeatures(TickData tickData) {
/* 289 */     Map<String, Object> features = new HashMap<>();
/*     */ 
/*     */     
/* 292 */     if (tickData.player != null) {
/*     */       
/* 294 */       features.put("player_animation_category", categorizeAnimation(tickData.player.animationId));
/* 295 */       features.put("player_graphic_category", categorizeGraphic(tickData.player.graphicId));
/*     */     } 
/* 297 */     if (tickData.opponent != null) {
/*     */       
/* 299 */       features.put("opponent_animation_category", categorizeAnimation(tickData.opponent.animationId));
/* 300 */       features.put("opponent_graphic_category", categorizeGraphic(tickData.opponent.graphicId));
/* 301 */       features.put("opponent_attack_type", tickData.opponent.attackType);
/*     */     } 
/*     */ 
/*     */     
/* 305 */     List<String> playerActions = new ArrayList<>();
/* 306 */     if (tickData.player != null) {
/*     */       
/* 308 */       if (tickData.player.attacked) playerActions.add("attack"); 
/* 309 */       if (tickData.player.moved) playerActions.add("move"); 
/* 310 */       if (tickData.player.ate) playerActions.add("eat"); 
/* 311 */       if (tickData.player.brewed) playerActions.add("brew"); 
/* 312 */       if (tickData.player.speced) playerActions.add("spec"); 
/* 313 */       if (tickData.player.froze) playerActions.add("freeze"); 
/* 314 */       if (tickData.player.prayed) playerActions.add("pray"); 
/*     */     } 
/* 316 */     features.put("player_actions", playerActions);
/*     */     
/* 318 */     List<String> opponentActions = new ArrayList<>();
/* 319 */     if (tickData.opponent != null) {
/*     */       
/* 321 */       if (tickData.opponent.attacked) opponentActions.add("attack"); 
/* 322 */       if (tickData.opponent.moved) opponentActions.add("move"); 
/* 323 */       if (tickData.opponent.ate) opponentActions.add("eat"); 
/* 324 */       if (tickData.opponent.brewed) opponentActions.add("brew"); 
/* 325 */       if (tickData.opponent.speced) opponentActions.add("spec"); 
/* 326 */       if (tickData.opponent.froze) opponentActions.add("freeze"); 
/*     */     } 
/* 328 */     features.put("opponent_actions", opponentActions);
/*     */ 
/*     */     
/* 331 */     if (tickData.combatState != null) {
/*     */       
/* 333 */       features.put("freeze_state", tickData.combatState.freezeState);
/* 334 */       features.put("distance_category", categorizeDistance(tickData.combatState.distance));
/* 335 */       features.put("hp_category_player", categorizeHP((tickData.player != null) ? tickData.player.hitpoints : -1, 
/* 336 */             (tickData.player != null) ? tickData.player.maxHitpoints : 100));
/*     */     } 
/*     */ 
/*     */     
/* 340 */     if (tickData.temporal != null) {
/*     */       
/* 342 */       features.put("time_since_opponent_attack", Integer.valueOf((int)tickData.temporal.ticksSinceOpponentAttack));
/* 343 */       features.put("time_category", categorizeTimeSinceAttack((int)tickData.temporal.ticksSinceOpponentAttack));
/*     */     } 
/*     */     
/* 346 */     return features;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private Player getCurrentTarget(Client client) {
/* 353 */     Objects.requireNonNull(client); Player localPlayer = (Player)Static.invoke(client::getLocalPlayer);
/* 354 */     if (localPlayer != null) {
/*     */       
/* 356 */       Objects.requireNonNull(localPlayer); Actor interacting = (Actor)Static.invoke(localPlayer::getInteracting);
/* 357 */       if (interacting instanceof Player)
/*     */       {
/* 359 */         return (Player)interacting;
/*     */       }
/*     */     } 
/* 362 */     return null;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private int getEquippedItemId(Client client, KitType kitType) {
/*     */     try {
/* 369 */       Objects.requireNonNull(client); Player localPlayer = (Player)Static.invoke(client::getLocalPlayer);
/* 370 */       if (localPlayer != null)
/*     */       {
/* 372 */         PlayerComposition comp = localPlayer.getPlayerComposition();
/* 373 */         if (comp != null)
/*     */         {
/* 375 */           return comp.getEquipmentId(kitType);
/*     */         }
/*     */       }
/*     */     
/* 379 */     } catch (Exception exception) {}
/* 380 */     return -1;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private int getTargetEquippedItemId(Player target, KitType kitType) {
/*     */     try {
/* 387 */       if (target != null)
/*     */       {
/* 389 */         PlayerComposition comp = target.getPlayerComposition();
/* 390 */         if (comp != null)
/*     */         {
/* 392 */           return comp.getEquipmentId(kitType);
/*     */         }
/*     */       }
/*     */     
/* 396 */     } catch (Exception exception) {}
/* 397 */     return -1;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private String getOverheadPrayer(Client client) {
/*     */     try {
/* 404 */       Objects.requireNonNull(client); Player localPlayer = (Player)Static.invoke(client::getLocalPlayer);
/* 405 */       if (localPlayer != null) {
/*     */         
/* 407 */         HeadIcon overheadIcon = localPlayer.getOverheadIcon();
/* 408 */         if (overheadIcon == HeadIcon.MELEE) return "melee"; 
/* 409 */         if (overheadIcon == HeadIcon.RANGED) return "ranged"; 
/* 410 */         if (overheadIcon == HeadIcon.MAGIC) return "magic";
/*     */       
/*     */       } 
/* 413 */     } catch (Exception exception) {}
/* 414 */     return "none";
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private String getTargetOverheadPrayer(Player target) {
/*     */     try {
/* 421 */       if (target != null) {
/*     */         
/* 423 */         HeadIcon overheadIcon = target.getOverheadIcon();
/* 424 */         if (overheadIcon == HeadIcon.MELEE) return "melee"; 
/* 425 */         if (overheadIcon == HeadIcon.RANGED) return "ranged"; 
/* 426 */         if (overheadIcon == HeadIcon.MAGIC) return "magic";
/*     */       
/*     */       } 
/* 429 */     } catch (Exception exception) {}
/* 430 */     return "none";
/*     */   }
/*     */ 
/*     */   
/*     */   private boolean tilesEqual(WorldPoint tile, int x, int y, int plane) {
/* 435 */     return (tile.getX() == x && tile.getY() == y && tile.getPlane() == plane);
/*     */   }
/*     */ 
/*     */   
/*     */   private int calculateDistance(WorldPoint a, WorldPoint b) {
/* 440 */     if (a.getPlane() != b.getPlane()) return -1; 
/* 441 */     int dx = Math.abs(a.getX() - b.getX());
/* 442 */     int dy = Math.abs(a.getY() - b.getY());
/* 443 */     return Math.max(dx, dy);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean isAttackAnimation(int animationId) {
/* 449 */     return (animationId == 1658 || animationId == 1659 || animationId == 7617 || animationId == 7618 || animationId == 7855 || animationId == 7854 || animationId == 423 || animationId == 422 || (animationId >= 1162 && animationId <= 1176) || (animationId >= 7617 && animationId <= 7620) || (animationId >= 7855 && animationId <= 7858));
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
/*     */   private boolean isSpecAnimation(int animationId) {
/* 461 */     return (animationId == 423 || animationId == 422 || animationId == 7514 || animationId == 7515 || animationId == 426 || animationId == 427);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean isFreezeGraphic(int graphicId) {
/* 469 */     return (graphicId == 369 || graphicId == 370 || graphicId == 367 || graphicId == 366);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private String categorizeAttackType(int animationId, int weaponId) {
/* 475 */     if ((animationId >= 1162 && animationId <= 1176) || animationId == 1658 || animationId == 1659 || animationId == 423 || animationId == 422)
/*     */     {
/*     */       
/* 478 */       return "melee";
/*     */     }
/* 480 */     if ((animationId >= 7617 && animationId <= 7620) || animationId == 7854)
/*     */     {
/* 482 */       return "ranged";
/*     */     }
/* 484 */     if ((animationId >= 7855 && animationId <= 7858) || animationId == 7618)
/*     */     {
/* 486 */       return "magic";
/*     */     }
/* 488 */     return "unknown";
/*     */   }
/*     */ 
/*     */   
/*     */   private String categorizeAnimation(int animationId) {
/* 493 */     if (animationId == -1 || animationId == 65535) return "idle"; 
/* 494 */     if (isAttackAnimation(animationId)) return "attack"; 
/* 495 */     if (isSpecAnimation(animationId)) return "spec"; 
/* 496 */     if (animationId >= 829 && animationId <= 838) return "eat"; 
/* 497 */     return "other_" + animationId % 100;
/*     */   }
/*     */ 
/*     */   
/*     */   private String categorizeGraphic(int graphicId) {
/* 502 */     if (graphicId == -1) return "none"; 
/* 503 */     if (isFreezeGraphic(graphicId)) return "freeze"; 
/* 504 */     return "graphic_" + graphicId % 50;
/*     */   }
/*     */ 
/*     */   
/*     */   private String categorizeDistance(int distance) {
/* 509 */     if (distance < 0) return "unknown"; 
/* 510 */     if (distance <= 1) return "adjacent"; 
/* 511 */     if (distance <= 3) return "close"; 
/* 512 */     if (distance <= 7) return "mid"; 
/* 513 */     return "far";
/*     */   }
/*     */ 
/*     */   
/*     */   private String categorizeHP(int hp, int maxHp) {
/* 518 */     if (hp < 0 || maxHp <= 0) return "unknown"; 
/* 519 */     double percent = hp * 100.0D / maxHp;
/* 520 */     if (percent < 30.0D) return "low"; 
/* 521 */     if (percent < 60.0D) return "mid"; 
/* 522 */     return "high";
/*     */   }
/*     */ 
/*     */   
/*     */   private String categorizeTimeSinceAttack(int ticks) {
/* 527 */     if (ticks < 0) return "unknown"; 
/* 528 */     if (ticks == 0) return "same_tick"; 
/* 529 */     if (ticks <= 1) return "very_recent"; 
/* 530 */     if (ticks <= 3) return "recent"; 
/* 531 */     if (ticks <= 7) return "moderate"; 
/* 532 */     return "old";
/*     */   }
/*     */ 
/*     */   
/*     */   private String getFreezeStateString(boolean playerFrozen, boolean opponentFrozen) {
/* 537 */     if (playerFrozen && opponentFrozen) return "bothFrozen"; 
/* 538 */     if (playerFrozen) return "weFrozenTargetUnfrozen"; 
/* 539 */     if (opponentFrozen) return "targetFrozenWeUnfrozen"; 
/* 540 */     return "bothUnfrozen";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void saveAsync() {
/*     */     try {
/* 551 */       for (Map.Entry<String, List<TickData>> entry : this.tickDataMap.entrySet())
/*     */       {
/* 553 */         String targetRSN = entry.getKey();
/* 554 */         List<TickData> tickData = entry.getValue();
/*     */ 
/*     */         
/* 557 */         String sanitizedRSN = targetRSN.replaceAll("[^a-zA-Z0-9_-]", "_");
/* 558 */         File targetFile = new File(this.combatLogsDir, sanitizedRSN + ".json");
/*     */ 
/*     */         
/* 561 */         String json = gson.toJson(tickData);
/* 562 */         FileWriter writer = new FileWriter(targetFile);
/*     */         
/* 564 */         try { writer.write(json);
/* 565 */           writer.close(); } catch (Throwable throwable) { try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }
/*     */            throw throwable; }
/* 567 */          if (this.config.debug())
/*     */         {
/* 569 */           Logger.norm("[TickDataLogger] Saved " + tickData.size() + " ticks for " + targetRSN + " to: " + targetFile.getAbsolutePath());
/*     */         }
/*     */       }
/*     */     
/* 573 */     } catch (IOException e) {
/*     */       
/* 575 */       Logger.norm("[TickDataLogger] Failed to save tick data: " + e.getMessage());
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void saveCurrentTarget() {
/* 584 */     if (this.currentTargetRSN == null || !this.tickDataMap.containsKey(this.currentTargetRSN)) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/* 591 */       List<TickData> tickData = this.tickDataMap.get(this.currentTargetRSN);
/*     */ 
/*     */       
/* 594 */       String sanitizedRSN = this.currentTargetRSN.replaceAll("[^a-zA-Z0-9_-]", "_");
/* 595 */       File targetFile = new File(this.combatLogsDir, sanitizedRSN + ".json");
/*     */ 
/*     */       
/* 598 */       String json = gson.toJson(tickData);
/* 599 */       FileWriter writer = new FileWriter(targetFile);
/*     */       
/* 601 */       try { writer.write(json);
/* 602 */         writer.close(); } catch (Throwable throwable) { try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }
/*     */          throw throwable; }
/* 604 */        if (this.config.debug())
/*     */       {
/* 606 */         Logger.norm("[TickDataLogger] Saved " + tickData.size() + " ticks for " + this.currentTargetRSN + " to: " + targetFile.getAbsolutePath());
/*     */       }
/*     */     }
/* 609 */     catch (IOException e) {
/*     */       
/* 611 */       Logger.norm("[TickDataLogger] Failed to save tick data for " + this.currentTargetRSN + ": " + e.getMessage());
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void shutdown() {
/* 620 */     saveAsync();
/*     */   }
/*     */   
/*     */   public static class TickData {
/*     */     public long tick;
/*     */     public String targetRSN;
/*     */     public TickDataLogger.PlayerTickData player;
/*     */     public TickDataLogger.OpponentTickData opponent;
/*     */     public TickDataLogger.CombatStateData combatState;
/*     */     public TickDataLogger.TemporalData temporal;
/*     */     public Map<String, Object> features;
/*     */   }
/*     */   
/*     */   public static class PlayerTickData {
/*     */     public int animationId;
/*     */     public int graphicId;
/*     */     public int spotAnim;
/*     */     public int tileX;
/*     */     public int tileY;
/*     */     public int tilePlane;
/*     */     public int hitpoints;
/*     */     public int maxHitpoints;
/*     */     public int prayerPoints;
/*     */     public int maxPrayer;
/*     */     public int runEnergy;
/*     */     public int specEnergy;
/*     */     public int weaponId;
/*     */     public int offhandId;
/*     */     public int helmetId;
/*     */     public int bodyId;
/*     */     public int legsId;
/*     */     public int glovesId;
/*     */     public int bootsId;
/*     */     public int ammoId;
/*     */     public boolean moved;
/*     */     public boolean animationChanged;
/*     */     public boolean graphicChanged;
/*     */     public boolean hpChanged;
/*     */     public boolean prayerChanged;
/*     */     public boolean energyChanged;
/*     */     public boolean specChanged;
/*     */     public boolean attacked;
/*     */     public boolean ate;
/*     */     public boolean brewed;
/*     */     public boolean prayed;
/*     */     public boolean speced;
/*     */     public boolean froze;
/*     */     public String overheadPrayer;
/*     */   }
/*     */   
/*     */   public static class OpponentTickData {
/*     */     public int animationId;
/*     */     public int graphicId;
/*     */     public int spotAnim;
/*     */     public int tileX;
/*     */     public int tileY;
/*     */     public int tilePlane;
/*     */     public int hitpoints;
/*     */     public int maxHitpoints;
/*     */     public int weaponId;
/*     */     public int offhandId;
/*     */     public int helmetId;
/*     */     public int bodyId;
/*     */     public int legsId;
/*     */     public boolean moved;
/*     */     public boolean animationChanged;
/*     */     public boolean graphicChanged;
/*     */     public boolean hpChanged;
/*     */     public boolean attacked;
/*     */     public boolean ate;
/*     */     public boolean brewed;
/*     */     public boolean speced;
/*     */     public boolean froze;
/*     */     public String attackType;
/*     */     public String overheadPrayer;
/*     */   }
/*     */   
/*     */   public static class CombatStateData {
/*     */     public boolean inCombat;
/*     */     public int distance;
/*     */     public boolean playerFrozen;
/*     */     public boolean opponentFrozen;
/*     */     public String freezeState;
/*     */     public boolean underTarget;
/*     */     public boolean targetUnderUs;
/*     */   }
/*     */   
/*     */   public static class TemporalData {
/*     */     public long ticksSinceLastPlayerAction;
/*     */     public long ticksSincePlayerAttack;
/*     */     public long ticksSinceOpponentAttack;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/TickDataLogger.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */