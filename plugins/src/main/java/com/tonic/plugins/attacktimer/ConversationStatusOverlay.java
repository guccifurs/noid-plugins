/*    */ package com.tonic.plugins.attacktimer;
/*    */ 
/*    */ import com.tonic.ui.VitaOverlay;
/*    */ import java.awt.Color;
/*    */ import java.awt.Dimension;
/*    */ import java.awt.Graphics2D;
/*    */ import java.util.ArrayList;
/*    */ import java.util.List;
/*    */ import net.runelite.client.ui.overlay.OverlayPosition;
/*    */ 
/*    */ 
/*    */ public class ConversationStatusOverlay
/*    */   extends VitaOverlay
/*    */ {
/*    */   private final AttackTimerConfig config;
/* 16 */   private List<String> activeConversations = new ArrayList<>();
/*    */   
/*    */   private boolean gambaMode = false;
/*    */ 
/*    */   
/*    */   public ConversationStatusOverlay(AttackTimerConfig config) {
/* 22 */     this.config = config;
/* 23 */     setPosition(OverlayPosition.TOP_LEFT);
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public void updateConversations(List<String> conversations, boolean gambaMode) {
/* 31 */     this.activeConversations = (conversations != null) ? new ArrayList<>(conversations) : new ArrayList<>();
/* 32 */     this.gambaMode = gambaMode;
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public Dimension render(Graphics2D graphics) {
/* 39 */     if (this.activeConversations == null || this.activeConversations.isEmpty())
/*    */     {
/* 41 */       return null;
/*    */     }
/*    */     
/* 44 */     clear();
/* 45 */     setWidth(180);
/*    */ 
/*    */     
/* 48 */     String title = this.gambaMode ? "Chatting (Gamba Mode)" : "Chatting";
/* 49 */     newLine(title, 14, this.gambaMode ? Color.CYAN : Color.WHITE);
/* 50 */     newLine("━━━━━━━━━━━━━━━━━━━━", 10, Color.GRAY);
/*    */ 
/*    */     
/* 53 */     for (String playerName : this.activeConversations) {
/*    */ 
/*    */       
/* 56 */       String shortName = playerName;
/* 57 */       if (playerName != null && playerName.contains(" ")) {
/*    */         
/* 59 */         String[] parts = playerName.split("\\s+");
/* 60 */         if (parts.length > 0)
/*    */         {
/* 62 */           shortName = parts[0];
/*    */         }
/*    */       } 
/*    */       
/* 66 */       newLine("• " + shortName, 12, Color.GREEN);
/*    */     } 
/*    */ 
/*    */     
/* 70 */     if (this.gambaMode && this.activeConversations.size() >= 2) {
/*    */       
/* 72 */       newLine("━━━━━━━━━━━━━━━━━━━━", 10, Color.GRAY);
/* 73 */       newLine("Multi-conversation", 10, Color.YELLOW);
/*    */     } 
/*    */     
/* 76 */     return super.render(graphics);
/*    */   }
/*    */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/ConversationStatusOverlay.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */