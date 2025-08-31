package net.spaceeye.vmod.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.spaceeye.vmod.rendering.ShipsColorModulator;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ClientShip;

import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@Mixin(value = LevelRenderer.class, priority = 1001)
public class MixinLevelRendererVanillaMixin {
    @Unique private float[] vmod$color;

    @TargetHandler(
            mixin = "org.valkyrienskies.mod.mixin.mod_compat.vanilla_renderer.MixinLevelRendererVanilla",
            name = "redirectRenderChunkLayer"
    )
    @Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/WeakHashMap;forEach(Ljava/util/function/BiConsumer;)V"), require = 0)
    private void vmod$getColor(WeakHashMap<ClientShip, Object> instance, BiConsumer<? super Object, ? super Object> entry) {
        instance.forEach((ship, chunks) -> {
            vmod$color = ShipsColorModulator.INSTANCE.getColor(ship.getId());
            entry.accept(ship, chunks);
        });
    }
    @TargetHandler(
            mixin = "org.valkyrienskies.mod.mixin.mod_compat.vanilla_renderer.MixinLevelRendererVanilla",
            name = "renderChunkLayer"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getShaderColor()[F"), require = 0)
    private void vmod$setColor(RenderType renderType, PoseStack poseStack, double d, double e, double f, Matrix4f matrix4f, ObjectList<Object> chunksToRender, CallbackInfo ci) {
        RenderSystem.setShaderColor(vmod$color[0], vmod$color[1], vmod$color[2], vmod$color[3]);
    }
}
