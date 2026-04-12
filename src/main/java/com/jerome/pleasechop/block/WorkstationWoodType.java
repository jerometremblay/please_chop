package com.jerome.pleasechop.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public enum WorkstationWoodType {
    OAK("oak", "minecraft:block/oak_log", "minecraft:block/oak_log_top"),
    SPRUCE("spruce", "minecraft:block/spruce_log", "minecraft:block/spruce_log_top"),
    BIRCH("birch", "minecraft:block/birch_log", "minecraft:block/birch_log_top"),
    JUNGLE("jungle", "minecraft:block/jungle_log", "minecraft:block/jungle_log_top"),
    ACACIA("acacia", "minecraft:block/acacia_log", "minecraft:block/acacia_log_top"),
    DARK_OAK("dark_oak", "minecraft:block/dark_oak_log", "minecraft:block/dark_oak_log_top"),
    PALE_OAK("pale_oak", "minecraft:block/pale_oak_log", "minecraft:block/pale_oak_log_top"),
    MANGROVE("mangrove", "minecraft:block/mangrove_log", "minecraft:block/mangrove_log_top"),
    CHERRY("cherry", "minecraft:block/cherry_log", "minecraft:block/cherry_log_top");

    private final String serializedName;
    private final String sideTexture;
    private final String endTexture;

    WorkstationWoodType(String serializedName, String sideTexture, String endTexture) {
        this.serializedName = serializedName;
        this.sideTexture = sideTexture;
        this.endTexture = endTexture;
    }

    public String serializedName() {
        return serializedName;
    }

    public String sideTexture() {
        return sideTexture;
    }

    public String endTexture() {
        return endTexture;
    }

    public static WorkstationWoodType fromSerializedName(String name) {
        for (WorkstationWoodType type : values()) {
            if (type.serializedName.equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static WorkstationWoodType fromWorkstationBlock(Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null) {
            return null;
        }
        String path = key.getPath();
        for (WorkstationWoodType type : values()) {
            if (path.equals(type.serializedName + "_lumberjack_workstation")) {
                return type;
            }
        }
        return null;
    }

    public Item logItem() {
        return switch (this) {
            case OAK -> Items.OAK_LOG;
            case SPRUCE -> Items.SPRUCE_LOG;
            case BIRCH -> Items.BIRCH_LOG;
            case JUNGLE -> Items.JUNGLE_LOG;
            case ACACIA -> Items.ACACIA_LOG;
            case DARK_OAK -> Items.DARK_OAK_LOG;
            case PALE_OAK -> Items.PALE_OAK_LOG;
            case MANGROVE -> Items.MANGROVE_LOG;
            case CHERRY -> Items.CHERRY_LOG;
        };
    }

    public Item strippedLogItem() {
        return switch (this) {
            case OAK -> Items.STRIPPED_OAK_LOG;
            case SPRUCE -> Items.STRIPPED_SPRUCE_LOG;
            case BIRCH -> Items.STRIPPED_BIRCH_LOG;
            case JUNGLE -> Items.STRIPPED_JUNGLE_LOG;
            case ACACIA -> Items.STRIPPED_ACACIA_LOG;
            case DARK_OAK -> Items.STRIPPED_DARK_OAK_LOG;
            case PALE_OAK -> Items.STRIPPED_PALE_OAK_LOG;
            case MANGROVE -> Items.STRIPPED_MANGROVE_LOG;
            case CHERRY -> Items.STRIPPED_CHERRY_LOG;
        };
    }

    public Item leavesItem() {
        return switch (this) {
            case OAK -> Items.OAK_LEAVES;
            case SPRUCE -> Items.SPRUCE_LEAVES;
            case BIRCH -> Items.BIRCH_LEAVES;
            case JUNGLE -> Items.JUNGLE_LEAVES;
            case ACACIA -> Items.ACACIA_LEAVES;
            case DARK_OAK -> Items.DARK_OAK_LEAVES;
            case PALE_OAK -> Items.PALE_OAK_LEAVES;
            case MANGROVE -> Items.MANGROVE_LEAVES;
            case CHERRY -> Items.CHERRY_LEAVES;
        };
    }

    public Item saplingItem() {
        return switch (this) {
            case OAK -> Items.OAK_SAPLING;
            case SPRUCE -> Items.SPRUCE_SAPLING;
            case BIRCH -> Items.BIRCH_SAPLING;
            case JUNGLE -> Items.JUNGLE_SAPLING;
            case ACACIA -> Items.ACACIA_SAPLING;
            case DARK_OAK -> Items.DARK_OAK_SAPLING;
            case PALE_OAK -> Items.PALE_OAK_SAPLING;
            case MANGROVE -> Items.MANGROVE_PROPAGULE;
            case CHERRY -> Items.CHERRY_SAPLING;
        };
    }

    public Item specialResourceItem() {
        return switch (this) {
            case JUNGLE -> Items.COCOA_BEANS;
            case PALE_OAK -> Items.RESIN_CLUMP;
            default -> null;
        };
    }
}
