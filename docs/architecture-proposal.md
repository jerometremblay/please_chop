# Please Chop Architecture

This document describes the current architecture of `Please Chop`.

It is no longer a proposal. The early scaffold and several classes originally planned here were never kept as-is. The actual implementation is intentionally narrower:
- one workstation block entity owns the runtime
- one tree detector builds explicit candidates
- one world-scoped reservation map prevents double-claims
- normal villager professions and trading stay in place

## Goals

- Keep lumberjacks inside the normal villager profession system.
- Let the workstation own the forestry runtime instead of building a generic task framework.
- Prefer explicit state machines and bounded heuristics over deep abstractions.
- Stay close to vanilla patterns where possible:
  - job site blocks
  - villager levels
  - villager trades
  - villager restocking

## Non-Goals

- No generalized AI behavior tree.
- No chest logistics or output network.
- No attempt to parse every possible modded tree shape.
- No live trade mutation every time a workstation block is swapped under an existing villager.

## Package Layout

```text
com.jerome.pleasechop
  PleaseChopMod
  block/
    LumberjackWorkstationBlock
    WorkstationWoodType
  block/entity/
    LumberjackWorkstationBlockEntity
  client/
    LumberjackWorkstationRenderer
    PleaseChopClientEvents
    PleaseChopConfigScreen
  config/
    PleaseChopConfig
  registry/
    ModBlockEntities
    ModBlocks
    ModPoiTypes
    ModVillagerProfessions
  trade/
    LumberjackTradeManager
  tree/
    TreeCandidateDetector
  world/
    TreeReservationData
```

This is deliberately flatter than the original proposal. The codebase ended up proving that a few concrete runtime owners were enough.

## Bootstrap and Registration

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/PleaseChopMod.java`

Responsibilities:
- register common config
- register the NeoForge config screen entry point
- register blocks, block entities, POIs, villager professions, and trades
- add workstation items to the functional creative tab

The mod id is `pleasechop`.

## Workstation Blocks

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/block/LumberjackWorkstationBlock.java`

The workstation block is:
- the placed block
- the villager job site block
- the block entity host

This is intentionally vanilla-like. There is no separate invisible controller block.

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/block/WorkstationWoodType.java`

This maps workstation variants to their wood-family behavior:
- item appearance
- trade specialization
- specialization-specific resources

It does **not** decide what trees a lumberjack may chop. Workstation wood type is for villager identity and trades, not harvesting permission.

## Profession and Requested Items

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/registry/ModPoiTypes.java`

Registers the workstation POI from the workstation block states.

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/registry/ModVillagerProfessions.java`

Registers the `LUMBERJACK` profession.

The profession also declares a broad `requestedItems` set so villagers are naturally interested in tree outputs:
- overworld logs
- stems
- leaves
- saplings and propagules
- sticks
- apples

This matters because the workstation runtime still directs work, but it should not fight vanilla inventory preferences more than necessary.

## Trade Specialization

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/trade/LumberjackTradeManager.java`

Trades stay level-based, like normal villagers. The custom part is only the wood specialization.

Current trade shape:

- Level 1
  - sells matching logs
  - sells matching saplings
  - oak specialization also sells apples
  - buys stone axes
  - buys bread
- Level 2
  - sells stripped logs
  - sells leaves
  - buys carrots
  - buys baked potatoes
- Level 3
  - sells sticks
  - sells specialization-specific extras where they make sense
  - buys iron axes
  - buys pumpkin pie
- Level 4
  - small chance of selling an enchanted iron axe
- Level 5
  - very small chance of selling an enchanted diamond axe

Wood specialization is stored on the villager in persistent data, but it is derived from the villager's current valid job site:
- losing the station clears specialization
- taking a valid lumberjack workstation sets specialization from that block

This is deliberately simpler than trying to hot-swap an active villager's offers live. Vanilla already tolerates profession identity being set by the workstation claim.

## Workstation Runtime

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/block/entity/LumberjackWorkstationBlockEntity.java`

This is the core runtime owner.

The block entity is responsible for:
- finding or validating the assigned lumberjack
- scanning trees
- starting manual or autonomous tree jobs
- ticking active chopping jobs
- tracking remembered drop sites
- tracking queued replant sites
- dispatching planting recovery
- dispatching scavenging
- maintaining debug state for the renderer
- handling villager maintenance such as inventory cleanup and restocking

This is the biggest custom system in the mod, but it is still just one explicit orchestrator instead of a distributed framework.

### Main Tick Order

At a high level the server tick does this:

1. age remembered sites and timers
2. run worker maintenance
3. if there is an active tree job, tick it
4. otherwise, in priority order:
   - autonomous chopping
   - planting recovery
   - scavenging

That ordering is intentional. Cutting trees is the main job. Recovery work only fills the gaps.

### Active Tree Job

Current tree job phases:
- `MOVING_TO_TREE`
- `CHOPPING`
- `LINGERING_UNDER_TREE`
- `PLANTING_SAPLING`
- `RETURNING_TO_WORKSTATION`

Important behavior:
- once chopping finishes, the replant site is queued immediately
- if lingering manages to plant right away, that queued site is removed
- the villager can collect nearby tree drops while lingering
- higher-level villagers chop faster and linger less
- non-critical return legs may be interrupted without losing the important work state

### Planting Recovery

Current planting recovery phases:
- `MOVING_TO_SITE`
- `PLANTING`
- `RETURNING_AFTER_PLANTING`
- `RETURNING_TO_WORKSTATION`

Design intent:
- chopping and replant recovery are separate jobs
- queued sites stay pending until they are actually planted
- the workstation scans the queue from most recent to oldest and picks the first site the villager can currently satisfy
- if planting succeeds, the site is removed
- if saplings are missing, the site remains queued

This is closer to a workstation-owned maintenance queue than to a single giant mixed “cut and fully clean up right now” job.

### Scavenging

Current scavenging phases:
- `MOVING_TO_SITE`
- `COLLECTING`
- `LINGERING_AT_SITE`
- `RETURNING_TO_WORKSTATION`

Scavenging is deliberately constrained:
- only remembered sites are eligible
- a site is only eligible if it has visible collectible drops now
- sites have visit cooldowns
- sites retire after a small number of completed visits

That keeps scavenging from taking over the whole workday.

### Worker Progression

Villager level affects runtime directly.

Current chop interval per log:
- level 1: `40` ticks
- level 2: `32`
- level 3: `24`
- level 4: `18`
- level 5: `12`

Current movement speed:
- level 1: `0.40`
- level 2: `0.425`
- level 3: `0.45`
- level 4: `0.475`
- level 5: `0.50`

Current daily tree limit:
- level 1: `1`
- each level adds `+1`

This matches the intended progression:
- novice villagers are slow and conservative
- advanced villagers cut more and spend less time idling under trees

### Maintenance

The workstation also handles villager maintenance:
- daily tree counters are reset during sleep cleanup
- collected tree output is cleared during sleep cleanup
- queued replant saplings are preserved instead of being deleted blindly
- normal villager restocking is triggered through the vanilla restock path rather than a custom trade-reset system

## Tree Detection

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/tree/TreeCandidateDetector.java`

This is the second major system.

The detector does **not** try to understand every connected log as a tree. It is intentionally conservative.

### Candidate Model

`CandidateTree` currently stores:
- `rootPos`
- `rootPositions`
- `trunkPositions`
- `logPositions`
- `leafPositions`
- `saplingItemId`

`rootPositions` are especially important:
- single-trunk trees use one grounded root
- wide trees use a grounded `2x2` root footprint

### Detection Strategy

The detector starts from grounded vertical base logs.

Supported families:
- normal single-trunk trees
- large spruce
- large jungle
- dark oak

Important rules:
- tree roots must be on valid sapling-growing ground
- extra ground contact outside the intended root invalidates the candidate
- foreign connected logs invalidate the candidate
- strict neighbor checks reject structural attachments
- replaceable clutter such as grass and flowers is ignored
- bee nests and beehives are allowed as tree-adjacent structure

### Leaf Bridges

Leaf bridges are only used narrowly:
- they can recover stray disconnected same-species branch logs
- they must not merge separate grounded trees into one candidate

If a leaf-bridged component has its own ground contact, the tree is rejected instead of merged.

That rule is there specifically to avoid chopping two nearby oaks as one tree just because their canopies overlap.

### Wide Trees

Wide-tree behavior differs by species:
- spruce: supports both `1x1` and `2x2`
- jungle: supports both `1x1` and `2x2`
- dark oak: wide-tree only

Wide spruce and jungle may fall back to the single-tree path when the only wide-tree failure is “invalid `2x2` base”.

### Debug Output

The detector can also produce rejection reasons:
- invalid root/base
- invalid touching block
- extra ground contact
- foreign wood attachment
- invalid wide-tree validation

Those reasons are used by workstation debug chat when enabled.

## World-Scoped Tree Reservation

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/world/TreeReservationData.java`

This is the anti-double-claim system.

Without it, two workstations can select the same tree at the same time and both waste time walking, chopping, lingering, and queueing replanting for the same stump.

Current design:
- world-scoped `SavedData`
- key = sorted root footprint of the tree
- value = reserving workstation position plus expiry tick

Operations:
- `tryReserve`
- `refresh`
- `release`
- `isReservedByOther`

This is intentionally a small world-local map, not a global singleton service.

## Client Rendering

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/client/LumberjackWorkstationRenderer.java`

The renderer has two jobs:
- render a custom axe-on-stump workstation visual
- render debug overlays when enabled

Debug visuals include:
- detected roots
- detected logs
- pending replant sites
- active work destination marker

The renderer deliberately expands its culling logic so debug shapes still render when the tree is in view even if the workstation block itself is not centered on screen.

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/client/PleaseChopClientEvents.java`

Client-only registration and event hooks live here.

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/client/PleaseChopConfigScreen.java`

This provides the NeoForge config UI hook for the mod.

## Config

### `/Users/jerome/PleaseChop/src/main/java/com/jerome/pleasechop/config/PleaseChopConfig.java`

The current config surface is intentionally small:
- `debug.chat`
- `debug.render`

Both are disabled by default.

This is preferable to inventing a large settings surface before the core behavior is stable.

## Assets

Workstation item icons now use texture-based item models under:
- `/Users/jerome/PleaseChop/src/main/resources/assets/pleasechop/models/item/`
- `/Users/jerome/PleaseChop/src/main/resources/assets/pleasechop/textures/item/`

The shared hand-drawn source is:
- `/Users/jerome/PleaseChop/src/main/resources/assets/pleasechop/textures/item/lumberjack_workstation.png`

Derived per-wood item textures exist for the workstation variants.

## Why This Shape

The current architecture is more custom than vanilla villager AI, but less custom than the original proposal would have become.

The important design choices are:
- keep the villager profession and trading system vanilla
- keep one explicit workstation-owned runtime
- isolate tree parsing and reservation as separate concerns
- avoid a generic task framework

That is the narrowest system that still solves the actual gameplay problems:
- tree parsing
- multi-workstation conflicts
- queued replant recovery
- predictable villager progression

## Current Caveats

- Tree parsing is intentionally conservative and will still reject ambiguous shapes.
- Scavenging and planting recovery are workstation-owned behaviors, not native villager brain tasks.
- Workstation item art is functional, but still an asset-polish area.
- Debug output exists for tuning and should stay optional.
