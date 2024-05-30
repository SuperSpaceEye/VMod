package net.spaceeye.vmod.forge

import net.minecraft.server.level.ServerLevel
import net.takeoff.blockentity.BearingBlockEntity
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld

object TakeoffMixinCompat {
    fun redirectDestroyConstraints(ci: CallbackInfo, be: BearingBlockEntity, attachConstraintId: Int?, hingeConstraintId: Int?) {
        val level = be.level as ServerLevel
        if (attachConstraintId != null) {
            try { (Utils.getShipObjectWorld(level) as ShipObjectServerWorld).removeConstraint(attachConstraintId) } catch (ignored: Exception) {}
        }
        if (hingeConstraintId != null) {
            try { (Utils.getShipObjectWorld(level) as ShipObjectServerWorld).removeConstraint(hingeConstraintId) } catch (ignored: Exception) {}
        }
        ci.cancel()
    }
}