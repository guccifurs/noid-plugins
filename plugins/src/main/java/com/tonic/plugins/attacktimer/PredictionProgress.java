/*    */ package com.tonic.plugins.attacktimer;
/*    */ 
/*    */ 
/*    */ public class PredictionProgress
/*    */ {
/*    */   private final double progressPercent;
/*    */   private final double currentConfidence;
/*    */   private final String statusMessage;
/*    */   
/*    */   public double getProgressPercent() {
/* 11 */     return this.progressPercent;
/* 12 */   } public double getCurrentConfidence() { return this.currentConfidence; } public String getStatusMessage() {
/* 13 */     return this.statusMessage;
/*    */   }
/*    */   
/*    */   public PredictionProgress(double progressPercent, double currentConfidence, String statusMessage) {
/* 17 */     this.progressPercent = Math.max(0.0D, Math.min(100.0D, progressPercent));
/* 18 */     this.currentConfidence = currentConfidence;
/* 19 */     this.statusMessage = statusMessage;
/*    */   }
/*    */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/PredictionProgress.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */