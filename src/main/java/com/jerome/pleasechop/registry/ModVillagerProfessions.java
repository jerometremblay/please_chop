package com.jerome.pleasechop.registry;

import com.google.common.collect.ImmutableSet;
import com.jerome.pleasechop.PleaseChopMod;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModVillagerProfessions {
    private static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, PleaseChopMod.MOD_ID);
    public static final ResourceKey<VillagerProfession> LUMBERJACK_KEY =
            ResourceKey.create(Registries.VILLAGER_PROFESSION, Objects.requireNonNull(Identifier.tryBuild(PleaseChopMod.MOD_ID, "lumberjack")));
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
            Items.OAK_LEAVES,
            Items.SPRUCE_LEAVES,
            Items.BIRCH_LEAVES,
            Items.JUNGLE_LEAVES,
            Items.ACACIA_LEAVES,
            Items.DARK_OAK_LEAVES,
            Items.MANGROVE_LEAVES,
            Items.CHERRY_LEAVES,
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
                    Component.translatable("entity.minecraft.villager.pleasechop.lumberjack"),
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
