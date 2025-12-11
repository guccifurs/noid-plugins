/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.api.widgets.InventoryAPI;
/*     */ import com.tonic.api.widgets.PrayerAPI;
/*     */ import com.tonic.data.wrappers.ItemEx;
/*     */ import java.util.Random;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class WeFrozenTargetUnfrozenOnPidHandler
/*     */   implements CombatStateHandler
/*     */ {
/*  15 */   private final Random random = new Random();
/*     */   
/*     */   private boolean triedWhipLastTick = false;
/*     */   
/*     */   private boolean needToBrewAfterLowHPAngler = false;
/*  20 */   private int ticksSinceLowHPAngler = 0;
/*     */ 
/*     */   
/*     */   private boolean needToSanfewAfterAngler = false;
/*     */ 
/*     */   
/*  26 */   private int ticksAtZero = 0;
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean handle(CombatStateContext ctx) {
/*  31 */     if (!ctx.weFrozen || ctx.targetFrozen) {
/*     */       
/*  33 */       this.triedWhipLastTick = false;
/*  34 */       this.ticksAtZero = 0;
/*  35 */       this.needToSanfewAfterAngler = false;
/*  36 */       return false;
/*     */     } 
/*     */     
/*  39 */     if (ctx.currentPidStatus != CombatStateContext.PidStatus.ON_PID && ctx.currentPidStatus != CombatStateContext.PidStatus.UNKNOWN)
/*     */     {
/*     */       
/*  42 */       return false;
/*     */     }
/*     */ 
/*     */     
/*  46 */     ctx.helpers.setMindAction("We Frozen ON_PID");
/*     */ 
/*     */     
/*  49 */     int attackTimerTicks = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*  50 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*     */ 
/*     */     
/*  53 */     if (attackTimerTicks == 2 || attackTimerTicks == 3) {
/*     */       
/*  55 */       int fakieChance = ctx.config.fakieChance();
/*  56 */       if (fakieChance > 0 && fakieChance <= 100) {
/*     */         
/*  58 */         int i = this.random.nextInt(100);
/*  59 */         if (i < fakieChance) {
/*     */ 
/*     */           
/*  62 */           String itemToEquip = this.random.nextBoolean() ? "Abyssal tentacle" : "Dragon crossbow";
/*  63 */           boolean equipped = ctx.helpers.equipItemByName(itemToEquip);
/*  64 */           if (ctx.config.logHandlerActions())
/*     */           {
/*  66 */             Logger.norm("[We Frozen Target Unfrozen ON_PID] Fakie triggered (" + fakieChance + "% chance, roll: " + i + ") - Attempting to equip: " + itemToEquip + " - Result: " + (equipped ? "Success" : "Failed (not in inventory or already equipped)"));
/*     */           }
/*     */         } 
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/*  73 */     if (attackTimerTicks == 0) {
/*     */       
/*  75 */       this.ticksAtZero++;
/*     */     }
/*     */     else {
/*     */       
/*  79 */       this.ticksAtZero = 0;
/*     */     } 
/*     */ 
/*     */     
/*  83 */     boolean attacksSynced = (attackTimerTicks == opponentTimerTicks);
/*     */ 
/*     */     
/*  86 */     int health = ctx.helpers.getPlayerHealth();
/*  87 */     int magicLevel = ctx.helpers.getPlayerMagicLevel();
/*  88 */     int prayerPoints = ctx.helpers.getPlayerPrayerPoints();
/*  89 */     boolean hasBrews = ctx.helpers.hasBrews();
/*     */ 
/*     */ 
/*     */     
/*  93 */     if (this.ticksAtZero > 2 && health < 105)
/*     */     {
/*  95 */       if (ctx.helpers.eatAngler())
/*     */       {
/*  97 */         if (ctx.config.logHandlerActions())
/*     */         {
/*  99 */           Logger.norm("[We Frozen Target Unfrozen ON_PID] Timer at 0 for " + this.ticksAtZero + " ticks (idle 3+) and HP < 105 (" + health + "), eating anglerfish (timer: " + attackTimerTicks + ")");
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 107 */     int oppTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/* 108 */     if (oppTimerTicks > 1 && health < 45 && !hasBrews) {
/*     */ 
/*     */       
/* 111 */       int anglerCount = 0;
/* 112 */       for (ItemEx item : InventoryAPI.getItems()) {
/*     */         
/* 114 */         if (item != null && item.getName() != null && item.getName().toLowerCase().contains("angler"))
/*     */         {
/* 116 */           anglerCount += item.getQuantity();
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/* 121 */       if (anglerCount == 0)
/*     */       {
/* 123 */         if (!PrayerAPI.REDEMPTION.isActive()) {
/*     */           
/* 125 */           PrayerAPI.REDEMPTION.turnOn();
/* 126 */           if (ctx.config.aiPrayersLogging())
/*     */           {
/* 128 */             Logger.norm("[We Frozen Target Unfrozen ON_PID] Redemption activated - HP: " + health + ", No food (anglers: 0, brews: 0), Opponent timer: " + oppTimerTicks);
/*     */           }
/*     */         } 
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 135 */     if (prayerPoints < 25)
/*     */     {
/* 137 */       if (ctx.helpers.drinkSanfewSerum())
/*     */       {
/* 139 */         if (ctx.config.debug())
/*     */         {
/* 141 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 143 */             Logger.norm("[We Frozen Target Unfrozen ON_PID] Prayer < 25 (" + prayerPoints + "), drank Sanfew serum");
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 151 */     if (!hasBrews && health < 55)
/*     */     {
/* 153 */       if (ctx.helpers.eatAngler())
/*     */       {
/* 155 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 157 */           Logger.norm("[We Frozen Target Unfrozen ON_PID] Out of brews and HP < 55 (" + health + "), eating angler");
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 164 */     if (health < 35) {
/*     */ 
/*     */       
/* 167 */       if (!this.needToBrewAfterLowHPAngler)
/*     */       {
/* 169 */         if (ctx.helpers.eatAngler())
/*     */         {
/* 171 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 173 */             Logger.norm("[We Frozen Target Unfrozen ON_PID] HP < 35 (" + health + "), eating anglerfish first, will brew on next tick");
/*     */           }
/* 175 */           this.needToBrewAfterLowHPAngler = true;
/* 176 */           this.ticksSinceLowHPAngler = 0;
/*     */ 
/*     */         
/*     */         }
/*     */ 
/*     */       
/*     */       }
/* 183 */       else if (ctx.helpers.sipSaradominBrew())
/*     */       {
/* 185 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 187 */           Logger.norm("[We Frozen Target Unfrozen ON_PID] Drank brew after low HP anglerfish (next tick)");
/*     */         }
/* 189 */         this.needToBrewAfterLowHPAngler = false;
/* 190 */         this.ticksSinceLowHPAngler = 0;
/*     */       
/*     */       }
/*     */       else
/*     */       {
/*     */         
/* 196 */         this.needToBrewAfterLowHPAngler = false;
/* 197 */         this.ticksSinceLowHPAngler = 0;
/*     */ 
/*     */       
/*     */       }
/*     */ 
/*     */     
/*     */     }
/* 204 */     else if (this.needToBrewAfterLowHPAngler) {
/*     */       
/* 206 */       this.needToBrewAfterLowHPAngler = false;
/* 207 */       this.ticksSinceLowHPAngler = 0;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 212 */     if (health < 72)
/*     */     {
/* 214 */       if (ctx.helpers.sipSaradominBrew())
/*     */       {
/* 216 */         if (ctx.config.debug())
/*     */         {
/* 218 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 220 */             Logger.norm("[We Frozen Target Unfrozen ON_PID] HP below 72 (" + health + "), drank Saradomin brew");
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 228 */     if (health >= 72 && magicLevel < 94)
/*     */     {
/* 230 */       if (ctx.helpers.drinkSanfewSerum())
/*     */       {
/* 232 */         if (ctx.config.debug())
/*     */         {
/* 234 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 236 */             Logger.norm("[We Frozen Target Unfrozen ON_PID] HP >= 72 (" + health + ") but magic < 94 (" + magicLevel + "), drank Sanfew serum");
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 245 */     if (magicLevel < 82 && health < 60) {
/*     */ 
/*     */       
/* 248 */       if (!this.needToSanfewAfterAngler)
/*     */       {
/* 250 */         if (ctx.helpers.eatAngler())
/*     */         {
/* 252 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 254 */             Logger.norm("[We Frozen Target Unfrozen ON_PID] Magic < 82 (" + magicLevel + ") AND HP < 60 (" + health + "), eating anglerfish first, will sanfew on next tick");
/*     */           }
/* 256 */           this.needToSanfewAfterAngler = true;
/*     */ 
/*     */         
/*     */         }
/*     */ 
/*     */       
/*     */       }
/* 263 */       else if (ctx.helpers.drinkSanfewSerum())
/*     */       {
/* 265 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 267 */           Logger.norm("[We Frozen Target Unfrozen ON_PID] Drank Sanfew serum after anglerfish (next tick)");
/*     */         }
/* 269 */         this.needToSanfewAfterAngler = false;
/*     */       
/*     */       }
/*     */       else
/*     */       {
/*     */         
/* 275 */         this.needToSanfewAfterAngler = false;
/*     */       }
/*     */     
/*     */     }
/* 279 */     else if (magicLevel < 82) {
/*     */ 
/*     */       
/* 282 */       if (ctx.helpers.drinkSanfewSerum())
/*     */       {
/* 284 */         if (ctx.config.debug())
/*     */         {
/* 286 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 288 */             Logger.norm("[We Frozen Target Unfrozen ON_PID] Magic low (" + magicLevel + "), drank Sanfew serum");
/*     */           }
/*     */         }
/*     */         
/* 292 */         this.needToSanfewAfterAngler = false;
/*     */ 
/*     */       
/*     */       }
/*     */ 
/*     */     
/*     */     }
/* 299 */     else if (this.needToSanfewAfterAngler) {
/*     */       
/* 301 */       this.needToSanfewAfterAngler = false;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 306 */     if (attackTimerTicks > 1) {
/*     */       
/* 308 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 309 */       this.triedWhipLastTick = false;
/* 310 */       return true;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 315 */     boolean opponentRecentlyMeleed = (ctx.opponentRecentMeleeTicks >= 0 && ctx.opponentRecentMeleeTicks <= 1);
/*     */     
/* 317 */     if (opponentRecentlyMeleed) {
/*     */ 
/*     */       
/* 320 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 322 */         Logger.norm("[We Frozen Target Unfrozen ON_PID] Opponent recently meleed (ticks: " + ctx.opponentRecentMeleeTicks + ") - always melee back");
/*     */       }
/* 324 */       AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]");
/* 325 */       this.triedWhipLastTick = true;
/* 326 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 330 */     boolean targetUnderUs = ctx.helpers.isUnderTarget(ctx.opponent);
/* 331 */     boolean targetDiagonal = AttackMethods.isTargetDiagonal(ctx, ctx.opponent);
/*     */     
/* 333 */     if (ctx.config.debug())
/*     */     {
/* 335 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 337 */         Logger.norm("[We Frozen Target Unfrozen ON_PID] Spam attack mode - underUs: " + targetUnderUs + ", diagonal: " + targetDiagonal + ", triedWhipLastTick: " + this.triedWhipLastTick + ", timer: " + attackTimerTicks + ", synced: " + attacksSynced);
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 344 */     if (this.triedWhipLastTick) {
/*     */ 
/*     */       
/* 347 */       int i = this.random.nextInt(100);
/* 348 */       if (i < 50) {
/*     */ 
/*     */         
/* 351 */         boolean useTankMage = (opponentTimerTicks <= 1);
/* 352 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 354 */           Logger.norm("[We Frozen Target Unfrozen ON_PID] Whip failed - switching to Magic (roll: " + i + ", oppTimer: " + opponentTimerTicks + ", tankMage: " + useTankMage + ")");
/*     */         }
/* 356 */         AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]", true, useTankMage);
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 361 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 363 */           Logger.norm("[We Frozen Target Unfrozen ON_PID] Whip failed - switching to Range (roll: " + i + ")");
/*     */         }
/* 365 */         AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]");
/*     */       } 
/* 367 */       this.triedWhipLastTick = false;
/* 368 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 372 */     if (targetDiagonal) {
/*     */       
/* 374 */       int i = this.random.nextInt(100);
/*     */       
/* 376 */       if (i < 50) {
/*     */ 
/*     */         
/* 379 */         boolean useTankMage = (opponentTimerTicks <= 1);
/* 380 */         AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]", true, useTankMage);
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 385 */         AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]");
/*     */       } 
/* 387 */       this.triedWhipLastTick = false;
/* 388 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 392 */     if (targetUnderUs) {
/*     */       
/* 394 */       int i = this.random.nextInt(100);
/*     */       
/* 396 */       if (i < 60) {
/*     */ 
/*     */         
/* 399 */         AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]");
/*     */         
/* 401 */         this.triedWhipLastTick = true;
/* 402 */         return true;
/*     */       } 
/* 404 */       if (i < 70) {
/*     */ 
/*     */         
/* 407 */         AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]");
/* 408 */         this.triedWhipLastTick = false;
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 413 */         boolean useTankMage = (opponentTimerTicks <= 1);
/* 414 */         AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]", true, useTankMage);
/* 415 */         this.triedWhipLastTick = false;
/*     */       } 
/* 417 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 421 */     int roll = this.random.nextInt(100);
/* 422 */     if (roll < 50) {
/*     */ 
/*     */       
/* 425 */       boolean useTankMage = (opponentTimerTicks <= 1);
/* 426 */       AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]", true, useTankMage);
/*     */     
/*     */     }
/*     */     else {
/*     */       
/* 431 */       AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen ON_PID]");
/*     */     } 
/* 433 */     this.triedWhipLastTick = false;
/* 434 */     return true;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/WeFrozenTargetUnfrozenOnPidHandler.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */