// Comprehensive user investigation command for admins
const { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { getUserComprehensiveData, searchUserByIdentifier } = require('../utils/database');

// Format GP amounts
function formatGP(amount) {
  if (!amount) return '0 GP';
  const num = Number(amount);
  if (num >= 1000000000) return `${(num / 1000000000).toFixed(2)}b GP`;
  if (num >= 1000000) return `${(num / 1000000).toFixed(2)}m GP`;
  if (num >= 1000) return `${(num / 1000).toFixed(2)}k GP`;
  return `${num.toLocaleString()} GP`;
}

// Format timestamps
function formatTimestamp(unixTime) {
  if (!unixTime) return 'N/A';
  return `<t:${unixTime}:R>`;
}

// Format currency
function formatCurrency(amount) {
  return `$${Number(amount).toFixed(2)}`;
}

// Create overview embed
function createOverviewEmbed(data, client) {
  const { user, rsns, betStats, pendingCryptoDeposits, pendingCryptoWithdrawals } = data;
  
  const totalGpDeposited = data.deposits.reduce((sum, d) => sum + Number(d.amount || 0), 0);
  const totalGpWithdrawn = data.withdrawals.reduce((sum, w) => sum + Number(w.amount || 0), 0);
  const totalCryptoDeposited = data.cryptoDeposits.filter(d => d.status === 'completed')
    .reduce((sum, d) => sum + Number(d.amount_usd || 0), 0);
  const totalCryptoWithdrawn = data.cryptoWithdrawals.filter(w => w.status === 'completed')
    .reduce((sum, w) => sum + Number(w.amount_usd || 0), 0);
  
  const winRate = betStats.total_bets > 0 
    ? ((user.wins / betStats.total_bets) * 100).toFixed(1) 
    : '0.0';
  
  const embed = new EmbedBuilder()
    .setTitle('🔍 User Investigation - Overview')
    .setDescription(`**Discord User:** <@${user.id}>\n**Display Name:** ${user.display_name || 'N/A'}\n**User ID:** \`${user.id}\``)
    .addFields(
      {
        name: '💰 Current Balance',
        value: formatGP(user.balance),
        inline: true
      },
      {
        name: '🎲 Win Rate',
        value: `${winRate}% (${user.wins}W / ${user.losses}L)`,
        inline: true
      },
      {
        name: '📊 Total Bets',
        value: betStats.total_bets.toLocaleString(),
        inline: true
      },
      {
        name: '💸 Total Wagered',
        value: formatGP(betStats.total_wagered),
        inline: true
      },
      {
        name: '📈 Net Profit/Loss',
        value: `${Number(betStats.net_profit) >= 0 ? '📈' : '📉'} ${formatGP(betStats.net_profit)}`,
        inline: true
      },
      {
        name: '🎁 Unclaimed Rakeback',
        value: formatGP(user.rakeback_unclaimed),
        inline: true
      },
      {
        name: '🏦 GP Transactions',
        value: `Deposited: ${formatGP(totalGpDeposited)}\nWithdrawn: ${formatGP(totalGpWithdrawn)}`,
        inline: true
      },
      {
        name: '💳 Crypto Transactions',
        value: `Deposited: ${formatCurrency(totalCryptoDeposited)}\nWithdrawn: ${formatCurrency(totalCryptoWithdrawn)}`,
        inline: true
      },
      {
        name: '⏳ Pending',
        value: `Crypto Deposits: ${pendingCryptoDeposits.length}\nCrypto Withdrawals: ${pendingCryptoWithdrawals.length}`,
        inline: true
      },
      {
        name: '🎮 Linked RSNs',
        value: rsns.length > 0 
          ? rsns.map(r => `\`${r.rsn}\``).join(', ')
          : 'None',
        inline: false
      },
      {
        name: '📅 Account Created',
        value: formatTimestamp(user.created_at),
        inline: true
      }
    )
    .setColor(0x5865F2)
    .setFooter({ text: `User Check • Use buttons below for detailed views` })
    .setTimestamp();
  
  return embed;
}

// Create bet history embed
function createBetHistoryEmbed(data, page = 0) {
  const { user, recentBets } = data;
  const pageSize = 10;
  const start = page * pageSize;
  const bets = recentBets.slice(start, start + pageSize);
  
  const embed = new EmbedBuilder()
    .setTitle('🎲 Bet History')
    .setDescription(`Recent bets for <@${user.id}>`)
    .setColor(0x5865F2);
  
  if (bets.length === 0) {
    embed.addFields({ name: 'No Bets', value: 'This user has no betting history.' });
  } else {
    bets.forEach((bet, i) => {
      const emoji = bet.outcome === 'win' ? '✅' : bet.outcome === 'loss' ? '❌' : '⏸️';
      const side = bet.side === 'red' ? '🔴' : '🔵';
      embed.addFields({
        name: `${emoji} ${side} Round ${bet.round_id || 'N/A'} ${formatTimestamp(bet.created_at)}`,
        value: `Bet: ${formatGP(bet.amount)} • Payout: ${formatGP(bet.payout)} • Net: ${formatGP(Number(bet.payout || 0) - Number(bet.amount))}`,
        inline: false
      });
    });
  }
  
  embed.setFooter({ text: `Page ${page + 1}/${Math.ceil(recentBets.length / pageSize)} • Total: ${recentBets.length} bets` });
  
  return embed;
}

// Create GP transactions embed
function createGPTransactionsEmbed(data) {
  const { user, deposits, withdrawals } = data;
  
  const embed = new EmbedBuilder()
    .setTitle('🏦 GP Transaction History')
    .setDescription(`GP deposits and withdrawals for <@${user.id}>`)
    .setColor(0x57F287);
  
  // Combine and sort by timestamp
  const allTransactions = [
    ...deposits.map(d => ({ ...d, type: 'deposit' })),
    ...withdrawals.map(w => ({ ...w, type: 'withdrawal' }))
  ].sort((a, b) => b.created_at - a.created_at).slice(0, 15);
  
  if (allTransactions.length === 0) {
    embed.addFields({ name: 'No Transactions', value: 'No GP transaction history found.' });
  } else {
    allTransactions.forEach(tx => {
      const emoji = tx.type === 'deposit' ? '📥' : '📤';
      const amount = tx.type === 'deposit' ? `+${formatGP(tx.amount)}` : `-${formatGP(tx.amount)}`;
      embed.addFields({
        name: `${emoji} ${tx.type.charAt(0).toUpperCase() + tx.type.slice(1)} ${formatTimestamp(tx.created_at)}`,
        value: `Amount: **${amount}**\nReason: ${tx.reason || 'N/A'}`,
        inline: true
      });
    });
  }
  
  return embed;
}

// Create crypto transactions embed
function createCryptoTransactionsEmbed(data) {
  const { user, cryptoDeposits, cryptoWithdrawals } = data;
  
  const embed = new EmbedBuilder()
    .setTitle('💳 Crypto Transaction History')
    .setDescription(`Crypto deposits and withdrawals for <@${user.id}>`)
    .setColor(0xFEE75C);
  
  const allTransactions = [
    ...cryptoDeposits.map(d => ({ ...d, type: 'deposit' })),
    ...cryptoWithdrawals.map(w => ({ ...w, type: 'withdrawal' }))
  ].sort((a, b) => b.created_at - a.created_at).slice(0, 10);
  
  if (allTransactions.length === 0) {
    embed.addFields({ name: 'No Transactions', value: 'No crypto transaction history found.' });
  } else {
    allTransactions.forEach(tx => {
      const emoji = tx.type === 'deposit' ? '📥' : '📤';
      const statusEmoji = tx.status === 'completed' ? '✅' : tx.status === 'pending' ? '⏳' : '❌';
      const currency = tx.currency?.toUpperCase() || 'CRYPTO';
      
      if (tx.type === 'deposit') {
        embed.addFields({
          name: `${emoji} Deposit ${formatTimestamp(tx.created_at)} ${statusEmoji}`,
          value: `**${formatCurrency(tx.amount_usd)}** (${formatGP(tx.amount_gp)})\nCurrency: ${currency}\nStatus: ${tx.status}`,
          inline: true
        });
      } else {
        embed.addFields({
          name: `${emoji} Withdrawal ${formatTimestamp(tx.created_at)} ${statusEmoji}`,
          value: `**${formatCurrency(tx.amount_usd)}** (${formatGP(tx.amount_gp)})\nCurrency: ${currency}\nStatus: ${tx.status}\nAddress: \`${tx.address?.substring(0, 10)}...\``,
          inline: true
        });
      }
    });
  }
  
  return embed;
}

// Create pending transactions embed
function createPendingTransactionsEmbed(data) {
  const { user, pendingCryptoDeposits, pendingCryptoWithdrawals } = data;
  
  const embed = new EmbedBuilder()
    .setTitle('⏳ Pending Transactions')
    .setDescription(`Active pending transactions for <@${user.id}>`)
    .setColor(0xED4245);
  
  let hasPending = false;
  
  if (pendingCryptoDeposits.length > 0) {
    hasPending = true;
    pendingCryptoDeposits.forEach(tx => {
      embed.addFields({
        name: `📥 Pending Crypto Deposit ${formatTimestamp(tx.created_at)}`,
        value: `Amount: ${formatCurrency(tx.amount_usd)} → ${formatGP(tx.amount_gp)}\nCurrency: ${tx.currency?.toUpperCase()}\nTXN ID: \`${tx.txn_id}\``,
        inline: false
      });
    });
  }
  
  if (pendingCryptoWithdrawals.length > 0) {
    hasPending = true;
    pendingCryptoWithdrawals.forEach(tx => {
      embed.addFields({
        name: `📤 Pending Crypto Withdrawal ${formatTimestamp(tx.created_at)}`,
        value: `Amount: ${formatCurrency(tx.amount_usd)} (${formatGP(tx.amount_gp)})\nCurrency: ${tx.currency?.toUpperCase()}\nAddress: \`${tx.address?.substring(0, 20)}...\`\nWithdrawal ID: \`${tx.withdrawal_id}\``,
        inline: false
      });
    });
  }
  
  if (!hasPending) {
    embed.addFields({ name: 'No Pending Transactions', value: 'All transactions are completed or none exist.' });
  }
  
  return embed;
}

// Create button row
function createButtonRow(userId) {
  const row1 = new ActionRowBuilder()
    .addComponents(
      new ButtonBuilder()
        .setCustomId(`check_overview_${userId}`)
        .setLabel('Overview')
        .setEmoji('🔍')
        .setStyle(ButtonStyle.Primary),
      new ButtonBuilder()
        .setCustomId(`check_bets_${userId}`)
        .setLabel('Bet History')
        .setEmoji('🎲')
        .setStyle(ButtonStyle.Secondary),
      new ButtonBuilder()
        .setCustomId(`check_gp_${userId}`)
        .setLabel('GP Transactions')
        .setEmoji('🏦')
        .setStyle(ButtonStyle.Secondary),
      new ButtonBuilder()
        .setCustomId(`check_crypto_${userId}`)
        .setLabel('Crypto')
        .setEmoji('💳')
        .setStyle(ButtonStyle.Secondary),
      new ButtonBuilder()
        .setCustomId(`check_pending_${userId}`)
        .setLabel('Pending')
        .setEmoji('⏳')
        .setStyle(ButtonStyle.Danger)
    );
  
  const row2 = new ActionRowBuilder()
    .addComponents(
      new ButtonBuilder()
        .setCustomId(`check_link_rsn_${userId}`)
        .setLabel('Manual Link RSN')
        .setEmoji('🔗')
        .setStyle(ButtonStyle.Success)
    );
  
  return [row1, row2];
}

module.exports = {
  createOverviewEmbed,
  createBetHistoryEmbed,
  createGPTransactionsEmbed,
  createCryptoTransactionsEmbed,
  createPendingTransactionsEmbed,
  createButtonRow,
  formatGP,
  formatTimestamp,
  formatCurrency
};
