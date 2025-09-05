package net.spaceeye.vmod.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NativeImage.class)
public interface NativeImageInvoker {
    @Invoker("<init>") static NativeImage theConstructor(NativeImage.Format format, int width, int height, boolean useStbFree, long pixels) {
        return null;
    }
}