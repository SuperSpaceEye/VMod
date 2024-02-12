package net.spaceeye.vsource.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.spaceeye.vsource.utils.LevelEvents;
import net.spaceeye.vsource.utils.ServerLevelHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
abstract public class MinecraftServerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    void injectInit(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository, GameProfileCache gameProfileCache, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci) {
        ServerLevelHolder.INSTANCE.setServer((MinecraftServer)(Object)this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    void injectStopServer(CallbackInfo ci) {
        LevelEvents.INSTANCE.getServerStopEvent().emit(new LevelEvents.ServerStopEvent((MinecraftServer)(Object)this));
    }
}
