package com.tonic.plugins.noidbets;

import com.tonic.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

class NoidBetsDiscordService
{
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10L);

    private final HttpClient httpClient;
    private final String apiUrl;

    NoidBetsDiscordService(String apiUrl)
    {
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    boolean isConfigured()
    {
        return apiUrl != null && !apiUrl.trim().isEmpty();
    }

    boolean verifyLinkCode(String code, String rsn)
    {
        if (!isConfigured())
        {
            return false;
        }

        if (code == null || code.trim().isEmpty() || rsn == null || rsn.trim().isEmpty())
        {
            return false;
        }

        String json = "{" +
                "\"code\":\"" + escapeJson(code.trim()) + "\"," +
                "\"rsn\":\"" + escapeJson(rsn.trim()) + "\"" +
                "}";

        return postJson("/api/link/verify", json, "[Noid Bets] Link verify");
    }

    boolean sendRoundCreated(String roundId, String player1, String player2)
    {
        if (!isConfigured())
        {
            return false;
        }

        if (roundId == null || roundId.trim().isEmpty())
        {
            return false;
        }

        String json = "{" +
                "\"roundId\":\"" + escapeJson(roundId.trim()) + "\"," +
                "\"player1\":\"" + escapeJson(player1 != null ? player1 : "") + "\"," +
                "\"player2\":\"" + escapeJson(player2 != null ? player2 : "") + "\"" +
                "}";

        return postJson("/api/duel/round-created", json, "[Noid Bets] Round created");
    }

    boolean sendRoundResult(String roundId, String winner, int p1Damage, int p2Damage, long durationMs)
    {
        if (!isConfigured())
        {
            return false;
        }

        if (roundId == null || roundId.trim().isEmpty() || winner == null || winner.trim().isEmpty())
        {
            return false;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"roundId\":\"").append(escapeJson(roundId.trim())).append("\",");
        sb.append("\"winner\":\"").append(escapeJson(winner.trim())).append("\",");
        sb.append("\"stats\":{");
        sb.append("\"p1Damage\":").append(Math.max(0, p1Damage)).append(",");
        sb.append("\"p2Damage\":").append(Math.max(0, p2Damage)).append(",");
        sb.append("\"durationMs\":").append(Math.max(0L, durationMs));
        sb.append("}}");
        String json = sb.toString();

        return postJson("/api/duel/round-result", json, "[Noid Bets] Round result");
    }

    boolean bankDeposit(String rsn, int amount)
    {
        if (!isConfigured())
        {
            return false;
        }

        if (rsn == null || rsn.trim().isEmpty() || amount <= 0)
        {
            return false;
        }

        String json = "{" +
                "\"rsn\":\"" + escapeJson(rsn.trim()) + "\"," +
                "\"amount\":" + Math.max(0, amount) +
                "}";

        return postJson("/api/bank/deposit", json, "[Noid Bets] Bank deposit");
    }

    boolean bankWithdraw(String rsn, int amount)
    {
        if (!isConfigured())
        {
            return false;
        }

        if (rsn == null || rsn.trim().isEmpty() || amount <= 0)
        {
            return false;
        }

        String json = "{" +
                "\"rsn\":\"" + escapeJson(rsn.trim()) + "\"," +
                "\"amount\":" + Math.max(0, amount) +
                "}";

        return postJson("/api/bank/withdraw", json, "[Noid Bets] Bank withdraw");
    }

    /**
     * Send a Discord DM notification to a user by their linked RSN.
     * Returns true if notification was sent successfully, false otherwise.
     */
    public boolean notifyUser(String rsn, String message)
    {
        if (!isConfigured()) return false;
        try
        {
            String escapedMsg = message.replace("\\", "\\\\").replace("\"", "\\\"");
            String json = String.format("{\"rsn\":\"%s\",\"message\":\"%s\"}", rsn, escapedMsg);
            return postJson("/api/bank/notify", json, "[Noid Bets] Notify user");
        }
        catch (Exception e)
        {
            Logger.norm("[Noid Bets] Notify user error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check user's balance by their linked RSN.
     * Returns balance amount, or -1 if error/not found.
     */
    public long checkBalance(String rsn)
    {
        if (!isConfigured())
        {
            Logger.norm("[Noid Bets] Check balance: Service not configured");
            return -1;
        }
        try
        {
            String json = String.format("{\"rsn\":\"%s\"}", escapeJson(rsn.trim()));
            Logger.norm("[Noid Bets] Checking balance for RSN: " + rsn);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/api/bank/check-balance"))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "VitaLite-Noid-Bets/1.0")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();
            
            Logger.norm("[Noid Bets] Balance check response: status=" + status + ", body=" + body);
            
            if (status >= 200 && status < 300)
            {
                if (body != null && body.contains("\"balance\":"))
                {
                    // Parse balance from JSON (simple extraction)
                    int balanceIdx = body.indexOf("\"balance\":");
                    if (balanceIdx >= 0)
                    {
                        String afterBalance = body.substring(balanceIdx + 10).trim();
                        int endIdx = afterBalance.indexOf(",");
                        if (endIdx < 0) endIdx = afterBalance.indexOf("}");
                        if (endIdx > 0)
                        {
                            String balanceStr = afterBalance.substring(0, endIdx).trim();
                            long balance = Long.parseLong(balanceStr);
                            Logger.norm("[Noid Bets] Parsed balance: " + balance + " GP");
                            return balance;
                        }
                    }
                }
                Logger.norm("[Noid Bets] Could not parse balance from response body");
            }
            else
            {
                Logger.norm("[Noid Bets] Balance check failed with status " + status);
            }
            return -1;
        }
        catch (Exception e)
        {
            Logger.norm("[Noid Bets] Check balance exception: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    private boolean postJson(String path, String json, String context)
    {
        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + path))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "VitaLite-Noid-Bets/1.0")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300)
            {
                return true;
            }

            String body = response.body();
            Logger.norm(context + " failed - status " + status + (body != null ? (", response: " + body) : ""));
            return false;
        }
        catch (IOException | InterruptedException e)
        {
            Logger.norm(context + " error: " + e.getMessage());
            return false;
        }
    }

    private String escapeJson(String input)
    {
        if (input == null)
        {
            return "";
        }

        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
