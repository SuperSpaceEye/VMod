package net.spaceeye.vmod.mixin;

import net.minecraft.client.KeyboardHandler;
import net.spaceeye.vmod.events.RandomEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    void test(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (RandomEvents.INSTANCE.getKeyPress().emit(new RandomEvents.OnKeyPress(key, scanCode, action, modifiers))) {
            ci.cancel();
        }
    }
}
