package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.types.DisabledCollisionMConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.DisableCollisionsGUIBuilder
import net.spaceeye.vmod.toolgun.modes.inputHandling.DisableCollisionsCRIHandler
import net.spaceeye.vmod.toolgun.modes.serializing.DisableCollisionsSerializable
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.mod.common.getShipManagingPos

class DisableCollisionsMode: BaseMode, DisableCollisionsSerializable, DisableCollisionsCRIHandler, DisableCollisionsGUIBuilder {
    val conn_primary = register { object : C2SConnection<DisableCollisionsMode>("disable_collisions_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<DisableCollisionsMode>(context.player, buf, ::DisableCollisionsMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }
    val conn_secondary = register { object : C2SConnection<DisableCollisionsMode>("disable_collisions_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<DisableCollisionsMode>(context.player, buf, ::DisableCollisionsMode) { item, serverLevel, player, raycastResult -> item.activateSecondaryFunction(serverLevel, player, raycastResult) } } }

    var previousResult: RaycastFunctions.RaycastResult? = null
    var primaryFirstRaycast = false

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(PositionModes.NORMAL, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeManagedConstraint(DisabledCollisionMConstraint(shipId1, shipId2)).addFor(player)
        resetState()
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return}
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return
        level.getAllDisabledCollisionsOfId(ship.id)?.forEach { (id, num) -> for (i in 0 until num) { level.enableCollisionBetween(ship.id, id) } }
    }

    override fun resetState() {
        previousResult = null
        primaryFirstRaycast = false
    }
}