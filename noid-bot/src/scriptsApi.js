/**
 * Script SDN API - REST endpoints for community script sharing
 * Uses SQLite for persistent storage
 * 
 * Endpoints:
 * - GET /api/scripts - List all scripts
 * - POST /api/scripts - Submit a new script
 * - PUT /api/scripts/:id - Edit own script
 * - DELETE /api/scripts/:id - Delete own script
 * - POST /api/scripts/:id/vote - Upvote a script (12hr cooldown per user per script)
 */

const express = require('express');
const path = require('path');
const crypto = require('crypto');
const Database = require('better-sqlite3');

const router = express.Router();

// Initialize SQLite database
const DB_PATH = path.join(__dirname, '../data/scripts.db');
const db = new Database(DB_PATH);

// Create tables if they don't exist
db.exec(`
    CREATE TABLE IF NOT EXISTS scripts (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        content TEXT NOT NULL,
        author_id TEXT NOT NULL,
        author_name TEXT NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS votes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        script_id TEXT NOT NULL,
        user_id TEXT NOT NULL,
        voted_at TEXT NOT NULL,
        UNIQUE(script_id, user_id),
        FOREIGN KEY(script_id) REFERENCES scripts(id) ON DELETE CASCADE
    );

    CREATE INDEX IF NOT EXISTS idx_votes_script ON votes(script_id);
    CREATE INDEX IF NOT EXISTS idx_votes_user ON votes(user_id);
    CREATE INDEX IF NOT EXISTS idx_scripts_author ON scripts(author_id);
`);

console.log('[ScriptSDN] SQLite database initialized');

// Rate limit tracking: { discordId: [timestamp1, timestamp2, ...] }
const submissionTimes = new Map();
const RATE_LIMIT_WINDOW = 60 * 60 * 1000; // 1 hour
const MAX_SUBMISSIONS_PER_HOUR = 3;
const VOTE_COOLDOWN = 12 * 60 * 60 * 1000; // 12 hours

/**
 * Check rate limit for user submissions
 */
function checkRateLimit(discordId) {
    const now = Date.now();
    const userSubmissions = submissionTimes.get(discordId) || [];

    const recentSubmissions = userSubmissions.filter(time => now - time < RATE_LIMIT_WINDOW);
    submissionTimes.set(discordId, recentSubmissions);

    if (recentSubmissions.length >= MAX_SUBMISSIONS_PER_HOUR) {
        const oldestInWindow = Math.min(...recentSubmissions);
        const minutesRemaining = Math.ceil((RATE_LIMIT_WINDOW - (now - oldestInWindow)) / 60000);
        return { allowed: false, minutesRemaining };
    }

    return { allowed: true };
}

/**
 * Record a submission for rate limiting
 */
function recordSubmission(discordId) {
    const userSubmissions = submissionTimes.get(discordId) || [];
    userSubmissions.push(Date.now());
    submissionTimes.set(discordId, userSubmissions);
}

/**
 * Check if user can vote on a script (12hr cooldown)
 */
function canVote(scriptId, userId) {
    const row = db.prepare(`
        SELECT voted_at FROM votes 
        WHERE script_id = ? AND user_id = ?
    `).get(scriptId, userId);

    if (!row) return { canVote: true };

    const lastVote = new Date(row.voted_at).getTime();
    const now = Date.now();
    const elapsed = now - lastVote;

    if (elapsed < VOTE_COOLDOWN) {
        const hoursRemaining = Math.ceil((VOTE_COOLDOWN - elapsed) / (60 * 60 * 1000));
        return { canVote: false, hoursRemaining };
    }

    return { canVote: true };
}

/**
 * Middleware to validate session token
 */
function validateAuth(req, res, next) {
    const sessionToken = req.headers['x-session-token'];
    const discordId = req.headers['x-discord-id'];
    const discordName = req.headers['x-discord-name'];

    if (!sessionToken || !discordId || !discordName) {
        return res.status(401).json({ error: 'Authentication required' });
    }

    req.user = {
        discordId,
        discordName,
        sessionToken
    };

    next();
}

/**
 * GET /api/scripts - List all scripts
 */
router.get('/', validateAuth, (req, res) => {
    try {
        const scripts = db.prepare(`
            SELECT s.*, 
                   (SELECT COUNT(*) FROM votes v WHERE v.script_id = s.id) as votes,
                   (SELECT 1 FROM votes v WHERE v.script_id = s.id AND v.user_id = ?) as user_voted
            FROM scripts s
            ORDER BY votes DESC, created_at DESC
        `).all(req.user.discordId);

        // Map to camelCase and add hasVoted flag
        const result = scripts.map(s => ({
            id: s.id,
            name: s.name,
            description: s.description,
            content: s.content,
            authorId: s.author_id,
            authorName: s.author_name,
            createdAt: s.created_at,
            updatedAt: s.updated_at,
            votes: s.votes || 0,
            hasVoted: !!s.user_voted
        }));

        res.json({ scripts: result });
    } catch (error) {
        console.error('[ScriptSDN] GET /scripts error:', error);
        res.status(500).json({ error: 'Failed to load scripts' });
    }
});

/**
 * POST /api/scripts - Submit a new script
 */
router.post('/', validateAuth, (req, res) => {
    try {
        const { name, description, content } = req.body;

        // Validate input
        if (!name || !name.trim()) {
            return res.status(400).json({ error: 'Script name is required' });
        }
        if (!content || !content.trim()) {
            return res.status(400).json({ error: 'Script content is required' });
        }
        if (name.length > 50) {
            return res.status(400).json({ error: 'Script name too long (max 50 chars)' });
        }
        if (description && description.length > 200) {
            return res.status(400).json({ error: 'Description too long (max 200 chars)' });
        }
        if (content.length > 50000) {
            return res.status(400).json({ error: 'Script content too large (max 50KB)' });
        }

        // Check rate limit
        const rateCheck = checkRateLimit(req.user.discordId);
        if (!rateCheck.allowed) {
            return res.status(429).json({
                error: `Rate limit exceeded. Try again in ${rateCheck.minutesRemaining} minutes.`
            });
        }

        const id = crypto.randomBytes(8).toString('hex');
        const now = new Date().toISOString();

        db.prepare(`
            INSERT INTO scripts (id, name, description, content, author_id, author_name, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        `).run(id, name.trim(), (description || '').trim(), content.trim(),
            req.user.discordId, req.user.discordName, now, now);

        recordSubmission(req.user.discordId);

        const newScript = {
            id,
            name: name.trim(),
            description: (description || '').trim(),
            content: content.trim(),
            authorId: req.user.discordId,
            authorName: req.user.discordName,
            createdAt: now,
            updatedAt: now,
            votes: 0,
            hasVoted: false
        };

        console.log(`[ScriptSDN] Script submitted: "${name}" by ${req.user.discordName}`);
        res.status(201).json({ script: newScript });

    } catch (error) {
        console.error('[ScriptSDN] POST /scripts error:', error);
        res.status(500).json({ error: 'Failed to submit script' });
    }
});

/**
 * PUT /api/scripts/:id - Edit own script
 */
router.put('/:id', validateAuth, (req, res) => {
    try {
        const { id } = req.params;
        const { name, description, content } = req.body;

        const script = db.prepare('SELECT * FROM scripts WHERE id = ?').get(id);

        if (!script) {
            return res.status(404).json({ error: 'Script not found' });
        }

        if (script.author_id !== req.user.discordId) {
            return res.status(403).json({ error: 'You can only edit your own scripts' });
        }

        // Validate input
        if (name && name.length > 50) {
            return res.status(400).json({ error: 'Script name too long (max 50 chars)' });
        }
        if (description && description.length > 200) {
            return res.status(400).json({ error: 'Description too long (max 200 chars)' });
        }
        if (content && content.length > 50000) {
            return res.status(400).json({ error: 'Script content too large (max 50KB)' });
        }

        const now = new Date().toISOString();
        const newName = name ? name.trim() : script.name;
        const newDesc = description !== undefined ? description.trim() : script.description;
        const newContent = content ? content.trim() : script.content;

        db.prepare(`
            UPDATE scripts SET name = ?, description = ?, content = ?, updated_at = ?
            WHERE id = ?
        `).run(newName, newDesc, newContent, now, id);

        const votes = db.prepare('SELECT COUNT(*) as count FROM votes WHERE script_id = ?').get(id).count;

        const updatedScript = {
            id,
            name: newName,
            description: newDesc,
            content: newContent,
            authorId: script.author_id,
            authorName: script.author_name,
            createdAt: script.created_at,
            updatedAt: now,
            votes
        };

        console.log(`[ScriptSDN] Script updated: "${newName}" by ${req.user.discordName}`);
        res.json({ script: updatedScript });

    } catch (error) {
        console.error('[ScriptSDN] PUT /scripts/:id error:', error);
        res.status(500).json({ error: 'Failed to update script' });
    }
});

/**
 * DELETE /api/scripts/:id - Delete own script
 */
router.delete('/:id', validateAuth, (req, res) => {
    try {
        const { id } = req.params;

        const script = db.prepare('SELECT * FROM scripts WHERE id = ?').get(id);

        if (!script) {
            return res.status(404).json({ error: 'Script not found' });
        }

        if (script.author_id !== req.user.discordId) {
            return res.status(403).json({ error: 'You can only delete your own scripts' });
        }

        // Delete votes first (cascade)
        db.prepare('DELETE FROM votes WHERE script_id = ?').run(id);
        db.prepare('DELETE FROM scripts WHERE id = ?').run(id);

        console.log(`[ScriptSDN] Script deleted: "${script.name}" by ${req.user.discordName}`);
        res.json({ success: true });

    } catch (error) {
        console.error('[ScriptSDN] DELETE /scripts/:id error:', error);
        res.status(500).json({ error: 'Failed to delete script' });
    }
});

/**
 * POST /api/scripts/:id/vote - Upvote a script (12hr cooldown)
 */
router.post('/:id/vote', validateAuth, (req, res) => {
    try {
        const { id } = req.params;

        const script = db.prepare('SELECT id FROM scripts WHERE id = ?').get(id);
        if (!script) {
            return res.status(404).json({ error: 'Script not found' });
        }

        // Check cooldown
        const voteCheck = canVote(id, req.user.discordId);
        if (!voteCheck.canVote) {
            return res.status(429).json({
                error: `You can vote again in ${voteCheck.hoursRemaining} hours`
            });
        }

        // Upsert vote (update timestamp if exists)
        const now = new Date().toISOString();
        db.prepare(`
            INSERT INTO votes (script_id, user_id, voted_at)
            VALUES (?, ?, ?)
            ON CONFLICT(script_id, user_id) DO UPDATE SET voted_at = ?
        `).run(id, req.user.discordId, now, now);

        // Get new vote count
        const votes = db.prepare('SELECT COUNT(*) as count FROM votes WHERE script_id = ?').get(id).count;

        res.json({ votes, hasVoted: true });

    } catch (error) {
        console.error('[ScriptSDN] POST /scripts/:id/vote error:', error);
        res.status(500).json({ error: 'Failed to register vote' });
    }
});

module.exports = router;
