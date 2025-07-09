package net.spaceeye.vmod.vEntityManaging.util

import com.fasterxml.jackson.annotation.JsonIgnore
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
import org.valkyrienskies.mod.common.dimensionId
import java.util.concurrent.CompletableFuture

interface ExtendableVEntityIMethods {
    fun iStillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean
    // SHOULDN'T RETURN GROUND SHIPID
    fun iAttachedToShips(dimensionIds: Collection<ShipId>): List<ShipId>

    // positions to which VEntity is "attached" to the ship/world
    // is needed for strip tool, moving VEntities on ship splitting
    fun iGetAttachmentPoints(shipId: Long): List<Vector3d>

    // is called on ship splitting
    fun iMoveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {TODO()}

    fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity?

    fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d)

    fun iNbtSerialize(): CompoundTag?
    fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): VEntity?

    @Internal fun iOnMakeVEntity(level: ServerLevel): List<CompletableFuture<Boolean>>
    @Internal fun iOnDeleteVEntity(level: ServerLevel)
}

abstract class ExtendableVEntity(): VEntity, ExtendableVEntityIMethods {
    final override var mID: VEntityId = -1
    final override var dimensionId: String? = null

    private val _extensions = mutableSetOf<VEntityExtension>()
    @get:JsonIgnore
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

    final override fun getAttachmentPoints(shipId: Long): List<Vector3d> {
        return iGetAttachmentPoints(shipId)
    }

    final override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        TODO()
        iMoveShipyardPosition(level, previous, new, newShipId)
        _extensions.forEach { it.onAfterMoveShipyardPositions(level, previous, new, newShipId) }
    }

    final override fun copyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
        _extensions.forEach { it.onBeforeCopyVEntity(level, mapped, centerPositions) }
        val new = iCopyVEntity(level, mapped, centerPositions)
        new?.let {
            it.dimensionId = level.dimensionId
            _extensions.forEach { it.onAfterCopyVEntity(level, mapped, centerPositions, new as ExtendableVEntity) }
        }
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
        saveTag.putString("dimensionId", dimensionId!!)

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
        dimensionId = tag.getString("dimensionId")

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
    final override fun onMakeVEntity(level: ServerLevel): List<CompletableFuture<Boolean>> {
        if (dimensionId == null) {dimensionId = level.dimensionId}
        return iOnMakeVEntity(level).also { _extensions.forEach { it.onMakeVEntity(level) } }
    }

    @Internal
    final override fun onDeleteVEntity(level: ServerLevel) {
        _extensions.forEach { it.onDeleteVEntity(level) }
        return iOnDeleteVEntity(level)
    }
}