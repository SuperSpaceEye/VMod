package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.VSConstraintsKeeper
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.AxisMConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.S2CConnection
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.AxisGUIBuilder
import net.spaceeye.vmod.toolgun.modes.inputHandling.AxisCRIHandler
import net.spaceeye.vmod.toolgun.modes.serializing.AxisSerializable
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.transformProviders.CenteredAroundPlacementAssistTransformProvider
import net.spaceeye.vmod.transformProviders.PlacementAssistTransformProvider
import net.spaceeye.vmod.utils.*
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

object AxisNetworking {
    private var obj: AxisMode? = null
    fun init(mode: AxisMode) {if (obj == null) { obj = mode} }

    val s2cHandleFailure = "handle_failure" idWithConns {
        object : S2CConnection<S2CHandleFailurePacket>(it, "axis_networking") {
            override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val obj = obj ?: return
                obj.resetState()
            }
        }
    }

    // Client has no information about constraints, so server should send it to the client
    val s2cSendTraversalInfo = "send_traversal_info" idWithConns {
        object : S2CConnection<S2CSendTraversalInfo>(it, "axis_networking") {
            override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = S2CSendTraversalInfo(buf)
                if (obj!!.clientCaughtShip == null) {return}

                val primaryTransform = obj!!.clientCaughtShip!!.transformProvider
                if (primaryTransform !is PlacementAssistTransformProvider) { return }

                obj!!.clientCaughtShips = pkt.data.filter { it != obj!!.clientCaughtShip!!.id }.toLongArray()
                val clientShipObjectWorld = Minecraft.getInstance().shipObjectWorld

                obj!!.clientCaughtShips!!.forEach {
                    val ship = clientShipObjectWorld.allShips.getById(it)
                    ship?.transformProvider = CenteredAroundPlacementAssistTransformProvider(primaryTransform, ship!!)
                }
            }
        }
    }

    class S2CHandleFailurePacket(): Serializable {
        override fun serialize(): FriendlyByteBuf { return getBuffer() }
        override fun deserialize(buf: FriendlyByteBuf) {}
    }

    class S2CSendTraversalInfo(): Serializable {
        var data: LongArray = longArrayOf()

        constructor(buf: FriendlyByteBuf): this() { deserialize(buf) }
        constructor(data: MutableSet<ShipId>): this() { this.data = data.toLongArray() }
        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeLongArray(data)

            return buf
        }

        override fun deserialize(buf: FriendlyByteBuf) {
            data = buf.readLongArray()
        }
    }

    private infix fun <TT: Serializable> String.idWithConns(constructor: (String) -> S2CConnection<TT>): S2CConnection<TT> {
        val instance = constructor(this)
        try { // Why? so that if it's registered on dedicated client/server it won't die
            NetworkManager.registerReceiver(instance.side, instance.id, instance.getHandler())
        } catch(e: NoSuchMethodError) {}
        return instance
    }
}

class AxisMode: BaseMode, AxisSerializable, AxisCRIHandler, AxisGUIBuilder {
    var compliance: Double = 1e-20
    var maxForce: Double = 1e10
    var width: Double = .2

    var fixedDistance: Double = -1.0
    var disableCollisions: Boolean = false
    var distanceFromBlock = 0.01

    var posMode = PositionModes.NORMAL
    var primaryStage = ThreeClicksActivationSteps.FIRST_RAYCAST
    var secondaryFirstRaycast = false

    var primaryAngle = Ref(0.0)

    init {
        AxisNetworking.init(this)
    }

    var clientCaughtShip: ClientShip? = null
    var clientCaughtShips: LongArray? = null

    val conn_secondary = register { object : C2SConnection<AxisMode>("axis_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<AxisMode>(context.player, buf, ::AxisMode) { item, serverLevel, player, raycastResult -> item.activateSecondaryFunction(serverLevel, player, raycastResult) } } }
    val conn_primary   = register { object : C2SConnection<AxisMode>("axis_mode_primary",   "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<AxisMode>(context.player, buf, ::AxisMode) {
            item, serverLevel, player, raycastResult ->
        activatePrimaryFunction(serverLevel, player, raycastResult)
    } } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activateSecondaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, level, raycastResult, { if (previousResult == null || secondaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeManagedConstraint(AxisMConstraint(
            spoint1, spoint2, rpoint1, rpoint2,
            ship1, ship2, shipId1, shipId2,
            compliance, maxForce,
            fixedDistance,
            disableCollisions,
            listOf(prresult.blockPosition, rresult.blockPosition),
            A2BRenderer(
                ship1 != null,
                ship2 != null,
                spoint1, spoint2,
                Color(0, 200, 0),
                width
            )
        )).addFor(player)

        resetState()
    }

    var firstResult: RaycastFunctions.RaycastResult? = null
    var secondResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (level !is ServerLevel) {throw RuntimeException("Function intended for server use only was activated on client. How.")}
        when (primaryStage) {
            ThreeClicksActivationSteps.SECOND_RAYCAST -> primaryFunctionFirst (level, player, raycastResult)
            ThreeClicksActivationSteps.FINALIZATION   -> primaryFunctionSecond(level, player, raycastResult)
            ThreeClicksActivationSteps.FIRST_RAYCAST  -> primaryFunctionThird (level, player, raycastResult)
        }
    }

    private fun handleFailure(player: Player) {
        AxisNetworking.s2cHandleFailure.sendToClient(player as ServerPlayer, AxisNetworking.S2CHandleFailurePacket())
        resetState()
    }

    private fun primaryFunctionFirst(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return handleFailure(player)}
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return handleFailure(player)
        firstResult = raycastResult
        val traversed = VSConstraintsKeeper.traverseGetConnectedShips(ship.id)
        AxisNetworking.s2cSendTraversalInfo.sendToClient(player as ServerPlayer, AxisNetworking.S2CSendTraversalInfo(traversed))
    }

    private fun primaryFunctionSecond(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (raycastResult.state.isAir) {return handleFailure(player) }
        val ship = level.getShipManagingPos(raycastResult.blockPosition)
        if (ship == level.getShipManagingPos(firstResult?.blockPosition ?: return handleFailure(player))) {return handleFailure(player)}
        secondResult = raycastResult
    }

    private fun primaryFunctionThird(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (firstResult == null || secondResult == null) {return handleFailure(player)}

        val firstResult = firstResult!!
        val secondResult = secondResult!!

        val ship1 = level.getShipManagingPos(firstResult.blockPosition)
        val ship2 = level.getShipManagingPos(secondResult.blockPosition)

        if (ship1 == null) {return handleFailure(player)}
        if (ship1 == ship2) {return handleFailure(player)}

        // not sure why i need to flip normal but it works
        val dir1 =  when {
            firstResult.globalNormalDirection!!.y ==  1.0 -> -firstResult.globalNormalDirection!!
            firstResult.globalNormalDirection!!.y == -1.0 -> -firstResult.globalNormalDirection!!
            else -> firstResult.globalNormalDirection!!
        }
        val dir2 = if (ship2 != null) { transformDirectionShipToWorld(ship2, secondResult.globalNormalDirection!!) } else secondResult.globalNormalDirection!!

        val angle = Quaterniond(AxisAngle4d(primaryAngle.it, dir2.toJomlVector3d()))

        val rotation = Quaterniond()
            .mul(angle)
            .mul(getQuatFromDir(dir2))
            .mul(getQuatFromDir(dir1))
            .normalize()


        val (spoint1, spoint2) = getModePositions(if (posMode == PositionModes.NORMAL) {posMode} else {PositionModes.CENTERED_ON_SIDE}, firstResult, secondResult)
        var rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, Vector3d(spoint2))

        // rotation IS IMPORTANT, so make a new transform with new rotation to translate points
        val newTransform = (ship1.transform as ShipTransformImpl).copy(shipToWorldRotation = rotation)

        // we cannot modify position in ship, we can, however, modify position in world
        // this translates ship so that after teleportation its spoint1 will be at rpoint2
        val point = rpoint2 - (
            posShipToWorld(ship1, spoint1, newTransform) - Vector3d(ship1.transform.positionInWorld)
        )

        teleportShipWithConnected(level, ship1, point, rotation)

        rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, Vector3d(spoint2))
        val rpoint1 = Vector3d(rpoint2) + dir2.normalize() * distanceFromBlock

        val shipId1: ShipId = ship1.id
        val shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        val constraint = AxisMConstraint(
            spoint1, spoint2, rpoint1, rpoint2, ship1, ship2, shipId1, shipId2,
            compliance, maxForce, fixedDistance, disableCollisions,
            listOf(firstResult.blockPosition, secondResult.blockPosition)
        )

        level.makeManagedConstraint(constraint).addFor(player)
        resetState()
    }

    fun resetState() {
        primaryStage = ThreeClicksActivationSteps.FIRST_RAYCAST
        if (clientCaughtShip != null) {
            clientCaughtShip!!.transformProvider = null
            clientCaughtShip = null
        }
        previousResult = null
        firstResult = null
        secondResult = null
        secondaryFirstRaycast = false
        clientCaughtShips?.forEach {
            Minecraft.getInstance().shipObjectWorld.allShips.getById(it)?.transformProvider = null
        }
        clientCaughtShips = null
    }
}