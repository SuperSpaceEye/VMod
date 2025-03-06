package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.rendering.ServerRenderingData
import net.spaceeye.vmod.rendering.types.debug.DebugPointRenderer
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.linspace
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.vEntityManaging.util.VEAutoSerializable
import org.joml.AxisAngle4d
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.core.apigame.physics.PhysicsEntityData
import org.valkyrienskies.core.apigame.physics.PhysicsEntityServer
import org.valkyrienskies.core.apigame.physics.VSCapsuleCollisionShapeData
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.PI

class PhysRopeConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
    override var shipId1: Long = -1
    override var shipId2: Long = -1
    override lateinit var sPos1: Vector3d
    override lateinit var sPos2: Vector3d

    var ropeLength: Float by get(i++, 0f)
    var segments: Int by get(i++, 0)
    var massPerSegment: Double by get(i++, 0.0)
    var radius: Double by get(i++, 0.0)

    var data = mutableListOf<PhysicsEntityData>()


    var entities = mutableListOf<PhysicsEntityServer>()

    constructor(
        shipId1: ShipId,
        shipId2: ShipId,
        sPos1: Vector3d,
        sPos2: Vector3d,
        ropeLength: Float,
        segments: Int,
        massPerSegment: Double,
        radius: Double,
    ): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.sPos1 = sPos1
        this.sPos2 = sPos2

        this.ropeLength = ropeLength
        this.segments = segments
        this.massPerSegment = massPerSegment
        this.radius = radius
    }

    override fun iCopyVEntity(
        level: ServerLevel,
        mapped: Map<ShipId, ShipId>
    ): VEntity? {
        TODO("Not yet implemented")
    }

    override fun iOnScaleBy(
        level: ServerLevel,
        scaleBy: Double,
        scalingCenter: Vector3d
    ) {
        TODO("Not yet implemented")
    }

//    private fun makeShip(level: ServerLevel, pos: Vector3d, rot: Quaterniond, mass: Double, length: Double, preferredRadius: Double): ServerShip {
//        val shipyardLength = length / preferredRadius // radius is also a scale
//        val nearestWhole = shipyardLength.roundToInt()
//        val scaleMultiplier = nearestWhole.toDouble() / shipyardLength
//        val scale = preferredRadius / scaleMultiplier
//
//        level.shipObjectWorld.dimensionToGroundBodyIdImmutable
//        val ship = level.shipObjectWorld.createNewShipAtBlock(pos.toJomlVector3i(), false, scale, level.dimensionId)
//        val center = Vector3d(ship.chunkClaim.xMiddle * 16 + 7, 128, ship.chunkClaim.zMiddle * 16 + 7)
//
//        val blockMass = mass / nearestWhole.toDouble()
//        val airType = BlockStateInfo.get(Blocks.AIR.defaultBlockState())!!
//        val plankType = BlockStateInfo.get(Blocks.OAK_PLANKS.defaultBlockState())!!
//
//        for (i in 0 until nearestWhole) {
//            val pos = (center + Vector3d(nearestWhole/2 - i, 0, 0)).toJomlVector3i()
//            level.shipObjectWorld.onSetBlock(pos.x, pos.y, pos.z, level.dimensionId, airType.second, plankType.second, airType.first, plankType.first)
//            CustomBlockMassManager.setCustomMass(level, pos.x, pos.y, pos.z, blockMass, plankType.second, plankType.first, ship)
//        }
//
//        level.shipObjectWorld.teleportShip(ship, ShipTeleportDataImpl(pos.toJomlVector3d(), rot))
//
//        return ship
//    }

    private fun makeLinkData(level: ServerLevel, pos: Vector3d, rot: Quaterniond, mass: Double, length: Double, radius: Double): PhysicsEntityData {
        val h = length
        val r = radius
        val rSq = r * r

        val cM_temp = h * rSq * PI
        val hsM_temp = 2.0 * rSq * r * PI * (1.0/3.0)

        val density = mass / (cM_temp + hsM_temp)

        val cM = cM_temp * density
        val hsM = hsM_temp * density

        val inertiaTensor = Matrix3d()

        inertiaTensor.m11 = rSq * cM * 0.5

        inertiaTensor.m22 = inertiaTensor.m11 * 0.5 + cM * h * h * (1.0/12.0)
        inertiaTensor.m00 = inertiaTensor.m22

        val temp0 = hsM * 2.0 * rSq / 5.0
        inertiaTensor.m11 += temp0 * 2.0
        val temp1 = h * 0.5
        val temp2 = temp0 + hsM * (temp1 * temp1 + 3.0 * (1.0/8.0) * h * r)
        inertiaTensor.m00 += temp2 * 2.0
        inertiaTensor.m22 += temp2 * 2.0

        return PhysicsEntityData(
            level.shipObjectWorld.allocateShipId(level.dimensionId),
            ShipTransformImpl.create(pos.toJomlVector3d(), Vector3d().toJomlVector3d(), rot),
            ShipInertiaDataImpl(Vector3d().toJomlVector3d(), mass, inertiaTensor),
            Vector3d().toJomlVector3d(),
            Vector3d().toJomlVector3d(),
            VSCapsuleCollisionShapeData(r, h),
            staticFrictionCoefficient =  1e30, dynamicFrictionCoefficient = 1e30, restitutionCoefficient = 1e30
        )
    }

    private fun makeData(level: ServerLevel) {
        val dimensionIds = level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values

        val rPos1 = if (dimensionIds.contains(shipId1)) {sPos1.copy()} else { posShipToWorld(level.shipObjectWorld.allShips.getById(shipId1), sPos1) }
        val rPos2 = if (dimensionIds.contains(shipId2)) {sPos2.copy()} else { posShipToWorld(level.shipObjectWorld.allShips.getById(shipId2), sPos2) }

        val rDir = (rPos1 - rPos2).normalize()

        val segmentLength = ropeLength / segments.toDouble()

        val startDir = rPos1 - rDir * (segmentLength / 2.0)
        val stopDir  = rPos2 + rDir * (segmentLength / 2.0)

        val rot = Quaterniond(AxisAngle4d(0.0, rDir.toJomlVector3d()))

        val xLin = linspace(startDir.x, stopDir.x, segments)
        val yLin = linspace(startDir.y, stopDir.y, segments)
        val zLin = linspace(startDir.z, stopDir.z, segments)

        var radius = radius
        var length = segmentLength * 0.5
        if (length < radius) { radius = length / 2 }
        if (length > radius * 4) { radius = length / 4 }

        for (i in 0 until segments) {
            val pos = Vector3d(xLin[i], yLin[i], zLin[i])
            data.add(makeLinkData(level, pos, rot, massPerSegment, length, radius))
        }
    }

    private fun makeConstraints(level: ServerLevel): Boolean {
        val dir = Vector3d(1, 0, 0)

        var prevId = shipId1
        var prevPos = sPos1
        var radius = 0.5

//        return true

        entities.withIndex().forEach { (i, entity) ->
            val length = (entity.collisionShapeData as VSCapsuleCollisionShapeData).length
            radius = (entity.collisionShapeData as VSCapsuleCollisionShapeData).radius
            cIDs.add(level.shipObjectWorld.createNewConstraint(VSRopeConstraint(
                shipId1, shipId2, 1e-300, prevPos.toJomlVector3d(), (dir * (length + radius)).toJomlVector3d(), 1e300, 0.0
            )) ?: run { return false })

            level.shipObjectWorld.disableCollisionBetweenBodies(prevId, entity.id)
            ServerRenderingData.addRenderer(listOf(entity.id, shipId1, shipId2), DebugPointRenderer(entity.id, prevPos))
            ServerRenderingData.addRenderer(listOf(entity.id, shipId1, shipId2), DebugPointRenderer(entity.id, (dir * (length + radius))))

            prevId = entity.id
            prevPos = (-dir * (length + radius))
        }

        ServerRenderingData.addRenderer(listOf(shipId1, shipId2), DebugPointRenderer(prevId, prevPos))
        ServerRenderingData.addRenderer(listOf(shipId1, shipId2), DebugPointRenderer(prevId, sPos2))

        cIDs.add(level.shipObjectWorld.createNewConstraint(VSRopeConstraint(
            prevId, shipId2, 1e-300, prevPos.toJomlVector3d(), sPos2.toJomlVector3d(), 1e300, 0.0
        )) ?: run { return false })
        level.shipObjectWorld.disableCollisionBetweenBodies(prevId, shipId2)

        return true
    }

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        if (data.isEmpty()) { makeData(level) }
        entities = data.map { level.shipObjectWorld.createPhysicsEntity(it, level.dimensionId) }.toMutableList()
        return makeConstraints(level)
    }

    override fun iOnDeleteVEntity(level: ServerLevel) {
        super.iOnDeleteVEntity(level)
        entities.forEach { level.shipObjectWorld.deletePhysicsEntity(it.id) }
    }
}

//class PhysRopeMConstraint(): TwoShipsMConstraint() {
//    lateinit var constraint: VSDistanceJoint
//    override val mainConstraint: VSJoint get() = constraint
//
//    var ropeLength = 0.0
//    var segments: Int = 0
//    var massPerSegment = 0.0
//    var radius: Double = 0.0
//
//    var entitiesUUIDs = mutableListOf<UUID>()
//    var entities = mutableListOf<PhysRopeComponentEntity>()
//    var rID: Int = -1
//
////    var firstRotation: Quaterniond? = null
////    var middleRotation: Quaterniond? = null
////    var lastRotation: Quaterniond? = null
//
//    constructor(
//        shipId0: ShipId,
//        shipId1: ShipId,
//        compliance: Double,
//        localPos0: Vector3dc,
//        localPos1: Vector3dc,
//        maxForce: Double,
//        ropeLength: Double,
//        segments: Int,
//        massPerSegment: Double,
//        radius: Double,
//        attachmentPoints: List<BlockPos>,
//    ): this() {
//        constraint = VSDistanceJoint(
//            shipId0, VSJointPose(localPos0, Quaterniond()),
//            shipId1, VSJointPose(localPos1, Quaterniond()),
//            maxDistance = ropeLength.toFloat()
//        )
//        attachmentPoints_ = attachmentPoints.toMutableList()
//        this.ropeLength = ropeLength
//        this.segments = segments
//        this.massPerSegment = massPerSegment
//        this.radius = radius
//    }
//
//    override fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
//        throw NotImplementedError()
////        if (previous != attachmentPoints_[0] && previous != attachmentPoints_[1]) {return}
////
////        val first = when (previous) {
////            attachmentPoints_[0] -> { true }
////            attachmentPoints_[1] -> { false }
////            else -> {throw AssertionError("Should be impossible")}
////        }
////
////        level.shipObjectWorld.removeConstraint(if (first) cIDs[0] else cIDs.last())
////
////        val shipIds = mutableListOf(constraint.shipId0, constraint.shipId1)
////        val localPoints = mutableListOf(
////            listOf(constraint.localPos0),
////            listOf(constraint.localPos1)
////        )
////        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)
////
////        constraint = constraint.copy(shipIds[0], shipIds[1], constraint.compliance, localPoints[0][0], localPoints[1][0])
////
////        val rpos = (Vector3d((if (first) 1 else -1), 0, 0) * (ropeLength / segments.toDouble() * 0.5)).toJomlVector3d()
////        cIDs[if (first) 0 else {cIDs.size - 1}] = level.shipObjectWorld.createNewConstraint(
////            VSRopeConstraint(constraint.shipId0, entities[if (first) 0 else {entities.size - 1}].physicsEntityData!!.shipId,
////                constraint.compliance,
////                constraint.localPos0, rpos,
////                constraint.maxForce, 0.0)
////        ) ?: run { level.removeManagedConstraint(mID) ; return }
//    }
//
//    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
//        val new = PhysRopeMConstraint()
//
//        new.attachmentPoints_ = copyAttachmentPoints(constraint, attachmentPoints_, level, mapped)
//        new.constraint = constraint.copy(level, mapped) ?: return null
//
//        new.ropeLength = ropeLength
//        new.segments = segments
//        new.massPerSegment = massPerSegment
//        new.radius = radius
//
//        return new
//    }
//
//    //TODO the way it works is pretty stupid, redo when possible
//    //why does it recreate phys entities instead of modifying them?
//    //shipObjectWorld.teleportPhysicsEntity doesn't scale entities when you give it new scale
//    //changing physEntityServer also doesn't seem to work but i'm not sure
//    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
//        val newPositions = entities.map {
//            (Vector3d(it.physicsEntityData!!.transform.positionInWorld) - scalingCenter) * scaleBy + scalingCenter
//        }
//
//        val oldEntities = entities
//
//        entities = mutableListOf()
//        entitiesUUIDs.clear()
//        cIDs.clear()
//
//        massPerSegment *= scaleBy
//        radius *= scaleBy
//        ropeLength *= scaleBy
//
//        createNewMConstraint(level)
//
//        oldEntities.zip(entities).zip(newPositions).forEach { (entities, pos) ->
//            val (old, new) = entities
//
//            level.shipObjectWorld.teleportPhysicsEntity(new.physicsEntityServer!!, ShipTeleportDataImpl(
//                pos.toJomlVector3d(),
//                old.physicsEntityServer!!.shipTransform.shipToWorldRotation,
//                org.joml.Vector3d(),
//                org.joml.Vector3d(),
//                old.physicsEntityServer!!.dimensionId
//            ))
//        }
//
//        oldEntities.forEach { it.kill() }
//    }
//
//    override fun iNbtSerialize(): CompoundTag? {
//        val tag = VSJointSerializationUtil.serializeConstraint(constraint) ?: return null
//
//        tag.putDouble("ropeLength", ropeLength)
//        tag.putInt("segments", segments)
//        tag.putDouble("massPerSegment", massPerSegment)
//        tag.putDouble("radius", radius)
//
//        val uuidTag = ListTag()
//
//        for (id in entitiesUUIDs) {
//            val item = CompoundTag()
//            item.putUUID("uuid", id)
//            uuidTag.add(item)
//        }
//
//        tag.put("uuids", uuidTag)
////        tag.putQuaterniond("firstRotation", firstRotation!!)
////        tag.putQuaterniond("middleRotation", middleRotation!!)
////        tag.putQuaterniond("lastRotation", lastRotation!!)
//
//        return tag
//    }
//
//    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
//        ropeLength = tag.getDouble("ropeLength")
//        segments = tag.getInt("segments")
//        massPerSegment = tag.getDouble("massPerSegment")
//        radius = tag.getDouble("radius")
//
////        firstRotation = tag.getQuaterniond("firstRotation")
////        middleRotation = tag.getQuaterniond("middleRotation")
////        lastRotation = tag.getQuaterniond("lastRotation")
//
//        entitiesUUIDs = (tag.get("uuids") as ListTag).map { (it as CompoundTag).getUUID("uuid") }.toMutableList()
//
//        tryConvertDimensionId(tag, lastDimensionIds); constraint = (VSJointDeserializationUtil.deserializeConstraint(tag) ?: return null) as VSDistanceJoint
//
//        return this
//    }
//
//    private fun createConstraints(level: ServerLevel, length: Double, worldDirection: Vector3d? = null) {
////        val dir = Vector3d(1, 0, 0)
////
////        var prevId = constraint.shipId0
////        var prevPos = constraint.localPos0
////
//////        var prevRot = firstRotation ?: getHingeRotation(level.shipObjectWorld.allShips.getById(prevId)?.transform, worldDirection!!)
//////        firstRotation = prevRot
////
////        for ((i, entity) in entities.withIndex()) {
////            val rpos = (dir * length).toJomlVector3d()
////
////            cIDs.add(level.shipObjectWorld.createNewConstraint(
////                VSRopeConstraint(prevId, entity.physicsEntityData!!.shipId,
////                    constraint.compliance,
////                    prevPos, rpos,
////                    constraint.maxForce, 0.0)
////            ) ?: run { level.removeManagedConstraint(mID) ; return })
////
////            level.shipObjectWorld.disableCollisionBetweenBodies(prevId, entity.physicsEntityData!!.shipId)
////
//////            val thisRot = middleRotation ?: getHingeRotation(entity.physicsEntityData!!.transform, worldDirection!!)
//////            middleRotation = thisRot
////
//////            level.shipObjectWorld.createNewConstraint(
//////                VSSphericalTwistLimitsConstraint(prevId, entity.physicsEntityData!!.shipId, constraint.compliance,
//////                    prevRot, thisRot,
//////                    1e200, Math.toRadians(-20.0), Math.toRadians(20.0))
//////            )
////
////            prevId = entity.physicsEntityData!!.shipId
////            prevPos = (-dir * length).toJomlVector3d()
//////            prevRot = thisRot
////        }
////
////        cIDs.add(level.shipObjectWorld.createNewConstraint(
////            VSRopeConstraint(prevId, constraint.shipId1,
////                constraint.compliance,
////                prevPos, constraint.localPos1,
////                constraint.maxForce, 0.0)
////        ) ?: run { level.removeManagedConstraint(mID) ; return })
////
////        level.shipObjectWorld.disableCollisionBetweenBodies(prevId, constraint.shipId1)
////
//////        val lastRotation_ = lastRotation ?: getHingeRotation(level.shipObjectWorld.allShips.getById(constraint.shipId1)?.transform, worldDirection!!)
//////        lastRotation = lastRotation_
////
//////        level.shipObjectWorld.createNewConstraint(
//////            VSSphericalTwistLimitsConstraint(prevId, constraint.shipId1, constraint.compliance,
//////                prevRot, lastRotation_,
//////                1e200, Math.toRadians(-20.0), Math.toRadians(20.0))
//////        )
////
////        var radius = radius
////        if (length < radius) { radius = length / 2 }
////        if (length > radius * 4) { radius = length / 4 } // capsule constraint is actually just 4 balls in a row so
////
////        rID = ServerRenderingData.addRenderer(
////            constraint.shipId0, constraint.shipId1, PhysRopeRenderer(
////                constraint.shipId0, constraint.shipId1, Vector3d(constraint.localPos0), Vector3d(constraint.localPos1),
////                Color(120, 0, 120),  radius, length * 2, entities.map { it.id }
////            )
////        )
//    }
//
//    private fun onLoadMConstraint(level: ServerLevel): Boolean {
//        val createConstraints = {
//            val segmentLength = ropeLength / segments.toDouble()
//            val length = segmentLength * 0.5
//
//            createConstraints(level, length)
//        }
//
//        synchronized(ServerPhysEntitiesHolder.entities) {
//            var loaded = 0
//            val toLoad = segments
//
//            entitiesUUIDs.forEach {
//                val entity = ServerPhysEntitiesHolder.entities[it] ?: return@forEach
//                entities.add(entity as PhysRopeComponentEntity)
//                loaded++
//            }
//
//            if (loaded == toLoad) {
//                val temp = mutableListOf<PhysRopeComponentEntity>()
//                temp.addAll(entities)
//                entities.forEach {
//                    temp[entitiesUUIDs.indexOf(it.uuid)] = it
//                }
//                entities = temp
//
//                createConstraints()
//
//                return@synchronized
//            }
//
//            ServerPhysEntitiesHolder.entityLoadedEvent.on {
//                (uuid, entity), handler ->
//                if (!entitiesUUIDs.contains(uuid)) { return@on }
//                entity as PhysRopeComponentEntity
//                entities.add(entity)
//
//                entity.physicsEntityData!!.isStatic = true
//
//                loaded++
//                if (toLoad < loaded) {return@on}
//
//                val temp = mutableListOf<PhysRopeComponentEntity>()
//                temp.addAll(entities)
//                entities.forEach {
//                    temp[entitiesUUIDs.indexOf(it.uuid)] = it
//                }
//                entities = temp
//
//                createConstraints()
//            }
//        }
//
//        return true
//    }
//
//    private fun createNewMConstraint(level: ServerLevel): Boolean {
//        val dimensionIds = level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values
//
////        val rpos1 = if(!dimensionIds.contains(constraint.shipId0)) posShipToWorld(level.shipObjectWorld.allShips.getById(constraint.shipId0), Vector3d(constraint.localPos0)) else Vector3d(constraint.localPos0)
////        val rpos2 = if(!dimensionIds.contains(constraint.shipId1)) posShipToWorld(level.shipObjectWorld.allShips.getById(constraint.shipId1), Vector3d(constraint.localPos1)) else Vector3d(constraint.localPos1)
////
////        val rdir = (rpos1 - rpos2).snormalize()
////        val rot = Quaterniond(AxisAngle4d(0.0, rdir.x, rdir.y, rdir.z)).normalize()
////
////        val xLin = linspace(rpos1.x, rpos2.x, segments + 2)
////        val yLin = linspace(rpos1.y, rpos2.y, segments + 2)
////        val zLin = linspace(rpos1.z, rpos2.z, segments + 2)
////
////        val segmentLength = ropeLength / segments.toDouble()
////
////        var radius = radius
////        var length = segmentLength * 0.5
////        if (length < radius) { radius = length / 2}
////        if (length > radius * 4) { radius = length / 4 }
////
////        for (i in 0 until segments) {
////            val pos = Vector3d(xLin[i+1], yLin[i+1], zLin[i+1])
////            val entity = PhysRopeComponentEntity.createEntity(level, massPerSegment, radius, length, pos.toJomlVector3d(), rot, false)
////            entities.add(entity)
////            entitiesUUIDs.add(entity.uuid)
////        }
////
////        createConstraints(level, length, rdir)
//
//        return true
//    }
//
//    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
//        RandomEvents.serverOnTick.on {
//            _, unregister ->
//            if (entitiesUUIDs.isNotEmpty()) {
//                onLoadMConstraint(level)
//            } else {
//                createNewMConstraint(level)
//            }
//
//            unregister()
//        }
//
//        return true
//    }
//
//    override fun iOnDeleteMConstraint(level: ServerLevel) {
//        RandomEvents.serverOnTick.on {
//            (server), handler ->
//            entities.forEach { it.kill() }
//        }
//        ServerRenderingData.removeRenderer(rID)
//    }
//}