package com.jerome.pleasechop.registry;

import com.google.common.collect.ImmutableSet;
import com.jerome.pleasechop.PleaseChopMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModVillagerProfessions {
    private static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, PleaseChopMod.MOD_ID);
    private static final ImmutableSet<net.minecraft.world.item.Item> LUMBERJACK_REQUESTED_ITEMS = ImmutableSet.of(
            Items.OAK_LOG,
            Items.SPRUCE_LOG,
            Items.BIRCH_LOG,
            Items.JUNGLE_LOG,
            Items.ACACIA_LOG,
            Items.DARK_OAK_LOG,
            Items.MANGROVE_LOG,
            Items.CHERRY_LOG,
            Items.CRIMSON_STEM,
            Items.WARPED_STEM,
            Items.STICK,
            Items.APPLE,
            Items.OAK_SAPLING,
            Items.SPRUCE_SAPLING,
            Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING,
            Items.ACACIA_SAPLING,
            Items.DARK_OAK_SAPLING,
            Items.MANGROVE_PROPAGULE,
            Items.CHERRY_SAPLING
    );

    public static final DeferredHolder<VillagerProfession, VillagerProfession> LUMBERJACK = PROFESSIONS.register("lumberjack",
            () -> new VillagerProfession(
                    "lumberjack",
                    holder -> holder.is(ModPoiTypes.LUMBERJACK_KEY),
                    holder -> holder.is(ModPoiTypes.LUMBERJACK_KEY),
                    LUMBERJACK_REQUESTED_ITEMS,
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_TOOLSMITH
            ));

    private ModVillagerProfessions() {
    }

    public static void register(IEventBus modEventBus) {
        PROFESSIONS.register(modEventBus);
    }
}
