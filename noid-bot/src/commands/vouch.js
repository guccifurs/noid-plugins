const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, ModalBuilder, TextInputBuilder, TextInputStyle, PermissionFlagsBits } = require('discord.js');

const VOUCH_CHANNEL_ID = '1446896617977544796';

module.exports = {
    data: new SlashCommandBuilder()
        .setName('vouch')
        .setDescription('Vouch system')
        .addSubcommand(sub =>
            sub.setName('setup')
                .setDescription('Setup the vouch channel with button'))
        .addSubcommand(sub =>
            sub.setName('stats')
                .setDescription('Show vouch statistics')),

    async execute(interaction) {
        const subcommand = interaction.options.getSubcommand();

        if (subcommand === 'setup') {
            await setupVouchChannel(interaction);
        } else if (subcommand === 'stats') {
            await showVouchStats(interaction);
        }
    },

    // Export handlers for use in index.js
    handleVouchButton,
    handleVouchModal
};

/**
 * Setup the vouch channel with embed and button
 */
async function setupVouchChannel(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const channel = interaction.guild.channels.cache.get(VOUCH_CHANNEL_ID);
    if (!channel) {
        return interaction.editReply({ content: `❌ Vouch channel not found (ID: ${VOUCH_CHANNEL_ID})` });
    }

    const embed = new EmbedBuilder()
        .setColor(0x00FF00)
        .setTitle('⭐ Vouches')
        .setDescription('**Welcome to our vouch channel!**\n\nClicked the button below to leave a vouch for NoidSwap.\n\n✅ Share your experience\n✅ Help others make a decision\n✅ Support the community')
        .setThumbnail(interaction.guild.iconURL())
        .setFooter({ text: 'NoidSwap Vouches • Click below to vouch' })
        .setTimestamp();

    const row = new ActionRowBuilder()
        .addComponents(
            new ButtonBuilder()
                .setCustomId('submit_vouch')
                .setLabel('⭐ Leave a Vouch')
                .setStyle(ButtonStyle.Success)
        );

    await channel.send({ embeds: [embed], components: [row] });

    await interaction.editReply({ content: `✅ Vouch button posted in ${channel}!` });
}

/**
 * Show vouch statistics
 */
async function showVouchStats(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const channel = interaction.guild.channels.cache.get(VOUCH_CHANNEL_ID);
    if (!channel) {
        return interaction.editReply({ content: `❌ Vouch channel not found` });
    }

    try {
        // Fetch messages to count vouches (bot messages with "Vouch" title)
        const messages = await channel.messages.fetch({ limit: 100 });
        const vouches = messages.filter(m =>
            m.author.id === interaction.client.user.id &&
            m.embeds.length > 0 &&
            m.embeds[0].title?.includes('Vouch from')
        );

        const embed = new EmbedBuilder()
            .setColor(0x00FF00)
            .setTitle('📊 Vouch Statistics')
            .addFields(
                { name: '⭐ Total Vouches', value: `${vouches.size}`, inline: true },
                { name: '📅 Channel', value: `<#${VOUCH_CHANNEL_ID}>`, inline: true }
            )
            .setFooter({ text: 'Last 100 messages scanned' })
            .setTimestamp();

        await interaction.editReply({ embeds: [embed] });
    } catch (error) {
        console.error('Vouch stats error:', error);
        await interaction.editReply({ content: '❌ Error fetching vouch stats' });
    }
}

/**
 * Handle vouch button click - shows modal
 */
async function handleVouchButton(interaction) {
    const modal = new ModalBuilder()
        .setCustomId('vouch_modal')
        .setTitle('⭐ Leave a Vouch');

    const ratingInput = new TextInputBuilder()
        .setCustomId('vouch_rating')
        .setLabel('Rating (1-5 stars)')
        .setPlaceholder('5')
        .setStyle(TextInputStyle.Short)
        .setMinLength(1)
        .setMaxLength(1)
        .setRequired(true);

    const messageInput = new TextInputBuilder()
        .setCustomId('vouch_message')
        .setLabel('Your Experience')
        .setPlaceholder('Tell us about your experience with NoidSwap...')
        .setStyle(TextInputStyle.Paragraph)
        .setMinLength(10)
        .setMaxLength(500)
        .setRequired(true);

    const productInput = new TextInputBuilder()
        .setCustomId('vouch_product')
        .setLabel('Product/Service (optional)')
        .setPlaceholder('e.g., Lifetime Subscription, Monthly, etc.')
        .setStyle(TextInputStyle.Short)
        .setMaxLength(100)
        .setRequired(false);

    modal.addComponents(
        new ActionRowBuilder().addComponents(ratingInput),
        new ActionRowBuilder().addComponents(messageInput),
        new ActionRowBuilder().addComponents(productInput)
    );

    await interaction.showModal(modal);
}

/**
 * Handle vouch modal submission
 */
async function handleVouchModal(interaction) {
    const rating = parseInt(interaction.fields.getTextInputValue('vouch_rating')) || 5;
    const message = interaction.fields.getTextInputValue('vouch_message');
    const product = interaction.fields.getTextInputValue('vouch_product') || 'NoidSwap';

    // Validate rating
    const clampedRating = Math.max(1, Math.min(5, rating));
    const stars = '⭐'.repeat(clampedRating) + '☆'.repeat(5 - clampedRating);

    const channel = interaction.guild.channels.cache.get(VOUCH_CHANNEL_ID);
    if (!channel) {
        return interaction.reply({ content: '❌ Vouch channel not found!', ephemeral: true });
    }

    // Check for duplicate vouch (same user in last 24 hours)
    try {
        const messages = await channel.messages.fetch({ limit: 50 });
        const recentVouch = messages.find(m =>
            m.author.id === interaction.client.user.id &&
            m.embeds.length > 0 &&
            m.embeds[0].footer?.text?.includes(interaction.user.id) &&
            (Date.now() - m.createdTimestamp) < 24 * 60 * 60 * 1000
        );

        if (recentVouch) {
            return interaction.reply({
                content: '⚠️ You already submitted a vouch in the last 24 hours! Please wait before vouching again.',
                ephemeral: true
            });
        }
    } catch (e) {
        console.error('Error checking duplicate vouch:', e);
    }

    // Create vouch embed
    const vouchEmbed = new EmbedBuilder()
        .setColor(clampedRating >= 4 ? 0x00FF00 : clampedRating >= 3 ? 0xFFA500 : 0xFF0000)
        .setTitle(`⭐ Vouch from ${interaction.user.displayName}`)
        .setThumbnail(interaction.user.displayAvatarURL())
        .setDescription(`> "${message}"`)
        .addFields(
            { name: 'Rating', value: stars, inline: true },
            { name: 'Product', value: product, inline: true }
        )
        .setFooter({ text: `User ID: ${interaction.user.id}` })
        .setTimestamp();

    try {
        // Find and delete the old button message
        const messages = await channel.messages.fetch({ limit: 20 });
        const buttonMessage = messages.find(m =>
            m.author.id === interaction.client.user.id &&
            m.components.length > 0 &&
            m.embeds.length > 0 &&
            m.embeds[0].title === '⭐ Vouches'
        );

        if (buttonMessage) {
            await buttonMessage.delete().catch(() => { });
        }

        // Post the vouch
        await channel.send({ embeds: [vouchEmbed] });

        // Repost the button embed at the bottom
        const buttonEmbed = new EmbedBuilder()
            .setColor(0x00FF00)
            .setTitle('⭐ Vouches')
            .setDescription('**Click the button below to leave a vouch!**\n\n✅ Share your experience\n✅ Help others decide\n✅ Support the community')
            .setFooter({ text: 'NoidSwap Vouches • Click below to vouch' });

        const row = new ActionRowBuilder()
            .addComponents(
                new ButtonBuilder()
                    .setCustomId('submit_vouch')
                    .setLabel('⭐ Leave a Vouch')
                    .setStyle(ButtonStyle.Success)
            );

        await channel.send({ embeds: [buttonEmbed], components: [row] });

        await interaction.reply({
            content: '✅ **Thank you for your vouch!** Your feedback has been posted.',
            ephemeral: true
        });
    } catch (error) {
        console.error('Error posting vouch:', error);
        await interaction.reply({ content: '❌ Failed to post vouch. Please try again.', ephemeral: true });
    }
}
