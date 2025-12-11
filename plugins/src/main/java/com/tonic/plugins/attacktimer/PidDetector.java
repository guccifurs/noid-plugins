/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import com.tonic.services.GameManager;
/*     */ import java.util.Objects;
/*     */ import net.runelite.api.Client;
/*     */ import net.runelite.api.Player;
/*     */ import net.runelite.api.events.AnimationChanged;
/*     */ import net.runelite.api.events.HitsplatApplied;
/*     */ import net.runelite.api.coords.WorldPoint;
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
/*     */ 
/*     */ public class PidDetector
/*     */ {
/*     */   private final AttackTimerConfig config;
/*     */   private final Client client;
/*  29 */   private volatile Player currentTarget = null;
/*     */ 
/*     */   
/*  32 */   private PidStatus currentPidStatus = PidStatus.UNKNOWN;
/*     */ 
/*     */   
/*  35 */   private int lastHitsplatTick = -1;
/*  36 */   private int lastWhipAnimationTick = -1;
/*     */ 
/*     */   
/*     */   private static final int ON_PID_TICK_DIFF = 0;
/*     */   
/*     */   private static final int OFF_PID_TICK_DIFF = 1;
/*     */   
/*  43 */   private int onPidDetections = 0;
/*  44 */   private int offPidDetections = 0;
/*  45 */   private int unknownCount = 0;
/*     */ 
/*     */   
/*     */   public PidDetector(AttackTimerConfig config, Client client) {
/*  49 */     this.config = config;
/*  50 */     this.client = client;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public PidStatus getCurrentPidStatus() {
/*  58 */     return this.currentPidStatus;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void setCurrentTarget(Player target) {
/*  66 */     this.currentTarget = target;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void onVarbit8121Changed(int newValue) {
/*  74 */     synchronized (this) {
/*     */ 
/*     */       
/*  77 */       this.currentPidStatus = PidStatus.UNKNOWN;
/*  78 */       this.lastHitsplatTick = -1;
/*  79 */       this.lastWhipAnimationTick = -1;
/*  80 */       this.unknownCount++;
/*     */       
/*  82 */       if (this.config.debug())
/*     */       {
/*  84 */         Logger.norm("[PID Detector] Varbit 8121 changed: " + newValue + " - Reset PID to UNKNOWN");
/*     */       }
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Subscribe
/*     */   public void onHitsplatApplied(HitsplatApplied event) {
/*  95 */     if (!this.config.enabled() || !this.config.combatAutomation()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 100 */     Objects.requireNonNull(this.client); Player localPlayer = (Player)Static.invoke(this.client::getLocalPlayer);
/* 101 */     if (localPlayer == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 108 */     if (this.currentTarget == null || event.getActor() != this.currentTarget) {
      
      // Fix for Player wrapper changes - check name if object comparison fails
      boolean shouldProcess = false;
      if (this.currentTarget != null && event.getActor() != null) {
        Objects.requireNonNull(this.currentTarget); String currentTargetName = (String)Static.invoke(this.currentTarget::getName);
        Objects.requireNonNull(event.getActor()); String actorName = (String)Static.invoke(event.getActor()::getName);
        if (currentTargetName != null && actorName != null && currentTargetName.equals(actorName)) {
          shouldProcess = true; // Names match, this is our target
        }
      }
      
      if (!shouldProcess) {
        if (this.config.debug()) {
          Logger.norm("[PID Detector] Hitsplat ignored - wrong target (actor: " + String.valueOf(event.getActor()) + ", target: " + String.valueOf(this.currentTarget) + ")");
        }
        return;
      }
    }
    
    int hitsplatType = event.getHitsplat().getHitsplatType();
/*     */     
/* 119 */     boolean isDamageFromMelee = (hitsplatType == 12 || hitsplatType == 17 || hitsplatType == 19 || hitsplatType == 21 || hitsplatType == 23 || hitsplatType == 25 || hitsplatType == 54);
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 127 */     if (isDamageFromMelee) {
/*     */       
/* 129 */       synchronized (this)
/*     */       {
/* 131 */         int currentTick = GameManager.getTickCount();
/* 132 */         this.lastHitsplatTick = currentTick;
/*     */         
/* 134 */         if (this.config.debug())
/*     */         {
/* 136 */           Logger.norm("[PID Detector] Hitsplat on OPPONENT detected on tick: " + currentTick + " (type: " + hitsplatType + ")");
/*     */         }
/*     */ 
/*     */         
/* 140 */         checkPidStatus();
/*     */       
/*     */       }
/*     */     
/*     */     }
/* 145 */     else if (this.config.debug()) {
/*     */       
/* 147 */       Logger.norm("[PID Detector] Hitsplat ignored - not melee damage (type: " + hitsplatType + ")");
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 

  @Subscribe
  public void onAnimationChanged(AnimationChanged event) {
    if (!this.config.enabled() || !this.config.combatAutomation()) {
      return;
    }

    Objects.requireNonNull(this.client); 
    Player localPlayer = (Player)Static.invoke(this.client::getLocalPlayer);
    if (localPlayer == null) {
      return;
    }

    if (event.getActor() != localPlayer) {
      return;
    }

    Objects.requireNonNull(localPlayer); 
    int animationId = ((Integer)Static.invoke(localPlayer::getAnimation)).intValue();
    boolean isWhipAnimation = (animationId == 1658 || animationId == 1659);
    
    if (isWhipAnimation) {
      synchronized (this) {
        int currentTick = GameManager.getTickCount();
        this.lastWhipAnimationTick = currentTick;
        
        if (this.config.debug()) {
          Logger.norm("[PID Detector] OUR whip animation detected on tick: " + currentTick + " (animation: " + animationId + ")");
        }

        checkPidStatus();
      }
    } else if (this.config.debug() && animationId != 65535 && animationId != -1) {
      Logger.norm("[PID Detector] Animation ignored - not whip (animation: " + animationId + ")");
    } 
  }
/*     */ 
/*     */ 
/*     */ 
/*     */ 

  private void checkPidStatus() {
/* 211 */     int varbit8121 = ((Integer)Static.invoke(() -> Integer.valueOf(this.client.getVarbitValue(8121)))).intValue();
/* 212 */     if (varbit8121 != 1) {
/*     */       
/* 214 */       if (this.config.debug())
/*     */       {
/* 216 */         Logger.norm("[PID Detector] Varbit 8121 != 1 (value: " + varbit8121 + ") - skipping PID check");
/*     */       }
/*     */       
/*     */       return;
/*     */     } 
/*     */     
/* 222 */     if (this.lastHitsplatTick == -1 || this.lastWhipAnimationTick == -1) {
/*     */       
/* 224 */       if (this.config.debug())
/*     */       {
/* 226 */         Logger.norm("[PID Detector] Missing data - hitsplat: " + this.lastHitsplatTick + ", animation: " + this.lastWhipAnimationTick);
/*     */       }
/*     */       
/*     */       return;
/*     */     } 
/*     */     
/* 232 */     int tickDifference = this.lastHitsplatTick - this.lastWhipAnimationTick;
/*     */ 
/*     */     
/* 235 */     PidStatus detectedStatus = analyzePidTiming(tickDifference);
/*     */     
/* 237 */     if (detectedStatus != null) {
/*     */ 
/*     */       
/* 240 */       this.currentPidStatus = detectedStatus;
/*     */ 
/*     */       
/* 243 */       if (detectedStatus == PidStatus.ON_PID) {
/*     */         
/* 245 */         this.onPidDetections++;
/*     */       }
/* 247 */       else if (detectedStatus == PidStatus.OFF_PID) {
/*     */         
/* 249 */         this.offPidDetections++;
/*     */       } 
/*     */       
/* 252 */       if (this.config.debug())
/*     */       {
/* 254 */         Logger.norm("[PID Detector] ✓ PID detected: " + String.valueOf(detectedStatus) + " (hitsplat tick: " + this.lastHitsplatTick + ", anim tick: " + this.lastWhipAnimationTick + ", diff: " + tickDifference + ") [Stats: ON=" + this.onPidDetections + " OFF=" + this.offPidDetections + " UNK=" + this.unknownCount + "]");
/*     */ 
/*     */ 
/*     */       
/*     */       }
/*     */ 
/*     */ 
/*     */     
/*     */     }
/* 263 */     else if (this.config.debug()) {
/*     */       
/* 265 */       Logger.norm("[PID Detector] Invalid timing (diff: " + tickDifference + ") - preserving status: " + String.valueOf(this.currentPidStatus));
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 270 */     this.lastHitsplatTick = -1;
/* 271 */     this.lastWhipAnimationTick = -1;
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
/*     */   private PidStatus analyzePidTiming(int tickDifference) {
/* 288 */     if (tickDifference == 0)
/*     */     {
/* 290 */       return PidStatus.ON_PID;
/*     */     }
/*     */ 
/*     */     
/* 294 */     if (tickDifference == 1)
/*     */     {
/* 296 */       return PidStatus.OFF_PID;
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 302 */     return null;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String getStats() {
/* 310 */     return String.format("ON=%d OFF=%d UNK=%d", new Object[] { Integer.valueOf(this.onPidDetections), Integer.valueOf(this.offPidDetections), Integer.valueOf(this.unknownCount) });
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private int calculateDistance(Player attacker, Player target) {
/* 318 */     WorldPoint attackerPos = attacker.getWorldLocation();
/* 319 */     WorldPoint targetPos = target.getWorldLocation();
/* 320 */     return Math.max(Math.abs(attackerPos.getX() - targetPos.getX()), Math.abs(attackerPos.getY() - targetPos.getY()));
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public enum PidStatus
/*     */   {
/* 329 */     ON_PID,
/* 330 */     OFF_PID,
/* 331 */     UNKNOWN;
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/PidDetector.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */