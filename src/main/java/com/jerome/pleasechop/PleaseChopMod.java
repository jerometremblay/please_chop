package com.jerome.pleasechop;

import com.jerome.pleasechop.registry.ModBlocks;
import com.jerome.pleasechop.registry.ModBlockEntities;
import com.jerome.pleasechop.registry.ModPoiTypes;
import com.jerome.pleasechop.registry.ModVillagerProfessions;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(PleaseChopMod.MOD_ID)
public final class PleaseChopMod {
    public static final String MOD_ID = "pleasechop";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PleaseChopMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModPoiTypes.register(modEventBus);
        ModVillagerProfessions.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabEntries);
        LOGGER.info("Loading {}", MOD_ID);
    }

    @SubscribeEvent
    private void addCreativeTabEntries(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            ModBlocks.WORKSTATION_ITEMS.forEach(event::accept);
        }
    }
}
