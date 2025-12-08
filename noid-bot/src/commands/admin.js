const { SlashCommandBuilder, EmbedBuilder, PermissionFlagsBits } = require('discord.js');
const api = require('../api');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('admin')
        .setDescription('Admin commands for subscription management')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
        .addSubcommand(subcommand =>
            subcommand
                .setName('grant')
                .setDescription('Grant subscription to a user')
                .addUserOption(option =>
                    option.setName('user')
                        .setDescription('The user to grant subscription to')
                        .setRequired(true))
                .addIntegerOption(option =>
                    option.setName('days')
                        .setDescription('Duration in days (0 = forever)')
                        .setRequired(false))
                .addStringOption(option =>
                    option.setName('tier')
                        .setDescription('Subscription tier')
                        .setRequired(false)
                        .addChoices(
                            { name: 'Standard', value: 'standard' },
                            { name: 'Premium', value: 'premium' },
                            { name: 'Lifetime', value: 'lifetime' }
                        )))
        .addSubcommand(subcommand =>
            subcommand
                .setName('revoke')
                .setDescription('Revoke subscription from a user')
                .addUserOption(option =>
                    option.setName('user')
                        .setDescription('The user to revoke subscription from')
                        .setRequired(true)))
        .addSubcommand(subcommand =>
            subcommand
                .setName('reset-hwid')
                .setDescription('Reset user HWID to allow login from new device')
                .addUserOption(option =>
                    option.setName('user')
                        .setDescription('The user to reset HWID for')
                        .setRequired(true)))
        .addSubcommand(subcommand =>
            subcommand
                .setName('check')
                .setDescription('Check a user\'s subscription status')
                .addUserOption(option =>
                    option.setName('user')
                        .setDescription('The user to check')
                        .setRequired(true)))
        .addSubcommand(subcommand =>
            subcommand
                .setName('stats')
                .setDescription('View usage statistics'))
        .addSubcommand(subcommand =>
            subcommand
                .setName('list')
                .setDescription('View all active subscriptions')),

    async execute(interaction) {
        const subcommand = interaction.options.getSubcommand();

        try {
            await interaction.deferReply({ ephemeral: true });

            switch (subcommand) {
                case 'grant':
                    await handleGrant(interaction);
                    break;
                case 'revoke':
                    await handleRevoke(interaction);
                    break;
                case 'reset-hwid':
                    await handleResetHwid(interaction);
                    break;
                case 'check':
                    await handleCheck(interaction);
                    break;
                case 'stats':
                    await handleStats(interaction);
                    break;
                case 'list':
                    await handleList(interaction);
                    break;
            }

        } catch (error) {
            console.error(`Admin ${subcommand} error:`, error);
            await interaction.editReply({
                content: '❌ An error occurred. Please try again later.'
            });
        }
    }
};

async function handleGrant(interaction) {
    const user = interaction.options.getUser('user');
    const days = interaction.options.getInteger('days') || 0;
    const tier = interaction.options.getString('tier') || 'standard';

    const result = await api.grantSubscription(user.id, user.username, tier, days > 0 ? days : null);

    // Try to add subscriber role
    try {
        const member = await interaction.guild.members.fetch(user.id);
        if (process.env.SUBSCRIBER_ROLE_ID) {
            await member.roles.add(process.env.SUBSCRIBER_ROLE_ID);
        }
    } catch (e) {
        console.warn('Could not add role:', e.message);
    }

    const embed = new EmbedBuilder()
        .setColor(0x00FF00)
        .setTitle('✅ Subscription Granted')
        .addFields(
            { name: 'User', value: `<@${user.id}>`, inline: true },
            { name: 'Tier', value: tier, inline: true },
            { name: 'Duration', value: days > 0 ? `${days} days` : 'Forever', inline: true }
        )
        .setFooter({ text: `Granted by ${interaction.user.username}` })
        .setTimestamp();

    await interaction.editReply({ embeds: [embed] });
}

async function handleRevoke(interaction) {
    const user = interaction.options.getUser('user');

    await api.revokeSubscription(user.id);

    // Try to remove subscriber role
    try {
        const member = await interaction.guild.members.fetch(user.id);
        if (process.env.SUBSCRIBER_ROLE_ID) {
            await member.roles.remove(process.env.SUBSCRIBER_ROLE_ID);
        }
    } catch (e) {
        console.warn('Could not remove role:', e.message);
    }

    const embed = new EmbedBuilder()
        .setColor(0xFF0000)
        .setTitle('❌ Subscription Revoked')
        .addFields(
            { name: 'User', value: `<@${user.id}>`, inline: true }
        )
        .setFooter({ text: `Revoked by ${interaction.user.username}` })
        .setTimestamp();

    await interaction.editReply({ embeds: [embed] });
}

async function handleCheck(interaction) {
    const user = interaction.options.getUser('user');

    const userData = await api.getUser(user.id);

    if (!userData || !userData.user) {
        return interaction.editReply({ content: `❌ <@${user.id}> is not registered.` });
    }

    const { user: u, subscription } = userData;

    const embed = new EmbedBuilder()
        .setColor(subscription?.active ? 0x00FF00 : 0xFF0000)
        .setTitle('📊 User Status')
        .addFields(
            { name: 'User', value: `<@${u.discordId}>`, inline: true },
            { name: 'Status', value: subscription?.active ? '✅ Active' : '❌ Inactive', inline: true },
            { name: 'HWID', value: u.hwid || 'Not linked', inline: true }
        );

    if (subscription) {
        embed.addFields(
            { name: 'Tier', value: subscription.tier || 'Standard', inline: true },
            {
                name: 'Expires', value: subscription.expiresAt
                    ? new Date(subscription.expiresAt).toLocaleDateString()
                    : 'Never', inline: true
            }
        );
    }

    await interaction.editReply({ embeds: [embed] });
}

async function handleStats(interaction) {
    const stats = await api.getStats();

    const embed = new EmbedBuilder()
        .setColor(0x0099FF)
        .setTitle('📈 Usage Statistics')
        .addFields(
            { name: 'Total Users', value: stats.totalUsers.toString(), inline: true },
            { name: 'Active Subscriptions', value: stats.activeSubscriptions.toString(), inline: true }
        );

    // Add plugin usage
    if (stats.pluginUsage && Object.keys(stats.pluginUsage).length > 0) {
        let usageText = '';
        for (const [plugin, actions] of Object.entries(stats.pluginUsage)) {
            const total = Object.values(actions).reduce((a, b) => a + b, 0);
            usageText += `**${plugin}**: ${total} actions\n`;
        }
        embed.addFields({ name: 'Plugin Usage (7 days)', value: usageText || 'No data' });
    }

    embed.setFooter({ text: 'Generated at' })
        .setTimestamp();

    await interaction.editReply({ embeds: [embed] });
}

async function handleResetHwid(interaction) {
    const user = interaction.options.getUser('user');

    await api.resetHwid(user.id);

    const embed = new EmbedBuilder()
        .setColor(0x00FFFF)
        .setTitle('🔄 HWID Reset')
        .setDescription(`HWID has been reset for <@${user.id}>. They can now login from a new device.`)
        .setFooter({ text: `Reset by ${interaction.user.username}` })
        .setTimestamp();

    await interaction.editReply({ embeds: [embed] });
}

// Store subscription data for pagination
const subscriptionPages = new Map();

async function handleList(interaction) {
    try {
        const data = await api.getActiveSubscriptions();
        const subscriptions = data.subscriptions || [];

        if (subscriptions.length === 0) {
            return interaction.editReply({
                embeds: [new EmbedBuilder()
                    .setColor(0xFFA500)
                    .setTitle('📋 Active Subscriptions')
                    .setDescription('No active subscriptions found.')
                ]
            });
        }

        // Fetch usernames from Discord
        const guild = interaction.guild;
        const formattedSubs = [];

        for (const sub of subscriptions) {
            let username = sub.discord_name || 'Unknown';
            try {
                const member = await guild.members.fetch(sub.discord_id).catch(() => null);
                if (member) {
                    username = member.user.username;
                }
            } catch (e) { }

            const expires = sub.expires_at
                ? new Date(sub.expires_at).toLocaleDateString()
                : '♾️ Never';
            const tier = (sub.tier || 'standard').charAt(0).toUpperCase() + (sub.tier || 'standard').slice(1);

            formattedSubs.push({
                username,
                discordId: sub.discord_id,
                tier,
                expires,
                hwid: sub.hwid ? '✅' : '❌'
            });
        }

        // Split into pages (10 per page)
        const pageSize = 10;
        const pages = [];
        for (let i = 0; i < formattedSubs.length; i += pageSize) {
            pages.push(formattedSubs.slice(i, i + pageSize));
        }

        // Store for pagination
        const sessionId = `list_${interaction.user.id}_${Date.now()}`;
        subscriptionPages.set(sessionId, { pages, totalCount: subscriptions.length });

        // Clean up old sessions after 5 minutes
        setTimeout(() => subscriptionPages.delete(sessionId), 5 * 60 * 1000);

        // Send first page
        await sendSubscriptionPage(interaction, sessionId, 0);

    } catch (error) {
        console.error('List subscriptions error:', error);
        await interaction.editReply({
            content: '❌ Error fetching subscriptions: ' + error.message
        });
    }
}

async function sendSubscriptionPage(interaction, sessionId, pageIndex) {
    const session = subscriptionPages.get(sessionId);
    if (!session) return;

    const { pages, totalCount } = session;
    const page = pages[pageIndex];
    const { ButtonBuilder, ButtonStyle, ActionRowBuilder } = require('discord.js');

    // Format page content
    const description = page.map((sub, i) => {
        const num = pageIndex * 10 + i + 1;
        return `**${num}.** ${sub.username}\n   └ <@${sub.discordId}> • ${sub.tier} • Expires: ${sub.expires} • HWID: ${sub.hwid}`;
    }).join('\n\n');

    const embed = new EmbedBuilder()
        .setColor(0x00FF00)
        .setTitle(`📋 Active Subscriptions (${totalCount} total)`)
        .setDescription(description)
        .setFooter({ text: `Page ${pageIndex + 1} of ${pages.length}` })
        .setTimestamp();

    // Create pagination buttons
    const buttons = new ActionRowBuilder();

    if (pageIndex > 0) {
        buttons.addComponents(
            new ButtonBuilder()
                .setCustomId(`sub_page_${sessionId}_${pageIndex - 1}`)
                .setLabel('◀ Previous')
                .setStyle(ButtonStyle.Secondary)
        );
    }

    if (pageIndex < pages.length - 1) {
        buttons.addComponents(
            new ButtonBuilder()
                .setCustomId(`sub_page_${sessionId}_${pageIndex + 1}`)
                .setLabel('Next ▶')
                .setStyle(ButtonStyle.Secondary)
        );
    }

    const payload = { embeds: [embed] };
    if (buttons.components.length > 0) {
        payload.components = [buttons];
    }

    if (interaction.replied || interaction.deferred) {
        await interaction.editReply(payload);
    } else {
        await interaction.reply({ ...payload, ephemeral: true });
    }
}

// Export for use in index.js button handler
module.exports.handleSubscriptionPage = async function (interaction, customId) {
    const parts = customId.split('_');
    const pageIndex = parseInt(parts[parts.length - 1]);
    const sessionId = parts.slice(2, -1).join('_');

    await interaction.deferUpdate();
    await sendSubscriptionPage(interaction, sessionId, pageIndex);
};

module.exports.subscriptionPages = subscriptionPages;
