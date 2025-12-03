/*      */ package com.tonic.plugins.attacktimer;
/*      */ 
/*      */ import com.tonic.Logger;
/*      */ import com.tonic.Static;
/*      */ import com.tonic.api.game.CombatAPI;
/*      */ import com.tonic.api.game.MovementAPI;
/*      */ import com.tonic.api.widgets.InventoryAPI;
/*      */ import com.tonic.api.widgets.PrayerAPI;
/*      */ import com.tonic.data.wrappers.ItemEx;
/*      */ import com.tonic.services.GameManager;
/*      */ import com.tonic.services.pathfinder.Walker;
/*      */ import com.tonic.services.pathfinder.collision.CollisionMap;
/*      */ import java.util.Objects;
/*      */ import java.util.Random;
/*      */ import net.runelite.api.coords.WorldPoint;
/*      */ 
/*      */ public class BothUnfrozenHandler
/*      */   implements CombatStateHandler {
/*   19 */   private final Random random = new Random();
/*   20 */   private WorldPoint lastClickedTile = null;
/*      */   
/*   22 */   private int ticksSinceLastFailedAttack = 0;
/*      */   
/*      */   private boolean needToBrewAfterLowHPAngler = false;
/*      */   
/*   26 */   private int ticksSinceLowHPAngler = 0;
/*      */   
/*      */   private boolean specModeActive = false;
/*      */   
/*   30 */   private int specModeTicks = 0;
/*      */   
/*      */   private boolean pidWhipModeActive = false;
/*      */   
/*   34 */   private int pidWhipModeTicks = 0;
/*      */ 
/*      */   
/*   37 */   private String lastAttackType = null;
/*      */ 
/*      */   
/*   40 */   private CollisionMap cachedCollisionMap = null;
/*   41 */   private int lastCollisionMapCheckTick = -1;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private CollisionMap getCollisionMap() {
/*   48 */     int currentTick = GameManager.getTickCount();
/*   49 */     if (this.cachedCollisionMap == null || currentTick != this.lastCollisionMapCheckTick) {
/*      */       
/*   51 */       this.cachedCollisionMap = Walker.getCollisionMap();
/*   52 */       this.lastCollisionMapCheckTick = currentTick;
/*      */     } 
/*   54 */     return this.cachedCollisionMap;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private boolean isWalkable(WorldPoint tile) {
/*   62 */     CollisionMap collisionMap = getCollisionMap();
/*   63 */     if (collisionMap == null)
/*      */     {
/*      */       
/*   66 */       return true;
/*      */     }
/*   68 */     return collisionMap.walkable((short)tile.getX(), (short)tile.getY(), (byte)tile.getPlane());
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public void onMeleeAnimationDetected(AttackTimerConfig config) {
/*   77 */     if (config != null && config.logHandlerActions())
/*      */     {
/*   79 */       Logger.norm("[Both Unfrozen] Melee animation detected");
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
/*      */   private WorldPoint generateKitingTile(WorldPoint playerPos, WorldPoint opponentPos, int targetDistance) {
/*   94 */     int targetX, targetY, deltaX = playerPos.getX() - opponentPos.getX();
/*   95 */     int deltaY = playerPos.getY() - opponentPos.getY();
/*      */ 
/*      */     
/*   98 */     int absDeltaX = Math.abs(deltaX);
/*   99 */     int absDeltaY = Math.abs(deltaY);
/*  100 */     int currentDistance = Math.max(absDeltaX, absDeltaY);
/*      */ 
/*      */     
/*  103 */     if (currentDistance == 0) {
/*      */ 
/*      */       
/*  106 */       int dir = this.random.nextInt(8);
/*  107 */       switch (dir) {
/*      */         case 0:
/*  109 */           deltaX = 1; deltaY = 0; break;
/*  110 */         case 1: deltaX = -1; deltaY = 0; break;
/*  111 */         case 2: deltaX = 0; deltaY = 1; break;
/*  112 */         case 3: deltaX = 0; deltaY = -1; break;
/*  113 */         case 4: deltaX = 1; deltaY = 1; break;
/*  114 */         case 5: deltaX = -1; deltaY = 1; break;
/*  115 */         case 6: deltaX = 1; deltaY = -1; break;
/*  116 */         case 7: deltaX = -1; deltaY = -1; break;
/*      */       } 
/*  118 */       absDeltaX = Math.abs(deltaX);
/*  119 */       absDeltaY = Math.abs(deltaY);
/*  120 */       currentDistance = 1;
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  127 */     if (currentDistance > 0) {
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  132 */       int signX = (deltaX >= 0) ? 1 : -1;
/*  133 */       int signY = (deltaY >= 0) ? 1 : -1;
/*      */ 
/*      */ 
/*      */       
/*  137 */       if (absDeltaX > 0 && absDeltaY > 0) {
/*      */ 
/*      */         
/*  140 */         if (absDeltaX >= absDeltaY)
/*      */         {
/*  142 */           targetX = opponentPos.getX() + targetDistance * signX;
/*  143 */           targetY = opponentPos.getY() + absDeltaY * targetDistance / absDeltaX * signY;
/*      */         }
/*      */         else
/*      */         {
/*  147 */           targetX = opponentPos.getX() + absDeltaX * targetDistance / absDeltaY * signX;
/*  148 */           targetY = opponentPos.getY() + targetDistance * signY;
/*      */         }
/*      */       
/*  151 */       } else if (absDeltaX > 0) {
/*      */ 
/*      */         
/*  154 */         targetX = opponentPos.getX() + targetDistance * signX;
/*  155 */         targetY = opponentPos.getY();
/*      */       
/*      */       }
/*      */       else {
/*      */         
/*  160 */         targetX = opponentPos.getX();
/*  161 */         targetY = opponentPos.getY() + targetDistance * signY;
/*      */       } 
/*      */ 
/*      */       
/*  165 */       int actualDx = Math.abs(targetX - opponentPos.getX());
/*  166 */       int actualDy = Math.abs(targetY - opponentPos.getY());
/*  167 */       int actualDistance = Math.max(actualDx, actualDy);
/*      */       
/*  169 */       if (actualDistance != targetDistance)
/*      */       {
/*      */         
/*  172 */         if (absDeltaX >= absDeltaY)
/*      */         {
/*  174 */           targetX = opponentPos.getX() + targetDistance * signX;
/*  175 */           targetY = opponentPos.getY();
/*      */         }
/*      */         else
/*      */         {
/*  179 */           targetX = opponentPos.getX();
/*  180 */           targetY = opponentPos.getY() + targetDistance * signY;
/*      */         }
/*      */       
/*      */       }
/*      */     }
/*      */     else {
/*      */       
/*  187 */       targetX = opponentPos.getX() + targetDistance;
/*  188 */       targetY = opponentPos.getY();
/*      */     } 
/*      */ 
/*      */     
/*  192 */     int randomOffsetX = this.random.nextInt(3) - 1;
/*  193 */     int randomOffsetY = this.random.nextInt(3) - 1;
/*      */     
/*  195 */     int newX = targetX + randomOffsetX;
/*  196 */     int newY = targetY + randomOffsetY;
/*      */ 
/*      */     
/*  199 */     int checkDx = Math.abs(newX - opponentPos.getX());
/*  200 */     int checkDy = Math.abs(newY - opponentPos.getY());
/*  201 */     int checkDistance = Math.max(checkDx, checkDy);
/*      */ 
/*      */     
/*  204 */     if (checkDistance < 5 || checkDistance > 8) {
/*      */       
/*  206 */       newX = targetX;
/*  207 */       newY = targetY;
/*      */     } 
/*      */     
/*  210 */     return new WorldPoint(newX, newY, playerPos.getPlane());
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private WorldPoint generateRandomTileFromTarget(WorldPoint opponentPos, WorldPoint lastClicked, int plane) {
/*  221 */     WorldPoint targetTile = null;
/*  222 */     int attempts = 0;
/*      */ 
/*      */ 
/*      */     
/*  226 */     while (targetTile == null && attempts < 200) {
/*      */ 
/*      */       
/*  229 */       int dx, dy, targetDistance = this.random.nextInt(4) + 6;
/*      */ 
/*      */       
/*  232 */       int[] directions = { 1, 0, -1, 0, 0, 1, 0, -1, 1, 1, -1, 1, 1, -1, -1, -1 };
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
/*  244 */       int dirIndex = this.random.nextInt(8);
/*  245 */       int baseDx = directions[dirIndex * 2];
/*  246 */       int baseDy = directions[dirIndex * 2 + 1];
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  252 */       if (baseDx == 0 || baseDy == 0) {
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*  257 */         dx = baseDx * targetDistance;
/*  258 */         dy = baseDy * targetDistance;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*  264 */         int perpendicularOffset = this.random.nextInt(targetDistance * 2 + 1) - targetDistance;
/*  265 */         if (baseDx == 0) {
/*      */ 
/*      */           
/*  268 */           dx = perpendicularOffset;
/*  269 */           dy = baseDy * targetDistance;
/*      */         
/*      */         }
/*      */         else {
/*      */           
/*  274 */           dx = baseDx * targetDistance;
/*  275 */           dy = perpendicularOffset;
/*      */         } 
/*      */ 
/*      */         
/*  279 */         int absDx = Math.abs(dx);
/*  280 */         int absDy = Math.abs(dy);
/*  281 */         if (Math.max(absDx, absDy) != targetDistance)
/*      */         {
/*      */           
/*  284 */           if (baseDx == 0)
/*      */           {
/*      */             
/*  287 */             dy = baseDy * targetDistance;
/*      */             
/*  289 */             if (absDx > targetDistance)
/*      */             {
/*  291 */               dx = (dx > 0) ? targetDistance : -targetDistance;
/*      */             
/*      */             }
/*      */           }
/*      */           else
/*      */           {
/*  297 */             dx = baseDx * targetDistance;
/*      */             
/*  299 */             if (absDy > targetDistance)
/*      */             {
/*  301 */               dy = (dy > 0) ? targetDistance : -targetDistance;
/*      */             }
/*      */           }
/*      */         
/*      */         }
/*      */       }
/*      */       else {
/*      */         
/*  309 */         dx = baseDx * targetDistance;
/*  310 */         dy = baseDy * targetDistance;
/*      */       } 
/*      */ 
/*      */       
/*  314 */       int targetX = opponentPos.getX() + dx;
/*  315 */       int targetY = opponentPos.getY() + dy;
/*      */ 
/*      */       
/*  318 */       int checkDx = Math.abs(targetX - opponentPos.getX());
/*  319 */       int checkDy = Math.abs(targetY - opponentPos.getY());
/*  320 */       int checkDistance = Math.max(checkDx, checkDy);
/*      */ 
/*      */       
/*  323 */       if (checkDistance == targetDistance && checkDistance >= 6 && checkDistance <= 9) {
/*      */         
/*  325 */         WorldPoint candidateTile = new WorldPoint(targetX, targetY, plane);
/*      */ 
/*      */         
/*  328 */         if ((lastClicked == null || !candidateTile.equals(lastClicked)) && isWalkable(candidateTile)) {
/*      */           
/*  330 */           targetTile = candidateTile;
/*      */           break;
/*      */         } 
/*      */       } 
/*  334 */       attempts++;
/*      */     } 
/*      */ 
/*      */     
/*  338 */     if (targetTile == null) {
/*      */ 
/*      */       
/*  341 */       int[] directions = { 1, 0, -1, 0, 0, 1, 0, -1, 1, 1, -1, 1, 1, -1, -1, -1 };
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
/*  352 */       int fallbackDistance = this.random.nextInt(4) + 6;
/*  353 */       int startDir = this.random.nextInt(8);
/*      */ 
/*      */       
/*  356 */       for (int i = 0; i < 8; i++) {
/*      */         
/*  358 */         int dirIndex = (startDir + i) % 8;
/*  359 */         int baseDx = directions[dirIndex * 2];
/*  360 */         int baseDy = directions[dirIndex * 2 + 1];
/*      */ 
/*      */         
/*  363 */         int dx = baseDx * fallbackDistance;
/*  364 */         int dy = baseDy * fallbackDistance;
/*      */ 
/*      */ 
/*      */         
/*  368 */         WorldPoint candidate = new WorldPoint(opponentPos.getX() + dx, opponentPos.getY() + dy, plane);
/*      */ 
/*      */ 
/*      */         
/*  372 */         if ((lastClicked == null || !candidate.equals(lastClicked)) && isWalkable(candidate)) {
/*      */           
/*  374 */           targetTile = candidate;
/*      */           
/*      */           break;
/*      */         } 
/*      */       } 
/*      */       
/*  380 */       if (targetTile == null) {
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*  385 */         WorldPoint defaultTile = new WorldPoint(opponentPos.getX() + 6, opponentPos.getY(), plane);
/*      */ 
/*      */ 
/*      */         
/*  389 */         if (isWalkable(defaultTile)) {
/*      */           
/*  391 */           targetTile = defaultTile;
/*      */         
/*      */         }
/*      */         else {
/*      */           
/*  396 */           boolean found = false;
/*  397 */           for (int radius = 1; radius <= 3 && !found; radius++) {
/*      */             
/*  399 */             for (int offsetX = -radius; offsetX <= radius && !found; offsetX++) {
/*      */               
/*  401 */               for (int offsetY = -radius; offsetY <= radius && !found; offsetY++) {
/*      */ 
/*      */                 
/*  404 */                 if (Math.abs(offsetX) == radius || Math.abs(offsetY) == radius) {
/*      */ 
/*      */ 
/*      */                   
/*  408 */                   WorldPoint checkTile = new WorldPoint(defaultTile.getX() + offsetX, defaultTile.getY() + offsetY, plane);
/*      */ 
/*      */ 
/*      */ 
/*      */                   
/*  413 */                   int checkDx = Math.abs(checkTile.getX() - opponentPos.getX());
/*  414 */                   int checkDy = Math.abs(checkTile.getY() - opponentPos.getY());
/*  415 */                   int checkDist = Math.max(checkDx, checkDy);
/*      */                   
/*  417 */                   if (checkDist >= 6 && checkDist <= 9 && isWalkable(checkTile)) {
/*      */                     
/*  419 */                     targetTile = checkTile;
/*  420 */                     found = true;
/*      */                   } 
/*      */                 } 
/*      */               } 
/*      */             } 
/*      */           } 
/*      */ 
/*      */           
/*  428 */           if (targetTile == null)
/*      */           {
/*  430 */             targetTile = defaultTile;
/*      */           }
/*      */         } 
/*      */       } 
/*      */     } 
/*      */     
/*  436 */     return targetTile;
/*      */   }
/*      */ 
/*      */ 
/*      */   
/*      */   public boolean handle(CombatStateContext ctx) {
/*  442 */     if (ctx.weFrozen || ctx.targetFrozen) {
/*      */       
/*  444 */       this.specModeActive = false;
/*  445 */       this.specModeTicks = 0;
/*      */       
/*  447 */       this.ticksSinceLastFailedAttack = 0;
/*  448 */       this.pidWhipModeActive = false;
/*  449 */       this.pidWhipModeTicks = 0;
/*  450 */       this.lastAttackType = null;
/*  451 */       return false;
/*      */     } 
/*      */ 
/*      */     
/*  455 */     ctx.helpers.setMindAction("Both Unfrozen");
/*      */ 
/*      */     
/*  458 */     int attackTimerTicks = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*      */ 
/*      */     
/*  461 */     if (this.ticksSinceLastFailedAttack > 0) {
/*      */       
/*  463 */       this.ticksSinceLastFailedAttack++;
/*  464 */       if (this.ticksSinceLastFailedAttack > 2)
/*      */       {
/*  466 */         this.ticksSinceLastFailedAttack = 0;
/*      */       }
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  473 */     boolean canAttackIdle = (attackTimerTicks == 0 && ctx.ticksAtZero >= 3);
/*  474 */     if ((attackTimerTicks <= 0 || canAttackIdle) && this.ticksSinceLastFailedAttack == 0) {
/*      */ 
/*      */       
/*  477 */       int i = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*  478 */       boolean attacksSynced = (attackTimerTicks == i);
/*  479 */       int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/*  480 */       boolean inMeleeRange = (distance < 5);
/*      */       
/*  482 */       int roll = this.random.nextInt(100);
/*  483 */       boolean attackExecuted = false;
/*      */       
/*  485 */       if (inMeleeRange) {
/*      */ 
/*      */         
/*  488 */         if (roll < 55)
/*      */         {
/*      */           
/*  491 */           boolean useTankMage = attacksSynced;
/*      */           
/*  493 */           attackExecuted = AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Both Unfrozen]", true, useTankMage, false, false);
/*  494 */           if (attackExecuted)
/*      */           {
/*  496 */             this.lastAttackType = "Magic";
/*      */           }
/*      */         }
/*  499 */         else if (roll < 90)
/*      */         {
/*      */           
/*  502 */           attackExecuted = AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[Both Unfrozen]");
/*  503 */           if (attackExecuted) this.lastAttackType = "Melee";
/*      */         
/*      */         }
/*      */         else
/*      */         {
/*  508 */           attackExecuted = AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Both Unfrozen]");
/*  509 */           if (attackExecuted) this.lastAttackType = "Range";
/*      */           
/*      */         
/*      */         }
/*      */       
/*      */       }
/*  515 */       else if (roll < 72) {
/*      */ 
/*      */         
/*  518 */         boolean useTankMage = attacksSynced;
/*      */         
/*  520 */         attackExecuted = AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Both Unfrozen]", true, useTankMage, false, false);
/*  521 */         if (attackExecuted)
/*      */         {
/*  523 */           this.lastAttackType = "Magic";
/*      */         
/*      */         }
/*      */       }
/*      */       else {
/*      */         
/*  529 */         attackExecuted = AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Both Unfrozen]");
/*  530 */         if (attackExecuted) this.lastAttackType = "Range";
/*      */       
/*      */       } 
/*      */       
/*  534 */       if (attackExecuted) {
/*      */         
/*  536 */         this.lastClickedTile = null;
/*  537 */         if (ctx.config.logHandlerActions())
/*      */         {
/*  539 */           Logger.norm("[Both Unfrozen] Timer is 0 - attack executed successfully");
/*      */         }
/*  541 */         return true;
/*      */       } 
/*      */ 
/*      */ 
/*      */       
/*  546 */       boolean fallbackExecuted = false;
/*      */       
/*  548 */       if (!inMeleeRange) {
/*      */ 
/*      */         
/*  551 */         int fallbackRoll = this.random.nextInt(100);
/*      */         
/*  553 */         if (fallbackRoll < 50)
/*      */         {
/*      */           
/*  556 */           fallbackExecuted = AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Both Unfrozen-Fallback]");
/*  557 */           if (fallbackExecuted) this.lastAttackType = "Range"; 
/*  558 */           if (fallbackExecuted && ctx.config.logHandlerActions())
/*      */           {
/*  560 */             Logger.norm("[Both Unfrozen] Primary attack failed - fallback to Range attack SUCCESS (distance: " + distance + ")");
/*      */           
/*      */           }
/*      */         }
/*      */         else
/*      */         {
/*  566 */           int opponentTimer = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*  567 */           boolean useTankMage = (attackTimerTicks == opponentTimer);
/*      */           
/*  569 */           fallbackExecuted = AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Both Unfrozen-Fallback]", true, useTankMage, false, false);
/*  570 */           if (fallbackExecuted)
/*      */           {
/*  572 */             this.lastAttackType = "Magic";
/*      */           }
/*  574 */           if (fallbackExecuted && ctx.config.logHandlerActions())
/*      */           {
/*  576 */             Logger.norm("[Both Unfrozen] Primary attack failed - fallback to Mage attack SUCCESS (distance: " + distance + ")");
/*      */           }
/*      */         }
/*      */       
/*      */       }
/*      */       else {
/*      */         
/*  583 */         int fallbackRoll = this.random.nextInt(100);
/*  584 */         if (fallbackRoll < 50) {
/*      */ 
/*      */           
/*  587 */           boolean useTankMage = attacksSynced;
/*      */           
/*  589 */           fallbackExecuted = AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Both Unfrozen-Fallback]", true, useTankMage, false, false);
/*  590 */           if (fallbackExecuted)
/*      */           {
/*  592 */             this.lastAttackType = "Magic";
/*      */           
/*      */           }
/*      */         }
/*      */         else {
/*      */           
/*  598 */           fallbackExecuted = AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Both Unfrozen-Fallback]");
/*  599 */           if (fallbackExecuted) this.lastAttackType = "Range";
/*      */         
/*      */         } 
/*      */       } 
/*  603 */       if (fallbackExecuted) {
/*      */ 
/*      */         
/*  606 */         this.lastClickedTile = null;
/*  607 */         if (ctx.config.logHandlerActions())
/*      */         {
/*  609 */           Logger.norm("[Both Unfrozen] Timer is 0 - fallback attack executed successfully");
/*      */         }
/*  611 */         return true;
/*      */       } 
/*      */ 
/*      */ 
/*      */       
/*  616 */       this.ticksSinceLastFailedAttack = 1;
/*  617 */       if (ctx.config.logHandlerActions())
/*      */       {
/*  619 */         Logger.norm("[Both Unfrozen] Timer is 0 - all attack types failed (distance: " + distance + ", inMeleeRange: " + inMeleeRange + ") - will retry next tick");
/*      */ 
/*      */       
/*      */       }
/*      */     
/*      */     }
/*  625 */     else if (attackTimerTicks <= 0 && this.ticksSinceLastFailedAttack > 0) {
/*      */ 
/*      */       
/*  628 */       this.ticksSinceLastFailedAttack++;
/*  629 */       if (this.ticksSinceLastFailedAttack > 2) {
/*      */ 
/*      */         
/*  632 */         this.ticksSinceLastFailedAttack = 0;
/*      */       }
/*  634 */       else if (ctx.config.logHandlerActions() && this.ticksSinceLastFailedAttack == 1) {
/*      */         
/*  636 */         Logger.norm("[Both Unfrozen] Timer is 0 but in cooldown after failed attack (tick " + this.ticksSinceLastFailedAttack + "/2) - will retry after cooldown");
/*      */       } 
/*      */     } 
/*      */ 
/*      */ 
/*      */     
/*  642 */     if (attackTimerTicks == 2 || attackTimerTicks == 3) {
/*      */       
/*  644 */       int fakieChance = ctx.config.fakieChance();
/*  645 */       if (fakieChance > 0 && fakieChance <= 100) {
/*      */         
/*  647 */         int roll = this.random.nextInt(100);
/*  648 */         if (roll < fakieChance) {
/*      */ 
/*      */           
/*  651 */           String itemToEquip = this.random.nextBoolean() ? "Abyssal tentacle" : "Dragon crossbow";
/*  652 */           boolean equipped = ctx.helpers.equipItemByName(itemToEquip);
/*  653 */           Logger.norm("[Both Unfrozen] Fakie triggered (" + fakieChance + "% chance, roll: " + roll + ") - Attempting to equip: " + itemToEquip + " - Result: " + (equipped ? "Success" : "Failed (not in inventory or already equipped)"));
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  663 */     int gearSwapTick = GameManager.getTickCount();
/*  664 */     boolean justCastMagic = "Magic".equals(this.lastAttackType);
/*  665 */     boolean tankGearJustEquipped = (ctx.lastEquipTick.value == gearSwapTick);
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  670 */     if (attackTimerTicks >= 1 && attackTimerTicks <= 5 && !justCastMagic && !tankGearJustEquipped) {
/*      */ 
/*      */       
/*  673 */       ctx.lastEquipTick.value = gearSwapTick;
/*  674 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  681 */     if (!this.specModeActive) {
/*      */ 
/*      */       
/*  684 */       boolean canWeMelee = AttackMethods.canWeMelee(ctx, ctx.opponent);
/*      */       
/*  686 */       if (canWeMelee) {
/*      */ 
/*      */         
/*  689 */         int anglerCount = 0;
/*  690 */         int brewCount = InventoryAPI.count(new String[] { "Saradomin brew(4)", "Saradomin brew(3)", "Saradomin brew(2)", "Saradomin brew(1)" });
/*      */ 
/*      */         
/*  693 */         for (ItemEx item : InventoryAPI.getItems()) {
/*      */           
/*  695 */           if (item != null && item.getName() != null && item.getName().toLowerCase().contains("angler"))
/*      */           {
/*  697 */             anglerCount += item.getQuantity();
/*      */           }
/*      */         } 
/*      */ 
/*      */         
/*  702 */         if (anglerCount < 2 && brewCount < 1) {
/*      */           
/*  704 */           int specEnergy = CombatAPI.getSpecEnergy();
/*  705 */           if (specEnergy >= 50) {
/*      */             
/*  707 */             this.specModeActive = true;
/*  708 */             this.specModeTicks = 0;
/*      */           } 
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  718 */     if (!this.specModeActive) {
/*      */       
/*  720 */       int specEnergy = CombatAPI.getSpecEnergy();
/*  721 */       if (specEnergy >= 50) {
/*      */ 
/*      */         
/*  724 */         int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/*  725 */         boolean canWeMelee = AttackMethods.canWeMelee(ctx, ctx.opponent);
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*  731 */         boolean canExecuteSpec = (canWeMelee && (attackTimerTicks <= 1 || (attackTimerTicks == 0 && ctx.ticksAtZero >= 3)));
/*  732 */         boolean canMoveCloser = (!canWeMelee && attackTimerTicks > 3);
/*      */         
/*  734 */         if (canExecuteSpec || canMoveCloser) {
/*      */ 
/*      */           
/*  737 */           int specRoll = this.random.nextInt(100);
/*  738 */           if (specRoll < 50) {
/*      */             
/*  740 */             this.specModeActive = true;
/*  741 */             this.specModeTicks = 0;
/*      */           } 
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */ 
/*      */     
/*  749 */     if (this.specModeActive) {
/*      */ 
/*      */       
/*  752 */       this.specModeTicks++;
/*  753 */       if (this.specModeTicks >= 2) {
/*      */ 
/*      */         
/*  756 */         this.specModeActive = false;
/*  757 */         this.specModeTicks = 0;
/*  758 */         Logger.norm("[Both Unfrozen] Spec mode timeout after 2 ticks - exiting spec mode and continuing with normal logic");
/*      */       } 
/*      */ 
/*      */ 
/*      */       
/*  763 */       if (this.specModeActive) {
/*      */         
/*  765 */         int specEnergy = CombatAPI.getSpecEnergy();
/*  766 */         if (specEnergy < 50) {
/*      */ 
/*      */           
/*  769 */           this.specModeActive = false;
/*  770 */           this.specModeTicks = 0;
/*  771 */           Logger.norm("[Both Unfrozen] Exited spec mode - spec energy dropped below 50% (" + specEnergy + "%)");
/*      */         
/*      */         }
/*      */         else {
/*      */           
/*  776 */           int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/*  777 */           boolean canWeMelee = AttackMethods.canWeMelee(ctx, ctx.opponent);
/*      */ 
/*      */           
/*  780 */           if (attackTimerTicks == 2)
/*      */           {
/*      */ 
/*      */ 
/*      */             
/*  785 */             if (!canWeMelee) {
/*      */ 
/*      */               
/*  788 */               this.specModeActive = false;
/*  789 */               Logger.norm("[Both Unfrozen] Exited spec mode at timer 2 - not in range (distance: " + distance + ", timer: " + attackTimerTicks + "), doing normal attack");
/*      */             } 
/*      */           }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */           
/*  800 */           if (canWeMelee && (attackTimerTicks <= 1 || (attackTimerTicks == 0 && ctx.ticksAtZero >= 3))) {
/*      */             
/*  802 */             boolean specExecuted = AttackMethods.executeSpecialAttack(ctx, ctx.opponent, "[Both Unfrozen]");
/*  803 */             if (specExecuted) this.lastAttackType = "Melee"; 
/*  804 */             if (specExecuted) {
/*      */               
/*  806 */               this.specModeActive = false;
/*  807 */               this.specModeTicks = 0;
/*  808 */               this.lastClickedTile = null;
/*  809 */               return true;
/*      */             } 
/*      */ 
/*      */ 
/*      */             
/*  814 */             this.specModeActive = false;
/*  815 */             this.specModeTicks = 0;
/*  816 */             Logger.norm("[Both Unfrozen] Spec execution failed at timer " + attackTimerTicks + " - exiting spec mode, allowing normal attacks");
/*      */           }
/*      */           else {
/*      */             
/*  820 */             if (!canWeMelee && attackTimerTicks > 3) {
/*      */ 
/*      */               
/*  823 */               Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/*  824 */               Objects.requireNonNull(ctx.opponent); WorldPoint opponentPos = (WorldPoint)Static.invoke(ctx.opponent::getWorldLocation);
/*      */ 
/*      */               
/*  827 */               int dx = opponentPos.getX() - playerPos.getX();
/*  828 */               int dy = opponentPos.getY() - playerPos.getY();
/*      */ 
/*      */               
/*  831 */               int moveX = 0;
/*  832 */               int moveY = 0;
/*      */               
/*  834 */               if (Math.abs(dx) > 0)
/*      */               {
/*  836 */                 moveX = (dx > 0) ? 1 : -1;
/*      */               }
/*  838 */               if (Math.abs(dy) > 0)
/*      */               {
/*  840 */                 moveY = (dy > 0) ? 1 : -1;
/*      */               }
/*      */ 
/*      */               
/*  844 */               if (moveX != 0 && moveY != 0)
/*      */               {
/*  846 */                 if (Math.abs(dx) > Math.abs(dy)) {
/*      */                   
/*  848 */                   moveY = 0;
/*      */                 }
/*      */                 else {
/*      */                   
/*  852 */                   moveX = 0;
/*      */                 } 
/*      */               }
/*      */ 
/*      */ 
/*      */ 
/*      */               
/*  859 */               WorldPoint moveTo = new WorldPoint(playerPos.getX() + moveX, playerPos.getY() + moveY, playerPos.getPlane());
/*      */ 
/*      */               
/*  862 */               MovementAPI.walkToWorldPoint(moveTo);
/*      */               
/*  864 */               return true;
/*      */             } 
/*      */             
/*  867 */             if (attackTimerTicks <= 0 && !canWeMelee) {
/*      */ 
/*      */               
/*  870 */               this.specModeActive = false;
/*  871 */               this.specModeTicks = 0;
/*  872 */               Logger.norm("[Both Unfrozen] Spec mode exit - timer " + attackTimerTicks + ", not in range (distance: " + distance + "), allowing normal attacks");
/*      */             } 
/*      */           } 
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  882 */     if (attackTimerTicks == 2 || attackTimerTicks == 3) {
/*      */ 
/*      */       
/*  885 */       boolean hasPid = (ctx.currentPidStatus == CombatStateContext.PidStatus.ON_PID);
/*  886 */       int i = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*  887 */       boolean attacksSynced = (attackTimerTicks == i);
/*      */       
/*  889 */       if (hasPid && !ctx.weFrozen && !ctx.targetFrozen && attacksSynced) {
/*      */ 
/*      */         
/*  892 */         int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/*  893 */         boolean inMeleeRange = (distance < 5);
/*  894 */         int roll = this.random.nextInt(100);
/*  895 */         boolean wouldMage = false;
/*      */         
/*  897 */         if (inMeleeRange) {
/*      */           
/*  899 */           wouldMage = (roll < 55);
/*      */         }
/*      */         else {
/*      */           
/*  903 */           wouldMage = (roll < 72);
/*      */         } 
/*      */         
/*  906 */         if (wouldMage) {
/*      */ 
/*      */ 
/*      */           
/*  910 */           Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/*  911 */           Objects.requireNonNull(ctx.opponent); WorldPoint opponentPos = (WorldPoint)Static.invoke(ctx.opponent::getWorldLocation);
/*      */           
/*  913 */           int dx = opponentPos.getX() - playerPos.getX();
/*  914 */           int dy = opponentPos.getY() - playerPos.getY();
/*      */ 
/*      */           
/*  917 */           int moveX = 0;
/*  918 */           int moveY = 0;
/*      */           
/*  920 */           if (Math.abs(dx) > 0)
/*      */           {
/*  922 */             moveX = (dx > 0) ? 1 : -1;
/*      */           }
/*  924 */           if (Math.abs(dy) > 0)
/*      */           {
/*  926 */             moveY = (dy > 0) ? 1 : -1;
/*      */           }
/*      */ 
/*      */           
/*  930 */           if (moveX != 0 && moveY != 0)
/*      */           {
/*  932 */             if (Math.abs(dx) > Math.abs(dy)) {
/*      */               
/*  934 */               moveY = 0;
/*      */             }
/*      */             else {
/*      */               
/*  938 */               moveX = 0;
/*      */             } 
/*      */           }
/*      */ 
/*      */ 
/*      */ 
/*      */           
/*  945 */           WorldPoint moveTo = new WorldPoint(playerPos.getX() + moveX, playerPos.getY() + moveY, playerPos.getPlane());
/*      */ 
/*      */           
/*  948 */           MovementAPI.walkToWorldPoint(moveTo);
/*  949 */           this.pidWhipModeActive = true;
/*  950 */           this.pidWhipModeTicks = 0;
/*      */           
/*  952 */           if (ctx.config.combatAutomationLogging())
/*      */           {
/*  954 */             Logger.norm("[Both Unfrozen] PID Whip Mode: Moving towards target (timer: " + attackTimerTicks + ", synced: " + attacksSynced + ", would mage: " + wouldMage + ")");
/*      */           }
/*      */ 
/*      */           
/*  958 */           return true;
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */     
/*  964 */     if (attackTimerTicks > 3 || attackTimerTicks < 1)
/*      */     {
/*  966 */       if (this.pidWhipModeActive) {
/*      */         
/*  968 */         this.pidWhipModeActive = false;
/*  969 */         this.pidWhipModeTicks = 0;
/*      */       } 
/*      */     }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  977 */     if ((attackTimerTicks == 1 || (attackTimerTicks == 0 && ctx.ticksAtZero >= 3)) && !this.specModeActive)
/*      */     {
/*      */       
/*  980 */       if (this.pidWhipModeActive) {
/*      */ 
/*      */         
/*  983 */         int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/*  984 */         boolean inMeleeRange = (distance <= 1);
/*      */         
/*  986 */         if (inMeleeRange) {
/*      */ 
/*      */           
/*  989 */           boolean attackExecuted = AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[Both Unfrozen] PID Whip");
/*  990 */           if (attackExecuted) this.lastAttackType = "Melee";
/*      */           
/*  992 */           if (attackExecuted) {
/*      */ 
/*      */ 
/*      */             
/*  996 */             this.pidWhipModeActive = false;
/*  997 */             this.pidWhipModeTicks = 0;
/*      */             
/*  999 */             if (ctx.config.combatAutomationLogging())
/*      */             {
/* 1001 */               Logger.norm("[Both Unfrozen] PID Whip executed (timer: 1, in melee range)");
/*      */             }
/*      */             
/* 1004 */             this.lastClickedTile = null;
/* 1005 */             return true;
/*      */           } 
/*      */ 
/*      */ 
/*      */           
/* 1010 */           int i = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*      */           
/* 1012 */           if (i > 1) {
/*      */ 
/*      */             
/* 1015 */             boolean useTankMage = true;
/*      */             
/* 1017 */             boolean fallbackExecuted = AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Both Unfrozen] PID Whip Fallback", true, useTankMage, false, false);
/* 1018 */             if (fallbackExecuted) this.lastAttackType = "Magic";
/*      */             
/* 1020 */             if (fallbackExecuted)
/*      */             {
/* 1022 */               if (ctx.config.combatAutomationLogging())
/*      */               {
/* 1024 */                 Logger.norm("[Both Unfrozen] PID Whip failed - using ice barrage fallback (opponent timer: " + i + ")");
/*      */               }
/*      */               
/* 1027 */               this.pidWhipModeActive = false;
/* 1028 */               this.pidWhipModeTicks = 0;
/* 1029 */               this.lastClickedTile = null;
/* 1030 */               return true;
/*      */             }
/*      */           
/*      */           }
/*      */           else {
/*      */             
/* 1036 */             boolean fallbackExecuted = AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Both Unfrozen] PID Whip Fallback");
/* 1037 */             if (fallbackExecuted) this.lastAttackType = "Range";
/*      */             
/* 1039 */             if (fallbackExecuted) {
/*      */               
/* 1041 */               if (ctx.config.combatAutomationLogging())
/*      */               {
/* 1043 */                 Logger.norm("[Both Unfrozen] PID Whip failed - using bolt fallback (opponent timer: " + i + ")");
/*      */               }
/*      */               
/* 1046 */               this.pidWhipModeActive = false;
/* 1047 */               this.pidWhipModeTicks = 0;
/* 1048 */               this.lastClickedTile = null;
/* 1049 */               return true;
/*      */             } 
/*      */           } 
/*      */ 
/*      */           
/* 1054 */           this.pidWhipModeActive = false;
/* 1055 */           this.pidWhipModeTicks = 0;
/*      */           
/* 1057 */           if (ctx.config.combatAutomationLogging())
/*      */           {
/* 1059 */             Logger.norm("[Both Unfrozen] PID Whip and all fallbacks failed - resetting mode");
/*      */           
/*      */           }
/*      */         
/*      */         }
/*      */         else {
/*      */           
/* 1066 */           this.pidWhipModeActive = false;
/* 1067 */           this.pidWhipModeTicks = 0;
/*      */           
/* 1069 */           if (ctx.config.combatAutomationLogging())
/*      */           {
/* 1071 */             Logger.norm("[Both Unfrozen] PID Whip Mode: Not in melee range yet (distance: " + distance + ") - resetting mode");
/*      */           }
/*      */         } 
/*      */       } 
/*      */     }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1081 */     if ((attackTimerTicks == 1 || (attackTimerTicks == 0 && ctx.ticksAtZero >= 3)) && !this.specModeActive && !this.pidWhipModeActive) {
/*      */ 
/*      */       
/* 1084 */       int i = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/* 1085 */       boolean attacksSynced = (attackTimerTicks == i);
/*      */ 
/*      */       
/* 1088 */       int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/* 1089 */       boolean inMeleeRange = (distance < 5);
/*      */       
/* 1091 */       int roll = this.random.nextInt(100);
/* 1092 */       boolean attackExecuted = false;
/*      */       
/* 1094 */       if (inMeleeRange) {
/*      */ 
/*      */         
/* 1097 */         if (roll < 55) {
/*      */ 
/*      */           
/* 1100 */           boolean useTankMage = attacksSynced;
/*      */           
/* 1102 */           attackExecuted = AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Both Unfrozen]", true, useTankMage, false, false);
/* 1103 */           if (attackExecuted) this.lastAttackType = "Magic";
/*      */         
/* 1105 */         } else if (roll < 90) {
/*      */ 
/*      */           
/* 1108 */           attackExecuted = AttackMethods.executeMeleeAttack(ctx, ctx.opponent, "[Both Unfrozen]");
/* 1109 */           if (attackExecuted) this.lastAttackType = "Melee";
/*      */         
/*      */         }
/*      */         else {
/*      */           
/* 1114 */           attackExecuted = AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Both Unfrozen]");
/* 1115 */           if (attackExecuted) this.lastAttackType = "Range";
/*      */           
/*      */         
/*      */         }
/*      */       
/*      */       }
/* 1121 */       else if (roll < 72) {
/*      */ 
/*      */         
/* 1124 */         boolean useTankMage = attacksSynced;
/*      */         
/* 1126 */         attackExecuted = AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Both Unfrozen]", true, useTankMage, false, false);
/* 1127 */         if (attackExecuted) this.lastAttackType = "Magic";
/*      */       
/*      */       }
/*      */       else {
/*      */         
/* 1132 */         attackExecuted = AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Both Unfrozen]");
/* 1133 */         if (attackExecuted) this.lastAttackType = "Range";
/*      */       
/*      */       } 
/*      */ 
/*      */       
/* 1138 */       if (attackExecuted) {
/*      */         
/* 1140 */         this.lastClickedTile = null;
/* 1141 */         return true;
/*      */       } 
/*      */ 
/*      */ 
/*      */       
/* 1146 */       if (ctx.config.logHandlerActions())
/*      */       {
/* 1148 */         Logger.norm("[Both Unfrozen] Timer is 1 - attack attempt failed (distance: " + distance + ", inMeleeRange: " + inMeleeRange + ", roll: " + roll + ")");
/*      */       }
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1155 */     int health = ctx.helpers.getPlayerHealth();
/* 1156 */     int magicLevel = ctx.helpers.getPlayerMagicLevel();
/* 1157 */     int prayerPoints = ctx.helpers.getPlayerPrayerPoints();
/* 1158 */     boolean hasBrews = ctx.helpers.hasBrews();
/*      */ 
/*      */     
/* 1161 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/* 1162 */     if (opponentTimerTicks > 1 && health < 45 && !hasBrews) {
/*      */ 
/*      */       
/* 1165 */       int anglerCount = 0;
/* 1166 */       for (ItemEx item : InventoryAPI.getItems()) {
/*      */         
/* 1168 */         if (item != null && item.getName() != null && item.getName().toLowerCase().contains("angler"))
/*      */         {
/* 1170 */           anglerCount += item.getQuantity();
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/* 1175 */       if (anglerCount == 0)
/*      */       {
/* 1177 */         if (!PrayerAPI.REDEMPTION.isActive()) {
/*      */           
/* 1179 */           PrayerAPI.REDEMPTION.turnOn();
/* 1180 */           Logger.norm("[Both Unfrozen] Redemption activated - HP: " + health + ", No food (anglers: 0, brews: 0), Opponent timer: " + opponentTimerTicks);
/*      */         } 
/*      */       }
/*      */     } 
/*      */ 
/*      */     
/* 1186 */     if (prayerPoints < 25)
/*      */     {
/* 1188 */       if (ctx.helpers.drinkSanfewSerum())
/*      */       {
/* 1190 */         if (ctx.config.debug())
/*      */         {
/* 1192 */           Logger.norm("[Both Unfrozen] Prayer < 25 (" + prayerPoints + "), drank Sanfew serum");
/*      */         }
/*      */       }
/*      */     }
/*      */ 
/*      */ 
/*      */     
/* 1199 */     if (!hasBrews && health < 55)
/*      */     {
/* 1201 */       if (ctx.helpers.eatAngler())
/*      */       {
/* 1203 */         Logger.norm("[Both Unfrozen] Out of brews and HP < 55 (" + health + "), eating angler");
/*      */       }
/*      */     }
/*      */ 
/*      */ 
/*      */     
/* 1209 */     if (health < 35) {
/*      */ 
/*      */       
/* 1212 */       if (!this.needToBrewAfterLowHPAngler)
/*      */       {
/* 1214 */         if (ctx.helpers.eatAngler())
/*      */         {
/* 1216 */           Logger.norm("[Both Unfrozen] HP < 35 (" + health + "), eating anglerfish first, will brew on next tick");
/* 1217 */           this.needToBrewAfterLowHPAngler = true;
/* 1218 */           this.ticksSinceLowHPAngler = 0;
/*      */ 
/*      */         
/*      */         }
/*      */ 
/*      */       
/*      */       }
/* 1225 */       else if (ctx.helpers.sipSaradominBrew())
/*      */       {
/* 1227 */         Logger.norm("[Both Unfrozen] Drank brew after low HP anglerfish (next tick)");
/* 1228 */         this.needToBrewAfterLowHPAngler = false;
/* 1229 */         this.ticksSinceLowHPAngler = 0;
/*      */       
/*      */       }
/*      */       else
/*      */       {
/*      */         
/* 1235 */         this.needToBrewAfterLowHPAngler = false;
/* 1236 */         this.ticksSinceLowHPAngler = 0;
/*      */ 
/*      */       
/*      */       }
/*      */ 
/*      */     
/*      */     }
/* 1243 */     else if (this.needToBrewAfterLowHPAngler) {
/*      */       
/* 1245 */       this.needToBrewAfterLowHPAngler = false;
/* 1246 */       this.ticksSinceLowHPAngler = 0;
/*      */     } 
/*      */ 
/*      */ 
/*      */     
/* 1251 */     if (health >= 68 && magicLevel <= 88)
/*      */     {
/* 1253 */       if (ctx.helpers.drinkSanfewSerum()) {
/*      */         
/* 1255 */         if (ctx.config.debug())
/*      */         {
/* 1257 */           Logger.norm("[Both Unfrozen] HP >= 68 (" + health + ") but magic drained (" + magicLevel + "), drank Sanfew serum immediately");
/*      */ 
/*      */         
/*      */         }
/*      */       
/*      */       }
/* 1263 */       else if (ctx.config.debug()) {
/*      */         
/* 1265 */         Logger.norm("[Both Unfrozen] HP >= 68 (" + health + ") but magic drained (" + magicLevel + ") - no Sanfew serum found");
/*      */       } 
/*      */     }
/*      */ 
/*      */ 
/*      */     
/* 1271 */     if (attackTimerTicks > 3)
/*      */     {
/*      */       
/* 1274 */       if (health < 68)
/*      */       {
/* 1276 */         if (ctx.helpers.sipSaradominBrew()) {
/*      */ 
/*      */ 
/*      */           
/* 1280 */           if (ctx.config.debug())
/*      */           {
/* 1282 */             Logger.norm("[Both Unfrozen] HP below 68 (" + health + "), drank Saradomin brew");
/*      */ 
/*      */           
/*      */           }
/*      */         
/*      */         }
/* 1288 */         else if (ctx.config.debug()) {
/*      */           
/* 1290 */           Logger.norm("[Both Unfrozen] HP below 68 (" + health + ") but no Saradomin brew found");
/*      */         } 
/*      */       }
/*      */     }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1299 */     if (attackTimerTicks > 1 && attackTimerTicks <= 5)
/*      */     {
/*      */ 
/*      */       
/* 1303 */       ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/*      */     }
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1309 */     if (attackTimerTicks > 1 && !this.specModeActive) {
/*      */ 
/*      */       
/* 1312 */       Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/* 1313 */       Objects.requireNonNull(ctx.opponent); WorldPoint opponentPos = (WorldPoint)Static.invoke(ctx.opponent::getWorldLocation);
/*      */ 
/*      */ 
/*      */       
/* 1317 */       WorldPoint targetTile = generateRandomTileFromTarget(opponentPos, this.lastClickedTile, playerPos.getPlane());
/*      */ 
/*      */       
/* 1320 */       this.lastClickedTile = targetTile;
/*      */ 
/*      */       
/* 1323 */       MovementAPI.walkToWorldPoint(targetTile);
/*      */       
/* 1325 */       if (ctx.config.logHandlerActions()) {
/*      */         
/* 1327 */         int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/* 1328 */         int targetDistance = Math.max(
/* 1329 */             Math.abs(targetTile.getX() - opponentPos.getX()), 
/* 1330 */             Math.abs(targetTile.getY() - opponentPos.getY()));
/*      */         
/* 1332 */         Logger.norm("[Both Unfrozen] Timer > 1 (" + attackTimerTicks + ") - Clicking random tile " + targetDistance + " tiles away from target (current distance: " + distance + ")");
/*      */       } 
/*      */       
/* 1335 */       return true;
/*      */     } 
/*      */     
/* 1338 */     return false;
/*      */   }
/*      */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/BothUnfrozenHandler.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */