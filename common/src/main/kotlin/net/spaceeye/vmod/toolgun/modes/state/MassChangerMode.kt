package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.gui.MassChangerGUI
import net.spaceeye.vmod.toolgun.modes.hud.MassChangerHUD
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.shipAttachments.WeightSynchronizer
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.shipObjectWorld

class MassChangerMode: ExtendableToolgunMode(), MassChangerGUI, MassChangerHUD {
    @JsonIgnore private var i = 0

    var newMass: Double by get(i++, 1000.0) { ServerLimits.instance.massLimit.get(it) }
    var massPerBlock: Double by get(i++, 1.0) { ServerLimits.instance.massPerBlock.get(it) }

    var syncMassPerBlock: Boolean by get(i++, true).also { it.setWrapper = {old, new -> updateGuiFn(new); new} }
    var persistent: Boolean by get(i++, false)

    @JsonIgnore override var updateGuiFn: (Boolean) -> Unit = {}

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        val ship = raycastResult.ship as? ServerShip ?: return

        val lShip = level.shipObjectWorld.loadedShips.getById(ship.id)!!
        if (!persistent) {
            WeightSynchronizer.updateMass(level, ship, false, syncMassPerBlock, massPerBlock, newMass)
            lShip.removeAttachment<WeightSynchronizer>()
            return
        }

        val atch = WeightSynchronizer.getOrCreate(lShip)
        atch.massPerBlock = massPerBlock
        atch.targetTotalMass = newMass
        atch.syncMassPerBlock = syncMassPerBlock
        atch.syncWithConnectedStructure = false
        atch.level = level
        atch.updateWeights = true
    }

    fun activateSecondaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return}
        val ship = raycastResult.ship as? ServerShip ?: return

        val lShip = level.shipObjectWorld.loadedShips.getById(ship.id)
        if (lShip?.getAttachment<WeightSynchronizer>() != null) {
            lShip.removeAttachment<WeightSynchronizer>()
        }
        WeightSynchronizer.updateMass(level, ship, true, false, -1.0, -1.0)
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(MassChangerMode::class) {
                it.addExtension<MassChangerMode> {
                    BasicConnectionExtension<MassChangerMode>("ship_mass_changer"
                        ,leftFunction  = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,rightFunction = { inst, level, player, rr -> inst.activateSecondaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}