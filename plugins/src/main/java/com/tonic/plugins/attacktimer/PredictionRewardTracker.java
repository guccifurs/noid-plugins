/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.HashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
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
/*     */ public class PredictionRewardTracker
/*     */ {
/*  25 */   private final Map<String, Map<String, Double>> contextRewards = new HashMap<>();
/*  26 */   public final List<TickContext> recentTicks = new ArrayList<>(); private static final int CONTEXT_WINDOW_TICKS = 50;
/*  27 */   private final Map<String, PendingPrediction> pendingPredictions = new HashMap<>();
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void logTick(TickContext tickContext) {
/*  35 */     if (tickContext == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/*  40 */     this.recentTicks.add(tickContext);
/*     */ 
/*     */     
/*  43 */     while (this.recentTicks.size() > 50)
/*     */     {
/*  45 */       this.recentTicks.remove(0);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String generateContextKey(int ticks) {
/*  54 */     if (this.recentTicks.isEmpty())
/*     */     {
/*  56 */       return "no_context";
/*     */     }
/*     */     
/*  59 */     int startIndex = Math.max(0, this.recentTicks.size() - ticks);
/*  60 */     List<TickContext> contextWindow = this.recentTicks.subList(startIndex, this.recentTicks.size());
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*  69 */     StringBuilder key = new StringBuilder();
/*     */ 
/*     */     
/*  72 */     int freezeStateTicks = Math.min(5, contextWindow.size());
/*  73 */     for (int i = contextWindow.size() - freezeStateTicks; i < contextWindow.size(); i++) {
/*     */       
/*  75 */       TickContext tick = contextWindow.get(i);
/*  76 */       key.append(tick.getFreezeState()).append("|");
/*     */     } 
/*  78 */     key.append(";");
/*     */ 
/*     */     
/*  81 */     List<String> recentAttacks = new ArrayList<>();
/*  82 */     for (int j = contextWindow.size() - 1; j >= 0 && recentAttacks.size() < 10; j--) {
/*     */       
/*  84 */       TickContext tick = contextWindow.get(j);
/*  85 */       if (tick.getAttackType() != null && !tick.getAttackType().isEmpty())
/*     */       {
/*  87 */         recentAttacks.add(0, tick.getAttackType());
/*     */       }
/*     */     } 
/*  90 */     key.append(String.join(",", (Iterable)recentAttacks)).append(";");
/*     */ 
/*     */     
/*  93 */     int movementTicks = Math.min(10, contextWindow.size());
/*  94 */     for (int k = contextWindow.size() - movementTicks; k < contextWindow.size(); k++) {
/*     */       
/*  96 */       TickContext tick = contextWindow.get(k);
/*  97 */       key.append(tick.isPlayerMoved() ? "M" : "-").append("|");
/*     */     } 
/*  99 */     key.append(";");
/*     */ 
/*     */     
/* 102 */     int prayerTicks = Math.min(5, contextWindow.size());
/* 103 */     for (int m = contextWindow.size() - prayerTicks; m < contextWindow.size(); m++) {
/*     */       
/* 105 */       TickContext tick = contextWindow.get(m);
/* 106 */       key.append(tick.getPrayer()).append("|");
/*     */     } 
/* 108 */     key.append(";");
/*     */ 
/*     */     
/* 111 */     int weaponTicks = Math.min(5, contextWindow.size());
/* 112 */     for (int n = contextWindow.size() - weaponTicks; n < contextWindow.size(); n++) {
/*     */       
/* 114 */       TickContext tick = contextWindow.get(n);
/* 115 */       key.append(tick.getWeapon()).append("|");
/*     */     } 
/*     */     
/* 118 */     return key.toString();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void recordPrediction(String contextKey, String predictedAttack, double qualityScore) {
/* 126 */     if (contextKey == null || predictedAttack == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 132 */     this.pendingPredictions.put(contextKey, new PendingPrediction(predictedAttack, qualityScore));
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void processAttackOutcome(String actualAttack) {
/* 141 */     if (actualAttack == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 147 */     String contextKey = generateContextKey(50);
/*     */ 
/*     */     
/* 150 */     PendingPrediction prediction = this.pendingPredictions.remove(contextKey);
/*     */     
/* 152 */     if (prediction != null) {
/*     */       
/* 154 */       String predictedAttack = prediction.getPredictedAttack();
/*     */ 
/*     */       
/* 157 */       Map<String, Double> rewards = this.contextRewards.computeIfAbsent(contextKey, k -> new HashMap<>());
/*     */ 
/*     */       
/* 160 */       rewards.putIfAbsent("melee", Double.valueOf(0.0D));
/* 161 */       rewards.putIfAbsent("ranged", Double.valueOf(0.0D));
/* 162 */       rewards.putIfAbsent("magic", Double.valueOf(0.0D));
/*     */       
/* 164 */       if (predictedAttack.equalsIgnoreCase(actualAttack)) {
/*     */ 
/*     */         
/* 167 */         rewards.put(actualAttack, Double.valueOf(((Double)rewards.get(actualAttack)).doubleValue() + 2.0D));
/*     */       
/*     */       }
/*     */       else {
/*     */         
/* 172 */         for (String attackType : Arrays.<String>asList(new String[] { "melee", "ranged", "magic" })) {
/*     */           
/* 174 */           if (!attackType.equalsIgnoreCase(predictedAttack))
/*     */           {
/* 176 */             rewards.put(attackType, Double.valueOf(((Double)rewards.get(attackType)).doubleValue() + 1.0D));
/*     */           }
/*     */         } 
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 183 */     updateSimilarContexts(contextKey, actualAttack, prediction);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void updateSimilarContexts(String contextKey, String actualAttack, PendingPrediction prediction) {
/* 191 */     if (prediction == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 196 */     String predictedAttack = prediction.getPredictedAttack();
/*     */ 
/*     */     
/* 199 */     for (Map.Entry<String, Map<String, Double>> entry : this.contextRewards.entrySet()) {
/*     */       
/* 201 */       String otherKey = entry.getKey();
/* 202 */       if (otherKey.equals(contextKey)) {
/*     */         continue;
/*     */       }
/*     */ 
/*     */ 
/*     */       
/* 208 */       if (isSimilarContext(contextKey, otherKey)) {
/*     */         
/* 210 */         Map<String, Double> rewards = entry.getValue();
/* 211 */         rewards.putIfAbsent("melee", Double.valueOf(0.0D));
/* 212 */         rewards.putIfAbsent("ranged", Double.valueOf(0.0D));
/* 213 */         rewards.putIfAbsent("magic", Double.valueOf(0.0D));
/*     */         
/* 215 */         if (predictedAttack.equalsIgnoreCase(actualAttack)) {
/*     */ 
/*     */           
/* 218 */           rewards.put(actualAttack, Double.valueOf(((Double)rewards.get(actualAttack)).doubleValue() + 1.0D));
/*     */           
/*     */           continue;
/*     */         } 
/*     */         
/* 223 */         for (String attackType : Arrays.<String>asList(new String[] { "melee", "ranged", "magic" })) {
/*     */           
/* 225 */           if (!attackType.equalsIgnoreCase(predictedAttack))
/*     */           {
/* 227 */             rewards.put(attackType, Double.valueOf(((Double)rewards.get(attackType)).doubleValue() + 0.5D));
/*     */           }
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean isSimilarContext(String key1, String key2) {
/* 240 */     if (key1 == null || key2 == null)
/*     */     {
/* 242 */       return false;
/*     */     }
/*     */     
/* 245 */     String[] parts1 = key1.split(";");
/* 246 */     String[] parts2 = key2.split(";");
/*     */     
/* 248 */     if (parts1.length < 2 || parts2.length < 2)
/*     */     {
/* 250 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 254 */     if (!parts1[0].equals(parts2[0]))
/*     */     {
/* 256 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 260 */     String[] attacks1 = parts1[1].split(",");
/* 261 */     String[] attacks2 = parts2[1].split(",");
/*     */     
/* 263 */     int matches = 0;
/* 264 */     int minLength = Math.min(attacks1.length, attacks2.length);
/* 265 */     for (int i = 0; i < minLength && i < 5; i++) {
/*     */       
/* 267 */       int idx1 = attacks1.length - 1 - i;
/* 268 */       int idx2 = attacks2.length - 1 - i;
/* 269 */       if (idx1 >= 0 && idx2 >= 0 && idx1 < attacks1.length && idx2 < attacks2.length && attacks1[idx1]
/*     */         
/* 271 */         .equals(attacks2[idx2]))
/*     */       {
/* 273 */         matches++;
/*     */       }
/*     */     } 
/*     */     
/* 277 */     return (matches >= 3);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public Map<String, Double> getRewardScores(String contextKey) {
/* 285 */     Map<String, Double> rewards = this.contextRewards.get(contextKey);
/* 286 */     if (rewards == null) {
/*     */ 
/*     */       
/* 289 */       Map<String, Double> empty = new HashMap<>();
/* 290 */       empty.put("melee", Double.valueOf(0.0D));
/* 291 */       empty.put("ranged", Double.valueOf(0.0D));
/* 292 */       empty.put("magic", Double.valueOf(0.0D));
/* 293 */       return empty;
/*     */     } 
/*     */ 
/*     */     
/* 297 */     Map<String, Double> result = new HashMap<>();
/* 298 */     result.put("melee", rewards.getOrDefault("melee", Double.valueOf(0.0D)));
/* 299 */     result.put("ranged", rewards.getOrDefault("ranged", Double.valueOf(0.0D)));
/* 300 */     result.put("magic", rewards.getOrDefault("magic", Double.valueOf(0.0D)));
/* 301 */     return result;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public Map<String, Double> getCurrentRewardScores() {
/* 309 */     String contextKey = generateContextKey(50);
/* 310 */     return getRewardScores(contextKey);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void cleanup() {
/* 320 */     if (this.pendingPredictions.size() > 100)
/*     */     {
/* 322 */       this.pendingPredictions.clear();
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public static class TickContext
/*     */   {
/*     */     private final String freezeState;
/*     */     
/*     */     private final String attackType;
/*     */     
/*     */     private final boolean playerMoved;
/*     */     
/*     */     private final String prayer;
/*     */     
/*     */     private final String weapon;
/*     */     private final long tick;
/*     */     
/*     */     public TickContext(String freezeState, String attackType, boolean playerMoved, String prayer, String weapon, long tick) {
/* 341 */       this.freezeState = freezeState;
/* 342 */       this.attackType = attackType;
/* 343 */       this.playerMoved = playerMoved;
/* 344 */       this.prayer = prayer;
/* 345 */       this.weapon = weapon;
/* 346 */       this.tick = tick;
/*     */     }
/*     */     
/* 349 */     public String getFreezeState() { return this.freezeState; }
/* 350 */     public String getAttackType() { return this.attackType; }
/* 351 */     public boolean isPlayerMoved() { return this.playerMoved; }
/* 352 */     public String getPrayer() { return this.prayer; }
/* 353 */     public String getWeapon() { return this.weapon; } public long getTick() {
/* 354 */       return this.tick;
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   private static class PendingPrediction
/*     */   {
/*     */     private final String predictedAttack;
/*     */     
/*     */     private final double qualityScore;
/*     */ 
/*     */     
/*     */     public PendingPrediction(String predictedAttack, double qualityScore) {
/* 367 */       this.predictedAttack = predictedAttack;
/* 368 */       this.qualityScore = qualityScore;
/*     */     }
/*     */     
/* 371 */     public String getPredictedAttack() { return this.predictedAttack; } public double getQualityScore() {
/* 372 */       return this.qualityScore;
/*     */     }
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/PredictionRewardTracker.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */