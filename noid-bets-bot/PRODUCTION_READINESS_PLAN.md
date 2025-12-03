# NoidBets Production Readiness & Scaling Plan

## 🔍 Current System Analysis

### What You Have (Working Great ✅)
- ✅ Automated duel arena betting system
- ✅ Discord bot with slash commands
- ✅ In-game GP deposit/withdrawal via VitaLite plugin
- ✅ SQLite database (100x faster than old JSON)
- ✅ Crypto deposits via NOWPayments
- ✅ Rakeback system
- ✅ Basic admin commands
- ✅ Win/loss tracking
- ✅ User P/L graphing

### Critical Gaps for Scale (❌)
1. **No structured logging system** - Only console.log/console.error
2. **No error monitoring** - Crashes go unnoticed
3. **No player support system** - Can't track issues
4. **No transaction audit logs** - Hard to investigate disputes
5. **No rate limiting** - Vulnerable to spam/abuse
6. **No backup automation** - Manual database backups only
7. **No monitoring/alerts** - Can't detect downtime
8. **No analytics dashboard** - Can't see business metrics
9. **Limited admin tools** - Hard to manage at scale
10. **No fraud detection** - Vulnerable to multi-accounting

---

## 🚨 CRITICAL PRIORITIES (Do These First)

### 1. Professional Logging System (HIGH PRIORITY)
**Problem**: `console.log` everywhere, no log retention, can't investigate issues

**Solution**: Winston + Log Rotation + Log Levels

```javascript
// utils/logger.js
const winston = require('winston');
require('winston-daily-rotate-file');

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.errors({ stack: true }),
    winston.format.json()
  ),
  transports: [
    // Rotate logs daily, keep 30 days
    new winston.transports.DailyRotateFile({
      filename: 'logs/error-%DATE%.log',
      datePattern: 'YYYY-MM-DD',
      level: 'error',
      maxFiles: '30d'
    }),
    new winston.transports.DailyRotateFile({
      filename: 'logs/combined-%DATE%.log',
      datePattern: 'YYYY-MM-DD',
      maxFiles: '30d'
    }),
    // Also log to console in dev
    new winston.transports.Console({
      format: winston.format.simple()
    })
  ]
});

module.exports = logger;
```

**What to Log**:
```javascript
// Transaction logs (CRITICAL for disputes)
logger.info('deposit', {
  userId: user.id,
  rsn: rsn,
  amount: amount,
  balanceBefore: oldBalance,
  balanceAfter: newBalance,
  timestamp: Date.now(),
  ip: req.ip
});

// Bet logs
logger.info('bet_placed', {
  userId, roundId, side, amount, balanceBefore, balanceAfter
});

// Withdrawal logs
logger.info('withdrawal_request', {
  userId, rsn, amount, status, reason
});

// Error logs with context
logger.error('withdrawal_failed', {
  userId, amount, error: err.message, stack: err.stack
});
```

**NPM Install**: `npm install winston winston-daily-rotate-file`

---

### 2. Transaction Audit System (HIGH PRIORITY)
**Problem**: Can't investigate disputes - no full transaction history

**Solution**: Dedicated audit_log table + detailed logging

```sql
-- Add to database schema
CREATE TABLE IF NOT EXISTS audit_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  timestamp INTEGER DEFAULT (unixepoch()),
  user_id TEXT NOT NULL,
  action TEXT NOT NULL,  -- 'deposit', 'withdrawal', 'bet', 'admin_adjust', etc.
  amount INTEGER,
  balance_before INTEGER,
  balance_after INTEGER,
  metadata TEXT,  -- JSON: { rsn, roundId, adminId, reason, ip, etc. }
  ip_address TEXT,
  user_agent TEXT
);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp);
```

**Benefits**:
- ✅ Full transaction history for investigations
- ✅ Can prove/disprove player claims
- ✅ Detect suspicious patterns
- ✅ Regulatory compliance (if needed)

---

### 3. Error Monitoring & Alerts (HIGH PRIORITY)
**Problem**: Bot crashes and you don't know

**Solution**: Sentry for error tracking + Discord webhooks for alerts

```javascript
// index.js - Add at top
const Sentry = require('@sentry/node');

Sentry.init({
  dsn: process.env.SENTRY_DSN,
  environment: process.env.NODE_ENV || 'production',
  tracesSampleRate: 0.1,
});

// Wrap critical operations
try {
  // ... withdrawal logic
} catch (error) {
  Sentry.captureException(error, {
    tags: { operation: 'withdrawal' },
    user: { id: userId },
    extra: { amount, rsn }
  });
  logger.error('withdrawal_exception', { error, userId, amount });
}

// Alert to Discord on critical errors
async function alertAdmin(title, description, severity = 'error') {
  const webhook = process.env.ADMIN_ALERT_WEBHOOK;
  if (!webhook) return;
  
  const color = severity === 'critical' ? 0xFF0000 : 0xFFA500;
  await fetch(webhook, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      embeds: [{
        title: `🚨 ${title}`,
        description,
        color,
        timestamp: new Date().toISOString()
      }]
    })
  });
}
```

**NPM Install**: `npm install @sentry/node`
**Setup**: Create free account at sentry.io

---

### 4. Rate Limiting & Abuse Prevention (HIGH PRIORITY)
**Problem**: No limits - users can spam commands, drain resources

**Solution**: Rate limit per user + IP-based limits

```javascript
const rateLimit = require('express-rate-limit');

// API endpoints
const apiLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 20, // 20 requests per minute
  message: { error: 'Too many requests, please slow down' }
});

app.use('/api/', apiLimiter);

// Stricter limit for sensitive operations
const withdrawalLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 5, // Only 5 withdrawals per minute per IP
  message: { error: 'Too many withdrawal requests' }
});

app.post('/api/bank/withdraw', withdrawalLimiter, async (req, res) => {
  // ... existing code
});

// In-memory cooldown for Discord commands
const commandCooldowns = new Map();

function checkCooldown(userId, commandName, cooldownMs) {
  const key = `${userId}:${commandName}`;
  const now = Date.now();
  const lastUsed = commandCooldowns.get(key);
  
  if (lastUsed && (now - lastUsed) < cooldownMs) {
    return { allowed: false, remaining: cooldownMs - (now - lastUsed) };
  }
  
  commandCooldowns.set(key, now);
  return { allowed: true };
}

// Usage in /bet command
const cooldown = checkCooldown(interaction.user.id, 'bet', 5000); // 5 sec cooldown
if (!cooldown.allowed) {
  return interaction.reply({
    content: `Slow down! Wait ${Math.ceil(cooldown.remaining / 1000)}s`,
    ephemeral: true
  });
}
```

**NPM Install**: `npm install express-rate-limit`

---

### 5. Automated Database Backups (HIGH PRIORITY)
**Problem**: Database corruption = all data lost

**Solution**: Automated hourly backups + off-site storage

```javascript
// utils/backup.js
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const BACKUP_DIR = path.join(__dirname, '..', '..', 'backups');
const DB_PATH = path.join(__dirname, '..', '..', 'users', 'noidbets.db');

function createBackup() {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const backupPath = path.join(BACKUP_DIR, `noidbets-${timestamp}.db`);
  
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
  
  // SQLite backup command
  exec(`sqlite3 ${DB_PATH} ".backup '${backupPath}'"`, (error) => {
    if (error) {
      logger.error('backup_failed', { error: error.message });
      return;
    }
    
    logger.info('backup_created', { path: backupPath });
    
    // Delete backups older than 7 days
    cleanOldBackups();
  });
}

function cleanOldBackups() {
  const files = fs.readdirSync(BACKUP_DIR);
  const now = Date.now();
  const sevenDays = 7 * 24 * 60 * 60 * 1000;
  
  files.forEach(file => {
    const filePath = path.join(BACKUP_DIR, file);
    const stats = fs.statSync(filePath);
    if (now - stats.mtimeMs > sevenDays) {
      fs.unlinkSync(filePath);
      logger.info('backup_deleted', { file });
    }
  });
}

// Run backup every hour
setInterval(createBackup, 60 * 60 * 1000);
createBackup(); // Initial backup on start

module.exports = { createBackup };
```

**Add to index.js**:
```javascript
require('./utils/backup');
```

---

## 🛠️ MEDIUM PRIORITY IMPROVEMENTS

### 6. Player Support System
**Problem**: Players have issues, no way to track/resolve them

**Solution**: Ticketing system + support commands

```javascript
// New table
CREATE TABLE IF NOT EXISTS support_tickets (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL,
  status TEXT DEFAULT 'open',  -- 'open', 'in_progress', 'resolved', 'closed'
  category TEXT,  -- 'deposit', 'withdrawal', 'bet_dispute', 'other'
  description TEXT,
  created_at INTEGER DEFAULT (unixepoch()),
  resolved_at INTEGER,
  resolved_by TEXT,  -- Admin user ID
  notes TEXT  -- Admin notes/resolution details
);

// New command
new SlashCommandBuilder()
  .setName('support')
  .setDescription('Create a support ticket')
  .addStringOption(o =>
    o.setName('issue')
      .setDescription('Describe your issue')
      .setRequired(true)
  )
  .addStringOption(o =>
    o.setName('category')
      .setDescription('Issue category')
      .setRequired(true)
      .addChoices(
        { name: 'Deposit Issue', value: 'deposit' },
        { name: 'Withdrawal Issue', value: 'withdrawal' },
        { name: 'Bet Dispute', value: 'bet_dispute' },
        { name: 'Other', value: 'other' }
      )
  ),

// Admin command
new SlashCommandBuilder()
  .setName('admin-tickets')
  .setDescription('Admin: View/manage support tickets')
  .addSubcommand(sc =>
    sc.setName('list')
      .setDescription('List open tickets')
  )
  .addSubcommand(sc =>
    sc.setName('resolve')
      .setDescription('Resolve a ticket')
      .addIntegerOption(o =>
        o.setName('ticket_id')
          .setDescription('Ticket ID to resolve')
          .setRequired(true)
      )
      .addStringOption(o =>
        o.setName('notes')
          .setDescription('Resolution notes')
          .setRequired(true)
      )
  ),
```

---

### 7. Fraud Detection System
**Problem**: Multi-accounting, collusion, suspicious behavior

**Solution**: Pattern detection + flagging

```javascript
// Detection patterns
async function checkSuspiciousActivity(userId) {
  const flags = [];
  
  // Check 1: Multiple accounts from same IP
  const recentIps = await db.prepare(`
    SELECT ip_address, COUNT(DISTINCT user_id) as user_count
    FROM audit_log
    WHERE user_id = ? AND timestamp > ?
    GROUP BY ip_address
    HAVING user_count > 1
  `).all(userId, Date.now() - 24*60*60*1000);
  
  if (recentIps.length > 0) {
    flags.push('multi_account_same_ip');
  }
  
  // Check 2: Rapid win rate (possible bet manipulation)
  const recentBets = await db.prepare(`
    SELECT outcome, COUNT(*) as count
    FROM bet_history
    WHERE user_id = ? AND created_at > ?
    GROUP BY outcome
  `).all(userId, Date.now() - 24*60*60*1000);
  
  const wins = recentBets.find(r => r.outcome === 'win')?.count || 0;
  const total = recentBets.reduce((sum, r) => sum + r.count, 0);
  
  if (total > 10 && (wins / total) > 0.90) {
    flags.push('suspicious_win_rate');
  }
  
  // Check 3: Bet pattern matching (always bets with another user)
  // TODO: Implement collusion detection
  
  return flags;
}

// Run after each bet
const flags = await checkSuspiciousActivity(userId);
if (flags.length > 0) {
  logger.warn('suspicious_activity', { userId, flags });
  await alertAdmin('Suspicious Activity Detected', 
    `User <@${userId}> flagged for: ${flags.join(', ')}`);
}
```

---

### 8. Analytics Dashboard
**Problem**: No visibility into business metrics

**Solution**: Admin analytics commands

```javascript
new SlashCommandBuilder()
  .setName('admin-analytics')
  .setDescription('Admin: View system analytics')
  .addSubcommand(sc =>
    sc.setName('overview')
      .setDescription('System overview (users, volume, etc.)')
  )
  .addSubcommand(sc =>
    sc.setName('revenue')
      .setDescription('Revenue metrics (house edge, rake, etc.)')
  )
  .addSubcommand(sc =>
    sc.setName('top_players')
      .setDescription('Top players by volume/profit')
  ),

// Implementation
async function getAnalytics() {
  const stats = {
    totalUsers: await db.prepare('SELECT COUNT(*) as count FROM users').get().count,
    activeUsers24h: await db.prepare(`
      SELECT COUNT(DISTINCT user_id) as count
      FROM bet_history
      WHERE created_at > ?
    `).get(Date.now() - 24*60*60*1000).count,
    
    totalVolume24h: await db.prepare(`
      SELECT SUM(amount) as total
      FROM bet_history
      WHERE created_at > ?
    `).get(Date.now() - 24*60*60*1000).total,
    
    houseProfit24h: await db.prepare(`
      SELECT SUM(CASE 
        WHEN outcome = 'win' THEN -(payout - amount)
        WHEN outcome = 'loss' THEN amount
        ELSE 0
      END) as profit
      FROM bet_history
      WHERE created_at > ?
    `).get(Date.now() - 24*60*60*1000).profit,
    
    avgBet24h: await db.prepare(`
      SELECT AVG(amount) as avg
      FROM bet_history
      WHERE created_at > ?
    `).get(Date.now() - 24*60*60*1000).avg,
  };
  
  return stats;
}
```

---

### 9. Enhanced Admin Tools
**Current**: Only `/admin-add-gp` and `/graph`

**Add These**:

```javascript
// View user details
new SlashCommandBuilder()
  .setName('admin-user')
  .setDescription('Admin: View detailed user information')
  .addUserOption(o =>
    o.setName('user').setDescription('User to inspect').setRequired(true)
  ),

// Ban/suspend user
new SlashCommandBuilder()
  .setName('admin-ban')
  .setDescription('Admin: Ban a user from betting')
  .addUserOption(o =>
    o.setName('user').setRequired(true)
  )
  .addStringOption(o =>
    o.setName('reason').setRequired(true)
  ),

// Refund a bet
new SlashCommandBuilder()
  .setName('admin-refund')
  .setDescription('Admin: Refund a specific bet')
  .addStringOption(o =>
    o.setName('round_id').setRequired(true)
  )
  .addUserOption(o =>
    o.setName('user').setRequired(true)
  ),

// View system health
new SlashCommandBuilder()
  .setName('admin-health')
  .setDescription('Admin: Check system health'),

// Manual withdrawal processing (for stuck withdrawals)
new SlashCommandBuilder()
  .setName('admin-process-withdrawal')
  .setDescription('Admin: Manually process a stuck withdrawal')
  .addUserOption(o =>
    o.setName('user').setRequired(true)
  )
  .addStringOption(o =>
    o.setName('amount').setRequired(true)
  ),
```

---

### 10. System Monitoring
**Problem**: Can't detect issues before players complain

**Solution**: Health checks + uptime monitoring

```javascript
// Health check endpoint
app.get('/health', async (req, res) => {
  try {
    // Check database
    const dbCheck = db.prepare('SELECT 1').get();
    
    // Check Discord bot
    const botConnected = client.ws.status === 0; // 0 = READY
    
    // Check recent errors
    const recentErrors = await db.prepare(`
      SELECT COUNT(*) as count
      FROM audit_log
      WHERE action = 'error' AND timestamp > ?
    `).get(Date.now() - 5*60*1000).count;
    
    const healthy = dbCheck && botConnected && (recentErrors < 10);
    
    res.status(healthy ? 200 : 503).json({
      status: healthy ? 'healthy' : 'unhealthy',
      timestamp: Date.now(),
      checks: {
        database: !!dbCheck,
        discordBot: botConnected,
        errorRate: recentErrors
      }
    });
  } catch (error) {
    res.status(503).json({ status: 'unhealthy', error: error.message });
  }
});

// Monitor with external service (uptimerobot.com or better stack)
// Set up alerts to ping Discord webhook on downtime
```

---

## 📊 RECOMMENDED TECH STACK ADDITIONS

```json
{
  "dependencies": {
    "winston": "^3.11.0",
    "winston-daily-rotate-file": "^4.7.1",
    "@sentry/node": "^7.91.0",
    "express-rate-limit": "^7.1.5",
    "dotenv": "^16.3.1"
  }
}
```

---

## 🎯 IMPLEMENTATION TIMELINE

### Week 1: Critical Foundation
- ✅ Set up Winston logging
- ✅ Add audit_log table
- ✅ Implement Sentry error tracking
- ✅ Add rate limiting
- ✅ Set up automated backups

### Week 2: Player Support
- ✅ Build support ticket system
- ✅ Add admin ticket management
- ✅ Create player history viewer

### Week 3: Fraud & Security
- ✅ Implement fraud detection patterns
- ✅ Add IP tracking
- ✅ Build ban/suspension system

### Week 4: Analytics & Monitoring
- ✅ Build analytics dashboard
- ✅ Add health check endpoint
- ✅ Set up external monitoring
- ✅ Enhanced admin tools

---

## 🔥 QUICK WINS (Do These Today)

1. **Add .env validation**:
```javascript
const required = ['DISCORD_TOKEN', 'CLIENT_ID', 'PORT'];
required.forEach(key => {
  if (!process.env[key]) throw new Error(`Missing ${key} in .env`);
});
```

2. **Add process crash handler**:
```javascript
process.on('uncaughtException', (error) => {
  logger.error('uncaught_exception', { error });
  alertAdmin('Bot Crash', `Uncaught exception: ${error.message}`, 'critical');
  process.exit(1);
});
```

3. **Add startup health check**:
```javascript
async function startupChecks() {
  try {
    await db.prepare('SELECT 1').get();
    logger.info('startup_check_db_ok');
  } catch (e) {
    logger.error('startup_check_db_failed', { error: e });
    process.exit(1);
  }
}
```

4. **Add request logging**:
```javascript
app.use((req, res, next) => {
  logger.info('http_request', {
    method: req.method,
    path: req.path,
    ip: req.ip,
    userAgent: req.get('user-agent')
  });
  next();
});
```

---

## 📈 SCALING CHECKLIST

When you hit these milestones, upgrade:

### 100 Users
- ✅ Logging system
- ✅ Audit logs
- ✅ Backups

### 500 Users
- ✅ Fraud detection
- ✅ Support system
- ✅ Analytics dashboard

### 1,000 Users
- 🔄 Consider PostgreSQL (better concurrent writes)
- 🔄 Redis for caching
- 🔄 Load balancer

### 5,000+ Users
- 🔄 Microservices architecture
- 🔄 Dedicated database server
- 🔄 CDN for static assets
- 🔄 Auto-scaling infrastructure

---

## 🚀 READY TO IMPLEMENT?

Start with the **5 Critical Priorities** above. They will:
- ✅ Prevent data loss
- ✅ Let you investigate issues
- ✅ Detect problems before players notice
- ✅ Protect against abuse
- ✅ Give you peace of mind

**Estimated time**: 2-3 days for all critical priorities
**Impact**: Production-ready system capable of handling 1000+ users

Want me to implement any of these? I can start with the logging system right now!
