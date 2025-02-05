package net.spaceeye.vmod.vEntityManaging.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.VEntityId
import net.spaceeye.vmod.vEntityManaging.util.VEntityExtension.Companion.toType
import net.spaceeye.vmod.utils.Vector3d
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId

interface ExtendableVEntityIMethods {
    fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean
    // SHOULDN'T RETURN GROUND SHIPID
    fun iAttachedToShips(dimensionIds: Collection<ShipId>): List<ShipId>

    // positions to which VEntity is "attached" to the ship/world
    // is needed for strip tool, moving VEntities on ship splitting
    fun iGetAttachmentPositions(shipId: Long): List<BlockPos>

    fun iGetAttachmentPoints(shipId: Long): List<Vector3d>

    // is called on ship splitting
    fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {TODO()}

    fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>): VEntity?

    fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d)

    fun iNbtSerialize(): CompoundTag?
    fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): VEntity?

    @Internal fun iOnMakeVEntity(level: ServerLevel): Boolean
    @Internal fun iOnDeleteVEntity(level: ServerLevel)
}

abstract class ExtendableVEntity(): VEntity, ExtendableVEntityIMethods {
    final override var mID: VEntityId = -1

    private val _extensions = mutableSetOf<VEntityExtension>()
    open val extensions: Collection<VEntityExtension> get() = _extensions

    open fun addExtension(extension: VEntityExtension): ExtendableVEntity {
        extension.onInit(this)
        _extensions.add(extension)
        return this
    }

    inline fun <reified T: VEntityExtension> getExtensionsOfType(): Collection<T> {
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

    final override fun copyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>): VEntity? {
        _extensions.forEach { it.onBeforeCopyVEntity(level, mapped) }
        val new = iCopyVEntity(level, mapped)
        new?.let { _extensions.forEach { it.onAfterCopyVEntity(level, mapped, new as ExtendableVEntity) } }
        return new
    }

    final override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        _extensions.forEach { it.onBeforeOnScaleByVEntity(level, scaleBy, scalingCenter) }
        return iOnScaleBy(level, scaleBy, scalingCenter)
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
    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): VEntity? {
        mID = tag.getInt("mID")

        val mainTag = tag.getCompound("Main")
        val mc = iNbtDeserialize(mainTag, lastDimensionIds)
        _extensions.clear()

        val extensionsTag = tag.getCompound("Extensions")
        _extensions.addAll(extensionsTag.allKeys.mapNotNull { type ->
            val ext = VEntityExtension.fromType(type)
            val success = ext.onDeserialize(extensionsTag.getCompound(type), lastDimensionIds)
            if (!success) return@mapNotNull null
            ext.onInit(this)
            ext
        })

        return mc
    }

    @Internal
    final override fun onMakeVEntity(level: ServerLevel): Boolean {
        _extensions.forEach { it.onMakeVEntity(level) }
        return iOnMakeVEntity(level)
    }

    @Internal
    final override fun onDeleteVEntity(level: ServerLevel) {
        _extensions.forEach { it.onDeleteVEntity(level) }
        return iOnDeleteVEntity(level)
    }
}