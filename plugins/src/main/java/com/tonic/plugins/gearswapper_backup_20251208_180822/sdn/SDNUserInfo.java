package com.tonic.plugins.gearswapper.sdn;

/**
 * Simple data holder for user authentication info.
 * This avoids direct dependency on NoidUser class which is in a separate plugin
 * JAR.
 */
public class SDNUserInfo {
    private final String sessionToken;
    private final String discordId;
    private final String discordName;

    public SDNUserInfo(String sessionToken, String discordId, String discordName) {
        this.sessionToken = sessionToken;
        this.discordId = discordId;
        this.discordName = discordName;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getDiscordName() {
        return discordName;
    }
}
