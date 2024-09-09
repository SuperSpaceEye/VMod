package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.constraintsManaging.util.dc
import net.spaceeye.vmod.constraintsManaging.util.mc
import net.spaceeye.vmod.constraintsManaging.util.sc
import net.spaceeye.vmod.network.*
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.copy
import net.spaceeye.vmod.utils.vs.copyAttachmentPoints
import net.spaceeye.vmod.utils.vs.transformDirectionWorldToShip
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class HydraulicsMConstraint(): TwoShipsMConstraint(), Tickable {
    enum class ConnectionMode {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }

    lateinit var constraint1: VSAttachmentConstraint
    lateinit var constraint2: VSAttachmentConstraint
    lateinit var rconstraint: VSTorqueConstraint

    override val mainConstraint: VSConstraint get() = constraint1

    var minLength: Double = -1.0
    var maxLength: Double = -1.0

    var extensionSpeed: Double = 1.0
    var extendedDist: Double = 0.0

    var addDist: Double = 0.0

    var channel: String = ""

    var connectionMode = ConnectionMode.FIXED_ORIENTATION

    var dir1: Vector3d? = null
    var dir2: Vector3d? = null

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

        _minLength: Double,
        _maxLength: Double,
        _extensionSpeed: Double,

        _channel: String,

        _connectionMode: ConnectionMode,

        attachmentPoints: List<BlockPos>,

        wDir: Vector3d? = null,

        sDir1: Vector3d? = null,
        sDir2: Vector3d? = null,
    ): this() {
        minLength = _minLength
        maxLength = _maxLength
        // extensionSpeed is in seconds. Constraint is being updated every mc tick
        extensionSpeed = _extensionSpeed / 20.0

        channel = _channel
        connectionMode = _connectionMode

        attachmentPoints_ = attachmentPoints.toMutableList()

        constraint1 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, minLength)

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) { return }

        val dist1 = rpoint1 - rpoint2
        val dir = (wDir ?: dist1).normalize() * (dist1.dist() * 2)

        this.dir1 = sDir1 ?: (ship1?.let { transformDirectionWorldToShip(it, +dir) } ?: +dir).normalize()
        this.dir2 = sDir2 ?: (ship2?.let { transformDirectionWorldToShip(it, -dir) } ?: -dir).normalize()
        val dir1 = this.dir1!!
        val dir2 = this.dir2!!

        constraint2 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            (spoint1 + dir1 * minLength / 2).toJomlVector3d(),
            (spoint2 + dir2 * minLength / 2).toJomlVector3d(),
            maxForce, minLength * 2
        )

        rconstraint = when (connectionMode) {
            ConnectionMode.FIXED_ORIENTATION -> {
                val frot1 = ship1?.transform?.shipToWorldRotation ?: Quaterniond()
                val frot2 = ship2?.transform?.shipToWorldRotation ?: Quaterniond()
                VSFixedOrientationConstraint(shipId0, shipId1, compliance, frot1.invert(Quaterniond()), frot2.invert(Quaterniond()), 1e300)
            }
            ConnectionMode.HINGE_ORIENTATION -> {
                val hrot1 = getHingeRotation(ship1?.transform, dir.normalize())
                val hrot2 = getHingeRotation(ship2?.transform, dir.normalize())
                VSHingeOrientationConstraint(shipId0, shipId1, compliance, hrot1, hrot2, maxForce)
            }
            ConnectionMode.FREE_ORIENTATION -> throw AssertionError("can't happen")
        }
    }

    override fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        throw NotImplementedError()
//        if (previous != attachmentPoints_[0] && previous != attachmentPoints_[1]) {return}
//        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
//        cIDs.clear()
//
//        val shipIds = mutableListOf(aconstraint1.shipId0, aconstraint1.shipId1)
//        val localPoints = mutableListOf(
//            if (connectionMode == ConnectionMode.FREE_ORIENTATION) {listOf(aconstraint1.localPos0)} else {listOf(aconstraint1.localPos0, aconstraint2.localPos0)},
//            if (connectionMode == ConnectionMode.FREE_ORIENTATION) {listOf(aconstraint1.localPos1)} else {listOf(aconstraint1.localPos1, aconstraint2.localPos1)},
//        )
//        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)
//
//        aconstraint1 = aconstraint1.copy(shipIds[0], shipIds[1], aconstraint1.compliance, localPoints[0][0], localPoints[1][0])
//        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1)!!)
//
////        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], rID)
////        renderer = ServerRenderingData.getRenderer(rID)
//
//        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return}
//        aconstraint2 = aconstraint2.copy(shipIds[0], shipIds[1], aconstraint2.compliance, localPoints[0][1], localPoints[1][1])
//        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2)!!)
//
//        rconstraint = rconstraint.copy(shipIds[0], shipIds[1])
//        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint)!!)
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        val new = HydraulicsMConstraint()

        new.attachmentPoints_ = copyAttachmentPoints(constraint1, attachmentPoints_, level, mapped)

        new.constraint1 = constraint1.copy(level, mapped) ?: return null
        new.constraint2 = constraint2.copy(level, mapped) ?: return null
        new.rconstraint = rconstraint.copy(mapped) ?: return null

        new.minLength = minLength
        new.maxLength = maxLength

        new.extensionSpeed = extensionSpeed
        new.extendedDist = extendedDist

        new.addDist = addDist
        new.channel = channel

        new.connectionMode = connectionMode

        new.dir1 = dir1?.copy()
        new.dir2 = dir2?.copy()

        return new
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        minLength *= scaleBy
        extendedDist *= scaleBy
        addDist *= scaleBy

        constraint1 = constraint1.copy(fixedDistance = constraint1.fixedDistance * scaleBy)
        level.shipObjectWorld.removeConstraint(cIDs[0])
        cIDs[0] = level.shipObjectWorld.createNewConstraint(constraint1)!!

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return}

        constraint2 = constraint2.copy(fixedDistance = constraint2.fixedDistance * scaleBy)
        level.shipObjectWorld.removeConstraint(cIDs[1])
        cIDs[1] = level.shipObjectWorld.createNewConstraint(constraint2)!!
    }

    override fun iNbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putDouble("addDist", addDist)
        tag.putDouble("minDistance", minLength)
        tag.putDouble("maxDistance", maxLength)
        tag.putDouble("extensionSpeed", extensionSpeed)
        tag.putDouble("extendedDist", extendedDist)
        tag.putString("channel", channel)
        tag.putInt("constraintMode", connectionMode.ordinal)

        sc("c1", constraint1, tag) {return null}
        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return tag}

        tag.putVector3d("dir1", dir1!!.toJomlVector3d())
        tag.putVector3d("dir2", dir2!!.toJomlVector3d())

        sc("c2", constraint2, tag) {return null}
        sc("c3", rconstraint, tag) {return null}

        return tag
    }

    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        addDist = tag.getDouble("addDist")
        minLength = tag.getDouble("minDistance")
        maxLength = tag.getDouble("maxDistance")
        extensionSpeed = tag.getDouble("extensionSpeed")
        extendedDist = tag.getDouble("extendedDist")
        channel = tag.getString("channel")

        connectionMode = if (tag.contains("constraintMode")) {ConnectionMode.values()[tag.getInt("constraintMode")]} else {ConnectionMode.FIXED_ORIENTATION}

        dc("c1", ::constraint1, tag, lastDimensionIds) {return null}
        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return this}

        dir1 = Vector3d(tag.getVector3d("dir1")!!)
        dir2 = Vector3d(tag.getVector3d("dir2")!!)

        dc("c2", ::constraint2, tag, lastDimensionIds) {return null}
        dc("c3", ::rconstraint, tag, lastDimensionIds) {return null}

        return this
    }

    var wasDeleted = false
    var fnToUse: (() -> Boolean)? = null
    var lastExtended: Double = 0.0

    // ======================================================================================

    var targetPercentage = 0.0

    var totalPercentage = 0.0
    var numMessages = 0

    private fun signalFn(): Boolean {
        if (numMessages != 0) {
            targetPercentage = totalPercentage / numMessages
            numMessages = 0
            totalPercentage = 0.0
        }
        val length = maxLength - minLength

        val currentPercentage = extendedDist / length
        if (abs(currentPercentage - targetPercentage) < 1e-6) { return false }
        val left = targetPercentage - currentPercentage
        val percentageStep = extensionSpeed / length
        extendedDist += min(percentageStep, abs(left)) * length * left.sign
        return true
    }

    private fun signalTick(msg: Message) {
        if (msg !is Signal) { return }

        totalPercentage = min(max(msg.percentage, 0.0), 1.0)
        numMessages++

        fnToUse = ::signalFn
    }

    // ======================================================================================

    override fun tick(server: MinecraftServer, unregister: () -> Unit) {
        if (wasDeleted) {
            unregister()
            return
        }
        if (fnToUse != null) { if (!fnToUse!!()) {fnToUse = null} }

        if (lastExtended == extendedDist) {return}
        lastExtended = extendedDist

        val shipObjectWorld = server.shipObjectWorld

        if (!shipObjectWorld.removeConstraint(cIDs[0])) {return}
        constraint1 = constraint1.copy(fixedDistance = minLength + extendedDist)
        cIDs[0] = shipObjectWorld.createNewConstraint(constraint1) ?: return

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return}

        if (!shipObjectWorld.removeConstraint(cIDs[1])) {return}

        constraint2 = constraint2.copy(
            fixedDistance = (minLength + extendedDist) * 2,
            localPos0 = (Vector3d(constraint1.localPos0) + dir1!! * (minLength + extendedDist) / 2).toJomlVector3d(),
            localPos1 = (Vector3d(constraint1.localPos1) + dir2!! * (minLength + extendedDist) / 2).toJomlVector3d()
        )
        cIDs[1] = shipObjectWorld.createNewConstraint(constraint2) ?: return
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        MessagingNetwork.register(channel) {
            msg, unregister ->
            if (wasDeleted) {unregister(); return@register}

            signalTick(msg)
        }

        mc(constraint1, cIDs, level) {return false}
        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return true}
        mc(constraint2, cIDs, level) {return false}
        mc(rconstraint, cIDs, level) {return false}

        return true
    }

    override fun iOnDeleteMConstraint(level: ServerLevel) {
        super.iOnDeleteMConstraint(level)
        wasDeleted = true
    }
}