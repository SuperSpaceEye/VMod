package net.spaceeye.vmod.mixin;

import net.spaceeye.vmod.events.AVSEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.apigame.ships.MutableQueryableShipData;
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld;

@Mixin(ShipObjectClientWorld.class)
abstract public class ShipObjectClientWorldMixin {
    @Final
    @Shadow

    private MutableQueryableShipData _loadedShips;

    // when ship is unloaded on client, it just gets removed
    @Inject(method = "removeShip", at = @At("HEAD"), remap = false)
    void vmod_removeShip(long shipId, CallbackInfo ci) {
        AVSEvents.INSTANCE.getClientShipUnloadEvent().emit(new AVSEvents.ClientShipUnloadEvent(_loadedShips.getById(shipId)));
    }
}
