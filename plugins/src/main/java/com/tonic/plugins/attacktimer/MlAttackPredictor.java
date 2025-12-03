/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import java.nio.file.Path;
/*     */ import java.nio.file.Paths;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
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
/*     */ public class MlAttackPredictor
/*     */ {
/*     */   private final AttackTimerConfig config;
/*     */   private boolean mlModelLoaded = false;
/*  30 */   private String modelPath = null;
/*     */ 
/*     */   
/*     */   private String inputName;
/*     */   
/*     */   private String outputName;
/*     */   
/*     */   private List<String> labelClasses;
/*     */   
/*     */   private final AttackContextPredictor fallbackPredictor;
/*     */ 
/*     */   
/*     */   public MlAttackPredictor(AttackTimerConfig config) {
/*  43 */     this.config = config;
/*  44 */     this.fallbackPredictor = new AttackContextPredictor(config);
/*     */ 
/*     */ 
/*     */     
/*  48 */     this.labelClasses = Arrays.asList(new String[] { "melee", "ranged", "magic" });
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean loadModel(Path modelPath) {
/*  56 */     if (modelPath == null || !modelPath.toFile().exists()) {
/*     */       
/*  58 */       Logger.norm("[MlAttackPredictor] Model file not found: " + String.valueOf(modelPath));
/*  59 */       this.mlModelLoaded = false;
/*  60 */       return false;
/*     */     } 
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
/*     */     try {
/*  76 */       this.modelPath = modelPath.toString();
/*  77 */       this.mlModelLoaded = true;
/*     */       
/*  79 */       Logger.norm("[MlAttackPredictor] ✅ ML model loaded from: " + String.valueOf(modelPath));
/*  80 */       Logger.norm("[MlAttackPredictor] Input: " + this.inputName + ", Output: " + this.outputName);
/*  81 */       return true;
/*     */     }
/*  83 */     catch (Exception e) {
/*     */       
/*  85 */       Logger.error("[MlAttackPredictor] Failed to load ML model: " + e.getMessage());
/*  86 */       this.mlModelLoaded = false;
/*  87 */       return false;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean loadDefaultModel() {
/*  96 */     String homeDir = System.getProperty("user.home");
/*  97 */     Path defaultModelPath = Paths.get(homeDir, new String[] { ".runelite", "vitalite", "models", "lightgbm.onnx" });
/*     */ 
/*     */     
/* 100 */     String[] modelNames = { "lightgbm.onnx", "xgboost.onnx", "catboost.onnx" };
/*     */     
/* 102 */     for (String modelName : modelNames) {
/*     */       
/* 104 */       Path modelPath = Paths.get(homeDir, new String[] { ".runelite", "vitalite", "models", modelName });
/* 105 */       if (modelPath.toFile().exists())
/*     */       {
/* 107 */         if (loadModel(modelPath)) {
/*     */           
/* 109 */           Logger.norm("[MlAttackPredictor] ✅ Loaded default model: " + modelName);
/* 110 */           return true;
/*     */         } 
/*     */       }
/*     */     } 
/*     */     
/* 115 */     Logger.norm("[MlAttackPredictor] ⚠️  No ONNX models found in ~/.runelite/vitalite/models/");
/* 116 */     Logger.norm("[MlAttackPredictor] Using heuristic fallback");
/* 117 */     return false;
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
/*     */   public void close() {}
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
/*     */   public List<PredictionResult> getTop5Predictions(AttackContext currentContext, CombatLogData logData) {
/* 148 */     if (this.mlModelLoaded && this.modelPath != null)
/*     */     {
/*     */       
/* 151 */       return predictWithMLModelAndPersonalization(currentContext, logData);
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 156 */     return this.fallbackPredictor.getTop5Predictions(currentContext, logData);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private List<PredictionResult> predictWithMLModelAndPersonalization(AttackContext currentContext, CombatLogData logData) {
/* 167 */     Map<String, Double> mlScores = predictWithMLModel(currentContext);
/*     */ 
/*     */     
/* 170 */     if (mlScores == null || mlScores.isEmpty())
/*     */     {
/* 172 */       return this.fallbackPredictor.getTop5Predictions(currentContext, logData);
/*     */     }
/*     */ 
/*     */     
/* 176 */     List<PredictionResult> basePredictions = new ArrayList<>();
/* 177 */     for (Map.Entry<String, Double> entry : mlScores.entrySet())
/*     */     {
/* 179 */       basePredictions.add(new PredictionResult(entry.getKey(), ((Double)entry.getValue()).doubleValue(), "ml_model"));
/*     */     }
/* 181 */     Collections.sort(basePredictions);
/* 182 */     Collections.reverse(basePredictions);
/*     */ 
/*     */     
/* 185 */     Map<String, Double> personalizationWeights = getPerTargetPersonalizationWeights(currentContext, logData);
/*     */ 
/*     */     
/* 188 */     Map<String, Double> finalScores = new HashMap<>();
/* 189 */     Map<String, String> methods = new HashMap<>();
/*     */     
/* 191 */     for (PredictionResult basePred : basePredictions) {
/*     */       
/* 193 */       String attackType = basePred.getAttackType();
/* 194 */       double mlScore = basePred.getQualityScore();
/*     */ 
/*     */ 
/*     */       
/* 198 */       double personalizationWeight = ((Double)personalizationWeights.getOrDefault(attackType, Double.valueOf(0.5D))).doubleValue();
/*     */ 
/*     */ 
/*     */ 
/*     */       
/* 203 */       double finalScore = mlScore * 0.7D + personalizationWeight * 0.3D;
/*     */       
/* 205 */       finalScores.put(attackType, Double.valueOf(finalScore));
/* 206 */       methods.put(attackType, "ml_hybrid");
/*     */     } 
/*     */ 
/*     */     
/* 210 */     List<PredictionResult> finalPredictions = new ArrayList<>();
/* 211 */     for (String attackType : Arrays.<String>asList(new String[] { "melee", "ranged", "magic" })) {
/*     */       
/* 213 */       double score = ((Double)finalScores.getOrDefault(attackType, Double.valueOf(0.0D))).doubleValue();
/* 214 */       String method = methods.getOrDefault(attackType, "ml_hybrid");
/* 215 */       finalPredictions.add(new PredictionResult(attackType, score, method));
/*     */     } 
/*     */ 
/*     */     
/* 219 */     Collections.sort(finalPredictions);
/* 220 */     Collections.reverse(finalPredictions);
/*     */     
/* 222 */     return finalPredictions.subList(0, Math.min(5, finalPredictions.size()));
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private Map<String, Double> getPerTargetPersonalizationWeights(AttackContext currentContext, CombatLogData logData) {
/* 232 */     Map<String, Double> weights = new HashMap<>();
/* 233 */     weights.put("melee", Double.valueOf(0.33D));
/* 234 */     weights.put("ranged", Double.valueOf(0.33D));
/* 235 */     weights.put("magic", Double.valueOf(0.33D));
/*     */     
/* 237 */     if (logData == null || logData.getAttackHistory().isEmpty())
/*     */     {
/* 239 */       return weights;
/*     */     }
/*     */     
/* 242 */     List<AttackContext> history = logData.getAttackHistory();
/* 243 */     Map<String, Map<String, Integer>> patternFrequency = logData.getPatternFrequency();
/*     */ 
/*     */     
/* 246 */     Map<String, Integer> attackCounts = new HashMap<>();
/* 247 */     attackCounts.put("melee", Integer.valueOf(0));
/* 248 */     attackCounts.put("ranged", Integer.valueOf(0));
/* 249 */     attackCounts.put("magic", Integer.valueOf(0));
/* 250 */     int totalMatches = 0;
/*     */ 
/*     */     
/* 253 */     for (int i = 0; i < history.size() - 1; i++) {
/*     */       
/* 255 */       AttackContext histContext = history.get(i);
/* 256 */       AttackContext nextAttack = history.get(i + 1);
/*     */ 
/*     */       
/* 259 */       boolean freezeStateMatch = equalsNullable(currentContext.getFreezeState(), histContext.getFreezeState());
/* 260 */       int hpDiff = Math.abs(currentContext.getTargetHP() - histContext.getTargetHP());
/* 261 */       int distanceDiff = Math.abs(currentContext.getDistance() - histContext.getDistance());
/*     */       
/* 263 */       if (freezeStateMatch && hpDiff <= 20 && distanceDiff <= 4) {
/*     */ 
/*     */         
/* 266 */         String attackType = nextAttack.getAttackStyle();
/* 267 */         if (attackType != null && attackCounts.containsKey(attackType)) {
/*     */           
/* 269 */           attackCounts.put(attackType, Integer.valueOf(((Integer)attackCounts.get(attackType)).intValue() + 1));
/* 270 */           totalMatches++;
/*     */         } 
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 276 */     if (totalMatches > 0) {
/*     */       
/* 278 */       for (Map.Entry<String, Integer> entry : attackCounts.entrySet())
/*     */       {
/* 280 */         double weight = ((Integer)entry.getValue()).intValue() / totalMatches;
/* 281 */         weights.put(entry.getKey(), Double.valueOf(weight));
/*     */       }
/*     */     
/*     */     }
/*     */     else {
/*     */       
/* 287 */       String contextKey = currentContext.generateContextKey();
/* 288 */       Map<String, Integer> patternCounts = patternFrequency.get(contextKey);
/* 289 */       if (patternCounts != null && !patternCounts.isEmpty()) {
/*     */         
/* 291 */         int patternTotal = patternCounts.values().stream().mapToInt(Integer::intValue).sum();
/* 292 */         for (Map.Entry<String, Integer> entry : patternCounts.entrySet()) {
/*     */           
/* 294 */           double weight = ((Integer)entry.getValue()).intValue() / patternTotal;
/* 295 */           weights.put(entry.getKey(), Double.valueOf(weight));
/*     */         } 
/*     */       } 
/*     */     } 
/*     */     
/* 300 */     return weights;
/*     */   }
/*     */ 
/*     */   
/*     */   private boolean equalsNullable(String a, String b) {
/* 305 */     if (a == null && b == null)
/*     */     {
/* 307 */       return true;
/*     */     }
/* 309 */     if (a == null || b == null)
/*     */     {
/* 311 */       return false;
/*     */     }
/* 313 */     return a.equals(b);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private Map<String, Double> predictWithMLModel(AttackContext currentContext) {
/* 322 */     if (!this.mlModelLoaded)
/*     */     {
/* 324 */       return null;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/* 330 */       float[] features = convertContextToFeatureVector(currentContext);
/* 331 */       if (features == null)
/*     */       {
/* 333 */         return null;
/*     */       }
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
/* 360 */       return null;
/*     */     }
/* 362 */     catch (Exception e) {
/*     */       
/* 364 */       Logger.error("[MlAttackPredictor] ML prediction failed: " + e.getMessage());
/* 365 */       return null;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public float[] convertContextToFeatureVector(AttackContext context) {
/* 375 */     if (context == null)
/*     */     {
/* 377 */       return null;
/*     */     }
/*     */ 
/*     */     
/* 381 */     float[] features = new float[23];
/* 382 */     int index = 0;
/*     */ 
/*     */     
/* 385 */     String freezeState = (context.getFreezeState() != null) ? context.getFreezeState() : "unknown";
/* 386 */     features[index++] = freezeState.equals("bothFrozen") ? 1.0F : 0.0F;
/* 387 */     features[index++] = freezeState.equals("bothUnfrozen") ? 1.0F : 0.0F;
/* 388 */     features[index++] = freezeState.equals("targetFrozenWeUnfrozen") ? 1.0F : 0.0F;
/* 389 */     features[index++] = freezeState.equals("weFrozenTargetUnfrozen") ? 1.0F : 0.0F;
/*     */ 
/*     */     
/* 392 */     String prayer = (context.getOverheadPrayer() != null) ? context.getOverheadPrayer() : "none";
/* 393 */     features[index++] = prayer.equals("melee") ? 1.0F : 0.0F;
/* 394 */     features[index++] = prayer.equals("ranged") ? 1.0F : 0.0F;
/* 395 */     features[index++] = prayer.equals("magic") ? 1.0F : 0.0F;
/* 396 */     features[index++] = prayer.equals("none") ? 1.0F : 0.0F;
/*     */ 
/*     */     
/* 399 */     String weapon = (context.getWeapon() != null) ? context.getWeapon() : "unknown";
/* 400 */     features[index++] = weapon.equals("whip") ? 1.0F : 0.0F;
/* 401 */     features[index++] = weapon.equals("crossbow") ? 1.0F : 0.0F;
/* 402 */     features[index++] = weapon.equals("staff") ? 1.0F : 0.0F;
/* 403 */     features[index++] = weapon.equals("ags") ? 1.0F : 0.0F;
/* 404 */     features[index++] = weapon.equals("claws") ? 1.0F : 0.0F;
/* 405 */     features[index++] = weapon.equals("bow") ? 1.0F : 0.0F;
/*     */ 
/*     */ 
/*     */     
/* 409 */     String pidStatus = (context.getPidStatus() != null) ? context.getPidStatus() : "UNKNOWN";
/* 410 */     features[index++] = pidStatus.equals("ON_PID") ? 1.0F : 0.0F;
/* 411 */     features[index++] = pidStatus.equals("OFF_PID") ? 1.0F : 0.0F;
/* 412 */     features[index++] = pidStatus.equals("UNKNOWN") ? 1.0F : 0.0F;
/*     */ 
/*     */     
/* 415 */     features[index++] = context.getTargetHP();
/* 416 */     features[index++] = context.getTargetSpec();
/* 417 */     features[index++] = context.getDistance();
/*     */     
/* 419 */     features[index++] = -1.0F;
/* 420 */     features[index++] = -1.0F;
/*     */     
/* 422 */     return features;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isModelLoaded() {
/* 430 */     return this.mlModelLoaded;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String getModelPath() {
/* 438 */     return this.modelPath;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/MlAttackPredictor.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */