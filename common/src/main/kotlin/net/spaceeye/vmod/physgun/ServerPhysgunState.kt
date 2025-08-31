package net.spaceeye.vmod.physgun

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.VMItems
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.ReservedRenderingPages
import net.spaceeye.vmod.rendering.types.PhysgunRayRenderer
import net.spaceeye.vmod.shipAttachments.PhysgunController
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

    var rID: Int = -1,

    var fromPos: Vector3d = Vector3d(),
    var idealRotation: Quaterniond = Quaterniond(),
    var mainShipId: ShipId = -1,
    var caughtShipIds: MutableList<ShipId> = mutableListOf(),

    var pConst: Double = VMConfig.SERVER.PHYSGUN.PCONST,
    var dConst: Double = VMConfig.SERVER.PHYSGUN.DCONST,
    var iConst: Double = VMConfig.SERVER.PHYSGUN.IDKCONST,
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

        pConst = VMConfig.SERVER.PHYSGUN.PCONST
        dConst = VMConfig.SERVER.PHYSGUN.DCONST
        iConst = VMConfig.SERVER.PHYSGUN.IDKCONST
    }
}

object ServerPhysgunState: ServerClosable() {
    val playerStates = ConcurrentHashMap<UUID, PlayerPhysgunState>()
    val activelySeeking = ConcurrentHashMap.newKeySet<UUID>()
    val active = ConcurrentHashMap.newKeySet<UUID>()

    override fun close() {
        playerStates.clear()
        activelySeeking.clear()
        active.clear()
    }

    class C2SPhysgunStateChanged(): AutoSerializable {
        @JsonIgnore private var i = 0

        var primaryActivated: Boolean by get(i++, false)
        var rotate: Boolean by get(i++, false)
        var freezeSelected: Boolean by get(i++, false)
        var freezeAll: Boolean by get(i++, false)
        var unfreezeAllOrOne: Boolean by get(i++, false)
        var preciseRotation: Boolean by get(i++, false)


        var quatDiff: Quaterniond by get(i++, Quaterniond())
        var increaseDistanceBy: Double by get(i++, 0.0)
    }

    val c2sPrimaryStateChanged = regC2S<C2SPhysgunStateChanged>(MOD_ID, "state_changed", "server_physgun") { pkt, player ->
        val state = playerStates.getOrPut(player.uuid) { PlayerPhysgunState() }
        synchronized(state.lock) {
            state.fromPkt(player, pkt)

            if (state.unfreezeAllOrOne && !state.primaryActivated) {
                val player = state.serverPlayer!!

                val dir = Vector3d(player.lookAngle).snormalize()
                val pos = Vector3d(player.eyePosition)

                val result = RaycastFunctions.raycast(player.level(), RaycastFunctions.Source(dir, pos))
                if (result.state.isAir) {return@regC2S}
                if (result.ship == null) {return@regC2S}

                (result.ship!! as ServerShip).isStatic = false

                return@regC2S
            }

            if (!state.primaryActivated) {
                state.mainShipId = -1
                state.caughtShipIds.clear()
                activelySeeking.remove(player.uuid)

                RenderingData.server.removeRenderer(state.rID)
                state.rID = -1
                return@regC2S
            }
            if (state.mainShipId == -1L && (state.freezeSelected || state.rotate)) {
                return@regC2S
            }
            if (state.mainShipId == -1L) {
                activelySeeking.add(player.uuid)
                return@regC2S
            }
            active.add(player.uuid)
            val level = state.serverPlayer!!.level()
            val ship = level.shipObjectWorld.loadedShips.getById(state.mainShipId)!!

            if (state.freezeSelected) {
                (ship as ServerShip).isStatic = true
                state.mainShipId = -1
                state.caughtShipIds.clear()
                return@regC2S
            }

            if (state.freezeAll) {
                traverseGetConnectedShips(ship.id).traversedShipIds.forEach { id ->
                    ((level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach) as ServerShip).isStatic = true
                }
                state.mainShipId = -1
                state.caughtShipIds.clear()
                return@regC2S
            }

            if (state.unfreezeAllOrOne) {
                traverseGetConnectedShips(ship.id).traversedShipIds.forEach { id ->
                    ((level.shipObjectWorld.loadedShips.getById(id) ?: return@forEach) as ServerShip).isStatic = false
                }
                return@regC2S
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

    init {
        PersistentEvents.serverOnTick.on {
            (server), _ ->
            val toRemove = mutableSetOf<UUID>()

            active.forEach {uuid ->
                val state = playerStates[uuid]
                if (state == null) {
                    toRemove.add(uuid)
                    return@forEach
                }
                synchronized(state.lock) {
                    if (state.mainShipId == -1L) {
                        toRemove.add(uuid)
                        RenderingData.server.removeRenderer(state.rID)
                        state.rID = -1
                        return@forEach
                    }

                    val player = if (
                           state.serverPlayer == null
                        || state.serverPlayer!!.mainHandItem.item != VMItems.PHYSGUN.get()
                    ) {
                        toRemove.add(uuid)
                        RenderingData.server.removeRenderer(state.rID)
                        state.rID = -1
                        state.mainShipId = -1L
                        state.caughtShipIds.clear()
                        return@forEach
                    } else state.serverPlayer!!

                    state.playerPos = Vector3d(player.eyePosition)
                    state.playerDir = Vector3d(player.lookAngle).snormalize()

                    val newPlayerRot = playerRotToQuat(player.xRot.toDouble(), player.yRot.toDouble())
                    val deltaRot = newPlayerRot.mul(state.playerLastRot.conjugate(), Quaterniond())
                    state.playerLastRot = newPlayerRot
                    state.idealRotation = deltaRot.mul(state.idealRotation).normalize()
                }
            }

            active.removeAll(toRemove)

            toRemove.clear()
            activelySeeking.forEach { uuid ->
                val state = playerStates[uuid]
                if (state == null) {
                    playerStates.remove(uuid)
                    toRemove.add(uuid)
                    return@forEach
                }

                val player = if (
                    state.serverPlayer == null
                    || state.serverPlayer!!.mainHandItem.item != VMItems.PHYSGUN.get()
                ) {
                    toRemove.add(uuid)
                    RenderingData.server.removeRenderer(state.rID)
                    state.rID = -1
                    state.mainShipId = -1L
                    state.caughtShipIds.clear()
                    return@forEach
                } else state.serverPlayer!!

                val dir = Vector3d(player.lookAngle).snormalize()
                val pos = Vector3d(player.eyePosition)

                state.playerPos = pos
                state.playerDir = dir

                val result = RaycastFunctions.raycast(player.level(), RaycastFunctions.Source(dir, pos))

                val pageId = ReservedRenderingPages.TimedRenderingObjects
                if (state.rID == -1) {
                    val renderer = PhysgunRayRenderer()
                    renderer.data.player = uuid
                    state.rID = RenderingData.server.addRenderer(listOf(pageId), renderer)
                }

                if (result.state.isAir) {return@forEach}
                if (result.ship == null) {return@forEach}

                state.distanceFromPlayer = (result.worldHitPos!! - pos).dist()
                state.fromPos = result.globalHitPos!!
                state.idealRotation = Quaterniond(result.ship!!.transform.shipToWorldRotation)
                state.mainShipId = result.shipId
                state.playerLastRot = playerRotToQuat(player.xRot.toDouble(), player.yRot.toDouble())

                val ship = server.shipObjectWorld.loadedShips.getById(state.mainShipId) ?: return@forEach
                ship.isStatic = false

                val traversedIds = traverseGetConnectedShips(ship.id).traversedShipIds
                traversedIds.remove(ship.id)

                state.caughtShipIds.clear()
                //TODO finish this
                if (VMConfig.SERVER.PHYSGUN.GRAB_ALL_CONNECTED_SHIPS) {
                    state.caughtShipIds.addAll(traversedIds)
                }

                val controller = PhysgunController.getOrCreate(ship)

                controller.sharedState = state

                val renderer = (RenderingData.server.getRenderer(state.rID) ?: return@forEach) as PhysgunRayRenderer
                renderer.data.player = uuid
                renderer.data.shipId = state.mainShipId
                renderer.data.hitPosInShipyard = result.globalHitPos!!
                RenderingData.server.setRenderer(listOf(pageId), state.rID, renderer)

                toRemove.add(uuid)
                active.add(uuid)
            }
            activelySeeking.removeAll(toRemove)
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