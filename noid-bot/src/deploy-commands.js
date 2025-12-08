require('dotenv').config();
const { REST, Routes } = require('discord.js');
const fs = require('fs');
const path = require('path');

const commands = [];
const commandsPath = path.join(__dirname, 'commands');
const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js'));

for (const file of commandFiles) {
    const filePath = path.join(commandsPath, file);
    const command = require(filePath);
    if ('data' in command && 'execute' in command) {
        commands.push(command.data.toJSON());
    }
}

const rest = new REST().setToken(process.env.BOT_TOKEN);

(async () => {
    try {
        console.log(`Deploying ${commands.length} slash commands...`);

        // Deploy to specific guild (faster for testing)
        if (process.env.GUILD_ID) {
            await rest.put(
                Routes.applicationGuildCommands(process.env.CLIENT_ID, process.env.GUILD_ID),
                { body: commands }
            );
            console.log(`Deployed to guild ${process.env.GUILD_ID}`);
        } else {
            // Deploy globally (takes up to 1 hour)
            await rest.put(
                Routes.applicationCommands(process.env.CLIENT_ID),
                { body: commands }
            );
            console.log('Deployed globally');
        }

        console.log('Successfully deployed commands!');
    } catch (error) {
        console.error('Error deploying commands:', error);
    }
})();
