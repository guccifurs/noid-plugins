/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import com.tonic.api.game.VarAPI;
/*     */ import com.tonic.services.GameManager;
/*     */ import java.util.Objects;
/*     */ import net.runelite.api.Client;
/*     */ import net.runelite.api.Player;
/*     */ import net.runelite.api.events.AnimationChanged;
/*     */ import net.runelite.client.eventbus.Subscribe;
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
/*     */ public class TargetSpec
/*     */ {
/*     */   private final AttackTimerConfig config;
/*     */   private final Client client;
/*  29 */   private int targetSpecPercent = 100;
/*  30 */   private int lastRegenTick = -1;
/*     */   
/*     */   private static final int REGEN_TICKS = 50;
/*     */   
/*     */   private static final int REGEN_AMOUNT = 10;
/*  35 */   private int lastVarbit8121 = -1;
/*     */   
/*     */   private static final int ARMADYL_GODSWORD_SPEC = 7644;
/*     */   
/*     */   private static final int DRAGON_CLAWS_SPEC = 7515;
/*     */   
/*     */   private static final int DRAGON_CLAWS_SPEC_2 = 7514;
/*     */   
/*  43 */   private volatile Player currentTarget = null;
/*     */ 
/*     */   
/*     */   public TargetSpec(AttackTimerConfig config, Client client) {
/*  47 */     this.config = config;
/*  48 */     this.client = client;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void setCurrentTarget(Player target) {
/*  56 */     this.currentTarget = target;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public int getTargetSpecPercent() {
/*  64 */     return this.targetSpecPercent;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void onVarbit8121Changed(int newValue) {
/*  72 */     synchronized (this) {
/*     */       
/*  74 */       if (this.lastVarbit8121 == -1) {
/*     */ 
/*     */         
/*  77 */         this.lastVarbit8121 = newValue;
/*     */         
/*     */         return;
/*     */       } 
/*     */       
/*  82 */       if (newValue != this.lastVarbit8121) {
/*     */         
/*  84 */         if (newValue == 1) {
/*     */ 
/*     */           
/*  87 */           this.targetSpecPercent = 100;
/*  88 */           this.lastRegenTick = GameManager.getTickCount();
/*     */           
/*  90 */           if (this.config.debug())
/*     */           {
/*  92 */             Logger.norm("[Target Spec] Varbit 8121 became 1 - reset spec to 100%");
/*     */           
/*     */           }
/*     */         }
/*     */         else {
/*     */           
/*  98 */           this.targetSpecPercent = 100;
/*  99 */           this.lastRegenTick = -1;
/*     */           
/* 101 */           if (this.config.debug())
/*     */           {
/* 103 */             Logger.norm("[Target Spec] Varbit 8121 != 1 - reset spec to 100%");
/*     */           }
/*     */         } 
/*     */         
/* 107 */         this.lastVarbit8121 = newValue;
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void onGameTick() {
/* 118 */     synchronized (this) {
/*     */ 
/*     */       
/* 121 */       int currentVarbit = VarAPI.getVar(8121);
/* 122 */       if (currentVarbit != 1) {
/*     */         return;
/*     */       }
/*     */ 
/*     */ 
/*     */       
/* 128 */       if (this.currentTarget == null) {
/*     */         return;
/*     */       }
/*     */ 
/*     */       
/* 133 */       int currentTick = GameManager.getTickCount();
/*     */ 
/*     */       
/* 136 */       if (this.lastRegenTick == -1) {
/*     */         
/* 138 */         this.lastRegenTick = currentTick;
/* 139 */         this.targetSpecPercent = 100;
/*     */         
/*     */         return;
/*     */       } 
/*     */       
/* 144 */       int ticksSinceLastRegen = currentTick - this.lastRegenTick;
/*     */       
/* 146 */       if (ticksSinceLastRegen >= 50) {
/*     */ 
/*     */         
/* 149 */         this.targetSpecPercent = Math.min(100, this.targetSpecPercent + 10);
/* 150 */         this.lastRegenTick = currentTick;
/*     */         
/* 152 */         if (this.config.debug())
/*     */         {
/* 154 */           Logger.norm("[Target Spec] Regenerated 10% - now at " + this.targetSpecPercent + "%");
/*     */         }
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Subscribe
/*     */   public void onAnimationChanged(AnimationChanged event) {
/* 166 */     if (!this.config.enabled() || !this.config.combatAutomation()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 172 */     Objects.requireNonNull(this.client); Player localPlayer = (Player)Static.invoke(this.client::getLocalPlayer);
/* 173 */     if (localPlayer == null || event.getActor() == localPlayer) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 179 */     if (this.currentTarget == null || event.getActor() != this.currentTarget) {
/*     */       // Fix for Player wrapper changes - check name if object comparison fails
/*     */       if (this.currentTarget != null && event.getActor() != null) {
/*     */         Objects.requireNonNull(this.currentTarget); String currentTargetName = (String)Static.invoke(this.currentTarget::getName);
/*     */         Objects.requireNonNull(event.getActor()); String actorName = (String)Static.invoke(event.getActor()::getName);
/*     */         if (currentTargetName != null && actorName != null && currentTargetName.equals(actorName)) {
/*     */           // Names match, this is our target
/*     */         } else {
/*     */           return;
/*     */         }
/*     */       } else {
/*     */         return;
/*     */       }
/*     */     }
/*     */ 
/*     */ 
/* 184 */     int animationId = event.getActor().getAnimation();
/* 185 */     boolean isSpecialAttack = (animationId == 7644 || animationId == 7515 || animationId == 7514);
/*     */ 
/*     */ 
/*     */     
/* 189 */     if (isSpecialAttack)
/*     */     {
/* 191 */       synchronized (this) {
/*     */ 
/*     */         
/* 194 */         this.targetSpecPercent = Math.max(0, this.targetSpecPercent - 50);
/*     */         
/* 196 */         String specName = (animationId == 7644) ? "AGS" : "Dragon Claws";
/* 197 */         Logger.norm("[Target Spec] Detected " + specName + " special attack - reduced to " + this.targetSpecPercent + "%");
/*     */       } 
/*     */     }
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/TargetSpec.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */