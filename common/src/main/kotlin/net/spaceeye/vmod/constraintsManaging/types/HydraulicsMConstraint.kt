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
    lateinit var aconstraint1: VSAttachmentConstraint
    lateinit var aconstraint2: VSAttachmentConstraint
    lateinit var rconstraint1: VSTorqueConstraint

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
    override val typeName: String get() = "HydraulicsMConstraint"
    override var saveCounter: Int = -1

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
            listOf(aconstraint1.localPos0, aconstraint2.localPos0),
            listOf(aconstraint1.localPos1, aconstraint2.localPos1)
        )
        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)

        aconstraint1 = VSAttachmentConstraint(shipIds[0], shipIds[1], aconstraint1.compliance, localPoints[0][0], localPoints[1][0], aconstraint1.maxForce, aconstraint1.fixedDistance)
        aconstraint2 = VSAttachmentConstraint(shipIds[0], shipIds[1], aconstraint2.compliance, localPoints[0][1], localPoints[1][1], aconstraint2.maxForce, aconstraint2.fixedDistance)
        rconstraint1 = VSFixedOrientationConstraint(shipIds[0], shipIds[1], rconstraint1.compliance, rconstraint1.localRot0, rconstraint1.localRot1, rconstraint1.maxTorque)

        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1)!!)
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2)!!)
        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint1)!!)

        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], mID)

        renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(mID)
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, aconstraint1, attachmentPoints_, renderer) {
            nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints, newRenderer ->

            val rpoint1 = if (nShip1 != null) { posShipToWorld(nShip1, localPos0) } else localPos0
            val rpoint2 = if (nShip2 != null) { posShipToWorld(nShip2, localPos1) } else localPos1

            val con = HydraulicsMConstraint(localPos0, localPos1, rpoint1, rpoint2, nShip1, nShip2, nShip1Id, nShip2Id, aconstraint1.compliance, aconstraint1.maxForce, minLength, maxLength, extensionSpeed * 20, channel, mode, newAttachmentPoints, newRenderer)
            con.addDist = addDist
            con.minLength = minLength
            con.maxLength = maxLength
            con.extendedDist = extendedDist
            con.extensionSpeed = extensionSpeed
            con.aconstraint1 = VSAttachmentConstraint(con.aconstraint1.shipId0, con.aconstraint1.shipId1, con.aconstraint1.compliance, con.aconstraint1.localPos0, con.aconstraint1.localPos1, con.aconstraint1.maxForce, con.minLength + con.extendedDist)
            con.aconstraint2 = VSAttachmentConstraint(con.aconstraint2.shipId0, con.aconstraint2.shipId1, con.aconstraint2.compliance, con.aconstraint2.localPos0, con.aconstraint2.localPos1, con.aconstraint2.maxForce, con.minLength + con.addDist + con.extendedDist)

            con
        }
    }

    override fun onScaleBy(level: ServerLevel, scaleBy: Double) {
        minLength *= scaleBy
        extendedDist *= scaleBy
        addDist *= scaleBy

        aconstraint1 = VSAttachmentConstraint(aconstraint1.shipId0, aconstraint1.shipId1, aconstraint1.compliance, aconstraint1.localPos0, aconstraint1.localPos1, aconstraint1.maxForce, minLength + extendedDist)
        aconstraint2 = VSAttachmentConstraint(aconstraint2.shipId0, aconstraint2.shipId1, aconstraint2.compliance, aconstraint2.localPos0, aconstraint2.localPos1, aconstraint2.maxForce, minLength + addDist + extendedDist)

        level.shipObjectWorld.removeConstraint(cIDs[0])
        level.shipObjectWorld.removeConstraint(cIDs[1])

        cIDs[0] = level.shipObjectWorld.createNewConstraint(aconstraint1)!!
        cIDs[1] = level.shipObjectWorld.createNewConstraint(aconstraint2)!!
    }

    override fun getVSIds(): Set<VSConstraintId> {
        return cIDs.toSet()
    }

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

        minLength: Double,
        maxLength: Double,
        extensionSpeed: Double,

        channel: String,

        messageModes: MessageModes,

        attachmentPoints: List<BlockPos>,

        renderer: BaseRenderer?): this() {
        aconstraint1 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, minLength)

        val dist1 = rpoint1 - rpoint2
        val len = dist1.dist()
        val dir = dist1.normalize() * ( if (len < 10 || len > 30) 20 else 40)

        val srpoint1 = rpoint1 + dir
        val srpoint2 = rpoint2 - dir

        val dist2 = srpoint1 - srpoint2
        addDist = dist2.dist() - dist1.dist()

        val sspoint1 = if (ship1 != null) posWorldToShip(ship1, srpoint1) else Vector3d(srpoint1)
        val sspoint2 = if (ship2 != null) posWorldToShip(ship2, srpoint2) else Vector3d(srpoint2)

        aconstraint2 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            sspoint1.toJomlVector3d(), sspoint2.toJomlVector3d(),
            maxForce, (minLength + addDist)
        )

        val rot1 = ship1?.transform?.shipToWorldRotation ?: Quaterniond()
        val rot2 = ship2?.transform?.shipToWorldRotation ?: Quaterniond()

        rconstraint1 = VSFixedOrientationConstraint(shipId0, shipId1, compliance, rot1.invert(Quaterniond()), rot2.invert(Quaterniond()), 1e300)

        this.renderer = renderer
        this.minLength = minLength
        this.maxLength = maxLength
        // extensionSpeed is in seconds. Constraint is being updated every mc tick
        this.extensionSpeed = extensionSpeed / 20.0

        this.channel = channel
        this.mode = messageModes

        attachmentPoints_ = attachmentPoints.toMutableList()
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(aconstraint1) ?: return null)
        tag.put("c2", VSConstraintSerializationUtil.serializeConstraint(aconstraint2) ?: return null)
        tag.put("c3", VSConstraintSerializationUtil.serializeConstraint(rconstraint1) ?: return null)

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

        tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); aconstraint1 = (deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c2"] as CompoundTag, lastDimensionIds); aconstraint2 = (deserializeConstraint(tag["c2"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c3"] as CompoundTag, lastDimensionIds); rconstraint1 = (deserializeConstraint(tag["c3"] as CompoundTag) ?: return null) as VSTorqueConstraint

        return this
    }

    var wasDeleted = false
    var fnToUse: (() -> Boolean)? = null
    var lastExtended: Double = 0.0
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

    private fun signalFn(): Boolean {
        if (numMessages != 0) {
            targetPercentage = totalPercentage / numMessages
            numMessages = 0
            totalPercentage = 0.0
        }

        val currentPercentage = extendedDist / maxLength
        if (abs(currentPercentage - targetPercentage) < 1e-6) { return false }
        val left = targetPercentage - currentPercentage
        val percentageStep = extensionSpeed / maxLength
        extendedDist += min(percentageStep, abs(left)) * maxLength * left.sign
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

    var targetPercentage = 0.0

    var totalPercentage = 0.0
    var numMessages = 0

    private fun signalTick(msg: Message) {
        if (msg !is Signal) { return }

        totalPercentage = min(max(msg.percentage, 0.0), 1.0)
        numMessages++

        fnToUse = ::signalFn
    }

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
        aconstraint1 = VSAttachmentConstraint(
            aconstraint1.shipId0,
            aconstraint1.shipId1,
            aconstraint1.compliance,
            aconstraint1.localPos0,
            aconstraint1.localPos1,
            aconstraint1.maxForce,
            minLength + extendedDist
        )
        cIDs[0] = shipObjectWorld.createNewConstraint(aconstraint1) ?: return

        if (!shipObjectWorld.removeConstraint(cIDs[1])) {return}
        aconstraint2 = VSAttachmentConstraint(
            aconstraint2.shipId0,
            aconstraint2.shipId1,
            aconstraint2.compliance,
            aconstraint2.localPos0,
            aconstraint2.localPos1,
            aconstraint2.maxForce,
            minLength + addDist + extendedDist
        )
        cIDs[1] = shipObjectWorld.createNewConstraint(aconstraint2) ?: return
    }

    private fun <T> clean(level: ServerLevel): T? {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        return null
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1) ?: clean(level) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2) ?: clean(level) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint1) ?: clean(level) ?: return false)

        MessagingNetwork.register(channel) {
            msg, unregister ->
            if (wasDeleted) {unregister(); return@register}

            when (mode) {
                MessageModes.Toggle -> toggleTick(msg)
                MessageModes.Signal -> signalTick(msg)
            }
        }

        if (renderer != null) { SynchronisedRenderingData.serverSynchronisedData.addRenderer(aconstraint1.shipId0, aconstraint1.shipId1, mID, renderer!!)
        } else { renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(mID) }
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        wasDeleted = true
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        SynchronisedRenderingData.serverSynchronisedData.removeRenderer(mID)
    }
}