package net.spaceeye.vmod.mixin;

import net.spaceeye.vmod.constraintsManaging.VSConstraintsTracker;
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
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.impl.shadow.DF;

import java.util.List;

@Mixin(DF.class)
abstract public class ShipObjectServerWorldMixin {
    @Final
    @Shadow
    public List<ShipData> o; // deletedShipObjects

    // it's called before they get deleted
    @Inject(method = "g", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/shadow/DF;o:Ljava/util/List;"), remap = false)
    void vmod_postTickMixin(CallbackInfo ci) {
        if (o == null) {return;}
        o.forEach((data) -> AVSEvents.INSTANCE.getServerShipRemoveEvent().emit(new AVSEvents.ServerShipRemoveEvent(data)));
    }

    @Inject(method = "createNewConstraint", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/shadow/DF;D:Ljava/util/Map;"), remap = false, locals = LocalCapture.CAPTURE_FAILHARD)
    void vmod_createNewConstraints(VSConstraint vsConstraint, CallbackInfoReturnable<Integer> cir, int var3) {
        VSConstraintsTracker.INSTANCE.addNewConstraint(vsConstraint, var3);
    }

    @Inject(method = "removeConstraint", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/shadow/DF;t:Ljava/util/List;"), remap = false, locals = LocalCapture.CAPTURE_FAILHARD)
    void vmod_removeConstraint(int constraintId, CallbackInfoReturnable<Boolean> cir, VSConstraint var2) {
        VSConstraintsTracker.INSTANCE.removeConstraint(var2, constraintId);
    }

    @Inject(method = "updateConstraint", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/shadow/DF;s:Ljava/util/List;"), remap = false)
    void vmod_updateConstraint(int constraintId, VSConstraint updatedVSConstraint, CallbackInfoReturnable<Boolean> cir) {
        VSConstraintsTracker.INSTANCE.updateConstraint(updatedVSConstraint, constraintId);
    }
}
