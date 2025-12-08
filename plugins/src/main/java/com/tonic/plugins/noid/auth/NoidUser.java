package com.tonic.plugins.noid.auth;

import lombok.Data;

import java.time.Instant;

/**
 * Represents an authenticated Noid user
 */
@Data
public class NoidUser {
    private String discordId;
    private String discordName;
    private String tier;
    private Instant expiresAt;
    private String sessionToken;
    private boolean hasSubscription = true;

    public boolean hasActiveSubscription() {
        if (!hasSubscription) {
            return false;
        }
        if (expiresAt == null) {
            return true; // Lifetime
        }
        return Instant.now().isBefore(expiresAt);
    }
}
