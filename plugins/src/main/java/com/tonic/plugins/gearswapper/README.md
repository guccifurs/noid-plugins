# Gear Swapper Plugin

Production-ready gear swapping plugin for RuneLite with PvP support.

## Features

✅ **5 Loadout Slots** - Configure up to 5 different gear setups
✅ **Hotkey Support** - Instant gear swaps with customizable keybinds
✅ **Item Name to ID** - Use item names instead of IDs (e.g., "Ahrims hood")
✅ **Spell Casting** - Auto-cast spells with magic level fallback
✅ **Auto-Attack** - Automatically attack target after swap (PvP arena support)
✅ **Instant Swaps** - All items equip simultaneously
✅ **Smart Caching** - Fast item lookups with intelligent caching

## Configuration

### Loadout Setup

Each of the 5 loadouts has:

1. **Keybind** - Hotkey to activate the loadout
2. **Name** - Display name for the loadout
3. **Items** - Comma-separated item names to equip
4. **Spell** - Spell to cast with fallback options
5. **Attack Target** - Auto-attack current target

### Item Names

Use full item names separated by commas. **Wildcard support** allows matching degraded items.

**Basic Examples:**
```
Ahrims hood, Ahrims robetop, Ahrims robeskirt, Occult necklace, Barrows gloves
```

```
Dharoks helm, Dharoks platebody, Dharoks platelegs, Amulet of fury
```

```
Black dhide body, Black dhide chaps, Anguish, Archers ring
```

**Wildcard Examples:**
```
Karil's leathertop*
```
Matches: `Karil's leathertop 100`, `Karil's leathertop 75`, `Karil's leathertop 25`, etc.

```
Abyssal tentacle*
```
Matches: `Abyssal tentacle`, `Abyssal tentacle (uncharged)`, etc.

```
Dharoks platebody*
```
Matches: `Dharoks platebody`, `Dharoks platebody 100`, `Dharoks platebody 50`, etc.

### Spell Configuration

Format: `Cast:PrimarySpell:Fallback1:Fallback2`

The plugin checks your Magic level and uses the highest spell you can cast.

**Examples:**

Ice spells with fallback:
```
Cast:Ice barrage:Ice blitz:Ice burst:Ice rush
```

Blood spells:
```
Cast:Blood barrage:Blood blitz:Blood burst
```

Standard spellbook:
```
Cast:Fire surge:Fire wave:Fire blast
```

**Supported Spells:**

**Ancient Magicks:**
- Ice: barrage (94), blitz (82), burst (70), rush (58)
- Blood: barrage (92), blitz (80), burst (68), rush (56)
- Shadow: barrage (88), blitz (76), burst (64), rush (52)
- Smoke: barrage (86), blitz (74), burst (62), rush (50)

**Standard Spellbook:**
- Fire surge (95), Fire wave (75), Fire blast (59), Fire bolt (35), Fire strike (13)

### Attack Target

Enable "Attack Target" to automatically attack your current target after swapping.

This uses the "Fight" option (index 1) which is perfect for PvP arena.

## Usage Examples

### Example 1: Mage Tank Setup
```
Name: Mage Tank
Items: Ahrims hood, Ahrims robetop, Ahrims robeskirt, Occult necklace, Barrows gloves, Eternal boots
Spell: Cast:Ice barrage:Ice blitz:Ice burst
Attack Target: ✓ Enabled
```

### Example 2: Melee Setup
```
Name: Melee DPS
Items: Neitiznot faceguard, Bandos chestplate, Bandos tassets, Amulet of torture, Ferocious gloves, Primordial boots
Spell: (empty)
Attack Target: ✓ Enabled
```

### Example 3: Range Setup
```
Name: Range
Items: Arma helmet, Arma chestplate, Arma chainskirt, Anguish, Barrows gloves, Pegasian boots
Spell: (empty)
Attack Target: ✓ Enabled
```

### Example 4: Venge Setup
```
Name: Venge
Items: (gear you want)
Spell: Cast:Vengeance
Attack Target: ✗ Disabled
```

### Example 5: F-Key Hybrid
```
Loadout 1 (F1): Mage gear + Cast:Ice barrage:Ice blitz
Loadout 2 (F2): Melee gear
Loadout 3 (F3): Range gear
Loadout 4 (F4): Tank gear
Loadout 5 (F5): Special attack gear
```

## How It Works

1. **Press Hotkey** → Plugin activates
2. **Find Items** → Searches inventory for configured items
3. **Equip All** → Instantly equips all items (no delays)
4. **Cast Spell** → Selects best spell based on magic level
5. **Attack** → Attacks current target if enabled

## Performance

- **Item Lookup:** Cached after first use (instant subsequent lookups)
- **Spell Lookup:** Pre-cached on plugin start
- **Swap Speed:** All items equip in same game tick
- **Memory:** Minimal overhead with smart caching

## Troubleshooting

**Items not equipping:**
- Check item names are exact (case-insensitive)
- Verify items are in your inventory
- Check spelling

**Spell not casting:**
- Verify you have the required magic level
- Make sure you're on the correct spellbook
- Check spell name spelling

**Attack not working:**
- Make sure you have a target selected
- Verify "Attack Target" is enabled
- Target must be attackable

## Technical Details

**Item Name Resolution:**
- Scans RuneLite item definitions
- Case-insensitive matching
- Caches results for performance

**Spell Fallback Logic:**
- Checks magic level in real-time
- Selects highest available spell
- Falls back to lower levels automatically

**Attack Mechanism:**
- Uses NPC_SECOND_OPTION for NPCs
- Uses PLAYER_SECOND_OPTION for players
- Perfect for PvP arena (Fight option)

## Compatibility

✅ Works with all RuneLite versions
✅ Compatible with other plugins
✅ No conflicts with vanilla client features
✅ Safe for all game modes

## Credits

Built for VitaLite custom client with production-ready code and full PvP support.
