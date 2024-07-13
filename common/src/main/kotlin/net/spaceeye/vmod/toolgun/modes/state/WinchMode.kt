//package net.spaceeye.vmod.toolgun.modes.state
//
//import dev.architectury.networking.NetworkManager
//import net.minecraft.network.FriendlyByteBuf
//import net.minecraft.world.entity.player.Player
//import net.minecraft.world.level.Level
//import net.spaceeye.vmod.constraintsManaging.addFor
//import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
//import net.spaceeye.vmod.constraintsManaging.types.WinchMConstraint
//import net.spaceeye.vmod.networking.C2SConnection
//import net.spaceeye.vmod.rendering.types.A2BRenderer
//import net.spaceeye.vmod.toolgun.modes.BaseMode
//import net.spaceeye.vmod.toolgun.modes.BaseNetworking
//import net.spaceeye.vmod.toolgun.modes.gui.WinchGUI
//import net.spaceeye.vmod.toolgun.modes.hud.WinchHUD
//import net.spaceeye.vmod.toolgun.modes.serializing.WinchSerializable
//import net.spaceeye.vmod.toolgun.modes.eventsHandling.WinchCEH
//import net.spaceeye.vmod.toolgun.modes.util.*
//import net.spaceeye.vmod.utils.RaycastFunctions
//import net.spaceeye.vmod.utils.Ref
//import net.spaceeye.vmod.utils.Vector3d
//import org.valkyrienskies.core.api.ships.ClientShip
//import org.valkyrienskies.core.api.ships.ServerShip
//import org.valkyrienskies.core.api.ships.properties.ShipId
//import java.awt.Color

//object WinchNetworking: PlacementAssistNetworking("winch_networking")
//
//class WinchMode: BaseMode, WinchSerializable, WinchCEH, WinchGUI, WinchHUD, PlacementAssistServerPart, PlacementAssistNetworkingUnit {
//    var compliance: Double = 1e-20
//    var maxForce: Double = 1e10
//    var width: Double = .2
//
//    var color: Color = Color(189, 94, 46, 255)
//
//    var extensionDistance: Double = 5.0
//    var extensionSpeed: Double = 1.0
//
//    var fixedMinLength: Double = -1.0
//
//    var channel: String = "winch"
//
//    var messageModes = net.spaceeye.vmod.network.MessageModes.Toggle
//
//    var primaryFirstRaycast = false
//
//    override var posMode = PositionModes.NORMAL
//
//    override var paDistanceFromBlock = 0.01
//    override var paStage: ThreeClicksActivationSteps = ThreeClicksActivationSteps.FIRST_RAYCAST
//    override var paAngle: Ref<Double> = Ref(0.0)
//    override var paScrollAngle: Double = Math.toRadians(10.0)
//    override var paFirstResult: RaycastFunctions.RaycastResult? = null
//    override var paSecondResult: RaycastFunctions.RaycastResult? = null
//    override var paCaughtShip: ClientShip? = null
//    override var paCaughtShips: LongArray? = null
//    override val paNetworkingObject: PlacementAssistNetworking = HydraulicsNetworking
//    override val paMConstraintBuilder =
//            { spoint1: Vector3d, spoint2: Vector3d, rpoint1: Vector3d, rpoint2: Vector3d, ship1: ServerShip, ship2: ServerShip?, shipId1: ShipId, shipId2: ShipId, rresults: Pair<RaycastFunctions.RaycastResult, RaycastFunctions.RaycastResult> ->
//                WinchMConstraint(
//                        spoint1, spoint2, shipId1, shipId2,
//                        compliance, maxForce,
//                        paDistanceFromBlock, paDistanceFromBlock + extensionDistance,
//                        extensionSpeed, channel, messageModes,
//                        listOf(rresults.first.blockPosition, rresults.second.blockPosition),
//                        A2BRenderer(
//                            ship1 != null,
//                            ship2 != null,
//                            spoint1, spoint2,
//                            color, width
//                        )
//                )
//            }
//
//    override fun init(type: BaseNetworking.EnvType) {
//        WinchNetworking.init(this, type)
//    }
//
//    val conn_primary = register { object : C2SConnection<WinchMode>("winch_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<WinchMode>(context.player, buf, ::WinchMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }
//    val conn_secondary = register { object : C2SConnection<WinchMode>("winch_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<WinchMode>(context.player, buf, ::WinchMode) {
//        item, serverLevel, player, raycastResult ->
//        item.activateFunctionPA(serverLevel, player, raycastResult)
//    } } }
//
//    var previousResult: RaycastFunctions.RaycastResult? = null
//    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
//            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->
//
//        val minLength = if (fixedMinLength <= 0.0) (rpoint1 - rpoint2).dist() else fixedMinLength
//        level.makeManagedConstraint(WinchMConstraint(
//            spoint1, spoint2, shipId1, shipId2,
//            compliance, maxForce,
//            minLength, minLength + extensionDistance,
//            extensionSpeed, channel, messageModes,
//            listOf(prresult.blockPosition, rresult.blockPosition),
//            A2BRenderer(
//                ship1 != null,
//                ship2 != null,
//                spoint1, spoint2,
//                color, width
//            )
//        )).addFor(player)
//
//        resetState()
//    }
//
//    override fun resetState() {
//        previousResult = null
//        primaryFirstRaycast = false
//
//        paServerResetState()
//        paClientResetState()
//    }
//}