package net.spaceeye.vsource.mixin;

import net.spaceeye.vsource.utils.AVSEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

import java.util.List;

@Mixin(ShipObjectServerWorld.class)
abstract public class ShipObjectServerWorldMixin {
    @Final
    @Shadow
    private List<ShipData> deletedShipObjects;

    // it's called before they get deleted
    @Inject(method = "postTick", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/game/ships/ShipObjectServerWorld;deletedShipObjects:Ljava/util/List;"), remap = false)
    void vsource_postTickMixin(CallbackInfo ci) {
        if (deletedShipObjects == null) {return;}
        deletedShipObjects.forEach((data) -> AVSEvents.INSTANCE.getServerShipRemoveEvent().emit(new AVSEvents.ServerShipRemoveEvent(data)));
    }
}
