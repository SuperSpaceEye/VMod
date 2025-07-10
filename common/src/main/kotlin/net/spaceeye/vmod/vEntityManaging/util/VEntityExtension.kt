package net.spaceeye.vmod.vEntityManaging.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.VEExtensionTypes
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.properties.ShipId

interface TickableVEntityExtension: VEntityExtension {
    fun tick(server: MinecraftServer)
}

interface VEntityExtension {
    fun onInit(obj: ExtendableVEntity)

    fun onAfterMoveAttachmentPoints(level: ServerLevel, pointsToMove: List<Vector3d>, oldShipId: ShipId, newShipId: ShipId, oldCenter: Vector3d, newCenter: Vector3d) {TODO()}

    fun onBeforeCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>) {}
    //should add new extension to new VEntity
    //TODO maybe just have return be VEntityExtensionv? which then will be added to new instead of manually adding it at the end?
    fun onAfterCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>, new: ExtendableVEntity)

    fun onBeforeOnScaleByVEntity(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) { }

    // will be called after VEntity is serialized
    fun onSerialize(): CompoundTag?
    // will be called after VEntity is deserialized
    // if returns true then everything is ok
    // if returns false then it failed to deserialize and extension will not be added
    fun onDeserialize(tag: CompoundTag): Boolean

    fun onMakeVEntity(level: ServerLevel)
    fun onDeleteVEntity(level: ServerLevel)

    companion object {
        fun fromType(type: String) = VEExtensionTypes.strTypeToSupplier(type).get()
        fun VEntityExtension.toType() = VEExtensionTypes.typeToString(this::class.java)
    }
}