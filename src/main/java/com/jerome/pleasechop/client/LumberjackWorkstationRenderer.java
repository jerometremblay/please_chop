package com.jerome.pleasechop.client;

import com.jerome.pleasechop.config.PleaseChopConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.jerome.pleasechop.block.entity.LumberjackWorkstationBlockEntity;
import com.jerome.pleasechop.tree.TreeCandidateDetector.CandidateTree;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemDisplayContext;

public final class LumberjackWorkstationRenderer implements BlockEntityRenderer<LumberjackWorkstationBlockEntity> {
    private final ItemRenderer itemRenderer;
    private static final ItemStack AXE_STACK = new ItemStack(Items.IRON_AXE);

    public LumberjackWorkstationRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(LumberjackWorkstationBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.9F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.scale(1.48F, 1.48F, 1.48F);
        this.itemRenderer.renderStatic(AXE_STACK, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight, packedOverlay, poseStack, buffer, blockEntity.getLevel(), 0);
        poseStack.popPose();

        if (!PleaseChopConfig.debugRenderEnabled()) {
            return;
        }

        for (BlockPos pos : blockEntity.getDebugRootBlocks()) {
            renderSingleLineBox(blockEntity, poseStack, buffer, pos, -0.10D, 1.0F, 0.45F, 0.15F);
        }

        for (BlockPos pos : blockEntity.getPendingPlantingRootBlocks()) {
            renderSingleLineBox(blockEntity, poseStack, buffer, pos, -0.14D, 0.25F, 0.55F, 1.0F);
        }

        for (CandidateTree tree : blockEntity.getHighlightedTrees()) {
            float[] color = colorFromRoot(tree.rootPos());
            for (BlockPos pos : tree.logPositions()) {
                if (blockEntity.getLevel() == null || !blockEntity.getLevel().getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS)) {
                    continue;
                }
                if (tree.rootPositions().contains(pos)) {
                    continue;
                }
                renderSingleLineBox(blockEntity, poseStack, buffer, pos, -0.08D, color[0], color[1], color[2]);
            }
            for (BlockPos pos : tree.rootPositions()) {
                if (blockEntity.getLevel() == null || !blockEntity.getLevel().getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS)) {
                    continue;
                }
                renderSingleLineBox(blockEntity, poseStack, buffer, pos, -0.12D, 1.0F, 1.0F, 0.2F);
            }
        }

        BlockPos activeStandPos = blockEntity.getActiveTreeStandPos();
        if (activeStandPos != null) {
            if (blockEntity.hasReachedActiveTreeStandPos()) {
                renderMarkerColumn(blockEntity, poseStack, buffer, activeStandPos, -0.18D, 0.65F, 1.0F, 0.65F);
            } else {
                renderMarkerColumn(blockEntity, poseStack, buffer, activeStandPos, -0.18D, 0.65F, 0.9F, 1.0F);
            }
        }
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static void renderSingleLineBox(LumberjackWorkstationBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, BlockPos pos, double inset, float red, float green, float blue) {
        double minX = pos.getX() - blockEntity.getBlockPos().getX() + inset;
        double minY = pos.getY() - blockEntity.getBlockPos().getY() + inset;
        double minZ = pos.getZ() - blockEntity.getBlockPos().getZ() + inset;
        double maxX = pos.getX() - blockEntity.getBlockPos().getX() + 1.0D - inset;
        double maxY = pos.getY() - blockEntity.getBlockPos().getY() + 1.0D - inset;
        double maxZ = pos.getZ() - blockEntity.getBlockPos().getZ() + 1.0D - inset;
        LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, 1.0F);
    }

    private static void renderMarkerColumn(LumberjackWorkstationBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, BlockPos pos, double inset, float red, float green, float blue) {
        double minX = pos.getX() - blockEntity.getBlockPos().getX() + inset;
        double minY = pos.getY() - blockEntity.getBlockPos().getY() - 0.02D;
        double minZ = pos.getZ() - blockEntity.getBlockPos().getZ() + inset;
        double maxX = pos.getX() - blockEntity.getBlockPos().getX() + 1.0D - inset;
        double maxY = pos.getY() - blockEntity.getBlockPos().getY() + 2.1D;
        double maxZ = pos.getZ() - blockEntity.getBlockPos().getZ() + 1.0D - inset;
        LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, 1.0F);
    }

    private static float[] colorFromRoot(BlockPos rootPos) {
        int seed = rootPos.hashCode();
        float red = 0.35F + ((seed >>> 16) & 0xFF) / 255.0F * 0.55F;
        float green = 0.35F + ((seed >>> 8) & 0xFF) / 255.0F * 0.55F;
        float blue = 0.35F + (seed & 0xFF) / 255.0F * 0.55F;
        return new float[]{red, green, blue};
    }
}
