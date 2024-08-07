package net.spaceeye.vmod.physgun

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.networking.AutoSerializable
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.shipForceInducers.PhysgunController
import net.spaceeye.vmod.toolgun.ServerToolGunState.idWithConnc
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.traverseGetConnectedShips
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max

fun playerRotToQuat(pitch: Double, yaw: Double): Quaterniond {
    return Quaterniond().rotateY(Math.toRadians(-yaw)).rotateX(Math.toRadians(pitch))  
}

data class PlayerPhysgunState(
    var lock: ReentrantLock = ReentrantLock(),

    var serverPlayer: ServerPlayer? = null,
    var primaryActivated: Boolean = false,
    var rotate: Boolean = false,
    var freezeSelected: Boolean = false,
    var freezeAll: Boolean = false,
    var unfreezeAllOrOne: Boolean = false,
    var preciseRotation: Boolean = false,

    var quatDiff: Quaterniond = Quaterniond(),
    var increaseDistanceBy: Double = 0.0,

    var distanceFromPlayer: Double = 0.0,
    var playerPos: Vector3d = Vector3d(),
    var playerDir: Vector3d = Vector3d(),
    var playerLastRot: Quaterniond = Quaterniond(),

    var fromPos: Vector3d = Vector3d(),
    var idealRotation: Quaterniond = Quaterniond(),
    var mainShipId: ShipId = -1,
    var caughtShipIds: MutableList<ShipId> = mutableListOf()
) {
    fun fromPkt(player: ServerPlayer, pkt: ServerPhysgunState.C2SPhysgunStateChanged) {
        serverPlayer = player
        primaryActivated = pkt.primaryActivated
        rotate = pkt.rotate
        freezeSelected = pkt.freezeSelected
        freezeAll = pkt.freezeAll
        unfreezeAllOrOne = pkt.unfreezeAllOrOne
        preciseRotation = pkt.preciseRotation

        quatDiff = pkt.quatDiff
        increaseDistanceBy = pkt.increaseDistanceBy
    }
}

object ServerPhysgunState: ServerClosable() {
    val playerStates = ConcurrentHashMap<UUID, PlayerPhysgunState>()
    val activelySeeking = mutableSetOf<UUID>()

    override fun close() {
        playerStates.clear()
        activelySeeking.clear()
    }

    class C2SPhysgunStateChanged(): AutoSerializable {
        var primaryActivated: Boolean by get(0, false)
        var rotate: Boolean by get(1, false)
        var freezeSelected: Boolean by get(2, false)
        var freezeAll: Boolean by get(3, false)
        var unfreezeAllOrOne: Boolean by get(4, false)
        var preciseRotation: Boolean by get(5, false)


        var quatDiff: Quaterniond by get(6, Quaterniond())
        var increaseDistanceBy: Double by get(7, 0.0)
    }

    val c2sPrimaryStateChanged = "state_changed" idWithConnc {
        object : C2SConnection<C2SPhysgunStateChanged>(it, "server_physgun") {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val pkt = C2SPhysgunStateChanged()
                pkt.deserialize(buf)

                val player = context.player as ServerPlayer

                val state = playerStates.getOrPut(player.uuid) { PlayerPhysgunState() }
                synchronized(state.lock) {
                    state.fromPkt(player, pkt)

                    if (state.unfreezeAllOrOne && !state.primaryActivated) {
                        val player = state.serverPlayer!!

                        val dir = Vector3d(player.lookAngle).snormalize()
                        val pos = Vector3d(player.eyePosition)

                        val result = RaycastFunctions.raycast(player.level, RaycastFunctions.Source(dir, pos))
                        if (result.state.isAir) {return}
                        if (result.ship == null) {return}

                        (result.ship!! as ServerShip).isStatic = false

                        return
                    }

                    if (!state.primaryActivated) {
                        state.mainShipId = -1
                        state.caughtShipIds.clear()
                        activelySeeking.remove(player.uuid)
                        return
                    }
                    if (state.mainShipId == -1L) {
                        activelySeeking.add(player.uuid)
                        return
                    }
                    val level = state.serverPlayer!!.level
                    val ship = level.shipObjectWorld.loadedShips.getById(state.mainShipId)!!

                    if (state.freezeSelected) {
                        (ship as ServerShip).isStatic = true
                        state.mainShipId = -1
                        state.caughtShipIds.clear()
                        return
                    }

                    if (state.freezeAll) {
                        traverseGetConnectedShips(ship.id).traversedShipIds.forEach { id ->
                            ((level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach) as ServerShip).isStatic = true
                        }
                        state.mainShipId = -1
                        state.caughtShipIds.clear()
                        return
                    }

                    if (state.unfreezeAllOrOne) {
                        traverseGetConnectedShips(ship.id).traversedShipIds.forEach { id ->
                            ((level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach) as ServerShip).isStatic = false
                        }
                        return
                    }


                    state.playerDir = Vector3d(player.lookAngle).snormalize()
                    state.playerPos = Vector3d(player.eyePosition)

                    state.idealRotation = state.quatDiff.mul(state.idealRotation, Quaterniond())

                    if (state.increaseDistanceBy != 0.0) {
                        state.distanceFromPlayer = max(state.distanceFromPlayer + state.increaseDistanceBy, 0.0)
                        state.increaseDistanceBy = 0.0
                    }
                }
            }
        }
    }

    init {
        RandomEvents.serverOnTick.on {
            (server), _ ->
            val toRemove = mutableSetOf<UUID>()
            activelySeeking.forEach { uuid ->
                val state = playerStates[uuid]
                if (state == null) {
                    playerStates.remove(uuid)
                    toRemove.add(uuid)
                    return@forEach
                }

                val player = state.serverPlayer!!

                val dir = Vector3d(player.lookAngle).snormalize()
                val pos = Vector3d(player.eyePosition)

                val result = RaycastFunctions.raycast(player.level, RaycastFunctions.Source(dir, pos))
                if (result.state.isAir) {return@forEach}
                if (result.ship == null) {return@forEach}

                state.distanceFromPlayer = (result.worldHitPos!! - pos).dist()
                state.fromPos = result.globalHitPos!!
                state.idealRotation = Quaterniond(result.ship!!.transform.shipToWorldRotation)
                state.mainShipId = result.shipId
                state.playerLastRot = playerRotToQuat(player.xRot.toDouble(), player.yRot.toDouble())

                val ship = server.shipObjectWorld.loadedShips.getById(state.mainShipId)!!
                ship.isStatic = false

                val traversedIds = traverseGetConnectedShips(ship.id).traversedShipIds
                traversedIds.remove(ship.id)

                state.caughtShipIds.clear()
                state.caughtShipIds.addAll(traversedIds)

                val controller = PhysgunController.getOrCreate(ship)

                controller.sharedState = state

                activelySeeking.remove(uuid)
            }
        }
    }
}

//TODO https://gamedev.stackexchange.com/questions/83601/from-3d-rotation-snap-to-nearest-90-directions https://math.stackexchange.com/questions/40164/how-do-you-rotate-a-vector-by-a-unit-quaternion
//
//                    val check = {
//                        highestDot: Double, closest: org.joml.Vector3d, currentRotation: Quaterniond, axis: org.joml.Vector3d, checkDir: org.joml.Vector3d ->
//
//                        val dot = rotateVecByQuat(axis, currentRotation).dot(checkDir)
//                        if (dot > highestDot) {
//                            Pair(dot, checkDir)
//                        } else {
//                            Pair(highestDot, closest)
//                        }
//                    }
//
//                    val closestToAxis = {
//                        currentRot: Quaterniond, axis: org.joml.Vector3d ->
//                        val checkAxes = listOf(
//                            org.joml.Vector3d( 1.0,  0.0,  0.0),
//                            org.joml.Vector3d(-1.0,  0.0,  0.0),
//                            org.joml.Vector3d( 0.0,  1.0,  0.0),
//                            org.joml.Vector3d( 0.0, -1.0,  0.0),
//                            org.joml.Vector3d( 0.0,  0.0,  1.0),
//                            org.joml.Vector3d( 0.0,  0.0, -1.0)
//                        )
//                        var closestAxis = checkAxes[0]
//                        var highestDot = -1.0
//                        checkAxes.forEach {
//                            val (_highestDot, _closestAxis) = check(highestDot, closestAxis, currentRot, axis, it)
//                            closestAxis = _closestAxis
//                            highestDot = _highestDot
//                        }
//                        closestAxis
//                    }
//
//                    val snapToNearestRightAngle = {
//                        currentRotation: Quaterniond ->
//                        val closestToForward = closestToAxis(currentRotation, org.joml.Vector3d(0.0, 0.0, 1.0))
//                        val closestToUp = closestToAxis(currentRotation, org.joml.Vector3d(0.0, 1.0, 0.0))
//                        Quaterniond().lookAlong(closestToForward, closestToUp)
//                    }
//
//                    if (state.preciseRotation) {
//                        state.idealRotation = snapToNearestRightAngle(state.idealRotation)
//                    }