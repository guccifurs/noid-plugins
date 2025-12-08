const jwt = require('jsonwebtoken');

/**
 * Middleware to validate API key for bot/admin requests
 */
function requireApiKey(req, res, next) {
    const apiKey = req.headers['x-api-key'];

    if (!apiKey || apiKey !== process.env.API_KEY) {
        return res.status(401).json({ error: 'Invalid API key' });
    }

    next();
}

/**
 * Middleware to validate JWT token for plugin requests
 */
function requireAuth(req, res, next) {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'No token provided' });
    }

    const token = authHeader.substring(7);

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded;
        next();
    } catch (err) {
        return res.status(401).json({ error: 'Invalid token' });
    }
}

/**
 * Optional auth - doesn't fail if no token, just sets req.user if valid
 */
function optionalAuth(req, res, next) {
    const authHeader = req.headers.authorization;

    if (authHeader && authHeader.startsWith('Bearer ')) {
        const token = authHeader.substring(7);
        try {
            req.user = jwt.verify(token, process.env.JWT_SECRET);
        } catch (err) {
            // Ignore invalid tokens for optional auth
        }
    }

    next();
}

/**
 * Generate JWT token for a user
 */
function generateToken(user) {
    return jwt.sign(
        {
            userId: user.id,
            discordId: user.discord_id,
            discordName: user.discord_name
        },
        process.env.JWT_SECRET,
        { expiresIn: '7d' }
    );
}

module.exports = {
    requireApiKey,
    requireAuth,
    optionalAuth,
    generateToken
};
