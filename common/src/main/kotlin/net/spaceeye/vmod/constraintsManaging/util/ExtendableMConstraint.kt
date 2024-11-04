package net.spaceeye.vmod.constraintsManaging.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.util.MConstraintExtension.Companion.toType
import net.spaceeye.vmod.utils.Vector3d
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId

abstract class ExtendableMConstraint(): MConstraint {
    abstract fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean
    // SHOULDN'T RETURN GROUND SHIPID
    abstract fun iAttachedToShips(dimensionIds: Collection<ShipId>): List<ShipId>

    // positions to which constraint is "attached" to the ship/world
    // is needed for strip tool, moving constraints on ship splitting
    abstract fun iGetAttachmentPositions(shipId: Long): List<BlockPos>

    abstract fun iGetAttachmentPoints(shipId: Long): List<Vector3d>

    // is called on ship splitting
    open fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {TODO()}

    abstract fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint?

    abstract fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d)
    abstract fun iGetVSIds(): Set<VSConstraintId>

    abstract fun iNbtSerialize(): CompoundTag?
    abstract fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint?

    @Internal abstract fun iOnMakeMConstraint(level: ServerLevel): Boolean
    @Internal abstract fun iOnDeleteMConstraint(level: ServerLevel)


    final override var mID: ManagedConstraintId = -1
    /**
     * DO NOT TOUCH IT
     */
    @set:Internal
    @get:Internal
    final override var __saveCounter: Int = -1

    private val _extensions = mutableSetOf<MConstraintExtension>()
    open val extensions: Collection<MConstraintExtension> get() = _extensions

    open fun addExtension(extension: MConstraintExtension): ExtendableMConstraint {
        extension.onInit(this)
        _extensions.add(extension)
        return this
    }

    inline fun <reified T: MConstraintExtension> getExtensionsOfType(): Collection<T> {
        return extensions.filterIsInstance<T>()
    }


    final override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        return iStillExists(allShips, dimensionIds)
    }

    final override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        return iAttachedToShips(dimensionIds)
    }

    final override fun getAttachmentPositions(shipId: Long): List<BlockPos> {
        return iGetAttachmentPositions(shipId)
    }

    final override fun getAttachmentPoints(shipId: Long): List<Vector3d> {
        return iGetAttachmentPoints(shipId)
    }

    final override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        TODO()
        iMoveShipyardPosition(level, previous, new, newShipId)
        _extensions.forEach { it.onAfterMoveShipyardPositions(level, previous, new, newShipId) }
    }

    final override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        _extensions.forEach { it.onBeforeCopyMConstraint(level, mapped) }
        val new = iCopyMConstraint(level, mapped)
        new?.let { _extensions.forEach { it.onAfterCopyMConstraint(level, mapped, new as ExtendableMConstraint) } }
        return new
    }

    final override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        _extensions.forEach { it.onBeforeOnScaleByMConstraint(level, scaleBy, scalingCenter) }
        return iOnScaleBy(level, scaleBy, scalingCenter)
    }

    final override fun getVSIds(): Set<VSConstraintId> {
        return iGetVSIds()
    }

    @NonExtendable
    override fun nbtSerialize(): CompoundTag? {
        val saveTag = CompoundTag()

        saveTag.putInt("mID", mID)

        val mainTag = iNbtSerialize() ?: return null
        saveTag.put("Main", mainTag)

        val extensionsTag = CompoundTag()
        _extensions.forEach {
            extensionsTag.put(it.toType(), it.onSerialize() ?: return@forEach)
        }

        saveTag.put("Extensions", extensionsTag)


        return saveTag
    }

    @NonExtendable
    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("mID")

        val mainTag = tag.getCompound("Main")
        val mc = iNbtDeserialize(mainTag, lastDimensionIds)
        _extensions.clear()

        val extensionsTag = tag.getCompound("Extensions")
        _extensions.addAll(extensionsTag.allKeys.mapNotNull { type ->
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
        _extensions.forEach { it.onMakeMConstraint(level) }
        return iOnMakeMConstraint(level)
    }

    @Internal
    final override fun onDeleteMConstraint(level: ServerLevel) {
        _extensions.forEach { it.onDeleteMConstraint(level) }
        return iOnDeleteMConstraint(level)
    }
}