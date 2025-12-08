const express = require('express');
const router = express.Router();
const { queries } = require('../db/database');
const { requireApiKey } = require('../middleware/auth');

/**
 * GET /users/:discordId
 * Get user info by Discord ID
 * Requires API key (bot/admin only)
 */
router.get('/:discordId', requireApiKey, (req, res) => {
    try {
        const { discordId } = req.params;

        const user = queries.getUserByDiscordId.get(discordId);

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        const subscription = queries.getActiveSubscription.get(user.id);

        res.json({
            user: {
                id: user.id,
                discordId: user.discord_id,
                discordName: user.discord_name,
                hwid: user.hwid ? '***' + user.hwid.slice(-8) : null,
                createdAt: user.created_at
            },
            subscription: subscription ? {
                tier: subscription.tier,
                expiresAt: subscription.expires_at,
                active: !!subscription.active
            } : null
        });

    } catch (err) {
        console.error('Get user error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
