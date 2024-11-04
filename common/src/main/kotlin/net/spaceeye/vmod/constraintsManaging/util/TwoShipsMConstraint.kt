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
import org.valkyrienskies.core.apigame.constraints.VSConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.core.apigame.constraints.VSForceConstraint
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId

abstract class TwoShipsMConstraint(): ExtendableMConstraint() {
    abstract val mainConstraint: VSConstraint

    val cIDs = mutableListOf<ConstraintId>() // should be used to store VS ids
    var attachmentPoints_ = mutableListOf<BlockPos>()

    override fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(mainConstraint.shipId0)
        val ship2Exists = allShips.contains(mainConstraint.shipId1)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(mainConstraint.shipId1))
                || (ship2Exists && dimensionIds.contains(mainConstraint.shipId0))
    }

    override fun iAttachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(mainConstraint.shipId0)) {toReturn.add(mainConstraint.shipId0)}
        if (!dimensionIds.contains(mainConstraint.shipId1)) {toReturn.add(mainConstraint.shipId1)}

        return toReturn
    }

    override fun iGetVSIds(): Set<VSConstraintId> = cIDs.toSet()
    override fun iGetAttachmentPositions(shipId: ShipId): List<BlockPos> = if (shipId == -1L) attachmentPoints_ else {
        when (shipId) {
            mainConstraint.shipId0 -> listOf(attachmentPoints_[0])
            mainConstraint.shipId1 -> listOf(attachmentPoints_[1])
            else -> listOf()
        }
    }
    override fun iGetAttachmentPoints(shipId: ShipId): List<Vector3d> = when (mainConstraint) {
        is VSForceConstraint -> if (shipId == -1L) listOf(
            Vector3d((mainConstraint as VSForceConstraint).localPos0),
            Vector3d((mainConstraint as VSForceConstraint).localPos1))
        else when(shipId) {
            mainConstraint.shipId0 -> listOf(Vector3d((mainConstraint as VSForceConstraint).localPos0))
            mainConstraint.shipId1 -> listOf(Vector3d((mainConstraint as VSForceConstraint).localPos1))
            else -> listOf()
        }
        else -> listOf()
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