/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import com.tonic.api.game.CombatAPI;
/*     */ import com.tonic.api.widgets.PrayerAPI;
/*     */ import com.tonic.data.magic.spellbooks.Ancient;
/*     */ import java.util.Objects;
/*     */ import java.util.Random;
/*     */ import net.runelite.api.Player;
/*     */ import net.runelite.api.coords.WorldPoint;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class AttackMethods
/*     */ {
/*     */   public static boolean executeMeleeAttack(CombatStateContext ctx, Player opponent, String logPrefix) {
/*  29 */     Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/*  30 */     Objects.requireNonNull(opponent); WorldPoint opponentPos = (WorldPoint)Static.invoke(opponent::getWorldLocation);
/*  31 */     int dx = Math.abs(playerPos.getX() - opponentPos.getX());
/*  32 */     int dy = Math.abs(playerPos.getY() - opponentPos.getY());
/*  33 */     int distance = Math.max(dx, dy);
/*  34 */     boolean canMelee = (distance <= 1);
/*     */     
/*  36 */     if (!canMelee) {
/*     */       
/*  38 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/*  40 */         Logger.norm(logPrefix + " Cannot melee - distance too far: " + logPrefix + " tiles");
/*     */       }
/*  42 */       return false;
/*     */     } 
/*     */ 
/*     */     
/*  46 */     int specEnergy = CombatAPI.getSpecEnergy();
/*  47 */     if (specEnergy >= 50) {
/*     */       
/*  49 */       Random random = new Random();
/*  50 */       int specRoll = random.nextInt(100);
/*  51 */       if (specRoll < 10)
/*     */       {
/*     */         
/*  54 */         if (executeSpecialAttack(ctx, opponent, logPrefix)) {
/*     */           
/*  56 */           if (ctx.config.combatAutomationLogging())
/*     */           {
/*  58 */             Logger.norm(logPrefix + " Melee attack rolled special attack (10% chance, spec energy: " + logPrefix + "%)");
/*     */           }
/*  60 */           return true;
/*     */         } 
/*     */       }
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/*  67 */     ctx.helpers.setMindAction("Melee Attack");
/*  68 */     ctx.helpers.equipGearFromConfig(ctx.config.meleeGear(), PrayerAPI.PIETY);
/*  69 */     ctx.helpers.attackPlayerWithFightOption(opponent);
/*     */     
/*  71 */     if (ctx.config.combatAutomationLogging())
/*     */     {
/*  73 */       Logger.norm(logPrefix + " Executing melee attack with Piety");
/*     */     }
/*     */     
/*  76 */     return true;
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
/*     */   
/*     */   public static boolean executeRangeAttack(CombatStateContext ctx, Player opponent, String logPrefix) {
/*  89 */     ctx.helpers.setMindAction("Range Attack");
/*  90 */     ctx.helpers.equipGearFromConfig(ctx.config.rangedGear(), PrayerAPI.RIGOUR);
/*  91 */     ctx.helpers.attackPlayerWithFightOption(opponent);
/*     */     
/*  93 */     if (ctx.config.combatAutomationLogging())
/*     */     {
/*  95 */       Logger.norm(logPrefix + " Executing range attack with Rigour");
/*     */     }
/*     */     
/*  98 */     return true;
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
/*     */   
/*     */   public static boolean executeMagicAttack(CombatStateContext ctx, Player opponent, String logPrefix, boolean fallbackToRange) {
/* 111 */     return executeMagicAttack(ctx, opponent, logPrefix, fallbackToRange, false, false, false);
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
/*     */ 
/*     */   
/*     */   public static boolean executeMagicAttack(CombatStateContext ctx, Player opponent, String logPrefix, boolean fallbackToRange, boolean useTankMage) {
/* 125 */     return executeMagicAttack(ctx, opponent, logPrefix, fallbackToRange, useTankMage, false, false);
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static boolean executeMagicAttack(CombatStateContext ctx, Player opponent, String logPrefix, boolean fallbackToRange, boolean useTankMage, boolean useBloodSpell, boolean equipTankGearAfter) {
/* 142 */     int magicLevel = ctx.helpers.getPlayerMagicLevel();
/*     */     
/* 144 */     if (magicLevel < 82) {
/*     */ 
/*     */       
/* 147 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 149 */         Logger.norm(logPrefix + " Magic level too low (" + logPrefix + " < 82) - falling back to range attack");
/*     */       }
/*     */       
/* 152 */       if (fallbackToRange)
/*     */       {
/* 154 */         return executeRangeAttack(ctx, opponent, logPrefix);
/*     */       }
/*     */       
/* 157 */       return false;
/*     */     } 
/*     */ 
/*     */     
/* 161 */     boolean canCast = false;
/* 162 */     if (useBloodSpell) {
/*     */ 
/*     */       
/* 165 */       canCast = (Ancient.BLOOD_BARRAGE.canCast() || Ancient.BLOOD_BLITZ.canCast());
/*     */     
/*     */     }
/*     */     else {
/*     */       
/* 170 */       canCast = Ancient.ICE_BARRAGE.canCast();
/*     */     } 
/*     */     
/* 173 */     if (!canCast) {
/*     */       
/* 175 */       if (ctx.config.combatAutomationLogging()) {
/*     */         
/* 177 */         String str = useBloodSpell ? "blood spell" : "ice barrage";
/* 178 */         Logger.norm(logPrefix + " Cannot cast " + logPrefix + " - missing runes or wrong spellbook");
/*     */       } 
/*     */       
/* 181 */       if (fallbackToRange) {
/*     */ 
/*     */         
/* 184 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 186 */           Logger.norm(logPrefix + " Falling back to range attack instead");
/*     */         }
/* 188 */         return executeRangeAttack(ctx, opponent, logPrefix);
/*     */       } 
/*     */       
/* 191 */       return false;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 196 */     String spellType = useBloodSpell ? "Blood spell" : "Ice Barrage";
/* 197 */     ctx.helpers.setMindAction("Magic Attack (" + spellType + ")");
/*     */     
/* 199 */     if (useTankMage) {
/*     */       
/* 201 */       ctx.helpers.equipGearFromConfig(ctx.config.tankMageGear(), PrayerAPI.AUGURY);
/* 202 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 204 */         Logger.norm(logPrefix + " Executing magic attack with tank mage gear and Augury (" + logPrefix + ")");
/*     */       }
/*     */     }
/*     */     else {
/*     */       
/* 209 */       ctx.helpers.equipGearFromConfig(ctx.config.magicGear(), PrayerAPI.AUGURY);
/* 210 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 212 */         Logger.norm(logPrefix + " Executing magic attack with Augury (" + logPrefix + ")");
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 217 */     if (useBloodSpell) {
/*     */       
/* 219 */       boolean bloodSpellCast = ctx.helpers.castBloodSpell(opponent);
/* 220 */       if (!bloodSpellCast)
/*     */       {
/*     */         
/* 223 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 225 */           Logger.norm(logPrefix + " Blood spell cast failed - falling back to range attack");
/*     */         }
/*     */         
/* 228 */         if (fallbackToRange)
/*     */         {
/* 230 */           return executeRangeAttack(ctx, opponent, logPrefix);
/*     */         }
/*     */         
/* 233 */         return false;
/*     */       }
/*     */     
/*     */     } else {
/*     */       
/* 238 */       ctx.helpers.castIceBarrage(opponent);
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 245 */     return true;
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
/*     */   public static boolean executeMagicAttack(CombatStateContext ctx, Player opponent, String logPrefix) {
/* 257 */     return executeMagicAttack(ctx, opponent, logPrefix, true, false, false, false);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static boolean canMeleeAttack(CombatStateContext ctx, Player opponent) {
/* 268 */     Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/* 269 */     Objects.requireNonNull(opponent); WorldPoint opponentPos = (WorldPoint)Static.invoke(opponent::getWorldLocation);
/* 270 */     int dx = Math.abs(playerPos.getX() - opponentPos.getX());
/* 271 */     int dy = Math.abs(playerPos.getY() - opponentPos.getY());
/* 272 */     int distance = Math.max(dx, dy);
/* 273 */     return (distance <= 1);
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
/*     */   
/*     */   public static boolean canWeMelee(CombatStateContext ctx, Player opponent) {
/* 286 */     boolean weFrozen = ctx.weFrozen;
/*     */ 
/*     */     
/* 289 */     int distance = getDistance(ctx, opponent);
/*     */ 
/*     */     
/* 292 */     return (!weFrozen && distance < 4);
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
/*     */ 
/*     */   
/*     */   public static boolean executeSpecialAttack(CombatStateContext ctx, Player opponent, String logPrefix) {
/* 306 */     int strengthLevel = ctx.helpers.getPlayerStrengthLevel();
/* 307 */     if (strengthLevel < 99) {
/*     */       
/* 309 */       if (ctx.config.logHandlerActions())
/*     */       {
/* 311 */         Logger.norm(logPrefix + " Cannot execute special attack - strength level: " + logPrefix + " (need at least 99)");
/*     */       }
/* 313 */       return false;
/*     */     } 
/*     */ 
/*     */     
/* 317 */     int specEnergy = CombatAPI.getSpecEnergy();
/* 318 */     if (specEnergy < 50) {
/*     */       
/* 320 */       if (ctx.config.logHandlerActions())
/*     */       {
/* 322 */         Logger.norm(logPrefix + " Cannot execute special attack - spec energy: " + logPrefix + "% (need at least 50%)");
/*     */       }
/* 324 */       return false;
/*     */     } 
/*     */ 
/*     */     
/* 328 */     int eatCooldownTicks = ctx.helpers.getEatCooldownTicks();
/* 329 */     if (strengthLevel < 110 && eatCooldownTicks <= 1) {
/*     */ 
/*     */       
/* 332 */       String str = ctx.config.specLoadout();
/* 333 */       if (str == null || str.trim().isEmpty()) {
/*     */         
/* 335 */         if (ctx.config.logHandlerActions())
/*     */         {
/* 337 */           Logger.norm(logPrefix + " Cannot execute special attack - spec loadout not configured (required for strength < 110)");
/*     */         }
/* 339 */         return false;
/*     */       } 
/*     */ 
/*     */       
/* 343 */       boolean superCombatDrank = ctx.helpers.drinkSuperCombat();
/* 344 */       if (!superCombatDrank)
/*     */       {
/* 346 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 348 */           Logger.norm(logPrefix + " Super combat potion not found in inventory, but proceeding with special attack anyway (strength: " + logPrefix + " < 110, spec energy: " + strengthLevel + "%)");
/*     */         }
/*     */       }
/*     */ 
/*     */ 
/*     */       
/* 354 */       ctx.helpers.equipGearFromConfig(str, PrayerAPI.PIETY);
/*     */       
/* 356 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 358 */         if (superCombatDrank) {
/*     */           
/* 360 */           Logger.norm(logPrefix + " Drank Super combat potion and equipped spec gear loadout (strength: " + logPrefix + " < 110, eat cooldown: " + strengthLevel + ")");
/*     */         }
/*     */         else {
/*     */           
/* 364 */           Logger.norm(logPrefix + " Equipped spec gear loadout without super combat (strength: " + logPrefix + " < 110, eat cooldown: " + strengthLevel + ", spec energy: " + eatCooldownTicks + "%)");
/*     */         } 
/*     */       }
/*     */ 
/*     */       
/* 369 */       if (!CombatAPI.isSpecEnabled()) {
/*     */         
/* 371 */         CombatAPI.toggleSpec();
/* 372 */         if (ctx.config.combatAutomationLogging())
/*     */         {
/* 374 */           Logger.norm(logPrefix + " Toggled special attack bar on (same tick as spec gear" + logPrefix + ")");
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/* 379 */       ctx.helpers.attackPlayerWithFightOption(opponent);
/*     */       
/* 381 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 383 */         Logger.norm(logPrefix + " Executing special attack (spec energy: " + logPrefix + "%, strength: " + specEnergy + ", eat cooldown: " + strengthLevel + eatCooldownTicks + ")");
/*     */       }
/* 385 */       return true;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 390 */     String specGearConfig = ctx.config.specLoadout();
/* 391 */     if (specGearConfig == null || specGearConfig.trim().isEmpty()) {
/*     */       
/* 393 */       if (ctx.config.logHandlerActions())
/*     */       {
/* 395 */         Logger.norm(logPrefix + " Cannot execute special attack - spec loadout not configured");
/*     */       }
/* 397 */       return false;
/*     */     } 
/*     */ 
/*     */     
/* 401 */     ctx.helpers.equipGearFromConfig(specGearConfig, PrayerAPI.PIETY);
/*     */ 
/*     */     
/* 404 */     if (!CombatAPI.isSpecEnabled()) {
/*     */       
/* 406 */       CombatAPI.toggleSpec();
/* 407 */       if (ctx.config.combatAutomationLogging())
/*     */       {
/* 409 */         Logger.norm(logPrefix + " Toggled special attack bar on");
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 414 */     ctx.helpers.attackPlayerWithFightOption(opponent);
/*     */     
/* 416 */     if (ctx.config.combatAutomationLogging())
/*     */     {
/* 418 */       Logger.norm(logPrefix + " Executing special attack (spec energy: " + logPrefix + "%, strength: " + specEnergy + ")");
/*     */     }
/* 420 */     return true;
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
/*     */   public static int getDistance(CombatStateContext ctx, Player opponent) {
/* 432 */     Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/* 433 */     Objects.requireNonNull(opponent); WorldPoint opponentPos = (WorldPoint)Static.invoke(opponent::getWorldLocation);
/*     */ 
/*     */     
/* 436 */     if (playerPos.getPlane() != opponentPos.getPlane())
/*     */     {
/* 438 */       return Integer.MAX_VALUE;
/*     */     }
/*     */     
/* 441 */     int dx = Math.abs(playerPos.getX() - opponentPos.getX());
/* 442 */     int dy = Math.abs(playerPos.getY() - opponentPos.getY());
/* 443 */     return Math.max(dx, dy);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static boolean canTargetMeleeUs(CombatStateContext ctx, Player opponent) {
/* 454 */     Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/* 455 */     Objects.requireNonNull(opponent); WorldPoint opponentPos = (WorldPoint)Static.invoke(opponent::getWorldLocation);
/*     */     
/* 457 */     int dx = Math.abs(playerPos.getX() - opponentPos.getX());
/* 458 */     int dy = Math.abs(playerPos.getY() - opponentPos.getY());
/* 459 */     int distance = Math.max(dx, dy);
/*     */ 
/*     */     
/* 462 */     return (distance == 1);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static boolean isTargetDiagonal(CombatStateContext ctx, Player opponent) {
/* 473 */     Objects.requireNonNull(ctx.localPlayer); WorldPoint playerPos = (WorldPoint)Static.invoke(ctx.localPlayer::getWorldLocation);
/* 474 */     Objects.requireNonNull(opponent); WorldPoint opponentPos = (WorldPoint)Static.invoke(opponent::getWorldLocation);
/*     */     
/* 476 */     int dx = Math.abs(playerPos.getX() - opponentPos.getX());
/* 477 */     int dy = Math.abs(playerPos.getY() - opponentPos.getY());
/*     */     
/* 479 */     return (dx > 0 && dy > 0);
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/AttackMethods.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */