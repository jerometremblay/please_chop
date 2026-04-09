package com.jerome.pleasechop.client;

import com.jerome.pleasechop.config.PleaseChopConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PleaseChopConfigScreen extends Screen {
    private final Screen parent;
    private boolean debugChat;
    private boolean debugRender;

    public PleaseChopConfigScreen(Screen parent) {
        super(Component.literal("Please Chop Settings"));
        this.parent = parent;
        this.debugChat = PleaseChopConfig.debugChatEnabled();
        this.debugRender = PleaseChopConfig.debugRenderEnabled();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 30;

        this.addRenderableWidget(Button.builder(debugChatLabel(), button -> {
            debugChat = !debugChat;
            button.setMessage(debugChatLabel());
        }).bounds(centerX - 100, startY, 200, 20).build());

        this.addRenderableWidget(Button.builder(debugRenderLabel(), button -> {
            debugRender = !debugRender;
            button.setMessage(debugRenderLabel());
        }).bounds(centerX - 100, startY + 24, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            PleaseChopConfig.setDebugChatEnabled(debugChat);
            PleaseChopConfig.setDebugRenderEnabled(debugRender);
            PleaseChopConfig.save();
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 100, startY + 60, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.minecraft.setScreen(parent))
                .bounds(centerX + 5, startY + 60, 95, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private Component debugChatLabel() {
        return Component.literal("Debug Chat: " + onOff(debugChat));
    }

    private Component debugRenderLabel() {
        return Component.literal("Debug Render: " + onOff(debugRender));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
