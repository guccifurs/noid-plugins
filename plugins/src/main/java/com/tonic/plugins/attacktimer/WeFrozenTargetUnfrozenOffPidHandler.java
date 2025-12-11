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
/*     */ public class WeFrozenTargetUnfrozenOffPidHandler
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
/*  39 */     if (ctx.currentPidStatus != CombatStateContext.PidStatus.OFF_PID && ctx.currentPidStatus != CombatStateContext.PidStatus.UNKNOWN)
/*     */     {
/*     */       
/*  42 */       return false;
/*     */     }
/*     */ 
/*     */     
/*  46 */     ctx.helpers.setMindAction("We Frozen OFF_PID");
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
/*  66 */             Logger.norm("[We Frozen Target Unfrozen OFF_PID] Fakie triggered (" + fakieChance + "% chance, roll: " + i + ") - Attempting to equip: " + itemToEquip + " - Result: " + (equipped ? "Success" : "Failed (not in inventory or already equipped)"));
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
/*  93 */     if (this.ticksAtZero >= 3 && health < 105)
/*     */     {
/*  95 */       if (ctx.helpers.eatAngler())
/*     */       {
/*  97 */         if (ctx.config.logHandlerActions())
/*     */         {
/*  99 */           Logger.norm("[We Frozen Target Unfrozen OFF_PID] Timer at 0 for " + this.ticksAtZero + " ticks (idle 3+) and HP < 105 (" + health + "), eating anglerfish (timer: " + attackTimerTicks + ")");
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
/* 128 */             Logger.norm("[We Frozen Target Unfrozen OFF_PID] Redemption activated - HP: " + health + ", No food (anglers: 0, brews: 0), Opponent timer: " + oppTimerTicks);
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
/* 143 */             Logger.norm("[We Frozen Target Unfrozen OFF_PID] Prayer < 25 (" + prayerPoints + "), drank Sanfew serum");
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
/* 157 */           Logger.norm("[We Frozen Target Unfrozen OFF_PID] Out of brews and HP < 55 (" + health + "), eating angler");
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
/* 173 */             Logger.norm("[We Frozen Target Unfrozen OFF_PID] HP < 35 (" + health + "), eating anglerfish first, will brew on next tick");
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
/* 187 */           Logger.norm("[We Frozen Target Unfrozen OFF_PID] Drank brew after low HP anglerfish (next tick)");
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
/* 220 */             Logger.norm("[We Frozen Target Unfrozen OFF_PID] HP below 72 (" + health + "), drank Saradomin brew");
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
/* 236 */             Logger.norm("[We Frozen Target Unfrozen OFF_PID] HP >= 72 (" + health + ") but magic < 94 (" + magicLevel + "), drank Sanfew serum");
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
/* 254 */             Logger.norm("[We Frozen Target Unfrozen OFF_PID] Magic < 82 (" + magicLevel + ") AND HP < 60 (" + health + "), eating anglerfish first, will sanfew on next tick");
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
/* 267 */           Logger.norm("[We Frozen Target Unfrozen OFF_PID] Drank Sanfew serum after anglerfish (next tick)");
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
/* 288 */             Logger.norm("[We Frozen Target Unfrozen OFF_PID] Magic low (" + magicLevel + "), drank Sanfew serum");
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
/* 315 */     boolean targetUnderUs = ctx.helpers.isUnderTarget(ctx.opponent);
/* 316 */     boolean targetDiagonal = AttackMethods.isTargetDiagonal(ctx, ctx.opponent);
/*     */     
/* 318 */     if (ctx.config.combatAutomationLogging())
/*     */     {
/* 320 */       Logger.norm("[We Frozen Target Unfrozen OFF_PID] Spam attack mode - underUs: " + targetUnderUs + ", diagonal: " + targetDiagonal + ", triedWhipLastTick: " + this.triedWhipLastTick + ", timer: " + attackTimerTicks + ", synced: " + attacksSynced);
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 326 */     if (this.triedWhipLastTick) {
/*     */ 
/*     */       
/* 329 */       int i = this.random.nextInt(100);
/* 330 */       if (i < 50) {
/*     */ 
/*     */         
/* 333 */         boolean useTankMage = (opponentTimerTicks <= 1);
/* 334 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 336 */           Logger.norm("[We Frozen Target Unfrozen OFF_PID] Whip failed - switching to Magic (roll: " + i + ", oppTimer: " + opponentTimerTicks + ", tankMage: " + useTankMage + ")");
/*     */         }
/* 338 */         AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen OFF_PID]", true, useTankMage);
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 343 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 345 */           Logger.norm("[We Frozen Target Unfrozen OFF_PID] Whip failed - switching to Range (roll: " + i + ")");
/*     */         }
/* 347 */         AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen OFF_PID]");
/*     */       } 
/* 349 */       this.triedWhipLastTick = false;
/* 350 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 354 */     if (targetDiagonal) {
/*     */       
/* 356 */       int i = this.random.nextInt(100);
/*     */       
/* 358 */       if (i < 50) {
/*     */ 
/*     */         
/* 361 */         boolean useTankMage = (opponentTimerTicks <= 1);
/* 362 */         AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen OFF_PID]", true, useTankMage);
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 367 */         AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen OFF_PID]");
/*     */       } 
/* 369 */       this.triedWhipLastTick = false;
/* 370 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 374 */     if (targetUnderUs) {
/*     */       
/* 376 */       int i = this.random.nextInt(100);
/*     */       
/* 378 */       if (i < 60) {
/*     */ 
/*     */         
/* 381 */         AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen OFF_PID]");
/*     */         
/* 383 */         this.triedWhipLastTick = true;
/* 384 */         return true;
/*     */       } 
/* 386 */       if (i < 70) {
/*     */ 
/*     */         
/* 389 */         AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen OFF_PID]");
/* 390 */         this.triedWhipLastTick = false;
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 395 */         boolean useTankMage = (opponentTimerTicks <= 1);
/* 396 */         AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen OFF_PID]", true, useTankMage);
/* 397 */         this.triedWhipLastTick = false;
/*     */       } 
/* 399 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 403 */     int roll = this.random.nextInt(100);
/* 404 */     if (roll < 50) {
/*     */ 
/*     */       
/* 407 */       boolean useTankMage = (opponentTimerTicks <= 1);
/* 408 */       AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen OFF_PID]", true, useTankMage);
/*     */     
/*     */     }
/*     */     else {
/*     */       
/* 413 */       AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[We Frozen Target Unfrozen OFF_PID]");
/*     */     } 
/* 415 */     this.triedWhipLastTick = false;
/* 416 */     return true;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/WeFrozenTargetUnfrozenOffPidHandler.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */