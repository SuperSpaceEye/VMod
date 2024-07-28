package net.spaceeye.vmod.compat.schem.forge

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity

object EntitySavingPlatformUtilsImpl {
    @JvmStatic
    fun saveToTag(entity: Entity, tag: CompoundTag): Unit { entity.save(tag) }
    @JvmStatic
    fun loadFromTag(entity: Entity, tag: CompoundTag): Unit { entity.load(tag) }
}