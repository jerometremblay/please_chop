package com.jerome.pleasechop.block;

public enum WorkstationWoodType {
    OAK("oak", "minecraft:block/oak_log", "minecraft:block/oak_log_top"),
    SPRUCE("spruce", "minecraft:block/spruce_log", "minecraft:block/spruce_log_top"),
    BIRCH("birch", "minecraft:block/birch_log", "minecraft:block/birch_log_top"),
    JUNGLE("jungle", "minecraft:block/jungle_log", "minecraft:block/jungle_log_top"),
    ACACIA("acacia", "minecraft:block/acacia_log", "minecraft:block/acacia_log_top"),
    DARK_OAK("dark_oak", "minecraft:block/dark_oak_log", "minecraft:block/dark_oak_log_top"),
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
}
