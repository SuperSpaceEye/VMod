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
import net.spaceeye.vmod.toolgun.modes.gui.PhysRopeGUI
import net.spaceeye.vmod.toolgun.modes.hud.PhysRopeHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.PhysRopeCEH
import net.spaceeye.vmod.toolgun.modes.serializing.PhysRopeSerializable
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesState
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.getModePositions
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorld
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class PhysRopeMode: BaseMode, PhysRopeSerializable, PhysRopeCEH, PhysRopeGUI, PhysRopeHUD, PlacementModesState {
    override var posMode = PositionModes.NORMAL
    override var precisePlacementAssistSideNum: Int = 3
    override var precisePlacementAssistRendererId: Int = -1

    var compliance = 1e-20
    var maxForce = 1e10
    var fixedDistance = -1.0

    var segments: Int = 16
    var massPerSegment: Double = 1000.0
    var radius: Double = 0.5


    var primaryFirstRaycast = false



    val conn_primary = register { object : C2SConnection<PhysRopeMode>("phys_rope_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<PhysRopeMode>(context.player, buf, ::PhysRopeMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return}
        val (res, previousResult) = if (previousResult == null || primaryFirstRaycast) { previousResult = raycastResult; Pair(false, null) } else { Pair(true, previousResult) }
        if (!res) {return}

        val ship1 = level.getShipManagingPos(previousResult!!.blockPosition)
        val ship2 = level.getShipManagingPos(raycastResult.blockPosition)

        //allow for constraint to be created on the same ship, but not on the ground
        //why? because rendering is ship based, and it won't render shit that is connected only to the ground
        if (ship1 == null && ship2 == null) {resetState(); return}

        val shipId1: ShipId = ship1?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
        val shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        val (spoint1, spoint2) = getModePositions(posMode, previousResult, raycastResult)

        val rpoint1 = if (ship1 == null) spoint1 else posShipToWorld(ship1, Vector3d(spoint1))
        val rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, Vector3d(spoint2))

        val dist = if (fixedDistance > 0) {fixedDistance} else {(rpoint1 - rpoint2).dist()}


        level.makeManagedConstraint(PhysRopeMConstraint(
            shipId1, shipId2,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, dist, segments, massPerSegment, radius,
            listOf(previousResult.blockPosition, raycastResult.blockPosition),
        )).addFor(player)

        resetState()
    }

    override fun resetState() {
        previousResult = null
        primaryFirstRaycast = false
    }
}