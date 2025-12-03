/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.sun.net.httpserver.HttpExchange;
/*     */ import com.sun.net.httpserver.HttpServer;
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.util.MessageUtil;
/*     */ import java.io.IOException;
/*     */ import java.io.OutputStream;
/*     */ import java.net.InetSocketAddress;
/*     */ import java.nio.charset.StandardCharsets;
/*     */ import java.util.concurrent.Executors;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class DiscordReplyServer
/*     */ {
/*     */   private HttpServer server;
/*     */   private final int port;
/*     */   private final boolean enabled;
/*     */   
/*     */   public DiscordReplyServer(int port, boolean enabled) {
/*  26 */     this.port = port;
/*  27 */     this.enabled = enabled;
/*     */   }
/*     */ 
/*     */   
/*     */   public void start() {
/*  32 */     if (!this.enabled) {
/*     */       
/*  34 */       Logger.norm("[Discord Reply Server] Server disabled, not starting");
/*     */       
/*     */       return;
/*     */     } 
/*  38 */     if (this.server != null) {
/*     */       
/*  40 */       Logger.norm("[Discord Reply Server] Server already running");
/*     */       
/*     */       return;
/*     */     } 
/*     */     
/*     */     try {
/*  46 */       this.server = HttpServer.create(new InetSocketAddress(this.port), 0);
/*  47 */       this.server.setExecutor(Executors.newCachedThreadPool());
/*     */ 
/*     */       
/*  50 */       this.server.createContext("/discord-reply", this::handleReply);
/*     */ 
/*     */       
/*  53 */       this.server.createContext("/health", this::handleHealth);
/*     */       
/*  55 */       this.server.start();
/*  56 */       Logger.norm("[Discord Reply Server] Started on port " + this.port);
/*     */     }
/*  58 */     catch (IOException e) {
/*     */       
/*  60 */       Logger.norm("[Discord Reply Server] Failed to start: " + e.getMessage());
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public void stop() {
/*  66 */     if (this.server != null) {
/*     */       
/*  68 */       this.server.stop(0);
/*  69 */       this.server = null;
/*  70 */       Logger.norm("[Discord Reply Server] Stopped");
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private void handleReply(HttpExchange exchange) throws IOException {
/*     */     try {
/*  78 */       if (!"POST".equals(exchange.getRequestMethod())) {
/*     */         
/*  80 */         sendResponse(exchange, 405, "Method Not Allowed");
/*     */         
/*     */         return;
/*     */       } 
/*     */       
/*  85 */       String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
/*     */ 
/*     */       
/*  88 */       String message = extractJsonValue(requestBody, "message");
/*  89 */       String messageId = extractJsonValue(requestBody, "messageId");
/*     */       
/*  91 */       if (message == null || message.trim().isEmpty()) {
/*     */         
/*  93 */         sendResponse(exchange, 400, "Missing 'message' field");
/*     */         
/*     */         return;
/*     */       } 
/*     */       
/*  98 */       if (message.length() > 80)
/*     */       {
/* 100 */         message = message.substring(0, 80);
/*     */       }
/*     */ 
/*     */       
/* 104 */       MessageUtil.sendPublicChatMessage(message);
/*     */       
/* 106 */       Logger.norm("[Discord Reply Server] Received reply (ID: " + messageId + "): " + message);
/*     */ 
/*     */       
/* 109 */       sendResponse(exchange, 200, "{\"status\":\"ok\",\"message\":\"Reply sent\"}");
/*     */     }
/* 111 */     catch (Exception e) {
/*     */       
/* 113 */       Logger.norm("[Discord Reply Server] Error handling reply: " + e.getMessage());
/* 114 */       sendResponse(exchange, 500, "Internal Server Error");
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   private void handleHealth(HttpExchange exchange) throws IOException {
/* 120 */     sendResponse(exchange, 200, "{\"status\":\"ok\"}");
/*     */   }
/*     */ 
/*     */   
/*     */   private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
/* 125 */     exchange.getResponseHeaders().set("Content-Type", "application/json");
/* 126 */     exchange.sendResponseHeaders(statusCode, response.length());
/* 127 */     OutputStream os = exchange.getResponseBody();
/*     */     try {
/* 129 */       os.write(response.getBytes(StandardCharsets.UTF_8));
/* 130 */       if (os != null) os.close(); 
/*     */     } catch (Throwable throwable) {
/*     */       if (os != null)
/*     */         try {
/*     */           os.close();
/*     */         } catch (Throwable throwable1) {
/*     */           throwable.addSuppressed(throwable1);
/*     */         }  
/*     */       throw throwable;
/*     */     }  } private String extractJsonValue(String json, String key) { try {
/* 140 */       String searchKey = "\"" + key + "\"";
/* 141 */       int keyIndex = json.indexOf(searchKey);
/* 142 */       if (keyIndex == -1)
/*     */       {
/* 144 */         return null;
/*     */       }
/*     */       
/* 147 */       int colonIndex = json.indexOf(":", keyIndex);
/* 148 */       if (colonIndex == -1)
/*     */       {
/* 150 */         return null;
/*     */       }
/*     */       
/* 153 */       int valueStart = colonIndex + 1;
/*     */       
/* 155 */       while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart)))
/*     */       {
/* 157 */         valueStart++;
/*     */       }
/*     */ 
/*     */       
/* 161 */       if (valueStart < json.length() && json.charAt(valueStart) == '"') {
/*     */         
/* 163 */         valueStart++;
/* 164 */         int i = json.indexOf('"', valueStart);
/* 165 */         if (i == -1)
/*     */         {
/* 167 */           return null;
/*     */         }
/* 169 */         return json.substring(valueStart, i);
/*     */       } 
/*     */ 
/*     */ 
/*     */       
/* 174 */       int valueEnd = valueStart;
/* 175 */       while (valueEnd < json.length() && json
/* 176 */         .charAt(valueEnd) != ',' && json
/* 177 */         .charAt(valueEnd) != '}' && json
/* 178 */         .charAt(valueEnd) != ']' && 
/* 179 */         !Character.isWhitespace(json.charAt(valueEnd)))
/*     */       {
/* 181 */         valueEnd++;
/*     */       }
/* 183 */       return json.substring(valueStart, valueEnd).trim();
/*     */     
/*     */     }
/* 186 */     catch (Exception e) {
/*     */       
/* 188 */       return null;
/*     */     }  }
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isRunning() {
/* 194 */     return (this.server != null);
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/DiscordReplyServer.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */