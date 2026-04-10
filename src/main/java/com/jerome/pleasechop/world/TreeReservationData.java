package com.jerome.pleasechop.world;

import com.jerome.pleasechop.tree.TreeCandidateDetector.CandidateTree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class TreeReservationData extends SavedData {
    private static final String DATA_NAME = "pleasechop_tree_reservations";
    private static final String RESERVATIONS_KEY = "reservations";
    private static final String TREE_ROOTS_KEY = "tree_roots";
    private static final String OWNER_POS_KEY = "owner_pos";
    private static final String EXPIRES_AT_KEY = "expires_at";

    private final Map<String, Reservation> reservations = new HashMap<>();

    public static TreeReservationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(
                TreeReservationData::new,
                TreeReservationData::load
        ), DATA_NAME);
    }

    private static TreeReservationData load(CompoundTag tag, HolderLookup.Provider registries) {
        TreeReservationData data = new TreeReservationData();
        ListTag reservationsTag = tag.getList(RESERVATIONS_KEY, Tag.TAG_COMPOUND);
        for (Tag entryTag : reservationsTag) {
            if (!(entryTag instanceof CompoundTag reservationTag)) {
                continue;
            }

            long[] rootValues = reservationTag.getLongArray(TREE_ROOTS_KEY);
            if (rootValues.length == 0) {
                continue;
            }

            String key = treeKey(rootValues);
            data.reservations.put(key, new Reservation(
                    reservationTag.getLong(OWNER_POS_KEY),
                    reservationTag.getLong(EXPIRES_AT_KEY)
            ));
        }
        return data;
    }

    public boolean tryReserve(CandidateTree tree, BlockPos ownerPos, long gameTime, long durationTicks) {
        pruneExpired(gameTime);
        String key = treeKey(tree);
        Reservation existing = reservations.get(key);
        long ownerPosLong = ownerPos.asLong();
        if (existing != null && existing.ownerPos != ownerPosLong && existing.expiresAt > gameTime) {
            return false;
        }

        reservations.put(key, new Reservation(ownerPosLong, gameTime + durationTicks));
        setDirty();
        return true;
    }

    public void refresh(CandidateTree tree, BlockPos ownerPos, long gameTime, long durationTicks) {
        String key = treeKey(tree);
        Reservation existing = reservations.get(key);
        long ownerPosLong = ownerPos.asLong();
        if (existing == null || existing.ownerPos != ownerPosLong) {
            reservations.put(key, new Reservation(ownerPosLong, gameTime + durationTicks));
        } else {
            existing.expiresAt = gameTime + durationTicks;
        }
        setDirty();
    }

    public void release(CandidateTree tree, BlockPos ownerPos) {
        String key = treeKey(tree);
        Reservation existing = reservations.get(key);
        if (existing != null && existing.ownerPos == ownerPos.asLong()) {
            reservations.remove(key);
            setDirty();
        }
    }

    public boolean isReservedByOther(CandidateTree tree, BlockPos ownerPos, long gameTime) {
        pruneExpired(gameTime);
        Reservation existing = reservations.get(treeKey(tree));
        return existing != null && existing.ownerPos != ownerPos.asLong() && existing.expiresAt > gameTime;
    }

    private void pruneExpired(long gameTime) {
        if (reservations.entrySet().removeIf(entry -> entry.getValue().expiresAt <= gameTime)) {
            setDirty();
        }
    }

    private static String treeKey(CandidateTree tree) {
        long[] roots = new long[tree.rootPositions().size()];
        for (int i = 0; i < tree.rootPositions().size(); i++) {
            roots[i] = tree.rootPositions().get(i).asLong();
        }
        return treeKey(roots);
    }

    private static String treeKey(long[] roots) {
        List<Long> values = new ArrayList<>(roots.length);
        for (long root : roots) {
            values.add(root);
        }
        values.sort(Comparator.naturalOrder());
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag reservationsTag = new ListTag();
        reservations.forEach((key, reservation) -> {
            CompoundTag reservationTag = new CompoundTag();
            String[] parts = key.split(",");
            long[] roots = new long[parts.length];
            for (int i = 0; i < parts.length; i++) {
                roots[i] = Long.parseLong(parts[i]);
            }
            reservationTag.putLongArray(TREE_ROOTS_KEY, roots);
            reservationTag.putLong(OWNER_POS_KEY, reservation.ownerPos);
            reservationTag.putLong(EXPIRES_AT_KEY, reservation.expiresAt);
            reservationsTag.add(reservationTag);
        });
        tag.put(RESERVATIONS_KEY, reservationsTag);
        return tag;
    }

    private static final class Reservation {
        private final long ownerPos;
        private long expiresAt;

        private Reservation(long ownerPos, long expiresAt) {
            this.ownerPos = ownerPos;
            this.expiresAt = expiresAt;
        }
    }
}
