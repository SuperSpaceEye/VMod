package net.spaceeye.vsource.mixin;

import net.minecraft.server.MinecraftServer;
import net.spaceeye.vsource.utils.LevelEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
abstract public class MinecraftServerMixin {
    @Inject(method = "stopServer", at = @At("HEAD"), remap = false)
    void injectStopServer(CallbackInfo ci) {
        LevelEvents.INSTANCE.getServerStopEvent().emit(new LevelEvents.ServerStopEvent((MinecraftServer)(Object)this));
    }
}
