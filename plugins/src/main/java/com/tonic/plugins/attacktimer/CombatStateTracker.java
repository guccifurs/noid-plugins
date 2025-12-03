/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Static;
/*     */ import com.tonic.services.GameManager;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.Objects;
/*     */ import net.runelite.api.Player;
/*     */ import net.runelite.api.coords.WorldPoint;
/*     */ 
/*     */ public class CombatStateTracker { private String playerLastAttack; private String playerLastWeapon; private String playerLastPrayer; private boolean playerLastMovement; private boolean playerLastFood; private boolean playerLastBrew; private boolean playerLastSpecial; private boolean playerLastFreeze; private int playerLastDamageDealt;
/*     */   private int playerLastDamageReceived;
/*     */   private long playerLastAttackTick;
/*     */   private long playerLastMovementTick;
/*     */   private long playerLastFoodTick;
/*     */   private long playerLastBrewTick;
/*     */   private long playerLastSpecialTick;
/*     */   private long playerLastFreezeTick;
/*     */   private String opponentLastAttack;
/*     */   private String opponentLastWeapon;
/*     */   private String opponentLastPrayer;
/*     */   private boolean opponentLastMovement;
/*     */   private boolean opponentLastFood;
/*     */   private boolean opponentLastBrew;
/*     */   private boolean opponentLastSpecial;
/*     */   
/*  18 */   public void setPlayerLastAttack(String playerLastAttack) { this.playerLastAttack = playerLastAttack; } private boolean opponentLastFreeze; private int opponentLastDamageDealt; private int opponentLastDamageReceived; private long opponentLastAttackTick; private long opponentLastMovementTick; private long fightStartTick; private boolean fightActive; private int damageDealtTotal; private int damageReceivedTotal; private int killCount; private int deathCount; private int opponentKillCount; private int opponentDeathCount; private WorldPoint playerLastTile; private WorldPoint opponentLastTile; private WorldPoint playerCurrentTile; private WorldPoint opponentCurrentTile; private int playerFoodCount; private int playerBrewCount; private int playerPrayerPoints; private int opponentFoodCount; private int opponentBrewCount; private int opponentPrayerPoints; public void setPlayerLastWeapon(String playerLastWeapon) { this.playerLastWeapon = playerLastWeapon; } public void setPlayerLastPrayer(String playerLastPrayer) { this.playerLastPrayer = playerLastPrayer; } public void setPlayerLastMovement(boolean playerLastMovement) { this.playerLastMovement = playerLastMovement; } public void setPlayerLastFood(boolean playerLastFood) { this.playerLastFood = playerLastFood; } public void setPlayerLastBrew(boolean playerLastBrew) { this.playerLastBrew = playerLastBrew; } public void setPlayerLastSpecial(boolean playerLastSpecial) { this.playerLastSpecial = playerLastSpecial; } public void setPlayerLastFreeze(boolean playerLastFreeze) { this.playerLastFreeze = playerLastFreeze; } public void setPlayerLastDamageDealt(int playerLastDamageDealt) { this.playerLastDamageDealt = playerLastDamageDealt; } public void setPlayerLastDamageReceived(int playerLastDamageReceived) { this.playerLastDamageReceived = playerLastDamageReceived; } public void setPlayerLastAttackTick(long playerLastAttackTick) { this.playerLastAttackTick = playerLastAttackTick; } public void setPlayerLastMovementTick(long playerLastMovementTick) { this.playerLastMovementTick = playerLastMovementTick; } public void setPlayerLastFoodTick(long playerLastFoodTick) { this.playerLastFoodTick = playerLastFoodTick; } public void setPlayerLastBrewTick(long playerLastBrewTick) { this.playerLastBrewTick = playerLastBrewTick; } public void setPlayerLastSpecialTick(long playerLastSpecialTick) { this.playerLastSpecialTick = playerLastSpecialTick; } public void setPlayerLastFreezeTick(long playerLastFreezeTick) { this.playerLastFreezeTick = playerLastFreezeTick; } public void setOpponentLastAttack(String opponentLastAttack) { this.opponentLastAttack = opponentLastAttack; } public void setOpponentLastWeapon(String opponentLastWeapon) { this.opponentLastWeapon = opponentLastWeapon; } public void setOpponentLastPrayer(String opponentLastPrayer) { this.opponentLastPrayer = opponentLastPrayer; } public void setOpponentLastMovement(boolean opponentLastMovement) { this.opponentLastMovement = opponentLastMovement; } public void setOpponentLastFood(boolean opponentLastFood) { this.opponentLastFood = opponentLastFood; } public void setOpponentLastBrew(boolean opponentLastBrew) { this.opponentLastBrew = opponentLastBrew; } public void setOpponentLastSpecial(boolean opponentLastSpecial) { this.opponentLastSpecial = opponentLastSpecial; } public void setOpponentLastFreeze(boolean opponentLastFreeze) { this.opponentLastFreeze = opponentLastFreeze; } public void setOpponentLastDamageDealt(int opponentLastDamageDealt) { this.opponentLastDamageDealt = opponentLastDamageDealt; } public void setOpponentLastDamageReceived(int opponentLastDamageReceived) { this.opponentLastDamageReceived = opponentLastDamageReceived; } public void setOpponentLastAttackTick(long opponentLastAttackTick) { this.opponentLastAttackTick = opponentLastAttackTick; } public void setOpponentLastMovementTick(long opponentLastMovementTick) { this.opponentLastMovementTick = opponentLastMovementTick; } public void setFightStartTick(long fightStartTick) { this.fightStartTick = fightStartTick; } public void setFightActive(boolean fightActive) { this.fightActive = fightActive; } public void setDamageDealtTotal(int damageDealtTotal) { this.damageDealtTotal = damageDealtTotal; } public void setDamageReceivedTotal(int damageReceivedTotal) { this.damageReceivedTotal = damageReceivedTotal; } public void setKillCount(int killCount) { this.killCount = killCount; } public void setDeathCount(int deathCount) { this.deathCount = deathCount; } public void setOpponentKillCount(int opponentKillCount) { this.opponentKillCount = opponentKillCount; } public void setOpponentDeathCount(int opponentDeathCount) { this.opponentDeathCount = opponentDeathCount; } public void setPlayerLastTile(WorldPoint playerLastTile) { this.playerLastTile = playerLastTile; } public void setOpponentLastTile(WorldPoint opponentLastTile) { this.opponentLastTile = opponentLastTile; } public void setPlayerCurrentTile(WorldPoint playerCurrentTile) { this.playerCurrentTile = playerCurrentTile; } public void setOpponentCurrentTile(WorldPoint opponentCurrentTile) { this.opponentCurrentTile = opponentCurrentTile; } public void setPlayerFoodCount(int playerFoodCount) { this.playerFoodCount = playerFoodCount; } public void setPlayerBrewCount(int playerBrewCount) { this.playerBrewCount = playerBrewCount; } public void setPlayerPrayerPoints(int playerPrayerPoints) { this.playerPrayerPoints = playerPrayerPoints; } public void setOpponentFoodCount(int opponentFoodCount) { this.opponentFoodCount = opponentFoodCount; } public void setOpponentBrewCount(int opponentBrewCount) { this.opponentBrewCount = opponentBrewCount; } public void setOpponentPrayerPoints(int opponentPrayerPoints) { this.opponentPrayerPoints = opponentPrayerPoints; } public void setLastOpponentHP(int lastOpponentHP) { this.lastOpponentHP = lastOpponentHP; } public void setAttackIntervals(List<Integer> attackIntervals) { this.attackIntervals = attackIntervals; } public void setFreezeHistory(List<Long> freezeHistory) { this.freezeHistory = freezeHistory; } public void setSpecHistory(List<Long> specHistory) { this.specHistory = specHistory; } public void setWeaponSwitchHistory(List<Long> weaponSwitchHistory) { this.weaponSwitchHistory = weaponSwitchHistory; } public void setPrayerSwitchHistory(List<Long> prayerSwitchHistory) { this.prayerSwitchHistory = prayerSwitchHistory; } public void setMovementHistory(List<Long> movementHistory) { this.movementHistory = movementHistory; } public void setAttackSequence(List<String> attackSequence) { this.attackSequence = attackSequence; } public void setPlayerAttackSequence(List<String> playerAttackSequence) { this.playerAttackSequence = playerAttackSequence; } public void setOpponentAttackSequence(List<String> opponentAttackSequence) { this.opponentAttackSequence = opponentAttackSequence; }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String getPlayerLastAttack() {
/*  24 */     return this.playerLastAttack;
/*     */   }
/*     */   public String getPlayerLastWeapon() {
/*  27 */     return this.playerLastWeapon;
/*     */   }
/*     */   public String getPlayerLastPrayer() {
/*  30 */     return this.playerLastPrayer;
/*     */   }
/*     */   public boolean isPlayerLastMovement() {
/*  33 */     return this.playerLastMovement;
/*     */   }
/*     */   public boolean isPlayerLastFood() {
/*  36 */     return this.playerLastFood;
/*     */   }
/*     */   public boolean isPlayerLastBrew() {
/*  39 */     return this.playerLastBrew;
/*     */   }
/*     */   public boolean isPlayerLastSpecial() {
/*  42 */     return this.playerLastSpecial;
/*     */   }
/*     */   public boolean isPlayerLastFreeze() {
/*  45 */     return this.playerLastFreeze;
/*     */   }
/*     */   public int getPlayerLastDamageDealt() {
/*  48 */     return this.playerLastDamageDealt;
/*     */   }
/*     */   public int getPlayerLastDamageReceived() {
/*  51 */     return this.playerLastDamageReceived;
/*     */   }
/*     */   public long getPlayerLastAttackTick() {
/*  54 */     return this.playerLastAttackTick;
/*     */   }
/*     */   public long getPlayerLastMovementTick() {
/*  57 */     return this.playerLastMovementTick;
/*     */   }
/*     */   public long getPlayerLastFoodTick() {
/*  60 */     return this.playerLastFoodTick;
/*     */   }
/*     */   public long getPlayerLastBrewTick() {
/*  63 */     return this.playerLastBrewTick;
/*     */   }
/*     */   public long getPlayerLastSpecialTick() {
/*  66 */     return this.playerLastSpecialTick;
/*     */   }
/*     */   public long getPlayerLastFreezeTick() {
/*  69 */     return this.playerLastFreezeTick;
/*     */   }
/*     */ 
/*     */   
/*     */   public String getOpponentLastAttack() {
/*  74 */     return this.opponentLastAttack;
/*     */   }
/*     */   public String getOpponentLastWeapon() {
/*  77 */     return this.opponentLastWeapon;
/*     */   }
/*     */   public String getOpponentLastPrayer() {
/*  80 */     return this.opponentLastPrayer;
/*     */   }
/*     */   public boolean isOpponentLastMovement() {
/*  83 */     return this.opponentLastMovement;
/*     */   }
/*     */   public boolean isOpponentLastFood() {
/*  86 */     return this.opponentLastFood;
/*     */   }
/*     */   public boolean isOpponentLastBrew() {
/*  89 */     return this.opponentLastBrew;
/*     */   }
/*     */   public boolean isOpponentLastSpecial() {
/*  92 */     return this.opponentLastSpecial;
/*     */   }
/*     */   public boolean isOpponentLastFreeze() {
/*  95 */     return this.opponentLastFreeze;
/*     */   }
/*     */   public int getOpponentLastDamageDealt() {
/*  98 */     return this.opponentLastDamageDealt;
/*     */   }
/*     */   public int getOpponentLastDamageReceived() {
/* 101 */     return this.opponentLastDamageReceived;
/*     */   }
/*     */   public long getOpponentLastAttackTick() {
/* 104 */     return this.opponentLastAttackTick;
/*     */   }
/*     */   public long getOpponentLastMovementTick() {
/* 107 */     return this.opponentLastMovementTick;
/*     */   }
/*     */ 
/*     */   
/*     */   public long getFightStartTick() {
/* 112 */     return this.fightStartTick;
/*     */   }
/*     */   public boolean isFightActive() {
/* 115 */     return this.fightActive;
/*     */   }
/*     */   public int getDamageDealtTotal() {
/* 118 */     return this.damageDealtTotal;
/*     */   }
/*     */   public int getDamageReceivedTotal() {
/* 121 */     return this.damageReceivedTotal;
/*     */   }
/*     */   public int getKillCount() {
/* 124 */     return this.killCount;
/*     */   }
/*     */   public int getDeathCount() {
/* 127 */     return this.deathCount;
/*     */   }
/*     */   public int getOpponentKillCount() {
/* 130 */     return this.opponentKillCount;
/*     */   }
/*     */   public int getOpponentDeathCount() {
/* 133 */     return this.opponentDeathCount;
/*     */   }
/*     */ 
/*     */   
/*     */   public WorldPoint getPlayerLastTile() {
/* 138 */     return this.playerLastTile;
/*     */   }
/*     */   public WorldPoint getOpponentLastTile() {
/* 141 */     return this.opponentLastTile;
/*     */   }
/*     */   public WorldPoint getPlayerCurrentTile() {
/* 144 */     return this.playerCurrentTile;
/*     */   }
/*     */   public WorldPoint getOpponentCurrentTile() {
/* 147 */     return this.opponentCurrentTile;
/*     */   }
/*     */ 
/*     */   
/*     */   public int getPlayerFoodCount() {
/* 152 */     return this.playerFoodCount;
/*     */   }
/*     */   public int getPlayerBrewCount() {
/* 155 */     return this.playerBrewCount;
/*     */   }
/*     */   public int getPlayerPrayerPoints() {
/* 158 */     return this.playerPrayerPoints;
/*     */   }
/*     */   public int getOpponentFoodCount() {
/* 161 */     return this.opponentFoodCount;
/*     */   }
/*     */   public int getOpponentBrewCount() {
/* 164 */     return this.opponentBrewCount;
/*     */   }
/*     */   public int getOpponentPrayerPoints() {
/* 167 */     return this.opponentPrayerPoints;
/*     */   }
/*     */   
/* 170 */   private int lastOpponentHP = -1; public int getLastOpponentHP() { return this.lastOpponentHP; }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/* 175 */   private List<Integer> attackIntervals = new ArrayList<>(); public List<Integer> getAttackIntervals() { return this.attackIntervals; }
/*     */ 
/*     */   
/* 178 */   private List<Long> freezeHistory = new ArrayList<>(); public List<Long> getFreezeHistory() { return this.freezeHistory; }
/*     */ 
/*     */   
/* 181 */   private List<Long> specHistory = new ArrayList<>(); public List<Long> getSpecHistory() { return this.specHistory; }
/*     */ 
/*     */   
/* 184 */   private List<Long> weaponSwitchHistory = new ArrayList<>(); public List<Long> getWeaponSwitchHistory() { return this.weaponSwitchHistory; }
/*     */ 
/*     */   
/* 187 */   private List<Long> prayerSwitchHistory = new ArrayList<>(); public List<Long> getPrayerSwitchHistory() { return this.prayerSwitchHistory; }
/*     */ 
/*     */   
/* 190 */   private List<Long> movementHistory = new ArrayList<>(); public List<Long> getMovementHistory() { return this.movementHistory; }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/* 195 */   private List<String> attackSequence = new ArrayList<>(); public List<String> getAttackSequence() { return this.attackSequence; }
/*     */ 
/*     */   
/* 198 */   private List<String> playerAttackSequence = new ArrayList<>(); public List<String> getPlayerAttackSequence() { return this.playerAttackSequence; }
/*     */ 
/*     */   
/* 201 */   private List<String> opponentAttackSequence = new ArrayList<>(); public List<String> getOpponentAttackSequence() { return this.opponentAttackSequence; }
/*     */ 
/*     */   
/*     */   public CombatStateTracker() {
/* 205 */     reset();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void reset() {
/* 214 */     this.playerLastAttack = null;
/* 215 */     this.playerLastWeapon = null;
/* 216 */     this.playerLastPrayer = null;
/* 217 */     this.playerLastMovement = false;
/* 218 */     this.playerLastFood = false;
/* 219 */     this.playerLastBrew = false;
/* 220 */     this.playerLastSpecial = false;
/* 221 */     this.playerLastFreeze = false;
/* 222 */     this.playerLastDamageDealt = -1;
/* 223 */     this.playerLastDamageReceived = -1;
/* 224 */     this.playerLastAttackTick = -1L;
/* 225 */     this.playerLastMovementTick = -1L;
/* 226 */     this.playerLastFoodTick = -1L;
/* 227 */     this.playerLastBrewTick = -1L;
/* 228 */     this.playerLastSpecialTick = -1L;
/* 229 */     this.playerLastFreezeTick = -1L;
/*     */ 
/*     */     
/* 232 */     this.opponentLastAttack = null;
/* 233 */     this.opponentLastWeapon = null;
/* 234 */     this.opponentLastPrayer = null;
/* 235 */     this.opponentLastMovement = false;
/* 236 */     this.opponentLastFood = false;
/* 237 */     this.opponentLastBrew = false;
/* 238 */     this.opponentLastSpecial = false;
/* 239 */     this.opponentLastFreeze = false;
/* 240 */     this.opponentLastDamageDealt = -1;
/* 241 */     this.opponentLastDamageReceived = -1;
/* 242 */     this.opponentLastAttackTick = -1L;
/* 243 */     this.opponentLastMovementTick = -1L;
/*     */ 
/*     */     
/* 246 */     this.fightStartTick = -1L;
/* 247 */     this.fightActive = false;
/* 248 */     this.damageDealtTotal = 0;
/* 249 */     this.damageReceivedTotal = 0;
/* 250 */     this.killCount = 0;
/* 251 */     this.deathCount = 0;
/* 252 */     this.opponentKillCount = 0;
/* 253 */     this.opponentDeathCount = 0;
/*     */ 
/*     */     
/* 256 */     this.playerLastTile = null;
/* 257 */     this.opponentLastTile = null;
/* 258 */     this.playerCurrentTile = null;
/* 259 */     this.opponentCurrentTile = null;
/*     */ 
/*     */     
/* 262 */     this.playerFoodCount = -1;
/* 263 */     this.playerBrewCount = -1;
/* 264 */     this.playerPrayerPoints = -1;
/* 265 */     this.opponentFoodCount = -1;
/* 266 */     this.opponentBrewCount = -1;
/* 267 */     this.opponentPrayerPoints = -1;
/* 268 */     this.lastOpponentHP = -1;
/*     */ 
/*     */     
/* 271 */     this.attackIntervals.clear();
/* 272 */     this.freezeHistory.clear();
/* 273 */     this.specHistory.clear();
/* 274 */     this.weaponSwitchHistory.clear();
/* 275 */     this.prayerSwitchHistory.clear();
/* 276 */     this.movementHistory.clear();
/*     */ 
/*     */     
/* 279 */     this.attackSequence.clear();
/* 280 */     this.playerAttackSequence.clear();
/* 281 */     this.opponentAttackSequence.clear();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void startFight(long startTick) {
/* 289 */     this.fightStartTick = startTick;
/* 290 */     this.fightActive = true;
/* 291 */     this.damageDealtTotal = 0;
/* 292 */     this.damageReceivedTotal = 0;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void endFight() {
/* 300 */     this.fightActive = false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerAttack(String attackStyle, String weapon, long currentTick) {
/* 309 */     this.playerLastAttack = attackStyle;
/* 310 */     this.playerLastWeapon = weapon;
/* 311 */     this.playerLastAttackTick = currentTick;
/*     */ 
/*     */     
/* 314 */     if (attackStyle != null) {
/*     */       
/* 316 */       this.playerAttackSequence.add(attackStyle);
/* 317 */       this.attackSequence.add(attackStyle);
/*     */ 
/*     */       
/* 320 */       while (this.playerAttackSequence.size() > 10)
/*     */       {
/* 322 */         this.playerAttackSequence.remove(0);
/*     */       }
/* 324 */       while (this.attackSequence.size() > 10)
/*     */       {
/* 326 */         this.attackSequence.remove(0);
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 331 */     if (this.playerLastAttackTick <= 0L || this.attackSequence.size() >= 2);
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
/*     */   public void updateOpponentAttack(String attackStyle, String weapon, long currentTick) {
/* 343 */     this.opponentLastAttack = attackStyle;
/* 344 */     this.opponentLastWeapon = weapon;
/* 345 */     this.opponentLastAttackTick = currentTick;
/*     */ 
/*     */     
/* 348 */     if (attackStyle != null) {
/*     */       
/* 350 */       this.opponentAttackSequence.add(attackStyle);
/* 351 */       this.attackSequence.add(attackStyle);
/*     */ 
/*     */       
/* 354 */       while (this.opponentAttackSequence.size() > 10)
/*     */       {
/* 356 */         this.opponentAttackSequence.remove(0);
/*     */       }
/* 358 */       while (this.attackSequence.size() > 10)
/*     */       {
/* 360 */         this.attackSequence.remove(0);
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 365 */     if (this.opponentLastAttackTick > 0L && this.attackSequence.size() >= 2) {
/*     */       
/* 367 */       long lastAttackTick = this.opponentLastAttackTick;
/* 368 */       int interval = (int)(currentTick - lastAttackTick);
/* 369 */       if (interval > 0 && interval < 20) {
/*     */         
/* 371 */         this.attackIntervals.add(Integer.valueOf(interval));
/* 372 */         while (this.attackIntervals.size() > 10)
/*     */         {
/* 374 */           this.attackIntervals.remove(0);
/*     */         }
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerMovement(WorldPoint currentTile, long currentTick) {
/* 385 */     this.playerLastTile = this.playerCurrentTile;
/* 386 */     this.playerCurrentTile = currentTile;
/* 387 */     this.playerLastMovement = (this.playerLastTile != null && !this.playerLastTile.equals(this.playerCurrentTile));
/* 388 */     this.playerLastMovementTick = currentTick;
/*     */     
/* 390 */     if (this.playerLastMovement) {
/*     */       
/* 392 */       this.movementHistory.add(Long.valueOf(currentTick));
/* 393 */       while (this.movementHistory.size() > 10)
/*     */       {
/* 395 */         this.movementHistory.remove(0);
/*     */       }
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentMovement(WorldPoint currentTile, long currentTick) {
/* 405 */     this.opponentLastTile = this.opponentCurrentTile;
/* 406 */     this.opponentCurrentTile = currentTile;
/* 407 */     this.opponentLastMovement = (this.opponentLastTile != null && !this.opponentLastTile.equals(this.opponentCurrentTile));
/* 408 */     this.opponentLastMovementTick = currentTick;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerFood(long currentTick) {
/* 416 */     this.playerLastFood = true;
/* 417 */     this.playerLastFoodTick = currentTick;
/*     */     
/* 419 */     if (this.playerFoodCount > 0)
/*     */     {
/* 421 */       this.playerFoodCount--;
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentFood(long currentTick) {
/* 430 */     this.opponentLastFood = true;
/*     */     
/* 432 */     if (this.opponentFoodCount > 0)
/*     */     {
/* 434 */       this.opponentFoodCount--;
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerBrew(long currentTick) {
/* 443 */     this.playerLastBrew = true;
/* 444 */     this.playerLastBrewTick = currentTick;
/*     */     
/* 446 */     if (this.playerBrewCount > 0)
/*     */     {
/* 448 */       this.playerBrewCount--;
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentBrew(long currentTick) {
/* 457 */     this.opponentLastBrew = true;
/*     */     
/* 459 */     if (this.opponentBrewCount > 0)
/*     */     {
/* 461 */       this.opponentBrewCount--;
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean checkOpponentConsumption(int currentHP, long currentTick) {
/* 473 */     if (currentHP < 0 || this.lastOpponentHP < 0) {
/*     */ 
/*     */       
/* 476 */       this.lastOpponentHP = currentHP;
/* 477 */       return false;
/*     */     } 
/*     */ 
/*     */     
/* 481 */     int hpIncrease = currentHP - this.lastOpponentHP;
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 486 */     if (hpIncrease >= 10) {
/*     */ 
/*     */ 
/*     */       
/* 490 */       if (hpIncrease >= 18) {
/*     */ 
/*     */         
/* 493 */         updateOpponentFood(currentTick);
/*     */       }
/* 495 */       else if (hpIncrease >= 10) {
/*     */ 
/*     */         
/* 498 */         updateOpponentBrew(currentTick);
/*     */       } 
/*     */       
/* 501 */       this.lastOpponentHP = currentHP;
/* 502 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 506 */     this.lastOpponentHP = currentHP;
/* 507 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerSpecial(long currentTick) {
/* 515 */     this.playerLastSpecial = true;
/* 516 */     this.playerLastSpecialTick = currentTick;
/*     */     
/* 518 */     this.specHistory.add(Long.valueOf(currentTick));
/* 519 */     while (this.specHistory.size() > 10)
/*     */     {
/* 521 */       this.specHistory.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentSpecial(long currentTick) {
/* 530 */     this.opponentLastSpecial = true;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerFreeze(long currentTick) {
/* 538 */     this.playerLastFreeze = true;
/* 539 */     this.playerLastFreezeTick = currentTick;
/*     */     
/* 541 */     this.freezeHistory.add(Long.valueOf(currentTick));
/* 542 */     while (this.freezeHistory.size() > 10)
/*     */     {
/* 544 */       this.freezeHistory.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentFreeze(long currentTick) {
/* 553 */     this.opponentLastFreeze = true;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerPrayer(String prayer, long currentTick) {
/* 561 */     if (prayer != null && !prayer.equals(this.playerLastPrayer)) {
/*     */       
/* 563 */       this.playerLastPrayer = prayer;
/*     */       
/* 565 */       this.prayerSwitchHistory.add(Long.valueOf(currentTick));
/* 566 */       while (this.prayerSwitchHistory.size() > 10)
/*     */       {
/* 568 */         this.prayerSwitchHistory.remove(0);
/*     */       }
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentPrayer(String prayer) {
/* 578 */     if (prayer != null && !prayer.equals(this.opponentLastPrayer))
/*     */     {
/* 580 */       this.opponentLastPrayer = prayer;
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePlayerWeaponSwitch(String weapon, long currentTick) {
/* 589 */     if (weapon != null && !weapon.equals(this.playerLastWeapon)) {
/*     */       
/* 591 */       this.playerLastWeapon = weapon;
/*     */       
/* 593 */       this.weaponSwitchHistory.add(Long.valueOf(currentTick));
/* 594 */       while (this.weaponSwitchHistory.size() > 10)
/*     */       {
/* 596 */         this.weaponSwitchHistory.remove(0);
/*     */       }
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateDamageDealt(int damage) {
/* 606 */     this.playerLastDamageDealt = damage;
/* 607 */     this.damageDealtTotal += damage;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateDamageReceived(int damage) {
/* 615 */     this.playerLastDamageReceived = damage;
/* 616 */     this.damageReceivedTotal += damage;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentDamageDealt(int damage) {
/* 624 */     this.opponentLastDamageDealt = damage;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateOpponentDamageReceived(int damage) {
/* 632 */     this.opponentLastDamageReceived = damage;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void clearActionFlags() {
/* 641 */     this.playerLastMovement = false;
/* 642 */     this.playerLastFood = false;
/* 643 */     this.playerLastBrew = false;
/* 644 */     this.playerLastSpecial = false;
/* 645 */     this.playerLastFreeze = false;
/*     */     
/* 647 */     this.opponentLastMovement = false;
/* 648 */     this.opponentLastFood = false;
/* 649 */     this.opponentLastBrew = false;
/* 650 */     this.opponentLastSpecial = false;
/* 651 */     this.opponentLastFreeze = false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public long getCombatDuration() {
/* 659 */     if (!this.fightActive || this.fightStartTick < 0L)
/*     */     {
/* 661 */       return 0L;
/*     */     }
/*     */     
/* 664 */     long currentTick = GameManager.getTickCount();
/* 665 */     return currentTick - this.fightStartTick;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public int calculateDistanceChange() {
/* 673 */     if (this.playerCurrentTile == null || this.opponentCurrentTile == null || this.playerLastTile == null || this.opponentLastTile == null)
/*     */     {
/*     */       
/* 676 */       return 0;
/*     */     }
/*     */ 
/*     */     
/* 680 */     int currentDistance = calculateDistance(this.playerCurrentTile, this.opponentCurrentTile);
/*     */ 
/*     */     
/* 683 */     int previousDistance = calculateDistance(this.playerLastTile, this.opponentLastTile);
/*     */     
/* 685 */     return currentDistance - previousDistance;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private int calculateDistance(WorldPoint a, WorldPoint b) {
/* 693 */     if (a.getPlane() != b.getPlane())
/*     */     {
/* 695 */       return -1;
/*     */     }
/*     */     
/* 698 */     int dx = Math.abs(a.getX() - b.getX());
/* 699 */     int dy = Math.abs(a.getY() - b.getY());
/* 700 */     return Math.max(dx, dy);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isPlayerUnderTarget(Player player, Player target) {
/* 708 */     if (player == null || target == null)
/*     */     {
/* 710 */       return false;
/*     */     }
/*     */     
/* 713 */     Objects.requireNonNull(player); WorldPoint playerPos = (WorldPoint)Static.invoke(player::getWorldLocation);
/* 714 */     Objects.requireNonNull(target); WorldPoint targetPos = (WorldPoint)Static.invoke(target::getWorldLocation);
/*     */     
/* 716 */     return (playerPos.getX() == targetPos.getX() && playerPos
/* 717 */       .getY() == targetPos.getY() && playerPos
/* 718 */       .getPlane() == targetPos.getPlane());
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isTargetUnderPlayer(Player player, Player target) {
/* 726 */     return isPlayerUnderTarget(target, player);
/*     */   } }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/CombatStateTracker.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */