require('dotenv').config();

const express = require('express');
const https = require('https');
const {
  Client,
  GatewayIntentBits,
  Partials,
  REST,
  Routes,
  SlashCommandBuilder,
  EmbedBuilder,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  ModalBuilder,
  TextInputBuilder,
  TextInputStyle,
} = require('discord.js');
const {
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
} = require('./utils/database');

const { createInvoice, getInvoiceDetails, checkInvoiceStatus, createPayout, getBalance } = require('./utils/plisio');
const checkUser = require('./commands/check-user');
const tickets = require('./utils/tickets');

const TOKEN = process.env.DISCORD_TOKEN;
const CLIENT_ID = process.env.CLIENT_ID;
const PORT = Number(process.env.PORT || 8081);

// Plisio configuration (crypto deposits)
const PLISIO_API_KEY = process.env.PLISIO_API_KEY;
const MIN_DEPOSIT_USD = Number(process.env.MIN_DEPOSIT_USD || 5);
const MAX_DEPOSIT_USD = Number(process.env.MAX_DEPOSIT_USD || 10000);
const MIN_WITHDRAWAL_USD = Number(process.env.MIN_WITHDRAWAL_USD || 10);
const MAX_WITHDRAWAL_USD = Number(process.env.MAX_WITHDRAWAL_USD || 5000);
const WITHDRAWAL_SPREAD = Number(process.env.WITHDRAWAL_SPREAD || 0.015);

// GP conversion rates with spread
// DEPOSIT: 1M GP = $0.15 => $1 = 6.67M GP (user buys GP)
// WITHDRAWAL: 1M GP = $0.135 => $1 = 7.41M GP (user sells GP back)
const GP_PER_USD_DEPOSIT = 1_000_000 / 0.15;
const GP_PER_USD_WITHDRAWAL = 1_000_000 / (0.15 - WITHDRAWAL_SPREAD);

// Backwards compatibility
const GP_PER_USD = GP_PER_USD_DEPOSIT;

// In-memory storage for transient state
const pendingLinks = new Map(); // code -> { discordUserId, createdAt }
let currentRound = null; // { roundId, red, blue, open, bets, messageId, channelId }
const queuedBets = new Map(); // userId -> { amount, side, displayName }

// Discord client
const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.DirectMessages,
    GatewayIntentBits.MessageContent,
  ],
  partials: [Partials.Channel],
});

function generateCode(length = 6) {
  const digits = '0123456789';
  let code = '';
  for (let i = 0; i < length; i++) {
    code += digits[Math.floor(Math.random() * digits.length)];
  }
  return code;
}

function parseAmountWithSuffix(input) {
  if (!input) return null;
  const s = String(input).trim().toLowerCase();
  const match = s.match(/^(\d+(?:\.\d+)?)([kmb])?$/);
  if (!match) return null;

  let value = parseFloat(match[1]);
  const suffix = match[2];

  if (!isFinite(value) || value <= 0) return null;

  let factor = 1;
  if (suffix === 'k') factor = 1_000;
  else if (suffix === 'm') factor = 1_000_000;
  else if (suffix === 'b') factor = 1_000_000_000;

  const amount = Math.floor(value * factor);
  return amount > 0 ? amount : null;
}

function formatAmountShort(amount) {
  if (amount >= 1_000_000_000) {
    const v = amount / 1_000_000_000;
    return (Number.isInteger(v) ? v.toString() : v.toFixed(1)) + 'b';
  }
  if (amount >= 1_000_000) {
    const v = amount / 1_000_000;
    return (Number.isInteger(v) ? v.toString() : v.toFixed(1)) + 'm';
  }
  if (amount >= 1_000) {
    const v = amount / 1_000;
    return (Number.isInteger(v) ? v.toString() : v.toFixed(1)) + 'k';
  }
  return amount.toLocaleString();
}

function formatAmountFull(amount) {
  return `${formatAmountShort(amount)} (${amount.toLocaleString()} GP)`;
}

function getCurrencyDisplayName(currency) {
  const names = {
    'BTC': 'Bitcoin (BTC)',
    'USDT': 'USDT (ERC20)',
    'LTC': 'Litecoin (LTC)'
  };
  return names[currency] || currency;
}

// Rakeback configuration (0.30% of each settled bet)
const RAKEBACK_RATE = 0.003;

function buildBetButtons(disabled = false) {
  return new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setCustomId('bet_red_button')
      .setLabel('Bet Red')
      .setStyle(ButtonStyle.Danger)
      .setDisabled(disabled),
    new ButtonBuilder()
      .setCustomId('bet_blue_button')
      .setLabel('Bet Blue')
      .setStyle(ButtonStyle.Primary)
      .setDisabled(disabled),
    new ButtonBuilder()
      .setCustomId('cancel_bet_button')
      .setLabel('Cancel Bet')
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(disabled),
    new ButtonBuilder()
      .setCustomId('change_bet_button')
      .setLabel('Change Bet')
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(disabled),
    new ButtonBuilder()
      .setCustomId('repeat_bet_button')
      .setLabel('Repeat Bet')
      .setStyle(ButtonStyle.Success)
      .setDisabled(disabled),
  );
}

function findUserBet(userId) {
  if (!currentRound || !Array.isArray(currentRound.bets)) return null;
  return currentRound.bets.find(b => b.discordUserId === userId) || null;
}

function upsertUserBet(userId, amount, side) {
  if (!currentRound) return;
  if (!Array.isArray(currentRound.bets)) {
    currentRound.bets = [];
  }
  const existing = currentRound.bets.find(b => b.discordUserId === userId);
  if (existing) {
    existing.amount = amount;
    existing.side = side;
  } else {
    currentRound.bets.push({ discordUserId: userId, amount, side });
  }
}

function removeUserBet(userId) {
  if (!currentRound || !Array.isArray(currentRound.bets)) return null;
  const idx = currentRound.bets.findIndex(b => b.discordUserId === userId);
  if (idx === -1) return null;
  const [removed] = currentRound.bets.splice(idx, 1);
  return removed;
}

// ---------- Slash commands ----------
const commands = [
  new SlashCommandBuilder()
    .setName('link')
    .setDescription('Link your Discord account to your in-game RSN via a code in DMs'),
  new SlashCommandBuilder()
    .setName('balance')
    .setDescription('Show your GP balance in the betting system'),
  new SlashCommandBuilder()
    .setName('stats')
    .setDescription('Show your betting stats (balance, W/L, streak)'),
  new SlashCommandBuilder()
    .setName('rakeback')
    .setDescription('Show your unclaimed rakeback at the current rate'),
  new SlashCommandBuilder()
    .setName('rakeback_claim')
    .setDescription('Claim your unclaimed rakeback into your balance'),
  new SlashCommandBuilder()
    .setName('deposit_usdt')
    .setDescription('Create a crypto deposit (USDT, BTC, ETH, etc.)')
    .addNumberOption(o =>
      o.setName('amount_usd')
        .setDescription('Deposit amount in USD')
        .setRequired(true)
    ),
  new SlashCommandBuilder()
    .setName('deposit_status')
    .setDescription('Check status of your recent crypto deposits'),
  new SlashCommandBuilder()
    .setName('withdraw')
    .setDescription('Withdraw GP to crypto')
    .addStringOption(o =>
      o.setName('currency')
        .setDescription('Cryptocurrency to withdraw')
        .setRequired(true)
        .addChoices(
          { name: 'Bitcoin (BTC)', value: 'BTC' },
          { name: 'USDT (ERC20)', value: 'USDT' },
          { name: 'Litecoin (LTC)', value: 'LTC' },
        )
    )
    .addStringOption(o =>
      o.setName('amount')
        .setDescription('Amount in USD (e.g., 10, 50, 100)')
        .setRequired(true)
    )
    .addStringOption(o =>
      o.setName('address')
        .setDescription('Your crypto wallet address')
        .setRequired(true)
    ),
  new SlashCommandBuilder()
    .setName('get_balances')
    .setDescription('Admin: Check Plisio crypto wallet balances'),
  new SlashCommandBuilder()
    .setName('check')
    .setDescription('Admin: Comprehensive user investigation tool')
    .addStringOption(o =>
      o.setName('identifier')
        .setDescription('Discord username, user ID, or RSN')
        .setRequired(true)
    ),
  new SlashCommandBuilder()
    .setName('setup-tickets')
    .setDescription('Admin: Post the support ticket panel'),
  new SlashCommandBuilder()
    .setName('queued-bet')
    .setDescription('Check if you have a bet queued for the next round'),
  new SlashCommandBuilder()
    .setName('cancel-queued-bet')
    .setDescription('Cancel your queued bet for the next round'),
  new SlashCommandBuilder()
    .setName('bet')
    .setDescription('Place a bet on the current duel round')
    .addStringOption(o =>
      o.setName('amount')
        .setDescription('Amount of GP to bet (e.g. 500k, 1m, 1b)')
        .setRequired(true)
    )
    .addStringOption(o =>
      o.setName('side')
        .setDescription('Which side to bet on')
        .setRequired(true)
        .addChoices(
          { name: 'Red', value: 'red' },
          { name: 'Blue', value: 'blue' },
        )
    ),
  new SlashCommandBuilder()
    .setName('admin-add-gp')
    .setDescription('Admin: add GP to a user')
    .addUserOption(o =>
      o.setName('user')
        .setDescription('User to credit')
        .setRequired(true)
    )
    .addStringOption(o =>
      o.setName('amount')
        .setDescription('Amount of GP to add (supports K/M/B, e.g. 500k, 1m, 1.5b)')
        .setRequired(true)
    ),
  new SlashCommandBuilder()
    .setName('graph')
    .setDescription('Admin: show a user\'s P/L graph based on betting history')
    .addUserOption(o =>
      o.setName('user')
        .setDescription('User to graph')
        .setRequired(true)
    ),
  new SlashCommandBuilder()
    .setName('settings')
    .setDescription('Configure betting bot settings (admin only)')
    .addSubcommand(sc =>
      sc.setName('betting_channel')
        .setDescription('Set the betting channel')
        .addChannelOption(o =>
          o.setName('channel')
            .setDescription('Channel where the betting panel lives')
            .setRequired(true)
        )
    )
    .addSubcommand(sc =>
      sc.setName('results_channel')
        .setDescription('Set the results channel')
        .addChannelOption(o =>
          o.setName('channel')
            .setDescription('Channel where results are posted')
            .setRequired(true)
        )
    )
    .addSubcommand(sc =>
      sc.setName('logo_url')
        .setDescription('Set the logo URL used in embeds')
        .addStringOption(o =>
          o.setName('url')
            .setDescription('Direct image URL for the logo')
            .setRequired(true)
        )
    )
    .addSubcommand(sc =>
      sc.setName('panel_thumbnail')
        .setDescription('Set the thumbnail URL for the betting panel UI')
        .addStringOption(o =>
          o.setName('url')
            .setDescription('Direct image URL for the panel thumbnail')
            .setRequired(true)
        )
    )
    .addSubcommand(sc =>
      sc.setName('reset')
        .setDescription('Reset settings and clear active round (admin only)')
    ),
].map(cmd => cmd.toJSON());

async function registerCommands() {
  if (!TOKEN || !CLIENT_ID) {
    console.error('DISCORD_TOKEN or CLIENT_ID missing in environment');
    return;
  }

  const rest = new REST({ version: '10' }).setToken(TOKEN);
  await rest.put(Routes.applicationCommands(CLIENT_ID), { body: commands });
  console.log('Registered global slash commands');
}

// ---------- Discord events ----------
client.once('ready', () => {
  console.log(`Logged in as ${client.user.tag}`);
  
  // Start crypto payment checker
  if (PLISIO_API_KEY) {
    startPaymentChecker();
  }
});

// ==================== Crypto Payment Checker ====================
// Polls Plisio API every 30 seconds to check for completed payments
// No webhook needed!

async function checkPendingPayments() {
  try {
    const pending = getPendingCryptoPayments(24 * 60 * 60); // Last 24 hours
    
    if (pending.length === 0) return;
    
    console.log(`Checking ${pending.length} pending crypto payments...`);
    
    for (const payment of pending) {
      try {
        const status = await checkInvoiceStatus(payment.txn_id);
        
        // Update if status changed
        if (status.status !== payment.status) {
          updateCryptoPaymentStatus(payment.txn_id, status.status);
          console.log(`Payment ${payment.txn_id} status: ${payment.status} -> ${status.status}`);
          
          // Credit user if completed
          if (status.status === 'completed' && payment.status !== 'completed') {
            const newBalance = adjustBalance(payment.user_id, payment.amount_gp, {
              reason: 'crypto-deposit',
            });
            
            console.log(`✅ Crypto deposit completed: User ${payment.user_id} - $${payment.amount_usd} (${payment.amount_gp} GP)`);
            
            // Notify user via DM
            try {
              const user = await client.users.fetch(payment.user_id);
              await user.send(
                `✅ **Crypto deposit confirmed!**\n` +
                `Amount: $${payment.amount_usd.toFixed(2)} → ${formatAmountFull(payment.amount_gp)}\n` +
                `New balance: **${formatAmountFull(newBalance)}**\n\n` +
                `You can now use your GP for betting!`
              );
            } catch (dmErr) {
              console.error(`Failed to DM user ${payment.user_id} about deposit:`, dmErr.message);
            }
          }
        }
      } catch (err) {
        // Skip individual payment errors
        console.error(`Error checking payment ${payment.txn_id}:`, err.message);
      }
      
      // Rate limit: wait 500ms between checks
      await new Promise(resolve => setTimeout(resolve, 500));
    }
  } catch (err) {
    console.error('Payment checker error:', err);
  }
}

function startPaymentChecker() {
  console.log('✅ Crypto payment checker started (polling every 30 seconds)');
  
  // Check immediately on start
  checkPendingPayments();
  
  // Then check every 30 seconds
  setInterval(checkPendingPayments, 30000);
}

function isAdminInteraction(interaction) {
  const settings = getSettings();
  const requiredRoleId = settings.adminRoleId;
  if (!interaction.inGuild()) {
    return false;
  }
  const member = interaction.member;
  if (!member || !member.roles) return false;
  if (member.permissions && member.permissions.has('Administrator')) return true;
  return member.roles.cache.has(requiredRoleId);
}

client.on('interactionCreate', async interaction => {
  // ----- Slash commands -----
  if (interaction.isChatInputCommand()) {
    if (interaction.commandName === 'link') {
    const code = generateCode();
    const normalized = code.toUpperCase();
    pendingLinks.set(normalized, { discordUserId: interaction.user.id, createdAt: Date.now() });

    try {
      await interaction.user.send(
        `Your link code: \`${code}\`\nIn-game, type: \`${code}\` in public or private chat to link your RSN.`
      );
      await interaction.reply({
        content: 'I sent you a DM with your link code. If you did not get it, enable DMs and run /link again.',
        ephemeral: true,
      });
    } catch (err) {
      console.error('DM failed:', err);
      await interaction.reply({
        content: 'I could not DM you. Please enable DMs from this server and try /link again.',
        ephemeral: true,
      });
    }
  }

  else if (interaction.commandName === 'balance') {
    const user = getOrCreateUser(interaction.user.id, interaction.user.tag);
    await interaction.reply({
      content: `Your balance: **${formatAmountFull(user.balance || 0)}**` +
        (user.linkedRsns && user.linkedRsns.length > 0 ? ` (linked RSNs: \`${user.linkedRsns.join(', ')}\`)` : ''),
      ephemeral: true,
    });
  }

  else if (interaction.commandName === 'stats') {
    const user = getOrCreateUser(interaction.user.id, interaction.user.tag);
    const history = Array.isArray(user.betHistory) ? user.betHistory : [];

    let wins = 0;
    let losses = 0;
    let refunds = 0;
    let totalWagered = 0;
    let countedBets = 0;
    for (const entry of history) {
      const amount = typeof entry.amount === 'number' ? entry.amount : 0;
      if (amount > 0) {
        totalWagered += amount;
        countedBets += 1;
      }

      if (entry.outcome === 'win') wins += 1;
      else if (entry.outcome === 'loss') losses += 1;
      else if (entry.outcome === 'refund') refunds += 1;
    }

    // Current win streak: count wins from the end until the first non-win
    let currentStreak = 0;
    for (let i = history.length - 1; i >= 0; i--) {
      const entry = history[i];
      if (entry.outcome === 'win') currentStreak += 1;
      else if (entry.outcome === 'loss') break;
    }

    const totalBets = countedBets;

    const embed = new EmbedBuilder()
      .setTitle('📊 Your Betting Stats')
      .setDescription(`Stats for **${interaction.user.tag}**`)
      .addFields(
        {
          name: '💰 Balance',
          value: formatAmountFull(user.balance || 0),
          inline: false,
        },
        {
          name: '💸 Total wagered',
          value: totalWagered > 0 ? formatAmountFull(totalWagered) : 'No bets placed yet',
          inline: false,
        },
        {
          name: '🏅 Record',
          value: `Wins: **${wins}**  |  Losses: **${losses}**` + (refunds > 0 ? `  |  Refunds: **${refunds}**` : ''),
          inline: false,
        },
        {
          name: '🔥 Current win streak',
          value: currentStreak > 0 ? `${currentStreak} win${currentStreak === 1 ? '' : 's'} in a row` : 'No active win streak',
          inline: false,
        },
        {
          name: '🎲 Total bets',
          value: totalBets.toString(),
          inline: false,
        },
      )
      .setColor(0x00FFAA)
      .setTimestamp(new Date());

    await interaction.reply({ embeds: [embed] });
  }

  else if (interaction.commandName === 'rakeback') {
    const user = getOrCreateUser(interaction.user.id, interaction.user.tag);
    const unclaimed = user.rakebackUnclaimed || 0;

    const embed = new EmbedBuilder()
      .setTitle('💸 Rakeback')
      .setDescription('Your current unclaimed rakeback from recent bets')
      .addFields(
        {
          name: 'Unclaimed rakeback',
          value: unclaimed > 0 ? formatAmountFull(unclaimed) : 'You have no unclaimed rakeback yet.',
          inline: false,
        },
      )
      .setColor(0xFFD700)
      .setTimestamp(new Date());

    await interaction.reply({ embeds: [embed] });
  }

  else if (interaction.commandName === 'rakeback_claim') {
    const { claimed, newBalance } = claimRakeback(interaction.user.id, interaction.user.tag);

    if (!claimed || claimed <= 0) {
      await interaction.reply({ content: 'You have no rakeback to claim.', ephemeral: true });
      return;
    }

    await interaction.reply({
      content: `You claimed **${formatAmountFull(claimed)}** in rakeback. New balance: **${formatAmountFull(newBalance)}**.`,
      ephemeral: true,
    });
  }

  else if (interaction.commandName === 'deposit_usdt') {
    const amountUsd = interaction.options.getNumber('amount_usd', true);

    if (!PLISIO_API_KEY) {
      await interaction.reply({
        content: 'Crypto deposits are not configured. Please contact an admin.',
        ephemeral: true,
      });
      return;
    }

    if (!amountUsd || !Number.isFinite(amountUsd) || amountUsd <= 0) {
      await interaction.reply({ 
        content: 'Deposit amount must be a positive number in USD.', 
        ephemeral: true 
      });
      return;
    }

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

    const estimatedGp = Math.floor(amountUsd * GP_PER_USD);

    await interaction.deferReply({ ephemeral: true });

    try {
      const invoice = await createInvoice({
        orderId: interaction.user.id,
        amount: amountUsd,
        orderName: `${formatAmountFull(estimatedGp)} GP Deposit`
      });

      // Fetch full invoice details (includes payment address, QR code, etc.)
      const invoiceDetails = await getInvoiceDetails(invoice.txn_id);
      
      // Debug: Log full invoice structure
      console.log('Plisio invoice details:', JSON.stringify(invoiceDetails, null, 2));

      // Merge basic invoice with full details
      const fullInvoice = { ...invoice, ...invoiceDetails };

      // Record payment in database
      recordCryptoPayment(interaction.user.id, {
        txn_id: fullInvoice.txn_id,
        amount_usd: amountUsd,
        amount_gp: estimatedGp,
        currency: fullInvoice.source_currency || 'USDT',
        wallet_hash: fullInvoice.wallet_hash,
        invoice_url: fullInvoice.invoice_url,
        qr_code: fullInvoice.qr_code,
        metadata: fullInvoice
      });

      // Extract currency and amount safely
      const currency = (fullInvoice.source_currency || 'USDT').toUpperCase();
      const amountToSend = fullInvoice.source_amount || fullInvoice.invoice_total_sum || amountUsd;
      
      const embed = new EmbedBuilder()
        .setTitle('💳 Crypto Deposit Created')
        .setDescription(`Send **${amountToSend} ${currency}** to complete your deposit`)
        .addFields(
          { name: '💰 Amount', value: `$${amountUsd.toFixed(2)} → ${formatAmountFull(estimatedGp)}`, inline: false },
          { name: '📍 Payment Address', value: `\`${fullInvoice.wallet_hash}\``, inline: false },
          { name: '🔗 Or pay online', value: `[Click here to open payment page](${fullInvoice.invoice_url})`, inline: false },
          { name: '⏰ Expires', value: `<t:${Math.floor(Date.now() / 1000) + 3600}:R>`, inline: false },
        )
        .setImage(fullInvoice.qr_code) // Plisio QR code
        .setColor(0x00D1FF)
        .setFooter({ text: 'Your balance will be credited automatically after payment confirmation' })
        .setTimestamp();

      await interaction.editReply({ embeds: [embed] });

      console.log(`Crypto deposit created: ${interaction.user.tag} - $${amountUsd} (${estimatedGp} GP) - TXN: ${invoice.txn_id}`);
    } catch (err) {
      console.error('Plisio create invoice error:', err);
      await interaction.editReply({
        content: 'Failed to create deposit. Please try again later or contact an admin.',
      });
    }
  }

  else if (interaction.commandName === 'deposit_status') {
    const payments = getUserCryptoPayments(interaction.user.id, 5);
    
    if (payments.length === 0) {
      await interaction.reply({
        content: 'You have no crypto deposit history yet. Use `/deposit_usdt` to create a deposit.',
        ephemeral: true
      });
      return;
    }
    
    const statusEmoji = {
      'pending': '⏳ Pending',
      'confirming': '🔄 Confirming',
      'completed': '✅ Completed',
      'expired': '⏰ Expired',
      'failed': '❌ Failed',
    };
    
    const fields = payments.map(p => {
      const status = statusEmoji[p.status] || '❓ Unknown';
      const date = `<t:${p.created_at}:R>`;
      
      let value = `Status: **${status}**\nCreated: ${date}`;
      
      if (p.status === 'pending') {
        value += `\n[Open payment page](${p.invoice_url})`;
      }
      
      return {
        name: `$${p.amount_usd.toFixed(2)} → ${formatAmountShort(p.amount_gp)}`,
        value,
        inline: true
      };
    });
    
    const embed = new EmbedBuilder()
      .setTitle('💳 Recent Crypto Deposits')
      .setDescription('Your last 5 crypto deposit attempts')
      .addFields(fields)
      .setColor(0x00D1FF)
      .setFooter({ text: 'Payments are checked every 30 seconds. Completed deposits are credited automatically.' })
      .setTimestamp();
    
    await interaction.reply({ embeds: [embed], ephemeral: true });
  }

  else if (interaction.commandName === 'withdraw') {
    const currency = interaction.options.getString('currency', true);
    const amountStr = interaction.options.getString('amount', true);
    const address = interaction.options.getString('address', true).trim();
    
    if (!PLISIO_API_KEY) {
      await interaction.reply({
        content: 'Crypto withdrawals are not configured. Please contact an admin.',
        ephemeral: true,
      });
      return;
    }

    const amountUsd = parseFloat(amountStr);
    
    if (!amountUsd || !Number.isFinite(amountUsd) || amountUsd <= 0) {
      await interaction.reply({
        content: 'Invalid amount. Specify USD value (e.g., 10, 50, 100).',
        ephemeral: true
      });
      return;
    }
    
    if (amountUsd < MIN_WITHDRAWAL_USD) {
      await interaction.reply({
        content: `Minimum withdrawal is **$${MIN_WITHDRAWAL_USD.toFixed(2)}**.`,
        ephemeral: true
      });
      return;
    }
    
    if (amountUsd > MAX_WITHDRAWAL_USD) {
      await interaction.reply({
        content: `Maximum withdrawal is **$${MAX_WITHDRAWAL_USD.toFixed(2)}**. Contact admin for larger amounts.`,
        ephemeral: true
      });
      return;
    }
    
    // Validate address format based on currency
    let validAddress = false;
    let addressError = '';
    
    if (currency === 'BTC') {
      // Bitcoin: starts with 1, 3, or bc1
      validAddress = /^(1|3|bc1)[a-zA-Z0-9]{25,62}$/.test(address);
      addressError = 'Invalid Bitcoin address. Must start with 1, 3, or bc1.';
    } else if (currency === 'USDT') {
      // USDT ERC20: uses Ethereum address format (0x...)
      validAddress = /^0x[a-fA-F0-9]{40}$/.test(address);
      addressError = 'Invalid USDT (ERC20) address. Must start with 0x and be 42 characters.';
    } else if (currency === 'LTC') {
      // Litecoin: starts with L or M
      validAddress = /^[LM][a-km-zA-HJ-NP-Z1-9]{26,33}$/.test(address);
      addressError = 'Invalid Litecoin address. Must start with L or M.';
    }
    
    if (!validAddress) {
      await interaction.reply({
        content: addressError,
        ephemeral: true
      });
      return;
    }
    
    // Calculate GP required with withdrawal rate (higher rate = more GP needed)
    const requiredGp = Math.ceil(amountUsd * GP_PER_USD_WITHDRAWAL);
    
    const user = getOrCreateUser(interaction.user.id, interaction.user.tag);
    
    if (user.balance < requiredGp) {
      const shortfall = requiredGp - user.balance;
      await interaction.reply({
        content: 
          `Insufficient balance for withdrawal.\n\n` +
          `**Required:** ${formatAmountFull(requiredGp)} ($${amountUsd.toFixed(2)} at withdrawal rate)\n` +
          `**Your balance:** ${formatAmountFull(user.balance)}\n` +
          `**Short:** ${formatAmountFull(shortfall)}\n\n` +
          `💡 *Note: Withdrawal rate is $1 = ${formatAmountShort(Math.floor(GP_PER_USD_WITHDRAWAL))}, which is ${(WITHDRAWAL_SPREAD * 100).toFixed(1)}% less favorable than deposit rate due to fees.*`,
        ephemeral: true
      });
      return;
    }
    
    // Check Plisio wallet balance before proceeding
    await interaction.deferReply({ ephemeral: true });
    
    try {
      const plisioBalance = await getBalance(currency);
      const availableBalance = parseFloat(plisioBalance.balance || 0);
      
      // For USDT (ERC20), also check ETH balance for gas fees
      if (currency === 'USDT') {
        const ethBalance = await getBalance('ETH');
        const availableEth = parseFloat(ethBalance.balance || 0);
        const minEthForGas = 0.00001; // ~$0.03 minimum ETH for gas
        
        if (availableEth < minEthForGas) {
          await interaction.editReply({
            content:
              `⚠️ **Withdrawal temporarily unavailable**\n\n` +
              `Our wallet doesn't have enough ETH for gas fees to process USDT (ERC20) withdrawals.\n\n` +
              `**Please contact an admin or open a ticket.**\n\n` +
              `Alternative: Try withdrawing as Bitcoin (BTC) or Litecoin (LTC) instead!`,
          });
          return;
        }
      }
      
      // Check if enough balance in the withdrawal currency
      // For USDT, we need at least the withdrawal amount
      const minRequired = currency === 'USDT' ? amountUsd : amountUsd * 1.01; // Add 1% buffer for native coins
      
      if (availableBalance < minRequired) {
        await interaction.editReply({
          content:
            `⚠️ **Withdrawal temporarily unavailable**\n\n` +
            `Our ${getCurrencyDisplayName(currency)} wallet has insufficient funds.\n\n` +
            `**Available:** ${availableBalance.toFixed(6)} ${currency}\n` +
            `**Required:** ${minRequired.toFixed(6)} ${currency}\n\n` +
            `**Please contact an admin or open a ticket.**`,
        });
        return;
      }
    } catch (balanceError) {
      console.error('Failed to check Plisio balance:', balanceError);
      await interaction.editReply({
        content:
          `⚠️ **Unable to verify wallet balance**\n\n` +
          `We couldn't check if our wallet has sufficient funds.\n\n` +
          `**Please contact an admin or open a ticket.**\n\n` +
          `Error: ${balanceError.message}`,
      });
      return;
    }
    
    // Show confirmation
    const depositEquivalent = amountUsd * GP_PER_USD_DEPOSIT;
    const embed = new EmbedBuilder()
      .setTitle('💸 Confirm Crypto Withdrawal')
      .setDescription('Please review and confirm the withdrawal details:')
      .addFields(
        { name: '💰 You pay', value: `${formatAmountFull(requiredGp)}`, inline: false },
        { name: '💵 You receive', value: `**$${amountUsd.toFixed(2)} ${getCurrencyDisplayName(currency)}**`, inline: false },
        { name: '📍 Address', value: `\`${address}\``, inline: false },
        { name: '📊 Withdrawal rate', value: `$1 = ${formatAmountShort(Math.floor(GP_PER_USD_WITHDRAWAL))}`, inline: true },
        { name: '📈 Deposit rate', value: `$1 = ${formatAmountShort(Math.floor(GP_PER_USD_DEPOSIT))}`, inline: true },
        { name: '💡 Rate difference', value: `${((GP_PER_USD_WITHDRAWAL / GP_PER_USD_DEPOSIT - 1) * 100).toFixed(1)}% spread`, inline: false },
      )
      .setColor(0xFFA500)
      .setFooter({ text: 'Processing time: 5-15 minutes • GP will be deducted immediately' });
    
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
    
    await interaction.editReply({
      embeds: [embed],
      components: [row],
    });
    
    // Store withdrawal data temporarily (will be processed when button clicked)
    // Create unique key per user to prevent multiple pending withdrawals
    client.pendingWithdrawals = client.pendingWithdrawals || new Map();
    
    // For native cryptocurrencies, use the currency code as-is
    // Plisio supports: BTC, ETH, LTC, TRX, BNB, etc.
    const plisioCurrency = currency;
    
    client.pendingWithdrawals.set(interaction.user.id, {
      amountUsd,
      requiredGp,
      address,
      currency: plisioCurrency,
      displayCurrency: getCurrencyDisplayName(currency)
    });
  }

  else if (interaction.commandName === 'get_balances') {
    if (!isAdminInteraction(interaction)) {
      await interaction.reply({ content: 'You do not have permission to use this command.', ephemeral: true });
      return;
    }
    
    if (!PLISIO_API_KEY) {
      await interaction.reply({
        content: 'Plisio API is not configured.',
        ephemeral: true
      });
      return;
    }
    
    await interaction.deferReply({ ephemeral: true });
    
    try {
      const currencies = ['usdt', 'btc', 'eth', 'ltc'];
      const balances = [];
      
      for (const currency of currencies) {
        try {
          const balance = await getBalance(currency);
          balances.push({
            name: `${currency.toUpperCase()} Balance`,
            value: `**${balance.balance || '0'}** ${currency.toUpperCase()}` +
                   (balance.balance_usd ? ` ($${parseFloat(balance.balance_usd).toFixed(2)} USD)` : ''),
            inline: true
          });
        } catch (err) {
          balances.push({
            name: `${currency.toUpperCase()} Balance`,
            value: `Error: ${err.message}`,
            inline: true
          });
        }
      }
      
      const embed = new EmbedBuilder()
        .setTitle('💰 Plisio Wallet Balances')
        .setDescription('Current crypto balances available for withdrawals')
        .addFields(balances)
        .setColor(0x00D1FF)
        .setFooter({ text: 'Make sure to fund your Plisio wallet to process withdrawals' })
        .setTimestamp();
      
      await interaction.editReply({ embeds: [embed] });
    } catch (err) {
      console.error('Get balances error:', err);
      await interaction.editReply({
        content: 'Failed to fetch balances from Plisio. Check console for errors.',
      });
    }
  }

  else if (interaction.commandName === 'check') {
    if (!isAdminInteraction(interaction)) {
      await interaction.reply({ content: 'You do not have permission to use this command.', ephemeral: true });
      return;
    }
    
    const identifier = interaction.options.getString('identifier', true);
    
    await interaction.deferReply({ ephemeral: true });
    
    try {
      // Search for user
      const searchResult = searchUserByIdentifier(identifier);
      
      if (!searchResult) {
        await interaction.editReply({
          content: `❌ **User not found**\n\nCould not find a user matching: \`${identifier}\`\n\nTry:\n• Discord user ID\n• Discord username (partial match)\n• Linked RSN (exact match)`,
        });
        return;
      }
      
      const { user, foundBy } = searchResult;
      
      // Fetch comprehensive data
      const data = getUserComprehensiveData(user.id);
      
      // Create overview embed and buttons
      const embed = checkUser.createOverviewEmbed(data, client);
      const buttons = checkUser.createButtonRow(user.id);
      
      // Add found-by notice
      const foundByText = {
        'discord_id': 'Discord User ID',
        'username': 'Discord Username',
        'rsn': 'Linked RSN'
      }[foundBy];
      
      embed.setFooter({ text: `User Check • Found by: ${foundByText} • Use buttons below for detailed views` });
      
      await interaction.editReply({
        embeds: [embed],
        components: buttons
      });
      
      // Store data temporarily for button interactions
      if (!client.checkUserData) client.checkUserData = new Map();
      client.checkUserData.set(user.id, { data, timestamp: Date.now() });
      
      // Clean up after 5 minutes
      setTimeout(() => {
        client.checkUserData.delete(user.id);
      }, 300000);
      
    } catch (err) {
      console.error('Check command error:', err);
      await interaction.editReply({
        content: `❌ **Error checking user**\n\n${err.message}`,
      });
    }
  }

  else if (interaction.commandName === 'setup-tickets') {
    if (!isAdminInteraction(interaction)) {
      await interaction.reply({ content: 'You do not have permission to use this command.', ephemeral: true });
      return;
    }
    
    const supportChannel = await client.channels.fetch(tickets.SUPPORT_CHANNEL_ID);
    if (!supportChannel) {
      await interaction.reply({ content: '❌ Support channel not found!', ephemeral: true });
      return;
    }
    
    const embed = new EmbedBuilder()
      .setTitle('🎫 Support Tickets')
      .setDescription(
        '**Need help?** Create a support ticket!\n\n' +
        '**How it works:**\n' +
        '• Click the button below to open a ticket\n' +
        '• A private channel will be created for you\n' +
        '• Only you and admins can see your ticket\n' +
        '• Explain your issue and we\'ll assist you\n\n' +
        '**What to include:**\n' +
        '• Your in-game RSN (if applicable)\n' +
        '• Transaction IDs (for deposits/withdrawals)\n' +
        '• Screenshots or evidence\n' +
        '• Detailed description of the issue\n\n' +
        '⚡ **Response Time:** Usually within a few hours'
      )
      .setColor(0x5865F2)
      .setThumbnail('https://i.imgur.com/YJBkXwi.png')
      .setFooter({ text: 'NoidBets Support • We\'re here to help!' })
      .setTimestamp();
    
    const button = new ActionRowBuilder()
      .addComponents(
        new ButtonBuilder()
          .setCustomId('create_ticket')
          .setLabel('Create Ticket')
          .setEmoji('🎫')
          .setStyle(ButtonStyle.Primary)
      );
    
    await supportChannel.send({
      embeds: [embed],
      components: [button]
    });
    
    await interaction.reply({ content: '✅ Ticket panel posted!', ephemeral: true });
  }

  else if (interaction.commandName === 'queued-bet') {
    const queuedBet = queuedBets.get(interaction.user.id);
    
    if (!queuedBet) {
      await interaction.reply({
        content: '❌ You don\'t have any bets queued for the next round.',
        ephemeral: true
      });
      return;
    }
    
    const user = getOrCreateUser(interaction.user.id, interaction.user.tag);
    const color = queuedBet.side === 'red' ? 0xFF0000 : 0x0000FF;
    const hasEnough = (user.balance || 0) >= queuedBet.amount;
    
    const embed = new EmbedBuilder()
      .setTitle('⏳ Your Queued Bet')
      .setDescription(
        `You have a bet queued for the next round.\n\n` +
        `**Amount:** ${formatAmountFull(queuedBet.amount)}\n` +
        `**Side:** ${queuedBet.side.toUpperCase()}\n\n` +
        `**Current Balance:** ${formatAmountFull(user.balance || 0)}\n` +
        `**Status:** ${hasEnough ? '✅ Ready to place' : '❌ Insufficient balance'}\n\n` +
        `${hasEnough ? 'Your bet will be placed automatically when the next round opens!' : '⚠️ Warning: You don\'t have enough balance. Please deposit more GP or your bet will be cancelled.'}`
      )
      .setColor(color)
      .setTimestamp();
    
    const cancelButton = new ActionRowBuilder()
      .addComponents(
        new ButtonBuilder()
          .setCustomId('cancel_queued_bet_button')
          .setLabel('Cancel Queued Bet')
          .setEmoji('🗑️')
          .setStyle(ButtonStyle.Danger)
      );
    
    await interaction.reply({ embeds: [embed], components: [cancelButton], ephemeral: true });
  }

  else if (interaction.commandName === 'cancel-queued-bet') {
    const queuedBet = queuedBets.get(interaction.user.id);
    
    if (!queuedBet) {
      await interaction.reply({
        content: '❌ You don\'t have any bets queued for the next round.',
        ephemeral: true
      });
      return;
    }
    
    // Remove from queue
    queuedBets.delete(interaction.user.id);
    
    const embed = new EmbedBuilder()
      .setTitle('🗑️ Queued Bet Cancelled')
      .setDescription(
        `Your queued bet has been cancelled.\n\n` +
        `**Amount:** ${formatAmountFull(queuedBet.amount)}\n` +
        `**Side:** ${queuedBet.side.toUpperCase()}\n\n` +
        `You can place a new bet when the next round opens.`
      )
      .setColor(0xFF9900)
      .setTimestamp();
    
    await interaction.reply({ embeds: [embed], ephemeral: true });
    console.log(`[Queue] ${interaction.user.tag} cancelled queued bet: ${formatAmountFull(queuedBet.amount)} on ${queuedBet.side}`);
  }

  else if (interaction.commandName === 'bet') {
    const rawAmount = interaction.options.getString('amount', true);
    const amount = parseAmountWithSuffix(rawAmount);
    const side = interaction.options.getString('side', true); // "red" | "blue"

    if (!amount || amount <= 0) {
      await interaction.reply({ content: 'Invalid bet amount. Use values like 500k, 1m, 1.5m, 1b.', ephemeral: true });
      setTimeout(() => {
        interaction.deleteReply().catch(() => {});
      }, 20_000);
      return;
    }

    const user = getOrCreateUser(interaction.user.id, interaction.user.tag);
    if ((user.balance || 0) < amount) {
      await interaction.reply({
        content: `Insufficient balance. You have ${formatAmountFull(user.balance || 0)}.`,
        ephemeral: true,
      });
      setTimeout(() => {
        interaction.deleteReply().catch(() => {});
      }, 20_000);
      return;
    }

    // If betting is closed, queue the bet for next round
    if (!currentRound || !currentRound.open) {
      const existingBet = queuedBets.get(interaction.user.id);
      
      queuedBets.set(interaction.user.id, {
        amount,
        side,
        displayName: interaction.user.tag
      });
      
      const color = side === 'red' ? 0xFF0000 : 0x0000FF;
      const embed = new EmbedBuilder()
        .setTitle('⏳ Bet Queued for Next Round')
        .setDescription(
          `Betting is currently closed, so your bet has been queued.\n\n` +
          `**Amount:** ${formatAmountFull(amount)}\n` +
          `**Side:** ${side.toUpperCase()}\n\n` +
          `✅ Your bet will automatically be placed when the next round opens!` +
          (existingBet ? `\n\n⚠️ Note: This replaced your previous queued bet of ${formatAmountFull(existingBet.amount)} on ${existingBet.side.toUpperCase()}` : '')
        )
        .setColor(color)
        .setFooter({ text: `Balance: ${formatAmountFull(user.balance)} • Use /queued-bet to view/cancel` })
        .setTimestamp();
      
      await interaction.reply({ embeds: [embed], ephemeral: true });
      console.log(`[Queue] ${interaction.user.tag} queued ${formatAmountFull(amount)} on ${side}${existingBet ? ' (replaced existing)' : ''}`);
      return;
    }

    const balanceBefore = user.balance || 0;
    const balanceAfter = adjustBalance(interaction.user.id, -amount, {
      displayName: interaction.user.tag,
      reason: 'bet',
    });
    currentRound.bets.push({
      discordUserId: interaction.user.id,
      amount,
      side,
    });

    const color = side === 'red' ? 0xFF0000 : 0x0000FF;

    const embed = new EmbedBuilder()
      .setTitle('💰 Bet placed')
      .setDescription(`Round \`${currentRound.roundId}\``)
      .addFields(
        {
          name: '🎲 Side',
          value: side.toUpperCase(),
          inline: true,
        },
        {
          name: '💰 Amount',
          value: formatAmountShort(amount),
          inline: true,
        },
        {
          name: '📉 Balance before',
          value: formatAmountFull(balanceBefore),
          inline: false,
        },
        {
          name: '📈 Balance after',
          value: formatAmountFull(balanceAfter),
          inline: false,
        },
      )
      .setColor(color)
      .setTimestamp(new Date());

    await interaction.reply({
      embeds: [embed],
      ephemeral: true,
    });

    // Auto-remove bet confirmation after 20 seconds
    setTimeout(() => {
      interaction.deleteReply().catch(() => {});
    }, 20_000);
  }

  else if (interaction.commandName === 'admin-add-gp') {
    if (!isAdminInteraction(interaction)) {
      await interaction.reply({ content: 'You do not have permission to use this command.', ephemeral: true });
      return;
    }
    const targetUser = interaction.options.getUser('user', true);
    const rawAmount = interaction.options.getString('amount', true);
    const amount = parseAmountWithSuffix(rawAmount);

    if (!amount || amount <= 0) {
      await interaction.reply({ content: 'Amount must be a positive number (supports formats like 500k, 1m, 1.5b).', ephemeral: true });
      return;
    }

    const newBalance = adjustBalance(targetUser.id, amount, {
      displayName: targetUser.tag,
      reason: 'admin-add-gp',
    });

    await interaction.reply({
      content: `Added **${formatAmountFull(amount)}** to <@${targetUser.id}>. New balance: **${formatAmountFull(newBalance)}**.`,
      ephemeral: true,
    });
  }
  else if (interaction.commandName === 'graph') {
    if (!isAdminInteraction(interaction)) {
      await interaction.reply({ content: 'You do not have permission to use this command.', ephemeral: true });
      return;
    }

    const targetUser = interaction.options.getUser('user', true);
    const userData = getOrCreateUser(targetUser.id, targetUser.tag);
    const rawHistory = Array.isArray(userData.betHistory) ? [...userData.betHistory] : [];

    if (rawHistory.length === 0) {
      await interaction.reply({ content: 'That user has no betting history to graph.', ephemeral: true });
      return;
    }

    // Sort by timestamp and take the most recent 100 entries to keep the chart URL reasonable
    rawHistory.sort((a, b) => (a.ts || 0) - (b.ts || 0));
    const history = rawHistory.slice(-100);

    const labels = [];
    const data = [];
    let cumulative = 0;
    let maxAbs = 0;

    const startIndex = rawHistory.length - history.length;
    history.forEach((entry, idx) => {
      const amount = typeof entry.amount === 'number' ? entry.amount : 0;
      const payout = typeof entry.payout === 'number' ? entry.payout : 0;
      let change = 0;

      if (entry.outcome === 'win') {
        change = payout - amount;
      } else if (entry.outcome === 'loss') {
        change = -amount;
      } else if (entry.outcome === 'refund') {
        change = 0;
      }

      cumulative += change;
      labels.push(`Bet ${startIndex + idx + 1}`);
      data.push(cumulative);
      const abs = Math.abs(cumulative);
      if (abs > maxAbs) maxAbs = abs;
    });

    if (data.length === 0) {
      await interaction.reply({ content: 'Could not build a graph because this user has no valid bets.', ephemeral: true });
      return;
    }

    // Rescale Y-axis into k / m / b units for readability
    let divisor = 1;
    let yLabel = 'P/L (GP)';
    if (maxAbs >= 1_000_000_000) {
      divisor = 1_000_000_000;
      yLabel = 'P/L (b GP)';
    } else if (maxAbs >= 1_000_000) {
      divisor = 1_000_000;
      yLabel = 'P/L (m GP)';
    } else if (maxAbs >= 1_000) {
      divisor = 1_000;
      yLabel = 'P/L (k GP)';
    }

    const scaledData = data.map(v => Math.round((v / divisor) * 100) / 100);

    const chartConfig = {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Cumulative P/L',
            data: scaledData,
            borderColor: 'rgba(0, 255, 200, 0.95)',
            backgroundColor: 'rgba(0, 255, 200, 0.18)',
            borderWidth: 3,
            fill: true,
            tension: 0.35,
            pointRadius: 0,
            pointHoverRadius: 4,
            pointHitRadius: 6,
            pointBackgroundColor: 'rgba(0, 255, 200, 0.95)',
          },
        ],
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            display: false,
          },
          title: {
            display: true,
            text: 'Cumulative Profit / Loss',
            color: '#ffffff',
          },
        },
        scales: {
          x: {
            display: true,
            title: {
              display: true,
              text: 'Bet number',
              color: '#aaaaaa',
            },
            ticks: { color: '#888888' },
            grid: { color: 'rgba(255, 255, 255, 0.08)' },
          },
          y: {
            display: true,
            title: {
              display: true,
              text: yLabel,
              color: '#aaaaaa',
            },
            ticks: { color: '#888888' },
            grid: { color: 'rgba(255, 255, 255, 0.08)' },
          },
        },
      },
    };

    const chartUrl = 'https://quickchart.io/chart?c=' + encodeURIComponent(JSON.stringify(chartConfig));

    const finalPl = cumulative;
    const plAbs = Math.abs(finalPl);
    const plLabelPrefix = finalPl > 0 ? 'Profit' : finalPl < 0 ? 'Loss' : 'Break-even';
    const plValue = finalPl === 0
      ? 'Break-even'
      : `${finalPl > 0 ? '+' : '-'}${formatAmountFull(plAbs)}`;

    const embed = new EmbedBuilder()
      .setTitle(`📈 P/L Graph for ${targetUser.tag}`)
      .setDescription('Cumulative profit / loss over recent bets')
      .addFields(
        {
          name: 'Current P/L',
          value: `${plLabelPrefix}: **${plValue}**`,
          inline: false,
        },
        {
          name: 'Bets graphed',
          value: data.length.toString(),
          inline: true,
        },
      )
      .setImage(chartUrl)
      .setColor(finalPl >= 0 ? 0x00FF00 : 0xFF0000)
      .setTimestamp(new Date());

    await interaction.reply({ embeds: [embed] });
  }
  else if (interaction.commandName === 'settings') {
    if (!isAdminInteraction(interaction)) {
      await interaction.reply({ content: 'You do not have permission to use this command.', ephemeral: true });
      return;
    }

    const sub = interaction.options.getSubcommand();
    if (sub === 'betting_channel') {
      const channel = interaction.options.getChannel('channel', true);
      updateSettings({ bettingChannelId: channel.id });
      await interaction.reply({ content: `Betting channel set to <#${channel.id}>.`, ephemeral: true });
    } else if (sub === 'results_channel') {
      const channel = interaction.options.getChannel('channel', true);
      updateSettings({ resultsChannelId: channel.id });
      await interaction.reply({ content: `Results channel set to <#${channel.id}>.`, ephemeral: true });
    } else if (sub === 'logo_url') {
      const url = interaction.options.getString('url', true).trim();
      updateSettings({ logoUrl: url });
      await interaction.reply({ content: 'Logo URL updated.', ephemeral: true });
    } else if (sub === 'panel_thumbnail') {
      const url = interaction.options.getString('url', true).trim();
      updateSettings({ panelThumbnailUrl: url });
      await interaction.reply({ content: 'Panel thumbnail URL updated.', ephemeral: true });
    } else if (sub === 'reset') {
      const existing = getSettings();
      updateSettings({
        bettingChannelId: existing.bettingChannelId,
        resultsChannelId: existing.resultsChannelId,
        panelThumbnailUrl: null,
        logoUrl: null,
      });
      currentRound = null;
      saveRoundMeta(null);
      await interaction.reply({ content: 'Settings reset and active round cleared.', ephemeral: true });
    }
  }
  }

  // ----- Button interactions -----
  else if (interaction.isButton()) {
    const userId = interaction.user.id;
    const customId = interaction.customId;

    // Handle ticket creation button
    if (customId === 'create_ticket') {
      if (tickets.hasActiveTicket(userId)) {
        const activeTicket = tickets.getActiveTicket(userId);
        await interaction.reply({
          content: `❌ You already have an open ticket: <#${activeTicket.channelId}>`,
          ephemeral: true
        });
        return;
      }
      
      await interaction.deferReply({ ephemeral: true });
      
      try {
        const adminRoleId = getSettings().adminRoleId;
        const { channel, ticketNumber } = await tickets.createTicketChannel(
          interaction.guild,
          interaction.user,
          adminRoleId
        );
        
        // Send welcome message in ticket
        const welcomeEmbed = new EmbedBuilder()
          .setTitle(`🎫 Support Ticket #${ticketNumber}`)
          .setDescription(
            `Welcome ${interaction.user}!\n\n` +
            `Thank you for creating a support ticket. A staff member will assist you shortly.\n\n` +
            `**Please provide:**\n` +
            `• Your in-game RSN (if applicable)\n` +
            `• Transaction IDs (for deposits/withdrawals)\n` +
            `• Screenshots or evidence\n` +
            `• Detailed description of your issue\n\n` +
            `**To close this ticket:** Click the button below when your issue is resolved.`
          )
          .setColor(0x57F287)
          .setTimestamp();
        
        const closeButton = new ActionRowBuilder()
          .addComponents(
            new ButtonBuilder()
              .setCustomId('close_ticket')
              .setLabel('Close Ticket')
              .setEmoji('🔒')
              .setStyle(ButtonStyle.Danger)
          );
        
        await channel.send({
          content: `${interaction.user} | <@&${adminRoleId}>`,
          embeds: [welcomeEmbed],
          components: [closeButton]
        });
        
        await interaction.editReply({
          content: `✅ Ticket created! Please check ${channel}`,
        });
        
        console.log(`[Tickets] ${interaction.user.tag} created ticket #${ticketNumber} (${channel.name})`);
        
      } catch (err) {
        console.error('Error creating ticket:', err);
        await interaction.editReply({
          content: `❌ Failed to create ticket: ${err.message}`,
        });
      }
      
      return;
    }

    // Handle ticket closing button
    if (customId === 'close_ticket') {
      const ticketInfo = tickets.getTicketByChannel(interaction.channel.id);
      
      if (!ticketInfo) {
        await interaction.reply({ content: '❌ This is not a valid ticket channel.', ephemeral: true });
        return;
      }
      
      const isAdmin = isAdminInteraction(interaction);
      const isTicketOwner = interaction.user.id === ticketInfo.userId;
      
      if (!isAdmin && !isTicketOwner) {
        await interaction.reply({ content: '❌ Only the ticket owner or admins can close this ticket.', ephemeral: true });
        return;
      }
      
      await interaction.deferReply();
      
      try {
        // Generate transcript
        await interaction.editReply({ content: '⏳ Generating transcript...' });
        const transcript = await tickets.generateTranscript(interaction.channel);
        const { filename, filepath } = await tickets.saveTranscript(interaction.channel, transcript);
        
        // Send transcript to user
        try {
          const user = await client.users.fetch(ticketInfo.userId);
          await user.send({
            content: `🎫 Your support ticket **#${ticketInfo.ticketNumber}** has been closed.`,
            files: [{
              attachment: filepath,
              name: filename
            }]
          });
        } catch (dmErr) {
          console.log(`[Tickets] Could not DM transcript to user ${ticketInfo.userId}: ${dmErr.message}`);
        }
        
        // Update channel message
        await interaction.editReply({ 
          content: `🔒 Ticket closed by ${interaction.user}\n` +
                   `📝 Transcript saved: \`${filename}\`\n` +
                   `⏰ This channel will be deleted in 10 seconds...`
        });
        
        // Remove from active tickets
        tickets.closeTicket(ticketInfo.userId);
        
        // Delete channel after delay
        setTimeout(async () => {
          try {
            await interaction.channel.delete();
            console.log(`[Tickets] Deleted ticket channel #${ticketInfo.ticketNumber}`);
          } catch (delErr) {
            console.error('Error deleting ticket channel:', delErr);
          }
        }, 10000);
        
      } catch (err) {
        console.error('Error closing ticket:', err);
        await interaction.editReply({
          content: `❌ Failed to close ticket: ${err.message}`,
        });
      }
      
      return;
    }

    // Handle check user command buttons (admin only)
    if (customId.startsWith('check_')) {
      if (!isAdminInteraction(interaction)) {
        await interaction.reply({ content: 'You do not have permission to use this.', ephemeral: true });
        return;
      }
      
      // Handle manual link RSN button (opens modal)
      if (customId.startsWith('check_link_rsn_')) {
        const targetUserId = customId.replace('check_link_rsn_', '');
        
        const modal = new ModalBuilder()
          .setCustomId(`modal_link_rsn_${targetUserId}`)
          .setTitle('Manual Link RSN');
        
        const rsnInput = new TextInputBuilder()
          .setCustomId('rsn_input')
          .setLabel('Enter RSN to link')
          .setStyle(TextInputStyle.Short)
          .setPlaceholder('e.g., PlayerName123')
          .setRequired(true)
          .setMaxLength(12);
        
        const row = new ActionRowBuilder().addComponents(rsnInput);
        modal.addComponents(row);
        
        await interaction.showModal(modal);
        return;
      }
      
      const [_, view, targetUserId] = customId.split('_');
      
      if (!client.checkUserData || !client.checkUserData.has(targetUserId)) {
        await interaction.update({
          content: '❌ Data expired. Please run `/check` again.',
          components: [],
          embeds: []
        });
        return;
      }
      
      const { data } = client.checkUserData.get(targetUserId);
      let embed;
      
      switch(view) {
        case 'overview':
          embed = checkUser.createOverviewEmbed(data, client);
          break;
        case 'bets':
          embed = checkUser.createBetHistoryEmbed(data, 0);
          break;
        case 'gp':
          embed = checkUser.createGPTransactionsEmbed(data);
          break;
        case 'crypto':
          embed = checkUser.createCryptoTransactionsEmbed(data);
          break;
        case 'pending':
          embed = checkUser.createPendingTransactionsEmbed(data);
          break;
        default:
          embed = checkUser.createOverviewEmbed(data, client);
      }
      
      const buttons = checkUser.createButtonRow(targetUserId);
      
      await interaction.update({
        embeds: [embed],
        components: buttons
      });
      
      return;
    }

    // Handle withdrawal confirmation buttons (always check these first, regardless of betting round)
    if (customId.startsWith('withdraw_confirm_')) {
      client.pendingWithdrawals = client.pendingWithdrawals || new Map();
      const withdrawalData = client.pendingWithdrawals.get(userId);
      
      if (!withdrawalData) {
        await interaction.update({
          content: 'Withdrawal expired or already processed. Please try again.',
          components: [],
          embeds: []
        });
        return;
      }
      
      client.pendingWithdrawals.delete(userId);
      
      await interaction.update({
        content: '⏳ Processing withdrawal...',
        components: [],
        embeds: []
      });
      
      try {
        // Deduct GP immediately
        const newBalance = adjustBalance(userId, -withdrawalData.requiredGp, {
          displayName: interaction.user.tag,
          reason: 'crypto-withdrawal'
        });
        
        // Create withdrawal record
        const withdrawalId = `WD-${Date.now()}-${userId.slice(-6)}`;
        recordCryptoWithdrawal(userId, {
          withdrawal_id: withdrawalId,
          amount_gp: withdrawalData.requiredGp,
          amount_usd: withdrawalData.amountUsd,
          currency: withdrawalData.currency,
          address: withdrawalData.address,
          metadata: {
            user_tag: interaction.user.tag,
            requested_at: Date.now()
          }
        });
        
        console.log(`Crypto withdrawal initiated: ${interaction.user.tag} - $${withdrawalData.amountUsd} (${withdrawalData.requiredGp} GP) - WD: ${withdrawalId}`);
        
        // Attempt to send via Plisio
        let payoutResult = null;
        let payoutError = null;
        
        try {
          payoutResult = await createPayout({
            currency: withdrawalData.currency,
            address: withdrawalData.address,
            amount: withdrawalData.amountUsd
          });
          
          updateCryptoWithdrawalStatus(
            withdrawalId, 
            'processing', 
            payoutResult.txn_id || payoutResult.id
          );
          
          console.log(`✅ Plisio payout created: ${withdrawalId} - TXN: ${payoutResult.txn_id || payoutResult.id}`);
          
          await interaction.followUp({
            content:
              `✅ **Withdrawal submitted!**\n` +
              `ID: \`${withdrawalId}\`\n` +
              `Amount: ${formatAmountFull(withdrawalData.requiredGp)} → $${withdrawalData.amountUsd.toFixed(2)} ${withdrawalData.displayCurrency || 'USDT'}\n` +
              `Status: **Processing**\n\n` +
              `Your crypto will arrive in 5-15 minutes.\n` +
              `New balance: **${formatAmountFull(newBalance)}**`,
            ephemeral: true
          });
          
          // Notify user when complete
          setTimeout(async () => {
            try {
              const user = await client.users.fetch(userId);
              await user.send(
                `🎉 **Crypto withdrawal complete!**\n` +
                `Amount: $${withdrawalData.amountUsd.toFixed(2)} ${withdrawalData.displayCurrency || 'crypto'} sent to your wallet.\n` +
                `Address: \`${withdrawalData.address}\`\n` +
                `Transaction ID: \`${withdrawalId}\``
              );
            } catch (dmErr) {
              console.error(`Failed to DM user ${userId} about withdrawal completion:`, dmErr.message);
            }
          }, 60000); // Notify after 1 minute
          
        } catch (payoutErr) {
          payoutError = payoutErr;
          console.error('Plisio payout error:', payoutErr);
          
          // Mark as failed but keep GP deducted (admin can process manually)
          updateCryptoWithdrawalStatus(withdrawalId, 'failed');
          
          await interaction.followUp({
            content:
              `⚠️ **Withdrawal pending manual processing**\n` +
              `ID: \`${withdrawalId}\`\n` +
              `Amount: $${withdrawalData.amountUsd.toFixed(2)} ${withdrawalData.displayCurrency || 'crypto'}\n` +
              `Address: \`${withdrawalData.address}\`\n\n` +
              `Your GP has been deducted. An admin will process your withdrawal manually within 24 hours.\n` +
              `Error: ${payoutErr.message}`,
            ephemeral: true
          });
          
          // Alert admin
          console.error(`❌ MANUAL WITHDRAWAL NEEDED: ${withdrawalId} - ${payoutErr.message}`);
        }
        
      } catch (error) {
        console.error('Withdrawal processing error:', error);
        await interaction.followUp({
          content: 'Error processing withdrawal. Please contact support. Your balance was not deducted.',
          ephemeral: true
        });
      }
      return;
    }
    
    if (customId.startsWith('withdraw_cancel_')) {
      client.pendingWithdrawals = client.pendingWithdrawals || new Map();
      client.pendingWithdrawals.delete(userId);
      
      await interaction.update({
        content: 'Withdrawal cancelled.',
        components: [],
        embeds: []
      });
      return;
    }

    if (!currentRound || !currentRound.open) {
      await interaction.reply({ content: 'Betting window is closed. Please wait for the next round.', ephemeral: true });
      setTimeout(() => {
        interaction.deleteReply().catch(() => {});
      }, 20_000);
      return;
    }

    if (customId === 'bet_red_button' || customId === 'bet_blue_button') {
      const side = customId === 'bet_red_button' ? 'red' : 'blue';
      const modal = new ModalBuilder()
        .setCustomId(`bet_modal_${side}`)
        .setTitle(`Bet on ${side.toUpperCase()}`)
        .addComponents(
          new ActionRowBuilder().addComponents(
            new TextInputBuilder()
              .setCustomId('bet_amount')
              .setLabel('Bet amount (min 1m, max 1b)')
              .setStyle(TextInputStyle.Short)
              .setRequired(true),
          ),
        );
      await interaction.showModal(modal);
      return;
    }

    if (customId === 'cancel_bet_button') {
      const existing = findUserBet(userId);
      if (!existing) {
        await interaction.reply({ content: 'You do not have an active bet this round.', ephemeral: true });
        setTimeout(() => {
          interaction.deleteReply().catch(() => {});
        }, 20_000);
        return;
      }

      adjustBalance(userId, existing.amount, {
        reason: 'bet-cancel',
      });
      removeUserBet(userId);

      await interaction.reply({
        content: `Your bet of **${formatAmountShort(existing.amount)}** has been cancelled and refunded.`,
        ephemeral: true,
      });
      return;
    }

    if (customId === 'cancel_queued_bet_button') {
      const queuedBet = queuedBets.get(userId);
      
      if (!queuedBet) {
        await interaction.update({
          content: '❌ You don\'t have any bets queued for the next round.',
          embeds: [],
          components: []
        });
        return;
      }
      
      // Remove from queue
      queuedBets.delete(userId);
      
      const embed = new EmbedBuilder()
        .setTitle('🗑️ Queued Bet Cancelled')
        .setDescription(
          `Your queued bet has been cancelled.\n\n` +
          `**Amount:** ${formatAmountFull(queuedBet.amount)}\n` +
          `**Side:** ${queuedBet.side.toUpperCase()}\n\n` +
          `You can place a new bet when the next round opens.`
        )
        .setColor(0xFF9900)
        .setTimestamp();
      
      await interaction.update({ embeds: [embed], components: [] });
      console.log(`[Queue] ${interaction.user.tag} cancelled queued bet: ${formatAmountFull(queuedBet.amount)} on ${queuedBet.side}`);
      return;
    }

    if (customId === 'change_bet_button') {
      const existing = findUserBet(userId);
      if (!existing) {
        await interaction.reply({ content: 'You do not have an active bet this round. Place a bet first.', ephemeral: true });
        setTimeout(() => {
          interaction.deleteReply().catch(() => {});
        }, 20_000);
        return;
      }

      const modal = new ModalBuilder()
        .setCustomId('change_bet_modal')
        .setTitle('Change your bet')
        .addComponents(
          new ActionRowBuilder().addComponents(
            new TextInputBuilder()
              .setCustomId('bet_amount')
              .setLabel('New bet amount (min 1m, max 1b)')
              .setStyle(TextInputStyle.Short)
              .setRequired(true),
          ),
          new ActionRowBuilder().addComponents(
            new TextInputBuilder()
              .setCustomId('bet_side')
              .setLabel('Side (red or blue)')
              .setStyle(TextInputStyle.Short)
              .setRequired(true),
          ),
        );
      await interaction.showModal(modal);
      return;
    }

    if (customId === 'repeat_bet_button') {
      const user = getOrCreateUser(userId, interaction.user.tag);
      const history = Array.isArray(user.betHistory) ? user.betHistory : [];

      if (history.length === 0) {
        await interaction.reply({ content: 'You do not have any previous bets to repeat.', ephemeral: true });
        setTimeout(() => {
          interaction.deleteReply().catch(() => {});
        }, 20_000);
        return;
      }

      const lastBet = [...history].reverse().find(entry => entry.side === 'red' || entry.side === 'blue');
      if (!lastBet) {
        await interaction.reply({ content: 'Your last bet does not have a valid side to repeat.', ephemeral: true });
        setTimeout(() => {
          interaction.deleteReply().catch(() => {});
        }, 20_000);
        return;
      }

      const amount = lastBet.amount;
      const side = lastBet.side;

      if (!amount || amount <= 0) {
        await interaction.reply({ content: 'Your last bet amount is invalid and cannot be repeated.', ephemeral: true });
        setTimeout(() => {
          interaction.deleteReply().catch(() => {});
        }, 20_000);
        return;
      }

      const existing = findUserBet(userId);
      let effectiveBalance = user.balance || 0;
      if (existing) {
        effectiveBalance += existing.amount; // they effectively get this back if changing
      }

      if (effectiveBalance < amount) {
        await interaction.reply({
          content: `Insufficient balance to repeat your last bet. You have ${formatAmountFull(effectiveBalance)} available (including your current bet).`,
          ephemeral: true,
        });
        setTimeout(() => {
          interaction.deleteReply().catch(() => {});
        }, 20_000);
        return;
      }

      // Refund existing bet if any
      if (existing) {
        adjustBalance(userId, existing.amount, {
          reason: 'bet-change-refund',
        });
      }

      const balanceBefore = getOrCreateUser(userId, interaction.user.tag).balance || 0;
      const balanceAfter = adjustBalance(userId, -amount, {
        displayName: interaction.user.tag,
        reason: 'bet',
      });
      upsertUserBet(userId, amount, side);

      const color = side === 'red' ? 0xFF0000 : 0x0000FF;

      const embed = new EmbedBuilder()
        .setTitle(existing ? '💱 Bet changed (repeat)' : '💰 Bet placed (repeat)')
        .setDescription(`Round \`${currentRound.roundId}\``)
        .addFields(
          {
            name: '🎲 Side',
            value: side.toUpperCase(),
            inline: true,
          },
          {
            name: '💰 Amount',
            value: formatAmountShort(amount),
            inline: true,
          },
          {
            name: '📉 Balance before',
            value: formatAmountFull(balanceBefore),
            inline: false,
          },
          {
            name: '📈 Balance after',
            value: formatAmountFull(balanceAfter),
            inline: false,
          },
        )
        .setColor(color)
        .setTimestamp(new Date());

      await interaction.reply({
        embeds: [embed],
        ephemeral: true,
      });

      setTimeout(() => {
        interaction.deleteReply().catch(() => {});
      }, 20_000);
      return;
    }
  }

  // ----- Modal submissions -----
  else if (interaction.isModalSubmit()) {
    const customId = interaction.customId;
    const userId = interaction.user.id;
    
    // Handle manual link RSN modal (admin only)
    if (customId.startsWith('modal_link_rsn_')) {
      if (!isAdminInteraction(interaction)) {
        await interaction.reply({ content: 'You do not have permission to use this.', ephemeral: true });
        return;
      }
      
      const targetUserId = customId.replace('modal_link_rsn_', '');
      const rsn = interaction.fields.getTextInputValue('rsn_input').trim();
      
      if (!rsn) {
        await interaction.reply({ content: '❌ RSN cannot be empty.', ephemeral: true });
        return;
      }
      
      try {
        // Link the RSN
        linkRsn(targetUserId, rsn);
        
        // Fetch updated data
        const data = getUserComprehensiveData(targetUserId);
        
        // Update stored data
        if (client.checkUserData) {
          client.checkUserData.set(targetUserId, { data, timestamp: Date.now() });
        }
        
        // Create updated overview embed
        const embed = checkUser.createOverviewEmbed(data, client);
        const buttons = checkUser.createButtonRow(targetUserId);
        
        await interaction.reply({
          content: `✅ Successfully linked RSN \`${rsn}\` to <@${targetUserId}>`,
          embeds: [embed],
          components: buttons,
          ephemeral: true
        });
        
        console.log(`[Admin] ${interaction.user.tag} manually linked RSN "${rsn}" to user ${targetUserId}`);
        
      } catch (err) {
        console.error('Manual link RSN error:', err);
        await interaction.reply({
          content: `❌ Failed to link RSN: ${err.message}`,
          ephemeral: true
        });
      }
      
      return;
    }
    
    // Parse bet amount first
    const rawAmount = interaction.fields.getTextInputValue('bet_amount');
    let amount = null;
    // Accept plain integers or K/M/B style amounts.
    if (/^\d+$/.test(rawAmount.trim())) {
      amount = parseInt(rawAmount.trim(), 10);
    } else {
      amount = parseAmountWithSuffix(rawAmount);
    }

    if (!amount || isNaN(amount)) {
      await interaction.reply({ content: 'Invalid amount. Use a number between 1,000,000 and 1,000,000,000 (or formats like 1m, 500k).', ephemeral: true });
      setTimeout(() => {
        interaction.deleteReply().catch(() => {});
      }, 20_000);
      return;
    }

    if (amount < 1_000_000 || amount > 1_000_000_000) {
      await interaction.reply({ content: 'Bet amount must be between 1m and 1b.', ephemeral: true });
      setTimeout(() => {
        interaction.deleteReply().catch(() => {});
      }, 20_000);
      return;
    }

    // Determine side
    let side = null;
    if (customId === 'bet_modal_red') {
      side = 'red';
    } else if (customId === 'bet_modal_blue') {
      side = 'blue';
    } else if (customId === 'change_bet_modal') {
      const sideInput = interaction.fields.getTextInputValue('bet_side');
      const s = sideInput.trim().toLowerCase();
      if (s !== 'red' && s !== 'blue') {
        await interaction.reply({ content: 'Side must be "red" or "blue".', ephemeral: true });
        setTimeout(() => {
          interaction.deleteReply().catch(() => {});
        }, 20_000);
        return;
      }
      side = s;
    } else {
      await interaction.reply({ content: 'Unknown bet modal.', ephemeral: true });
      setTimeout(() => {
        interaction.deleteReply().catch(() => {});
      }, 20_000);
      return;
    }

    // Check balance
    const user = getOrCreateUser(userId, interaction.user.tag);
    if ((user.balance || 0) < amount) {
      await interaction.reply({
        content: `Insufficient balance. You have ${formatAmountFull(user.balance || 0)}.`,
        ephemeral: true,
      });
      setTimeout(() => {
        interaction.deleteReply().catch(() => {});
      }, 20_000);
      return;
    }

    // If betting is closed, queue the bet for next round
    if (!currentRound || !currentRound.open) {
      const existingBet = queuedBets.get(userId);
      
      queuedBets.set(userId, {
        amount,
        side,
        displayName: interaction.user.tag
      });
      
      const color = side === 'red' ? 0xFF0000 : 0x0000FF;
      const embed = new EmbedBuilder()
        .setTitle('⏳ Bet Queued for Next Round')
        .setDescription(
          `Betting is currently closed, so your bet has been queued.\n\n` +
          `**Amount:** ${formatAmountFull(amount)}\n` +
          `**Side:** ${side.toUpperCase()}\n\n` +
          `✅ Your bet will automatically be placed when the next round opens!` +
          (existingBet ? `\n\n⚠️ Note: This replaced your previous queued bet of ${formatAmountFull(existingBet.amount)} on ${existingBet.side.toUpperCase()}` : '')
        )
        .setColor(color)
        .setFooter({ text: `Balance: ${formatAmountFull(user.balance)} • Use /queued-bet to view/cancel` })
        .setTimestamp();
      
      await interaction.reply({ embeds: [embed], ephemeral: true });
      console.log(`[Queue] ${interaction.user.tag} queued ${formatAmountFull(amount)} on ${side} (via panel)${existingBet ? ' (replaced existing)' : ''}`);
      return;
    }

    // Adjust balance taking existing bet into account.
    const existing = findUserBet(userId);
    let effectiveBalance = user.balance || 0;
    if (existing) {
      effectiveBalance += existing.amount; // they get refunded first logically
    }

    if (effectiveBalance < amount) {
      await interaction.reply({
        content: `Insufficient balance for this change. You have ${formatAmountFull(effectiveBalance)} available (including your current bet).`,
        ephemeral: true,
      });
      setTimeout(() => {
        interaction.deleteReply().catch(() => {});
      }, 20_000);
      return;
    }

    // Refund existing bet if any
    if (existing) {
      adjustBalance(userId, existing.amount, {
        reason: 'bet-change-refund',
      });
    }

    // Deduct new amount
    const balanceBefore = getOrCreateUser(userId, interaction.user.tag).balance || 0;
    const balanceAfter = adjustBalance(userId, -amount, {
      displayName: interaction.user.tag,
      reason: 'bet',
    });
    upsertUserBet(userId, amount, side);

    const color = side === 'red' ? 0xFF0000 : 0x0000FF;

    const embed = new EmbedBuilder()
      .setTitle(existing ? '💱 Bet changed' : '💰 Bet placed')
      .setDescription(`Round \`${currentRound.roundId}\``)
      .addFields(
        {
          name: '🎲 Side',
          value: side.toUpperCase(),
          inline: true,
        },
        {
          name: '💰 Amount',
          value: formatAmountShort(amount),
          inline: true,
        },
        {
          name: '📉 Balance before',
          value: formatAmountFull(balanceBefore),
          inline: false,
        },
        {
          name: '📈 Balance after',
          value: formatAmountFull(balanceAfter),
          inline: false,
        },
      )
      .setColor(color)
      .setTimestamp(new Date());

    await interaction.reply({
      embeds: [embed],
      ephemeral: true,
    });

    // Auto-remove bet confirmation after 20 seconds
    setTimeout(() => {
      interaction.deleteReply().catch(() => {});
    }, 20_000);
  }
});

// ---------- Express HTTP server ----------
const app = express();
app.use(express.json());

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.post('/api/link/verify', async (req, res) => {
  const { code, rsn } = req.body || {};
  if (!code || !rsn) {
    return res.status(400).json({ error: 'Missing code or rsn' });
  }

  const rawCode = typeof code === 'string' ? code.trim() : '';
  const normalized = rawCode.toUpperCase();
  const pending = pendingLinks.get(normalized);
  if (!pending) {
    return res.status(400).json({ error: 'Invalid or expired code' });
  }

  const discordUserId = pending.discordUserId;
  pendingLinks.delete(normalized);

  linkRsn(discordUserId, rsn);

  try {
    const user = await client.users.fetch(discordUserId);
    await user.send(`Linked RSN \`${rsn}\` to your Discord account for betting.`);
  } catch (err) {
    console.error('Failed to DM link confirmation:', err);
  }

  console.log(`Linked RSN "${rsn}" to Discord user ${discordUserId}`);
  return res.json({ status: 'ok' });
});

// RSN-based bank endpoints for VitaLite NoidBets deposit/withdraw bot mode
app.post('/api/bank/deposit', async (req, res) => {
  try {
    const { rsn, amount } = req.body || {};
    console.log(`[API] /api/bank/deposit called with RSN: "${rsn}", amount: ${amount}`);
    
    if (!rsn || amount === undefined) {
      return res.status(400).json({ error: 'Missing rsn or amount' });
    }

    const user = findUserByRsn(rsn);
    if (!user) {
      console.log(`[API] ❌ Deposit FAILED: RSN "${rsn}" not linked`);
      return res.status(404).json({ error: 'RSN not linked to any Discord user' });
    }
    
    console.log(`[API] ✓ Found user for RSN "${rsn}": ${user.id} (${user.displayName})`);

    const numAmount = Number(amount);
    if (!isFinite(numAmount) || numAmount <= 0) {
      return res.status(400).json({ error: 'Invalid amount' });
    }

    const newBalance = adjustBalance(user.id, numAmount, {
      displayName: user.displayName || null,
      reason: 'gp-deposit',
    });

    return res.json({ status: 'ok', discordUserId: user.id, newBalance });
  } catch (err) {
    console.error('Error in /api/bank/deposit:', err);
    return res.status(500).json({ error: 'Internal error' });
  }
});

// Send DM notification to user by RSN
app.post('/api/bank/notify', async (req, res) => {
  try {
    const { rsn, message } = req.body || {};
    console.log(`[API] /api/bank/notify called with RSN: "${rsn}"`);
    
    if (!rsn || !message) {
      return res.status(400).json({ error: 'Missing rsn or message' });
    }

    const user = findUserByRsn(rsn);
    if (!user) {
      console.log(`[API] ❌ Notify FAILED: RSN "${rsn}" not linked`);
      return res.status(404).json({ error: 'RSN not linked to any Discord user' });
    }
    
    console.log(`[API] ✓ Found user for RSN "${rsn}": ${user.id}`);

    try {
      const discordUser = await client.users.fetch(user.id);
      await discordUser.send(message);
      return res.json({ status: 'ok', sent: true });
    } catch (dmErr) {
      console.error('Failed to send DM to user:', dmErr);
      return res.json({ status: 'ok', sent: false, error: 'DM failed' });
    }
  } catch (err) {
    console.error('Error in /api/bank/notify:', err);
    return res.status(500).json({ error: 'Internal error' });
  }
});

// Check user balance by RSN (for withdrawal validation)
app.post('/api/bank/check-balance', async (req, res) => {
  try {
    const { rsn } = req.body || {};
    console.log(`[API] /api/bank/check-balance called with RSN: "${rsn}"`);
    
    if (!rsn) {
      return res.status(400).json({ error: 'Missing rsn' });
    }

    const user = findUserByRsn(rsn);
    if (!user) {
      console.log(`[API] ❌ Balance check FAILED: RSN "${rsn}" not linked`);
      return res.status(404).json({ error: 'RSN not linked to any Discord user' });
    }
    
    console.log(`[API] ✓ Balance check SUCCESS for RSN "${rsn}": ${user.balance} GP`);

    const balance = Number(user.balance || 0);
    return res.json({ status: 'ok', balance: balance });
  } catch (err) {
    console.error('Error in /api/bank/check-balance:', err);
    return res.status(500).json({ error: 'Internal error' });
  }
});

app.post('/api/bank/withdraw', async (req, res) => {
  try {
    const { rsn, amount } = req.body || {};
    if (!rsn || amount === undefined) {
      return res.status(400).json({ error: 'Missing rsn or amount' });
    }

    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      return res.status(400).json({ error: 'Invalid amount' });
    }

    const user = findUserByRsn(rsn);
    if (!user) {
      return res.status(400).json({ error: 'RSN not linked' });
    }

    const currentBalance = Number(user.balance || 0);
    const requested = Math.floor(numericAmount);
    if (!Number.isFinite(currentBalance) || currentBalance <= 0 || currentBalance < requested) {
      return res.status(400).json({ error: 'Insufficient balance', balance: currentBalance });
    }

    const newBalance = adjustBalance(user.id, -requested, {
      displayName: user.displayName || null,
      reason: 'gp-withdraw',
    });

    return res.json({ status: 'ok', discordUserId: user.id, amount: requested, newBalance });
  } catch (err) {
    console.error('Error in /api/bank/withdraw:', err);
    return res.status(500).json({ error: 'Internal error' });
  }
});

app.post('/api/duel/round-created', async (req, res) => {
  const { roundId, player1, player2 } = req.body || {};
  if (!roundId || !player1 || !player2) {
    return res.status(400).json({ error: 'Missing roundId/player1/player2' });
  }

  if (!client.isReady()) {
    return res.status(503).json({ error: 'Discord client not ready' });
  }

  try {
    const settings = getSettings();
    const channel = await client.channels.fetch(settings.bettingChannelId);
    if (!channel || !channel.isTextBased()) {
      return res.status(500).json({ error: 'Betting channel invalid' });
    }

    const stats = getStats();
    const history = Array.isArray(stats.lastWinners) ? stats.lastWinners : [];
    const last50 = history.slice(-50);
    const redLastCount = last50.filter(w => w === 'red').length;
    const blueLastCount = last50.filter(w => w === 'blue').length;
    const embed = new EmbedBuilder()
      .setTitle(`💥 Duel Arena Bets - Round ${roundId}`)
      .setDescription('Betting window: **30s** remaining.')
      .addFields(
        {
          name: '🟥 Red side',
          value: `👊 Fighters:\n${player1}\n\n💰 Bets:\nNone yet`,
          inline: true,
        },
        {
          name: '🟦 Blue side',
          value: `👊 Fighters:\n${player2}\n\n💰 Bets:\nNone yet`,
          inline: true,
        },
        {
          name: '🔥 Streaks',
          value: `Red streak: ${stats.redStreak} | Blue streak: ${stats.blueStreak}`,
          inline: false,
        },
        {
          name: '📈 Last 50',
          value: `Red: ${redLastCount} | Blue: ${blueLastCount}`,
          inline: false,
        },
      )
      .setColor(0x00FF00) // green while betting is open
      .setTimestamp(new Date());

    const panelThumb = settings.panelThumbnailUrl || settings.logoUrl;
    if (panelThumb) {
      embed.setThumbnail(panelThumb);
    }

    // Add animated banner GIF inside the betting panel embed
    embed.setImage('https://dswa1xdat8uez.cloudfront.net/2861g%2Fpreview%2F72971504%2Fmain_full.gif?response-content-disposition=inline%3Bfilename%3D%22main_full.gif%22%3B&response-content-type=image%2Fgif&Expires=1763659081&Signature=h20GYSaNjrZUEiVlewO97YQEwMcIYGycE9b-5m-7zCGatTLMEtyJWkmBMrusc3sWVFlzlsOK669S8kAixDICqY0YP2Mc4~7K7w801iTIaSRB7YplsDGBfJMYRLDTxgrxi7TZqLrHMxSgu4pr5prFnMCGEvkJnkC-kErHZZeX2wjHRF0wALYgJzCMYWEmt~tG3d0h1EoVtLxQrKrPFzf5pYlCzcQ7uzNxXj5M1GZ3-PoJe2CcAacZlr1jOBXYF8~LtwiA5oelJdA~GW0acLP-UWchj0XdXyPZUxzbZtXmWz~wA8wKsw-i333H21xZFT9uAB6mt1hf-AjaXALA7N3RoA__&Key-Pair-Id=APKAJT5WQLLEOADKLHBQ');

    let msg = null;
    if (settings.panelMessageId) {
      try {
        msg = await channel.messages.fetch(settings.panelMessageId);
        await msg.edit({ embeds: [embed], components: [buildBetButtons(false)] });
      } catch (e) {
        console.warn('Stored panelMessageId invalid, sending new panel message:', e);
        msg = await channel.send({ embeds: [embed], components: [buildBetButtons(false)] });
        updateSettings({ panelMessageId: msg.id });
      }
    } else {
      msg = await channel.send({ embeds: [embed], components: [buildBetButtons(false)] });
      updateSettings({ panelMessageId: msg.id });
    }

    currentRound = {
      roundId,
      red: player1,
      blue: player2,
      open: true,
      bets: [],
      messageId: msg.id,
      channelId: msg.channelId,
    };
    saveRoundMeta({
      roundId,
      red: player1,
      blue: player2,
      open: true,
      messageId: msg.id,
      channelId: msg.channelId,
    });

    // Process queued bets from previous round
    if (queuedBets.size > 0) {
      console.log(`[Queue] Processing ${queuedBets.size} queued bet(s)...`);
      
      for (const [userId, queuedBet] of queuedBets.entries()) {
        try {
          const user = getOrCreateUser(userId, queuedBet.displayName);
          
          // Check if user still has enough balance
          if ((user.balance || 0) >= queuedBet.amount) {
            // Deduct balance and place bet
            adjustBalance(userId, -queuedBet.amount, {
              displayName: queuedBet.displayName,
              reason: 'bet',
            });
            
            currentRound.bets.push({
              discordUserId: userId,
              amount: queuedBet.amount,
              side: queuedBet.side,
            });
            
            console.log(`[Queue] ✓ Placed queued bet: ${queuedBet.displayName} - ${formatAmountFull(queuedBet.amount)} on ${queuedBet.side}`);
            
            // Try to DM user confirmation
            try {
              const discordUser = await client.users.fetch(userId);
              const color = queuedBet.side === 'red' ? 0xFF0000 : 0x0000FF;
              const confirmEmbed = new EmbedBuilder()
                .setTitle('✅ Queued Bet Placed!')
                .setDescription(
                  `Your queued bet has been placed in the new round!\n\n` +
                  `**Amount:** ${formatAmountFull(queuedBet.amount)}\n` +
                  `**Side:** ${queuedBet.side.toUpperCase()}\n` +
                  `**Round:** ${roundId}`
                )
                .setColor(color)
                .setFooter({ text: `New Balance: ${formatAmountFull(user.balance)}` })
                .setTimestamp();
              
              await discordUser.send({ embeds: [confirmEmbed] });
            } catch (dmErr) {
              console.log(`[Queue] Could not DM user ${userId}: ${dmErr.message}`);
            }
          } else {
            console.log(`[Queue] ✗ Insufficient balance for ${queuedBet.displayName} - bet removed from queue`);
            
            // Try to notify user
            try {
              const discordUser = await client.users.fetch(userId);
              await discordUser.send(
                `❌ Your queued bet of ${formatAmountFull(queuedBet.amount)} could not be placed - insufficient balance.\n` +
                `Current balance: ${formatAmountFull(user.balance || 0)}`
              );
            } catch (dmErr) {
              console.log(`[Queue] Could not DM user ${userId}: ${dmErr.message}`);
            }
          }
        } catch (err) {
          console.error(`[Queue] Error processing queued bet for user ${userId}:`, err);
        }
      }
      
      // Clear the queue
      queuedBets.clear();
      console.log(`[Queue] Queue cleared`);
    }

    const endTs = Date.now() + 30_000;
    const intervalId = setInterval(async () => {
      try {
        if (!currentRound || currentRound.roundId !== roundId) {
          clearInterval(intervalId);
          return;
        }

        if (!currentRound.open) {
          // Already closed by timer or result; just stop updating.
          clearInterval(intervalId);
          return;
        }

        const now = Date.now();
        const remainingMs = endTs - now;
        const remaining = Math.max(0, Math.ceil(remainingMs / 1000));

        if (remaining <= 0) {
          currentRound.open = false;
        }

        const ch = await client.channels.fetch(currentRound.channelId);
        const message = await ch.messages.fetch(currentRound.messageId);
        const base = message.embeds[0] ? EmbedBuilder.from(message.embeds[0]) : new EmbedBuilder();

        // Build per-side summaries
        const bets = currentRound.bets || [];
        const redBets = bets.filter(b => b.side === 'red');
        const blueBets = bets.filter(b => b.side === 'blue');

        const formatSideLines = (arr) => {
          if (arr.length === 0) return ['None yet'];
          const shown = arr.slice(0, 5).map(bet => `<@${bet.discordUserId}> — ${formatAmountShort(bet.amount)}`);
          if (arr.length > 5) {
            shown.push(`+${arr.length - 5} more…`);
          }
          return shown;
        };

        const redLines = formatSideLines(redBets);
        const blueLines = formatSideLines(blueBets);

        if (!currentRound.open) {
          base
            .setDescription('Betting is now **closed**. Waiting for duel result...')
            .setColor(0xFF0000); // red when closed
        } else {
          base.setDescription(`Betting window: **${remaining}s** remaining.`);
        }

        const statsNow = getStats();
        const historyNow = Array.isArray(statsNow.lastWinners) ? statsNow.lastWinners : [];
        const last50Now = historyNow.slice(-50);
        const redLastCountNow = last50Now.filter(w => w === 'red').length;
        const blueLastCountNow = last50Now.filter(w => w === 'blue').length;

        const redFighter = currentRound.red || 'Unknown';
        const blueFighter = currentRound.blue || 'Unknown';

        const redValue = `👊 Fighters:\n${redFighter}\n\n💰 Bets:\n${redLines.join('\n')}`;
        const blueValue = `👊 Fighters:\n${blueFighter}\n\n💰 Bets:\n${blueLines.join('\n')}`;

        // Make sure the animated GIF banner stays applied on every edit
        base.setImage('https://dswa1xdat8uez.cloudfront.net/2861g%2Fpreview%2F72971504%2Fmain_full.gif?response-content-disposition=inline%3Bfilename%3D%22main_full.gif%22%3B&response-content-type=image%2Fgif&Expires=1763659081&Signature=h20GYSaNjrZUEiVlewO97YQEwMcIYGycE9b-5m-7zCGatTLMEtyJWkmBMrusc3sWVFlzlsOK669S8kAixDICqY0YP2Mc4~7K7w801iTIaSRB7YplsDGBfJMYRLDTxgrxi7TZqLrHMxSgu4pr5prFnMCGEvkJnkC-kErHZZeX2wjHRF0wALYgJzCMYWEmt~tG3d0h1EoVtLxQrKrPFzf5pYlCzcQ7uzNxXj5M1GZ3-PoJe2CcAacZlr1jOBXYF8~LtwiA5oelJdA~GW0acLP-UWchj0XdXyPZUxzbZtXmWz~wA8wKsw-i333H21xZFT9uAB6mt1hf-AjaXALA7N3RoA__&Key-Pair-Id=APKAJT5WQLLEOADKLHBQ');

        base.setFields(
          { name: '🟥 Red side', value: redValue, inline: true },
          { name: '🟦 Blue side', value: blueValue, inline: true },
          {
            name: '🔥 Streaks',
            value: `Red streak: ${statsNow.redStreak} | Blue streak: ${statsNow.blueStreak}`,
            inline: false,
          },
          {
            name: '📈 Last 50',
            value: `Red: ${redLastCountNow} | Blue: ${blueLastCountNow}`,
            inline: false,
          },
        );

        const components = [buildBetButtons(!currentRound.open)];

        await message.edit({ embeds: [base], components });

        if (!currentRound.open) {
          clearInterval(intervalId);
        }
      } catch (err) {
        console.error('Failed to update betting timer embed:', err);
      }
    }, 3_000);

    return res.json({ status: 'ok' });
  } catch (err) {
    console.error('Error in /api/duel/round-created:', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/duel/round-result', async (req, res) => {
  const { roundId, winner, stats } = req.body || {};
  if (!roundId || !winner) {
    return res.status(400).json({ error: 'Missing roundId or winner' });
  }

  if (!currentRound || currentRound.roundId !== roundId) {
    return res.status(400).json({ error: 'Round not found or already settled' });
  }

  currentRound.open = false;
  const bets = currentRound.bets;

  if (winner === 'draw') {
    // Refund all bets
    for (const bet of bets) {
      adjustBalance(bet.discordUserId, bet.amount, {
        reason: 'round-refund',
      });
      recordBetHistory(bet.discordUserId, {
        displayName: null,
        roundId,
        side: bet.side,
        amount: bet.amount,
        outcome: 'refund',
        payout: bet.amount,
      });
    }
  } else {
    const winners = bets.filter(b => b.side === winner);
    const losers = bets.filter(b => b.side !== winner);

    for (const bet of winners) {
      const payout = Math.floor(bet.amount * 1.95);
      const newBalance = adjustBalance(bet.discordUserId, payout, {
        reason: 'round-win',
      });
      recordBetHistory(bet.discordUserId, {
        displayName: null,
        roundId,
        side: bet.side,
        amount: bet.amount,
        outcome: 'win',
        payout,
      });
      // Accrue rakeback for winning bet (unclaimed until /rakeback_claim)
      const rakebackWin = Math.floor(bet.amount * RAKEBACK_RATE);
      if (rakebackWin > 0) {
        addRakeback(bet.discordUserId, rakebackWin);
      }

      // Attempt to DM the user with their winnings and new balance
      try {
        const user = await client.users.fetch(bet.discordUserId);
        await user.send(
          `Congratulations! You won **${formatAmountFull(payout)}** on round \`${roundId}\`\n` +
          `Your new betting balance is **${formatAmountFull(newBalance)}**.`,
        );
      } catch (dmErr) {
        console.error('Failed to DM round winnings to user', bet.discordUserId, dmErr);
      }
    }

    for (const bet of losers) {
      recordBetHistory(bet.discordUserId, {
        displayName: null,
        roundId,
        side: bet.side,
        amount: bet.amount,
        outcome: 'loss',
        payout: 0,
      });
      // Accrue rakeback for losing bet (unclaimed until /rakeback_claim)
      const rakebackLoss = Math.floor(bet.amount * RAKEBACK_RATE);
      if (rakebackLoss > 0) {
        addRakeback(bet.discordUserId, rakebackLoss);
      }
    }
  }

  // Update global streaks
  if (winner === 'red' || winner === 'blue') {
    recordWinnerSide(winner);
  }

  try {
    const settings = getSettings();
    const resultsChannelId = settings.resultsChannelId || currentRound.channelId;
    const ch = await client.channels.fetch(resultsChannelId);

    const totalPot = bets.reduce((sum, b) => sum + b.amount, 0);
    const winnerText = winner === 'red' ? currentRound.red
      : winner === 'blue' ? currentRound.blue
      : 'Draw';

    const winners = winner === 'draw' ? [] : bets.filter(b => b.side === winner);
    const winnersLines = winners.length === 0
      ? ['None']
      : winners.map(bet => {
          const payout = Math.floor(bet.amount * 1.95);
          return `<@${bet.discordUserId}> - ${formatAmountShort(payout)}`;
        });

    const winnerColor = winner === 'red' ? 0xFF0000
      : winner === 'blue' ? 0x0000FF
      : 0x808080;

    const streaks = getStats();

    const resultEmbed = new EmbedBuilder()
      .setTitle(`🏆 Duel Result - Round ${currentRound.roundId}`)
      .setDescription('Betting closed. Result received.')
      .addFields(
        {
          name: '🏆 Winner',
          value: winnerText,
          inline: false,
        },
        {
          name: '💰 Total Pot',
          value: formatAmountFull(totalPot),
          inline: true,
        },
        {
          name: '🎉 Winners',
          value: `Congratulations to the winners:\n${winnersLines.join('\n')}`,
          inline: false,
        },
        {
          name: '🔥 Streaks',
          value: `Red streak: ${streaks.redStreak} | Blue streak: ${streaks.blueStreak}`,
          inline: false,
        },
      )
      .setColor(winnerColor)
      .setTimestamp(new Date());

    if (settings.logoUrl) {
      resultEmbed.setThumbnail(settings.logoUrl);
    }

    await ch.send({ embeds: [resultEmbed] });
  } catch (err) {
    console.error('Failed to edit result embed:', err);
  }

  currentRound = null;
  saveRoundMeta(null);
  return res.json({ status: 'ok' });
});

app.listen(PORT, () => {
  console.log(`HTTP server listening on port ${PORT}`);
});

(async () => {
  try {
    await registerCommands();
    if (!TOKEN) {
      console.error('DISCORD_TOKEN missing in environment');
      return;
    }
    await client.login(TOKEN);
  } catch (err) {
    console.error('Failed to start bot:', err);
  }
})();
