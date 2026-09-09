package net.jfrx.slashblade.maidnativepower.client.renderer;

import jp.nyatla.nymmd.MmdException;
import jp.nyatla.nymmd.MmdMotionPlayerGL2;
import jp.nyatla.nymmd.MmdPmdModelMc;
import jp.nyatla.nymmd.MmdVmdMotionMc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.client.renderer.model.BladeMotionManager;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.TimeValueHelper;
import mods.flammpfeil.slashblade.util.VectorHelper;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.Objects;

/** Removes the player rig's absolute placement before applying motion to a maid locator. */
final class MaidBladeMotion {
    private static final ResourceLocation BLADE_HOLDER = ResourceLocation.fromNamespaceAndPath("slashblade", "model/bladeholder.pmd");
    private static final Pose REST = new Pose(new Matrix4f(), new Matrix4f());
    private MmdMotionPlayerGL2 player;
    private MmdVmdMotionMc referenceMotion;
    private int referenceFrame;
    private Matrix4f inverseBladeRest;
    private Matrix4f inverseSheathRest;
    private boolean loggedFailure;

    record Pose(Matrix4f blade, Matrix4f sheath) {
    }

    Pose sample(LivingEntity entity, ISlashBladeState state, float partialTick) {
        var comboTicks = state.peekCurrentComboStateTicks(entity);
        ComboState combo = ComboStateRegistry.REGISTRY.get(comboTicks.getValue());
        // At rest, leave the chosen model locator's placement unchanged.
        if (combo == null || combo == ComboStateRegistry.NONE.get() || comboTicks.getValue().equals(state.getComboRoot())) {
            return REST;
        }
        ComboState root = Objects.requireNonNullElse(ComboStateRegistry.REGISTRY.get(state.getComboRoot()), ComboStateRegistry.STANDBY.get());
        MmdVmdMotionMc restMotion = BladeMotionManager.getInstance().getMotion(root.getMotionLoc());
        MmdVmdMotionMc motion = BladeMotionManager.getInstance().getMotion(combo.getMotionLoc());
        if (motion == null || restMotion == null) {
            return REST;
        }
        try {
            if (player == null) {
                player = new MmdMotionPlayerGL2();
                player.setPmd(new MmdPmdModelMc(BLADE_HOLDER));
            }
            // Motion identity also invalidates the rest cache after resource reloads.
            if (referenceMotion != restMotion || referenceFrame != root.getStartFrame()) {
                player.setVmd(restMotion);
                player.updateMotionBonesAndSkinning((float) TimeValueHelper.getMSecFromFrames(root.getStartFrame()));
                inverseBladeRest = bonePose("hardpointA").invert();
                inverseSheathRest = bonePose("hardpointB").invert();
                referenceMotion = restMotion;
                referenceFrame = root.getStartFrame();
            }
            player.setVmd(motion);
            double start = TimeValueHelper.getMSecFromFrames(combo.getStartFrame());
            double end = TimeValueHelper.getMSecFromFrames(combo.getEndFrame());
            double span = Math.min(TimeValueHelper.getMSecFromFrames(motion.getMaxFrame()), Math.abs(end - start));
            double elapsed = Math.max(0, TimeValueHelper.getMSecFromTicks(comboTicks.getKey() + partialTick));
            elapsed = span <= 0 ? 0 : combo.getLoop() ? elapsed % span : Math.min(span, elapsed);
            player.updateMotionBonesAndSkinning((float) (start + Math.copySign(elapsed, end - start)));
            return new Pose(new Matrix4f(inverseBladeRest).mul(bonePose("hardpointA")),
                    new Matrix4f(inverseSheathRest).mul(bonePose("hardpointB")));
        } catch (MmdException | IOException exception) {
            if (!loggedFailure) {
                NativePowerOfMaid.LOGGER.error("Unable to sample maid blade motion; using native rest placement", exception);
                loggedFailure = true;
            }
            player = null;
            referenceMotion = null;
            return REST;
        }
    }

    private Matrix4f bonePose(String boneName) {
        int index = player.getBoneIndexByName(boneName);
        if (index < 0) {
            return new Matrix4f();
        }
        float[] values = new float[16];
        player._skinning_mat[index].getValue(values);
        // SlashBlade mirrors X and uses 1/16 OBJ units under its motion rig.
        // inverse(rest) * current cancels the rig's origin/height and converts its
        // translation into OBJ units before the snapshot's model scale is applied.
        return new Matrix4f().scaling(-1, 1, 1).mul(VectorHelper.matrix4fFromArray(values))
                .scale(-1, 1, 1).scale(0.0625F);
    }
}
