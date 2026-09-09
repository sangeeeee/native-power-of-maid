package net.jfrx.slashblade.maidnativepower.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mods.flammpfeil.slashblade.client.renderer.layers.LayerMainBlade;
import net.jfrx.slashblade.maidnativepower.client.renderer.LayerMaidBladeRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LayerMainBlade.class)
public abstract class MixinLayerMainBlade {
    @ModifyExpressionValue(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean nativePowerAllowDedicatedMaidLayer(boolean blacklisted) {
        // SlashBlade excludes maids from its generic layer. Our dedicated layer
        // supplies the maid's model transforms and must still render the blade.
        return blacklisted && !((Object) this instanceof LayerMaidBladeRenderer<?, ?>);
    }
}
