package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.PhysRopeMConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.PhysRopeGUIBuilder
import net.spaceeye.vmod.toolgun.modes.inputHandling.PhysRopeCRIHandler
import net.spaceeye.vmod.toolgun.modes.serializing.PhysRopeSerializable
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions

class PhysRopeMode: BaseMode, PhysRopeSerializable, PhysRopeCRIHandler, PhysRopeGUIBuilder {
    var compliance = 1e-20
    var maxForce = 1e10
    var fixedDistance = -1.0

    var posMode = PositionModes.NORMAL

    var width: Double = .2
    var segments: Int = 16
    var massPerSegment: Double = 1000.0
    var radius: Double = 0.5


    var primaryFirstRaycast = false



    val conn_primary = register { object : C2SConnection<PhysRopeMode>("phys_rope_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<PhysRopeMode>(context.player, buf, ::PhysRopeMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val dist = if (fixedDistance > 0) {fixedDistance} else {(rpoint1 - rpoint2).dist()}


        level.makeManagedConstraint(PhysRopeMConstraint(
            shipId1, shipId2,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, dist, segments, massPerSegment, radius,
            listOf(prresult.blockPosition, rresult.blockPosition),
        )).addFor(player)

        resetState()
    }

    override fun resetState() {
        previousResult = null
        primaryFirstRaycast = false
    }
}