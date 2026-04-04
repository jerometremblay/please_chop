package com.jerome.pleasechop.registry;

import com.jerome.pleasechop.PleaseChopMod;
import com.jerome.pleasechop.block.entity.LumberjackWorkstationBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PleaseChopMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LumberjackWorkstationBlockEntity>> LUMBERJACK_WORKSTATION =
            BLOCK_ENTITY_TYPES.register("lumberjack_workstation",
                    () -> BlockEntityType.Builder.of(
                            LumberjackWorkstationBlockEntity::new,
                            ModBlocks.WORKSTATION_BLOCKS.stream().map(block -> (Block) block.get()).toArray(Block[]::new)
                    ).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
