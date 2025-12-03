/*    */ package com.tonic.plugins.attacktimer;
/*    */ 
/*    */ import java.util.ArrayList;
/*    */ import java.util.HashMap;
/*    */ import java.util.List;
/*    */ import java.util.Map;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class CombatLogData
/*    */ {
/*    */   public void setBothFrozen(CombatAttackStats bothFrozen) {
/* 15 */     this.bothFrozen = bothFrozen; } public void setBothUnfrozen(CombatAttackStats bothUnfrozen) { this.bothUnfrozen = bothUnfrozen; } public void setTargetFrozenWeUnfrozen(CombatAttackStats targetFrozenWeUnfrozen) { this.targetFrozenWeUnfrozen = targetFrozenWeUnfrozen; } public void setWeFrozenTargetUnfrozen(CombatAttackStats weFrozenTargetUnfrozen) { this.weFrozenTargetUnfrozen = weFrozenTargetUnfrozen; } public void setAttackHistory(List<AttackContext> attackHistory) { this.attackHistory = attackHistory; } public void setPatternFrequency(Map<String, Map<String, Integer>> patternFrequency) { this.patternFrequency = patternFrequency; }
/*    */ 
/*    */ 
/*    */   
/* 19 */   private CombatAttackStats bothFrozen = new CombatAttackStats(); public CombatAttackStats getBothFrozen() { return this.bothFrozen; }
/*    */ 
/*    */   
/* 22 */   private CombatAttackStats bothUnfrozen = new CombatAttackStats(); public CombatAttackStats getBothUnfrozen() { return this.bothUnfrozen; }
/*    */ 
/*    */   
/* 25 */   private CombatAttackStats targetFrozenWeUnfrozen = new CombatAttackStats(); public CombatAttackStats getTargetFrozenWeUnfrozen() { return this.targetFrozenWeUnfrozen; }
/*    */ 
/*    */   
/* 28 */   private CombatAttackStats weFrozenTargetUnfrozen = new CombatAttackStats(); public CombatAttackStats getWeFrozenTargetUnfrozen() { return this.weFrozenTargetUnfrozen; }
/*    */ 
/*    */   
/* 31 */   private List<AttackContext> attackHistory = new ArrayList<>(); public List<AttackContext> getAttackHistory() { return this.attackHistory; }
/*    */ 
/*    */ 
/*    */   
/* 35 */   private Map<String, Map<String, Integer>> patternFrequency = new HashMap<>(); public Map<String, Map<String, Integer>> getPatternFrequency() { return this.patternFrequency; }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public void addAttackToHistory(AttackContext context, int maxHistorySize) {
/* 42 */     if (context == null) {
/*    */       return;
/*    */     }
/*    */ 
/*    */     
/* 47 */     this.attackHistory.add(context);
/*    */ 
/*    */     
/* 50 */     while (this.attackHistory.size() > maxHistorySize)
/*    */     {
/* 52 */       this.attackHistory.remove(0);
/*    */     }
/*    */ 
/*    */     
/* 56 */     if (this.attackHistory.size() >= 2) {
/*    */       
/* 58 */       AttackContext previousContext = this.attackHistory.get(this.attackHistory.size() - 2);
/* 59 */       String contextKey = previousContext.generateContextKey();
/* 60 */       String nextAttack = context.getAttackStyle();
/*    */       
/* 62 */       this.patternFrequency.computeIfAbsent(contextKey, k -> new HashMap<>());
/* 63 */       Map<String, Integer> nextAttackCounts = this.patternFrequency.get(contextKey);
/* 64 */       nextAttackCounts.put(nextAttack, Integer.valueOf(((Integer)nextAttackCounts.getOrDefault(nextAttack, Integer.valueOf(0))).intValue() + 1));
/*    */     } 
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public List<AttackContext> getRecentHistory(int count) {
/* 73 */     if (this.attackHistory.isEmpty())
/*    */     {
/* 75 */       return new ArrayList<>();
/*    */     }
/*    */     
/* 78 */     int startIndex = Math.max(0, this.attackHistory.size() - count);
/* 79 */     return new ArrayList<>(this.attackHistory.subList(startIndex, this.attackHistory.size()));
/*    */   }
/*    */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/CombatLogData.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */