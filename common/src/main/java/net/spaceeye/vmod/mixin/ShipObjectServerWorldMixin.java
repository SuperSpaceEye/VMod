package net.spaceeye.vmod.mixin;

import net.spaceeye.vmod.constraintsManaging.VSJointsTracker;
import net.spaceeye.vmod.events.AVSEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.apigame.joints.VSJoint;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.impl.shadow.Ep;

import java.util.List;

@Mixin(Ep.class)
abstract public class ShipObjectServerWorldMixin {
    //first instance, can be double-checked by finding deleteShip fn
    @Final
    @Shadow
    public List<ShipData> o; // deletedShipObjects


    // it's called before they get deleted
    // should be second result with first line smth like "this.e.a(Ep.c.POST_TICK_START);"
    @Inject(method = "g", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/shadow/Ep;o:Ljava/util/List;"), remap = false)
    void vmod_postTickMixin(CallbackInfo ci) {
        if (o == null) {return;}
        o.forEach((data) -> AVSEvents.INSTANCE.getServerShipRemoveEvent().emit(new AVSEvents.ServerShipRemoveEvent(data)));
    }
}
