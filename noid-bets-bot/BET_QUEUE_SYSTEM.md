# Bet Queue System

## Overview
Users can now place bets even when betting is closed. These bets are automatically queued and placed when the next round opens.

## Features

### 1. **Automatic Queuing**
- When users try to bet while betting is closed, their bet is queued instead of rejected
- Queue stores: amount, side, and user info
- Only ONE queued bet per user (new bets replace old ones)

### 2. **Automatic Placement**
- When a new round opens, all queued bets are automatically processed
- Balance is checked before placing each bet
- Users receive DM confirmation when their bet is placed
- If insufficient balance, user is notified and bet is cancelled

### 3. **User Commands**

#### `/bet <amount> <side>`
- Place a bet normally if betting is open
- Queue a bet if betting is closed
- Replaces any existing queued bet

#### `/queued-bet`
- Check if you have a bet queued
- Shows amount, side, and balance status
- Warns if insufficient balance

#### `/cancel-queued-bet`
- Cancel your queued bet
- Can be done anytime before next round opens

## User Experience

### When Betting is Closed:
```
User: /bet 1m red
Bot: ⏳ Bet Queued for Next Round
     Betting is currently closed, so your bet has been queued.
     
     Amount: 1,000,000 GP
     Side: RED
     
     ✅ Your bet will automatically be placed when the next round opens!
     Balance: 5,000,000 GP • Use /cancel-queued-bet to cancel
```

### When Round Opens:
```
Bot DM: ✅ Queued Bet Placed!
        Your queued bet has been placed in the new round!
        
        Amount: 1,000,000 GP
        Side: RED
        Round: 12345
        
        New Balance: 4,000,000 GP
```

### If Insufficient Balance:
```
Bot DM: ❌ Your queued bet of 1,000,000 GP could not be placed - insufficient balance.
        Current balance: 500,000 GP
```

## Admin/Server Logs

### Queue Processing:
```
[Queue] Processing 3 queued bet(s)...
[Queue] ✓ Placed queued bet: Username#1234 - 1,000,000 GP on red
[Queue] ✓ Placed queued bet: Player#5678 - 500,000 GP on blue
[Queue] ✗ Insufficient balance for Broke#9999 - bet removed from queue
[Queue] Queue cleared
```

### User Actions:
```
[Queue] Username#1234 queued 1,000,000 GP on red
[Queue] Username#1234 queued 2,000,000 GP on blue (replaced existing)
[Queue] Username#1234 cancelled queued bet: 2,000,000 GP on blue
```

## Technical Details

### Storage
- In-memory Map: `queuedBets` (userId -> bet details)
- Cleared after each round opens
- Not persisted to database (resets on bot restart)

### Processing Flow
1. New round opens via `/start` or VitaLite plugin
2. Queue is checked for bets
3. Each bet is validated:
   - User exists
   - Sufficient balance
4. Valid bets are placed automatically
5. Users are notified via DM
6. Queue is cleared

### Balance Management
- Balance NOT deducted when bet is queued
- Balance IS deducted when bet is placed (round opens)
- Prevents issues with withdrawals while bet is queued

## Benefits
- ✅ No more "Bets are closed" frustration
- ✅ Users can queue bets during fight
- ✅ Automatic placement = no manual action needed
- ✅ Balance validation prevents errors
- ✅ DM notifications keep users informed
- ✅ Easy to check/cancel queued bets
