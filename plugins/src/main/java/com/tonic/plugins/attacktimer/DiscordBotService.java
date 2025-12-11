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
/*     */ public class DiscordBotService
/*     */ {
/*  17 */   private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10L);
/*     */   
/*     */   private final HttpClient httpClient;
/*     */   
/*     */   private final String botApiUrl;
/*     */   
/*     */   public DiscordBotService(String botApiUrl) {
/*  24 */     this.botApiUrl = botApiUrl;
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
/*     */   public boolean sendChatMessage(String playerName, String message, boolean isLocalPlayer, String messageId, String channelId) {
/*  41 */     if (this.botApiUrl == null || this.botApiUrl.trim().isEmpty())
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
/*  56 */     if (channelId == null || channelId.trim().isEmpty()) {
/*     */       
/*  58 */       Logger.norm("[Discord Bot Service] Channel ID is required but not provided");
/*  59 */       return false;
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/*  65 */       StringBuilder jsonBuilder = new StringBuilder();
/*  66 */       jsonBuilder.append("{");
/*  67 */       jsonBuilder.append("\"playerName\":\"").append(escapeJson(playerName)).append("\",");
/*  68 */       jsonBuilder.append("\"message\":\"").append(escapeJson(message)).append("\",");
/*  69 */       jsonBuilder.append("\"isLocalPlayer\":").append(isLocalPlayer).append(",");
/*  70 */       jsonBuilder.append("\"messageId\":\"").append(escapeJson((messageId != null) ? messageId : "")).append("\",");
/*  71 */       jsonBuilder.append("\"channelId\":\"").append(escapeJson(channelId)).append("\"");
/*  72 */       jsonBuilder.append("}");
/*  73 */       String jsonPayload = jsonBuilder.toString();
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */       
/*  81 */       HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.botApiUrl + "/send-message")).header("Content-Type", "application/json").header("User-Agent", "VitaLite-Discord-Bot-Service/1.0").timeout(REQUEST_TIMEOUT).POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).build();
/*     */       
/*  83 */       HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
/*     */       
/*  85 */       if (response.statusCode() >= 200 && response.statusCode() < 300)
/*     */       {
/*  87 */         return true;
/*     */       }
/*     */ 
/*     */       
/*  91 */       String responseBody = response.body();
/*  92 */       Logger.norm("[Discord Bot Service] Failed to send message - status code: " + response.statusCode() + (
/*  93 */           (responseBody != null) ? (", Response: " + responseBody) : ""));
/*  94 */       return false;
/*     */     
/*     */     }
/*  97 */     catch (IOException|InterruptedException e) {
/*     */       
/*  99 */       Logger.norm("[Discord Bot Service] Error sending message: " + e.getMessage());
/* 100 */       return false;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isAvailable() {
/* 110 */     if (this.botApiUrl == null || this.botApiUrl.trim().isEmpty())
/*     */     {
/* 112 */       return false;
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/* 122 */       HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.botApiUrl + "/health")).header("User-Agent", "VitaLite-Discord-Bot-Service/1.0").timeout(Duration.ofSeconds(2L)).GET().build();
/*     */       
/* 124 */       HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
/* 125 */       return (response.statusCode() >= 200 && response.statusCode() < 300);
/*     */     }
/* 127 */     catch (Exception e) {
/*     */       
/* 129 */       return false;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private String escapeJson(String input) {
/* 138 */     if (input == null)
/*     */     {
/* 140 */       return "";
/*     */     }
/*     */     
/* 143 */     return input.replace("\\", "\\\\")
/* 144 */       .replace("\"", "\\\"")
/* 145 */       .replace("\n", "\\n")
/* 146 */       .replace("\r", "\\r")
/* 147 */       .replace("\t", "\\t");
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/DiscordBotService.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */