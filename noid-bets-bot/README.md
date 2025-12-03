# Noid Bets Discord Bot

This folder contains the Node.js Discord bot for the **Noid Bets** duel betting system.

Suggested setup:

1. Initialize a Node project here:
   ```bash
   cd noid-bets-bot
   npm init -y
   ```
2. Install dependencies:
   ```bash
   npm install discord.js express
   npm install --save-dev nodemon
   ```
3. Create an `index.js` based on the bot skeleton provided in the IDE (Noid Bets Discord bot).
4. Configure environment variables:
   - `DISCORD_TOKEN`
   - `CLIENT_ID`
   - `BETTING_CHANNEL_ID` (your bets channel, e.g. 1428451757129863291)
   - `PORT` (e.g. 8081)

Then run the bot with:

```bash
npm run dev
```
