const crypto = require('crypto');

/**
 * Generate a unique session token
 */
function generateSessionToken() {
    return crypto.randomBytes(32).toString('hex');
}

/**
 * Validate HWID format (basic validation)
 * HWID should be a hex string of specific length
 */
function isValidHwid(hwid) {
    if (!hwid || typeof hwid !== 'string') {
        return false;
    }

    // HWID should be 32-64 hex characters
    return /^[a-fA-F0-9]{32,64}$/.test(hwid);
}

/**
 * Hash an HWID for storage (optional extra security)
 */
function hashHwid(hwid) {
    return crypto.createHash('sha256').update(hwid).digest('hex');
}

module.exports = {
    generateSessionToken,
    isValidHwid,
    hashHwid
};
