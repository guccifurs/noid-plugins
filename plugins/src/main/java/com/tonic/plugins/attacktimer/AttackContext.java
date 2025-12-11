/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ public class AttackContext {
/*     */   private long timestamp;
/*     */   private String overheadPrayer;
/*     */   private int targetHP;
/*     */   private String weapon;
/*     */   private int targetSpec;
/*     */   
/*     */   public void setTimestamp(long timestamp) {
/*  11 */     this.timestamp = timestamp; } private String attackStyle; private String freezeState; private int distance; private String pidStatus; private int playerFreezeTicksRemaining; private int opponentFreezeTicksRemaining; public void setOverheadPrayer(String overheadPrayer) { this.overheadPrayer = overheadPrayer; } public void setTargetHP(int targetHP) { this.targetHP = targetHP; } public void setWeapon(String weapon) { this.weapon = weapon; } public void setTargetSpec(int targetSpec) { this.targetSpec = targetSpec; } public void setAttackStyle(String attackStyle) { this.attackStyle = attackStyle; } public void setFreezeState(String freezeState) { this.freezeState = freezeState; } public void setDistance(int distance) { this.distance = distance; } public void setPidStatus(String pidStatus) { this.pidStatus = pidStatus; } public void setPlayerFreezeTicksRemaining(int playerFreezeTicksRemaining) { this.playerFreezeTicksRemaining = playerFreezeTicksRemaining; } public void setOpponentFreezeTicksRemaining(int opponentFreezeTicksRemaining) { this.opponentFreezeTicksRemaining = opponentFreezeTicksRemaining; }
/*     */ 
/*     */   
/*     */   public long getTimestamp() {
/*  15 */     return this.timestamp;
/*     */   }
/*     */   public String getOverheadPrayer() {
/*  18 */     return this.overheadPrayer;
/*     */   }
/*     */   public int getTargetHP() {
/*  21 */     return this.targetHP;
/*     */   }
/*     */   public String getWeapon() {
/*  24 */     return this.weapon;
/*     */   }
/*     */   public int getTargetSpec() {
/*  27 */     return this.targetSpec;
/*     */   }
/*     */   public String getAttackStyle() {
/*  30 */     return this.attackStyle;
/*     */   }
/*     */   public String getFreezeState() {
/*  33 */     return this.freezeState;
/*     */   }
/*     */   public int getDistance() {
/*  36 */     return this.distance;
/*     */   }
/*     */   public String getPidStatus() {
/*  39 */     return this.pidStatus;
/*     */   }
/*     */   
/*     */   public int getPlayerFreezeTicksRemaining() {
/*  43 */     return this.playerFreezeTicksRemaining;
/*     */   }
/*     */   public int getOpponentFreezeTicksRemaining() {
/*  46 */     return this.opponentFreezeTicksRemaining;
/*     */   }
/*     */ 
/*     */   
/*     */   public AttackContext() {
/*  51 */     this.playerFreezeTicksRemaining = -1;
/*  52 */     this.opponentFreezeTicksRemaining = -1;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public AttackContext(long timestamp, String overheadPrayer, int targetHP, String weapon, int targetSpec, String attackStyle, String freezeState, int distance, String pidStatus, int playerFreezeTicksRemaining, int opponentFreezeTicksRemaining) {
/*  59 */     this.timestamp = timestamp;
/*  60 */     this.overheadPrayer = overheadPrayer;
/*  61 */     this.targetHP = targetHP;
/*  62 */     this.weapon = weapon;
/*  63 */     this.targetSpec = targetSpec;
/*  64 */     this.attackStyle = attackStyle;
/*  65 */     this.freezeState = freezeState;
/*  66 */     this.distance = distance;
/*  67 */     this.pidStatus = pidStatus;
/*  68 */     this.playerFreezeTicksRemaining = playerFreezeTicksRemaining;
/*  69 */     this.opponentFreezeTicksRemaining = opponentFreezeTicksRemaining;
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
/*     */   public String generateContextKey() {
/*  81 */     int hpRange = this.targetHP / 10 * 10;
/*     */ 
/*     */     
/*  84 */     int specRange = this.targetSpec / 10 * 10;
/*     */     
/*  86 */     return String.format("prayer:%s,hp:%d-%d,weapon:%s,spec:%d-%d,freeze:%s", new Object[] {
/*  87 */           (this.overheadPrayer != null) ? this.overheadPrayer : "none", 
/*  88 */           Integer.valueOf(hpRange), Integer.valueOf(hpRange + 10), 
/*  89 */           (this.weapon != null) ? this.weapon : "unknown", 
/*  90 */           Integer.valueOf(specRange), Integer.valueOf(specRange + 10), 
/*  91 */           (this.freezeState != null) ? this.freezeState : "unknown"
/*     */         });
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean matches(AttackContext other, int hpTolerance, int specTolerance, int distanceTolerance) {
/*  99 */     if (other == null)
/*     */     {
/* 101 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 105 */     if (!equalsNullable(this.overheadPrayer, other.overheadPrayer))
/*     */     {
/* 107 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 111 */     if (Math.abs(this.targetHP - other.targetHP) > hpTolerance)
/*     */     {
/* 113 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 117 */     if (!equalsNullable(this.weapon, other.weapon))
/*     */     {
/* 119 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 123 */     if (Math.abs(this.targetSpec - other.targetSpec) > specTolerance)
/*     */     {
/* 125 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 129 */     if (!equalsNullable(this.freezeState, other.freezeState))
/*     */     {
/* 131 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 135 */     if (Math.abs(this.distance - other.distance) > distanceTolerance)
/*     */     {
/* 137 */       return false;
/*     */     }
/*     */ 
/*     */     
/* 141 */     if (this.pidStatus != null && other.pidStatus != null)
/*     */     {
/* 143 */       if (!this.pidStatus.equals("UNKNOWN") && !other.pidStatus.equals("UNKNOWN"))
/*     */       {
/* 145 */         if (!this.pidStatus.equals(other.pidStatus))
/*     */         {
/* 147 */           return false;
/*     */         }
/*     */       }
/*     */     }
/*     */     
/* 152 */     return true;
/*     */   }
/*     */ 
/*     */   
/*     */   private boolean equalsNullable(String a, String b) {
/* 157 */     if (a == null && b == null)
/*     */     {
/* 159 */       return true;
/*     */     }
/* 161 */     if (a == null || b == null)
/*     */     {
/* 163 */       return false;
/*     */     }
/* 165 */     return a.equals(b);
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/AttackContext.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */