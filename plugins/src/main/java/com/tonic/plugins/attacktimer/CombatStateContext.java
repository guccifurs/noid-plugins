/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.api.widgets.PrayerAPI;
/*     */ import net.runelite.api.Player;
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
/*     */ public class CombatStateContext
/*     */ {
/*     */   public final AttackTimerConfig config;
/*     */   public final Player localPlayer;
/*     */   public final Player opponent;
/*     */   public final int playerTimerTicks;
/*     */   public final int opponentTimerTicks;
/*     */   public final int ticksAtZero;
/*     */   public final boolean weFrozen;
/*     */   public final boolean targetFrozen;
/*     */   public final int playerFreezeTimerTicks;
/*     */   public final int opponentFreezeTimerTicks;
/*     */   public final int opponentFreezeImmunityTicks;
/*     */   public final PidStatus currentPidStatus;
/*     */   public final int opponentSpecPercent;
/*     */   public final int opponentRecentMeleeTicks;
/*     */   public final MutableInt lastEquipTick;
/*     */   public final HelperMethods helpers;
/*     */   
/*     */   public CombatStateContext(AttackTimerConfig config, Player localPlayer, Player opponent, int playerTimerTicks, int opponentTimerTicks, boolean weFrozen, boolean targetFrozen, int playerFreezeTimerTicks, int opponentFreezeTimerTicks, int opponentFreezeImmunityTicks, PidStatus currentPidStatus, int opponentSpecPercent, int opponentRecentMeleeTicks, MutableInt lastEquipTick, HelperMethods helpers) {
/*  95 */     this(config, localPlayer, opponent, playerTimerTicks, opponentTimerTicks, weFrozen, targetFrozen, playerFreezeTimerTicks, opponentFreezeTimerTicks, opponentFreezeImmunityTicks, currentPidStatus, opponentSpecPercent, opponentRecentMeleeTicks, lastEquipTick, helpers, 0);
/*     */   }
/*     */   
/*     */   // Simplified constructor that matches backup code pattern
/*     */   public CombatStateContext(AttackTimerConfig config, Player localPlayer, Player opponent, int playerTimerTicks, int opponentTimerTicks, boolean weFrozen, boolean targetFrozen, int playerFreezeTimerTicks, int opponentFreezeTimerTicks, PidStatus currentPidStatus, MutableBoolean waitingCastAnim, MutableBoolean waitingCastXP, MutableBoolean waitingCrossbow, MutableInt waitingCrossbowTicks, MutableInt lastEquipTick, HelperMethods helpers) {
/*     */     this.config = config;
/*     */     this.localPlayer = localPlayer;
/*     */     this.opponent = opponent;
/*     */     this.playerTimerTicks = playerTimerTicks;
/*     */     this.opponentTimerTicks = opponentTimerTicks;
/*     */     this.ticksAtZero = 0;
/*     */     this.weFrozen = weFrozen;
/*     */     this.targetFrozen = targetFrozen;
/*     */     this.playerFreezeTimerTicks = playerFreezeTimerTicks;
/*     */     this.opponentFreezeTimerTicks = opponentFreezeTimerTicks;
/*     */     this.opponentFreezeImmunityTicks = 0;
/*     */     this.currentPidStatus = currentPidStatus;
/*     */     this.opponentSpecPercent = 0;
/*     */     this.opponentRecentMeleeTicks = 0;
/*     */     this.lastEquipTick = lastEquipTick;
/*     */     this.helpers = helpers;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public CombatStateContext(AttackTimerConfig config, Player localPlayer, Player opponent, int playerTimerTicks, int opponentTimerTicks, boolean weFrozen, boolean targetFrozen, int playerFreezeTimerTicks, int opponentFreezeTimerTicks, int opponentFreezeImmunityTicks, PidStatus currentPidStatus, int opponentSpecPercent, int opponentRecentMeleeTicks, MutableInt lastEquipTick, HelperMethods helpers, int ticksAtZero) {
/* 118 */     this.config = config;
/* 119 */     this.localPlayer = localPlayer;
/* 120 */     this.opponent = opponent;
/* 121 */     this.playerTimerTicks = playerTimerTicks;
/* 122 */     this.opponentTimerTicks = opponentTimerTicks;
/* 123 */     this.ticksAtZero = ticksAtZero;
/* 124 */     this.weFrozen = weFrozen;
/* 125 */     this.targetFrozen = targetFrozen;
/* 126 */     this.playerFreezeTimerTicks = playerFreezeTimerTicks;
/* 127 */     this.opponentFreezeTimerTicks = opponentFreezeTimerTicks;
/* 128 */     this.opponentFreezeImmunityTicks = opponentFreezeImmunityTicks;
/* 129 */     this.currentPidStatus = currentPidStatus;
/* 130 */     this.opponentSpecPercent = opponentSpecPercent;
/* 131 */     this.opponentRecentMeleeTicks = opponentRecentMeleeTicks;
/* 132 */     this.lastEquipTick = lastEquipTick;
/* 133 */     this.helpers = helpers;
/*     */   } public static interface HelperMethods {
/*     */     void equipGearFromConfig(String param1String); void equipGearFromConfig(String param1String, PrayerAPI param1PrayerAPI); void castIceBarrage(Player param1Player); boolean castBloodSpell(Player param1Player); boolean castBloodBlitz(Player param1Player); boolean castBloodBarrage(Player param1Player); void attackPlayerWithFightOption(Player param1Player); void clickUnderTarget(Player param1Player); int getPlayerHealth(); int getPlayerMagicLevel(); int getPlayerRangeLevel(); int getPlayerStrengthLevel(); int getPlayerPrayerPoints(); int getEatCooldownTicks(); boolean hasBrews(); boolean eatAngler(); boolean sipSaradominBrew(); boolean drinkSanfewSerum(); boolean drinkSuperCombat(); boolean drinkBastion();
/*     */     boolean isUnderTarget(Player param1Player);
/*     */     void walkAwayFromTarget(Player param1Player, int param1Int);
/*     */     boolean isTargetDead(Player param1Player);
/*     */     boolean equipItemByName(String param1String);
/*     */     int getTargetHPPercentage(Player param1Player);
/*     */     void setMindAction(String param1String); }
/*     */   public static class MutableInt { public MutableInt(int value) {
/* 143 */       this.value = value;
/*     */     }
/*     */     
/*     */     public int value; }
/*     */   
/*     */   public static class MutableBoolean { public MutableBoolean(boolean value) {
/*     */       this.value = value;
/*     */     }
/*     */     
/*     */     public boolean value; }
/*     */   
/*     */   public enum PidStatus {
/* 149 */     ON_PID,
/* 150 */     OFF_PID,
/* 151 */     UNKNOWN;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/CombatStateContext.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */