const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, StringSelectMenuBuilder, PermissionFlagsBits } = require('discord.js');
const axios = require('axios');

const API_URL = process.env.API_URL || 'http://localhost:3000';
const API_KEY = process.env.API_KEY;

module.exports = {
    data: new SlashCommandBuilder()
        .setName('user')
        .setDescription('View plugin user data (Admin only)')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
        .addSubcommand(sub => sub
            .setName('list')
            .setDescription('List all users'))
        .addSubcommand(sub => sub
            .setName('view')
            .setDescription('View detailed user data')
            .addStringOption(opt => opt
                .setName('discord_id')
                .setDescription('Discord ID of the user')
                .setRequired(true)))
        .addSubcommand(sub => sub
            .setName('search')
            .setDescription('Search for users')
            .addStringOption(opt => opt
                .setName('query')
                .setDescription('Search by name or Discord ID')
                .setRequired(true)))
        .addSubcommand(sub => sub
            .setName('stats')
            .setDescription('View overall plugin usage statistics')),

    async execute(interaction) {
        // Double-check admin permission
        if (!interaction.member.permissions.has(PermissionFlagsBits.Administrator)) {
            return interaction.reply({
                content: '❌ This command is for administrators only.',
                ephemeral: true
            });
        }

        const sub = interaction.options.getSubcommand();

        switch (sub) {
            case 'list':
                await handleList(interaction);
                break;
            case 'view':
                await handleView(interaction);
                break;
            case 'search':
                await handleSearch(interaction);
                break;
            case 'stats':
                await handleStats(interaction);
                break;
        }
    }
};

async function handleList(interaction) {
    await interaction.deferReply({ ephemeral: true });

    try {
        const response = await axios.get(`${API_URL}/admin/users?limit=25`, {
            headers: { 'x-api-key': API_KEY }
        });

        const { users, total } = response.data;

        if (!users || users.length === 0) {
            return interaction.editReply({ content: '📭 No users found.' });
        }

        const embed = new EmbedBuilder()
            .setColor(0x5865F2)
            .setTitle('📋 Plugin Users')
            .setDescription(`Showing ${users.length} of ${total} users`)
            .addFields(
                users.slice(0, 10).map(u => ({
                    name: `${u.subscriptionActive ? '✅' : '❌'} ${u.discordName || 'Unknown'}`,
                    value: `ID: \`${u.discordId}\`\nTier: ${u.tier || 'None'}\nHWID: ${u.hasHwid ? '✅' : '❌'}`,
                    inline: true
                }))
            )
            .setFooter({ text: 'Use /user view <discord_id> for detailed info' })
            .setTimestamp();

        await interaction.editReply({ embeds: [embed] });

    } catch (error) {
        console.error('User list error:', error.message);
        await interaction.editReply({ content: '❌ Failed to fetch users: ' + error.message });
    }
}

async function handleView(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const discordId = interaction.options.getString('discord_id');

    try {
        const response = await axios.get(`${API_URL}/admin/user/${discordId}`, {
            headers: { 'x-api-key': API_KEY }
        });

        const data = response.data;
        const { user, subscription, sessions, usageStats, recentUsage, totalUsageCount } = data;

        // Build embeds
        const embeds = [];

        // User info embed
        const userEmbed = new EmbedBuilder()
            .setColor(subscription?.active ? 0x00FF00 : 0xFF0000)
            .setTitle(`👤 ${user.discordName || 'Unknown User'}`)
            .addFields(
                { name: '📇 Discord ID', value: `\`${user.discordId}\``, inline: true },
                { name: '🆔 Internal ID', value: `\`${user.id}\``, inline: true },
                { name: '📅 Registered', value: user.createdAt ? `<t:${Math.floor(new Date(user.createdAt).getTime() / 1000)}:R>` : 'Unknown', inline: true },
                { name: '🖥️ HWID', value: user.hwid || 'Not linked', inline: true },
                { name: '📊 Total Usage', value: `${totalUsageCount} events`, inline: true },
                { name: '🔗 Active Sessions', value: `${sessions.length}`, inline: true }
            );

        if (subscription) {
            userEmbed.addFields(
                { name: '💳 Subscription', value: `**${subscription.tier}** tier`, inline: true },
                { name: '⏰ Expires', value: subscription.expiresAt ? `<t:${Math.floor(new Date(subscription.expiresAt).getTime() / 1000)}:R>` : 'Never', inline: true },
                { name: '✅ Status', value: 'Active', inline: true }
            );
        } else {
            userEmbed.addFields(
                { name: '💳 Subscription', value: '❌ None', inline: true }
            );
        }

        embeds.push(userEmbed);

        // Usage stats embed
        if (usageStats && usageStats.length > 0) {
            const usageEmbed = new EmbedBuilder()
                .setColor(0x5865F2)
                .setTitle('📊 Usage Stats (Last 30 Days)')
                .addFields(
                    usageStats.slice(0, 9).map(stat => ({
                        name: stat.plugin_name || 'Unknown',
                        value: `${stat.count} ${stat.action}s\nFirst: ${stat.first_use ? stat.first_use.split('T')[0] : 'N/A'}\nLast: ${stat.last_use ? stat.last_use.split('T')[0] : 'N/A'}`,
                        inline: true
                    }))
                );
            embeds.push(usageEmbed);
        }

        // Recent activity embed
        if (recentUsage && recentUsage.length > 0) {
            const activityLines = recentUsage.slice(0, 10).map(u => {
                const time = u.timestamp ? `<t:${Math.floor(new Date(u.timestamp).getTime() / 1000)}:R>` : 'Unknown';
                return `• ${u.plugin_name} - ${u.action} (${time})`;
            });

            const activityEmbed = new EmbedBuilder()
                .setColor(0x5865F2)
                .setTitle('🕐 Recent Activity')
                .setDescription(activityLines.join('\n'));
            embeds.push(activityEmbed);
        }

        // Sessions embed
        if (sessions && sessions.length > 0) {
            const sessionLines = sessions.slice(0, 5).map(s => {
                const lastActive = s.lastActive ? `<t:${Math.floor(new Date(s.lastActive).getTime() / 1000)}:R>` : 'Unknown';
                return `• IP: \`${s.ip || 'Unknown'}\` - Last active: ${lastActive}`;
            });

            const sessionEmbed = new EmbedBuilder()
                .setColor(0x5865F2)
                .setTitle('🔗 Sessions')
                .setDescription(sessionLines.join('\n'));
            embeds.push(sessionEmbed);
        }

        await interaction.editReply({ embeds });

    } catch (error) {
        if (error.response?.status === 404) {
            return interaction.editReply({ content: '❌ User not found.' });
        }
        console.error('User view error:', error.message);
        await interaction.editReply({ content: '❌ Failed to fetch user: ' + error.message });
    }
}

async function handleSearch(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const query = interaction.options.getString('query');

    try {
        const response = await axios.get(`${API_URL}/admin/search?q=${encodeURIComponent(query)}`, {
            headers: { 'x-api-key': API_KEY }
        });

        const { users } = response.data;

        if (!users || users.length === 0) {
            return interaction.editReply({ content: `📭 No users found matching "${query}"` });
        }

        const embed = new EmbedBuilder()
            .setColor(0x5865F2)
            .setTitle(`🔍 Search Results for "${query}"`)
            .setDescription(`Found ${users.length} user(s)`)
            .addFields(
                users.slice(0, 10).map(u => ({
                    name: `${u.subscriptionActive ? '✅' : '❌'} ${u.discordName || 'Unknown'}`,
                    value: `ID: \`${u.discordId}\`\nTier: ${u.tier || 'None'}`,
                    inline: true
                }))
            )
            .setFooter({ text: 'Use /user view <discord_id> for detailed info' });

        await interaction.editReply({ embeds: [embed] });

    } catch (error) {
        console.error('User search error:', error.message);
        await interaction.editReply({ content: '❌ Failed to search: ' + error.message });
    }
}

async function handleStats(interaction) {
    await interaction.deferReply({ ephemeral: true });

    try {
        const response = await axios.get(`${API_URL}/admin/stats`, {
            headers: { 'x-api-key': API_KEY }
        });

        const { totalUsers, activeSubscriptions, activeToday, activeWeek, usageByPlugin } = response.data;

        const embed = new EmbedBuilder()
            .setColor(0x5865F2)
            .setTitle('📊 Plugin Statistics')
            .addFields(
                { name: '👥 Total Users', value: `${totalUsers}`, inline: true },
                { name: '✅ Active Subscriptions', value: `${activeSubscriptions}`, inline: true },
                { name: '📈 Conversion Rate', value: totalUsers > 0 ? `${Math.round((activeSubscriptions / totalUsers) * 100)}%` : '0%', inline: true },
                { name: '🕐 Active Today', value: `${activeToday}`, inline: true },
                { name: '📅 Active This Week', value: `${activeWeek}`, inline: true },
                { name: '\u200B', value: '\u200B', inline: true }
            );

        // Add plugin usage breakdown
        if (usageByPlugin && usageByPlugin.length > 0) {
            const pluginStats = usageByPlugin.slice(0, 6).map(s =>
                `**${s.plugin_name}**: ${s.count} ${s.action}s`
            ).join('\n');
            embed.addFields({ name: '🎮 Plugin Usage (7 days)', value: pluginStats || 'No data' });
        }

        embed.setTimestamp();

        await interaction.editReply({ embeds: [embed] });

    } catch (error) {
        console.error('Stats error:', error.message);
        await interaction.editReply({ content: '❌ Failed to fetch stats: ' + error.message });
    }
}
