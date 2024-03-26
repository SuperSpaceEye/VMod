package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.getAllManagedConstraintIdsOfShipId
import net.spaceeye.vmod.constraintsManaging.removeManagedConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.StripGUIBuilder
import net.spaceeye.vmod.toolgun.modes.inputHandling.StripCRIHandler
import net.spaceeye.vmod.toolgun.modes.serializing.StripSerializable
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.mod.common.getShipManagingPos

class StripMode: BaseMode, StripSerializable, StripCRIHandler, StripGUIBuilder {
    val conn_primary = register { object : C2SConnection<StripMode>("strip_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<StripMode>(context.player, buf, ::StripMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return
        level.getAllManagedConstraintIdsOfShipId(ship.id).forEach { level.removeManagedConstraint(it) }
    }
}