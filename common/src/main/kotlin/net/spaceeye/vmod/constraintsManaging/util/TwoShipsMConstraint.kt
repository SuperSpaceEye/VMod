package net.spaceeye.vmod.constraintsManaging.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.deserializeBlockPositions
import net.spaceeye.vmod.utils.serializeBlockPositions
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.mod.common.shipObjectWorld

abstract class TwoShipsMConstraint(): ExtendableMConstraint() {
    abstract val mainConstraint: VSJoint

    val cIDs = mutableListOf<VSJointId>() // should be used to store VS ids
    var attachmentPoints_ = mutableListOf<BlockPos>()

    override fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(mainConstraint.shipId0!!)
        val ship2Exists = allShips.contains(mainConstraint.shipId1!!)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(mainConstraint.shipId1))
                || (ship2Exists && dimensionIds.contains(mainConstraint.shipId0))
    }

    override fun iAttachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        //TODO if mainConstraint.shipId is null, then it should be connected to world, but it can also be connected to world
        // through dimensionToGroundBodyIdImmutable
        if (!dimensionIds.contains(mainConstraint.shipId0)) {toReturn.add(mainConstraint.shipId0!!)}
        if (!dimensionIds.contains(mainConstraint.shipId1)) {toReturn.add(mainConstraint.shipId1!!)}

        return toReturn
    }

    override fun iGetVSIds(): Set<VSJointId> = cIDs.toSet()
    override fun iGetAttachmentPositions(shipId: ShipId): List<BlockPos> = if (shipId == -1L) attachmentPoints_ else {
        when (shipId) {
            mainConstraint.shipId0 -> listOf(attachmentPoints_[0])
            mainConstraint.shipId1 -> listOf(attachmentPoints_[1])
            else -> listOf()
        }
    }
    override fun iGetAttachmentPoints(shipId: ShipId): List<Vector3d> =
        if (shipId == -1L) listOf(
            Vector3d(mainConstraint.pose0.pos),
            Vector3d(mainConstraint.pose1.pos)
        ) else {
            when(shipId) {
            mainConstraint.shipId0 -> listOf(Vector3d(mainConstraint.pose0.pos))
            mainConstraint.shipId1 -> listOf(Vector3d(mainConstraint.pose1.pos))
            else -> listOf()
            }
        }

    @NonExtendable
    override fun nbtSerialize(): CompoundTag? {
        val saveTag = super.nbtSerialize() ?: return null
        saveTag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))
        return saveTag
    }

    @NonExtendable
    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)
        return super.nbtDeserialize(tag, lastDimensionIds)
    }

    /**
     * IF YOU'RE OVERRIDING THIS REMEMBER TO CALL SUPER METHOD
     */
    override fun iOnDeleteMConstraint(level: ServerLevel) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
    }
}