package net.spaceeye.vmod.vEntityManaging.extensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import net.spaceeye.vmod.vEntityManaging.util.VEntityExtension
import org.valkyrienskies.core.api.ships.properties.ShipId

class ConvertedFromLegacy: VEntityExtension {
    override fun onInit(obj: ExtendableVEntity) {}

    override fun onAfterCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, new: ExtendableVEntity) {
        new.addExtension(ConvertedFromLegacy())
    }

    override fun onSerialize(): CompoundTag? {
        return CompoundTag()
    }

    override fun onDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): Boolean {
        return true
    }

    override fun onMakeVEntity(level: ServerLevel) {}
    override fun onDeleteVEntity(level: ServerLevel) {}
}