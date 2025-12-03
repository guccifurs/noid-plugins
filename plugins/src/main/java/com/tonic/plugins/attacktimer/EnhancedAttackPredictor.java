/*      */ package com.tonic.plugins.attacktimer;
/*      */ 
/*      */ import com.tonic.Logger;
/*      */ import java.util.ArrayList;
/*      */ import java.util.HashMap;
/*      */ import java.util.HashSet;
/*      */ import java.util.List;
/*      */ import java.util.Map;
/*      */ import java.util.Set;
/*      */ 
/*      */ 
/*      */ 
/*      */ public class EnhancedAttackPredictor
/*      */ {
/*      */   private final AttackTimerConfig config;
/*      */   private PredictionRewardTracker rewardTracker;
/*   17 */   private String lastCacheKey = null;
/*   18 */   private List<PredictionResult> cachedPredictions = null;
/*   19 */   private int cachedHistorySize = -1;
/*      */   
/*      */   private static final int MIN_SEQUENCE_LENGTH = 3;
/*      */   
/*      */   private static final int MAX_SEQUENCE_LENGTH = 10;
/*      */   
/*      */   private static final double MIN_SEQUENCE_SIMILARITY = 0.5D;
/*      */   
/*      */   private static final int MAX_INTERVAL_HISTORY = 5;
/*      */   
/*      */   private static final int INTERVAL_TOLERANCE = 2;
/*      */   
/*      */   public EnhancedAttackPredictor(AttackTimerConfig config) {
/*   32 */     this.config = config;
/*      */   }
/*      */ 
/*      */   
/*      */   public void setRewardTracker(PredictionRewardTracker rewardTracker) {
/*   37 */     this.rewardTracker = rewardTracker;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public List<PredictionResult> getTop5Predictions(ExtendedAttackContext currentContext, EnhancedCombatLogData logData) {
/*   46 */     if (currentContext == null || logData == null || logData.getExtendedAttackHistory().isEmpty())
/*      */     {
/*   48 */       return getDefaultPredictions();
/*      */     }
/*      */     
/*   51 */     List<ExtendedAttackContext> history = logData.getExtendedAttackHistory();
/*   52 */     int currentHistorySize = history.size();
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*   57 */     String cacheKey = generateExtendedCacheKey(currentContext, currentHistorySize);
/*      */ 
/*      */ 
/*      */     
/*   61 */     boolean useCache = false;
/*   62 */     if (useCache && cacheKey != null && cacheKey.equals(this.lastCacheKey) && this.cachedPredictions != null && this.cachedHistorySize == currentHistorySize)
/*      */     {
/*   64 */       return new ArrayList<>(this.cachedPredictions);
/*      */     }
/*      */ 
/*      */     
/*   68 */     Map<String, Double> scores = new HashMap<>();
/*   69 */     Map<String, Map<String, Double>> strategyContributions = new HashMap<>();
/*      */     
/*   71 */     scores.put("melee", Double.valueOf(0.0D));
/*   72 */     scores.put("ranged", Double.valueOf(0.0D));
/*   73 */     scores.put("magic", Double.valueOf(0.0D));
/*      */ 
/*      */     
/*   76 */     for (String attackType : scores.keySet())
/*      */     {
/*   78 */       strategyContributions.put(attackType, new HashMap<>());
/*      */     }
/*      */ 
/*      */     
/*   82 */     Map<String, Double> movementContributions = calculateMovementBasedPrediction(currentContext, history, scores, strategyContributions);
/*      */ 
/*      */     
/*   85 */     Map<String, Double> resourceContributions = calculateResourceBasedPrediction(currentContext, history, scores, strategyContributions);
/*      */ 
/*      */     
/*   88 */     Map<String, Double> sequenceContributions = calculateSequenceBasedPrediction(currentContext, history, scores, strategyContributions);
/*      */ 
/*      */     
/*   91 */     Map<String, Double> temporalContributions = calculateTemporalPatternPrediction(currentContext, history, scores, strategyContributions);
/*      */ 
/*      */     
/*   94 */     Map<String, Double> phaseContributions = calculatePhaseAwarePrediction(currentContext, history, scores, strategyContributions);
/*      */ 
/*      */     
/*   97 */     Map<String, Double> freezeStateContributions = calculateFreezeStateBasedPrediction(currentContext, history, scores, strategyContributions);
/*      */ 
/*      */ 
/*      */     
/*  101 */     Map<String, Double> rewardContributions = calculateRewardBasedPrediction(currentContext, scores, strategyContributions);
/*      */ 
/*      */ 
/*      */     
/*  105 */     double totalScoreBeforeFallback = ((Double)scores.get("melee")).doubleValue() + ((Double)scores.get("ranged")).doubleValue() + ((Double)scores.get("magic")).doubleValue();
/*      */ 
/*      */ 
/*      */     
/*  109 */     Map<String, Integer> historicalDistribution = new HashMap<>();
/*  110 */     historicalDistribution.put("melee", Integer.valueOf(0));
/*  111 */     historicalDistribution.put("ranged", Integer.valueOf(0));
/*  112 */     historicalDistribution.put("magic", Integer.valueOf(0));
/*  113 */     for (ExtendedAttackContext ctx : history) {
/*      */       
/*  115 */       String attackStyle = ctx.getAttackStyle();
/*  116 */       if (attackStyle != null && historicalDistribution.containsKey(attackStyle))
/*      */       {
/*  118 */         historicalDistribution.put(attackStyle, Integer.valueOf(((Integer)historicalDistribution.get(attackStyle)).intValue() + 1));
/*      */       }
/*      */     } 
/*  121 */     int totalHistorical = ((Integer)historicalDistribution.get("melee")).intValue() + ((Integer)historicalDistribution.get("ranged")).intValue() + ((Integer)historicalDistribution.get("magic")).intValue();
/*  122 */     if (totalHistorical > 0) {
/*      */       
/*  124 */       double meleePct = ((Integer)historicalDistribution.get("melee")).intValue() / totalHistorical * 100.0D;
/*  125 */       double rangedPct = ((Integer)historicalDistribution.get("ranged")).intValue() / totalHistorical * 100.0D;
/*  126 */       double magicPct = ((Integer)historicalDistribution.get("magic")).intValue() / totalHistorical * 100.0D;
/*  127 */       Logger.norm("[Enhanced Predictor] Historical data distribution: Melee=" + String.format("%.1f", new Object[] { Double.valueOf(meleePct)
/*  128 */             }) + "%, Ranged=" + String.format("%.1f", new Object[] { Double.valueOf(rangedPct)
/*  129 */             }) + "%, Magic=" + String.format("%.1f", new Object[] { Double.valueOf(magicPct) }) + "% (total: " + totalHistorical + ")");
/*      */     } 
/*      */     
/*  132 */     Logger.norm("[Enhanced Predictor] Strategy contributions BEFORE frequency fallback:");
/*  133 */     Logger.norm("  Total score: " + String.format("%.2f", new Object[] { Double.valueOf(totalScoreBeforeFallback) }));
/*  134 */     Logger.norm("  Melee: " + String.format("%.2f", new Object[] { scores.get("melee") }));
/*  135 */     Logger.norm("  Ranged: " + String.format("%.2f", new Object[] { scores.get("ranged") }));
/*  136 */     Logger.norm("  Magic: " + String.format("%.2f", new Object[] { scores.get("magic") }));
/*  137 */     Logger.norm("  History size: " + history.size());
/*  138 */     Logger.norm("  Current freeze state: " + currentContext.getFreezeState());
/*  139 */     Logger.norm("  Current distance: " + currentContext.getDistance());
/*  140 */     Logger.norm("  Current HP%: " + currentContext.getTargetHP());
/*      */ 
/*      */     
/*  143 */     for (String attackType : strategyContributions.keySet()) {
/*      */       
/*  145 */       Map<String, Double> contribs = strategyContributions.get(attackType);
/*  146 */       if (contribs != null && !contribs.isEmpty()) {
/*      */         
/*  148 */         Logger.norm("  " + attackType + " contributions: " + String.valueOf(contribs));
/*      */         
/*      */         continue;
/*      */       } 
/*  152 */       Logger.norm("  " + attackType + " contributions: NONE");
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  159 */     if (totalScoreBeforeFallback < 0.01D && history.size() >= 10) {
/*      */       
/*  161 */       Logger.norm("[Enhanced Predictor] WARNING: All strategies contributed 0 - using frequency fallback");
/*      */       
/*  163 */       Map<String, Integer> attackCounts = new HashMap<>();
/*  164 */       attackCounts.put("melee", Integer.valueOf(0));
/*  165 */       attackCounts.put("ranged", Integer.valueOf(0));
/*  166 */       attackCounts.put("magic", Integer.valueOf(0));
/*      */ 
/*      */       
/*  169 */       int countEnd = history.size();
/*  170 */       int countStart = Math.max(0, countEnd - 20);
/*  171 */       for (int i = countStart; i < countEnd; i++) {
/*      */         
/*  173 */         ExtendedAttackContext ctx = history.get(i);
/*  174 */         if (ctx.getAttackStyle() != null && attackCounts.containsKey(ctx.getAttackStyle()))
/*      */         {
/*  176 */           attackCounts.put(ctx.getAttackStyle(), Integer.valueOf(((Integer)attackCounts.get(ctx.getAttackStyle())).intValue() + 1));
/*      */         }
/*      */       } 
/*      */       
/*  180 */       int totalCount = ((Integer)attackCounts.get("melee")).intValue() + ((Integer)attackCounts.get("ranged")).intValue() + ((Integer)attackCounts.get("magic")).intValue();
/*  181 */       if (totalCount > 0)
/*      */       {
/*      */ 
/*      */         
/*  185 */         double meleeFreq = ((Integer)attackCounts.get("melee")).intValue() / totalCount * 100.0D;
/*  186 */         double rangedFreq = ((Integer)attackCounts.get("ranged")).intValue() / totalCount * 100.0D;
/*  187 */         double magicFreq = ((Integer)attackCounts.get("magic")).intValue() / totalCount * 100.0D;
/*      */         
/*  189 */         scores.put("melee", Double.valueOf(meleeFreq));
/*  190 */         scores.put("ranged", Double.valueOf(rangedFreq));
/*  191 */         scores.put("magic", Double.valueOf(magicFreq));
/*      */ 
/*      */         
/*  194 */         ((Map<String, Double>)strategyContributions.get("melee")).put("frequency_based", Double.valueOf(meleeFreq));
/*  195 */         ((Map<String, Double>)strategyContributions.get("ranged")).put("frequency_based", Double.valueOf(rangedFreq));
/*  196 */         ((Map<String, Double>)strategyContributions.get("magic")).put("frequency_based", Double.valueOf(magicFreq));
/*      */         
/*  198 */         Logger.norm("[Enhanced Predictor] Frequency fallback applied: Melee=" + String.format("%.1f", new Object[] { Double.valueOf(meleeFreq)
/*  199 */               }) + "%, Ranged=" + String.format("%.1f", new Object[] { Double.valueOf(rangedFreq) }) + "%, Magic=" + String.format("%.1f", new Object[] { Double.valueOf(magicFreq) }) + "%");
/*      */       }
/*      */     
/*  202 */     } else if (totalScoreBeforeFallback < 1.0D) {
/*      */       
/*  204 */       Logger.norm("[Enhanced Predictor] Strategies contributed little (" + String.format("%.2f", new Object[] { Double.valueOf(totalScoreBeforeFallback) }) + ") but not zero - will normalize existing contributions (no frequency fallback)");
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  210 */     double totalScore = ((Double)scores.get("melee")).doubleValue() + ((Double)scores.get("ranged")).doubleValue() + ((Double)scores.get("magic")).doubleValue();
/*  211 */     if (totalScore > 0.01D) {
/*      */ 
/*      */       
/*  214 */       double meleeNormalized = ((Double)scores.get("melee")).doubleValue() / totalScore * 100.0D;
/*  215 */       double rangedNormalized = ((Double)scores.get("ranged")).doubleValue() / totalScore * 100.0D;
/*  216 */       double magicNormalized = ((Double)scores.get("magic")).doubleValue() / totalScore * 100.0D;
/*      */       
/*  218 */       scores.put("melee", Double.valueOf(meleeNormalized));
/*  219 */       scores.put("ranged", Double.valueOf(rangedNormalized));
/*  220 */       scores.put("magic", Double.valueOf(magicNormalized));
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  226 */       Logger.norm("[Enhanced Predictor] Normalized scores (no cap): Melee=" + String.format("%.1f", new Object[] { Double.valueOf(meleeNormalized)
/*  227 */             }) + "%, Ranged=" + String.format("%.1f", new Object[] { Double.valueOf(rangedNormalized)
/*  228 */             }) + "%, Magic=" + String.format("%.1f", new Object[] { Double.valueOf(magicNormalized) }) + "%");
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  233 */       if (this.config != null && this.config.debug())
/*      */       {
/*  235 */         Logger.norm("[Enhanced Predictor] AFTER normalization + diversity fix:");
/*  236 */         Logger.norm("  Melee: " + String.format("%.1f", new Object[] { scores.get("melee") }) + "%");
/*  237 */         Logger.norm("  Ranged: " + String.format("%.1f", new Object[] { scores.get("ranged") }) + "%");
/*  238 */         Logger.norm("  Magic: " + String.format("%.1f", new Object[] { scores.get("magic") }) + "%");
/*      */       }
/*      */     
/*      */     }
/*      */     else {
/*      */       
/*  244 */       scores.put("melee", Double.valueOf(33.33D));
/*  245 */       scores.put("ranged", Double.valueOf(33.33D));
/*  246 */       scores.put("magic", Double.valueOf(33.34D));
/*      */       
/*  248 */       if (this.config != null && this.config.debug())
/*      */       {
/*  250 */         Logger.norm("[Enhanced Predictor] No strategies contributed - using defaults (33/33/33)");
/*      */       }
/*      */     } 
/*      */ 
/*      */     
/*  255 */     double maxScore = Math.max(Math.max(((Double)scores.get("melee")).doubleValue(), ((Double)scores.get("ranged")).doubleValue()), ((Double)scores.get("magic")).doubleValue());
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  261 */     if (maxScore < 5.0D && history.size() < 5) {
/*      */ 
/*      */       
/*  264 */       scores.put("melee", Double.valueOf(33.33D));
/*  265 */       scores.put("ranged", Double.valueOf(33.33D));
/*  266 */       scores.put("magic", Double.valueOf(33.34D));
/*      */     } 
/*      */ 
/*      */ 
/*      */     
/*  271 */     Map<String, String> methods = new HashMap<>();
/*  272 */     for (String attackType : scores.keySet()) {
/*      */       
/*  274 */       Map<String, Double> contributions = strategyContributions.get(attackType);
/*  275 */       if (contributions != null && !contributions.isEmpty()) {
/*      */ 
/*      */         
/*  278 */         List<Map.Entry<String, Double>> sortedContributions = new ArrayList<>(contributions.entrySet());
/*  279 */         sortedContributions.sort((a, b) -> Double.compare(((Double)b.getValue()).doubleValue(), ((Double)a.getValue()).doubleValue()));
/*      */         
/*  281 */         if (sortedContributions.size() >= 2) {
/*      */           
/*  283 */           String firstStrategy = (String)((Map.Entry)sortedContributions.get(0)).getKey();
/*  284 */           String secondStrategy = (String)((Map.Entry)sortedContributions.get(1)).getKey();
/*  285 */           double firstContribution = ((Double)((Map.Entry)sortedContributions.get(0)).getValue()).doubleValue();
/*  286 */           double secondContribution = ((Double)((Map.Entry)sortedContributions.get(1)).getValue()).doubleValue();
/*      */ 
/*      */           
/*  289 */           if (firstStrategy.equals("freeze_state_based") && secondContribution > 0.1D) {
/*      */             
/*  291 */             methods.put(attackType, "freeze_state_based+" + secondStrategy);
/*      */             continue;
/*      */           } 
/*  294 */           if (secondStrategy.equals("freeze_state_based") && firstContribution > 0.1D && secondContribution > 0.1D) {
/*      */             
/*  296 */             methods.put(attackType, firstStrategy + "+freeze_state_based");
/*      */             
/*      */             continue;
/*      */           } 
/*      */           
/*  301 */           methods.put(attackType, firstStrategy);
/*      */           continue;
/*      */         } 
/*  304 */         if (sortedContributions.size() == 1) {
/*      */           
/*  306 */           methods.put(attackType, (String)((Map.Entry)sortedContributions.get(0)).getKey());
/*      */           
/*      */           continue;
/*      */         } 
/*  310 */         methods.put(attackType, "default");
/*      */         
/*      */         continue;
/*      */       } 
/*      */       
/*  315 */       methods.put(attackType, "default");
/*      */     } 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  321 */     if (maxScore < 5.0D && history.size() < 5) {
/*      */ 
/*      */       
/*  324 */       methods.put("melee", "default");
/*  325 */       methods.put("ranged", "default");
/*  326 */       methods.put("magic", "default");
/*      */     } 
/*      */ 
/*      */     
/*  330 */     List<PredictionResult> predictions = new ArrayList<>();
/*  331 */     for (String attackType : scores.keySet()) {
/*      */       
/*  333 */       double score = ((Double)scores.get(attackType)).doubleValue();
/*  334 */       String method = methods.getOrDefault(attackType, "default");
/*      */       
/*  336 */       predictions.add(new PredictionResult(attackType, score, method));
/*      */     } 
/*      */ 
/*      */     
/*  340 */     predictions.sort((a, b) -> Double.compare(b.getQualityScore(), a.getQualityScore()));
/*  341 */     predictions = predictions.subList(0, Math.min(5, predictions.size()));
/*      */ 
/*      */     
/*  344 */     this.lastCacheKey = cacheKey;
/*  345 */     this.cachedPredictions = new ArrayList<>(predictions);
/*  346 */     this.cachedHistorySize = currentHistorySize;
/*      */     
/*  348 */     return predictions;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public PredictionProgress getPredictionProgress(ExtendedAttackContext currentContext, EnhancedCombatLogData logData) {
/*      */     double attackProgress;
/*      */     String statusMessage;
/*  358 */     if (logData == null || logData.getExtendedAttackHistory().isEmpty())
/*      */     {
/*  360 */       return new PredictionProgress(0.0D, 0.0D, "No data collected yet");
/*      */     }
/*      */     
/*  363 */     List<ExtendedAttackContext> history = logData.getExtendedAttackHistory();
/*  364 */     int attackCount = history.size();
/*      */ 
/*      */     
/*  367 */     double progress = 0.0D;
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  372 */     if (attackCount < 20) {
/*      */       
/*  374 */       attackProgress = attackCount / 20.0D * 100.0D;
/*      */     }
/*  376 */     else if (attackCount < 100) {
/*      */       
/*  378 */       attackProgress = 100.0D;
/*      */     
/*      */     }
/*      */     else {
/*      */       
/*  383 */       attackProgress = Math.min(120.0D, 100.0D + (attackCount - 100) / 400.0D * 20.0D);
/*      */     } 
/*  385 */     progress += Math.min(100.0D, attackProgress) * 0.4D;
/*      */ 
/*      */ 
/*      */ 
/*      */     
/*  390 */     Set<String> uniqueSequences = new HashSet<>();
/*  391 */     for (int i = 0; i <= history.size() - 3; i++) {
/*      */ 
/*      */       
/*  394 */       for (int seqLen = 3; seqLen <= Math.min(5, history.size() - i); seqLen++) {
/*      */         
/*  396 */         List<String> sequence = new ArrayList<>();
/*  397 */         boolean validSequence = true;
/*  398 */         for (int j = 0; j < seqLen; j++) {
/*      */           
/*  400 */           ExtendedAttackContext ctx = history.get(i + j);
/*  401 */           if (ctx.getAttackStyle() != null) {
/*      */             
/*  403 */             sequence.add(ctx.getAttackStyle());
/*      */           }
/*      */           else {
/*      */             
/*  407 */             validSequence = false;
/*      */             break;
/*      */           } 
/*      */         } 
/*  411 */         if (validSequence && sequence.size() == seqLen) {
/*      */           
/*  413 */           String seqKey = String.join(",", (Iterable)sequence);
/*  414 */           uniqueSequences.add(seqKey);
/*      */         } 
/*      */       } 
/*      */     } 
/*      */     
/*  419 */     double sequenceProgress = Math.min(100.0D, uniqueSequences.size() / 10.0D * 100.0D);
/*  420 */     progress += sequenceProgress * 0.3D;
/*      */ 
/*      */ 
/*      */     
/*  424 */     double currentConfidence = 0.0D;
/*  425 */     if (currentContext != null) {
/*      */ 
/*      */       
/*  428 */       List<PredictionResult> testPredictions = getTop5Predictions(currentContext, logData);
/*  429 */       if (testPredictions != null && !testPredictions.isEmpty())
/*      */       {
/*  431 */         currentConfidence = ((PredictionResult)testPredictions.get(0)).getQualityScore();
/*      */       }
/*      */     } 
/*      */     
/*  435 */     double confidenceProgress = Math.min(100.0D, currentConfidence / 30.0D * 100.0D);
/*  436 */     progress += confidenceProgress * 0.3D;
/*      */ 
/*      */     
/*  439 */     progress = Math.min(100.0D, progress);
/*      */ 
/*      */ 
/*      */     
/*  443 */     if (progress < 25.0D) {
/*      */       
/*  445 */       statusMessage = "Collecting data... (" + attackCount + " attacks)";
/*      */     }
/*  447 */     else if (progress < 50.0D) {
/*      */       
/*  449 */       statusMessage = "Building patterns... (" + attackCount + " attacks, " + uniqueSequences.size() + " sequences)";
/*      */     }
/*  451 */     else if (progress < 75.0D) {
/*      */       
/*  453 */       statusMessage = "Analyzing patterns... (" + attackCount + " attacks, " + uniqueSequences.size() + " sequences, " + String.format("%.1f", new Object[] { Double.valueOf(currentConfidence) }) + "% confidence)";
/*      */     }
/*  455 */     else if (progress < 95.0D) {
/*      */       
/*  457 */       statusMessage = "Almost ready... (" + attackCount + " attacks, " + uniqueSequences.size() + " sequences, " + String.format("%.1f", new Object[] { Double.valueOf(currentConfidence) }) + "% confidence)";
/*      */     }
/*      */     else {
/*      */       
/*  461 */       statusMessage = "Enhanced predictor active! (" + attackCount + " attacks, " + uniqueSequences.size() + " sequences)";
/*      */     } 
/*      */     
/*  464 */     return new PredictionProgress(progress, currentConfidence, statusMessage);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private Map<String, Double> calculateFreezeStateBasedPrediction(ExtendedAttackContext current, List<ExtendedAttackContext> history, Map<String, Double> scores, Map<String, Map<String, Double>> strategyContributions) {
/*  475 */     Map<String, Double> contributions = new HashMap<>();
/*  476 */     contributions.put("melee", Double.valueOf(0.0D));
/*  477 */     contributions.put("ranged", Double.valueOf(0.0D));
/*  478 */     contributions.put("magic", Double.valueOf(0.0D));
/*      */     
/*  480 */     if (history.size() < 2)
/*      */     {
/*  482 */       return contributions;
/*      */     }
/*      */     
/*  485 */     String currentFreezeState = current.getFreezeState();
/*  486 */     Map<String, Double> weightedCounts = new HashMap<>();
/*  487 */     weightedCounts.put("melee", Double.valueOf(0.0D));
/*  488 */     weightedCounts.put("ranged", Double.valueOf(0.0D));
/*  489 */     weightedCounts.put("magic", Double.valueOf(0.0D));
/*  490 */     double totalWeight = 0.0D;
/*  491 */     int exactMatches = 0;
/*  492 */     int similarMatches = 0;
/*      */ 
/*      */ 
/*      */     
/*  496 */     for (int i = 0; i < history.size() - 1; i++) {
/*      */       
/*  498 */       ExtendedAttackContext historical = history.get(i);
/*  499 */       String historicalFreezeState = historical.getFreezeState();
/*      */       
/*  501 */       boolean freezeStateMatches = false;
/*  502 */       double matchWeight = 1.0D;
/*      */       
/*  504 */       if (currentFreezeState != null && historicalFreezeState != null) {
/*      */         
/*  506 */         if (currentFreezeState.equals(historicalFreezeState))
/*      */         {
/*      */           
/*  509 */           freezeStateMatches = true;
/*  510 */           matchWeight = 1.0D;
/*  511 */           exactMatches++;
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*      */         }
/*  517 */         else if (currentFreezeState.contains("frozen") && historicalFreezeState.contains("frozen"))
/*      */         {
/*  519 */           freezeStateMatches = true;
/*  520 */           matchWeight = 0.5D;
/*  521 */           similarMatches++;
/*      */         }
/*  523 */         else if (currentFreezeState.contains("unfrozen") && historicalFreezeState.contains("unfrozen"))
/*      */         {
/*  525 */           freezeStateMatches = true;
/*  526 */           matchWeight = 0.5D;
/*  527 */           similarMatches++;
/*      */         }
/*      */       
/*      */       }
/*  531 */       else if (currentFreezeState == null && historicalFreezeState == null) {
/*      */ 
/*      */         
/*  534 */         freezeStateMatches = true;
/*  535 */         matchWeight = 0.8D;
/*  536 */         similarMatches++;
/*      */       } 
/*      */       
/*  539 */       if (freezeStateMatches) {
/*      */ 
/*      */         
/*  542 */         double contextSimilarity = calculateExtendedContextSimilarity(current, historical);
/*      */ 
/*      */ 
/*      */         
/*  546 */         if (contextSimilarity >= 0.6D) {
/*      */ 
/*      */ 
/*      */           
/*  550 */           double weight = matchWeight * (1.0D + contextSimilarity * 2.0D);
/*      */ 
/*      */           
/*  553 */           ExtendedAttackContext nextContext = history.get(i + 1);
/*  554 */           String nextAttack = nextContext.getAttackStyle();
/*      */           
/*  556 */           if (nextAttack != null && weightedCounts.containsKey(nextAttack)) {
/*      */             
/*  558 */             weightedCounts.put(nextAttack, Double.valueOf(((Double)weightedCounts.get(nextAttack)).doubleValue() + weight));
/*  559 */             totalWeight += weight;
/*      */           } 
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */     
/*  566 */     if (totalWeight > 0.0D) {
/*      */       
/*  568 */       for (String attackType : weightedCounts.keySet())
/*      */       {
/*  570 */         double score = ((Double)weightedCounts.get(attackType)).doubleValue() / totalWeight * 10.0D;
/*  571 */         scores.put(attackType, Double.valueOf(((Double)scores.get(attackType)).doubleValue() + score));
/*  572 */         contributions.put(attackType, Double.valueOf(score));
/*      */ 
/*      */         
/*  575 */         if (!strategyContributions.containsKey(attackType))
/*      */         {
/*  577 */           strategyContributions.put(attackType, new HashMap<>());
/*      */         }
/*  579 */         ((Map<String, Double>)strategyContributions.get(attackType)).put("freeze_state_based", Double.valueOf(score));
/*      */       }
/*      */     
/*  582 */     } else if (history.size() >= 3) {
/*      */ 
/*      */ 
/*      */       
/*  586 */       Map<String, Integer> weakCounts = new HashMap<>();
/*  587 */       weakCounts.put("melee", Integer.valueOf(0));
/*  588 */       weakCounts.put("ranged", Integer.valueOf(0));
/*  589 */       weakCounts.put("magic", Integer.valueOf(0));
/*      */ 
/*      */       
/*  592 */       for (int j = 0; j < history.size() - 1; j++) {
/*      */         
/*  594 */         ExtendedAttackContext nextContext = history.get(j + 1);
/*  595 */         String nextAttack = nextContext.getAttackStyle();
/*  596 */         if (nextAttack != null && weakCounts.containsKey(nextAttack))
/*      */         {
/*  598 */           weakCounts.put(nextAttack, Integer.valueOf(((Integer)weakCounts.get(nextAttack)).intValue() + 1));
/*      */         }
/*      */       } 
/*      */       
/*  602 */       int totalWeak = ((Integer)weakCounts.get("melee")).intValue() + ((Integer)weakCounts.get("ranged")).intValue() + ((Integer)weakCounts.get("magic")).intValue();
/*  603 */       if (totalWeak > 0) {
/*      */ 
/*      */         
/*  606 */         double weakMelee = ((Integer)weakCounts.get("melee")).intValue() / totalWeak * 5.0D;
/*  607 */         double weakRanged = ((Integer)weakCounts.get("ranged")).intValue() / totalWeak * 5.0D;
/*  608 */         double weakMagic = ((Integer)weakCounts.get("magic")).intValue() / totalWeak * 5.0D;
/*      */         
/*  610 */         scores.put("melee", Double.valueOf(((Double)scores.get("melee")).doubleValue() + weakMelee));
/*  611 */         scores.put("ranged", Double.valueOf(((Double)scores.get("ranged")).doubleValue() + weakRanged));
/*  612 */         scores.put("magic", Double.valueOf(((Double)scores.get("magic")).doubleValue() + weakMagic));
/*      */         
/*  614 */         contributions.put("melee", Double.valueOf(weakMelee));
/*  615 */         contributions.put("ranged", Double.valueOf(weakRanged));
/*  616 */         contributions.put("magic", Double.valueOf(weakMagic));
/*      */         
/*  618 */         ((Map<String, Double>)strategyContributions.get("melee")).put("freeze_state_based_weak", Double.valueOf(weakMelee));
/*  619 */         ((Map<String, Double>)strategyContributions.get("ranged")).put("freeze_state_based_weak", Double.valueOf(weakRanged));
/*  620 */         ((Map<String, Double>)strategyContributions.get("magic")).put("freeze_state_based_weak", Double.valueOf(weakMagic));
/*      */       } 
/*      */     } 
/*      */     
/*  624 */     return contributions;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private Map<String, Double> calculateSequenceBasedPrediction(ExtendedAttackContext current, List<ExtendedAttackContext> history, Map<String, Double> scores, Map<String, Map<String, Double>> strategyContributions) {
/*      */     List<String> currentSequence;
/*  635 */     Map<String, Double> contributions = new HashMap<>();
/*  636 */     contributions.put("melee", Double.valueOf(0.0D));
/*  637 */     contributions.put("ranged", Double.valueOf(0.0D));
/*  638 */     contributions.put("magic", Double.valueOf(0.0D));
/*      */     
/*  640 */     if (history.size() < 3)
/*      */     {
/*  642 */       return contributions;
/*      */     }
/*      */ 
/*      */ 
/*      */     
/*  647 */     if (current.getAttackSequence() != null && !current.getAttackSequence().isEmpty()) {
/*      */       
/*  649 */       currentSequence = new ArrayList<>(current.getAttackSequence());
/*      */     
/*      */     }
/*      */     else {
/*      */       
/*  654 */       currentSequence = new ArrayList<>();
/*  655 */       int startIdx = Math.max(0, history.size() - 10);
/*  656 */       for (int j = startIdx; j < history.size(); j++) {
/*      */         
/*  658 */         ExtendedAttackContext ctx = history.get(j);
/*  659 */         if (ctx.getAttackStyle() != null)
/*      */         {
/*  661 */           currentSequence.add(ctx.getAttackStyle());
/*      */         }
/*      */       } 
/*      */     } 
/*      */     
/*  666 */     if (currentSequence.isEmpty() || currentSequence.size() < 3)
/*      */     {
/*  668 */       return contributions;
/*      */     }
/*      */     
/*  671 */     int sequenceLength = Math.min(currentSequence.size(), 10);
/*      */ 
/*      */     
/*  674 */     List<String> sequenceToMatch = currentSequence.subList(
/*  675 */         Math.max(0, currentSequence.size() - sequenceLength), currentSequence
/*  676 */         .size());
/*      */ 
/*      */     
/*  679 */     Map<String, Double> weightedCounts = new HashMap<>();
/*  680 */     weightedCounts.put("melee", Double.valueOf(0.0D));
/*  681 */     weightedCounts.put("ranged", Double.valueOf(0.0D));
/*  682 */     weightedCounts.put("magic", Double.valueOf(0.0D));
/*  683 */     double totalWeight = 0.0D;
/*      */ 
/*      */ 
/*      */     
/*  687 */     for (int i = 0; i <= history.size() - sequenceLength - 1; i++) {
/*      */ 
/*      */       
/*  690 */       List<String> historicalSequence = new ArrayList<>();
/*  691 */       for (int j = 0; j < sequenceLength; j++) {
/*      */         
/*  693 */         ExtendedAttackContext ctx = history.get(i + j);
/*  694 */         if (ctx.getAttackStyle() != null)
/*      */         {
/*  696 */           historicalSequence.add(ctx.getAttackStyle());
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/*  701 */       if (historicalSequence.size() == sequenceToMatch.size()) {
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*  707 */         double similarity = calculateSequenceSimilarity(sequenceToMatch, historicalSequence);
/*      */         
/*  709 */         if (similarity >= 0.5D)
/*      */         {
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */           
/*  717 */           if (i + sequenceLength < history.size()) {
/*      */             
/*  719 */             ExtendedAttackContext nextContext = history.get(i + sequenceLength);
/*  720 */             String nextAttack = nextContext.getAttackStyle();
/*      */             
/*  722 */             if (nextAttack != null && weightedCounts.containsKey(nextAttack)) {
/*      */ 
/*      */ 
/*      */               
/*  726 */               double weight = similarity * (1.0D + sequenceLength * 0.2D);
/*  727 */               weightedCounts.put(nextAttack, Double.valueOf(((Double)weightedCounts.get(nextAttack)).doubleValue() + weight));
/*  728 */               totalWeight += weight;
/*      */             } 
/*      */           } 
/*      */         }
/*      */       } 
/*      */     } 
/*  734 */     if (totalWeight > 0.0D) {
/*      */ 
/*      */       
/*  737 */       double avgSimilarity = 0.0D;
/*  738 */       int similarityCount = 0;
/*      */       
/*  740 */       for (int j = 0; j <= history.size() - sequenceLength - 1; j++) {
/*      */         
/*  742 */         List<String> historicalSequence = new ArrayList<>();
/*  743 */         for (int k = 0; k < sequenceLength; k++) {
/*      */           
/*  745 */           ExtendedAttackContext ctx = history.get(j + k);
/*  746 */           if (ctx.getAttackStyle() != null)
/*      */           {
/*  748 */             historicalSequence.add(ctx.getAttackStyle());
/*      */           }
/*      */         } 
/*      */         
/*  752 */         if (historicalSequence.size() == sequenceToMatch.size()) {
/*      */           
/*  754 */           double sim = calculateSequenceSimilarity(sequenceToMatch, historicalSequence);
/*  755 */           if (sim >= 0.5D) {
/*      */             
/*  757 */             avgSimilarity += sim;
/*  758 */             similarityCount++;
/*      */           } 
/*      */         } 
/*      */       } 
/*      */       
/*  763 */       if (similarityCount > 0)
/*      */       {
/*  765 */         avgSimilarity /= similarityCount;
/*      */       }
/*      */ 
/*      */       
/*  769 */       if (totalWeight > 0.0D)
/*      */       {
/*  771 */         for (String attackType : weightedCounts.keySet()) {
/*      */           
/*  773 */           double score = ((Double)weightedCounts.get(attackType)).doubleValue() / totalWeight * 20.0D;
/*  774 */           scores.put(attackType, Double.valueOf(((Double)scores.get(attackType)).doubleValue() + score));
/*  775 */           contributions.put(attackType, Double.valueOf(score));
/*      */ 
/*      */           
/*  778 */           if (!strategyContributions.containsKey(attackType))
/*      */           {
/*  780 */             strategyContributions.put(attackType, new HashMap<>());
/*      */           }
/*  782 */           ((Map<String, Double>)strategyContributions.get(attackType)).put("sequence_match", Double.valueOf(score));
/*      */         } 
/*      */       }
/*      */     } 
/*      */     
/*  787 */     return contributions;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private double calculateSequenceSimilarity(List<String> seq1, List<String> seq2) {
/*  795 */     if (seq1.size() != seq2.size() || seq1.isEmpty())
/*      */     {
/*  797 */       return 0.0D;
/*      */     }
/*      */ 
/*      */     
/*  801 */     double weightedMatches = 0.0D;
/*  802 */     double totalWeight = 0.0D;
/*  803 */     for (int i = 0; i < seq1.size(); i++) {
/*      */       
/*  805 */       double weight = (i + 1.0D) / seq1.size();
/*  806 */       if (((String)seq1.get(i)).equals(seq2.get(i)))
/*      */       {
/*  808 */         weightedMatches += weight;
/*      */       }
/*  810 */       totalWeight += weight;
/*      */     } 
/*      */     
/*  813 */     return (totalWeight > 0.0D) ? (weightedMatches / totalWeight) : 0.0D;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private Map<String, Double> calculateTemporalPatternPrediction(ExtendedAttackContext current, List<ExtendedAttackContext> history, Map<String, Double> scores, Map<String, Map<String, Double>> strategyContributions) {
/*  824 */     Map<String, Double> contributions = new HashMap<>();
/*  825 */     contributions.put("melee", Double.valueOf(0.0D));
/*  826 */     contributions.put("ranged", Double.valueOf(0.0D));
/*  827 */     contributions.put("magic", Double.valueOf(0.0D));
/*      */     
/*  829 */     if (history.size() < 3)
/*      */     {
/*  831 */       return contributions;
/*      */     }
/*      */     
/*  834 */     Map<String, Double> weightedCounts = new HashMap<>();
/*  835 */     weightedCounts.put("melee", Double.valueOf(0.0D));
/*  836 */     weightedCounts.put("ranged", Double.valueOf(0.0D));
/*  837 */     weightedCounts.put("magic", Double.valueOf(0.0D));
/*  838 */     double totalWeight = 0.0D;
/*      */ 
/*      */     
/*  841 */     List<Integer> currentIntervals = current.getAttackIntervals();
/*  842 */     List<Long> currentFreezeHistory = current.getFreezeHistory();
/*  843 */     List<Long> currentSpecHistory = current.getSpecHistory();
/*  844 */     List<Long> currentWeaponSwitchHistory = current.getWeaponSwitchHistory();
/*  845 */     List<Long> currentPrayerSwitchHistory = current.getPrayerSwitchHistory();
/*      */ 
/*      */     
/*  848 */     for (int i = 1; i < history.size() - 1; i++) {
/*      */       
/*  850 */       ExtendedAttackContext historical = history.get(i);
/*      */ 
/*      */       
/*  853 */       double temporalSimilarity = 0.0D;
/*  854 */       int patternMatches = 0;
/*  855 */       int totalPatterns = 0;
/*      */ 
/*      */       
/*  858 */       if (currentIntervals != null && !currentIntervals.isEmpty() && historical
/*  859 */         .getAttackIntervals() != null && !historical.getAttackIntervals().isEmpty()) {
/*      */         
/*  861 */         totalPatterns++;
/*  862 */         if (matchAttackIntervals(currentIntervals, historical.getAttackIntervals()))
/*      */         {
/*  864 */           patternMatches++;
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/*  869 */       if (currentFreezeHistory != null && !currentFreezeHistory.isEmpty() && historical
/*  870 */         .getFreezeHistory() != null && !historical.getFreezeHistory().isEmpty()) {
/*      */         
/*  872 */         totalPatterns++;
/*  873 */         if (matchTemporalHistory(currentFreezeHistory, historical.getFreezeHistory()))
/*      */         {
/*  875 */           patternMatches++;
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/*  880 */       if (currentSpecHistory != null && !currentSpecHistory.isEmpty() && historical
/*  881 */         .getSpecHistory() != null && !historical.getSpecHistory().isEmpty()) {
/*      */         
/*  883 */         totalPatterns++;
/*  884 */         if (matchTemporalHistory(currentSpecHistory, historical.getSpecHistory()))
/*      */         {
/*  886 */           patternMatches++;
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/*  891 */       if (currentWeaponSwitchHistory != null && !currentWeaponSwitchHistory.isEmpty() && historical
/*  892 */         .getWeaponSwitchHistory() != null && !historical.getWeaponSwitchHistory().isEmpty()) {
/*      */         
/*  894 */         totalPatterns++;
/*  895 */         if (matchTemporalHistory(currentWeaponSwitchHistory, historical.getWeaponSwitchHistory()))
/*      */         {
/*  897 */           patternMatches++;
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/*  902 */       if (currentPrayerSwitchHistory != null && !currentPrayerSwitchHistory.isEmpty() && historical
/*  903 */         .getPrayerSwitchHistory() != null && !historical.getPrayerSwitchHistory().isEmpty()) {
/*      */         
/*  905 */         totalPatterns++;
/*  906 */         if (matchTemporalHistory(currentPrayerSwitchHistory, historical.getPrayerSwitchHistory()))
/*      */         {
/*  908 */           patternMatches++;
/*      */         }
/*      */       } 
/*      */       
/*  912 */       if (totalPatterns > 0)
/*      */       {
/*  914 */         temporalSimilarity = patternMatches / totalPatterns;
/*      */       }
/*      */ 
/*      */ 
/*      */       
/*  919 */       if (temporalSimilarity >= 0.6D) {
/*      */ 
/*      */         
/*  922 */         ExtendedAttackContext nextContext = history.get(i + 1);
/*  923 */         String nextAttack = nextContext.getAttackStyle();
/*      */         
/*  925 */         if (nextAttack != null && weightedCounts.containsKey(nextAttack)) {
/*      */ 
/*      */           
/*  928 */           double weight = temporalSimilarity * 15.0D;
/*  929 */           weightedCounts.put(nextAttack, Double.valueOf(((Double)weightedCounts.get(nextAttack)).doubleValue() + weight));
/*  930 */           totalWeight += weight;
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */     
/*  936 */     if (totalWeight > 0.0D) {
/*      */       
/*  938 */       for (String attackType : weightedCounts.keySet())
/*      */       {
/*  940 */         double score = ((Double)weightedCounts.get(attackType)).doubleValue() / totalWeight * 15.0D;
/*  941 */         scores.put(attackType, Double.valueOf(((Double)scores.get(attackType)).doubleValue() + score));
/*  942 */         contributions.put(attackType, Double.valueOf(score));
/*      */ 
/*      */         
/*  945 */         if (!strategyContributions.containsKey(attackType))
/*      */         {
/*  947 */           strategyContributions.put(attackType, new HashMap<>());
/*      */         }
/*  949 */         ((Map<String, Double>)strategyContributions.get(attackType)).put("temporal_pattern", Double.valueOf(score));
/*      */       }
/*      */     
/*      */     }
/*      */     else {
/*      */       
/*  955 */       Logger.norm("[Enhanced Predictor] Temporal strategy contributed 0 - totalPatterns checked: " + (history
/*  956 */           .size() - 2) + ", currentIntervals: " + (
/*  957 */           (currentIntervals != null) ? currentIntervals.size() : 0) + ", currentFreezeHistory: " + (
/*      */           
/*  959 */           (currentFreezeHistory != null) ? currentFreezeHistory.size() : 0));
/*      */     } 
/*      */     
/*  962 */     return contributions;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private boolean matchAttackIntervals(List<Integer> intervals1, List<Integer> intervals2) {
/*  970 */     if (intervals1 == null || intervals2 == null || intervals1.isEmpty() || intervals2.isEmpty())
/*      */     {
/*  972 */       return false;
/*      */     }
/*      */     
/*  975 */     int minSize = Math.min(intervals1.size(), intervals2.size());
/*  976 */     int matches = 0;
/*      */     
/*  978 */     for (int i = 0; i < minSize; i++) {
/*      */       
/*  980 */       int diff = Math.abs(((Integer)intervals1.get(i)).intValue() - ((Integer)intervals2.get(i)).intValue());
/*  981 */       if (diff <= 2)
/*      */       {
/*  983 */         matches++;
/*      */       }
/*      */     } 
/*      */     
/*  987 */     return (matches >= minSize * 0.7D);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private boolean matchTemporalHistory(List<Long> history1, List<Long> history2) {
/*  995 */     if (history1 == null || history2 == null || history1.isEmpty() || history2.isEmpty())
/*      */     {
/*  997 */       return false;
/*      */     }
/*      */     
/* 1000 */     if (history1.size() != history2.size())
/*      */     {
/* 1002 */       return false;
/*      */     }
/*      */ 
/*      */ 
/*      */     
/* 1007 */     long currentTick = ((Long)history1.get(history1.size() - 1)).longValue();
/* 1008 */     long historicalTick = ((Long)history2.get(history2.size() - 1)).longValue();
/*      */     
/* 1010 */     int matches = 0;
/* 1011 */     for (int i = 0; i < history1.size(); i++) {
/*      */       
/* 1013 */       long diff1 = currentTick - ((Long)history1.get(i)).longValue();
/* 1014 */       long diff2 = historicalTick - ((Long)history2.get(i)).longValue();
/* 1015 */       long diff = Math.abs(diff1 - diff2);
/*      */       
/* 1017 */       if (diff <= 3L)
/*      */       {
/* 1019 */         matches++;
/*      */       }
/*      */     } 
/*      */     
/* 1023 */     return (matches >= history1.size() * 0.6D);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private Map<String, Double> calculatePhaseAwarePrediction(ExtendedAttackContext current, List<ExtendedAttackContext> history, Map<String, Double> scores, Map<String, Map<String, Double>> strategyContributions) {
/* 1034 */     Map<String, Double> contributions = new HashMap<>();
/* 1035 */     contributions.put("melee", Double.valueOf(0.0D));
/* 1036 */     contributions.put("ranged", Double.valueOf(0.0D));
/* 1037 */     contributions.put("magic", Double.valueOf(0.0D));
/*      */     
/* 1039 */     if (current.getFightPhase() == null || history.size() < 2)
/*      */     {
/* 1041 */       return contributions;
/*      */     }
/*      */     
/* 1044 */     String currentPhase = current.getFightPhase();
/* 1045 */     Map<String, Double> weightedCounts = new HashMap<>();
/* 1046 */     weightedCounts.put("melee", Double.valueOf(0.0D));
/* 1047 */     weightedCounts.put("ranged", Double.valueOf(0.0D));
/* 1048 */     weightedCounts.put("magic", Double.valueOf(0.0D));
/* 1049 */     double totalWeight = 0.0D;
/*      */ 
/*      */     
/* 1052 */     for (int i = 0; i < history.size() - 1; i++) {
/*      */       
/* 1054 */       ExtendedAttackContext historical = history.get(i);
/*      */       
/* 1056 */       if (currentPhase.equals(historical.getFightPhase())) {
/*      */ 
/*      */         
/* 1059 */         double contextSimilarity = calculateExtendedContextSimilarity(current, historical);
/*      */ 
/*      */ 
/*      */         
/* 1063 */         if (contextSimilarity >= 0.7D) {
/*      */ 
/*      */           
/* 1066 */           ExtendedAttackContext nextContext = history.get(i + 1);
/* 1067 */           String nextAttack = nextContext.getAttackStyle();
/*      */           
/* 1069 */           if (nextAttack != null && weightedCounts.containsKey(nextAttack)) {
/*      */ 
/*      */             
/* 1072 */             double weight = contextSimilarity * 10.0D;
/* 1073 */             weightedCounts.put(nextAttack, Double.valueOf(((Double)weightedCounts.get(nextAttack)).doubleValue() + weight));
/* 1074 */             totalWeight += weight;
/*      */           } 
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */     
/* 1081 */     if (totalWeight > 0.0D) {
/*      */       
/* 1083 */       String methodName = "phase_aware_" + currentPhase.toLowerCase();
/* 1084 */       for (String attackType : weightedCounts.keySet()) {
/*      */         
/* 1086 */         double score = ((Double)weightedCounts.get(attackType)).doubleValue() / totalWeight * 10.0D;
/* 1087 */         scores.put(attackType, Double.valueOf(((Double)scores.get(attackType)).doubleValue() + score));
/* 1088 */         contributions.put(attackType, Double.valueOf(score));
/*      */ 
/*      */         
/* 1091 */         if (!strategyContributions.containsKey(attackType))
/*      */         {
/* 1093 */           strategyContributions.put(attackType, new HashMap<>());
/*      */         }
/* 1095 */         ((Map<String, Double>)strategyContributions.get(attackType)).put(methodName, Double.valueOf(score));
/*      */       } 
/*      */     } 
/*      */     
/* 1099 */     return contributions;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private Map<String, Double> calculateMovementBasedPrediction(ExtendedAttackContext current, List<ExtendedAttackContext> history, Map<String, Double> scores, Map<String, Map<String, Double>> strategyContributions) {
/* 1110 */     Map<String, Double> contributions = new HashMap<>();
/* 1111 */     contributions.put("melee", Double.valueOf(0.0D));
/* 1112 */     contributions.put("ranged", Double.valueOf(0.0D));
/* 1113 */     contributions.put("magic", Double.valueOf(0.0D));
/*      */     
/* 1115 */     if (history.size() < 2)
/*      */     {
/* 1117 */       return contributions;
/*      */     }
/*      */     
/* 1120 */     Map<String, Double> weightedCounts = new HashMap<>();
/* 1121 */     weightedCounts.put("melee", Double.valueOf(0.0D));
/* 1122 */     weightedCounts.put("ranged", Double.valueOf(0.0D));
/* 1123 */     weightedCounts.put("magic", Double.valueOf(0.0D));
/* 1124 */     double totalWeight = 0.0D;
/*      */ 
/*      */     
/* 1127 */     int currentDistance = current.getDistance();
/* 1128 */     int currentDistanceChange = current.getDistanceChange();
/* 1129 */     boolean currentUnderTarget = current.isUnderTarget();
/* 1130 */     boolean currentTargetUnderUs = current.isTargetUnderUs();
/* 1131 */     String currentMovementDirection = current.getMovementDirection();
/* 1132 */     boolean currentPlayerLastMovement = current.isPlayerLastMovement();
/* 1133 */     boolean currentOpponentLastMovement = current.isOpponentLastMovement();
/*      */ 
/*      */ 
/*      */     
/* 1137 */     for (int i = 0; i < history.size() - 1; i++) {
/*      */       
/* 1139 */       ExtendedAttackContext historical = history.get(i);
/*      */ 
/*      */ 
/*      */ 
/*      */       
/* 1144 */       int historyPosition = history.size() - 1 - i;
/* 1145 */       double recencyMultiplier = (historyPosition <= 50) ? 3.0D : 1.0D;
/*      */ 
/*      */       
/* 1148 */       double movementSimilarity = 0.0D;
/* 1149 */       int movementMatches = 0;
/* 1150 */       int totalMovementFactors = 0;
/*      */ 
/*      */       
/* 1153 */       if (currentDistance >= 0 && historical.getDistance() >= 0) {
/*      */         
/* 1155 */         totalMovementFactors++;
/* 1156 */         int distanceDiff = Math.abs(currentDistance - historical.getDistance());
/* 1157 */         if (distanceDiff <= 1)
/*      */         {
/* 1159 */           movementMatches++;
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/* 1164 */       if (Math.abs(currentDistanceChange - historical.getDistanceChange()) <= 1) {
/*      */         
/* 1166 */         totalMovementFactors++;
/* 1167 */         movementMatches++;
/*      */       } 
/*      */ 
/*      */       
/* 1171 */       if (currentUnderTarget == historical.isUnderTarget()) {
/*      */         
/* 1173 */         totalMovementFactors++;
/* 1174 */         movementMatches++;
/*      */       } 
/*      */ 
/*      */       
/* 1178 */       if (currentTargetUnderUs == historical.isTargetUnderUs()) {
/*      */         
/* 1180 */         totalMovementFactors++;
/* 1181 */         movementMatches++;
/*      */       } 
/*      */ 
/*      */       
/* 1185 */       if (currentMovementDirection != null && currentMovementDirection.equals(historical.getMovementDirection())) {
/*      */         
/* 1187 */         totalMovementFactors++;
/* 1188 */         movementMatches++;
/*      */       } 
/*      */ 
/*      */       
/* 1192 */       if (currentPlayerLastMovement == historical.isPlayerLastMovement()) {
/*      */         
/* 1194 */         totalMovementFactors++;
/* 1195 */         movementMatches++;
/*      */       } 
/*      */ 
/*      */       
/* 1199 */       if (currentOpponentLastMovement == historical.isOpponentLastMovement()) {
/*      */         
/* 1201 */         totalMovementFactors++;
/* 1202 */         movementMatches++;
/*      */       } 
/*      */       
/* 1205 */       if (totalMovementFactors > 0)
/*      */       {
/* 1207 */         movementSimilarity = movementMatches / totalMovementFactors;
/*      */       }
/*      */ 
/*      */ 
/*      */       
/* 1212 */       if (movementSimilarity >= 0.8D) {
/*      */ 
/*      */         
/* 1215 */         ExtendedAttackContext nextContext = history.get(i + 1);
/* 1216 */         String nextAttack = nextContext.getAttackStyle();
/*      */         
/* 1218 */         if (nextAttack != null && weightedCounts.containsKey(nextAttack)) {
/*      */ 
/*      */ 
/*      */           
/* 1222 */           double weight = movementSimilarity * 25.0D * recencyMultiplier;
/* 1223 */           weightedCounts.put(nextAttack, Double.valueOf(((Double)weightedCounts.get(nextAttack)).doubleValue() + weight));
/* 1224 */           totalWeight += weight;
/*      */ 
/*      */           
/* 1227 */           if (this.config != null && this.config.debug() && i < 5)
/*      */           {
/* 1229 */             Logger.norm("[Movement Match] History[" + i + "] -> " + nextAttack + " (similarity: " + 
/* 1230 */                 String.format("%.2f", new Object[] { Double.valueOf(movementSimilarity) }) + ", distance: " + historical
/* 1231 */                 .getDistance() + ", freeze: " + historical
/* 1232 */                 .getFreezeState() + ")");
/*      */           }
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */     
/* 1239 */     if (totalWeight > 0.0D) {
/*      */ 
/*      */       
/* 1242 */       double magicWeight = ((Double)weightedCounts.get("magic")).doubleValue();
/* 1243 */       double meleeWeight = ((Double)weightedCounts.get("melee")).doubleValue();
/* 1244 */       double rangedWeight = ((Double)weightedCounts.get("ranged")).doubleValue();
/* 1245 */       Logger.norm("[Movement Strategy] Total weight: " + String.format("%.2f", new Object[] { Double.valueOf(totalWeight)
/* 1246 */             }) + ", Magic: " + String.format("%.2f", new Object[] { Double.valueOf(magicWeight)
/* 1247 */             }) + ", Melee: " + String.format("%.2f", new Object[] { Double.valueOf(meleeWeight)
/* 1248 */             }) + ", Ranged: " + String.format("%.2f", new Object[] { Double.valueOf(rangedWeight) }));
/*      */       
/* 1250 */       for (String attackType : weightedCounts.keySet())
/*      */       {
/* 1252 */         double score = ((Double)weightedCounts.get(attackType)).doubleValue() / totalWeight * 25.0D;
/* 1253 */         scores.put(attackType, Double.valueOf(((Double)scores.get(attackType)).doubleValue() + score));
/* 1254 */         contributions.put(attackType, Double.valueOf(score));
/*      */ 
/*      */         
/* 1257 */         if (!strategyContributions.containsKey(attackType))
/*      */         {
/* 1259 */           strategyContributions.put(attackType, new HashMap<>());
/*      */         }
/* 1261 */         ((Map<String, Double>)strategyContributions.get(attackType)).put("movement_based", Double.valueOf(score));
/*      */       }
/*      */     
/*      */     } else {
/*      */       
/* 1266 */       Logger.norm("[Movement Strategy] No matches found - totalWeight: 0");
/*      */     } 
/*      */     
/* 1269 */     return contributions;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private Map<String, Double> calculateResourceBasedPrediction(ExtendedAttackContext current, List<ExtendedAttackContext> history, Map<String, Double> scores, Map<String, Map<String, Double>> strategyContributions) {
/* 1280 */     Map<String, Double> contributions = new HashMap<>();
/* 1281 */     contributions.put("melee", Double.valueOf(0.0D));
/* 1282 */     contributions.put("ranged", Double.valueOf(0.0D));
/* 1283 */     contributions.put("magic", Double.valueOf(0.0D));
/*      */     
/* 1285 */     if (history.size() < 2)
/*      */     {
/* 1287 */       return contributions;
/*      */     }
/*      */     
/* 1290 */     Map<String, Double> weightedCounts = new HashMap<>();
/* 1291 */     weightedCounts.put("melee", Double.valueOf(0.0D));
/* 1292 */     weightedCounts.put("ranged", Double.valueOf(0.0D));
/* 1293 */     weightedCounts.put("magic", Double.valueOf(0.0D));
/* 1294 */     double totalWeight = 0.0D;
/*      */ 
/*      */     
/* 1297 */     int currentOpponentHP = current.getTargetHP();
/* 1298 */     int currentOpponentFoodCount = current.getOpponentFoodCount();
/* 1299 */     int currentOpponentBrewCount = current.getOpponentBrewCount();
/* 1300 */     int currentOpponentPrayerPoints = current.getOpponentPrayerPoints();
/* 1301 */     boolean currentOpponentLastFood = current.isOpponentLastFood();
/* 1302 */     boolean currentOpponentLastBrew = current.isOpponentLastBrew();
/*      */ 
/*      */ 
/*      */     
/* 1306 */     for (int i = 0; i < history.size() - 1; i++) {
/*      */       
/* 1308 */       ExtendedAttackContext historical = history.get(i);
/*      */ 
/*      */ 
/*      */ 
/*      */       
/* 1313 */       int historyPosition = history.size() - 1 - i;
/* 1314 */       double recencyMultiplier = (historyPosition <= 50) ? 3.0D : 1.0D;
/*      */ 
/*      */       
/* 1317 */       double resourceSimilarity = 0.0D;
/* 1318 */       int resourceMatches = 0;
/* 1319 */       int totalResourceFactors = 0;
/*      */ 
/*      */       
/* 1322 */       if (currentOpponentHP >= 0 && historical.getTargetHP() >= 0) {
/*      */         
/* 1324 */         totalResourceFactors++;
/* 1325 */         int hpDiff = Math.abs(currentOpponentHP - historical.getTargetHP());
/* 1326 */         if (hpDiff <= 10)
/*      */         {
/* 1328 */           resourceMatches++;
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/* 1333 */       if (currentOpponentFoodCount >= 0 && historical.getOpponentFoodCount() >= 0) {
/*      */         
/* 1335 */         totalResourceFactors++;
/* 1336 */         int foodDiff = Math.abs(currentOpponentFoodCount - historical.getOpponentFoodCount());
/* 1337 */         if (foodDiff <= 2)
/*      */         {
/* 1339 */           resourceMatches++;
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/* 1344 */       if (currentOpponentBrewCount >= 0 && historical.getOpponentBrewCount() >= 0) {
/*      */         
/* 1346 */         totalResourceFactors++;
/* 1347 */         int brewDiff = Math.abs(currentOpponentBrewCount - historical.getOpponentBrewCount());
/* 1348 */         if (brewDiff <= 2)
/*      */         {
/* 1350 */           resourceMatches++;
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/* 1355 */       if (currentOpponentPrayerPoints >= 0 && historical.getOpponentPrayerPoints() >= 0) {
/*      */         
/* 1357 */         totalResourceFactors++;
/* 1358 */         int prayerDiff = Math.abs(currentOpponentPrayerPoints - historical.getOpponentPrayerPoints());
/* 1359 */         if (prayerDiff <= 20)
/*      */         {
/* 1361 */           resourceMatches++;
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/* 1366 */       if (currentOpponentLastFood == historical.isOpponentLastFood()) {
/*      */         
/* 1368 */         totalResourceFactors++;
/* 1369 */         resourceMatches++;
/*      */       } 
/*      */ 
/*      */       
/* 1373 */       if (currentOpponentLastBrew == historical.isOpponentLastBrew()) {
/*      */         
/* 1375 */         totalResourceFactors++;
/* 1376 */         resourceMatches++;
/*      */       } 
/*      */       
/* 1379 */       if (totalResourceFactors > 0)
/*      */       {
/* 1381 */         resourceSimilarity = resourceMatches / totalResourceFactors;
/*      */       }
/*      */ 
/*      */ 
/*      */       
/* 1386 */       if (resourceSimilarity >= 0.8D) {
/*      */ 
/*      */         
/* 1389 */         ExtendedAttackContext nextContext = history.get(i + 1);
/* 1390 */         String nextAttack = nextContext.getAttackStyle();
/*      */         
/* 1392 */         if (nextAttack != null && weightedCounts.containsKey(nextAttack)) {
/*      */ 
/*      */ 
/*      */           
/* 1396 */           double weight = resourceSimilarity * 25.0D * recencyMultiplier;
/* 1397 */           weightedCounts.put(nextAttack, Double.valueOf(((Double)weightedCounts.get(nextAttack)).doubleValue() + weight));
/* 1398 */           totalWeight += weight;
/*      */ 
/*      */           
/* 1401 */           if (this.config != null && this.config.debug() && i < 5)
/*      */           {
/* 1403 */             Logger.norm("[Resource Match] History[" + i + "] -> " + nextAttack + " (similarity: " + 
/* 1404 */                 String.format("%.2f", new Object[] { Double.valueOf(resourceSimilarity) }) + ", HP: " + historical
/* 1405 */                 .getTargetHP() + ", freeze: " + historical
/* 1406 */                 .getFreezeState() + ")");
/*      */           }
/*      */         } 
/*      */       } 
/*      */     } 
/*      */ 
/*      */     
/* 1413 */     if (totalWeight > 0.0D) {
/*      */ 
/*      */       
/* 1416 */       double magicWeight = ((Double)weightedCounts.get("magic")).doubleValue();
/* 1417 */       double meleeWeight = ((Double)weightedCounts.get("melee")).doubleValue();
/* 1418 */       double rangedWeight = ((Double)weightedCounts.get("ranged")).doubleValue();
/* 1419 */       Logger.norm("[Resource Strategy] Total weight: " + String.format("%.2f", new Object[] { Double.valueOf(totalWeight)
/* 1420 */             }) + ", Magic: " + String.format("%.2f", new Object[] { Double.valueOf(magicWeight)
/* 1421 */             }) + ", Melee: " + String.format("%.2f", new Object[] { Double.valueOf(meleeWeight)
/* 1422 */             }) + ", Ranged: " + String.format("%.2f", new Object[] { Double.valueOf(rangedWeight) }));
/*      */       
/* 1424 */       for (String attackType : weightedCounts.keySet())
/*      */       {
/* 1426 */         double score = ((Double)weightedCounts.get(attackType)).doubleValue() / totalWeight * 25.0D;
/* 1427 */         scores.put(attackType, Double.valueOf(((Double)scores.get(attackType)).doubleValue() + score));
/* 1428 */         contributions.put(attackType, Double.valueOf(score));
/*      */ 
/*      */         
/* 1431 */         if (!strategyContributions.containsKey(attackType))
/*      */         {
/* 1433 */           strategyContributions.put(attackType, new HashMap<>());
/*      */         }
/* 1435 */         ((Map<String, Double>)strategyContributions.get(attackType)).put("resource_based", Double.valueOf(score));
/*      */       }
/*      */     
/*      */     } else {
/*      */       
/* 1440 */       Logger.norm("[Resource Strategy] No matches found - totalWeight: 0");
/*      */     } 
/*      */     
/* 1443 */     return contributions;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private double calculateExtendedContextSimilarity(ExtendedAttackContext current, ExtendedAttackContext historical) {
/* 1452 */     double totalScore = 0.0D;
/* 1453 */     double totalWeight = 0.0D;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1460 */     if (current.getFreezeState() != null && historical.getFreezeState() != null) {
/*      */       
/* 1462 */       double freezeScore = current.getFreezeState().equals(historical.getFreezeState()) ? 1.0D : 0.0D;
/* 1463 */       totalScore += freezeScore * 0.15D;
/* 1464 */       totalWeight += 0.15D;
/*      */     } 
/*      */ 
/*      */     
/* 1468 */     if (current.getTargetHP() >= 0 && historical.getTargetHP() >= 0) {
/*      */       
/* 1470 */       int hpDiff = Math.abs(current.getTargetHP() - historical.getTargetHP());
/* 1471 */       double hpScore = (hpDiff <= 10) ? 1.0D : ((hpDiff <= 20) ? 0.7D : ((hpDiff <= 30) ? 0.4D : 0.1D));
/* 1472 */       totalScore += hpScore * 0.25D;
/* 1473 */       totalWeight += 0.25D;
/*      */     } 
/*      */ 
/*      */     
/* 1477 */     if (current.getTargetSpec() >= 0 && historical.getTargetSpec() >= 0) {
/*      */       
/* 1479 */       int specDiff = Math.abs(current.getTargetSpec() - historical.getTargetSpec());
/* 1480 */       double specScore = (specDiff <= 10) ? 1.0D : ((specDiff <= 20) ? 0.7D : ((specDiff <= 30) ? 0.4D : 0.1D));
/* 1481 */       totalScore += specScore * 0.2D;
/* 1482 */       totalWeight += 0.2D;
/*      */     } 
/*      */ 
/*      */     
/* 1486 */     if (current.getOverheadPrayer() != null && historical.getOverheadPrayer() != null) {
/*      */       
/* 1488 */       double prayerScore = current.getOverheadPrayer().equals(historical.getOverheadPrayer()) ? 1.0D : 0.5D;
/* 1489 */       totalScore += prayerScore * 0.2D;
/* 1490 */       totalWeight += 0.2D;
/*      */     } 
/*      */ 
/*      */     
/* 1494 */     if (current.getDistance() >= 0 && historical.getDistance() >= 0) {
/*      */       
/* 1496 */       int distanceDiff = Math.abs(current.getDistance() - historical.getDistance());
/* 1497 */       double distanceScore = (distanceDiff <= 1) ? 1.0D : ((distanceDiff <= 2) ? 0.8D : ((distanceDiff <= 4) ? 0.5D : 0.2D));
/* 1498 */       totalScore += distanceScore * 0.2D;
/* 1499 */       totalWeight += 0.2D;
/*      */     } 
/*      */ 
/*      */     
/* 1503 */     if (current.getPlayerLastAttack() != null && historical.getPlayerLastAttack() != null) {
/*      */       
/* 1505 */       double playerAttackScore = current.getPlayerLastAttack().equals(historical.getPlayerLastAttack()) ? 1.0D : 0.0D;
/* 1506 */       totalScore += playerAttackScore * 0.05D;
/* 1507 */       totalWeight += 0.05D;
/*      */     } 
/*      */ 
/*      */     
/* 1511 */     if (totalWeight > 0.0D)
/*      */     {
/* 1513 */       return totalScore / totalWeight;
/*      */     }
/*      */     
/* 1516 */     return 0.0D;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private String generateExtendedCacheKey(ExtendedAttackContext context, int historySize) {
/* 1524 */     if (context == null)
/*      */     {
/* 1526 */       return null;
/*      */     }
/*      */ 
/*      */     
/* 1530 */     int hpRange = context.getTargetHP() / 10 * 10;
/*      */ 
/*      */     
/* 1533 */     int specRange = context.getTargetSpec() / 10 * 10;
/*      */ 
/*      */     
/* 1536 */     StringBuilder key = new StringBuilder();
/* 1537 */     key.append((context.getOverheadPrayer() != null) ? context.getOverheadPrayer() : "none");
/* 1538 */     key.append("|").append(hpRange);
/* 1539 */     key.append("|").append((context.getFreezeState() != null) ? context.getFreezeState() : "none");
/* 1540 */     key.append("|").append((context.getWeapon() != null) ? context.getWeapon() : "unknown");
/* 1541 */     key.append("|").append(specRange);
/* 1542 */     key.append("|").append((context.getFightPhase() != null) ? context.getFightPhase() : "unknown");
/* 1543 */     key.append("|").append(context.isUnderTarget());
/* 1544 */     key.append("|").append(context.isTargetUnderUs());
/* 1545 */     key.append("|").append(context.getDistance());
/* 1546 */     key.append("|").append(historySize);
/*      */ 
/*      */     
/* 1549 */     if (context.getAttackSequence() != null && !context.getAttackSequence().isEmpty()) {
/*      */       
/* 1551 */       int seqSize = Math.min(3, context.getAttackSequence().size());
/* 1552 */       List<String> lastSeq = context.getAttackSequence().subList(context
/* 1553 */           .getAttackSequence().size() - seqSize, context
/* 1554 */           .getAttackSequence().size());
/*      */       
/* 1556 */       key.append("|").append(String.join("->", (Iterable)lastSeq));
/*      */     } 
/*      */     
/* 1559 */     return key.toString();
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public void clearCache() {
/* 1567 */     this.lastCacheKey = null;
/* 1568 */     this.cachedPredictions = null;
/* 1569 */     this.cachedHistorySize = -1;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private Map<String, Double> calculateRewardBasedPrediction(ExtendedAttackContext currentContext, Map<String, Double> scores, Map<String, Map<String, Double>> strategyContributions) {
/* 1581 */     Map<String, Double> contributions = new HashMap<>();
/* 1582 */     contributions.put("melee", Double.valueOf(0.0D));
/* 1583 */     contributions.put("ranged", Double.valueOf(0.0D));
/* 1584 */     contributions.put("magic", Double.valueOf(0.0D));
/*      */     
/* 1586 */     if (this.rewardTracker == null)
/*      */     {
/* 1588 */       return contributions;
/*      */     }
/*      */ 
/*      */ 
/*      */     
/*      */     try {
/* 1594 */       Map<String, Double> rewardScores = this.rewardTracker.getCurrentRewardScores();
/*      */ 
/*      */       
/* 1597 */       double totalReward = ((Double)rewardScores.get("melee")).doubleValue() + ((Double)rewardScores.get("ranged")).doubleValue() + ((Double)rewardScores.get("magic")).doubleValue();
/*      */       
/* 1599 */       if (totalReward > 0.0D)
/*      */       {
/*      */         
/* 1602 */         double meleeReward = ((Double)rewardScores.get("melee")).doubleValue() / totalReward * 5.0D;
/* 1603 */         double rangedReward = ((Double)rewardScores.get("ranged")).doubleValue() / totalReward * 5.0D;
/* 1604 */         double magicReward = ((Double)rewardScores.get("magic")).doubleValue() / totalReward * 5.0D;
/*      */ 
/*      */         
/* 1607 */         scores.put("melee", Double.valueOf(((Double)scores.get("melee")).doubleValue() + meleeReward));
/* 1608 */         scores.put("ranged", Double.valueOf(((Double)scores.get("ranged")).doubleValue() + rangedReward));
/* 1609 */         scores.put("magic", Double.valueOf(((Double)scores.get("magic")).doubleValue() + magicReward));
/*      */ 
/*      */         
/* 1612 */         contributions.put("melee", Double.valueOf(meleeReward));
/* 1613 */         contributions.put("ranged", Double.valueOf(rangedReward));
/* 1614 */         contributions.put("magic", Double.valueOf(magicReward));
/*      */         
/* 1616 */         ((Map<String, Double>)strategyContributions.get("melee")).put("reward_based", Double.valueOf(meleeReward));
/* 1617 */         ((Map<String, Double>)strategyContributions.get("ranged")).put("reward_based", Double.valueOf(rangedReward));
/* 1618 */         ((Map<String, Double>)strategyContributions.get("magic")).put("reward_based", Double.valueOf(magicReward));
/*      */       }
/*      */       else
/*      */       {
/* 1622 */         Logger.norm("[Enhanced Predictor] Reward strategy contributed 0 - totalReward: " + totalReward + ", rewardTracker: " + (
/* 1623 */             (this.rewardTracker != null) ? "exists" : "null"));
/*      */       }
/*      */     
/* 1626 */     } catch (Exception exception) {}
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     
/* 1632 */     return contributions;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private List<PredictionResult> getDefaultPredictions() {
/* 1640 */     List<PredictionResult> predictions = new ArrayList<>();
/* 1641 */     predictions.add(new PredictionResult("melee", 33.33D, "default"));
/* 1642 */     predictions.add(new PredictionResult("ranged", 33.33D, "default"));
/* 1643 */     predictions.add(new PredictionResult("magic", 33.34D, "default"));
/* 1644 */     return predictions;
/*      */   }
/*      */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/EnhancedAttackPredictor.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */