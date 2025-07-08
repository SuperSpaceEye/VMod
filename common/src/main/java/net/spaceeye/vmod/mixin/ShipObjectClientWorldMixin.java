package net.spaceeye.vmod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.spaceeye.vmod.events.AVSEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.apigame.ships.MutableQueryableShipData;
import org.valkyrienskies.core.impl.game.phys_entities.PhysicsEntityClient;
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld;
import org.valkyrienskies.core.impl.networking.impl.PhysEntityCreateData;

@Mixin(ShipObjectClientWorld.class)
abstract public class ShipObjectClientWorldMixin {
    @Final @Shadow(remap = false) private MutableQueryableShipData _loadedShips;

    // when ship is unloaded on client, it just gets removed
    @Inject(method = "removeShip", at = @At("HEAD"), remap = false)
    void vmod_removeShip(long shipId, CallbackInfo ci) {
        AVSEvents.INSTANCE.getClientShipUnloadEvent().emit(new AVSEvents.ClientShipUnloadEvent(_loadedShips.getById(shipId)));
    }

    @Inject(method = "addPhysicsEntity", at = @At("RETURN"), remap = false)
    void vmod_onPhysEntityAdd(PhysEntityCreateData physicsEntityCreateData, CallbackInfo ci, @Local PhysicsEntityClient var2) {
        AVSEvents.INSTANCE.getClientPhysEntityLoad().emit(new AVSEvents.ClientPhysEntityLoad(var2));
    }

    @Inject(method = "removePhysicsEntity", at = @At("HEAD"), remap = false)
    void vmod_onPhysEntityRemoval(long shipId, CallbackInfo ci) {
        AVSEvents.INSTANCE.getClientPhysEntityUnload().emit(shipId);
    }
}
