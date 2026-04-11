package com.jerome.pleasechop.trade;

import com.jerome.pleasechop.PleaseChopMod;
import com.jerome.pleasechop.block.WorkstationWoodType;
import com.jerome.pleasechop.registry.ModVillagerProfessions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

public final class LumberjackTradeManager {
    private static final String SPECIALIZATION_TAG = PleaseChopMod.MOD_ID + ":wood_specialization";

    private LumberjackTradeManager() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LumberjackTradeManager::onVillagerTrades);
    }

    private static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagerProfessions.LUMBERJACK_KEY) {
            return;
        }

        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        trades.get(1).add(new SpecializedSellListing(type -> type.logItem(), 1, 8, 16, 1));
        trades.get(1).add(new SpecializedSellListing(type -> type.saplingItem(), 1, 3, 12, 1));
        trades.get(1).add(new SpecializedOptionalSellListing(type -> type == WorkstationWoodType.OAK ? Items.APPLE : null, 1, 4, 12, 1));
        trades.get(1).add(new AxeBuyListing(Items.STONE_AXE, 2, 12, 2));
        trades.get(1).add(new VillagerTrades.EmeraldForItems(Items.BREAD, 8, 12, 2));

        trades.get(2).add(new SpecializedSellListing(type -> type.strippedLogItem(), 1, 6, 12, 5));
        trades.get(2).add(new SpecializedSellListing(type -> type.leavesItem(), 1, 8, 12, 5));
        trades.get(2).add(new VillagerTrades.EmeraldForItems(Items.CARROT, 12, 12, 10));
        trades.get(2).add(new VillagerTrades.EmeraldForItems(Items.BAKED_POTATO, 10, 12, 10));

        trades.get(3).add(new VillagerTrades.ItemsForEmeralds(Items.STICK, 1, 12, 12, 10));
        trades.get(3).add(new SpecializedOptionalSellListing(WorkstationWoodType::specialResourceItem, 1, 4, 12, 10));
        trades.get(3).add(new AxeBuyListing(Items.IRON_AXE, 4, 12, 20));
        trades.get(3).add(new VillagerTrades.EmeraldForItems(Items.PUMPKIN_PIE, 4, 12, 20));

        trades.get(4).add(new RareListing(0.15F, new VillagerTrades.EnchantedItemForEmeralds(Items.IRON_AXE, 8, 3, 15, 0.2F)));

        trades.get(5).add(new RareListing(0.08F, new VillagerTrades.EnchantedItemForEmeralds(Items.DIAMOND_AXE, 18, 3, 30, 0.2F)));
    }

    private static WorkstationWoodType getOrResolveSpecialization(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (jobSite.isEmpty()) {
            data.remove(SPECIALIZATION_TAG);
            return null;
        }

        Level currentLevel = villager.level();
        if (!(currentLevel instanceof ServerLevel serverLevel)) {
            return null;
        }

        ServerLevel jobSiteLevel = serverLevel.getServer().getLevel(jobSite.get().dimension());
        if (jobSiteLevel == null) {
            return null;
        }

        WorkstationWoodType resolved = WorkstationWoodType.fromWorkstationBlock(jobSiteLevel.getBlockState(jobSite.get().pos()).getBlock());
        if (resolved != null) {
            WorkstationWoodType saved = data.contains(SPECIALIZATION_TAG)
                    ? WorkstationWoodType.fromSerializedName(data.getStringOr(SPECIALIZATION_TAG, ""))
                    : null;
            if (saved != resolved) {
                data.putString(SPECIALIZATION_TAG, resolved.serializedName());
            }
            return resolved;
        }

        data.remove(SPECIALIZATION_TAG);
        return null;
    }

    @FunctionalInterface
    private interface WoodTypeItemResolver {
        Item resolve(WorkstationWoodType woodType);
    }

    private record SpecializedSellListing(WoodTypeItemResolver resolver, int emeraldCost, int count, int maxUses, int villagerXp)
            implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {
            if (!(trader instanceof Villager villager)) {
                return null;
            }
            WorkstationWoodType type = getOrResolveSpecialization(villager);
            if (type == null) {
                return null;
            }
            Item item = resolver.resolve(type);
            return item == null ? null : new VillagerTrades.ItemsForEmeralds(item, emeraldCost, count, maxUses, villagerXp).getOffer(level, trader, random);
        }
    }

    private record SpecializedOptionalSellListing(WoodTypeItemResolver resolver, int emeraldCost, int count, int maxUses, int villagerXp)
            implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {
            if (!(trader instanceof Villager villager)) {
                return null;
            }
            WorkstationWoodType type = getOrResolveSpecialization(villager);
            if (type == null) {
                return null;
            }
            Item item = resolver.resolve(type);
            return item == null ? null : new VillagerTrades.ItemsForEmeralds(item, emeraldCost, count, maxUses, villagerXp).getOffer(level, trader, random);
        }
    }

    private record RareListing(float chance, VillagerTrades.ItemListing delegate)
            implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {
            if (random.nextFloat() > chance) {
                return null;
            }
            return delegate.getOffer(level, trader, random);
        }
    }

    private record AxeBuyListing(Item axeItem, int emeraldAmount, int maxUses, int villagerXp)
            implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {
            return new MerchantOffer(new ItemCost(axeItem), new ItemStack(Items.EMERALD, emeraldAmount), maxUses, villagerXp, 0.05F);
        }
    }
}
