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
/*     */ public class TargetFrozenWeUnfrozenOffPidHandler
/*     */   implements CombatStateHandler
/*     */ {
/*  19 */   private final Random random = new Random();
/*     */   
/*     */   private boolean needToBrewAfterLowHPAngler = false;
/*     */   
/*  23 */   private int ticksSinceLowHPAngler = 0;
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
/*  37 */   private int ticksUnderTarget = 0;
/*  38 */   private Player lastUnderTargetPlayer = null;
/*     */ 
/*     */   
/*  41 */   private int runAwayTicksRemaining = 0;
/*     */ 
/*     */   
/*     */   private boolean waitingForAttackAnimation = false;
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean handle(CombatStateContext ctx) {
/*  49 */     if (!ctx.targetFrozen || ctx.weFrozen) {
/*     */       
/*  51 */       this.specModeActive = false;
/*  52 */       this.didDDAttack = false;
/*  53 */       this.needToWalkBackAfterDDAttack = false;
/*  54 */       this.ticksUnderTarget = 0;
/*  55 */       this.lastUnderTargetPlayer = null;
/*  56 */       this.needToSanfewAfterBrew = false;
/*     */       
/*  58 */       this.runAwayTicksRemaining = 0;
/*  59 */       this.waitingForAttackAnimation = false;
/*  60 */       return false;
/*     */     } 
/*     */     
/*  63 */     if (ctx.currentPidStatus != CombatStateContext.PidStatus.OFF_PID)
/*     */     {
/*  65 */       return false;
/*     */     }
/*     */ 
/*     */     
/*  69 */     ctx.helpers.setMindAction("Target Frozen OFF_PID");
/*     */ 
/*     */     
/*  72 */     if (this.needToWalkBackAfterDDAttack && ctx.opponent != null) {
/*     */       
/*  74 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/*  76 */         Logger.norm("[Target Frozen OFF_PID] Walking back under target after DD Attack");
/*     */       }
/*  78 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/*  79 */       ctx.helpers.clickUnderTarget(ctx.opponent);
/*  80 */       this.needToWalkBackAfterDDAttack = false;
/*     */       
/*  82 */       return true;
/*     */     } 
/*     */ 
/*     */     
/*  86 */     int attackTimerTicks = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*  87 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*     */ 
/*     */     
/*  90 */     boolean isUnderTarget = (ctx.opponent != null) ? ctx.helpers.isUnderTarget(ctx.opponent) : false;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*  97 */     boolean shouldRunAway = (this.runAwayTicksRemaining > 0 || (ctx.opponentFreezeTimerTicks > 0 && ctx.opponentFreezeTimerTicks <= 4 && (isUnderTarget || ctx.opponentFreezeTimerTicks <= 2)));
/*     */ 
/*     */ 
/*     */     
/* 101 */     if (shouldRunAway) {
/*     */ 
/*     */       
/* 104 */       if (this.runAwayTicksRemaining == 0) {
/*     */ 
/*     */         
/* 107 */         this.runAwayTicksRemaining = 4;
/* 108 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 110 */           Logger.norm("[Target Frozen OFF_PID] Opponent freeze timer at " + ctx.opponentFreezeTimerTicks + " ticks (<= 4) - starting to run away for 4 ticks");
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/* 115 */       if (this.waitingForAttackAnimation)
/*     */       {
/*     */         
/* 118 */         if (ctx.localPlayer != null) {
/*     */           
/* 120 */           int currentAnimation = ctx.localPlayer.getAnimation();
/*     */ 
/*     */           
/* 123 */           boolean isMagicAnimation = false;
/* 124 */           int[] magicAnims = { 7855, 7854, 7856, 1978, 1979, 711, 1167, 1162, 428, 763, 7853 };
/* 125 */           for (int magicAnim : magicAnims) {
/*     */             
/* 127 */             if (currentAnimation == magicAnim) {
/*     */               
/* 129 */               isMagicAnimation = true;
/*     */               
/*     */               break;
/*     */             } 
/*     */           } 
/*     */           
/* 135 */           boolean isRangeAnimation = false;
/* 136 */           int[] rangeAnims = { 9168, 9169, 9170, 7617, 7618, 9171, 9172, 4230, 7615, 7616, 426, 7619 };
/* 137 */           for (int rangeAnim : rangeAnims) {
/*     */             
/* 139 */             if (currentAnimation == rangeAnim) {
/*     */               
/* 141 */               isRangeAnimation = true;
/*     */               
/*     */               break;
/*     */             } 
/*     */           } 
/* 146 */           if (isMagicAnimation || isRangeAnimation) {
/*     */ 
/*     */             
/* 149 */             this.waitingForAttackAnimation = false;
/* 150 */             if (ctx.config.combatAutomationLogging())
/*     */             {
/* 152 */               Logger.norm("[Target Frozen OFF_PID] Attack animation started (animation: " + currentAnimation + "), continuing run-away");
/*     */             
/*     */             }
/*     */           }
/*     */           else {
/*     */             
/* 158 */             ctx.helpers.setMindAction("Waiting for attack animation to start...");
/* 159 */             if (ctx.config.combatAutomationLogging())
/*     */             {
/* 161 */               Logger.norm("[Target Frozen OFF_PID] Waiting for attack animation to start (current animation: " + currentAnimation + ")");
/*     */             }
/* 163 */             return true;
/*     */           } 
/*     */         } 
/*     */       }
/*     */ 
/*     */       
/* 169 */       if (attackTimerTicks == 1 && !this.waitingForAttackAnimation) {
/*     */         
/* 171 */         boolean attacksSynced = (Math.abs(attackTimerTicks - opponentTimerTicks) <= 1);
/*     */         
/* 173 */         if (attacksSynced) {
/*     */ 
/*     */           
/* 176 */           int i = ctx.helpers.getPlayerMagicLevel();
/*     */           
/* 178 */           if (i >= 94)
/*     */           {
/* 180 */             ctx.helpers.equipGearFromConfig(ctx.config.tankMageGear(), PrayerAPI.AUGURY);
/* 181 */             boolean barrageCast = ctx.helpers.castBloodBarrage(ctx.opponent);
/* 182 */             if (!barrageCast)
/*     */             {
/* 184 */               AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */             }
/*     */           }
/* 187 */           else if (i >= 82)
/*     */           {
/* 189 */             ctx.helpers.equipGearFromConfig(ctx.config.tankMageGear(), PrayerAPI.AUGURY);
/* 190 */             boolean blitzCast = ctx.helpers.castBloodBlitz(ctx.opponent);
/* 191 */             if (!blitzCast)
/*     */             {
/* 193 */               AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */             }
/*     */           }
/*     */           else
/*     */           {
/* 198 */             AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */           }
/*     */         
/*     */         }
/*     */         else {
/*     */           
/* 204 */           int roll = this.random.nextInt(100);
/* 205 */           if (roll < 50) {
/*     */             
/* 207 */             AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */           }
/*     */           else {
/*     */             
/* 211 */             AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]", true, false, true, false);
/*     */           } 
/*     */         } 
/*     */ 
/*     */         
/* 216 */         this.waitingForAttackAnimation = true;
/* 217 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 219 */           Logger.norm("[Target Frozen OFF_PID] Attack executed, waiting for animation to start before continuing run-away");
/*     */         }
/* 221 */         return true;
/*     */       } 
/*     */ 
/*     */ 
/*     */       
/* 226 */       if (!this.waitingForAttackAnimation) {
/*     */         
/* 228 */         ctx.helpers.walkAwayFromTarget(ctx.opponent, 10);
/*     */ 
/*     */         
/* 231 */         this.runAwayTicksRemaining--;
/*     */         
/* 233 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 235 */           Logger.norm("[Target Frozen OFF_PID] Running away - " + this.runAwayTicksRemaining + " ticks remaining");
/*     */         }
/*     */       } 
/*     */       
/* 239 */       return true;
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 245 */     if (this.runAwayTicksRemaining > 0) {
/*     */       
/* 247 */       this.runAwayTicksRemaining = 0;
/* 248 */       this.waitingForAttackAnimation = false;
/*     */     } 
/*     */ 
/*     */     
/* 252 */     if (ctx.opponentFreezeTimerTicks > 2 && ctx.opponentFreezeTimerTicks <= 4 && !isUnderTarget && ctx.opponent != null) {
/*     */       
/* 254 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 255 */       ctx.helpers.clickUnderTarget(ctx.opponent);
/* 256 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 258 */         Logger.norm("[Target Frozen OFF_PID] Freeze timer at " + ctx.opponentFreezeTimerTicks + " ticks (3-4) and not under target - getting under first before run-away");
/*     */       }
/* 260 */       return true;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 265 */     if (attackTimerTicks == 2 || attackTimerTicks == 3) {
/*     */       
/* 267 */       int fakieChance = ctx.config.fakieChance();
/* 268 */       if (fakieChance > 0 && fakieChance <= 100) {
/*     */         
/* 270 */         int roll = this.random.nextInt(100);
/* 271 */         if (roll < fakieChance) {
/*     */ 
/*     */           
/* 274 */           String itemToEquip = this.random.nextBoolean() ? "Abyssal tentacle" : "Dragon crossbow";
/* 275 */           boolean equipped = ctx.helpers.equipItemByName(itemToEquip);
/* 276 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 278 */             Logger.norm("[Target Frozen We Unfrozen OFF_PID] Fakie triggered (" + fakieChance + "% chance, roll: " + roll + ") - Attempting to equip: " + itemToEquip + " - Result: " + (equipped ? "Success" : "Failed (not in inventory or already equipped)"));
/*     */           }
/*     */         } 
/*     */       } 
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 287 */     if (!this.specModeActive) {
/*     */ 
/*     */       
/* 290 */       boolean canWeMelee = AttackMethods.canWeMelee(ctx, ctx.opponent);
/*     */       
/* 292 */       if (canWeMelee) {
/*     */ 
/*     */         
/* 295 */         int anglerCount = 0;
/* 296 */         int brewCount = InventoryAPI.count(new String[] { "Saradomin brew(4)", "Saradomin brew(3)", "Saradomin brew(2)", "Saradomin brew(1)" });
/*     */ 
/*     */         
/* 299 */         for (ItemEx item : InventoryAPI.getItems()) {
/*     */           
/* 301 */           if (item != null && item.getName() != null && item.getName().toLowerCase().contains("angler"))
/*     */           {
/* 303 */             anglerCount += item.getQuantity();
/*     */           }
/*     */         } 
/*     */ 
/*     */         
/* 308 */         if (anglerCount < 2 && brewCount < 1) {
/*     */           
/* 310 */           int specEnergy = CombatAPI.getSpecEnergy();
/* 311 */           if (specEnergy >= 50) {
/*     */             
/* 313 */             this.specModeActive = true;
/* 314 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 316 */               Logger.norm("[Target Frozen OFF_PID] FORCE SPEC: Low food (anglers: " + anglerCount + ", brews: " + brewCount + ") + can melee, entering spec mode (spec: " + specEnergy + "%)");
/*     */             }
/*     */           } 
/*     */         } 
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 324 */     if (ctx.opponent != null)
/*     */     {
/* 326 */       isUnderTarget = ctx.helpers.isUnderTarget(ctx.opponent);
/*     */     }
/*     */ 
/*     */     
/* 330 */     if (isUnderTarget && ctx.opponent == this.lastUnderTargetPlayer) {
/*     */ 
/*     */       
/* 333 */       this.ticksUnderTarget++;
/*     */     }
/* 335 */     else if (isUnderTarget && ctx.opponent != this.lastUnderTargetPlayer) {
/*     */ 
/*     */       
/* 338 */       this.ticksUnderTarget = 1;
/* 339 */       this.lastUnderTargetPlayer = ctx.opponent;
/*     */     
/*     */     }
/*     */     else {
/*     */       
/* 344 */       this.ticksUnderTarget = 0;
/* 345 */       this.lastUnderTargetPlayer = null;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 350 */     if (!isUnderTarget && this.runAwayTicksRemaining == 0) {
/*     */ 
/*     */ 
/*     */       
/* 354 */       if (attackTimerTicks <= 1 || (attackTimerTicks == 0 && ctx.ticksAtZero >= 3)) {
/*     */ 
/*     */         
/* 357 */         int roll = this.random.nextInt(100);
/*     */ 
/*     */         
/* 360 */         boolean canMeleeClose = AttackMethods.canWeMelee(ctx, ctx.opponent);
/*     */         
/* 362 */         boolean canMeleeNow = AttackMethods.canMeleeAttack(ctx, ctx.opponent);
/*     */ 
/*     */ 
/*     */         
/* 366 */         boolean canUseMage = !this.didDDAttack;
/*     */         
/* 368 */         if (this.didDDAttack) {
/*     */ 
/*     */           
/* 371 */           this.didDDAttack = false;
/*     */ 
/*     */           
/* 374 */           if (roll < 50) {
/*     */ 
/*     */             
/* 377 */             int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/* 378 */             boolean canWalkToMelee = (!ctx.weFrozen && distance < 6);
/* 379 */             if (canWalkToMelee)
/*     */             {
/*     */               
/* 382 */               int specEnergy = CombatAPI.getSpecEnergy();
/* 383 */               boolean shouldSpec = false;
/* 384 */               if (specEnergy >= 50) {
/*     */                 
/* 386 */                 int specRoll = this.random.nextInt(100);
/* 387 */                 if (specRoll < 10)
/*     */                 {
/* 389 */                   shouldSpec = true;
/*     */                 }
/*     */               } 
/*     */               
/* 393 */               if (shouldSpec)
/*     */               {
/*     */                 
/* 396 */                 if (AttackMethods.executeSpecialAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]")) {
/*     */                   
/* 398 */                   if (ctx.config.combatAutomationLogging())
/*     */                   {
/* 400 */                     Logger.norm("[Target Frozen OFF_PID] DD Attack melee rolled special attack (10% chance, spec energy: " + specEnergy + "%)");
/*     */                   }
/* 402 */                   return true;
/*     */                 } 
/*     */               }
/*     */ 
/*     */ 
/*     */               
/* 408 */               ctx.helpers.equipGearFromConfig(ctx.config.meleeGear());
/* 409 */               ctx.helpers.attackPlayerWithFightOption(ctx.opponent);
/*     */             
/*     */             }
/*     */             else
/*     */             {
/* 414 */               AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */             }
/*     */           
/*     */           }
/*     */           else {
/*     */             
/* 420 */             AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */           } 
/* 422 */           return true;
/*     */         } 
/* 424 */         if (canMeleeNow && roll < 40) {
/*     */ 
/*     */           
/* 427 */           AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */         }
/* 429 */         else if (roll < 65) {
/*     */ 
/*     */           
/* 432 */           AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */         }
/* 434 */         else if (canUseMage) {
/*     */ 
/*     */           
/* 437 */           AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]", true, false, true, false);
/*     */         
/*     */         }
/*     */         else {
/*     */           
/* 442 */           AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */         } 
/* 444 */         return true;
/*     */       } 
/*     */ 
/*     */ 
/*     */       
/* 449 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 450 */       ctx.helpers.clickUnderTarget(ctx.opponent);
/* 451 */       return true;
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 459 */     if (attackTimerTicks > 2 && opponentTimerTicks > 2) {
/*     */       
/* 461 */       int ddChance = ctx.config.ddAttackChance();
/* 462 */       if (ddChance > 0 && ddChance <= 50) {
/*     */         
/* 464 */         int roll = this.random.nextInt(100);
/* 465 */         if (roll < ddChance) {
/*     */ 
/*     */           
/* 468 */           Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/*     */           
/* 470 */           if (playerPos != null) {
/*     */ 
/*     */             
/* 473 */             int[] directions = { 1, 1, 1, -1, -1, -1, -1, 1 };
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */             
/* 480 */             int dirIndex = this.random.nextInt(4) * 2;
/* 481 */             int dirX = directions[dirIndex];
/* 482 */             int dirY = directions[dirIndex + 1];
/*     */ 
/*     */             
/* 485 */             int distance = this.random.nextInt(3) + 1;
/*     */             
/* 487 */             int targetX = playerPos.getX() + dirX * distance;
/* 488 */             int targetY = playerPos.getY() + dirY * distance;
/*     */             
/* 490 */             WorldPoint diagonalTile = new WorldPoint(targetX, targetY, playerPos.getPlane());
/*     */ 
/*     */             
/* 493 */             MovementAPI.walkToWorldPoint(diagonalTile);
/*     */             
/* 495 */             String[] dirNames = { "NE", "SE", "SW", "NW" };
/* 496 */             if (ctx.config.combatAutomationLogging())
/*     */             {
/* 498 */               Logger.norm("[Target Frozen OFF_PID] DD Attack triggered (" + ddChance + "% chance, roll: " + roll + ") - Stepping " + distance + " tiles " + dirNames[dirIndex / 2] + " (my timer: " + attackTimerTicks + ", opp timer: " + opponentTimerTicks + ", will walk back under next tick)");
/*     */             }
/*     */ 
/*     */ 
/*     */             
/* 503 */             this.didDDAttack = true;
/* 504 */             this.needToWalkBackAfterDDAttack = true;
/* 505 */             return true;
/*     */           } 
/*     */         } 
/*     */       } 
/*     */     } 
/*     */     
/* 511 */     int health = ctx.helpers.getPlayerHealth();
/* 512 */     int magicLevel = ctx.helpers.getPlayerMagicLevel();
/* 513 */     int strengthLevel = ctx.helpers.getPlayerStrengthLevel();
/* 514 */     int rangeLevel = ctx.helpers.getPlayerRangeLevel();
/* 515 */     int prayerPoints = ctx.helpers.getPlayerPrayerPoints();
/* 516 */     boolean hasBrews = ctx.helpers.hasBrews();
/*     */ 
/*     */     
/* 519 */     int oppTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/* 520 */     if (oppTimerTicks > 1 && health < 45 && !hasBrews) {
/*     */ 
/*     */       
/* 523 */       int anglerCount = 0;
/* 524 */       for (ItemEx item : InventoryAPI.getItems()) {
/*     */         
/* 526 */         if (item != null && item.getName() != null && item.getName().toLowerCase().contains("angler"))
/*     */         {
/* 528 */           anglerCount += item.getQuantity();
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/* 533 */       if (anglerCount == 0)
/*     */       {
/* 535 */         if (!PrayerAPI.REDEMPTION.isActive()) {
/*     */           
/* 537 */           PrayerAPI.REDEMPTION.turnOn();
/* 538 */           if (ctx.config.aiPrayersLogging())
/*     */           {
/* 540 */             Logger.norm("[Target Frozen OFF_PID] Redemption activated - HP: " + health + ", No food (anglers: 0, brews: 0), Opponent timer: " + oppTimerTicks);
/*     */           }
/*     */         } 
/*     */       }
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 548 */     boolean performedEatingAction = false;
/*     */ 
/*     */ 
/*     */     
/* 552 */     if (prayerPoints < 25)
/*     */     {
/* 554 */       if (ctx.helpers.drinkSanfewSerum())
/*     */       {
/* 556 */         if (ctx.config.debug())
/*     */         {
/* 558 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 560 */             Logger.norm("[Target Frozen OFF_PID] Prayer < 25 (" + prayerPoints + "), drank Sanfew serum (parallel to combat)");
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 569 */     boolean canEat = (this.ticksUnderTarget >= 2 && isUnderTarget);
/*     */     
/* 571 */     if (canEat) {
/*     */ 
/*     */       
/* 574 */       int targetHP = ctx.helpers.getTargetHPPercentage(ctx.opponent);
/*     */ 
/*     */ 
/*     */ 
/*     */       
/* 579 */       if (!hasBrews && this.needToSanfewAfterBrew)
/*     */       {
/* 581 */         this.needToSanfewAfterBrew = false;
/*     */       }
/*     */       
/* 584 */       if (targetHP >= 0 && targetHP < 55 && hasBrews) {
/*     */ 
/*     */         
/* 587 */         if (this.needToSanfewAfterBrew)
/*     */         {
/* 589 */           if (ctx.helpers.drinkSanfewSerum())
/*     */           {
/* 591 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 593 */               Logger.norm("[Target Frozen OFF_PID] Target HP < 55 (" + targetHP + "), after brewing, magic < 82, drank Sanfew serum (parallel to combat)");
/*     */             }
/*     */ 
/*     */             
/* 597 */             this.needToSanfewAfterBrew = false;
/*     */           
/*     */           }
/*     */           else
/*     */           {
/* 602 */             this.needToSanfewAfterBrew = false;
/*     */ 
/*     */           
/*     */           }
/*     */ 
/*     */         
/*     */         }
/* 609 */         else if (health < 80)
/*     */         {
/* 611 */           if (ctx.helpers.sipSaradominBrew())
/*     */           {
/* 613 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 615 */               Logger.norm("[Target Frozen OFF_PID] Target HP < 55 (" + targetHP + "), our HP < 80 (" + health + "), brewing towards 80+ (parallel to combat)");
/*     */             }
/*     */ 
/*     */ 
/*     */             
/* 620 */             int magicLevelAfterBrew = ctx.helpers.getPlayerMagicLevel();
/* 621 */             if (magicLevelAfterBrew < 82)
/*     */             {
/* 623 */               this.needToSanfewAfterBrew = true;
/* 624 */               if (ctx.config.logHandlerActions())
/*     */               {
/* 626 */                 Logger.norm("[Target Frozen OFF_PID] Target HP < 55 (" + targetHP + "), after brewing, magic < 82 (" + magicLevelAfterBrew + "), will sanfew on next tick");
/*     */               
/*     */               }
/*     */             }
/*     */           
/*     */           }
/*     */         
/*     */         }
/* 634 */         else if (magicLevel < 82)
/*     */         {
/* 636 */           if (ctx.helpers.drinkSanfewSerum())
/*     */           {
/* 638 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 640 */               Logger.norm("[Target Frozen OFF_PID] Target HP < 55 (" + targetHP + "), our HP >= 80 (" + health + "), magic < 82 (" + magicLevel + "), drank Sanfew serum");
/*     */ 
/*     */             
/*     */             }
/*     */ 
/*     */           
/*     */           }
/*     */         
/*     */         }
/* 649 */         else if (ctx.config.logHandlerActions())
/*     */         {
/* 651 */           Logger.norm("[Target Frozen OFF_PID] Target HP < 55 (" + targetHP + "), our HP >= 80 (" + health + "), magic >= 82 (" + magicLevel + "), skipping eating to continue attacking");
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
/* 663 */         if (!hasBrews && health < 55)
/*     */         {
/* 665 */           if (ctx.helpers.eatAngler()) {
/*     */             
/* 667 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 669 */               Logger.norm("[Target Frozen OFF_PID] Out of brews and HP < 55 (" + health + "), eating angler");
/*     */             }
/* 671 */             performedEatingAction = true;
/*     */           } 
/*     */         }
/*     */ 
/*     */         
/* 676 */         if (health < 35) {
/*     */ 
/*     */           
/* 679 */           if (!this.needToBrewAfterLowHPAngler)
/*     */           {
/* 681 */             if (ctx.helpers.eatAngler())
/*     */             {
/* 683 */               if (ctx.config.logHandlerActions())
/*     */               {
/* 685 */                 Logger.norm("[Target Frozen OFF_PID] HP < 35 (" + health + "), eating anglerfish first, will brew on next tick");
/*     */               }
/* 687 */               this.needToBrewAfterLowHPAngler = true;
/* 688 */               this.ticksSinceLowHPAngler = 0;
/* 689 */               performedEatingAction = true;
/*     */ 
/*     */             
/*     */             }
/*     */ 
/*     */           
/*     */           }
/* 696 */           else if (ctx.helpers.sipSaradominBrew())
/*     */           {
/* 698 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 700 */               Logger.norm("[Target Frozen OFF_PID] Drank brew after low HP anglerfish (next tick, parallel to combat)");
/*     */             }
/* 702 */             this.needToBrewAfterLowHPAngler = false;
/* 703 */             this.ticksSinceLowHPAngler = 0;
/*     */           
/*     */           }
/*     */           else
/*     */           {
/*     */             
/* 709 */             this.needToBrewAfterLowHPAngler = false;
/* 710 */             this.ticksSinceLowHPAngler = 0;
/*     */ 
/*     */           
/*     */           }
/*     */ 
/*     */         
/*     */         }
/* 717 */         else if (this.needToBrewAfterLowHPAngler) {
/*     */           
/* 719 */           this.needToBrewAfterLowHPAngler = false;
/* 720 */           this.ticksSinceLowHPAngler = 0;
/*     */         } 
/*     */ 
/*     */ 
/*     */         
/* 725 */         if (health < 90)
/*     */         {
/* 727 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 729 */             Logger.norm("[Target Frozen OFF_PID] Health low (" + health + "), eating angler");
/*     */           }
/* 731 */           if (ctx.helpers.eatAngler())
/*     */           {
/* 733 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 735 */               Logger.norm("[Target Frozen OFF_PID] Ate anglerfish");
/*     */             }
/* 737 */             performedEatingAction = true;
/*     */ 
/*     */           
/*     */           }
/* 741 */           else if (ctx.config.logHandlerActions())
/*     */           {
/* 743 */             Logger.norm("[Target Frozen OFF_PID] No anglerfish found in inventory");
/*     */           }
/*     */         
/*     */         }
/*     */       
/*     */       } 
/* 749 */     } else if (isUnderTarget && this.ticksUnderTarget < 2) {
/*     */ 
/*     */       
/* 752 */       if (ctx.config.logHandlerActions())
/*     */       {
/* 754 */         Logger.norm("[Target Frozen OFF_PID] Under target but only " + this.ticksUnderTarget + " tick(s) - waiting 2 ticks before eating");
/*     */       }
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 761 */     if (canEat && health > 90 && magicLevel < 94) {
/*     */ 
/*     */ 
/*     */       
/* 765 */       int targetHP = ctx.helpers.getTargetHPPercentage(ctx.opponent);
/* 766 */       if (targetHP < 0 || targetHP >= 55)
/*     */       {
/* 768 */         if (ctx.helpers.drinkSanfewSerum())
/*     */         {
/* 770 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 772 */             Logger.norm("[Target Frozen OFF_PID] HP > 90 (" + health + ") but magic low (" + magicLevel + "), drank Sanfew serum (parallel to combat)");
/*     */           }
/*     */         }
/*     */       }
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 781 */     if (canEat && strengthLevel < 110)
/*     */     {
/* 783 */       if (ctx.helpers.drinkSuperCombat())
/*     */       {
/* 785 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 787 */           Logger.norm("[Target Frozen OFF_PID] Strength low (" + strengthLevel + "), drank Super combat potion (parallel to combat)");
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 795 */     if (canEat && rangeLevel < 110)
/*     */     {
/* 797 */       if (ctx.helpers.drinkBastion())
/*     */       {
/* 799 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 801 */           Logger.norm("[Target Frozen OFF_PID] Range low (" + rangeLevel + "), drank Bastion potion (parallel to combat)");
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 810 */     if (performedEatingAction && attackTimerTicks != 1) {
/*     */       
/* 812 */       if (ctx.config.logHandlerActions())
/*     */       {
/* 814 */         Logger.norm("[Target Frozen OFF_PID] Ate/drank this tick - skipping attack logic (timer: " + attackTimerTicks + ")");
/*     */       }
/*     */       
/* 817 */       return true;
/*     */     } 
/* 819 */     if (performedEatingAction && attackTimerTicks == 1)
/*     */     {
/* 821 */       if (ctx.config.logHandlerActions())
/*     */       {
/* 823 */         Logger.norm("[Target Frozen OFF_PID] Ate anglerfish but timer is 1 - allowing attack on same tick");
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 831 */     if (attackTimerTicks <= 1 || (attackTimerTicks == 0 && ctx.ticksAtZero >= 3)) {
/*     */ 
/*     */       
/* 834 */       if (this.specModeActive) {
/*     */         
/* 836 */         boolean canWeMelee = AttackMethods.canWeMelee(ctx, ctx.opponent);
/* 837 */         if (canWeMelee) {
/*     */           
/* 839 */           boolean specExecuted = AttackMethods.executeSpecialAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/* 840 */           if (specExecuted)
/*     */           {
/* 842 */             this.specModeActive = false;
/* 843 */             return true;
/*     */           }
/*     */         
/*     */         }
/*     */         else {
/*     */           
/* 849 */           this.specModeActive = false;
/* 850 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 852 */             Logger.norm("[Target Frozen OFF_PID] Exited spec mode - can no longer melee");
/*     */           }
/*     */         } 
/*     */       } 
/*     */ 
/*     */       
/* 858 */       if (this.didDDAttack) {
/*     */ 
/*     */         
/* 861 */         this.didDDAttack = false;
/*     */ 
/*     */         
/* 864 */         int i = this.random.nextInt(100);
/* 865 */         if (i < 50) {
/*     */ 
/*     */           
/* 868 */           int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/* 869 */           boolean canWalkToMelee = (!ctx.weFrozen && distance < 6);
/* 870 */           if (canWalkToMelee)
/*     */           {
/*     */             
/* 873 */             int specEnergy = CombatAPI.getSpecEnergy();
/* 874 */             boolean shouldSpec = false;
/* 875 */             if (specEnergy >= 50) {
/*     */               
/* 877 */               int specRoll = this.random.nextInt(100);
/* 878 */               if (specRoll < 10)
/*     */               {
/* 880 */                 shouldSpec = true;
/*     */               }
/*     */             } 
/*     */             
/* 884 */             if (shouldSpec)
/*     */             {
/*     */               
/* 887 */               if (AttackMethods.executeSpecialAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]")) {
/*     */                 
/* 889 */                 if (ctx.config.combatAutomationLogging())
/*     */                 {
/* 891 */                   Logger.norm("[Target Frozen OFF_PID] DD Attack melee rolled special attack (10% chance, spec energy: " + specEnergy + "%)");
/*     */                 }
/* 893 */                 return true;
/*     */               } 
/*     */             }
/*     */ 
/*     */ 
/*     */             
/* 899 */             ctx.helpers.equipGearFromConfig(ctx.config.meleeGear());
/* 900 */             ctx.helpers.attackPlayerWithFightOption(ctx.opponent);
/*     */           
/*     */           }
/*     */           else
/*     */           {
/* 905 */             AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */           }
/*     */         
/*     */         }
/*     */         else {
/*     */           
/* 911 */           AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */         } 
/* 913 */         return true;
/*     */       } 
/*     */ 
/*     */       
/* 917 */       int roll = this.random.nextInt(100);
/*     */       
/* 919 */       boolean canMelee = AttackMethods.canMeleeAttack(ctx, ctx.opponent);
/*     */       
/* 921 */       if (canMelee && roll < 40) {
/*     */ 
/*     */         
/* 924 */         AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */       }
/* 926 */       else if (roll < 65) {
/*     */ 
/*     */         
/* 929 */         AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]");
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 934 */         AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Target Frozen OFF_PID]", true, false, true, false);
/*     */       } 
/* 936 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 940 */     ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 941 */     return true;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/TargetFrozenWeUnfrozenOffPidHandler.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */