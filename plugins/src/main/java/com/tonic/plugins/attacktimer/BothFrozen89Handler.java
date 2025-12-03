/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.api.game.CombatAPI;
/*     */ import com.tonic.api.widgets.InventoryAPI;
/*     */ import com.tonic.api.widgets.PrayerAPI;
/*     */ import com.tonic.data.AttackStyle;
/*     */ import com.tonic.data.wrappers.ItemEx;
/*     */ import java.util.Random;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class BothFrozen89Handler
/*     */   implements CombatStateHandler
/*     */ {
/*  20 */   private final Random random = new Random();
/*     */   
/*     */   private boolean needToBrewAfterLowHPAngler = false;
/*     */   
/*  24 */   private int ticksSinceLowHPAngler = 0;
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean hasSetLongrange = false;
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void resetState() {
/*  34 */     this.hasSetLongrange = false;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean handle(CombatStateContext ctx) {
/*  40 */     if (!ctx.weFrozen || !ctx.targetFrozen) {
/*     */ 
/*     */       
/*  43 */       this.hasSetLongrange = false;
/*  44 */       return false;
/*     */     } 
/*     */ 
/*     */     
/*  48 */     int distance = AttackMethods.getDistance(ctx, ctx.opponent);
/*     */ 
/*     */     
/*  51 */     if (distance != 8 && distance != 9) {
/*     */ 
/*     */       
/*  54 */       this.hasSetLongrange = false;
/*  55 */       return false;
/*     */     } 
/*     */ 
/*     */     
/*  59 */     ctx.helpers.setMindAction("Both Frozen 8-9");
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*  64 */     if (!this.hasSetLongrange) {
/*     */       
/*  66 */       boolean crossbowEquipped = ctx.helpers.equipItemByName("Dragon crossbow");
/*  67 */       if (!crossbowEquipped)
/*     */       {
/*     */         
/*  70 */         crossbowEquipped = ctx.helpers.equipItemByName("crossbow");
/*     */       }
/*     */ 
/*     */       
/*  74 */       AttackStyle currentStyle = CombatAPI.getAttackStyle();
/*  75 */       if (currentStyle != AttackStyle.FOURTH) {
/*     */ 
/*     */         
/*  78 */         CombatAPI.setAttackStyle(AttackStyle.FOURTH);
/*  79 */         if (ctx.config.logHandlerActions())
/*     */         {
/*  81 */           Logger.norm("[Both Frozen 8-9] Setting crossbow to longrange via CombatAPI.setAttackStyle(FOURTH) (distance: " + distance + ", was: " + String.valueOf(currentStyle) + ")");
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/*  86 */       this.hasSetLongrange = true;
/*     */     } 
/*     */ 
/*     */     
/*  90 */     int attackTimerTicks = (ctx.playerTimerTicks < 0) ? 0 : ctx.playerTimerTicks;
/*  91 */     int opponentTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/*     */ 
/*     */     
/*  94 */     if (attackTimerTicks == 2 || attackTimerTicks == 3) {
/*     */       
/*  96 */       int fakieChance = ctx.config.fakieChance();
/*  97 */       if (fakieChance > 0 && fakieChance <= 100) {
/*     */         
/*  99 */         int roll = this.random.nextInt(100);
/* 100 */         if (roll < fakieChance) {
/*     */ 
/*     */           
/* 103 */           String itemToEquip = this.random.nextBoolean() ? "Abyssal tentacle" : "Dragon crossbow";
/* 104 */           boolean equipped = ctx.helpers.equipItemByName(itemToEquip);
/* 105 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 107 */             Logger.norm("[Both Frozen 8-9] Fakie triggered (" + fakieChance + "% chance, roll: " + roll + ") - Attempting to equip: " + itemToEquip + " - Result: " + (equipped ? "Success" : "Failed (not in inventory or already equipped)"));
/*     */           }
/*     */ 
/*     */           
/* 111 */           if (equipped && itemToEquip.toLowerCase().contains("crossbow"))
/*     */           {
/* 113 */             CombatAPI.setAttackStyle(AttackStyle.FOURTH);
/* 114 */             if (ctx.config.logHandlerActions())
/*     */             {
/* 116 */               Logger.norm("[Both Frozen 8-9] Fakie equipped crossbow - ensuring longrange is set");
/*     */             }
/*     */           }
/*     */         
/* 120 */         } else if (ctx.config.logHandlerActions()) {
/*     */           
/* 122 */           Logger.norm("[Both Frozen 8-9] Fakie roll failed (" + fakieChance + "% chance, roll: " + roll + ")");
/*     */         } 
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 128 */     boolean attacksSynced = (attackTimerTicks == opponentTimerTicks);
/*     */ 
/*     */ 
/*     */     
/* 132 */     if (attackTimerTicks > 1 && attackTimerTicks <= 5) {
/*     */ 
/*     */       
/* 135 */       boolean karilEquipped = ctx.helpers.equipItemByName("Karil's leathertop*");
/* 136 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 138 */         Logger.norm("[Both Frozen 8-9] Recent attack at distance " + distance + " - equipping only Karil's leathertop (result: " + karilEquipped + ")");
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 143 */     int health = ctx.helpers.getPlayerHealth();
/* 144 */     int magicLevel = ctx.helpers.getPlayerMagicLevel();
/* 145 */     int prayerPoints = ctx.helpers.getPlayerPrayerPoints();
/* 146 */     boolean hasBrews = ctx.helpers.hasBrews();
/*     */ 
/*     */     
/* 149 */     int oppTimerTicks = (ctx.opponentTimerTicks < 0) ? 0 : ctx.opponentTimerTicks;
/* 150 */     if (oppTimerTicks > 1 && health < 45 && !hasBrews) {
/*     */ 
/*     */       
/* 153 */       int anglerCount = 0;
/* 154 */       for (ItemEx item : InventoryAPI.getItems()) {
/*     */         
/* 156 */         if (item != null && item.getName() != null && item.getName().toLowerCase().contains("angler"))
/*     */         {
/* 158 */           anglerCount += item.getQuantity();
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/* 163 */       if (anglerCount == 0)
/*     */       {
/* 165 */         if (!PrayerAPI.REDEMPTION.isActive()) {
/*     */           
/* 167 */           PrayerAPI.REDEMPTION.turnOn();
/* 168 */           if (ctx.config.aiPrayersLogging())
/*     */           {
/* 170 */             Logger.norm("[Both Frozen 8-9] Redemption activated - HP: " + health + ", No food (anglers: 0, brews: 0), Opponent timer: " + oppTimerTicks);
/*     */           }
/*     */         } 
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 177 */     if (prayerPoints < 25)
/*     */     {
/* 179 */       if (ctx.helpers.drinkSanfewSerum())
/*     */       {
/* 181 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 183 */           Logger.norm("[Both Frozen 8-9] Prayer < 25 (" + prayerPoints + "), drank Sanfew serum");
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 190 */     if (!hasBrews && health < 55)
/*     */     {
/* 192 */       if (ctx.helpers.eatAngler())
/*     */       {
/* 194 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 196 */           Logger.norm("[Both Frozen 8-9] Out of brews and HP < 55 (" + health + "), eating angler");
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 203 */     if (health < 35) {
/*     */ 
/*     */       
/* 206 */       if (!this.needToBrewAfterLowHPAngler)
/*     */       {
/* 208 */         if (ctx.helpers.eatAngler())
/*     */         {
/* 210 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 212 */             Logger.norm("[Both Frozen 8-9] HP < 35 (" + health + "), eating anglerfish first, will brew on next tick");
/*     */           }
/* 214 */           this.needToBrewAfterLowHPAngler = true;
/* 215 */           this.ticksSinceLowHPAngler = 0;
/*     */ 
/*     */         
/*     */         }
/*     */ 
/*     */       
/*     */       }
/* 222 */       else if (ctx.helpers.sipSaradominBrew())
/*     */       {
/* 224 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 226 */           Logger.norm("[Both Frozen 8-9] Drank brew after low HP anglerfish (next tick)");
/*     */         }
/* 228 */         this.needToBrewAfterLowHPAngler = false;
/* 229 */         this.ticksSinceLowHPAngler = 0;
/*     */       
/*     */       }
/*     */       else
/*     */       {
/*     */         
/* 235 */         this.needToBrewAfterLowHPAngler = false;
/* 236 */         this.ticksSinceLowHPAngler = 0;
/*     */ 
/*     */       
/*     */       }
/*     */ 
/*     */     
/*     */     }
/* 243 */     else if (this.needToBrewAfterLowHPAngler) {
/*     */       
/* 245 */       this.needToBrewAfterLowHPAngler = false;
/* 246 */       this.ticksSinceLowHPAngler = 0;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 251 */     if (health >= 95 && magicLevel < 94)
/*     */     {
/* 253 */       if (ctx.helpers.drinkSanfewSerum()) {
/*     */         
/* 255 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 257 */           Logger.norm("[Both Frozen 8-9] HP >= 95 (" + health + ") but magic < 94 (" + magicLevel + "), drank Sanfew serum immediately");
/*     */ 
/*     */         
/*     */         }
/*     */       
/*     */       }
/* 263 */       else if (ctx.config.logHandlerActions()) {
/*     */         
/* 265 */         Logger.norm("[Both Frozen 8-9] HP >= 95 (" + health + ") but magic < 94 (" + magicLevel + ") - no Sanfew serum found");
/*     */       } 
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 271 */     if (attackTimerTicks > 3)
/*     */     {
/*     */       
/* 274 */       if (health < 60)
/*     */       {
/* 276 */         if (ctx.helpers.sipSaradominBrew()) {
/*     */ 
/*     */ 
/*     */           
/* 280 */           if (ctx.config.logHandlerActions())
/*     */           {
/* 282 */             Logger.norm("[Both Frozen 8-9] HP below 60 (" + health + "), drank Saradomin brew");
/*     */ 
/*     */           
/*     */           }
/*     */         
/*     */         }
/* 288 */         else if (ctx.config.logHandlerActions()) {
/*     */           
/* 290 */           Logger.norm("[Both Frozen 8-9] HP below 60 (" + health + ") but no Saradomin brew found");
/*     */         } 
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 297 */     boolean targetCanMeleeUs = AttackMethods.canTargetMeleeUs(ctx, ctx.opponent);
/*     */     
/* 299 */     if (ctx.config.combatAutomationLogging())
/*     */     {
/* 301 */       Logger.norm("[Both Frozen 8-9] Target can melee us: " + targetCanMeleeUs + ", playerTimer: " + attackTimerTicks + ", oppTimer: " + opponentTimerTicks + ", synced: " + attacksSynced + ", distance: " + distance);
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 311 */     if (attackTimerTicks <= 1 || (attackTimerTicks == 0 && ctx.ticksAtZero >= 3)) {
/*     */ 
/*     */       
/* 314 */       int roll = this.random.nextInt(100);
/*     */       
/* 316 */       if (roll < 50) {
/*     */ 
/*     */         
/* 319 */         AttackMethods.executeMagicAttack(ctx, ctx.opponent, "[Both Frozen 8-9]", true, false, true, false);
/* 320 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 322 */           Logger.norm("[Both Frozen 8-9] Distance " + distance + " - casting blood spell");
/*     */         
/*     */         }
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 329 */         ctx.helpers.equipItemByName("Dragon crossbow");
/* 330 */         ctx.helpers.equipItemByName("crossbow");
/*     */ 
/*     */         
/* 333 */         CombatAPI.setAttackStyle(AttackStyle.FOURTH);
/*     */         
/* 335 */         AttackMethods.executeRangeAttack(ctx, ctx.opponent, "[Both Frozen 8-9]");
/* 336 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 338 */           Logger.norm("[Both Frozen 8-9] Distance " + distance + " - range attack with longrange crossbow");
/*     */         }
/*     */       } 
/* 341 */       return true;
/*     */     } 
/*     */ 
/*     */     
/* 345 */     ctx.helpers.equipGearFromConfig(ctx.config.tankGear());
/* 346 */     return true;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/BothFrozen89Handler.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */