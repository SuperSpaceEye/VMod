package net.spaceeye.vmod.compat.schem.fabric

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import net.minecraft.nbt.CompoundTag

object CreateSuperglueCompatPlatformUtilsImpl {
    @JvmStatic
    fun saveToTag(entity: SuperGlueEntity, tag: CompoundTag): Unit { entity.save(tag) }
    @JvmStatic
    fun loadFromTag(entity: SuperGlueEntity, tag: CompoundTag): Unit { entity.load(tag) }
}