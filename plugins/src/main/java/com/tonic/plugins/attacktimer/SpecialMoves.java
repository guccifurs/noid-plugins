/*      */ package com.tonic.plugins.attacktimer;
/*      */ 
/*      */ import com.tonic.Logger;
/*      */ import com.tonic.Static;
/*      */ import com.tonic.api.game.MovementAPI;
/*      */ import com.tonic.api.widgets.PrayerAPI;
/*      */ import com.tonic.services.GameManager;
/*      */ import java.util.Objects;
/*      */ import java.util.Random;
/*      */ import net.runelite.api.Player;
/*      */ import net.runelite.api.coords.WorldPoint;
/*      */ 
/*      */ 
/*      */ 
/*      */ public class SpecialMoves
/*      */ {
/*   17 */   private static final Random random = new Random();
/*      */ 
/*      */   
/*   20 */   private static Player lastAgsMoveTarget = null;
/*      */   private static boolean agsMoveTriggeredThisState = false;
/*   22 */   private static Player currentStateTarget = null;
/*      */   
/*      */   private static boolean agsMovePending = false;
/*      */   
/*      */   private static boolean agsMoveWaitingForAnimation = false;
/*      */   private static boolean walkAwayMovePending = false;
/*      */   private static boolean walkAwayMoveExecuted = false;
/*   29 */   private static Player walkAwayMoveTarget = null;
/*   30 */   private static int walkAwayDistance = 0;
/*      */   
/*      */   private static boolean freeHitActive = false;
/*      */   
/*   34 */   private static Player freeHitTarget = null;
/*      */   private static boolean freeHitAttackExecuted = false;
/*   36 */   private static int freeHitCooldownTicks = 0;
/*      */   
/*      */   private static boolean racksonActive = false;
/*      */   
/*      */   private static boolean racksonTriggeredThisState = false;
/*   41 */   private static Player racksonTarget = null;
/*   42 */   private static Player racksonStateTarget = null;
/*      */   private static boolean racksonWasInStateLastTick = false;
/*   44 */   private static int racksonSequence = 0;
/*   45 */   private static int racksonPhase = 0;
/*      */   private static boolean racksonWaitingForAnimation = false;
/*   47 */   private static int racksonWaitingForAnimationTicks = 0;
/*      */   
/*      */   private static boolean racksonJustAttacked = false;
/*      */   
/*   51 */   private static SpecialMovesOverlay overlay = null;
/*   52 */   private static SpecialMovesOverlay.SpecialMovesStatus currentStatus = SpecialMovesOverlay.SpecialMovesStatus.IDLE;
/*   53 */   private static long statusActivatedTime = 0L;
/*      */ 
/*      */   
/*      */   private static final long STATUS_DISPLAY_DURATION = 3000L;
/*      */ 
/*      */ 
/*      */   
/*      */   public static void setOverlay(SpecialMovesOverlay overlayInstance) {
/*   61 */     overlay = overlayInstance;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static void updateStatus(SpecialMovesOverlay.SpecialMovesStatus newStatus) {
/*   69 */     currentStatus = newStatus;
/*   70 */     if (overlay != null)
/*      */     {
/*   72 */       overlay.updateStatus(newStatus);
/*      */     }
/*      */     
/*   75 */     if (newStatus == SpecialMovesOverlay.SpecialMovesStatus.ACTIVATED)
/*      */     {
/*   77 */       statusActivatedTime = System.currentTimeMillis();
/*      */     }
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static void updateStatusTick() {
/*   87 */     if (currentStatus == SpecialMovesOverlay.SpecialMovesStatus.ACTIVATED) {
/*      */       
/*   89 */       long currentTime = System.currentTimeMillis();
/*   90 */       if (currentTime - statusActivatedTime > 3000L)
/*      */       {
/*   92 */         updateStatus(SpecialMovesOverlay.SpecialMovesStatus.IDLE);
/*      */       }
/*      */     } 
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static boolean isFreeHitActive() {
/*  103 */     return freeHitActive;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean isEatingNeeded(CombatStateContext ctx, Player opponent) {
/*  116 */     if (freeHitActive && opponent == freeHitTarget)
/*      */     {
/*  118 */       return false;
/*      */     }
/*      */ 
/*      */ 
/*      */     
/*  123 */     int health = ctx.helpers.getPlayerHealth();
/*  124 */     int magicLevel = ctx.helpers.getPlayerMagicLevel();
/*  125 */     int prayerPoints = ctx.helpers.getPlayerPrayerPoints();
/*  126 */     boolean hasBrews = ctx.helpers.hasBrews();
/*      */ 
/*      */     
/*  129 */     if (ctx.targetFrozen && !ctx.weFrozen)
/*      */     {
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  135 */       if (ctx.helpers.isUnderTarget(opponent)) {
/*      */ 
/*      */         
/*  138 */         if (prayerPoints < 25)
/*      */         {
/*  140 */           return true;
/*      */         }
/*      */ 
/*      */         
/*  144 */         int targetHP = ctx.helpers.getTargetHPPercentage(opponent);
/*      */ 
/*      */         
/*  147 */         if (targetHP >= 0 && targetHP < 73 && hasBrews) {
/*      */ 
/*      */           
/*  150 */           if (health < 80)
/*      */           {
/*  152 */             return true;
/*      */           }
/*      */           
/*  155 */           if (magicLevel < 82)
/*      */           {
/*  157 */             return true;
/*      */           
/*      */           }
/*      */         
/*      */         }
/*      */         else {
/*      */           
/*  164 */           if (!hasBrews && health < 55)
/*      */           {
/*  166 */             return true;
/*      */           }
/*      */ 
/*      */           
/*  170 */           if (health < 35)
/*      */           {
/*  172 */             return true;
/*      */           }
/*      */ 
/*      */           
/*  176 */           if (health < 90)
/*      */           {
/*  178 */             return true;
/*      */           }
/*      */ 
/*      */           
/*  182 */           if (health > 90 && magicLevel < 94)
/*      */           {
/*  184 */             return true;
/*      */           }
/*      */         } 
/*      */       } 
/*      */     }
/*      */ 
/*      */     
/*  191 */     if (ctx.weFrozen && !ctx.targetFrozen) {
/*      */ 
/*      */ 
/*      */       
/*  195 */       if (health < 72)
/*      */       {
/*  197 */         return true;
/*      */       }
/*      */ 
/*      */       
/*  201 */       if (health >= 72 && magicLevel < 94)
/*      */       {
/*  203 */         return true;
/*      */       }
/*      */ 
/*      */       
/*  207 */       if (magicLevel < 82 && health < 60)
/*      */       {
/*  209 */         return true;
/*      */       }
/*      */     } 
/*      */     
/*  213 */     if (ctx.weFrozen && ctx.targetFrozen) {
/*      */ 
/*      */ 
/*      */       
/*  217 */       if (health < 60)
/*      */       {
/*  219 */         return true;
/*      */       }
/*      */ 
/*      */       
/*  223 */       if (health >= 60 && magicLevel < 94)
/*      */       {
/*  225 */         return true;
/*      */       }
/*      */     } 
/*      */     
/*  229 */     return false;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static boolean checkAndExecuteSpecialMoves(CombatStateContext ctx, Player opponent) {
/*  241 */     int attackTimerTicks = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*      */ 
/*      */ 
/*      */     
/*  245 */     boolean currentTargetFrozenWeUnfrozen = (ctx.targetFrozen && !ctx.weFrozen);
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  250 */     boolean justEnteredState = (currentTargetFrozenWeUnfrozen && !racksonWasInStateLastTick);
/*      */     
/*  252 */     if (currentTargetFrozenWeUnfrozen) {
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  257 */       if (justEnteredState || racksonStateTarget == null) {
/*      */ 
/*      */         
/*  260 */         racksonActive = false;
/*  261 */         racksonTriggeredThisState = false;
/*  262 */         racksonTarget = null;
/*  263 */         racksonSequence = 0;
/*  264 */         racksonPhase = 0;
/*  265 */         racksonWaitingForAnimation = false;
/*  266 */         racksonWaitingForAnimationTicks = 0;
/*  267 */         racksonJustAttacked = false;
/*  268 */         racksonStateTarget = opponent;
/*      */         
/*  270 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/*  272 */           Logger.norm("[Rackson] State entry detected - Reset tracking. Just entered state: " + justEnteredState + " (target cannot change when varbit == 1)");
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/*  277 */       racksonWasInStateLastTick = true;
/*      */     
/*      */     }
/*      */     else {
/*      */       
/*  282 */       if (racksonActive || racksonTriggeredThisState) {
/*      */         
/*  284 */         racksonActive = false;
/*  285 */         racksonTriggeredThisState = false;
/*  286 */         racksonTarget = null;
/*  287 */         racksonSequence = 0;
/*  288 */         racksonPhase = 0;
/*  289 */         racksonWaitingForAnimation = false;
/*  290 */         racksonWaitingForAnimationTicks = 0;
/*  291 */         racksonJustAttacked = false;
/*      */         
/*  293 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/*  295 */           Logger.norm("[Rackson] Exited state - Reset tracking");
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/*  300 */       racksonWasInStateLastTick = false;
/*      */     } 
/*      */ 
/*      */ 
/*      */     
/*  305 */     if (ctx.config.freeHit()) {
/*      */ 
/*      */ 
/*      */       
/*  309 */       boolean freeHitExecuted = checkAndExecuteFreeHit(ctx, opponent);
/*      */ 
/*      */       
/*  312 */       if (freeHitActive && opponent == freeHitTarget && racksonActive && opponent == racksonTarget) {
/*      */ 
/*      */         
/*  315 */         racksonActive = false;
/*  316 */         racksonPhase = 0;
/*  317 */         racksonWaitingForAnimation = false;
/*  318 */         racksonWaitingForAnimationTicks = 0;
/*  319 */         racksonJustAttacked = false;
/*      */         
/*  321 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/*  323 */           Logger.norm("[Rackson] Free hit triggered - Cancelling Rackson to allow free hit");
/*      */         }
/*      */       } 
/*      */       
/*  327 */       if (freeHitExecuted)
/*      */       {
/*  329 */         return true;
/*      */       }
/*      */     } 
/*      */ 
/*      */     
/*  334 */     if (racksonActive && opponent == racksonTarget) {
/*      */ 
/*      */       
/*  337 */       int health = ctx.helpers.getPlayerHealth();
/*  338 */       boolean weFrozen = ctx.weFrozen;
/*  339 */       boolean stateChanged = !currentTargetFrozenWeUnfrozen;
/*      */       
/*  341 */       if (health <= 55 || weFrozen || stateChanged) {
/*      */ 
/*      */         
/*  344 */         if (health <= 55 || weFrozen) {
/*      */           
/*  346 */           ctx.helpers.clickUnderTarget(opponent);
/*  347 */           if (ctx.config.combatAutomationLogging())
/*      */           {
/*  349 */             Logger.norm("[Rackson] Exit condition met - HP: " + health + ", Frozen: " + weFrozen + ", State changed: " + stateChanged + " - Clicking under target");
/*      */           }
/*      */         } 
/*      */ 
/*      */         
/*  354 */         racksonActive = false;
/*  355 */         racksonPhase = 0;
/*  356 */         racksonWaitingForAnimation = false;
/*  357 */         racksonWaitingForAnimationTicks = 0;
/*  358 */         racksonJustAttacked = false;
/*  359 */         return true;
/*      */       } 
/*      */ 
/*      */       
/*  363 */       boolean racksonExecuted = executeRackson(ctx, opponent, attackTimerTicks);
/*  364 */       if (racksonExecuted)
/*      */       {
/*  366 */         return true;
/*      */       }
/*      */ 
/*      */ 
/*      */       
/*  371 */       if (racksonWaitingForAnimation)
/*      */       {
/*  373 */         return true;
/*      */       }
/*      */ 
/*      */ 
/*      */       
/*  378 */       return false;
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  384 */     if (currentTargetFrozenWeUnfrozen && !racksonTriggeredThisState && ctx.config
/*      */       
/*  386 */       .rackson() > 0 && justEnteredState) {
/*      */ 
/*      */ 
/*      */       
/*  390 */       int health = ctx.helpers.getPlayerHealth();
/*  391 */       if (health > 78) {
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*  397 */         boolean triggered = executeRacksonPrepareImmediate(ctx, opponent, attackTimerTicks);
/*  398 */         if (triggered && ctx.config.combatAutomationLogging())
/*      */         {
/*  400 */           Logger.norm("[Rackson] Triggered immediately on state entry - Timer: " + attackTimerTicks + ", HP: " + health);
/*      */         }
/*  402 */         else if (!triggered && ctx.config.combatAutomationLogging())
/*      */         {
/*  404 */           Logger.norm("[Rackson] Failed to trigger - Chance roll or other condition failed");
/*      */         }
/*      */       
/*  407 */       } else if (ctx.config.combatAutomationLogging()) {
/*      */         
/*  409 */         Logger.norm("[Rackson] HP too low to trigger - HP: " + health + " (required > 78)");
/*      */       } 
/*      */     } 
/*      */ 
/*      */     
/*  414 */     if ((freeHitActive || freeHitCooldownTicks > 0) && opponent == freeHitTarget)
/*      */     {
/*      */       
/*  417 */       return true;
/*      */     }
/*      */ 
/*      */     
/*  421 */     if (isEatingNeeded(ctx, opponent))
/*      */     {
/*      */ 
/*      */       
/*  425 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  429 */     if (opponent != currentStateTarget || !currentTargetFrozenWeUnfrozen) {
/*      */       
/*  431 */       agsMoveTriggeredThisState = false;
/*  432 */       agsMovePending = false;
/*  433 */       agsMoveWaitingForAnimation = false;
/*  434 */       currentStateTarget = opponent;
/*      */     } 
/*      */ 
/*      */     
/*  438 */     if (opponent != walkAwayMoveTarget || !currentTargetFrozenWeUnfrozen) {
/*      */       
/*  440 */       walkAwayMovePending = false;
/*  441 */       walkAwayMoveExecuted = false;
/*  442 */       walkAwayMoveTarget = null;
/*  443 */       walkAwayDistance = 0;
/*      */     } 
/*      */ 
/*      */ 
/*      */     
/*  448 */     if (walkAwayMoveExecuted && opponent == walkAwayMoveTarget && (!racksonActive || opponent != racksonTarget)) {
/*      */ 
/*      */       
/*  451 */       ctx.helpers.clickUnderTarget(opponent);
/*      */ 
/*      */       
/*  454 */       walkAwayMoveExecuted = false;
/*  455 */       walkAwayMovePending = false;
/*  456 */       walkAwayMoveTarget = null;
/*  457 */       walkAwayDistance = 0;
/*      */       
/*  459 */       if (ctx.config.combatAutomationLogging()) {
/*      */         
/*  461 */         int currentAttackTimer = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*  462 */         Logger.norm("[Special Moves] Walk-Away Move - Moved back under target, clearing flags. Attack timer: " + currentAttackTimer + " - allowing normal combat to continue");
/*      */       } 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  469 */       return false;
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  475 */     if (walkAwayMovePending && opponent == walkAwayMoveTarget) {
/*      */ 
/*      */       
/*  478 */       walkAwayMovePending = false;
/*  479 */       walkAwayMoveExecuted = false;
/*  480 */       walkAwayMoveTarget = null;
/*  481 */       walkAwayDistance = 0;
/*      */       
/*  483 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/*  485 */         Logger.norm("[Special Moves] Walk-Away Move CANCELLED - Walk-away is disabled as it breaks freeze state and combat flow");
/*      */       }
/*  487 */       return false;
/*      */     } 
/*      */ 
/*      */ 
/*      */     
/*  492 */     if (agsMovePending && attackTimerTicks == 1 && (!racksonActive || opponent != racksonTarget))
/*      */     {
/*  494 */       return executeSpecialMove1AgsAttack(ctx, opponent);
/*      */     }
/*      */ 
/*      */     
/*  498 */     if (attackTimerTicks == 3) {
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  503 */       if (racksonActive && opponent == racksonTarget)
/*      */       {
/*  505 */         return false;
/*      */       }
/*      */ 
/*      */       
/*  509 */       if (executeSpecialMove1AgsPrepare(ctx, opponent))
/*      */       {
/*  511 */         return true;
/*      */       }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  522 */       if (executeSpecialMove3(ctx, opponent))
/*      */       {
/*  524 */         return true;
/*      */       }
/*      */     } 
/*      */     
/*  528 */     return false;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeSpecialMove1AgsPrepare(CombatStateContext ctx, Player opponent) {
/*  540 */     int agsChance = ctx.config.specialMove1Ags();
/*  541 */     if (agsChance <= 0 || agsChance > 100)
/*      */     {
/*  543 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  547 */     if (!ctx.targetFrozen || ctx.weFrozen)
/*      */     {
/*  549 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  553 */     if (agsMoveTriggeredThisState)
/*      */     {
/*  555 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  559 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*  560 */     int attackTimerTicks = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*  561 */     if (opponentTimerTicks <= attackTimerTicks)
/*      */     {
/*  563 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  567 */     int roll = random.nextInt(100);
/*  568 */     if (roll >= agsChance)
/*      */     {
/*  570 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  574 */     agsMovePending = true;
/*  575 */     lastAgsMoveTarget = opponent;
/*      */     
/*  577 */     if (ctx.config.combatAutomationLogging())
/*      */     {
/*  579 */       Logger.norm("[Special Moves] AGS Move prepared (timer 3) - Will execute at timer 1. Opponent timer: " + opponentTimerTicks + ", Player timer: " + attackTimerTicks + ", Chance: " + agsChance + "%, Roll: " + roll);
/*      */     }
/*      */ 
/*      */ 
/*      */     
/*  584 */     return true;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeSpecialMove1AgsAttack(CombatStateContext ctx, Player opponent) {
/*  594 */     if (!agsMovePending || opponent != lastAgsMoveTarget)
/*      */     {
/*  596 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  600 */     if (!ctx.targetFrozen || ctx.weFrozen) {
/*      */ 
/*      */       
/*  603 */       agsMovePending = false;
/*  604 */       return false;
/*      */     } 
/*      */ 
/*      */     
/*  608 */     if (!PrayerAPI.PIETY.isActive()) {
/*      */       
/*  610 */       PrayerAPI.PIETY.turnOn();
/*  611 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/*  613 */         Logger.norm("[Special Moves] AGS Move - Activated Piety prayer");
/*      */       }
/*      */     } 
/*      */ 
/*      */     
/*  618 */     boolean agsEquipped = ctx.helpers.equipItemByName("Armadyl godsword");
/*      */     
/*  620 */     if (agsEquipped) {
/*      */ 
/*      */       
/*  623 */       updateStatus(SpecialMovesOverlay.SpecialMovesStatus.ACTIVATED);
/*      */ 
/*      */       
/*  626 */       ctx.helpers.attackPlayerWithFightOption(opponent);
/*      */ 
/*      */       
/*  629 */       agsMoveWaitingForAnimation = true;
/*  630 */       agsMovePending = false;
/*      */       
/*  632 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/*  634 */         Logger.norm("[Special Moves] AGS Move executed (timer 1) - Activated Piety, equipped AGS, attacked target (regular attack, no spec), waiting for animation");
/*      */       }
/*  636 */       return true;
/*      */     } 
/*  638 */     if (ctx.config.combatAutomationLogging()) {
/*      */       
/*  640 */       Logger.norm("[Special Moves] AGS Move failed (timer 1) - Armadyl Godsword not found in inventory");
/*  641 */       agsMovePending = false;
/*      */     } 
/*      */     
/*  644 */     return false;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static void onAgsAnimationDetected(CombatStateContext ctx, Player opponent) {
/*  654 */     if (!agsMoveWaitingForAnimation || opponent != lastAgsMoveTarget) {
/*      */       return;
/*      */     }
/*      */ 
/*      */ 
/*      */     
/*  660 */     if (!ctx.targetFrozen || ctx.weFrozen) {
/*      */ 
/*      */       
/*  663 */       agsMoveWaitingForAnimation = false;
/*      */       
/*      */       return;
/*      */     } 
/*      */     
/*  668 */     ctx.helpers.clickUnderTarget(opponent);
/*      */ 
/*      */     
/*  671 */     ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/*      */ 
/*      */     
/*  674 */     agsMoveTriggeredThisState = true;
/*  675 */     agsMoveWaitingForAnimation = false;
/*      */     
/*  677 */     if (ctx.config.combatAutomationLogging())
/*      */     {
/*  679 */       Logger.norm("[Special Moves] AGS animation detected - Clicked under target, equipped tank gear");
/*      */     }
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeSpecialMove2(CombatStateContext ctx, Player opponent) {
/*  694 */     int move2Chance = ctx.config.specialMove2();
/*  695 */     if (move2Chance <= 0 || move2Chance > 100)
/*      */     {
/*  697 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  701 */     if (!ctx.targetFrozen || ctx.weFrozen)
/*      */     {
/*  703 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  707 */     if (!ctx.helpers.isUnderTarget(opponent))
/*      */     {
/*  709 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  713 */     if (walkAwayMovePending || walkAwayMoveExecuted)
/*      */     {
/*  715 */       return false;
/*      */     }
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  721 */     int attackTimerTicks = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*  722 */     if (attackTimerTicks != 2 && attackTimerTicks != 3)
/*      */     {
/*      */       
/*  725 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  729 */     int roll = random.nextInt(100);
/*  730 */     if (roll >= move2Chance)
/*      */     {
/*  732 */       return false;
/*      */     }
/*      */ 
/*      */     
/*  736 */     walkAwayDistance = random.nextInt(3) + 2;
/*      */ 
/*      */     
/*  739 */     walkAwayMovePending = true;
/*  740 */     walkAwayMoveTarget = opponent;
/*      */     
/*  742 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*      */     
/*  744 */     if (ctx.config.combatAutomationLogging())
/*      */     {
/*  746 */       Logger.norm("[Special Moves] Walk-Away Move prepared (timer " + attackTimerTicks + ") - Will execute when target timer hits 1 or 0. Opponent timer: " + opponentTimerTicks + ", Player timer: " + attackTimerTicks + ", Distance: " + walkAwayDistance + " tiles, Chance: " + move2Chance + "%, Roll: " + roll);
/*      */     }
/*      */ 
/*      */ 
/*      */     
/*  751 */     return true;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeSpecialMove3(CombatStateContext ctx, Player opponent) {
/*  760 */     int move3Chance = ctx.config.specialMove3();
/*  761 */     if (move3Chance <= 0 || move3Chance > 100)
/*      */     {
/*  763 */       return false;
/*      */     }
/*      */ 
/*      */ 
/*      */     
/*  768 */     int roll = random.nextInt(100);
/*  769 */     if (roll >= move3Chance)
/*      */     {
/*  771 */       return false;
/*      */     }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  778 */     if (ctx.config.combatAutomationLogging())
/*      */     {
/*  780 */       Logger.norm("[Special Moves] Special Move 3 triggered (" + move3Chance + "% chance, roll: " + roll + ") - Not yet implemented");
/*      */     }
/*      */ 
/*      */     
/*  784 */     return false;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static void resetAgsMoveTracking() {
/*  792 */     agsMoveTriggeredThisState = false;
/*  793 */     agsMovePending = false;
/*  794 */     agsMoveWaitingForAnimation = false;
/*  795 */     lastAgsMoveTarget = null;
/*  796 */     currentStateTarget = null;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static void resetWalkAwayMoveTracking() {
/*  804 */     walkAwayMovePending = false;
/*  805 */     walkAwayMoveExecuted = false;
/*  806 */     walkAwayMoveTarget = null;
/*  807 */     walkAwayDistance = 0;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static void executeWalkAwayMove(CombatStateContext ctx, Player target, int distance) {
/*  816 */     Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/*  817 */     Objects.requireNonNull(target); WorldPoint targetPos = (WorldPoint)Static.invoke(target::getWorldLocation);
/*      */ 
/*      */     
/*  820 */     int dx = playerPos.getX() - targetPos.getX();
/*  821 */     int dy = playerPos.getY() - targetPos.getY();
/*      */ 
/*      */     
/*  824 */     if (dx == 0 && dy == 0) {
/*      */ 
/*      */       
/*  827 */       int direction = random.nextInt(4);
/*  828 */       int i = 0;
/*  829 */       int j = 0;
/*  830 */       switch (direction) {
/*      */         case 0:
/*  832 */           j = 1; break;
/*  833 */         case 1: j = -1; break;
/*  834 */         case 2: i = 1; break;
/*  835 */         case 3: i = -1; break;
/*      */       } 
/*  837 */       int k = playerPos.getX() + i * distance;
/*  838 */       int m = playerPos.getY() + j * distance;
/*      */       
/*  840 */       WorldPoint worldPoint = new WorldPoint(k, m, playerPos.getPlane());
/*      */       
/*  842 */       MovementAPI.walkToWorldPoint(worldPoint);
/*      */       
/*      */       return;
/*      */     } 
/*      */     
/*  847 */     int primaryDirX = (dx > 0) ? 1 : ((dx < 0) ? -1 : 0);
/*  848 */     int primaryDirY = (dy > 0) ? 1 : ((dy < 0) ? -1 : 0);
/*      */ 
/*      */     
/*  851 */     int dirX = primaryDirX;
/*  852 */     int dirY = primaryDirY;
/*      */     
/*  854 */     if (random.nextInt(100) < 30)
/*      */     {
/*      */       
/*  857 */       if (primaryDirX != 0 && primaryDirY != 0) {
/*      */ 
/*      */         
/*  860 */         if (random.nextBoolean())
/*      */         {
/*  862 */           dirX = primaryDirX;
/*  863 */           dirY = 0;
/*      */         }
/*      */         else
/*      */         {
/*  867 */           dirX = 0;
/*  868 */           dirY = primaryDirY;
/*      */         }
/*      */       
/*  871 */       } else if (primaryDirX != 0) {
/*      */ 
/*      */         
/*  874 */         dirX = 0;
/*  875 */         dirY = random.nextBoolean() ? 1 : -1;
/*      */       }
/*  877 */       else if (primaryDirY != 0) {
/*      */ 
/*      */         
/*  880 */         dirX = random.nextBoolean() ? 1 : -1;
/*  881 */         dirY = 0;
/*      */       } 
/*      */     }
/*      */ 
/*      */     
/*  886 */     int targetX = playerPos.getX() + dirX * distance;
/*  887 */     int targetY = playerPos.getY() + dirY * distance;
/*      */ 
/*      */     
/*  890 */     WorldPoint walkTile = new WorldPoint(targetX, targetY, playerPos.getPlane());
/*      */     
/*  892 */     MovementAPI.walkToWorldPoint(walkTile);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static boolean isWalkAwayMoveActive() {
/*  900 */     return (walkAwayMovePending || walkAwayMoveExecuted);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static boolean isWaitingForAgsAnimation() {
/*  908 */     return agsMoveWaitingForAnimation;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static Player getAgsAnimationTarget() {
/*  916 */     return lastAgsMoveTarget;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean checkAndExecuteFreeHit(CombatStateContext ctx, Player opponent) {
/*  930 */     if (!ctx.targetFrozen || ctx.weFrozen) {
/*      */ 
/*      */       
/*  933 */       if (freeHitActive && opponent == freeHitTarget) {
/*      */         
/*  935 */         freeHitActive = false;
/*  936 */         freeHitAttackExecuted = false;
/*      */         
/*  938 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/*  940 */           Logger.norm("[Free Hit] State changed - target frozen: " + ctx.targetFrozen + ", we frozen: " + ctx.weFrozen + " - deactivating free hit (cooldown continues if active)");
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/*  945 */       if (freeHitCooldownTicks > 0 && opponent == freeHitTarget) {
/*      */         
/*  947 */         ctx.helpers.clickUnderTarget(opponent);
/*  948 */         freeHitCooldownTicks--;
/*  949 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/*  951 */           Logger.norm("[Free Hit] Cooldown active during state change - staying under target (" + freeHitCooldownTicks + " ticks remaining)");
/*      */         }
/*  953 */         return true;
/*      */       } 
/*  955 */       return false;
/*      */     } 
/*      */     
/*  958 */     int playerTimerTicks = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*  959 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*      */ 
/*      */     
/*  962 */     if (opponent != freeHitTarget) {
/*      */       
/*  964 */       freeHitActive = false;
/*  965 */       freeHitAttackExecuted = false;
/*  966 */       freeHitCooldownTicks = 0;
/*  967 */       freeHitTarget = opponent;
/*      */     } 
/*      */ 
/*      */     
/*  971 */     boolean freeHitDetected = false;
/*      */ 
/*      */     
/*  974 */     CombatStateContext.PidStatus pidStatus = ctx.currentPidStatus;
/*      */     
/*  976 */     if (pidStatus == CombatStateContext.PidStatus.OFF_PID || pidStatus == CombatStateContext.PidStatus.UNKNOWN) {
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  981 */       int timerDiff = opponentTimerTicks - playerTimerTicks;
/*  982 */       if (timerDiff >= 2)
/*      */       {
/*  984 */         freeHitDetected = true;
/*      */       }
/*      */     }
/*  987 */     else if (pidStatus == CombatStateContext.PidStatus.ON_PID) {
/*      */ 
/*      */ 
/*      */       
/*  991 */       int timerDiff = opponentTimerTicks - playerTimerTicks;
/*  992 */       if (timerDiff >= 1)
/*      */       {
/*  994 */         freeHitDetected = true;
/*      */       }
/*      */     } 
/*      */ 
/*      */     
/*  999 */     if (freeHitDetected && !freeHitActive) {
/*      */       
/* 1001 */       freeHitActive = true;
/* 1002 */       freeHitTarget = opponent;
/* 1003 */       freeHitAttackExecuted = false;
/*      */ 
/*      */       
/* 1006 */       updateStatus(SpecialMovesOverlay.SpecialMovesStatus.ACTIVATED);
/*      */       
/* 1008 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/* 1010 */         Logger.norm("[Free Hit] Free hit detected - PID: " + String.valueOf(pidStatus) + ", Player timer: " + playerTimerTicks + ", Opponent timer: " + opponentTimerTicks + ", Difference: " + (opponentTimerTicks - playerTimerTicks));
/*      */       }
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1018 */     if (freeHitActive && !freeHitDetected) {
/*      */       
/* 1020 */       freeHitActive = false;
/* 1021 */       freeHitAttackExecuted = false;
/*      */ 
/*      */ 
/*      */       
/* 1025 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/* 1027 */         Logger.norm("[Free Hit] Free hit opportunity lost - PID: " + String.valueOf(pidStatus) + ", Player timer: " + playerTimerTicks + ", Opponent timer: " + opponentTimerTicks);
/*      */       }
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1034 */     if (freeHitCooldownTicks > 0 && opponent == freeHitTarget) {
/*      */ 
/*      */       
/* 1037 */       ctx.helpers.clickUnderTarget(opponent);
/* 1038 */       freeHitCooldownTicks--;
/*      */       
/* 1040 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/* 1042 */         Logger.norm("[Free Hit] Cooldown active - staying under target (" + freeHitCooldownTicks + " ticks remaining)");
/*      */       }
/*      */ 
/*      */       
/* 1046 */       return true;
/*      */     } 
/*      */ 
/*      */     
/* 1050 */     if (freeHitActive && opponent == freeHitTarget) {
/*      */ 
/*      */       
/* 1053 */       if (freeHitAttackExecuted) {
/*      */         
/* 1055 */         ctx.helpers.clickUnderTarget(opponent);
/* 1056 */         freeHitAttackExecuted = false;
/*      */ 
/*      */         
/* 1059 */         freeHitCooldownTicks = 3;
/* 1060 */         freeHitActive = false;
/*      */         
/* 1062 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1064 */           Logger.norm("[Free Hit] Attack executed - clicking under target, starting 3-tick cooldown");
/*      */         }
/* 1066 */         return true;
/*      */       } 
/*      */ 
/*      */       
/* 1070 */       if (playerTimerTicks > 1) {
/*      */         
/* 1072 */         ctx.helpers.clickUnderTarget(opponent);
/* 1073 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1075 */           Logger.norm("[Free Hit] Timer > 1 (" + playerTimerTicks + ") - clicking under target");
/*      */         }
/* 1077 */         return true;
/*      */       } 
/*      */ 
/*      */       
/* 1081 */       if (playerTimerTicks == 1) {
/*      */ 
/*      */         
/* 1084 */         int styleRoll = random.nextInt(4);
/* 1085 */         boolean attackExecuted = false;
/*      */         
/* 1087 */         if (styleRoll == 0) {
/*      */ 
/*      */           
/* 1090 */           attackExecuted = AttackMethods.executeRangeAttack(ctx, opponent, "[Free Hit]");
/* 1091 */           if (ctx.config.combatAutomationLogging())
/*      */           {
/* 1093 */             Logger.norm("[Free Hit] Timer 1 - Executing RANGED attack");
/*      */           }
/*      */         }
/* 1096 */         else if (styleRoll == 1) {
/*      */ 
/*      */           
/* 1099 */           attackExecuted = AttackMethods.executeMagicAttack(ctx, opponent, "[Free Hit]", true, false, true, false);
/* 1100 */           if (ctx.config.combatAutomationLogging())
/*      */           {
/* 1102 */             Logger.norm("[Free Hit] Timer 1 - Executing MAGIC BLOOD BARRAGE attack");
/*      */           }
/*      */         }
/* 1105 */         else if (styleRoll == 2) {
/*      */ 
/*      */           
/* 1108 */           attackExecuted = AttackMethods.executeMeleeAttack(ctx, opponent, "[Free Hit]");
/* 1109 */           if (ctx.config.combatAutomationLogging())
/*      */           {
/* 1111 */             Logger.norm("[Free Hit] Timer 1 - Executing WHIP (melee) attack");
/*      */           
/*      */           }
/*      */         }
/*      */         else {
/*      */           
/* 1117 */           boolean agsEquipped = ctx.helpers.equipItemByName("Armadyl godsword");
/* 1118 */           if (agsEquipped) {
/*      */             
/* 1120 */             ctx.helpers.attackPlayerWithFightOption(opponent);
/* 1121 */             attackExecuted = true;
/* 1122 */             if (ctx.config.combatAutomationLogging())
/*      */             {
/* 1124 */               Logger.norm("[Free Hit] Timer 1 - Executing AGS attack");
/*      */             
/*      */             }
/*      */           }
/*      */           else {
/*      */             
/* 1130 */             attackExecuted = AttackMethods.executeMeleeAttack(ctx, opponent, "[Free Hit]");
/* 1131 */             if (ctx.config.combatAutomationLogging())
/*      */             {
/* 1133 */               Logger.norm("[Free Hit] Timer 1 - AGS not found, falling back to melee attack");
/*      */             }
/*      */           } 
/*      */         } 
/*      */         
/* 1138 */         if (attackExecuted) {
/*      */           
/* 1140 */           freeHitAttackExecuted = true;
/* 1141 */           return true;
/*      */         } 
/*      */       } 
/*      */ 
/*      */       
/* 1146 */       return true;
/*      */     } 
/*      */     
/* 1149 */     return false;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static void resetFreeHitTracking() {
/* 1157 */     freeHitActive = false;
/* 1158 */     freeHitAttackExecuted = false;
/* 1159 */     freeHitTarget = null;
/* 1160 */     freeHitCooldownTicks = 0;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static boolean isFreeHitCooldownActive() {
/* 1169 */     return (freeHitCooldownTicks > 0);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeRacksonPrepare(CombatStateContext ctx, Player opponent, int attackTimerTicks) {
/* 1185 */     int racksonChance = ctx.config.rackson();
/* 1186 */     if (racksonChance <= 0 || racksonChance > 100)
/*      */     {
/* 1188 */       return false;
/*      */     }
/*      */ 
/*      */     
/* 1192 */     if (!ctx.targetFrozen || ctx.weFrozen || !ctx.helpers.isUnderTarget(opponent))
/*      */     {
/* 1194 */       return false;
/*      */     }
/*      */ 
/*      */     
/* 1198 */     if (racksonTriggeredThisState)
/*      */     {
/* 1200 */       return false;
/*      */     }
/*      */ 
/*      */     
/* 1204 */     int roll = random.nextInt(100);
/* 1205 */     if (roll >= racksonChance)
/*      */     {
/* 1207 */       return false;
/*      */     }
/*      */ 
/*      */     
/* 1211 */     boolean sequence1Selected = random.nextBoolean();
/*      */     
/* 1213 */     if (sequence1Selected && attackTimerTicks == 2) {
/*      */ 
/*      */       
/* 1216 */       racksonSequence = 1;
/* 1217 */       racksonPhase = 1;
/* 1218 */       racksonActive = true;
/* 1219 */       racksonTarget = opponent;
/* 1220 */       racksonTriggeredThisState = true;
/*      */ 
/*      */       
/* 1223 */       updateStatus(SpecialMovesOverlay.SpecialMovesStatus.ACTIVATED);
/*      */       
/* 1225 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/* 1227 */         Logger.norm("[Rackson] Sequence 1 prepared at timer 2 - Chance: " + racksonChance + "%, Roll: " + roll);
/*      */       }
/* 1229 */       return true;
/*      */     } 
/* 1231 */     if (!sequence1Selected && attackTimerTicks == 1) {
/*      */ 
/*      */       
/* 1234 */       racksonSequence = 2;
/* 1235 */       racksonPhase = 1;
/* 1236 */       racksonActive = true;
/* 1237 */       racksonTarget = opponent;
/* 1238 */       racksonTriggeredThisState = true;
/*      */ 
/*      */       
/* 1241 */       updateStatus(SpecialMovesOverlay.SpecialMovesStatus.ACTIVATED);
/*      */       
/* 1243 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/* 1245 */         Logger.norm("[Rackson] Sequence 2 prepared at timer 1 - Chance: " + racksonChance + "%, Roll: " + roll);
/*      */       }
/* 1247 */       return true;
/*      */     } 
/*      */     
/* 1250 */     return false;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeRacksonPrepareImmediate(CombatStateContext ctx, Player opponent, int attackTimerTicks) {
/* 1264 */     int racksonChance = ctx.config.rackson();
/* 1265 */     if (racksonChance <= 0 || racksonChance > 100)
/*      */     {
/* 1267 */       return false;
/*      */     }
/*      */ 
/*      */     
/* 1271 */     if (!ctx.targetFrozen || ctx.weFrozen)
/*      */     {
/* 1273 */       return false;
/*      */     }
/*      */ 
/*      */     
/* 1277 */     if (racksonTriggeredThisState)
/*      */     {
/* 1279 */       return false;
/*      */     }
/*      */ 
/*      */     
/* 1283 */     int health = ctx.helpers.getPlayerHealth();
/* 1284 */     if (health <= 78) {
/*      */       
/* 1286 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/* 1288 */         Logger.norm("[Rackson] HP too low to enter - HP: " + health + " (required > 78)");
/*      */       }
/* 1290 */       return false;
/*      */     } 
/*      */ 
/*      */     
/* 1294 */     int roll = random.nextInt(100);
/* 1295 */     if (roll >= racksonChance)
/*      */     {
/* 1297 */       return false;
/*      */     }
/*      */ 
/*      */     
/* 1301 */     boolean isUnderTarget = ctx.helpers.isUnderTarget(opponent);
/* 1302 */     int distance = AttackMethods.getDistance(ctx, opponent);
/* 1303 */     boolean isTwoTilesAway = (distance == 2);
/*      */ 
/*      */     
/* 1306 */     boolean sequence1Selected = random.nextBoolean();
/*      */     
/* 1308 */     if (sequence1Selected) {
/*      */ 
/*      */ 
/*      */       
/* 1312 */       racksonSequence = 1;
/* 1313 */       racksonActive = true;
/* 1314 */       racksonTarget = opponent;
/* 1315 */       racksonTriggeredThisState = true;
/*      */ 
/*      */ 
/*      */ 
/*      */       
/* 1320 */       if (attackTimerTicks == 2 && isUnderTarget) {
/*      */ 
/*      */         
/* 1323 */         racksonPhase = 1;
/*      */       }
/* 1325 */       else if (attackTimerTicks == 1) {
/*      */         
/* 1327 */         if (isUnderTarget)
/*      */         {
/*      */           
/* 1330 */           racksonPhase = 7;
/*      */         }
/* 1332 */         else if (isTwoTilesAway)
/*      */         {
/*      */           
/* 1335 */           racksonPhase = 6;
/*      */         
/*      */         }
/*      */         else
/*      */         {
/* 1340 */           racksonPhase = 3;
/*      */         }
/*      */       
/* 1343 */       } else if (attackTimerTicks == 3) {
/*      */         
/* 1345 */         if (isUnderTarget)
/*      */         {
/*      */           
/* 1348 */           racksonPhase = 5;
/*      */         
/*      */         }
/*      */         else
/*      */         {
/* 1353 */           racksonPhase = 5;
/*      */         }
/*      */       
/* 1356 */       } else if (attackTimerTicks == 4) {
/*      */         
/* 1358 */         if (isUnderTarget)
/*      */         {
/*      */           
/* 1361 */           racksonPhase = 4;
/*      */         }
/* 1363 */         else if (isTwoTilesAway)
/*      */         {
/*      */           
/* 1366 */           racksonPhase = 3;
/*      */         }
/*      */         else
/*      */         {
/* 1370 */           racksonPhase = 4;
/*      */         }
/*      */       
/* 1373 */       } else if (attackTimerTicks == 5) {
/*      */         
/* 1375 */         if (isUnderTarget)
/*      */         {
/*      */           
/* 1378 */           racksonPhase = 4;
/*      */         
/*      */         }
/*      */         else
/*      */         {
/* 1383 */           racksonPhase = 3;
/*      */         }
/*      */       
/*      */       }
/*      */       else {
/*      */         
/* 1389 */         racksonPhase = 3;
/*      */       } 
/*      */ 
/*      */       
/* 1393 */       updateStatus(SpecialMovesOverlay.SpecialMovesStatus.ACTIVATED);
/*      */       
/* 1395 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/* 1397 */         Logger.norm("[Rackson] Sequence 1 prepared immediately - Timer: " + attackTimerTicks + ", Under: " + isUnderTarget + ", Distance: " + distance + ", Phase: " + racksonPhase + " - Chance: " + racksonChance + "%, Roll: " + roll);
/*      */       }
/*      */ 
/*      */       
/* 1401 */       return true;
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1407 */     racksonSequence = 2;
/* 1408 */     racksonActive = true;
/* 1409 */     racksonTarget = opponent;
/* 1410 */     racksonTriggeredThisState = true;
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1415 */     if (attackTimerTicks == 1 && isUnderTarget) {
/*      */ 
/*      */       
/* 1418 */       racksonPhase = 1;
/*      */     }
/* 1420 */     else if (attackTimerTicks == 1 && !isUnderTarget) {
/*      */ 
/*      */       
/* 1423 */       racksonPhase = 2;
/*      */     }
/* 1425 */     else if (attackTimerTicks == 2) {
/*      */       
/* 1427 */       if (isUnderTarget)
/*      */       {
/*      */         
/* 1430 */         racksonPhase = 6;
/*      */       }
/* 1432 */       else if (isTwoTilesAway)
/*      */       {
/*      */         
/* 1435 */         racksonPhase = 5;
/*      */       }
/*      */       else
/*      */       {
/* 1439 */         racksonPhase = 5;
/*      */       }
/*      */     
/* 1442 */     } else if (attackTimerTicks == 3) {
/*      */       
/* 1444 */       if (isUnderTarget)
/*      */       {
/*      */         
/* 1447 */         racksonPhase = 4;
/*      */       
/*      */       }
/*      */       else
/*      */       {
/* 1452 */         racksonPhase = 4;
/*      */       }
/*      */     
/* 1455 */     } else if (attackTimerTicks == 4) {
/*      */       
/* 1457 */       if (isUnderTarget)
/*      */       {
/*      */         
/* 1460 */         racksonPhase = 3;
/*      */       }
/* 1462 */       else if (isTwoTilesAway)
/*      */       {
/*      */         
/* 1465 */         racksonPhase = 2;
/*      */       }
/*      */       else
/*      */       {
/* 1469 */         racksonPhase = 3;
/*      */       }
/*      */     
/* 1472 */     } else if (attackTimerTicks == 5) {
/*      */       
/* 1474 */       if (isUnderTarget)
/*      */       {
/*      */         
/* 1477 */         racksonPhase = 3;
/*      */       
/*      */       }
/*      */       else
/*      */       {
/* 1482 */         racksonPhase = 2;
/*      */       }
/*      */     
/*      */     }
/*      */     else {
/*      */       
/* 1488 */       racksonPhase = 2;
/*      */     } 
/*      */ 
/*      */     
/* 1492 */     updateStatus(SpecialMovesOverlay.SpecialMovesStatus.ACTIVATED);
/*      */     
/* 1494 */     if (ctx.config.combatAutomationLogging())
/*      */     {
/* 1496 */       Logger.norm("[Rackson] Sequence 2 prepared immediately - Timer: " + attackTimerTicks + ", Under: " + isUnderTarget + ", Distance: " + distance + ", Phase: " + racksonPhase + " - Chance: " + racksonChance + "%, Roll: " + roll);
/*      */     }
/*      */ 
/*      */     
/* 1500 */     return true;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeRackson(CombatStateContext ctx, Player opponent, int attackTimerTicks) {
/* 1514 */     if (!racksonActive || opponent != racksonTarget)
/*      */     {
/* 1516 */       return false;
/*      */     }
/*      */ 
/*      */ 
/*      */     
/* 1521 */     if (attackTimerTicks > 1) {
/*      */       
/* 1523 */       int currentTick = GameManager.getTickCount();
/*      */       
/* 1525 */       if (ctx.lastEquipTick.value != currentTick) {
/*      */         
/* 1527 */         ctx.lastEquipTick.value = currentTick;
/* 1528 */         ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 1529 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1531 */           Logger.norm("[Rackson] Timer > 1 (" + attackTimerTicks + ") - Equipped tank gear");
/*      */         }
/*      */       } 
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1539 */     if (racksonWaitingForAnimation)
/*      */     {
/*      */       
/* 1542 */       return true;
/*      */     }
/*      */ 
/*      */     
/* 1546 */     boolean executed = false;
/* 1547 */     if (racksonSequence == 1) {
/*      */       
/* 1549 */       executed = executeRacksonSequence1(ctx, opponent, attackTimerTicks);
/*      */     }
/* 1551 */     else if (racksonSequence == 2) {
/*      */       
/* 1553 */       executed = executeRacksonSequence2(ctx, opponent, attackTimerTicks);
/*      */     } 
/*      */ 
/*      */     
/* 1557 */     if (!executed && ctx.config.combatAutomationLogging())
/*      */     {
/* 1559 */       Logger.norm("[Rackson] Active but not executing - Sequence: " + racksonSequence + ", Phase: " + racksonPhase + ", Timer: " + attackTimerTicks);
/*      */     }
/*      */     
/* 1562 */     return executed;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeRacksonSequence1(CombatStateContext ctx, Player opponent, int attackTimerTicks) {
/* 1574 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*      */     
/* 1576 */     if (racksonPhase == 1 && attackTimerTicks == 2) {
/*      */ 
/*      */       
/* 1579 */       executeRacksonMoveAway(ctx, opponent, 2);
/* 1580 */       racksonPhase = 2;
/*      */       
/* 1582 */       if (ctx.config.combatAutomationLogging())
/*      */       {
/* 1584 */         Logger.norm("[Rackson Sequence 1] Phase 1 - Moved away 2 tiles at timer 2");
/*      */       }
/* 1586 */       return true;
/*      */     } 
/* 1588 */     if (racksonPhase == 2 && attackTimerTicks == 1) {
/*      */ 
/*      */       
/* 1591 */       boolean attackExecuted = executeRacksonAttack(ctx, opponent, attackTimerTicks, opponentTimerTicks);
/* 1592 */       if (attackExecuted)
/*      */       {
/* 1594 */         racksonWaitingForAnimation = true;
/* 1595 */         racksonPhase = 3;
/*      */         
/* 1597 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1599 */           Logger.norm("[Rackson Sequence 1] Phase 2 - Attacked at timer 1, waiting for animation");
/*      */         }
/* 1601 */         return true;
/*      */       }
/*      */     
/* 1604 */     } else if (racksonPhase >= 3) {
/*      */ 
/*      */ 
/*      */       
/* 1608 */       int loopPhase = (racksonPhase - 3) % 5;
/*      */       
/* 1610 */       if (loopPhase == 0 && attackTimerTicks == 5) {
/*      */ 
/*      */         
/* 1613 */         ctx.helpers.clickUnderTarget(opponent);
/* 1614 */         racksonPhase++;
/*      */         
/* 1616 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1618 */           Logger.norm("[Rackson Sequence 1] Loop phase " + loopPhase + " (Timer 5) - Moved under target");
/*      */         }
/* 1620 */         return true;
/*      */       } 
/* 1622 */       if (loopPhase == 1 && attackTimerTicks == 4) {
/*      */ 
/*      */         
/* 1625 */         executeRacksonMoveAway(ctx, opponent, 2);
/* 1626 */         racksonPhase++;
/*      */         
/* 1628 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1630 */           Logger.norm("[Rackson Sequence 1] Loop phase " + loopPhase + " (Timer 4) - Moved away 2 tiles");
/*      */         }
/* 1632 */         return true;
/*      */       } 
/* 1634 */       if (loopPhase == 2 && attackTimerTicks == 3) {
/*      */ 
/*      */         
/* 1637 */         ctx.helpers.clickUnderTarget(opponent);
/* 1638 */         racksonPhase++;
/*      */         
/* 1640 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1642 */           Logger.norm("[Rackson Sequence 1] Loop phase " + loopPhase + " (Timer 3) - Moved under target");
/*      */         }
/* 1644 */         return true;
/*      */       } 
/* 1646 */       if (loopPhase == 3 && attackTimerTicks == 2) {
/*      */ 
/*      */         
/* 1649 */         executeRacksonMoveAway(ctx, opponent, 2);
/* 1650 */         racksonPhase++;
/*      */         
/* 1652 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1654 */           Logger.norm("[Rackson Sequence 1] Loop phase " + loopPhase + " (Timer 2) - Moved away 2 tiles");
/*      */         }
/* 1656 */         return true;
/*      */       } 
/* 1658 */       if (loopPhase == 4 && attackTimerTicks == 1) {
/*      */ 
/*      */         
/* 1661 */         boolean attackExecuted = executeRacksonAttack(ctx, opponent, attackTimerTicks, opponentTimerTicks);
/* 1662 */         if (attackExecuted) {
/*      */           
/* 1664 */           racksonWaitingForAnimation = true;
/* 1665 */           racksonWaitingForAnimationTicks = 0;
/* 1666 */           racksonPhase++;
/*      */           
/* 1668 */           if (ctx.config.combatAutomationLogging())
/*      */           {
/* 1670 */             Logger.norm("[Rackson Sequence 1] Loop phase " + loopPhase + " (Timer 1) - Attacked, waiting for animation");
/*      */           }
/* 1672 */           return true;
/*      */         } 
/*      */       } 
/*      */     } 
/*      */     
/* 1677 */     return false;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeRacksonSequence2(CombatStateContext ctx, Player opponent, int attackTimerTicks) {
/* 1688 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*      */     
/* 1690 */     if (racksonPhase == 1 && attackTimerTicks == 1) {
/*      */ 
/*      */       
/* 1693 */       boolean attackExecuted = executeRacksonAttack(ctx, opponent, attackTimerTicks, opponentTimerTicks);
/* 1694 */       if (attackExecuted)
/*      */       {
/* 1696 */         racksonWaitingForAnimation = true;
/* 1697 */         racksonPhase = 2;
/*      */         
/* 1699 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1701 */           Logger.norm("[Rackson Sequence 2] Phase 1 - Attacked from under target at timer 1, waiting for animation");
/*      */         }
/* 1703 */         return true;
/*      */       }
/*      */     
/* 1706 */     } else if (racksonPhase >= 2) {
/*      */ 
/*      */ 
/*      */       
/* 1710 */       int loopPhase = (racksonPhase - 2) % 5;
/*      */       
/* 1712 */       if (loopPhase == 0 && attackTimerTicks == 5) {
/*      */ 
/*      */         
/* 1715 */         ctx.helpers.clickUnderTarget(opponent);
/* 1716 */         racksonPhase++;
/*      */         
/* 1718 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1720 */           Logger.norm("[Rackson Sequence 2] Loop phase " + loopPhase + " (Timer 5) - Moved under target");
/*      */         }
/* 1722 */         return true;
/*      */       } 
/* 1724 */       if (loopPhase == 1 && attackTimerTicks == 4) {
/*      */ 
/*      */         
/* 1727 */         executeRacksonMoveAway(ctx, opponent, 2);
/* 1728 */         racksonPhase++;
/*      */         
/* 1730 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1732 */           Logger.norm("[Rackson Sequence 2] Loop phase " + loopPhase + " (Timer 4) - Moved away 2 tiles");
/*      */         }
/* 1734 */         return true;
/*      */       } 
/* 1736 */       if (loopPhase == 2 && attackTimerTicks == 3) {
/*      */ 
/*      */         
/* 1739 */         ctx.helpers.clickUnderTarget(opponent);
/* 1740 */         racksonPhase++;
/*      */         
/* 1742 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1744 */           Logger.norm("[Rackson Sequence 2] Loop phase " + loopPhase + " (Timer 3) - Moved under target");
/*      */         }
/* 1746 */         return true;
/*      */       } 
/* 1748 */       if (loopPhase == 3 && attackTimerTicks == 2) {
/*      */ 
/*      */         
/* 1751 */         executeRacksonMoveAway(ctx, opponent, 2);
/* 1752 */         racksonPhase++;
/*      */         
/* 1754 */         if (ctx.config.combatAutomationLogging())
/*      */         {
/* 1756 */           Logger.norm("[Rackson Sequence 2] Loop phase " + loopPhase + " (Timer 2) - Moved away 2 tiles");
/*      */         }
/* 1758 */         return true;
/*      */       } 
/* 1760 */       if (loopPhase == 4 && attackTimerTicks == 1) {
/*      */ 
/*      */         
/* 1763 */         boolean attackExecuted = executeRacksonAttack(ctx, opponent, attackTimerTicks, opponentTimerTicks);
/* 1764 */         if (attackExecuted) {
/*      */           
/* 1766 */           racksonWaitingForAnimation = true;
/* 1767 */           racksonWaitingForAnimationTicks = 0;
/* 1768 */           racksonPhase++;
/*      */           
/* 1770 */           if (ctx.config.combatAutomationLogging())
/*      */           {
/* 1772 */             Logger.norm("[Rackson Sequence 2] Loop phase " + loopPhase + " (Timer 1) - Attacked, waiting for animation");
/*      */           }
/* 1774 */           return true;
/*      */         } 
/*      */       } 
/*      */     } 
/*      */     
/* 1779 */     return false;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static void executeRacksonMoveAway(CombatStateContext ctx, Player target, int distance) {
/* 1791 */     Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/* 1792 */     Objects.requireNonNull(target); WorldPoint targetPos = (WorldPoint)Static.invoke(target::getWorldLocation);
/*      */ 
/*      */ 
/*      */     
/* 1796 */     int direction = random.nextInt(8);
/* 1797 */     int dirX = 0;
/* 1798 */     int dirY = 0;
/* 1799 */     switch (direction) {
/*      */       case 0:
/* 1801 */         dirY = 1; break;
/* 1802 */       case 1: dirX = 1; dirY = 1; break;
/* 1803 */       case 2: dirX = 1; break;
/* 1804 */       case 3: dirX = 1; dirY = -1; break;
/* 1805 */       case 4: dirY = -1; break;
/* 1806 */       case 5: dirX = -1; dirY = -1; break;
/* 1807 */       case 6: dirX = -1; break;
/* 1808 */       case 7: dirX = -1; dirY = 1;
/*      */         break;
/*      */     } 
/*      */     
/* 1812 */     int targetX = playerPos.getX() + dirX * distance;
/* 1813 */     int targetY = playerPos.getY() + dirY * distance;
/*      */ 
/*      */     
/* 1816 */     WorldPoint walkTile = new WorldPoint(targetX, targetY, playerPos.getPlane());
/*      */     
/* 1818 */     MovementAPI.walkToWorldPoint(walkTile);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private static boolean executeRacksonAttack(CombatStateContext ctx, Player opponent, int playerTimerTicks, int opponentTimerTicks) {
/* 1833 */     boolean useMagic = random.nextBoolean();
/*      */     
/* 1835 */     if (useMagic) {
/*      */ 
/*      */       
/* 1838 */       boolean needTankBarrage = (playerTimerTicks == opponentTimerTicks || opponentTimerTicks == 0);
/*      */       
/* 1840 */       if (needTankBarrage)
/*      */       {
/*      */         
/* 1843 */         return AttackMethods.executeMagicAttack(ctx, opponent, "[Rackson]", true, true, true, false);
/*      */       }
/*      */ 
/*      */ 
/*      */       
/* 1848 */       return AttackMethods.executeMagicAttack(ctx, opponent, "[Rackson]", true, false, true, false);
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1854 */     return AttackMethods.executeRangeAttack(ctx, opponent, "[Rackson]");
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static void resetRacksonTracking() {
/* 1863 */     racksonActive = false;
/* 1864 */     racksonTriggeredThisState = false;
/* 1865 */     racksonTarget = null;
/* 1866 */     racksonStateTarget = null;
/* 1867 */     racksonWasInStateLastTick = false;
/* 1868 */     racksonSequence = 0;
/* 1869 */     racksonPhase = 0;
/* 1870 */     racksonWaitingForAnimation = false;
/* 1871 */     racksonWaitingForAnimationTicks = 0;
/* 1872 */     racksonJustAttacked = false;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static boolean isRacksonActive() {
/* 1881 */     return racksonActive;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static boolean isWaitingForRacksonAnimation() {
/* 1890 */     return racksonWaitingForAnimation;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static int getRacksonWaitingTicks() {
/* 1899 */     return racksonWaitingForAnimationTicks;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static void incrementRacksonWaitingTicks() {
/* 1907 */     if (racksonWaitingForAnimation)
/*      */     {
/* 1909 */       racksonWaitingForAnimationTicks++;
/*      */     }
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static Player getRacksonTarget() {
/* 1919 */     return racksonTarget;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public static void onRacksonAnimationDetected(CombatStateContext ctx, Player opponent) {
/* 1938 */     if (!racksonWaitingForAnimation || opponent != racksonTarget) {
/*      */       return;
/*      */     }
/*      */ 
/*      */ 
/*      */     
/* 1944 */     if (!ctx.targetFrozen || ctx.weFrozen) {
/*      */ 
/*      */       
/* 1947 */       racksonWaitingForAnimation = false;
/*      */       
/*      */       return;
/*      */     } 
/*      */     
/* 1952 */     ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 1953 */     ctx.helpers.clickUnderTarget(opponent);
/* 1954 */     racksonWaitingForAnimation = false;
/* 1955 */     racksonWaitingForAnimationTicks = 0;
/* 1956 */     racksonPhase++;
/* 1957 */     racksonJustAttacked = false;
/*      */     
/* 1959 */     if (ctx.config.combatAutomationLogging())
/*      */     {
/* 1961 */       Logger.norm("[Rackson] Attack detected (animation or XP drop) - Equipped tank gear and moved under target (same tick), Phase: " + racksonPhase);
/*      */     }
/*      */   }
/*      */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/SpecialMoves.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */