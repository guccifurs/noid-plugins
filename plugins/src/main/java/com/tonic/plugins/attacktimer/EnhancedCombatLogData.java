/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import java.util.ArrayList;
/*     */ import java.util.HashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class EnhancedCombatLogData
/*     */ {
/*     */   public void setBothFrozen(CombatAttackStats bothFrozen) {
/*  15 */     this.bothFrozen = bothFrozen; } public void setBothUnfrozen(CombatAttackStats bothUnfrozen) { this.bothUnfrozen = bothUnfrozen; } public void setTargetFrozenWeUnfrozen(CombatAttackStats targetFrozenWeUnfrozen) { this.targetFrozenWeUnfrozen = targetFrozenWeUnfrozen; } public void setWeFrozenTargetUnfrozen(CombatAttackStats weFrozenTargetUnfrozen) { this.weFrozenTargetUnfrozen = weFrozenTargetUnfrozen; } public void setExtendedAttackHistory(List<ExtendedAttackContext> extendedAttackHistory) { this.extendedAttackHistory = extendedAttackHistory; } public void setAttackHistory(List<AttackContext> attackHistory) { this.attackHistory = attackHistory; } public void setPatternFrequency(Map<String, Map<String, Integer>> patternFrequency) { this.patternFrequency = patternFrequency; } public void setFights(List<FightMetadata> fights) { this.fights = fights; } public void setCurrentFight(FightMetadata currentFight) { this.currentFight = currentFight; }
/*     */ 
/*     */ 
/*     */   
/*  19 */   private CombatAttackStats bothFrozen = new CombatAttackStats(); public CombatAttackStats getBothFrozen() { return this.bothFrozen; }
/*  20 */    private CombatAttackStats bothUnfrozen = new CombatAttackStats(); public CombatAttackStats getBothUnfrozen() { return this.bothUnfrozen; }
/*  21 */    private CombatAttackStats targetFrozenWeUnfrozen = new CombatAttackStats(); public CombatAttackStats getTargetFrozenWeUnfrozen() { return this.targetFrozenWeUnfrozen; }
/*  22 */    private CombatAttackStats weFrozenTargetUnfrozen = new CombatAttackStats(); public CombatAttackStats getWeFrozenTargetUnfrozen() { return this.weFrozenTargetUnfrozen; }
/*     */ 
/*     */   
/*  25 */   private List<ExtendedAttackContext> extendedAttackHistory = new ArrayList<>(); public List<ExtendedAttackContext> getExtendedAttackHistory() { return this.extendedAttackHistory; }
/*     */ 
/*     */   
/*  28 */   private List<AttackContext> attackHistory = new ArrayList<>(); public List<AttackContext> getAttackHistory() { return this.attackHistory; }
/*     */ 
/*     */   
/*  31 */   private Map<String, Map<String, Integer>> patternFrequency = new HashMap<>(); public Map<String, Map<String, Integer>> getPatternFrequency() { return this.patternFrequency; }
/*     */ 
/*     */   
/*  34 */   private Map<String, Map<String, Map<String, CombatAttackStats>>> freezeStateDistanceTimerStats = new HashMap<>(); public Map<String, Map<String, Map<String, CombatAttackStats>>> getFreezeStateDistanceTimerStats() { return this.freezeStateDistanceTimerStats; }
/*     */ 
/*     */   
/*  37 */   private List<FightMetadata> fights = new ArrayList<>(); public List<FightMetadata> getFights() { return this.fights; }
/*     */ 
/*     */   
/*  40 */   private FightMetadata currentFight = null; public FightMetadata getCurrentFight() { return this.currentFight; }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void addExtendedAttackToHistory(ExtendedAttackContext context, int maxHistorySize) {
/*  44 */     if (context == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/*  49 */     this.extendedAttackHistory.add(context);
/*     */ 
/*     */     
/*  52 */     while (this.extendedAttackHistory.size() > maxHistorySize)
/*     */     {
/*  54 */       this.extendedAttackHistory.remove(0);
/*     */     }
/*     */ 
/*     */     
/*  58 */     this.attackHistory.add(context.toBaseContext());
/*  59 */     while (this.attackHistory.size() > maxHistorySize)
/*     */     {
/*  61 */       this.attackHistory.remove(0);
/*     */     }
/*     */ 
/*     */     
/*  65 */     if (this.extendedAttackHistory.size() >= 2) {
/*     */       
/*  67 */       ExtendedAttackContext previousContext = this.extendedAttackHistory.get(this.extendedAttackHistory.size() - 2);
/*  68 */       String contextKey = previousContext.generateContextKey();
/*  69 */       String nextAttack = context.getAttackStyle();
/*     */       
/*  71 */       this.patternFrequency.computeIfAbsent(contextKey, k -> new HashMap<>());
/*  72 */       Map<String, Integer> nextAttackCounts = this.patternFrequency.get(contextKey);
/*  73 */       nextAttackCounts.put(nextAttack, Integer.valueOf(((Integer)nextAttackCounts.getOrDefault(nextAttack, Integer.valueOf(0))).intValue() + 1));
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public List<ExtendedAttackContext> getRecentExtendedHistory(int count) {
/*  82 */     if (this.extendedAttackHistory.isEmpty())
/*     */     {
/*  84 */       return new ArrayList<>();
/*     */     }
/*     */     
/*  87 */     int startIndex = Math.max(0, this.extendedAttackHistory.size() - count);
/*  88 */     return new ArrayList<>(this.extendedAttackHistory.subList(startIndex, this.extendedAttackHistory.size()));
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void startFight(long startTick, String opponentRSN, String playerRSN) {
/*  96 */     this.currentFight = new FightMetadata();
/*  97 */     this.currentFight.setFightId(generateFightId());
/*  98 */     this.currentFight.setStartTick(startTick);
/*  99 */     this.currentFight.setOpponentRSN(opponentRSN);
/* 100 */     this.currentFight.setPlayerRSN(playerRSN);
/* 101 */     this.currentFight.setFightPhase("EARLY");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void endFight(long endTick, String winner, int playerKillCount, int playerDeathCount, int opponentKillCount, int opponentDeathCount) {
/* 110 */     if (this.currentFight == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 115 */     this.currentFight.setEndTick(endTick);
/* 116 */     this.currentFight.setDurationTicks(endTick - this.currentFight.getStartTick());
/* 117 */     this.currentFight.setWinner(winner);
/* 118 */     this.currentFight.setPlayerKillCount(playerKillCount);
/* 119 */     this.currentFight.setPlayerDeathCount(playerDeathCount);
/* 120 */     this.currentFight.setOpponentKillCount(opponentKillCount);
/* 121 */     this.currentFight.setOpponentDeathCount(opponentDeathCount);
/*     */     
/* 123 */     this.fights.add(this.currentFight);
/*     */ 
/*     */     
/* 126 */     while (this.fights.size() > 100)
/*     */     {
/* 128 */       this.fights.remove(0);
/*     */     }
/*     */     
/* 131 */     this.currentFight = null;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private String generateFightId() {
/* 139 */     return "fight_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000.0D);
/*     */   }
/*     */   public static class FightMetadata {
/*     */     private String fightId; private long startTick; private long endTick; private long durationTicks;
/*     */     private String opponentRSN;
/*     */     private String playerRSN;
/*     */     
/* 146 */     public void setFightId(String fightId) { this.fightId = fightId; } private String winner; private int playerKillCount; private int playerDeathCount; private int opponentKillCount; private int opponentDeathCount; private String fightPhase; public void setStartTick(long startTick) { this.startTick = startTick; } public void setEndTick(long endTick) { this.endTick = endTick; } public void setDurationTicks(long durationTicks) { this.durationTicks = durationTicks; } public void setOpponentRSN(String opponentRSN) { this.opponentRSN = opponentRSN; } public void setPlayerRSN(String playerRSN) { this.playerRSN = playerRSN; } public void setWinner(String winner) { this.winner = winner; } public void setPlayerKillCount(int playerKillCount) { this.playerKillCount = playerKillCount; } public void setPlayerDeathCount(int playerDeathCount) { this.playerDeathCount = playerDeathCount; } public void setOpponentKillCount(int opponentKillCount) { this.opponentKillCount = opponentKillCount; } public void setOpponentDeathCount(int opponentDeathCount) { this.opponentDeathCount = opponentDeathCount; } public void setFightPhase(String fightPhase) { this.fightPhase = fightPhase; }
/*     */ 
/*     */     
/* 149 */     public String getFightId() { return this.fightId; }
/* 150 */     public long getStartTick() { return this.startTick; }
/* 151 */     public long getEndTick() { return this.endTick; }
/* 152 */     public long getDurationTicks() { return this.durationTicks; }
/* 153 */     public String getOpponentRSN() { return this.opponentRSN; }
/* 154 */     public String getPlayerRSN() { return this.playerRSN; }
/* 155 */     public String getWinner() { return this.winner; }
/* 156 */     public int getPlayerKillCount() { return this.playerKillCount; }
/* 157 */     public int getPlayerDeathCount() { return this.playerDeathCount; }
/* 158 */     public int getOpponentKillCount() { return this.opponentKillCount; }
/* 159 */     public int getOpponentDeathCount() { return this.opponentDeathCount; } public String getFightPhase() {
/* 160 */       return this.fightPhase;
/*     */     }
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/EnhancedCombatLogData.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */