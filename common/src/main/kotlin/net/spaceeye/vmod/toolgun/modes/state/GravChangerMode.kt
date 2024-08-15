package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.shipForceInducers.GravityController
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.GravChangerGUI
import net.spaceeye.vmod.toolgun.modes.hud.GravChangerHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.GravChangerCEH
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.traverseGetAllTouchingShips
import net.spaceeye.vmod.utils.vs.traverseGetConnectedShips
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class GravChangerMode: BaseMode, GravChangerCEH, GravChangerHUD, GravChangerGUI {
    enum class Mode {
        Individual,
        AllConnected,
        AllConnectedAndTouching
    }

    var gravityVector: Vector3d by get(0, Vector3d(0, -10, 0))
    var mode: Mode by get(1, Mode.Individual)

    val conn_primary = register { object : C2SConnection<GravChangerMode>("gravity_changer_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<GravChangerMode>(context.player, buf, ::GravChangerMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }
    val conn_secondary = register { object : C2SConnection<GravChangerMode>("gravity_changer_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<GravChangerMode>(context.player, buf, ::GravChangerMode) { item, serverLevel, player, raycastResult -> item.activateSecondaryFunction(serverLevel, player, raycastResult) } } }

    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val origin = level.shipObjectWorld.loadedShips.getById((level.getShipManagingPos(raycastResult.globalHitPos?.toBlockPos() ?: return) ?: return).id) ?: return

        val traversed = when (mode) {
            Mode.Individual -> {
                GravityController.getOrCreate(origin).gravityVector = Vector3d(gravityVector)
                return
            }
            Mode.AllConnected -> {
                traverseGetConnectedShips(origin.id).traversedShipIds
            }
            Mode.AllConnectedAndTouching -> {
                traverseGetAllTouchingShips(level, origin.id)
            }
        }

        traversed.forEach { id ->
            GravityController
                .getOrCreate(level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach)
                .gravityVector = Vector3d(gravityVector)
        }
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val origin = level.shipObjectWorld.loadedShips.getById((level.getShipManagingPos(raycastResult.globalHitPos?.toBlockPos() ?: return) ?: return).id) ?: return


        val traversed = when (mode) {
            Mode.Individual -> {
                GravityController.getOrCreate(origin).reset()
                return
            }
            Mode.AllConnected -> {
                traverseGetConnectedShips(origin.id).traversedShipIds
            }
            Mode.AllConnectedAndTouching -> {
                traverseGetAllTouchingShips(level, origin.id)
            }
        }

        traversed.forEach { id ->
            GravityController
                .getOrCreate(level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach)
                .reset()
        }
    }
}