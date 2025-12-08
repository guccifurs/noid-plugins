const express = require('express');
const router = express.Router();
const { queries } = require('../db/database');
const { requireApiKey } = require('../middleware/auth');

/**
 * GET /stats
 * Get usage statistics
 * Requires API key
 */
router.get('/', requireApiKey, (req, res) => {
    try {
        const userCount = queries.getUserCount.get();
        const activeSubCount = queries.getActiveSubscriptionCount.get();
        const usageStats = queries.getUsageStats.all();

        // Group usage by plugin
        const pluginUsage = {};
        for (const stat of usageStats) {
            if (!pluginUsage[stat.plugin_name]) {
                pluginUsage[stat.plugin_name] = {};
            }
            pluginUsage[stat.plugin_name][stat.action] = stat.count;
        }

        res.json({
            totalUsers: userCount.count,
            activeSubscriptions: activeSubCount.count,
            pluginUsage,
            generatedAt: new Date().toISOString()
        });

    } catch (err) {
        console.error('Stats error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
