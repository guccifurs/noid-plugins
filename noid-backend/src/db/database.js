const Database = require('better-sqlite3');
const path = require('path');

const dbPath = path.join(__dirname, '../../data/noid.db');
const db = new Database(dbPath);

// Enable WAL mode for better concurrency
db.pragma('journal_mode = WAL');

// Create tables
db.exec(`
    -- Users table
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        discord_id TEXT UNIQUE NOT NULL,
        discord_name TEXT,
        hwid TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    -- Subscriptions table
    CREATE TABLE IF NOT EXISTS subscriptions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER REFERENCES users(id),
        tier TEXT DEFAULT 'standard',
        expires_at DATETIME,
        active INTEGER DEFAULT 1,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    -- Sessions table (for plugin auth)
    CREATE TABLE IF NOT EXISTS sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER REFERENCES users(id),
        session_token TEXT UNIQUE,
        ip_address TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        last_active DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    -- Usage stats
    CREATE TABLE IF NOT EXISTS usage_stats (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER REFERENCES users(id),
        plugin_name TEXT,
        action TEXT,
        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    -- Create indexes
    CREATE INDEX IF NOT EXISTS idx_users_discord_id ON users(discord_id);
    CREATE INDEX IF NOT EXISTS idx_users_hwid ON users(hwid);
    CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(session_token);
    CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions(user_id);
`);

// Helper functions
const queries = {
    // User operations
    getUserByDiscordId: db.prepare('SELECT * FROM users WHERE discord_id = ?'),
    getUserByHwid: db.prepare('SELECT * FROM users WHERE hwid = ?'),
    getUserById: db.prepare('SELECT * FROM users WHERE id = ?'),
    createUser: db.prepare('INSERT INTO users (discord_id, discord_name, hwid) VALUES (?, ?, ?)'),
    updateUserHwid: db.prepare('UPDATE users SET hwid = ? WHERE id = ?'),

    // Subscription operations
    getActiveSubscription: db.prepare(`
        SELECT s.*, u.discord_id, u.discord_name 
        FROM subscriptions s 
        JOIN users u ON s.user_id = u.id 
        WHERE s.user_id = ? AND s.active = 1 AND (s.expires_at IS NULL OR s.expires_at > datetime('now'))
    `),
    createSubscription: db.prepare('INSERT INTO subscriptions (user_id, tier, expires_at) VALUES (?, ?, ?)'),
    deactivateSubscription: db.prepare('UPDATE subscriptions SET active = 0 WHERE user_id = ?'),

    // Session operations
    getSessionByToken: db.prepare(`
        SELECT s.*, u.discord_id, u.discord_name 
        FROM sessions s 
        JOIN users u ON s.user_id = u.id 
        WHERE s.session_token = ?
    `),
    createSession: db.prepare('INSERT INTO sessions (user_id, session_token, ip_address) VALUES (?, ?, ?)'),
    updateSessionActivity: db.prepare('UPDATE sessions SET last_active = CURRENT_TIMESTAMP WHERE session_token = ?'),
    deleteSession: db.prepare('DELETE FROM sessions WHERE session_token = ?'),
    deleteUserSessions: db.prepare('DELETE FROM sessions WHERE user_id = ?'),

    // Stats operations
    logUsage: db.prepare('INSERT INTO usage_stats (user_id, plugin_name, action) VALUES (?, ?, ?)'),
    getUsageStats: db.prepare(`
        SELECT plugin_name, action, COUNT(*) as count 
        FROM usage_stats 
        WHERE timestamp > datetime('now', '-7 days')
        GROUP BY plugin_name, action
    `),
    getUserCount: db.prepare('SELECT COUNT(*) as count FROM users'),
    getActiveSubscriptionCount: db.prepare(`
        SELECT COUNT(*) as count FROM subscriptions 
        WHERE active = 1 AND (expires_at IS NULL OR expires_at > datetime('now'))
    `),

    // Admin operations
    getAllUsers: db.prepare(`
        SELECT u.*, s.tier, s.expires_at, s.active as sub_active
        FROM users u
        LEFT JOIN subscriptions s ON u.id = s.user_id AND s.active = 1
        ORDER BY u.created_at DESC
        LIMIT ? OFFSET ?
    `)
};

module.exports = { db, queries };
