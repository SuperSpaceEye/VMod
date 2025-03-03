package net.spaceeye.vmod.compat.schem

import dev.architectury.platform.Platform
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.ELOG
import org.valkyrienskies.core.api.ships.ServerShip

interface SchemCompatItem {
    fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit)
    fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, pos: BlockPos, state: BlockState, delayLoading: (delay: Boolean, ((CompoundTag?) -> CompoundTag?)?) -> Unit, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit)
}

object SchemCompatObj {
    private val items = mutableListOf<SchemCompatItem>()

    fun safeAdd(name: String, supplier: () -> SchemCompatItem) {
        try {
            if (Platform.isModLoaded(name)) {items.add(supplier()) }
        } catch (e: Exception) {
            ELOG("Failed to apply compat for $name because of:\n${e.stackTraceToString()}")
        } catch (e: Error) {
            ELOG("Failed to apply compat for $name because of:\n${e.stackTraceToString()}")
        }
    }

    init {
        safeAdd("vs_clockwork") { ClockworkSchemCompat() }
        safeAdd("trackwork") { TrackworkSchemCompat() }
        safeAdd("takeoff") { TakeoffSchemCompat() }
    }

    fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?): Boolean {
        var cancel = false
        items.forEach {
            try {
                it.onCopy(level, pos, state, ships, be, tag) { cancel = true }
            } catch (e: Exception) { ELOG("Compat object $it has failed onCopy with exception:\n${e.stackTraceToString()}")
            } catch (e: Error) { ELOG("Compat object $it has failed onCopy with error:\n${e.stackTraceToString()}") }
        }
        return cancel
    }
    fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, pos: BlockPos, state: BlockState, delayLoading: (delay: Boolean, ((CompoundTag?) -> CompoundTag?)?) -> Unit): ((BlockEntity?) -> Unit)? {
        val callbacks = mutableListOf<(BlockEntity?) -> Unit>()
        items.forEach {
            try {
                it.onPaste(level, oldToNewId, tag, pos, state, delayLoading) { cb -> callbacks.add(cb) }
            } catch (e: Exception) { ELOG("Compat object $it has failed onPaste with exception:\n${e.stackTraceToString()}")
            } catch (e: Error) { ELOG("Compat object $it has failed onPaste with error:\n${e.stackTraceToString()}") }
        }
        if (callbacks.isEmpty()) {return null}
        return {be -> callbacks.forEach {it(be)}}
    }
}