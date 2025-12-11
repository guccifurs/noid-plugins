/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import net.runelite.api.coords.WorldPoint;
/*     */ 
/*     */ public class ExtendedAttackContext extends AttackContext {
/*     */   private String playerLastAttack;
/*     */   private String playerLastWeapon;
/*     */   private String playerLastPrayer;
/*     */   private boolean playerLastMovement;
/*     */   private boolean playerLastFood;
/*     */   private boolean playerLastBrew;
/*     */   private boolean playerLastSpecial;
/*     */   private boolean playerLastFreeze;
/*     */   private int playerLastDamageDealt;
/*     */   private int playerLastDamageReceived;
/*     */   
/*  15 */   public void setPlayerLastAttack(String playerLastAttack) { this.playerLastAttack = playerLastAttack; } public void setPlayerLastWeapon(String playerLastWeapon) { this.playerLastWeapon = playerLastWeapon; } public void setPlayerLastPrayer(String playerLastPrayer) { this.playerLastPrayer = playerLastPrayer; } public void setPlayerLastMovement(boolean playerLastMovement) { this.playerLastMovement = playerLastMovement; } public void setPlayerLastFood(boolean playerLastFood) { this.playerLastFood = playerLastFood; } public void setPlayerLastBrew(boolean playerLastBrew) { this.playerLastBrew = playerLastBrew; } public void setPlayerLastSpecial(boolean playerLastSpecial) { this.playerLastSpecial = playerLastSpecial; } public void setPlayerLastFreeze(boolean playerLastFreeze) { this.playerLastFreeze = playerLastFreeze; } public void setPlayerLastDamageDealt(int playerLastDamageDealt) { this.playerLastDamageDealt = playerLastDamageDealt; } public void setPlayerLastDamageReceived(int playerLastDamageReceived) { this.playerLastDamageReceived = playerLastDamageReceived; } public void setAttackSequence(List<String> attackSequence) { this.attackSequence = attackSequence; } public void setAttackIntervals(List<Integer> attackIntervals) { this.attackIntervals = attackIntervals; } public void setFreezeHistory(List<Long> freezeHistory) { this.freezeHistory = freezeHistory; } public void setSpecHistory(List<Long> specHistory) { this.specHistory = specHistory; } public void setWeaponSwitchHistory(List<Long> weaponSwitchHistory) { this.weaponSwitchHistory = weaponSwitchHistory; } public void setPrayerSwitchHistory(List<Long> prayerSwitchHistory) { this.prayerSwitchHistory = prayerSwitchHistory; } public void setMovementHistory(List<Long> movementHistory) { this.movementHistory = movementHistory; } public void setCombatDuration(long combatDuration) { this.combatDuration = combatDuration; } public void setFightPhase(String fightPhase) { this.fightPhase = fightPhase; } public void setDamageDealtTotal(int damageDealtTotal) { this.damageDealtTotal = damageDealtTotal; } public void setDamageReceivedTotal(int damageReceivedTotal) { this.damageReceivedTotal = damageReceivedTotal; } public void setKillCount(int killCount) { this.killCount = killCount; } public void setDeathCount(int deathCount) { this.deathCount = deathCount; } public void setOpponentKillCount(int opponentKillCount) { this.opponentKillCount = opponentKillCount; } public void setOpponentDeathCount(int opponentDeathCount) { this.opponentDeathCount = opponentDeathCount; } public void setPlayerTileX(int playerTileX) { this.playerTileX = playerTileX; } public void setPlayerTileY(int playerTileY) { this.playerTileY = playerTileY; } public void setPlayerTilePlane(int playerTilePlane) { this.playerTilePlane = playerTilePlane; } public void setOpponentTileX(int opponentTileX) { this.opponentTileX = opponentTileX; } public void setOpponentTileY(int opponentTileY) { this.opponentTileY = opponentTileY; } public void setOpponentTilePlane(int opponentTilePlane) { this.opponentTilePlane = opponentTilePlane; } public void setDistanceChange(int distanceChange) { this.distanceChange = distanceChange; } public void setMovementDirection(String movementDirection) { this.movementDirection = movementDirection; } public void setUnderTarget(boolean underTarget) { this.underTarget = underTarget; } public void setTargetUnderUs(boolean targetUnderUs) { this.targetUnderUs = targetUnderUs; } public void setLastMovementTick(long lastMovementTick) { this.lastMovementTick = lastMovementTick; } public void setOpponentFoodCount(int opponentFoodCount) { this.opponentFoodCount = opponentFoodCount; } public void setOpponentBrewCount(int opponentBrewCount) { this.opponentBrewCount = opponentBrewCount; } public void setOpponentPrayerPoints(int opponentPrayerPoints) { this.opponentPrayerPoints = opponentPrayerPoints; } public void setPlayerFoodCount(int playerFoodCount) { this.playerFoodCount = playerFoodCount; } public void setPlayerBrewCount(int playerBrewCount) { this.playerBrewCount = playerBrewCount; } public void setPlayerPrayerPoints(int playerPrayerPoints) { this.playerPrayerPoints = playerPrayerPoints; } public void setOpponentLastAttack(String opponentLastAttack) { this.opponentLastAttack = opponentLastAttack; } public void setOpponentLastWeapon(String opponentLastWeapon) { this.opponentLastWeapon = opponentLastWeapon; } public void setOpponentLastPrayer(String opponentLastPrayer) { this.opponentLastPrayer = opponentLastPrayer; } public void setOpponentLastMovement(boolean opponentLastMovement) { this.opponentLastMovement = opponentLastMovement; } public void setOpponentLastFood(boolean opponentLastFood) { this.opponentLastFood = opponentLastFood; } public void setOpponentLastBrew(boolean opponentLastBrew) { this.opponentLastBrew = opponentLastBrew; } public void setOpponentLastSpecial(boolean opponentLastSpecial) { this.opponentLastSpecial = opponentLastSpecial; } public void setOpponentLastFreeze(boolean opponentLastFreeze) { this.opponentLastFreeze = opponentLastFreeze; } public void setOpponentLastDamageDealt(int opponentLastDamageDealt) { this.opponentLastDamageDealt = opponentLastDamageDealt; } public void setOpponentLastDamageReceived(int opponentLastDamageReceived) { this.opponentLastDamageReceived = opponentLastDamageReceived; } public void setRecentPattern(String recentPattern) { this.recentPattern = recentPattern; } public void setPatternFrequency(int patternFrequency) { this.patternFrequency = patternFrequency; } public void setPatternConfidence(int patternConfidence) { this.patternConfidence = patternConfidence; } public void setOpponentTendency(String opponentTendency) { this.opponentTendency = opponentTendency; } public void setOpponentSkillLevel(String opponentSkillLevel) { this.opponentSkillLevel = opponentSkillLevel; }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String getPlayerLastAttack() {
/*  21 */     return this.playerLastAttack;
/*     */   }
/*     */   public String getPlayerLastWeapon() {
/*  24 */     return this.playerLastWeapon;
/*     */   }
/*     */   public String getPlayerLastPrayer() {
/*  27 */     return this.playerLastPrayer;
/*     */   }
/*     */   public boolean isPlayerLastMovement() {
/*  30 */     return this.playerLastMovement;
/*     */   }
/*     */   public boolean isPlayerLastFood() {
/*  33 */     return this.playerLastFood;
/*     */   }
/*     */   public boolean isPlayerLastBrew() {
/*  36 */     return this.playerLastBrew;
/*     */   }
/*     */   public boolean isPlayerLastSpecial() {
/*  39 */     return this.playerLastSpecial;
/*     */   }
/*     */   public boolean isPlayerLastFreeze() {
/*  42 */     return this.playerLastFreeze;
/*     */   }
/*     */   public int getPlayerLastDamageDealt() {
/*  45 */     return this.playerLastDamageDealt;
/*     */   }
/*     */   public int getPlayerLastDamageReceived() {
/*  48 */     return this.playerLastDamageReceived;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*  53 */   private List<String> attackSequence = new ArrayList<>(); public List<String> getAttackSequence() { return this.attackSequence; }
/*     */ 
/*     */   
/*  56 */   private List<Integer> attackIntervals = new ArrayList<>(); public List<Integer> getAttackIntervals() { return this.attackIntervals; }
/*     */ 
/*     */   
/*  59 */   private List<Long> freezeHistory = new ArrayList<>(); public List<Long> getFreezeHistory() { return this.freezeHistory; }
/*     */ 
/*     */   
/*  62 */   private List<Long> specHistory = new ArrayList<>(); public List<Long> getSpecHistory() { return this.specHistory; }
/*     */ 
/*     */   
/*  65 */   private List<Long> weaponSwitchHistory = new ArrayList<>(); public List<Long> getWeaponSwitchHistory() { return this.weaponSwitchHistory; }
/*     */ 
/*     */   
/*  68 */   private List<Long> prayerSwitchHistory = new ArrayList<>(); public List<Long> getPrayerSwitchHistory() { return this.prayerSwitchHistory; }
/*     */ 
/*     */   
/*  71 */   private List<Long> movementHistory = new ArrayList<>(); private long combatDuration; private String fightPhase; private int damageDealtTotal; private int damageReceivedTotal; private int killCount; private int deathCount; private int opponentKillCount; private int opponentDeathCount; private int playerTileX; private int playerTileY; private int playerTilePlane; private int opponentTileX; private int opponentTileY; private int opponentTilePlane; private int distanceChange; private String movementDirection; private boolean underTarget; private boolean targetUnderUs; private long lastMovementTick; private int opponentFoodCount; public List<Long> getMovementHistory() { return this.movementHistory; }
/*     */   
/*     */   private int opponentBrewCount; private int opponentPrayerPoints; private int playerFoodCount; private int playerBrewCount; private int playerPrayerPoints; private String opponentLastAttack; private String opponentLastWeapon; private String opponentLastPrayer; private boolean opponentLastMovement; private boolean opponentLastFood; private boolean opponentLastBrew; private boolean opponentLastSpecial; private boolean opponentLastFreeze; private int opponentLastDamageDealt; private int opponentLastDamageReceived; private String recentPattern; private int patternFrequency; private int patternConfidence; private String opponentTendency; private String opponentSkillLevel; private int playerAttackTimerTicks;
/*     */   
/*     */   public long getCombatDuration() {
/*  76 */     return this.combatDuration;
/*     */   }
/*     */   public String getFightPhase() {
/*  79 */     return this.fightPhase;
/*     */   }
/*     */   public int getDamageDealtTotal() {
/*  82 */     return this.damageDealtTotal;
/*     */   }
/*     */   public int getDamageReceivedTotal() {
/*  85 */     return this.damageReceivedTotal;
/*     */   }
/*     */   public int getKillCount() {
/*  88 */     return this.killCount;
/*     */   }
/*     */   public int getDeathCount() {
/*  91 */     return this.deathCount;
/*     */   }
/*     */   public int getOpponentKillCount() {
/*  94 */     return this.opponentKillCount;
/*     */   }
/*     */   public int getOpponentDeathCount() {
/*  97 */     return this.opponentDeathCount;
/*     */   }
/*     */ 
/*     */   
/*     */   public int getPlayerTileX() {
/* 102 */     return this.playerTileX;
/*     */   }
/*     */   public int getPlayerTileY() {
/* 105 */     return this.playerTileY;
/*     */   }
/*     */   public int getPlayerTilePlane() {
/* 108 */     return this.playerTilePlane;
/*     */   }
/*     */   public int getOpponentTileX() {
/* 111 */     return this.opponentTileX;
/*     */   }
/*     */   public int getOpponentTileY() {
/* 114 */     return this.opponentTileY;
/*     */   }
/*     */   public int getOpponentTilePlane() {
/* 117 */     return this.opponentTilePlane;
/*     */   }
/*     */   public int getDistanceChange() {
/* 120 */     return this.distanceChange;
/*     */   }
/*     */   public String getMovementDirection() {
/* 123 */     return this.movementDirection;
/*     */   }
/*     */   public boolean isUnderTarget() {
/* 126 */     return this.underTarget;
/*     */   }
/*     */   public boolean isTargetUnderUs() {
/* 129 */     return this.targetUnderUs;
/*     */   }
/*     */   public long getLastMovementTick() {
/* 132 */     return this.lastMovementTick;
/*     */   }
/*     */ 
/*     */   
/*     */   public int getOpponentFoodCount() {
/* 137 */     return this.opponentFoodCount;
/*     */   }
/*     */   public int getOpponentBrewCount() {
/* 140 */     return this.opponentBrewCount;
/*     */   }
/*     */   public int getOpponentPrayerPoints() {
/* 143 */     return this.opponentPrayerPoints;
/*     */   }
/*     */   public int getPlayerFoodCount() {
/* 146 */     return this.playerFoodCount;
/*     */   }
/*     */   public int getPlayerBrewCount() {
/* 149 */     return this.playerBrewCount;
/*     */   }
/*     */   public int getPlayerPrayerPoints() {
/* 152 */     return this.playerPrayerPoints;
/*     */   }
/*     */ 
/*     */   
/*     */   public String getOpponentLastAttack() {
/* 157 */     return this.opponentLastAttack;
/*     */   }
/*     */   public String getOpponentLastWeapon() {
/* 160 */     return this.opponentLastWeapon;
/*     */   }
/*     */   public String getOpponentLastPrayer() {
/* 163 */     return this.opponentLastPrayer;
/*     */   }
/*     */   public boolean isOpponentLastMovement() {
/* 166 */     return this.opponentLastMovement;
/*     */   }
/*     */   public boolean isOpponentLastFood() {
/* 169 */     return this.opponentLastFood;
/*     */   }
/*     */   public boolean isOpponentLastBrew() {
/* 172 */     return this.opponentLastBrew;
/*     */   }
/*     */   public boolean isOpponentLastSpecial() {
/* 175 */     return this.opponentLastSpecial;
/*     */   }
/*     */   public boolean isOpponentLastFreeze() {
/* 178 */     return this.opponentLastFreeze;
/*     */   }
/*     */   public int getOpponentLastDamageDealt() {
/* 181 */     return this.opponentLastDamageDealt;
/*     */   }
/*     */   public int getOpponentLastDamageReceived() {
/* 184 */     return this.opponentLastDamageReceived;
/*     */   }
/*     */ 
/*     */   
/*     */   public String getRecentPattern() {
/* 189 */     return this.recentPattern;
/*     */   }
/*     */   public int getPatternFrequency() {
/* 192 */     return this.patternFrequency;
/*     */   }
/*     */   public int getPatternConfidence() {
/* 195 */     return this.patternConfidence;
/*     */   }
/*     */   public String getOpponentTendency() {
/* 198 */     return this.opponentTendency;
/*     */   }
/*     */   public String getOpponentSkillLevel() {
/* 201 */     return this.opponentSkillLevel;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public int getPlayerAttackTimerTicks() {
/* 208 */     return this.playerAttackTimerTicks;
/*     */   }
/*     */   public void setPlayerAttackTimerTicks(int playerAttackTimerTicks) {
/* 210 */     this.playerAttackTimerTicks = playerAttackTimerTicks;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public ExtendedAttackContext() {
/* 210 */     this.attackSequence = new ArrayList<>();
/* 211 */     this.attackIntervals = new ArrayList<>();
/* 212 */     this.freezeHistory = new ArrayList<>();
/* 213 */     this.specHistory = new ArrayList<>();
/* 214 */     this.weaponSwitchHistory = new ArrayList<>();
/* 215 */     this.prayerSwitchHistory = new ArrayList<>();
/* 216 */     this.movementHistory = new ArrayList<>();
/*     */ 
/*     */     
/* 219 */     this.playerLastDamageDealt = -1;
/* 220 */     this.playerLastDamageReceived = -1;
/* 221 */     this.opponentLastDamageDealt = -1;
/* 222 */     this.opponentLastDamageReceived = -1;
/* 223 */     this.distanceChange = 0;
/* 224 */     this.opponentPrayerPoints = -1;
/* 225 */     this.playerPrayerPoints = -1;
/* 226 */     this.patternConfidence = 0;
/* 227 */     this.lastMovementTick = -1L;
/* 228 */     this.playerAttackTimerTicks = -1;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public ExtendedAttackContext(AttackContext baseContext) {
/* 237 */     setTimestamp(baseContext.getTimestamp());
/* 238 */     setOverheadPrayer(baseContext.getOverheadPrayer());
/* 239 */     setTargetHP(baseContext.getTargetHP());
/* 240 */     setWeapon(baseContext.getWeapon());
/* 241 */     setTargetSpec(baseContext.getTargetSpec());
/* 242 */     setAttackStyle(baseContext.getAttackStyle());
/* 243 */     setFreezeState(baseContext.getFreezeState());
/* 244 */     setDistance(baseContext.getDistance());
/* 245 */     setPidStatus(baseContext.getPidStatus());
/* 246 */     setPlayerFreezeTicksRemaining(baseContext.getPlayerFreezeTicksRemaining());
/* 247 */     setOpponentFreezeTicksRemaining(baseContext.getOpponentFreezeTicksRemaining());
/*     */ 
/*     */     
/* 250 */     this.attackSequence = new ArrayList<>();
/* 251 */     this.attackIntervals = new ArrayList<>();
/* 252 */     this.freezeHistory = new ArrayList<>();
/* 253 */     this.specHistory = new ArrayList<>();
/* 254 */     this.weaponSwitchHistory = new ArrayList<>();
/* 255 */     this.prayerSwitchHistory = new ArrayList<>();
/* 256 */     this.movementHistory = new ArrayList<>();
/*     */ 
/*     */     
/* 259 */     this.playerLastDamageDealt = -1;
/* 260 */     this.playerLastDamageReceived = -1;
/* 261 */     this.opponentLastDamageDealt = -1;
/* 262 */     this.opponentLastDamageReceived = -1;
/* 263 */     this.distanceChange = 0;
/* 264 */     this.opponentPrayerPoints = -1;
/* 265 */     this.playerPrayerPoints = -1;
/* 266 */     this.patternConfidence = 0;
/* 267 */     this.lastMovementTick = -1L;
/* 268 */     this.playerTileX = -1;
/* 269 */     this.playerTileY = -1;
/* 270 */     this.playerTilePlane = -1;
/* 271 */     this.opponentTileX = -1;
/* 272 */     this.opponentTileY = -1;
/* 273 */     this.opponentTilePlane = -1;
/* 274 */     this.playerAttackTimerTicks = -1;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public AttackContext toBaseContext() {
/* 281 */     return new AttackContext(
/* 282 */         getTimestamp(), 
/* 283 */         getOverheadPrayer(), 
/* 284 */         getTargetHP(), 
/* 285 */         getWeapon(), 
/* 286 */         getTargetSpec(), 
/* 287 */         getAttackStyle(), 
/* 288 */         getFreezeState(), 
/* 289 */         getDistance(), 
/* 290 */         getPidStatus(), 
/* 291 */         getPlayerFreezeTicksRemaining(), 
/* 292 */         getOpponentFreezeTicksRemaining());
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void addToAttackSequence(String attackStyle, int maxSize) {
/* 301 */     if (attackStyle == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 306 */     this.attackSequence.add(attackStyle);
/* 307 */     while (this.attackSequence.size() > maxSize)
/*     */     {
/* 309 */       this.attackSequence.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void addAttackInterval(int interval, int maxSize) {
/* 318 */     this.attackIntervals.add(Integer.valueOf(interval));
/* 319 */     while (this.attackIntervals.size() > maxSize)
/*     */     {
/* 321 */       this.attackIntervals.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void addFreezeEvent(long tick, int maxSize) {
/* 330 */     this.freezeHistory.add(Long.valueOf(tick));
/* 331 */     while (this.freezeHistory.size() > maxSize)
/*     */     {
/* 333 */       this.freezeHistory.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void addSpecEvent(long tick, int maxSize) {
/* 342 */     this.specHistory.add(Long.valueOf(tick));
/* 343 */     while (this.specHistory.size() > maxSize)
/*     */     {
/* 345 */       this.specHistory.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void addWeaponSwitchEvent(long tick, int maxSize) {
/* 354 */     this.weaponSwitchHistory.add(Long.valueOf(tick));
/* 355 */     while (this.weaponSwitchHistory.size() > maxSize)
/*     */     {
/* 357 */       this.weaponSwitchHistory.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void addPrayerSwitchEvent(long tick, int maxSize) {
/* 366 */     this.prayerSwitchHistory.add(Long.valueOf(tick));
/* 367 */     while (this.prayerSwitchHistory.size() > maxSize)
/*     */     {
/* 369 */       this.prayerSwitchHistory.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void addMovementEvent(long tick, int maxSize) {
/* 378 */     this.movementHistory.add(Long.valueOf(tick));
/* 379 */     while (this.movementHistory.size() > maxSize)
/*     */     {
/* 381 */       this.movementHistory.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void setPlayerTile(WorldPoint tile) {
/* 390 */     if (tile != null) {
/*     */       
/* 392 */       this.playerTileX = tile.getX();
/* 393 */       this.playerTileY = tile.getY();
/* 394 */       this.playerTilePlane = tile.getPlane();
/*     */     }
/*     */     else {
/*     */       
/* 398 */       this.playerTileX = -1;
/* 399 */       this.playerTileY = -1;
/* 400 */       this.playerTilePlane = -1;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public WorldPoint getPlayerTile() {
/* 409 */     if (this.playerTileX == -1 || this.playerTileY == -1)
/*     */     {
/* 411 */       return null;
/*     */     }
/* 413 */     return new WorldPoint(this.playerTileX, this.playerTileY, this.playerTilePlane);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void setOpponentTile(WorldPoint tile) {
/* 421 */     if (tile != null) {
/*     */       
/* 423 */       this.opponentTileX = tile.getX();
/* 424 */       this.opponentTileY = tile.getY();
/* 425 */       this.opponentTilePlane = tile.getPlane();
/*     */     }
/*     */     else {
/*     */       
/* 429 */       this.opponentTileX = -1;
/* 430 */       this.opponentTileY = -1;
/* 431 */       this.opponentTilePlane = -1;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public WorldPoint getOpponentTile() {
/* 440 */     if (this.opponentTileX == -1 || this.opponentTileY == -1)
/*     */     {
/* 442 */       return null;
/*     */     }
/* 444 */     return new WorldPoint(this.opponentTileX, this.opponentTileY, this.opponentTilePlane);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static String calculateMovementDirection(WorldPoint from, WorldPoint to) {
/* 452 */     if (from == null || to == null)
/*     */     {
/* 454 */       return "None";
/*     */     }
/*     */     
/* 457 */     if (from.equals(to))
/*     */     {
/* 459 */       return "None";
/*     */     }
/*     */     
/* 462 */     int dx = to.getX() - from.getX();
/* 463 */     int dy = to.getY() - from.getY();
/*     */     
/* 465 */     if (dx == 0 && dy == 0)
/*     */     {
/* 467 */       return "None";
/*     */     }
/* 469 */     if (dx == 0 && dy > 0)
/*     */     {
/* 471 */       return "N";
/*     */     }
/* 473 */     if (dx == 0 && dy < 0)
/*     */     {
/* 475 */       return "S";
/*     */     }
/* 477 */     if (dx > 0 && dy == 0)
/*     */     {
/* 479 */       return "E";
/*     */     }
/* 481 */     if (dx < 0 && dy == 0)
/*     */     {
/* 483 */       return "W";
/*     */     }
/* 485 */     if (dx > 0 && dy > 0)
/*     */     {
/* 487 */       return "NE";
/*     */     }
/* 489 */     if (dx < 0 && dy > 0)
/*     */     {
/* 491 */       return "NW";
/*     */     }
/* 493 */     if (dx > 0 && dy < 0)
/*     */     {
/* 495 */       return "SE";
/*     */     }
/* 497 */     if (dx < 0 && dy < 0)
/*     */     {
/* 499 */       return "SW";
/*     */     }
/*     */     
/* 502 */     return "None";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static String calculateMovementDirection(int fromX, int fromY, int toX, int toY) {
/* 510 */     if (fromX == -1 || fromY == -1 || toX == -1 || toY == -1)
/*     */     {
/* 512 */       return "None";
/*     */     }
/*     */     
/* 515 */     if (fromX == toX && fromY == toY)
/*     */     {
/* 517 */       return "None";
/*     */     }
/*     */     
/* 520 */     int dx = toX - fromX;
/* 521 */     int dy = toY - fromY;
/*     */     
/* 523 */     if (dx == 0 && dy == 0)
/*     */     {
/* 525 */       return "None";
/*     */     }
/* 527 */     if (dx == 0 && dy > 0)
/*     */     {
/* 529 */       return "N";
/*     */     }
/* 531 */     if (dx == 0 && dy < 0)
/*     */     {
/* 533 */       return "S";
/*     */     }
/* 535 */     if (dx > 0 && dy == 0)
/*     */     {
/* 537 */       return "E";
/*     */     }
/* 539 */     if (dx < 0 && dy == 0)
/*     */     {
/* 541 */       return "W";
/*     */     }
/* 543 */     if (dx > 0 && dy > 0)
/*     */     {
/* 545 */       return "NE";
/*     */     }
/* 547 */     if (dx < 0 && dy > 0)
/*     */     {
/* 549 */       return "NW";
/*     */     }
/* 551 */     if (dx > 0 && dy < 0)
/*     */     {
/* 553 */       return "SE";
/*     */     }
/* 555 */     if (dx < 0 && dy < 0)
/*     */     {
/* 557 */       return "SW";
/*     */     }
/*     */     
/* 560 */     return "None";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static String determineFightPhase(long combatDuration, int playerHP, int opponentHP) {
/* 569 */     if (combatDuration < 50L || (playerHP > 70 && opponentHP > 70))
/*     */     {
/* 571 */       return "EARLY";
/*     */     }
/*     */     
/* 574 */     if (combatDuration > 200L || playerHP < 30 || opponentHP < 30)
/*     */     {
/* 576 */       return "LATE";
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 581 */     return "MID";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String generateContextKey() {
/* 592 */     String baseKey = super.generateContextKey();
/*     */ 
/*     */     
/* 595 */     StringBuilder key = new StringBuilder(baseKey);
/*     */ 
/*     */     
/* 598 */     if (this.playerLastAttack != null)
/*     */     {
/* 600 */       key.append(",playerLastAttack:").append(this.playerLastAttack);
/*     */     }
/* 602 */     if (this.playerLastWeapon != null)
/*     */     {
/* 604 */       key.append(",playerLastWeapon:").append(this.playerLastWeapon);
/*     */     }
/* 606 */     if (this.playerLastPrayer != null)
/*     */     {
/* 608 */       key.append(",playerLastPrayer:").append(this.playerLastPrayer);
/*     */     }
/*     */ 
/*     */     
/* 612 */     if (this.opponentLastAttack != null)
/*     */     {
/* 614 */       key.append(",opponentLastAttack:").append(this.opponentLastAttack);
/*     */     }
/* 616 */     if (this.opponentLastWeapon != null)
/*     */     {
/* 618 */       key.append(",opponentLastWeapon:").append(this.opponentLastWeapon);
/*     */     }
/*     */ 
/*     */     
/* 622 */     if (this.fightPhase != null)
/*     */     {
/* 624 */       key.append(",fightPhase:").append(this.fightPhase);
/*     */     }
/*     */ 
/*     */     
/* 628 */     key.append(",underTarget:").append(this.underTarget);
/* 629 */     key.append(",targetUnderUs:").append(this.targetUnderUs);
/*     */ 
/*     */     
/* 632 */     if (this.recentPattern != null && !this.recentPattern.isEmpty())
/*     */     {
/* 634 */       key.append(",pattern:").append(this.recentPattern);
/*     */     }
/*     */     
/* 637 */     return key.toString();
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/ExtendedAttackContext.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */