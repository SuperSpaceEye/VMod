package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.types.ThrusterMConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.rendering.types.ConeBlockRenderer
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.ThrusterGUI
import net.spaceeye.vmod.toolgun.modes.hud.ThrusterHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.ThrusterCEH
import net.spaceeye.vmod.toolgun.modes.serializing.ThrusterSerializable
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesState
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.getModePosition
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.getQuatFromDir
import org.valkyrienskies.mod.common.getShipManagingPos

class ThrusterMode: BaseMode, ThrusterSerializable, ThrusterCEH, ThrusterHUD, ThrusterGUI, PlacementModesState {
    override var posMode = PositionModes.NORMAL
    override var precisePlacementAssistSideNum: Int = 3
    override var precisePlacementAssistRendererId: Int = -1

    var force = 10000.0
    var channel = "thruster"


    val conn_primary = register { object : C2SConnection<ThrusterMode>("thruster_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<ThrusterMode>(context.player, buf, ::ThrusterMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel

        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return

        val pos = getModePosition(posMode, raycastResult, precisePlacementAssistSideNum)
        val basePos = pos + raycastResult.globalNormalDirection!! * 0.5

        level.makeManagedConstraint(ThrusterMConstraint(
            ship.id,
            basePos,
            raycastResult.blockPosition,
            -raycastResult.globalNormalDirection!!,
            force, channel,
            ConeBlockRenderer(
                basePos, getQuatFromDir(raycastResult.globalNormalDirection!!), 1.0f
            )
        )).addFor(player)
    }
}