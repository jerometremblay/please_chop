package com.jerome.pleasechop.registry;

import com.jerome.pleasechop.PleaseChopMod;
import com.jerome.pleasechop.block.entity.LumberjackWorkstationBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PleaseChopMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LumberjackWorkstationBlockEntity>> LUMBERJACK_WORKSTATION =
            BLOCK_ENTITY_TYPES.register("lumberjack_workstation",
                    () -> new BlockEntityType<>(LumberjackWorkstationBlockEntity::new,
                            Set.copyOf(ModBlocks.WORKSTATION_BLOCKS.stream().map(DeferredHolder::get).toList())));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
