package net.spaceeye.vmod.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.spaceeye.vmod.vsStuff.VSJointsTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.apigame.joints.VSJoint;
import org.valkyrienskies.core.impl.shadow.FO;

@Mixin(FO.class)
public abstract class PhysLevelImpl {
    @Shadow public abstract VSJoint getJointById(int par1);

    @Inject(method = "addJoint", at = @At(value = "RETURN"), remap = false)
    void vmod$createNewConstraintTracker(VSJoint newJoint, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() == null) {return;}
        VSJointsTracker.onCreateNewConstraint(newJoint);
    }

    @Inject(method = "updateJoint", at = @At(value = "HEAD"), remap = false)
    void vmod$updateConstraintTrackerTop(int jointId, VSJoint updatedJoint, CallbackInfoReturnable<Boolean> cir, @Share("vmod$oldJoint") LocalRef<VSJoint> vmod$oldJoint) {
        vmod$oldJoint.set(this.getJointById(jointId));
    }
    @Inject(method = "updateJoint", at = @At(value = "RETURN"), remap = false)
    void vmod$updateConstraintTrackerRet(int jointId, VSJoint updatedVSJoint, CallbackInfoReturnable<Boolean> cir, @Share("vmod$oldJoint") LocalRef<VSJoint> vmod$oldJoint) {
        if (cir.getReturnValue() == null) {return;}
        VSJointsTracker.onUpdateConstraint(jointId, vmod$oldJoint.get(), updatedVSJoint);
    }

    @Inject(method = "removeJoint", at = @At(value = "HEAD"), remap = false)
    void vmod$removeConstraintTrackerTop(int jointId, CallbackInfoReturnable<Boolean> cir, @Share("vmod$toRemoveJoint") LocalRef<VSJoint> vmod$toRemoveJoint) {
        vmod$toRemoveJoint.set(this.getJointById(jointId));
    }
    @Inject(method = "removeJoint", at = @At(value = "RETURN"), remap = false)
    void vmod$removeConstraintTrackerRet(int jointId, CallbackInfoReturnable<Boolean> cir, @Share("vmod$toRemoveJoint") LocalRef<VSJoint> vmod$toRemoveJoint) {
        if (cir.getReturnValue() == null) {return;}
        VSJointsTracker.onRemoveConstraint(jointId, vmod$toRemoveJoint.get());
    }
}
