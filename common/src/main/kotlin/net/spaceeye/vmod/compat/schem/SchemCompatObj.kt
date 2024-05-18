package net.spaceeye.vmod.compat.schem

import dev.architectury.platform.Platform
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState

interface SchemCompatItem {
    fun onCopy()
    fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState)
}

object SchemCompatObj {
    val items = mutableListOf<SchemCompatItem>()

    init {
        if (Platform.isModLoaded("vs_clockwork")) {
            items.add(ClockworkSchemCompat())
        }
    }

    fun onCopy() {
        items.forEach { it.onCopy() }
    }
    fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState) {
        items.forEach { it.onPaste(level, oldToNewId, tag, state) }
    }
}