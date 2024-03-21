package net.spaceeye.vmod.mixin;

import net.spaceeye.vmod.events.AVSEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.apigame.world.chunks.ChunkUnwatchTask;
import org.valkyrienskies.core.apigame.world.chunks.ChunkWatchTask;
import org.valkyrienskies.core.impl.chunk_tracking.a;
import org.valkyrienskies.core.impl.shadow.zB;

import java.util.HashSet;

@Mixin(org.valkyrienskies.core.impl.chunk_tracking.h.class)
abstract public class ShipObjectServerWorldChunkTrackerMixin {
    @Shadow HashSet<zB> i;

    // when ship gets unloaded on server it doesn't get removed from loadedShips or allShips, but it does some other
    // thing, which this mixin tracks, also the point when this event is called is not really when ship actually gets
    // unloaded but it doesn't really matter
    @Inject(method = "a(Ljava/lang/Iterable;Ljava/lang/Iterable;)Lorg/valkyrienskies/core/impl/chunk_tracking/a;", at = @At("RETURN"), remap = false)
    void vmod_applyTasksAndGenerateTrackingInfo(Iterable<? extends ChunkWatchTask> par1, Iterable<? extends ChunkUnwatchTask> par2, CallbackInfoReturnable<a> cir) {
        if (i.isEmpty()) {return;}
        i.forEach((ship) -> AVSEvents.INSTANCE.getServerShipUnloadEvent().emit(new AVSEvents.ServerShipUnloadEvent(ship.asShipDataCommon())));
    }
}
