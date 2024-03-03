package net.spaceeye.vsource.constraintsManaging.types

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.WLOG
import net.spaceeye.vsource.constraintsManaging.ConstraintManager
import net.spaceeye.vsource.constraintsManaging.ManagedConstraintId
import net.spaceeye.vsource.constraintsManaging.VSConstraintDeserializationUtil.deserializeConstraint
import net.spaceeye.vsource.constraintsManaging.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vsource.constraintsManaging.VSConstraintSerializationUtil
import net.spaceeye.vsource.network.Activate
import net.spaceeye.vsource.network.Deactivate
import net.spaceeye.vsource.network.MessagingNetwork
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.rendering.types.BaseRenderer
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.utils.posWorldToShip
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId
import kotlin.math.sin

private const val pi2 = Math.PI / 2

//TODO Rename muscle constraint to hydraulics
class MuscleMConstraint(): MConstraint, Tickable {
    lateinit var aconstraint1: VSAttachmentConstraint
    lateinit var aconstraint2: VSAttachmentConstraint
    lateinit var rconstraint1: VSConstraint

    val cIDs = mutableListOf<ConstraintId>()

    var minLength: Double = -1.0
    var maxLength: Double = -1.0
    var extendedPercent: Double = 0.0 //between 0 and 1

    var ticksToWork = 20
    var currentTick = 0

    var addDist: Double = 0.0

    var renderer: BaseRenderer? = null

    override lateinit var mID: ManagedConstraintId
    override val typeName: String get() = "MuscleMConstraint"
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
        ticksToWork: Int,

        renderer: BaseRenderer?,
    ): this() {
        aconstraint1 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, minLength)

        val dist1 = rpoint1 - rpoint2
        val dir = dist1.normalize()

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
            maxForce, minLength + addDist
        )

        val rot2 = ship1?.transform?.shipToWorldRotation ?: Quaterniond()
        val rot1 = ship2?.transform?.shipToWorldRotation ?: Quaterniond()

        rconstraint1 = VSFixedOrientationConstraint(shipId0, shipId1, compliance, rot1, rot2, 1e300)

        this.renderer = renderer
        this.minLength = minLength
        this.maxLength = maxLength
        this.ticksToWork = ticksToWork
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(aconstraint1) ?: return null)
        tag.put("c2", VSConstraintSerializationUtil.serializeConstraint(aconstraint2) ?: return null)
        tag.put("c3", VSConstraintSerializationUtil.serializeConstraint(rconstraint1) ?: return null)

        tag.putInt("managedID", mID.id)

        tag.putDouble("extendedPercent", extendedPercent)
        tag.putDouble("addDist", addDist)
        tag.putDouble("minDistance", minLength)
        tag.putDouble("maxDistance", maxLength)
        tag.putInt("ticksToWork", ticksToWork)
        tag.putInt("currentTick", currentTick)
        tag.putBoolean("isActivating", fnToUse == ::activatingFn)
        tag.putBoolean("isDeactivating", fnToUse == ::deactivatingFn)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); aconstraint1 = (deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c2"] as CompoundTag, lastDimensionIds); aconstraint2 = (deserializeConstraint(tag["c2"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c3"] as CompoundTag, lastDimensionIds); rconstraint1 = (deserializeConstraint(tag["c3"] as CompoundTag) ?: return null) as VSSphericalTwistLimitsConstraint

        mID = ManagedConstraintId(if (tag.contains("managedID")) tag.getInt("managedID") else -1)

        extendedPercent = tag.getDouble("extendedPercent")
        addDist = tag.getDouble("addDist")
        minLength = tag.getDouble("minDistance")
        maxLength = tag.getDouble("maxDistance")
        ticksToWork = tag.getInt("ticksToWork")
        currentTick = tag.getInt("currentTick")

        fnToUse = when {
            tag.getBoolean("isActivating") -> ::activatingFn
            tag.getBoolean("isDeactivating") -> ::deactivatingFn
            else -> null
        }

        return this
    }

    var wasDeleted = false
    var fnToUse: (() -> Boolean)? = null
    var lastExtended: Double = 0.0

    private fun activatingFn(): Boolean {
        if (currentTick >= ticksToWork) { return false }
        extendedPercent = sin(++currentTick / ticksToWork.toDouble() * pi2)
        return true
    }

    private fun deactivatingFn(): Boolean {
        if (currentTick <= 0) {return false}
        extendedPercent = sin(--currentTick / ticksToWork.toDouble() * pi2)
        return true
    }

    override fun tick(server: MinecraftServer, unregister: () -> Unit) {
        if (fnToUse != null) { if (!fnToUse!!()) {fnToUse = null} }

        if (lastExtended == extendedPercent) {return}
        lastExtended = extendedPercent

        val shipObjectWorld = server.shipObjectWorld

        shipObjectWorld.removeConstraint(cIDs[0])
        cIDs[0] = shipObjectWorld.createNewConstraint(VSAttachmentConstraint(
            aconstraint1.shipId0,
            aconstraint1.shipId1,
            aconstraint1.compliance,
            aconstraint1.localPos0,
            aconstraint1.localPos1,
            aconstraint1.maxForce,
            minLength + (maxLength - minLength) * extendedPercent
        )) ?: return

        shipObjectWorld.removeConstraint(cIDs[1])
        cIDs[1] = shipObjectWorld.createNewConstraint(VSAttachmentConstraint(
            aconstraint2.shipId0,
            aconstraint2.shipId1,
            aconstraint2.compliance,
            aconstraint2.localPos0,
            aconstraint2.localPos1,
            aconstraint2.maxForce,
            minLength + addDist + (maxLength - minLength) * extendedPercent
        )) ?: return
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint1) ?: return false)

        MessagingNetwork.register("muscle") {
            msg, unregister ->
            if (wasDeleted) {unregister(); return@register}
            if (fnToUse != null) {return@register}
            when (msg) {
                is Activate -> { fnToUse = ::activatingFn }
                is Deactivate -> { fnToUse = ::deactivatingFn }
            }
        }

        if (renderer != null) { SynchronisedRenderingData.serverSynchronisedData.addRenderer(aconstraint1.shipId0, aconstraint1.shipId1, mID.id, renderer!!) }
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        wasDeleted = true
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        SynchronisedRenderingData.serverSynchronisedData.removeRenderer(mID.id)
    }
}