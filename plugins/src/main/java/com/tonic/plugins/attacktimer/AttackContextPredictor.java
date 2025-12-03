/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.HashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ 
/*     */ 
/*     */ public class AttackContextPredictor
/*     */ {
/*     */   private final AttackTimerConfig config;
/*  14 */   private String lastCacheKey = null;
/*  15 */   private List<PredictionResult> cachedPredictions = null;
/*  16 */   private int cachedHistorySize = -1;
/*     */ 
/*     */   
/*     */   public AttackContextPredictor(AttackTimerConfig config) {
/*  20 */     this.config = config;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private String generateCacheKey(AttackContext context, int historySize) {
/*  29 */     if (context == null)
/*     */     {
/*  31 */       return null;
/*     */     }
/*     */ 
/*     */     
/*  35 */     int hpRange = context.getTargetHP() / 10 * 10;
/*     */ 
/*     */     
/*  38 */     int specRange = context.getTargetSpec() / 10 * 10;
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*  43 */     int playerFreezeRange = (context.getPlayerFreezeTicksRemaining() >= 0) ? (context.getPlayerFreezeTicksRemaining() / 5 * 5) : -1;
/*     */     
/*  45 */     int opponentFreezeRange = (context.getOpponentFreezeTicksRemaining() >= 0) ? (context.getOpponentFreezeTicksRemaining() / 5 * 5) : -1;
/*     */ 
/*     */     
/*  48 */     return String.format("%s|%d|%s|%s|%d|%d|%d|%d", new Object[] {
/*  49 */           (context.getOverheadPrayer() != null) ? context.getOverheadPrayer() : "none", 
/*  50 */           Integer.valueOf(hpRange), 
/*  51 */           (context.getFreezeState() != null) ? context.getFreezeState() : "none", 
/*  52 */           (context.getWeapon() != null) ? context.getWeapon() : "unknown", 
/*  53 */           Integer.valueOf(specRange), 
/*  54 */           Integer.valueOf(playerFreezeRange), 
/*  55 */           Integer.valueOf(opponentFreezeRange), 
/*  56 */           Integer.valueOf(historySize)
/*     */         });
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void clearCache() {
/*  64 */     this.lastCacheKey = null;
/*  65 */     this.cachedPredictions = null;
/*  66 */     this.cachedHistorySize = -1;
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
/*     */   public List<PredictionResult> getTop5Predictions(AttackContext currentContext, CombatLogData logData) {
/*  78 */     if (currentContext == null || logData == null || logData.getAttackHistory().isEmpty())
/*     */     {
/*  80 */       return getDefaultPredictions();
/*     */     }
/*     */     
/*  83 */     List<AttackContext> history = logData.getAttackHistory();
/*  84 */     int currentHistorySize = history.size();
/*     */ 
/*     */     
/*  87 */     String cacheKey = generateCacheKey(currentContext, currentHistorySize);
/*     */ 
/*     */     
/*  90 */     if (cacheKey != null && cacheKey.equals(this.lastCacheKey) && this.cachedPredictions != null && this.cachedHistorySize == currentHistorySize)
/*     */     {
/*     */       
/*  93 */       return new ArrayList<>(this.cachedPredictions);
/*     */     }
/*     */ 
/*     */     
/*  97 */     Map<String, Map<String, Integer>> patternFrequency = logData.getPatternFrequency();
/*     */ 
/*     */     
/* 100 */     Map<String, Double> scores = new HashMap<>();
/* 101 */     Map<String, String> methods = new HashMap<>();
/*     */ 
/*     */     
/* 104 */     scores.put("melee", Double.valueOf(0.0D));
/* 105 */     scores.put("ranged", Double.valueOf(0.0D));
/* 106 */     scores.put("magic", Double.valueOf(0.0D));
/* 107 */     methods.put("melee", "no_data");
/* 108 */     methods.put("ranged", "no_data");
/* 109 */     methods.put("magic", "no_data");
/*     */ 
/*     */     
/* 112 */     double exactMatchScore = calculateExactMatchScore(currentContext, history, scores, methods);
/*     */ 
/*     */     
/* 115 */     if (exactMatchScore == 0.0D)
/*     */     {
/* 117 */       calculatePartialMatchScore(currentContext, history, scores, methods);
/*     */     }
/*     */ 
/*     */     
/* 121 */     calculatePatternFrequencyScore(currentContext, patternFrequency, scores, methods);
/*     */ 
/*     */     
/* 124 */     calculateSequenceScore(currentContext, history, scores, methods);
/*     */ 
/*     */     
/* 127 */     List<PredictionResult> predictions = new ArrayList<>();
/* 128 */     for (String attackType : Arrays.<String>asList(new String[] { "melee", "ranged", "magic" })) {
/*     */       
/* 130 */       double score = ((Double)scores.getOrDefault(attackType, Double.valueOf(0.0D))).doubleValue();
/* 131 */       String method = methods.getOrDefault(attackType, "no_data");
/* 132 */       predictions.add(new PredictionResult(attackType, score, method));
/*     */     } 
/*     */ 
/*     */     
/* 136 */     Collections.sort(predictions);
/*     */ 
/*     */     
/* 139 */     List<PredictionResult> result = predictions.subList(0, Math.min(5, predictions.size()));
/*     */ 
/*     */     
/* 142 */     this.lastCacheKey = cacheKey;
/* 143 */     this.cachedPredictions = new ArrayList<>(result);
/* 144 */     this.cachedHistorySize = currentHistorySize;
/*     */     
/* 146 */     return result;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private double calculateExactMatchScore(AttackContext currentContext, List<AttackContext> history, Map<String, Double> scores, Map<String, String> methods) {
/* 156 */     double totalWeight = 0.0D;
/* 157 */     Map<String, Double> weightedCounts = new HashMap<>();
/* 158 */     weightedCounts.put("melee", Double.valueOf(0.0D));
/* 159 */     weightedCounts.put("ranged", Double.valueOf(0.0D));
/* 160 */     weightedCounts.put("magic", Double.valueOf(0.0D));
/*     */ 
/*     */ 
/*     */     
/* 164 */     int skipLast = 1;
/* 165 */     for (int i = 0; i < history.size() - 1 - skipLast; i++) {
/*     */       
/* 167 */       AttackContext historicalContext = history.get(i);
/* 168 */       AttackContext nextContext = history.get(i + 1);
/*     */ 
/*     */       
/* 171 */       if (matchesContextWithFreezeTicks(currentContext, historicalContext, 10, 10, 2, 5)) {
/*     */         
/* 173 */         String nextAttack = nextContext.getAttackStyle();
/* 174 */         if (nextAttack != null && weightedCounts.containsKey(nextAttack)) {
/*     */ 
/*     */           
/* 177 */           double weight = 1.0D;
/* 178 */           weightedCounts.put(nextAttack, Double.valueOf(((Double)weightedCounts.get(nextAttack)).doubleValue() + weight));
/* 179 */           totalWeight += weight;
/*     */         } 
/*     */       } 
/*     */     } 
/*     */     
/* 184 */     if (totalWeight > 0.0D)
/*     */     {
/* 186 */       for (String attackType : weightedCounts.keySet()) {
/*     */         
/* 188 */         double score = ((Double)weightedCounts.get(attackType)).doubleValue() / totalWeight * 100.0D;
/* 189 */         if (score > ((Double)scores.get(attackType)).doubleValue()) {
/*     */           
/* 191 */           scores.put(attackType, Double.valueOf(score));
/* 192 */           methods.put(attackType, "exact_match");
/*     */         } 
/*     */       } 
/*     */     }
/*     */     
/* 197 */     return totalWeight;
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
/*     */   private double calculateContextSimilarity(AttackContext current, AttackContext historical) {
/* 213 */     double totalScore = 0.0D;
/* 214 */     double totalWeight = 0.0D;
/*     */ 
/*     */     
/* 217 */     double weaponScore = 0.0D;
/* 218 */     if (current.getWeapon() != null && historical.getWeapon() != null) {
/*     */       
/* 220 */       weaponScore = current.getWeapon().equals(historical.getWeapon()) ? 1.0D : 0.0D;
/*     */     }
/* 222 */     else if (current.getWeapon() == null && historical.getWeapon() == null) {
/*     */       
/* 224 */       weaponScore = 1.0D;
/*     */     }
/*     */     else {
/*     */       
/* 228 */       weaponScore = 0.5D;
/*     */     } 
/* 230 */     totalScore += weaponScore * 0.335D;
/* 231 */     totalWeight += 0.335D;
/*     */ 
/*     */     
/* 234 */     double freezeStateScore = 0.0D;
/* 235 */     if (current.getFreezeState() != null && historical.getFreezeState() != null) {
/*     */       
/* 237 */       freezeStateScore = current.getFreezeState().equals(historical.getFreezeState()) ? 1.0D : 0.0D;
/*     */     }
/* 239 */     else if (current.getFreezeState() == null && historical.getFreezeState() == null) {
/*     */       
/* 241 */       freezeStateScore = 1.0D;
/*     */     }
/*     */     else {
/*     */       
/* 245 */       freezeStateScore = 0.0D;
/*     */     } 
/* 247 */     totalScore += freezeStateScore * 0.213D;
/* 248 */     totalWeight += 0.213D;
/*     */ 
/*     */     
/* 251 */     double distanceScore = calculateDistanceSimilarity(current, historical);
/* 252 */     totalScore += distanceScore * 0.204D;
/* 253 */     totalWeight += 0.204D;
/*     */ 
/*     */     
/* 256 */     double hpScore = 0.0D;
/* 257 */     if (current.getTargetHP() >= 0 && historical.getTargetHP() >= 0) {
/*     */       
/* 259 */       int hpDiff = Math.abs(current.getTargetHP() - historical.getTargetHP());
/* 260 */       if (hpDiff <= 5) {
/*     */         
/* 262 */         hpScore = 1.0D;
/*     */       }
/* 264 */       else if (hpDiff <= 10) {
/*     */         
/* 266 */         hpScore = 0.8D;
/*     */       }
/* 268 */       else if (hpDiff <= 20) {
/*     */         
/* 270 */         hpScore = 0.5D;
/*     */       }
/*     */       else {
/*     */         
/* 274 */         hpScore = 0.2D;
/*     */       } 
/*     */     } 
/* 277 */     totalScore += hpScore * 0.12D;
/* 278 */     totalWeight += 0.12D;
/*     */ 
/*     */     
/* 281 */     double specScore = 0.0D;
/* 282 */     if (current.getTargetSpec() >= 0 && historical.getTargetSpec() >= 0) {
/*     */       
/* 284 */       int specDiff = Math.abs(current.getTargetSpec() - historical.getTargetSpec());
/* 285 */       if (specDiff <= 5) {
/*     */         
/* 287 */         specScore = 1.0D;
/*     */       }
/* 289 */       else if (specDiff <= 10) {
/*     */         
/* 291 */         specScore = 0.8D;
/*     */       }
/* 293 */       else if (specDiff <= 20) {
/*     */         
/* 295 */         specScore = 0.5D;
/*     */       }
/*     */       else {
/*     */         
/* 299 */         specScore = 0.2D;
/*     */       } 
/*     */     } 
/* 302 */     totalScore += specScore * 0.064D;
/* 303 */     totalWeight += 0.064D;
/*     */ 
/*     */     
/* 306 */     double prayerScore = 0.0D;
/* 307 */     if (current.getOverheadPrayer() != null && historical.getOverheadPrayer() != null) {
/*     */       
/* 309 */       prayerScore = current.getOverheadPrayer().equals(historical.getOverheadPrayer()) ? 1.0D : 0.5D;
/*     */     }
/* 311 */     else if (current.getOverheadPrayer() == null && historical.getOverheadPrayer() == null) {
/*     */       
/* 313 */       prayerScore = 1.0D;
/*     */     }
/*     */     else {
/*     */       
/* 317 */       prayerScore = 0.8D;
/*     */     } 
/* 319 */     totalScore += prayerScore * 0.045D;
/* 320 */     totalWeight += 0.045D;
/*     */ 
/*     */     
/* 323 */     double freezeTickSimilarity = calculateFreezeTickSimilarity(current, historical);
/* 324 */     totalScore += freezeTickSimilarity * 0.018D;
/* 325 */     totalWeight += 0.018D;
/*     */ 
/*     */     
/* 328 */     if (totalWeight > 0.0D)
/*     */     {
/* 330 */       return totalScore / totalWeight;
/*     */     }
/* 332 */     return 0.0D;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private double calculateDistanceSimilarity(AttackContext current, AttackContext historical) {
/* 341 */     if (current.getDistance() < 0 || historical.getDistance() < 0)
/*     */     {
/* 343 */       return 0.5D;
/*     */     }
/*     */     
/* 346 */     int distanceDiff = Math.abs(current.getDistance() - historical.getDistance());
/*     */     
/* 348 */     if (distanceDiff == 0)
/*     */     {
/* 350 */       return 1.0D;
/*     */     }
/* 352 */     if (distanceDiff <= 1)
/*     */     {
/* 354 */       return 0.95D;
/*     */     }
/* 356 */     if (distanceDiff <= 2)
/*     */     {
/* 358 */       return 0.85D;
/*     */     }
/* 360 */     if (distanceDiff <= 4)
/*     */     {
/* 362 */       return 0.7D;
/*     */     }
/* 364 */     if (distanceDiff <= 6)
/*     */     {
/* 366 */       return 0.5D;
/*     */     }
/* 368 */     if (distanceDiff <= 10)
/*     */     {
/* 370 */       return 0.3D;
/*     */     }
/*     */ 
/*     */     
/* 374 */     return 0.1D;
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
/*     */   private boolean matchesContextWithFreezeTicks(AttackContext current, AttackContext historical, int hpTolerance, int specTolerance, int distanceTolerance, int freezeTickTolerance) {
/* 387 */     double similarity = calculateContextSimilarity(current, historical);
/*     */ 
/*     */ 
/*     */     
/* 391 */     if (current.getFreezeState() != null && historical.getFreezeState() != null) {
/*     */       
/* 393 */       if (!current.getFreezeState().equals(historical.getFreezeState()))
/*     */       {
/* 395 */         return false;
/*     */       }
/*     */     }
/* 398 */     else if (current.getFreezeState() != null || historical.getFreezeState() != null) {
/*     */       
/* 400 */       return false;
/*     */     } 
/*     */ 
/*     */     
/* 404 */     return (similarity >= 0.85D);
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
/*     */   private void calculatePartialMatchScore(AttackContext currentContext, List<AttackContext> history, Map<String, Double> scores, Map<String, String> methods) {
/* 416 */     double totalWeight = 0.0D;
/* 417 */     Map<String, Double> weightedCounts = new HashMap<>();
/* 418 */     weightedCounts.put("melee", Double.valueOf(0.0D));
/* 419 */     weightedCounts.put("ranged", Double.valueOf(0.0D));
/* 420 */     weightedCounts.put("magic", Double.valueOf(0.0D));
/*     */ 
/*     */     
/* 423 */     int skipLast = 1;
/* 424 */     for (int i = 0; i < history.size() - 1 - skipLast; i++) {
/*     */       
/* 426 */       AttackContext historicalContext = history.get(i);
/* 427 */       AttackContext nextContext = history.get(i + 1);
/*     */       
/* 429 */       String nextAttack = nextContext.getAttackStyle();
/* 430 */       if (nextAttack != null && weightedCounts.containsKey(nextAttack)) {
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
/* 444 */         double similarity = calculateContextSimilarity(currentContext, historicalContext);
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
/* 456 */         if (similarity >= 0.4D) {
/*     */ 
/*     */ 
/*     */ 
/*     */           
/* 461 */           double weight = 0.0D;
/* 462 */           String method = "";
/*     */           
/* 464 */           if (similarity >= 0.9D) {
/*     */             
/* 466 */             weight = 1.0D;
/* 467 */             method = "partial_match_excellent";
/*     */           }
/* 469 */           else if (similarity >= 0.8D) {
/*     */             
/* 471 */             weight = 0.9D;
/* 472 */             method = "partial_match_very_good";
/*     */           }
/* 474 */           else if (similarity >= 0.7D) {
/*     */             
/* 476 */             weight = 0.8D;
/* 477 */             method = "partial_match_good";
/*     */           }
/* 479 */           else if (similarity >= 0.6D) {
/*     */             
/* 481 */             weight = 0.7D;
/* 482 */             method = "partial_match_decent";
/*     */           }
/* 484 */           else if (similarity >= 0.5D) {
/*     */             
/* 486 */             weight = 0.6D;
/* 487 */             method = "partial_match_okay";
/*     */           }
/*     */           else {
/*     */             
/* 491 */             weight = 0.5D;
/* 492 */             method = "partial_match_poor";
/*     */           } 
/*     */           
/* 495 */           if (weight > 0.0D) {
/*     */             
/* 497 */             weightedCounts.put(nextAttack, Double.valueOf(((Double)weightedCounts.get(nextAttack)).doubleValue() + weight));
/* 498 */             totalWeight += weight;
/*     */           } 
/*     */         } 
/*     */       } 
/* 502 */     }  if (totalWeight > 0.0D)
/*     */     {
/* 504 */       for (String attackType : weightedCounts.keySet()) {
/*     */         
/* 506 */         double score = ((Double)weightedCounts.get(attackType)).doubleValue() / totalWeight * 100.0D;
/* 507 */         if (score > ((Double)scores.get(attackType)).doubleValue()) {
/*     */           
/* 509 */           scores.put(attackType, Double.valueOf(score));
/* 510 */           methods.put(attackType, "partial_match");
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
/*     */   
/*     */   private void calculatePatternFrequencyScore(AttackContext currentContext, Map<String, Map<String, Integer>> patternFrequency, Map<String, Double> scores, Map<String, String> methods) {
/* 523 */     String contextKey = currentContext.generateContextKey();
/* 524 */     Map<String, Integer> nextAttackCounts = patternFrequency.get(contextKey);
/*     */     
/* 526 */     if (nextAttackCounts == null || nextAttackCounts.isEmpty()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 531 */     int total = nextAttackCounts.values().stream().mapToInt(Integer::intValue).sum();
/* 532 */     if (total == 0) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 537 */     for (String attackType : Arrays.<String>asList(new String[] { "melee", "ranged", "magic" })) {
/*     */       
/* 539 */       int count = ((Integer)nextAttackCounts.getOrDefault(attackType, Integer.valueOf(0))).intValue();
/* 540 */       double score = count * 100.0D / total;
/*     */       
/* 542 */       if (score > ((Double)scores.get(attackType)).doubleValue()) {
/*     */         
/* 544 */         scores.put(attackType, Double.valueOf(score));
/* 545 */         methods.put(attackType, "pattern_frequency");
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void calculateSequenceScore(AttackContext currentContext, List<AttackContext> history, Map<String, Double> scores, Map<String, String> methods) {
/* 558 */     if (history.size() < 3) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 565 */     int historySize = history.size();
/* 566 */     int startIndex = Math.max(0, historySize - 6);
/* 567 */     int endIndex = historySize - 1;
/*     */     
/* 569 */     if (startIndex >= endIndex || endIndex <= 0) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 574 */     List<AttackContext> recentHistory = history.subList(startIndex, endIndex);
/*     */     
/* 576 */     if (recentHistory.isEmpty()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 582 */     double totalWeight = 0.0D;
/* 583 */     Map<String, Double> weightedCounts = new HashMap<>();
/* 584 */     weightedCounts.put("melee", Double.valueOf(0.0D));
/* 585 */     weightedCounts.put("ranged", Double.valueOf(0.0D));
/* 586 */     weightedCounts.put("magic", Double.valueOf(0.0D));
/*     */ 
/*     */     
/* 589 */     for (int seqLen = 2; seqLen <= 4 && seqLen <= recentHistory.size(); seqLen++) {
/*     */ 
/*     */       
/* 592 */       int seqStart = recentHistory.size() - seqLen;
/* 593 */       if (seqStart >= 0) {
/*     */ 
/*     */ 
/*     */         
/* 597 */         List<AttackContext> seqContexts = recentHistory.subList(seqStart, recentHistory.size());
/* 598 */         AttackContext lastSeqContext = seqContexts.get(seqContexts.size() - 1);
/*     */ 
/*     */         
/* 601 */         StringBuilder seqBuilder = new StringBuilder();
/* 602 */         for (AttackContext ctx : seqContexts) {
/*     */           
/* 604 */           if (ctx.getAttackStyle() != null)
/*     */           {
/* 606 */             seqBuilder.append(ctx.getAttackStyle()).append(",");
/*     */           }
/*     */         } 
/* 609 */         if (seqBuilder.length() != 0) {
/*     */ 
/*     */ 
/*     */           
/* 613 */           String currentSeq = seqBuilder.substring(0, seqBuilder.length() - 1);
/*     */ 
/*     */ 
/*     */           
/* 617 */           for (int i = 0; i < history.size() - seqLen - 1; i++) {
/*     */ 
/*     */             
/* 620 */             StringBuilder histSeqBuilder = new StringBuilder();
/* 621 */             for (int j = 0; j < seqLen; j++) {
/*     */               
/* 623 */               AttackContext ctx = history.get(i + j);
/* 624 */               if (ctx.getAttackStyle() != null)
/*     */               {
/* 626 */                 histSeqBuilder.append(ctx.getAttackStyle()).append(",");
/*     */               }
/*     */             } 
/* 629 */             if (histSeqBuilder.length() != 0) {
/*     */ 
/*     */ 
/*     */               
/* 633 */               String histSeq = histSeqBuilder.substring(0, histSeqBuilder.length() - 1);
/*     */ 
/*     */               
/* 636 */               if (currentSeq.equals(histSeq)) {
/*     */ 
/*     */                 
/* 639 */                 AttackContext histLastSeqContext = history.get(i + seqLen - 1);
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
/* 650 */                 double contextSimilarity = calculateContextSimilarity(lastSeqContext, histLastSeqContext);
/*     */ 
/*     */                 
/* 653 */                 if (contextSimilarity >= 0.4D)
/*     */                 
/*     */                 { 
/*     */ 
/*     */ 
/*     */ 
/*     */                   
/* 660 */                   double contextWeight = 0.4D + contextSimilarity * 0.6D;
/*     */ 
/*     */                   
/* 663 */                   AttackContext nextContext = history.get(i + seqLen);
/* 664 */                   String nextAttack = nextContext.getAttackStyle();
/* 665 */                   if (nextAttack != null && weightedCounts.containsKey(nextAttack))
/*     */                   
/*     */                   { 
/*     */                     
/* 669 */                     double weight = 0.25D * contextWeight;
/* 670 */                     weightedCounts.put(nextAttack, Double.valueOf(((Double)weightedCounts.get(nextAttack)).doubleValue() + weight));
/* 671 */                     totalWeight += weight; }  } 
/*     */               } 
/*     */             } 
/*     */           } 
/*     */         } 
/*     */       } 
/* 677 */     }  if (totalWeight > 0.0D)
/*     */     {
/* 679 */       for (String attackType : weightedCounts.keySet()) {
/*     */         
/* 681 */         double score = ((Double)weightedCounts.get(attackType)).doubleValue() / totalWeight * 100.0D;
/* 682 */         if (score > ((Double)scores.get(attackType)).doubleValue()) {
/*     */           
/* 684 */           scores.put(attackType, Double.valueOf(score));
/* 685 */           methods.put(attackType, "sequence");
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
/*     */   
/*     */   private double calculateFreezeTickSimilarity(AttackContext current, AttackContext historical) {
/* 698 */     double similarity = 1.0D;
/*     */ 
/*     */     
/* 701 */     if (current.getPlayerFreezeTicksRemaining() >= 0 && historical.getPlayerFreezeTicksRemaining() >= 0) {
/*     */       
/* 703 */       int tickDiff = Math.abs(current.getPlayerFreezeTicksRemaining() - historical.getPlayerFreezeTicksRemaining());
/* 704 */       if (tickDiff <= 2)
/*     */       {
/*     */         
/* 707 */         similarity *= 1.0D;
/*     */       }
/* 709 */       else if (tickDiff <= 5)
/*     */       {
/*     */         
/* 712 */         similarity *= 0.9D;
/*     */       }
/* 714 */       else if (tickDiff <= 10)
/*     */       {
/*     */         
/* 717 */         similarity *= 0.7D;
/*     */       }
/* 719 */       else if (tickDiff <= 15)
/*     */       {
/*     */         
/* 722 */         similarity *= 0.5D;
/*     */       
/*     */       }
/*     */       else
/*     */       {
/* 727 */         similarity *= 0.3D;
/*     */       }
/*     */     
/* 730 */     } else if (current.getPlayerFreezeTicksRemaining() >= 0 || historical.getPlayerFreezeTicksRemaining() >= 0) {
/*     */ 
/*     */       
/* 733 */       similarity *= 0.5D;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 738 */     if (current.getOpponentFreezeTicksRemaining() >= 0 && historical.getOpponentFreezeTicksRemaining() >= 0) {
/*     */       
/* 740 */       int tickDiff = Math.abs(current.getOpponentFreezeTicksRemaining() - historical.getOpponentFreezeTicksRemaining());
/* 741 */       if (tickDiff <= 2)
/*     */       {
/*     */         
/* 744 */         similarity *= 1.0D;
/*     */       }
/* 746 */       else if (tickDiff <= 5)
/*     */       {
/*     */         
/* 749 */         similarity *= 0.9D;
/*     */       }
/* 751 */       else if (tickDiff <= 10)
/*     */       {
/*     */         
/* 754 */         similarity *= 0.7D;
/*     */       }
/* 756 */       else if (tickDiff <= 15)
/*     */       {
/*     */         
/* 759 */         similarity *= 0.5D;
/*     */       
/*     */       }
/*     */       else
/*     */       {
/* 764 */         similarity *= 0.3D;
/*     */       }
/*     */     
/* 767 */     } else if (current.getOpponentFreezeTicksRemaining() >= 0 || historical.getOpponentFreezeTicksRemaining() >= 0) {
/*     */ 
/*     */       
/* 770 */       similarity *= 0.5D;
/*     */     } 
/*     */ 
/*     */     
/* 774 */     return similarity;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private List<PredictionResult> getDefaultPredictions() {
/* 782 */     List<PredictionResult> predictions = new ArrayList<>();
/* 783 */     predictions.add(new PredictionResult("melee", 33.3D, "default"));
/* 784 */     predictions.add(new PredictionResult("ranged", 33.3D, "default"));
/* 785 */     predictions.add(new PredictionResult("magic", 33.3D, "default"));
/* 786 */     return predictions;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/AttackContextPredictor.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */