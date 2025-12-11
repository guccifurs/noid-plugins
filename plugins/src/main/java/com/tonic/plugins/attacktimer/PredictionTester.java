/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.google.gson.Gson;
/*     */ import com.google.gson.GsonBuilder;
/*     */ import com.google.gson.reflect.TypeToken;
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.io.PrintStream;
/*     */ import java.lang.reflect.Type;
/*     */ import java.nio.file.Files;
/*     */ import java.nio.file.Path;
/*     */ import java.util.ArrayList;
/*     */ import java.util.HashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ 
/*     */ 
/*     */ 
/*     */ public class PredictionTester
/*     */ {
/*     */   private static final String LOG_FILE_NAME = "combat-attack-log.json";
/*  24 */   private static final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
/*     */   
/*     */   private final AttackContextPredictor predictor;
/*     */   
/*     */   private final AttackTimerConfig config;
/*     */   
/*     */   private final PrintStream output;
/*     */   private final boolean useLogger;
/*  32 */   private int totalTests = 0;
/*  33 */   private int correctPredictions = 0;
/*  34 */   private int correctTop3 = 0;
/*     */ 
/*     */   
/*  37 */   private Map<String, TestStats> freezeStateStats = new HashMap<>();
/*     */ 
/*     */   
/*  40 */   private Map<String, TestStats> methodStats = new HashMap<>();
/*     */ 
/*     */   
/*  43 */   private Map<String, TestStats> targetStats = new HashMap<>();
/*     */   
/*     */   public static class TestStats
/*     */   {
/*  47 */     int total = 0;
/*  48 */     int correct = 0;
/*  49 */     int correctTop3 = 0;
/*  50 */     Map<String, Integer> predictedCounts = new HashMap<>();
/*  51 */     Map<String, Integer> actualCounts = new HashMap<>();
/*     */ 
/*     */     
/*     */     public double getAccuracy() {
/*  55 */       return (this.total > 0) ? (this.correct * 100.0D / this.total) : 0.0D;
/*     */     }
/*     */ 
/*     */     
/*     */     public double getTop3Accuracy() {
/*  60 */       return (this.total > 0) ? (this.correctTop3 * 100.0D / this.total) : 0.0D;
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public PredictionTester(AttackTimerConfig config) {
/*  66 */     this(config, System.out, false);
/*     */   }
/*     */ 
/*     */   
/*     */   public PredictionTester(AttackTimerConfig config, PrintStream output, boolean useLogger) {
/*  71 */     this.config = config;
/*  72 */     this.predictor = new AttackContextPredictor(config);
/*  73 */     this.output = output;
/*  74 */     this.useLogger = useLogger;
/*     */ 
/*     */     
/*  77 */     this.freezeStateStats.put("bothFrozen", new TestStats());
/*  78 */     this.freezeStateStats.put("bothUnfrozen", new TestStats());
/*  79 */     this.freezeStateStats.put("targetFrozenWeUnfrozen", new TestStats());
/*  80 */     this.freezeStateStats.put("weFrozenTargetUnfrozen", new TestStats());
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void log(String message) {
/*  88 */     if (this.useLogger) {
/*     */       
/*  90 */       Logger.norm(message);
/*     */     }
/*     */     else {
/*     */       
/*  94 */       this.output.println(message);
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public Map<String, CombatLogData> loadCombatLogs() {
/* 103 */     Path vitaDir = Static.VITA_DIR;
/* 104 */     File logFile = new File(vitaDir.toFile(), "combat-attack-log.json");
/*     */     
/* 106 */     if (!logFile.exists()) {
/*     */       
/* 108 */       log("[PredictionTester] Combat log file not found: " + logFile.getAbsolutePath());
/* 109 */       return new HashMap<>();
/*     */     } 
/*     */ 
/*     */     
/*     */     try {
/* 114 */       String json = new String(Files.readAllBytes(logFile.toPath()));
/* 115 */       Type type = (new TypeToken<Map<String, CombatLogData>>() {  }).getType();
/* 116 */       Map<String, CombatLogData> logs = (Map<String, CombatLogData>)gson.fromJson(json, type);
/*     */       
/* 118 */       log("[PredictionTester] Loaded combat logs for " + logs.size() + " targets");
/* 119 */       return logs;
/*     */     }
/* 121 */     catch (IOException e) {
/*     */       
/* 123 */       log("[PredictionTester] Failed to load combat log: " + e.getMessage());
/* 124 */       return new HashMap<>();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void runTests(int minHistorySize, boolean testAllTargets) {
/* 135 */     Map<String, CombatLogData> logs = loadCombatLogs();
/*     */     
/* 137 */     if (logs.isEmpty()) {
/*     */       
/* 139 */       log("[PredictionTester] No combat log data found. Cannot run tests.");
/*     */       
/*     */       return;
/*     */     } 
/* 143 */     log("[PredictionTester] Starting prediction accuracy tests...");
/* 144 */     log("[PredictionTester] Minimum history size: " + minHistorySize);
/* 145 */     log("[PredictionTester] =========================================");
/*     */ 
/*     */     
/* 148 */     resetStats();
/*     */ 
/*     */     
/* 151 */     for (Map.Entry<String, CombatLogData> entry : logs.entrySet()) {
/*     */       
/* 153 */       String targetRSN = entry.getKey();
/* 154 */       CombatLogData logData = entry.getValue();
/*     */       
/* 156 */       List<AttackContext> history = logData.getAttackHistory();
/*     */       
/* 158 */       if (history == null || history.size() < minHistorySize + 1) {
/*     */         
/* 160 */         if (testAllTargets)
/*     */         {
/* 162 */           log("[PredictionTester] Skipping " + targetRSN + " - insufficient data (" + (
/* 163 */               (history != null) ? history.size() : 0) + " attacks, need " + minHistorySize + 1 + ")");
/*     */         }
/*     */         
/*     */         continue;
/*     */       } 
/* 168 */       log("[PredictionTester] Testing " + targetRSN + " (" + history.size() + " attacks)");
/* 169 */       testTarget(targetRSN, logData, minHistorySize);
/*     */     } 
/*     */ 
/*     */     
/* 173 */     printResults();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void testTarget(String targetRSN, CombatLogData logData, int minHistorySize) {
/* 181 */     List<AttackContext> fullHistory = logData.getAttackHistory();
/*     */ 
/*     */     
/* 184 */     TestStats targetStat = new TestStats();
/* 185 */     this.targetStats.put(targetRSN, targetStat);
/*     */ 
/*     */     
/* 188 */     CombatLogData testLogData = new CombatLogData();
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 193 */     for (int i = minHistorySize; i < fullHistory.size() - 1; i++) {
/*     */ 
/*     */       
/* 196 */       AttackContext currentContext = fullHistory.get(i);
/*     */ 
/*     */       
/* 199 */       AttackContext actualNextContext = fullHistory.get(i + 1);
/* 200 */       String actualNextAttack = actualNextContext.getAttackStyle();
/*     */       
/* 202 */       if (actualNextAttack != null && !actualNextAttack.trim().isEmpty()) {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */         
/* 208 */         testLogData.getAttackHistory().clear();
/* 209 */         for (int j = 0; j <= i; j++)
/*     */         {
/* 211 */           testLogData.addAttackToHistory(fullHistory.get(j), 5000);
/*     */         }
/*     */ 
/*     */         
/* 215 */         List<PredictionResult> predictions = this.predictor.getTop5Predictions(currentContext, testLogData);
/*     */         
/* 217 */         if (predictions != null && !predictions.isEmpty()) {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */           
/* 223 */           String predictedAttack = ((PredictionResult)predictions.get(0)).getAttackType();
/*     */ 
/*     */           
/* 226 */           boolean isCorrect = (predictedAttack != null && predictedAttack.equalsIgnoreCase(actualNextAttack));
/*     */ 
/*     */           
/* 229 */           boolean inTop3 = false;
/* 230 */           for (int k = 0; k < Math.min(3, predictions.size()); k++) {
/*     */             
/* 232 */             if (((PredictionResult)predictions.get(k)).getAttackType().equalsIgnoreCase(actualNextAttack)) {
/*     */               
/* 234 */               inTop3 = true;
/*     */               
/*     */               break;
/*     */             } 
/*     */           } 
/*     */           
/* 240 */           String predictionMethod = ((PredictionResult)predictions.get(0)).getMethod();
/*     */ 
/*     */           
/* 243 */           String freezeState = currentContext.getFreezeState();
/* 244 */           if (freezeState == null || freezeState.trim().isEmpty())
/*     */           {
/* 246 */             freezeState = "unknown";
/*     */           }
/*     */ 
/*     */           
/* 250 */           updateStats(targetRSN, freezeState, predictionMethod, predictedAttack, actualNextAttack, isCorrect, inTop3);
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void updateStats(String targetRSN, String freezeState, String method, String predicted, String actual, boolean isCorrect, boolean inTop3) {
/* 260 */     this.totalTests++;
/*     */     
/* 262 */     if (isCorrect)
/*     */     {
/* 264 */       this.correctPredictions++;
/*     */     }
/*     */     
/* 267 */     if (inTop3)
/*     */     {
/* 269 */       this.correctTop3++;
/*     */     }
/*     */ 
/*     */     
/* 273 */     TestStats freezeStat = this.freezeStateStats.get(freezeState);
/* 274 */     if (freezeStat == null) {
/*     */       
/* 276 */       freezeStat = new TestStats();
/* 277 */       this.freezeStateStats.put(freezeState, freezeStat);
/*     */     } 
/* 279 */     freezeStat.total++;
/* 280 */     if (isCorrect) freezeStat.correct++; 
/* 281 */     if (inTop3) freezeStat.correctTop3++; 
/* 282 */     freezeStat.predictedCounts.put(predicted, Integer.valueOf(((Integer)freezeStat.predictedCounts.getOrDefault(predicted, Integer.valueOf(0))).intValue() + 1));
/* 283 */     freezeStat.actualCounts.put(actual, Integer.valueOf(((Integer)freezeStat.actualCounts.getOrDefault(actual, Integer.valueOf(0))).intValue() + 1));
/*     */ 
/*     */     
/* 286 */     TestStats methodStat = this.methodStats.get(method);
/* 287 */     if (methodStat == null) {
/*     */       
/* 289 */       methodStat = new TestStats();
/* 290 */       this.methodStats.put(method, methodStat);
/*     */     } 
/* 292 */     methodStat.total++;
/* 293 */     if (isCorrect) methodStat.correct++; 
/* 294 */     if (inTop3) methodStat.correctTop3++; 
/* 295 */     methodStat.predictedCounts.put(predicted, Integer.valueOf(((Integer)methodStat.predictedCounts.getOrDefault(predicted, Integer.valueOf(0))).intValue() + 1));
/* 296 */     methodStat.actualCounts.put(actual, Integer.valueOf(((Integer)methodStat.actualCounts.getOrDefault(actual, Integer.valueOf(0))).intValue() + 1));
/*     */ 
/*     */     
/* 299 */     TestStats targetStat = this.targetStats.get(targetRSN);
/* 300 */     if (targetStat != null) {
/*     */       
/* 302 */       targetStat.total++;
/* 303 */       if (isCorrect) targetStat.correct++; 
/* 304 */       if (inTop3) targetStat.correctTop3++; 
/* 305 */       targetStat.predictedCounts.put(predicted, Integer.valueOf(((Integer)targetStat.predictedCounts.getOrDefault(predicted, Integer.valueOf(0))).intValue() + 1));
/* 306 */       targetStat.actualCounts.put(actual, Integer.valueOf(((Integer)targetStat.actualCounts.getOrDefault(actual, Integer.valueOf(0))).intValue() + 1));
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void resetStats() {
/* 315 */     this.totalTests = 0;
/* 316 */     this.correctPredictions = 0;
/* 317 */     this.correctTop3 = 0;
/* 318 */     this.freezeStateStats.clear();
/* 319 */     this.methodStats.clear();
/* 320 */     this.targetStats.clear();
/*     */ 
/*     */     
/* 323 */     this.freezeStateStats.put("bothFrozen", new TestStats());
/* 324 */     this.freezeStateStats.put("bothUnfrozen", new TestStats());
/* 325 */     this.freezeStateStats.put("targetFrozenWeUnfrozen", new TestStats());
/* 326 */     this.freezeStateStats.put("weFrozenTargetUnfrozen", new TestStats());
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void printResults() {
/* 334 */     log("");
/* 335 */     log("[PredictionTester] =========================================");
/* 336 */     log("[PredictionTester] PREDICTION ACCURACY TEST RESULTS");
/* 337 */     log("[PredictionTester] =========================================");
/* 338 */     log("");
/*     */ 
/*     */     
/* 341 */     double overallAccuracy = (this.totalTests > 0) ? (this.correctPredictions * 100.0D / this.totalTests) : 0.0D;
/* 342 */     double top3Accuracy = (this.totalTests > 0) ? (this.correctTop3 * 100.0D / this.totalTests) : 0.0D;
/*     */     
/* 344 */     log("[PredictionTester] OVERALL RESULTS:");
/* 345 */     log("[PredictionTester]   Total Tests: " + this.totalTests);
/* 346 */     log("[PredictionTester]   Correct (Top 1): " + this.correctPredictions + " (" + String.format("%.2f", new Object[] { Double.valueOf(overallAccuracy) }) + "%)");
/* 347 */     log("[PredictionTester]   Correct (Top 3): " + this.correctTop3 + " (" + String.format("%.2f", new Object[] { Double.valueOf(top3Accuracy) }) + "%)");
/* 348 */     log("");
/*     */ 
/*     */     
/* 351 */     log("[PredictionTester] PER FREEZE STATE:");
/* 352 */     for (Map.Entry<String, TestStats> entry : this.freezeStateStats.entrySet()) {
/*     */       
/* 354 */       String freezeState = entry.getKey();
/* 355 */       TestStats stats = entry.getValue();
/*     */       
/* 357 */       if (stats.total > 0) {
/*     */         
/* 359 */         log("[PredictionTester]   " + freezeState + ":");
/* 360 */         log("[PredictionTester]     Tests: " + stats.total);
/* 361 */         log("[PredictionTester]     Top 1 Accuracy: " + String.format("%.2f", new Object[] { Double.valueOf(stats.getAccuracy()) }) + "%");
/* 362 */         log("[PredictionTester]     Top 3 Accuracy: " + String.format("%.2f", new Object[] { Double.valueOf(stats.getTop3Accuracy()) }) + "%");
/*     */ 
/*     */         
/* 365 */         log("[PredictionTester]     Actual Distribution: " + formatDistribution(stats.actualCounts));
/* 366 */         log("[PredictionTester]     Predicted Distribution: " + formatDistribution(stats.predictedCounts));
/*     */       } 
/*     */     } 
/* 369 */     log("");
/*     */ 
/*     */     
/* 372 */     log("[PredictionTester] PER PREDICTION METHOD:");
/* 373 */     for (Map.Entry<String, TestStats> entry : this.methodStats.entrySet()) {
/*     */       
/* 375 */       String method = entry.getKey();
/* 376 */       TestStats stats = entry.getValue();
/*     */       
/* 378 */       if (stats.total > 0) {
/*     */         
/* 380 */         log("[PredictionTester]   " + method + ":");
/* 381 */         log("[PredictionTester]     Tests: " + stats.total);
/* 382 */         log("[PredictionTester]     Top 1 Accuracy: " + String.format("%.2f", new Object[] { Double.valueOf(stats.getAccuracy()) }) + "%");
/* 383 */         log("[PredictionTester]     Top 3 Accuracy: " + String.format("%.2f", new Object[] { Double.valueOf(stats.getTop3Accuracy()) }) + "%");
/*     */       } 
/*     */     } 
/* 386 */     log("");
/*     */ 
/*     */     
/* 389 */     log("[PredictionTester] PER TARGET (Top 10 by test count):");
/* 390 */     List<Map.Entry<String, TestStats>> sortedTargets = new ArrayList<>(this.targetStats.entrySet());
/* 391 */     sortedTargets.sort((a, b) -> Integer.compare(((TestStats)b.getValue()).total, ((TestStats)a.getValue()).total));
/*     */     
/* 393 */     int count = 0;
/* 394 */     for (Map.Entry<String, TestStats> entry : sortedTargets) {
/*     */       
/* 396 */       if (count >= 10)
/*     */         break; 
/* 398 */       String target = entry.getKey();
/* 399 */       TestStats stats = entry.getValue();
/*     */       
/* 401 */       log("[PredictionTester]   " + target + ":");
/* 402 */       log("[PredictionTester]     Tests: " + stats.total);
/* 403 */       log("[PredictionTester]     Top 1 Accuracy: " + String.format("%.2f", new Object[] { Double.valueOf(stats.getAccuracy()) }) + "%");
/* 404 */       log("[PredictionTester]     Top 3 Accuracy: " + String.format("%.2f", new Object[] { Double.valueOf(stats.getTop3Accuracy()) }) + "%");
/* 405 */       count++;
/*     */     } 
/* 407 */     log("");
/*     */     
/* 409 */     log("[PredictionTester] =========================================");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private String formatDistribution(Map<String, Integer> distribution) {
/* 417 */     if (distribution.isEmpty())
/*     */     {
/* 419 */       return "N/A";
/*     */     }
/*     */     
/* 422 */     int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
/* 423 */     if (total == 0)
/*     */     {
/* 425 */       return "N/A";
/*     */     }
/*     */     
/* 428 */     List<String> parts = new ArrayList<>();
/* 429 */     for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
/*     */       
/* 431 */       double percentage = ((Integer)entry.getValue()).intValue() * 100.0D / total;
/* 432 */       parts.add((String)entry.getKey() + ": " + (String)entry.getKey() + "%");
/*     */     } 
/*     */     
/* 435 */     return String.join(", ", (Iterable)parts);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private static AttackTimerConfig createTestConfig() {
/* 444 */     return new AttackTimerConfig()
/*     */       {
/*     */         
/*     */         public int predictionHistorySize()
/*     */         {
/* 449 */           return 5000;
/*     */         }
/*     */       };
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static void main(String[] args) {
/* 461 */     int minHistorySize = 5;
/* 462 */     boolean testAllTargets = false;
/*     */ 
/*     */     
/* 465 */     if (args.length > 0) {
/*     */       
/*     */       try {
/*     */         
/* 469 */         minHistorySize = Integer.parseInt(args[0]);
/*     */       }
/* 471 */       catch (NumberFormatException e) {
/*     */         
/* 473 */         System.err.println("Invalid minHistorySize argument, using default: 5");
/*     */       } 
/*     */     }
/*     */     
/* 477 */     if (args.length > 1)
/*     */     {
/* 479 */       testAllTargets = Boolean.parseBoolean(args[1]);
/*     */     }
/*     */     
/* 482 */     AttackTimerConfig config = createTestConfig();
/* 483 */     PredictionTester tester = new PredictionTester(config);
/*     */     
/* 485 */     System.out.println("Starting prediction accuracy tests...");
/* 486 */     System.out.println("Min history size: " + minHistorySize);
/* 487 */     System.out.println("Test all targets: " + testAllTargets);
/* 488 */     System.out.println();
/*     */     
/* 490 */     tester.runTests(minHistorySize, testAllTargets);
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/PredictionTester.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */