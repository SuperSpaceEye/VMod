package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.constraintsManaging.util.dc
import net.spaceeye.vmod.constraintsManaging.util.mc
import net.spaceeye.vmod.constraintsManaging.util.sc
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getHingeRotation
import net.spaceeye.vmod.utils.vs.*
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

    lateinit var constraint1: VSAttachmentConstraint
    lateinit var constraint2: VSAttachmentConstraint
    lateinit var rconstraint: VSTorqueConstraint

    override val mainConstraint: VSConstraint get() = constraint1

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

        constraint1 = VSAttachmentConstraint(
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

        constraint2 = VSAttachmentConstraint(
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

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        val new = ConnectionMConstraint()

        new.fixedLength = fixedLength
        new.connectionMode = connectionMode
        new.attachmentPoints_ = copyAttachmentPoints(constraint1, attachmentPoints_, level, mapped)

        new.constraint1 = constraint1.copy(level, mapped) ?: return null
        if (connectionMode != ConnectionModes.FREE_ORIENTATION) {
            new.constraint2 = constraint2.copy(level, mapped) ?: return null
            new.rconstraint = rconstraint.copy(mapped) ?: return null
        }

        return new
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        constraint1 = constraint1.copy(fixedDistance = constraint1.fixedDistance * scaleBy)
        level.shipObjectWorld.removeConstraint(cIDs[0])
        cIDs[0] = level.shipObjectWorld.createNewConstraint(constraint1)!!

        if (connectionMode == ConnectionModes.FREE_ORIENTATION) {return}

        constraint2 = constraint2.copy(fixedDistance = constraint2.fixedDistance * scaleBy)
        level.shipObjectWorld.removeConstraint(cIDs[1])
        cIDs[1] = level.shipObjectWorld.createNewConstraint(constraint2)!!
    }

    override fun iNbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putInt("mode", connectionMode.ordinal)

        sc("c1", constraint1, tag) {return null}
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) {return tag}

        sc("c2", constraint2, tag) {return null}
        sc("c3", rconstraint, tag) {return null}

        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        connectionMode = ConnectionModes.values()[tag.getInt("mode")]

        dc("c1", ::constraint1, tag, lastDimensionIds) {return null}
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) {return this}

        dc("c2", ::constraint2, tag, lastDimensionIds) {return null}
        dc("c3", ::rconstraint, tag, lastDimensionIds) {return null}

        return this
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        mc(constraint1, cIDs, level) {return false}
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) { return true }
        mc(constraint2, cIDs, level) {return false}
        mc(rconstraint, cIDs, level) {return false}

        return true
    }
}