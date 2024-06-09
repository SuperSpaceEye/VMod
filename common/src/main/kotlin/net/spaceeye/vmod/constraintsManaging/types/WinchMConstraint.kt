package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.network.*
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.deserializeConstraint
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.rendering.SynchronisedRenderingData
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import net.spaceeye.vmod.utils.vs.posShipToWorld
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

//TODO this doesn't really work
class WinchMConstraint(): MConstraint, MRenderable, Tickable {
    lateinit var constraint: VSRopeConstraint

    var attachmentPoints_ = mutableListOf<BlockPos>()

    val cIDs = mutableListOf<ConstraintId>()

    var minLength: Double = -1.0
    var maxLength: Double = -1.0

    var extensionSpeed: Double = 1.0
    var extendedDist: Double = 0.0

    var addDist: Double = 0.0

    var channel: String = ""

    var mode = MessageModes.Toggle

    override var renderer: BaseRenderer? = null

    override var mID: ManagedConstraintId = -1
    override val typeName: String get() = "WinchMConstraint"
    override var saveCounter: Int = -1

    constructor(
        // shipyard pos
        spoint1: Vector3d,
        spoint2: Vector3d,
        shipId0: ShipId,
        shipId1: ShipId,
        compliance: Double,
        maxForce: Double,

        _minLength: Double,
        _maxLength: Double,
        _extensionSpeed: Double,

        _channel: String,

        messageModes: MessageModes,

        attachmentPoints: List<BlockPos>,

        _renderer: BaseRenderer? = null,
        ): this() {
        renderer = _renderer
        minLength = _minLength
        maxLength = _maxLength
        // extensionSpeed is in seconds. Constraint is being updated every mc tick
        extensionSpeed = _extensionSpeed / 20.0

        channel = _channel
        mode = messageModes

        attachmentPoints_ = attachmentPoints.toMutableList()
        constraint = VSRopeConstraint(shipId0, shipId1, compliance, spoint1.toJomlVector3d(), spoint2.toJomlVector3d(), maxForce, minLength)
    }

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
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        cIDs.clear()

        val shipIds = mutableListOf(constraint.shipId0, constraint.shipId1)
        val localPoints = mutableListOf(
            listOf(constraint.localPos0),
            listOf(constraint.localPos1)
        )
        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)

        constraint = constraint.copy(shipIds[0], shipIds[1], constraint.compliance, localPoints[0][0], localPoints[1][0])

        cIDs.add(level.shipObjectWorld.createNewConstraint(constraint)!!)

        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], mID)
        renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(mID)
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, constraint, attachmentPoints_, renderer) {
                nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints, newRenderer ->
            val con = WinchMConstraint(localPos0, localPos1, nShip1Id, nShip2Id, constraint.compliance, constraint.maxForce, minLength, maxLength, extensionSpeed, channel, mode, newAttachmentPoints, newRenderer)
            con
        }
    }

    override fun onScaleBy(level: ServerLevel, scaleBy: Double) {
        minLength *= scaleBy
        extendedDist *= scaleBy
        addDist *= scaleBy

        constraint = constraint.copy(ropeLength = constraint.ropeLength * scaleBy)
        level.shipObjectWorld.removeConstraint(cIDs[0])
        cIDs[0] = level.shipObjectWorld.createNewConstraint(constraint)!!
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

        serializeRenderer(tag)

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(constraint) ?: return null)
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

        deserializeRenderer(tag)

        tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); constraint = (deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSRopeConstraint

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
        constraint = constraint.copy(ropeLength = minLength + extendedDist)
        cIDs[0] = shipObjectWorld.createNewConstraint(constraint) ?: return
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

        if (renderer != null) { SynchronisedRenderingData.serverSynchronisedData.addRenderer(constraint.shipId0, constraint.shipId1, mID, renderer!!)
        } else { renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(mID) }

        cIDs.add(level.shipObjectWorld.createNewConstraint(constraint) ?: clean(level) ?: return false)
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        wasDeleted = true
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        SynchronisedRenderingData.serverSynchronisedData.removeRenderer(mID)
    }
}