package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.compat.vsBackwardsCompat.rotation
import net.spaceeye.vmod.compat.vsBackwardsCompat.scaling
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getHingeRotation
import net.spaceeye.vmod.utils.getQuatd
import net.spaceeye.vmod.utils.getVector3d
import net.spaceeye.vmod.utils.linspace
import net.spaceeye.vmod.utils.putQuatd
import net.spaceeye.vmod.utils.putVector3d
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.vEntityManaging.util.VEAutoSerializable
import org.joml.Matrix3d
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
    var maxForce: Double by get(i++, -1.0)
    var stiffness: Double by get(i++, -1.0)

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

    private fun makeInertiaTensor(l: Double, r: Double, mass: Double): Matrix3d {
        val rSq = r * r

        val cM_temp = l * rSq * PI
        val hsM_temp = 2.0 * rSq * r * PI * (1.0/3.0)

        val density = mass / (cM_temp + hsM_temp)

        val cM = cM_temp * density
        val hsM = hsM_temp * density

        val inertiaTensor = Matrix3d()

        inertiaTensor.m11 = rSq * cM * 0.5

        inertiaTensor.m22 = inertiaTensor.m11 * 0.5 + cM * l * l * (1.0/12.0)
        inertiaTensor.m00 = inertiaTensor.m22

        val temp0 = hsM * 2.0 * rSq / 5.0
        inertiaTensor.m11 += temp0 * 2.0
        val temp1 = l * 0.5
        val temp2 = temp0 + hsM * (temp1 * temp1 + 3.0 * (1.0/8.0) * l * r)
        inertiaTensor.m00 += temp2 * 2.0
        inertiaTensor.m22 += temp2 * 2.0
        return inertiaTensor
    }

    private fun makeData(level: ServerLevel): Boolean {
        val dimensionIds = level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values

        val rPos1 = if (dimensionIds.contains(shipId1)) {sPos1.copy()} else { posShipToWorld(level.shipObjectWorld.allShips.getById(shipId1), sPos1) }
        val rPos2 = if (dimensionIds.contains(shipId2)) {sPos2.copy()} else { posShipToWorld(level.shipObjectWorld.allShips.getById(shipId2), sPos2) }

        val rDir = (rPos1 - rPos2).normalize()

        val segmentLength = ropeLength / segments.toDouble()

        val startDir = rPos1 - rDir * (segmentLength / 2.0)
        val stopDir  = rPos2 + rDir * (segmentLength / 2.0)

        val rot = getHingeRotation(rDir)

        val xLin = linspace(startDir.x, stopDir.x, segments)
        val yLin = linspace(startDir.y, stopDir.y, segments)
        val zLin = linspace(startDir.z, stopDir.z, segments)

        var radius = radius
        var length = segmentLength * 0.5
        if (length < radius) { radius = length / 2 }
        if (length > radius * 4) { radius = length / 4 }

        val linkLength = length - radius
        val tensor = makeInertiaTensor(linkLength, radius, massPerSegment)
        if (!tensor.isFinite) {return false}

        for (i in 0 until segments) {
            val pos = Vector3d(xLin[i], yLin[i], zLin[i])
            data.add(PhysicsEntityData(
                level.shipObjectWorld.allocateShipId(level.dimensionId),
                ShipTransformImpl(pos.toJomlVector3d(), org.joml.Vector3d(), rot, org.joml.Vector3d()),
                ShipInertiaDataImpl(org.joml.Vector3d(), massPerSegment, tensor.get(Matrix3d())),
                org.joml.Vector3d(), org.joml.Vector3d(),
                VSCapsuleCollisionShapeData(radius, linkLength)
            ))
        }
        return true
    }

    private fun makeConstraints(level: ServerLevel): Boolean {
        val dir = Vector3d(1, 0, 0)
        val stiffness = if (stiffness <= 0) {1e300} else {stiffness}
        val maxForce = if (maxForce < 0) {1e300} else {maxForce}
        val compliance = 1.0 / stiffness

        var prevId = shipId1
        var prevPos = sPos1

        entities.forEach { entity ->
            val length = (entity.collisionShapeData as VSCapsuleCollisionShapeData).length
            val radius = (entity.collisionShapeData as VSCapsuleCollisionShapeData).radius
            cIDs.add(level.shipObjectWorld.createNewConstraint(VSRopeConstraint(
                prevId, entity.id, compliance, prevPos.toJomlVector3d(), (dir * (length + radius)).toJomlVector3d(), maxForce, 0.0
            )) ?: run { return false })

            level.shipObjectWorld.disableCollisionBetweenBodies(prevId, entity.id)

            prevId = entity.id
            prevPos = (-dir * (length + radius))
        }

        cIDs.add(level.shipObjectWorld.createNewConstraint(VSRopeConstraint(
            prevId, shipId2, compliance, prevPos.toJomlVector3d(), sPos2.toJomlVector3d(), maxForce, 0.0
        )) ?: run { return false })
        level.shipObjectWorld.disableCollisionBetweenBodies(prevId, shipId2)

        return true
    }

    override fun iNbtSerialize(): CompoundTag? {
        val tag = super.iNbtSerialize() ?: return null
        tag.put("data", ListTag().also {
            it.addAll(entities.map { entity -> CompoundTag().also {
                val shape = entity.collisionShapeData as VSCapsuleCollisionShapeData
                it.putLong("shipId", entity.id)
                it.putVector3d("positionInWorld", entity.shipTransform.positionInWorld)
                it.putQuatd("rotation", entity.shipTransform.rotation)
                it.putVector3d("scaling", entity.shipTransform.scaling)
                it.putDouble("mass", entity.inertiaData.mass)
                it.putDouble("length", shape.length)
                it.putDouble("radius", shape.radius)
                it.putVector3d("linearVelocity", entity.linearVelocity)
                it.putVector3d("angularVelocity", entity.angularVelocity)
            }})}
        )
        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): VEntity? {
        val entity = super.iNbtDeserialize(tag, lastDimensionIds) ?: return null
        entity as PhysRopeConstraint
        entity.data = (tag.get("data") as ListTag).map { tag -> tag as CompoundTag
            PhysicsEntityData(
                tag.getLong("shipId"),
                ShipTransformImpl(tag.getVector3d("positionInWorld")!!, org.joml.Vector3d(), tag.getQuatd("rotation")!!, tag.getVector3d("scaling")!!),
                ShipInertiaDataImpl(org.joml.Vector3d(), tag.getDouble("mass"), makeInertiaTensor(tag.getDouble("length"), tag.getDouble("radius"), tag.getDouble("mass"))),
                tag.getVector3d("linearVelocity")!!,
                tag.getVector3d("angularVelocity")!!,
                VSCapsuleCollisionShapeData(tag.getDouble("radius"), tag.getDouble("length"))
                )
        }.toMutableList()
        return entity
    }

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        if (data.isEmpty()) { if (!makeData(level)) return false }
        entities = data.map { level.shipObjectWorld.createPhysicsEntity(it, level.dimensionId) }.toMutableList()
        return makeConstraints(level)
    }

    override fun iOnDeleteVEntity(level: ServerLevel) {
        super.iOnDeleteVEntity(level)
        entities.forEach { level.shipObjectWorld.deletePhysicsEntity(it.id) }
    }
}