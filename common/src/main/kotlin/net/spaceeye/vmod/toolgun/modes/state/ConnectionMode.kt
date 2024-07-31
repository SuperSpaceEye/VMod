package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.ConnectionMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.gui.ConnectionGUI
import net.spaceeye.vmod.toolgun.modes.hud.ConnectionHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.ConnectionCEH
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.utils.*
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color

object ConnectionNetworking: PlacementAssistNetworking("connection_networking")

class ConnectionMode: BaseMode, ConnectionCEH, ConnectionGUI, ConnectionHUD, PlacementAssistServerPart, PlacementAssistNetworkingUnit {
    var compliance: Double by get(0, 1e-20, {ServerLimits.instance.compliance.get(it as Double)})
    var maxForce: Double by get(1, 1e10, {ServerLimits.instance.maxForce.get(it as Double)})
    var width: Double by get(2, .2)

    var color: Color by get(3, Color(62, 62, 62, 255))

    var fixedDistance: Double by get(4, -1.0, {ServerLimits.instance.fixedDistance.get(it as Double)})
    var connectionMode: ConnectionMConstraint.ConnectionModes by get(5, ConnectionMConstraint.ConnectionModes.FIXED_ORIENTATION)
    var primaryFirstRaycast: Boolean by get(6, false)

    override var posMode: PositionModes by get(7, PositionModes.NORMAL)
    override var precisePlacementAssistSideNum: Int by get(8, 3, {ServerLimits.instance.precisePlacementAssistSides.get(it as Int)})

    override var paDistanceFromBlock: Double by get(9, 0.01, {ServerLimits.instance.distanceFromBlock.get(it as Double)})
    override var paStage: ThreeClicksActivationSteps by get(10, ThreeClicksActivationSteps.FIRST_RAYCAST)
    override var paAngle: Ref<Double> by get(11, Ref(0.0), customSerialize = {it, buf -> buf.writeDouble((it as Ref<Double>).it)}, customDeserialize = {buf -> paAngle.it = buf.readDouble(); paAngle})
    override var paScrollAngle: Double by get(12, Math.toRadians(10.0))

    val conn_primary   = register { object : C2SConnection<ConnectionMode>("connection_mode_primary",   "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<ConnectionMode>(context.player, buf, ::ConnectionMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }
    val conn_secondary = register { object : C2SConnection<ConnectionMode>("connection_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<ConnectionMode>(context.player, buf, ::ConnectionMode) {
            item, serverLevel, player, raycastResult ->
        item.activateFunctionPA(serverLevel, player, raycastResult)
    } } }

    override var precisePlacementAssistRendererId: Int = -1

    override var paFirstResult: RaycastFunctions.RaycastResult? = null
    override var paSecondResult: RaycastFunctions.RaycastResult? = null
    override var paCaughtShip: ClientShip? = null
    override var paCaughtShips: LongArray? = null
    override val paNetworkingObject: PlacementAssistNetworking = ConnectionNetworking
    override val paMConstraintBuilder =
        { spoint1: Vector3d, spoint2: Vector3d, rpoint1: Vector3d, rpoint2: Vector3d, ship1: ServerShip, ship2: ServerShip?, shipId1: ShipId, shipId2: ShipId, rresults: Pair<RaycastFunctions.RaycastResult, RaycastFunctions.RaycastResult> ->
            ConnectionMConstraint(
                spoint1, spoint2, rpoint1, rpoint2, ship1, ship2, shipId1, shipId2,
                compliance, maxForce, fixedDistance, connectionMode,
                listOf(rresults.first.blockPosition, rresults.second.blockPosition)
            )
        }

    override fun init(type: BaseNetworking.EnvType) {
        ConnectionNetworking.init(this, type)
    }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, precisePlacementAssistSideNum, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeManagedConstraint(ConnectionMConstraint(
            spoint1, spoint2, rpoint1, rpoint2,
            ship1, ship2, shipId1, shipId2,
            compliance, maxForce,
            fixedDistance, connectionMode,
            listOf(prresult.blockPosition, rresult.blockPosition),
            A2BRenderer(
                ship1 != null,
                ship2 != null,
                spoint1, spoint2,
                color, width
            )
        )){it.addFor(player)}

        resetState()
    }

    override fun resetState() {
        previousResult = null
        primaryFirstRaycast = false

        paServerResetState()
        paClientResetState()
    }
}