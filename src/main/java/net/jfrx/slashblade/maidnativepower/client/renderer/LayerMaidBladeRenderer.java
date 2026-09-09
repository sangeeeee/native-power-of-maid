package net.jfrx.slashblade.maidnativepower.client.renderer;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.layers.LayerMainBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class LayerMaidBladeRenderer<T extends Mob, M extends EntityModel<T>> extends LayerMainBlade<T, M> {
    public LayerMaidBladeRenderer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void renderOffhandItem(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity) {
    }

    @Override
    public void setUserPose(PoseStack poseStack, T entity, float partialTick, ISlashBladeState state) {
        M model = this.getParentModel();
        if (model instanceof BedrockModel<?> bedrockModel && bedrockModel.hasWaistPositioningModel(HumanoidArm.LEFT)) {
            bedrockModel.translateToPositioningWaist(HumanoidArm.LEFT, poseStack);
        } else if (model instanceof ILocationModel locationModel) {
            RenderUtils.prepMatrixForLocator(poseStack, locationModel.leftHandBones());
        } else {
            poseStack.translate(0.25, 0.85, 0.0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F));
        }

        poseStack.translate(-0.3F, -0.2F, -0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
        super.setUserPose(poseStack, entity, partialTick, state);
    }
}
