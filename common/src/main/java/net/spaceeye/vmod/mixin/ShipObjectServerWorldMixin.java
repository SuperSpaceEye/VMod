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
//    @Final
//    @Shadow
//    public List<ShipData> o; // deletedShipObjects
//
//    // it's called before they get deleted
//    @Inject(method = "g", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/shadow/DF;o:Ljava/util/List;"), remap = false)
//    void vmod_postTickMixin(CallbackInfo ci) {
//        if (o == null) {return;}
//        o.forEach((data) -> AVSEvents.INSTANCE.getServerShipRemoveEvent().emit(new AVSEvents.ServerShipRemoveEvent(data)));
//    }
//
//    @Inject(method = "createNewConstraint", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/shadow/DF;D:Ljava/util/Map;"), remap = false, locals = LocalCapture.CAPTURE_FAILHARD)
//    void vmod_createNewConstraints(VSJoint VSJoint, CallbackInfoReturnable<Integer> cir, int var3) {
//        VSJointsTracker.INSTANCE.addNewConstraint(VSJoint, var3);
//    }
//
//    @Inject(method = "removeConstraint", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/shadow/DF;t:Ljava/util/List;"), remap = false, locals = LocalCapture.CAPTURE_FAILHARD)
//    void vmod_removeConstraint(int constraintId, CallbackInfoReturnable<Boolean> cir, VSJoint var2) {
//        VSJointsTracker.INSTANCE.removeConstraint(var2, constraintId);
//    }
//
//    @Inject(method = "updateConstraint", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/shadow/DF;s:Ljava/util/List;"), remap = false)
//    void vmod_updateConstraint(int constraintId, VSJoint updatedVSJoint, CallbackInfoReturnable<Boolean> cir) {
//        VSJointsTracker.INSTANCE.updateConstraint(updatedVSJoint, constraintId);
//    }
}
