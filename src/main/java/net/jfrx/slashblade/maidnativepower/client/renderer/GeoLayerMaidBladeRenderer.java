package net.jfrx.slashblade.maidnativepower.client.renderer;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.Objects;
import java.util.Map.Entry;
import jp.nyatla.nymmd.MmdException;
import jp.nyatla.nymmd.MmdMotionPlayerGL2;
import jp.nyatla.nymmd.MmdPmdModelMc;
import jp.nyatla.nymmd.MmdVmdMotionMc;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.BladeMotionManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.event.client.UserPoseOverrider;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.TimeValueHelper;
import mods.flammpfeil.slashblade.util.VectorHelper;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class GeoLayerMaidBladeRenderer<T extends Mob, R extends IGeoEntityRenderer<T>> extends GeoLayerRenderer<T, R> {
    private static final ResourceLocation BLADE_HOLDER = ResourceLocation.fromNamespaceAndPath("slashblade", "model/bladeholder.pmd");
    private static final ResourceLocation CHARGE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    private MmdPmdModelMc bladeHolder;
    private MmdMotionPlayerGL2 motionPlayer;

    public GeoLayerMaidBladeRenderer(R renderer) {
        super(renderer);
    }

    public GeoLayerMaidBladeRenderer<T, R> copy(R renderer) {
        return new GeoLayerMaidBladeRenderer<>(renderer);
    }

    public void render(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        T entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        ItemStack stack = entity.getMainHandItem();
        if (!stack.isEmpty()) {
            MmdMotionPlayerGL2 player = this.getMotionPlayer();
            if (player != null) {
                BladeStateAccess.of(stack).ifPresent(state -> this.renderBlade(poseStack, buffer, packedLight, entity, partialTick, stack, state, player));
            }
        }
    }

    private MmdMotionPlayerGL2 getMotionPlayer() {
        if (this.motionPlayer != null) {
            return this.motionPlayer;
        } else {
            try {
                if (this.bladeHolder == null) {
                    this.bladeHolder = new MmdPmdModelMc(BLADE_HOLDER);
                }

                this.motionPlayer = new MmdMotionPlayerGL2();
                this.motionPlayer.setPmd(this.bladeHolder);
                return this.motionPlayer;
            } catch (MmdException | IOException exception) {
                NativePowerOfMaid.LOGGER.error("Failed to initialize the maid SlashBlade renderer", exception);
                return null;
            }
        }
    }

    private void renderBlade(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        T entity,
        float partialTick,
        ItemStack stack,
        ISlashBladeState state,
        MmdMotionPlayerGL2 player
    ) {
        Entry<Integer, ResourceLocation> comboTicks = state.peekCurrentComboStateTicks(entity);
        ComboState combo = Objects.requireNonNullElse(
            (ComboState)ComboStateRegistry.REGISTRY.get(comboTicks.getValue()), (ComboState)ComboStateRegistry.NONE.get()
        );
        double time = TimeValueHelper.getMSecFromTicks(comboTicks.getKey().intValue() + partialTick);
        if (combo == ComboStateRegistry.NONE.get()) {
            combo = Objects.requireNonNullElse((ComboState)ComboStateRegistry.REGISTRY.get(state.getComboRoot()), (ComboState)ComboStateRegistry.STANDBY.get());
        }

        MmdVmdMotionMc motion = BladeMotionManager.getInstance().getMotion(combo.getMotionLoc());

        try {
            player.setVmd(motion);
            double maxDuration = motion == null ? 0.0 : TimeValueHelper.getMSecFromFrames(motion.getMaxFrame());
            double start = TimeValueHelper.getMSecFromFrames(combo.getStartFrame());
            double end = TimeValueHelper.getMSecFromFrames(combo.getEndFrame());
            double span = Math.min(maxDuration, Math.abs(end - start));
            if (span > 0.0) {
                if (combo.getLoop()) {
                    time %= span;
                }

                time = Math.min(span, time);
            } else {
                time = 0.0;
            }

            player.updateMotion((float)(start + time));
        } catch (MmdException exception) {
            NativePowerOfMaid.LOGGER.error("Failed to update the maid SlashBlade motion", exception);
            return;
        }

        poseStack.pushPose();
        this.setUserPose(poseStack, entity, partialTick);
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.scale(0.125F, 0.125F, 0.125F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        ResourceLocation texture = state.getTexture().orElse(DefaultResources.resourceDefaultTexture);
        WavefrontObject model = BladeModelManager.getInstance().getModel(state.getModel().orElse(DefaultResources.resourceDefaultModel));
        this.renderBladePart(poseStack, buffer, packedLight, stack, state, player, model, texture);
        this.renderSheathPart(poseStack, buffer, packedLight, entity, partialTick, stack, state, player, model, texture);
        poseStack.popPose();
    }

    private void renderBladePart(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        ItemStack stack,
        ISlashBladeState state,
        MmdMotionPlayerGL2 player,
        WavefrontObject model,
        ResourceLocation texture
    ) {
        poseStack.pushPose();
        applyBoneTransform(poseStack, player, "hardpointA");
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        String part = state.isBroken() ? "blade_damaged" : "blade";
        BladeRenderState.renderOverrided(stack, model, part, texture, poseStack, buffer, packedLight);
        BladeRenderState.renderOverridedLuminous(stack, model, part + "_luminous", texture, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private void renderSheathPart(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        T entity,
        float partialTick,
        ItemStack stack,
        ISlashBladeState state,
        MmdMotionPlayerGL2 player,
        WavefrontObject model,
        ResourceLocation texture
    ) {
        poseStack.pushPose();
        applyBoneTransform(poseStack, player, "hardpointB");
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        BladeRenderState.renderOverrided(stack, model, "sheath", texture, poseStack, buffer, packedLight);
        BladeRenderState.renderOverridedLuminous(stack, model, "sheath_luminous", texture, poseStack, buffer, packedLight);
        if (state.isCharged(entity)) {
            BladeRenderState.renderChargeEffect(stack, entity.tickCount + partialTick, model, "effect", CHARGE_TEXTURE, poseStack, buffer, packedLight);
        }

        poseStack.popPose();
    }

    private static void applyBoneTransform(PoseStack poseStack, MmdMotionPlayerGL2 player, String boneName) {
        int boneIndex = player.getBoneIndexByName(boneName);
        if (boneIndex >= 0) {
            float[] values = new float[16];
            player._skinning_mat[boneIndex].getValue(values);
            Matrix4f transform = VectorHelper.matrix4fFromArray(values);
            poseStack.scale(-1.0F, 1.0F, 1.0F);
            Pose pose = poseStack.last();
            pose.pose().mul(transform);
            pose.normal().mul(new Matrix3f(transform).invert().transpose());
            poseStack.scale(-1.0F, 1.0F, 1.0F);
        }
    }

    private void setUserPose(PoseStack poseStack, T entity, float partialTick) {
        ILocationModel model = this.getLocationModel(entity);
        if (!model.leftWaistBones().isEmpty()) {
            RenderUtils.prepMatrixForLocator(poseStack, model.leftWaistBones());
        } else if (!model.leftHandBones().isEmpty()) {
            RenderUtils.prepMatrixForLocator(poseStack, model.leftHandBones());
        } else {
            poseStack.translate(0.25, 1.0, 0);
        }
        // Gecko locators use an upward Y axis; the blade motion uses vanilla model coordinates.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.translate(-0.3F, -0.2F, -0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
        float comboRotation = UserPoseOverrider.getInterpolatedComboRotation(entity, partialTick);
        if (comboRotation != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(comboRotation));
        }
    }
}
