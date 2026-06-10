package net.spaceeye.vmod.physgun

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.server.level.ServerLevel
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
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.traverseGetConnectedShips
import org.joml.Quaterniond
import org.valkyrienskies.core.api.bodies.PhysVsBody
import org.valkyrienskies.core.api.bodies.ServerVsBody
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs
import kotlin.math.max

fun playerRotToQuat(pitch: Double, yaw: Double): Quaterniond {
    return Quaterniond().rotateY(Math.toRadians(-yaw)).rotateX(Math.toRadians(pitch))  
}

data class ShipOffsetData(
    /** Offset from grab point to ship center, in grab-frame local space */
    val localOffset: JVector3d,
    /** Ship rotation relative to the grab-frame rotation at grab time */
    val localRotation: Quaterniond
)

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
    var rawIdealRotation: Quaterniond = Quaterniond(),
    var mainShipId: ShipId = -1,
    var caughtShipIds: MutableList<ShipId> = mutableListOf(),

    var pConst: Double = VMConfig.SERVER.PHYSGUN.PCONST,
    var dConst: Double = VMConfig.SERVER.PHYSGUN.DCONST,

    var shipOffsets: MutableMap<ShipId, ShipOffsetData> = mutableMapOf()
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
    }
}

object ServerPhysgunState: ServerClosable() {
    val playerStates = ConcurrentHashMap<UUID, PlayerPhysgunState>()
    val activelySeeking = ConcurrentHashMap.newKeySet<UUID>()
    val active = ConcurrentHashMap.newKeySet<UUID>()
    val physTickState = ConcurrentHashMap<Long, PlayerPhysgunState>()

    override fun close() {
        playerStates.clear()
        activelySeeking.clear()
        active.clear()
        physTickState.clear()
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

                val result = RaycastFunctions.fromPhysRaycast(player.uuid) ?: RaycastFunctions.raycast(player.level(), RaycastFunctions.Source(dir, pos))

                (result.body as? ServerVsBody)?.isStatic = false

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
            val level = state.serverPlayer!!.level() as ServerLevel
            val body = level.shipObjectWorld.allBodies.getById(state.mainShipId) ?: return@regC2S

            if (state.freezeSelected) {
                body.isStatic = true
                state.mainShipId = -1
                state.caughtShipIds.clear()
                return@regC2S
            }

            if (state.freezeAll) {
                traverseGetConnectedShips(body.id).traversedShipIds.forEach { id ->
                    (level.shipObjectWorld.allBodies.getById(id) ?: return@forEach).isStatic = true
                }
                state.mainShipId = -1
                state.caughtShipIds.clear()
                return@regC2S
            }

            if (state.unfreezeAllOrOne) {
                traverseGetConnectedShips(body.id).traversedShipIds.forEach { id ->
                    (level.shipObjectWorld.allBodies.getById(id) ?: return@forEach).isStatic = false
                }
                return@regC2S
            }


            state.playerDir = Vector3d(player.lookAngle).snormalize()
            state.playerPos = Vector3d(player.eyePosition)

            state.rawIdealRotation = state.quatDiff.mul(state.rawIdealRotation, Quaterniond()).normalize()

            if (state.increaseDistanceBy != 0.0) {
                state.distanceFromPlayer = max(state.distanceFromPlayer + state.increaseDistanceBy, 0.0)
                state.increaseDistanceBy = 0.0
            }
        }
    }

    @PhysTickOnly
    fun physTick(state: PlayerPhysgunState, physShip: PhysVsBody, physLevel: PhysLevel) {
        if (!state.lock.tryLock()) return

        if (state.mainShipId == -1L) {
            physTickState.remove(physShip.id) // sus as shit
            state.lock.unlock()
            return
        }

        // Compute offsets once on the first tick (e.g. after a fresh grab or server restart)
        if (state.shipOffsets.isEmpty() || !state.shipOffsets.containsKey(state.mainShipId)) {
            val mainBody = physLevel.getBodyById(state.mainShipId)
            if (mainBody == null) {
                state.lock.unlock()
                return
            }
            computeOffsets(state, mainBody, state.caughtShipIds, physLevel)
        }

        val targetRefPos = Vector3d(state.playerPos + state.playerDir * state.distanceFromPlayer)
        val targetRefRot = Quaterniond(state.idealRotation)

        // Influence the main ship AND every connected ship
        val allShipIds = state.caughtShipIds.toMutableSet()
        allShipIds.add(state.mainShipId)

        for (shipId in allShipIds) {
            val body = physLevel.getBodyById(shipId) ?: continue
            val offset = state.shipOffsets[shipId] ?: continue

            // ===== 1. Target pose for this ship =====
            val rotatedOffset = JVector3d()
            targetRefRot.transform(offset.localOffset, rotatedOffset)
            val targetPos = Vector3d(targetRefPos) + Vector3d(rotatedOffset)

            val targetRot = Quaterniond(targetRefRot).mul(offset.localRotation)

            // ===== 2. Position PD (mass-scaled → uniform acceleration) =====
            val currentPos = Vector3d(body.kinematics.transform.position)
            val currentVel = Vector3d(body.velocity)

            val posError = targetPos - currentPos
            val force = (posError * state.pConst - currentVel * state.dConst) * body.inertiaData.mass
            body.applyWorldForce(force.toJomlVector3d())

            // ===== 3. Rotation PD =====
            val currentRot = Quaterniond(body.kinematics.transform.rotation)

            // Shortest rotation from current to target
            val rotError = Quaterniond(targetRot).mul(currentRot.invert(Quaterniond())).normalize()

            // Take the shortest quaternion path (flip if w < 0)
            if (rotError.w < 0.0) {
                rotError.x = -rotError.x
                rotError.y = -rotError.y
                rotError.z = -rotError.z
                rotError.w = -rotError.w
            }

            // Approximate rotation vector: axis * angle ≈ 2*(x,y,z) for small errors
            val rotErrorVec = Vector3d(rotError.x * 2.0, rotError.y * 2.0, rotError.z * 2.0)
            val targetOmega = rotErrorVec * state.pConst
            targetOmega -= Vector3d(body.angularVelocity) * state.dConst

            // torque = I_world * targetOmega
            val localOmega = JVector3d()
            body.kinematics.rotation.transformInverse(targetOmega.toJomlVector3d(), localOmega)

            val localTorque = JVector3d()
            body.inertiaData.inertiaTensor.transform(localOmega, localTorque)

            val worldTorque = JVector3d()
            body.kinematics.rotation.transform(localTorque, worldTorque)

            body.applyWorldTorque(worldTorque)
        }

        state.lock.unlock()
    }

    /** Call once when the grab starts. Populates [PlayerPhysgunState.shipOffsets] for the main ship and every caught body. */
    fun computeOffsets(state: PlayerPhysgunState, mainBody: PhysVsBody, caughtShipIds: List<ShipId>, level: PhysLevel) {
        val mainTransform = mainBody.kinematics.transform
        val refPos = JVector3d()
        mainTransform.toWorld.transformPosition(state.fromPos.x, state.fromPos.y, state.fromPos.z, refPos)

        val refRot = Quaterniond(mainTransform.rotation)
        val refRotInv = Quaterniond(refRot).invert()

        // Include the main ship so it is also controlled
        val allIds = caughtShipIds.toMutableList()
        if (!allIds.contains(state.mainShipId)) {
            allIds.add(state.mainShipId)
        }

        for (shipId in allIds) {
            val ship = level.getBodyById(shipId) ?: continue
            val shipTransform = ship.kinematics.transform

            // Offset from grab-point to ship center, in grab-frame local space
            val worldOffset = JVector3d(shipTransform.position).sub(refPos)
            val localOffset = JVector3d()
            refRotInv.transform(worldOffset, localOffset)

            // Rotation of this ship relative to the grab-frame at grab time
            val localRotation = Quaterniond(refRotInv).mul(shipTransform.rotation)

            state.shipOffsets[shipId] = ShipOffsetData(localOffset, localRotation)
        }
    }

    init {
        vsApi.physTickEvent.on { val level = it.world
            val keys = physTickState.keys
            for (id in keys) {
                val ship = level.getBodyById(id) ?: continue
                val state = physTickState[id] ?: continue
                physTick(state, ship, level)
            }
        }

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

                    // Always accumulate the continuous, unsnapped rotation
                    state.rawIdealRotation = deltaRot.mul(state.rawIdealRotation).normalize()

                    // Snap to grid only when precise mode is active
                    state.idealRotation = if (state.preciseRotation) {
                        snapQuaternion(state.rawIdealRotation, 45.0)
                    } else {
                        Quaterniond(state.rawIdealRotation)
                    }
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

                val result = RaycastFunctions.fromPhysRaycast(player.uuid) //;/?: RaycastFunctions.raycast(player.level(), RaycastFunctions.Source(dir, pos))

                val pageId = ReservedRenderingPages.TimedRenderingObjects
                if (state.rID == -1) {
                    val renderer = PhysgunRayRenderer()
                    renderer.data.player = uuid
                    state.rID = RenderingData.server.addRenderer(listOf(pageId), renderer)
                }

                if (result?.body == null) {return@forEach}

                state.distanceFromPlayer = (result.worldHitPos!! - pos).dist()
                state.fromPos = result.globalHitPos!!
                state.idealRotation = Quaterniond(result.body!!.kinematics.rotation)
                state.rawIdealRotation = Quaterniond(state.idealRotation)
                state.mainShipId = result.shipId
                state.shipOffsets.clear()
                state.playerLastRot = playerRotToQuat(player.xRot.toDouble(), player.yRot.toDouble())

                val body = server.shipObjectWorld.allBodies.getById(state.mainShipId) ?: return@forEach
                body.isStatic = false

                val traversedIds = traverseGetConnectedShips(body.id).traversedShipIds
                traversedIds.remove(body.id)

                state.caughtShipIds.clear()
                //TODO finish this
                if (true || VMConfig.SERVER.PHYSGUN.GRAB_ALL_CONNECTED_SHIPS) {
                    state.caughtShipIds.addAll(traversedIds)
                }

                physTickState[body.id] = state

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

    /** Cache of snap tables so we only generate them once per angle. */
    private val snapTables = mutableMapOf<Double, List<Quaterniond>>()

    /** Generate every Y/P/R combination in [snapDegrees] increments, canonicalized. */
    private fun generateSnapTable(snapDegrees: Double): List<Quaterniond> {
        val stepRad = Math.toRadians(snapDegrees)
        val steps = (360.0 / snapDegrees).toInt().coerceAtLeast(1)
        val raw = mutableListOf<Quaterniond>()

        for (yi in 0 until steps) {
            for (pi in 0 until steps) {
                for (ri in 0 until steps) {
                    val q = Quaterniond()
                        .rotateYXZ(yi * stepRad, pi * stepRad, ri * stepRad)
                        .normalize()
                    // Canonicalize: force w >= 0 so q and -q don't duplicate
                    if (q.w < 0.0) {
                        q.x = -q.x; q.y = -q.y; q.z = -q.z; q.w = -q.w
                    }
                    raw.add(q)
                }
            }
        }

        // Deduplicate (90° snapping collapses to 24 unique orientations)
        val unique = mutableListOf<Quaterniond>()
        for (q in raw) {
            if (unique.none { abs(it.dot(q)) > 0.99999 }) {
                unique.add(Quaterniond(q)) // copy
            }
        }
        return unique
    }

    /** Return the nearest grid orientation to [q] in quaternion geodesic distance. */
    fun snapQuaternion(q: Quaterniond, snapDegrees: Double): Quaterniond {
        if (snapDegrees <= 0.0) return Quaterniond(q)

        val table = snapTables.getOrPut(snapDegrees) { generateSnapTable(snapDegrees) }

        val qw = if (q.w < 0.0) -q.w else q.w
        val qx = if (q.w < 0.0) -q.x else q.x
        val qy = if (q.w < 0.0) -q.y else q.y
        val qz = if (q.w < 0.0) -q.z else q.z

        var bestDot = Double.NEGATIVE_INFINITY
        val best = Quaterniond()

        for (candidate in table) {
            val dot = qx * candidate.x + qy * candidate.y + qz * candidate.z + qw * candidate.w
            if (dot >= bestDot) {
                bestDot = dot
                best.set(candidate)
            }
        }
        return best
    }
}