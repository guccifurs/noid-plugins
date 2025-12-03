/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import com.tonic.api.game.CombatAPI;
/*     */ import com.tonic.api.game.MovementAPI;
/*     */ import com.tonic.api.widgets.InventoryAPI;
/*     */ import com.tonic.api.widgets.PrayerAPI;
/*     */ import com.tonic.data.wrappers.ItemEx;
/*     */ import java.util.Objects;
/*     */ import java.util.Random;
/*     */ import net.runelite.api.Player;
/*     */ import net.runelite.api.coords.WorldPoint;
/*     */ 
/*     */ 
/*     */ public class TargetFrozenWeUnfrozenOnPidHandler
/*     */   implements CombatStateHandler
/*     */ {
/*  19 */   private final Random random = new Random();
/*     */   private boolean needToWalkBackUnderTarget = false;
/*  21 */   private Player targetFrozenTarget = null;
/*     */   
/*     */   private boolean needToBrewAfterLowHPAngler = false;
/*     */   
/*  25 */   private int ticksSinceLowHPAngler = 0;
/*     */ 
/*     */   
/*     */   private boolean needToSanfewAfterBrew = false;
/*     */ 
/*     */   
/*     */   private boolean specModeActive = false;
/*     */ 
/*     */   
/*     */   private boolean didDDAttack = false;
/*     */ 
/*     */   
/*     */   private boolean needToWalkBackAfterDDAttack = false;
/*     */   
/*  39 */   private int ticksUnderTarget = 0;
/*  40 */   private Player lastUnderTargetPlayer = null;
/*     */ 
/*     */   
/*  43 */   private int runAwayTicksRemaining = 0;
/*     */ 
/*     */   
/*     */   private boolean waitingForAttackAnimation = false;
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean handle(CombatStateContext ctx) {
/*  51 */     if (!ctx.targetFrozen || ctx.weFrozen) {
/*     */ 
/*     */       
/*  54 */       this.needToWalkBackUnderTarget = false;
/*  55 */       this.targetFrozenTarget = null;
/*  56 */       this.specModeActive = false;
/*  57 */       this.didDDAttack = false;
/*  58 */       this.needToWalkBackAfterDDAttack = false;
/*  59 */       this.ticksUnderTarget = 0;
/*  60 */       this.lastUnderTargetPlayer = null;
/*  61 */       this.needToSanfewAfterBrew = false;
/*     */       
/*  63 */       this.runAwayTicksRemaining = 0;
/*  64 */       this.waitingForAttackAnimation = false;
/*  65 */       return false;
/*     */     } 
/*     */ 
/*     */     
/*  69 */     int attackTimerTicks = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*     */ 
/*     */     
/*  72 */     boolean isUnderTarget = (ctx.opponent != null) ? ctx.helpers.isUnderTarget(ctx.opponent) : false;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*  79 */     boolean shouldRunAway = (this.runAwayTicksRemaining > 0 || (ctx.opponentFreezeTimerTicks > 0 && ctx.opponentFreezeTimerTicks <= 4 && (isUnderTarget || ctx.opponentFreezeTimerTicks <= 2)));
/*     */ 
/*     */ 
/*     */     
/*  83 */     if (shouldRunAway) {
/*     */ 
/*     */       
/*  86 */       if (this.runAwayTicksRemaining == 0) {
/*     */ 
/*     */         
/*  89 */         this.runAwayTicksRemaining = 4;
/*  90 */         ctx.helpers.setMindAction("Running Away (" + this.runAwayTicksRemaining + " ticks)");
/*  91 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/*  93 */           Logger.norm("[Target Frozen ON_PID] Opponent freeze timer at " + ctx.opponentFreezeTimerTicks + " ticks (<= 4) - starting to run away for 4 ticks");
/*     */         }
/*     */       }
/*     */       else {
/*     */         
/*  98 */         ctx.helpers.setMindAction("Running Away (" + this.runAwayTicksRemaining + " ticks)");
/*     */       } 
/*     */ 
/*     */       
/* 102 */       if (this.waitingForAttackAnimation)
/*     */       {
/*     */         
/* 105 */         if (ctx.localPlayer != null) {
/*     */           
/* 107 */           int currentAnimation = ctx.localPlayer.getAnimation();
/*     */ 
/*     */           
/* 110 */           boolean isMagicAnimation = false;
/* 111 */           int[] magicAnims = { 7855, 7854, 7856, 1978, 1979, 711, 1167, 1162, 428, 763, 7853 };
/* 112 */           for (int magicAnim : magicAnims) {
/*     */             
/* 114 */             if (currentAnimation == magicAnim) {
/*     */               
/* 116 */               isMagicAnimation = true;
/*     */               
/*     */               break;
/*     */             } 
/*     */           } 
/*     */           
/* 122 */           boolean isRangeAnimation = false;
/* 123 */           int[] rangeAnims = { 9168, 9169, 9170, 7617, 7618, 9171, 9172, 4230, 7615, 7616, 426, 7619 };
/* 124 */           for (int rangeAnim : rangeAnims) {
/*     */             
/* 126 */             if (currentAnimation == rangeAnim) {
/*     */               
/* 128 */               isRangeAnimation = true;
/*     */               
/*     */               break;
/*     */             } 
/*     */           } 
/* 133 */           if (isMagicAnimation || isRangeAnimation) {
/*     */ 
/*     */             
/* 136 */             this.waitingForAttackAnimation = false;
/* 137 */             if (ctx.config.combatAutomationLogging())
/*     */             {
/* 139 */               Logger.norm("[Target Frozen ON_PID] Attack animation started (animation: " + currentAnimation + "), continuing run-away");
/*     */             
/*     */             }
/*     */           }
/*     */           else {
/*     */             
/* 145 */             ctx.helpers.setMindAction("Waiting for attack animation to start...");
/* 146 */             if (ctx.config.combatAutomationLogging())
/*     */             {
/* 148 */               Logger.norm("[Target Frozen ON_PID] Waiting for attack animation to start (current animation: " + currentAnimation + ")");
/*     */             }
/* 150 */             return true;
/*     */           } 
/*     */         } 
/*     */       }
/*     */ 
/*     */       
/* 156 */       if (attackTimerTicks == 1 && !this.waitingForAttackAnimation) {
/*     */         
/* 158 */         int i = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/* 159 */         boolean bool = (Math.abs(attackTimerTicks - i) <= 1);
/*     */         
/* 161 */         if (bool) {
/*     */ 
/*     */           
/* 164 */           int j = ctx.helpers.getPlayerMagicLevel();
/*     */           
/* 166 */           if (j >= 94)
/*     */           {
/* 168 */             ctx.helpers.equipGearFromConfig(ctx.config.tankMageGear(), PrayerAPI.AUGURY);
/* 169 */             boolean barrageCast = ctx.helpers.castBloodBarrage(ctx.opponent);
/* 170 */             if (!barrageCast)
/*     */             {
/* 172 */               AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */             }
/*     */           }
/* 175 */           else if (j >= 82)
/*     */           {
/* 177 */             ctx.helpers.equipGearFromConfig(ctx.config.tankMageGear(), PrayerAPI.AUGURY);
/* 178 */             boolean blitzCast = ctx.helpers.castBloodBlitz(ctx.opponent);
/* 179 */             if (!blitzCast)
/*     */             {
/* 181 */               AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */             }
/*     */           }
/*     */           else
/*     */           {
/* 186 */             AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */           }
/*     */         
/*     */         }
/*     */         else {
/*     */           
/* 192 */           int roll = this.random.nextInt(100);
/* 193 */           if (roll < 50) {
/*     */             
/* 195 */             AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */           }
/*     */           else {
/*     */             
/* 199 */             AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]", true, false, true, false);
/*     */           } 
/*     */         } 
/*     */ 
/*     */         
/* 204 */         this.waitingForAttackAnimation = true;
/* 205 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 207 */           Logger.norm("[Target Frozen ON_PID] Attack executed, waiting for animation to start before continuing run-away");
/*     */         }
/* 209 */         return true;
/*     */       } 
/*     */ 
/*     */ 
/*     */       
/* 214 */       if (!this.waitingForAttackAnimation) {
/*     */         
/* 216 */         ctx.helpers.walkAwayFromTarget(ctx.opponent, 10);
/*     */ 
/*     */         
/* 219 */         this.runAwayTicksRemaining--;
/*     */         
/* 221 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 223 */           Logger.norm("[Target Frozen ON_PID] Running away - " + this.runAwayTicksRemaining + " ticks remaining");
/*     */         }
/*     */       } 
/*     */       
/* 227 */       return true;
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 233 */     if (this.runAwayTicksRemaining > 0) {
/*     */       
/* 235 */       this.runAwayTicksRemaining = 0;
/* 236 */       this.waitingForAttackAnimation = false;
/*     */     } 
/*     */ 
/*     */     
/* 240 */     if (ctx.opponentFreezeTimerTicks > 2 && ctx.opponentFreezeTimerTicks <= 4 && !isUnderTarget && ctx.opponent != null) {
/*     */       
/* 242 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 243 */       ctx.helpers.clickUnderTarget(ctx.opponent);
/* 244 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 246 */         Logger.norm("[Target Frozen ON_PID] Freeze timer at " + ctx.opponentFreezeTimerTicks + " ticks (3-4) and not under target - getting under first before run-away");
/*     */       }
/* 248 */       return true;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 253 */     if (ctx.currentPidStatus != CombatStateContext.PidStatus.ON_PID && ctx.currentPidStatus != CombatStateContext.PidStatus.UNKNOWN)
/*     */     {
/*     */       
/* 256 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 260 */     ctx.helpers.setMindAction("Target Frozen ON_PID");
/*     */ 
/*     */     
/* 263 */     if (this.needToWalkBackAfterDDAttack && ctx.opponent != null) {
/*     */       
/* 265 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 267 */         Logger.norm("[Target Frozen ON_PID] Walking back under target after DD Attack");
/*     */       }
/* 269 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 270 */       ctx.helpers.clickUnderTarget(ctx.opponent);
/* 271 */       this.needToWalkBackAfterDDAttack = false;
/*     */       
/* 273 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 277 */     if (this.needToWalkBackUnderTarget && this.targetFrozenTarget != null) {
/*     */       
/* 279 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 281 */         Logger.norm("[Target Frozen ON_PID] Walking back under target after random walk-away");
/*     */       }
/* 283 */       ctx.helpers.clickUnderTarget(this.targetFrozenTarget);
/* 284 */       this.needToWalkBackUnderTarget = false;
/* 285 */       this.targetFrozenTarget = null;
/* 286 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 290 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*     */ 
/*     */     
/* 293 */     if (attackTimerTicks == 2 || attackTimerTicks == 3) {
/*     */       
/* 295 */       int fakieChance = ctx.config.fakieChance();
/* 296 */       if (fakieChance > 0 && fakieChance <= 100) {
/*     */         
/* 298 */         int roll = this.random.nextInt(100);
/* 299 */         if (roll < fakieChance) {
/*     */ 
/*     */           
/* 302 */           String itemToEquip = this.random.nextBoolean() ? "Abyssal tentacle" : "Dragon crossbow";
/* 303 */           boolean equipped = ctx.helpers.equipItemByName(itemToEquip);
/* 304 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 306 */             Logger.norm("[Target Frozen We Unfrozen ON_PID] Fakie triggered (" + fakieChance + "% chance, roll: " + roll + ") - Attempting to equip: " + itemToEquip + " - Result: " + (equipped ? "Success" : "Failed (not in inventory or already equipped)"));
/*     */           }
/*     */         } 
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 313 */     boolean attacksSynced = (Math.abs(attackTimerTicks - opponentTimerTicks) <= 1);
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 318 */     if (!this.specModeActive) {
/*     */ 
/*     */       
/* 321 */       boolean canWeMelee = AttackMethods.canWeMelee(ctx, ctx.opponent);
/*     */       
/* 323 */       if (canWeMelee) {
/*     */ 
/*     */         
/* 326 */         int anglerCount = 0;
/* 327 */         int brewCount = InventoryAPI.count(new String[] { "Saradomin brew(4)", "Saradomin brew(3)", "Saradomin brew(2)", "Saradomin brew(1)" });
/*     */ 
/*     */         
/* 330 */         for (ItemEx item : InventoryAPI.getItems()) {
/*     */           
/* 332 */           if (item != null && item.getName() != null && item.getName().toLowerCase().contains("angler"))
/*     */           {
/* 334 */             anglerCount += item.getQuantity();
/*     */           }
/*     */         } 
/*     */ 
/*     */         
/* 339 */         if (anglerCount < 2 && brewCount < 1) {
/*     */           
/* 341 */           int specEnergy = CombatAPI.getSpecEnergy();
/* 342 */           if (specEnergy >= 50) {
/*     */             
/* 344 */             this.specModeActive = true;
/* 345 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 347 */               Logger.norm("[Target Frozen ON_PID] FORCE SPEC: Low food (anglers: " + anglerCount + ", brews: " + brewCount + ") + can melee, entering spec mode (spec: " + specEnergy + "%)");
/*     */             }
/*     */           } 
/*     */         } 
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 355 */     if (ctx.opponent != null)
/*     */     {
/* 357 */       isUnderTarget = ctx.helpers.isUnderTarget(ctx.opponent);
/*     */     }
/*     */ 
/*     */     
/* 361 */     if (isUnderTarget && ctx.opponent == this.lastUnderTargetPlayer) {
/*     */ 
/*     */       
/* 364 */       this.ticksUnderTarget++;
/*     */     }
/* 366 */     else if (isUnderTarget && ctx.opponent != this.lastUnderTargetPlayer) {
/*     */ 
/*     */       
/* 369 */       this.ticksUnderTarget = 1;
/* 370 */       this.lastUnderTargetPlayer = ctx.opponent;
/*     */     
/*     */     }
/*     */     else {
/*     */       
/* 375 */       this.ticksUnderTarget = 0;
/* 376 */       this.lastUnderTargetPlayer = null;
/*     */     } 
/*     */     
/* 379 */     if (ctx.config.combatAutomationLogging())
/*     */     {
/* 381 */       Logger.norm("[Target Frozen ON_PID] State check - underTarget: " + isUnderTarget + ", ticksUnderTarget: " + this.ticksUnderTarget + ", myTimer: " + attackTimerTicks + ", oppTimer: " + opponentTimerTicks + ", synced: " + attacksSynced);
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 387 */     if (!isUnderTarget && this.runAwayTicksRemaining == 0) {
/*     */ 
/*     */       
/* 390 */       if (attackTimerTicks <= 1) {
/*     */ 
/*     */ 
/*     */         
/* 394 */         boolean canMeleeClose = AttackMethods.canWeMelee(ctx, ctx.opponent);
/*     */         
/* 396 */         boolean canMeleeNow = AttackMethods.canMeleeAttack(ctx, ctx.opponent);
/*     */ 
/*     */ 
/*     */         
/* 400 */         int roll = this.random.nextInt(100);
/* 401 */         boolean canUseMage = (!attacksSynced && opponentTimerTicks > 1 && !this.didDDAttack);
/*     */         
/* 403 */         if (this.didDDAttack) {
/*     */ 
/*     */           
/* 406 */           this.didDDAttack = false;
/*     */ 
/*     */           
/* 409 */           if (roll < 50) {
/*     */ 
/*     */             
/* 412 */             int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/* 413 */             boolean canWalkToMelee = (!ctx.weFrozen && distance < 6);
/* 414 */             if (canWalkToMelee)
/*     */             {
/*     */               
/* 417 */               int specEnergy = CombatAPI.getSpecEnergy();
/* 418 */               boolean shouldSpec = false;
/* 419 */               if (specEnergy >= 50) {
/*     */                 
/* 421 */                 int specRoll = this.random.nextInt(100);
/* 422 */                 if (specRoll < 10)
/*     */                 {
/* 424 */                   shouldSpec = true;
/*     */                 }
/*     */               } 
/*     */               
/* 428 */               if (shouldSpec)
/*     */               {
/*     */                 
/* 431 */                 if (AttackMethods.executeSpecialAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]")) {
/*     */                   
/* 433 */                   if (ctx.config.combatAutomationLogging())
/*     */                   {
/* 435 */                     Logger.norm("[Target Frozen ON_PID] DD Attack melee rolled special attack (10% chance, spec energy: " + specEnergy + "%)");
/*     */                   }
/* 437 */                   return true;
/*     */                 } 
/*     */               }
/*     */ 
/*     */ 
/*     */               
/* 443 */               ctx.helpers.equipGearFromConfig(ctx.config.meleeGear());
/* 444 */               ctx.helpers.attackPlayerWithFightOption(ctx.opponent);
/*     */             
/*     */             }
/*     */             else
/*     */             {
/* 449 */               AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */             }
/*     */           
/*     */           }
/*     */           else {
/*     */             
/* 455 */             AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */           } 
/* 457 */           return true;
/*     */         } 
/* 459 */         if (canMeleeNow && roll < 15) {
/*     */ 
/*     */           
/* 462 */           AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */         }
/* 464 */         else if (canUseMage && roll < 85) {
/*     */ 
/*     */           
/* 467 */           AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]", true, false, true, false);
/*     */         
/*     */         }
/*     */         else {
/*     */           
/* 472 */           AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */         } 
/* 474 */         return true;
/*     */       } 
/*     */ 
/*     */ 
/*     */       
/* 479 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 480 */       ctx.helpers.clickUnderTarget(ctx.opponent);
/* 481 */       return true;
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 489 */     if (attackTimerTicks > 2 && opponentTimerTicks > 2) {
/*     */       
/* 491 */       int ddChance = ctx.config.ddAttackChance();
/* 492 */       if (ddChance > 0 && ddChance <= 50) {
/*     */         
/* 494 */         int roll = this.random.nextInt(100);
/* 495 */         if (roll < ddChance) {
/*     */ 
/*     */           
/* 498 */           Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/*     */           
/* 500 */           if (playerPos != null) {
/*     */ 
/*     */             
/* 503 */             int[] directions = { 1, 1, 1, -1, -1, -1, -1, 1 };
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */             
/* 510 */             int dirIndex = this.random.nextInt(4) * 2;
/* 511 */             int dirX = directions[dirIndex];
/* 512 */             int dirY = directions[dirIndex + 1];
/*     */ 
/*     */             
/* 515 */             int distance = this.random.nextInt(3) + 1;
/*     */             
/* 517 */             int targetX = playerPos.getX() + dirX * distance;
/* 518 */             int targetY = playerPos.getY() + dirY * distance;
/*     */             
/* 520 */             WorldPoint diagonalTile = new WorldPoint(targetX, targetY, playerPos.getPlane());
/*     */ 
/*     */             
/* 523 */             MovementAPI.walkToWorldPoint(diagonalTile);
/*     */             
/* 525 */             String[] dirNames = { "NE", "SE", "SW", "NW" };
/* 526 */             if (ctx.config.combatAutomationLogging())
/*     */             {
/* 528 */               Logger.norm("[Target Frozen ON_PID] DD Attack triggered (" + ddChance + "% chance, roll: " + roll + ") - Stepping " + distance + " tiles " + dirNames[dirIndex / 2] + " (my timer: " + attackTimerTicks + ", opp timer: " + opponentTimerTicks + ", will walk back under next tick)");
/*     */             }
/*     */ 
/*     */ 
/*     */             
/* 533 */             this.didDDAttack = true;
/* 534 */             this.needToWalkBackAfterDDAttack = true;
/* 535 */             return true;
/*     */           } 
/*     */         } 
/*     */       } 
/*     */     } 
/*     */     
/* 541 */     int health = ctx.helpers.getPlayerHealth();
/* 542 */     int magicLevel = ctx.helpers.getPlayerMagicLevel();
/* 543 */     int strengthLevel = ctx.helpers.getPlayerStrengthLevel();
/* 544 */     int rangeLevel = ctx.helpers.getPlayerRangeLevel();
/* 545 */     int prayerPoints = ctx.helpers.getPlayerPrayerPoints();
/* 546 */     boolean hasBrews = ctx.helpers.hasBrews();
/*     */ 
/*     */     
/* 549 */     int oppTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/* 550 */     if (oppTimerTicks > 1 && health < 45 && !hasBrews) {
/*     */ 
/*     */       
/* 553 */       int anglerCount = 0;
/* 554 */       for (ItemEx item : InventoryAPI.getItems()) {
/*     */         
/* 556 */         if (item != null && item.getName() != null && item.getName().toLowerCase().contains("angler"))
/*     */         {
/* 558 */           anglerCount += item.getQuantity();
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/* 563 */       if (anglerCount == 0)
/*     */       {
/* 565 */         if (!PrayerAPI.REDEMPTION.isActive()) {
/*     */           
/* 567 */           PrayerAPI.REDEMPTION.turnOn();
/* 568 */           if (ctx.config.aiPrayersLogging())
/*     */           {
/* 570 */             Logger.norm("[Target Frozen ON_PID] Redemption activated - HP: " + health + ", No food (anglers: 0, brews: 0), Opponent timer: " + oppTimerTicks);
/*     */           }
/*     */         } 
/*     */       }
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 578 */     boolean performedEatingAction = false;
/*     */ 
/*     */ 
/*     */     
/* 582 */     if (prayerPoints < 25)
/*     */     {
/* 584 */       if (ctx.helpers.drinkSanfewSerum())
/*     */       {
/* 586 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 588 */           Logger.norm("[Target Frozen ON_PID] Prayer < 25 (" + prayerPoints + "), drank Sanfew serum (parallel to combat)");
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 596 */     boolean canEat = (this.ticksUnderTarget >= 2 && isUnderTarget);
/*     */     
/* 598 */     if (canEat) {
/*     */ 
/*     */       
/* 601 */       int targetHP = ctx.helpers.getTargetHPPercentage(ctx.opponent);
/*     */ 
/*     */ 
/*     */ 
/*     */       
/* 606 */       if (!hasBrews && this.needToSanfewAfterBrew)
/*     */       {
/* 608 */         this.needToSanfewAfterBrew = false;
/*     */       }
/*     */       
/* 611 */       if (targetHP >= 0 && targetHP < 55 && hasBrews) {
/*     */ 
/*     */         
/* 614 */         if (this.needToSanfewAfterBrew)
/*     */         {
/* 616 */           if (ctx.helpers.drinkSanfewSerum())
/*     */           {
/* 618 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 620 */               Logger.norm("[Target Frozen ON_PID] Target HP < 55 (" + targetHP + "), after brewing, magic < 82, drank Sanfew serum (parallel to combat)");
/*     */             }
/*     */ 
/*     */             
/* 624 */             this.needToSanfewAfterBrew = false;
/*     */           
/*     */           }
/*     */           else
/*     */           {
/* 629 */             this.needToSanfewAfterBrew = false;
/*     */ 
/*     */           
/*     */           }
/*     */ 
/*     */         
/*     */         }
/* 636 */         else if (health < 80)
/*     */         {
/* 638 */           if (ctx.helpers.sipSaradominBrew())
/*     */           {
/* 640 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 642 */               Logger.norm("[Target Frozen ON_PID] Target HP < 55 (" + targetHP + "), our HP < 80 (" + health + "), brewing towards 80+ (parallel to combat)");
/*     */             }
/*     */ 
/*     */ 
/*     */             
/* 647 */             int magicLevelAfterBrew = ctx.helpers.getPlayerMagicLevel();
/* 648 */             if (magicLevelAfterBrew < 82)
/*     */             {
/* 650 */               this.needToSanfewAfterBrew = true;
/* 651 */               if (ctx.config.logHandlerActions())
/*     */               {
/* 653 */                 Logger.norm("[Target Frozen ON_PID] Target HP < 55 (" + targetHP + "), after brewing, magic < 82 (" + magicLevelAfterBrew + "), will sanfew on next tick");
/*     */               
/*     */               }
/*     */             }
/*     */           
/*     */           }
/*     */         
/*     */         }
/* 661 */         else if (magicLevel < 82)
/*     */         {
/* 663 */           if (ctx.helpers.drinkSanfewSerum())
/*     */           {
/* 665 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 667 */               Logger.norm("[Target Frozen ON_PID] Target HP < 55 (" + targetHP + "), our HP >= 80 (" + health + "), magic < 82 (" + magicLevel + "), drank Sanfew serum");
/*     */ 
/*     */             
/*     */             }
/*     */ 
/*     */           
/*     */           }
/*     */         
/*     */         }
/* 676 */         else if (ctx.config.logHandlerActions())
/*     */         {
/* 678 */           Logger.norm("[Target Frozen ON_PID] Target HP < 55 (" + targetHP + "), our HP >= 80 (" + health + "), magic >= 82 (" + magicLevel + "), skipping eating to continue attacking");
/*     */ 
/*     */         
/*     */         }
/*     */ 
/*     */ 
/*     */       
/*     */       }
/*     */       else {
/*     */ 
/*     */ 
/*     */         
/* 690 */         if (!hasBrews && health < 55)
/*     */         {
/* 692 */           if (ctx.helpers.eatAngler()) {
/*     */             
/* 694 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 696 */               Logger.norm("[Target Frozen ON_PID] Out of brews and HP < 55 (" + health + "), eating angler");
/*     */             }
/* 698 */             performedEatingAction = true;
/*     */           } 
/*     */         }
/*     */ 
/*     */         
/* 703 */         if (health < 35) {
/*     */ 
/*     */           
/* 706 */           if (!this.needToBrewAfterLowHPAngler)
/*     */           {
/* 708 */             if (ctx.helpers.eatAngler())
/*     */             {
/* 710 */               if (ctx.config.logHandlerActions())
/*     */               {
/* 712 */                 Logger.norm("[Target Frozen ON_PID] HP < 35 (" + health + "), eating anglerfish first, will brew on next tick");
/*     */               }
/* 714 */               this.needToBrewAfterLowHPAngler = true;
/* 715 */               this.ticksSinceLowHPAngler = 0;
/* 716 */               performedEatingAction = true;
/*     */ 
/*     */             
/*     */             }
/*     */ 
/*     */           
/*     */           }
/* 723 */           else if (ctx.helpers.sipSaradominBrew())
/*     */           {
/* 725 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 727 */               Logger.norm("[Target Frozen ON_PID] Drank brew after low HP anglerfish (next tick, parallel to combat)");
/*     */             }
/* 729 */             this.needToBrewAfterLowHPAngler = false;
/* 730 */             this.ticksSinceLowHPAngler = 0;
/*     */           
/*     */           }
/*     */           else
/*     */           {
/*     */             
/* 736 */             this.needToBrewAfterLowHPAngler = false;
/* 737 */             this.ticksSinceLowHPAngler = 0;
/*     */ 
/*     */           
/*     */           }
/*     */ 
/*     */         
/*     */         }
/* 744 */         else if (this.needToBrewAfterLowHPAngler) {
/*     */           
/* 746 */           this.needToBrewAfterLowHPAngler = false;
/* 747 */           this.ticksSinceLowHPAngler = 0;
/*     */         } 
/*     */ 
/*     */ 
/*     */         
/* 752 */         if (health < 90)
/*     */         {
/* 754 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 756 */             Logger.norm("[Target Frozen ON_PID] Health low (" + health + "), eating angler");
/*     */           }
/* 758 */           if (ctx.helpers.eatAngler())
/*     */           {
/* 760 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 762 */               Logger.norm("[Target Frozen ON_PID] Ate anglerfish");
/*     */             }
/* 764 */             performedEatingAction = true;
/*     */ 
/*     */           
/*     */           }
/* 768 */           else if (ctx.config.logHandlerActions())
/*     */           {
/* 770 */             Logger.norm("[Target Frozen ON_PID] No anglerfish found in inventory");
/*     */           }
/*     */         
/*     */         }
/*     */       
/*     */       } 
/* 776 */     } else if (isUnderTarget && this.ticksUnderTarget < 2) {
/*     */ 
/*     */       
/* 779 */       if (ctx.config.logHandlerActions())
/*     */       {
/* 781 */         Logger.norm("[Target Frozen ON_PID] Under target but only " + this.ticksUnderTarget + " tick(s) - waiting 2 ticks before eating");
/*     */       }
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 788 */     if (canEat && health > 90 && magicLevel < 94) {
/*     */ 
/*     */ 
/*     */       
/* 792 */       int targetHP = ctx.helpers.getTargetHPPercentage(ctx.opponent);
/* 793 */       if (targetHP < 0 || targetHP >= 55)
/*     */       {
/* 795 */         if (ctx.helpers.drinkSanfewSerum())
/*     */         {
/* 797 */           if (ctx.config.debug())
/*     */           {
/* 799 */             Logger.norm("[Target Frozen ON_PID] HP > 90 (" + health + ") but magic low (" + magicLevel + "), drank Sanfew serum (parallel to combat)");
/*     */           }
/*     */         }
/*     */       }
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 808 */     if (canEat && strengthLevel < 110)
/*     */     {
/* 810 */       if (ctx.helpers.drinkSuperCombat())
/*     */       {
/* 812 */         if (ctx.config.debug())
/*     */         {
/* 814 */           Logger.norm("[Target Frozen ON_PID] Strength low (" + strengthLevel + "), drank Super combat potion (parallel to combat)");
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 822 */     if (canEat && rangeLevel < 110)
/*     */     {
/* 824 */       if (ctx.helpers.drinkBastion())
/*     */       {
/* 826 */         if (ctx.config.debug())
/*     */         {
/* 828 */           Logger.norm("[Target Frozen ON_PID] Range low (" + rangeLevel + "), drank Bastion potion (parallel to combat)");
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 837 */     if (performedEatingAction && attackTimerTicks != 1) {
/*     */       
/* 839 */       if (ctx.config.logHandlerActions())
/*     */       {
/* 841 */         Logger.norm("[Target Frozen ON_PID] Ate/drank this tick - skipping attack logic (timer: " + attackTimerTicks + ")");
/*     */       }
/*     */       
/* 844 */       return true;
/*     */     } 
/* 846 */     if (performedEatingAction && attackTimerTicks == 1)
/*     */     {
/* 848 */       if (ctx.config.logHandlerActions())
/*     */       {
/* 850 */         Logger.norm("[Target Frozen ON_PID] Ate anglerfish but timer is 1 - allowing attack on same tick");
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 860 */     if (attackTimerTicks <= 1 || (attackTimerTicks == 0 && ctx.ticksAtZero >= 3)) {
/*     */ 
/*     */       
/* 863 */       if (this.specModeActive) {
/*     */         
/* 865 */         boolean canWeMelee = AttackMethods.canWeMelee(ctx, ctx.opponent);
/* 866 */         if (canWeMelee) {
/*     */           
/* 868 */           boolean specExecuted = AttackMethods.executeSpecialAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/* 869 */           if (specExecuted)
/*     */           {
/* 871 */             this.specModeActive = false;
/* 872 */             return true;
/*     */           }
/*     */         
/*     */         }
/*     */         else {
/*     */           
/* 878 */           this.specModeActive = false;
/* 879 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 881 */             Logger.norm("[Target Frozen ON_PID] Exited spec mode - can no longer melee");
/*     */           }
/*     */         } 
/*     */       } 
/*     */ 
/*     */       
/* 887 */       if (this.didDDAttack) {
/*     */ 
/*     */         
/* 890 */         this.didDDAttack = false;
/*     */ 
/*     */         
/* 893 */         int roll = this.random.nextInt(100);
/* 894 */         if (roll < 50) {
/*     */ 
/*     */           
/* 897 */           int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/* 898 */           boolean canWalkToMelee = (!ctx.weFrozen && distance < 6);
/* 899 */           if (canWalkToMelee)
/*     */           {
/*     */             
/* 902 */             int specEnergy = CombatAPI.getSpecEnergy();
/* 903 */             boolean shouldSpec = false;
/* 904 */             if (specEnergy >= 50) {
/*     */               
/* 906 */               int specRoll = this.random.nextInt(100);
/* 907 */               if (specRoll < 10)
/*     */               {
/* 909 */                 shouldSpec = true;
/*     */               }
/*     */             } 
/*     */             
/* 913 */             if (shouldSpec)
/*     */             {
/*     */               
/* 916 */               if (AttackMethods.executeSpecialAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]")) {
/*     */                 
/* 918 */                 if (ctx.config.combatAutomationLogging())
/*     */                 {
/* 920 */                   Logger.norm("[Target Frozen ON_PID] DD Attack melee rolled special attack (10% chance, spec energy: " + specEnergy + "%)");
/*     */                 }
/* 922 */                 return true;
/*     */               } 
/*     */             }
/*     */ 
/*     */ 
/*     */             
/* 928 */             ctx.helpers.equipGearFromConfig(ctx.config.meleeGear());
/* 929 */             ctx.helpers.attackPlayerWithFightOption(ctx.opponent);
/*     */           
/*     */           }
/*     */           else
/*     */           {
/* 934 */             AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */           }
/*     */         
/*     */         }
/*     */         else {
/*     */           
/* 940 */           AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */         } 
/* 942 */         return true;
/*     */       } 
/*     */       
/* 945 */       boolean canMelee = AttackMethods.canMeleeAttack(ctx, ctx.opponent);
/*     */ 
/*     */       
/* 948 */       if (attacksSynced || (attackTimerTicks <= 0 && opponentTimerTicks <= 0)) {
/*     */         
/* 950 */         int roll = this.random.nextInt(100);
/* 951 */         if (canMelee && roll < 50)
/*     */         {
/*     */           
/* 954 */           AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */         
/*     */         }
/*     */         else
/*     */         {
/* 959 */           AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */         }
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 965 */         int roll = this.random.nextInt(100);
/* 966 */         boolean canUseMage = (!attacksSynced && opponentTimerTicks > 1);
/*     */         
/* 968 */         if (canMelee && roll < 15) {
/*     */ 
/*     */           
/* 971 */           AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */         }
/* 973 */         else if (canUseMage && roll < 85) {
/*     */ 
/*     */           
/* 976 */           AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]", true, false, true, false);
/*     */         
/*     */         }
/*     */         else {
/*     */           
/* 981 */           AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen ON_PID]");
/*     */         } 
/*     */       } 
/* 984 */       return true;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 989 */     if (this.runAwayTicksRemaining == 0) {
/*     */       
/* 991 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 992 */       ctx.helpers.clickUnderTarget(ctx.opponent);
/*     */     } 
/* 994 */     return true;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/TargetFrozenWeUnfrozenOnPidHandler.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */