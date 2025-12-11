/*    */ package com.tonic.plugins.attacktimer;
/*    */ 
/*    */ public class PredictionResult
/*    */   implements Comparable<PredictionResult>
/*    */ {
/*    */   private final String attackType;
/*    */   private final double qualityScore;
/*    */   private final String method;
/*    */   
/*    */   public String getAttackType() {
/* 11 */     return this.attackType;
/* 12 */   } public double getQualityScore() { return this.qualityScore; } public String getMethod() {
/* 13 */     return this.method;
/*    */   }
/*    */   
/*    */   public PredictionResult(String attackType, double qualityScore, String method) {
/* 17 */     this.attackType = attackType;
/* 18 */     this.qualityScore = qualityScore;
/* 19 */     this.method = method;
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public int compareTo(PredictionResult other) {
/* 26 */     return Double.compare(other.qualityScore, this.qualityScore);
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   public String toString() {
/* 32 */     return String.format("%s: %.1f%% (%s)", new Object[] { this.attackType, Double.valueOf(this.qualityScore), this.method });
/*    */   }
/*    */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/PredictionResult.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */