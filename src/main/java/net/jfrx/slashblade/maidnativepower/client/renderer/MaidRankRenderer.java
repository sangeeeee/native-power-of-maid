package net.jfrx.slashblade.maidnativepower.client.renderer;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.jfrx.slashblade.maidnativepower.config.NativePowerOfMaidClientConfig;
import net.jfrx.slashblade.maidnativepower.task.TaskSlashBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = NativePowerOfMaid.MODID, value = Dist.CLIENT)
public class MaidRankRenderer {
    public static final ResourceLocation RANK_IMG = ResourceLocation.fromNamespaceAndPath("slashblade", "textures/gui/rank.png");

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof EntityMaid maid) || !TaskSlashBlade.UID.equals(maid.getTask().getUid())) {
            return;
        }
        var concentration = maid.getData(CapabilityConcentrationRank.RANK_POINT);
        long now = maid.level().getGameTime();
        IConcentrationRank.ConcentrationRanks rank = concentration.getRank(now);
        if (rank == IConcentrationRank.ConcentrationRanks.NONE) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            poseStack.translate(0, maid.getBbHeight() + 0.5F, 0);
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            float size = NativePowerOfMaidClientConfig.MAID_RANK_SIZE.get().floatValue();
            poseStack.scale(size, -size, size);

            // Queue the billboard with entity nameplates so world rendering uses
            // the matching shader, vertex format, and render state.
            VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.textSeeThrough(RANK_IMG));
            Matrix4f pose = poseStack.last().pose();
            int x = NativePowerOfMaidClientConfig.MAID_RANK_X.get();
            int y = NativePowerOfMaidClientConfig.MAID_RANK_Y.get();
            int rankOffset = 32 * (rank.level - 1);
            int textOffset = now < concentration.getLastRankRise() + 20L ? 128 : 0;
            float rankProgress = concentration.getRankProgress(now);
            int progress = (int) (33 * rankProgress);
            int progressIcon = (int) (18 * rankProgress);
            int progressIconInv = 17 - progressIcon;
            drawTexturedQuad(buffer, pose, x, y, textOffset + 64, rankOffset, 64, 32);
            drawTexturedQuad(buffer, pose, x, y + progressIconInv + 7, textOffset, rankOffset + progressIconInv + 7, 64, progressIcon);
            drawTexturedQuad(buffer, pose, x, y + 32, 0, 240, 64, 16);
            drawTexturedQuad(buffer, pose, x + 16, y + 32, 16, 224, progress, 16);
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawTexturedQuad(VertexConsumer buffer, Matrix4f pose, int x, int y, int u, int v, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float uvScale = 1.0F / 256;
        buffer.addVertex(pose, x, y + height, 0).setColor(-1).setUv(u * uvScale, (v + height) * uvScale).setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(pose, x + width, y + height, 0).setColor(-1).setUv((u + width) * uvScale, (v + height) * uvScale).setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(pose, x + width, y, 0).setColor(-1).setUv((u + width) * uvScale, v * uvScale).setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(pose, x, y, 0).setColor(-1).setUv(u * uvScale, v * uvScale).setLight(LightTexture.FULL_BRIGHT);
    }
}
