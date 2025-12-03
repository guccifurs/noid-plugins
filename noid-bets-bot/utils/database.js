// SQLite-backed database for the betting bot
// Replaces the JSON file with a proper relational database

const Database = require('better-sqlite3');
const path = require('path');

const DATA_DIR = path.join(__dirname, '..', '..', 'users');
const DB_PATH = path.join(DATA_DIR, 'noidbets.db');

const DEFAULT_ADMIN_ROLE_ID = '1440421585281355896';
const DEFAULT_BETTING_CHANNEL_ID = '1440416961098944653';
const DEFAULT_RESULTS_CHANNEL_ID = '1440416910197002321';

// Initialize database connection
const db = new Database(DB_PATH);
db.pragma('journal_mode = WAL'); // Better performance for concurrent reads

// Create schema
function initializeSchema() {
  db.exec(`
    -- Settings table
    CREATE TABLE IF NOT EXISTS settings (
      key TEXT PRIMARY KEY,
      value TEXT
    );

    -- Users table
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      display_name TEXT,
      balance INTEGER DEFAULT 0,
      wins INTEGER DEFAULT 0,
      losses INTEGER DEFAULT 0,
      rakeback_unclaimed INTEGER DEFAULT 0,
      created_at INTEGER DEFAULT (unixepoch())
    );

    -- User RSNs (many-to-many relationship)
    CREATE TABLE IF NOT EXISTS user_rsns (
      user_id TEXT NOT NULL,
      rsn TEXT NOT NULL,
      linked_at INTEGER DEFAULT (unixepoch()),
      PRIMARY KEY (user_id, rsn),
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
    CREATE INDEX IF NOT EXISTS idx_rsn_lower ON user_rsns(LOWER(rsn));

    -- Bet history
    CREATE TABLE IF NOT EXISTS bet_history (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id TEXT NOT NULL,
      display_name TEXT,
      round_id TEXT,
      side TEXT,
      amount INTEGER,
      outcome TEXT,
      payout INTEGER,
      created_at INTEGER DEFAULT (unixepoch()),
      FOREIGN KEY (user_id) REFERENCES users(id)
    );
    CREATE INDEX IF NOT EXISTS idx_bet_user ON bet_history(user_id);
    CREATE INDEX IF NOT EXISTS idx_bet_round ON bet_history(round_id);

    -- Deposits
    CREATE TABLE IF NOT EXISTS deposits (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id TEXT NOT NULL,
      amount INTEGER,
      reason TEXT,
      created_at INTEGER DEFAULT (unixepoch()),
      FOREIGN KEY (user_id) REFERENCES users(id)
    );
    CREATE INDEX IF NOT EXISTS idx_deposit_user ON deposits(user_id);

    -- Withdrawals
    CREATE TABLE IF NOT EXISTS withdrawals (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id TEXT NOT NULL,
      amount INTEGER,
      reason TEXT,
      created_at INTEGER DEFAULT (unixepoch()),
      FOREIGN KEY (user_id) REFERENCES users(id)
    );
    CREATE INDEX IF NOT EXISTS idx_withdrawal_user ON withdrawals(user_id);

    -- Stats table (single row)
    CREATE TABLE IF NOT EXISTS stats (
      id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
      red_streak INTEGER DEFAULT 0,
      blue_streak INTEGER DEFAULT 0,
      last_winner TEXT
    );

    -- Last winners history (stores as JSON array for simplicity)
    CREATE TABLE IF NOT EXISTS last_winners (
      id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
      winners TEXT DEFAULT '[]'
    );

    -- Round metadata (single row, optional)
    CREATE TABLE IF NOT EXISTS round_meta (
      id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
      data TEXT
    );

    -- Crypto payments (Plisio integration)
    CREATE TABLE IF NOT EXISTS crypto_payments (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      txn_id TEXT UNIQUE NOT NULL,
      user_id TEXT NOT NULL,
      amount_usd REAL NOT NULL,
      amount_gp INTEGER NOT NULL,
      currency TEXT DEFAULT 'USDT',
      wallet_hash TEXT,
      invoice_url TEXT,
      qr_code TEXT,
      status TEXT DEFAULT 'pending',
      created_at INTEGER DEFAULT (unixepoch()),
      confirmed_at INTEGER,
      metadata TEXT,
      FOREIGN KEY (user_id) REFERENCES users(id)
    );
    CREATE INDEX IF NOT EXISTS idx_crypto_txn ON crypto_payments(txn_id);
    CREATE INDEX IF NOT EXISTS idx_crypto_user ON crypto_payments(user_id);
    CREATE INDEX IF NOT EXISTS idx_crypto_status ON crypto_payments(status);

    -- Crypto withdrawals
    CREATE TABLE IF NOT EXISTS crypto_withdrawals (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      withdrawal_id TEXT UNIQUE NOT NULL,
      user_id TEXT NOT NULL,
      amount_gp INTEGER NOT NULL,
      amount_usd REAL NOT NULL,
      currency TEXT DEFAULT 'USDT',
      address TEXT NOT NULL,
      status TEXT DEFAULT 'pending',
      txn_hash TEXT,
      created_at INTEGER DEFAULT (unixepoch()),
      processed_at INTEGER,
      metadata TEXT,
      FOREIGN KEY (user_id) REFERENCES users(id)
    );
    CREATE INDEX IF NOT EXISTS idx_withdrawal_id ON crypto_withdrawals(withdrawal_id);
    CREATE INDEX IF NOT EXISTS idx_withdrawal_user ON crypto_withdrawals(user_id);
    CREATE INDEX IF NOT EXISTS idx_withdrawal_status ON crypto_withdrawals(status);
  `);

  // Initialize default settings if they don't exist
  const settingsCount = db.prepare('SELECT COUNT(*) as count FROM settings').get().count;
  if (settingsCount === 0) {
    const insertSetting = db.prepare('INSERT INTO settings (key, value) VALUES (?, ?)');
    insertSetting.run('bettingChannelId', DEFAULT_BETTING_CHANNEL_ID);
    insertSetting.run('resultsChannelId', DEFAULT_RESULTS_CHANNEL_ID);
    insertSetting.run('adminRoleId', DEFAULT_ADMIN_ROLE_ID);
    insertSetting.run('panelMessageId', 'null');
    insertSetting.run('panelThumbnailUrl', 'null');
    insertSetting.run('logoUrl', 'null');
  }

  // Initialize stats if it doesn't exist
  const statsCount = db.prepare('SELECT COUNT(*) as count FROM stats').get().count;
  if (statsCount === 0) {
    db.prepare('INSERT INTO stats (id, red_streak, blue_streak, last_winner) VALUES (1, 0, 0, NULL)').run();
  }

  // Initialize last_winners if it doesn't exist
  const winnersCount = db.prepare('SELECT COUNT(*) as count FROM last_winners').get().count;
  if (winnersCount === 0) {
    db.prepare('INSERT INTO last_winners (id, winners) VALUES (1, ?)').run('[]');
  }
}

// Initialize schema on load
initializeSchema();

// ==================== Settings Functions ====================

function getSettings() {
  const rows = db.prepare('SELECT key, value FROM settings').all();
  const settings = {};
  for (const row of rows) {
    try {
      settings[row.key] = row.value === 'null' ? null : (row.value.startsWith('{') || row.value.startsWith('[') ? JSON.parse(row.value) : row.value);
    } catch {
      settings[row.key] = row.value;
    }
  }
  return settings;
}

function updateSettings(patch) {
  const update = db.prepare('INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)');
  const updateMany = db.transaction((patches) => {
    for (const [key, value] of Object.entries(patches)) {
      update.run(key, typeof value === 'object' ? JSON.stringify(value) : String(value));
    }
  });
  updateMany(patch);
  return getSettings();
}

// ==================== User Functions ====================

function getOrCreateUser(userId, displayName) {
  let user = db.prepare('SELECT * FROM users WHERE id = ?').get(userId);
  
  if (!user) {
    db.prepare(`
      INSERT INTO users (id, display_name, balance, wins, losses, rakeback_unclaimed)
      VALUES (?, ?, 0, 0, 0, 0)
    `).run(userId, displayName || null);
    user = db.prepare('SELECT * FROM users WHERE id = ?').get(userId);
  } else if (displayName && user.display_name !== displayName) {
    db.prepare('UPDATE users SET display_name = ? WHERE id = ?').run(displayName, userId);
    user.display_name = displayName;
  }

  // Get linked RSNs
  const rsns = db.prepare('SELECT rsn FROM user_rsns WHERE user_id = ?').all(userId);
  user.linkedRsns = rsns.map(r => r.rsn);

  // Get bet history
  const bets = db.prepare('SELECT * FROM bet_history WHERE user_id = ? ORDER BY created_at DESC').all(userId);
  user.betHistory = bets.map(b => ({
    displayName: b.display_name,
    roundId: b.round_id,
    side: b.side,
    amount: b.amount,
    outcome: b.outcome,
    payout: b.payout,
    ts: b.created_at * 1000 // Convert to milliseconds
  }));

  // Get deposits
  const deps = db.prepare('SELECT * FROM deposits WHERE user_id = ? ORDER BY created_at DESC').all(userId);
  user.deposits = deps.map(d => ({
    amount: d.amount,
    reason: d.reason,
    ts: d.created_at * 1000
  }));

  // Get withdrawals
  const wds = db.prepare('SELECT * FROM withdrawals WHERE user_id = ? ORDER BY created_at DESC').all(userId);
  user.withdrawals = wds.map(w => ({
    amount: w.amount,
    reason: w.reason,
    ts: w.created_at * 1000
  }));

  return {
    id: user.id,
    displayName: user.display_name,
    linkedRsns: user.linkedRsns,
    balance: user.balance,
    wins: user.wins,
    losses: user.losses,
    betHistory: user.betHistory,
    deposits: user.deposits,
    withdrawals: user.withdrawals,
    rakebackUnclaimed: user.rakeback_unclaimed
  };
}

function adjustBalance(userId, amount, opts = {}) {
  const result = db.transaction(() => {
    // Create user if doesn't exist
    getOrCreateUser(userId, opts.displayName);

    // Update balance (ensure it doesn't go negative)
    const currentBalance = db.prepare('SELECT balance FROM users WHERE id = ?').get(userId).balance;
    const newBalance = Math.max(0, currentBalance + amount);
    db.prepare('UPDATE users SET balance = ? WHERE id = ?').run(newBalance, userId);

    // Record transaction
    const timestamp = Math.floor(Date.now() / 1000);
    if (amount > 0) {
      db.prepare('INSERT INTO deposits (user_id, amount, reason, created_at) VALUES (?, ?, ?, ?)')
        .run(userId, amount, opts.reason || 'adjust', timestamp);
    } else if (amount < 0) {
      db.prepare('INSERT INTO withdrawals (user_id, amount, reason, created_at) VALUES (?, ?, ?, ?)')
        .run(userId, -amount, opts.reason || 'adjust', timestamp);
    }

    return newBalance;
  })();

  return result;
}

function recordBetHistory(userId, entry) {
  const timestamp = Math.floor(Date.now() / 1000);
  db.prepare(`
    INSERT INTO bet_history (user_id, display_name, round_id, side, amount, outcome, payout, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `).run(
    userId,
    entry.displayName || null,
    entry.roundId,
    entry.side,
    entry.amount,
    entry.outcome,
    entry.payout,
    timestamp
  );
}

function normalizeRsn(rsn) {
  if (!rsn) return '';
  // Replace non-breaking spaces (U+00A0) with regular spaces, then trim
  return rsn.replace(/\u00A0/g, ' ').trim();
}

function linkRsn(userId, rsn) {
  const normalizedRsn = normalizeRsn(rsn);
  console.log(`[DB] Linking RSN: userId="${userId}", rsn="${normalizedRsn}" (length: ${normalizedRsn.length})`);
  console.log(`[DB] RSN bytes: ${Buffer.from(normalizedRsn).toString('hex')}`);
  
  db.prepare('INSERT OR IGNORE INTO user_rsns (user_id, rsn) VALUES (?, ?)')
    .run(userId, normalizedRsn);
  
  // Verify it was stored
  const stored = db.prepare('SELECT rsn FROM user_rsns WHERE user_id = ? AND LOWER(rsn) = LOWER(?)')
    .get(userId, normalizedRsn);
  console.log(`[DB] Verify stored RSN: ${stored ? `"${stored.rsn}"` : 'NOT FOUND'}`);
}

function findUserByRsn(rsn) {
  if (!rsn) {
    console.log(`[DB] findUserByRsn: rsn is empty/null`);
    return null;
  }
  
  const normalizedRsn = normalizeRsn(rsn);
  console.log(`[DB] Looking up RSN: "${normalizedRsn}" (length: ${normalizedRsn.length})`);
  console.log(`[DB] RSN bytes: ${Buffer.from(normalizedRsn).toString('hex')}`);
  
  const result = db.prepare(`
    SELECT DISTINCT u.* FROM users u
    JOIN user_rsns ur ON ur.user_id = u.id
    WHERE LOWER(ur.rsn) = LOWER(?)
  `).get(normalizedRsn);
  
  if (!result) {
    console.log(`[DB] No user found for RSN: "${normalizedRsn}"`);
    // Show all stored RSNs for debugging
    const allRsns = db.prepare('SELECT rsn, user_id FROM user_rsns LIMIT 20').all();
    console.log(`[DB] First 20 stored RSNs:`, allRsns.map(r => `"${r.rsn}" (${Buffer.from(r.rsn).toString('hex')})`).join(', '));
    return null;
  }
  
  console.log(`[DB] Found user: id="${result.id}", displayName="${result.display_name}"`);
  return getOrCreateUser(result.id);
}

// ==================== Rakeback Functions ====================

function addRakeback(userId, amount) {
  if (!amount || amount <= 0) return 0;
  
  getOrCreateUser(userId);
  db.prepare('UPDATE users SET rakeback_unclaimed = rakeback_unclaimed + ? WHERE id = ?')
    .run(amount, userId);
  
  const user = db.prepare('SELECT rakeback_unclaimed FROM users WHERE id = ?').get(userId);
  return user.rakeback_unclaimed;
}

function claimRakeback(userId, displayName) {
  const result = db.transaction(() => {
    const user = getOrCreateUser(userId, displayName);
    const toClaim = user.rakebackUnclaimed || 0;
    
    if (toClaim <= 0) {
      return { claimed: 0, newBalance: user.balance || 0 };
    }
    
    // Reset rakeback
    db.prepare('UPDATE users SET rakeback_unclaimed = 0 WHERE id = ?').run(userId);
    
    // Add to balance
    const newBalance = adjustBalance(userId, toClaim, {
      displayName,
      reason: 'rakeback-claim',
    });
    
    return { claimed: toClaim, newBalance };
  })();
  
  return result;
}

// ==================== Stats Functions ====================

function getStats() {
  const stats = db.prepare('SELECT * FROM stats WHERE id = 1').get();
  const winnersRow = db.prepare('SELECT winners FROM last_winners WHERE id = 1').get();
  
  let lastWinners = [];
  try {
    lastWinners = JSON.parse(winnersRow.winners || '[]');
  } catch (e) {
    lastWinners = [];
  }
  
  return {
    redStreak: stats.red_streak,
    blueStreak: stats.blue_streak,
    lastWinner: stats.last_winner,
    lastWinners: lastWinners
  };
}

function recordWinnerSide(side) {
  db.transaction(() => {
    if (side !== 'red' && side !== 'blue') {
      db.prepare(`
        UPDATE stats SET
          last_winner = NULL,
          red_streak = 0,
          blue_streak = 0
        WHERE id = 1
      `).run();
      return;
    }
    
    const stats = db.prepare('SELECT * FROM stats WHERE id = 1').get();
    
    let redStreak = 0;
    let blueStreak = 0;
    
    if (stats.last_winner === side) {
      if (side === 'red') {
        redStreak = stats.red_streak + 1;
        blueStreak = 0;
      } else {
        blueStreak = stats.blue_streak + 1;
        redStreak = 0;
      }
    } else {
      if (side === 'red') {
        redStreak = 1;
        blueStreak = 0;
      } else {
        blueStreak = 1;
        redStreak = 0;
      }
    }
    
    db.prepare(`
      UPDATE stats SET
        last_winner = ?,
        red_streak = ?,
        blue_streak = ?
      WHERE id = 1
    `).run(side, redStreak, blueStreak);
    
    // Update last winners history
    const winnersRow = db.prepare('SELECT winners FROM last_winners WHERE id = 1').get();
    let lastWinners = [];
    try {
      lastWinners = JSON.parse(winnersRow.winners || '[]');
    } catch (e) {
      lastWinners = [];
    }
    
    lastWinners.push(side);
    if (lastWinners.length > 50) {
      lastWinners = lastWinners.slice(-50);
    }
    
    db.prepare('UPDATE last_winners SET winners = ? WHERE id = 1')
      .run(JSON.stringify(lastWinners));
  })();
}

// ==================== Round Metadata ====================

function saveRoundMeta(roundMeta) {
  const data = roundMeta ? JSON.stringify(roundMeta) : null;
  db.prepare('INSERT OR REPLACE INTO round_meta (id, data) VALUES (1, ?)').run(data);
}

function getRoundMeta() {
  const row = db.prepare('SELECT data FROM round_meta WHERE id = 1').get();
  if (!row || !row.data) return null;
  try {
    return JSON.parse(row.data);
  } catch (e) {
    return null;
  }
}

// ==================== Helper for compatibility ====================

function getDb() {
  // For compatibility with old code that expects full DB object
  return {
    settings: getSettings(),
    users: getAllUsersAsObject(),
    stats: getStats(),
    round: getRoundMeta()
  };
}

function getAllUsersAsObject() {
  const users = db.prepare('SELECT id FROM users').all();
  const result = {};
  for (const user of users) {
    result[user.id] = getOrCreateUser(user.id);
  }
  return result;
}

// ==================== Crypto Payment Functions ====================

function recordCryptoPayment(userId, paymentData) {
  return db.prepare(`
    INSERT INTO crypto_payments 
    (txn_id, user_id, amount_usd, amount_gp, currency, wallet_hash, invoice_url, qr_code, status, metadata)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?)
  `).run(
    paymentData.txn_id,
    userId,
    paymentData.amount_usd,
    paymentData.amount_gp,
    paymentData.currency || 'USDT',
    paymentData.wallet_hash || null,
    paymentData.invoice_url || null,
    paymentData.qr_code || null,
    JSON.stringify(paymentData.metadata || {})
  );
}

function updateCryptoPaymentStatus(txnId, status, confirmedAt = null) {
  const timestamp = confirmedAt || (status === 'completed' ? Math.floor(Date.now() / 1000) : null);
  return db.prepare(`
    UPDATE crypto_payments 
    SET status = ?, confirmed_at = ?
    WHERE txn_id = ?
  `).run(status, timestamp, txnId);
}

function getCryptoPayment(txnId) {
  return db.prepare('SELECT * FROM crypto_payments WHERE txn_id = ?').get(txnId);
}

function getPendingCryptoPayments(maxAge = 24 * 60 * 60) {
  const cutoff = Math.floor(Date.now() / 1000) - maxAge;
  return db.prepare(`
    SELECT * FROM crypto_payments 
    WHERE status = 'pending' AND created_at > ?
    ORDER BY created_at DESC
  `).all(cutoff);
}

function getUserCryptoPayments(userId, limit = 10) {
  return db.prepare(`
    SELECT * FROM crypto_payments 
    WHERE user_id = ?
    ORDER BY created_at DESC
    LIMIT ?
  `).all(userId, limit);
}

// ==================== Crypto Withdrawal Functions ====================

function recordCryptoWithdrawal(userId, withdrawalData) {
  return db.prepare(`
    INSERT INTO crypto_withdrawals 
    (withdrawal_id, user_id, amount_gp, amount_usd, currency, address, status, metadata)
    VALUES (?, ?, ?, ?, ?, ?, 'pending', ?)
  `).run(
    withdrawalData.withdrawal_id,
    userId,
    withdrawalData.amount_gp,
    withdrawalData.amount_usd,
    withdrawalData.currency || 'USDT',
    withdrawalData.address,
    JSON.stringify(withdrawalData.metadata || {})
  );
}

function updateCryptoWithdrawalStatus(withdrawalId, status, txnHash = null, processedAt = null) {
  const timestamp = processedAt || (status === 'completed' ? Math.floor(Date.now() / 1000) : null);
  return db.prepare(`
    UPDATE crypto_withdrawals 
    SET status = ?, txn_hash = ?, processed_at = ?
    WHERE withdrawal_id = ?
  `).run(status, txnHash, timestamp, withdrawalId);
}

function getCryptoWithdrawal(withdrawalId) {
  return db.prepare('SELECT * FROM crypto_withdrawals WHERE withdrawal_id = ?').get(withdrawalId);
}

function getPendingCryptoWithdrawals() {
  return db.prepare(`
    SELECT * FROM crypto_withdrawals 
    WHERE status = 'pending'
    ORDER BY created_at ASC
  `).all();
}

function getUserCryptoWithdrawals(userId, limit = 10) {
  return db.prepare(`
    SELECT * FROM crypto_withdrawals 
    WHERE user_id = ?
    ORDER BY created_at DESC
    LIMIT ?
  `).all(userId, limit);
}

// ==================== Comprehensive User Data Functions ====================

function getUserComprehensiveData(userId) {
  const user = getOrCreateUser(userId);
  
  // Get all linked RSNs
  const rsns = db.prepare('SELECT rsn, linked_at FROM user_rsns WHERE user_id = ? ORDER BY linked_at DESC').all(userId);
  
  // Get bet history stats
  const betStats = db.prepare(`
    SELECT 
      COUNT(*) as total_bets,
      SUM(amount) as total_wagered,
      SUM(CASE WHEN outcome = 'win' THEN payout ELSE 0 END) as total_won,
      SUM(CASE WHEN outcome = 'loss' THEN amount ELSE 0 END) as total_lost,
      SUM(CASE WHEN outcome = 'win' THEN payout ELSE -amount END) as net_profit
    FROM bet_history
    WHERE user_id = ?
  `).get(userId) || { total_bets: 0, total_wagered: 0, total_won: 0, total_lost: 0, net_profit: 0 };
  
  // Get recent bets
  const recentBets = db.prepare(`
    SELECT * FROM bet_history 
    WHERE user_id = ? 
    ORDER BY created_at DESC 
    LIMIT 20
  `).all(userId);
  
  // Get GP transaction history
  const deposits = db.prepare(`
    SELECT * FROM deposits 
    WHERE user_id = ? 
    ORDER BY created_at DESC 
    LIMIT 20
  `).all(userId);
  
  const withdrawals = db.prepare(`
    SELECT * FROM withdrawals 
    WHERE user_id = ? 
    ORDER BY created_at DESC 
    LIMIT 20
  `).all(userId);
  
  // Get crypto transactions
  const cryptoDeposits = db.prepare(`
    SELECT * FROM crypto_payments 
    WHERE user_id = ? 
    ORDER BY created_at DESC 
    LIMIT 20
  `).all(userId);
  
  const cryptoWithdrawals = db.prepare(`
    SELECT * FROM crypto_withdrawals 
    WHERE user_id = ? 
    ORDER BY created_at DESC 
    LIMIT 20
  `).all(userId);
  
  // Get pending crypto transactions
  const pendingCryptoDeposits = db.prepare(`
    SELECT * FROM crypto_payments 
    WHERE user_id = ? AND status = 'pending'
    ORDER BY created_at DESC
  `).all(userId);
  
  const pendingCryptoWithdrawals = db.prepare(`
    SELECT * FROM crypto_withdrawals 
    WHERE user_id = ? AND status = 'pending'
    ORDER BY created_at DESC
  `).all(userId);
  
  return {
    user,
    rsns,
    betStats,
    recentBets,
    deposits,
    withdrawals,
    cryptoDeposits,
    cryptoWithdrawals,
    pendingCryptoDeposits,
    pendingCryptoWithdrawals
  };
}

function searchUserByIdentifier(identifier) {
  // Try as Discord ID first
  let user = db.prepare('SELECT * FROM users WHERE id = ?').get(identifier);
  if (user) return { user, foundBy: 'discord_id' };
  
  // Try as Discord username (case-insensitive partial match)
  user = db.prepare(`
    SELECT * FROM users 
    WHERE LOWER(display_name) LIKE LOWER(?)
    LIMIT 1
  `).get(`%${identifier}%`);
  if (user) return { user, foundBy: 'username' };
  
  // Try as RSN
  const rsnResult = db.prepare(`
    SELECT u.* FROM users u
    JOIN user_rsns ur ON ur.user_id = u.id
    WHERE LOWER(ur.rsn) = LOWER(?)
    LIMIT 1
  `).get(identifier);
  if (rsnResult) return { user: rsnResult, foundBy: 'rsn' };
  
  return null;
}

// Export functions
module.exports = {
  getDb,
  getSettings,
  updateSettings,
  getOrCreateUser,
  adjustBalance,
  recordBetHistory,
  linkRsn,
  findUserByRsn,
  getUserComprehensiveData,
  searchUserByIdentifier,
  getStats,
  recordWinnerSide,
  saveRoundMeta,
  addRakeback,
  claimRakeback,
  recordCryptoPayment,
  updateCryptoPaymentStatus,
  getCryptoPayment,
  getPendingCryptoPayments,
  getUserCryptoPayments,
  recordCryptoWithdrawal,
  updateCryptoWithdrawalStatus,
  getCryptoWithdrawal,
  getPendingCryptoWithdrawals,
  getUserCryptoWithdrawals,
};
