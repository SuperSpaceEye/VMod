package net.spaceeye.vmod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.spaceeye.vmod.events.AVSEvents;
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager;
import net.spaceeye.vmod.vsStuff.VSJointsTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.apigame.joints.VSJoint;
import org.valkyrienskies.core.apigame.world.chunks.BlockType;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.impl.shadow.Ep;

import java.util.List;
import java.util.Objects;


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

    @WrapOperation(method = "onSetBlock", at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/core/impl/shadow/Er;onSetBlock(IIILjava/lang/String;Lorg/valkyrienskies/core/apigame/world/chunks/BlockType;Lorg/valkyrienskies/core/apigame/world/chunks/BlockType;DD)V"), remap = false)
    void vmod$onSetBlock(Ep instance, int posX, int posY, int posZ, String dimensionId, BlockType oldBlockType, BlockType newBlockType, double oldBlockMass, double newBlockMass, Operation<Void> original) {
        var mass = CustomBlockMassManager.INSTANCE.getCustomMass(dimensionId, posX, posY, posZ);
        original.call(instance, posX, posY, posZ, dimensionId, oldBlockType, newBlockType, Objects.requireNonNullElse(mass, oldBlockMass), newBlockMass);
    }

    @Inject(method = "createNewConstraint", at = @At(value = "RETURN"), remap = false)
    void vmod$createNewConstraintTracker(VSJoint vsJoint, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() == null) {return;}
        VSJointsTracker.onCreateNewConstraint(vsJoint);
    }

    @Inject(method = "updateConstraint", at = @At(value = "HEAD"), remap = false)
    void vmod$updateConstraintTrackerTop(int constraintId, VSJoint updatedVSJoint, CallbackInfoReturnable<Boolean> cir, @Share("vmod$oldJoint") LocalRef<VSJoint> vmod$oldJoint) {
        vmod$oldJoint.set(((ShipObjectWorldAccessor)(Object)this).getConstraints().get(constraintId));
    }
    @Inject(method = "updateConstraint", at = @At(value = "RETURN"), remap = false)
    void vmod$updateConstraintTrackerRet(int constraintId, VSJoint updatedVSJoint, CallbackInfoReturnable<Boolean> cir, @Share("vmod$oldJoint") LocalRef<VSJoint> vmod$oldJoint) {
        if (cir.getReturnValue() == null) {return;}
        VSJointsTracker.onUpdateConstraint(constraintId, vmod$oldJoint.get(), updatedVSJoint);
    }

    @Inject(method = "removeConstraint", at = @At(value = "HEAD"), remap = false)
    void vmod$removeConstraintTrackerTop(int constraintId, CallbackInfoReturnable<Boolean> cir, @Share("vmod$toRemoveJoint") LocalRef<VSJoint> vmod$toRemoveJoint) {
        vmod$toRemoveJoint.set(((ShipObjectWorldAccessor)(Object)this).getConstraints().get(constraintId));
    }
    @Inject(method = "removeConstraint", at = @At(value = "RETURN"), remap = false)
    void vmod$removeConstraintTrackerRet(int constraintId, CallbackInfoReturnable<Boolean> cir, @Share("vmod$toRemoveJoint") LocalRef<VSJoint> vmod$toRemoveJoint) {
        if (cir.getReturnValue() == null) {return;}
        VSJointsTracker.onRemoveConstraint(constraintId, vmod$toRemoveJoint.get());
    }
}
