const axios = require('axios');

const api = axios.create({
    baseURL: process.env.API_URL,
    headers: {
        'Content-Type': 'application/json',
        'x-api-key': process.env.API_KEY
    }
});

/**
 * Get user info by Discord ID
 */
async function getUser(discordId) {
    try {
        const response = await api.get(`/users/${discordId}`);
        return response.data;
    } catch (error) {
        if (error.response?.status === 404) {
            return null;
        }
        throw error;
    }
}

/**
 * Grant subscription to a user
 */
async function grantSubscription(discordId, discordName, tier = 'standard', durationDays = null) {
    const response = await api.post('/admin/grant', {
        discordId,
        discordName,
        tier,
        durationDays
    });
    return response.data;
}

/**
 * Revoke subscription from a user
 */
async function revokeSubscription(discordId) {
    const response = await api.post('/admin/revoke', {
        discordId
    });
    return response.data;
}

/**
 * Reset user's HWID to allow new device login
 */
async function resetHwid(discordId) {
    const response = await api.post('/admin/reset-hwid', {
        discordId
    });
    return response.data;
}

/**
 * Get stats
 */
async function getStats() {
    const response = await api.get('/stats');
    return response.data;
}

/**
 * Get all active subscriptions
 */
async function getActiveSubscriptions() {
    const response = await api.get('/auth/subscriptions/active');
    return response.data;
}

module.exports = {
    getUser,
    grantSubscription,
    revokeSubscription,
    resetHwid,
    getStats,
    getActiveSubscriptions
};

