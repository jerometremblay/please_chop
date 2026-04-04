# Please Chop Architecture Proposal

## Goals

- Add one workstation block that can recruit and manage one lumberjack villager through the normal villager profession system.
- Keep the implementation close to the existing `PleaseStore` pattern: one main block, one main block entity, explicit state, no generalized task framework.
- Prefer deterministic tree harvesting over clever AI. The workstation should own the job state, and the villager should mostly execute movement plus a few scripted actions.

## Vanilla Baseline

Before adding custom behavior, the baseline Minecraft-friendly design is:

- A workstation block marks a villager profession.
- A villager with that profession gets a workstation memory and normal village behavior.
- Extra work logic runs from the workstation block entity when a lumberjack is assigned.

That is the direction this proposal follows. There is no separate controller block. The placed block is the workstation, the POI, and the runtime owner.

## Recommended Package Layout

```text
com.jerome.pleasechop
  PleaseChopMod
  block/
    LumberjackWorkstationBlock
  block/entity/
    LumberjackWorkstationBlockEntity
  client/
    PleaseChopClientEvents
    LumberjackWorkstationRenderer
  config/
    PleaseChopConfig
  data/
    LumberjackTreeScan
    LumberjackTreePlan
    LumberjackTripRuntime
    TreeBlockColumn
  entity/ai/
    LumberjackNavigationHelper
    LumberjackHarvestHelper
    LumberjackInventoryHelper
  profession/
    ModPoiTypes
    ModVillagerProfessions
  registry/
    ModBlockEntities
    ModBlocks
```

## Core Block

### `PleaseChopMod`

Responsibilities:

- Register blocks, items, block entities, POIs, villager professions, and config.
- Add the workstation item to the creative tab.

Methods:

- `public PleaseChopMod(IEventBus modEventBus, ModContainer modContainer)`
- `private void addCreativeTabEntries(BuildCreativeModeTabContentsEvent event)`

### `LumberjackWorkstationBlock`

Reasoning:

- Keep it structurally parallel to the existing block-with-block-entity pattern in your mods.
- This block is not a controller. It is the actual villager workstation and POI.
- I do not recommend redstone as the primary trigger. That would be a custom pattern fighting vanilla instead of using it.

State:

- `DirectionProperty FACING`

Methods:

- `public LumberjackWorkstationBlock(BlockBehaviour.Properties properties)`
- `protected MapCodec<? extends BaseEntityBlock> codec()`
- `protected RenderShape getRenderShape(BlockState state)`
- `protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)`
- `protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType)`
- `public BlockState getStateForPlacement(BlockPlaceContext context)`
- `protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)`
- `public BlockEntity newBlockEntity(BlockPos pos, BlockState state)`
- `public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)`
- `protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)`

## Profession and POI

### `ModPoiTypes`

Responsibilities:

- Register one POI matching the workstation block states.

Methods:

- `public static void register(IEventBus modEventBus)`
- `private static Set<BlockState> collectStates(Block block)`

### `ModVillagerProfessions`

Responsibilities:

- Register `LUMBERJACK`.
- Bind profession to workstation POI.

Methods:

- `public static void register(IEventBus modEventBus)`

This is the most vanilla part of the design. I would not replace profession registration with a custom “fake profession” system.

Expected profession behavior:

- An unemployed villager can claim the workstation and become a lumberjack.
- The workstation block entity should prefer its owning lumberjack if one exists.
- If that villager dies or loses the workstation, the block entity can recruit a new one through the same vanilla profession loop.
- The mod does not manage chests or downstream logistics. Players can combine it with other mods if they want output capture.

## Main Runtime Owner

### `LumberjackWorkstationBlockEntity`

Reasoning:

- This should be the main orchestrator, just like `PleaseStoreControllerBlockEntity`.
- Do not build a generic behavior tree or job engine. One explicit state machine is enough.
- The workstation should own at most one active lumberjack. Do not make this multi-worker in V1.

Persistent fields:

- `@Nullable UUID assignedVillagerUuid`
- `@Nullable BlockPos forestAnchorPos`
- `@Nullable BlockPos activeTreeRootPos`
- `boolean running`
- `LumberjackTripPhase tripPhase`

Runtime-only fields:

- `@Nullable LumberjackTreePlan activeTreePlan`
- `@Nullable LumberjackTripRuntime activeTrip`
- `long phaseDeadlineGameTime`
- `int failedPathAttempts`

Suggested enum:

- `LumberjackTripPhase`
  - `IDLE`
  - `FINDING_WORKER`
  - `SCANNING_TREE`
  - `WALKING_TO_TREE`
  - `CHOPPING_TREE`
  - `COLLECTING_DROPS`
  - `REPLANTING`
  - `RETURNING_TO_WORKSTATION`

Public methods:

- `public LumberjackWorkstationBlockEntity(BlockPos pos, BlockState blockState)`
- `public void onWorkstationClaimed(ServerLevel level, Villager villager)`
- `public void setForestAnchorPos(@Nullable BlockPos forestAnchorPos)`
- `public void tryStartWorkCycle(ServerLevel level)`
- `public void releaseWorker()`
- `public static void tick(Level level, BlockPos pos, BlockState state, LumberjackWorkstationBlockEntity workstation)`

Tick-phase methods:

- `private void tickServer(ServerLevel level)`
- `private boolean ensureAssignedVillager(ServerLevel level)`
- `private boolean isAssignedVillagerStillValid(ServerLevel level)`
- `private void tickSleepCleanup(ServerLevel level, Villager villager)`
- `private void startTreeSearch(ServerLevel level)`
- `private void tickWalkToTree(ServerLevel level, Villager villager)`
- `private void tickChopTree(ServerLevel level, Villager villager)`
- `private void tickCollectDrops(ServerLevel level, Villager villager)`
- `private void tickReplant(ServerLevel level, Villager villager)`
- `private void tickReturnToWorkstation(ServerLevel level, Villager villager)`
- `private void finishRun(ServerLevel level)`
- `private void failRun(ServerLevel level, String reason)`

Assignment methods:

- `private Optional<Villager> findClaimedLumberjack(ServerLevel level)`
- `private Optional<Villager> findNearbyUnemployedVillager(ServerLevel level)`
- `private boolean canAssignVillager(Villager villager)`
- `private void assignVillager(ServerLevel level, Villager villager)`
- `private void releaseVillagerClaim(ServerLevel level)`
- `private void refreshVillagerClaim(ServerLevel level)`

Persistence methods:

- `public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)`
- `protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)`
- `public CompoundTag getUpdateTag(HolderLookup.Provider registries)`
- `public ClientboundBlockEntityDataPacket getUpdatePacket()`
- `public void setRemoved()`

## Tree Detection

### `LumberjackTreeScan`

Reasoning:

- Tree finding is the hardest part to get correct.
- Keep it isolated from the block entity so it can be tested and tuned without dragging navigation logic into it.

Responsibilities:

- Given a forest anchor and radius, find the nearest valid tree candidate.
- Reject player builds and partial trees as much as possible with explicit heuristics.

Methods:

- `public static @Nullable LumberjackTreePlan findNearestTree(ServerLevel level, BlockPos origin, int radius)`
- `public static @Nullable LumberjackTreePlan findNearestTree(ServerLevel level, BlockPos forestAnchor, int radius)`
- `private static boolean isValidLog(BlockState state)`
- `private static boolean isValidLeaf(BlockState state)`
- `private static boolean isHarvestableSapling(BlockState state)`
- `private static @Nullable BlockPos findRootLog(ServerLevel level, BlockPos startPos)`
- `private static Set<BlockPos> collectConnectedLogs(ServerLevel level, BlockPos rootPos)`
- `private static Set<BlockPos> collectConnectedLeaves(ServerLevel level, Set<BlockPos> logs)`
- `private static boolean hasSaplingSupport(ServerLevel level, BlockPos rootPos)`
- `private static boolean looksLikeNaturalTree(Set<BlockPos> logs, Set<BlockPos> leaves, BlockPos rootPos)`

### `LumberjackTreePlan`

Value object returned by the scan.

Fields:

- `BlockPos rootPos`
- `List<BlockPos> logBreakOrder`
- `List<BlockPos> leafCheckPositions`
- `BlockPos replantPos`
- `BlockState replantSaplingState`
- `AABB collectionBounds`

Methods:

- `public BlockPos getRootPos()`
- `public List<BlockPos> getLogBreakOrder()`
- `public BlockPos getNextLogPos(int index)`
- `public boolean hasRemainingLogs(int index)`
- `public BlockPos getReplantPos()`
- `public BlockState getReplantSaplingState()`
- `public AABB getCollectionBounds()`

I strongly prefer a precomputed plan over rescanning every few ticks.

## Inventory Model

This is the part where your new requirement matters most.

The lumberjack keeps all gathered output in their own villager inventory. The mod does not push items into chests, pipes, or adjacent inventories. That is a hard boundary, and it is a good one.

The problem is obvious though: without output extraction, the villager eventually fills up and stops functioning. So V1 needs explicit daily decay.
The problem is obvious though: without output extraction, the villager eventually fills up and stops functioning. So V1 needs explicit cleanup.

### `LumberjackInventoryHelper`

Responsibilities:

- Check villager inventory capacity.
- Apply a simple sleep-time cleanup rule so the lumberjack remains functional even if the player never extracts output.

Methods:

- `public static boolean pickupItemEntity(Villager villager, ItemEntity itemEntity)`
- `public static boolean hasFreeWorkingSpace(Villager villager, int requiredSlots)`
- `public static boolean cleanupOnSleep(Villager villager)`
- `private static boolean isHarvestOutput(ItemStack stack)`

Recommended V1 cleanup rule:

- When the lumberjack enters their normal sleep cycle, clear harvested forestry output from their inventory.
- Apply cleanup only to harvest output categories: logs, sticks, saplings, apples. Do not delete unrelated player-inserted items if you can avoid it.
- Do not preserve saplings specially. The next harvested tree should replenish them naturally.

I would not call this “storage” or “deposit.” It is a maintenance cleanup to keep the profession functional.

I also would not make it too clever. “Sleep clears harvest output” is more legible than a percentage decay system.

## Runtime Trip State

### `LumberjackTripRuntime`

Reasoning:

- Keep per-run mutable counters out of the block entity field list where possible.
- This is not a framework object. It is just a bag of explicit runtime state.

Fields:

- `int choppedLogCount`
- `int nextLogIndex`
- `int nextCollectionTick`
- `int nextSwingTick`
- `boolean saplingReserved`
- `boolean depositedAnyItems`

Methods:

- `public void resetForNewPlan(LumberjackTreePlan plan, long gameTime)`
- `public boolean canSwing(long gameTime)`
- `public void markSwing(long gameTime, int cooldownTicks)`
- `public boolean canCollect(long gameTime)`
- `public void markCollected(long gameTime, int cooldownTicks)`

## Focused Helpers

### `LumberjackNavigationHelper`

Responsibilities:

- Wrap villager pathing and arrival checks.
- Centralize stall detection.

Methods:

- `public static void enableDoorNavigation(Villager villager)`
- `public static boolean moveTo(Villager villager, BlockPos pos, double speed)`
- `public static boolean hasArrived(Villager villager, BlockPos pos, double maxDistanceSqr)`
- `public static boolean isStalled(Villager villager, Vec3 previousPos, double minMovementDistanceSqr)`

### `LumberjackHarvestHelper`

Responsibilities:

- Perform log breaking in a controlled way.
- Spawn vanilla drops by using the block’s normal destroy flow.

Methods:

- `public static boolean breakLog(ServerLevel level, BlockPos pos, @Nullable Villager villager)`
- `public static List<ItemEntity> findNearbyDrops(ServerLevel level, AABB bounds)`
- `public static boolean pickupDrops(Villager villager, List<ItemEntity> drops)`
- `public static boolean replantSapling(ServerLevel level, BlockPos pos, BlockState saplingState, Villager villager)`

## Config

### `PleaseChopConfig`

Suggested first settings:

- `villagerSearchRadius`
- `treeSearchRadius`
- `forestAnchorRadius`
- `maxTreeLogs`
- `pathfindTimeoutTicks`
- `dropCollectionRadius`
- `requireSaplingReplant`
- `sleepCleanupEnabled`

Methods:

- same structure as `PleaseStoreConfig`

I would keep this small. Too many knobs usually means the base behavior is not stable yet.

## Proposed First Delivery Slice

The smallest slice that proves the mod without overdesign:

1. Workstation block + POI + profession registration.
2. One assigned lumberjack villager.
3. Workstation-owned work cycle starts automatically when the lumberjack is idle and daytime/work conditions are valid.
4. Scan nearest simple overworld tree made of vanilla logs and leaves.
5. Villager walks to tree.
6. Tree logs are broken in a fixed order.
7. Villager picks up drops in a radius.
8. One matching sapling is replanted.
9. Villager returns to the workstation.
10. When the villager sleeps, harvest output in their inventory is cleaned up so the lumberjack remains usable without external extraction.

## Deliberate Non-Goals For V1

- No generalized villager job scheduler.
- No support for every modded tree on day one.
- No multi-worker coordination.
- No GUI unless item-based setup becomes clearly insufficient.
- No autonomous infinite farming loop. Triggered trips are easier to reason about and much safer.
- No custom profession system outside the vanilla workstation model.

## Main Design Questions To Resolve

These are the decisions worth discussing before implementation:

1. Should `forestAnchorPos` be configurable at all in V1, or should the workstation position itself be the only search origin?
2. Should the workstation rely purely on profession assignment, or also persist a stronger one-villager ownership lock once claimed?
3. What exact sleep cleanup rule feels fair enough to keep the villager functional without making storage mods irrelevant?
4. How aggressive should tree detection be about avoiding player-built log structures?
5. Should replanting be mandatory, or should the run still succeed if no sapling is available?
