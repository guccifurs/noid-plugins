// Simple JSON-backed database for the betting bot.
// Data file path: /users/Datbase.json (relative to project root).
// This stores:
// - settings (betting/results channels, logo URL, admin role)
// - users (balances, linked RSNs, history)
// - stats (red/blue streaks)
// - current round metadata (optional)

const fs = require('fs');
const path = require('path');

const DATA_DIR = path.join(__dirname, '..', '..', 'users');
const DATA_FILE = path.join(DATA_DIR, 'Datbase.json');

const DEFAULT_ADMIN_ROLE_ID = '1440421585281355896';
const DEFAULT_BETTING_CHANNEL_ID = '1440416961098944653';
const DEFAULT_RESULTS_CHANNEL_ID = '1440416910197002321';

let db = null;

function ensureLoaded() {
  if (db) return db;

  try {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  } catch (e) {
    console.error('Failed to ensure data directory exists:', e);
  }

  if (fs.existsSync(DATA_FILE)) {
    try {
      const raw = fs.readFileSync(DATA_FILE, 'utf8');
      db = JSON.parse(raw);
    } catch (e) {
      console.error('Failed to read/parse Datbase.json, starting fresh:', e);
      db = null;
    }
  }

  if (!db || typeof db !== 'object') {
    db = {
      settings: {
        bettingChannelId: DEFAULT_BETTING_CHANNEL_ID,
        resultsChannelId: DEFAULT_RESULTS_CHANNEL_ID,
        panelMessageId: null,
        panelThumbnailUrl: null,
        logoUrl: null,
        adminRoleId: DEFAULT_ADMIN_ROLE_ID,
      },
      users: {},
      stats: {
        redStreak: 0,
        blueStreak: 0,
        lastWinner: null, // 'red' | 'blue' | null
        lastWinners: [], // array of recent winners ('red' | 'blue'), capped at 50
      },
      round: null, // will hold last known round metadata (optional)
    };
    flush();
  }

  return db;
}

function flush() {
  if (!db) return;
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2), 'utf8');
  } catch (e) {
    console.error('Failed to write Datbase.json:', e);
  }
}

function getDb() {
  return ensureLoaded();
}

function getSettings() {
  const d = ensureLoaded();
  return d.settings;
}

function updateSettings(patch) {
  const d = ensureLoaded();
  d.settings = { ...d.settings, ...patch };
  flush();
  return d.settings;
}

function getOrCreateUser(userId, displayName) {
  const d = ensureLoaded();
  if (!d.users[userId]) {
    d.users[userId] = {
      id: userId,
      displayName: displayName || null,
      linkedRsns: [],
      balance: 0,
      wins: 0,
      losses: 0,
      betHistory: [],
      deposits: [],
      withdrawals: [],
      rakebackUnclaimed: 0,
    };
  } else if (displayName && d.users[userId].displayName !== displayName) {
    d.users[userId].displayName = displayName;
  }
  if (typeof d.users[userId].rakebackUnclaimed !== 'number') {
    d.users[userId].rakebackUnclaimed = 0;
  }
  return d.users[userId];
}

function addRakeback(userId, amount) {
  if (!amount || amount <= 0) return 0;
  const user = getOrCreateUser(userId);
  user.rakebackUnclaimed = (user.rakebackUnclaimed || 0) + amount;
  flush();
  return user.rakebackUnclaimed;
}

function claimRakeback(userId, displayName) {
  const user = getOrCreateUser(userId, displayName);
  const toClaim = user.rakebackUnclaimed || 0;
  if (toClaim <= 0) {
    return { claimed: 0, newBalance: user.balance || 0 };
  }
  user.rakebackUnclaimed = 0;
  const newBalance = adjustBalance(userId, toClaim, {
    displayName,
    reason: 'rakeback-claim',
  });
  flush();
  return { claimed: toClaim, newBalance };
}

function adjustBalance(userId, amount, opts = {}) {
  const user = getOrCreateUser(userId, opts.displayName);
  user.balance = Math.max(0, (user.balance || 0) + amount);
  if (amount > 0) {
    user.deposits.push({ amount, reason: opts.reason || 'adjust', ts: Date.now() });
  } else if (amount < 0) {
    user.withdrawals.push({ amount: -amount, reason: opts.reason || 'adjust', ts: Date.now() });
  }
  flush();
  return user.balance;
}

function recordBetHistory(userId, entry) {
  const user = getOrCreateUser(userId, entry.displayName);
  user.betHistory.push({ ...entry, ts: Date.now() });
  flush();
}

function linkRsn(userId, rsn) {
  const user = getOrCreateUser(userId);
  if (!user.linkedRsns.includes(rsn)) {
    user.linkedRsns.push(rsn);
  }
  flush();
}

function findUserByRsn(rsn) {
  if (!rsn) return null;
  const d = ensureLoaded();
  const target = String(rsn).trim().toLowerCase();
  if (!target) return null;

  const users = d.users || {};
  for (const userId of Object.keys(users)) {
    const user = users[userId];
    if (!user) continue;
    const linked = Array.isArray(user.linkedRsns) ? user.linkedRsns : [];
    const match = linked.some(name =>
      typeof name === 'string' && name.trim().toLowerCase() === target
    );
    if (match) {
      return user;
    }
  }
  return null;
}

function getStats() {
  const d = ensureLoaded();
  return d.stats;
}

function recordWinnerSide(side) {
  const d = ensureLoaded();
  if (!d.stats || typeof d.stats !== 'object') {
    d.stats = {
      redStreak: 0,
      blueStreak: 0,
      lastWinner: null,
      lastWinners: [],
    };
  }
  if (!Array.isArray(d.stats.lastWinners)) {
    d.stats.lastWinners = [];
  }
  if (side !== 'red' && side !== 'blue') {
    d.stats.lastWinner = null;
    d.stats.redStreak = 0;
    d.stats.blueStreak = 0;
  } else {
    if (d.stats.lastWinner === side) {
      if (side === 'red') d.stats.redStreak += 1;
      else d.stats.blueStreak += 1;
    } else {
      d.stats.lastWinner = side;
      if (side === 'red') {
        d.stats.redStreak = 1;
        d.stats.blueStreak = 0;
      } else {
        d.stats.blueStreak = 1;
        d.stats.redStreak = 0;
      }
    }

    // Maintain last 50 winners history
    d.stats.lastWinners.push(side);
    if (d.stats.lastWinners.length > 50) {
      d.stats.lastWinners.splice(0, d.stats.lastWinners.length - 50);
    }
  }
  flush();
}

function saveRoundMeta(roundMeta) {
  const d = ensureLoaded();
  d.round = roundMeta ? { ...roundMeta } : null;
  flush();
}

module.exports = {
  getDb,
  getSettings,
  updateSettings,
  getOrCreateUser,
  adjustBalance,
  recordBetHistory,
  linkRsn,
  findUserByRsn,
  getStats,
  recordWinnerSide,
  saveRoundMeta,
  addRakeback,
  claimRakeback,
};
