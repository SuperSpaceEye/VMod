package net.spaceeye.vmod.mixin;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockRenderDispatcher.class)
public interface BlockRenderDispatcherAccessor {
    @Accessor("modelRenderer") @NotNull ModelBlockRenderer getModelRenderer();
    @Accessor("blockEntityRenderer") @NotNull BlockEntityWithoutLevelRenderer getBlockEntityRenderer();
}
