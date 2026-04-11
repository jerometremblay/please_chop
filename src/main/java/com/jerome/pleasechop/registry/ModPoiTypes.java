package com.jerome.pleasechop.registry;

import com.google.common.collect.ImmutableSet;
import com.jerome.pleasechop.PleaseChopMod;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModPoiTypes {
    private static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, PleaseChopMod.MOD_ID);

    public static final ResourceKey<PoiType> LUMBERJACK_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Objects.requireNonNull(Identifier.tryBuild(PleaseChopMod.MOD_ID, "lumberjack")));

    public static final DeferredHolder<PoiType, PoiType> LUMBERJACK = POI_TYPES.register("lumberjack",
            () -> new PoiType(collectWorkstationStates(), 1, 1));

    private ModPoiTypes() {
    }

    public static void register(IEventBus modEventBus) {
        POI_TYPES.register(modEventBus);
    }

    private static Set<BlockState> collectWorkstationStates() {
        ImmutableSet.Builder<BlockState> builder = ImmutableSet.builder();
        ModBlocks.WORKSTATION_BLOCKS.forEach(block -> builder.addAll(block.get().getStateDefinition().getPossibleStates()));
        return builder.build();
    }
}
