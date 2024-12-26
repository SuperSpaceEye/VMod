package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.PhysRopeMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.gui.PhysRopeGUI
import net.spaceeye.vmod.toolgun.modes.hud.PhysRopeHUD
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.getModePositions
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorld
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class PhysRopeMode: ExtendableToolgunMode(), PhysRopeGUI, PhysRopeHUD {
    var compliance: Double by get(0, 1e-20, { ServerLimits.instance.compliance.get(it) })
    var maxForce: Double by get(1, 1e10, { ServerLimits.instance.maxForce.get(it) })
    var fixedDistance: Double by get(2, -1.0, {ServerLimits.instance.fixedDistance.get(it)})

    var primaryFirstRaycast: Boolean by get(3, false)

    var segments: Int by get(4, 16, {ServerLimits.instance.physRopeSegments.get(it)})
    var massPerSegment: Double by get(5, 1000.0, {ServerLimits.instance.physRopeMassPerSegment.get(it)})
    var radius: Double by get(6, 0.5, {ServerLimits.instance.physRopeRadius.get(it)})


    var posMode: PositionModes = PositionModes.NORMAL
    var precisePlacementAssistSideNum: Int = 3

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
        ).addExtension(Strippable())){it.addFor(player)}

        resetState()
    }

    override fun eResetState() {
        previousResult = null
        primaryFirstRaycast = false
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(PhysRopeMode::class) {
                it.addExtension<PhysRopeMode> {
                    BasicConnectionExtension<PhysRopeMode>("phys_rope_mode"
                        ,allowResetting = true
                        ,primaryFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,primaryClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension<PhysRopeMode> {
                    PlacementModesExtension(false, {mode -> it.posMode = mode}, {num -> it.precisePlacementAssistSideNum = num})
                }.addExtension<PhysRopeMode> {
                    BlockMenuOpeningExtension<PhysRopeMode> { inst -> inst.primaryFirstRaycast }
                }
            }
        }
    }
}