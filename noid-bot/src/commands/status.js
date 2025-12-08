const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');
const api = require('../api');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('status')
        .setDescription('Check your subscription status'),

    async execute(interaction) {
        try {
            await interaction.deferReply({ ephemeral: true });

            const userData = await api.getUser(interaction.user.id);

            if (!userData || !userData.user) {
                const embed = new EmbedBuilder()
                    .setColor(0xFF0000)
                    .setTitle('❌ Not Registered')
                    .setDescription('You have not linked your account yet.')
                    .addFields(
                        { name: 'How to Link', value: 'Use `/link <hwid>` to link your HWID from NoidPlugin.' }
                    )
                    .setFooter({ text: 'NoidPlugin Authentication' });

                return interaction.editReply({ embeds: [embed] });
            }

            const { user, subscription } = userData;

            let statusColor, statusText;
            if (subscription && subscription.active) {
                statusColor = 0x00FF00;
                statusText = '✅ Active';
            } else {
                statusColor = 0xFF0000;
                statusText = '❌ Inactive';
            }

            const embed = new EmbedBuilder()
                .setColor(statusColor)
                .setTitle('📊 Subscription Status')
                .addFields(
                    { name: 'Discord', value: `<@${user.discordId}>`, inline: true },
                    { name: 'Status', value: statusText, inline: true },
                    { name: 'HWID Linked', value: user.hwid ? '✅ Yes' : '❌ No', inline: true }
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

            embed.setFooter({ text: 'NoidPlugin Authentication' })
                .setTimestamp();

            await interaction.editReply({ embeds: [embed] });

        } catch (error) {
            console.error('Status error:', error);
            await interaction.editReply({
                content: '❌ Failed to fetch status. Please try again later.'
            });
        }
    }
};
