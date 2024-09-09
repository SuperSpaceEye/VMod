package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.shipObjectWorld

class ShipRemoverMode: ExtendableToolgunMode(), SimpleHUD {
    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.ship == null) {return}
        level.shipObjectWorld.deleteShip(raycastResult.ship as ServerShip)
    }

    override val itemName get() = Component.literal("Ship Remover")

    override fun makeSubText(makeText: (String) -> Unit) {}

    companion object {
        init {
            ToolgunModes.registerWrapper(ShipRemoverMode::class) {
                it.addExtension<ShipRemoverMode> {
                    BasicConnectionExtension<ShipRemoverMode>("ship_remover"
                        ,primaryFunction = { mode, level, player, rr -> mode.activatePrimaryFunction(level, player, rr) }
                    )
                }
            }
        }
    }
}