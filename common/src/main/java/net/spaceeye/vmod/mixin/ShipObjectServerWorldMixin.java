package net.spaceeye.vmod.mixin;

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
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.world.chunks.BlockType;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

import java.util.List;
import java.util.Objects;


@Mixin(ShipObjectServerWorld.class)
abstract public class ShipObjectServerWorldMixin {
    //first instance, can be double-checked by finding deleteShip fn
    @Final
    @Shadow
    private List<ShipData> deletedShipObjects; // deletedShipObjects


    // it's called before they get deleted
    // should be second result with first line smth like "this.e.a(Ep.c.POST_TICK_START);"
    @Inject(method = "postTick", at = @At(value = "FIELD", target = "Lorg/valkyrienskies/core/impl/game/ships/ShipObjectServerWorld;deletedShipObjects:Ljava/util/List;"), remap = false)
    void vmod_postTickMixin(CallbackInfo ci) {
        if (deletedShipObjects == null) {return;}
        deletedShipObjects.forEach((data) -> AVSEvents.INSTANCE.getServerShipRemoveEvent().emit(new AVSEvents.ServerShipRemoveEvent(data)));
    }

    @ModifyArgs(
            method = "onSetBlock",
            at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/core/impl/game/ships/ShipObjectWorld;onSetBlock(IIILjava/lang/String;Lorg/valkyrienskies/core/apigame/world/chunks/BlockType;Lorg/valkyrienskies/core/apigame/world/chunks/BlockType;DD)V"),
            remap = false
    )
    void vmod$onSetBlock(Args args, int posX, int posY, int posZ, String dimensionId, BlockType oldBlockType, BlockType newBlockType, double oldBlockMass, double newBlockMass) {
        Double mass = CustomBlockMassManager.INSTANCE.getCustomMass(dimensionId, posX, posY, posZ);
        if (oldBlockType != newBlockType) {
            CustomBlockMassManager.INSTANCE.removeCustomMass(dimensionId, posX, posY, posZ);
        }
        args.set(6, Objects.requireNonNullElse(mass, oldBlockMass));
    }

    @Inject(method = "createNewConstraint", at = @At(value = "RETURN"), remap = false)
    void vmod$createNewConstraintTracker(VSConstraint vsJoint, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() == null) {return;}
        VSJointsTracker.onCreateNewConstraint(vsJoint);
    }

    @Inject(method = "updateConstraint", at = @At(value = "HEAD"), remap = false)
    void vmod$updateConstraintTrackerTop(int constraintId, VSConstraint updatedVSJoint, CallbackInfoReturnable<Boolean> cir, @Share("vmod$oldJoint") LocalRef<VSConstraint> vmod$oldJoint) {
        vmod$oldJoint.set(((ShipObjectWorldAccessor)(Object)this).getConstraints().get(constraintId));
    }
    @Inject(method = "updateConstraint", at = @At(value = "RETURN"), remap = false)
    void vmod$updateConstraintTrackerRet(int constraintId, VSConstraint updatedVSJoint, CallbackInfoReturnable<Boolean> cir, @Share("vmod$oldJoint") LocalRef<VSConstraint> vmod$oldJoint) {
        if (cir.getReturnValue() == null) {return;}
        VSJointsTracker.onUpdateConstraint(constraintId, vmod$oldJoint.get(), updatedVSJoint);
    }

    @Inject(method = "removeConstraint", at = @At(value = "HEAD"), remap = false)
    void vmod$removeConstraintTrackerTop(int constraintId, CallbackInfoReturnable<Boolean> cir, @Share("vmod$toRemoveJoint") LocalRef<VSConstraint> vmod$toRemoveJoint) {
        vmod$toRemoveJoint.set(((ShipObjectWorldAccessor)(Object)this).getConstraints().get(constraintId));
    }
    @Inject(method = "removeConstraint", at = @At(value = "RETURN"), remap = false)
    void vmod$removeConstraintTrackerRet(int constraintId, CallbackInfoReturnable<Boolean> cir, @Share("vmod$toRemoveJoint") LocalRef<VSConstraint> vmod$toRemoveJoint) {
        if (cir.getReturnValue() == null) {return;}
        VSJointsTracker.onRemoveConstraint(constraintId, vmod$toRemoveJoint.get());
    }
}
