package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.extensions.RenderableExtension
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.ConnectionMConstraint
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.toolgun.modes.gui.ConnectionGUI
import net.spaceeye.vmod.toolgun.modes.hud.ConnectionHUD
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.toolgun.modes.*
import net.spaceeye.vmod.toolgun.modes.extensions.*
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.transformDirectionWorldToShipNoScaling
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color

class ConnectionMode: ExtendableToolgunMode(), ConnectionGUI, ConnectionHUD {
    @JsonIgnore private var i = 0

    var maxForce: Float by get(i++, -1f, { ServerLimits.instance.maxForce.get(it) })
    var stiffness: Float by get(i++, 0f, {ServerLimits.instance.stiffness.get(it)})
    var damping: Float by get(i++, 0f, {ServerLimits.instance.damping.get(it)})

    var width: Double by get(i++, .2, {DoubleLimit(0.01).get(it)}) //TODO

    var color: Color by get(i++, Color(62, 62, 62, 255))

    var fixedDistance: Float by get(i++, -1.0f, {ServerLimits.instance.fixedDistance.get(it)})
    var connectionMode: ConnectionMConstraint.ConnectionModes by get(i++, ConnectionMConstraint.ConnectionModes.FIXED_ORIENTATION)
    var primaryFirstRaycast: Boolean by get(i++, false)

    var posMode = PositionModes.NORMAL
    var precisePlacementAssistSideNum = 3

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->
        val wDir = (rpoint1 - rpoint2).normalize()
        val distance = if (fixedDistance < 0) {(rpoint1 - rpoint2).dist().toFloat()} else {fixedDistance}

        level.makeManagedConstraint(ConnectionMConstraint(
            spoint1, spoint2,
            ship1?.let { transformDirectionWorldToShipNoScaling(it, wDir) } ?: wDir.copy(),
            ship2?.let { transformDirectionWorldToShipNoScaling(it, wDir) } ?: wDir.copy(),
            Quaterniond(ship1?.transform?.shipToWorldRotation ?: Quaterniond()),
            Quaterniond(ship2?.transform?.shipToWorldRotation ?: Quaterniond()),
            shipId1, shipId2, maxForce, stiffness, damping, distance, connectionMode,
            listOf(prresult.blockPosition, rresult.blockPosition),
        ).addExtension(RenderableExtension(A2BRenderer(
            ship1?.id ?: -1L,
            ship2?.id ?: -1L,
            spoint1, spoint2,
            color, width
        ))).addExtension(Strippable())){it.addFor(player)}

        resetState()
    }

    override fun eResetState() {
        previousResult = null
        primaryFirstRaycast = false
    }

    companion object {
        val paNetworkingObj = PlacementAssistNetworking("connection_networking")
        init {
            ToolgunModes.registerWrapper(ConnectionMode::class) {
                it.addExtension<ConnectionMode> {
                    BasicConnectionExtension<ConnectionMode>("connection_mode"
                        ,allowResetting = true
                        ,primaryFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,primaryClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                        ,blockPrimary   = {inst -> inst.getExtensionOfType<PlacementAssistExtension>().paStage != ThreeClicksActivationSteps.FIRST_RAYCAST}
                        ,blockSecondary = {inst -> inst.primaryFirstRaycast}
                    )
                }.addExtension<ConnectionMode> {
                    BlockMenuOpeningExtension<ConnectionMode> { inst -> inst.primaryFirstRaycast }
                }.addExtension<ConnectionMode> {
                    PlacementAssistExtension(true, {mode -> it.posMode = mode}, {num -> it.precisePlacementAssistSideNum = num}, paNetworkingObj,
                        { spoint1: Vector3d, spoint2: Vector3d, rpoint1: Vector3d, rpoint2: Vector3d, ship1: ServerShip, ship2: ServerShip?, shipId1: ShipId, shipId2: ShipId, rresults: Pair<RaycastFunctions.RaycastResult, RaycastFunctions.RaycastResult>, paDistanceFromBlock: Double ->
                            val wDir = rresults.second.worldNormalDirection!!
                            ConnectionMConstraint(
                                spoint1, spoint2,
                                ship1?.let { transformDirectionWorldToShipNoScaling(it, wDir) } ?: wDir.copy(),
                                ship2?.let { transformDirectionWorldToShipNoScaling(it, wDir) } ?: wDir.copy(),
                                Quaterniond(ship1?.transform?.shipToWorldRotation ?: Quaterniond()),
                                Quaterniond(ship2?.transform?.shipToWorldRotation ?: Quaterniond()),
                                shipId1, shipId2, it.maxForce, it.stiffness, it.damping, it.fixedDistance, it.connectionMode,
                                listOf(rresults.first.blockPosition, rresults.second.blockPosition),
                            ).addExtension(Strippable())
                        }
                    )
                }
            }
        }
    }
}