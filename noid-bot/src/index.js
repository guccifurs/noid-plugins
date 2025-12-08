require('dotenv').config();
const { Client, Collection, GatewayIntentBits, Events, EmbedBuilder, ChannelType, PermissionFlagsBits } = require('discord.js');
const express = require('express');
const fs = require('fs');
const path = require('path');
const api = require('./api');
const scriptsApi = require('./scriptsApi');

const client = new Client({
    intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMembers,
        GatewayIntentBits.GuildMessages
    ]
});

// Load commands
client.commands = new Collection();
const commandsPath = path.join(__dirname, 'commands');
const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js'));

for (const file of commandFiles) {
    const filePath = path.join(commandsPath, file);
    const command = require(filePath);
    if ('data' in command && 'execute' in command) {
        client.commands.set(command.data.name, command);
        console.log(`Loaded command: ${command.data.name}`);
    }
}

// Load events
const eventsPath = path.join(__dirname, 'events');
const eventFiles = fs.readdirSync(eventsPath).filter(file => file.endsWith('.js'));

for (const file of eventFiles) {
    const filePath = path.join(eventsPath, file);
    const event = require(filePath);
    if (event.once) {
        client.once(event.name, (...args) => event.execute(...args));
    } else {
        client.on(event.name, (...args) => event.execute(...args));
    }
    console.log(`Loaded event: ${event.name}`);
}

// Commands that don't require admin
const PUBLIC_COMMANDS = ['subscription', 'ticket', 'vouch'];
const ADMIN_ROLE_ID = '1446959996553396244';

// Handle slash commands
client.on(Events.InteractionCreate, async interaction => {
    if (interaction.isChatInputCommand()) {
        const command = client.commands.get(interaction.commandName);
        if (!command) return;

        // Check for admin role (unless public command)
        if (!PUBLIC_COMMANDS.includes(interaction.commandName)) {
            const member = interaction.member;
            if (!member || !member.roles.cache.has(ADMIN_ROLE_ID)) {
                return interaction.reply({
                    content: '❌ You do not have permission to use this command. Admin role required.',
                    ephemeral: true
                });
            }
        }

        try {
            await command.execute(interaction);
        } catch (error) {
            console.error(`Error executing ${interaction.commandName}:`, error);
            const reply = { content: 'There was an error executing this command!', ephemeral: true };
            if (interaction.replied || interaction.deferred) {
                await interaction.followUp(reply);
            } else {
                await interaction.reply(reply);
            }
        }
    }

    // Handle button interactions
    if (interaction.isButton()) {
        await handleButton(interaction);
    }

    // Handle modal submissions
    if (interaction.isModalSubmit()) {
        await handleModal(interaction);
    }
});

/**
 * Handle button interactions
 */
async function handleButton(interaction) {
    const customId = interaction.customId;

    try {
        switch (customId) {
            case 'verify_member':
                await handleVerify(interaction);
                break;
            case 'create_ticket':
                await handleCreateTicket(interaction);
                break;
            case 'check_subscription':
                await handleCheckSubscription(interaction);
                break;
            case 'buy_info':
                await handleBuyInfo(interaction);
                break;
            case 'submit_vouch':
                const vouchCmd = require('./commands/vouch');
                await vouchCmd.handleVouchButton(interaction);
                break;
            default:
                // Check for subscription page navigation
                if (customId.startsWith('sub_page_')) {
                    const adminCmd = require('./commands/admin');
                    if (adminCmd.handleSubscriptionPage) {
                        await adminCmd.handleSubscriptionPage(interaction, customId);
                        return;
                    }
                }
                console.log(`Unknown button: ${customId}`);
        }
    } catch (error) {
        console.error(`Button error (${customId}):`, error);
        const reply = { content: '❌ An error occurred. Please try again.', ephemeral: true };
        if (interaction.replied || interaction.deferred) {
            await interaction.followUp(reply);
        } else {
            await interaction.reply(reply);
        }
    }
}

/**
 * Handle modal submissions
 */
async function handleModal(interaction) {
    const customId = interaction.customId;

    try {
        switch (customId) {
            case 'vouch_modal':
                const vouchCmd = require('./commands/vouch');
                await vouchCmd.handleVouchModal(interaction);
                break;
            default:
                console.log(`Unknown modal: ${customId}`);
        }
    } catch (error) {
        console.error(`Modal error (${customId}):`, error);
        const reply = { content: '❌ An error occurred. Please try again.', ephemeral: true };
        if (interaction.replied || interaction.deferred) {
            await interaction.followUp(reply);
        } else {
            await interaction.reply(reply);
        }
    }
}

/**
 * Verify button - grants Member role
 */
async function handleVerify(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const guild = interaction.guild;
    const member = interaction.member;

    // Find Member role
    let memberRole = guild.roles.cache.find(r => r.name === 'Member');
    if (!memberRole && process.env.MEMBER_ROLE_ID) {
        memberRole = guild.roles.cache.get(process.env.MEMBER_ROLE_ID);
    }

    if (!memberRole) {
        return interaction.editReply({ content: '❌ Member role not found. Please contact an admin.' });
    }

    if (member.roles.cache.has(memberRole.id)) {
        return interaction.editReply({
            embeds: [new EmbedBuilder()
                .setColor(0xFFA500)
                .setTitle('✓ Already Verified')
                .setDescription('You already have the Member role!')
            ]
        });
    }

    try {
        await member.roles.add(memberRole);
        await interaction.editReply({
            embeds: [new EmbedBuilder()
                .setColor(0x00FF00)
                .setTitle('✅ Verification Complete!')
                .setDescription('Welcome to NoidSwap! You now have access to the server.\n\n• Check out #🔍-features to see what NoidSwap can do\n• Visit #💰-buy to get a subscription\n• Open a ticket in #🎫-open-ticket if you need help')
            ]
        });
    } catch (error) {
        console.error('Verify error:', error);
        await interaction.editReply({ content: '❌ Failed to grant role. Please contact an admin.' });
    }
}

/**
 * Create ticket button - creates private channel
 */
async function handleCreateTicket(interaction) {
    await interaction.deferReply({ ephemeral: true });

    const guild = interaction.guild;
    const user = interaction.user;

    // Find tickets category or create one
    let ticketsCategory = guild.channels.cache.find(c =>
        c.type === ChannelType.GuildCategory && c.name.toLowerCase().includes('ticket')
    );

    if (!ticketsCategory) {
        ticketsCategory = await guild.channels.create({
            name: '🎫 Tickets',
            type: ChannelType.GuildCategory,
            reason: 'Ticket system category'
        });
    }

    // Check if user already has an open ticket channel
    const existingTicket = guild.channels.cache.find(c =>
        c.type === ChannelType.GuildText &&
        c.parentId === ticketsCategory.id &&
        c.name.includes(user.username.toLowerCase().replace(/[^a-z0-9]/g, '-'))
    );

    if (existingTicket) {
        return interaction.editReply({
            embeds: [new EmbedBuilder()
                .setColor(0xFFA500)
                .setTitle('⚠️ Ticket Exists')
                .setDescription(`You already have an open ticket: ${existingTicket}\n\nPlease use your existing ticket or close it first.`)
            ]
        });
    }

    try {
        // Get roles for permissions
        const modRole = guild.roles.cache.find(r => r.name === 'Moderator');
        const adminRole = guild.roles.cache.find(r => r.name === 'Admin') ||
            guild.roles.cache.find(r => r.permissions.has('Administrator'));

        // Sanitize username for channel name
        const sanitizedName = user.username.toLowerCase().replace(/[^a-z0-9]/g, '-').slice(0, 20);

        // Create private ticket channel
        const ticketChannel = await guild.channels.create({
            name: `🎫-${sanitizedName}`,
            type: ChannelType.GuildText,
            parent: ticketsCategory.id,
            reason: `Ticket created by ${user.tag}`,
            permissionOverwrites: [
                // Deny everyone by default
                {
                    id: guild.id,
                    deny: ['ViewChannel']
                },
                // Allow the ticket creator
                {
                    id: user.id,
                    allow: ['ViewChannel', 'SendMessages', 'ReadMessageHistory', 'AttachFiles', 'EmbedLinks']
                },
                // Allow moderators (if role exists)
                ...(modRole ? [{
                    id: modRole.id,
                    allow: ['ViewChannel', 'SendMessages', 'ReadMessageHistory', 'AttachFiles', 'EmbedLinks', 'ManageMessages']
                }] : []),
                // Allow admins (if role exists and different from mod)
                ...(adminRole && adminRole.id !== modRole?.id ? [{
                    id: adminRole.id,
                    allow: ['ViewChannel', 'SendMessages', 'ReadMessageHistory', 'AttachFiles', 'EmbedLinks', 'ManageMessages', 'ManageChannels']
                }] : []),
                // Allow the bot
                {
                    id: guild.members.me.id,
                    allow: ['ViewChannel', 'SendMessages', 'ReadMessageHistory', 'ManageChannels', 'ManageMessages']
                }
            ]
        });

        // Send welcome message
        await ticketChannel.send({
            content: `<@${user.id}>`,
            embeds: [new EmbedBuilder()
                .setColor(0x00FF00)
                .setTitle('🎫 Support Ticket')
                .setDescription(`Hello ${user}! A moderator will be with you shortly.\n\n**Please describe your issue:**\n• What is the problem?\n• When did it start?\n• Any error messages?`)
                .addFields(
                    { name: '🔒 Close Ticket', value: 'Use `/ticket close` when done' }
                )
                .setFooter({ text: 'NoidSwap Support' })
                .setTimestamp()
            ]
        });

        await interaction.editReply({
            embeds: [new EmbedBuilder()
                .setColor(0x00FF00)
                .setTitle('✅ Ticket Created!')
                .setDescription(`Your ticket has been created: ${ticketChannel}\n\nA moderator will respond as soon as possible.`)
            ]
        });

    } catch (error) {
        console.error('Create ticket error:', error);
        await interaction.editReply({ content: '❌ Failed to create ticket: ' + error.message });
    }
}

/**
 * Check subscription button
 */
async function handleCheckSubscription(interaction) {
    await interaction.deferReply({ ephemeral: true });

    try {
        const userData = await api.getUser(interaction.user.id);

        if (!userData || !userData.user) {
            return interaction.editReply({
                embeds: [new EmbedBuilder()
                    .setColor(0xFF0000)
                    .setTitle('❌ Not Registered')
                    .setDescription('You don\'t have an account yet.\n\nPurchase a subscription in #💰-buy to get started!')
                ]
            });
        }

        const { user, subscription } = userData;
        const isActive = subscription?.active;

        const embed = new EmbedBuilder()
            .setColor(isActive ? 0x00FF00 : 0xFF0000)
            .setTitle(isActive ? '✅ Active Subscription' : '❌ No Active Subscription')
            .setThumbnail(interaction.user.displayAvatarURL());

        if (subscription && isActive) {
            let timeRemaining = 'Lifetime';
            if (subscription.expiresAt) {
                const now = new Date();
                const expires = new Date(subscription.expiresAt);
                const diffMs = expires - now;

                if (diffMs > 0) {
                    const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
                    const hours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                    timeRemaining = days > 0 ? `${days}d ${hours}h` : `${hours}h`;
                }
            }

            embed.addFields(
                { name: '📦 Tier', value: subscription.tier || 'Standard', inline: true },
                { name: '⏱️ Time Left', value: timeRemaining, inline: true },
                { name: '💻 HWID', value: user.hwid ? '✅ Linked' : '❌ Not linked', inline: true }
            );
        } else {
            embed.setDescription('You don\'t have an active subscription.\n\nVisit #💰-buy to purchase!');
        }

        await interaction.editReply({ embeds: [embed] });

    } catch (error) {
        console.error('Check subscription error:', error);
        await interaction.editReply({ content: '❌ Error checking subscription.' });
    }
}

/**
 * Buy info button
 */
async function handleBuyInfo(interaction) {
    await interaction.reply({
        ephemeral: true,
        embeds: [new EmbedBuilder()
            .setColor(0x00FF00)
            .setTitle('💎 How to Purchase')
            .setDescription('To purchase NoidSwap:\n\n1️⃣ Open a ticket in #🎫-open-ticket\n2️⃣ Tell us which plan you want\n3️⃣ We\'ll provide payment details\n4️⃣ You\'ll get access instantly!')
            .addFields(
                { name: '💳 Payment Methods', value: '• PayPal\n• Crypto (BTC, ETH)\n• OSRS GP' }
            )
        ]
    });
}

/**
 * Member join event - send welcome message
 */
client.on(Events.GuildMemberAdd, async member => {
    console.log(`[Member Join] ${member.user.tag} joined`);

    // Find verify channel
    const verifyChannel = member.guild.channels.cache.find(c => c.name === '✅-verify');

    if (verifyChannel) {
        try {
            // Send ephemeral-like welcome (DM)
            await member.send({
                embeds: [new EmbedBuilder()
                    .setColor(0x00FF00)
                    .setTitle('👋 Welcome to NoidSwap!')
                    .setDescription(`Hey ${member.user.username}!\n\nTo access the server, please verify in ${verifyChannel}`)
                    .setFooter({ text: 'NoidSwap - The Ultimate Gear Swapper' })
                ]
            }).catch(() => console.log(`[Welcome] Couldn't DM ${member.user.tag}`));
        } catch (e) {
            // User has DMs disabled
        }
    }
});

/**
 * Sync roles with subscription status
 */
async function syncSubscriptionRoles() {
    try {
        const guild = client.guilds.cache.get(process.env.GUILD_ID);
        if (!guild) return;

        const subscriberRoleId = process.env.SUBSCRIBER_ROLE_ID;
        const subscriberRole = guild.roles.cache.get(subscriberRoleId);
        if (!subscriberRole) return;

        const response = await fetch(`${process.env.API_URL}/auth/subscriptions/active`, {
            headers: { 'x-api-key': process.env.API_KEY }
        });

        if (!response.ok) return;

        const { subscriptions } = await response.json();
        const activeDiscordIds = new Set(subscriptions.map(s => s.discord_id));

        await guild.members.fetch();

        let added = 0, removed = 0;

        for (const [memberId, member] of guild.members.cache) {
            const hasRole = member.roles.cache.has(subscriberRoleId);
            const hasActiveSubscription = activeDiscordIds.has(memberId);

            if (hasActiveSubscription && !hasRole) {
                try {
                    await member.roles.add(subscriberRole);
                    added++;
                } catch (err) { }
            } else if (!hasActiveSubscription && hasRole) {
                try {
                    await member.roles.remove(subscriberRole);
                    removed++;
                } catch (err) { }
            }
        }

        if (added > 0 || removed > 0) {
            console.log(`[Role Sync] +${added} / -${removed}`);
        }
    } catch (err) {
        console.error('[Role Sync] Error:', err.message);
    }
}

// Start role sync after ready
client.once('ready', () => {
    console.log(`✅ ${client.user.tag} is online!`);
    setTimeout(syncSubscriptionRoles, 5000);
    setInterval(syncSubscriptionRoles, 5 * 60 * 1000);
});

client.login(process.env.BOT_TOKEN);

// ============================================
// Script SDN API Server (Express)
// ============================================
const API_PORT = process.env.SCRIPT_API_PORT || 3001;

const app = express();
app.use(express.json({ limit: '1mb' }));

// CORS headers for the Java client
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Content-Type, x-session-token, x-discord-id, x-discord-name');
    if (req.method === 'OPTIONS') {
        return res.sendStatus(200);
    }
    next();
});

// Mount scripts API
app.use('/api/scripts', scriptsApi);

// Health check endpoint
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', service: 'script-sdn' });
});

// Start API server
app.listen(API_PORT, () => {
    console.log(`🚀 Script SDN API listening on port ${API_PORT}`);
});
