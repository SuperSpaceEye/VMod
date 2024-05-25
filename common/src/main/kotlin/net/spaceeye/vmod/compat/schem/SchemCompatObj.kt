package net.spaceeye.vmod.compat.schem

import dev.architectury.platform.Platform
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.api.ships.ServerShip

interface SchemCompatItem {
    fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit)
    fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit)
}

object SchemCompatObj {
    val items = mutableListOf<SchemCompatItem>()

    init {
        if (Platform.isModLoaded("vs_clockwork")) { items.add(ClockworkSchemCompat()) }
        if (Platform.isModLoaded("trackwork")) { items.add(TrackworkSchemCompat()) }
        if (Platform.isModLoaded("vs_takeoff")) { items.add(TakeoffSchemCompat()) }
        if (Platform.isModLoaded("create")) { items.add(CreateSchemCompat()) }
    }

    fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?): Boolean {
        var cancel = false
        items.forEach { it.onCopy(level, pos, state, ships, be, tag) { cancel = true } }
        return cancel
    }
    fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState): ((BlockEntity?) -> Unit)? {
        val callbacks = mutableListOf<(BlockEntity?) -> Unit>()
        items.forEach { it.onPaste(level, oldToNewId, tag, state) { cb -> callbacks.add (cb) } }
        if (callbacks.isEmpty()) {return null}
        return {be -> callbacks.forEach {it(be)}}
    }
}