package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.network.*
import net.spaceeye.vmod.rendering.ServerRenderingData
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.deserializeConstraint
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import net.spaceeye.vmod.utils.vs.copy
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.utils.vs.posWorldToShip
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class HydraulicsMConstraint(): MConstraint, MRenderable, Tickable {
    enum class ConnectionMode {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }

    lateinit var aconstraint1: VSAttachmentConstraint
    lateinit var aconstraint2: VSAttachmentConstraint
    lateinit var rconstraint: VSTorqueConstraint

    var rID: Int = -1

    var attachmentPoints_ = mutableListOf<BlockPos>()

    val cIDs = mutableListOf<ConstraintId>()

    var minLength: Double = -1.0
    var maxLength: Double = -1.0

    var extensionSpeed: Double = 1.0
    var extendedDist: Double = 0.0

    var addDist: Double = 0.0

    var channel: String = ""

    var mode = MessageModes.Toggle

    var connectionMode = ConnectionMode.FIXED_ORIENTATION

    override var renderer: BaseRenderer? = null

    override var mID: ManagedConstraintId = -1
    override val typeName: String get() = "HydraulicsMConstraint"
    override var saveCounter: Int = -1

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

        messageModes: MessageModes,
        _connectionMode: ConnectionMode,

        attachmentPoints: List<BlockPos>,

        _renderer: BaseRenderer? = null,

        _dir: Vector3d? = null
    ): this() {
        renderer = _renderer
        minLength = _minLength
        maxLength = _maxLength
        // extensionSpeed is in seconds. Constraint is being updated every mc tick
        extensionSpeed = _extensionSpeed / 20.0

        channel = _channel
        mode = messageModes
        connectionMode = _connectionMode

        attachmentPoints_ = attachmentPoints.toMutableList()

        aconstraint1 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, minLength)

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) { return }

        val dist1 = rpoint1 - rpoint2
        val len = (_dir ?: dist1).dist()
        val dir = (_dir ?: run { dist1.normalize() }) * ( if (len < 10 || len > 30) 20 else 40)

        val rpoint1 = rpoint1 + dir
        val rpoint2 = rpoint2 - dir

        val dist2 = rpoint1 - rpoint2
        addDist = dist2.dist() - dist1.dist()

        val spoint1 = if (ship1 != null) posWorldToShip(ship1, rpoint1) else Vector3d(rpoint1)
        val spoint2 = if (ship2 != null) posWorldToShip(ship2, rpoint2) else Vector3d(rpoint2)

        aconstraint2 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, (minLength + addDist)
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

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(aconstraint1.shipId0)
        val ship2Exists = allShips.contains(aconstraint1.shipId1)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(aconstraint1.shipId1))
                || (ship2Exists && dimensionIds.contains(aconstraint1.shipId0))
    }

    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(aconstraint1.shipId0)) {toReturn.add(aconstraint1.shipId0)}
        if (!dimensionIds.contains(aconstraint1.shipId1)) {toReturn.add(aconstraint1.shipId1)}

        return toReturn
    }

    override fun getAttachmentPoints(): List<BlockPos> = attachmentPoints_
    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        if (previous != attachmentPoints_[0] && previous != attachmentPoints_[1]) {return}
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        cIDs.clear()

        val shipIds = mutableListOf(aconstraint1.shipId0, aconstraint1.shipId1)
        val localPoints = mutableListOf(
            if (connectionMode == ConnectionMode.FREE_ORIENTATION) {listOf(aconstraint1.localPos0)} else {listOf(aconstraint1.localPos0, aconstraint2.localPos0)},
            if (connectionMode == ConnectionMode.FREE_ORIENTATION) {listOf(aconstraint1.localPos1)} else {listOf(aconstraint1.localPos1, aconstraint2.localPos1)},
        )
        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)

        aconstraint1 = aconstraint1.copy(shipIds[0], shipIds[1], aconstraint1.compliance, localPoints[0][0], localPoints[1][0])
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1)!!)

        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], rID)
        renderer = ServerRenderingData.getRenderer(rID)

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return}
        aconstraint2 = aconstraint2.copy(shipIds[0], shipIds[1], aconstraint2.compliance, localPoints[0][1], localPoints[1][1])
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2)!!)

        rconstraint = rconstraint.copy(shipIds[0], shipIds[1])
        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint)!!)
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, aconstraint1, attachmentPoints_, renderer) {
            nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints, newRenderer ->

            val rpoint1 = if (nShip1 != null) { posShipToWorld(nShip1, localPos0) } else localPos0
            val rpoint2 = if (nShip2 != null) { posShipToWorld(nShip2, localPos1) } else localPos1

            if (connectionMode == ConnectionMode.FREE_ORIENTATION) {
                val ret = HydraulicsMConstraint(localPos0, localPos1, rpoint1, rpoint2, nShip1, nShip2, nShip1Id, nShip2Id, aconstraint1.compliance, aconstraint1.maxForce, minLength, maxLength, extensionSpeed, channel, mode, connectionMode, newAttachmentPoints, newRenderer, null)
                ret.addDist = addDist
                ret.extendedDist = extendedDist
                ret.extensionSpeed = extensionSpeed
                return@commonCopy ret
            }

            // Why? if the mode chosen is hinge and its points are very close to each other, due to inaccuracy the
            // direction will be wrong after copy. So instead use supporting constraint to get the direction.
            commonCopy(level, mapped, aconstraint2, attachmentPoints_, null) {
                    _, _, _, _, slocalPos0, slocalPos1, _, _ ->

                val srpoint1 = if (nShip1 != null) { posShipToWorld(nShip1, slocalPos0) } else slocalPos0
                val srpoint2 = if (nShip2 != null) { posShipToWorld(nShip2, slocalPos1) } else slocalPos1

                val dir = srpoint1 - srpoint2

                val ret = HydraulicsMConstraint(localPos0, localPos1, rpoint1, rpoint2, nShip1, nShip2, nShip1Id, nShip2Id, aconstraint1.compliance, aconstraint1.maxForce, minLength, maxLength, extensionSpeed, channel, mode, connectionMode, newAttachmentPoints, newRenderer, dir)
                ret.addDist = addDist
                ret.extendedDist = extendedDist
                ret.extensionSpeed = extensionSpeed
                return@commonCopy ret
            }
        }
    }

    override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        minLength *= scaleBy
        extendedDist *= scaleBy
        addDist *= scaleBy

        aconstraint1 = aconstraint1.copy(fixedDistance = aconstraint1.fixedDistance * scaleBy)
        level.shipObjectWorld.removeConstraint(cIDs[0])
        cIDs[0] = level.shipObjectWorld.createNewConstraint(aconstraint1)!!

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return}

        aconstraint2 = aconstraint1.copy(fixedDistance = aconstraint2.fixedDistance * scaleBy)
        level.shipObjectWorld.removeConstraint(cIDs[1])
        cIDs[1] = level.shipObjectWorld.createNewConstraint(aconstraint2)!!
    }

    override fun getVSIds(): Set<VSConstraintId> {
        return cIDs.toSet()
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putInt("managedID", mID)

        tag.putDouble("addDist", addDist)
        tag.putDouble("minDistance", minLength)
        tag.putDouble("maxDistance", maxLength)
        tag.putDouble("extensionSpeed", extensionSpeed)
        tag.putDouble("extendedDist", extendedDist)
        tag.putBoolean("isActivating", fnToUse == ::activatingFn)
        tag.putBoolean("isDeactivating", fnToUse == ::deactivatingFn)
        tag.putString("channel", channel)
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))
        tag.putInt("mode", mode.ordinal)
        tag.putInt("constraintMode", connectionMode.ordinal)

        serializeRenderer(tag)

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(aconstraint1) ?: return null)
        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return tag}
        tag.put("c2", VSConstraintSerializationUtil.serializeConstraint(aconstraint2) ?: return null)
        tag.put("c3", VSConstraintSerializationUtil.serializeConstraint(rconstraint) ?: return null)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("managedID")

        addDist = tag.getDouble("addDist")
        minLength = tag.getDouble("minDistance")
        maxLength = tag.getDouble("maxDistance")
        extensionSpeed = tag.getDouble("extensionSpeed")
        extendedDist = tag.getDouble("extendedDist")
        channel = tag.getString("channel")
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)

        fnToUse = when {
            tag.getBoolean("isActivating") -> ::activatingFn
            tag.getBoolean("isDeactivating") -> ::deactivatingFn
            else -> null
        }

        mode = MessageModes.values()[tag.getInt("mode")]
        connectionMode = if (tag.contains("constraintMode")) {ConnectionMode.values()[tag.getInt("constraintMode")]} else {ConnectionMode.FIXED_ORIENTATION}

        deserializeRenderer(tag)

        tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); aconstraint1 = (deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return this}
        tryConvertDimensionId(tag["c2"] as CompoundTag, lastDimensionIds); aconstraint2 = (deserializeConstraint(tag["c2"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c3"] as CompoundTag, lastDimensionIds); rconstraint = (deserializeConstraint(tag["c3"] as CompoundTag) ?: return null) as VSTorqueConstraint

        return this
    }

    var wasDeleted = false
    var fnToUse: (() -> Boolean)? = null
    var lastExtended: Double = 0.0

    // ======================================================================================

    // a sum of all activation(+1) and deactivations (-1)
    // if the sum is 0, then do nothing
    // if the sum is >0, then set function to activating
    // if the sum if <0, then set function to deactivating
    // gets reset every tick
    // is needed for when there are multiple commands in the same tick
    var activationCounter = 0

    private fun activatingFn(): Boolean {
        extendedDist = min(extendedDist + extensionSpeed, maxLength - minLength)
        if (extendedDist >= maxLength - minLength) { return false }
        return true
    }

    private fun deactivatingFn(): Boolean {
        extendedDist = max(extendedDist - extensionSpeed, 0.0)
        if (extendedDist <= 0) {return false}
        return true
    }

    private fun toggleTick(msg: Message) {
        when (msg) {
            is Activate   -> { if(fnToUse == null) {activationCounter = 0}; activationCounter++ }
            is Deactivate -> { if(fnToUse == null) {activationCounter = 0}; activationCounter-- }
            else -> return
        }

        when {
            activationCounter > 0 -> { fnToUse = ::activatingFn }
            activationCounter < 0 -> { fnToUse = ::deactivatingFn }
        }
    }

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
        activationCounter = 0

        val shipObjectWorld = server.shipObjectWorld

        if (!shipObjectWorld.removeConstraint(cIDs[0])) {return}
        aconstraint1 = aconstraint1.copy(fixedDistance = minLength + extendedDist)
        cIDs[0] = shipObjectWorld.createNewConstraint(aconstraint1) ?: return

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return}

        if (!shipObjectWorld.removeConstraint(cIDs[1])) {return}
        aconstraint2 = aconstraint2.copy(fixedDistance = minLength + addDist + extendedDist)
        cIDs[1] = shipObjectWorld.createNewConstraint(aconstraint2) ?: return
    }

    private fun <T> clean(level: ServerLevel): T? {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        return null
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        MessagingNetwork.register(channel) {
            msg, unregister ->
            if (wasDeleted) {unregister(); return@register}

            when (mode) {
                MessageModes.Toggle -> toggleTick(msg)
                MessageModes.Signal -> signalTick(msg)
            }
        }

        if (renderer != null) { rID = ServerRenderingData.addRenderer(aconstraint1.shipId0, aconstraint1.shipId1, renderer!!)
        } else { renderer = ServerRenderingData.getRenderer(rID) }

        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1) ?: clean(level) ?: return false)
        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return true}
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2) ?: clean(level) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint) ?: clean(level) ?: return false)

        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        wasDeleted = true
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        ServerRenderingData.removeRenderer(rID)
    }
}