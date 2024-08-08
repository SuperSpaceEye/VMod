package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.extensions.RenderableExtension
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.HydraulicsMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.gui.HydraulicsGUI
import net.spaceeye.vmod.toolgun.modes.hud.HydraulicsHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.HydraulicsCEH
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Ref
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color

object HydraulicsNetworking: PlacementAssistNetworking("hydraulics_networking")

class HydraulicsMode: BaseMode, HydraulicsCEH, HydraulicsGUI, HydraulicsHUD, PlacementAssistServerPart, PlacementAssistNetworkingUnit {
    var compliance: Double by get(0, 1e-20, { ServerLimits.instance.compliance.get(it as Double) })
    var maxForce: Double by get(1, 1e10, { ServerLimits.instance.maxForce.get(it as Double) })
    var width: Double by get(2, .2)

    var color: Color by get(3, Color(62, 62, 200, 255))

    var fixedMinLength: Double by get(4, -1.0, {ServerLimits.instance.fixedDistance.get(it as Double)})
    var connectionMode: HydraulicsMConstraint.ConnectionMode by get(5, HydraulicsMConstraint.ConnectionMode.FIXED_ORIENTATION)
    var primaryFirstRaycast: Boolean by get(6, false)

    override var posMode: PositionModes by get(7, PositionModes.NORMAL)
    override var precisePlacementAssistSideNum: Int by get(8, 3, {ServerLimits.instance.precisePlacementAssistSides.get(it as Int)})

    override var paDistanceFromBlock: Double by get(9, 0.01, {ServerLimits.instance.distanceFromBlock.get(it as Double)})
    override var paStage: ThreeClicksActivationSteps by get(10, ThreeClicksActivationSteps.FIRST_RAYCAST)
    override var paAngle: Ref<Double> by get(11, Ref(0.0), customSerialize = {it, buf -> buf.writeDouble((it as Ref<Double>).it)}, customDeserialize = {buf -> paAngle.it = buf.readDouble(); paAngle})
    override var paScrollAngle: Double by get(12, Math.toRadians(10.0))

    var extensionDistance: Double by get(13, 5.0, {ServerLimits.instance.extensionDistance.get(it as Double)})
    var extensionSpeed: Double by get(14, 1.0, {ServerLimits.instance.extensionSpeed.get(it as Double)})
    var channel: String by get(15, "hydraulics", {ServerLimits.instance.channelLength.get(it as String)})

    val conn_primary = register { object : C2SConnection<HydraulicsMode>("hydraulics_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<HydraulicsMode>(context.player, buf, ::HydraulicsMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }
    val conn_secondary = register { object : C2SConnection<HydraulicsMode>("hydraulics_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<HydraulicsMode>(context.player, buf, ::HydraulicsMode) {
            item, serverLevel, player, raycastResult ->
        item.activateFunctionPA(serverLevel, player, raycastResult)
    } } }

    override var precisePlacementAssistRendererId: Int = -1

    override var paFirstResult: RaycastFunctions.RaycastResult? = null
    override var paSecondResult: RaycastFunctions.RaycastResult? = null
    override var paCaughtShip: ClientShip? = null
    override var paCaughtShips: LongArray? = null
    override val paNetworkingObject: PlacementAssistNetworking = HydraulicsNetworking
    override val paMConstraintBuilder =
            { spoint1: Vector3d, spoint2: Vector3d, rpoint1: Vector3d, rpoint2: Vector3d, ship1: ServerShip, ship2: ServerShip?, shipId1: ShipId, shipId2: ShipId, rresults: Pair<RaycastFunctions.RaycastResult, RaycastFunctions.RaycastResult> ->
                HydraulicsMConstraint(
                        spoint1, spoint2, rpoint1, rpoint2, ship1, ship2, shipId1, shipId2,
                        compliance, maxForce,
                        paDistanceFromBlock, paDistanceFromBlock + extensionDistance,
                        extensionSpeed, channel, connectionMode,
                        listOf(rresults.first.blockPosition, rresults.second.blockPosition)
                ).addExtension(RenderableExtension(A2BRenderer(
                    ship1?.id ?: -1L,
                    ship2?.id ?: -1L,
                    spoint1, spoint2,
                    color, width
                )))
            }

    override fun init(type: BaseNetworking.EnvType) {
        HydraulicsNetworking.init(this, type)
    }

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
        )))){it.addFor(player)}

        resetState()
    }

    override fun resetState() {
        previousResult = null
        primaryFirstRaycast = false

        paServerResetState()
        paClientResetState()
    }
}