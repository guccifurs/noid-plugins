const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, ChannelType, PermissionFlagsBits, ModalBuilder, TextInputBuilder, TextInputStyle } = require('discord.js');

// Category ID where suggestion channel will be created
const SUGGESTION_CATEGORY_ID = '1447038262211248138';

// Will be set when the channel is created
let suggestionChannelId = null;

module.exports = {
    data: new SlashCommandBuilder()
        .setName('suggestion')
        .setDescription('Manage suggestion channel')
        .addSubcommand(sub =>
            sub.setName('setup')
                .setDescription('Create and setup the suggestion channel'))
        .addSubcommand(sub =>
            sub.setName('stats')
                .setDescription('Show suggestion statistics')),

    async execute(interaction) {
        const subcommand = interaction.options.getSubcommand();

        if (subcommand === 'setup') {
            await setupSuggestionChannel(interaction);
        } else if (subcommand === 'stats') {
            await showSuggestionStats(interaction);
        }
    },

    // Export handlers for use in index.js
    handleSuggestionButton,
    handleSuggestionModal,
    getSuggestionChannelId: () => suggestionChannelId,
    setSuggestionChannelId: (id) => { suggestionChannelId = id; }
};

/**
 * Setup the suggestion channel with embed and button
 */
async function setupSuggestionChannel(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const guild = interaction.guild;
    const category = guild.channels.cache.get(SUGGESTION_CATEGORY_ID);

    if (!category) {
        return interaction.editReply({ content: `❌ Category not found (ID: ${SUGGESTION_CATEGORY_ID})` });
    }

    // Get Member role (adjust this ID to your actual member role)
    const memberRole = guild.roles.cache.find(r => r.name.toLowerCase().includes('member') || r.name.toLowerCase().includes('verified'));

    try {
        // Create the suggestion channel
        const channel = await guild.channels.create({
            name: '💡-suggestions',
            type: ChannelType.GuildText,
            parent: SUGGESTION_CATEGORY_ID,
            permissionOverwrites: [
                // Deny @everyone by default
                {
                    id: guild.id,
                    deny: [PermissionFlagsBits.ViewChannel]
                },
                // Allow members (if role exists)
                ...(memberRole ? [{
                    id: memberRole.id,
                    allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.ReadMessageHistory, PermissionFlagsBits.AddReactions],
                    deny: [PermissionFlagsBits.SendMessages]
                }] : []),
                // Allow the bot
                {
                    id: guild.members.me.id,
                    allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory, PermissionFlagsBits.ManageMessages, PermissionFlagsBits.AddReactions]
                }
            ]
        });

        // Store the channel ID
        suggestionChannelId = channel.id;

        // Post the welcome embed with button
        const embed = new EmbedBuilder()
            .setColor(0x5865F2)
            .setTitle('💡 Suggestions')
            .setDescription('**Welcome to the suggestion channel!**\n\nClick the button below to submit a suggestion.\n\n✅ Share your ideas\n✅ Vote on suggestions with 👍 and 👎\n✅ Help us improve!')
            .setThumbnail(guild.iconURL())
            .setFooter({ text: 'NoidSwap Suggestions • Click below to suggest' })
            .setTimestamp();

        const row = new ActionRowBuilder()
            .addComponents(
                new ButtonBuilder()
                    .setCustomId('submit_suggestion')
                    .setLabel('💡 Make a Suggestion')
                    .setStyle(ButtonStyle.Primary)
            );

        await channel.send({ embeds: [embed], components: [row] });

        await interaction.editReply({ content: `✅ Suggestion channel created: ${channel}\n\nMembers can now submit and vote on suggestions!` });

    } catch (error) {
        console.error('Error creating suggestion channel:', error);
        await interaction.editReply({ content: '❌ Failed to create suggestion channel: ' + error.message });
    }
}

/**
 * Show suggestion statistics
 */
async function showSuggestionStats(interaction) {
    await interaction.deferReply({ ephemeral: true });

    if (!suggestionChannelId) {
        // Try to find the channel by name
        const channel = interaction.guild.channels.cache.find(c => c.name === '💡-suggestions');
        if (channel) {
            suggestionChannelId = channel.id;
        } else {
            return interaction.editReply({ content: '❌ Suggestion channel not found. Run `/suggestion setup` first.' });
        }
    }

    const channel = interaction.guild.channels.cache.get(suggestionChannelId);
    if (!channel) {
        return interaction.editReply({ content: '❌ Suggestion channel not found' });
    }

    try {
        const messages = await channel.messages.fetch({ limit: 100 });
        const suggestions = messages.filter(m =>
            m.author.id === interaction.client.user.id &&
            m.embeds.length > 0 &&
            m.embeds[0].title?.includes('Suggestion from')
        );

        let totalUpvotes = 0;
        let totalDownvotes = 0;

        for (const [, msg] of suggestions) {
            const thumbsUp = msg.reactions.cache.get('👍');
            const thumbsDown = msg.reactions.cache.get('👎');
            if (thumbsUp) totalUpvotes += thumbsUp.count - 1; // -1 to exclude bot's reaction
            if (thumbsDown) totalDownvotes += thumbsDown.count - 1;
        }

        const embed = new EmbedBuilder()
            .setColor(0x5865F2)
            .setTitle('📊 Suggestion Statistics')
            .addFields(
                { name: '💡 Total Suggestions', value: `${suggestions.size}`, inline: true },
                { name: '👍 Total Upvotes', value: `${totalUpvotes}`, inline: true },
                { name: '👎 Total Downvotes', value: `${totalDownvotes}`, inline: true },
                { name: '📅 Channel', value: `<#${suggestionChannelId}>`, inline: true }
            )
            .setFooter({ text: 'Last 100 messages scanned' })
            .setTimestamp();

        await interaction.editReply({ embeds: [embed] });
    } catch (error) {
        console.error('Suggestion stats error:', error);
        await interaction.editReply({ content: '❌ Error fetching suggestion stats' });
    }
}

/**
 * Handle suggestion button click - shows modal
 */
async function handleSuggestionButton(interaction) {
    const modal = new ModalBuilder()
        .setCustomId('suggestion_modal')
        .setTitle('💡 Make a Suggestion');

    const titleInput = new TextInputBuilder()
        .setCustomId('suggestion_title')
        .setLabel('Suggestion Title')
        .setPlaceholder('Brief title for your suggestion...')
        .setStyle(TextInputStyle.Short)
        .setMinLength(5)
        .setMaxLength(100)
        .setRequired(true);

    const descriptionInput = new TextInputBuilder()
        .setCustomId('suggestion_description')
        .setLabel('Description')
        .setPlaceholder('Describe your suggestion in detail...')
        .setStyle(TextInputStyle.Paragraph)
        .setMinLength(20)
        .setMaxLength(1000)
        .setRequired(true);

    const categoryInput = new TextInputBuilder()
        .setCustomId('suggestion_category')
        .setLabel('Category (optional)')
        .setPlaceholder('e.g., Feature, Bug Fix, Improvement, UI, etc.')
        .setStyle(TextInputStyle.Short)
        .setMaxLength(50)
        .setRequired(false);

    modal.addComponents(
        new ActionRowBuilder().addComponents(titleInput),
        new ActionRowBuilder().addComponents(descriptionInput),
        new ActionRowBuilder().addComponents(categoryInput)
    );

    await interaction.showModal(modal);
}

/**
 * Handle suggestion modal submission
 */
async function handleSuggestionModal(interaction) {
    const title = interaction.fields.getTextInputValue('suggestion_title');
    const description = interaction.fields.getTextInputValue('suggestion_description');
    const category = interaction.fields.getTextInputValue('suggestion_category') || 'General';

    // Find the suggestion channel
    let channel = null;
    if (suggestionChannelId) {
        channel = interaction.guild.channels.cache.get(suggestionChannelId);
    }
    if (!channel) {
        channel = interaction.guild.channels.cache.find(c => c.name === '💡-suggestions');
        if (channel) suggestionChannelId = channel.id;
    }

    if (!channel) {
        return interaction.reply({ content: '❌ Suggestion channel not found!', ephemeral: true });
    }

    // Check for duplicate suggestion (same user in last hour)
    try {
        const messages = await channel.messages.fetch({ limit: 30 });
        const recentSuggestion = messages.find(m =>
            m.author.id === interaction.client.user.id &&
            m.embeds.length > 0 &&
            m.embeds[0].footer?.text?.includes(interaction.user.id) &&
            (Date.now() - m.createdTimestamp) < 60 * 60 * 1000
        );

        if (recentSuggestion) {
            return interaction.reply({
                content: '⚠️ You already submitted a suggestion in the last hour! Please wait before submitting again.',
                ephemeral: true
            });
        }
    } catch (e) {
        console.error('Error checking duplicate suggestion:', e);
    }

    // Create suggestion embed
    const suggestionEmbed = new EmbedBuilder()
        .setColor(0x5865F2)
        .setTitle(`💡 Suggestion from ${interaction.user.displayName}`)
        .setThumbnail(interaction.user.displayAvatarURL())
        .addFields(
            { name: '📝 Title', value: title },
            { name: '📋 Description', value: description },
            { name: '🏷️ Category', value: category, inline: true },
            { name: '📊 Voting', value: 'React with 👍 or 👎 to vote!', inline: true }
        )
        .setFooter({ text: `User ID: ${interaction.user.id} • Suggestion #${Date.now().toString(36)}` })
        .setTimestamp();

    try {
        // Find and delete the old button message
        const messages = await channel.messages.fetch({ limit: 20 });
        const buttonMessage = messages.find(m =>
            m.author.id === interaction.client.user.id &&
            m.components.length > 0 &&
            m.embeds.length > 0 &&
            m.embeds[0].title === '💡 Suggestions'
        );

        if (buttonMessage) {
            await buttonMessage.delete().catch(() => { });
        }

        // Post the suggestion
        const suggestionMsg = await channel.send({ embeds: [suggestionEmbed] });

        // Add voting reactions
        await suggestionMsg.react('👍');
        await suggestionMsg.react('👎');

        // Repost the button embed at the bottom
        const buttonEmbed = new EmbedBuilder()
            .setColor(0x5865F2)
            .setTitle('💡 Suggestions')
            .setDescription('**Click the button below to submit a suggestion!**\n\n✅ Share your ideas\n✅ Vote on suggestions with 👍 and 👎\n✅ Help us improve!')
            .setFooter({ text: 'NoidSwap Suggestions • Click below to suggest' });

        const row = new ActionRowBuilder()
            .addComponents(
                new ButtonBuilder()
                    .setCustomId('submit_suggestion')
                    .setLabel('💡 Make a Suggestion')
                    .setStyle(ButtonStyle.Primary)
            );

        await channel.send({ embeds: [buttonEmbed], components: [row] });

        await interaction.reply({
            content: '✅ **Thank you for your suggestion!** It has been posted for the community to vote on.',
            ephemeral: true
        });
    } catch (error) {
        console.error('Error posting suggestion:', error);
        await interaction.reply({ content: '❌ Failed to post suggestion. Please try again.', ephemeral: true });
    }
}
