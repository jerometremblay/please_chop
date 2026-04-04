package com.jerome.pleasechop.registry;

import com.jerome.pleasechop.PleaseChopMod;
import com.jerome.pleasechop.block.LumberjackWorkstationBlock;
import com.jerome.pleasechop.block.WorkstationWoodType;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PleaseChopMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PleaseChopMod.MOD_ID);

    public static final DeferredBlock<Block> OAK_LUMBERJACK_WORKSTATION = registerWorkstation(WorkstationWoodType.OAK);
    public static final DeferredBlock<Block> SPRUCE_LUMBERJACK_WORKSTATION = registerWorkstation(WorkstationWoodType.SPRUCE);
    public static final DeferredBlock<Block> BIRCH_LUMBERJACK_WORKSTATION = registerWorkstation(WorkstationWoodType.BIRCH);
    public static final DeferredBlock<Block> JUNGLE_LUMBERJACK_WORKSTATION = registerWorkstation(WorkstationWoodType.JUNGLE);
    public static final DeferredBlock<Block> ACACIA_LUMBERJACK_WORKSTATION = registerWorkstation(WorkstationWoodType.ACACIA);
    public static final DeferredBlock<Block> DARK_OAK_LUMBERJACK_WORKSTATION = registerWorkstation(WorkstationWoodType.DARK_OAK);
    public static final DeferredBlock<Block> MANGROVE_LUMBERJACK_WORKSTATION = registerWorkstation(WorkstationWoodType.MANGROVE);
    public static final DeferredBlock<Block> CHERRY_LUMBERJACK_WORKSTATION = registerWorkstation(WorkstationWoodType.CHERRY);

    public static final DeferredItem<BlockItem> OAK_LUMBERJACK_WORKSTATION_ITEM = registerWorkstationItem(WorkstationWoodType.OAK, OAK_LUMBERJACK_WORKSTATION);
    public static final DeferredItem<BlockItem> SPRUCE_LUMBERJACK_WORKSTATION_ITEM = registerWorkstationItem(WorkstationWoodType.SPRUCE, SPRUCE_LUMBERJACK_WORKSTATION);
    public static final DeferredItem<BlockItem> BIRCH_LUMBERJACK_WORKSTATION_ITEM = registerWorkstationItem(WorkstationWoodType.BIRCH, BIRCH_LUMBERJACK_WORKSTATION);
    public static final DeferredItem<BlockItem> JUNGLE_LUMBERJACK_WORKSTATION_ITEM = registerWorkstationItem(WorkstationWoodType.JUNGLE, JUNGLE_LUMBERJACK_WORKSTATION);
    public static final DeferredItem<BlockItem> ACACIA_LUMBERJACK_WORKSTATION_ITEM = registerWorkstationItem(WorkstationWoodType.ACACIA, ACACIA_LUMBERJACK_WORKSTATION);
    public static final DeferredItem<BlockItem> DARK_OAK_LUMBERJACK_WORKSTATION_ITEM = registerWorkstationItem(WorkstationWoodType.DARK_OAK, DARK_OAK_LUMBERJACK_WORKSTATION);
    public static final DeferredItem<BlockItem> MANGROVE_LUMBERJACK_WORKSTATION_ITEM = registerWorkstationItem(WorkstationWoodType.MANGROVE, MANGROVE_LUMBERJACK_WORKSTATION);
    public static final DeferredItem<BlockItem> CHERRY_LUMBERJACK_WORKSTATION_ITEM = registerWorkstationItem(WorkstationWoodType.CHERRY, CHERRY_LUMBERJACK_WORKSTATION);

    public static final List<DeferredBlock<Block>> WORKSTATION_BLOCKS = List.of(
            OAK_LUMBERJACK_WORKSTATION,
            SPRUCE_LUMBERJACK_WORKSTATION,
            BIRCH_LUMBERJACK_WORKSTATION,
            JUNGLE_LUMBERJACK_WORKSTATION,
            ACACIA_LUMBERJACK_WORKSTATION,
            DARK_OAK_LUMBERJACK_WORKSTATION,
            MANGROVE_LUMBERJACK_WORKSTATION,
            CHERRY_LUMBERJACK_WORKSTATION
    );

    public static final List<DeferredItem<BlockItem>> WORKSTATION_ITEMS = List.of(
            OAK_LUMBERJACK_WORKSTATION_ITEM,
            SPRUCE_LUMBERJACK_WORKSTATION_ITEM,
            BIRCH_LUMBERJACK_WORKSTATION_ITEM,
            JUNGLE_LUMBERJACK_WORKSTATION_ITEM,
            ACACIA_LUMBERJACK_WORKSTATION_ITEM,
            DARK_OAK_LUMBERJACK_WORKSTATION_ITEM,
            MANGROVE_LUMBERJACK_WORKSTATION_ITEM,
            CHERRY_LUMBERJACK_WORKSTATION_ITEM
    );

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    private static DeferredBlock<Block> registerWorkstation(WorkstationWoodType woodType) {
        return BLOCKS.register(woodType.serializedName() + "_lumberjack_workstation",
                () -> new LumberjackWorkstationBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(2.0F)
                        .sound(SoundType.WOOD)));
    }

    private static DeferredItem<BlockItem> registerWorkstationItem(WorkstationWoodType woodType, DeferredBlock<Block> block) {
        return ITEMS.register(woodType.serializedName() + "_lumberjack_workstation",
                () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
