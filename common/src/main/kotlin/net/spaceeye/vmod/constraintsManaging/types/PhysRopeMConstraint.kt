package net.spaceeye.vmod.constraintsManaging.types

import gg.essential.elementa.state.map
import net.minecraft.core.BlockPos
import net.minecraft.nbt.*
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.utils.vs.VSJointDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.entities.events.ServerPhysEntitiesHolder
import net.spaceeye.vmod.entities.PhysRopeComponentEntity
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.rendering.ServerRenderingData
import net.spaceeye.vmod.rendering.types.PhysRopeRenderer
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.*
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSDistanceJoint
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointPose
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color
import java.util.*

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