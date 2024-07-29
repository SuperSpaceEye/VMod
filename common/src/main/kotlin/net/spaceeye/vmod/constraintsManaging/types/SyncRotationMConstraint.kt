package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint
import org.valkyrienskies.core.apigame.constraints.VSTorqueConstraint
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId

class SyncRotationMConstraint(): MConstraint {
    override var mID: ManagedConstraintId = -1
    override var saveCounter: Int = -1

    override val typeName: String = "SyncRotationMConstraint"

    lateinit var rotConstraint: VSTorqueConstraint
    val cIDs = mutableListOf<ConstraintId>()

    constructor(
        shipId1: ShipId,
        shipId2: ShipId,

        srot1: Quaterniondc,
        srot2: Quaterniondc,

        compliance: Double,
        maxForce: Double
    ): this() {
        rotConstraint = VSFixedOrientationConstraint(shipId1, shipId2, compliance,
            srot1.invert(Quaterniond()),
            srot2.invert(Quaterniond()),
            maxForce
            )
    }

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(rotConstraint.shipId0)
        val ship2Exists = allShips.contains(rotConstraint.shipId1)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(rotConstraint.shipId1))
                || (ship2Exists && dimensionIds.contains(rotConstraint.shipId0))
    }

    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(rotConstraint.shipId0)) {toReturn.add(rotConstraint.shipId0)}
        if (!dimensionIds.contains(rotConstraint.shipId1)) {toReturn.add(rotConstraint.shipId1)}

        return toReturn
    }

    override fun getAttachmentPoints(): List<BlockPos> = listOf()

    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        TODO("Not yet implemented")
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        if (!mapped.keys.contains(rotConstraint.shipId0) && !mapped.keys.contains(rotConstraint.shipId1)) return null

        return SyncRotationMConstraint(
            mapped[rotConstraint.shipId0]!!,
            mapped[rotConstraint.shipId1]!!,
            rotConstraint.localRot0.invert(Quaterniond()),
            rotConstraint.localRot1.invert(Quaterniond()),
            rotConstraint.compliance,
            rotConstraint.maxTorque
        )
    }

    override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}
    override fun getVSIds(): Set<VSConstraintId> = cIDs.toSet()

    override fun nbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putInt("mID", mID)

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(rotConstraint) ?: return null)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("mID")

        VSConstraintDeserializationUtil.tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); rotConstraint = (VSConstraintDeserializationUtil.deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSTorqueConstraint

        return this
    }

    private fun <T> clean(level: ServerLevel): T? {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        return null
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(rotConstraint) ?: clean(level) ?: return false)

        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
    }
}