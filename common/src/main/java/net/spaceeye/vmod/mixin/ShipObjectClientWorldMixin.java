package net.spaceeye.vmod.mixin;

import net.spaceeye.vmod.events.AVSEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld;
import org.valkyrienskies.core.impl.networking.impl.PhysEntityCreateData;

@Mixin(ShipObjectClientWorld.class)
abstract public class ShipObjectClientWorldMixin {
    @Inject(method = "addPhysicsEntity", at = @At("HEAD"), remap = false)
    void vmod_onPhysEntityAdd(PhysEntityCreateData physicsEntityCreateData, CallbackInfo ci) {
        AVSEvents.INSTANCE.getClientPhysEntityLoad().emit(physicsEntityCreateData);
    }

    @Inject(method = "removePhysicsEntity", at = @At("HEAD"), remap = false)
    void vmod_onPhysEntityRemoval(long shipId, CallbackInfo ci) {
        AVSEvents.INSTANCE.getClientPhysEntityUnload().emit(shipId);
    }
}
