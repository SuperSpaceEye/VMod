package net.spaceeye.vmod.constraintsManaging.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.utils.Vector3d
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId

abstract class AbstractMConstraint: MConstraint {
    abstract fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean
    // SHOULDN'T RETURN GROUND SHIPID
    abstract fun iAttachedToShips(dimensionIds: Collection<ShipId>): List<ShipId>

    // positions to which constraint is "attached" to the ship/world
    // is needed for strip tool, moving constraints on ship splitting
    abstract fun iGetAttachmentPositions(): List<BlockPos>

    abstract fun iGetAttachmentPoints(): List<Vector3d>

    // is called on ship splitting
    abstract fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId)

    abstract fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint?

    abstract fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d)
    abstract fun iGetVSIds(): Set<VSConstraintId>

    abstract fun iNbtSerialize(): CompoundTag?
    abstract fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint?

    @Internal abstract fun iOnMakeMConstraint(level: ServerLevel): Boolean
    @Internal abstract fun iOnDeleteMConstraint(level: ServerLevel)


    final override var mID: ManagedConstraintId = -1
    //DO NOT TOUCH IT
    @set:Internal
    @get:Internal
    final override var __saveCounter: Int = -1

    val extensions = mutableListOf<MConstraintExtension>()

    fun addExtension(extension: MConstraintExtension) {
        extension.onInit(this)
        extensions.add(extension)
    }

    inline fun <reified T: MConstraintExtension> getExtension(type: Class<T>): List<T> {
        return extensions.filterIsInstance<T>()
    }


    final override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        return iStillExists(allShips, dimensionIds)
    }

    final override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        return iAttachedToShips(dimensionIds)
    }

    final override fun getAttachmentPositions(): List<BlockPos> {
        return iGetAttachmentPositions()
    }

    final override fun getAttachmentPoints(): List<Vector3d> {
        return iGetAttachmentPoints()
    }

    final override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        TODO()
        iMoveShipyardPosition(level, previous, new, newShipId)
        extensions.forEach { it.onAfterMoveShipyardPositions(level, previous, new, newShipId) }
    }

    final override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        extensions.forEach { it.onBeforeCopyMConstraint(level, mapped) }
        val new = iCopyMConstraint(level, mapped)
        new?.let { extensions.forEach { it.onAfterCopyMConstraint(level, mapped, new as AbstractMConstraint) } }
        return new
    }

    final override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        return iOnScaleBy(level, scaleBy, scalingCenter)
    }

    final override  fun getVSIds(): Set<VSConstraintId> {
        return iGetVSIds()
    }

    final override fun nbtSerialize(): CompoundTag? {
        val saveTag = CompoundTag()

        val mainTag = iNbtSerialize() ?: return null
        saveTag.put("Main", mainTag)

        val extensionsTag = CompoundTag()
        extensions.forEach {
            extensionsTag.put(it.typeName, it.onSerialize() ?: return@forEach)
        }

        saveTag.put("Extensions", extensionsTag)


        return saveTag
    }

    final override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        val mainTag = tag.getCompound("Main")
        val mc = iNbtDeserialize(mainTag, lastDimensionIds)
        extensions.clear()

        val extensionsTag = tag.getCompound("Extensions")
        extensions.addAll(extensionsTag.allKeys.mapNotNull {type ->
            val ext = MConstraintExtension.fromType(type)
            val success = ext.onDeserialize(extensionsTag.getCompound(type), lastDimensionIds)
            if (!success) return@mapNotNull null
            ext.onInit(this)
            ext
        })

        return mc
    }

    @Internal
    final override fun onMakeMConstraint(level: ServerLevel): Boolean {
        extensions.forEach { it.onMakeMConstraint(level) }
        return iOnMakeMConstraint(level)
    }

    @Internal
    final override fun onDeleteMConstraint(level: ServerLevel) {
        extensions.forEach { it.onDeleteMConstraint(level) }
        return iOnDeleteMConstraint(level)
    }
}