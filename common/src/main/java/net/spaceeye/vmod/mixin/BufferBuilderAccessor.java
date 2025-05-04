package net.spaceeye.vmod.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Invoker("ensureCapacity") void vmod$ensureCapacity(int increaseAmount);
    @Accessor("format") @NotNull VertexFormat vmod$getVertexFormat();
    @Accessor("buffer") @NotNull ByteBuffer vmod$getBuffer();
    @Accessor("fullFormat") boolean vmod$fullFormat();

    @Accessor("nextElementByte") int vmod$nextElementByte();
    @Accessor("nextElementByte") void vmod$nextElementByte(int value);

    @Accessor("vertices") int vmod$vertices();
    @Accessor("vertices") void vmod$vertices(int value);
}
