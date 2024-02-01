package net.spaceeye.vssource.mixin;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.spaceeye.vssource.ConstraintManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

//Why this exists? ConstraintManager needs to be called as soon as overworld serverlevel exists to begin loading ship
// constraints, and idfk better way to do this

@Mixin(ServerLevel.class)
abstract public class ServerLevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    public void injectInit(MinecraftServer minecraftServer, Executor executor, LevelStorageSource.LevelStorageAccess levelStorageAccess, ServerLevelData serverLevelData, ResourceKey resourceKey, Holder holder, ChunkProgressListener chunkProgressListener, ChunkGenerator chunkGenerator, boolean bl, long l, List list, boolean bl2, CallbackInfo ci) {
        Object obj = this;
        if (!(obj instanceof ServerLevel)) {return;}
        ServerLevel level = (ServerLevel)obj;
        if (level.getServer() == null || level.getServer().overworld() == null) {return;}
        ConstraintManager.Companion.getInstance((ServerLevel)obj);
    }
}
