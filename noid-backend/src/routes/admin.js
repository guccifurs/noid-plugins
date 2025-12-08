const express = require('express');
const router = express.Router();
const { db, queries } = require('../db/database');
const { requireApiKey } = require('../middleware/auth');

// All admin routes require API key
router.use(requireApiKey);

/**
 * POST /admin/grant
 * Grant subscription to a user
 */
router.post('/grant', (req, res) => {
    try {
        const { discordId, discordName, tier = 'standard', durationDays } = req.body;

        if (!discordId) {
            return res.status(400).json({ error: 'Discord ID required' });
        }

        // Find or create user
        let user = queries.getUserByDiscordId.get(discordId);

        if (!user) {
            // Create user without HWID (they'll link it later)
            const result = queries.createUser.run(discordId, discordName || 'Unknown', null);
            user = queries.getUserById.get(result.lastInsertRowid);
        } else if (discordName && user.discord_name === 'Unknown') {
            // Update name if we have it now
            db.prepare('UPDATE users SET discord_name = ? WHERE id = ?').run(discordName, user.id);
        }

        // Calculate expiration
        let expiresAt = null;
        if (durationDays && durationDays > 0) {
            const expDate = new Date();
            expDate.setDate(expDate.getDate() + durationDays);
            expiresAt = expDate.toISOString();
        }

        // Deactivate old subscriptions
        queries.deactivateSubscription.run(user.id);

        // Create new subscription
        queries.createSubscription.run(user.id, tier, expiresAt);

        res.json({
            success: true,
            message: `Subscription granted to ${discordId}`,
            expiresAt
        });

    } catch (err) {
        console.error('Grant subscription error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * POST /admin/revoke
 * Revoke subscription from a user
 */
router.post('/revoke', (req, res) => {
    try {
        const { discordId } = req.body;

        if (!discordId) {
            return res.status(400).json({ error: 'Discord ID required' });
        }

        const user = queries.getUserByDiscordId.get(discordId);

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // Deactivate subscriptions
        queries.deactivateSubscription.run(user.id);

        // Delete all sessions (force logout)
        queries.deleteUserSessions.run(user.id);

        res.json({
            success: true,
            message: `Subscription revoked from ${discordId}`
        });

    } catch (err) {
        console.error('Revoke subscription error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * GET /admin/users
 * List all users with pagination
 */
router.get('/users', (req, res) => {
    try {
        const limit = parseInt(req.query.limit) || 50;
        const offset = parseInt(req.query.offset) || 0;

        const users = queries.getAllUsers.all(limit, offset);
        const totalUsers = queries.getUserCount.get();

        res.json({
            users: users.map(u => ({
                id: u.id,
                discordId: u.discord_id,
                discordName: u.discord_name,
                hasHwid: !!u.hwid,
                tier: u.tier,
                expiresAt: u.expires_at,
                subscriptionActive: !!u.sub_active,
                createdAt: u.created_at
            })),
            total: totalUsers.count,
            limit,
            offset
        });

    } catch (err) {
        console.error('List users error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * POST /admin/reset-hwid
 * Reset user's HWID to allow login from new device
 */
router.post('/reset-hwid', (req, res) => {
    try {
        const { discordId } = req.body;

        if (!discordId) {
            return res.status(400).json({ error: 'Discord ID required' });
        }

        const user = queries.getUserByDiscordId.get(discordId);

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // Clear HWID
        queries.updateUserHwid.run(null, user.id);

        // Delete all sessions (force re-auth)
        queries.deleteUserSessions.run(user.id);

        res.json({
            success: true,
            message: `HWID reset for ${discordId}`
        });

    } catch (err) {
        console.error('Reset HWID error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * GET /admin/user/:discordId
 * Get detailed user info including usage stats
 */
router.get('/user/:discordId', (req, res) => {
    try {
        const { discordId } = req.params;

        const user = queries.getUserByDiscordId.get(discordId);

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // Get subscription
        const subscription = queries.getActiveSubscription.get(user.id);

        // Get sessions
        const sessions = db.prepare(`
            SELECT session_token, ip_address, created_at, last_active 
            FROM sessions 
            WHERE user_id = ? 
            ORDER BY last_active DESC
        `).all(user.id);

        // Get usage stats (last 30 days)
        const usageStats = db.prepare(`
            SELECT plugin_name, action, COUNT(*) as count, 
                   MIN(timestamp) as first_use, MAX(timestamp) as last_use
            FROM usage_stats 
            WHERE user_id = ? AND timestamp > datetime('now', '-30 days')
            GROUP BY plugin_name, action
            ORDER BY count DESC
        `).all(user.id);

        // Get recent usage log (last 50 entries)
        const recentUsage = db.prepare(`
            SELECT plugin_name, action, timestamp 
            FROM usage_stats 
            WHERE user_id = ? 
            ORDER BY timestamp DESC 
            LIMIT 50
        `).all(user.id);

        // Get total usage count
        const totalUsage = db.prepare(`
            SELECT COUNT(*) as count FROM usage_stats WHERE user_id = ?
        `).get(user.id);

        res.json({
            user: {
                id: user.id,
                discordId: user.discord_id,
                discordName: user.discord_name,
                hwid: user.hwid ? user.hwid.substring(0, 8) + '...' : null,
                hwidFull: user.hwid,
                createdAt: user.created_at
            },
            subscription: subscription ? {
                tier: subscription.tier,
                expiresAt: subscription.expires_at,
                active: true
            } : null,
            sessions: sessions.map(s => ({
                ip: s.ip_address,
                createdAt: s.created_at,
                lastActive: s.last_active
            })),
            usageStats,
            recentUsage,
            totalUsageCount: totalUsage.count
        });

    } catch (err) {
        console.error('Get user detail error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * GET /admin/search
 * Search users by name or Discord ID
 */
router.get('/search', (req, res) => {
    try {
        const { q } = req.query;

        if (!q || q.length < 2) {
            return res.json({ users: [] });
        }

        const users = db.prepare(`
            SELECT u.*, s.tier, s.expires_at, s.active as sub_active
            FROM users u
            LEFT JOIN subscriptions s ON u.id = s.user_id AND s.active = 1
            WHERE u.discord_id LIKE ? OR u.discord_name LIKE ?
            ORDER BY u.created_at DESC
            LIMIT 25
        `).all(`%${q}%`, `%${q}%`);

        res.json({
            users: users.map(u => ({
                id: u.id,
                discordId: u.discord_id,
                discordName: u.discord_name,
                tier: u.tier,
                subscriptionActive: !!u.sub_active
            }))
        });

    } catch (err) {
        console.error('Search users error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * GET /admin/stats
 * Get overall usage statistics
 */
router.get('/stats', (req, res) => {
    try {
        const totalUsers = queries.getUserCount.get();
        const activeSubscriptions = queries.getActiveSubscriptionCount.get();
        const usageStats = queries.getUsageStats.all();

        // Get active users in last 24h
        const activeToday = db.prepare(`
            SELECT COUNT(DISTINCT user_id) as count 
            FROM usage_stats 
            WHERE timestamp > datetime('now', '-1 day')
        `).get();

        // Get active users in last 7 days
        const activeWeek = db.prepare(`
            SELECT COUNT(DISTINCT user_id) as count 
            FROM usage_stats 
            WHERE timestamp > datetime('now', '-7 days')
        `).get();

        res.json({
            totalUsers: totalUsers.count,
            activeSubscriptions: activeSubscriptions.count,
            activeToday: activeToday.count,
            activeWeek: activeWeek.count,
            usageByPlugin: usageStats
        });

    } catch (err) {
        console.error('Get stats error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
