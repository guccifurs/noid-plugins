/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.ui.VitaOverlay;
/*     */ import java.awt.Color;
/*     */ import java.awt.Dimension;
/*     */ import java.awt.Graphics2D;
/*     */ import net.runelite.client.ui.overlay.OverlayPosition;
/*     */ 
/*     */ 
/*     */ public class SpecialMovesOverlay
/*     */   extends VitaOverlay
/*     */ {
/*     */   private final AttackTimerConfig config;
/*  14 */   private SpecialMovesStatus status = SpecialMovesStatus.IDLE;
/*  15 */   private String combatState = "Idle";
/*     */ 
/*     */ 
/*     */   
/*     */   public SpecialMovesOverlay(AttackTimerConfig config) {
/*  20 */     this.config = config;
/*  21 */     setPosition(OverlayPosition.TOP_LEFT);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateStatus(SpecialMovesStatus newStatus) {
/*  29 */     this.status = newStatus;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateCombatState(String state) {
/*  37 */     this.combatState = (state != null) ? state : "Idle";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public SpecialMovesStatus getStatus() {
/*  45 */     return this.status;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public Dimension render(Graphics2D graphics) {
/*  53 */     clear();
/*  54 */     setWidth(180);
/*     */ 
/*     */     
/*  57 */     Color statusColor = (this.status == SpecialMovesStatus.ACTIVATED) ? Color.GREEN : Color.GRAY;
/*     */ 
/*     */     
/*  60 */     if (isSpecialMovesEnabled())
/*     */     {
/*  62 */       newLine("Status: " + this.status.getDisplayName(), 14, statusColor);
/*     */     }
/*     */ 
/*     */     
/*  66 */     newLine("State: " + this.combatState, 14, Color.CYAN);
/*     */     
/*  68 */     return super.render(graphics);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean isSpecialMovesEnabled() {
/*  76 */     return (this.config.specialMove1Ags() > 0 || this.config
/*  77 */       .specialMove2() > 0 || this.config
/*  78 */       .specialMove3() > 0 || this.config
/*  79 */       .freeHit() || this.config
/*  80 */       .rackson() > 0);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateCombatStateName(String stateName) {
/*  88 */     this.combatState = (stateName != null) ? stateName : "Idle";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public enum SpecialMovesStatus
/*     */   {
/*  96 */     IDLE("Idle"),
/*  97 */     ACTIVATED("Activated");
/*     */     
/*     */     private final String displayName;
/*     */ 
/*     */     
/*     */     SpecialMovesStatus(String displayName) {
/* 103 */       this.displayName = displayName;
/*     */     }
/*     */ 
/*     */     
/*     */     public String getDisplayName() {
/* 108 */       return this.displayName;
/*     */     }
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/SpecialMovesOverlay.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */