package net.spaceeye.vmod.toolgun.modes.state

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.extensions.RenderableExtension
import net.spaceeye.vmod.constraintsManaging.extensions.SignalActivator
import net.spaceeye.vmod.constraintsManaging.extensions.Strippable
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.HydraulicsMConstraint
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.*
import net.spaceeye.vmod.toolgun.modes.gui.HydraulicsGUI
import net.spaceeye.vmod.toolgun.modes.hud.HydraulicsHUD
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color


object HydraulicsNetworking: PlacementAssistNetworking("hydraulics_networking")

class HydraulicsMode: ExtendableToolgunMode(), HydraulicsGUI, HydraulicsHUD {
    var compliance: Double by get(0, 1e-20, { ServerLimits.instance.compliance.get(it as Double) })
    var maxForce: Double by get(1, 1e10, { ServerLimits.instance.maxForce.get(it as Double) })
    var width: Double by get(2, .2, { DoubleLimit(0.01).get(it)})

    var color: Color by get(3, Color(62, 62, 200, 255))

    var fixedMinLength: Double by get(4, -1.0, {ServerLimits.instance.fixedDistance.get(it)})
    var connectionMode: HydraulicsMConstraint.ConnectionMode by get(5, HydraulicsMConstraint.ConnectionMode.FIXED_ORIENTATION)
    var primaryFirstRaycast: Boolean by get(6, false)

    var extensionDistance: Double by get(13, 5.0, {ServerLimits.instance.extensionDistance.get(it)})
    var extensionSpeed: Double by get(14, 1.0, {ServerLimits.instance.extensionSpeed.get(it)})
    var channel: String by get(15, "hydraulics", {ServerLimits.instance.channelLength.get(it)})

    var posMode = PositionModes.NORMAL
    var precisePlacementAssistSideNum = 3

    var previousResult: RaycastFunctions.RaycastResult? = null
    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val minLength = if (fixedMinLength <= 0.0) (rpoint1 - rpoint2).dist() else fixedMinLength
        level.makeManagedConstraint(HydraulicsMConstraint(
            spoint1, spoint2, rpoint1, rpoint2,
            ship1, ship2, shipId1, shipId2,
            compliance, maxForce,
            minLength, minLength + extensionDistance,
            extensionSpeed, channel, connectionMode,
            listOf(prresult.blockPosition, rresult.blockPosition)
        ).addExtension(RenderableExtension(A2BRenderer(
            ship1?.id ?: -1L,
            ship2?.id ?: -1L,
            spoint1, spoint2,
            color, width
        ))).addExtension(SignalActivator(
            "channel", "targetPercentage"
        )).addExtension(Strippable())){it.addFor(player)}

        resetState()
    }

    override fun eResetState() {
        previousResult = null
        primaryFirstRaycast = false
    }

    companion object {
        init {
            ToolgunModes.registerWrapper(HydraulicsMode::class) {
                it.addExtension<HydraulicsMode> {
                    BasicConnectionExtension<HydraulicsMode>("hydraulics_mode"
                        ,allowResetting = true
                        ,primaryFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,primaryClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                        ,blockPrimary   = {inst -> inst.getExtensionOfType<PlacementAssistExtension>().paStage != ThreeClicksActivationSteps.FIRST_RAYCAST}
                        ,blockSecondary = {inst -> inst.primaryFirstRaycast}
                    )
                }.addExtension<HydraulicsMode>{
                    BlockMenuOpeningExtension<HydraulicsMode> { inst -> inst.primaryFirstRaycast }
                }.addExtension<HydraulicsMode> {
                    PlacementAssistExtension(true, { mode -> it.posMode = mode}, { num -> it.precisePlacementAssistSideNum = num}, HydraulicsNetworking,
                        { spoint1: Vector3d, spoint2: Vector3d, rpoint1: Vector3d, rpoint2: Vector3d, ship1: ServerShip, ship2: ServerShip?, shipId1: ShipId, shipId2: ShipId, rresults: Pair<RaycastFunctions.RaycastResult, RaycastFunctions.RaycastResult>, paDistanceFromBlock: Double ->
                            HydraulicsMConstraint(
                                spoint1, spoint2, rpoint1, rpoint2, ship1, ship2, shipId1, shipId2,
                                it.compliance, it.maxForce,
                                paDistanceFromBlock, paDistanceFromBlock + it.extensionDistance,
                                it.extensionSpeed, it.channel, it.connectionMode,
                                listOf(rresults.first.blockPosition, rresults.second.blockPosition),
                                rresults.second.worldNormalDirection!!
                            ).addExtension(RenderableExtension(A2BRenderer(
                                ship1?.id ?: -1L,
                                ship2?.id ?: -1L,
                                spoint1, spoint2,
                                it.color, it.width
                            ))).addExtension(SignalActivator(
                                "channel", "targetPercentage"
                            )).addExtension(Strippable())
                        }
                    )
                }
            }
        }
    }
}