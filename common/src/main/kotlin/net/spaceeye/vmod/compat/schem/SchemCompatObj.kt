package net.spaceeye.vmod.compat.schem

import dev.architectury.platform.Platform
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import org.apache.logging.log4j.Logger
import org.valkyrienskies.core.api.ships.ServerShip

object SchemCompatObj: BaseVSchemCompatProvider(VM.logger) {
    init {
        safeAdd("vs_clockwork") { ClockworkSchemCompat() }
        safeAdd("trackwork") { TrackworkSchemCompat() }

        safeAdd("create") { CreateContraptionsCompat() }
        safeAdd("create") { CreateKineticsCompat() }
    }
}

interface SchemCompatItem {
    fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, centerPositions: Map<Long, JVector3d>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit)
    fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, centerPositions: Map<Long, Pair<JVector3d, JVector3d>>, tag: CompoundTag, pos: BlockPos, state: BlockState, tagTransformer: (((CompoundTag?) -> CompoundTag?)?) -> Unit, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit)

    fun onEntityCopy(level: ServerLevel, entity: Entity, tag: CompoundTag, pos: Vector3d, shipCenter: Vector3d) {}
    fun onEntityPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, pos: Vector3d, shipCenter: Vector3d) {}
}

interface ExternalVSchemCompatProvider {
    fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, centerPositions: Map<Long, JVector3d>, be: BlockEntity?, tag: CompoundTag?): Boolean
    fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, centerPositions: Map<Long, Pair<JVector3d, JVector3d>>, tag: CompoundTag, pos: BlockPos, state: BlockState, tagTransformer: (((CompoundTag?) -> CompoundTag?)?) -> Unit): ((BlockEntity?) -> Unit)?

    fun onEntityCopy(level: ServerLevel, entity: Entity, tag: CompoundTag, pos: Vector3d, shipCenter: Vector3d)
    fun onEntityPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, pos: Vector3d, shipCenter: Vector3d)
}

open class BaseVSchemCompatProvider(val logger: Logger): ExternalVSchemCompatProvider {
    private val items = mutableListOf<SchemCompatItem>()

    fun safeAdd(name: String, supplier: () -> SchemCompatItem) {
        try {
            if (Platform.isModLoaded(name)) {items.add(supplier()) }
        } catch (e: Exception) {
            logger.error("Failed to apply compat for $name because of:\n${e.stackTraceToString()}")
        } catch (e: Error) {
            logger.error("Failed to apply compat for $name because of:\n${e.stackTraceToString()}")
        }
    }

    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, centerPositions: Map<Long, JVector3d>, be: BlockEntity?, tag: CompoundTag?): Boolean {
        var cancel = false
        items.forEach {
            try {
                it.onCopy(level, pos, state, ships, centerPositions, be, tag) { cancel = true }
            } catch (e: Exception) { ELOG("Compat object $it has failed onCopy with exception:\n${e.stackTraceToString()}")
            } catch (e: Error) { ELOG("Compat object $it has failed onCopy with error:\n${e.stackTraceToString()}") }
        }
        return cancel
    }
    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, centerPositions: Map<Long, Pair<JVector3d, JVector3d>>, tag: CompoundTag, pos: BlockPos, state: BlockState, tagTransformer: (((CompoundTag?) -> CompoundTag?)?) -> Unit): ((BlockEntity?) -> Unit)? {
        val callbacks = mutableListOf<(BlockEntity?) -> Unit>()
        items.forEach {
            try {
                it.onPaste(level, oldToNewId, centerPositions, tag, pos, state, tagTransformer) { cb -> callbacks.add(cb) }
            } catch (e: Exception) { ELOG("Compat object $it has failed onPaste with exception:\n${e.stackTraceToString()}")
            } catch (e: Error) { ELOG("Compat object $it has failed onPaste with error:\n${e.stackTraceToString()}") }
        }
        if (callbacks.isEmpty()) {return null}
        return {be -> callbacks.forEach {it(be)}}
    }

    override fun onEntityCopy(level: ServerLevel, entity: Entity, tag: CompoundTag, pos: Vector3d, shipCenter: Vector3d) {
        items.forEach {
            try {
                it.onEntityCopy(level, entity, tag, pos, shipCenter)
            } catch (e: Exception) { ELOG("Compat object $it has failed onEntityCopy with exception:\n${e.stackTraceToString()}")
            } catch (e: Error) { ELOG("Compat object $it has failed onEntityCopy with error:\n${e.stackTraceToString()}") }
        }
    }
    override fun onEntityPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, pos: Vector3d, shipCenter: Vector3d) {
        items.forEach {
            try {
                it.onEntityPaste(level, oldToNewId, tag, pos, shipCenter)
            } catch (e: Exception) { ELOG("Compat object $it has failed onEntityPaste with exception:\n${e.stackTraceToString()}")
            } catch (e: Error) { ELOG("Compat object $it has failed onEntityPaste with error:\n${e.stackTraceToString()}") }
        }
    }
}