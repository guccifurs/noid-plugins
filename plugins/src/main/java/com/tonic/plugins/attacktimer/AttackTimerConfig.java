/*     */ package com.tonic.plugins.attacktimer;
/*     */ 
/*     */ import net.runelite.client.config.Config;
/*     */ import net.runelite.client.config.ConfigGroup;
/*     */ import net.runelite.client.config.ConfigItem;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ @ConfigGroup("attacktimer")
/*     */ public interface AttackTimerConfig
/*     */   extends Config
/*     */ {
/*     */   @ConfigItem(name = "Enabled", keyName = "enabled", description = "Enable attack timer detection and display", position = 0)
/*     */   default boolean enabled() {
/*  18 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Debug Animation IDs", keyName = "debug", description = "Log animation IDs to console for debugging (helps find correct IDs)", position = 1)
/*     */   default boolean debug() {
/*  29 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Debug XP Drop Detection", keyName = "debugXpDropDetection", description = "Log XP drop detection attempts using multiple methods (widget text, graphics, projectiles, etc.)", position = 2)
/*     */   default boolean debugXpDropDetection() {
/*  40 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "AI Prayers", keyName = "aiPrayers", description = "Enable AI-powered prayer switching based on opponent attack patterns", position = 3)
/*     */   default boolean aiPrayers() {
/*  51 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Reactive Prayer Switching", keyName = "reactivePrayerSwitching", description = "Switch prayers when opponent timer < 2 (timer 1 or 0): Range↔Mage (distance > 3), or switch to Melee (distance < 4, target unfrozen). Only works when AI Prayers and Contextual Predictions are disabled.", position = 3)
/*     */   default boolean reactivePrayerSwitching() {
/*  62 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Combat Automation", keyName = "combatAutomation", description = "Enable automated combat (both unfrozen state: cast ice barrage, equip tank gear)", position = 4)
/*     */   default boolean combatAutomation() {
/*  73 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Combat Automation Logging", keyName = "combatAutomationLogging", description = "Enable logging for combat automation (attack selections, movements, state transitions)", position = 21)
/*     */   default boolean combatAutomationLogging() {
/*  84 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "AI Prayers Logging", keyName = "aiPrayersLogging", description = "Enable logging for AI prayers (prayer selections, activations, redemption)", position = 22)
/*     */   default boolean aiPrayersLogging() {
/*  95 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Handler Actions Logging", keyName = "logHandlerActions", description = "Enable logging for handler actions (state-specific combat decisions, eating, drinking)", position = 23)
/*     */   default boolean logHandlerActions() {
/* 106 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Delay Detection Logging", keyName = "logDelayDetection", description = "Enable logging for delay detection flags (when attack timer is ready but blocked)", position = 24)
/*     */   default boolean logDelayDetection() {
/* 117 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Damage Tracker Logging", keyName = "damageTrackerLogging", description = "Enable logging for damage tracker (damage dealt to opponents)", position = 25)
/*     */   default boolean damageTrackerLogging() {
/* 128 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Auto Accept Duel Logging", keyName = "autoAcceptDuelLogging", description = "Enable logging for auto accept duel feature", position = 26)
/*     */   default boolean autoAcceptDuelLogging() {
/* 139 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Debug HP Check", keyName = "debugHpCheck", description = "Enable HP debugging - displays HP using multiple methods in console (runs outside automation)", position = 27)
/*     */   default boolean debugHpCheck() {
/* 150 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Target System Logging", keyName = "targetSystemLogging", description = "Enable logging for target system (target detection and caching)", position = 27)
/*     */   default boolean targetSystemLogging() {
/* 161 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "AI Chatbot", keyName = "aiChatbot", description = "Enable AI chatbot that responds to target messages during combat with sarcastic replies", position = 28)
/*     */   default boolean aiChatbot() {
/* 172 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "AI Chatbot API Key", keyName = "aiChatbotApiKey", description = "Groq API key for AI chatbot (get from https://console.groq.com/keys). Required for moonshot model.", secret = true, position = 29)
/*     */   default String aiChatbotApiKey() {
/* 184 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "AI Chatbot Model", keyName = "aiChatbotModel", description = "Model ID. Groq (recommended): 'moonshotai/kimi-k2-instruct-0905' (best for conversations). Together AI: 'openai/gpt-oss-120b', 'meta-llama/Llama-3-8b-chat-hf'. Groq alternatives: 'llama-3.3-70b-versatile', 'llama-3.1-70b-versatile', 'llama-3.1-8b-instant', 'mixtral-8x7b-32768'.", position = 30)
/*     */   default String aiChatbotModel() {
/* 195 */     return "moonshotai/kimi-k2-instruct-0905";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "AI Chatbot Provider", keyName = "aiChatbotProvider", description = "Provider for chat completions (optional). Examples: 'fireworks-ai', 'anthropic', 'openai'. Leave empty for default Hugging Face models.", position = 31)
/*     */   default String aiChatbotProvider() {
/* 206 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "AI Chatbot API Type", keyName = "aiChatbotApiType", description = "API service to use: 'groq' (recommended for moonshot model), 'together' (for gpt-oss-120b), or 'huggingface' (deprecated)", position = 32)
/*     */   default String aiChatbotApiType() {
/* 217 */     return "groq";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "AI Chatbot System Prompt", keyName = "aiChatbotSystemPrompt", description = "Custom system prompt to control AI behavior. Leave empty to use default natural behavior. Example: 'You are a player in OSRS. Keep responses short and natural, maximum 35 characters.'", position = 33)
/*     */   default String aiChatbotSystemPrompt() {
/* 228 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Combat-Based Responses", keyName = "combatBasedResponses", description = "Enable AI responses triggered by combat events (failed freezes, special attacks, etc.)", position = 34)
/*     */   default boolean combatBasedResponses() {
/* 239 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Combat Response Logging", keyName = "combatResponseLogging", description = "Enable logging for combat-based AI responses", position = 35)
/*     */   default boolean combatResponseLogging() {
/* 250 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Discord Webhooks", keyName = "discordWebhooks", description = "Enable Discord webhooks for chat messages from you and your target during combat (varbit == 1)", position = 36)
/*     */   default boolean discordWebhooks() {
/* 261 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Discord Webhook URL", keyName = "discordWebhookUrl", description = "Discord webhook URL to send chat messages to (get from Discord server settings > Integrations > Webhooks)", secret = true, position = 37)
/*     */   default String discordWebhookUrl() {
/* 273 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "578Lobby", keyName = "lobby578Webhooks", description = "Enable Discord webhooks for ALL chat messages when outside combat (varbit != 1)", position = 38)
/*     */   default boolean lobby578Webhooks() {
/* 284 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "578Lobby Webhook URL", keyName = "lobby578WebhookUrl", description = "Discord webhook URL for lobby messages (get from Discord server settings > Integrations > Webhooks)", secret = true, position = 39)
/*     */   default String lobby578WebhookUrl() {
/* 296 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Discord Reply Callback URL", keyName = "discordReplyCallbackUrl", description = "HTTP endpoint URL where Discord bot will send replies (e.g., http://localhost:8080/discord-reply). Leave empty to disable reply buttons.", secret = false, position = 45)
/*     */   default String discordReplyCallbackUrl() {
/* 308 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Discord Reply Server Port", keyName = "discordReplyServerPort", description = "Port for the local HTTP server to receive Discord replies (default: 8080). Must match your callback URL.", position = 46)
/*     */   default int discordReplyServerPort() {
/* 319 */     return 8080;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Enable Discord Reply Server", keyName = "enableDiscordReplyServer", description = "Enable local HTTP server to receive Discord replies and send them as in-game chat messages", position = 47)
/*     */   default boolean enableDiscordReplyServer() {
/* 330 */     return true;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Discord Bot Token", keyName = "discordBotToken", description = "Discord bot token for handling reply button interactions (required for reply buttons to work). Get from Discord Developer Portal.", secret = true, position = 48)
/*     */   default String discordBotToken() {
/* 342 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Discord Application ID", keyName = "discordApplicationId", description = "Discord application ID (required for reply buttons). Get from Discord Developer Portal.", secret = false, position = 49)
/*     */   default String discordApplicationId() {
/* 354 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Use Bot API for Messages", keyName = "useBotApiForMessages", description = "Use Discord bot API instead of webhooks for messages (enables reply buttons). Requires bot to be running.", position = 50)
/*     */   default boolean useBotApiForMessages() {
/* 365 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Discord Bot API URL", keyName = "discordBotApiUrl", description = "URL of the Discord bot HTTP server for sending messages with buttons (e.g., http://localhost:8081). Leave empty to use webhooks without buttons.", secret = false, position = 51)
/*     */   default String discordBotApiUrl() {
/* 377 */     return "http://localhost:8081";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Discord Channel ID", keyName = "discordChannelId", description = "Discord channel ID where messages should be sent (required for bot API). Get by right-clicking channel > Copy ID (Developer Mode must be enabled).", secret = false, position = 52)
/*     */   default String discordChannelId() {
/* 389 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Gear Test", keyName = "gearTest", description = "Test gear switching: equip tank gear directly from inventory (items not in inventory are ignored)", position = 4)
/*     */   default boolean gearTest() {
/* 400 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Tank", keyName = "tank", description = "Equip tank gear and activate Augury prayer", position = 5)
/*     */   default boolean tank() {
/* 411 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Range Attack", keyName = "rangeAttack", description = "Equip ranged gear, activate Rigour, attack target, then switch to tank gear after crossbow animation", position = 6)
/*     */   default boolean rangeAttack() {
/* 422 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Magic Attack", keyName = "magicAttack", description = "Equip magic gear, cast Ice Barrage (or Ice Blitz if mage < 94), then switch to tank gear after cast", position = 7)
/*     */   default boolean magicAttack() {
/* 433 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Tank Mage Attack", keyName = "tankMageAttack", description = "Equip tank mage gear, cast Ice Barrage (or Ice Blitz if mage < 94), then switch to tank gear after cast", position = 8)
/*     */   default boolean tankMageAttack() {
/* 444 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Melee Attack", keyName = "meleeAttack", description = "Equip melee gear, attack target, then switch to tank gear after melee animation", position = 9)
/*     */   default boolean meleeAttack() {
/* 455 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Test: Crossbow Longrange", keyName = "testCrossbowLongrange", description = "TEST: Equip Dragon crossbow and set to longrange (works independently of automation)", position = 11)
/*     */   default boolean testCrossbowLongrange() {
/* 466 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Tank Gear", keyName = "tankGear", description = "Tank gear items (comma separated). Supports wildcards: Karil's leathertop* matches 'Karil's leathertop' and 'Karil's leathertop100'", position = 12)
/*     */   default String tankGear() {
/* 477 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Ranged Gear", keyName = "rangedGear", description = "Ranged gear items (comma separated). Supports wildcards: *defender* matches 'avernic defender' and 'dragon defender'", position = 11)
/*     */   default String rangedGear() {
/* 488 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Melee Gear", keyName = "meleeGear", description = "Melee gear items (comma separated). Supports wildcards: Karil's leathertop* matches 'Karil's leathertop' and 'Karil's leathertop100'", position = 12)
/*     */   default String meleeGear() {
/* 499 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Magic Gear", keyName = "magicGear", description = "Magic gear items (comma separated). Supports wildcards: *defender* matches 'avernic defender' and 'dragon defender'", position = 13)
/*     */   default String magicGear() {
/* 510 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Spec Loadout", keyName = "specLoadout", description = "Special attack loadout items (comma separated). Supports wildcards: Karil's leathertop* matches 'Karil's leathertop' and 'Karil's leathertop100'", position = 14)
/*     */   default String specLoadout() {
/* 521 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Tank Mage Gear", keyName = "tankMageGear", description = "Tank mage gear items (comma separated). Supports wildcards: *defender* matches 'avernic defender' and 'dragon defender'", position = 15)
/*     */   default String tankMageGear() {
/* 532 */     return "";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Debug Diagonal Detection", keyName = "debugDiagonal", description = "Debug: Check and log if target is diagonal (1 tile away but can't be meleed)", position = 16)
/*     */   default boolean debugDiagonal() {
/* 543 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Auto Accept Duel", keyName = "autoAcceptDuel", description = "Automatically accept duel requests in PvP arena", position = 17)
/*     */   default boolean autoAcceptDuel() {
/* 554 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Debug Opponent Weapons", keyName = "debugOpponentWeapons", description = "Track opponent weapon animations and log which array they should be added to", position = 19)
/*     */   default boolean debugOpponentWeapons() {
/* 565 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "DD Attack Chance", keyName = "ddAttackChance", description = "Chance (1-50%) for diagonal step when target frozen and timer at 2. Percentage per attack, not per tick.", position = 20)
/*     */   default int ddAttackChance() {
/* 576 */     return 30;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Fakie Chance", keyName = "fakieChance", description = "Chance (0-100%) to randomly equip Abyssal tentacle or Dragon crossbow when attack timer is 2 or 3 ticks. 0 = never, 100 = always.", position = 21)
/*     */   default int fakieChance() {
/* 587 */     return 0;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Special Move 1: AGS", keyName = "specialMove1Ags", description = "Chance (0-100%) for AGS special move. Triggers when tick timer is 3.", position = 22)
/*     */   default int specialMove1Ags() {
/* 598 */     return 0;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Walk-away", keyName = "specialMove2", description = "Chance (0-100%) per attack for walk-away special move. When target timer hits 1 or 0, walk out 2-4 tiles, then walk back under next tick. Only works when target frozen and we unfrozen.", position = 23)
/*     */   default int specialMove2() {
/* 609 */     return 0;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Special Move 3", keyName = "specialMove3", description = "Chance (0-100%) for special move 3. Triggers when tick timer is 3.", position = 24)
/*     */   default int specialMove3() {
/* 620 */     return 0;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Free Hit", keyName = "freeHit", description = "Enable free hit special move. Detects free hit opportunities based on tick timer differences (Off PID: 2+ ticks ahead, On PID: 1+ tick ahead). Takes priority over eating and other actions.", position = 25)
/*     */   default boolean freeHit() {
/* 631 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Rackson", keyName = "rackson", description = "Chance (0-100%) for Rackson special move. Advanced movement pattern: loops between attacking and moving 2 tiles away. Only works when target frozen and we unfrozen. Triggers once per state entry.", position = 26)
/*     */   default int rackson() {
/* 642 */     return 0;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Attack Context Prediction", keyName = "attackContextPrediction", description = "Enable contextual attack prediction based on prayer, HP, weapon, spec, freeze state", position = 39)
/*     */   default boolean attackContextPrediction() {
/* 653 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Predictions Activate Prayers", keyName = "predictionsActivatePrayers", description = "Allow contextual predictions to activate prayers (overrides AI prayers when quality score >= threshold)", position = 40)
/*     */   default boolean predictionsActivatePrayers() {
/* 664 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Prediction Overlay", keyName = "predictionOverlay", description = "Show overlay displaying top 5 attack predictions with quality scores", position = 41)
/*     */   default boolean predictionOverlay() {
/* 675 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @ConfigItem(name = "Prediction Min Quality", keyName = "predictionMinQuality", description = "Minimum quality score (0-100%) required to use prediction for prayer switching", position = 42)
/*     */   default int predictionMinQuality() {
/* 686 */     return 60;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
    @ConfigItem(name = "Prediction History Size", keyName = "predictionHistorySize", description = "How many attacks to remember per target (10-5000)", position = 43)
    default int predictionHistorySize() {
        return 5000;
    }
}