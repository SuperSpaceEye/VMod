package net.spaceeye.vmod.forge.mixin;

//import net.spaceeye.vmod.forge.TakeoffMixinCompat;
//import net.takeoff.blockentity.BearingBlockEntity;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.valkyrienskies.core.api.ships.ServerShip;
//import org.valkyrienskies.core.impl.game.ships.ShipObjectServer;
//import org.valkyrienskies.core.impl.hooks.VSEvents;
//import org.valkyrienskies.core.impl.networking.RegisteredHandler;

//@Mixin(BearingBlockEntity.class)
public abstract class TakeoffBearingMixin {
//    @Shadow private Integer attachConstraintId;
//    @Shadow private Integer hingeConstraintId;
//
//    @Inject(method = "destroyConstraints", at = @At(value = "HEAD"), remap = false, cancellable = true)
//    public void vmodRedirect$destroyConstraints(CallbackInfo ci) {
//        TakeoffMixinCompat.INSTANCE.redirectDestroyConstraints(ci, ((BearingBlockEntity)(Object)this), attachConstraintId, hingeConstraintId);
//    }
//
//    @Inject(method = "load$lambda$1$lambda$0", at = @At(value = "INVOKE", target = "Lnet/takeoff/blockentity/BearingBlockEntity;createConstraints(Lorg/valkyrienskies/core/api/ships/ServerShip;)Z"), remap = false, cancellable = true)
//    private static void vmodRedirect$loadLambda(BearingBlockEntity this$0, ShipObjectServer $otherShip, VSEvents.ShipLoadEvent handler, RegisteredHandler ship, CallbackInfo ci) {
//        this$0.createConstraints((ServerShip) $otherShip);
//        ci.cancel();
//    }
}
