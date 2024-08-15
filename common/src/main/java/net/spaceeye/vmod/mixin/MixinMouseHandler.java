package net.spaceeye.vmod.mixin;

import net.minecraft.client.MouseHandler;
import net.spaceeye.vmod.events.RandomEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {
    @Shadow private double xpos;
    @Shadow private double ypos;

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    void vmod$onMouseMoveEvent(long windowPointer, double xpos, double ypos, CallbackInfo ci) {
        if (RandomEvents.INSTANCE.getMouseMove().emit(new RandomEvents.OnMouseMove(xpos, ypos))) {
            ci.cancel();
            this.xpos = xpos;
            this.ypos = ypos;
        }
    }
}
