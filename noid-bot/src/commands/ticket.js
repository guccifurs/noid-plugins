const { SlashCommandBuilder, EmbedBuilder, ChannelType, PermissionFlagsBits } = require('discord.js');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('ticket')
        .setDescription('Ticket management commands')
        .addSubcommand(sub => sub
            .setName('close')
            .setDescription('Close the current ticket')),

    async execute(interaction) {
        const sub = interaction.options.getSubcommand();

        if (sub === 'close') {
            await closeTicket(interaction);
        }
    }
};

async function closeTicket(interaction) {
    const channel = interaction.channel;
    const user = interaction.user;
    const guild = interaction.guild;

    // Check if this is a ticket channel (in Tickets category and starts with 🎫)
    const isTicketChannel = channel.type === ChannelType.GuildText &&
        channel.name.startsWith('🎫') &&
        channel.parent?.name.toLowerCase().includes('ticket');

    if (!isTicketChannel) {
        return interaction.reply({
            content: '❌ This command can only be used inside a ticket channel.',
            ephemeral: true
        });
    }

    // Check if user has permission to close (ticket creator, mod, or admin)
    const modRole = guild.roles.cache.find(r => r.name === 'Moderator');
    const member = guild.members.cache.get(user.id);

    const isTicketOwner = channel.permissionOverwrites.cache.has(user.id);
    const isMod = modRole && member.roles.cache.has(modRole.id);
    const isAdmin = member.permissions.has(PermissionFlagsBits.Administrator);

    if (!isTicketOwner && !isMod && !isAdmin) {
        return interaction.reply({
            content: '❌ You do not have permission to close this ticket.',
            ephemeral: true
        });
    }

    await interaction.deferReply();

    try {
        const messages = await channel.messages.fetch({ limit: 100 });

        const closeEmbed = new EmbedBuilder()
            .setColor(0xFF0000)
            .setTitle('🔒 Ticket Closing')
            .setDescription(`This ticket is being closed by ${interaction.user}\n\n**This channel will be deleted in 5 seconds.**`)
            .addFields({ name: 'Messages', value: `${messages.size}` })
            .setTimestamp();

        await interaction.followUp({ embeds: [closeEmbed] });

        // Wait 5 seconds then delete the channel
        setTimeout(async () => {
            try {
                await channel.delete(`Ticket closed by ${interaction.user.tag}`);
            } catch (deleteError) {
                console.error('Error deleting ticket channel:', deleteError);
            }
        }, 5000);

    } catch (error) {
        console.error('Close ticket error:', error);
        try {
            await interaction.followUp({
                content: '❌ Error closing ticket: ' + error.message
            });
        } catch (e) {
            // Already replied, ignore
        }
    }
}
