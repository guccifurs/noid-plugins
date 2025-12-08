const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');
const api = require('../api');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('subscription')
        .setDescription('Check your subscription status'),

    // This command doesn't require admin - anyone can check their own subscription
    requiresAdmin: false,

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const userData = await api.getUser(interaction.user.id);

            if (!userData || !userData.user) {
                return interaction.editReply({
                    embeds: [new EmbedBuilder()
                        .setColor(0xFF0000)
                        .setTitle('❌ Not Registered')
                        .setDescription('You don\'t have an account yet.\n\nPurchase a subscription to get started!')
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
                // Calculate time remaining
                let timeRemaining = 'Lifetime';
                if (subscription.expiresAt) {
                    const now = new Date();
                    const expires = new Date(subscription.expiresAt);
                    const diffMs = expires - now;

                    if (diffMs > 0) {
                        const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
                        const hours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));

                        if (days > 0) {
                            timeRemaining = `${days} day${days !== 1 ? 's' : ''}, ${hours} hour${hours !== 1 ? 's' : ''}`;
                        } else {
                            timeRemaining = `${hours} hour${hours !== 1 ? 's' : ''}`;
                        }
                    } else {
                        timeRemaining = 'Expired';
                    }
                }

                embed.addFields(
                    { name: '📦 Tier', value: subscription.tier || 'Standard', inline: true },
                    { name: '⏱️ Time Remaining', value: timeRemaining, inline: true },
                    { name: '💻 HWID', value: user.hwid ? '✅ Linked' : '❌ Not linked', inline: true }
                );

                if (subscription.expiresAt) {
                    embed.addFields({
                        name: '📅 Expires On',
                        value: new Date(subscription.expiresAt).toLocaleDateString('en-US', {
                            weekday: 'long',
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric'
                        })
                    });
                }
            } else {
                embed.setDescription('You don\'t have an active subscription.\n\nVisit #💰-buy to purchase!');
            }

            embed.setFooter({ text: 'NoidSwap' })
                .setTimestamp();

            await interaction.editReply({ embeds: [embed] });

        } catch (error) {
            console.error('Subscription check error:', error);
            await interaction.editReply({
                content: '❌ Error checking subscription. Please try again later.'
            });
        }
    }
};
