package com.tonic.plugins.noidbets;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.TradeAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.GameManager;
import com.tonic.util.MessageUtil;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
        name = "Noid Bets (Bank Bot)",
        description = "Automated duel arena reporting and Discord betting integration.",
        tags = {"noid", "bets", "duel", "discord"}
)
public class NoidBetsPlugin extends Plugin
{
    @Inject
    private NoidBetsConfig config;

    @Inject
    private Client client;

    private NoidBetsDiscordService discordService;
    private String lastApiUrl;

    // Simple automation state
    private static final int DUEL_VARBIT = 8121;
    // ~30 seconds at 0.6s per tick ~= 50 ticks
    private static final int HOME_WAIT_TICKS = 50;

    private int homeWaitTicks = 0;
    private boolean homeWaitLogged = false;
    private boolean challengeSentThisCycle = false;
    private boolean awaitingDuelConfirm = false;

    // Duel confirmation timing state (in game ticks)
    private int duelConfirmStage = 0; // 0 = idle, 1 = after Max/Med, 2 = after first confirm
    private int duelConfirmTicks = 0;

    // In-arena combat loop
    private int arenaAttackTicks = 0;

    // Arena exit flags
    private boolean shouldClickPortal;
    private boolean shouldClickGateway;
    private int gatewayClickAttempts;
    private static final int MAX_GATEWAY_CLICK_ATTEMPTS = 50;

    // Tick counter used for throttling arena-exit clicks
    private int exitTickCounter;

    private boolean firstHitsplatSeen;
    private boolean winnerKnown;

    private String currentRoundId;
    private long roundStartMillis;

    // Bank/deposit mode trade tracking
    private boolean lastTradeOpen = false;
    private int lastTradeCoins = 0;
    private String lastTradePartnerRsn = null;
    private boolean withdrawalProcessed = false;
    private int withdrawalAmount = 0;
    private boolean tradeAcceptedMessageSeen = false;

    @Provides
    NoidBetsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NoidBetsConfig.class);
    }

    private void sendRoundResultToDiscord(String winnerLabel)
    {
        if (discordService == null || !discordService.isConfigured())
        {
            return;
        }

        if (currentRoundId == null)
        {
            return;
        }

        String roundId = currentRoundId;
        currentRoundId = null;

        long durationMs;
        if (roundStartMillis > 0L)
        {
            durationMs = System.currentTimeMillis() - roundStartMillis;
        }
        else
        {
            durationMs = 0L;
        }
        roundStartMillis = 0L;

        int p1Damage = 0;
        int p2Damage = 0;

        new Thread(() -> {
            try
            {
                boolean ok = discordService.sendRoundResult(roundId, winnerLabel, p1Damage, p2Damage, durationMs);
                if (!ok)
                {
                    Logger.norm("[Noid Bets] Failed to send round-result for round " + roundId + ".");
                }
            }
            catch (Exception e)
            {
                Logger.norm("[Noid Bets] Error sending round-result: " + e.getMessage());
            }
        }).start();
    }

    private int getInventoryCoins()
    {
        ItemEx coins = InventoryAPI.getItem("Coins");
        return coins != null ? coins.getQuantity() : 0;
    }

    private String getCurrentTradePartnerName()
    {
        Widget title = WidgetAPI.get(InterfaceID.Trademain.TITLE);
        if (title == null)
        {
            return null;
        }

        String raw = title.getText();
        if (raw == null)
        {
            return null;
        }

        // Strip any tags and try to extract the RSN after "Trading with".
        String plain = raw.replaceAll("<[^>]+>", "").trim();
        String lower = plain.toLowerCase();
        int idx = lower.indexOf("trading with");
        if (idx == -1)
        {
            return null;
        }

        String rest = plain.substring(idx + "trading with".length()).trim();
        if (rest.startsWith(":"))
        {
            rest = rest.substring(1).trim();
        }

        return rest.isEmpty() ? null : rest;
    }

    @Override
    protected void startUp() throws Exception
    {
        initDiscordService();
        Logger.norm("[Noid Bets] Plugin started (bank-mode build v1)");
    }

    @Override
    protected void shutDown() throws Exception
    {
        discordService = null;
        lastApiUrl = null;
        Logger.norm("[Noid Bets] Plugin stopped");
    }

    private void initDiscordService()
    {
        String apiUrl = config.discordBotApiUrl();
        if (apiUrl != null && !apiUrl.trim().isEmpty() && (discordService == null || !apiUrl.equals(lastApiUrl)))
        {
            discordService = new NoidBetsDiscordService(apiUrl.trim());
            lastApiUrl = apiUrl.trim();
        }
    }

    private WorldPoint getHomeTile()
    {
        // Host stands at (3366, 3274, 0), non-host at (3367, 3274, 0)
        boolean host = config.hostReporter();
        int x = host ? 3366 : 3367;
        int y = 3274;
        int plane = 0;
        return new WorldPoint(x, y, plane);
    }

    private String normalizeName(String name)
    {
        if (name == null)
        {
            return "";
        }

        String normalized = name.replaceAll("<[^>]+>", "");
        normalized = normalized.replace('\u00A0', ' ');
        normalized = normalized.replace('\u2007', ' ');
        normalized = normalized.replace('\u202F', ' ');
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private boolean namesMatch(String a, String b)
    {
        if (a == null || b == null)
        {
            return false;
        }

        String n1 = normalizeName(a);
        String n2 = normalizeName(b);

        if (n1.equalsIgnoreCase(n2))
        {
            return true;
        }

        String ns1 = n1.replaceAll("\\s+", "");
        String ns2 = n2.replaceAll("\\s+", "");
        return !ns1.isEmpty() && ns1.equalsIgnoreCase(ns2);
    }

    private void trySendChallengeToTarget(Player localPlayer)
    {
        String targetRsn = config.targetRsn();
        if (targetRsn == null || targetRsn.trim().isEmpty())
        {
            return;
        }

        String expected = normalizeName(targetRsn);

        java.util.List<PlayerEx> players = GameManager.playerList();
        PlayerEx localEx = PlayerAPI.getLocal();
        if (players == null || localEx == null)
        {
            return;
        }

        PlayerEx bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        WorldPoint ourPos = localPlayer.getWorldLocation();

        for (PlayerEx pEx : players)
        {
            Player p = pEx.getPlayer();
            if (p == null || p.getName() == null)
            {
                continue;
            }

            if (pEx.getId() == localEx.getId())
            {
                continue;
            }

            String candidateName = Static.invoke(p::getName);
            if (!namesMatch(candidateName, expected))
            {
                continue;
            }

            WorldPoint pos = Static.invoke(p::getWorldLocation);
            if (ourPos != null && pos != null)
            {
                int dist = Math.abs(pos.getX() - ourPos.getX()) + Math.abs(pos.getY() - ourPos.getY());
                if (dist < bestDistance)
                {
                    bestDistance = dist;
                    bestMatch = pEx;
                }
            }
            else if (bestMatch == null)
            {
                bestMatch = pEx;
            }
        }

        if (bestMatch != null)
        {
            Logger.norm("[Noid Bets] Sending duel challenge to target '" + expected + "'");
            PlayerAPI.interact(bestMatch, new String[]{"Challenge"});
            challengeSentThisCycle = true;
        }
        else
        {
            Logger.norm("[Noid Bets] Could not find target '" + expected + "' nearby to challenge.");
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        String message = event.getMessage();
        
        // Detect "Accepted trade." message to confirm trade completion
        if (config.depositWithdrawMode() && message != null && message.contains("Accepted trade."))
        {
            tradeAcceptedMessageSeen = true;
            Logger.norm("[NoidBets Bank] ✓ Game message: 'Accepted trade.' received - trade completed successfully");
        }

        if (!config.enabled())
        {
            return;
        }

        initDiscordService();

        if (discordService == null || !discordService.isConfigured())
        {
            return;
        }
        if (message == null)
        {
            return;
        }

        // Non-host: detect duel invitations and mark that we should
        // handle the duel confirmation screens automatically.
        if (!config.hostReporter())
        {
            String lower = message.toLowerCase();
            boolean isDuelInvitation = lower.contains("inviting you to a unranked duel")
                    || lower.contains("inviting you to an unranked duel")
                    || lower.contains("challenges you to a duel")
                    || lower.contains("challenges you to an unranked duel")
                    || lower.contains("sent you a duel challenge")
                    || lower.contains("challenged you");

            if (isDuelInvitation)
            {
                awaitingDuelConfirm = true;
                Logger.norm("[Noid Bets] Detected duel invitation, will auto-accept.");
            }
        }

        String lowerMessage = message.toLowerCase();

        // Death message means we lost -> always use Portal only. This must
        // override any previous tentative winner detection (e.g. from
        // "unranked duel wins" seen before the death line), so it is handled
        // unconditionally first.
        if (lowerMessage.contains("oh dear") && lowerMessage.contains("you are dead"))
        {
            Player local = Static.invoke(client::getLocalPlayer);
            String ourName = local != null ? Static.invoke(local::getName) : "Unknown";
            String opponent = config.targetRsn();
            Logger.norm("[Noid Bets] Duel result: " + opponent + " defeated " + ourName + " (we died).");

            shouldClickPortal = true;
            shouldClickGateway = false;
            winnerKnown = true;

            if (config.hostReporter())
            {
                // Host is red, opponent is blue.
                sendRoundResultToDiscord("blue");
            }
        }
        else if (!winnerKnown)
        {
            // Only "unranked duel wins" is treated as a win; plain "you can leave"
            // can also appear for the loser and must not make us click Gateway.
            if (lowerMessage.contains("unranked duel wins"))
            {
                Player local = Static.invoke(client::getLocalPlayer);
                String ourName = local != null ? Static.invoke(local::getName) : "Unknown";
                Logger.norm("[Noid Bets] Duel result: we (" + ourName + ") won the duel.");

                shouldClickGateway = true;
                shouldClickPortal = false;
                gatewayClickAttempts = 0;
                winnerKnown = true;

                if (config.hostReporter())
                {
                    // Host is red, opponent is blue.
                    sendRoundResultToDiscord("red");
                }
            }
        }

        // Bank bot: when someone sends us a trade request ("<name> wishes to trade with you"),
        // initiate a trade back to them so the trade window opens automatically.
        if (config.depositWithdrawMode())
        {
            try
            {
                String plain = message.replaceAll("<[^>]+>", "").trim();
                String lowerPlain = plain.toLowerCase();
                String pattern = "wishes to trade with you";
                int idx = lowerPlain.indexOf(pattern);
                if (idx > 0)
                {
                    String namePart = plain.substring(0, idx).trim();
                    String expected = normalizeName(namePart);

                    java.util.List<PlayerEx> players = GameManager.playerList();
                    PlayerEx localEx = PlayerAPI.getLocal();
                    if (players != null && localEx != null && !expected.isEmpty())
                    {
                        PlayerEx bestMatch = null;
                        int bestDistance = Integer.MAX_VALUE;

                        Player local = Static.invoke(client::getLocalPlayer);
                        WorldPoint ourPos = local != null ? Static.invoke(local::getWorldLocation) : null;

                        for (PlayerEx pEx : players)
                        {
                            Player p = pEx.getPlayer();
                            if (p == null || p.getName() == null)
                            {
                                continue;
                            }

                            if (pEx.getId() == localEx.getId())
                            {
                                continue;
                            }

                            String candidateName = Static.invoke(p::getName);
                            if (!namesMatch(candidateName, expected))
                            {
                                continue;
                            }

                            WorldPoint pos = Static.invoke(p::getWorldLocation);
                            if (ourPos != null && pos != null)
                            {
                                int dist = Math.abs(pos.getX() - ourPos.getX()) + Math.abs(pos.getY() - ourPos.getY());
                                if (dist < bestDistance)
                                {
                                    bestDistance = dist;
                                    bestMatch = pEx;
                                }
                            }
                            else if (bestMatch == null)
                            {
                                bestMatch = pEx;
                            }
                        }

                        if (bestMatch != null)
                        {
                            Logger.norm("[Noid Bets] Detected trade request from '" + expected + "', initiating trade.");
                            PlayerAPI.interact(bestMatch, new String[]{"Trade"});
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Logger.norm("[Noid Bets] Error while handling trade request message: " + e.getMessage());
            }

            // Bank bot: if we're in a trade and receive a chat message from the trade partner
            if (TradeAPI.isOpen() && TradeAPI.isOnMainScreen())
            {
                String chatMsg = event.getMessage();
                if (chatMsg != null)
                {
                    String lower = chatMsg.toLowerCase().trim();
                    
                    // Check for balance inquiry
                    if (lower.equals("bal") || lower.equals("balance"))
                    {
                        handleBalanceInquiry(event);
                        return;
                    }
                }
                
                // Check for withdrawal request if not already processed
                if (!withdrawalProcessed)
                {
                    handleWithdrawalRequest(event);
                }
            }
        }

        String trimmed = message.trim();
        // Only consider link codes when we are NOT inside the duel arena instance.
        int arenaVar = VarAPI.getVar(DUEL_VARBIT);
        if (arenaVar == 1)
        {
            return;
        }

        // Accept pure numeric codes, e.g. "432422" from chat (public or private),
        // but ignore very short numbers like 1-3 which are often used as countdowns.
        if (!trimmed.matches("\\d+"))
        {
            return;
        }

        if (trimmed.length() < 4)
        {
            return;
        }

        String code = trimmed;
        if (code.isEmpty())
        {
            return;
        }

        String rsn = event.getName();
        if (rsn == null || rsn.trim().isEmpty())
        {
            return;
        }

        Logger.norm("[Noid Bets] Detected !link from '" + rsn + "' with code '" + code + "'");

        new Thread(() -> {
            try
            {
                boolean ok = discordService.verifyLinkCode(code, rsn);
                if (ok)
                {
                    Logger.norm("[Noid Bets] Linked RSN '" + rsn + "' using code '" + code + "'");
                }
                else
                {
                    Logger.norm("[Noid Bets] Failed to link RSN '" + rsn + "' using code '" + code + "'");
                }
            }
            catch (Exception e)
            {
                Logger.norm("[Noid Bets] Error verifying link code: " + e.getMessage());
            }
        }).start();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!config.enabled())
        {
            return;
        }
        // In bank mode, only run trade/bank logic and skip all duel arena
        // automation (movement, portal/gateway clicks, challenges, etc.).
        if (config.depositWithdrawMode())
        {
            handleBankTradeDeposit();
            return;
        }

        // Basic arena state: varbit 8121 == 1 means we are inside the duel arena instance.
        int arenaVar = VarAPI.getVar(DUEL_VARBIT);

        Player localPlayer = Static.invoke(client::getLocalPlayer);
        if (localPlayer == null)
        {
            return;
        }

        boolean isHost = config.hostReporter();

        WorldPoint current = localPlayer.getWorldLocation();

        // Compute current region and arena membership similar to AttackTimer.
        int currentRegionId = -1;
        boolean inArenaRegion = false;
        if (current != null)
        {
            currentRegionId = ((current.getX() >> 6) << 8) | (current.getY() >> 6);
            inArenaRegion = (currentRegionId == 13363 || currentRegionId == 13362);
        }

        // When fully out of arena context, reset duel-result / hitsplat state.
        if (arenaVar == 0)
        {
            firstHitsplatSeen = false;
            winnerKnown = false;
        }

        if (shouldClickPortal || shouldClickGateway)
        {
            exitTickCounter++;
        }
        else
        {
            exitTickCounter = 0;
        }

        WorldPoint homeTile = getHomeTile();

        if (arenaVar != 1)
        {
            // If we are back in the arena region (e.g. lobby / home tiles) and
            // no longer in the duel instance (varbit != 1), stop Portal logic.
            // This must run before we short-circuit into handleArenaExit, otherwise
            // we would remain stuck trying to exit even after returning to the lobby.
            if (shouldClickPortal && arenaVar != 1 && inArenaRegion)
            {
                Logger.norm("[Noid Bets] Back in arena/lobby region (" + currentRegionId + ") after death - stopping Portal clicks.");
                shouldClickPortal = false;
                exitTickCounter = 0;
            }

            // If we still have exit flags set (e.g. Gateway for the winner, or
            // Portal while outside the arena region), handle exiting before
            // returning home.
            if (shouldClickPortal || shouldClickGateway)
            {
                handleArenaExit(arenaVar, inArenaRegion, currentRegionId);
                return;
            }

            // Not in arena: ensure we are at our designated home tile and then wait there.
            if (current == null || !current.equals(homeTile))
            {
                // Walk back to home tile and reset wait timer.
                MovementAPI.walkToWorldPoint(homeTile);
                homeWaitTicks = 0;
                homeWaitLogged = false;
                challengeSentThisCycle = false;
                return;
            }

            // Already at home tile: increment wait timer.
            homeWaitTicks++;

            if (!homeWaitLogged && homeWaitTicks == 1)
            {
                Logger.norm("[Noid Bets] Reached home tile " + homeTile + ", starting 30s wait.");
                homeWaitLogged = true;

                // Host: the moment we start the 30s wait at home tile is when
                // betting should open in Discord. Only open a new round if we
                // don't already have one in progress.
                if (isHost && currentRoundId == null && discordService != null && discordService.isConfigured())
                {
                    String hostName;
                    try
                    {
                        hostName = Static.invoke(localPlayer::getName);
                    }
                    catch (Exception e)
                    {
                        hostName = "Unknown";
                    }

                    String opponentName = config.targetRsn();
                    String roundId = Long.toString(System.currentTimeMillis());
                    currentRoundId = roundId;
                    roundStartMillis = System.currentTimeMillis();

                    String finalHostName = hostName;
                    String finalOpponentName = opponentName;
                    new Thread(() -> {
                        try
                        {
                            boolean ok = discordService.sendRoundCreated(roundId, finalHostName, finalOpponentName);
                            if (!ok)
                            {
                                Logger.norm("[Noid Bets] Failed to send round-created for round " + roundId + ".");
                            }
                        }
                        catch (Exception e)
                        {
                            Logger.norm("[Noid Bets] Error sending round-created: " + e.getMessage());
                        }
                    }).start();
                }
            }
            else if (homeWaitTicks >= HOME_WAIT_TICKS)
            {
                // We have been at home tile for ~30 seconds.
                // Only the hostReporter account should send the duel challenge.
                if (!challengeSentThisCycle)
                {
                    trySendChallengeToTarget(localPlayer);
                    challengeSentThisCycle = true;
                    awaitingDuelConfirm = true;
                    duelConfirmStage = 0;
                    duelConfirmTicks = 0;
                }
            }
        }
        else
        {
            // Inside arena; reset home wait timer. Combat is handled here. Once we
            // detect a winner, exit flags are triggered by chat messages and processed
            // both inside and outside the arena instance.
            homeWaitTicks = 0;
            challengeSentThisCycle = false;
            awaitingDuelConfirm = false;
            duelConfirmStage = 0;
            duelConfirmTicks = 0;
            arenaAttackTicks++;

            // If we already know the winner, start exit behaviour instead of attacking.
            if (shouldClickPortal || shouldClickGateway)
            {
                handleArenaExit(arenaVar, inArenaRegion, currentRegionId);
            }
            else
            {
                handleArenaCombat(localPlayer, arenaAttackTicks);
            }
        }

        // After we have sent/received a duel invitation, click through the
        // confirmation widgets to accept the duel with timed delays.
        if (awaitingDuelConfirm)
        {
            duelConfirmTicks++;
            handleDuelConfirmation();
        }
    }

    private void handleBankTradeDeposit()
    {
        if (!config.depositWithdrawMode())
        {
            lastTradeOpen = false;
            lastTradeCoins = 0;
            lastTradePartnerRsn = null;
            return;
        }

        boolean tradeOpen = TradeAPI.isOpen();
        int currentCoins = getInventoryCoins();

        // Trade just opened
        if (tradeOpen && !lastTradeOpen)
        {
            String partner = getCurrentTradePartnerName();
            lastTradeCoins = currentCoins;
            lastTradePartnerRsn = partner;
            withdrawalProcessed = false;
            withdrawalAmount = 0;
            tradeAcceptedMessageSeen = false;
            Logger.norm("[NoidBets Bank] Trade opened with '" + partner + "' | Starting coins: " + currentCoins);

            // CRITICAL: Verify RSN is linked BEFORE accepting any trade
            if (partner != null && !partner.isEmpty() && discordService != null && discordService.isConfigured())
            {
                final String rsn = partner;
                new Thread(() -> {
                    try
                    {
                        Logger.norm("[NoidBets Bank] Verifying RSN link for '" + rsn + "'...");
                        long balance = discordService.checkBalance(rsn);
                        
                        if (balance < 0)
                        {
                            Logger.norm("[NoidBets Bank] ✗ RSN NOT LINKED: '" + rsn + "' - DECLINING TRADE");
                            try
                            {
                                Thread.sleep(500); // Small delay to ensure trade screen is ready
                                TradeAPI.decline();
                                Logger.norm("[NoidBets Bank] ✗ Trade declined - RSN not linked");
                            }
                            catch (Exception e)
                            {
                                Logger.norm("[NoidBets Bank] Error declining trade: " + e.getMessage());
                            }
                            return;
                        }
                        
                        Logger.norm("[NoidBets Bank] ✓ RSN VERIFIED: '" + rsn + "' has balance " + balance + " GP");
                        
                        // Send DM notification that bot is in trade
                        boolean sent = discordService.notifyUser(rsn, "🤖 Bank bot confirmed: In trade with you now.");
                        if (sent)
                        {
                            Logger.norm("[NoidBets Bank] ✓ Sent trade-open DM to " + rsn);
                        }
                    }
                    catch (Exception e)
                    {
                        Logger.norm("[NoidBets Bank] Failed to verify RSN or send DM: " + e.getMessage());
                    }
                }).start();
            }
        }

        // Trade is open - log state every tick for debugging
        if (tradeOpen)
        {
            try
            {
                // Check what the other player is offering
                java.util.List<ItemEx> receiving = TradeAPI.getReceivingItems();
                int coinsOffered = 0;
                if (receiving != null)
                {
                    for (ItemEx it : receiving)
                    {
                        if (it != null && "Coins".equalsIgnoreCase(it.getName()))
                        {
                            coinsOffered = it.getQuantity();
                            break;
                        }
                    }
                }

                // Check what we're offering
                java.util.List<ItemEx> offering = TradeAPI.getOfferingItems();
                int coinsWeOffer = 0;
                if (offering != null)
                {
                    for (ItemEx it : offering)
                    {
                        if (it != null && "Coins".equalsIgnoreCase(it.getName()))
                        {
                            coinsWeOffer = it.getQuantity();
                            break;
                        }
                    }
                }

                boolean onMain = TradeAPI.isOnMainScreen();
                boolean onConfirm = TradeAPI.isOnConfirmationScreen();
                boolean weAccepted = TradeAPI.isAcceptedByPlayer();
                boolean theyAccepted = TradeAPI.isAcceptedByOther();

                // Auto-accept logic: accept when either receiving or sending coins
                boolean shouldAutoAccept = coinsOffered > 0 || coinsWeOffer > 0;
                
                if (shouldAutoAccept)
                {
                    if (onMain && theyAccepted && !weAccepted)
                    {
                        Logger.norm("[NoidBets Bank] Auto-accepting MAIN screen");
                        TradeAPI.accept();
                    }
                    else if (onConfirm && !weAccepted)
                    {
                        Logger.norm("[NoidBets Bank] Auto-accepting CONFIRM screen");
                        TradeAPI.accept();
                    }
                }
            }
            catch (Exception e)
            {
                Logger.norm("[NoidBets Bank] Error reading trade state: " + e.getMessage());
            }
        }
        // Trade just closed
        else if (lastTradeOpen)
        {
            int gained = currentCoins - lastTradeCoins;
            int lost = lastTradeCoins - currentCoins;
            Logger.norm("[NoidBets Bank] Trade closed | Before: " + lastTradeCoins + " | After: " + currentCoins
                    + " | Gained: " + gained + " GP | Lost: " + lost + " GP | Partner: '" + lastTradePartnerRsn + "'");

            // If this was a withdrawal, verify trade completion
            if (withdrawalProcessed && withdrawalAmount > 0)
            {
                final String rsn = lastTradePartnerRsn;
                final int amount = withdrawalAmount;
                
                // Verify: Trade must be accepted AND we must have lost the exact amount
                boolean tradeCompleted = tradeAcceptedMessageSeen && (lost == amount);
                
                if (tradeCompleted)
                {
                    Logger.norm("[NoidBets Bank] ✓ Withdrawal VERIFIED: Trade completed + GP lost matches withdrawal amount");
                    Logger.norm("[NoidBets Bank] ✓ Withdrawal COMPLETED: " + amount + " GP withdrawn by " + rsn);

                    if (discordService != null && discordService.isConfigured())
                    {
                        new Thread(() -> {
                            String formattedAmount = String.format("%,d", amount);
                            discordService.notifyUser(rsn, "✅ Withdrawal complete: **" + formattedAmount + " GP** withdrawn successfully.");
                            Logger.norm("[NoidBets Bank] ✓ Sent withdrawal-complete DM to " + rsn);
                        }).start();
                    }
                }
                else
                {
                    // Trade was declined or failed - REFUND the balance
                    Logger.norm("[NoidBets Bank] ⚠️ Withdrawal FAILED: Trade declined or GP mismatch");
                    Logger.norm("[NoidBets Bank] ⚠️ Trade accepted message: " + tradeAcceptedMessageSeen);
                    Logger.norm("[NoidBets Bank] ⚠️ GP lost: " + lost + " | Expected: " + amount);
                    Logger.norm("[NoidBets Bank] ⚠️ REFUNDING " + amount + " GP to " + rsn + "'s balance");

                    if (discordService != null && discordService.isConfigured())
                    {
                        new Thread(() -> {
                            try
                            {
                                // Refund by depositing back
                                boolean refunded = discordService.bankDeposit(rsn, amount);
                                if (refunded)
                                {
                                    Logger.norm("[NoidBets Bank] ✓ REFUND SUCCESS: " + amount + " GP returned to " + rsn);
                                    String formattedAmount = String.format("%,d", amount);
                                    discordService.notifyUser(rsn, "⚠️ Withdrawal cancelled: Trade was declined. **" + formattedAmount + " GP** has been refunded to your balance.");
                                }
                                else
                                {
                                    Logger.norm("[NoidBets Bank] ✗ REFUND FAILED: Could not return " + amount + " GP to " + rsn + " - ADMIN ACTION REQUIRED");
                                    discordService.notifyUser(rsn, "🚨 CRITICAL: Withdrawal failed but refund error. Contact admin immediately!");
                                }
                            }
                            catch (Exception e)
                            {
                                Logger.norm("[NoidBets Bank] ✗ REFUND EXCEPTION: " + e.getMessage());
                            }
                        }).start();
                    }
                }
            }
            // Otherwise, if we gained GP, it's a deposit
            else if (gained > 0 && lastTradePartnerRsn != null && !lastTradePartnerRsn.isEmpty())
            {
                final String rsn = lastTradePartnerRsn;
                final int amount = gained;

                if (discordService == null || !discordService.isConfigured())
                {
                    Logger.norm("[NoidBets Bank] ERROR: Discord service not configured - cannot record deposit!");
                }
                else
                {
                    Logger.norm("[NoidBets Bank] Recording deposit of " + amount + " GP for RSN: " + rsn);
                    new Thread(() -> {
                        try
                        {
                            boolean ok = discordService.bankDeposit(rsn, amount);
                            if (ok)
                            {
                                Logger.norm("[NoidBets Bank] ✓ Deposit SUCCESS: " + amount + " GP credited to " + rsn);

                                // Send success DM to user
                                String formattedAmount = String.format("%,d", amount);
                                String successMsg = "✅ Deposit successful: **" + formattedAmount + " GP** credited to your balance.";
                                boolean dmSent = discordService.notifyUser(rsn, successMsg);
                                if (dmSent)
                                {
                                    Logger.norm("[NoidBets Bank] ✓ Sent deposit-success DM to " + rsn);
                                }
                            }
                            else
                            {
                                Logger.norm("[NoidBets Bank] ✗ Deposit FAILED: Backend rejected deposit for " + rsn);
                            }
                        }
                        catch (Exception e)
                        {
                            Logger.norm("[NoidBets Bank] ✗ Deposit ERROR: " + e.getMessage());
                        }
                    }).start();
                }
            }
            else if (gained < 0)
            {
                Logger.norm("[NoidBets Bank] Coins decreased by " + Math.abs(gained) + " GP");
            }

            lastTradeCoins = 0;
            lastTradePartnerRsn = null;
            withdrawalProcessed = false;
            withdrawalAmount = 0;
            tradeAcceptedMessageSeen = false;
        }

        lastTradeOpen = tradeOpen;
    }

    /**
     * Handle balance inquiry from chat during trade
     */
    private void handleBalanceInquiry(ChatMessage event)
    {
        if (discordService == null || !discordService.isConfigured())
        {
            return;
        }

        String message = event.getMessage();
        if (message == null)
        {
            return;
        }

        // Get trade partner RSN
        String partnerRsn = getCurrentTradePartnerName();
        if (partnerRsn == null || partnerRsn.isEmpty())
        {
            return;
        }

        // Check if message is from trade partner
        String senderName = event.getName();
        if (senderName == null || !namesMatch(senderName, partnerRsn))
        {
            return;
        }

        Logger.norm("[NoidBets Bank] Balance inquiry from '" + partnerRsn + "'");

        final String rsn = partnerRsn;

        new Thread(() -> {
            try
            {
                long userBalance = discordService.checkBalance(rsn);
                
                if (userBalance < 0)
                {
                    Logger.norm("[NoidBets Bank] Balance check failed for " + rsn);
                    MessageUtil.sendPublicChatMessage("Could not find your balance. Make sure your RSN is linked.");
                    return;
                }
                
                String formattedBalance = String.format("%,d", userBalance);
                Logger.norm("[NoidBets Bank] Sending balance to " + rsn + ": " + userBalance + " GP");
                MessageUtil.sendPublicChatMessage("Your balance: " + formattedBalance + " GP");
            }
            catch (Exception e)
            {
                Logger.norm("[NoidBets Bank] Balance inquiry error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Parse GP amount from chat message (e.g., "5m", "10m", "500k", "1b")
     * Returns the amount in GP, or -1 if invalid
     */
    private int parseGpAmount(String text)
    {
        if (text == null || text.isEmpty())
        {
            return -1;
        }

        String cleaned = text.toLowerCase().trim();
        // Match patterns like "5m", "10m", "500k", "1b", "1.5m", etc.
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([kmb])");
        java.util.regex.Matcher matcher = pattern.matcher(cleaned);

        if (matcher.find())
        {
            try
            {
                double value = Double.parseDouble(matcher.group(1));
                String suffix = matcher.group(2);

                int multiplier = 1;
                if ("k".equals(suffix))
                {
                    multiplier = 1_000;
                }
                else if ("m".equals(suffix))
                {
                    multiplier = 1_000_000;
                }
                else if ("b".equals(suffix))
                {
                    multiplier = 1_000_000_000;
                }

                int amount = (int) (value * multiplier);
                return amount > 0 ? amount : -1;
            }
            catch (Exception e)
            {
                return -1;
            }
        }

        return -1;
    }

    /**
     * Handle withdrawal request from chat during trade.
     * This immediately deducts balance from backend, then offers GP in trade.
     */
    private void handleWithdrawalRequest(ChatMessage event)
    {
        if (discordService == null || !discordService.isConfigured())
        {
            return;
        }

        String message = event.getMessage();
        if (message == null)
        {
            return;
        }

        // Parse amount from message
        int amount = parseGpAmount(message);
        if (amount <= 0)
        {
            return;
        }

        // Get trade partner RSN
        String partnerRsn = getCurrentTradePartnerName();
        if (partnerRsn == null || partnerRsn.isEmpty())
        {
            return;
        }

        // Check if message is from trade partner
        String senderName = event.getName();
        if (senderName == null || !namesMatch(senderName, partnerRsn))
        {
            return;
        }

        Logger.norm("[NoidBets Bank] Withdrawal request from '" + partnerRsn + "' for " + amount + " GP");

        final String rsn = partnerRsn;
        final int requestedAmount = amount;

        new Thread(() -> {
            try
            {
                // Step 1: Check user's actual balance BEFORE deducting
                Logger.norm("[NoidBets Bank] ⏳ Step 1/5: Checking user balance...");
                long userBalance = discordService.checkBalance(rsn);
                
                Logger.norm("[NoidBets Bank] ✓ Balance check result: " + userBalance + " GP");
                
                if (userBalance < 0)
                {
                    Logger.norm("[NoidBets Bank] ✗ Balance check failed or RSN not linked (returned -1)");
                    MessageUtil.sendPublicChatMessage("Sorry, but your RSN is not linked. Please link first.");
                    return;
                }
                
                if (userBalance < requestedAmount)
                {
                    Logger.norm("[NoidBets Bank] ✗ Insufficient balance: Has " + userBalance + " GP, wants " + requestedAmount + " GP");
                    String formattedBalance = String.format("%,d", userBalance);
                    MessageUtil.sendPublicChatMessage("Sorry, but you only have " + formattedBalance + " GP.");
                    return;
                }
                
                Logger.norm("[NoidBets Bank] ✓ Step 1/5 COMPLETE: User has sufficient balance (" + userBalance + " GP)");

                // Step 2: Check bot inventory
                Logger.norm("[NoidBets Bank] ⏳ Step 2/5: Checking bot inventory for sufficient coins...");
                ItemEx coins = InventoryAPI.getItem("Coins");
                int botCoins = coins != null ? coins.getQuantity() : 0;
                
                if (botCoins < requestedAmount)
                {
                    Logger.norm("[NoidBets Bank] ✗ Bot inventory insufficient: Has " + botCoins + " GP, needs " + requestedAmount + " GP");
                    String formattedBotCoins = String.format("%,d", botCoins);
                    MessageUtil.sendPublicChatMessage("Sorry, we only have " + formattedBotCoins + " GP left. Open a ticket to refill.");
                    return;
                }
                
                Logger.norm("[NoidBets Bank] ✓ Step 2/5 COMPLETE: Bot has sufficient inventory (" + botCoins + " GP available)");

                // Step 3: CRITICAL - Immediately deduct balance to prevent double-spending
                Logger.norm("[NoidBets Bank] ⏳ Step 3/5: Deducting balance from backend...");
                boolean deducted = discordService.bankWithdraw(rsn, requestedAmount);
                
                if (!deducted)
                {
                    Logger.norm("[NoidBets Bank] ✗ WITHDRAWAL REJECTED: Backend declined balance deduction");
                    MessageUtil.sendPublicChatMessage("Withdrawal error. Please contact admin.");
                    return;
                }

                Logger.norm("[NoidBets Bank] ✓ Step 3/5 COMPLETE: Balance deducted successfully from backend");
                Logger.norm("[NoidBets Bank] ✓ CONFIRMED: " + requestedAmount + " GP removed from " + rsn + "'s balance");

                // Step 4: Offer GP in trade
                Logger.norm("[NoidBets Bank] ⏳ Step 4/5: Offering " + requestedAmount + " GP in trade window...");
                TradeAPI.offer("Coins", requestedAmount);
                Logger.norm("[NoidBets Bank] ✓ Step 4/5 COMPLETE: GP offered in trade window");
                Logger.norm("[NoidBets Bank] ⏳ Step 5/5: Waiting for trade completion...");

                // Mark withdrawal as processed
                withdrawalProcessed = true;
                withdrawalAmount = requestedAmount;

                // Send success DM
                String formattedAmount = String.format("%,d", requestedAmount);
                discordService.notifyUser(rsn, "💸 Withdrawal approved: **" + formattedAmount + " GP** deducted from balance. Please accept both trade screens.");

            }
            catch (Exception e)
            {
                Logger.norm("[NoidBets Bank] ✗ WITHDRAWAL EXCEPTION: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (!config.enabled())
        {
            return;
        }

        // Only care about hitsplats while inside the arena instance
        int arenaVar = VarAPI.getVar(DUEL_VARBIT);
        if (arenaVar != 1)
        {
            return;
        }

        if (firstHitsplatSeen)
        {
            return;
        }

        if (event == null || event.getHitsplat() == null || event.getHitsplat().getAmount() <= 0)
        {
            return;
        }

        if (!(event.getActor() instanceof Player))
        {
            return;
        }

        firstHitsplatSeen = true;
        Logger.norm("[Noid Bets] First hitsplat detected inside arena; stopping manual attack clicks.");
    }

    private void handleArenaExit(int currentVarbit, boolean inArenaRegion, int currentRegionId)
    {
        // Click Portal when we died ("Oh dear, you are dead!")
        if (shouldClickPortal)
        {
            // Align with AttackTimer: while we are outside the arena region,
            // click Portal every 3 ticks once varbit != 1. As soon as we
            // return to the arena/lobby region, stop clicking and allow the
            // normal home-tile logic to proceed.
            if (currentVarbit != 1 && !inArenaRegion && exitTickCounter % 3 == 0)
            {
                try
                {
                    var portal = TileObjectAPI.get("Portal");
                    if (portal != null)
                    {
                        Logger.norm("[Noid Bets] Clicking Portal (action index 0) to leave arena death area.");
                        TileObjectAPI.interact(portal, 0);
                    }
                }
                catch (Exception e)
                {
                    Logger.norm("[Noid Bets] Error while clicking Portal: " + e.getMessage());
                }
            }
        }

        // Click Gateway when we won / can leave.
        // This is mutually exclusive with the Portal path above: if
        // shouldClickPortal is true we always prioritise Portal and return.
        else if (shouldClickGateway && gatewayClickAttempts < MAX_GATEWAY_CLICK_ATTEMPTS)
        {
            // If we've fully left the arena context, stop clicking.
            if (currentVarbit == 0)
            {
                Logger.norm("[Noid Bets] Left arena instance (varbit=" + currentVarbit + ") - stopping Gateway clicks.");
                shouldClickGateway = false;
                gatewayClickAttempts = 0;
                return;
            }

            gatewayClickAttempts++;
            try
            {
                var gateway = TileObjectAPI.getContains("Gateway");
                if (gateway != null)
                {
                    Logger.norm("[Noid Bets] Clicking Gateway (Leave) to exit arena (attempt " + gatewayClickAttempts + "/" + MAX_GATEWAY_CLICK_ATTEMPTS + ").");
                    TileObjectAPI.interact(gateway, "Leave");
                }
            }
            catch (Exception e)
            {
                Logger.norm("[Noid Bets] Error while clicking Gateway: " + e.getMessage());
            }

            if (gatewayClickAttempts >= MAX_GATEWAY_CLICK_ATTEMPTS)
            {
                Logger.norm("[Noid Bets] Reached max Gateway click attempts, giving up.");
                shouldClickGateway = false;
                gatewayClickAttempts = 0;
            }
        }
    }

    private void handleArenaCombat(Player localPlayer, int ticksInArena)
    {
        String targetRsn = config.targetRsn();
        if (targetRsn == null || targetRsn.trim().isEmpty())
        {
            return;
        }

        String expected = normalizeName(targetRsn);

        java.util.List<PlayerEx> players = GameManager.playerList();
        PlayerEx localEx = PlayerAPI.getLocal();
        if (players == null || localEx == null)
        {
            return;
        }

        PlayerEx bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        WorldPoint ourPos = localPlayer.getWorldLocation();

        for (PlayerEx pEx : players)
        {
            Player p = pEx.getPlayer();
            if (p == null || p.getName() == null)
            {
                continue;
            }

            if (pEx.getId() == localEx.getId())
            {
                continue;
            }

            String candidateName = Static.invoke(p::getName);
            if (!namesMatch(candidateName, expected))
            {
                continue;
            }

            WorldPoint pos = Static.invoke(p::getWorldLocation);
            if (ourPos != null && pos != null)
            {
                int dist = Math.abs(pos.getX() - ourPos.getX()) + Math.abs(pos.getY() - ourPos.getY());
                if (dist < bestDistance)
                {
                    bestDistance = dist;
                    bestMatch = pEx;
                }
            }
            else if (bestMatch == null)
            {
                bestMatch = pEx;
            }
        }

        if (bestMatch == null)
        {
            return;
        }

        // If we already saw the first hitsplat or know the winner, stop manual
        // clicking and let combat play out / exit logic handle leaving.
        if (firstHitsplatSeen || winnerKnown)
        {
            return;
        }

        // Host should move toward the non-host before spamming clicks to ensure
        // they are in melee range as quickly as possible.
        boolean isHost = config.hostReporter();
        if (isHost)
        {
            WorldPoint targetPos = Static.invoke(bestMatch.getPlayer()::getWorldLocation);
            if (ourPos != null && targetPos != null)
            {
                int dist = Math.abs(targetPos.getX() - ourPos.getX()) + Math.abs(targetPos.getY() - ourPos.getY());
                if (dist > 1)
                {
                    MovementAPI.walkToWorldPoint(targetPos);
                    Logger.norm("[Noid Bets] Host moving toward target '" + expected + "' inside arena (distance=" + dist + ").");
                    return;
                }
            }
        }

        Logger.norm("[Noid Bets] Attacking target '" + expected + "' inside arena.");
        try
        {
            String[] actions = bestMatch.getActions();
            if (actions != null)
            {
                StringBuilder sb = new StringBuilder();
                int fightIndex = -1;
                for (int i = 0; i < actions.length; i++)
                {
                    if (actions[i] != null)
                    {
                        sb.append('[').append(i).append(':').append(actions[i]).append("] ");
                        if (fightIndex == -1 && actions[i].toLowerCase().contains("fight"))
                        {
                            fightIndex = i;
                        }
                    }
                }
                Logger.norm("[Noid Bets] Arena actions for target '" + expected + "': " + sb.toString());

                if (fightIndex != -1)
                {
                    Logger.norm("[Noid Bets] Using Fight option index " + fightIndex + " for target '" + expected + "'.");
                    PlayerAPI.interact(bestMatch, fightIndex);
                }
                else
                {
                    // Fallback to string-based interact if we somehow didn't find Fight
                    Logger.norm("[Noid Bets] No explicit 'Fight' option found, falling back to string-based interact.");
                    PlayerAPI.interact(bestMatch, "Fight");
                }
            }
            else
            {
                // No actions available; nothing to do
            }
        }
        catch (Exception e)
        {
            Logger.norm("[Noid Bets] Error while trying to attack target inside arena: " + e.getMessage());
        }
    }

    private void handleDuelConfirmation()
    {
        Static.invoke(() -> {
            // Stage 0: click Max/Med as soon as it's visible, then start 8s timer
            if (duelConfirmStage == 0)
            {
                Widget maxMedWidget = WidgetAPI.get(757, 10, 0);
                if (maxMedWidget != null && WidgetAPI.isVisible(maxMedWidget))
                {
                    String[] actions = maxMedWidget.getActions();
                    if (actions != null)
                    {
                        for (int i = 0; i < actions.length; i++)
                        {
                            if (actions[i] != null && !actions[i].isEmpty())
                            {
                                WidgetAPI.interact(maxMedWidget, i + 1);
                                duelConfirmStage = 1;
                                duelConfirmTicks = 0;
                                return;
                            }
                        }
                    }
                }
            }

            // Stage 1: after ~8 seconds (13 ticks), click first confirm
            if (duelConfirmStage == 1)
            {
                if (duelConfirmTicks < 13)
                {
                    return;
                }

                Widget firstConfirm = WidgetAPI.get(757, 22);
                if (firstConfirm != null && firstConfirm.getId() != -1)
                {
                    String[] actions = firstConfirm.getActions();
                    if (actions != null)
                    {
                        for (int i = 0; i < actions.length; i++)
                        {
                            if (actions[i] != null && !actions[i].isEmpty())
                            {
                                WidgetAPI.interact(firstConfirm, i + 1);
                                duelConfirmStage = 2;
                                duelConfirmTicks = 0;
                                return;
                            }
                        }
                    }
                }
            }

            // Stage 2: click second confirm periodically (every ~4 seconds) until duel starts
            if (duelConfirmStage == 2)
            {
                // Wait a few ticks between clicks to mimic human behaviour (~4s => ~7 ticks)
                if (duelConfirmTicks < 7)
                {
                    return;
                }

                // If we are already in the arena, stop trying to confirm.
                int arenaVar = VarAPI.getVar(DUEL_VARBIT);
                if (arenaVar == 1)
                {
                    awaitingDuelConfirm = false;
                    duelConfirmStage = 0;
                    duelConfirmTicks = 0;
                    return;
                }

                Widget secondConfirmCheck = WidgetAPI.get(757, 143);
                if (secondConfirmCheck != null && secondConfirmCheck.getId() != -1)
                {
                    // Mirror DuelChallengeHandler: send a raw packet click to the
                    // PvpArenaUnrankedduel.Opponent Confirm interface (widgetId 49610895).
                    WidgetAPI.interact(1, 49610895, -1, -1);
                    duelConfirmTicks = 0;
                    return;
                }
            }
        });
    }

    // TODO: Add duel detection, automated challenge/accept, fighting, and Discord reporting
}
