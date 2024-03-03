package net.spaceeye.vsource.constraintsManaging.types

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsManaging.ManagedConstraintId
import net.spaceeye.vsource.constraintsManaging.VSConstraintDeserializationUtil.deserializeConstraint
import net.spaceeye.vsource.constraintsManaging.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vsource.constraintsManaging.VSConstraintSerializationUtil
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.rendering.types.BaseRenderer
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.utils.posWorldToShip
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSSphericalSwingLimitsConstraint
import org.valkyrienskies.core.apigame.constraints.VSSphericalTwistLimitsConstraint
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId

class WeldMConstraint(): MConstraint {
    lateinit var aconstraint1: VSAttachmentConstraint
    lateinit var aconstraint2: VSAttachmentConstraint
    lateinit var rconstraint1: VSSphericalTwistLimitsConstraint
    lateinit var rconstraint2: VSSphericalSwingLimitsConstraint

    val cIDs = mutableListOf<ConstraintId>()

    var renderer: BaseRenderer? = null

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
        fixedLength: Double = -1.0,
        renderer: BaseRenderer?
    ): this() {
        aconstraint1 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, if (fixedLength < 0) (rpoint1 - rpoint2).dist() else fixedLength)

        val dist1 = rpoint1 - rpoint2
        val dir = dist1.snormalize()

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
            maxForce, if (fixedLength < 0) (rpoint1 - rpoint2).dist() else fixedLength + addDist
        )

        val rot1 = ship1?.transform?.shipToWorldRotation ?: Quaterniond()
        val rot2 = ship2?.transform?.shipToWorldRotation ?: Quaterniond()

        rconstraint1 = VSSphericalTwistLimitsConstraint(shipId0, shipId1, 1e-10, rot2, rot1, 1e200, 0.0, 0.01)
        rconstraint2 = VSSphericalSwingLimitsConstraint(shipId0, shipId1, 1e-10, rot2, rot1, 1e200, 0.0, 0.01)

        this.renderer = renderer
    }

    override lateinit var mID: ManagedConstraintId
    override val typeName: String get() = "WeldMConstraint"

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

    override fun nbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(aconstraint1) ?: return null)
        tag.put("c2", VSConstraintSerializationUtil.serializeConstraint(aconstraint2) ?: return null)
        tag.put("c3", VSConstraintSerializationUtil.serializeConstraint(rconstraint1) ?: return null)
        tag.put("c4", VSConstraintSerializationUtil.serializeConstraint(rconstraint2) ?: return null)
        tag.putInt("managedID", mID.id)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); aconstraint1 = (deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c2"] as CompoundTag, lastDimensionIds); aconstraint2 = (deserializeConstraint(tag["c2"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c3"] as CompoundTag, lastDimensionIds); rconstraint1 = (deserializeConstraint(tag["c3"] as CompoundTag) ?: return null) as VSSphericalTwistLimitsConstraint
        tryConvertDimensionId(tag["c4"] as CompoundTag, lastDimensionIds); rconstraint2 = (deserializeConstraint(tag["c4"] as CompoundTag) ?: return null) as VSSphericalSwingLimitsConstraint

        mID = ManagedConstraintId(if (tag.contains("managedID")) tag.getInt("managedID") else -1)
        return this
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint1) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint2) ?: return false)

        if (renderer != null) { SynchronisedRenderingData.serverSynchronisedData.addRenderer(aconstraint1.shipId0, aconstraint1.shipId1, mID.id, renderer!!) }
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        SynchronisedRenderingData.serverSynchronisedData.removeRenderer(mID.id)
    }
}