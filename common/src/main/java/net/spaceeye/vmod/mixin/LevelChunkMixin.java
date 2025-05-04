package net.spaceeye.vmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.spaceeye.vmod.events.PersistentEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
    @Shadow public abstract Level getLevel();

    @Inject(method = "setBlockState", at = @At("HEAD"))
    void vmod$setBlockState(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        var level = getLevel();
        if (level.isClientSide()) {return;}
        PersistentEvents.INSTANCE.getOnBlockStateChange().emit(new PersistentEvents.OnBlockStateChange((ServerLevel)level, pos, state, isMoving));
    }
}
