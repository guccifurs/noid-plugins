const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, ChannelType, PermissionFlagsBits, ModalBuilder, TextInputBuilder, TextInputStyle } = require('discord.js');

// Category ID where bug report channel will be created
const BUG_REPORT_CATEGORY_ID = '1447038262211248138';

// Admin role ID for marking bugs as fixed
const ADMIN_ROLE_ID = '1446959996553396244';

// Will be set when the channel is created
let bugReportChannelId = null;

module.exports = {
    data: new SlashCommandBuilder()
        .setName('bugreport')
        .setDescription('Manage bug report channel')
        .addSubcommand(sub =>
            sub.setName('setup')
                .setDescription('Create and setup the bug report channel'))
        .addSubcommand(sub =>
            sub.setName('stats')
                .setDescription('Show bug report statistics')),

    async execute(interaction) {
        const subcommand = interaction.options.getSubcommand();

        if (subcommand === 'setup') {
            await setupBugReportChannel(interaction);
        } else if (subcommand === 'stats') {
            await showBugReportStats(interaction);
        }
    },

    // Export handlers for use in index.js
    handleBugReportButton,
    handleBugReportModal,
    handleMarkFixedButton,
    getBugReportChannelId: () => bugReportChannelId,
    setBugReportChannelId: (id) => { bugReportChannelId = id; }
};

/**
 * Setup the bug report channel with embed and button
 */
async function setupBugReportChannel(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const guild = interaction.guild;
    const category = guild.channels.cache.get(BUG_REPORT_CATEGORY_ID);

    if (!category) {
        return interaction.editReply({ content: `❌ Category not found (ID: ${BUG_REPORT_CATEGORY_ID})` });
    }

    // Get Member role
    const memberRole = guild.roles.cache.find(r => r.name.toLowerCase().includes('member') || r.name.toLowerCase().includes('verified'));

    try {
        // Create the bug report channel
        const channel = await guild.channels.create({
            name: '🐛-bug-reports',
            type: ChannelType.GuildText,
            parent: BUG_REPORT_CATEGORY_ID,
            permissionOverwrites: [
                // Deny @everyone by default
                {
                    id: guild.id,
                    deny: [PermissionFlagsBits.ViewChannel]
                },
                // Allow members (if role exists)
                ...(memberRole ? [{
                    id: memberRole.id,
                    allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.ReadMessageHistory],
                    deny: [PermissionFlagsBits.SendMessages]
                }] : []),
                // Allow the bot
                {
                    id: guild.members.me.id,
                    allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory, PermissionFlagsBits.ManageMessages]
                }
            ]
        });

        // Store the channel ID
        bugReportChannelId = channel.id;

        // Post the welcome embed with button
        const embed = new EmbedBuilder()
            .setColor(0xFF6B6B)
            .setTitle('🐛 Bug Reports')
            .setDescription('**Found a bug? Report it here!**\n\nClick the button below to submit a bug report.\n\n🔴 New reports appear in red\n🟢 Fixed bugs turn green\n✅ Help us improve!')
            .setThumbnail(guild.iconURL())
            .setFooter({ text: 'NoidSwap Bug Reports • Click below to report' })
            .setTimestamp();

        const row = new ActionRowBuilder()
            .addComponents(
                new ButtonBuilder()
                    .setCustomId('submit_bugreport')
                    .setLabel('🐛 Report a Bug')
                    .setStyle(ButtonStyle.Danger)
            );

        await channel.send({ embeds: [embed], components: [row] });

        await interaction.editReply({ content: `✅ Bug report channel created: ${channel}\n\nMembers can now submit bug reports!` });

    } catch (error) {
        console.error('Error creating bug report channel:', error);
        await interaction.editReply({ content: '❌ Failed to create bug report channel: ' + error.message });
    }
}

/**
 * Show bug report statistics
 */
async function showBugReportStats(interaction) {
    await interaction.deferReply({ ephemeral: true });

    if (!bugReportChannelId) {
        const channel = interaction.guild.channels.cache.find(c => c.name === '🐛-bug-reports');
        if (channel) {
            bugReportChannelId = channel.id;
        } else {
            return interaction.editReply({ content: '❌ Bug report channel not found. Run `/bugreport setup` first.' });
        }
    }

    const channel = interaction.guild.channels.cache.get(bugReportChannelId);
    if (!channel) {
        return interaction.editReply({ content: '❌ Bug report channel not found' });
    }

    try {
        const messages = await channel.messages.fetch({ limit: 100 });
        const bugReports = messages.filter(m =>
            m.author.id === interaction.client.user.id &&
            m.embeds.length > 0 &&
            m.embeds[0].title?.includes('Bug Report from')
        );

        let openBugs = 0;
        let fixedBugs = 0;

        for (const [, msg] of bugReports) {
            const embed = msg.embeds[0];
            if (embed.color === 0x00FF00) {
                fixedBugs++;
            } else {
                openBugs++;
            }
        }

        const embed = new EmbedBuilder()
            .setColor(0xFF6B6B)
            .setTitle('📊 Bug Report Statistics')
            .addFields(
                { name: '🐛 Total Reports', value: `${bugReports.size}`, inline: true },
                { name: '🔴 Open', value: `${openBugs}`, inline: true },
                { name: '🟢 Fixed', value: `${fixedBugs}`, inline: true },
                { name: '📅 Channel', value: `<#${bugReportChannelId}>`, inline: true }
            )
            .setFooter({ text: 'Last 100 messages scanned' })
            .setTimestamp();

        await interaction.editReply({ embeds: [embed] });
    } catch (error) {
        console.error('Bug report stats error:', error);
        await interaction.editReply({ content: '❌ Error fetching bug report stats' });
    }
}

/**
 * Handle bug report button click - shows modal
 */
async function handleBugReportButton(interaction) {
    const modal = new ModalBuilder()
        .setCustomId('bugreport_modal')
        .setTitle('🐛 Report a Bug');

    const titleInput = new TextInputBuilder()
        .setCustomId('bugreport_title')
        .setLabel('Bug Title')
        .setPlaceholder('Brief description of the bug...')
        .setStyle(TextInputStyle.Short)
        .setMinLength(5)
        .setMaxLength(100)
        .setRequired(true);

    const descriptionInput = new TextInputBuilder()
        .setCustomId('bugreport_description')
        .setLabel('What happened?')
        .setPlaceholder('Describe the bug in detail...')
        .setStyle(TextInputStyle.Paragraph)
        .setMinLength(20)
        .setMaxLength(1000)
        .setRequired(true);

    const stepsInput = new TextInputBuilder()
        .setCustomId('bugreport_steps')
        .setLabel('Steps to reproduce (optional)')
        .setPlaceholder('1. Do this\n2. Then this\n3. Bug happens')
        .setStyle(TextInputStyle.Paragraph)
        .setMaxLength(500)
        .setRequired(false);

    const expectedInput = new TextInputBuilder()
        .setCustomId('bugreport_expected')
        .setLabel('Expected behavior (optional)')
        .setPlaceholder('What should have happened instead?')
        .setStyle(TextInputStyle.Short)
        .setMaxLength(200)
        .setRequired(false);

    modal.addComponents(
        new ActionRowBuilder().addComponents(titleInput),
        new ActionRowBuilder().addComponents(descriptionInput),
        new ActionRowBuilder().addComponents(stepsInput),
        new ActionRowBuilder().addComponents(expectedInput)
    );

    await interaction.showModal(modal);
}

/**
 * Handle bug report modal submission
 */
async function handleBugReportModal(interaction) {
    const title = interaction.fields.getTextInputValue('bugreport_title');
    const description = interaction.fields.getTextInputValue('bugreport_description');
    const steps = interaction.fields.getTextInputValue('bugreport_steps') || 'Not provided';
    const expected = interaction.fields.getTextInputValue('bugreport_expected') || 'Not provided';

    // Find the bug report channel
    let channel = null;
    if (bugReportChannelId) {
        channel = interaction.guild.channels.cache.get(bugReportChannelId);
    }
    if (!channel) {
        channel = interaction.guild.channels.cache.find(c => c.name === '🐛-bug-reports');
        if (channel) bugReportChannelId = channel.id;
    }

    if (!channel) {
        return interaction.reply({ content: '❌ Bug report channel not found!', ephemeral: true });
    }

    // Generate unique bug ID
    const bugId = `BUG-${Date.now().toString(36).toUpperCase()}`;

    // Create bug report embed (RED = unfixed)
    const bugEmbed = new EmbedBuilder()
        .setColor(0xFF0000)
        .setTitle(`🐛 Bug Report from ${interaction.user.displayName}`)
        .setThumbnail(interaction.user.displayAvatarURL())
        .addFields(
            { name: '📝 Title', value: title },
            { name: '📋 Description', value: description },
            { name: '🔄 Steps to Reproduce', value: steps },
            { name: '✅ Expected Behavior', value: expected },
            { name: '🔴 Status', value: '**OPEN**', inline: true },
            { name: '🆔 Bug ID', value: bugId, inline: true }
        )
        .setFooter({ text: `Reporter ID: ${interaction.user.id}` })
        .setTimestamp();

    // Create "Mark as Fixed" button for admins
    const actionRow = new ActionRowBuilder()
        .addComponents(
            new ButtonBuilder()
                .setCustomId(`mark_fixed_${bugId}`)
                .setLabel('✅ Mark as Fixed')
                .setStyle(ButtonStyle.Success)
        );

    try {
        // Find and delete the old button message
        const messages = await channel.messages.fetch({ limit: 20 });
        const buttonMessage = messages.find(m =>
            m.author.id === interaction.client.user.id &&
            m.components.length > 0 &&
            m.embeds.length > 0 &&
            m.embeds[0].title === '🐛 Bug Reports'
        );

        if (buttonMessage) {
            await buttonMessage.delete().catch(() => { });
        }

        // Post the bug report with the fix button
        await channel.send({ embeds: [bugEmbed], components: [actionRow] });

        // Repost the submit button embed at the bottom
        const buttonEmbed = new EmbedBuilder()
            .setColor(0xFF6B6B)
            .setTitle('🐛 Bug Reports')
            .setDescription('**Click the button below to report a bug!**\n\n🔴 New reports appear in red\n🟢 Fixed bugs turn green\n✅ Help us improve!')
            .setFooter({ text: 'NoidSwap Bug Reports • Click below to report' });

        const row = new ActionRowBuilder()
            .addComponents(
                new ButtonBuilder()
                    .setCustomId('submit_bugreport')
                    .setLabel('🐛 Report a Bug')
                    .setStyle(ButtonStyle.Danger)
            );

        await channel.send({ embeds: [buttonEmbed], components: [row] });

        await interaction.reply({
            content: `✅ **Bug report submitted!** Your report (${bugId}) has been logged.`,
            ephemeral: true
        });
    } catch (error) {
        console.error('Error posting bug report:', error);
        await interaction.reply({ content: '❌ Failed to post bug report. Please try again.', ephemeral: true });
    }
}

/**
 * Handle "Mark as Fixed" button click (admin only)
 */
async function handleMarkFixedButton(interaction) {
    // Check if user has admin role
    const member = interaction.member;
    if (!member || !member.roles.cache.has(ADMIN_ROLE_ID)) {
        return interaction.reply({
            content: '❌ Only admins can mark bugs as fixed.',
            ephemeral: true
        });
    }

    const message = interaction.message;
    const embed = message.embeds[0];

    if (!embed) {
        return interaction.reply({ content: '❌ Could not find bug report embed.', ephemeral: true });
    }

    // Get the bug ID from the button customId
    const bugId = interaction.customId.replace('mark_fixed_', '');

    // Create updated embed (GREEN = fixed)
    const fixedEmbed = EmbedBuilder.from(embed)
        .setColor(0x00FF00)
        .spliceFields(4, 1, { name: '🟢 Status', value: `**FIXED** by ${interaction.user.displayName}`, inline: true });

    // Remove the button (bug is fixed)
    try {
        await message.edit({ embeds: [fixedEmbed], components: [] });
        await interaction.reply({
            content: `✅ Bug ${bugId} has been marked as **FIXED**!`,
            ephemeral: true
        });
    } catch (error) {
        console.error('Error marking bug as fixed:', error);
        await interaction.reply({ content: '❌ Failed to update bug status.', ephemeral: true });
    }
}
