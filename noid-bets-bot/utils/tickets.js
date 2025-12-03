// Support ticket system utilities
const { PermissionsBitField, ChannelType, EmbedBuilder } = require('discord.js');
const fs = require('fs');
const path = require('path');

const SUPPORT_CHANNEL_ID = '1441059609212096543';
const TICKETS_DIR = path.join(__dirname, '..', 'tickets');

// Ensure tickets directory exists
if (!fs.existsSync(TICKETS_DIR)) {
  fs.mkdirSync(TICKETS_DIR, { recursive: true });
}

// Active tickets storage (in-memory)
const activeTickets = new Map(); // userId -> { channelId, createdAt }

async function createTicketChannel(guild, user, adminRoleId) {
  const ticketNumber = Date.now().toString().slice(-6);
  const channelName = `ticket-${user.username}-${ticketNumber}`;
  
  // Get admin role
  const adminRole = await guild.roles.fetch(adminRoleId);
  if (!adminRole) {
    throw new Error('Admin role not found');
  }
  
  // Create ticket category if it doesn't exist
  let category = guild.channels.cache.find(c => 
    c.type === ChannelType.GuildCategory && c.name.toLowerCase() === '🎫 support tickets'
  );
  
  if (!category) {
    category = await guild.channels.create({
      name: '🎫 Support Tickets',
      type: ChannelType.GuildCategory,
      permissionOverwrites: [
        {
          id: guild.id,
          deny: [PermissionsBitField.Flags.ViewChannel]
        },
        {
          id: adminRoleId,
          allow: [
            PermissionsBitField.Flags.ViewChannel,
            PermissionsBitField.Flags.SendMessages,
            PermissionsBitField.Flags.ManageChannels
          ]
        }
      ]
    });
  }
  
  // Create ticket channel
  const channel = await guild.channels.create({
    name: channelName,
    type: ChannelType.GuildText,
    parent: category.id,
    permissionOverwrites: [
      {
        id: guild.id,
        deny: [PermissionsBitField.Flags.ViewChannel]
      },
      {
        id: user.id,
        allow: [
          PermissionsBitField.Flags.ViewChannel,
          PermissionsBitField.Flags.SendMessages,
          PermissionsBitField.Flags.ReadMessageHistory,
          PermissionsBitField.Flags.AttachFiles
        ]
      },
      {
        id: adminRoleId,
        allow: [
          PermissionsBitField.Flags.ViewChannel,
          PermissionsBitField.Flags.SendMessages,
          PermissionsBitField.Flags.ReadMessageHistory,
          PermissionsBitField.Flags.ManageChannels,
          PermissionsBitField.Flags.ManageMessages
        ]
      }
    ]
  });
  
  // Store active ticket
  activeTickets.set(user.id, {
    channelId: channel.id,
    createdAt: Date.now(),
    ticketNumber
  });
  
  return { channel, ticketNumber };
}

async function generateTranscript(channel) {
  const messages = [];
  let lastId;
  
  // Fetch all messages
  while (true) {
    const options = { limit: 100 };
    if (lastId) options.before = lastId;
    
    const batch = await channel.messages.fetch(options);
    if (batch.size === 0) break;
    
    messages.push(...batch.values());
    lastId = batch.last().id;
    
    if (batch.size < 100) break;
  }
  
  // Sort messages chronologically
  messages.reverse();
  
  // Generate transcript text
  let transcript = `Ticket Transcript: ${channel.name}\n`;
  transcript += `Created: ${new Date(channel.createdTimestamp).toLocaleString()}\n`;
  transcript += `Closed: ${new Date().toLocaleString()}\n`;
  transcript += `Total Messages: ${messages.length}\n`;
  transcript += `\n${'='.repeat(80)}\n\n`;
  
  for (const msg of messages) {
    const timestamp = new Date(msg.createdTimestamp).toLocaleString();
    const author = `${msg.author.tag} (${msg.author.id})`;
    
    transcript += `[${timestamp}] ${author}\n`;
    
    if (msg.content) {
      transcript += `${msg.content}\n`;
    }
    
    if (msg.embeds.length > 0) {
      transcript += `[Embeds: ${msg.embeds.length}]\n`;
      msg.embeds.forEach((embed, i) => {
        if (embed.title) transcript += `  Embed ${i + 1} Title: ${embed.title}\n`;
        if (embed.description) transcript += `  Description: ${embed.description}\n`;
      });
    }
    
    if (msg.attachments.size > 0) {
      transcript += `[Attachments: ${msg.attachments.size}]\n`;
      msg.attachments.forEach(att => {
        transcript += `  - ${att.name} (${att.url})\n`;
      });
    }
    
    transcript += '\n';
  }
  
  return transcript;
}

async function saveTranscript(channel, transcript) {
  const filename = `${channel.name}-${Date.now()}.txt`;
  const filepath = path.join(TICKETS_DIR, filename);
  
  fs.writeFileSync(filepath, transcript, 'utf-8');
  
  return { filename, filepath };
}

function hasActiveTicket(userId) {
  return activeTickets.has(userId);
}

function getActiveTicket(userId) {
  return activeTickets.get(userId);
}

function closeTicket(userId) {
  activeTickets.delete(userId);
}

function getTicketByChannel(channelId) {
  for (const [userId, ticket] of activeTickets.entries()) {
    if (ticket.channelId === channelId) {
      return { userId, ...ticket };
    }
  }
  return null;
}

module.exports = {
  SUPPORT_CHANNEL_ID,
  createTicketChannel,
  generateTranscript,
  saveTranscript,
  hasActiveTicket,
  getActiveTicket,
  closeTicket,
  getTicketByChannel
};
