# Crypto Deposit/Withdrawal Implementation Plan

## 🔍 Current State Analysis

### ✅ What You Have (Deposits Only)
- `/deposit_usdt` command creates NOWPayments payment
- IPN endpoint receives payment confirmations
- Auto-credits GP balance on successful payment
- Discord DM notification on deposit
- GP conversion: 1M GP = $0.15 → **$1 = 6.67M GP**

### ❌ Critical Gaps

#### **DEPOSITS (Incomplete)**
1. ❌ **No IPN signature verification** - Anyone can fake payment confirmations
2. ❌ **No payment tracking** - Can't see pending/failed payments
3. ❌ **No minimum deposit** - Can spam with $0.01 deposits
4. ❌ **No duplicate protection** - Same payment could be credited twice
5. ❌ **No failed payment handling** - Users don't know when payment expires
6. ❌ **Missing env variables** - NOWPAYMENTS_API_KEY and NOWPAYMENTS_IPN_URL not configured
7. ❌ **No database logging** - Can't investigate crypto disputes
8. ❌ **Limited currencies** - Only USDT supported

#### **WITHDRAWALS (Not Implemented)**
1. ❌ **No crypto withdrawal command** - Users can't cash out
2. ❌ **No payout API integration** - Can't send crypto to users
3. ❌ **No withdrawal limits** - Need min/max amounts
4. ❌ **No withdrawal queue** - Manual processing required
5. ❌ **No fee system** - Who pays blockchain fees?

---

## 🚨 PHASE 1: Fix Critical Deposit Issues (2-3 hours)

### 1.1 Add Missing Environment Variables

**Add to `.env`**:
```bash
# NOWPayments API Configuration
NOWPAYMENTS_API_KEY=your_api_key_here
NOWPAYMENTS_IPN_SECRET=your_ipn_secret_here  # NEW - for signature verification
NOWPAYMENTS_IPN_URL=https://your-domain.com/api/crypto/nowpayments-ipn

# Crypto Deposit Settings
MIN_DEPOSIT_USD=5.00
MAX_DEPOSIT_USD=10000.00
```

**How to get credentials**:
1. Sign up at https://nowpayments.io/
2. Go to Settings → API Keys
3. Generate API Key and IPN Secret Key
4. Save them securely

---

### 1.2 Add Crypto Payments Table (SQLite)

```sql
-- Track all crypto payment attempts
CREATE TABLE IF NOT EXISTS crypto_payments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  payment_id TEXT UNIQUE,  -- NOWPayments payment ID
  user_id TEXT NOT NULL,
  amount_usd REAL NOT NULL,
  amount_gp INTEGER NOT NULL,
  pay_currency TEXT,  -- 'usdt', 'btc', etc.
  pay_address TEXT,
  pay_amount REAL,
  status TEXT DEFAULT 'pending',  -- 'pending', 'confirming', 'finished', 'failed', 'expired', 'refunded'
  created_at INTEGER DEFAULT (unixepoch()),
  confirmed_at INTEGER,
  metadata TEXT,  -- JSON: { payment_status, actually_paid, txn_id, etc. }
  FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_crypto_payment_id ON crypto_payments(payment_id);
CREATE INDEX IF NOT EXISTS idx_crypto_user ON crypto_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_crypto_status ON crypto_payments(status);
```

**Add to database.js**:
```javascript
// Add schema to initializeSchema()
db.exec(`
  CREATE TABLE IF NOT EXISTS crypto_payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    payment_id TEXT UNIQUE,
    user_id TEXT NOT NULL,
    amount_usd REAL NOT NULL,
    amount_gp INTEGER NOT NULL,
    pay_currency TEXT,
    pay_address TEXT,
    pay_amount REAL,
    status TEXT DEFAULT 'pending',
    created_at INTEGER DEFAULT (unixepoch()),
    confirmed_at INTEGER,
    metadata TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id)
  );
  CREATE INDEX IF NOT EXISTS idx_crypto_payment_id ON crypto_payments(payment_id);
  CREATE INDEX IF NOT EXISTS idx_crypto_user ON crypto_payments(user_id);
  CREATE INDEX IF NOT EXISTS idx_crypto_status ON crypto_payments(status);
`);

// Helper functions
function recordCryptoPayment(userId, paymentData) {
  return db.prepare(`
    INSERT INTO crypto_payments 
    (payment_id, user_id, amount_usd, amount_gp, pay_currency, pay_address, pay_amount, status, metadata)
    VALUES (?, ?, ?, ?, ?, ?, ?, 'pending', ?)
  `).run(
    paymentData.payment_id,
    userId,
    paymentData.price_amount,
    paymentData.gp_amount,
    paymentData.pay_currency,
    paymentData.pay_address,
    paymentData.pay_amount,
    JSON.stringify(paymentData)
  );
}

function updateCryptoPaymentStatus(paymentId, status, metadata = {}) {
  const timestamp = status === 'finished' ? Date.now() / 1000 : null;
  return db.prepare(`
    UPDATE crypto_payments 
    SET status = ?, confirmed_at = ?, metadata = ?
    WHERE payment_id = ?
  `).run(status, timestamp, JSON.stringify(metadata), paymentId);
}

function getCryptoPayment(paymentId) {
  return db.prepare('SELECT * FROM crypto_payments WHERE payment_id = ?').get(paymentId);
}

module.exports = {
  // ... existing exports
  recordCryptoPayment,
  updateCryptoPaymentStatus,
  getCryptoPayment,
};
```

---

### 1.3 Implement IPN Signature Verification

**Critical Security Issue**: Currently, anyone can POST to your IPN endpoint and credit themselves GP!

```javascript
// utils/nowpayments.js
const crypto = require('crypto');

function verifyIPNSignature(req, ipnSecret) {
  const receivedSignature = req.get('x-nowpayments-sig');
  if (!receivedSignature) {
    return false;
  }
  
  // NOWPayments signs with HMAC-SHA512
  const payload = JSON.stringify(req.body);
  const expectedSignature = crypto
    .createHmac('sha512', ipnSecret)
    .update(payload)
    .digest('hex');
  
  return receivedSignature === expectedSignature;
}

module.exports = { verifyIPNSignature };
```

**Update IPN endpoint**:
```javascript
const { verifyIPNSignature } = require('./utils/nowpayments');
const { recordCryptoPayment, updateCryptoPaymentStatus, getCryptoPayment } = require('./utils/database');

const NOWPAYMENTS_IPN_SECRET = process.env.NOWPAYMENTS_IPN_SECRET;

app.post('/api/crypto/nowpayments-ipn', async (req, res) => {
  try {
    // CRITICAL: Verify signature first
    if (!verifyIPNSignature(req, NOWPAYMENTS_IPN_SECRET)) {
      logger.warn('crypto_ipn_invalid_signature', { ip: req.ip });
      return res.status(401).json({ error: 'Invalid signature' });
    }
    
    const body = req.body || {};
    const paymentId = body.payment_id;
    const paymentStatus = body.payment_status;
    const orderId = body.order_id;  // Discord user ID
    const priceAmount = Number(body.price_amount);
    
    logger.info('crypto_ipn_received', {
      paymentId,
      paymentStatus,
      userId: orderId,
      amount: priceAmount
    });
    
    // Check if already processed
    const existing = getCryptoPayment(paymentId);
    if (existing && existing.status === 'finished') {
      logger.warn('crypto_ipn_duplicate', { paymentId });
      return res.json({ status: 'already_processed' });
    }
    
    // Update payment status
    updateCryptoPaymentStatus(paymentId, paymentStatus, body);
    
    // Only credit on 'finished' status
    if (paymentStatus !== 'finished') {
      return res.json({ status: 'status_updated' });
    }
    
    const gpAmount = Math.floor(priceAmount * GP_PER_USD);
    const newBalance = adjustBalance(orderId, gpAmount, {
      reason: 'crypto-deposit',
    });
    
    // Log to audit table
    logger.info('crypto_deposit_completed', {
      userId: orderId,
      paymentId,
      usd: priceAmount,
      gp: gpAmount,
      newBalance
    });
    
    // Notify user
    try {
      const user = await client.users.fetch(orderId);
      await user.send(
        `✅ **Crypto deposit confirmed!**\n` +
        `Amount: $${priceAmount.toFixed(2)} → ${formatAmountFull(gpAmount)}\n` +
        `New balance: **${formatAmountFull(newBalance)}**`
      );
    } catch (dmErr) {
      logger.error('crypto_dm_failed', { userId: orderId, error: dmErr.message });
    }
    
    return res.json({ status: 'ok' });
  } catch (err) {
    logger.error('crypto_ipn_error', { error: err.message, stack: err.stack });
    return res.status(500).json({ error: 'Internal error' });
  }
});
```

---

### 1.4 Add Minimum Deposit Limit

```javascript
const MIN_DEPOSIT_USD = Number(process.env.MIN_DEPOSIT_USD || 5);
const MAX_DEPOSIT_USD = Number(process.env.MAX_DEPOSIT_USD || 10000);

// In /deposit_usdt command
if (amountUsd < MIN_DEPOSIT_USD) {
  await interaction.reply({
    content: `Minimum deposit is **$${MIN_DEPOSIT_USD.toFixed(2)}**.`,
    ephemeral: true
  });
  return;
}

if (amountUsd > MAX_DEPOSIT_USD) {
  await interaction.reply({
    content: `Maximum deposit is **$${MAX_DEPOSIT_USD.toFixed(2)}**. Contact admin for larger amounts.`,
    ephemeral: true
  });
  return;
}
```

---

### 1.5 Track Payment Creation

```javascript
// In /deposit_usdt command, after creating payment
try {
  const payment = await createNowpaymentsPayment({
    priceAmount: amountUsd,
    orderId: interaction.user.id,
  });
  
  // Record in database
  recordCryptoPayment(interaction.user.id, {
    payment_id: payment.payment_id,
    price_amount: amountUsd,
    gp_amount: estimatedGp,
    pay_currency: payment.pay_currency,
    pay_address: payment.pay_address,
    pay_amount: payment.pay_amount,
  });
  
  logger.info('crypto_payment_created', {
    userId: interaction.user.id,
    paymentId: payment.payment_id,
    usd: amountUsd,
    gp: estimatedGp
  });
  
  // ... rest of code
}
```

---

### 1.6 Add Payment Status Command

```javascript
new SlashCommandBuilder()
  .setName('deposit_status')
  .setDescription('Check status of your recent crypto deposits'),

// Handler
else if (interaction.commandName === 'deposit_status') {
  const payments = db.prepare(`
    SELECT * FROM crypto_payments
    WHERE user_id = ?
    ORDER BY created_at DESC
    LIMIT 5
  `).all(interaction.user.id);
  
  if (payments.length === 0) {
    await interaction.reply({
      content: 'You have no crypto deposit history.',
      ephemeral: true
    });
    return;
  }
  
  const statusEmoji = {
    'pending': '⏳',
    'confirming': '🔄',
    'finished': '✅',
    'failed': '❌',
    'expired': '⏰',
  };
  
  const fields = payments.map(p => ({
    name: `${statusEmoji[p.status] || '❓'} $${p.amount_usd.toFixed(2)} → ${formatAmountShort(p.amount_gp)}`,
    value: `Status: **${p.status}**\n` +
           `Date: <t:${p.created_at}:R>\n` +
           (p.status === 'pending' ? `Pay ${p.pay_amount} ${p.pay_currency.toUpperCase()} to:\n\`${p.pay_address}\`` : ''),
    inline: false
  }));
  
  const embed = new EmbedBuilder()
    .setTitle('💳 Recent Crypto Deposits')
    .addFields(fields)
    .setColor(0x00D1FF)
    .setTimestamp();
  
  await interaction.reply({ embeds: [embed], ephemeral: true });
}
```

---

## 🚀 PHASE 2: Implement Crypto Withdrawals (4-6 hours)

### 2.1 NOWPayments Payout API Integration

**Important**: NOWPayments has two systems:
1. **Payments API** (what you use for deposits) - customers pay YOU
2. **Payouts API** (needed for withdrawals) - YOU pay customers

**Setup Required**:
1. Upgrade to NOWPayments business account
2. Enable Payouts in dashboard
3. Fund your payout wallet with crypto
4. Get payout API credentials

**Add to .env**:
```bash
NOWPAYMENTS_PAYOUT_API_KEY=your_payout_api_key
MIN_WITHDRAWAL_USD=10.00
MAX_WITHDRAWAL_USD=5000.00
WITHDRAWAL_FEE_PERCENT=2.0  # 2% fee to cover blockchain fees + profit
```

---

### 2.2 Crypto Withdrawal Table

```sql
CREATE TABLE IF NOT EXISTS crypto_withdrawals (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  withdrawal_id TEXT UNIQUE,  -- Our internal ID
  payout_id TEXT,  -- NOWPayments payout ID
  user_id TEXT NOT NULL,
  amount_gp INTEGER NOT NULL,
  amount_usd REAL NOT NULL,
  fee_usd REAL NOT NULL,
  net_usd REAL NOT NULL,  -- amount_usd - fee_usd
  currency TEXT NOT NULL,  -- 'usdt', 'btc', etc.
  address TEXT NOT NULL,  -- User's crypto address
  status TEXT DEFAULT 'pending',  -- 'pending', 'processing', 'sent', 'failed', 'cancelled'
  created_at INTEGER DEFAULT (unixepoch()),
  processed_at INTEGER,
  txn_hash TEXT,
  metadata TEXT,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_withdrawal_id ON crypto_withdrawals(withdrawal_id);
CREATE INDEX IF NOT EXISTS idx_withdrawal_user ON crypto_withdrawals(user_id);
CREATE INDEX IF NOT EXISTS idx_withdrawal_status ON crypto_withdrawals(status);
```

---

### 2.3 Withdraw Command

```javascript
new SlashCommandBuilder()
  .setName('withdraw_usdt')
  .setDescription('Withdraw your GP balance to USDT')
  .addStringOption(o =>
    o.setName('amount')
      .setDescription('Amount in USD (e.g., 10, 50, 100)')
      .setRequired(true)
  )
  .addStringOption(o =>
    o.setName('address')
      .setDescription('Your USDT (TRC20) wallet address')
      .setRequired(true)
  ),

// Handler
else if (interaction.commandName === 'withdraw_usdt') {
  const amountUsdStr = interaction.options.getString('amount', true);
  const address = interaction.options.getString('address', true).trim();
  
  const amountUsd = parseFloat(amountUsdStr);
  
  if (!amountUsd || !Number.isFinite(amountUsd) || amountUsd <= 0) {
    await interaction.reply({
      content: 'Invalid amount. Specify USD value (e.g., 10, 50, 100).',
      ephemeral: true
    });
    return;
  }
  
  const MIN_WITHDRAWAL_USD = Number(process.env.MIN_WITHDRAWAL_USD || 10);
  const MAX_WITHDRAWAL_USD = Number(process.env.MAX_WITHDRAWAL_USD || 5000);
  
  if (amountUsd < MIN_WITHDRAWAL_USD || amountUsd > MAX_WITHDRAWAL_USD) {
    await interaction.reply({
      content: `Withdrawal must be between $${MIN_WITHDRAWAL_USD} and $${MAX_WITHDRAWAL_USD}.`,
      ephemeral: true
    });
    return;
  }
  
  // Validate USDT address (basic check - TRC20 starts with T)
  if (!address.startsWith('T') || address.length < 30) {
    await interaction.reply({
      content: 'Invalid USDT (TRC20) address. Must start with "T" and be at least 30 characters.',
      ephemeral: true
    });
    return;
  }
  
  const WITHDRAWAL_FEE_PERCENT = Number(process.env.WITHDRAWAL_FEE_PERCENT || 2.0);
  const feeUsd = amountUsd * (WITHDRAWAL_FEE_PERCENT / 100);
  const netUsd = amountUsd - feeUsd;
  const requiredGp = Math.ceil(amountUsd * GP_PER_USD);
  
  const user = getOrCreateUser(interaction.user.id, interaction.user.tag);
  
  if (user.balance < requiredGp) {
    await interaction.reply({
      content: `Insufficient balance. You need ${formatAmountFull(requiredGp)} but have ${formatAmountFull(user.balance)}.`,
      ephemeral: true
    });
    return;
  }
  
  // Show confirmation
  const embed = new EmbedBuilder()
    .setTitle('💸 Confirm Withdrawal')
    .setDescription('Please confirm the withdrawal details:')
    .addFields(
      { name: 'Amount', value: `$${amountUsd.toFixed(2)} (${formatAmountFull(requiredGp)})`, inline: false },
      { name: 'Fee', value: `$${feeUsd.toFixed(2)} (${WITHDRAWAL_FEE_PERCENT}%)`, inline: false },
      { name: 'You receive', value: `**$${netUsd.toFixed(2)} USDT**`, inline: false },
      { name: 'Address', value: `\`${address}\``, inline: false },
    )
    .setColor(0xFFA500)
    .setFooter({ text: 'Processing time: 5-30 minutes' });
  
  const row = new ActionRowBuilder()
    .addComponents(
      new ButtonBuilder()
        .setCustomId(`withdraw_confirm_${Date.now()}`)
        .setLabel('Confirm Withdrawal')
        .setStyle(ButtonStyle.Danger),
      new ButtonBuilder()
        .setCustomId(`withdraw_cancel_${Date.now()}`)
        .setLabel('Cancel')
        .setStyle(ButtonStyle.Secondary)
    );
  
  await interaction.reply({
    embeds: [embed],
    components: [row],
    ephemeral: true
  });
  
  // Store withdrawal data temporarily
  pendingWithdrawals.set(interaction.user.id, {
    amountUsd,
    amountGp: requiredGp,
    feeUsd,
    netUsd,
    address,
    currency: 'usdt'
  });
}
```

---

### 2.4 Withdrawal Confirmation Handler

```javascript
// Add to button interaction handler
if (interaction.customId.startsWith('withdraw_confirm_')) {
  const withdrawalData = pendingWithdrawals.get(interaction.user.id);
  if (!withdrawalData) {
    await interaction.update({
      content: 'Withdrawal expired. Please try again.',
      components: [],
      embeds: []
    });
    return;
  }
  
  pendingWithdrawals.delete(interaction.user.id);
  
  await interaction.update({
    content: '⏳ Processing withdrawal...',
    components: [],
    embeds: []
  });
  
  try {
    // Deduct balance immediately
    const newBalance = adjustBalance(interaction.user.id, -withdrawalData.amountGp, {
      displayName: interaction.user.tag,
      reason: 'crypto-withdrawal'
    });
    
    // Create withdrawal record
    const withdrawalId = `WD-${Date.now()}-${interaction.user.id.slice(-6)}`;
    db.prepare(`
      INSERT INTO crypto_withdrawals 
      (withdrawal_id, user_id, amount_gp, amount_usd, fee_usd, net_usd, currency, address, status)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending')
    `).run(
      withdrawalId,
      interaction.user.id,
      withdrawalData.amountGp,
      withdrawalData.amountUsd,
      withdrawalData.feeUsd,
      withdrawalData.netUsd,
      withdrawalData.currency,
      withdrawalData.address
    );
    
    logger.info('crypto_withdrawal_initiated', {
      withdrawalId,
      userId: interaction.user.id,
      amountUsd: withdrawalData.amountUsd,
      amountGp: withdrawalData.amountGp,
      address: withdrawalData.address
    });
    
    // Queue for admin approval (Phase 3) or auto-process (Phase 4)
    // For now, just notify user
    await interaction.followUp({
      content:
        `✅ **Withdrawal submitted!**\n` +
        `ID: \`${withdrawalId}\`\n` +
        `Amount: ${formatAmountFull(withdrawalData.amountGp)} → $${withdrawalData.netUsd.toFixed(2)} USDT\n` +
        `Status: **Pending approval**\n\n` +
        `Your GP has been deducted. Processing time: 5-30 minutes.\n` +
        `New balance: **${formatAmountFull(newBalance)}**`,
      ephemeral: true
    });
    
    // Alert admin
    await alertAdmin(
      '💸 New Withdrawal Request',
      `User: <@${interaction.user.id}>\n` +
      `ID: \`${withdrawalId}\`\n` +
      `Amount: $${withdrawalData.amountUsd.toFixed(2)} (${formatAmountFull(withdrawalData.amountGp)})\n` +
      `Net payout: $${withdrawalData.netUsd.toFixed(2)} USDT\n` +
      `Address: \`${withdrawalData.address}\`\n\n` +
      `Use \`/admin-process-withdrawal ${withdrawalId}\` to approve.`
    );
    
  } catch (error) {
    logger.error('crypto_withdrawal_error', { error: error.message });
    await interaction.followUp({
      content: 'Error processing withdrawal. Please contact support.',
      ephemeral: true
    });
  }
}
```

---

## 🛠️ PHASE 3: Admin Withdrawal Processing (2-3 hours)

### 3.1 Admin Withdrawal Commands

```javascript
new SlashCommandBuilder()
  .setName('admin-withdrawals')
  .setDescription('Admin: View pending crypto withdrawals'),

new SlashCommandBuilder()
  .setName('admin-process-withdrawal')
  .setDescription('Admin: Process a crypto withdrawal')
  .addStringOption(o =>
    o.setName('withdrawal_id')
      .setDescription('Withdrawal ID to process')
      .setRequired(true)
  ),
```

### 3.2 Manual Payout Process

**For now (before full automation)**:
1. Admin reviews withdrawal in dashboard
2. Manually sends USDT from external wallet
3. Updates withdrawal status
4. Bot notifies user

**Implementation later**: Auto-process via NOWPayments Payout API

---

## 📊 PHASE 4: Full Automation (Future)

1. **Auto-payout via NOWPayments Payout API**
2. **Multi-currency support** (BTC, ETH, LTC)
3. **KYC/AML compliance** (for larger withdrawals)
4. **Daily withdrawal limits**
5. **Fraud detection** (rapid deposit → withdraw)

---

## 🎯 IMPLEMENTATION PRIORITY

### **Week 1: Make Deposits Bulletproof**
- ✅ Add crypto_payments table
- ✅ IPN signature verification
- ✅ Minimum deposit limits
- ✅ Payment status tracking
- ✅ Duplicate protection
- ✅ Environment variables

**Impact**: Secure, auditable deposits ready for users

### **Week 2: Basic Withdrawals**
- ✅ Withdrawal command + UI
- ✅ crypto_withdrawals table
- ✅ Balance deduction
- ✅ Admin approval queue
- ✅ Manual processing workflow

**Impact**: Users can cash out (with admin approval)

### **Week 3: Automation (Optional)**
- 🔄 NOWPayments Payout API integration
- 🔄 Auto-processing under limits
- 🔄 Fraud detection

**Impact**: Fully automated crypto casino

---

## ⚡ QUICK START CHECKLIST

### Before You Can Accept Crypto:

1. **Sign up for NOWPayments**
   - Go to https://nowpayments.io/
   - Complete KYC verification
   - Get API credentials

2. **Configure Environment**
   ```bash
   NOWPAYMENTS_API_KEY=your_key
   NOWPAYMENTS_IPN_SECRET=your_secret
   NOWPAYMENTS_IPN_URL=https://your-domain.com/api/crypto/nowpayments-ipn
   MIN_DEPOSIT_USD=5.00
   ```

3. **Set up public endpoint**
   - Your bot must be accessible from internet
   - Use ngrok (dev) or deploy to VPS (prod)
   - NOWPayments must reach your IPN endpoint

4. **Test with sandbox**
   - NOWPayments has sandbox mode
   - Test full flow before going live

5. **Database migration**
   - Add crypto_payments table
   - Deploy updated database.js

---

## 💰 COSTS & FEES

### NOWPayments Fees:
- **Deposits**: 0.5% - 1% per transaction
- **Payouts**: ~1% + blockchain fee
- **Monthly fee**: ~$10-50 depending on volume

### Your Fees (Recommended):
- **Deposits**: FREE (you eat the cost)
- **Withdrawals**: 2-3% (covers your costs + profit)

### Profitability:
- User deposits $100 → You get $99 (1% fee)
- You credit 660M GP ($100 worth)
- User withdraws $80 → You pay $78.40 (2% fee)
- **Net profit**: $20.60 from house edge + $1.60 from withdrawal fee

---

## 🚨 CRITICAL SECURITY NOTES

1. **Never trust IPN without signature** - Always verify HMAC
2. **Prevent duplicate credits** - Check payment_id before crediting
3. **Rate limit deposits** - Max 5 per hour per user
4. **Monitor for fraud** - Flag rapid deposit → bet → withdraw
5. **Backup payout wallet** - Don't keep all funds in hot wallet
6. **Two-factor for withdrawals** - Admin must approve large amounts

---

Want me to implement **Phase 1 (Secure Deposits)** first? I can:
1. Add the crypto_payments table
2. Implement IPN signature verification
3. Add payment status tracking
4. Set up environment variables
5. Add /deposit_status command

Total time: **2-3 hours** to make deposits production-ready.

Should I start?
