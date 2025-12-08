const { Events } = require('discord.js');

module.exports = {
    name: Events.ClientReady,
    once: true,
    execute(client) {
        console.log(`✅ Noid Bot is ready! Logged in as ${client.user.tag}`);

        // Set activity
        client.user.setActivity('NoidPlugin Auth', { type: 3 }); // Watching

        console.log(`Serving ${client.guilds.cache.size} guild(s)`);
    }
};
