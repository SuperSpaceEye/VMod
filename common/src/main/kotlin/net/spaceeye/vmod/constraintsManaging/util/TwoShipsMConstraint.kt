package net.spaceeye.vmod.constraintsManaging.util

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.core.apigame.constraints.VSForceConstraint
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId

abstract class TwoShipsMConstraint(override val typeName: String): MConstraint {
    override var mID: ManagedConstraintId = -1 //should be saved to/loaded from tag
    override var __saveCounter: Int = -1

    abstract val mainConstraint: VSConstraint

    val cIDs = mutableListOf<ConstraintId>() // should be used to store VS ids
    var attachmentPoints_ = mutableListOf<BlockPos>()

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(mainConstraint.shipId0)
        val ship2Exists = allShips.contains(mainConstraint.shipId1)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(mainConstraint.shipId1))
                || (ship2Exists && dimensionIds.contains(mainConstraint.shipId0))
    }

    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(mainConstraint.shipId0)) {toReturn.add(mainConstraint.shipId0)}
        if (!dimensionIds.contains(mainConstraint.shipId1)) {toReturn.add(mainConstraint.shipId1)}

        return toReturn
    }

    override fun getVSIds(): Set<VSConstraintId> = cIDs.toSet()

    override fun getAttachmentPositions(): List<BlockPos> = attachmentPoints_

    override fun getAttachmentPoints(): List<Vector3d> = when (mainConstraint) {
        is VSForceConstraint -> listOf(
            Vector3d((mainConstraint as VSForceConstraint).localPos0),
            Vector3d((mainConstraint as VSForceConstraint).localPos1))
        else -> listOf()
    }

    protected fun <T> clean(level: ServerLevel): T? {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        return null
    }
}