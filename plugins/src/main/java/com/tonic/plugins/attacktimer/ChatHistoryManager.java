/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.google.gson.Gson;
/*     */ import com.google.gson.GsonBuilder;
/*     */ import com.google.gson.reflect.TypeToken;
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.lang.reflect.Type;
/*     */ import java.nio.file.Files;
/*     */ import java.nio.file.Path;
/*     */ import java.time.ZoneId;
/*     */ import java.time.ZonedDateTime;
/*     */ import java.time.format.DateTimeFormatter;
/*     */ import java.util.ArrayList;
/*     */ import java.util.HashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.concurrent.Executors;
/*     */ import java.util.concurrent.ScheduledExecutorService;
/*     */ import java.util.concurrent.ScheduledFuture;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import java.util.concurrent.locks.ReentrantReadWriteLock;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class ChatHistoryManager
/*     */ {
/*     */   private static final String HISTORY_FILE_NAME = "chat-history.json";
/*     */   private static final int MAX_MESSAGES_PER_PLAYER = 100;
/*     */   private static final long SAVE_DEBOUNCE_MS = 1000L;
/*  36 */   private static final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
/*  37 */   private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
/*     */   
/*     */   private final Map<String, List<AIChatMessage>> playerHistories;
/*     */   
/*     */   private final File historyFile;
/*     */   
/*     */   private final ScheduledExecutorService saveExecutor;
/*     */   
/*  45 */   private ScheduledFuture<?> pendingSave = null;
/*  46 */   private final ReentrantReadWriteLock saveLock = new ReentrantReadWriteLock();
/*     */   
/*     */   private volatile boolean shutdown = false;
/*     */ 
/*     */   
/*     */   public ChatHistoryManager() {
/*  52 */     Path vitaDir = Static.VITA_DIR;
/*  53 */     this.historyFile = new File(vitaDir.toFile(), "chat-history.json");
/*     */     
/*  55 */     Map<String, List<AIChatMessage>> loadedHistories = null;
/*  56 */     if (this.historyFile.exists()) {
/*     */       
/*     */       try {
/*     */         
/*  60 */         String json = new String(Files.readAllBytes(this.historyFile.toPath()));
/*  61 */         Type type = (new TypeToken<Map<String, List<AIChatMessage>>>() {  }).getType();
/*  62 */         loadedHistories = (Map<String, List<AIChatMessage>>)gson.fromJson(json, type);
/*  63 */         Logger.norm("[ChatHistory] Loaded existing chat history from: " + this.historyFile.getAbsolutePath());
/*     */       }
/*  65 */       catch (Exception e) {
/*     */         
/*  67 */         Logger.norm("[ChatHistory] Failed to load existing history, creating new: " + e.getMessage());
/*     */       } 
/*     */     }
/*     */ 
/*     */     
/*  72 */     this.playerHistories = (loadedHistories != null) ? loadedHistories : new HashMap<>();
/*     */ 
/*     */     
/*  75 */     this.saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
/*     */           Thread t = new Thread(r, "ChatHistory-Save");
/*     */           t.setDaemon(true);
/*     */           return t;
/*     */         });
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void addMessage(String playerName, String role, String content) {
/*  90 */     if (playerName == null || playerName.trim().isEmpty()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/*  95 */     if (role == null || content == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 101 */     String timestamp = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);
/*     */ 
/*     */     
/* 104 */     this.saveLock.writeLock().lock();
/*     */     
/*     */     try {
/* 107 */       List<AIChatMessage> history = this.playerHistories.computeIfAbsent(playerName, k -> new ArrayList<>());
/*     */ 
/*     */       
/* 110 */       history.add(new AIChatMessage(timestamp, role, content));
/*     */ 
/*     */       
/* 113 */       if (history.size() > 100)
/*     */       {
/* 115 */         history.remove(0);
/*     */       }
/*     */     }
/*     */     finally {
/*     */       
/* 120 */       this.saveLock.writeLock().unlock();
/*     */     } 
/*     */ 
/*     */     
/* 124 */     scheduleAsyncSave();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public List<AIChatMessage> getHistory(String playerName) {
/* 134 */     if (playerName == null || playerName.trim().isEmpty())
/*     */     {
/* 136 */       return new ArrayList<>();
/*     */     }
/*     */     
/* 139 */     this.saveLock.readLock().lock();
/*     */     
/*     */     try {
/* 142 */       List<AIChatMessage> history = this.playerHistories.get(playerName);
/* 143 */       if (history == null)
/*     */       {
/* 145 */         return new ArrayList();
/*     */       }
/*     */       
/* 148 */       return new ArrayList<>(history);
/*     */     }
/*     */     finally {
/*     */       
/* 152 */       this.saveLock.readLock().unlock();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void clearHistory(String playerName) {
/* 162 */     if (playerName == null || playerName.trim().isEmpty()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 167 */     this.saveLock.writeLock().lock();
/*     */     
/*     */     try {
/* 170 */       this.playerHistories.remove(playerName);
/*     */     }
/*     */     finally {
/*     */       
/* 174 */       this.saveLock.writeLock().unlock();
/*     */     } 
/*     */ 
/*     */     
/* 178 */     scheduleAsyncSave();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public List<String> getAllPlayerNames() {
/* 187 */     this.saveLock.readLock().lock();
/*     */     
/*     */     try {
/* 190 */       return new ArrayList(this.playerHistories.keySet());
/*     */     }
/*     */     finally {
/*     */       
/* 194 */       this.saveLock.readLock().unlock();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void scheduleAsyncSave() {
/* 203 */     if (this.shutdown) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 209 */     if (this.pendingSave != null)
/*     */     {
/* 211 */       this.pendingSave.cancel(false);
/*     */     }
/*     */ 
/*     */     
/* 215 */     this.pendingSave = this.saveExecutor.schedule(() -> saveToFileAsync(), 1000L, TimeUnit.MILLISECONDS);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void saveToFileAsync() {
/*     */     Map<String, List<AIChatMessage>> snapshot;
/* 227 */     this.saveLock.readLock().lock();
/*     */     
/*     */     try {
/* 230 */       snapshot = new HashMap<>();
/* 231 */       for (Map.Entry<String, List<AIChatMessage>> entry : this.playerHistories.entrySet())
/*     */       {
/* 233 */         snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
/*     */       }
/*     */     }
/*     */     finally {
/*     */       
/* 238 */       this.saveLock.readLock().unlock();
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/* 244 */       String json = gson.toJson(snapshot);
/* 245 */       Files.write(this.historyFile.toPath(), json.getBytes(), new java.nio.file.OpenOption[0]);
/*     */     }
/* 247 */     catch (IOException e) {
/*     */       
/* 249 */       Logger.norm("[ChatHistory] Failed to save chat history: " + e.getMessage());
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void saveToFile() {
/*     */     Map<String, List<AIChatMessage>> snapshot;
/* 260 */     this.saveLock.readLock().lock();
/*     */     
/*     */     try {
/* 263 */       snapshot = new HashMap<>();
/* 264 */       for (Map.Entry<String, List<AIChatMessage>> entry : this.playerHistories.entrySet())
/*     */       {
/* 266 */         snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
/*     */       }
/*     */     }
/*     */     finally {
/*     */       
/* 271 */       this.saveLock.readLock().unlock();
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/* 277 */       String json = gson.toJson(snapshot);
/* 278 */       Files.write(this.historyFile.toPath(), json.getBytes(), new java.nio.file.OpenOption[0]);
/* 279 */       Logger.norm("[ChatHistory] Saved chat history to: " + this.historyFile.getAbsolutePath());
/*     */     }
/* 281 */     catch (IOException e) {
/*     */       
/* 283 */       Logger.norm("[ChatHistory] Failed to save chat history: " + e.getMessage());
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void shutdown() {
/* 292 */     this.shutdown = true;
/*     */ 
/*     */     
/* 295 */     if (this.pendingSave != null)
/*     */     {
/* 297 */       this.pendingSave.cancel(false);
/*     */     }
/*     */ 
/*     */     
/* 301 */     saveToFile();
/*     */ 
/*     */     
/* 304 */     this.saveExecutor.shutdown();
/*     */     
/*     */     try {
/* 307 */       if (!this.saveExecutor.awaitTermination(5L, TimeUnit.SECONDS))
/*     */       {
/* 309 */         this.saveExecutor.shutdownNow();
/*     */       }
/*     */     }
/* 312 */     catch (InterruptedException e) {
/*     */       
/* 314 */       this.saveExecutor.shutdownNow();
/* 315 */       Thread.currentThread().interrupt();
/*     */     } 
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/ChatHistoryManager.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */