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
        globalMap: MutableMap<String, Any>,
        unregister: () -> Unit
        ) -> Serializable?
typealias PasteEventSignature = (
        level: ServerLevel,
        loadedShips: List<Pair<ServerShip, Long>>,
        file: Serializable?,
        globalMap: MutableMap<String, Any>,
        unregister: () -> Unit
        ) -> Unit

private data class Events(
    val copyEvent: CopyEventSignature,
    val pasteBeforeEvent: PasteEventSignature,
    val pasteAfterEvent: PasteEventSignature,
    val next: MutableList<String> = mutableListOf(),
    var globalMap: MutableMap<String, Any> = mutableMapOf()
)

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
            schematic.deserialize(buffer)
        } catch (e: AssertionError) {return null
        } catch (e: Exception) { ELOG("Failed to load schematic with exception:\n${e.stackTraceToString()}"); return null
        } catch (e: Error) { ELOG("Failed to load schematic with error:\n${e.stackTraceToString()}"); return null }

        return schematic
    }

    private val rootEvents = mutableMapOf<String, Events>()
    private val allEvents = mutableMapOf<String, Events>()
    private val toAddEvent = mutableMapOf<String, MutableList<String>>()

    fun registerCopyPasteEvents(name: String, onCopy: CopyEventSignature, onPasteAfter: PasteEventSignature, onPasteBefore: PasteEventSignature = {_, _, _, _, _ ->}) {
        val events = Events(onCopy, onPasteBefore, onPasteAfter)
        rootEvents[name] = events
        allEvents[name] = events

        val toAdd = toAddEvent[name]
        if (toAdd != null) {
            events.next.addAll(toAdd)
            toAddEvent.remove(name)
        }
    }

    fun registerOrderedCopyPasteEvents(name: String, after: String, onCopy: CopyEventSignature, onPasteAfter: PasteEventSignature, onPasteBefore: PasteEventSignature = {_, _, _, _, _ ->}) {
        val events = Events(onCopy, onPasteBefore, onPasteAfter)
        allEvents[name] = events

        val toAdd = toAddEvent[name]
        if (toAdd != null) {
            events.next.addAll(toAdd)
            toAddEvent.remove(name)
        }

        val node = allEvents[after]
        if (node == null) {
            toAddEvent.getOrPut(after) { mutableListOf() }.add(name)
            return
        }
        node.next.add(name)
    }

    fun getGlobalMap(name: String): Map<String, Any>? = allEvents[name]?.globalMap

    // Is called on copy, before blocks were copied
    internal fun onCopy(level: ServerLevel, shipsToBeSaved: List<ServerShip>): List<Pair<String, Serializable>> {
        val toRemove = mutableListOf<String>()
        val toReturn = mutableListOf<Pair<String, Serializable>>()

        val toExecute = mutableListOf<String>()
        val executed = mutableSetOf<String>()

        toExecute.addAll(rootEvents.keys)

        while (toExecute.isNotEmpty()) {
            val name = toExecute.removeLast()
            if (executed.contains(name)) {continue}
            executed.add(name)

            val event = allEvents[name] ?: continue

            val file = try { event.copyEvent(level, shipsToBeSaved, event.globalMap) {toRemove.add(name)}
            } catch (e: Exception) { ELOG("Event $name failed onCopy with exception:\n${e.stackTraceToString()}"); continue
            } catch (e: Error)     { ELOG("Event $name failed onCopy with exception:\n${e.stackTraceToString()}"); continue}
            if (file != null) toReturn.add(Pair(name, file))

            toExecute.addAll(event.next.filter { !executed.contains(it) })
        }
        toRemove.forEach { allEvents.remove(it); rootEvents.remove(it) }

        return toReturn
    }

    // Is called after all ServerShips are created, but blocks haven't been placed yet, so VS didn't "create them"
    internal fun onPasteBeforeBlocksAreLoaded(level: ServerLevel, emptyShips: List<Pair<ServerShip, Long>>, files: List<Pair<String, Serializable>>) {
        val toRemove = mutableListOf<String>()
        val filesMap = files.toMap()

        val toExecute = mutableListOf<String>()
        val executed = mutableSetOf<String>()

        toExecute.addAll(rootEvents.keys)

        while (toExecute.isNotEmpty()) {
            val name = toExecute.removeLast()
            if (executed.contains(name)) {continue}
            executed.add(name)

            val event = allEvents[name] ?: continue

            try { event.pasteBeforeEvent(level, emptyShips, filesMap[name], event.globalMap) {toRemove.add(name)}
            } catch (e: Exception) { ELOG("Event $name failed onPasteBeforeBlocksAreLoaded with exception:\n${e.stackTraceToString()}"); continue
            } catch (e: Error)     { ELOG("Event $name failed onPasteBeforeBlocksAreLoaded with exception:\n${e.stackTraceToString()}"); continue}

            toExecute.addAll(event.next.filter { !executed.contains(it) })
        }
        toRemove.forEach { allEvents.remove(it); rootEvents.remove(it) }
    }

    // Is called after all ServerShips are created with blocks placed in shipyard
    internal fun onPasteAfterBlocksAreLoaded(level: ServerLevel, loadedShips: List<Pair<ServerShip, Long>>, files: List<Pair<String, Serializable>>) {
        val toRemove = mutableListOf<String>()
        val filesMap = files.toMap()

        val toExecute = mutableListOf<String>()
        val executed = mutableSetOf<String>()

        toExecute.addAll(rootEvents.keys)

        while (toExecute.isNotEmpty()) {
            val name = toExecute.removeLast()
            if (executed.contains(name)) {continue}
            executed.add(name)

            val event = allEvents[name] ?: continue

            try { event.pasteAfterEvent(level, loadedShips, filesMap[name], event.globalMap) {toRemove.add(name)}
            } catch (e: Exception) { ELOG("Event $name failed onPasteAfterBlocksAreLoaded with exception:\n${e.stackTraceToString()}"); continue
            } catch (e: Error)     { ELOG("Event $name failed onPasteAfterBlocksAreLoaded with exception:\n${e.stackTraceToString()}"); continue}

            toExecute.addAll(event.next.filter { !executed.contains(it) })
        }
        toRemove.forEach { allEvents.remove(it); rootEvents.remove(it) }
    }
}