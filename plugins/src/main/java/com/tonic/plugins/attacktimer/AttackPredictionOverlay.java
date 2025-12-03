/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.ui.VitaOverlay;
/*     */ import java.awt.Color;
/*     */ import java.awt.Dimension;
/*     */ import java.awt.Graphics2D;
/*     */ import java.util.List;
/*     */ import net.runelite.client.ui.overlay.OverlayPosition;
/*     */ 
/*     */ 
/*     */ public class AttackPredictionOverlay
/*     */   extends VitaOverlay
/*     */ {
/*     */   private final AttackTimerConfig config;
/*     */   private List<PredictionResult> predictions;
/*  16 */   private int correctCount = 0;
/*  17 */   private int totalCount = 0;
/*  18 */   private String predictorType = "Basic";
/*  19 */   private String predictionMethod = "";
/*  20 */   private PredictionProgress progress = null;
/*     */ 
/*     */ 
/*     */   
/*     */   public AttackPredictionOverlay(AttackTimerConfig config) {
/*  25 */     this.config = config;
/*  26 */     setPosition(OverlayPosition.TOP_RIGHT);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePredictions(List<PredictionResult> predictions) {
/*  34 */     this.predictions = predictions;
/*     */ 
/*     */     
/*  37 */     if (predictions != null && !predictions.isEmpty()) {
/*     */       
/*  39 */       PredictionResult topPred = predictions.get(0);
/*  40 */       String method = topPred.getMethod();
/*     */ 
/*     */       
/*  43 */       if (method != null && (method
/*  44 */         .startsWith("freeze_state_based") || method
/*  45 */         .startsWith("sequence_match") || method
/*  46 */         .startsWith("temporal_pattern") || method
/*  47 */         .startsWith("phase_aware_") || method
/*  48 */         .startsWith("movement_based") || method
/*  49 */         .startsWith("resource_based"))) {
/*     */ 
/*     */         
/*  52 */         this.predictorType = "Enhanced";
/*  53 */         this.predictionMethod = method;
/*     */       }
/*     */       else {
/*     */         
/*  57 */         this.predictorType = "Basic";
/*  58 */         this.predictionMethod = (method != null) ? method : "default";
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updatePredictions(List<PredictionResult> predictions, String predictorType) {
/*  69 */     this.predictions = predictions;
/*  70 */     this.predictorType = (predictorType != null) ? predictorType : "Basic";
/*     */ 
/*     */     
/*  73 */     if (predictions != null && !predictions.isEmpty()) {
/*     */       
/*  75 */       PredictionResult topPred = predictions.get(0);
/*  76 */       String method = topPred.getMethod();
/*  77 */       this.predictionMethod = (method != null) ? method : "default";
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateAccuracy(int correct, int total) {
/*  86 */     this.correctCount = correct;
/*  87 */     this.totalCount = total;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateProgress(PredictionProgress progress) {
/*  95 */     this.progress = progress;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public Dimension render(Graphics2D graphics) {
/* 101 */     if (!this.config.attackContextPrediction() || !this.config.predictionOverlay())
/*     */     {
/* 103 */       return null;
/*     */     }
/*     */     
/* 106 */     if (this.predictions == null || this.predictions.isEmpty())
/*     */     {
/* 108 */       return null;
/*     */     }
/*     */     
/* 111 */     clear();
/* 112 */     setWidth(280);
/*     */ 
/*     */     
/* 115 */     int baseHeight = 220;
/* 116 */     int progressBarHeight = (this.progress != null) ? 40 : 0;
/* 117 */     setHeight(baseHeight + progressBarHeight);
/*     */ 
/*     */     
/* 120 */     newLine("Attack Predictions", 14, Color.CYAN);
/* 121 */     newLine("━━━━━━━━━━━━━━━━━━━━", 10, Color.GRAY);
/*     */ 
/*     */     
/* 124 */     Color predictorColor = this.predictorType.equals("Enhanced") ? Color.GREEN : Color.YELLOW;
/* 125 */     newLine("Predictor: " + this.predictorType, 11, predictorColor);
/*     */     
/* 127 */     if (this.predictionMethod != null && !this.predictionMethod.isEmpty() && !this.predictionMethod.equals("default")) {
/*     */       
/* 129 */       String methodDisplay = formatMethodName(this.predictionMethod);
/* 130 */       newLine("Method: " + methodDisplay, 10, Color.LIGHT_GRAY);
/*     */     } 
/*     */ 
/*     */     
/* 134 */     if (this.progress != null) {
/*     */       Color barColor;
/* 136 */       newLine("━━━━━━━━━━━━━━━━━━━━", 10, Color.GRAY);
/* 137 */       newLine("Enhanced Predictor Progress", 10, Color.CYAN);
/*     */       
/* 139 */       double progressPercent = this.progress.getProgressPercent();
/* 140 */       String statusMessage = this.progress.getStatusMessage();
/*     */ 
/*     */       
/* 143 */       int barWidth = 20;
/* 144 */       int filledWidth = (int)(progressPercent / 100.0D * barWidth);
/* 145 */       String bar = "";
/*     */ 
/*     */ 
/*     */       
/* 149 */       if (progressPercent < 25.0D) {
/*     */         
/* 151 */         barColor = Color.RED;
/*     */       }
/* 153 */       else if (progressPercent < 50.0D) {
/*     */         
/* 155 */         barColor = Color.ORANGE;
/*     */       }
/* 157 */       else if (progressPercent < 75.0D) {
/*     */         
/* 159 */         barColor = Color.YELLOW;
/*     */       }
/* 161 */       else if (progressPercent < 95.0D) {
/*     */         
/* 163 */         barColor = new Color(144, 238, 144);
/*     */       }
/*     */       else {
/*     */         
/* 167 */         barColor = Color.GREEN;
/*     */       } 
/*     */ 
/*     */       
/* 171 */       for (int j = 0; j < barWidth; j++) {
/*     */         
/* 173 */         if (j < filledWidth) {
/*     */           
/* 175 */           bar = bar + "█";
/*     */         }
/*     */         else {
/*     */           
/* 179 */           bar = bar + "░";
/*     */         } 
/*     */       } 
/*     */ 
/*     */       
/* 184 */       String progressLine = String.format("%s %.0f%%", new Object[] { bar, Double.valueOf(progressPercent) });
/* 185 */       newLine(progressLine, 11, barColor);
/*     */ 
/*     */       
/* 188 */       newLine(statusMessage, 9, Color.LIGHT_GRAY);
/*     */     } 
/*     */     
/* 191 */     newLine("━━━━━━━━━━━━━━━━━━━━", 10, Color.GRAY);
/*     */ 
/*     */     
/* 194 */     if (this.totalCount > 0) {
/*     */       
/* 196 */       double accuracyPercent = this.correctCount * 100.0D / this.totalCount;
/* 197 */       Color accuracyColor = (accuracyPercent >= 70.0D) ? Color.GREEN : ((accuracyPercent >= 50.0D) ? Color.YELLOW : Color.RED);
/* 198 */       String accuracyText = String.format("Accuracy: %d/%d (%.1f%%)", new Object[] { Integer.valueOf(this.correctCount), Integer.valueOf(this.totalCount), Double.valueOf(accuracyPercent) });
/* 199 */       newLine(accuracyText, 12, accuracyColor);
/* 200 */       newLine("━━━━━━━━━━━━━━━━━━━━", 10, Color.GRAY);
/*     */     } 
/*     */ 
/*     */     
/* 204 */     int count = Math.min(5, this.predictions.size());
/* 205 */     for (int i = 0; i < count; i++) {
/*     */       Color attackColor;
/* 207 */       PredictionResult pred = this.predictions.get(i);
/* 208 */       String attackType = pred.getAttackType();
/* 209 */       double score = pred.getQualityScore();
/*     */ 
/*     */ 
/*     */       
/* 213 */       switch (attackType.toLowerCase()) {
/*     */         
/*     */         case "melee":
/* 216 */           attackColor = Color.RED;
/*     */           break;
/*     */         case "ranged":
/* 219 */           attackColor = Color.GREEN;
/*     */           break;
/*     */         case "magic":
/* 222 */           attackColor = Color.BLUE;
/*     */           break;
/*     */         default:
/* 225 */           attackColor = Color.WHITE;
/*     */           break;
/*     */       } 
/*     */       
/* 229 */       int barWidth = 10;
/* 230 */       int filledWidth = (int)(score / 100.0D * barWidth);
/* 231 */       String bar = "";
/* 232 */       for (int j = 0; j < barWidth; j++) {
/*     */         
/* 234 */         if (j < filledWidth) {
/*     */           
/* 236 */           bar = bar + "█";
/*     */         }
/*     */         else {
/*     */           
/* 240 */           bar = bar + "░";
/*     */         } 
/*     */       } 
/*     */ 
/*     */       
/* 245 */       String line = String.format("%-8s %s %.0f%%", new Object[] {
/* 246 */             capitalize(attackType), bar, Double.valueOf(score)
/*     */           });
/* 248 */       newLine(line, 11, attackColor);
/*     */     } 
/*     */     
/* 251 */     return super.render(graphics);
/*     */   }
/*     */ 
/*     */   
/*     */   private String capitalize(String str) {
/* 256 */     if (str == null || str.isEmpty())
/*     */     {
/* 258 */       return str;
/*     */     }
/* 260 */     return str.substring(0, 1).toUpperCase() + str.substring(0, 1).toUpperCase();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private String formatSingleMethodName(String method) {
/* 268 */     if (method == null || method.isEmpty())
/*     */     {
/* 270 */       return "default";
/*     */     }
/*     */     
/* 273 */     switch (method) {
/*     */       
/*     */       case "freeze_state_based":
/* 276 */         return "Freeze State";
/*     */       case "sequence_match":
/* 278 */         return "Sequence";
/*     */       case "temporal_pattern":
/* 280 */         return "Temporal";
/*     */       case "movement_based":
/* 282 */         return "Movement";
/*     */       case "resource_based":
/* 284 */         return "Resource";
/*     */       case "exact_match":
/* 286 */         return "Exact Match";
/*     */       case "partial_match_excellent":
/* 288 */         return "Partial (Excellent)";
/*     */       case "partial_match_very_good":
/* 290 */         return "Partial (Very Good)";
/*     */       case "partial_match_good":
/* 292 */         return "Partial (Good)";
/*     */       case "pattern_frequency":
/* 294 */         return "Pattern Frequency";
/*     */       case "sequence":
/* 296 */         return "Sequence";
/*     */     } 
/*     */     
/* 299 */     if (method.startsWith("phase_aware_")) {
/*     */       
/* 301 */       String phase = method.substring("phase_aware_".length());
/* 302 */       return "Phase (" + capitalize(phase) + ")";
/*     */     } 
/*     */     
/* 305 */     return method.replace("_", " ");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private String formatMethodName(String method) {
/* 314 */     if (method == null || method.isEmpty())
/*     */     {
/* 316 */       return "default";
/*     */     }
/*     */ 
/*     */     
/* 320 */     if (method.contains("+")) {
/*     */       
/* 322 */       String[] parts = method.split("\\+");
/* 323 */       StringBuilder result = new StringBuilder();
/* 324 */       for (int i = 0; i < parts.length; i++) {
/*     */         
/* 326 */         if (i > 0)
/*     */         {
/* 328 */           result.append(" + ");
/*     */         }
/* 330 */         result.append(formatSingleMethodName(parts[i]));
/*     */       } 
/* 332 */       return result.toString();
/*     */     } 
/*     */     
/* 335 */     return formatSingleMethodName(method);
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/AttackPredictionOverlay.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */