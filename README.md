# Please Chop

`Please Chop` is a NeoForge mod for Minecraft `1.21.x` that adds a lumberjack workstation and a `LUMBERJACK` villager profession.

The mod stays close to vanilla where it can:
- workstation blocks are normal villager job sites
- villagers keep normal profession levels and trading
- the workstation block entity owns the custom forestry runtime instead of replacing the villager AI with a generic framework

![CurseForge thumbnail](docs/media/curseforge-thumbnail.png)

## Features

- One lumberjack workstation variant per overworld wood family:
  - oak
  - spruce
  - birch
  - jungle
  - acacia
  - dark oak
  - mangrove
  - cherry
- A `LUMBERJACK` villager profession tied to those workstations.
- Manual and autonomous tree chopping.
- Tree detection for:
  - normal single-trunk trees
  - large spruce
  - large jungle
  - dark oak
- Replant tracking:
  - trees are queued for replanting as soon as chopping finishes
  - successful immediate planting during lingering removes the queued site
  - queued sites are retried later when the villager has the right saplings
- Site scavenging for leftover tree drops.
- Workstation-specific trade specialization:
  - the villager's trade wood type follows the workstation it is currently assigned to
  - chopping and replanting still work for any supported tree type
- Debug chat and debug world overlays, disabled by default.

## Current Behavior

The workstation block entity is the main runtime owner.

At a high level:
- it finds or keeps one assigned lumberjack
- scans for eligible nearby trees
- reserves a tree so another workstation cannot claim it at the same time
- sends the villager to chop it
- lingers briefly for drops and possible immediate replanting
- remembers leftover drop sites and queued replant sites for later recovery

The villager stays a normal villager:
- trade level controls chop speed and daily tree quota
- normal villager restocking still applies
- job-site specialization only changes what the villager sells

## Trades

Lumberjack trades are specialization-aware and level-based.

Examples:
- level 1:
  - sells matching logs
  - sells matching saplings
  - oak lumberjacks also sell apples
  - buys stone axes
  - buys bread
- level 2:
  - sells stripped logs
  - sells leaves
  - buys carrots and baked potatoes
- level 3:
  - sells sticks
  - sells species extras where applicable
  - buys iron axes
- level 4-5:
  - very low chance of selling enchanted axes

The specialization is reset from the villager's current workstation, not from the last item it chopped.

## Config

The mod uses NeoForge's built-in config screen.

Current common config options:
- `debug.chat = false`
- `debug.render = false`

Open `Mods -> Please Chop -> Config` to change them.

## Debug

When enabled:
- debug chat reports workstation transitions and tree rejections
- debug world rendering shows:
  - detected roots
  - detected logs
  - pending replant sites
  - active work destination markers

## Requirements

- Minecraft `1.21.x` (developed against `1.21.1`)
- NeoForge `21.1.x` (developed against `21.1.221`)

## Development

```bash
./gradlew runClient
./gradlew build
```

## Notes

- Tree detection is intentionally conservative. The mod prefers rejecting ambiguous shapes over harvesting player builds.
- Reservations are world-scoped, so two workstations should not claim the same tree at once.
- Replanting is queue-based. If the villager does not have the right sapling yet, the site stays queued.
- The current workstation item icons use texture assets in `assets/pleasechop/textures/item/`. The generated per-wood icons are serviceable, but still a presentation area rather than a gameplay system.

## Architecture

See [docs/architecture-proposal.md](docs/architecture-proposal.md) for the current architecture overview.
