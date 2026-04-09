package com.jerome.pleasechop.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class TreeCandidateDetector {
    private static final int SEARCH_RADIUS = 16;
    private static final int MAX_HORIZONTAL_DRIFT = 4;
    private static final int MAX_HEIGHT = 48;
    private static final int MIN_TRUNK_HEIGHT = 4;
    private static final int MIN_LEAF_COUNT = 8;
    private static final int MAX_TRUNK_LAYER_WIDTH = 4;
    private static final int MAX_LOGS_PER_TRUNK_BLOCK = 24;
    private static final int OAK_MAX_LOGS = 48;
    private static final int WIDE_TREE_MAX_LOGS = 160;
    private static final int HUGE_SPRUCE_MAX_LOGS = 192;
    private static final int CANOPY_SCAN_DEPTH = 4;
    private static final int WIDE_SPRUCE_CANOPY_SCAN_DEPTH = 10;

    private TreeCandidateDetector() {
    }

    public static List<CandidateTree> findCandidateTrees(ServerLevel level, BlockPos workstationPos) {
        return scanDebug(level, workstationPos).candidateTrees();
    }

    public static DetectionScan scanDebug(ServerLevel level, BlockPos workstationPos) {
        List<CandidateTree> candidateTrees = new ArrayList<>();
        List<String> rejectionMessages = new ArrayList<>();
        Set<BlockPos> scannedRoots = new HashSet<>();
        Set<BlockPos> debugRoots = new HashSet<>();

        BlockPos.betweenClosedStream(workstationPos.offset(-SEARCH_RADIUS, -4, -SEARCH_RADIUS), workstationPos.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))
                .map(BlockPos::immutable)
                .filter(pos -> isGroundedVerticalBaseLog(level, pos))
                .map(pos -> findBaseAnchor(level, pos))
                .filter(rootPos -> scannedRoots.add(rootPos))
                .forEach(rootPos -> {
                    debugRoots.addAll(resolveDebugRootFootprint(level, rootPos));
                    AnalysisResult result = analyzeCandidate(level, rootPos);
                    if (result.candidate() != null) {
                        candidateTrees.add(result.candidate());
                    } else if (result.rejectionReason() != null) {
                        rejectionMessages.add(formatPos(rootPos) + " -> " + result.rejectionReason());
                    }
                });

        candidateTrees.sort(Comparator.comparingDouble(candidate -> candidate.rootPos().distSqr(workstationPos)));

        List<BlockPos> debugRootBlocks = debugRoots.stream()
                .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                        .thenComparingInt(BlockPos::getX)
                        .thenComparingInt(BlockPos::getZ))
                .toList();

        return new DetectionScan(List.copyOf(candidateTrees), debugRootBlocks, List.copyOf(rejectionMessages));
    }

    public static List<BlockPos> findDebugRootBlocks(ServerLevel level, BlockPos workstationPos) {
        return scanDebug(level, workstationPos).debugRootBlocks();
    }

    private static AnalysisResult analyzeCandidate(ServerLevel level, BlockPos rootPos) {
        Block rootBlock = level.getBlockState(rootPos).getBlock();
        WideTreeType wideTreeType = resolveWideTreeType(rootBlock);
        if (wideTreeType != null) {
            AnalysisResult wideResult = analyzeWideCandidate(level, rootPos, rootBlock, wideTreeType);
            if (wideResult.candidate() != null) {
                return wideResult;
            }
            if (!shouldFallbackToSingleTree(wideTreeType, wideResult.rejectionReason())) {
                return AnalysisResult.reject("block=" + BuiltInRegistries.BLOCK.getKey(rootBlock) + ", wide=" + wideResult.rejectionReason());
            }
        }

        AnalysisResult singleResult = analyzeSingleTreeCandidate(level, rootPos, rootBlock);
        if (singleResult.candidate() == null) {
            return AnalysisResult.reject("block=" + BuiltInRegistries.BLOCK.getKey(rootBlock) + ", single=" + singleResult.rejectionReason());
        }

        return singleResult;
    }

    private static boolean shouldFallbackToSingleTree(WideTreeType wideTreeType, String rejectionReason) {
        if (rejectionReason == null || !rejectionReason.startsWith("invalid 2x2 wide base")) {
            return false;
        }
        return wideTreeType == WideTreeType.SPRUCE || wideTreeType == WideTreeType.JUNGLE;
    }

    private static List<BlockPos> resolveDebugRootFootprint(ServerLevel level, BlockPos rootPos) {
        Block rootBlock = level.getBlockState(rootPos).getBlock();
        if (resolveWideTreeType(rootBlock) != null) {
            List<BlockPos> wideBase = findExactWideBase(level, rootPos, rootBlock);
            if (!wideBase.isEmpty()) {
                return wideBase;
            }
        }
        return List.of(rootPos);
    }

    private static AnalysisResult analyzeSingleTreeCandidate(ServerLevel level, BlockPos rootPos, Block rootBlock) {
        if (!isGroundedVerticalBaseLog(level, rootPos)) {
            return AnalysisResult.reject("root is not a grounded vertical log");
        }

        Set<BlockPos> rootLayer = Set.of(rootPos);
        Block expectedLeafBlock = resolveLeafBlock(rootBlock);
        if (expectedLeafBlock == null) {
            return AnalysisResult.reject("no supported leaf block for root " + rootBlock);
        }
        int maxLogs = calculateMaxLogs(rootBlock, 1);
        Set<BlockPos> connectedLogs = collectConnectedSpeciesLogs(level, List.of(rootPos), rootBlock, maxLogs);
        connectedLogs = includeLeafBridgedLogs(level, connectedLogs, rootBlock, expectedLeafBlock, rootPos, maxLogs);
        if (connectedLogs.isEmpty() || connectedLogs.size() > maxLogs) {
            return AnalysisResult.reject("connected logs invalid size=" + connectedLogs.size() + " max=" + maxLogs);
        }
        String neighborFailure = findInvalidTreeNeighbor(level, connectedLogs, rootLayer, rootBlock, false);
        if (neighborFailure != null) {
            return AnalysisResult.reject("tree touches invalid block " + neighborFailure);
        }
        if (hasForeignLogConnection(level, connectedLogs, rootBlock, rootPos)) {
            return AnalysisResult.reject("connected logs touch a different wood type");
        }
        if (hasGroundConnectionOutsideRoot(level, connectedLogs, rootLayer, rootBlock, rootPos)) {
            return AnalysisResult.reject("connected logs reach another ground contact outside the root");
        }

        List<BlockPos> trunkPositions = collectStem(level, rootLayer, maxLogs);
        if (trunkPositions.size() < MIN_TRUNK_HEIGHT) {
            return AnalysisResult.reject("trunk too short height=" + trunkPositions.size());
        }

        int topY = trunkPositions.stream().mapToInt(BlockPos::getY).max().orElse(rootPos.getY());
        int canopyStartY = Math.max(rootPos.getY(), topY - CANOPY_SCAN_DEPTH);
        int leafCount = countNearbyLeaves(level, trunkPositions, canopyStartY, topY);
        if (leafCount < MIN_LEAF_COUNT) {
            return AnalysisResult.reject("not enough nearby leaves count=" + leafCount + " min=" + MIN_LEAF_COUNT);
        }

        List<BlockPos> logPositions = connectedLogs.stream()
                .sorted(Comparator.<BlockPos>comparingInt((BlockPos pos) -> pos.getY()).reversed()
                        .thenComparingInt(pos -> pos.getX())
                        .thenComparingInt(pos -> pos.getZ()))
                .toList();

        List<BlockPos> leafPositions = collectAdjacentLeaves(level, connectedLogs).stream()
                .sorted(Comparator.<BlockPos>comparingInt(pos -> pos.getY()).reversed()
                        .thenComparingInt(pos -> pos.getX())
                        .thenComparingInt(pos -> pos.getZ()))
                .toList();

        Item saplingItem = resolveSaplingItem(rootBlock);
        if (saplingItem == Items.AIR) {
            return AnalysisResult.reject("no supported sapling item for root " + rootBlock);
        }

        List<BlockPos> rootPositions = rootLayer.stream()
                .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getX)
                        .thenComparingInt(BlockPos::getZ))
                .toList();
        if (!hasValidSaplingGround(level, rootPositions, saplingItem)) {
            return AnalysisResult.reject("root ground is not valid for replanting " + BuiltInRegistries.ITEM.getKey(saplingItem));
        }

        return AnalysisResult.accept(new CandidateTree(rootPos, rootPositions, trunkPositions, logPositions, leafPositions, BuiltInRegistries.ITEM.getKey(saplingItem).toString()));
    }

    private static AnalysisResult analyzeWideCandidate(ServerLevel level, BlockPos rootPos, Block rootBlock, WideTreeType wideTreeType) {
        List<BlockPos> rootPositions = findExactWideBase(level, rootPos, rootBlock);
        if (rootPositions.isEmpty()) {
            return AnalysisResult.reject("invalid 2x2 wide base for " + wideTreeType);
        }

        int maxLogs = calculateMaxLogs(rootBlock, rootPositions.size());
        Set<BlockPos> connectedLogs = collectConnectedSpeciesLogs(level, rootPositions, rootBlock, maxLogs);
        if (connectedLogs.isEmpty() || connectedLogs.size() > maxLogs) {
            return AnalysisResult.reject("wide connected logs invalid size=" + connectedLogs.size() + " max=" + maxLogs);
        }
        if (hasGroundConnectionOutsideRoot(level, connectedLogs, new HashSet<>(rootPositions), rootBlock, rootPos)) {
            return AnalysisResult.reject("wide connected logs reach another ground contact outside the 2x2 root");
        }

        List<BlockPos> trunkPositions = collectWideTrunk(level, rootPositions, rootBlock, wideTreeType);
        if (calculateTrunkHeight(trunkPositions) < MIN_TRUNK_HEIGHT) {
            return AnalysisResult.reject("wide trunk too short height=" + calculateTrunkHeight(trunkPositions));
        }

        Block expectedLeafBlock = resolveLeafBlock(rootBlock);
        if (expectedLeafBlock == null) {
            return AnalysisResult.reject("no supported leaf block for wide root " + rootBlock);
        }

        connectedLogs = includeLeafBridgedLogs(level, connectedLogs, rootBlock, expectedLeafBlock, rootPositions.get(0), maxLogs);
        if (connectedLogs.size() > maxLogs) {
            return AnalysisResult.reject("wide logs exceeded max after leaf bridge size=" + connectedLogs.size() + " max=" + maxLogs);
        }
        String neighborFailure = findInvalidTreeNeighbor(level, connectedLogs, new HashSet<>(rootPositions), rootBlock, wideTreeType == WideTreeType.JUNGLE);
        if (neighborFailure != null) {
            return AnalysisResult.reject("wide tree touches invalid block " + neighborFailure);
        }
        if (hasForeignLogConnection(level, connectedLogs, rootBlock, rootPositions.get(0))) {
            return AnalysisResult.reject("wide logs touch a different wood type");
        }
        if (!validateWideTree(level, rootPositions, connectedLogs, trunkPositions, rootBlock, expectedLeafBlock, wideTreeType)) {
            return AnalysisResult.reject("wide tree validation failed for " + wideTreeType);
        }

        int topY = trunkPositions.stream().mapToInt(BlockPos::getY).max().orElse(rootPos.getY());
        int trunkHeight = calculateTrunkHeight(trunkPositions);
        int canopyDepth = wideTreeType == WideTreeType.SPRUCE
                ? Math.min(trunkHeight, Math.max(WIDE_SPRUCE_CANOPY_SCAN_DEPTH, trunkHeight / 2))
                : CANOPY_SCAN_DEPTH;
        int canopyStartY = Math.max(rootPos.getY(), topY - canopyDepth + 1);
        int leafCount = wideTreeType == WideTreeType.SPRUCE
                ? countNearbyLeaves(level, trunkPositions, canopyStartY, topY, 3, -2, 4)
                : countNearbyLeaves(level, trunkPositions, canopyStartY, topY);
        if (leafCount < MIN_LEAF_COUNT) {
            return AnalysisResult.reject("wide canopy leaf count too low count=" + leafCount + " min=" + MIN_LEAF_COUNT);
        }

        List<BlockPos> logPositions = connectedLogs.stream()
                .sorted(Comparator.<BlockPos>comparingInt((BlockPos pos) -> pos.getY()).reversed()
                        .thenComparingInt(pos -> pos.getX())
                        .thenComparingInt(pos -> pos.getZ()))
                .toList();

        List<BlockPos> leafPositions = collectAdjacentLeaves(level, connectedLogs).stream()
                .sorted(Comparator.<BlockPos>comparingInt(pos -> pos.getY()).reversed()
                        .thenComparingInt(pos -> pos.getX())
                        .thenComparingInt(pos -> pos.getZ()))
                .toList();

        Item saplingItem = resolveSaplingItem(rootBlock);
        if (saplingItem == Items.AIR) {
            return AnalysisResult.reject("no supported sapling item for wide root " + rootBlock);
        }
        if (!hasValidSaplingGround(level, rootPositions, saplingItem)) {
            return AnalysisResult.reject("wide root ground is not valid for replanting " + BuiltInRegistries.ITEM.getKey(saplingItem));
        }

        return AnalysisResult.accept(new CandidateTree(rootPos, rootPositions, trunkPositions, logPositions, leafPositions, BuiltInRegistries.ITEM.getKey(saplingItem).toString()));
    }

    private static Set<BlockPos> collectConnectedSpeciesLogs(ServerLevel level, List<BlockPos> rootPositions, Block logBlock, int maxLogs) {
        Set<BlockPos> visited = new HashSet<>(rootPositions);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(rootPositions);
        BlockPos anchor = rootPositions.get(0);

        while (!queue.isEmpty() && visited.size() <= maxLogs) {
            BlockPos current = queue.removeFirst();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        BlockPos next = current.offset(dx, dy, dz);
                        if (visited.contains(next)) {
                            continue;
                        }

                        if (Math.abs(next.getX() - anchor.getX()) > MAX_HORIZONTAL_DRIFT + 2
                                || Math.abs(next.getZ() - anchor.getZ()) > MAX_HORIZONTAL_DRIFT + 2
                                || next.getY() < anchor.getY()
                                || next.getY() - anchor.getY() > MAX_HEIGHT) {
                            continue;
                        }

                        if (!level.getBlockState(next).is(logBlock)) {
                            continue;
                        }

                        visited.add(next);
                        queue.addLast(next);
                    }
                }
            }
        }

        return visited;
    }

    private static Set<BlockPos> includeLeafBridgedLogs(ServerLevel level, Set<BlockPos> connectedLogs, Block logBlock, Block expectedLeafBlock, BlockPos anchor, int maxLogs) {
        Set<BlockPos> expandedLogs = new HashSet<>(connectedLogs);
        Set<BlockPos> connectedLeaves = collectAdjacentMatchingLeaves(level, expandedLogs, expectedLeafBlock);
        boolean added;

        do {
            added = false;
            List<BlockPos> candidates = connectedLeaves.stream()
                    .flatMap(leafPos -> BlockPos.betweenClosedStream(leafPos.offset(-1, -1, -1), leafPos.offset(1, 1, 1)))
                    .map(BlockPos::immutable)
                    .filter(candidate -> !expandedLogs.contains(candidate))
                    .filter(candidate -> isWithinTreeBounds(candidate, anchor, 2))
                    .filter(candidate -> level.getBlockState(candidate).is(logBlock))
                    .filter(candidate -> hasOnlyLeafBridge(level, candidate, expandedLogs, expectedLeafBlock))
                    .distinct()
                    .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                            .thenComparingInt(BlockPos::getX)
                            .thenComparingInt(BlockPos::getZ))
                    .toList();

            for (BlockPos candidate : candidates) {
                if (expandedLogs.size() >= maxLogs) {
                    return expandedLogs;
                }
                expandedLogs.add(candidate);
                added = true;
            }

            if (added) {
                connectedLeaves = collectAdjacentMatchingLeaves(level, expandedLogs, expectedLeafBlock);
            }
        } while (added);

        return expandedLogs;
    }

    private static List<BlockPos> findExactWideBase(ServerLevel level, BlockPos rootPos, Block rootBlock) {
        List<BlockPos> basePositions = List.of(rootPos, rootPos.east(), rootPos.south(), rootPos.south().east());
        for (BlockPos basePos : basePositions) {
            BlockState baseState = level.getBlockState(basePos);
            if (!baseState.is(rootBlock) || !isVerticalLog(baseState)) {
                return List.of();
            }
        }

        boolean hasGroundedSupport = basePositions.stream().anyMatch(basePos -> {
            BlockState belowState = level.getBlockState(basePos.below());
            return !isLog(belowState) && isValidTreeGround(level, basePos.below(), belowState);
        });
        if (!hasGroundedSupport) {
            return List.of();
        }

        return basePositions.stream()
                .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ))
                .toList();
    }

    private static List<BlockPos> collectStem(ServerLevel level, Set<BlockPos> rootLayer, int maxLogs) {
        List<BlockPos> stemPositions = new ArrayList<>();
        Set<BlockPos> currentLayer = new HashSet<>(rootLayer);
        int baseY = rootLayer.stream().mapToInt(BlockPos::getY).min().orElse(0);

        while (!currentLayer.isEmpty() && stemPositions.size() <= maxLogs) {
            List<BlockPos> sortedLayer = currentLayer.stream()
                    .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                            .thenComparingInt(BlockPos::getX)
                            .thenComparingInt(BlockPos::getZ))
                    .toList();
            stemPositions.addAll(sortedLayer);

            int nextY = currentLayer.iterator().next().getY() + 1;
            if (nextY - baseY > MAX_HEIGHT) {
                break;
            }

            Set<BlockPos> nextLayer = new HashSet<>();
            for (BlockPos current : currentLayer) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos candidate = new BlockPos(current.getX() + dx, nextY, current.getZ() + dz);
                        if (!isLog(level.getBlockState(candidate))) {
                            continue;
                        }
                        nextLayer.add(candidate);
                    }
                }
            }

            if (nextLayer.size() > MAX_TRUNK_LAYER_WIDTH) {
                nextLayer = nextLayer.stream()
                        .sorted(Comparator.<BlockPos>comparingInt(pos -> Math.abs(pos.getX() - rootLayer.iterator().next().getX())
                                + Math.abs(pos.getZ() - rootLayer.iterator().next().getZ()))
                                .thenComparingInt(BlockPos::getX)
                                .thenComparingInt(BlockPos::getZ))
                        .limit(MAX_TRUNK_LAYER_WIDTH)
                        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
            }

            currentLayer = nextLayer;
        }

        return stemPositions;
    }

    private static List<BlockPos> collectWideTrunk(ServerLevel level, List<BlockPos> rootPositions, Block logBlock, WideTreeType wideTreeType) {
        if (wideTreeType == WideTreeType.DARK_OAK) {
            return collectDarkOakTrunk(level, rootPositions, logBlock);
        }

        List<BlockPos> trunkPositions = new ArrayList<>();
        int baseY = rootPositions.get(0).getY();
        int consecutiveSparseLayers = 0;

        for (int offsetY = 0; offsetY <= MAX_HEIGHT; offsetY++) {
            int currentY = baseY + offsetY;
            int layerCount = 0;
            for (BlockPos rootPos : rootPositions) {
                BlockPos candidate = new BlockPos(rootPos.getX(), currentY, rootPos.getZ());
                if (level.getBlockState(candidate).is(logBlock)) {
                    trunkPositions.add(candidate);
                    layerCount++;
                }
            }

            if (layerCount == 0) {
                break;
            }

            if (layerCount < rootPositions.size()) {
                consecutiveSparseLayers++;
            } else {
                consecutiveSparseLayers = 0;
            }

            if (consecutiveSparseLayers > 2) {
                break;
            }
        }

        return trunkPositions;
    }

    private static List<BlockPos> collectDarkOakTrunk(ServerLevel level, List<BlockPos> rootPositions, Block logBlock) {
        List<BlockPos> trunkPositions = new ArrayList<>();
        Set<BlockPos> currentLayer = new HashSet<>(rootPositions);
        BlockPos anchor = rootPositions.get(0);
        int baseY = anchor.getY();
        int emptyLayers = 0;

        for (int offsetY = 0; offsetY <= MAX_HEIGHT; offsetY++) {
            int currentY = baseY + offsetY;
            Set<BlockPos> layer = new HashSet<>();

            if (offsetY == 0) {
                layer.addAll(rootPositions);
            } else {
                for (BlockPos previous : currentLayer) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos candidate = new BlockPos(previous.getX() + dx, currentY, previous.getZ() + dz);
                            if (!isWithinTreeBounds(candidate, anchor, 2)) {
                                continue;
                            }
                            if (level.getBlockState(candidate).is(logBlock)) {
                                layer.add(candidate);
                            }
                        }
                    }
                }
            }

            if (layer.isEmpty()) {
                emptyLayers++;
                if (emptyLayers > 1) {
                    break;
                }
                continue;
            }

            emptyLayers = 0;
            trunkPositions.addAll(layer.stream()
                    .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                            .thenComparingInt(BlockPos::getX)
                            .thenComparingInt(BlockPos::getZ))
                    .toList());
            currentLayer = layer;
        }

        return trunkPositions;
    }

    private static int countNearbyLeaves(ServerLevel level, List<BlockPos> trunkPositions, int minY, int maxY) {
        return countNearbyLeaves(level, trunkPositions, minY, maxY, 2, -1, 3);
    }

    private static int countNearbyLeaves(ServerLevel level, List<BlockPos> trunkPositions, int minY, int maxY, int horizontalRadius, int minDy, int maxDy) {
        Set<BlockPos> countedLeaves = new HashSet<>();
        for (BlockPos trunkLog : trunkPositions) {
            if (trunkLog.getY() < minY || trunkLog.getY() > maxY) {
                continue;
            }
            for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                for (int dy = minDy; dy <= maxDy; dy++) {
                    for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                        BlockPos candidate = trunkLog.offset(dx, dy, dz);
                        if (isLeaf(level.getBlockState(candidate))) {
                            countedLeaves.add(candidate);
                        }
                    }
                }
            }
        }
        return countedLeaves.size();
    }

    private static Set<BlockPos> collectAdjacentLeaves(ServerLevel level, Set<BlockPos> connectedLogs) {
        Set<BlockPos> adjacentLeaves = new HashSet<>();
        for (BlockPos logPos : connectedLogs) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        BlockPos candidate = logPos.offset(dx, dy, dz);
                        if (isLeaf(level.getBlockState(candidate))) {
                            adjacentLeaves.add(candidate);
                        }
                    }
                }
            }
        }

        return adjacentLeaves;
    }

    private static Set<BlockPos> collectAdjacentMatchingLeaves(ServerLevel level, Set<BlockPos> connectedLogs, Block expectedLeafBlock) {
        Set<BlockPos> adjacentLeaves = new HashSet<>();
        for (BlockPos logPos : connectedLogs) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        BlockPos candidate = logPos.offset(dx, dy, dz);
                        if (level.getBlockState(candidate).is(expectedLeafBlock)) {
                            adjacentLeaves.add(candidate);
                        }
                    }
                }
            }
        }

        return adjacentLeaves;
    }

    private static BlockPos findBaseAnchor(ServerLevel level, BlockPos basePos) {
        Block rootBlock = level.getBlockState(basePos).getBlock();
        if (resolveWideTreeType(rootBlock) == null) {
            return basePos;
        }

        for (int offsetX = -1; offsetX <= 0; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 0; offsetZ++) {
                BlockPos anchor = basePos.offset(offsetX, 0, offsetZ);
                List<BlockPos> wideBase = findExactWideBase(level, anchor, rootBlock);
                if (!wideBase.isEmpty() && wideBase.contains(basePos)) {
                    return anchor;
                }
            }
        }

        return basePos;
    }

    private static boolean hasOnlyLeafBridge(ServerLevel level, BlockPos logPos, Set<BlockPos> connectedLogs, Block expectedLeafBlock) {
        boolean touchesConnectedLeaf = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    BlockPos neighbor = logPos.offset(dx, dy, dz);
                    if (connectedLogs.contains(neighbor)) {
                        return false;
                    }

                    BlockState neighborState = level.getBlockState(neighbor);
                    if (neighborState.is(expectedLeafBlock)) {
                        touchesConnectedLeaf = true;
                    }
                }
            }
        }

        return touchesConnectedLeaf;
    }

    private static boolean hasGroundConnectionOutsideRoot(ServerLevel level, Set<BlockPos> connectedLogs, Set<BlockPos> rootPositions, Block logBlock, BlockPos anchor) {
        Set<BlockPos> visited = new HashSet<>(connectedLogs);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(connectedLogs);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!rootPositions.contains(current) && touchesGround(level, current)) {
                return true;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        BlockPos next = current.offset(dx, dy, dz);
                        if (visited.contains(next) || !isWithinGroundConnectionBounds(next, anchor)) {
                            continue;
                        }

                        if (!level.getBlockState(next).is(logBlock)) {
                            continue;
                        }

                        visited.add(next);
                        queue.addLast(next);
                    }
                }
            }
        }

        return false;
    }

    private static String findInvalidTreeNeighbor(ServerLevel level, Set<BlockPos> connectedLogs, Set<BlockPos> rootPositions, Block logBlock, boolean allowVines) {
        for (BlockPos logPos : connectedLogs) {
            for (Direction direction : Direction.values()) {
                if (direction == Direction.DOWN && rootPositions.contains(logPos)) {
                    continue;
                }

                BlockPos neighbor = logPos.relative(direction);
                if (connectedLogs.contains(neighbor)) {
                    continue;
                }

                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.isAir()) {
                    continue;
                }
                if (neighborState.canBeReplaced()) {
                    continue;
                }
                if (neighborState.is(BlockTags.FLOWERS)) {
                    continue;
                }
                if (neighborState.is(logBlock) || neighborState.is(BlockTags.LEAVES)) {
                    continue;
                }
                if (allowVines && isVine(neighborState)) {
                    continue;
                }
                return "at " + formatPos(neighbor) + " block=" + BuiltInRegistries.BLOCK.getKey(neighborState.getBlock()) + " touching log=" + formatPos(logPos) + " face=" + direction;
            }
        }

        return null;
    }

    private static boolean hasForeignLogConnection(ServerLevel level, Set<BlockPos> connectedLogs, Block logBlock, BlockPos anchor) {
        Set<BlockPos> visited = new HashSet<>(connectedLogs);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(connectedLogs);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        BlockPos next = current.offset(dx, dy, dz);
                        if (visited.contains(next) || !isWithinGroundConnectionBounds(next, anchor)) {
                            continue;
                        }

                        BlockState nextState = level.getBlockState(next);
                        if (!isLog(nextState)) {
                            continue;
                        }
                        if (!nextState.is(logBlock)) {
                            return true;
                        }

                        visited.add(next);
                        queue.addLast(next);
                    }
                }
            }
        }

        return false;
    }

    private static String formatPos(BlockPos pos) {
        return "[" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "]";
    }

    private static boolean touchesGround(ServerLevel level, BlockPos logPos) {
        BlockState belowState = level.getBlockState(logPos.below());
        return !isLog(belowState) && isValidTreeGround(level, logPos.below(), belowState);
    }

    private static boolean isWithinGroundConnectionBounds(BlockPos pos, BlockPos anchor) {
        return Math.abs(pos.getX() - anchor.getX()) <= SEARCH_RADIUS + MAX_HORIZONTAL_DRIFT
                && Math.abs(pos.getZ() - anchor.getZ()) <= SEARCH_RADIUS + MAX_HORIZONTAL_DRIFT
                && pos.getY() >= anchor.getY() - 2
                && pos.getY() - anchor.getY() <= MAX_HEIGHT;
    }

    private static boolean isWithinTreeBounds(BlockPos pos, BlockPos anchor, int extraHorizontalDrift) {
        return Math.abs(pos.getX() - anchor.getX()) <= MAX_HORIZONTAL_DRIFT + extraHorizontalDrift
                && Math.abs(pos.getZ() - anchor.getZ()) <= MAX_HORIZONTAL_DRIFT + extraHorizontalDrift
                && pos.getY() >= anchor.getY()
                && pos.getY() - anchor.getY() <= MAX_HEIGHT;
    }

    private static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private static boolean isVerticalLog(BlockState state) {
        if (!isLog(state)) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
        }
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            return state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y;
        }
        return true;
    }

    private static boolean isGroundedVerticalBaseLog(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isVerticalLog(state)) {
            return false;
        }

        BlockState belowState = level.getBlockState(pos.below());
        if (isLog(belowState)) {
            return false;
        }

        return isValidTreeGround(level, pos.below(), belowState);
    }

    private static boolean hasValidSaplingGround(ServerLevel level, List<BlockPos> rootPositions, Item saplingItem) {
        if (!(saplingItem instanceof BlockItem saplingBlockItem)) {
            return false;
        }

        BlockState saplingState = saplingBlockItem.getBlock().defaultBlockState();
        for (BlockPos rootPos : rootPositions) {
            if (!saplingState.canSurvive(level, rootPos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidTreeGround(ServerLevel level, BlockPos groundPos, BlockState groundState) {
        return groundState.is(BlockTags.DIRT)
                || groundState.is(Blocks.GRASS_BLOCK)
                || groundState.is(Blocks.PODZOL)
                || groundState.is(Blocks.MYCELIUM)
                || groundState.is(Blocks.ROOTED_DIRT)
                || groundState.is(Blocks.MOSS_BLOCK)
                || groundState.is(Blocks.MUD)
                || groundState.isFaceSturdy(level, groundPos, Direction.UP);
    }

    private static boolean isLeaf(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    private static boolean isVine(BlockState state) {
        return state.is(Blocks.VINE);
    }

    private static Block resolveLeafBlock(Block block) {
        if (block == Blocks.OAK_LOG) {
            return Blocks.OAK_LEAVES;
        }
        if (block == Blocks.SPRUCE_LOG) {
            return Blocks.SPRUCE_LEAVES;
        }
        if (block == Blocks.BIRCH_LOG) {
            return Blocks.BIRCH_LEAVES;
        }
        if (block == Blocks.JUNGLE_LOG) {
            return Blocks.JUNGLE_LEAVES;
        }
        if (block == Blocks.ACACIA_LOG) {
            return Blocks.ACACIA_LEAVES;
        }
        if (block == Blocks.DARK_OAK_LOG) {
            return Blocks.DARK_OAK_LEAVES;
        }
        if (block == Blocks.MANGROVE_LOG) {
            return Blocks.MANGROVE_LEAVES;
        }
        if (block == Blocks.CHERRY_LOG) {
            return Blocks.CHERRY_LEAVES;
        }
        return null;
    }

    private static int calculateTrunkHeight(List<BlockPos> trunkPositions) {
        if (trunkPositions.isEmpty()) {
            return 0;
        }
        int minY = trunkPositions.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxY = trunkPositions.stream().mapToInt(BlockPos::getY).max().orElse(0);
        return maxY - minY + 1;
    }

    private static boolean validateWideTree(ServerLevel level, List<BlockPos> rootPositions, Set<BlockPos> connectedLogs, List<BlockPos> trunkPositions, Block rootBlock, Block expectedLeafBlock, WideTreeType wideTreeType) {
        BlockPos anchor = rootPositions.get(0);
        int minX = anchor.getX();
        int maxX = anchor.getX() + 1;
        int minZ = anchor.getZ();
        int maxZ = anchor.getZ() + 1;
        int baseY = anchor.getY();

        if (wideTreeType == WideTreeType.SPRUCE) {
            for (BlockPos trunkPos : trunkPositions) {
                if (trunkPos.getX() < minX || trunkPos.getX() > maxX || trunkPos.getZ() < minZ || trunkPos.getZ() > maxZ) {
                    return false;
                }
            }
        }
        for (BlockPos logPos : connectedLogs) {
            if (!level.getBlockState(logPos).is(rootBlock)) {
                return false;
            }
        }

        Set<BlockPos> trunkSet = new HashSet<>(trunkPositions);
        for (BlockPos trunkPos : trunkPositions) {
            List<Direction> directions = trunkPos.getY() == baseY
                    ? List.of(Direction.UP)
                    : List.of(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
            for (Direction direction : directions) {
                BlockPos neighbor = trunkPos.relative(direction);
                if (trunkSet.contains(neighbor)) {
                    continue;
                }
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.isAir() || neighborState.getFluidState().is(FluidTags.WATER)) {
                    continue;
                }
                if (wideTreeType == WideTreeType.SPRUCE) {
                    if (neighborState.is(rootBlock) && connectedLogs.contains(neighbor)) {
                        continue;
                    }
                    if (neighborState.is(expectedLeafBlock)) {
                        continue;
                    }
                    continue;
                }
                if (wideTreeType == WideTreeType.JUNGLE) {
                    if (neighborState.is(expectedLeafBlock) || isVine(neighborState) || neighborState.is(rootBlock)) {
                        continue;
                    }
                    return false;
                }
                if (wideTreeType == WideTreeType.DARK_OAK) {
                    if (neighborState.is(expectedLeafBlock) || neighborState.is(rootBlock)) {
                        continue;
                    }
                    return false;
                }
            }
        }

        return true;
    }

    private static int calculateMaxLogs(Block rootBlock, int rootWidth) {
        int baseLimit = Math.max(MAX_LOGS_PER_TRUNK_BLOCK, rootWidth * MAX_LOGS_PER_TRUNK_BLOCK);
        if (rootWidth == 1 && rootBlock == Blocks.OAK_LOG) {
            return Math.max(baseLimit, OAK_MAX_LOGS);
        }
        if (rootWidth >= 4) {
            if (rootBlock == Blocks.SPRUCE_LOG) {
                return Math.max(baseLimit, HUGE_SPRUCE_MAX_LOGS);
            }
            return Math.max(baseLimit, WIDE_TREE_MAX_LOGS);
        }
        return baseLimit;
    }

    private static WideTreeType resolveWideTreeType(Block block) {
        if (block == Blocks.SPRUCE_LOG) {
            return WideTreeType.SPRUCE;
        }
        if (block == Blocks.JUNGLE_LOG) {
            return WideTreeType.JUNGLE;
        }
        if (block == Blocks.DARK_OAK_LOG) {
            return WideTreeType.DARK_OAK;
        }
        return null;
    }

    private static Item resolveSaplingItem(Block block) {
        if (block == Blocks.OAK_LOG) {
            return Items.OAK_SAPLING;
        }
        if (block == Blocks.SPRUCE_LOG) {
            return Items.SPRUCE_SAPLING;
        }
        if (block == Blocks.BIRCH_LOG) {
            return Items.BIRCH_SAPLING;
        }
        if (block == Blocks.JUNGLE_LOG) {
            return Items.JUNGLE_SAPLING;
        }
        if (block == Blocks.ACACIA_LOG) {
            return Items.ACACIA_SAPLING;
        }
        if (block == Blocks.DARK_OAK_LOG) {
            return Items.DARK_OAK_SAPLING;
        }
        if (block == Blocks.MANGROVE_LOG) {
            return Items.MANGROVE_PROPAGULE;
        }
        if (block == Blocks.CHERRY_LOG) {
            return Items.CHERRY_SAPLING;
        }
        return Items.AIR;
    }

    public record CandidateTree(BlockPos rootPos, List<BlockPos> rootPositions, List<BlockPos> trunkPositions, List<BlockPos> logPositions, List<BlockPos> leafPositions, String saplingItemId) {
    }

    public record DetectionScan(List<CandidateTree> candidateTrees, List<BlockPos> debugRootBlocks, List<String> rejectionMessages) {
    }

    private record AnalysisResult(CandidateTree candidate, String rejectionReason) {
        private static AnalysisResult accept(CandidateTree candidate) {
            return new AnalysisResult(candidate, null);
        }

        private static AnalysisResult reject(String reason) {
            return new AnalysisResult(null, reason);
        }
    }

    private enum WideTreeType {
        SPRUCE,
        JUNGLE,
        DARK_OAK
    }
}
