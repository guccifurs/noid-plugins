/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import com.tonic.Logger;
/*     */ import com.tonic.Static;
/*     */ import com.tonic.api.entities.PlayerAPI;
/*     */ import com.tonic.api.widgets.WidgetAPI;
/*     */ import com.tonic.data.wrappers.PlayerEx;
/*     */ import com.tonic.services.GameManager;
/*     */ import java.nio.charset.StandardCharsets;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.List;
/*     */ import java.util.Objects;
/*     */ import java.util.regex.Matcher;
/*     */ import java.util.regex.Pattern;
/*     */ import net.runelite.api.Player;
/*     */ import net.runelite.api.coords.WorldPoint;
/*     */ import net.runelite.api.events.ChatMessage;
/*     */ import net.runelite.api.widgets.Widget;
/*     */ import net.runelite.client.eventbus.Subscribe;
/*     */ 
/*     */ public class DuelChallengeHandler {
/*     */   private final AttackTimerConfig config;
/*     */   
/*     */   private enum State {
/*  25 */     IDLE,
/*  26 */     WAITING_FOR_SCREEN,
/*  27 */     HANDLING_SCREEN;
/*     */   }
/*     */   
/*  30 */   private State state = State.IDLE;
/*  31 */   private String challengeSender = null;
/*  32 */   private int ticksSinceScreenAppeared = 0;
/*  33 */   private int ticksSinceSecondConfirmStart = 0;
/*  34 */   private int ticksSinceFirstConfirm = 0;
/*  35 */   private int ticksSinceChallengeSent = 0;
/*  36 */   private int ticksSinceChallengeReceived = 0;
/*  37 */   private int lastClickedWidgetId = -1;
/*  38 */   private int lastClickedChildId = -1;
/*  39 */   private int lastClickTick = -1;
/*  40 */   private int challengeRetryAttempts = 0;
/*  41 */   private int ticksSinceMaxMedClicked = 0;
/*     */   
/*     */   private static final int MAX_CHALLENGE_RETRY_ATTEMPTS = 10;
/*     */   
/*     */   private static final int MAX_SECOND_CONFIRM_TICKS = 100;
/*     */   
/*     */   private static final int CHALLENGE_TIMEOUT_TICKS = 5;
/*     */   private static final int CHALLENGE_RECEIVE_COOLDOWN_TICKS = 5;
/*     */   private static final int FIRST_CONFIRM_DELAY_TICKS = 5;
/*     */   private static final int MAX_MED_TO_FIRST_CONFIRM_DELAY = 1;
/*     */   private boolean maxMedClicked = false;
/*     */   private boolean firstConfirmClicked = false;
/*     */   private boolean challengeSent = false;
/*     */   
/*     */   public DuelChallengeHandler(AttackTimerConfig config) {
/*  56 */     this.config = config;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isPendingDuelConfirmation() {
/*  64 */     return (this.state != State.IDLE);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String getExpectedSenderRSN() {
/*  72 */     return this.challengeSender;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void setHasClickedAccept(boolean clicked) {}
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean hasClickedAccept() {
/*  88 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void resetChallengeFlags() {
/*  96 */     synchronized (this) {
/*     */       
/*  98 */       this.state = State.IDLE;
/*  99 */       this.challengeSender = null;
/* 100 */       this.ticksSinceScreenAppeared = 0;
/* 101 */       this.ticksSinceSecondConfirmStart = 0;
/* 102 */       this.ticksSinceFirstConfirm = 0;
/* 103 */       this.ticksSinceChallengeSent = 0;
/* 104 */       this.ticksSinceChallengeReceived = 0;
/* 105 */       this.maxMedClicked = false;
/* 106 */       this.firstConfirmClicked = false;
/* 107 */       this.challengeSent = false;
/* 108 */       this.challengeRetryAttempts = 0;
/* 109 */       this.lastClickedWidgetId = -1;
/* 110 */       this.lastClickedChildId = -1;
/* 111 */       this.lastClickTick = -1;
/* 112 */       this.ticksSinceMaxMedClicked = 0;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean shouldClickWidget(int widgetId, int childId, int currentTick) {
/* 121 */     synchronized (this) {
/*     */       
/* 123 */       if (this.lastClickedWidgetId != widgetId || this.lastClickedChildId != childId || this.lastClickTick != currentTick) {
/*     */         
/* 125 */         this.lastClickedWidgetId = widgetId;
/* 126 */         this.lastClickedChildId = childId;
/* 127 */         this.lastClickTick = currentTick;
/* 128 */         return true;
/*     */       } 
/* 130 */       return false;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private String normalizeName(String name) {
/* 140 */     if (name == null)
/*     */     {
/* 142 */       return "";
/*     */     }
/*     */     
/* 145 */     String normalized = name.replaceAll("<[^>]+>", "");
/*     */     
/* 147 */     normalized = normalized.replaceAll("<col=[^>]+>", "");
/* 148 */     normalized = normalized.replace("</col>", "");
/*     */     
/* 150 */     normalized = normalized.replace(' ', ' ');
/* 151 */     normalized = normalized.replace(' ', ' ');
/* 152 */     normalized = normalized.replace(' ', ' ');
/*     */     
/* 154 */     normalized = normalized.replaceAll("\\s+", " ").trim();
/* 155 */     return normalized;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean namesMatch(String name1, String name2) {
/* 164 */     if (name1 == null || name2 == null)
/*     */     {
/* 166 */       return false;
/*     */     }
/*     */     
/* 169 */     String normalized1 = normalizeName(name1);
/* 170 */     String normalized2 = normalizeName(name2);
/*     */ 
/*     */     
/* 173 */     if (normalized1.equalsIgnoreCase(normalized2))
/*     */     {
/* 175 */       return true;
/*     */     }
/*     */ 
/*     */     
/* 179 */     String noSpaces1 = normalized1.replaceAll("\\s+", "");
/* 180 */     String noSpaces2 = normalized2.replaceAll("\\s+", "");
/* 181 */     if (noSpaces1.equalsIgnoreCase(noSpaces2) && !noSpaces1.isEmpty())
/*     */     {
/* 183 */       return true;
/*     */     }
/*     */     
/* 186 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private String extractSenderName(ChatMessage event, String message) {
/* 196 */     String sender = event.getName();
/*     */     
/* 198 */     if (sender != null && !sender.isEmpty()) {
/*     */ 
/*     */       
/* 201 */       String normalized = normalizeName(sender);
/* 202 */       if (this.config.autoAcceptDuelLogging())
/*     */       {
/* 204 */         Logger.norm("[Auto Accept Duel] Extracted sender from event.getName(): '" + normalized + "' (original: '" + sender + "')");
/*     */       }
/* 206 */       return normalized;
/*     */     } 
/*     */ 
/*     */     
/* 210 */     if (message.contains("<col=")) {
/*     */ 
/*     */       
/* 213 */       Pattern pattern = Pattern.compile("<col=[^>]+>([^<]+)</col>");
/* 214 */       Matcher matcher = pattern.matcher(message);
/* 215 */       if (matcher.find()) {
/*     */         
/* 217 */         sender = matcher.group(1);
/* 218 */         if (sender != null && !sender.trim().isEmpty()) {
/*     */           
/* 220 */           String normalized = normalizeName(sender);
/* 221 */           if (this.config.autoAcceptDuelLogging())
/*     */           {
/* 223 */             Logger.norm("[Auto Accept Duel] Extracted sender from color tag: '" + normalized + "' (original: '" + sender + "')");
/*     */           }
/* 225 */           return normalized;
/*     */         } 
/*     */       } 
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 232 */     String lowerMessage = message.toLowerCase();
/* 233 */     String[] patterns = { " is inviting you", " challenges you", " sent you", " challenged you", " inviting you to", " challenges you to" };
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 242 */     for (String pattern : patterns) {
/*     */       
/* 244 */       int idx = lowerMessage.indexOf(pattern);
/* 245 */       if (idx > 0) {
/*     */ 
/*     */         
/* 248 */         String potentialName = message.substring(0, idx);
/*     */ 
/*     */         
/* 251 */         if (potentialName.contains("<col=")) {
/*     */           
/* 253 */           int startIdx = potentialName.indexOf(">");
/* 254 */           int endIdx = potentialName.indexOf("</col>");
/* 255 */           if (startIdx > 0 && endIdx > startIdx)
/*     */           {
/* 257 */             potentialName = potentialName.substring(startIdx + 1, endIdx);
/*     */           }
/*     */         } 
/*     */ 
/*     */         
/* 262 */         potentialName = potentialName.replaceAll("<[^>]+>", "");
/* 263 */         potentialName = potentialName.trim();
/*     */ 
/*     */         
/* 266 */         if (potentialName.length() > 0 && potentialName.length() <= 30) {
/*     */           
/* 268 */           String normalized = normalizeName(potentialName);
/* 269 */           if (this.config.autoAcceptDuelLogging())
/*     */           {
/* 271 */             Logger.norm("[Auto Accept Duel] Extracted sender from pattern '" + pattern + "': '" + normalized + "' (original: '" + potentialName + "')");
/*     */           }
/* 273 */           return normalized;
/*     */         } 
/*     */       } 
/*     */     } 
/*     */     
/* 278 */     if (this.config.autoAcceptDuelLogging())
/*     */     {
/* 280 */       Logger.norm("[Auto Accept Duel] Could not extract sender name from message: " + message);
/*     */     }
/*     */     
/* 283 */     return normalizeName(sender);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Subscribe
/*     */   public void onChatMessage(ChatMessage event) {
/* 292 */     if (!this.config.enabled() || !this.config.autoAcceptDuel()) {
/*     */       return;
/*     */     }

/*     */     
/* 297 */     String message = event.getMessage();
/* 298 */     if (message == null) {
/*     */       return;
/*     */     }
/*     */     
/*     */     // Debug: Log all chat messages when autoAcceptDuelLogging is enabled
/*     */     if (this.config.autoAcceptDuelLogging()) {
/*     */       Logger.norm("[Auto Accept Duel] Received chat message: " + message);
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 304 */     String lowerMessage = message.toLowerCase();
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 312 */     boolean isDuelInvitation = (lowerMessage.contains("inviting you to a unranked duel") || lowerMessage.contains("inviting you to an unranked duel") || lowerMessage.contains("challenges you to a duel") || lowerMessage.contains("challenges you to an unranked duel") || lowerMessage.contains("sent you a duel challenge") || lowerMessage.contains("challenged you") || (lowerMessage.contains("unranked duel") && lowerMessage.contains("invit")) || (lowerMessage.contains("duel") && (lowerMessage.contains("invit") || lowerMessage.contains("challenge"))));
/*     */     
/* 314 */     if (isDuelInvitation) {
/*     */       
/* 316 */       synchronized (this) {
/*     */ 
/*     */         
/* 319 */         if (this.ticksSinceChallengeReceived > 0 && this.ticksSinceChallengeReceived < 5) {
/*     */           
/* 321 */           if (this.config.autoAcceptDuelLogging())
/*     */           {
/* 323 */             Logger.norm("[Auto Accept Duel] Ignoring duplicate challenge - receive cooldown active (tick " + this.ticksSinceChallengeReceived + "/5)");
/*     */           }
/*     */           
/*     */           return;
/*     */         } 
/*     */         
/* 329 */         if (this.ticksSinceChallengeSent > 0 && this.ticksSinceChallengeSent < 5) {
/*     */           
/* 331 */           if (this.config.autoAcceptDuelLogging())
/*     */           {
/* 333 */             Logger.norm("[Auto Accept Duel] Ignoring challenge - send timeout active (tick " + this.ticksSinceChallengeSent + "/5)");
/*     */           }
/*     */           
/*     */           return;
/*     */         } 
/*     */       } 
/*     */       
/* 340 */       String sender = extractSenderName(event, message);
/*     */       
/* 342 */       if (sender == null || sender.isEmpty()) {
/*     */         
/* 344 */         if (this.config.autoAcceptDuelLogging())
/*     */         {
/* 346 */           Logger.norm("[Auto Accept Duel] Could not extract sender name from message: " + message);
/*     */         }
/*     */         
/*     */         return;
/*     */       } 
/* 351 */       if (this.config.autoAcceptDuelLogging())
/*     */       {
/* 353 */         Logger.norm("[Auto Accept Duel] Challenge received from '" + sender + "'");
/*     */       }
/*     */ 
/*     */ 
/*     */       
/* 358 */       synchronized (this) {
/*     */         
/* 360 */         this.state = State.WAITING_FOR_SCREEN;
/* 361 */         this.challengeSender = sender;
/* 362 */         this.ticksSinceScreenAppeared = 0;
/* 363 */         this.maxMedClicked = false;
/* 364 */         this.firstConfirmClicked = false;
/* 365 */         this.ticksSinceSecondConfirmStart = 0;
/* 366 */         this.ticksSinceFirstConfirm = 0;
/* 367 */         this.ticksSinceChallengeReceived = 1;
/* 368 */         this.challengeSent = false;
/* 369 */         this.challengeRetryAttempts = 0;
/*     */       } 
/*     */       
/* 372 */       if (this.config.autoAcceptDuelLogging())
/*     */       {
/* 374 */         Logger.norm("[Auto Accept Duel] ✓ Challenge received from '" + sender + "' - will send challenge back");
/*     */       }
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void sendChallengeBackIfNeeded() {
/* 384 */     if (!this.config.enabled() || !this.config.autoAcceptDuel()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 390 */     synchronized (this) {
/*     */       
/* 392 */       if (this.ticksSinceChallengeSent > 0) {
/*     */         
/* 394 */         this.ticksSinceChallengeSent++;
/* 395 */         if (this.ticksSinceChallengeSent >= 5)
/*     */         {
/* 397 */           this.ticksSinceChallengeSent = 0;
/*     */         }
/*     */       } 
/*     */       
/* 401 */       if (this.ticksSinceChallengeReceived > 0) {
/*     */         
/* 403 */         this.ticksSinceChallengeReceived++;
/* 404 */         if (this.ticksSinceChallengeReceived >= 5)
/*     */         {
/* 406 */           this.ticksSinceChallengeReceived = 0;
/*     */         }
/*     */       } 
/*     */ 
/*     */ 
/*     */       
/* 412 */       if (this.state != State.WAITING_FOR_SCREEN || this.challengeSender == null || this.challengeSent || this.ticksSinceChallengeSent > 0) {
/*     */         return;
/*     */       }
/*     */ 
/*     */ 
/*     */       
/* 418 */       this.challengeSent = true;
/* 419 */       this.ticksSinceChallengeSent = 1;
/*     */     } 
/*     */     
/* 422 */     Static.invoke(() -> {
/*     */           String senderToChallenge;
/*     */ 
/*     */           
/*     */           if (!this.config.enabled() || !this.config.autoAcceptDuel()) {
/*     */             return;
/*     */           }
/*     */ 
/*     */           
/*     */           List<PlayerEx> allPlayers = GameManager.playerList();
/*     */ 
/*     */           
/*     */           PlayerEx localPlayer = PlayerAPI.getLocal();
/*     */ 
/*     */           
/*     */           if (localPlayer == null) {
/*     */             return;
/*     */           }
/*     */ 
/*     */           
/*     */           Objects.requireNonNull(localPlayer);
/*     */ 
/*     */           
/*     */           WorldPoint localPos = localPlayer.getWorldPoint();
/*     */ 
/*     */           
/*     */           Player targetPlayer = null;
/*     */ 
/*     */           
/*     */           int maxDistance = 200;
/*     */ 
/*     */           
/*     */           synchronized (this) {
/*     */             senderToChallenge = this.challengeSender;
/*     */           } 
/*     */           
/*     */           if (this.config.autoAcceptDuelLogging()) {
/*     */             Logger.norm("[Auto Accept Duel] Searching for player: '" + senderToChallenge + "' in player list (total players: " + allPlayers.size() + ")");
/*     */             
/*     */             if (senderToChallenge != null) {
/*     */               StringBuilder hex = new StringBuilder();
/*     */               
/*     */               for (byte b : senderToChallenge.getBytes(StandardCharsets.UTF_8)) {
/*     */                 hex.append(String.format("%02x ", new Object[] { Byte.valueOf(b) }));
/*     */               } 
/*     */               
/*     */               Logger.norm("[Auto Accept Duel] Name bytes (hex): " + hex.toString().trim());
/*     */             } 
/*     */           } 
/*     */           
/*     */           List<String> nearbyPlayerNames = new ArrayList<>();
/*     */           
/*     */           List<Player> matchingPlayers = new ArrayList<>();
/*     */           
/*     */           for (PlayerEx pEx : allPlayers) {
/*     */             Player p = pEx.getPlayer();
/*     */             if (p == null || p.getName() == null) {
/*     */               continue;
/*     */             }
/*     */             
/*     */             if (pEx.getId() == localPlayer.getId()) {
/*     */               continue;
/*     */             }
/*     */             
/*     */             Objects.requireNonNull(p);
/*     */             
/*     */             String playerName = (String)Static.invoke(p::getName);
/*     */             
/*     */             if (playerName != null) {
/*     */               if (namesMatch(playerName, senderToChallenge)) {
/*     */                 matchingPlayers.add(p);
/*     */               }
/*     */             }
/*     */           } 
/*     */           
/*     */           if (!matchingPlayers.isEmpty()) {
/*     */             Player closestPlayer = null;
/*     */             
/*     */             int closestDistance = Integer.MAX_VALUE;
/*     */             
/*     */             for (Player p : matchingPlayers) {
/*     */               Objects.requireNonNull(p);
/*     */               
/*     */               WorldPoint pPos = (WorldPoint)Static.invoke(p::getWorldLocation);
/*     */               
/*     */               if (localPos != null && pPos != null) {
/*     */                 int distance = Math.abs(pPos.getX() - localPos.getX()) + Math.abs(pPos.getY() - localPos.getY());
/*     */                 
/*     */                 if (distance < closestDistance && distance <= maxDistance) {
/*     */                   closestDistance = distance;
/*     */                   
/*     */                   closestPlayer = p;
/*     */                 } 
/*     */                 
/*     */                 continue;
/*     */               } 
/*     */               
/*     */               if (closestPlayer == null) {
/*     */                 closestPlayer = p;
/*     */               }
/*     */             } 
/*     */             
/*     */             if (closestPlayer != null) {
/*     */               targetPlayer = closestPlayer;
/*     */               
/*     */               Objects.requireNonNull(closestPlayer);
/*     */               
/*     */               String playerName = (String)Static.invoke(closestPlayer::getName);
/*     */               
/*     */               if (this.config.autoAcceptDuelLogging()) {
/*     */                 Logger.norm("[Auto Accept Duel] ✓ Found matching player: '" + playerName + "' (matched with '" + senderToChallenge + "') at distance " + closestDistance);
/*     */               }
/*     */             } else if (this.config.autoAcceptDuelLogging()) {
/*     */               Logger.norm("[Auto Accept Duel] Found " + matchingPlayers.size() + " matching player(s) but all are outside distance limit (" + maxDistance + ")");
/*     */             } 
/*     */           } 
/*     */           
/*     */           for (PlayerEx pEx : allPlayers) {
/*     */             Player p = pEx.getPlayer();
/*     */             if (p == null || p.getName() == null || pEx.getId() == localPlayer.getId()) {
/*     */               continue;
/*     */             }
/*     */             
/*     */             Objects.requireNonNull(p);
/*     */             
/*     */             WorldPoint pPos = (WorldPoint)Static.invoke(p::getWorldLocation);
/*     */             
/*     */             boolean withinDistance = true;
/*     */             
/*     */             if (localPos != null && pPos != null) {
/*     */               int distance = Math.abs(pPos.getX() - localPos.getX()) + Math.abs(pPos.getY() - localPos.getY());
/*     */               
/*     */               if (distance > maxDistance) {
/*     */                 withinDistance = false;
/*     */               }
/*     */             } 
/*     */             
/*     */             if (withinDistance) {
/*     */               Objects.requireNonNull(p);
/*     */               
/*     */               String playerName = (String)Static.invoke(p::getName);
/*     */               
/*     */               if (playerName != null) {
/*     */                 nearbyPlayerNames.add(playerName);
/*     */               }
/*     */             } 
/*     */           } 
/*     */           
/*     */           if (targetPlayer == null && this.config.autoAcceptDuelLogging()) {
/*     */             if (!nearbyPlayerNames.isEmpty()) {
/*     */               Logger.norm("[Auto Accept Duel] Nearby players found (" + nearbyPlayerNames.size() + "): " + String.join(", ", (Iterable)nearbyPlayerNames));
/*     */             } else {
/*     */               Logger.norm("[Auto Accept Duel] No nearby players found within distance " + maxDistance);
/*     */             } 
/*     */             
/*     */             Logger.norm("[Auto Accept Duel] Searching for: '" + senderToChallenge + "' (normalized: '" + normalizeName(senderToChallenge) + "')");
/*     */             
/*     */             for (String nearbyName : nearbyPlayerNames) {
/*     */               String normalized = normalizeName(nearbyName);
/*     */               
/*     */               String noSpaces = normalized.replaceAll("\\s+", "");
/*     */               
/*     */               String searchNormalized = normalizeName(senderToChallenge);
/*     */               
/*     */               String searchNoSpaces = searchNormalized.replaceAll("\\s+", "");
/*     */               
/*     */               StringBuilder nearbyHex = new StringBuilder();
/*     */               
/*     */               for (byte b : nearbyName.getBytes(StandardCharsets.UTF_8)) {
/*     */                 nearbyHex.append(String.format("%02x ", new Object[] { Byte.valueOf(b) }));
/*     */               } 
/*     */               
/* 592 */               boolean matches = (normalized.equalsIgnoreCase(searchNormalized) || noSpaces.equalsIgnoreCase(searchNoSpaces));
/*     */               Logger.norm("[Auto Accept Duel]   - '" + nearbyName + "' -> normalized: '" + normalized + "', no-spaces: '" + noSpaces + "' | hex: " + nearbyHex.toString().trim() + " | Match: " + matches);
/*     */             } 
/*     */             if (nearbyPlayerNames.isEmpty()) {
/*     */               Logger.norm("[Auto Accept Duel] Checking ALL players for name match (ignoring distance)...");
/*     */               for (PlayerEx pEx : allPlayers) {
/*     */                 Player p = pEx.getPlayer();
/*     */                 if (p == null || p.getName() == null || pEx.getId() == localPlayer.getId()) {
/*     */                   continue;
/*     */                 }
/*     */                 Objects.requireNonNull(p);
/*     */                 String playerName = (String)Static.invoke(p::getName);
/*     */                 if (playerName != null && namesMatch(playerName, senderToChallenge)) {
/*     */                   Logger.norm("[Auto Accept Duel] ✓ Found player '" + playerName + "' in full list but outside distance limit!");
/*     */                   targetPlayer = p;
/*     */                   break;
/*     */                 } 
/*     */               } 
/*     */             } 
/*     */           } 
/*     */           synchronized (this) {
/*     */             if (this.state != State.WAITING_FOR_SCREEN || !senderToChallenge.equals(this.challengeSender)) {
/*     */               return;
/*     */             }
/*     */             if (targetPlayer != null) {
/*     */               PlayerEx targetPlayerEx = null;
/*     */               for (PlayerEx pEx : allPlayers) {
/*     */                 if (pEx.getPlayer() == targetPlayer) {
/*     */                   targetPlayerEx = pEx;
/*     */                   break;
/*     */                 }
/*     */               }
/*     */               if (targetPlayerEx != null) {
/*     */                 PlayerAPI.interact(targetPlayerEx, new String[] { "Challenge" });
/*     */               }
/*     */               this.challengeRetryAttempts = 0;
/*     */               if (this.config.autoAcceptDuelLogging()) {
/*     */                 Logger.norm("[Auto Accept Duel] ✓ Sent challenge back to '" + senderToChallenge + "' (ONCE) - 5 tick timeout active");
/*     */               }
/*     */             } else {
/*     */               this.challengeSent = false;
/*     */               this.ticksSinceChallengeSent = 0;
/*     */               this.challengeRetryAttempts++;
/*     */               if (this.challengeRetryAttempts >= 10) {
/*     */                 this.state = State.IDLE;
/*     */                 this.challengeSender = null;
/*     */                 this.challengeRetryAttempts = 0;
/*     */                 if (this.config.autoAcceptDuelLogging()) {
/*     */                   Logger.norm("[Auto Accept Duel] Could not find player '" + senderToChallenge + "' after 10 attempts - giving up");
/*     */                 }
/*     */               } else if (this.challengeRetryAttempts % 3 == 0) {
/*     */                 if (this.config.autoAcceptDuelLogging()) {
/*     */                   Logger.norm("[Auto Accept Duel] Could not find player '" + senderToChallenge + "' yet (attempt " + this.challengeRetryAttempts + "/10)");
/*     */                 }
/*     */               } 
/*     */             } 
/*     */           } 
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
/*     */   public void handleDuelConfirmation() {
/* 676 */     if (!this.config.enabled() || !this.config.autoAcceptDuel()) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/* 681 */     Static.invoke(() -> {
/*     */           Widget firstConfirmCheck = WidgetAPI.get(757, 22);
/*     */           
/*     */           Widget maxMedCheck = WidgetAPI.get(757, 10, 0);
/* 685 */           boolean screenVisible = ((firstConfirmCheck != null && firstConfirmCheck.getId() != -1) || (maxMedCheck != null && maxMedCheck.getId() != -1 && WidgetAPI.isVisible(maxMedCheck)));
/*     */           
/*     */           synchronized (this) {
/*     */             if (!screenVisible && this.state == State.HANDLING_SCREEN) {
/*     */               if (this.config.autoAcceptDuelLogging()) {
/*     */                 Logger.norm("[Auto Accept Duel] Confirmation screen disappeared - resetting");
/*     */               }
/*     */               
/*     */               resetChallengeFlags();
/*     */               
/*     */               return;
/*     */             } 
/*     */             
/*     */             if (screenVisible && this.state == State.WAITING_FOR_SCREEN) {
/*     */               this.state = State.HANDLING_SCREEN;
/*     */               
/*     */               this.ticksSinceScreenAppeared = 0;
/*     */               
/*     */               this.maxMedClicked = false;
/*     */               
/*     */               this.firstConfirmClicked = false;
/*     */               
/*     */               this.ticksSinceSecondConfirmStart = 0;
/*     */               
/*     */               this.ticksSinceMaxMedClicked = 0;
/*     */               
/*     */               if (this.config.autoAcceptDuelLogging()) {
/*     */                 Logger.norm("[Auto Accept Duel] Confirmation screen appeared - starting to handle (clicking immediately)");
/*     */               }
/*     */             } 
/*     */             
/*     */             if (screenVisible && this.state == State.IDLE) {
/*     */               this.state = State.HANDLING_SCREEN;
/*     */               
/*     */               this.ticksSinceScreenAppeared = 0;
/*     */               
/*     */               this.maxMedClicked = false;
/*     */               
/*     */               this.firstConfirmClicked = false;
/*     */               
/*     */               this.ticksSinceSecondConfirmStart = 0;
/*     */               
/*     */               this.ticksSinceMaxMedClicked = 0;
/*     */               
/*     */               if (this.config.autoAcceptDuelLogging()) {
/*     */                 Logger.norm("[Auto Accept Duel] Confirmation screen appeared (new challenge) - starting to handle (clicking immediately)");
/*     */               }
/*     */             } 
/*     */             
/*     */             if (this.state != State.HANDLING_SCREEN) {
/*     */               return;
/*     */             }
/*     */           } 
/*     */           
/*     */           this.ticksSinceScreenAppeared++;
/*     */           
/*     */           int currentTick = GameManager.getTickCount();
/*     */           
/*     */           Widget firstConfirmCheck2 = WidgetAPI.get(757, 22);
/*     */           
/*     */           Widget maxMedCheck2 = WidgetAPI.get(757, 10, 0);
/*     */           Widget secondConfirmCheck = WidgetAPI.get(757, 143);
/* 747 */           boolean allGone = ((firstConfirmCheck2 == null || firstConfirmCheck2.getId() == -1) && (maxMedCheck2 == null || maxMedCheck2.getId() == -1) && (secondConfirmCheck == null || secondConfirmCheck.getId() == -1));
/*     */           if (allGone) {
/*     */             synchronized (this) {
/*     */               if (this.config.autoAcceptDuelLogging())
/*     */                 Logger.norm("[Auto Accept Duel] All widgets gone - duel starting (SUCCESS)"); 
/*     */               resetChallengeFlags();
/*     */             } 
/*     */             return;
/*     */           } 
/*     */           synchronized (this) {
/*     */             if (!this.maxMedClicked) {
/*     */               Widget maxMedWidget = WidgetAPI.get(757, 10, 0);
/*     */               if (this.config.autoAcceptDuelLogging()) {
/*     */                 Logger.norm("[Auto Accept Duel] Checking Max/Med widget (757:10:0) - widget: " + ((maxMedWidget != null) ? "found" : "null") + ", visible: " + String.valueOf((maxMedWidget != null) ? Boolean.valueOf(WidgetAPI.isVisible(maxMedWidget)) : "N/A") + ", shouldClick: " + String.valueOf((maxMedWidget != null) ? Boolean.valueOf(shouldClickWidget(757, 10, currentTick)) : "N/A") + ", ticksSinceScreenAppeared: " + this.ticksSinceScreenAppeared);
/*     */                 if (maxMedWidget != null) {
/*     */                   String[] actions = maxMedWidget.getActions();
/*     */                   Logger.norm("[Auto Accept Duel] Max/Med widget actions: " + ((actions != null) ? Arrays.toString((Object[])actions) : "null") + ", widget ID: " + maxMedWidget.getId());
/*     */                 } 
/*     */               } 
/*     */               if (maxMedWidget != null && WidgetAPI.isVisible(maxMedWidget) && shouldClickWidget(757, 10, currentTick)) {
/*     */                 String[] actions = maxMedWidget.getActions();
/*     */                 if (actions != null && actions.length > 0) {
/*     */                   for (int i = 0; i < actions.length; i++) {
/*     */                     if (actions[i] != null && !actions[i].isEmpty()) {
/*     */                       if (this.config.autoAcceptDuelLogging())
/*     */                         Logger.norm("[Auto Accept Duel] Clicking Max/Med using action: '" + actions[i] + "' (index: " + i + ")"); 
/*     */                       WidgetAPI.interact(maxMedWidget, i + 1);
/*     */                       break;
/*     */                     } 
/*     */                   } 
/*     */                 } else {
/*     */                   if (this.config.autoAcceptDuelLogging())
/*     */                     Logger.norm("[Auto Accept Duel] Max/Med widget has no actions - using direct packet method (action=1)"); 
/*     */                   WidgetAPI.interact(1, 757, 10, 0, -1);
/*     */                 } 
/*     */                 this.maxMedClicked = true;
/*     */                 this.ticksSinceMaxMedClicked = 0;
/*     */                 if (this.config.autoAcceptDuelLogging())
/*     */                   Logger.norm("[Auto Accept Duel] ✓ Clicked Max/Med (757:10:0) - will click First Confirm after 1 tick"); 
/*     */                 return;
/*     */               } 
/*     */               if (this.config.autoAcceptDuelLogging())
/*     */                 if (maxMedWidget == null) {
/*     */                   Logger.norm("[Auto Accept Duel] Max/Med widget (757:10:0) is NULL - cannot click");
/*     */                 } else if (!WidgetAPI.isVisible(maxMedWidget)) {
/*     */                   Logger.norm("[Auto Accept Duel] Max/Med widget (757:10:0) is NOT VISIBLE - cannot click");
/*     */                 } else if (!shouldClickWidget(757, 10, currentTick)) {
/*     */                   Logger.norm("[Auto Accept Duel] Max/Med widget (757:10:0) shouldClickWidget returned FALSE (already clicked this tick?)");
/*     */                 }  
/*     */             } 
/*     */             if (this.maxMedClicked)
/*     */               this.ticksSinceMaxMedClicked++; 
/*     */             if (this.maxMedClicked && !this.firstConfirmClicked && this.ticksSinceMaxMedClicked >= 1) {
/*     */               Widget firstConfirmWidget = WidgetAPI.get(757, 22);
/*     */               if (firstConfirmWidget != null && firstConfirmWidget.getId() != -1 && shouldClickWidget(757, 22, currentTick)) {
/*     */                 WidgetAPI.interact(firstConfirmWidget, new String[] { "Confirm" });
/*     */                 this.firstConfirmClicked = true;
/*     */                 this.ticksSinceFirstConfirm = 0;
/*     */                 this.ticksSinceSecondConfirmStart = 0;
/*     */                 if (this.config.autoAcceptDuelLogging())
/*     */                   Logger.norm("[Auto Accept Duel] ✓ Clicked First Confirm (757:22) after Max/Med (tick " + this.ticksSinceMaxMedClicked + " after max/med)"); 
/*     */                 return;
/*     */               } 
/*     */               if (this.config.autoAcceptDuelLogging() && this.ticksSinceMaxMedClicked == 1)
/*     */                 Logger.norm("[Auto Accept Duel] Max/Med clicked but First Confirm (757:22) not ready yet - will retry next tick"); 
/*     */             } 
/*     */             if (this.firstConfirmClicked) {
/*     */               this.ticksSinceFirstConfirm++;
/*     */               if (this.ticksSinceFirstConfirm < 5) {
/*     */                 if (this.config.autoAcceptDuelLogging() && this.ticksSinceFirstConfirm == 1)
/*     */                   Logger.norm("[Auto Accept Duel] Waiting 5 ticks after First Confirm before trying Second Confirm..."); 
/*     */                 return;
/*     */               } 
/*     */               this.ticksSinceSecondConfirmStart++;
/*     */               if (this.ticksSinceSecondConfirmStart > 100) {
/*     */                 if (this.config.autoAcceptDuelLogging())
/*     */                   Logger.norm("[Auto Accept Duel] Timeout waiting for Second Confirm (757:143) after 100 ticks - giving up"); 
/*     */                 resetChallengeFlags();
/*     */                 return;
/*     */               } 
/*     */               if (shouldClickWidget(757, 143, currentTick)) {
/*     */                 if (this.config.autoAcceptDuelLogging())
/*     */                   Logger.norm("[Auto Accept Duel] Sending packet: action=1, widgetId=InterfaceID.PvpArenaUnrankedduel.OPPONENT_CONFIRM, childId=-1, itemId=-1"); 
/*     */                 WidgetAPI.interact(1, 49610895, -1, -1);
/*     */                 if (this.config.autoAcceptDuelLogging())
/*     */                   Logger.norm("[Auto Accept Duel] ✓ Clicked Second Confirm using InterfaceID constant - tick " + this.ticksSinceSecondConfirmStart); 
/*     */               } 
/*     */             } 
/*     */           } 
/*     */         });
/*     */   }
/*     */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/DuelChallengeHandler.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */