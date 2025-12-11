package com.tonic.plugins.attacktimer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tonic.Logger;
import com.tonic.Static;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimpleCombatLogger {
  private static final String LOG_FILE_NAME = "simple-combat-attack-log.json";
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  
  // Structure: targetRSN -> freezeState -> distance -> myAttackTimer -> attackStyle -> count
  private final Map<String, Map<String, Map<String, Map<String, Map<String, Integer>>>>> data;
  private final File logFile;
  private final AttackTimerConfig config;
  private final ScheduledExecutorService saveExecutor;
  
  public SimpleCombatLogger(AttackTimerConfig config) {
    this.config = config;
    Path vitaDir = Static.VITA_DIR;
    this.logFile = new File(vitaDir.toFile(), LOG_FILE_NAME);
    
    Map<String, Map<String, Map<String, Map<String, Map<String, Integer>>>>> loaded = null;
    if (this.logFile.exists()) {
      try {
        String json = new String(Files.readAllBytes(this.logFile.toPath()));
        Type type = new TypeToken<Map<String, Map<String, Map<String, Map<String, Map<String, Integer>>>>>>() {}.getType();
        loaded = gson.fromJson(json, type);
        Logger.norm("[SimpleCombatLogger] Loaded existing log from: " + this.logFile.getAbsolutePath());
      } catch (Exception e) {
        Logger.norm("[SimpleCombatLogger] Failed to load existing log, creating new: " + e.getMessage());
      }
    }
    this.data = (loaded != null) ? loaded : new HashMap<>();
    
    this.saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "SimpleCombatLogger-Save");
      t.setDaemon(true);
      return t;
    });
  }
  
  public void logAttack(String targetRSN, int distance, int myAttackTimer, String freezeState, String attackStyle) {
    if (targetRSN == null || targetRSN.trim().isEmpty() || attackStyle == null) {
      return;
    }
    
    String distanceKey = String.valueOf(distance);
    String timerKey = String.valueOf(myAttackTimer);
    
    Map<String, Map<String, Map<String, Map<String, Integer>>>> targetData = this.data.computeIfAbsent(targetRSN, k -> new HashMap<>());
    Map<String, Map<String, Map<String, Integer>>> freezeData = targetData.computeIfAbsent(freezeState, k -> new HashMap<>());
    Map<String, Map<String, Integer>> distanceMap = freezeData.computeIfAbsent(distanceKey, k -> new HashMap<>());
    Map<String, Integer> timerMap = distanceMap.computeIfAbsent(timerKey, k -> new HashMap<>());
    
    String styleKey = attackStyle.toLowerCase();
    timerMap.merge(styleKey, 1, Integer::sum);
    
    if (this.config.debug()) {
      Logger.norm("[SimpleCombatLogger] Logged: freeze=" + freezeState + " | " + targetRSN + " | distance=" + distance + " | myTimer=" + myAttackTimer + " | style=" + attackStyle);
    }
    
    scheduleAsyncSave();
  }
  
  public PrayerOdds getPrayerOdds(String targetRSN, int distance, int myAttackTimer, String freezeState) {
    String distanceKey = String.valueOf(distance);
    String timerKey = String.valueOf(myAttackTimer);
    
    Map<String, Map<String, Map<String, Map<String, Integer>>>> targetData = this.data.get(targetRSN);
    if (targetData == null) {
      return new PrayerOdds(33, 33, 34); // fallback
    }
    
    Map<String, Map<String, Map<String, Integer>>> freezeData = targetData.get(freezeState);
    if (freezeData == null) {
      return new PrayerOdds(33, 33, 34); // fallback
    }
    
    Map<String, Map<String, Integer>> distanceMap = freezeData.get(distanceKey);
    if (distanceMap == null) {
      return new PrayerOdds(33, 33, 34); // fallback
    }
    
    Map<String, Integer> timerMap = distanceMap.get(timerKey);
    if (timerMap == null || timerMap.isEmpty()) {
      return new PrayerOdds(33, 33, 34); // fallback
    }
    
    int ranged = timerMap.getOrDefault("ranged", 0);
    int melee = timerMap.getOrDefault("melee", 0);
    int magic = timerMap.getOrDefault("magic", 0);
    int total = ranged + melee + magic;
    
    if (total == 0) {
      return new PrayerOdds(33, 33, 34); // fallback
    }
    
    return new PrayerOdds(
      (ranged * 100) / total,
      (melee * 100) / total,
      (magic * 100) / total
    );
  }
  
  private void scheduleAsyncSave() {
    this.saveExecutor.schedule(this::saveToFileAsync, 3, TimeUnit.SECONDS);
  }
  
  private void saveToFileAsync() {
    try {
      String json = gson.toJson(this.data);
      Files.write(this.logFile.toPath(), json.getBytes());
      if (this.config.debug()) {
        Logger.norm("[SimpleCombatLogger] Saved data to: " + this.logFile.getAbsolutePath());
      }
    } catch (IOException e) {
      Logger.norm("[SimpleCombatLogger] Failed to save data: " + e.getMessage());
    }
  }
  
  public void shutdown() {
    saveToFileAsync();
    this.saveExecutor.shutdown();
    try {
      if (!this.saveExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
        this.saveExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      this.saveExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
  
  public static class PrayerOdds {
    public final int rangedPercent;
    public final int meleePercent;
    public final int magicPercent;
    
    public PrayerOdds(int rangedPercent, int meleePercent, int magicPercent) {
      this.rangedPercent = rangedPercent;
      this.meleePercent = meleePercent;
      this.magicPercent = magicPercent;
    }
  }
}
