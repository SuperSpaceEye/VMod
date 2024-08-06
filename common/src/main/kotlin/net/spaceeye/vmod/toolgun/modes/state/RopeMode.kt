package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.RopeMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.RopeGUI
import net.spaceeye.vmod.toolgun.modes.hud.RopeHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.RopeCEH
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesState
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.utils.RaycastFunctions

class RopeMode: BaseMode, RopeCEH, RopeGUI, RopeHUD, PlacementModesState {
    var compliance: Double by get(0, 1e-20, { ServerLimits.instance.compliance.get(it as Double) })
    var maxForce: Double by get(1, 1e10, { ServerLimits.instance.maxForce.get(it as Double) })
    var fixedDistance: Double by get(2, -1.0, {ServerLimits.instance.fixedDistance.get(it as Double)})

    override var posMode: PositionModes by get(3, PositionModes.NORMAL)
    override var precisePlacementAssistSideNum: Int by get(4, 3, {ServerLimits.instance.precisePlacementAssistSides.get(it as Int)})
    var primaryFirstRaycast: Boolean by get(5, false)

    var segments: Int by get(6, 16)
    var width: Double by get(7, .2)

    val conn_primary = register { object : C2SConnection<RopeMode>("rope_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<RopeMode>(context.player, buf, ::RopeMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    override var precisePlacementAssistRendererId: Int = -1
    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val dist = if (fixedDistance > 0) {fixedDistance} else {(rpoint1 - rpoint2).dist()}

        level.makeManagedConstraint(RopeMConstraint(
            shipId1, shipId2,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, dist,
            listOf(prresult.blockPosition, rresult.blockPosition),
            RopeRenderer(
                ship1?.id ?: -1L,
                ship2?.id ?: -1L,
                spoint1, spoint2,
                dist, width, segments
            )
        )){it.addFor(player)}

        resetState()
    }

    override fun resetState() {
        previousResult = null
        primaryFirstRaycast = false
    }
}