package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.PhysRopeMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.PhysRopeGUI
import net.spaceeye.vmod.toolgun.modes.hud.PhysRopeHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.PhysRopeCEH
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesState
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.getModePositions
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.toolgun.serializing.SerializableItem.get
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorld
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class PhysRopeMode: BaseMode, PhysRopeCEH, PhysRopeGUI, PhysRopeHUD, PlacementModesState {
    var compliance: Double by get(0, 1e-20, { ServerLimits.instance.compliance.get(it as Double) })
    var maxForce: Double by get(1, 1e10, { ServerLimits.instance.maxForce.get(it as Double) })
    var fixedDistance: Double by get(2, -1.0, {ServerLimits.instance.fixedDistance.get(it as Double)})

    override var posMode: PositionModes by get(3, PositionModes.NORMAL)
    override var precisePlacementAssistSideNum: Int by get(4, 3, {ServerLimits.instance.precisePlacementAssistSides.get(it as Int)})
    var primaryFirstRaycast: Boolean by get(5, false)

    var segments: Int by get(6, 16, {ServerLimits.instance.physRopeSegments.get(it as Int)})
    var massPerSegment: Double by get(7, 1000.0, {ServerLimits.instance.physRopeMassPerSegment.get(it as Double)})
    var radius: Double by get(8, 0.5, {ServerLimits.instance.physRopeRadius.get(it as Double)})

    val conn_primary = register { object : C2SConnection<PhysRopeMode>("phys_rope_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<PhysRopeMode>(context.player, buf, ::PhysRopeMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    override var precisePlacementAssistRendererId: Int = -1
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