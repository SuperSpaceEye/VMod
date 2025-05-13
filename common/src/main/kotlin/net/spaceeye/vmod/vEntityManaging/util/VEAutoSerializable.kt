package net.spaceeye.vmod.vEntityManaging.util

import net.minecraft.nbt.CompoundTag
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.reflectable.TagAutoSerializable
import org.valkyrienskies.core.api.ships.properties.ShipId
import net.spaceeye.vmod.reflectable.ReflectableItem

/**
 * Adds [TagAutoSerializable] interface and implements [iNbtSerialize] and [iNbtDeserialize] for [ExtendableVEntity]
 */
interface VEAutoSerializable: VEntity, TagAutoSerializable, ExtendableVEntityIMethods {
    override fun iNbtSerialize(): CompoundTag? = tSerialize()
    override fun iNbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): VEntity? = tDeserialize(tag).let { this }

    /**
     * set "NoTagSerialization" of metadata to true if you don't want for value to get serialized with tSerialize
     */
    fun <T: Any> get(pos: Int, default: T) = ReflectableItem.get(pos, default)
}