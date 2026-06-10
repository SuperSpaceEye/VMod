package net.spaceeye.vmod.mixin;

import net.spaceeye.vmod.events.AVSEvents;
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.shadow.Eh;
import org.valkyrienskies.core.impl.shadow.Et;
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType;

import java.util.Objects;

@Mixin(Et.class)
abstract public class ShipObjectServerWorldMixin {
//    @Shadow @Final public List<PhysicsEntityServer> l;

    //let's hope that this works
    @Inject(method = "deleteShip", at = @At("HEAD"), remap = false)
    void vmod$onDeleteShip(ServerShip ship, CallbackInfo ci) {
        AVSEvents.INSTANCE.getServerShipRemoveEvent().emit(new AVSEvents.ServerShipRemoveEvent(ship));
    }

    @Unique int vmod$posX = 0;
    @Unique int vmod$posY = 0;
    @Unique int vmod$posZ = 0;
    @Unique String vmod$dimensionId = "";
    @Unique VsiBlockType vmod$oldBT = null;
    @Unique VsiBlockType vmod$newBT = null;
    @Unique double vmod$oldMass = 0.0;

    @Inject(method = "onSetBlock", at = @At(value = "HEAD"), remap = false)
    void vmod$onSetBlock(int posX, int posY, int posZ, String dimensionId, VsiBlockType oldBlockType, VsiBlockType newBlockType, double oldBlockMass, double newBlockMass, CallbackInfo ci) {
        vmod$posX = posX;
        vmod$posY = posY;
        vmod$posZ = posZ;
        vmod$dimensionId = dimensionId;
        vmod$oldBT = oldBlockType;
        vmod$newBT = newBlockType;
        vmod$oldMass = oldBlockMass;
    }

    @ModifyArg(method = "onSetBlock",
            at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/core/impl/shadow/Ev;onSetBlock(IIILjava/lang/String;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;DD)V"),
            remap = false,
            index = 6
    )
    double vmod$onSetBlock(double oldBlockMass) {
        Double mass = CustomBlockMassManager.INSTANCE.getCustomMass(vmod$dimensionId, vmod$posX, vmod$posY, vmod$posZ);
        if (vmod$oldBT != vmod$newBT) {
            CustomBlockMassManager.INSTANCE.removeCustomMass(vmod$dimensionId, vmod$posX, vmod$posY, vmod$posZ);
        }
        return Objects.requireNonNullElse(mass, vmod$oldMass);
    }

//    @ModifyArgs(
//            method = "onSetBlock",
//            at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/core/impl/shadow/Et;onSetBlock(IIILjava/lang/String;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;DD)V"),
//            remap = false
//    )
//    void vmod$onSetBlock(Args args) {
//        var posX = (int)args.get(0);
//        var posY = (int)args.get(1);
//        var posZ = (int)args.get(2);
//        var dimensionId = (String)args.get(3);
//        var oldBT = (VsiBlockType)args.get(4);
//        var newBT = (VsiBlockType)args.get(5);
//        var oldMass = (double)args.get(6);
//
//        Double mass = CustomBlockMassManager.INSTANCE.getCustomMass(dimensionId, posX, posY, posZ);
//        if (oldBT != newBT) {
//            CustomBlockMassManager.INSTANCE.removeCustomMass(dimensionId, posX, posY, posZ);
//        }
//
//        args.set(6, Objects.requireNonNullElse(mass, oldMass));
//    }
}
