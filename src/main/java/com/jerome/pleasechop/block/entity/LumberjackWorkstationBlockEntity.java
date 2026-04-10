package com.jerome.pleasechop.block.entity;

import com.jerome.pleasechop.config.PleaseChopConfig;
import com.jerome.pleasechop.registry.ModBlockEntities;
import com.jerome.pleasechop.registry.ModVillagerProfessions;
import com.jerome.pleasechop.tree.TreeCandidateDetector;
import com.jerome.pleasechop.tree.TreeCandidateDetector.CandidateTree;
import com.jerome.pleasechop.world.TreeReservationData;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LumberjackWorkstationBlockEntity extends BlockEntity {
    private static final String SLEEP_CLEANED_TAG = "pleasechop_sleep_cleaned";
    private static final String TREES_CUT_TODAY_TAG = "pleasechop_trees_cut_today";
    private static final String HIGHLIGHT_TREES_KEY = "highlight_trees";
    private static final String DEBUG_ROOT_BLOCKS_KEY = "debug_root_blocks";
    private static final String REMEMBERED_DROP_SITES_KEY = "remembered_drop_sites";
    private static final String PENDING_PLANTINGS_KEY = "pending_plantings";
    private static final String SITE_POS_KEY = "site_pos";
    private static final String SITE_AGE_TICKS_KEY = "site_age_ticks";
    private static final String SITE_COOLDOWN_TICKS_KEY = "site_cooldown_ticks";
    private static final String SITE_VISITED_IN_SWEEP_KEY = "site_visited_in_sweep";
    private static final String SITE_VISIT_COUNT_KEY = "site_visit_count";
    private static final String PLANTING_ROOT_POSITIONS_KEY = "planting_root_positions";
    private static final String PLANTING_SAPLING_ITEM_ID_KEY = "planting_sapling_item_id";
    private static final String ROOT_POS_KEY = "root_pos";
    private static final String ROOT_POSITIONS_KEY = "root_positions";
    private static final String TRUNK_POSITIONS_KEY = "trunk_positions";
    private static final String LOG_POSITIONS_KEY = "log_positions";
    private static final String LEAF_POSITIONS_KEY = "leaf_positions";
    private static final String SAPLING_ITEM_ID_KEY = "sapling_item_id";
    private static final int WORKER_SEARCH_RADIUS = 32;
    private static final int MIN_LINGER_TICKS = 400;
    private static final int QUIET_LINGER_TICKS = 400;
    private static final int MAX_LINGER_TICKS = 3200;
    private static final int PLANTING_RETRY_DELAY_TICKS = 400;
    private static final int WANDER_RETARGET_TICKS = 30;
    private static final int NOVICE_CHOP_INTERVAL_TICKS = 40;
    private static final int APPRENTICE_CHOP_INTERVAL_TICKS = 32;
    private static final int JOURNEYMAN_CHOP_INTERVAL_TICKS = 24;
    private static final int EXPERT_CHOP_INTERVAL_TICKS = 18;
    private static final int MASTER_CHOP_INTERVAL_TICKS = 12;
    private static final int ITEM_TARGET_TIMEOUT_TICKS = 60;
    private static final int MOVEMENT_STUCK_TICKS = 60;
    private static final int FAILED_ITEM_COOLDOWN_TICKS = 100;
    private static final int SCAVENGE_SITE_MAX_AGE_TICKS = 24000;
    private static final int SCAVENGE_SITE_RECHECK_COOLDOWN_TICKS = 200;
    private static final int SCAVENGE_SITE_AVAILABILITY_COOLDOWN_TICKS = 1200;
    private static final int MAX_REMEMBERED_DROP_SITES = 16;
    private static final int MAX_PENDING_PLANTINGS = 16;
    private static final int MAX_SCAVENGE_VISITS_PER_SITE = 2;
    private static final int PLANTING_RECOVERY_RETRY_TICKS = 100;
    private static final int AUTONOMOUS_WORK_CHECK_INTERVAL_TICKS = 10;
    private static final int AUTONOMOUS_WORK_START_CHANCE = 2;
    private static final int AUTONOMOUS_WORK_APPROACH_BUFFER_TICKS = 200;
    private static final int AUTONOMOUS_REJECTION_DEBUG_COOLDOWN_TICKS = 200;
    private static final int TREE_RESERVATION_DURATION_TICKS = 2400;
    private static final int SCAVENGE_MIN_LINGER_TICKS = 200;
    private static final int SCAVENGE_QUIET_LINGER_TICKS = 200;
    private static final int SCAVENGE_MAX_LINGER_TICKS = 800;
    private static final double MOVEMENT_PROGRESS_DISTANCE_SQR = 0.09D;
    private static final double TREE_WORK_START_DISTANCE_SQR = 12.25D;
    private static final double TREE_REACH_DISTANCE_SQR = 6.25D;
    private static final double ITEM_PICKUP_DISTANCE_SQR = 3.0D;
    private static final double WORKSTATION_REACH_DISTANCE_SQR = 4.0D;
    private static final float NOVICE_WORK_SPEED = 0.40F;
    private static final float APPRENTICE_WORK_SPEED = 0.425F;
    private static final float JOURNEYMAN_WORK_SPEED = 0.45F;
    private static final float EXPERT_WORK_SPEED = 0.475F;
    private static final float MASTER_WORK_SPEED = 0.50F;
    private List<CandidateTree> highlightedTrees = List.of();
    private List<BlockPos> debugRootBlocks = List.of();
    private final List<RememberedDropSite> rememberedDropSites = new ArrayList<>();
    private final List<PendingPlantingSite> pendingPlantings = new ArrayList<>();
    private final Set<String> seenAutonomousRejectionMessages = new HashSet<>();
    private ActiveJob activeJob;
    private PlantingRecoveryJob plantingRecoveryJob;
    private ScavengeJob scavengeJob;
    private int plantingRecoveryRetryTicks;
    private boolean plantingRecoveryIdle;
    private int lastPlantingRecoveryInventorySignature;
    private int autonomousWorkCheckTicks;
    private long lastAutonomousRejectionDebugTick;

    public LumberjackWorkstationBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LUMBERJACK_WORKSTATION.get(), pos, blockState);
        this.lastPlantingRecoveryInventorySignature = Integer.MIN_VALUE;
    }

    public void debugScanForTreeCandidates(ServerLevel level) {
        TreeCandidateDetector.DetectionScan scan = TreeCandidateDetector.scanDebug(level, worldPosition);
        debugRootBlocks = new ArrayList<>(scan.debugRootBlocks());
        highlightedTrees = new ArrayList<>(scan.candidateTrees());
        debugChat(level, "scan found " + debugRootBlocks.size() + " debug root blocks and " + highlightedTrees.size() + " candidate trees");
        scan.rejectionMessages().forEach(message -> debugChat(level, "rejected root " + message));
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
    }

    public void startWorkCycle(ServerLevel level) {
        debugScanForTreeCandidates(level);
        if (highlightedTrees.isEmpty()) {
            debugChat(level, "start rejected: no candidate tree");
            return;
        }
        if (activeJob != null) {
            debugChat(level, "start rejected: work job already active");
            return;
        }

        Villager worker = findAssignedWorker(level).orElse(null);
        if (worker == null) {
            debugChat(level, "start rejected: no assigned lumberjack");
            return;
        }

        if (!canCutAnotherTreeToday(worker)) {
            debugChat(level, "start rejected: daily tree limit reached " + getTreesCutToday(worker) + "/" + getDailyTreeLimit(worker));
            return;
        }

        if (scavengeJob != null) {
            stopScavengeJob(worker);
        } else {
            worker.getNavigation().stop();
            worker.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            worker.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            worker.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        }

        List<CandidateTree> availableTrees = highlightedTrees.stream()
                .filter(tree -> !isTreeReservedByOther(level, tree))
                .toList();
        if (availableTrees.isEmpty()) {
            debugChat(level, "start rejected: all candidate trees are reserved");
            return;
        }

        CandidateTree selectedTree = availableTrees.get(level.random.nextInt(availableTrees.size()));
        if (!tryReserveTree(level, selectedTree)) {
            debugChat(level, "start rejected: tree already reserved " + formatPos(selectedTree.rootPos()));
            return;
        }
        activeJob = new ActiveJob(worker.getUUID(), selectedTree, collectChopTargets(selectedTree), findTreeStandPos(selectedTree), worker.getBrain().isActive(Activity.WORK));
        debugChat(level, "started work job for " + worker.getName().getString() + " tree=" + formatPos(selectedTree.rootPos()) + " stand=" + formatPos(activeJob.treeStandPos()));
        setChanged();
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, LumberjackWorkstationBlockEntity workstation) {
        workstation.tickServer(level);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, LumberjackWorkstationBlockEntity workstation) {
        if (level instanceof ServerLevel serverLevel) {
            workstation.tickServer(serverLevel);
        }
    }

    public List<CandidateTree> getHighlightedTrees() {
        return highlightedTrees;
    }

    public List<BlockPos> getDebugRootBlocks() {
        return debugRootBlocks;
    }

    public List<BlockPos> getPendingPlantingRootBlocks() {
        List<BlockPos> roots = new ArrayList<>();
        for (PendingPlantingSite site : pendingPlantings) {
            roots.addAll(site.rootPositions());
        }
        return roots;
    }

    public BlockPos getActiveTreeStandPos() {
        return activeJob == null ? null : activeJob.treeStandPos();
    }

    public boolean hasReachedActiveTreeStandPos() {
        return activeJob != null && activeJob.phase() != WorkPhase.MOVING_TO_TREE;
    }

    private void tickServer(ServerLevel level) {
        tickRememberedDropSites();
        if (plantingRecoveryRetryTicks > 0) {
            plantingRecoveryRetryTicks--;
        }

        findAssignedWorkerForMaintenance(level).ifPresent(worker -> {
            tickSleepCleanup(worker);
            tickVanillaRestock(worker);
        });

        if (activeJob == null) {
            tickAutonomousWorkServer(level);
            if (activeJob != null) {
                return;
            }
            tickPlantingRecoveryServer(level);
            if (plantingRecoveryJob != null) {
                return;
            }
            tickScavengeServer(level);
            return;
        }

        Villager worker = findWorker(level, activeJob.workerId());
        if (worker == null || !isAssignedLumberjack(worker)) {
            debugChat(level, "stopping active job: worker unavailable");
            stopActiveJob(worker);
            return;
        }

        refreshTreeReservation(level);

        if (worker.isTrading()) {
            stopWorkerMovement(worker);
            return;
        }

        if (activeJob.endsWithWorkShift() && !worker.getBrain().isActive(Activity.WORK)) {
            debugChat(level, "stopping active job: work shift ended");
            stopActiveJob(worker);
            return;
        }

        switch (activeJob.phase()) {
            case MOVING_TO_TREE -> tickMoveToTree(worker);
            case CHOPPING -> tickChopping(level, worker);
            case LINGERING_UNDER_TREE -> tickLingeringUnderTree(level, worker);
            case PLANTING_SAPLING, RETURNING_TO_WORKSTATION -> tickReturnToWorkstation(worker);
        }
    }

    private void tickVanillaRestock(Villager worker) {
        if (worker.level() == level && worker.getBrain().isActive(Activity.WORK)) {
            tryRestockAtWorkstation(worker);
        }
    }

    private void tickAutonomousWorkServer(ServerLevel level) {
        if (plantingRecoveryJob != null || scavengeJob != null) {
            return;
        }

        if (autonomousWorkCheckTicks > 0) {
            autonomousWorkCheckTicks--;
            return;
        }
        autonomousWorkCheckTicks = AUTONOMOUS_WORK_CHECK_INTERVAL_TICKS;

        Villager worker = findAssignedWorker(level).orElse(null);
        if (worker == null || !worker.getBrain().isActive(Activity.WORK) || worker.isTrading() || !worker.getNavigation().isDone()) {
            return;
        }
        if (!canCutAnotherTreeToday(worker)) {
            return;
        }
        if (level.random.nextInt(AUTONOMOUS_WORK_START_CHANCE) != 0) {
            return;
        }

        long remainingWorkTicks = getRemainingWorkTicks(level);
        if (remainingWorkTicks <= 0) {
            return;
        }

        TreeCandidateDetector.DetectionScan scan = TreeCandidateDetector.scanDebug(level, worldPosition);
        List<CandidateTree> candidates = scan.candidateTrees().stream()
                .filter(tree -> !isTreeReservedByOther(level, tree))
                .filter(tree -> estimateAutonomousWorkDuration(tree, worker) <= remainingWorkTicks)
                .toList();
        if (candidates.isEmpty()) {
            debugAutonomousRejections(level, scan);
            return;
        }

        CandidateTree selectedTree = candidates.get(level.random.nextInt(candidates.size()));
        if (!tryReserveTree(level, selectedTree)) {
            return;
        }
        seenAutonomousRejectionMessages.clear();
        activeJob = new ActiveJob(worker.getUUID(), selectedTree, collectChopTargets(selectedTree), findTreeStandPos(selectedTree), true);
        debugChat(level, "autonomous start for " + worker.getName().getString() + " tree=" + formatPos(selectedTree.rootPos()) + " stand=" + formatPos(activeJob.treeStandPos()));
        setChanged();
    }

    private void debugAutonomousRejections(ServerLevel level, TreeCandidateDetector.DetectionScan scan) {
        if (!PleaseChopConfig.debugChatEnabled()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime - lastAutonomousRejectionDebugTick < AUTONOMOUS_REJECTION_DEBUG_COOLDOWN_TICKS) {
            return;
        }
        List<String> newMessages = scan.rejectionMessages().stream()
                .filter(seenAutonomousRejectionMessages::add)
                .toList();
        if (newMessages.isEmpty()) {
            return;
        }
        lastAutonomousRejectionDebugTick = gameTime;
        newMessages.forEach(message -> debugChat(level, "autonomous rejected root " + message));
    }

    private void tickScavengeServer(ServerLevel level) {
        if (plantingRecoveryJob != null) {
            return;
        }
        if (scavengeJob == null) {
            tryStartScavengeJob(level);
            return;
        }

        Villager worker = findWorker(level, scavengeJob.workerId());
        if (worker == null || !isAssignedLumberjack(worker) || !worker.getBrain().isActive(Activity.WORK)) {
            debugChat(level, "stopping scavenge: worker unavailable");
            stopScavengeJob(worker);
            return;
        }

        if (worker.isTrading()) {
            stopWorkerMovement(worker);
            return;
        }

        scavengeJob.tickFailedItems();
        switch (scavengeJob.phase()) {
            case MOVING_TO_SITE -> tickMoveToScavengeSite(level, worker);
            case COLLECTING -> tickScavengeCollecting(level, worker);
            case LINGERING_AT_SITE -> tickScavengeLingering(level, worker);
            case RETURNING_TO_WORKSTATION -> tickScavengeReturning(worker);
        }
    }

    private void tickPlantingRecoveryServer(ServerLevel level) {
        if (pendingPlantings.isEmpty() && plantingRecoveryJob == null) {
            plantingRecoveryIdle = true;
            return;
        }

        Villager worker = findAssignedWorker(level).orElse(null);
        if (worker == null || !worker.getBrain().isActive(Activity.WORK)) {
            if (plantingRecoveryJob != null) {
                if (plantingRecoveryJob.phase() == PlantingRecoveryPhase.RETURNING_AFTER_PLANTING) {
                    debugChat(level, "stopping planting recovery: post-plant return interrupted by end of work");
                } else {
                    debugChat(level, "stopping planting recovery: outside work hours");
                }
                stopPlantingRecoveryJob(worker);
            }
            return;
        }

        int inventorySignature = getInventorySignature(worker);
        if (plantingRecoveryJob == null) {
            if (plantingRecoveryIdle && inventorySignature == lastPlantingRecoveryInventorySignature) {
                return;
            }
            plantingRecoveryIdle = false;
            lastPlantingRecoveryInventorySignature = inventorySignature;
            tryStartPlantingRecoveryJob(level, worker);
            return;
        }

        plantingRecoveryIdle = false;
        lastPlantingRecoveryInventorySignature = inventorySignature;

        worker = findWorker(level, plantingRecoveryJob.workerId());
        if (worker == null || !isAssignedLumberjack(worker)) {
            debugChat(level, "stopping planting recovery: worker unavailable");
            stopPlantingRecoveryJob(worker);
            return;
        }

        if (worker.isTrading()) {
            stopWorkerMovement(worker);
            return;
        }

        switch (plantingRecoveryJob.phase()) {
            case MOVING_TO_SITE -> tickMoveToPlantingRecoverySite(level, worker);
            case PLANTING -> tickPlantingRecoveryPlanting(level, worker);
            case RETURNING_AFTER_PLANTING, RETURNING_TO_WORKSTATION -> tickPlantingRecoveryReturning(worker);
        }
    }

    private void tickMoveToTree(Villager worker) {
        BlockPos standPos = activeJob.treeStandPos();
        boolean reached = guideVillager(worker, standPos, 2);

        if (reached || isAtStandPos(worker, standPos)) {
            stopWorkerMovement(worker);
            if (!activeJob.countedAgainstDailyLimit()) {
                incrementTreesCutToday(worker);
                activeJob.markCountedAgainstDailyLimit();
            }
            activeJob.beginChopping();
            debugChat((ServerLevel) this.level, "transition MOVING_TO_TREE -> CHOPPING at stand=" + formatPos(standPos));
            setChanged();
            return;
        }

        if (!reached && activeJob.advanceMovementStuck(worker.position())) {
            debugChat((ServerLevel) this.level, "stopping active job: stuck moving to tree stand=" + formatPos(standPos));
            stopActiveJob(worker);
        }
    }

    private void tickChopping(ServerLevel level, Villager worker) {
        stopWorkerMovement(worker);

        BlockPos nextLogPos = activeJob.peekNextLog();
        while (nextLogPos != null && !isChopTargetBlock(level.getBlockState(nextLogPos))) {
            activeJob.popNextLog();
            nextLogPos = activeJob.peekNextLog();
        }

        if (nextLogPos == null) {
            rememberDropSite(activeJob.tree().rootPos());
            rememberPendingPlanting(activeJob.tree());
            debugChat(level, "queued replanting at " + formatPos(activeJob.tree().rootPos()));
            activeJob.beginLingering();
            debugChat(level, "transition CHOPPING -> LINGERING_UNDER_TREE tree=" + formatPos(activeJob.tree().rootPos()));
            return;
        }

        worker.getLookControl().setLookAt(Vec3.atCenterOf(nextLogPos));
        if (activeJob.advancePhaseTimer(getChopIntervalTicks(worker))) {
            worker.swing(InteractionHand.MAIN_HAND);
            level.destroyBlock(nextLogPos, true, worker, 512);
            activeJob.popNextLog();
            debugChat(level, "chopped block " + formatPos(nextLogPos) + " remaining=" + activeJob.remainingLogCount());
            setChanged();
        }
    }

    private void tickLingeringUnderTree(ServerLevel level, Villager worker) {
        activeJob.incrementLingerTime();
        activeJob.tickPlantingRetryCooldown();
        activeJob.tickFailedItems();

        if (!activeJob.saplingPlanted()
                && activeJob.canRetryPlanting()
                && isWithinPlantingReach(worker, activeJob.tree())
                && plantCandidateTreeIfPossible(level, worker, activeJob.tree())) {
            activeJob.markSaplingPlanted();
            removePendingPlanting(activeJob.tree().rootPositions(), activeJob.tree().saplingItemId());
            debugChat(level, "planted during lingering at " + formatPos(activeJob.tree().rootPos()));
            setChanged();
            return;
        }

        ItemEntity targetItem = activeJob.resolveTargetItem(level);
        if (targetItem == null) {
            targetItem = findNearestCollectibleDrop(level, activeJob.tree(), this::isCollectibleDrop);
            activeJob.setTargetItem(targetItem);
        }

        if (targetItem != null) {
            activeJob.noteCollectibleSeen();
            boolean reached = guideVillagerToItem(worker, targetItem);
            worker.getLookControl().setLookAt(targetItem.position());
            if (worker.blockPosition().equals(targetItem.blockPosition()) && worker.distanceToSqr(targetItem) <= ITEM_PICKUP_DISTANCE_SQR) {
                if (collectItem(worker, targetItem)) {
                    debugChat(level, "collected item " + targetItem.getItem().getHoverName().getString() + " at " + formatPos(targetItem.blockPosition()));
                    activeJob.setTargetItem(null);
                    activeJob.resetPhaseTimer();
                    activeJob.noteCollectibleSeen();
                    setChanged();
                } else {
                    debugChat(level, "could not collect item "
                            + targetItem.getItem().getHoverName().getString()
                            + " at " + formatPos(targetItem.blockPosition())
                            + " wanted=" + worker.wantsToPickUp(targetItem.getItem())
                            + " canAdd=" + worker.getInventory().canAddItem(targetItem.getItem()));
                    activeJob.markItemFailed(targetItem);
                    activeJob.clearTargetItem();
                    setChanged();
                }
            }
            else if (!reached && (activeJob.advanceItemTargetTimeout() || activeJob.advanceMovementStuck(worker.position()))) {
                activeJob.markItemFailed(targetItem);
                activeJob.clearTargetItem();
                setChanged();
            }
        } else {
            activeJob.incrementQuietTime();
            tickTreeWander(level, worker);
        }

        if (activeJob.shouldFinishLingering(
                hasVisibleStuckTreeDrop(level, worker, activeJob.tree()),
                haveTrackedLeavesSettled(level, activeJob.tree()),
                getMinLingerTicks(worker),
                getQuietLingerTicks(worker),
                getMaxLingerTicks(worker))) {
            worker.getNavigation().stop();
            activeJob.beginReturning();
            debugChat(level, "transition LINGERING_UNDER_TREE -> RETURNING_TO_WORKSTATION");
            setChanged();
        }
    }

    private void tickTreeWander(ServerLevel level, Villager worker) {
        BlockPos wanderTarget = activeJob.wanderTarget();
        boolean needsNewTarget = wanderTarget == null
                || worker.blockPosition().closerThan(wanderTarget, 1)
                || activeJob.advancePhaseTimer(WANDER_RETARGET_TICKS);
        if (needsNewTarget) {
            wanderTarget = findRandomTreeWanderPos(level, activeJob.tree());
            activeJob.setWanderTarget(wanderTarget);
            activeJob.resetPhaseTimer();
        }

        if (wanderTarget != null) {
            if (!guideVillager(worker, wanderTarget, 1) && activeJob.advanceMovementStuck(worker.position())) {
                activeJob.setWanderTarget(findRandomTreeWanderPos(level, activeJob.tree()));
                activeJob.resetPhaseTimer();
                setChanged();
            }
        }
    }

    private void tickReturnToWorkstation(Villager worker) {
        BlockPos returnPos = findAdjacentStandPos(worldPosition).orElse(worldPosition);
        boolean reached = guideVillager(worker, returnPos, 1);
        if (worker.distanceToSqr(Vec3.atCenterOf(returnPos)) <= WORKSTATION_REACH_DISTANCE_SQR) {
            ServerLevel serverLevel = (ServerLevel) this.level;
            tryRestockAtWorkstation(worker);
            debugChat(serverLevel, "return complete at workstation pendingReplants=" + pendingPlantings.size());
            stopActiveJob(worker);
            return;
        }

        if (!reached && activeJob.advanceMovementStuck(worker.position())) {
            debugChat((ServerLevel) this.level, "stopping active job: stuck returning to workstation");
            stopActiveJob(worker);
        }
    }

    private void tryStartPlantingRecoveryJob(ServerLevel level) {
        Villager worker = findAssignedWorker(level).orElse(null);
        tryStartPlantingRecoveryJob(level, worker);
    }

    private void tryStartPlantingRecoveryJob(ServerLevel level, Villager worker) {
        if (plantingRecoveryRetryTicks > 0) {
            return;
        }
        if (pendingPlantings.isEmpty()) {
            debugChat(level, "planting recovery skipped: no queued sites");
            plantingRecoveryIdle = true;
            return;
        }

        if (scavengeJob != null) {
            return;
        }

        if (worker == null) {
            debugChat(level, "planting recovery skipped: no assigned worker at workstation");
            return;
        }

        PendingPlantingSite site = findNextPendingPlanting(level, worker);
        if (site == null) {
            debugChat(level, "planting recovery waiting at workstation: " + describePendingPlantingBlocker(level, worker));
            plantingRecoveryIdle = true;
            plantingRecoveryRetryTicks = PLANTING_RECOVERY_RETRY_TICKS;
            return;
        }

        plantingRecoveryJob = new PlantingRecoveryJob(worker.getUUID(), site);
        plantingRecoveryRetryTicks = 0;
        debugChat(level, "transition IDLE -> PLANTING_RECOVERY MOVING_TO_SITE " + formatPos(site.rootPos()));
        setChanged();
    }

    private void tickMoveToPlantingRecoverySite(ServerLevel level, Villager worker) {
        BlockPos standPos = findPlantingRecoveryStandPos(plantingRecoveryJob.site());
        boolean reached = guidePlantingRecoveryWorker(worker, standPos, 1);
        if (isWithinPlantingReach(worker, plantingRecoveryJob.site())) {
            worker.getNavigation().stop();
            plantingRecoveryJob.beginPlanting();
            debugChat(level, "transition PLANTING_RECOVERY MOVING_TO_SITE -> PLANTING at " + formatPos(plantingRecoveryJob.site().rootPos()));
            setChanged();
            return;
        }

        if (!reached && plantingRecoveryJob.advanceMovementStuck(worker.position())) {
            debugChat(level, "stopping planting recovery: stuck moving to site");
            stopPlantingRecoveryJob(worker);
        }
    }

    private void tickPlantingRecoveryPlanting(ServerLevel level, Villager worker) {
        PendingPlantingSite site = plantingRecoveryJob.site();
        worker.getLookControl().setLookAt(Vec3.atCenterOf(site.rootPos()));
        if (!isWithinPlantingReach(worker, site)) {
            if (plantingRecoveryJob.advanceMovementStuck(worker.position())) {
                plantingRecoveryJob.beginReturningAfterFailure();
                debugChat(level, "transition PLANTING_RECOVERY PLANTING -> RETURNING_TO_WORKSTATION (out of range)");
                setChanged();
            }
            return;
        }

        Item expectedSaplingItem = getSaplingItem(site.saplingItemId());
        if (expectedSaplingItem != Items.AIR && countSaplings(worker, expectedSaplingItem) < site.rootPositions().size()) {
            ItemEntity saplingDrop = findNearestCollectibleDrop(level, site.rootPos(), stack -> stack.is(expectedSaplingItem), itemEntity -> true);
            if (saplingDrop != null) {
                boolean reached = guidePlantingRecoveryWorkerToItem(worker, saplingDrop);
                worker.getLookControl().setLookAt(saplingDrop.position());
                if (worker.blockPosition().equals(saplingDrop.blockPosition()) && worker.distanceToSqr(saplingDrop) <= ITEM_PICKUP_DISTANCE_SQR) {
                    if (collectItem(worker, saplingDrop)) {
                        debugChat(level, "collected replant sapling " + BuiltInRegistries.ITEM.getKey(expectedSaplingItem) + " at " + formatPos(saplingDrop.blockPosition()));
                        setChanged();
                        return;
                    }
                    debugChat(level, "could not collect replant sapling "
                            + BuiltInRegistries.ITEM.getKey(expectedSaplingItem)
                            + " at " + formatPos(saplingDrop.blockPosition())
                            + " wanted=" + worker.wantsToPickUp(saplingDrop.getItem())
                            + " canAdd=" + worker.getInventory().canAddItem(saplingDrop.getItem()));
                    plantingRecoveryJob.beginReturningAfterFailure();
                    setChanged();
                    return;
                }
                if (!reached && plantingRecoveryJob.advanceMovementStuck(worker.position())) {
                    plantingRecoveryJob.beginReturningAfterFailure();
                    debugChat(level, "transition PLANTING_RECOVERY PLANTING -> RETURNING_TO_WORKSTATION (stuck fetching sapling)");
                    setChanged();
                }
                return;
            }
        }

        if (plantPendingSiteIfPossible(level, worker, site)) {
            removePendingPlanting(site);
            plantingRecoveryJob.beginReturningAfterPlanting();
            debugChat(level, "transition PLANTING_RECOVERY PLANTING -> RETURNING_AFTER_PLANTING");
            setChanged();
            return;
        }

        plantingRecoveryJob.beginReturningAfterFailure();
        debugChat(level, "transition PLANTING_RECOVERY PLANTING -> RETURNING_TO_WORKSTATION (missing saplings)");
        setChanged();
    }

    private void tickPlantingRecoveryReturning(Villager worker) {
        BlockPos returnPos = findAdjacentStandPos(worldPosition).orElse(worldPosition);
        boolean reached = guidePlantingRecoveryWorker(worker, returnPos, 1);
        if (worker.distanceToSqr(Vec3.atCenterOf(returnPos)) <= WORKSTATION_REACH_DISTANCE_SQR) {
            tryRestockAtWorkstation(worker);
            if (plantingRecoveryJob.phase() == PlantingRecoveryPhase.RETURNING_AFTER_PLANTING) {
                debugChat((ServerLevel) this.level, "planting recovery post-plant return complete");
            } else {
                debugChat((ServerLevel) this.level, "planting recovery return complete");
            }
            stopPlantingRecoveryJob(worker);
            return;
        }

        if (!reached && plantingRecoveryJob.advanceMovementStuck(worker.position())) {
            if (plantingRecoveryJob.phase() == PlantingRecoveryPhase.RETURNING_AFTER_PLANTING) {
                debugChat((ServerLevel) this.level, "stopping planting recovery: post-plant return interrupted");
            } else {
                debugChat((ServerLevel) this.level, "stopping planting recovery: stuck returning");
            }
            stopPlantingRecoveryJob(worker);
        }
    }

    private boolean collectItem(Villager worker, ItemEntity itemEntity) {
        int beforeCount = itemEntity.getItem().getCount();
        InventoryCarrier.pickUpItem(worker, worker, itemEntity);
        if (!itemEntity.isRemoved()) {
            return itemEntity.getItem().getCount() < beforeCount;
        }
        return true;
    }

    private void tryStartScavengeJob(ServerLevel level) {
        Villager worker = findAssignedWorker(level).orElse(null);
        if (worker == null || !worker.getBrain().isActive(Activity.WORK) || !worker.getNavigation().isDone()) {
            return;
        }

        if (rememberedDropSites.isEmpty()) {
            return;
        }

        RememberedDropSite site = findNextScavengeSite(level, worker);
        if (site == null) {
            return;
        }

        scavengeJob = new ScavengeJob(worker.getUUID(), site.pos());
        site.cooldownTicks = SCAVENGE_SITE_RECHECK_COOLDOWN_TICKS;
        debugChat(level, "selected scavenge site " + formatPos(site.pos()) + " age=" + site.ageTicks + " visitedInSweep=" + site.visitedInSweep);
        debugChat(level, "transition IDLE -> SCAVENGE MOVING_TO_SITE " + formatPos(site.pos()));
        setChanged();
    }

    private void tickMoveToScavengeSite(ServerLevel level, Villager worker) {
        BlockPos standPos = findAdjacentStandPos(scavengeJob.sitePos()).orElse(scavengeJob.sitePos());
        boolean reached = guideScavengeWorker(worker, standPos, 1);
        if (worker.distanceToSqr(Vec3.atCenterOf(standPos)) <= TREE_REACH_DISTANCE_SQR) {
            worker.getNavigation().stop();
            scavengeJob.beginCollecting();
            debugChat(level, "transition SCAVENGE MOVING_TO_SITE -> COLLECTING at " + formatPos(scavengeJob.sitePos()));
            setChanged();
            return;
        }

        if (!reached && scavengeJob.advanceMovementStuck(worker.position())) {
            debugChat(level, "stopping scavenge: stuck moving to site");
            stopScavengeJob(worker);
        }
    }

    private void tickScavengeCollecting(ServerLevel level, Villager worker) {
        scavengeJob.incrementLingerTime();
        ItemEntity targetItem = scavengeJob.resolveTargetItem(level);
        if (targetItem == null) {
            targetItem = findNearestCollectibleDrop(level, scavengeJob.sitePos(), this::isCollectibleDrop, scavengeJob::hasItemFailureCooldown);
            scavengeJob.setTargetItem(targetItem);
        }

        if (targetItem != null) {
            scavengeJob.noteCollectibleSeen();
            boolean reached = guideScavengeWorkerToItem(worker, targetItem);
            worker.getLookControl().setLookAt(targetItem.position());
            if (worker.blockPosition().equals(targetItem.blockPosition()) && worker.distanceToSqr(targetItem) <= ITEM_PICKUP_DISTANCE_SQR) {
                if (collectItem(worker, targetItem)) {
                    debugChat(level, "scavenge collected item " + targetItem.getItem().getHoverName().getString() + " at " + formatPos(targetItem.blockPosition()));
                    scavengeJob.setTargetItem(null);
                    scavengeJob.noteCollectibleSeen();
                    setChanged();
                    return;
                }

                debugChat(level, "scavenge could not collect item "
                        + targetItem.getItem().getHoverName().getString()
                        + " at " + formatPos(targetItem.blockPosition())
                        + " wanted=" + worker.wantsToPickUp(targetItem.getItem())
                        + " canAdd=" + worker.getInventory().canAddItem(targetItem.getItem()));
                scavengeJob.markItemFailed(targetItem);
                scavengeJob.clearTargetItem();
                setChanged();
                return;
            }

            if (!reached && (scavengeJob.advanceItemTargetTimeout() || scavengeJob.advanceMovementStuck(worker.position()))) {
                scavengeJob.markItemFailed(targetItem);
                scavengeJob.clearTargetItem();
                setChanged();
            }
            return;
        }

        scavengeJob.beginLingering();
        debugChat(level, "transition SCAVENGE COLLECTING -> LINGERING_AT_SITE");
        setChanged();
    }

    private void tickScavengeLingering(ServerLevel level, Villager worker) {
        scavengeJob.incrementLingerTime();
        scavengeJob.incrementQuietTime();

        ItemEntity targetItem = findNearestCollectibleDrop(level, scavengeJob.sitePos(), this::isCollectibleDrop, scavengeJob::hasItemFailureCooldown);
        if (targetItem != null) {
            scavengeJob.setTargetItem(targetItem);
            scavengeJob.noteCollectibleSeen();
            scavengeJob.beginCollecting();
            debugChat(level, "transition SCAVENGE LINGERING_AT_SITE -> COLLECTING");
            setChanged();
            return;
        }

        tickScavengeWander(level, worker);
        if (scavengeJob.shouldFinishLingering()) {
            RememberedDropSite site = findRememberedDropSite(scavengeJob.sitePos()).orElse(null);
            if (site != null) {
                site.cooldownTicks = SCAVENGE_SITE_RECHECK_COOLDOWN_TICKS;
            }
            scavengeJob.beginReturning();
            debugChat(level, "transition SCAVENGE LINGERING_AT_SITE -> RETURNING_TO_WORKSTATION");
            setChanged();
        }
    }

    private void tickScavengeReturning(Villager worker) {
        BlockPos returnPos = findAdjacentStandPos(worldPosition).orElse(worldPosition);
        boolean reached = guideScavengeWorker(worker, returnPos, 1);
        if (worker.distanceToSqr(Vec3.atCenterOf(returnPos)) <= WORKSTATION_REACH_DISTANCE_SQR) {
            tryRestockAtWorkstation(worker);
            handleCompletedScavengeSite(scavengeJob.sitePos());
            debugChat((ServerLevel) this.level, "scavenge return complete");
            stopScavengeJob(worker);
            return;
        }

        if (!reached && scavengeJob.advanceMovementStuck(worker.position())) {
            debugChat((ServerLevel) this.level, "stopping scavenge: stuck returning");
            stopScavengeJob(worker);
        }
    }

    private void tickScavengeWander(ServerLevel level, Villager worker) {
        BlockPos wanderTarget = scavengeJob.wanderTarget();
        boolean needsNewTarget = wanderTarget == null
                || worker.blockPosition().closerThan(wanderTarget, 1)
                || scavengeJob.advancePhaseTimer(WANDER_RETARGET_TICKS);
        if (needsNewTarget) {
            wanderTarget = findRandomScavengeWanderPos(level, scavengeJob.sitePos());
            scavengeJob.setWanderTarget(wanderTarget);
            scavengeJob.resetPhaseTimer();
        }

        if (wanderTarget != null) {
            if (!guideScavengeWorker(worker, wanderTarget, 1) && scavengeJob.advanceMovementStuck(worker.position())) {
                scavengeJob.setWanderTarget(findRandomScavengeWanderPos(level, scavengeJob.sitePos()));
                scavengeJob.resetPhaseTimer();
                setChanged();
            }
        }
    }

    private void stopActiveJob(Villager worker) {
        releaseTreeReservation();
        if (worker != null) {
            stopWorkerMovement(worker);
            worker.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        }

        activeJob = null;
        setChanged();
    }

    private boolean tryReserveTree(ServerLevel level, CandidateTree tree) {
        return TreeReservationData.get(level).tryReserve(tree, worldPosition, level.getGameTime(), TREE_RESERVATION_DURATION_TICKS);
    }

    private boolean isTreeReservedByOther(ServerLevel level, CandidateTree tree) {
        return TreeReservationData.get(level).isReservedByOther(tree, worldPosition, level.getGameTime());
    }

    private void refreshTreeReservation(ServerLevel level) {
        if (activeJob == null) {
            return;
        }
        TreeReservationData.get(level).refresh(activeJob.tree(), worldPosition, level.getGameTime(), TREE_RESERVATION_DURATION_TICKS);
    }

    private void releaseTreeReservation() {
        if (!(level instanceof ServerLevel serverLevel) || activeJob == null) {
            return;
        }
        TreeReservationData.get(serverLevel).release(activeJob.tree(), worldPosition);
    }

    private void stopScavengeJob(Villager worker) {
        if (worker != null) {
            stopWorkerMovement(worker);
            worker.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        }

        scavengeJob = null;
        setChanged();
    }

    private void stopPlantingRecoveryJob(Villager worker) {
        if (worker != null) {
            stopWorkerMovement(worker);
            worker.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        }

        plantingRecoveryJob = null;
        setChanged();
    }

    private void debugChat(ServerLevel level, String message) {
        if (!PleaseChopConfig.debugChatEnabled()) {
            return;
        }
        level.players().forEach(player -> player.displayClientMessage(Component.literal("[PleaseChop " + worldPosition.getX() + "," + worldPosition.getY() + "," + worldPosition.getZ() + "] " + message), false));
    }

    private static String formatPos(BlockPos pos) {
        return "[" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "]";
    }

    private void stopWorkerMovement(Villager worker) {
        worker.getNavigation().stop();
        worker.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        worker.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    private void tryRestockAtWorkstation(Villager worker) {
        if (!worldPosition.closerToCenterThan(worker.position(), 2.5D)) {
            return;
        }
        if (worker.shouldRestock()) {
            worker.restock();
        }
    }

    private boolean guideVillager(Villager worker, BlockPos targetPos, int closeEnoughDist) {
        clearLeafObstacle(worker, targetPos);
        float workSpeed = getWorkSpeed(worker);
        Vec3 targetCenter = Vec3.atBottomCenterOf(targetPos);
        activeJob.updateMovementTarget(targetCenter);
        worker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(targetPos));
        if (worker.distanceToSqr(targetCenter) <= closeEnoughDist * closeEnoughDist) {
            activeJob.resetMovementProgress(worker.position());
            return true;
        }
        if (activeJob.shouldIssueMoveCommand() || worker.getNavigation().isDone()) {
            worker.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(targetPos, workSpeed, closeEnoughDist));
            worker.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, workSpeed);
        }
        return false;
    }

    private boolean guideVillagerToItem(Villager worker, ItemEntity itemEntity) {
        BlockPos itemPos = itemEntity.blockPosition();
        clearLeafObstacle(worker, itemPos);
        float workSpeed = getWorkSpeed(worker);
        if (worker.blockPosition().equals(itemPos)) {
            Vec3 targetPos = itemEntity.position();
            activeJob.updateMovementTarget(targetPos);
            worker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(targetPos));
            if (worker.distanceToSqr(itemEntity) <= ITEM_PICKUP_DISTANCE_SQR) {
                activeJob.resetMovementProgress(worker.position());
                return true;
            }
            if (activeJob.shouldIssueMoveCommand() || worker.getNavigation().isDone()) {
                worker.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(targetPos, workSpeed, 0));
                worker.getNavigation().moveTo(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), workSpeed);
            }
            return false;
        }

        return guideVillager(worker, itemPos, 0);
    }

    private boolean guideScavengeWorker(Villager worker, BlockPos targetPos, int closeEnoughDist) {
        clearLeafObstacle(worker, targetPos);
        float workSpeed = getWorkSpeed(worker);
        Vec3 targetCenter = Vec3.atBottomCenterOf(targetPos);
        scavengeJob.updateMovementTarget(targetCenter);
        worker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(targetPos));
        if (worker.distanceToSqr(targetCenter) <= closeEnoughDist * closeEnoughDist) {
            scavengeJob.resetMovementProgress(worker.position());
            return true;
        }
        if (scavengeJob.shouldIssueMoveCommand() || worker.getNavigation().isDone()) {
            worker.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(targetPos, workSpeed, closeEnoughDist));
            worker.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, workSpeed);
        }
        return false;
    }

    private boolean guideScavengeWorkerToItem(Villager worker, ItemEntity itemEntity) {
        BlockPos itemPos = itemEntity.blockPosition();
        clearLeafObstacle(worker, itemPos);
        float workSpeed = getWorkSpeed(worker);
        if (worker.blockPosition().equals(itemPos)) {
            Vec3 targetPos = itemEntity.position();
            scavengeJob.updateMovementTarget(targetPos);
            worker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(targetPos));
            if (worker.distanceToSqr(itemEntity) <= ITEM_PICKUP_DISTANCE_SQR) {
                scavengeJob.resetMovementProgress(worker.position());
                return true;
            }
            if (scavengeJob.shouldIssueMoveCommand() || worker.getNavigation().isDone()) {
                worker.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(targetPos, workSpeed, 0));
                worker.getNavigation().moveTo(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), workSpeed);
            }
            return false;
        }

        return guideScavengeWorker(worker, itemPos, 0);
    }

    private boolean guidePlantingRecoveryWorker(Villager worker, BlockPos targetPos, int closeEnoughDist) {
        clearLeafObstacle(worker, targetPos);
        float workSpeed = getWorkSpeed(worker);
        Vec3 targetCenter = Vec3.atBottomCenterOf(targetPos);
        plantingRecoveryJob.updateMovementTarget(targetCenter);
        worker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(targetPos));
        if (worker.distanceToSqr(targetCenter) <= closeEnoughDist * closeEnoughDist) {
            plantingRecoveryJob.resetMovementProgress(worker.position());
            return true;
        }
        if (plantingRecoveryJob.shouldIssueMoveCommand() || worker.getNavigation().isDone()) {
            worker.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(targetPos, workSpeed, closeEnoughDist));
            worker.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, workSpeed);
        }
        return false;
    }

    private boolean guidePlantingRecoveryWorkerToItem(Villager worker, ItemEntity itemEntity) {
        BlockPos itemPos = itemEntity.blockPosition();
        clearLeafObstacle(worker, itemPos);
        float workSpeed = getWorkSpeed(worker);
        if (worker.blockPosition().equals(itemPos)) {
            Vec3 targetPos = itemEntity.position();
            plantingRecoveryJob.updateMovementTarget(targetPos);
            worker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(targetPos));
            if (worker.distanceToSqr(itemEntity) <= ITEM_PICKUP_DISTANCE_SQR) {
                plantingRecoveryJob.resetMovementProgress(worker.position());
                return true;
            }
            if (plantingRecoveryJob.shouldIssueMoveCommand() || worker.getNavigation().isDone()) {
                worker.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(targetPos, workSpeed, 0));
                worker.getNavigation().moveTo(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), workSpeed);
            }
            return false;
        }

        return guidePlantingRecoveryWorker(worker, itemPos, 0);
    }

    private void clearLeafObstacle(Villager worker, BlockPos targetPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos workerPos = worker.blockPosition();
        int stepX = Integer.compare(targetPos.getX(), workerPos.getX());
        int stepZ = Integer.compare(targetPos.getZ(), workerPos.getZ());
        if (stepX == 0 && stepZ == 0) {
            return;
        }

        BlockPos nextFeetPos = new BlockPos(workerPos.getX() + stepX, workerPos.getY(), workerPos.getZ() + stepZ);
        BlockPos nextHeadPos = nextFeetPos.above();
        breakTreeObstacleBlock(serverLevel, worker, nextFeetPos);
        breakTreeObstacleBlock(serverLevel, worker, nextHeadPos);
    }

    private void breakTreeObstacleBlock(ServerLevel level, Villager worker, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.LEAVES) && !isBeehive(state)) {
            return;
        }

        worker.getLookControl().setLookAt(Vec3.atCenterOf(pos));
        worker.swing(InteractionHand.MAIN_HAND);
        level.destroyBlock(pos, true, worker, 512);
    }

    private boolean isBeehive(BlockState state) {
        return state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE);
    }

    private boolean isChopTargetBlock(BlockState state) {
        return state.is(BlockTags.LOGS) || isBeehive(state);
    }

    private List<BlockPos> collectChopTargets(CandidateTree tree) {
        LinkedHashSet<BlockPos> targets = new LinkedHashSet<>(tree.logPositions());
        tree.logPositions().stream()
                .flatMap(logPos -> java.util.Arrays.stream(Direction.values()).map(logPos::relative))
                .filter(candidate -> isBeehive(level.getBlockState(candidate)))
                .distinct()
                .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed()
                        .thenComparingInt(BlockPos::getX)
                        .thenComparingInt(BlockPos::getZ))
                .forEach(targets::add);
        return List.copyOf(targets);
    }

    private Optional<Villager> findAssignedWorker(ServerLevel level) {
        AABB searchBox = new AABB(worldPosition).inflate(WORKER_SEARCH_RADIUS);
        return level.getEntitiesOfClass(Villager.class, searchBox, this::isAssignedLumberjack).stream()
                .min(Comparator.comparingDouble(villager -> villager.distanceToSqr(Vec3.atCenterOf(worldPosition))));
    }

    private Optional<Villager> findAssignedWorkerForMaintenance(ServerLevel level) {
        AABB searchBox = new AABB(worldPosition).inflate(WORKER_SEARCH_RADIUS);
        return level.getEntitiesOfClass(Villager.class, searchBox, this::isAssignedLumberjackIgnoringSleep).stream()
                .min(Comparator.comparingDouble(villager -> villager.distanceToSqr(Vec3.atCenterOf(worldPosition))));
    }

    private Villager findWorker(ServerLevel level, UUID workerId) {
        AABB searchBox = new AABB(worldPosition).inflate(WORKER_SEARCH_RADIUS);
        return level.getEntitiesOfClass(Villager.class, searchBox, villager -> villager.getUUID().equals(workerId)).stream()
                .findFirst()
                .orElse(null);
    }

    private boolean isAssignedLumberjack(Villager villager) {
        if (!villager.isAlive() || villager.isBaby() || villager.isSleeping()) {
            return false;
        }

        return isAssignedLumberjackIgnoringSleep(villager);
    }

    private boolean isAssignedLumberjackIgnoringSleep(Villager villager) {
        if (!villager.isAlive() || villager.isBaby()) {
            return false;
        }

        if (villager.getVillagerData().getProfession() != ModVillagerProfessions.LUMBERJACK.get()) {
            return false;
        }

        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        return jobSite.isPresent()
                && jobSite.get().dimension().equals(level.dimension())
                && jobSite.get().pos().equals(worldPosition);
    }

    private BlockPos findTreeStandPos(CandidateTree tree) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos baseLogPos : tree.rootPositions()) {
            for (int radius = 1; radius <= 2; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        for (int dy = -1; dy <= 1; dy++) {
                            BlockPos candidate = baseLogPos.offset(dx, dy, dz);
                            if (candidate.equals(baseLogPos)) {
                                continue;
                            }
                            if (isStandable(candidate)) {
                                candidates.add(candidate);
                            }
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return findFallbackStandPos(tree);
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distSqr(tree.rootPos())))
                .orElse(findFallbackStandPos(tree));
    }

    private BlockPos findFallbackStandPos(CandidateTree tree) {
        BlockPos center = tree.rootPositions().isEmpty() ? tree.rootPos() : tree.rootPositions().get(0);
        for (int radius = 1; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos candidate = center.offset(dx, dy, dz);
                        if (isStandable(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return center.above();
    }

    private boolean isStandable(BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState feetState = level.getBlockState(pos);
        net.minecraft.world.level.block.state.BlockState headState = level.getBlockState(pos.above());
        net.minecraft.world.level.block.state.BlockState belowState = level.getBlockState(pos.below());
        return feetState.canBeReplaced()
                && headState.canBeReplaced()
                && belowState.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private boolean isAtStandPos(Villager worker, BlockPos standPos) {
        return worker.distanceToSqr(Vec3.atCenterOf(standPos)) <= TREE_WORK_START_DISTANCE_SQR;
    }

    private boolean isWithinPlantingReach(Villager worker, CandidateTree tree) {
        for (BlockPos plantingPos : tree.rootPositions()) {
            if (worker.distanceToSqr(Vec3.atCenterOf(plantingPos)) <= TREE_WORK_START_DISTANCE_SQR) {
                return true;
            }
        }
        return false;
    }

    private boolean isWithinPlantingReach(Villager worker, PendingPlantingSite site) {
        for (BlockPos plantingPos : site.rootPositions()) {
            if (worker.distanceToSqr(Vec3.atCenterOf(plantingPos)) <= TREE_WORK_START_DISTANCE_SQR) {
                return true;
            }
        }
        return false;
    }

    private BlockPos findRandomTreeWanderPos(ServerLevel level, CandidateTree tree) {
        List<BlockPos> candidates = new ArrayList<>();
        AABB bounds = createTreeBounds(tree).inflate(2.0D, 0.0D, 2.0D);
        int minX = (int) Math.floor(bounds.minX);
        int maxX = (int) Math.ceil(bounds.maxX);
        int minZ = (int) Math.floor(bounds.minZ);
        int maxZ = (int) Math.ceil(bounds.maxZ);
        int y = tree.rootPos().getY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (isStandable(candidate)) {
                    candidates.add(candidate);
                }
            }
        }

        if (candidates.isEmpty()) {
            return findTreeStandPos(tree);
        }

        return candidates.get(level.random.nextInt(candidates.size()));
    }

    private BlockPos findRandomScavengeWanderPos(ServerLevel level, BlockPos sitePos) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos candidate = sitePos.offset(dx, dy, dz);
                        if (isStandable(candidate)) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return findAdjacentStandPos(sitePos).orElse(sitePos.above());
        }

        return candidates.get(level.random.nextInt(candidates.size()));
    }

    private BlockPos findPlantingRecoveryStandPos(PendingPlantingSite site) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos rootPos : site.rootPositions()) {
            for (int radius = 1; radius <= 2; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        for (int dy = -1; dy <= 1; dy++) {
                            BlockPos candidate = rootPos.offset(dx, dy, dz);
                            if (site.rootPositions().contains(candidate)) {
                                continue;
                            }
                            if (isStandable(candidate)) {
                                candidates.add(candidate);
                            }
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            BlockPos center = site.rootPositions().isEmpty() ? site.rootPos() : site.rootPositions().get(0);
            return findAdjacentStandPos(center).orElse(center.above());
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distSqr(site.rootPos())))
                .orElse(site.rootPos().above());
    }

    private ItemEntity findNearestCollectibleDrop(ServerLevel level, CandidateTree tree, Predicate<ItemStack> itemFilter) {
        AABB searchBox = createTreeBounds(tree).inflate(3.0D, 2.0D, 3.0D);
        return level.getEntitiesOfClass(ItemEntity.class, searchBox, itemEntity -> itemFilter.test(itemEntity.getItem())
                && (activeJob == null || !activeJob.hasItemFailureCooldown(itemEntity))).stream()
                .min(Comparator.comparingDouble(itemEntity -> itemEntity.distanceToSqr(Vec3.atCenterOf(tree.rootPos()))))
                .orElse(null);
    }

    private ItemEntity findNearestCollectibleDrop(ServerLevel level, BlockPos sitePos, Predicate<ItemStack> itemFilter, Predicate<ItemEntity> extraFilter) {
        AABB searchBox = new AABB(sitePos).inflate(4.0D, 3.0D, 4.0D);
        return level.getEntitiesOfClass(ItemEntity.class, searchBox, itemEntity -> itemFilter.test(itemEntity.getItem()) && extraFilter.test(itemEntity)).stream()
                .min(Comparator.comparingDouble(itemEntity -> itemEntity.distanceToSqr(Vec3.atCenterOf(sitePos))))
                .orElse(null);
    }

    private boolean hasVisibleStuckTreeDrop(ServerLevel level, Villager worker, CandidateTree tree) {
        AABB searchBox = createTreeBounds(tree).inflate(3.0D, 2.0D, 3.0D);
        int stuckThresholdY = tree.rootPos().getY() + 2;
        return level.getEntitiesOfClass(ItemEntity.class, searchBox, itemEntity -> isCollectibleDrop(itemEntity.getItem())).stream()
                .anyMatch(itemEntity -> itemEntity.getY() >= stuckThresholdY && worker.hasLineOfSight(itemEntity));
    }

    private boolean haveTrackedLeavesSettled(ServerLevel level, CandidateTree tree) {
        if (tree.leafPositions().stream().noneMatch(pos -> level.getBlockState(pos).is(BlockTags.LEAVES))) {
            return true;
        }

        return activeJob != null && activeJob.hasReachedLeafWaitCap();
    }

    private AABB createTreeBounds(CandidateTree tree) {
        AABB bounds = new AABB(tree.rootPos());
        for (BlockPos logPos : tree.logPositions()) {
            bounds = bounds.minmax(new AABB(logPos));
        }
        return bounds;
    }

    private boolean isCollectibleDrop(ItemStack stack) {
        if (stack.is(Items.STICK) || stack.is(Items.APPLE)) {
            return true;
        }

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        BlockState blockState = blockItem.getBlock().defaultBlockState();
        return blockState.is(BlockTags.LOGS)
                || blockState.is(BlockTags.LEAVES)
                || blockState.is(BlockTags.SAPLINGS)
                || blockState.is(Blocks.BEE_NEST)
                || blockState.is(Blocks.BEEHIVE);
    }

    private void tickSleepCleanup(Villager worker) {
        if (!worker.isSleeping()) {
            worker.getPersistentData().remove(SLEEP_CLEANED_TAG);
            return;
        }

        CompoundTag persistentData = worker.getPersistentData();
        if (persistentData.getBoolean(SLEEP_CLEANED_TAG)) {
            return;
        }

        clearCollectedTreeItems(worker);
        persistentData.putInt(TREES_CUT_TODAY_TAG, 0);
        persistentData.putBoolean(SLEEP_CLEANED_TAG, true);
        if (level instanceof ServerLevel serverLevel) {
            debugChat(serverLevel, "cleared lumberjack inventory on sleep for " + worker.getName().getString());
        }
    }

    private boolean canCutAnotherTreeToday(Villager worker) {
        return getTreesCutToday(worker) < getDailyTreeLimit(worker);
    }

    private int getTreesCutToday(Villager worker) {
        return worker.getPersistentData().getInt(TREES_CUT_TODAY_TAG);
    }

    private int getDailyTreeLimit(Villager worker) {
        return Math.max(1, worker.getVillagerData().getLevel());
    }

    private void incrementTreesCutToday(Villager worker) {
        CompoundTag data = worker.getPersistentData();
        data.putInt(TREES_CUT_TODAY_TAG, data.getInt(TREES_CUT_TODAY_TAG) + 1);
    }

    private int getChopIntervalTicks(Villager worker) {
        return switch (worker.getVillagerData().getLevel()) {
            case 1 -> NOVICE_CHOP_INTERVAL_TICKS;
            case 2 -> APPRENTICE_CHOP_INTERVAL_TICKS;
            case 3 -> JOURNEYMAN_CHOP_INTERVAL_TICKS;
            case 4 -> EXPERT_CHOP_INTERVAL_TICKS;
            default -> MASTER_CHOP_INTERVAL_TICKS;
        };
    }

    private int getMinLingerTicks(Villager worker) {
        return switch (worker.getVillagerData().getLevel()) {
            case 1 -> MIN_LINGER_TICKS;
            case 2 -> 360;
            case 3 -> 320;
            case 4 -> 280;
            default -> 240;
        };
    }

    private int getQuietLingerTicks(Villager worker) {
        return switch (worker.getVillagerData().getLevel()) {
            case 1 -> QUIET_LINGER_TICKS;
            case 2 -> 360;
            case 3 -> 320;
            case 4 -> 280;
            default -> 240;
        };
    }

    private int getMaxLingerTicks(Villager worker) {
        return switch (worker.getVillagerData().getLevel()) {
            case 1 -> MAX_LINGER_TICKS;
            case 2 -> 2800;
            case 3 -> 2400;
            case 4 -> 2000;
            default -> 1600;
        };
    }

    private float getWorkSpeed(Villager worker) {
        return switch (worker.getVillagerData().getLevel()) {
            case 1 -> NOVICE_WORK_SPEED;
            case 2 -> APPRENTICE_WORK_SPEED;
            case 3 -> JOURNEYMAN_WORK_SPEED;
            case 4 -> EXPERT_WORK_SPEED;
            default -> MASTER_WORK_SPEED;
        };
    }

    private int getInventorySignature(Villager worker) {
        int signature = 1;
        SimpleContainer inventory = worker.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            signature = 31 * signature + ItemStack.hashItemAndComponents(stack);
            signature = 31 * signature + stack.getCount();
        }
        return signature;
    }

    private long estimateAutonomousWorkDuration(CandidateTree tree, Villager worker) {
        return (long) tree.logPositions().size() * getChopIntervalTicks(worker)
                + AUTONOMOUS_WORK_APPROACH_BUFFER_TICKS;
    }

    private long getRemainingWorkTicks(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;
        long workEndTime = Schedule.WORK_START_TIME + Schedule.TOTAL_WORK_TIME;
        return workEndTime - dayTime;
    }

    private void clearCollectedTreeItems(Villager worker) {
        Map<Item, Integer> reservedSaplings = getReservedSaplingsForCleanup(worker);
        SimpleContainer inventory = worker.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !isCollectibleDrop(stack)) {
                continue;
            }
            if (reservePendingSaplings(stack, reservedSaplings)) {
                continue;
            }
            inventory.setItem(slot, ItemStack.EMPTY);
        }
    }

    private Map<Item, Integer> getReservedSaplingsForCleanup(Villager worker) {
        Map<Item, Integer> reservedSaplings = new HashMap<>();
        for (PendingPlantingSite site : pendingPlantings) {
            Item saplingItem = getSaplingItem(site.saplingItemId());
            if (saplingItem == Items.AIR) {
                continue;
            }
            reservedSaplings.merge(saplingItem, site.rootPositions().size(), Integer::sum);
        }

        SimpleContainer inventory = worker.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !isSaplingItem(stack.getItem())) {
                continue;
            }
            reservedSaplings.merge(stack.getItem(), stack.getMaxStackSize(), Math::max);
        }
        return reservedSaplings;
    }

    private boolean isSaplingItem(Item item) {
        return getExpectedSaplingBlockItem(item) != null;
    }

    private boolean reservePendingSaplings(ItemStack stack, Map<Item, Integer> reservedSaplings) {
        Integer reservedCount = reservedSaplings.get(stack.getItem());
        if (reservedCount == null || reservedCount <= 0) {
            return false;
        }

        if (stack.getCount() <= reservedCount) {
            reservedSaplings.put(stack.getItem(), reservedCount - stack.getCount());
            return true;
        }

        stack.shrink(reservedCount);
        reservedSaplings.put(stack.getItem(), 0);
        return false;
    }

    private boolean canPlantPendingSite(ServerLevel level, Villager worker, PendingPlantingSite site) {
        Item expectedSaplingItem = getSaplingItem(site.saplingItemId());
        if (expectedSaplingItem == Items.AIR) {
            return false;
        }

        BlockItem saplingBlockItem = getExpectedSaplingBlockItem(expectedSaplingItem);
        if (saplingBlockItem == null) {
            return false;
        }

        for (BlockPos plantingPos : site.rootPositions()) {
            BlockState saplingState = saplingBlockItem.getBlock().defaultBlockState();
            if (!level.getBlockState(plantingPos).canBeReplaced() || !saplingState.canSurvive(level, plantingPos)) {
                return false;
            }
        }

        return countSaplings(worker, expectedSaplingItem) >= site.rootPositions().size();
    }

    private boolean plantCandidateTreeIfPossible(ServerLevel level, Villager worker, CandidateTree tree) {
        return plantPendingSiteIfPossible(level, worker, new PendingPlantingSite(tree.rootPos(), tree.rootPositions(), tree.saplingItemId(), 0));
    }

    private boolean plantPendingSiteIfPossible(ServerLevel level, Villager worker, PendingPlantingSite site) {
        Item expectedSaplingItem = getSaplingItem(site.saplingItemId());
        if (expectedSaplingItem == Items.AIR) {
            return false;
        }

        BlockItem saplingBlockItem = getExpectedSaplingBlockItem(expectedSaplingItem);
        if (saplingBlockItem == null) {
            return false;
        }

        for (BlockPos plantingPos : site.rootPositions()) {
            BlockState saplingState = saplingBlockItem.getBlock().defaultBlockState();
            if (!level.getBlockState(plantingPos).canBeReplaced() || !saplingState.canSurvive(level, plantingPos)) {
                return false;
            }
        }

        if (countSaplings(worker, expectedSaplingItem) < site.rootPositions().size()) {
            return false;
        }

        for (BlockPos plantingPos : site.rootPositions()) {
            level.setBlock(plantingPos, saplingBlockItem.getBlock().defaultBlockState(), 3);
            if (!consumeSapling(worker, expectedSaplingItem)) {
                return false;
            }
        }
        worker.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private Item getSaplingItem(String saplingItemId) {
        net.minecraft.resources.ResourceLocation itemId = net.minecraft.resources.ResourceLocation.tryParse(saplingItemId);
        return itemId == null ? Items.AIR : BuiltInRegistries.ITEM.get(itemId);
    }

    private BlockItem getExpectedSaplingBlockItem(Item expectedSaplingItem) {
        if (!(expectedSaplingItem instanceof BlockItem blockItem)) {
            return null;
        }
        BlockState saplingState = blockItem.getBlock().defaultBlockState();
        return saplingState.is(BlockTags.SAPLINGS) ? blockItem : null;
    }

    private int countSaplings(Villager worker, Item saplingItem) {
        int count = 0;
        SimpleContainer inventory = worker.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(saplingItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean consumeSapling(Villager worker, Item saplingItem) {
        SimpleContainer inventory = worker.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(saplingItem)) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    private void tickRememberedDropSites() {
        rememberedDropSites.removeIf(site -> {
            site.ageTicks++;
            if (site.cooldownTicks > 0) {
                site.cooldownTicks--;
            }
            return site.ageTicks >= SCAVENGE_SITE_MAX_AGE_TICKS;
        });
        pendingPlantings.forEach(site -> site.ageTicks++);
    }

    private void rememberDropSite(BlockPos sitePos) {
        Optional<RememberedDropSite> existingSite = findRememberedDropSite(sitePos);
        if (existingSite.isPresent()) {
            existingSite.get().ageTicks = 0;
            existingSite.get().cooldownTicks = SCAVENGE_SITE_RECHECK_COOLDOWN_TICKS;
            existingSite.get().visitedInSweep = false;
            setChanged();
            return;
        }

        if (rememberedDropSites.size() >= MAX_REMEMBERED_DROP_SITES) {
            rememberedDropSites.sort(Comparator.comparingInt(site -> -site.ageTicks));
            rememberedDropSites.remove(0);
        }

        rememberedDropSites.add(new RememberedDropSite(sitePos, 0, SCAVENGE_SITE_RECHECK_COOLDOWN_TICKS, false, 0));
        setChanged();
    }

    private void rememberPendingPlanting(CandidateTree tree) {
        if (findPendingPlanting(tree.rootPositions(), tree.saplingItemId()).isPresent()) {
            PendingPlantingSite existingSite = findPendingPlanting(tree.rootPositions(), tree.saplingItemId()).orElseThrow();
            existingSite.ageTicks = 0;
            pendingPlantings.remove(existingSite);
            pendingPlantings.add(existingSite);
            setChanged();
            return;
        }

        if (pendingPlantings.size() >= MAX_PENDING_PLANTINGS) {
            pendingPlantings.remove(0);
        }

        pendingPlantings.add(new PendingPlantingSite(tree.rootPos(), tree.rootPositions(), tree.saplingItemId(), 0));
        plantingRecoveryIdle = false;
        lastPlantingRecoveryInventorySignature = Integer.MIN_VALUE;
        setChanged();
    }

    private Optional<PendingPlantingSite> findPendingPlanting(List<BlockPos> rootPositions, String saplingItemId) {
        return pendingPlantings.stream()
                .filter(site -> site.saplingItemId().equals(saplingItemId) && site.rootPositions().equals(rootPositions))
                .findFirst();
    }

    private PendingPlantingSite findNextPendingPlanting(ServerLevel level, Villager worker) {
        for (int i = pendingPlantings.size() - 1; i >= 0; i--) {
            PendingPlantingSite site = pendingPlantings.get(i);
            if (canPlantPendingSite(level, worker, site)) {
                return site;
            }
        }
        return null;
    }

    private String describePendingPlantingBlocker(ServerLevel level, Villager worker) {
        if (pendingPlantings.isEmpty()) {
            return "no queued sites";
        }

        for (int i = pendingPlantings.size() - 1; i >= 0; i--) {
            PendingPlantingSite site = pendingPlantings.get(i);
            Item expectedSaplingItem = getSaplingItem(site.saplingItemId());
            if (expectedSaplingItem == Items.AIR) {
                return "queued site " + formatPos(site.rootPos()) + " has unknown sapling " + site.saplingItemId();
            }

            BlockItem saplingBlockItem = getExpectedSaplingBlockItem(expectedSaplingItem);
            if (saplingBlockItem == null) {
                return "queued site " + formatPos(site.rootPos()) + " sapling item is not placeable";
            }

            if (countSaplings(worker, expectedSaplingItem) < site.rootPositions().size()) {
                return "queued site " + formatPos(site.rootPos()) + " missing saplings " + BuiltInRegistries.ITEM.getKey(expectedSaplingItem);
            }

            for (BlockPos plantingPos : site.rootPositions()) {
                BlockState saplingState = saplingBlockItem.getBlock().defaultBlockState();
                if (!level.getBlockState(plantingPos).canBeReplaced()) {
                    return "queued site " + formatPos(site.rootPos()) + " blocked at " + formatPos(plantingPos);
                }
                if (!saplingState.canSurvive(level, plantingPos)) {
                    return "queued site " + formatPos(site.rootPos()) + " invalid ground at " + formatPos(plantingPos);
                }
            }
        }

        return "no plantable queued site";
    }

    private void removePendingPlanting(PendingPlantingSite site) {
        pendingPlantings.removeIf(existing -> existing.rootPositions().equals(site.rootPositions())
                && existing.saplingItemId().equals(site.saplingItemId()));
        lastPlantingRecoveryInventorySignature = Integer.MIN_VALUE;
        setChanged();
    }

    private void removePendingPlanting(List<BlockPos> rootPositions, String saplingItemId) {
        pendingPlantings.removeIf(existing -> existing.rootPositions().equals(rootPositions)
                && existing.saplingItemId().equals(saplingItemId));
        setChanged();
    }

    private RememberedDropSite findNextScavengeSite(ServerLevel level, Villager worker) {
        List<RememberedDropSite> eligibleSites = rememberedDropSites.stream()
                .filter(site -> site.cooldownTicks <= 0)
                .filter(site -> hasVisibleCollectibleDropAtSite(level, worker, site.pos()))
                .toList();
        if (eligibleSites.isEmpty()) {
            return null;
        }

        List<RememberedDropSite> unvisitedSites = eligibleSites.stream()
                .filter(site -> !site.visitedInSweep)
                .toList();
        if (unvisitedSites.isEmpty()) {
            eligibleSites.forEach(site -> site.visitedInSweep = false);
            debugChat(level, "reset scavenge sweep; all eligible sites were visited");
            unvisitedSites = eligibleSites;
        }

        RememberedDropSite nextSite = unvisitedSites.stream()
                .max(Comparator.comparingInt(site -> site.ageTicks))
                .orElse(null);
        if (nextSite != null) {
            nextSite.visitedInSweep = true;
        }
        return nextSite;
    }

    private boolean hasVisibleCollectibleDropAtSite(ServerLevel level, Villager worker, BlockPos sitePos) {
        return findNearestCollectibleDrop(level, sitePos, this::isCollectibleDrop, worker::hasLineOfSight) != null;
    }

    private Optional<RememberedDropSite> findRememberedDropSite(BlockPos sitePos) {
        return rememberedDropSites.stream().filter(site -> site.pos().equals(sitePos)).findFirst();
    }

    private void handleCompletedScavengeSite(BlockPos sitePos) {
        RememberedDropSite site = findRememberedDropSite(sitePos).orElse(null);
        if (site == null) {
            return;
        }

        site.visitCount++;
        if (site.visitCount >= MAX_SCAVENGE_VISITS_PER_SITE) {
            rememberedDropSites.remove(site);
            pendingPlantings.removeIf(existing -> existing.rootPos().equals(sitePos));
            debugChat((ServerLevel) level, "retired scavenge site " + formatPos(sitePos) + " after " + site.visitCount + " visits");
        } else {
            site.cooldownTicks = Math.max(site.cooldownTicks, SCAVENGE_SITE_AVAILABILITY_COOLDOWN_TICKS);
        }
        setChanged();
    }

    private Optional<BlockPos> findAdjacentStandPos(BlockPos centerPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = centerPos.relative(direction);
            if (isStandable(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag treeTags = tag.getList(HIGHLIGHT_TREES_KEY, Tag.TAG_COMPOUND);
        List<CandidateTree> trees = new ArrayList<>(treeTags.size());
        for (int i = 0; i < treeTags.size(); i++) {
            CompoundTag treeTag = treeTags.getCompound(i);
            BlockPos rootPos = BlockPos.of(treeTag.getLong(ROOT_POS_KEY));
            long[] rootValues = treeTag.getLongArray(ROOT_POSITIONS_KEY);
            long[] trunkValues = treeTag.getLongArray(TRUNK_POSITIONS_KEY);
            long[] logValues = treeTag.getLongArray(LOG_POSITIONS_KEY);
            long[] leafValues = treeTag.getLongArray(LEAF_POSITIONS_KEY);
            List<BlockPos> rootPositions = new ArrayList<>(rootValues.length);
            for (long value : rootValues) {
                rootPositions.add(BlockPos.of(value));
            }
            List<BlockPos> trunkPositions = new ArrayList<>(trunkValues.length);
            for (long value : trunkValues) {
                trunkPositions.add(BlockPos.of(value));
            }
            List<BlockPos> logPositions = new ArrayList<>(logValues.length);
            for (long value : logValues) {
                logPositions.add(BlockPos.of(value));
            }
            List<BlockPos> leafPositions = new ArrayList<>(leafValues.length);
            for (long value : leafValues) {
                leafPositions.add(BlockPos.of(value));
            }
            String saplingItemId = treeTag.getString(SAPLING_ITEM_ID_KEY);
            trees.add(new CandidateTree(rootPos, rootPositions.isEmpty() ? List.of(rootPos) : rootPositions, trunkPositions, logPositions, leafPositions, saplingItemId));
        }
        highlightedTrees = trees;

        long[] debugRootValues = tag.getLongArray(DEBUG_ROOT_BLOCKS_KEY);
        List<BlockPos> rootBlocks = new ArrayList<>(debugRootValues.length);
        for (long value : debugRootValues) {
            rootBlocks.add(BlockPos.of(value));
        }
        debugRootBlocks = rootBlocks;

        rememberedDropSites.clear();
        ListTag siteTags = tag.getList(REMEMBERED_DROP_SITES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < siteTags.size(); i++) {
            CompoundTag siteTag = siteTags.getCompound(i);
            rememberedDropSites.add(new RememberedDropSite(
                    BlockPos.of(siteTag.getLong(SITE_POS_KEY)),
                    siteTag.getInt(SITE_AGE_TICKS_KEY),
                    siteTag.getInt(SITE_COOLDOWN_TICKS_KEY),
                    siteTag.getBoolean(SITE_VISITED_IN_SWEEP_KEY),
                    siteTag.getInt(SITE_VISIT_COUNT_KEY)));
        }

        pendingPlantings.clear();
        ListTag plantingTags = tag.getList(PENDING_PLANTINGS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < plantingTags.size(); i++) {
            CompoundTag plantingTag = plantingTags.getCompound(i);
            long[] rootValues = plantingTag.getLongArray(PLANTING_ROOT_POSITIONS_KEY);
            List<BlockPos> rootPositions = new ArrayList<>(rootValues.length);
            for (long value : rootValues) {
                rootPositions.add(BlockPos.of(value));
            }
            BlockPos rootPos = rootPositions.isEmpty() ? BlockPos.of(plantingTag.getLong(ROOT_POS_KEY)) : rootPositions.get(0);
            pendingPlantings.add(new PendingPlantingSite(
                    rootPos,
                    rootPositions.isEmpty() ? List.of(rootPos) : rootPositions,
                    plantingTag.getString(PLANTING_SAPLING_ITEM_ID_KEY),
                    plantingTag.getInt(SITE_AGE_TICKS_KEY)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag treeTags = new ListTag();
        for (CandidateTree tree : highlightedTrees) {
            CompoundTag treeTag = new CompoundTag();
            treeTag.putLong(ROOT_POS_KEY, tree.rootPos().asLong());
            long[] rootValues = new long[tree.rootPositions().size()];
            for (int i = 0; i < tree.rootPositions().size(); i++) {
                rootValues[i] = tree.rootPositions().get(i).asLong();
            }
            long[] trunkValues = new long[tree.trunkPositions().size()];
            for (int i = 0; i < tree.trunkPositions().size(); i++) {
                trunkValues[i] = tree.trunkPositions().get(i).asLong();
            }
            long[] logValues = new long[tree.logPositions().size()];
            for (int i = 0; i < tree.logPositions().size(); i++) {
                logValues[i] = tree.logPositions().get(i).asLong();
            }
            long[] leafValues = new long[tree.leafPositions().size()];
            for (int i = 0; i < tree.leafPositions().size(); i++) {
                leafValues[i] = tree.leafPositions().get(i).asLong();
            }
            treeTag.putLongArray(ROOT_POSITIONS_KEY, rootValues);
            treeTag.putLongArray(TRUNK_POSITIONS_KEY, trunkValues);
            treeTag.putLongArray(LOG_POSITIONS_KEY, logValues);
            treeTag.putLongArray(LEAF_POSITIONS_KEY, leafValues);
            treeTag.putString(SAPLING_ITEM_ID_KEY, tree.saplingItemId());
            treeTags.add(treeTag);
        }
        tag.put(HIGHLIGHT_TREES_KEY, treeTags);
        long[] debugRootValues = new long[debugRootBlocks.size()];
        for (int i = 0; i < debugRootBlocks.size(); i++) {
            debugRootValues[i] = debugRootBlocks.get(i).asLong();
        }
        tag.putLongArray(DEBUG_ROOT_BLOCKS_KEY, debugRootValues);

        ListTag siteTags = new ListTag();
        for (RememberedDropSite site : rememberedDropSites) {
            CompoundTag siteTag = new CompoundTag();
            siteTag.putLong(SITE_POS_KEY, site.pos().asLong());
            siteTag.putInt(SITE_AGE_TICKS_KEY, site.ageTicks);
            siteTag.putInt(SITE_COOLDOWN_TICKS_KEY, site.cooldownTicks);
            siteTag.putBoolean(SITE_VISITED_IN_SWEEP_KEY, site.visitedInSweep);
            siteTag.putInt(SITE_VISIT_COUNT_KEY, site.visitCount);
            siteTags.add(siteTag);
        }
        tag.put(REMEMBERED_DROP_SITES_KEY, siteTags);

        ListTag plantingTags = new ListTag();
        for (PendingPlantingSite site : pendingPlantings) {
            CompoundTag plantingTag = new CompoundTag();
            plantingTag.putLong(ROOT_POS_KEY, site.rootPos().asLong());
            long[] rootValues = new long[site.rootPositions().size()];
            for (int i = 0; i < site.rootPositions().size(); i++) {
                rootValues[i] = site.rootPositions().get(i).asLong();
            }
            plantingTag.putLongArray(PLANTING_ROOT_POSITIONS_KEY, rootValues);
            plantingTag.putString(PLANTING_SAPLING_ITEM_ID_KEY, site.saplingItemId());
            plantingTag.putInt(SITE_AGE_TICKS_KEY, site.ageTicks);
            plantingTags.add(plantingTag);
        }
        tag.put(PENDING_PLANTINGS_KEY, plantingTags);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ListTag treeTags = new ListTag();
        for (CandidateTree tree : highlightedTrees) {
            CompoundTag treeTag = new CompoundTag();
            treeTag.putLong(ROOT_POS_KEY, tree.rootPos().asLong());
            long[] rootValues = new long[tree.rootPositions().size()];
            for (int i = 0; i < tree.rootPositions().size(); i++) {
                rootValues[i] = tree.rootPositions().get(i).asLong();
            }
            long[] trunkValues = new long[tree.trunkPositions().size()];
            for (int i = 0; i < tree.trunkPositions().size(); i++) {
                trunkValues[i] = tree.trunkPositions().get(i).asLong();
            }
            long[] logValues = new long[tree.logPositions().size()];
            for (int i = 0; i < tree.logPositions().size(); i++) {
                logValues[i] = tree.logPositions().get(i).asLong();
            }
            long[] leafValues = new long[tree.leafPositions().size()];
            for (int i = 0; i < tree.leafPositions().size(); i++) {
                leafValues[i] = tree.leafPositions().get(i).asLong();
            }
            treeTag.putLongArray(ROOT_POSITIONS_KEY, rootValues);
            treeTag.putLongArray(TRUNK_POSITIONS_KEY, trunkValues);
            treeTag.putLongArray(LOG_POSITIONS_KEY, logValues);
            treeTag.putLongArray(LEAF_POSITIONS_KEY, leafValues);
            treeTag.putString(SAPLING_ITEM_ID_KEY, tree.saplingItemId());
            treeTags.add(treeTag);
        }
        tag.put(HIGHLIGHT_TREES_KEY, treeTags);
        long[] debugRootValues = new long[debugRootBlocks.size()];
        for (int i = 0; i < debugRootBlocks.size(); i++) {
            debugRootValues[i] = debugRootBlocks.get(i).asLong();
        }
        tag.putLongArray(DEBUG_ROOT_BLOCKS_KEY, debugRootValues);

        ListTag siteTags = new ListTag();
        for (RememberedDropSite site : rememberedDropSites) {
            CompoundTag siteTag = new CompoundTag();
            siteTag.putLong(SITE_POS_KEY, site.pos().asLong());
            siteTag.putInt(SITE_AGE_TICKS_KEY, site.ageTicks);
            siteTag.putInt(SITE_COOLDOWN_TICKS_KEY, site.cooldownTicks);
            siteTag.putBoolean(SITE_VISITED_IN_SWEEP_KEY, site.visitedInSweep);
            siteTags.add(siteTag);
        }
        tag.put(REMEMBERED_DROP_SITES_KEY, siteTags);

        ListTag plantingTags = new ListTag();
        for (PendingPlantingSite site : pendingPlantings) {
            CompoundTag plantingTag = new CompoundTag();
            plantingTag.putLong(ROOT_POS_KEY, site.rootPos().asLong());
            long[] rootValues = new long[site.rootPositions().size()];
            for (int i = 0; i < site.rootPositions().size(); i++) {
                rootValues[i] = site.rootPositions().get(i).asLong();
            }
            plantingTag.putLongArray(PLANTING_ROOT_POSITIONS_KEY, rootValues);
            plantingTag.putString(PLANTING_SAPLING_ITEM_ID_KEY, site.saplingItemId());
            plantingTag.putInt(SITE_AGE_TICKS_KEY, site.ageTicks);
            plantingTags.add(plantingTag);
        }
        tag.put(PENDING_PLANTINGS_KEY, plantingTags);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private enum WorkPhase {
        MOVING_TO_TREE,
        CHOPPING,
        LINGERING_UNDER_TREE,
        PLANTING_SAPLING,
        RETURNING_TO_WORKSTATION
    }

    private enum ScavengePhase {
        MOVING_TO_SITE,
        COLLECTING,
        LINGERING_AT_SITE,
        RETURNING_TO_WORKSTATION
    }

    private enum PlantingRecoveryPhase {
        MOVING_TO_SITE,
        PLANTING,
        RETURNING_AFTER_PLANTING,
        RETURNING_TO_WORKSTATION
    }

    private static final class RememberedDropSite {
        private final BlockPos pos;
        private int ageTicks;
        private int cooldownTicks;
        private boolean visitedInSweep;
        private int visitCount;

        private RememberedDropSite(BlockPos pos, int ageTicks, int cooldownTicks, boolean visitedInSweep, int visitCount) {
            this.pos = pos;
            this.ageTicks = ageTicks;
            this.cooldownTicks = cooldownTicks;
            this.visitedInSweep = visitedInSweep;
            this.visitCount = visitCount;
        }

        private BlockPos pos() {
            return pos;
        }
    }

    private static final class PendingPlantingSite {
        private final BlockPos rootPos;
        private final List<BlockPos> rootPositions;
        private final String saplingItemId;
        private int ageTicks;

        private PendingPlantingSite(BlockPos rootPos, List<BlockPos> rootPositions, String saplingItemId, int ageTicks) {
            this.rootPos = rootPos;
            this.rootPositions = List.copyOf(rootPositions);
            this.saplingItemId = saplingItemId;
            this.ageTicks = ageTicks;
        }

        private BlockPos rootPos() {
            return rootPos;
        }

        private List<BlockPos> rootPositions() {
            return rootPositions;
        }

        private String saplingItemId() {
            return saplingItemId;
        }
    }

    private static final class ActiveJob {
        private final UUID workerId;
        private final CandidateTree tree;
        private final Deque<BlockPos> remainingLogs;
        private final boolean endsWithWorkShift;
        private WorkPhase phase;
        private int phaseTimer;
        private int lingerTicks;
        private int quietTicks;
        private UUID targetItemId;
        private int targetItemTimeoutTicks;
        private BlockPos wanderTarget;
        private BlockPos treeStandPos;
        private boolean saplingPlanted;
        private int plantingRetryCooldownTicks;
        private boolean countedAgainstDailyLimit;
        private Vec3 movementTarget;
        private Vec3 lastProgressPos;
        private int movementStuckTicks;
        private boolean movementCommandIssued;
        private final Map<UUID, Integer> failedItems;

        private ActiveJob(UUID workerId, CandidateTree tree, List<BlockPos> chopTargets, BlockPos treeStandPos, boolean endsWithWorkShift) {
            this.workerId = workerId;
            this.tree = tree;
            this.treeStandPos = treeStandPos;
            this.remainingLogs = new ArrayDeque<>(chopTargets);
            this.endsWithWorkShift = endsWithWorkShift;
            this.phase = WorkPhase.MOVING_TO_TREE;
            this.failedItems = new HashMap<>();
        }

        private UUID workerId() {
            return workerId;
        }

        private CandidateTree tree() {
            return tree;
        }

        private BlockPos treeStandPos() {
            return treeStandPos;
        }

        private boolean endsWithWorkShift() {
            return endsWithWorkShift;
        }

        private WorkPhase phase() {
            return phase;
        }

        private void setPhase(WorkPhase phase) {
            if (this.phase != phase) {
                this.phase = phase;
                resetPhaseTimer();
                resetMovementTracking();
            }
        }

        private void beginChopping() {
            setPhase(WorkPhase.CHOPPING);
        }

        private void beginLingering() {
            setPhase(WorkPhase.LINGERING_UNDER_TREE);
            clearTargetItem();
            this.wanderTarget = null;
        }

        private void beginPlanting() {
            setPhase(WorkPhase.PLANTING_SAPLING);
            clearTargetItem();
            this.wanderTarget = null;
        }

        private void beginReturning() {
            setPhase(WorkPhase.RETURNING_TO_WORKSTATION);
        }

        private boolean countedAgainstDailyLimit() {
            return countedAgainstDailyLimit;
        }

        private void markCountedAgainstDailyLimit() {
            this.countedAgainstDailyLimit = true;
        }

        private BlockPos peekNextLog() {
            return remainingLogs.peekFirst();
        }

        private void popNextLog() {
            remainingLogs.pollFirst();
            resetPhaseTimer();
        }

        private int remainingLogCount() {
            return remainingLogs.size();
        }

        private boolean advancePhaseTimer(int intervalTicks) {
            phaseTimer++;
            return phaseTimer >= intervalTicks;
        }

        private void resetPhaseTimer() {
            phaseTimer = 0;
        }

        private void incrementLingerTime() {
            lingerTicks++;
        }

        private boolean saplingPlanted() {
            return saplingPlanted;
        }

        private void markSaplingPlanted() {
            saplingPlanted = true;
            plantingRetryCooldownTicks = 0;
        }

        private void delayPlantingRetry() {
            plantingRetryCooldownTicks = PLANTING_RETRY_DELAY_TICKS;
        }

        private void tickPlantingRetryCooldown() {
            if (plantingRetryCooldownTicks > 0) {
                plantingRetryCooldownTicks--;
            }
        }

        private boolean canRetryPlanting() {
            return plantingRetryCooldownTicks <= 0;
        }

        private void incrementQuietTime() {
            quietTicks++;
        }

        private void noteCollectibleSeen() {
            quietTicks = 0;
        }

        private boolean shouldFinishLingering(boolean hasVisibleStuckTreeDrop, boolean trackedLeavesSettled, int minLingerTicks, int quietLingerTicks, int maxLingerTicks) {
            if (!trackedLeavesSettled) {
                return false;
            }

            if (hasVisibleStuckTreeDrop) {
                return false;
            }

            if (lingerTicks >= maxLingerTicks) {
                return true;
            }

            return lingerTicks >= minLingerTicks && quietTicks >= quietLingerTicks;
        }

        private boolean hasReachedLeafWaitCap() {
            return lingerTicks >= MAX_LINGER_TICKS;
        }

        private void setTargetItem(ItemEntity itemEntity) {
            targetItemId = itemEntity == null ? null : itemEntity.getUUID();
            targetItemTimeoutTicks = 0;
        }

        private void clearTargetItem() {
            targetItemId = null;
            targetItemTimeoutTicks = 0;
            resetMovementTracking();
        }

        private boolean advanceItemTargetTimeout() {
            targetItemTimeoutTicks++;
            return targetItemTimeoutTicks >= ITEM_TARGET_TIMEOUT_TICKS;
        }

        private BlockPos wanderTarget() {
            return wanderTarget;
        }

        private void setWanderTarget(BlockPos wanderTarget) {
            this.wanderTarget = wanderTarget;
            resetMovementTracking();
        }

        private ItemEntity resolveTargetItem(ServerLevel level) {
            if (targetItemId == null) {
                return null;
            }

            AABB searchBox = createSearchBox(tree);
            return level.getEntitiesOfClass(ItemEntity.class, searchBox, itemEntity -> itemEntity.getUUID().equals(targetItemId)).stream()
                    .findFirst()
                    .orElse(null);
        }

        private void updateMovementTarget(Vec3 targetPos) {
            if (movementTarget == null || movementTarget.distanceToSqr(targetPos) > 0.001D) {
                movementTarget = targetPos;
                movementCommandIssued = false;
                movementStuckTicks = 0;
                lastProgressPos = null;
            }
        }

        private boolean shouldIssueMoveCommand() {
            if (!movementCommandIssued) {
                movementCommandIssued = true;
                return true;
            }
            return false;
        }

        private boolean advanceMovementStuck(Vec3 workerPos) {
            if (lastProgressPos == null || lastProgressPos.distanceToSqr(workerPos) > MOVEMENT_PROGRESS_DISTANCE_SQR) {
                lastProgressPos = workerPos;
                movementStuckTicks = 0;
                return false;
            }

            movementStuckTicks++;
            return movementStuckTicks >= MOVEMENT_STUCK_TICKS;
        }

        private void resetMovementProgress(Vec3 workerPos) {
            lastProgressPos = workerPos;
            movementStuckTicks = 0;
        }

        private void resetMovementTracking() {
            movementTarget = null;
            lastProgressPos = null;
            movementStuckTicks = 0;
            movementCommandIssued = false;
        }

        private void markItemFailed(ItemEntity itemEntity) {
            failedItems.put(itemEntity.getUUID(), FAILED_ITEM_COOLDOWN_TICKS);
            resetMovementTracking();
        }

        private void tickFailedItems() {
            failedItems.entrySet().removeIf(entry -> entry.getValue() <= 1);
            failedItems.replaceAll((uuid, ticks) -> ticks - 1);
        }

        private boolean hasItemFailureCooldown(ItemEntity itemEntity) {
            return failedItems.containsKey(itemEntity.getUUID());
        }

        private static AABB createSearchBox(CandidateTree tree) {
            AABB bounds = new AABB(tree.rootPos());
            for (BlockPos logPos : tree.logPositions()) {
                bounds = bounds.minmax(new AABB(logPos));
            }
            return bounds.inflate(4.0D, 3.0D, 4.0D);
        }
    }

    private static final class ScavengeJob {
        private final UUID workerId;
        private final BlockPos sitePos;
        private ScavengePhase phase;
        private int phaseTimer;
        private int lingerTicks;
        private int quietTicks;
        private UUID targetItemId;
        private BlockPos wanderTarget;
        private int targetItemTimeoutTicks;
        private Vec3 movementTarget;
        private Vec3 lastProgressPos;
        private int movementStuckTicks;
        private boolean movementCommandIssued;
        private final Map<UUID, Integer> failedItems;

        private ScavengeJob(UUID workerId, BlockPos sitePos) {
            this.workerId = workerId;
            this.sitePos = sitePos;
            this.phase = ScavengePhase.MOVING_TO_SITE;
            this.failedItems = new HashMap<>();
        }

        private UUID workerId() {
            return workerId;
        }

        private BlockPos sitePos() {
            return sitePos;
        }

        private ScavengePhase phase() {
            return phase;
        }

        private void beginCollecting() {
            phase = ScavengePhase.COLLECTING;
            clearTargetItem();
            resetMovementTracking();
            resetPhaseTimer();
            quietTicks = 0;
        }

        private void beginLingering() {
            phase = ScavengePhase.LINGERING_AT_SITE;
            clearTargetItem();
            resetMovementTracking();
            resetPhaseTimer();
            wanderTarget = null;
        }

        private void beginReturning() {
            phase = ScavengePhase.RETURNING_TO_WORKSTATION;
            clearTargetItem();
            resetMovementTracking();
            resetPhaseTimer();
            wanderTarget = null;
        }

        private void setTargetItem(ItemEntity itemEntity) {
            targetItemId = itemEntity == null ? null : itemEntity.getUUID();
            targetItemTimeoutTicks = 0;
        }

        private void clearTargetItem() {
            targetItemId = null;
            targetItemTimeoutTicks = 0;
            resetMovementTracking();
        }

        private ItemEntity resolveTargetItem(ServerLevel level) {
            if (targetItemId == null) {
                return null;
            }

            AABB searchBox = new AABB(sitePos).inflate(4.0D, 3.0D, 4.0D);
            return level.getEntitiesOfClass(ItemEntity.class, searchBox, itemEntity -> itemEntity.getUUID().equals(targetItemId)).stream()
                    .findFirst()
                    .orElse(null);
        }

        private boolean advanceItemTargetTimeout() {
            targetItemTimeoutTicks++;
            return targetItemTimeoutTicks >= ITEM_TARGET_TIMEOUT_TICKS;
        }

        private boolean advancePhaseTimer(int intervalTicks) {
            phaseTimer++;
            return phaseTimer >= intervalTicks;
        }

        private void resetPhaseTimer() {
            phaseTimer = 0;
        }

        private void incrementLingerTime() {
            lingerTicks++;
        }

        private void incrementQuietTime() {
            quietTicks++;
        }

        private void noteCollectibleSeen() {
            quietTicks = 0;
        }

        private boolean shouldFinishLingering() {
            if (lingerTicks >= SCAVENGE_MAX_LINGER_TICKS) {
                return true;
            }

            return lingerTicks >= SCAVENGE_MIN_LINGER_TICKS && quietTicks >= SCAVENGE_QUIET_LINGER_TICKS;
        }

        private BlockPos wanderTarget() {
            return wanderTarget;
        }

        private void setWanderTarget(BlockPos wanderTarget) {
            this.wanderTarget = wanderTarget;
            resetMovementTracking();
        }

        private void updateMovementTarget(Vec3 targetPos) {
            if (movementTarget == null || movementTarget.distanceToSqr(targetPos) > 0.001D) {
                movementTarget = targetPos;
                movementCommandIssued = false;
                movementStuckTicks = 0;
                lastProgressPos = null;
            }
        }

        private boolean shouldIssueMoveCommand() {
            if (!movementCommandIssued) {
                movementCommandIssued = true;
                return true;
            }
            return false;
        }

        private boolean advanceMovementStuck(Vec3 workerPos) {
            if (lastProgressPos == null || lastProgressPos.distanceToSqr(workerPos) > MOVEMENT_PROGRESS_DISTANCE_SQR) {
                lastProgressPos = workerPos;
                movementStuckTicks = 0;
                return false;
            }

            movementStuckTicks++;
            return movementStuckTicks >= MOVEMENT_STUCK_TICKS;
        }

        private void resetMovementProgress(Vec3 workerPos) {
            lastProgressPos = workerPos;
            movementStuckTicks = 0;
        }

        private void resetMovementTracking() {
            movementTarget = null;
            lastProgressPos = null;
            movementStuckTicks = 0;
            movementCommandIssued = false;
        }

        private void markItemFailed(ItemEntity itemEntity) {
            failedItems.put(itemEntity.getUUID(), FAILED_ITEM_COOLDOWN_TICKS);
            resetMovementTracking();
        }

        private void tickFailedItems() {
            failedItems.entrySet().removeIf(entry -> entry.getValue() <= 1);
            failedItems.replaceAll((uuid, ticks) -> ticks - 1);
        }

        private boolean hasItemFailureCooldown(ItemEntity itemEntity) {
            return failedItems.containsKey(itemEntity.getUUID());
        }
    }

    private static final class PlantingRecoveryJob {
        private final UUID workerId;
        private final PendingPlantingSite site;
        private PlantingRecoveryPhase phase;
        private Vec3 movementTarget;
        private Vec3 lastProgressPos;
        private int movementStuckTicks;
        private boolean movementCommandIssued;

        private PlantingRecoveryJob(UUID workerId, PendingPlantingSite site) {
            this.workerId = workerId;
            this.site = site;
            this.phase = PlantingRecoveryPhase.MOVING_TO_SITE;
        }

        private UUID workerId() {
            return workerId;
        }

        private PendingPlantingSite site() {
            return site;
        }

        private PlantingRecoveryPhase phase() {
            return phase;
        }

        private void beginPlanting() {
            phase = PlantingRecoveryPhase.PLANTING;
            resetMovementTracking();
        }

        private void beginReturningAfterPlanting() {
            phase = PlantingRecoveryPhase.RETURNING_AFTER_PLANTING;
            resetMovementTracking();
        }

        private void beginReturningAfterFailure() {
            phase = PlantingRecoveryPhase.RETURNING_TO_WORKSTATION;
            resetMovementTracking();
        }

        private void updateMovementTarget(Vec3 targetPos) {
            if (movementTarget == null || movementTarget.distanceToSqr(targetPos) > 0.001D) {
                movementTarget = targetPos;
                movementCommandIssued = false;
                movementStuckTicks = 0;
                lastProgressPos = null;
            }
        }

        private boolean shouldIssueMoveCommand() {
            if (!movementCommandIssued) {
                movementCommandIssued = true;
                return true;
            }
            return false;
        }

        private boolean advanceMovementStuck(Vec3 workerPos) {
            if (lastProgressPos == null || lastProgressPos.distanceToSqr(workerPos) > MOVEMENT_PROGRESS_DISTANCE_SQR) {
                lastProgressPos = workerPos;
                movementStuckTicks = 0;
                return false;
            }

            movementStuckTicks++;
            return movementStuckTicks >= MOVEMENT_STUCK_TICKS;
        }

        private void resetMovementProgress(Vec3 workerPos) {
            lastProgressPos = workerPos;
            movementStuckTicks = 0;
        }

        private void resetMovementTracking() {
            movementTarget = null;
            lastProgressPos = null;
            movementStuckTicks = 0;
            movementCommandIssued = false;
        }
    }
}
