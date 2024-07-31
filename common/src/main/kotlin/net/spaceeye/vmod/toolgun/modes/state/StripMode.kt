package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.ConstraintManager
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.getAllManagedConstraintIdsOfShipId
import net.spaceeye.vmod.constraintsManaging.removeManagedConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.StripGUI
import net.spaceeye.vmod.toolgun.modes.hud.StripHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.StripCEH
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.utils.RaycastFunctions
import org.valkyrienskies.mod.common.getShipManagingPos

class StripMode: BaseMode, StripCEH, StripGUI, StripHUD {
    enum class StripModes {
        StripAll,
        StripInRadius
    }

    var radius: Int by get(0, 1, {ServerLimits.instance.stripRadius.get(it as Int)})
    var mode: StripModes by get(1, StripModes.StripAll)

    val conn_primary = register { object : C2SConnection<StripMode>("strip_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<StripMode>(context.player, buf, ::StripMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        when (mode) {
            StripModes.StripAll -> stripAll(level as ServerLevel, raycastResult)
            StripModes.StripInRadius -> stripInRadius(level as ServerLevel, raycastResult)
        }
    }

    private fun stripAll(level: ServerLevel, raycastResult: RaycastFunctions.RaycastResult) {
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return

//        level.shipObjectWorld.deleteShip(ship)
//        return

        level.getAllManagedConstraintIdsOfShipId(ship.id).forEach { level.removeManagedConstraint(it) }
    }

    private fun stripInRadius(level: ServerLevel, raycastResult: RaycastFunctions.RaycastResult) {
        val instance = ConstraintManager.getInstance()

        val b = raycastResult.blockPosition
        val r = radius

        for (x in b.x-r .. b.x+r) {
        for (y in b.y-r .. b.y+r) {
        for (z in b.z-r .. b.z+r) {
            val list = instance.tryGetIdsOfPosition(BlockPos(x, y, z)) ?: continue
            val temp = mutableListOf<ManagedConstraintId>()
            temp.addAll(list)
            temp.forEach { level.removeManagedConstraint(it) }
        } } }
    }
}