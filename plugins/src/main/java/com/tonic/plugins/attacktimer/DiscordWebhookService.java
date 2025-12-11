/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import java.io.IOException;
/*     */ import java.net.URI;
/*     */ import java.net.http.HttpClient;
/*     */ import java.net.http.HttpRequest;
/*     */ import java.net.http.HttpResponse;
/*     */ import java.time.Duration;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class DiscordWebhookService
/*     */ {
/*  17 */   private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5L);
/*     */   
/*     */   private final HttpClient httpClient;
/*     */   
/*     */   private final String webhookUrl;
/*     */   
/*     */   public DiscordWebhookService(String webhookUrl) {
/*  24 */     this.webhookUrl = webhookUrl;
/*  25 */     this
/*     */       
/*  27 */       .httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
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
/*     */   public boolean sendChatMessage(String playerName, String message, boolean isLocalPlayer, String callbackUrl, String messageId) {
/*  41 */     if (this.webhookUrl == null || this.webhookUrl.trim().isEmpty())
/*     */     {
/*  43 */       return false;
/*     */     }
/*     */     
/*  46 */     if (playerName == null || playerName.trim().isEmpty())
/*     */     {
/*  48 */       playerName = isLocalPlayer ? "The bot" : "Unknown";
/*     */     }
/*     */     
/*  51 */     if (message == null || message.trim().isEmpty())
/*     */     {
/*  53 */       return false;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/*  59 */       String escapedPlayerName = escapeJson(playerName);
/*  60 */       String escapedMessage = escapeJson(message);
/*     */ 
/*     */       
/*  63 */       int embedColor = isLocalPlayer ? 15158332 : 3447003;
/*     */ 
/*     */       
/*  66 */       String embedTitle = isLocalPlayer ? "bot" : escapedPlayerName;
/*     */ 
/*     */       
/*  69 */       StringBuilder jsonBody = new StringBuilder();
/*  70 */       jsonBody.append("{\"embeds\":[{\"title\":\"").append(embedTitle)
/*  71 */         .append("\",\"description\":\"").append(escapedMessage)
/*  72 */         .append("\",\"color\":").append(embedColor).append("}]");
/*     */ 
/*     */       
/*  75 */       if (callbackUrl != null && !callbackUrl.trim().isEmpty() && messageId != null && !messageId.trim().isEmpty()) {
/*     */ 
/*     */         
/*  78 */         String escapedCallbackUrl = escapeJson(callbackUrl);
/*  79 */         String escapedMessageId = escapeJson(messageId);
/*     */ 
/*     */ 
/*     */         
/*  83 */         jsonBody.append(",\"components\":[{\"type\":1,\"components\":[{\"type\":2,\"style\":1,\"label\":\"Reply\",\"custom_id\":\"reply_").append(escapedMessageId)
/*  84 */           .append("\",\"emoji\":{\"name\":\"💬\"}}]}]");
/*     */       } 
/*     */       
/*  87 */       jsonBody.append("}");
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */       
/*  95 */       HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.webhookUrl)).header("Content-Type", "application/json").header("User-Agent", "VitaLite-Discord-Webhook/1.0").timeout(REQUEST_TIMEOUT).POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString())).build();
/*     */       
/*  97 */       HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
/*     */       
/*  99 */       if (response.statusCode() >= 200 && response.statusCode() < 300) {
/*     */ 
/*     */         
/* 102 */         String str = response.body();
/* 103 */         if (str != null && (str.contains("components") || str.contains("interaction")))
/*     */         {
/* 105 */           Logger.norm("[Discord Webhook] Message sent but Discord may have ignored components. Response: " + str);
/*     */         }
/* 107 */         return true;
/*     */       } 
/*     */ 
/*     */       
/* 111 */       String responseBody = response.body();
/* 112 */       Logger.norm("[Discord Webhook] Failed to send message - status code: " + response.statusCode() + (
/* 113 */           (responseBody != null) ? (", Response: " + responseBody) : ""));
/* 114 */       return false;
/*     */     
/*     */     }
/* 117 */     catch (IOException|InterruptedException e) {
/*     */       
/* 119 */       Logger.norm("[Discord Webhook] Error sending message: " + e.getMessage());
/* 120 */       return false;
/*     */     } 
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
/*     */   public boolean sendChatMessage(String playerName, String message, boolean isLocalPlayer) {
/* 133 */     return sendChatMessage(playerName, message, isLocalPlayer, null, null);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private String escapeJson(String input) {
/* 141 */     if (input == null)
/*     */     {
/* 143 */       return "";
/*     */     }
/*     */     
/* 146 */     return input.replace("\\", "\\\\")
/* 147 */       .replace("\"", "\\\"")
/* 148 */       .replace("\n", "\\n")
/* 149 */       .replace("\r", "\\r")
/* 150 */       .replace("\t", "\\t");
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/DiscordWebhookService.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */