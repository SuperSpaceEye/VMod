package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.gui.PhysRopeGUI
import net.spaceeye.vmod.toolgun.modes.hud.PhysRopeHUD
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.getModePositions
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.types.PhysRopeRenderer
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.vEntityManaging.addFor
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.vEntityManaging.types.constraints.PhysRopeConstraint
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

class PhysRopeMode: ExtendableToolgunMode(), PhysRopeGUI, PhysRopeHUD {
    @JsonIgnore private var i = 0
    //    var compliance: Double by get(i++, 1e-20, { ServerLimits.instance.compliance.get(it) })
    var maxForce: Float by get(i++, 1e10f, { ServerLimits.instance.maxForce.get(it) })
    var fixedDistance: Float by get(i++, -1f, {ServerLimits.instance.fixedDistance.get(it)})

    var primaryFirstRaycast: Boolean by get(i++, false)

    var segments: Int by get(i++, 16, {ServerLimits.instance.physRopeSegments.get(it)})
    var massPerSegment: Double by get(i++, 1000.0, {ServerLimits.instance.physRopeMassPerSegment.get(it)})
    var radius: Double by get(i++, 0.5, {ServerLimits.instance.physRopeRadius.get(it)})


    val posMode: PositionModes get() = getExtensionOfType<PlacementModesExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementModesExtension>().precisePlacementAssistSideNum

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

        val dist = if (fixedDistance > 0) {fixedDistance} else {(rpoint1 - rpoint2).dist().toFloat()}

        PhysRopeConstraint(shipId1, shipId2, spoint1, spoint2, dist, segments, massPerSegment, radius)
            .also { it.addExtension(RenderableExtension(PhysRopeRenderer(shipId1, shipId2, spoint1, spoint2, Color(120, 0, 120), listOf()).addDelayedFn { r -> r.shipIds = it.entities.map { it.id }.toLongArray() })) }
            .addExtension(Strippable())
            .also {level.makeVEntity(it) {it.addFor(player)} }

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
                        ,leftFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,leftClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension<PhysRopeMode> {
                    PlacementModesExtension(false)
                }.addExtension<PhysRopeMode> {
                    BlockMenuOpeningExtension<PhysRopeMode> { inst -> inst.primaryFirstRaycast }
                }
            }
        }
    }
}