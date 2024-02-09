package net.spaceeye.vsource.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.spaceeye.vsource.rendering.RenderEvents;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//@Mixin(LevelRenderer.class)
//public abstract class GameRendererMixin {
//    @Inject(method = "renderLevel", at=@At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;globalBlockEntities:Ljava/util/Set;", ordinal = 0))
//    void vsource_render(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
//        RenderEvents.INSTANCE.getWORLD().invoker().rendered(poseStack, frustumPos, camera);
//    }
//}

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Camera mainCamera;

    @Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0 ))
    void vssource_postWorldRender(float partialTicks, long finishTimeNano, PoseStack matrixStack, CallbackInfo ci) {
        RenderEvents.INSTANCE.getWORLD().invoker().rendered(matrixStack, mainCamera);
    }
}
