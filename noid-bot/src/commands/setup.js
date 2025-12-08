const { SlashCommandBuilder, EmbedBuilder, PermissionFlagsBits, ChannelType, ButtonBuilder, ButtonStyle, ActionRowBuilder } = require('discord.js');

// Color scheme
const COLORS = {
    PRIMARY: 0x00FF00,    // NoidSwap green
    SECONDARY: 0x2F3136,  // Discord dark
    INFO: 0x5865F2,       // Discord blurple
    WARNING: 0xFFA500,    // Orange
    ERROR: 0xFF0000       // Red
};

module.exports = {
    data: new SlashCommandBuilder()
        .setName('setup')
        .setDescription('Server setup commands')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
        .addSubcommand(sub => sub
            .setName('server')
            .setDescription('Build the entire server structure (categories, channels, embeds)'))
        .addSubcommand(sub => sub
            .setName('update-buy')
            .setDescription('Update the buy embed with current prices')),

    async execute(interaction) {
        const sub = interaction.options.getSubcommand();

        if (sub === 'server') {
            await setupServer(interaction);
        } else if (sub === 'update-buy') {
            await updateBuyEmbed(interaction);
        }
    }
};

async function updateBuyEmbed(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const guild = interaction.guild;

    // Find buy channel
    const buyChannel = guild.channels.cache.find(c => c.name.includes('buy'));
    if (!buyChannel) {
        return interaction.editReply({ content: '❌ Buy channel not found. Look for a channel with "buy" in its name.' });
    }

    // Fetch bot's messages
    const messages = await buyChannel.messages.fetch({ limit: 20 });
    const botMessage = messages.find(m => m.author.id === interaction.client.user.id && m.embeds.length > 0);

    if (!botMessage) {
        return interaction.editReply({ content: '❌ Could not find the buy embed. Try posting a new one.' });
    }

    // Create updated embed
    const buyButton = new ButtonBuilder()
        .setCustomId('buy_info')
        .setLabel('💎 Get NoidSwap')
        .setStyle(ButtonStyle.Success);

    const newEmbed = new EmbedBuilder()
        .setColor(0x00FF00)
        .setTitle('💰 Purchase NoidSwap')
        .setDescription('Get access to the most powerful gear swapper available!')
        .addFields(
            { name: '💳 Pricing', value: '• **Monthly**: $17.99/month\n• **Lifetime**: $350 one-time', inline: true },
            { name: '✅ Included', value: '• All features\n• Auto-updates\n• Support\n• Looper scripts', inline: true }
        )
        .setFooter({ text: 'Contact staff to purchase' });

    await botMessage.edit({
        embeds: [newEmbed],
        components: [new ActionRowBuilder().addComponents(buyButton)]
    });

    await interaction.editReply({ content: '✅ Buy embed updated with new prices!' });
}

async function setupServer(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const guild = interaction.guild;
    const log = [];

    try {
        // Get or create roles
        log.push('🔧 Setting up roles...');

        let memberRole = guild.roles.cache.find(r => r.name === 'Member');
        if (!memberRole) {
            memberRole = await guild.roles.create({
                name: 'Member',
                color: 0x3498DB,
                reason: 'NoidBot setup'
            });
            log.push('  ✅ Created Member role');
        } else {
            log.push('  ✓ Member role exists');
        }

        let modRole = guild.roles.cache.find(r => r.name === 'Moderator');
        if (!modRole) {
            modRole = await guild.roles.create({
                name: 'Moderator',
                color: 0xE74C3C,
                permissions: [PermissionFlagsBits.ManageMessages, PermissionFlagsBits.KickMembers],
                reason: 'NoidBot setup'
            });
            log.push('  ✅ Created Moderator role');
        } else {
            log.push('  ✓ Moderator role exists');
        }

        // Get subscriber role from env
        const subscriberRole = guild.roles.cache.get(process.env.SUBSCRIBER_ROLE_ID);
        if (!subscriberRole) {
            log.push('  ⚠️ Subscriber role not found - check SUBSCRIBER_ROLE_ID in .env');
        } else {
            log.push('  ✓ Subscriber role found');
        }

        // ========== CATEGORIES & CHANNELS ==========

        // 📢 INFORMATION
        log.push('\n📢 Creating INFORMATION category...');
        const infoCategory = await guild.channels.create({
            name: '📢 INFORMATION',
            type: ChannelType.GuildCategory,
            position: 0
        });

        const rulesChannel = await guild.channels.create({
            name: '📋-rules',
            type: ChannelType.GuildText,
            parent: infoCategory.id,
            permissionOverwrites: [
                { id: guild.id, deny: [PermissionFlagsBits.SendMessages] }
            ]
        });
        log.push('  ✅ #📋-rules');

        const featuresChannel = await guild.channels.create({
            name: '🔍-features',
            type: ChannelType.GuildText,
            parent: infoCategory.id,
            permissionOverwrites: [
                { id: guild.id, deny: [PermissionFlagsBits.SendMessages] }
            ]
        });
        log.push('  ✅ #🔍-features');

        const buyChannel = await guild.channels.create({
            name: '💰-buy',
            type: ChannelType.GuildText,
            parent: infoCategory.id,
            permissionOverwrites: [
                { id: guild.id, deny: [PermissionFlagsBits.SendMessages] }
            ]
        });
        log.push('  ✅ #💰-buy');

        // 🔐 VERIFICATION
        log.push('\n🔐 Creating VERIFICATION category...');
        const verifyCategory = await guild.channels.create({
            name: '🔐 VERIFICATION',
            type: ChannelType.GuildCategory,
            position: 1
        });

        const verifyChannel = await guild.channels.create({
            name: '✅-verify',
            type: ChannelType.GuildText,
            parent: verifyCategory.id,
            permissionOverwrites: [
                { id: guild.id, deny: [PermissionFlagsBits.SendMessages] },
                { id: memberRole.id, deny: [PermissionFlagsBits.ViewChannel] }
            ]
        });
        log.push('  ✅ #✅-verify');

        // 💬 COMMUNITY
        log.push('\n💬 Creating COMMUNITY category...');
        const communityCategory = await guild.channels.create({
            name: '💬 COMMUNITY',
            type: ChannelType.GuildCategory,
            position: 2,
            permissionOverwrites: [
                { id: guild.id, deny: [PermissionFlagsBits.ViewChannel] },
                { id: memberRole.id, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages] }
            ]
        });

        await guild.channels.create({
            name: '🌐-general',
            type: ChannelType.GuildText,
            parent: communityCategory.id
        });
        log.push('  ✅ #🌐-general');

        const ticketOpenChannel = await guild.channels.create({
            name: '🎫-open-ticket',
            type: ChannelType.GuildText,
            parent: communityCategory.id,
            permissionOverwrites: [
                { id: guild.id, deny: [PermissionFlagsBits.SendMessages] },
                { id: memberRole.id, allow: [PermissionFlagsBits.ViewChannel] }
            ]
        });
        log.push('  ✅ #🎫-open-ticket');

        // 🔥 SUBSCRIBERS
        log.push('\n🔥 Creating SUBSCRIBERS category...');
        const subPerms = [
            { id: guild.id, deny: [PermissionFlagsBits.ViewChannel] },
            { id: memberRole.id, deny: [PermissionFlagsBits.ViewChannel] }
        ];
        if (subscriberRole) {
            subPerms.push({ id: subscriberRole.id, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages] });
        }

        const subscriberCategory = await guild.channels.create({
            name: '🔥 SUBSCRIBERS',
            type: ChannelType.GuildCategory,
            position: 3,
            permissionOverwrites: subPerms
        });

        await guild.channels.create({
            name: '💬-noidswap-discussion',
            type: ChannelType.GuildText,
            parent: subscriberCategory.id
        });
        log.push('  ✅ #💬-noidswap-discussion');

        await guild.channels.create({
            name: '🐛-bug-reports',
            type: ChannelType.GuildText,
            parent: subscriberCategory.id
        });
        log.push('  ✅ #🐛-bug-reports');

        await guild.channels.create({
            name: '📜-looper-scripts',
            type: ChannelType.GuildForum,
            parent: subscriberCategory.id,
            topic: 'Share and discuss your looper scripts!'
        });
        log.push('  ✅ #📜-looper-scripts (forum)');

        const subCheckChannel = await guild.channels.create({
            name: '📊-my-subscription',
            type: ChannelType.GuildText,
            parent: subscriberCategory.id,
            permissionOverwrites: [
                { id: guild.id, deny: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages] },
                ...subPerms.slice(1).map(p => ({ ...p, deny: [PermissionFlagsBits.SendMessages] }))
            ]
        });
        log.push('  ✅ #📊-my-subscription');

        // 🔧 MODERATION
        log.push('\n🔧 Creating MODERATION category...');
        const modCategory = await guild.channels.create({
            name: '🔧 MODERATION',
            type: ChannelType.GuildCategory,
            position: 4,
            permissionOverwrites: [
                { id: guild.id, deny: [PermissionFlagsBits.ViewChannel] },
                { id: modRole.id, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages] }
            ]
        });

        await guild.channels.create({
            name: '📝-mod-chat',
            type: ChannelType.GuildText,
            parent: modCategory.id
        });
        log.push('  ✅ #📝-mod-chat');

        const ticketsChannel = await guild.channels.create({
            name: '🎟️-tickets',
            type: ChannelType.GuildText,
            parent: modCategory.id
        });
        log.push('  ✅ #🎟️-tickets');

        // ========== EMBEDS ==========
        log.push('\n📄 Posting embeds...');

        // Rules embed
        await rulesChannel.send({
            embeds: [new EmbedBuilder()
                .setColor(COLORS.PRIMARY)
                .setTitle('📋 Server Rules')
                .setDescription('Welcome to the NoidSwap community! Please follow these rules:')
                .addFields(
                    { name: '1️⃣ Be Respectful', value: 'Treat everyone with respect. No harassment, hate speech, or discrimination.' },
                    { name: '2️⃣ No Spam', value: 'Don\'t spam messages, emojis, or pings.' },
                    { name: '3️⃣ English Only', value: 'Please communicate in English for moderation purposes.' },
                    { name: '4️⃣ No Account Sharing', value: 'Your subscription is personal. Do not share your HWID or credentials.' },
                    { name: '5️⃣ Report Issues Properly', value: 'Use #🐛-bug-reports for bugs, #🎫-open-ticket for support.' }
                )
                .setFooter({ text: 'Breaking rules may result in a ban' })
                .setTimestamp()
            ]
        });
        log.push('  ✅ Rules embed');

        // Features embed
        await featuresChannel.send({
            embeds: [new EmbedBuilder()
                .setColor(COLORS.PRIMARY)
                .setTitle('🔍 NoidSwap Features')
                .setDescription('The most advanced gear swapper for OSRS PvP')
                .addFields(
                    { name: '⚡ Instant Gear Swaps', value: 'Switch your entire loadout with one hotkey', inline: true },
                    { name: '🎯 Smart Targeting', value: 'Automatic target tracking and acquisition', inline: true },
                    { name: '🔄 Looper Scripts', value: 'Create complex combat scripts with conditionals', inline: true },
                    { name: '❄️ Freeze Timers', value: 'Visual overlay for freeze durations', inline: true },
                    { name: '🛡️ Prayer Switching', value: 'Automated prayer management', inline: true },
                    { name: '📊 Debug Tools', value: 'Animation IDs, coordinates, overlays', inline: true },
                    { name: '🔥 Trigger System', value: 'React to animations, HP changes, and more', inline: true },
                    { name: '🌐 Network Updates', value: 'Instant updates without restart', inline: true }
                )
                .setImage('https://i.imgur.com/placeholder.png') // Add your image
                .setFooter({ text: 'NoidSwap - Dominate the Wilderness' })
            ]
        });
        log.push('  ✅ Features embed');

        // Buy embed with button
        const buyButton = new ButtonBuilder()
            .setCustomId('buy_info')
            .setLabel('💎 Get NoidSwap')
            .setStyle(ButtonStyle.Success);

        await buyChannel.send({
            embeds: [new EmbedBuilder()
                .setColor(COLORS.PRIMARY)
                .setTitle('💰 Purchase NoidSwap')
                .setDescription('Get access to the most powerful gear swapper available!')
                .addFields(
                    { name: '💳 Pricing', value: '• **Monthly**: $17.99/month\n• **Lifetime**: $350 one-time', inline: true },
                    { name: '✅ Included', value: '• All features\n• Auto-updates\n• Support\n• Looper scripts', inline: true }
                )
                .setFooter({ text: 'Contact staff to purchase' })
            ],
            components: [new ActionRowBuilder().addComponents(buyButton)]
        });
        log.push('  ✅ Buy embed');

        // Verify embed with button
        const verifyButton = new ButtonBuilder()
            .setCustomId('verify_member')
            .setLabel('✅ Verify & Enter Server')
            .setStyle(ButtonStyle.Primary);

        await verifyChannel.send({
            embeds: [new EmbedBuilder()
                .setColor(COLORS.INFO)
                .setTitle('🔐 Verification Required')
                .setDescription('Click the button below to verify and gain access to the server.\n\nBy verifying, you agree to follow our server rules.')
                .setFooter({ text: 'Welcome to NoidSwap!' })
            ],
            components: [new ActionRowBuilder().addComponents(verifyButton)]
        });
        log.push('  ✅ Verification embed');

        // Ticket embed with button
        const ticketButton = new ButtonBuilder()
            .setCustomId('create_ticket')
            .setLabel('🎫 Create Support Ticket')
            .setStyle(ButtonStyle.Secondary);

        await ticketOpenChannel.send({
            embeds: [new EmbedBuilder()
                .setColor(COLORS.SECONDARY)
                .setTitle('🎫 Support Tickets')
                .setDescription('Need help? Click below to create a private support ticket.\n\n**Before creating a ticket:**\n• Check #📋-rules\n• Check #🔍-features\n• Search existing discussions')
                .setFooter({ text: 'A moderator will respond as soon as possible' })
            ],
            components: [new ActionRowBuilder().addComponents(ticketButton)]
        });
        log.push('  ✅ Ticket embed');

        // Subscription check embed
        const subButton = new ButtonBuilder()
            .setCustomId('check_subscription')
            .setLabel('📊 Check My Subscription')
            .setStyle(ButtonStyle.Primary);

        await subCheckChannel.send({
            embeds: [new EmbedBuilder()
                .setColor(COLORS.PRIMARY)
                .setTitle('📊 Subscription Status')
                .setDescription('Click the button below to check your subscription details:\n\n• Time remaining\n• Tier\n• HWID status')
                .setFooter({ text: 'Your subscription info is private' })
            ],
            components: [new ActionRowBuilder().addComponents(subButton)]
        });
        log.push('  ✅ Subscription check embed');

        // Success!
        log.push('\n✅ **Server setup complete!**');

        await interaction.editReply({
            embeds: [new EmbedBuilder()
                .setColor(COLORS.PRIMARY)
                .setTitle('✅ Server Setup Complete')
                .setDescription('```\n' + log.join('\n') + '\n```')
                .addFields(
                    { name: 'Member Role ID', value: memberRole.id, inline: true },
                    { name: 'Mod Role ID', value: modRole.id, inline: true }
                )
                .setFooter({ text: 'Save these role IDs to your .env file!' })
            ]
        });

    } catch (error) {
        console.error('Setup error:', error);
        await interaction.editReply({
            content: `❌ Setup failed: ${error.message}\n\n**Progress:**\n${log.join('\n')}`
        });
    }
}
