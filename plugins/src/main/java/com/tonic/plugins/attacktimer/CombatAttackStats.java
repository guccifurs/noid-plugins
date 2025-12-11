/*    */ package com.tonic.plugins.attacktimer;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class CombatAttackStats
/*    */ {
/*    */   public void setMelee(int melee) {
/* 10 */     this.melee = melee; } public void setRanged(int ranged) { this.ranged = ranged; } public void setMagic(int magic) { this.magic = magic; }
/*    */ 
/*    */   
/* 13 */   private int melee = 0; public int getMelee() { return this.melee; }
/* 14 */    private int ranged = 0; public int getRanged() { return this.ranged; }
/* 15 */    private int magic = 0; public int getMagic() { return this.magic; }
/*    */ 
/*    */   
/*    */   public void incrementMelee() {
/* 19 */     this.melee++;
/*    */   }
/*    */ 
/*    */   
/*    */   public void incrementRanged() {
/* 24 */     this.ranged++;
/*    */   }
/*    */ 
/*    */   
/*    */   public void incrementMagic() {
/* 29 */     this.magic++;
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public void incrementUnknown() {
/* 36 */     this.melee++;
/*    */   }
/*    */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/CombatAttackStats.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */