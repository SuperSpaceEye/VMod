package net.spaceeye.vmod.mixin;

import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.spaceeye.vmod.VMKt.ELOG;

@Mixin(org.valkyrienskies.core.impl.game.ships.PhysShipImpl.class)
abstract class PhysShipImpl {
//    @Inject(method = "applyInvariantForce", at = @At("HEAD"), remap = false)
//    void vmod$applyInvariantForce(Vector3dc force, CallbackInfo ci) {
//        ELOG("applyInvariantForce force " + force.toString());
//    }
//
//    @Inject(method = "applyInvariantForceToPos", at = @At("HEAD"), remap = false)
//    void vmod$applyInvariantForceToPos(Vector3dc force, Vector3dc pos, CallbackInfo ci) {
//        ELOG("applyInvariantForceToPos force " + force.toString());
//    }
//
//    @Inject(method = "applyInvariantTorque", at = @At("HEAD"), remap = false)
//    void vmod$applyInvariantTorque(Vector3dc force, CallbackInfo ci) {
//        ELOG("applyInvariantTorque force " + force.toString());
//    }
//
//    @Inject(method = "applyRotDependentForce", at = @At("HEAD"), remap = false)
//    void vmod$applyRotDependentForce(Vector3dc force, CallbackInfo ci) {
//        ELOG("applyRotDependentForce force " + force.toString());
//    }
//
//    @Inject(method = "applyRotDependentForceToPos", at = @At("HEAD"), remap = false)
//    void vmod$applyRotDependentForceToPos(Vector3dc force, Vector3dc pos, CallbackInfo ci) {
//        ELOG("applyRotDependentForceToPos force " + force.toString());
//    }
//
//    @Inject(method = "applyRotDependentTorque", at = @At("HEAD"), remap = false)
//    void vmod$applyRotDependentTorque(Vector3dc force, CallbackInfo ci) {
//        ELOG("applyRotDependentTorque force " + force.toString());
//    }
}
