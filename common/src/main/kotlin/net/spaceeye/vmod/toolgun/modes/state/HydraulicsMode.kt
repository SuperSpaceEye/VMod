package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.vEntityManaging.addFor
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.extensions.SignalActivator
import net.spaceeye.vmod.vEntityManaging.extensions.Strippable
import net.spaceeye.vmod.vEntityManaging.makeVEntity
import net.spaceeye.vmod.vEntityManaging.types.constraints.HydraulicsConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.toolgun.gui.Presettable
import net.spaceeye.vmod.toolgun.gui.Presettable.Companion.presettable
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.*
import net.spaceeye.vmod.toolgun.modes.gui.HydraulicsGUI
import net.spaceeye.vmod.toolgun.modes.hud.HydraulicsHUD
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.transformDirectionWorldToShip
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color

object HydraulicsNetworking: PlacementAssistNetworking("hydraulics_networking")

class HydraulicsMode: ExtendableToolgunMode(), HydraulicsGUI, HydraulicsHUD {
    @JsonIgnore private var i = 0

    var maxForce: Float by get(i++, -1f) { ServerLimits.instance.maxForce.get(it) }.presettable()
    var stiffness: Float by get(i++, -1f) { ServerLimits.instance.stiffness.get(it) }.presettable()
    var damping: Float by get(i++, -1f) { ServerLimits.instance.damping.get(it) }.presettable()

    var width: Double by get(i++, .2).presettable()

    var color: Color by get(i++, Color(62, 62, 200, 255)).presettable()
    var fullbright: Boolean by get(i++, false).presettable()

    var fixedMinLength: Float by get(i++, -1f) { ServerLimits.instance.fixedDistance.get(it) }.presettable()
    var connectionMode: HydraulicsConstraint.ConnectionMode by get(i++, HydraulicsConstraint.ConnectionMode.FIXED_ORIENTATION).presettable()

    var extensionDistance: Float by get(i++, 5f) { ServerLimits.instance.extensionDistance.get(it) }.presettable()
    var extensionSpeed: Float by get(i++, 1f) { ServerLimits.instance.extensionSpeed.get(it) }.presettable()
    var channel: String by get(i++, "hydraulics") { ServerLimits.instance.channelLength.get(it) }.presettable()

    var primaryFirstRaycast: Boolean by get(i++, false)

    val posMode: PositionModes get() = getExtensionOfType<PlacementAssistExtension>().posMode
    val precisePlacementAssistSideNum: Int get() = getExtensionOfType<PlacementAssistExtension>().precisePlacementAssistSideNum
    val paMiddleFirstRaycast: Boolean get() = getExtensionOfType<PlacementAssistExtension>().middleFirstRaycast

    var previousResult: RaycastFunctions.RaycastResult? = null
    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val minLength = if (fixedMinLength < 0.0) (rpoint2 - rpoint1).dist().toFloat() else fixedMinLength
        val wDir = (rpoint2 - rpoint1).normalize()

        level.makeVEntity(HydraulicsConstraint(
            spoint1, spoint2, //directions get scaled
            ship1?.let { transformDirectionWorldToShip(it, wDir) } ?: wDir.copy(),
            ship2?.let { transformDirectionWorldToShip(it, wDir) } ?: wDir.copy(),
            ship1?.transform?.shipToWorldRotation ?: Quaterniond(),
            ship2?.transform?.shipToWorldRotation ?: Quaterniond(),
            shipId1, shipId2,
            maxForce, stiffness, damping,
            minLength, minLength + extensionDistance,
            extensionSpeed, channel, connectionMode
        ).addExtension(RenderableExtension(A2BRenderer(
            ship1?.id ?: -1L,
            ship2?.id ?: -1L,
            spoint1, spoint2,
            color, width, fullbright, RenderingUtils.whiteTexture
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
                it.addExtension {
                    BasicConnectionExtension<HydraulicsMode>("hydraulics_mode"
                        ,allowResetting = true
                        ,leftFunction       = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }
                        ,leftClientCallback = { inst -> inst.primaryFirstRaycast = !inst.primaryFirstRaycast; inst.refreshHUD() }
                    )
                }.addExtension{
                    BlockMenuOpeningExtension<HydraulicsMode> { inst -> inst.primaryFirstRaycast || inst.paMiddleFirstRaycast }
                }.addExtension {
                    PlacementAssistExtension(true, HydraulicsNetworking,
                        { (it as HydraulicsMode).primaryFirstRaycast },
                        { (it as HydraulicsMode).connectionMode == HydraulicsConstraint.ConnectionMode.HINGE_ORIENTATION },
                        { spoint1: Vector3d, spoint2: Vector3d, rpoint1: Vector3d, rpoint2: Vector3d, ship1: ServerShip, ship2: ServerShip?, shipId1: ShipId, shipId2: ShipId, rresults: Pair<RaycastFunctions.RaycastResult, RaycastFunctions.RaycastResult>, paDistanceFromBlock: Double ->
                            HydraulicsConstraint(
                                spoint1, spoint2, //scale directions manually
                                 rresults.first .globalNormalDirection!! / (ship1?.transform?.scaling?.x() ?: 1.0),
                                -rresults.second.globalNormalDirection!! / (ship2?.transform?.scaling?.x() ?: 1.0),
                                ship1?.transform?.shipToWorldRotation ?: Quaterniond(),
                                ship2?.transform?.shipToWorldRotation ?: Quaterniond(),
                                shipId1, shipId2,
                                it.maxForce, it.stiffness, it.damping,
                                paDistanceFromBlock.toFloat(), paDistanceFromBlock.toFloat() + it.extensionDistance,
                                it.extensionSpeed, it.channel, it.connectionMode
                            ).addExtension(RenderableExtension(A2BRenderer(
                                ship1?.id ?: -1L,
                                ship2?.id ?: -1L,
                                spoint1, spoint2,
                                it.color, it.width, it.fullbright, RenderingUtils.whiteTexture
                            ))).addExtension(SignalActivator(
                                "channel", "targetPercentage"
                            )).addExtension(Strippable())
                        }
                    )
                }.addExtension { Presettable() }
            }
        }
    }
}