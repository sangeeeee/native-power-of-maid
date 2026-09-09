package net.jfrx.slashblade.maidnativepower.client.renderer;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.init.DefaultResources;
import net.jfrx.slashblade.maidnativepower.task.TaskSlashBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Uses the snapshot's model locators, with SlashBlade motion relative to its rest pose. */
public final class MaidBladeRenderer {
    private static final MaidBladeMotion MOTION = new MaidBladeMotion();
    private static final ResourceLocation CHARGE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");

    private MaidBladeRenderer() {
    }

    public static boolean usesAnimatedBlade(LivingEntity entity) {
        return entity instanceof EntityMaid maid && TaskSlashBlade.UID.equals(maid.getTask().getUid());
    }

    public static void renderBedrock(LivingEntity maid, BedrockModel<?> model, PoseStack poseStack,
                                     MultiBufferSource buffer, int light, ItemStack stack, float partialTick) {
        poseStack.pushPose();
        try {
            // Keep the snapshot's locator placement. A hand locator also supports
            // small models which omit the optional waist locator.
            if (model.hasWaistPositioningModel(HumanoidArm.LEFT)) {
                model.translateToPositioningWaist(HumanoidArm.LEFT, poseStack);
            } else if (model.hasArmPositioningModel(HumanoidArm.LEFT)) {
                model.translateToPositioningHand(HumanoidArm.LEFT, poseStack);
            } else {
                poseStack.translate(0.25, 0.85, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(-20));
            }
            poseStack.translate(0, 0, -0.5);
            poseStack.scale(0.007F, 0.007F, 0.007F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
            render(maid, poseStack, buffer, light, stack, partialTick);
        } finally {
            poseStack.popPose();
        }
    }

    public static void renderGecko(LivingEntity maid, ILocationModel model, PoseStack poseStack,
                                   MultiBufferSource buffer, int light, ItemStack stack, float partialTick) {
        poseStack.pushPose();
        try {
            if (!model.leftWaistBones().isEmpty()) {
                RenderUtils.prepMatrixForLocator(poseStack, model.leftWaistBones());
            } else if (!model.leftHandBones().isEmpty()) {
                RenderUtils.prepMatrixForLocator(poseStack, model.leftHandBones());
            } else {
                poseStack.translate(-0.25, 1.25, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(20));
            }
            poseStack.translate(0, 0, -0.7);
            poseStack.scale(0.01F, 0.01F, 0.01F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            render(maid, poseStack, buffer, light, stack, partialTick);
        } finally {
            poseStack.popPose();
        }
    }

    private static void render(LivingEntity maid, PoseStack poseStack, MultiBufferSource buffer, int light,
                               ItemStack stack, float partialTick) {
        BladeStateAccess.of(stack).ifPresent(state -> {
            ResourceLocation texture = state.getTexture().orElse(DefaultResources.resourceDefaultTexture);
            WavefrontObject model = BladeModelManager.getInstance().getModel(state.getModel().orElse(DefaultResources.resourceDefaultModel));
            MaidBladeMotion.Pose motion = MOTION.sample(maid, state, partialTick);
            renderPart(maid, state, stack, model, texture, "sheath", motion.sheath(), poseStack, buffer, light, partialTick);
            renderPart(maid, state, stack, model, texture, state.isBroken() ? "blade_damaged" : "blade",
                    motion.blade(), poseStack, buffer, light, partialTick);
        });
    }

    private static void renderPart(LivingEntity maid, ISlashBladeState state, ItemStack stack, WavefrontObject model,
                                   ResourceLocation texture, String part, Matrix4f motion, PoseStack poseStack,
                                   MultiBufferSource buffer, int light, float partialTick) {
        poseStack.pushPose();
        try {
            poseStack.last().pose().mul(motion);
            poseStack.last().normal().mul(new Matrix3f(motion).invert().transpose());
            BladeRenderState.renderOverrided(stack, model, part, texture, poseStack, buffer, light);
            BladeRenderState.renderOverridedLuminous(stack, model, part + "_luminous", texture, poseStack, buffer, light);
            if ("sheath".equals(part) && state.isCharged(maid)) {
                BladeRenderState.renderChargeEffect(stack, maid.tickCount + partialTick, model, "effect", CHARGE_TEXTURE, poseStack, buffer, light);
            }
        } finally {
            poseStack.popPose();
        }
    }
}
