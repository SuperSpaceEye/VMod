package net.spaceeye.vsource.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.spaceeye.vsource.rendering.CustomRenderingKt.renderInWorld;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Camera mainCamera;
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0 ))
    void vssource_postWorldRender(float partialTicks, long finishTimeNano, PoseStack matrixStack, CallbackInfo ci) {
        minecraft.getProfiler().push("vsource_rendering_stage");
        renderInWorld(matrixStack, mainCamera, minecraft);
        minecraft.getProfiler().pop();
    }
}
