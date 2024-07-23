package net.spaceeye.vmod.mixin;

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

import static net.spaceeye.vmod.rendering.CustomRenderingKt.renderInWorld;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Camera mainCamera;
    @Shadow @Final private Minecraft minecraft;

    //TODO maybe use this?
//    @WrapOperation(method = "renderLevel",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V"))
//    void vmod(LevelRenderer instance, PoseStack outlinebuffersource, float i, long j, boolean k, Camera l, GameRenderer i1, LightTexture texture, Matrix4f multibuffersource, Operation<Void> original) {
//        // this one should render block shit
////        minecraft.getProfiler().push("vmod_rendering_stage");
////        renderInWorld(outlinebuffersource, mainCamera, minecraft);
////        minecraft.getProfiler().pop();
//
//        original.call(instance, outlinebuffersource, i, j, k, l, i1, texture, multibuffersource);
//
//        // this should render normal vmod contraints
////        minecraft.getProfiler().push("vmod_rendering_stage");
////        renderInWorld(outlinebuffersource, mainCamera, minecraft);
////        minecraft.getProfiler().pop();
//    }

    //this works for vmod rendering
    @Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0 ))
    void vmod_postWorldRender(float partialTicks, long finishTimeNano, PoseStack matrixStack, CallbackInfo ci) {
        minecraft.getProfiler().push("vmod_main_rendering_stage");
        renderInWorld(matrixStack, mainCamera, minecraft, false);
        minecraft.getProfiler().pop();
    }
}
