package net.spaceeye.vmod.toolgun.modes.state

import gg.essential.elementa.components.UIContainer
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.shipObjectWorld

class ShipRemoverMode: ExtendableToolgunMode() {
    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.ship == null) {return}
        level.shipObjectWorld.deleteShip(raycastResult.ship as ServerShip)
    }

    override val itemName: TranslatableComponent
        get() = TranslatableComponent("Ship Remover")

    override fun eMakeHUD(screen: UIContainer) {
        object : SimpleHUD {
            override fun makeSubText(makeText: (String) -> Unit) {}
        }.makeHUD(screen)
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(ShipRemoverMode::class) {
                it as ExtendableToolgunMode;
                it.addExtension {
                    object : BasicConnectionExtension<ShipRemoverMode>("ship_remover") {
                        override fun getPrimaryFunction(): ((mode: ShipRemoverMode, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? =
                            { mode, level, player, rr -> mode.activatePrimaryFunction(level, player, rr) }
                    }
                }
            }
        }
    }
}