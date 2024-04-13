package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.*
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.entities.ServerEntitiesHolder
import net.spaceeye.vmod.entities.PhysRopeComponentEntity
import net.spaceeye.vmod.rendering.SynchronisedRenderingData
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.EA2BRenderer
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import net.spaceeye.vmod.utils.vs.posShipToWorld
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId
import java.awt.Color
import java.util.*

class PhysRopeMConstraint(): MConstraint, MRenderable {
    lateinit var constraint: VSRopeConstraint
    override var renderer: BaseRenderer? = null

    var attachmentPoints_ = mutableListOf<BlockPos>()

    var ropeLength = 0.0
    var segments: Int = 0

    var vsId: ConstraintId = -1

    var entitiesUUIDs = mutableListOf<UUID>()
    var entities = mutableListOf<PhysRopeComponentEntity>()

    constructor(
        shipId0: ShipId,
        shipId1: ShipId,
        compliance: Double,
        localPos0: Vector3dc,
        localPos1: Vector3dc,
        maxForce: Double,
        ropeLength: Double,
        segments: Int,
        attachmentPoints: List<BlockPos>,
    ): this() {
        constraint = VSRopeConstraint(shipId0, shipId1, compliance, localPos0, localPos1, maxForce, ropeLength)
        attachmentPoints_ = attachmentPoints.toMutableList()
        this.ropeLength = ropeLength
        this.segments = segments
    }

    override var mID: ManagedConstraintId = -1
    override val typeName: String get() = "PhysRopeMConstraint"
    override var saveCounter: Int = -1

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(constraint.shipId0)
        val ship2Exists = allShips.contains(constraint.shipId1)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(constraint.shipId1))
                || (ship2Exists && dimensionIds.contains(constraint.shipId0))
    }

    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(constraint.shipId0)) {toReturn.add(constraint.shipId0)}
        if (!dimensionIds.contains(constraint.shipId1)) {toReturn.add(constraint.shipId1)}

        return toReturn
    }

    override fun getAttachmentPoints(): List<BlockPos> = attachmentPoints_
    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        if (previous != attachmentPoints_[0] && previous != attachmentPoints_[1]) {return}
        level.shipObjectWorld.removeConstraint(vsId)

        val shipIds = mutableListOf(constraint.shipId0, constraint.shipId1)
        val localPoints = mutableListOf(
            listOf(constraint.localPos0),
            listOf(constraint.localPos1)
        )
        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)

        constraint = VSRopeConstraint(shipIds[0], shipIds[1], constraint.compliance, localPoints[0][0], localPoints[1][0], constraint.maxForce, constraint.ropeLength)

        vsId = level.shipObjectWorld.createNewConstraint(constraint)!!

        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], mID)
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, constraint, attachmentPoints_, renderer) {
            nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints, newRenderer ->
            val con = PhysRopeMConstraint(nShip1?.id ?: constraint.shipId0, nShip2?.id ?: constraint.shipId1, constraint.compliance, localPos0.toJomlVector3d(), localPos1.toJomlVector3d(), constraint.maxForce, constraint.ropeLength, segments, newAttachmentPoints)
            con.ropeLength = ropeLength
            con
        }
    }

    override fun onScale(level: ServerLevel, scale: Double) {
        constraint = VSRopeConstraint(constraint.shipId0, constraint.shipId1, constraint.compliance, constraint.localPos0, constraint.localPos1, constraint.maxForce, ropeLength * scale)

        level.shipObjectWorld.removeConstraint(vsId)
        vsId = level.shipObjectWorld.createNewConstraint(constraint)!!
    }

    override fun getVSIds(): Set<VSConstraintId> {
        return setOf(vsId)
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = VSConstraintSerializationUtil.serializeConstraint(constraint) ?: return null

        tag.putInt("managedID", mID)
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))
        tag.putDouble("ropeLength", ropeLength)
        tag.putInt("segments", segments)

        val uuidTag = ListTag()

        for (id in entitiesUUIDs) {
            val item = CompoundTag()
            item.putUUID("uuid", id)
            uuidTag.add(item)
        }

        tag.put("uuids", uuidTag)

        serializeRenderer(tag)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("managedID")
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)
        ropeLength = tag.getDouble("ropeLength")
        segments = tag.getInt("segments")
        entitiesUUIDs = (tag.get("uuids") as ListTag).map { (it as CompoundTag).getUUID("uuid") }.toMutableList()

        deserializeRenderer(tag)

        tryConvertDimensionId(tag, lastDimensionIds); constraint = (VSConstraintDeserializationUtil.deserializeConstraint(tag) ?: return null) as VSRopeConstraint

        return this
    }

    private fun onLoadMConstraint(level: ServerLevel): Boolean {
        val createConstraints = {
            val segmentLength = ropeLength / segments.toDouble()

            val dir = Vector3d(1, 0, 0)

            var length = segmentLength

            var prevId = constraint.shipId0
            var prevPos = constraint.localPos0

            for ((i, entity) in entities.withIndex()) {
                val data = entity.physicsEntityData!!

                val rpos = data.transform.transformDirectionNoScalingFromShipToWorld((dir * length).toJomlVector3d(), org.joml.Vector3d())

                level.shipObjectWorld.createNewConstraint(
                        VSRopeConstraint(prevId, entity.physicsEntityData!!.shipId,
                                constraint.compliance,
                                prevPos, rpos,
                                constraint.maxForce, 0.01)
                )

                if (i != 0) {
                    level.shipObjectWorld.disableCollisionBetweenBodies(prevId, entity.physicsEntityData!!.shipId)
                }

                prevId = entity.physicsEntityData!!.shipId
                prevPos = data.transform.transformDirectionNoScalingFromShipToWorld((-dir * length).toJomlVector3d(), org.joml.Vector3d())
            }

            level.shipObjectWorld.createNewConstraint(
                    VSRopeConstraint(prevId, constraint.shipId1,
                            constraint.compliance,
                            prevPos, constraint.localPos1,
                            constraint.maxForce, 0.01)
            )
        }

        synchronized(ServerEntitiesHolder.entities) {
            var loaded = 0
            val toLoad = segments

            entitiesUUIDs.forEach {
                val entity = ServerEntitiesHolder.entities[it] ?: return@forEach
                entities.add(entity as PhysRopeComponentEntity)
                loaded++
            }

            if (loaded == toLoad) {
                val temp = mutableListOf<PhysRopeComponentEntity>()
                temp.addAll(entities)
                entities.forEach {
                    temp[entitiesUUIDs.indexOf(it.creationUUID)] = it
                }
                entities = temp

                createConstraints()

                return@synchronized
            }

            ServerEntitiesHolder.entityLoadedEvent.on {
                (uuid, entity), handler ->
                if (!entitiesUUIDs.contains(uuid)) { return@on }
                entity as PhysRopeComponentEntity
                entities.add(entity)

                entity.physicsEntityData!!.isStatic = true

                loaded++
                if (toLoad < loaded) {return@on}

                val temp = mutableListOf<PhysRopeComponentEntity>()
                temp.addAll(entities)
                entities.forEach {
                    temp[entitiesUUIDs.indexOf(it.creationUUID)] = it
                }
                entities = temp

                createConstraints()
            }
        }

        return true
    }

    private fun createNewMConstraint(level: ServerLevel): Boolean {
        val dimensionIds = level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values

        val rpos1 = if(!dimensionIds.contains(constraint.shipId0)) posShipToWorld(level.shipObjectWorld.allShips.getById(constraint.shipId0), Vector3d(constraint.localPos0)) else Vector3d(constraint.localPos0)
        val rpos2 = if(!dimensionIds.contains(constraint.shipId1)) posShipToWorld(level.shipObjectWorld.allShips.getById(constraint.shipId1), Vector3d(constraint.localPos1)) else Vector3d(constraint.localPos1)

        val rdir = (rpos1 - rpos2).snormalize()
        val rot = getQuatFromDir(rdir).normalize()

        val dir = Vector3d(1, 0, 0)

        val xLin = linspace(rpos1.x, rpos2.x, segments + 2)
        val yLin = linspace(rpos1.y, rpos2.y, segments + 2)
        val zLin = linspace(rpos1.z, rpos2.z, segments + 2)

        val segmentLength = ropeLength / segments.toDouble()


        var radius = 0.5
        var length = segmentLength * 0.5
        if (length < radius) { radius = length / 2}

        var prevId = constraint.shipId0
        var prevPos = constraint.localPos0

        for (i in 0 until segments) {
            val pos = Vector3d(xLin[i+1], yLin[i+1], zLin[i+1])
            val entity = PhysRopeComponentEntity.createEntity(
                    level, 1000.0, radius, length, pos.toJomlVector3d(), rot, false)
            entities.add(entity)
            entitiesUUIDs.add(entity.creationUUID!!)

            val data = entity.physicsEntityData!!

            val rpos = data.transform.transformDirectionNoScalingFromShipToWorld((dir * length).toJomlVector3d(), org.joml.Vector3d())

            level.shipObjectWorld.createNewConstraint(
                    VSRopeConstraint(prevId, entity.physicsEntityData!!.shipId,
                            constraint.compliance,
                            prevPos, rpos,
                            constraint.maxForce, 0.01)
            )

            if (i != 0) {
                level.shipObjectWorld.disableCollisionBetweenBodies(prevId, entity.physicsEntityData!!.shipId)
            }

            prevId = entity.physicsEntityData!!.shipId
            prevPos = data.transform.transformDirectionNoScalingFromShipToWorld((-dir * length).toJomlVector3d(), org.joml.Vector3d())

            SynchronisedRenderingData.serverSynchronisedData.addRenderer(
                    constraint.shipId0, constraint.shipId1, mID + i, EA2BRenderer(
                    entity, Color(120, 0, 120), 0.5, length * 2
            ))
        }

        level.shipObjectWorld.createNewConstraint(
                VSRopeConstraint(prevId, constraint.shipId1,
                        constraint.compliance,
                        prevPos, constraint.localPos1,
                        constraint.maxForce, 0.01)
        )

        ConstraintManager.getInstance().constraintIdCounter.setCounter(mID + segments)

        return true
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
//        vsId = level.shipObjectWorld.createNewConstraint(constraint) ?: return false
        if (renderer != null) { SynchronisedRenderingData.serverSynchronisedData.addRenderer(constraint.shipId0, constraint.shipId1, mID, renderer!!)
        } else { renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(mID) }

        return if (entitiesUUIDs.isNotEmpty()) {
            onLoadMConstraint(level)
        } else {
            createNewMConstraint(level)
        }
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
//        level.shipObjectWorld.removeConstraint(vsId)
//        SynchronisedRenderingData.serverSynchronisedData.removeRenderer(mID)
    }
}