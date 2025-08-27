package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.rendering.ShipsColorModulator
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.ConstantClientRaycastingExtension
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.translate.SHIP_REMOVER
import net.spaceeye.vmod.translate.SHIP_REMOVER_HUD_1
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.shipObjectWorld

class ShipRemoverMode: ExtendableToolgunMode(), SimpleHUD {
    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.ship == null) {return}
        level.shipObjectWorld.deleteShip(raycastResult.ship as ServerShip)
    }

    override val itemName = SHIP_REMOVER

    override fun makeSubText(makeText: (String) -> Unit) {
        makeText(SHIP_REMOVER_HUD_1.get())
    }

    var clientLastShipId = -1L

    override fun eOnCloseMode() {
        ShipsColorModulator.deleteColor(clientLastShipId)
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(ShipRemoverMode::class) {
                it.addExtension {
                    BasicConnectionExtension<ShipRemoverMode>("ship_remover"
                        ,leftFunction = { mode, level, player, rr -> mode.activatePrimaryFunction(level, player, rr) }
                    )
                }.addExtension {
                    ConstantClientRaycastingExtension<ShipRemoverMode>({ mode, rr ->
                        if (mode.clientLastShipId != rr.shipId) {
                            ShipsColorModulator.deleteColor(mode.clientLastShipId)
                            ShipsColorModulator.setColor(rr.shipId, floatArrayOf(1f, 0.5f, 0.5f, 1f))
                            mode.clientLastShipId = rr.shipId
                        }
                    })
                }
            }
        }
    }
}