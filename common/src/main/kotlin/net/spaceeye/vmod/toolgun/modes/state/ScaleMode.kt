package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.ScaleGUI
import net.spaceeye.vmod.toolgun.modes.hud.ScaleHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.ScaleCEH
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.toolgun.serializing.SerializableItem.get
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.teleportShipWithConnected
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.mod.common.shipObjectWorld

class ScaleMode: BaseMode, ScaleCEH, ScaleGUI, ScaleHUD {
    var scale: Double by get(0, 1.0)
    var scaleAllConnected: Boolean by get(1, true)

    val conn_primary = register { object : C2SConnection<ScaleMode>("scale_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<ScaleMode>(context.player, buf, ::ScaleMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel
        val ship: ServerShip = raycastResult.ship as ServerShip? ?: return

        if (scaleAllConnected) {
            teleportShipWithConnected(level, ship, Vector3d(ship.transform.positionInWorld), Quaterniond(ship.transform.shipToWorldRotation), scale)
        } else {
            level.shipObjectWorld.teleportShip(ship, ShipTeleportDataImpl(ship.transform.positionInWorld, ship.transform.shipToWorldRotation, ship.velocity, ship.omega, ship.chunkClaimDimension, scale))
        }
    }
}