package net.spaceeye.vmod.toolgun.modes.state

import gg.essential.elementa.components.UIContainer
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.shipObjectWorld

class ShipRemoverMode: BasicMode<ShipRemoverMode>("ship_remover"), SimpleHUD {
    override fun getPrimaryFunction(): ((mode: ShipRemoverMode, level: ServerLevel, player: ServerPlayer, rr: RaycastFunctions.RaycastResult) -> Unit)? =
        {mode, level, player, rr -> (mode).activatePrimaryFunction(level, player, rr)}

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.ship == null) {return}
        level.shipObjectWorld.deleteShip(raycastResult.ship as ServerShip)
    }

    override val itemName get() = Component.literal("Ship Remover")

    override fun makeGUISettings(parentWindow: UIContainer) {}
    override fun makeSubText(makeText: (String) -> Unit) {
        makeText("LMB to delete ship")
    }
}