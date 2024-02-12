package net.spaceeye.vsource.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.spaceeye.vsource.utils.LevelEvents;
import net.spaceeye.vsource.utils.ServerLevelHolder;
import net.spaceeye.vsource.utils.constraintsSaving.ConstraintManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static net.spaceeye.vsource.VSKt.LOG;

@Mixin(ClientLevel.class)
abstract public class ClientLevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    public void injectInit(ClientPacketListener clientPacketListener, ClientLevel.ClientLevelData clientLevelData, ResourceKey resourceKey, Holder holder, int i, int j, Supplier supplier, LevelRenderer levelRenderer, boolean bl, long l, CallbackInfo ci) {
        Object obj = this;
        if (!(obj instanceof ClientLevel)) {return;}
        ClientLevel level = (ClientLevel)obj;
        LevelEvents.INSTANCE.getClientLevelInitEvent().emit(new LevelEvents.ClientLevelInitEvent(level));
    }

    @Inject(method = "disconnect", at = @At("HEAD"))
    public void injectDisconnect(CallbackInfo ci) {
        Object obj = this;
        if (!(obj instanceof ClientLevel)) {return;}
        ClientLevel level = (ClientLevel)obj;
        LevelEvents.INSTANCE.getClientDisconnectEvent().emit(new LevelEvents.ClientDisconnectEvent(level));
    }
}
