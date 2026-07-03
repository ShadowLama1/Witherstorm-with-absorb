# Witherstorm

A Paper 26.1.1 (Minecraft 1.21.11) server plugin that adds Minecraft Story
Mode's **Witherstorm** as a multi-phase boss fight with custom mechanics,
drops, and lore.

## Requirements

- Paper 26.1.1+ (Minecraft 1.21.11)
- Java 25+

## Building

```bash
./gradlew jar
```

The compiled plugin JAR is written to `build/libs/Witherstorm-1.0.0.jar`.
Drop it into your server's `plugins/` folder.

## Usage

### Summoning

There are two ways to summon the Witherstorm:

1. **Command:** `/witherstorm summon` (or `/ws summon`) — summons the storm at
   your location. Requires the `witherstorm.admin` permission (default: op).

2. **Item:** Ops are given a **Witherstorm Heart** (a custom Nether Star) on
   first join. Right-click **Soul Sand** with the heart to summon the storm.
   The heart is consumed on use (unless in Creative).

### Commands

| Command | Description |
|---------|-------------|
| `/witherstorm summon` | Summon the Witherstorm at your location. |
| `/witherstorm despawn` | Remove an active Witherstorm. |
| `/witherstorm reload` | Reload `config.yml` values. |
| `/witherstorm help` | Show command help. |

Aliases: `/ws`, `/storm`.

## The Fight

The Witherstorm has **three phases**, each more dangerous than the last:

### Phase 1 — Awakening (100% → 66% HP)
- The storm is **armored**: incoming damage is halved.
- **Tractor Beam** pulls nearby players toward it and levitates them.
- **Tentacle Slam** deals AoE damage and knockback around the storm.

### Phase 2 — Feeding (66% → 33% HP)
- The storm begins **consuming blocks** in a radius around it, growing
  larger and gaining health for every block eaten (up to a configurable cap).
- **Wither Skeleton minions** are summoned periodically (up to a max count).
- All Phase 1 attacks continue.

### Phase 3 — Command Core Exposed (33% → 0% HP)
- **Wither Skull Barrage**: rapid, spread projectiles aimed at the nearest player.
- Minion summoning and block consumption intensify.
- All previous attacks continue.

### Death

On death, the storm drops:
- Configured loot (Nether Star, Wither Skeleton Skulls, Elytra, Enchanted
  Golden Apples, Dragon Breath, Beacon — all chance-based).
- The **Command Block Fragment** — a mythic lore item with custom model data
  for resource pack integration.
- 1500 experience points (configurable).

## Configuration

All balance values, attack timings, drops, and lore text are configurable in
`plugins/Witherstorm/config.yml`. Edit the file and run `/witherstorm reload`
to apply changes without restarting.

Key settings:
- `boss.max-health` — base health (default 600 / 300 hearts).
- `boss.growth-cap` — max health after block consumption (default 1200).
- `attacks.*` — enable/disable and tune each attack.
- `drops.*` — drop table and the Command Block Fragment item.
- `lore.*` — all broadcast messages and item lore (MiniMessage format).
- `world.allowed-worlds` — restrict summoning to specific worlds (empty = all).
- `world.summon-block` — block required for the heart summon (default SOUL_SAND).

## Lore

> *"It consumes. It grows. It cannot be stopped."*

The Witherstorm Heart pulses with the energy of a forgotten experiment. When
placed upon soul sand, it tears open a rift and the storm awakens — hungry,
unending, and impossible to contain. Only by exposing its command core can
it be destroyed, and even then, a fragment of that power may survive.
