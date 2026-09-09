package net.jfrx.slashblade.maidnativepower.gametest;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.compat.slashblade.SlashBladeRender;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.mojang.blaze3d.vertex.PoseStack;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/** Compares actual render submissions against the pinned snapshot, without drawing extra geometry. */
final class BladePlacementChecks {
    private static final MultiBufferSource NO_DRAW = type -> { throw new AssertionError("Probe draw was not intercepted"); };
    private static Map<String, Transform> probe;

    private record Transform(Matrix4f pose, Matrix3f normal) {
    }

    static boolean capture(RenderOverrideEvent event) {
        if (probe == null) return false;
        if (probe.put(event.getTarget(), new Transform(new Matrix4f(event.getPoseStack().last().pose()),
                new Matrix3f(event.getPoseStack().last().normal()))) != null) {
            throw new AssertionError("Duplicate blade part: " + event.getTarget());
        }
        event.setCanceled(true);
        return true;
    }

    @SuppressWarnings("unchecked")
    static void check(RenderLivingEvent.Post<?, ?> event, EntityMaid maid) {
        // The preview uses the snapshot's unmodified task path with the exact same prepared model bones.
        EntityMaid preview = new EntityMaid(maid.level());
        ItemStack stack = maid.getMainHandItem().copy();
        var state = BladeStateAccess.of(stack).orElseThrow();
        long now = maid.level().getGameTime();
        state.setComboSeq(ComboStateRegistry.NONE.getId());
        state.setLastActionTime(now - 1000);
        Runnable nativeRender;
        Runnable animatedRender;
        if (event.getRenderer() instanceof GeckoEntityMaidRenderer<?> renderer) {
            var gecko = (GeckoEntityMaidRenderer<Mob>) renderer;
            ILocationModel model = gecko.getGeoEntity(maid).getGeoModel();
            // Expose the hand as the native anchor for models without a waist locator.
            ILocationModel nativeAnchor = model.leftWaistBones().isEmpty() && !model.leftHandBones().isEmpty() ? new ILocationModel() {
                @Override
                public java.util.List<? extends com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone> leftWaistBones() {
                    return model.leftHandBones();
                }
            } : model;
            nativeRender = () -> SlashBladeRender.renderMaidMainhandSlashBlade(preview, nativeAnchor, new PoseStack(), NO_DRAW, 0xF000F0, stack, 0);
            animatedRender = () -> SlashBladeRender.renderMaidMainhandSlashBlade(maid, model, new PoseStack(), NO_DRAW, 0xF000F0, stack, 0);
            ILocationModel noLocator = new ILocationModel() {};
            assertEqual(collect(() -> SlashBladeRender.renderMaidMainhandSlashBlade(preview, noLocator, new PoseStack(), NO_DRAW, 0xF000F0, stack, 0)),
                    collect(() -> SlashBladeRender.renderMaidMainhandSlashBlade(maid, noLocator, new PoseStack(), NO_DRAW, 0xF000F0, stack, 0)), "missing locator fallback");
        } else {
            BedrockModel<Mob> model = (BedrockModel<Mob>) event.getRenderer().getModel();
            BedrockModel<Mob> nativeAnchor = !model.hasWaistPositioningModel(HumanoidArm.LEFT) && model.hasArmPositioningModel(HumanoidArm.LEFT) ? new BedrockModel<>() {
                @Override
                public boolean hasWaistPositioningModel(HumanoidArm arm) { return true; }

                @Override
                public void translateToPositioningWaist(HumanoidArm arm, PoseStack pose) { model.translateToPositioningHand(arm, pose); }
            } : model;
            nativeRender = () -> SlashBladeRender.renderMaidMainhandSlashBlade(preview, nativeAnchor, new PoseStack(), NO_DRAW, 0xF000F0, stack, 0);
            animatedRender = () -> SlashBladeRender.renderMaidMainhandSlashBlade(maid, model, new PoseStack(), NO_DRAW, 0xF000F0, stack, 0);
            BedrockModel<Mob> noLocator = new BedrockModel<>();
            assertEqual(collect(() -> SlashBladeRender.renderMaidMainhandSlashBlade(preview, noLocator, new PoseStack(), NO_DRAW, 0xF000F0, stack, 0)),
                    collect(() -> SlashBladeRender.renderMaidMainhandSlashBlade(maid, noLocator, new PoseStack(), NO_DRAW, 0xF000F0, stack, 0)), "missing locator fallback");
        }
        Map<String, Transform> rest = collect(animatedRender);
        assertEqual(collect(nativeRender), rest, maid.getModelId() + " rest");
        state.setComboSeq(ComboStateRegistry.COMBO_A1.getId());
        state.setLastActionTime(now - 1);
        Map<String, Transform> attack = collect(animatedRender);
        if (attack.get("blade").pose.equals(rest.get("blade").pose, 0.00001F)) {
            throw new AssertionError("Combo motion was lost: " + maid.getModelId());
        }
        if (state.getLastActionTime() != now - 1 || !state.getComboSeq().equals(ComboStateRegistry.COMBO_A1.getId())) {
            throw new AssertionError("Rendering changed the combat state");
        }
        state.setComboSeq(ComboStateRegistry.NONE.getId());
        state.setLastActionTime(now - 1000);
        assertEqual(rest, collect(animatedRender), maid.getModelId() + " return to rest");
    }

    private static Map<String, Transform> collect(Runnable render) {
        probe = new HashMap<>();
        try {
            render.run();
            if (!probe.keySet().containsAll(java.util.List.of("blade", "sheath", "blade_luminous", "sheath_luminous"))) {
                throw new AssertionError("Missing blade geometry: " + probe.keySet());
            }
            for (Transform transform : probe.values()) {
                if (!transform.pose.isFinite() || !transform.normal.isFinite()) {
                    throw new AssertionError("Non-finite blade transform");
                }
            }
            return Map.copyOf(probe);
        } finally {
            probe = null;
        }
    }

    private static void assertEqual(Map<String, Transform> expected, Map<String, Transform> actual, String label) {
        if (!expected.keySet().equals(actual.keySet())) throw new AssertionError(label + ": part mismatch");
        for (String part : expected.keySet()) {
            Transform a = expected.get(part);
            Transform b = actual.get(part);
            if (!a.pose.equals(b.pose, 0.00001F) || !a.normal.equals(b.normal, 0.00001F)) {
                throw new AssertionError(label + ": displaced " + part + "\nexpected=" + a + "\nactual=" + b);
            }
        }
    }
}
