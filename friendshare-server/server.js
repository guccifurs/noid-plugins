/**
 * FriendShare WebSocket Server
 * Handles connections, friend detection, and screen frame relay
 */

const WebSocket = require('ws');

const PORT = process.env.PORT || 8765;

// Store connected users: rsn -> { ws, friends: [], sharing: { with: rsn, role: 'sender'|'receiver' } }
const users = new Map();

// Create WebSocket server
const wss = new WebSocket.Server({ port: PORT });

console.log(`[FriendShare] Server starting on port ${PORT}...`);

wss.on('connection', (ws) => {
    let userRsn = null;

    ws.on('message', (data) => {
        try {
            const msg = JSON.parse(data);
            handleMessage(ws, msg, userRsn, (rsn) => { userRsn = rsn; });
        } catch (e) {
            console.error('[FriendShare] Invalid message:', e.message);
        }
    });

    ws.on('close', () => {
        if (userRsn) {
            console.log(`[FriendShare] ${userRsn} disconnected`);

            // End any active share session
            const user = users.get(userRsn);
            if (user && user.sharing) {
                endShareSession(userRsn, user.sharing.with);
            }

            users.delete(userRsn);
            broadcastOnlineStatus();
        }
    });

    ws.on('error', (err) => {
        console.error('[FriendShare] WebSocket error:', err.message);
    });
});

function handleMessage(ws, msg, currentRsn, setRsn) {
    const { type, ...payload } = msg;

    switch (type) {
        case 'auth':
            handleAuth(ws, payload, setRsn);
            break;
        case 'update_friends':
            handleUpdateFriends(currentRsn, payload);
            break;
        case 'share_request':
            handleShareRequest(currentRsn, payload);
            break;
        case 'share_accept':
            handleShareAccept(currentRsn, payload);
            break;
        case 'share_decline':
            handleShareDecline(currentRsn, payload);
            break;
        case 'frame':
            handleFrame(currentRsn, payload);
            break;
        case 'share_end':
            handleShareEnd(currentRsn);
            break;
        default:
            console.log(`[FriendShare] Unknown message type: ${type}`);
    }
}

function handleAuth(ws, { rsn, friends }, setRsn) {
    if (!rsn) {
        ws.send(JSON.stringify({ type: 'error', message: 'RSN required' }));
        return;
    }

    // Normalize RSN (lowercase for comparison)
    const normalizedRsn = rsn.toLowerCase();

    console.log(`[FriendShare] ${rsn} connected with ${friends?.length || 0} friends`);

    users.set(normalizedRsn, {
        ws,
        rsn: rsn, // Keep original case for display
        friends: (friends || []).map(f => f.toLowerCase()),
        sharing: null
    });

    setRsn(normalizedRsn);

    // Send confirmation
    ws.send(JSON.stringify({ type: 'auth_success' }));

    // Broadcast updated online friends to everyone
    broadcastOnlineStatus();
}

function handleUpdateFriends(rsn, { friends }) {
    const user = users.get(rsn);
    if (user) {
        user.friends = (friends || []).map(f => f.toLowerCase());
        broadcastOnlineStatus();
    }
}

function handleShareRequest(fromRsn, { target }) {
    if (!fromRsn || !target) return;

    const targetRsn = target.toLowerCase();
    const targetUser = users.get(targetRsn);
    const fromUser = users.get(fromRsn);

    if (!targetUser) {
        fromUser?.ws.send(JSON.stringify({
            type: 'share_error',
            message: 'User not online'
        }));
        return;
    }

    // Check if they are mutual friends
    if (!areMutualFriends(fromRsn, targetRsn)) {
        fromUser?.ws.send(JSON.stringify({
            type: 'share_error',
            message: 'Must be mutual friends'
        }));
        return;
    }

    console.log(`[FriendShare] ${fromRsn} requesting share with ${targetRsn}`);

    // Send request to target
    targetUser.ws.send(JSON.stringify({
        type: 'share_incoming',
        from: fromUser.rsn // Send original case
    }));
}

function handleShareAccept(accepterRsn, { from }) {
    const fromRsn = from.toLowerCase();
    const fromUser = users.get(fromRsn);
    const accepterUser = users.get(accepterRsn);

    if (!fromUser || !accepterUser) return;

    console.log(`[FriendShare] ${accepterRsn} accepted share from ${fromRsn}`);

    // Set up sharing session - fromUser is the sender (sharer), accepter is viewer
    fromUser.sharing = { with: accepterRsn, role: 'sender' };
    accepterUser.sharing = { with: fromRsn, role: 'receiver' };

    // Notify both
    fromUser.ws.send(JSON.stringify({
        type: 'share_start',
        peer: accepterUser.rsn,
        role: 'sender'
    }));
    accepterUser.ws.send(JSON.stringify({
        type: 'share_start',
        peer: fromUser.rsn,
        role: 'receiver'
    }));
}

function handleShareDecline(declinerRsn, { from }) {
    const fromRsn = from.toLowerCase();
    const fromUser = users.get(fromRsn);

    if (fromUser) {
        fromUser.ws.send(JSON.stringify({
            type: 'share_declined',
            by: declinerRsn
        }));
    }

    console.log(`[FriendShare] ${declinerRsn} declined share from ${fromRsn}`);
}

function handleFrame(senderRsn, { data }) {
    const sender = users.get(senderRsn);
    if (!sender || !sender.sharing || sender.sharing.role !== 'sender') return;

    const receiver = users.get(sender.sharing.with);
    if (!receiver) {
        endShareSession(senderRsn, sender.sharing.with);
        return;
    }

    // Relay frame to receiver
    receiver.ws.send(JSON.stringify({
        type: 'frame',
        data: data
    }));
}

function handleShareEnd(rsn) {
    const user = users.get(rsn);
    if (user && user.sharing) {
        endShareSession(rsn, user.sharing.with);
    }
}

function endShareSession(rsn1, rsn2) {
    console.log(`[FriendShare] Ending share session between ${rsn1} and ${rsn2}`);

    const user1 = users.get(rsn1);
    const user2 = users.get(rsn2);

    if (user1) {
        user1.sharing = null;
        user1.ws.send(JSON.stringify({ type: 'share_ended' }));
    }
    if (user2) {
        user2.sharing = null;
        user2.ws.send(JSON.stringify({ type: 'share_ended' }));
    }
}

function areMutualFriends(rsn1, rsn2) {
    const user1 = users.get(rsn1);
    const user2 = users.get(rsn2);

    if (!user1 || !user2) return false;

    return user1.friends.includes(rsn2) && user2.friends.includes(rsn1);
}

function broadcastOnlineStatus() {
    for (const [rsn, user] of users) {
        // Find friends who are also online with GearSwapper
        const onlineFriends = [];

        for (const friendRsn of user.friends) {
            const friendUser = users.get(friendRsn);
            if (friendUser && friendUser.friends.includes(rsn)) {
                // Mutual friend is online
                onlineFriends.push(friendUser.rsn); // Original case
            }
        }

        user.ws.send(JSON.stringify({
            type: 'online_friends',
            friends: onlineFriends
        }));
    }
}

console.log(`[FriendShare] Server ready on ws://0.0.0.0:${PORT}`);
