/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.google.gson.Gson;
/*     */ import com.google.gson.GsonBuilder;
/*     */ import com.google.gson.reflect.TypeToken;
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import java.io.File;
/*     */ import java.io.FileWriter;
/*     */ import java.io.IOException;
/*     */ import java.lang.reflect.Type;
/*     */ import java.nio.file.Files;
/*     */ import java.nio.file.Path;
/*     */ import java.util.HashMap;
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
/*     */ public class CombatLogger
/*     */ {
/*     */   private static final String LOG_FILE_NAME = "combat-attack-log.json";
/*  29 */   private static final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
/*     */   
/*     */   private static final long SAVE_DEBOUNCE_MS = 5000L;
/*     */   
/*     */   private final Map<String, CombatLogData> targetLogs;
/*     */   
/*     */   private final File logFile;
/*     */   
/*     */   private final AttackTimerConfig config;
/*     */   private final ScheduledExecutorService saveExecutor;
/*  39 */   private ScheduledFuture<?> pendingSave = null;
/*  40 */   private final ReentrantReadWriteLock saveLock = new ReentrantReadWriteLock();
/*     */   
/*     */   private volatile boolean shutdown = false;
/*     */   
/*     */   public CombatLogger(AttackTimerConfig config) {
/*  45 */     this.config = config;
/*     */     
/*  47 */     Path vitaDir = Static.VITA_DIR;
/*  48 */     this.logFile = new File(vitaDir.toFile(), "combat-attack-log.json");
/*     */     
/*  50 */     Map<String, CombatLogData> loadedLogs = null;
/*  51 */     if (this.logFile.exists()) {
/*     */       
/*     */       try {
/*     */         
/*  55 */         String json = new String(Files.readAllBytes(this.logFile.toPath()));
/*  56 */         Type type = (new TypeToken<Map<String, CombatLogData>>() {  }).getType();
/*  57 */         loadedLogs = (Map<String, CombatLogData>)gson.fromJson(json, type);
/*  58 */         Logger.norm("[CombatLogger] Loaded existing combat log from: " + this.logFile.getAbsolutePath());
/*     */       }
/*  60 */       catch (Exception e) {
/*     */         
/*  62 */         Logger.norm("[CombatLogger] Failed to load existing log, creating new: " + e.getMessage());
/*     */       } 
/*     */     }
/*     */ 
/*     */     
/*  67 */     this.targetLogs = (loadedLogs != null) ? loadedLogs : new HashMap<>();
/*     */ 
/*     */     
/*  70 */     this.saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
/*     */           Thread t = new Thread(r, "CombatLogger-Save");
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
/*     */   
/*     */   public void logOpponentAttack(String targetRSN, String attackType, boolean playerFrozen, boolean opponentFrozen) {
/*  86 */     if (targetRSN == null || targetRSN.trim().isEmpty())
/*     */     {
/*  88 */       targetRSN = "Unknown";
/*     */     }
/*     */ 
/*     */     
/*  92 */     CombatLogData logData = this.targetLogs.computeIfAbsent(targetRSN, k -> new CombatLogData());
/*     */     
/*  94 */     CombatAttackStats targetStats = null;
/*     */ 
/*     */     
/*  97 */     if (playerFrozen && opponentFrozen) {
/*     */       
/*  99 */       targetStats = logData.getBothFrozen();
/*     */     }
/* 101 */     else if (!playerFrozen && !opponentFrozen) {
/*     */       
/* 103 */       targetStats = logData.getBothUnfrozen();
/*     */     }
/* 105 */     else if (!playerFrozen && opponentFrozen) {
/*     */       
/* 107 */       targetStats = logData.getTargetFrozenWeUnfrozen();
/*     */     }
/* 109 */     else if (playerFrozen && !opponentFrozen) {
/*     */       
/* 111 */       targetStats = logData.getWeFrozenTargetUnfrozen();
/*     */     } 
/*     */     
/* 114 */     if (targetStats != null) {
/*     */ 
/*     */       
/* 117 */       switch (attackType.toLowerCase()) {
/*     */         
/*     */         case "melee":
/* 120 */           targetStats.incrementMelee();
/*     */           break;
/*     */         case "ranged":
/* 123 */           targetStats.incrementRanged();
/*     */           break;
/*     */         case "magic":
/* 126 */           targetStats.incrementMagic();
/*     */           break;
/*     */       } 
/*     */ 
/*     */ 
/*     */ 
/*     */       
/* 133 */       scheduleAsyncSave();
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
/*     */   private void scheduleAsyncSave() {
/* 145 */     if (this.shutdown) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 150 */     this.saveLock.writeLock().lock();
/*     */ 
/*     */     
/*     */     try {
/* 154 */       if (this.pendingSave != null && !this.pendingSave.isDone())
/*     */       {
/* 156 */         this.pendingSave.cancel(false);
/*     */       }
/*     */ 
/*     */       
/* 160 */       this.pendingSave = this.saveExecutor.schedule(() -> saveToFileAsync(), 5000L, TimeUnit.MILLISECONDS);
/*     */     
/*     */     }
/*     */     finally {
/*     */ 
/*     */       
/* 166 */       this.saveLock.writeLock().unlock();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void saveToFileAsync() {
/*     */     Map<String, CombatLogData> dataToSave;
/* 177 */     this.saveLock.readLock().lock();
/*     */     
/*     */     try {
/* 180 */       dataToSave = new HashMap<>(this.targetLogs);
/*     */     }
/*     */     finally {
/*     */       
/* 184 */       this.saveLock.readLock().unlock();
/*     */     } 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/* 191 */       File parentDir = this.logFile.getParentFile();
/* 192 */       if (parentDir != null && !parentDir.exists())
/*     */       {
/* 194 */         parentDir.mkdirs();
/*     */       }
/*     */ 
/*     */       
/* 198 */       FileWriter writer = new FileWriter(this.logFile, false);
/*     */       
/* 200 */       try { gson.toJson(dataToSave, writer);
/* 201 */         writer.flush();
/* 202 */         writer.close(); } catch (Throwable throwable) { try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; }
/*     */     
/* 204 */     } catch (IOException e) {
/*     */       
/* 206 */       Logger.norm("[CombatLogger] Failed to save combat log: " + e.getMessage());
/*     */     }
/* 208 */     catch (Exception e) {
/*     */       
/* 210 */       Logger.norm("[CombatLogger] Error in async save: " + e.getMessage());
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void saveToFile() {
/* 219 */     this.saveLock.readLock().lock();
/*     */ 
/*     */     
/*     */     try {
/* 223 */       File parentDir = this.logFile.getParentFile();
/* 224 */       if (parentDir != null && !parentDir.exists())
/*     */       {
/* 226 */         parentDir.mkdirs();
/*     */       }
/*     */ 
/*     */       
/* 230 */       FileWriter writer = new FileWriter(this.logFile, false);
/*     */       
/* 232 */       try { gson.toJson(this.targetLogs, writer);
/* 233 */         writer.flush();
/* 234 */         writer.close(); } catch (Throwable throwable) { try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; }
/*     */     
/* 236 */     } catch (IOException e) {
/*     */       
/* 238 */       Logger.norm("[CombatLogger] Failed to save combat log: " + e.getMessage());
/*     */     }
/*     */     finally {
/*     */       
/* 242 */       this.saveLock.readLock().unlock();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void shutdown() {
/* 251 */     this.shutdown = true;
/*     */ 
/*     */     
/* 254 */     this.saveLock.writeLock().lock();
/*     */     
/*     */     try {
/* 257 */       if (this.pendingSave != null && !this.pendingSave.isDone())
/*     */       {
/* 259 */         this.pendingSave.cancel(false);
/*     */       }
/*     */     }
/*     */     finally {
/*     */       
/* 264 */       this.saveLock.writeLock().unlock();
/*     */     } 
/*     */ 
/*     */     
/* 268 */     saveToFile();
/*     */ 
/*     */     
/* 271 */     this.saveExecutor.shutdown();
/*     */     
/*     */     try {
/* 274 */       if (!this.saveExecutor.awaitTermination(2L, TimeUnit.SECONDS))
/*     */       {
/* 276 */         this.saveExecutor.shutdownNow();
/*     */       }
/*     */     }
/* 279 */     catch (InterruptedException e) {
/*     */       
/* 281 */       this.saveExecutor.shutdownNow();
/* 282 */       Thread.currentThread().interrupt();
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void reset() {
/* 291 */     this.targetLogs.clear();
/* 292 */     saveToFile();
/* 293 */     Logger.norm("[CombatLogger] Reset all combat statistics");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void resetTarget(String targetRSN) {
/* 301 */     if (targetRSN != null && this.targetLogs.containsKey(targetRSN)) {
/*     */       
/* 303 */       this.targetLogs.remove(targetRSN);
/* 304 */       saveToFile();
/* 305 */       Logger.norm("[CombatLogger] Reset combat statistics for: " + targetRSN);
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public CombatLogData getLogDataForTarget(String targetRSN) {
/* 314 */     return this.targetLogs.get(targetRSN);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void logAttackWithContext(String targetRSN, String attackStyle, AttackContext context) {
/* 325 */     if (targetRSN == null || targetRSN.trim().isEmpty())
/*     */     {
/* 327 */       targetRSN = "Unknown";
/*     */     }
/*     */     
/* 330 */     if (context == null) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 336 */     CombatLogData logData = this.targetLogs.computeIfAbsent(targetRSN, k -> new CombatLogData());
/*     */ 
/*     */     
/* 339 */     int maxHistorySize = (this.config != null) ? this.config.predictionHistorySize() : 5000;
/* 340 */     logData.addAttackToHistory(context, maxHistorySize);
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 345 */     boolean playerFrozen = (context.getFreezeState() != null && (context.getFreezeState().equals("bothFrozen") || context.getFreezeState().equals("weFrozenTargetUnfrozen")));
/*     */ 
/*     */     
/* 348 */     boolean opponentFrozen = (context.getFreezeState() != null && (context.getFreezeState().equals("bothFrozen") || context.getFreezeState().equals("targetFrozenWeUnfrozen")));
/*     */     
/* 350 */     logOpponentAttack(targetRSN, attackStyle, playerFrozen, opponentFrozen);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public Map<String, CombatLogData> getAllLogs() {
/* 360 */     return new HashMap<>(this.targetLogs);
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/CombatLogger.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */