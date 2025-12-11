/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.google.gson.Gson;
/*     */ import com.google.gson.GsonBuilder;
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import java.io.File;
/*     */ import java.io.FileReader;
/*     */ import java.nio.file.Path;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.HashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Objects;
/*     */ 
/*     */ public class TickBasedPredictor {
/*     */   private static final String COMBAT_LOGS_FOLDER = "CombatLogs";
/*     */   private static final String WEIGHTS_FILE_SUFFIX = "_weights.json";
/*  21 */   private static final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
/*     */ 
/*     */   
/*     */   private final AttackTimerConfig config;
/*     */ 
/*     */   
/*     */   private final File combatLogsDir;
/*     */   
/*     */   private final Map<String, List<TickDataLogger.TickData>> tickDataCache;
/*     */   
/*     */   private final Map<String, StrategyWeights> weightsCache;
/*     */ 
/*     */   
/*     */   public static class StrategyWeights
/*     */   {
/*  36 */     public double freeze_state = 0.1D;
/*  37 */     public double sequence = 0.2D;
/*  38 */     public double temporal = 0.15D;
/*  39 */     public double phase_aware = 0.1D;
/*  40 */     public double movement = 0.15D;
/*  41 */     public double resource = 0.1D;
/*  42 */     public double frequency = 0.05D;
/*  43 */     public double reward = 0.05D;
/*     */ 
/*     */     
/*  46 */     public double player_action = 0.03D;
/*  47 */     public double player_damage = 0.02D;
/*  48 */     public double opponent_action = 0.03D;
/*  49 */     public double opponent_damage = 0.02D;
/*  50 */     public double health_state = 0.02D;
/*  51 */     public double resource_state = 0.01D;
/*  52 */     public double event_timing = 0.015D;
/*  53 */     public double pattern_match = 0.005D;
/*     */ 
/*     */     
/*     */     public void normalize() {
/*  57 */       double total = this.freeze_state + this.sequence + this.temporal + this.phase_aware + this.movement + this.resource + this.frequency + this.reward + this.player_action + this.player_damage + this.opponent_action + this.opponent_damage + this.health_state + this.resource_state + this.event_timing + this.pattern_match;
/*     */ 
/*     */       
/*  60 */       if (total > 0.0D) {
/*     */         
/*  62 */         this.freeze_state /= total;
/*  63 */         this.sequence /= total;
/*  64 */         this.temporal /= total;
/*  65 */         this.phase_aware /= total;
/*  66 */         this.movement /= total;
/*  67 */         this.resource /= total;
/*  68 */         this.frequency /= total;
/*  69 */         this.reward /= total;
/*  70 */         this.player_action /= total;
/*  71 */         this.player_damage /= total;
/*  72 */         this.opponent_action /= total;
/*  73 */         this.opponent_damage /= total;
/*  74 */         this.health_state /= total;
/*  75 */         this.resource_state /= total;
/*  76 */         this.event_timing /= total;
/*  77 */         this.pattern_match /= total;
/*     */       } 
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public TickBasedPredictor(AttackTimerConfig config) {
/*  84 */     this.config = config;
/*  85 */     Path vitaDir = Static.VITA_DIR;
/*  86 */     this.combatLogsDir = new File(vitaDir.toFile(), "CombatLogs");
/*  87 */     this.tickDataCache = new HashMap<>();
/*  88 */     this.weightsCache = new HashMap<>();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public List<TickDataLogger.TickData> loadTickData(String targetRSN) {
/*  96 */     if (this.tickDataCache.containsKey(targetRSN))
/*     */     {
/*  98 */       return this.tickDataCache.get(targetRSN);
/*     */     }
/*     */ 
/*     */     
/*     */     try {
/* 103 */       String sanitizedRSN = targetRSN.replaceAll("[^a-zA-Z0-9_-]", "_");
/* 104 */       File tickFile = new File(this.combatLogsDir, sanitizedRSN + ".json");
/*     */       
/* 106 */       if (!tickFile.exists()) {
/*     */         
/* 108 */         if (this.config.debug())
/*     */         {
/* 110 */           Logger.norm("[TickBasedPredictor] No tick data file found for: " + targetRSN);
/*     */         }
/* 112 */         return new ArrayList<>();
/*     */       } 
/*     */ 
/*     */       
/* 116 */       FileReader reader = new FileReader(tickFile);
/*     */       
/* 118 */       try { TickDataLogger.TickData[] tickDataArray = (TickDataLogger.TickData[])gson.fromJson(reader, TickDataLogger.TickData[].class);
/* 119 */         List<TickDataLogger.TickData> tickDataList = Arrays.asList(tickDataArray);
/*     */         
/* 121 */         this.tickDataCache.put(targetRSN, tickDataList);
/*     */         
/* 123 */         if (this.config.debug())
/*     */         {
/* 125 */           Logger.norm("[TickBasedPredictor] Loaded " + tickDataList.size() + " ticks for: " + targetRSN);
/*     */         }
/*     */         
/* 128 */         List<TickDataLogger.TickData> list1 = tickDataList;
/* 129 */         reader.close(); return list1; } catch (Throwable throwable) { try { reader.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; }
/*     */     
/* 131 */     } catch (Exception e) {
/*     */       
/* 133 */       Logger.norm("[TickBasedPredictor] Failed to load tick data for " + targetRSN + ": " + e.getMessage());
/* 134 */       return new ArrayList<>();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public StrategyWeights loadWeights(String targetRSN) {
/* 143 */     if (this.weightsCache.containsKey(targetRSN))
/*     */     {
/* 145 */       return this.weightsCache.get(targetRSN);
/*     */     }
/*     */     
/*     */     try {
/*     */       StrategyWeights weights;
/* 150 */       String sanitizedRSN = targetRSN.replaceAll("[^a-zA-Z0-9_-]", "_");
/* 151 */       File weightsFile = new File(this.combatLogsDir, sanitizedRSN + "_weights.json");
/*     */ 
/*     */       
/* 154 */       if (weightsFile.exists())
/*     */       
/*     */       { 
/* 157 */         FileReader reader = new FileReader(weightsFile);
/*     */         
/* 159 */         try { weights = (StrategyWeights)gson.fromJson(reader, StrategyWeights.class);
/* 160 */           if (weights != null)
/*     */           {
/* 162 */             weights.normalize();
/*     */           }
/* 164 */           reader.close(); }
/*     */         catch (Throwable throwable) { try { reader.close(); }
/*     */           catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }
/*     */            throw throwable; }
/*     */          }
/* 169 */       else { weights = new StrategyWeights();
/* 170 */         weights.normalize();
/*     */         
/* 172 */         if (this.config.debug())
/*     */         {
/* 174 */           Logger.norm("[TickBasedPredictor] No weights file found for " + targetRSN + ", using defaults");
/*     */         } }
/*     */ 
/*     */       
/* 178 */       this.weightsCache.put(targetRSN, weights);
/* 179 */       return weights;
/*     */     }
/* 181 */     catch (Exception e) {
/*     */       
/* 183 */       Logger.norm("[TickBasedPredictor] Failed to load weights for " + targetRSN + ": " + e.getMessage());
/* 184 */       StrategyWeights defaultWeights = new StrategyWeights();
/* 185 */       defaultWeights.normalize();
/* 186 */       return defaultWeights;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public List<PredictionResult> predictFromTickData(String targetRSN, TickDataLogger.TickData currentTick) {
/* 196 */     if (currentTick == null || targetRSN == null)
/*     */     {
/* 198 */       return getDefaultPredictions();
/*     */     }
/*     */ 
/*     */     
/* 202 */     List<TickDataLogger.TickData> tickHistory = loadTickData(targetRSN);
/*     */     
/* 204 */     if (tickHistory.isEmpty())
/*     */     {
/* 206 */       return getDefaultPredictions();
/*     */     }
/*     */ 
/*     */     
/* 210 */     StrategyWeights weights = loadWeights(targetRSN);
/*     */ 
/*     */     
/* 213 */     Map<String, Double> scores = new HashMap<>();
/* 214 */     scores.put("melee", Double.valueOf(0.0D));
/* 215 */     scores.put("ranged", Double.valueOf(0.0D));
/* 216 */     scores.put("magic", Double.valueOf(0.0D));
/*     */ 
/*     */     
/* 219 */     Map<String, Object> currentFeatures = currentTick.features;
/* 220 */     if (currentFeatures == null)
/*     */     {
/* 222 */       return getDefaultPredictions();
/*     */     }
/*     */ 
/*     */     
/* 226 */     for (int i = 0; i < tickHistory.size() - 1; i++) {
/*     */       
/* 228 */       TickDataLogger.TickData historicalTick = tickHistory.get(i);
/* 229 */       TickDataLogger.TickData nextTick = tickHistory.get(i + 1);
/*     */       
/* 231 */       if (nextTick.opponent != null && nextTick.opponent.attackType != null) {
/*     */ 
/*     */ 
/*     */ 
/*     */         
/* 236 */         String nextAttack = nextTick.opponent.attackType;
/* 237 */         if (scores.containsKey(nextAttack)) {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */           
/* 243 */           double similarity = calculateFeatureSimilarity(currentFeatures, historicalTick.features);
/*     */           
/* 245 */           if (similarity >= 0.1D) {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */             
/* 251 */             double weight = calculateWeightedScore(currentFeatures, historicalTick.features, weights, similarity);
/*     */             
/* 253 */             scores.put(nextAttack, Double.valueOf(((Double)scores.get(nextAttack)).doubleValue() + weight));
/*     */           } 
/*     */         } 
/*     */       } 
/* 257 */     }  double totalScore = ((Double)scores.get("melee")).doubleValue() + ((Double)scores.get("ranged")).doubleValue() + ((Double)scores.get("magic")).doubleValue();
/* 258 */     if (totalScore > 0.01D) {
/*     */       
/* 260 */       scores.put("melee", Double.valueOf(((Double)scores.get("melee")).doubleValue() / totalScore * 100.0D));
/* 261 */       scores.put("ranged", Double.valueOf(((Double)scores.get("ranged")).doubleValue() / totalScore * 100.0D));
/* 262 */       scores.put("magic", Double.valueOf(((Double)scores.get("magic")).doubleValue() / totalScore * 100.0D));
/*     */     
/*     */     }
/*     */     else {
/*     */ 
/*     */       
/* 268 */       scores.put("melee", Double.valueOf(33.33D));
/* 269 */       scores.put("ranged", Double.valueOf(33.33D));
/* 270 */       scores.put("magic", Double.valueOf(33.34D));
/*     */     } 
/*     */ 
/*     */     
/* 274 */     List<PredictionResult> predictions = new ArrayList<>();
/* 275 */     for (Map.Entry<String, Double> entry : scores.entrySet())
/*     */     {
/* 277 */       predictions.add(new PredictionResult(entry.getKey(), ((Double)entry.getValue()).doubleValue(), "tick_based"));
/*     */     }
/*     */ 
/*     */     
/* 281 */     predictions.sort((a, b) -> Double.compare(b.getQualityScore(), a.getQualityScore()));
/*     */ 
/*     */     
/* 284 */     return predictions.subList(0, Math.min(5, predictions.size()));
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private double calculateFeatureSimilarity(Map<String, Object> current, Map<String, Object> historical) {
/* 292 */     if (current == null || historical == null)
/*     */     {
/* 294 */       return 0.0D;
/*     */     }
/*     */     
/* 297 */     double similarity = 0.0D;
/* 298 */     int total = 0;
/*     */ 
/*     */     
/* 301 */     String[] keyFeatures = { "freeze_state", "distance_category", "hp_category_player", "opponent_attack_type", "player_animation_category", "opponent_animation_category", "time_category" };
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 307 */     for (String feature : keyFeatures) {
/*     */       
/* 309 */       total++;
/* 310 */       Object currentVal = current.get(feature);
/* 311 */       Object historicalVal = historical.get(feature);
/*     */       
/* 313 */       if (currentVal != null && historicalVal != null && currentVal.equals(historicalVal))
/*     */       {
/*     */         
/* 316 */         similarity++;
/*     */       }
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 322 */     List<String> currentActions = (List<String>)current.get("opponent_actions");
/*     */     
/* 324 */     List<String> historicalActions = (List<String>)historical.get("opponent_actions");
/*     */     
/* 326 */     if (currentActions != null && historicalActions != null) {
/*     */       
/* 328 */       total++;
/* 329 */       if (currentActions.equals(historicalActions)) {
/*     */ 
/*     */         
/* 332 */         similarity++;
/*     */       }
/* 334 */       else if (!Collections.disjoint(currentActions, historicalActions)) {
/*     */         
/* 336 */         similarity += 0.5D;
/*     */       } 
/*     */     } 
/*     */     
/* 340 */     if (total > 0)
/*     */     {
/* 342 */       similarity /= total;
/*     */     }
/*     */     
/* 345 */     return similarity;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private double calculateWeightedScore(Map<String, Object> current, Map<String, Object> historical, StrategyWeights weights, double baseSimilarity) {
/* 354 */     double weight = 0.0D;
/*     */ 
/*     */     
/* 357 */     if (Objects.equals(current.get("freeze_state"), historical.get("freeze_state")))
/*     */     {
/* 359 */       weight += weights.freeze_state;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 364 */     if (Objects.equals(current.get("opponent_attack_type"), historical.get("opponent_attack_type")))
/*     */     {
/* 366 */       weight += weights.sequence * 0.5D;
/*     */     }
/*     */ 
/*     */     
/* 370 */     if (Objects.equals(current.get("time_category"), historical.get("time_category")))
/*     */     {
/* 372 */       weight += weights.temporal;
/*     */     }
/*     */ 
/*     */     
/* 376 */     if (Objects.equals(current.get("distance_category"), historical.get("distance_category")))
/*     */     {
/* 378 */       weight += weights.movement;
/*     */     }
/*     */ 
/*     */     
/* 382 */     if (Objects.equals(current.get("hp_category_player"), historical.get("hp_category_player")))
/*     */     {
/* 384 */       weight += weights.resource;
/*     */     }
/*     */ 
/*     */     
/* 388 */     if (Objects.equals(current.get("player_animation_category"), historical.get("player_animation_category")))
/*     */     {
/* 390 */       weight += weights.player_action;
/*     */     }
/*     */ 
/*     */     
/* 394 */     if (Objects.equals(current.get("opponent_animation_category"), historical.get("opponent_animation_category")))
/*     */     {
/* 396 */       weight += weights.opponent_action;
/*     */     }
/*     */ 
/*     */     
/* 400 */     if (Objects.equals(current.get("hp_category_player"), historical.get("hp_category_player")))
/*     */     {
/* 402 */       weight += weights.health_state;
/*     */     }
/*     */ 
/*     */     
/* 406 */     return weight * baseSimilarity;
/*     */   }
/*     */ 
/*     */   
/*     */   public List<PredictionResult> getDefaultPredictions() {
/* 411 */     List<PredictionResult> predictions = new ArrayList<>();
/* 412 */     predictions.add(new PredictionResult("melee", 33.33D, "default"));
/* 413 */     predictions.add(new PredictionResult("ranged", 33.33D, "default"));
/* 414 */     predictions.add(new PredictionResult("magic", 33.34D, "default"));
/* 415 */     return predictions;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void clearCaches() {
/* 423 */     this.tickDataCache.clear();
/* 424 */     this.weightsCache.clear();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void clearCache(String targetRSN) {
/* 432 */     this.tickDataCache.remove(targetRSN);
/* 433 */     this.weightsCache.remove(targetRSN);
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/TickBasedPredictor.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */