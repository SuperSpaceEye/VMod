package net.spaceeye.vmod.fabric.mixin;

import net.takeoff.blockentity.BearingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServer;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;
import org.valkyrienskies.core.impl.hooks.VSEvents;
import org.valkyrienskies.core.impl.networking.RegisteredHandler;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(BearingBlockEntity.class)
public abstract class TakeoffBearingMixin {
    @Shadow private Integer attachConstraintId;
    @Shadow private Integer hingeConstraintId;

    @Inject(method = "destroyConstraints", at = @At(value = "HEAD"), remap = false, cancellable = true)
    public void vmodRedirect$destroyConstraints(CallbackInfo ci) {
        var level = ((BearingBlockEntity)(Object)this).getLevel();
        if (attachConstraintId != null) {
            try {
                ((ShipObjectServerWorld) VSGameUtilsKt.getShipObjectWorld(level)).removeConstraint(attachConstraintId);
            } catch (Exception ignored) {}
        }
        if (hingeConstraintId != null) {
            try {
                ((ShipObjectServerWorld) VSGameUtilsKt.getShipObjectWorld(level)).removeConstraint(hingeConstraintId);
            } catch (Exception ignored) {}
        }
        ci.cancel();
    }

    @Inject(method = "load$lambda$1$lambda$0", at = @At(value = "INVOKE", target = "Lnet/takeoff/blockentity/BearingBlockEntity;createConstraints(Lorg/valkyrienskies/core/api/ships/ServerShip;)Z"), remap = false, cancellable = true)
    private static void vmodRedirect$loadLambda(BearingBlockEntity this$0, ShipObjectServer $otherShip, VSEvents.ShipLoadEvent handler, RegisteredHandler ship, CallbackInfo ci) {
        this$0.createConstraints($otherShip);
        ci.cancel();
    }
}