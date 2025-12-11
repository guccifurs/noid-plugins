/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import java.io.FileWriter;
/*     */ import java.io.IOException;
/*     */ import java.nio.file.Path;
/*     */ import java.util.List;
/*     */ import java.util.Map;
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
/*     */ public class TrainingDataExporter
/*     */ {
/*     */   private static final String TRAINING_DATA_FILE = "attack_prediction_training_data.csv";
/*     */   
/*     */   public static void exportToCSV(Map<String, CombatLogData> allLogs, Path outputDir) {
/*  26 */     if (allLogs == null || allLogs.isEmpty()) {
/*     */       
/*  28 */       Logger.norm("[TrainingDataExporter] No combat log data to export");
/*     */       
/*     */       return;
/*     */     } 
/*  32 */     Path csvFile = outputDir.resolve("attack_prediction_training_data.csv");
/*     */     try {
/*  34 */       FileWriter writer = new FileWriter(csvFile.toFile());
/*     */ 
/*     */       
/*  37 */       try { writer.write(getCSVHeader() + "\n");
/*     */         
/*  39 */         int totalExamples = 0;
/*     */ 
/*     */         
/*  42 */         for (Map.Entry<String, CombatLogData> entry : allLogs.entrySet()) {
/*     */           
/*  44 */           String targetRSN = entry.getKey();
/*  45 */           CombatLogData logData = entry.getValue();
/*  46 */           List<AttackContext> history = logData.getAttackHistory();
/*     */ 
/*     */           
/*  49 */           if (history.size() < 2) {
/*     */             continue;
/*     */           }
/*     */ 
/*     */ 
/*     */           
/*  55 */           for (int i = 1; i < history.size(); i++) {
/*     */             
/*  57 */             AttackContext context = history.get(i - 1);
/*  58 */             AttackContext nextAttack = history.get(i);
/*     */             
/*  60 */             String label = nextAttack.getAttackStyle();
/*  61 */             if (label != null && !label.isEmpty()) {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */               
/*  67 */               writer.write(convertContextToCSVRow(context, label) + "\n");
/*  68 */               totalExamples++;
/*     */             } 
/*     */           } 
/*     */         } 
/*  72 */         Logger.norm("[TrainingDataExporter] Exported " + totalExamples + " training examples to: " + String.valueOf(csvFile.toAbsolutePath()));
/*  73 */         writer.close(); } catch (Throwable throwable) { try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; } 
/*  74 */     } catch (IOException e) {
/*     */       
/*  76 */       Logger.error("[TrainingDataExporter] Failed to export training data: " + e.getMessage());
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private static String getCSVHeader() {
/*  85 */     return String.join(",", new CharSequence[] { "freeze_state_bothFrozen", "freeze_state_bothUnfrozen", "freeze_state_targetFrozenWeUnfrozen", "freeze_state_weFrozenTargetUnfrozen", "overhead_prayer_melee", "overhead_prayer_ranged", "overhead_prayer_magic", "overhead_prayer_none", "weapon_whip", "weapon_crossbow", "weapon_staff", "weapon_ags", "weapon_claws", "weapon_bow", "weapon_unknown", "pid_status_ON_PID", "pid_status_OFF_PID", "pid_status_UNKNOWN", "target_hp", "target_spec", "distance", "player_freeze_ticks", "opponent_freeze_ticks", "next_attack" });
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private static String convertContextToCSVRow(AttackContext context, String label) {
/* 124 */     String freezeState = (context.getFreezeState() != null) ? context.getFreezeState() : "unknown";
/* 125 */     int freeze_bothFrozen = freezeState.equals("bothFrozen") ? 1 : 0;
/* 126 */     int freeze_bothUnfrozen = freezeState.equals("bothUnfrozen") ? 1 : 0;
/* 127 */     int freeze_targetFrozenWeUnfrozen = freezeState.equals("targetFrozenWeUnfrozen") ? 1 : 0;
/* 128 */     int freeze_weFrozenTargetUnfrozen = freezeState.equals("weFrozenTargetUnfrozen") ? 1 : 0;
/*     */ 
/*     */     
/* 131 */     String prayer = (context.getOverheadPrayer() != null) ? context.getOverheadPrayer() : "none";
/* 132 */     int prayer_melee = prayer.equals("melee") ? 1 : 0;
/* 133 */     int prayer_ranged = prayer.equals("ranged") ? 1 : 0;
/* 134 */     int prayer_magic = prayer.equals("magic") ? 1 : 0;
/* 135 */     int prayer_none = prayer.equals("none") ? 1 : 0;
/*     */ 
/*     */     
/* 138 */     String weapon = (context.getWeapon() != null) ? context.getWeapon() : "unknown";
/* 139 */     int weapon_whip = weapon.equals("whip") ? 1 : 0;
/* 140 */     int weapon_crossbow = weapon.equals("crossbow") ? 1 : 0;
/* 141 */     int weapon_staff = weapon.equals("staff") ? 1 : 0;
/* 142 */     int weapon_ags = weapon.equals("ags") ? 1 : 0;
/* 143 */     int weapon_claws = weapon.equals("claws") ? 1 : 0;
/* 144 */     int weapon_bow = weapon.equals("bow") ? 1 : 0;
/*     */ 
/*     */     
/* 147 */     int weapon_unknown = (!weapon.equals("whip") && !weapon.equals("crossbow") && !weapon.equals("staff") && !weapon.equals("ags") && !weapon.equals("claws") && !weapon.equals("bow")) ? 1 : 0;
/*     */ 
/*     */     
/* 150 */     String pidStatus = (context.getPidStatus() != null) ? context.getPidStatus() : "UNKNOWN";
/* 151 */     int pid_ON_PID = pidStatus.equals("ON_PID") ? 1 : 0;
/* 152 */     int pid_OFF_PID = pidStatus.equals("OFF_PID") ? 1 : 0;
/* 153 */     int pid_UNKNOWN = pidStatus.equals("UNKNOWN") ? 1 : 0;
/*     */ 
/*     */     
/* 156 */     int targetHP = context.getTargetHP();
/* 157 */     int targetSpec = context.getTargetSpec();
/* 158 */     int distance = context.getDistance();
/*     */     
/* 160 */     int playerFreezeTicks = -1;
/* 161 */     int opponentFreezeTicks = -1;
/*     */ 
/*     */     
/* 164 */     return String.join(",", new CharSequence[] { 
/* 165 */           String.valueOf(freeze_bothFrozen), 
/* 166 */           String.valueOf(freeze_bothUnfrozen), 
/* 167 */           String.valueOf(freeze_targetFrozenWeUnfrozen), 
/* 168 */           String.valueOf(freeze_weFrozenTargetUnfrozen), 
/* 169 */           String.valueOf(prayer_melee), 
/* 170 */           String.valueOf(prayer_ranged), 
/* 171 */           String.valueOf(prayer_magic), 
/* 172 */           String.valueOf(prayer_none), 
/* 173 */           String.valueOf(weapon_whip), 
/* 174 */           String.valueOf(weapon_crossbow), 
/* 175 */           String.valueOf(weapon_staff), 
/* 176 */           String.valueOf(weapon_ags), 
/* 177 */           String.valueOf(weapon_claws), 
/* 178 */           String.valueOf(weapon_bow), 
/* 179 */           String.valueOf(weapon_unknown), 
/* 180 */           String.valueOf(pid_ON_PID), 
/* 181 */           String.valueOf(pid_OFF_PID), 
/* 182 */           String.valueOf(pid_UNKNOWN), 
/* 183 */           String.valueOf(targetHP), 
/* 184 */           String.valueOf(targetSpec), 
/* 185 */           String.valueOf(distance), 
/* 186 */           String.valueOf(playerFreezeTicks), 
/* 187 */           String.valueOf(opponentFreezeTicks), label });
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static void exportFromCombatLogger(CombatLogger combatLogger, Path outputDir) {
/* 197 */     if (combatLogger == null) {
/*     */       
/* 199 */       Logger.norm("[TrainingDataExporter] CombatLogger is null, cannot export");
/*     */       
/*     */       return;
/*     */     } 
/* 203 */     Map<String, CombatLogData> allLogs = combatLogger.getAllLogs();
/* 204 */     exportToCSV(allLogs, outputDir);
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/TrainingDataExporter.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */