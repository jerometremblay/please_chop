package com.jerome.pleasechop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.jerome.pleasechop.block.entity.LumberjackWorkstationBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class LumberjackWorkstationRenderer implements BlockEntityRenderer<LumberjackWorkstationBlockEntity, LumberjackWorkstationRenderer.State> {
    private static final ItemStack AXE_STACK = new ItemStack(Items.IRON_AXE);

    private final ItemModelResolver itemModelResolver;

    public LumberjackWorkstationRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LumberjackWorkstationBlockEntity blockEntity, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, breakProgress);
        state.axe.clear();
        itemModelResolver.updateForTopItem(state.axe, AXE_STACK, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.98F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(42.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-128.0F));
        poseStack.scale(1.22F, 1.22F, 1.22F);
        state.axe.submit(poseStack, submitNodeCollector, state.lightCoords, 0, 0);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    public static final class State extends BlockEntityRenderState {
        private final ItemStackRenderState axe = new ItemStackRenderState();
    }
}
