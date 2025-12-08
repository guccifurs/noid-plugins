const express = require('express');
const router = express.Router();
const { db, queries } = require('../db/database');
const { generateToken } = require('../middleware/auth');
const { generateSessionToken, isValidHwid } = require('../utils/hwid');

/**
 * POST /auth/validate
 * Validate and auto-link HWID to Discord account
 * Flow:
 * - User sends discordId + hwid
 * - If user has subscription and no HWID → auto-link, authenticate
 * - If user has subscription and same HWID → authenticate
 * - If user has subscription and different HWID → "Limit reached!"
 * - If no subscription → "No subscription"
 */
router.post('/validate', (req, res) => {
    try {
        const { hwid, discordId } = req.body;

        if (!hwid || !isValidHwid(hwid)) {
            return res.status(400).json({
                authenticated: false,
                error: 'Invalid HWID format'
            });
        }

        if (!discordId) {
            return res.status(400).json({
                authenticated: false,
                error: 'Discord ID required'
            });
        }

        // Find user by Discord ID
        let user = queries.getUserByDiscordId.get(discordId);

        if (!user) {
            return res.status(401).json({
                authenticated: false,
                error: 'Account not found. Contact admin for subscription.'
            });
        }

        // Check for active subscription
        const subscription = queries.getActiveSubscription.get(user.id);

        if (!subscription) {
            return res.status(403).json({
                authenticated: false,
                error: 'No active subscription',
                user: {
                    discordId: user.discord_id,
                    discordName: user.discord_name
                }
            });
        }

        // Check HWID linking
        if (user.hwid === null || user.hwid === '') {
            // First login - auto-link this HWID
            queries.updateUserHwid.run(hwid, user.id);
            console.log(`[Auth] Auto-linked HWID for ${user.discord_name}`);
        } else if (user.hwid !== hwid) {
            // Different HWID - limit reached!
            return res.status(403).json({
                authenticated: false,
                error: 'HWID Limit reached! Contact admin to reset.'
            });
        }
        // else: same HWID, continue

        // Create session
        const sessionToken = generateSessionToken();
        const ip = req.ip || req.connection.remoteAddress;
        queries.createSession.run(user.id, sessionToken, ip);

        // Generate JWT
        const token = generateToken(user);

        // Log usage
        queries.logUsage.run(user.id, 'NoidPlugin', 'login');

        res.json({
            authenticated: true,
            token,
            sessionToken,
            user: {
                discordId: user.discord_id,
                discordName: user.discord_name,
                tier: subscription.tier,
                expiresAt: subscription.expires_at
            }
        });

    } catch (err) {
        console.error('Auth validate error:', err);
        res.status(500).json({ authenticated: false, error: 'Server error' });
    }
});

/**
 * POST /auth/link
 * Link a Discord account to an HWID
 * Called by the Discord bot when user runs /subscribe
 */
router.post('/link', (req, res) => {
    try {
        const { discordId, discordName, hwid, apiKey } = req.body;

        // Verify API key (bot authentication)
        if (apiKey !== process.env.API_KEY) {
            return res.status(401).json({ error: 'Invalid API key' });
        }

        if (!discordId || !isValidHwid(hwid)) {
            return res.status(400).json({ error: 'Missing or invalid parameters' });
        }

        // Check if user exists
        let user = queries.getUserByDiscordId.get(discordId);

        if (user) {
            // Update HWID
            queries.updateUserHwid.run(hwid, user.id);
        } else {
            // Create new user
            const result = queries.createUser.run(discordId, discordName || 'Unknown', hwid);
            user = queries.getUserById.get(result.lastInsertRowid);
        }

        res.json({
            success: true,
            message: 'HWID linked successfully',
            userId: user.id
        });

    } catch (err) {
        console.error('Auth link error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * POST /auth/logout
 * Invalidate a session
 */
router.post('/logout', (req, res) => {
    try {
        const { sessionToken } = req.body;

        if (sessionToken) {
            queries.deleteSession.run(sessionToken);
        }

        res.json({ success: true });

    } catch (err) {
        console.error('Logout error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * POST /auth/heartbeat
 * Keep session alive and log plugin usage
 */
router.post('/heartbeat', (req, res) => {
    try {
        const { sessionToken, pluginName } = req.body;

        if (!sessionToken) {
            return res.status(400).json({ error: 'Missing session token' });
        }

        const session = queries.getSessionByToken.get(sessionToken);

        if (!session) {
            return res.status(401).json({ authenticated: false, error: 'Invalid session' });
        }

        // Update session activity
        queries.updateSessionActivity.run(sessionToken);

        // Log plugin usage if provided
        if (pluginName) {
            queries.logUsage.run(session.user_id, pluginName, 'heartbeat');
        }

        res.json({ authenticated: true });

    } catch (err) {
        console.error('Heartbeat error:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
     * GET /auth/discord/url
     * Get Discord OAuth URL for login
     */
router.get('/discord/url', (req, res) => {
    const clientId = process.env.DISCORD_CLIENT_ID;
    const redirectUri = encodeURIComponent(process.env.DISCORD_REDIRECT_URI);
    const scope = encodeURIComponent('identify');

    // Generate state for CSRF protection
    const state = require('crypto').randomBytes(16).toString('hex');

    const url = `https://discord.com/api/oauth2/authorize?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=${scope}&state=${state}`;

    res.json({ url, state });
});

/**
 * GET /auth/discord/callback
 * Handle Discord OAuth callback
 */
router.get('/discord/callback', async (req, res) => {
    try {
        const { code, state } = req.query;

        if (!code) {
            return res.send('<html><body><h2>Error: No authorization code received</h2></body></html>');
        }

        // Exchange code for token
        const tokenResponse = await fetch('https://discord.com/api/oauth2/token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                client_id: process.env.DISCORD_CLIENT_ID,
                client_secret: process.env.DISCORD_CLIENT_SECRET,
                grant_type: 'authorization_code',
                code: code,
                redirect_uri: process.env.DISCORD_REDIRECT_URI
            })
        });

        if (!tokenResponse.ok) {
            const error = await tokenResponse.text();
            console.error('Token exchange error:', error);
            return res.send('<html><body><h2>Error exchanging code</h2><p>' + error + '</p></body></html>');
        }

        const tokenData = await tokenResponse.json();

        // Get user info from Discord
        const userResponse = await fetch('https://discord.com/api/users/@me', {
            headers: { 'Authorization': `Bearer ${tokenData.access_token}` }
        });

        if (!userResponse.ok) {
            return res.send('<html><body><h2>Error fetching user info</h2></body></html>');
        }

        const discordUser = await userResponse.json();

        // Find or create user in our database
        let user = queries.getUserByDiscordId.get(discordUser.id);

        if (!user) {
            // User doesn't exist - create without subscription
            const result = queries.createUser.run(discordUser.id, discordUser.username, null);
            user = queries.getUserById.get(result.lastInsertRowid);
        } else {
            // Update username in case it changed
            db.prepare('UPDATE users SET discord_name = ? WHERE id = ?').run(discordUser.username, user.id);
        }

        // Check subscription
        const subscription = queries.getActiveSubscription.get(user.id);

        // Create session
        const sessionToken = generateSessionToken();
        const ip = req.ip || req.connection.remoteAddress;
        queries.createSession.run(user.id, sessionToken, ip);

        // Generate JWT
        const token = generateToken(user);

        // Log usage
        queries.logUsage.run(user.id, 'NoidPlugin', 'discord_oauth');

        // Store auth data for polling - use state from query if available
        const authState = state || discordUser.id;
        pendingAuths.set(authState, {
            token,
            sessionToken,
            user: {
                discordId: discordUser.id,
                discordName: discordUser.username,
                tier: subscription ? subscription.tier : 'none',
                expiresAt: subscription ? subscription.expires_at : null,
                hasSubscription: !!subscription
            }
        });

        // Clean up after 5 minutes
        setTimeout(() => pendingAuths.delete(authState), 300000);

        // Return success page
        res.send(`
            <html>
            <head><title>Noid - Login Successful</title></head>
            <body style="background:#2c2f33;color:white;font-family:Arial;text-align:center;padding-top:100px;">
                <h1 style="color:#43b581;">✅ Login Successful!</h1>
                <p>Welcome, <strong>${discordUser.username}</strong>!</p>
                <p>${subscription ? 'Subscription: ' + subscription.tier : '⚠️ No active subscription'}</p>
                <p style="color:#888;margin-top:30px;">You can close this window and return to RuneLite.</p>
            </body>
            </html>
        `);

    } catch (err) {
        console.error('OAuth callback error:', err);
        res.send('<html><body><h2>Error: ' + err.message + '</h2></body></html>');
    }
});

// Store pending auths for polling
const pendingAuths = new Map();

/**
 * GET /auth/discord/poll
 * Poll for auth completion (called by plugin)
 * Supports polling by state OR discordId
 */
router.get('/discord/poll', (req, res) => {
    const { state, discordId } = req.query;

    // Try state first, then discordId
    const key = state || discordId;

    if (!key) {
        return res.json({ authenticated: false });
    }

    const authData = pendingAuths.get(key);
    if (authData) {
        pendingAuths.delete(key);
        return res.json({ authenticated: true, ...authData });
    }

    res.json({ authenticated: false });
});

/**
 * GET /auth/session/restore
 * Restore session by Discord ID (for plugin restart persistence)
 */
router.get('/session/restore', (req, res) => {
    try {
        const { discordId } = req.query;

        if (!discordId) {
            return res.json({ authenticated: false });
        }

        // Check if user exists and has active subscription
        const user = queries.getUserByDiscordId.get(discordId);
        if (!user) {
            return res.json({ authenticated: false, error: 'User not found' });
        }

        const subscription = queries.getActiveSubscription.get(user.id);
        if (!subscription) {
            return res.json({
                authenticated: false,
                error: 'No active subscription',
                user: {
                    discordId: user.discord_id,
                    discordName: user.discord_name
                }
            });
        }

        // Create a new session token for this restore
        const crypto = require('crypto');
        const sessionToken = crypto.randomBytes(32).toString('hex');

        // Delete old sessions for this user (optional - keeps it clean)
        queries.deleteUserSessions.run(user.id);

        // Create new session
        queries.createSession.run(user.id, sessionToken, req.ip || 'restored');

        console.log(`[Session Restore] Created session for ${user.discord_name} (${user.discord_id})`);

        // Return user info with session token for session restore
        res.json({
            authenticated: true,
            sessionToken: sessionToken,
            user: {
                discordId: user.discord_id,
                discordName: user.discord_name,
                tier: subscription.tier,
                expiresAt: subscription.expires_at,
                hasSubscription: true
            }
        });

    } catch (err) {
        console.error('Session restore error:', err);
        res.json({ authenticated: false, error: 'Server error' });
    }
});

/**
 * GET /subscriptions/active
 * Get all active subscriptions (for Discord role sync)
 */
router.get('/subscriptions/active', (req, res) => {
    try {
        const apiKey = req.get('x-api-key');
        if (apiKey !== process.env.API_KEY) {
            return res.status(401).json({ error: 'Unauthorized' });
        }

        // Get all users with active subscriptions
        const activeSubscriptions = db.prepare(`
            SELECT u.discord_id, u.discord_name, s.tier, s.expires_at
            FROM users u
            JOIN subscriptions s ON u.id = s.user_id
            WHERE s.expires_at IS NULL OR s.expires_at > datetime('now')
        `).all();

        res.json({ subscriptions: activeSubscriptions });

    } catch (err) {
        console.error('Error fetching active subscriptions:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
