package com.jerome.pleasechop.client;

import com.jerome.pleasechop.PleaseChopMod;
import com.jerome.pleasechop.registry.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = PleaseChopMod.MOD_ID, value = Dist.CLIENT)
public final class PleaseChopClientEvents {
    private PleaseChopClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LUMBERJACK_WORKSTATION.get(), LumberjackWorkstationRenderer::new);
    }
}
