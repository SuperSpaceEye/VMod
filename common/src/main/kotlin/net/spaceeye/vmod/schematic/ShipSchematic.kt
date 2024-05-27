package net.spaceeye.vmod.schematic

import io.netty.buffer.Unpooled
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.schematic.containers.ShipSchematicV1
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import org.valkyrienskies.core.api.ships.ServerShip
import java.util.function.Supplier

typealias CopyEventSignature = (
        level: ServerLevel,
        shipsToBeSaved: List<ServerShip>,
        unregister: () -> Unit
        ) -> Serializable?
typealias PasteEventSignature = (
        level: ServerLevel,
        loadedShips: List<Pair<ServerShip, Long>>,
        file: Serializable,
        unregister: () -> Unit
        ) -> Unit

object ShipSchematic {
    const val currentSchematicVersion: Int = 1

    private val schematicVersions = mapOf<Int, Supplier<IShipSchematic>>(
            Pair(1, Supplier { ShipSchematicV1() } )
    )


    fun getSchematicConstructor(version: Int = currentSchematicVersion): Supplier<IShipSchematic> {
        val schem = schematicVersions[version] ?: throw AssertionError("Invalid schematic version")
        return schem
    }

    fun getSchematicFromBytes(bytes: ByteArray): IShipSchematic? {
        val buffer = Unpooled.wrappedBuffer(bytes)

        val schematic = try {
            getSchematicConstructor(buffer.readInt()).get()
        } catch (e: AssertionError) {return null
        } catch (e: Exception) { ELOG("Failed to load schematic with exception:\n${e.stackTraceToString()}"); return null
        } catch (e: Error) { ELOG("Failed to load schematic with error:\n${e.stackTraceToString()}"); return null }

        try {
            schematic.loadFromByteBuffer(buffer)
        } catch (e: AssertionError) {return null
        } catch (e: Exception) { ELOG("Failed to load schematic with exception:\n${e.stackTraceToString()}"); return null
        } catch (e: Error) { ELOG("Failed to load schematic with error:\n${e.stackTraceToString()}"); return null }

        return schematic
    }

    private val copyEvents = mutableMapOf<String, CopyEventSignature>()
    private val pasteEventsBefore = mutableMapOf<String, PasteEventSignature>()
    private val pasteEventsAfter = mutableMapOf<String, PasteEventSignature>()

    fun registerCopyPasteEvents(name: String, onCopy: CopyEventSignature, onPasteAfter: PasteEventSignature, onPasteBefore: PasteEventSignature = {_, _, _, _ ->}) {
        copyEvents[name] = onCopy
        pasteEventsBefore[name] = onPasteBefore
        pasteEventsAfter[name] = onPasteAfter
    }

    internal fun onCopy(level: ServerLevel, shipsToBeSaved: List<ServerShip>): List<Pair<String, Serializable>> {
        val toRemove = mutableListOf<String>()
        val toReturn = mutableListOf<Pair<String, Serializable>>()
        for ((name, fn) in copyEvents) {
            val file = try { fn(level, shipsToBeSaved) {toRemove.add(name)} ?: continue
            } catch (e: Exception) { ELOG("Event $name failed onCopy with exception:\n${e.stackTraceToString()}"); continue
            } catch (e: Error)     { ELOG("Event $name failed onCopy with exception:\n${e.stackTraceToString()}"); continue}
            toReturn.add(Pair(name, file))
        }
        toRemove.forEach { copyEvents.remove(it) }

        return toReturn
    }

    internal fun onPasteBeforeBlocksAreLoaded(level: ServerLevel, loadedShips: List<Pair<ServerShip, Long>>, files: List<Pair<String, Serializable>>) {
        val toRemove = mutableListOf<String>()
        for ((name, file) in files) {
            val event = pasteEventsBefore[name] ?: continue
            try { event(level, loadedShips, file) { toRemove.add(name) }
            } catch (e: Exception) { ELOG("Event $name failed onPasteBeforeBlocksAreLoaded with exception:\n${e.stackTraceToString()}"); continue
            } catch (e: Error)     { ELOG("Event $name failed onPasteBeforeBlocksAreLoaded with exception:\n${e.stackTraceToString()}"); continue }
        }
        toRemove.forEach { pasteEventsBefore.remove(it) }
    }

    internal fun onPasteAfterBlocksAreLoaded(level: ServerLevel, loadedShips: List<Pair<ServerShip, Long>>, files: List<Pair<String, Serializable>>) {
        val toRemove = mutableListOf<String>()
        for ((name, file) in files) {
            val event = pasteEventsAfter[name] ?: continue
            try { event(level, loadedShips, file) { toRemove.add(name) }
            } catch (e: Exception) { ELOG("Event $name failed onPasteAfterBlocksAreLoaded with exception:\n${e.stackTraceToString()}"); continue
            } catch (e: Error)     { ELOG("Event $name failed onPasteAfterBlocksAreLoaded with exception:\n${e.stackTraceToString()}"); continue }
        }
        toRemove.forEach { pasteEventsAfter.remove(it) }
    }
}