package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getHingeRotation
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.deserializeConstraint
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import net.spaceeye.vmod.utils.vs.copy
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.utils.vs.posWorldToShip
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.mod.common.shipObjectWorld

class ConnectionMConstraint(): TwoShipsMConstraint() {
    enum class ConnectionModes {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }

    lateinit var aconstraint1: VSAttachmentConstraint
    lateinit var aconstraint2: VSAttachmentConstraint
    lateinit var rconstraint: VSTorqueConstraint

    override val mainConstraint: VSConstraint get() = aconstraint1

    var fixedLength: Double = 0.0
    var connectionMode: ConnectionModes = ConnectionModes.FIXED_ORIENTATION

    constructor(
        // shipyard pos
        spoint1: Vector3d,
        spoint2: Vector3d,
        // world pos
        rpoint1: Vector3d,
        rpoint2: Vector3d,
        ship1: Ship?,
        ship2: Ship?,
        shipId0: ShipId,
        shipId1: ShipId,
        compliance: Double,
        maxForce: Double,

        fixedLength: Double,
        connectionMode: ConnectionModes,

        attachmentPoints: List<BlockPos>,

        _dir: Vector3d? = null
    ): this() {
        this.fixedLength = if (fixedLength < 0) (rpoint1 - rpoint2).dist() else fixedLength
        attachmentPoints_ = attachmentPoints.toMutableList()

        aconstraint1 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, this.fixedLength)

        this.connectionMode = connectionMode

        if (connectionMode == ConnectionModes.FREE_ORIENTATION) { return }

        val dist1 = rpoint1 - rpoint2
        val len = (_dir ?: dist1).dist()
        val dir = (_dir ?: run { dist1.normalize() }) * ( if (len < 10 || len > 30) 20 else 40)

        val rpoint1 = rpoint1 + dir
        val rpoint2 = rpoint2 - dir

        val dist2 = rpoint1 - rpoint2
        val addDist = dist2.dist() - dist1.dist()

        val spoint1 = if (ship1 != null) posWorldToShip(ship1, rpoint1) else Vector3d(rpoint1)
        val spoint2 = if (ship2 != null) posWorldToShip(ship2, rpoint2) else Vector3d(rpoint2)

        aconstraint2 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, this.fixedLength + addDist
        )

        rconstraint = when (connectionMode) {
            ConnectionModes.FIXED_ORIENTATION -> {
                val frot1 = ship1?.transform?.shipToWorldRotation ?: Quaterniond()
                val frot2 = ship2?.transform?.shipToWorldRotation ?: Quaterniond()
                VSFixedOrientationConstraint(shipId0, shipId1, compliance, frot1.invert(Quaterniond()), frot2.invert(Quaterniond()), maxForce)
            }
            ConnectionModes.HINGE_ORIENTATION -> {
                val hrot1 = getHingeRotation(ship1?.transform, dir.normalize())
                val hrot2 = getHingeRotation(ship2?.transform, dir.normalize())
                VSHingeOrientationConstraint(shipId0, shipId1, compliance, hrot1, hrot2, maxForce)
            }
            ConnectionModes.FREE_ORIENTATION -> throw AssertionError("can't happen")
        }
    }

    override fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        throw NotImplementedError()
        if (previous != attachmentPoints_[0] && previous != attachmentPoints_[1]) {return}
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        cIDs.clear()

        val shipIds = mutableListOf(aconstraint1.shipId0, aconstraint1.shipId1)
        val localPoints = mutableListOf(
            if (connectionMode == ConnectionModes.FREE_ORIENTATION) {listOf(aconstraint1.localPos0)} else {listOf(aconstraint1.localPos0, aconstraint2.localPos0)},
            if (connectionMode == ConnectionModes.FREE_ORIENTATION) {listOf(aconstraint1.localPos1)} else {listOf(aconstraint1.localPos1, aconstraint2.localPos1)},
        )
        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)

        aconstraint1 = aconstraint1.copy(shipIds[0], shipIds[1], aconstraint1.compliance, localPoints[0][0], localPoints[1][0])
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1)!!)

//        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], rID)
//        renderer = ServerRenderingData.getRenderer(rID)

        if (connectionMode == ConnectionModes.FREE_ORIENTATION) {return}
        aconstraint2 = aconstraint2.copy(shipIds[0], shipIds[1], aconstraint2.compliance, localPoints[0][1], localPoints[1][1])
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2)!!)

        rconstraint = rconstraint.copy(shipIds[0], shipIds[1])

        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint)!!)
    }

    //TODO it's probably wrong?
    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, aconstraint1, attachmentPoints_) {
            nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints ->

            val rpoint1 = if (nShip1 != null) { posShipToWorld(nShip1, localPos0) } else localPos0
            val rpoint2 = if (nShip2 != null) { posShipToWorld(nShip2, localPos1) } else localPos1

            if (connectionMode == ConnectionModes.FREE_ORIENTATION) {
                return@commonCopy ConnectionMConstraint(localPos0, localPos1, rpoint1, rpoint2, nShip1, nShip2, nShip1Id, nShip2Id, aconstraint1.compliance, aconstraint1.maxForce, aconstraint1.fixedDistance, connectionMode, newAttachmentPoints)
            }

            // Why? if the mode chosen is hinge and its points are very close to each other, due to inaccuracy the
            // direction will be wrong after copy. So instead use supporting constraint to get the direction.
            commonCopy(level, mapped, aconstraint2, attachmentPoints_) {
                _, _, _, _, slocalPos0, slocalPos1, _ ->

                val srpoint1 = if (nShip1 != null) { posShipToWorld(nShip1, slocalPos0) } else slocalPos0
                val srpoint2 = if (nShip2 != null) { posShipToWorld(nShip2, slocalPos1) } else slocalPos1

                val dir = srpoint1 - srpoint2

                ConnectionMConstraint(localPos0, localPos1, rpoint1, rpoint2, nShip1, nShip2, nShip1Id, nShip2Id, aconstraint1.compliance, aconstraint1.maxForce, aconstraint1.fixedDistance, connectionMode, newAttachmentPoints, dir)
            }
        }
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        aconstraint1 = aconstraint1.copy(fixedDistance = aconstraint1.fixedDistance * scaleBy)
        level.shipObjectWorld.removeConstraint(cIDs[0])
        cIDs[0] = level.shipObjectWorld.createNewConstraint(aconstraint1)!!

        if (connectionMode == ConnectionModes.FREE_ORIENTATION) {return}

        aconstraint2 = aconstraint2.copy(fixedDistance = aconstraint2.fixedDistance * scaleBy)
        level.shipObjectWorld.removeConstraint(cIDs[1])
        cIDs[1] = level.shipObjectWorld.createNewConstraint(aconstraint2)!!
    }

    override fun iNbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putInt("mode", connectionMode.ordinal)

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(aconstraint1) ?: return null)
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) {return tag}

        tag.put("c2", VSConstraintSerializationUtil.serializeConstraint(aconstraint2) ?: return null)
        tag.put("c3", VSConstraintSerializationUtil.serializeConstraint(rconstraint) ?: return null)

        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        connectionMode = ConnectionModes.values()[tag.getInt("mode")]

        tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); aconstraint1 = (deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) {return this}

        tryConvertDimensionId(tag["c2"] as CompoundTag, lastDimensionIds); aconstraint2 = (deserializeConstraint(tag["c2"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c3"] as CompoundTag, lastDimensionIds); rconstraint  = (deserializeConstraint(tag["c3"] as CompoundTag) ?: return null) as VSTorqueConstraint

        return this
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1) ?: clean(level) ?: return false)
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) { return true }
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2) ?: clean(level) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint ) ?: clean(level) ?: return false)

        return true
    }

    override fun iOnDeleteMConstraint(level: ServerLevel) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
    }
}