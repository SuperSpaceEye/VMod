package net.spaceeye.vmod.constraintsManaging.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.utils.*
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.mod.common.shipObjectWorld

abstract class NewTwoShipsMConstraint(): ExtendableMConstraint() {
    val cIDs = mutableListOf<VSJointId>() // should be used to store VS ids
    var attachmentPoints_ = mutableListOf<BlockPos>()

    abstract var shipId1: Long
    abstract var shipId2: Long
    abstract var sPos1: Vector3d
    abstract var sPos2: Vector3d

    override fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(shipId1)
        val ship2Exists = allShips.contains(shipId2)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(shipId1))
                || (ship2Exists && dimensionIds.contains(shipId2))
    }

    override fun iAttachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(shipId1)) {toReturn.add(shipId1)}
        if (!dimensionIds.contains(shipId2)) {toReturn.add(shipId2)}

        return toReturn
    }

    override fun iGetVSIds(): Set<VSJointId> = cIDs.toSet()
    override fun iGetAttachmentPositions(shipId: ShipId): List<BlockPos> = if (shipId == -1L) attachmentPoints_ else {
        when (shipId) {
            shipId1 -> listOf(attachmentPoints_[0])
            shipId2 -> listOf(attachmentPoints_[1])
            else -> listOf()
        }
    }
    override fun iGetAttachmentPoints(shipId: ShipId): List<Vector3d> =
        if (shipId == -1L) listOf(
            sPos1.copy(),
            sPos2.copy()
        ) else {
            when(shipId) {
                shipId1 -> listOf(sPos1.copy())
                shipId2 -> listOf(sPos2.copy())
            else -> listOf()
            }
        }

    @NonExtendable
    override fun nbtSerialize(): CompoundTag? {
        val tag = super.nbtSerialize() ?: return null
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))
        tag.putMyVector3d("sPos1", sPos1)
        tag.putMyVector3d("sPos2", sPos2)
        tag.putLong("shipId1", shipId1)
        tag.putLong("shipId2", shipId2)
        return tag
    }

    @NonExtendable
    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)
        sPos1 = tag.getMyVector3d("sPos1")
        sPos2 = tag.getMyVector3d("sPos2")
        shipId1 = tag.getLong("shipId1")
        shipId2 = tag.getLong("shipId2")

        val map = ServerLevelHolder.shipObjectWorld!!.dimensionToGroundBodyIdImmutable
        shipId1 = map[lastDimensionIds[shipId1]] ?: shipId1
        shipId2 = map[lastDimensionIds[shipId2]] ?: shipId2

        return super.nbtDeserialize(tag, lastDimensionIds)
    }

    /**
     * IF YOU'RE OVERRIDING THIS REMEMBER TO CALL SUPER METHOD
     */
    override fun iOnDeleteMConstraint(level: ServerLevel) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
    }
}