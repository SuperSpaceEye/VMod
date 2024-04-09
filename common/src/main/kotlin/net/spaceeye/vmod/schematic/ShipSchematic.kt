package net.spaceeye.vmod.schematic

import io.netty.buffer.Unpooled
import net.spaceeye.vmod.schematic.containers.ShipSchematicV1
import net.spaceeye.vmod.schematic.icontainers.IFile
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import org.valkyrienskies.core.api.ships.ServerShip
import java.util.function.Supplier

typealias CopyEventSignature = (
        shipsToBeSaved: List<ServerShip>,
        unregister: () -> Unit
        ) -> IFile?
typealias PasteEventSignature = (
        loadedShips: List<Pair<ServerShip, Long>>,
        file: IFile,
        unregister: () -> Unit
        ) -> Unit

object ShipSchematic {
    val currentSchematicVersion: Int = 1

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
        } catch (e: AssertionError) {return null}

        schematic.loadFromByteBuffer(buffer)
        return schematic
    }

    private val copyEvents = mutableMapOf<String, CopyEventSignature>()
    private val pasteEvents = mutableMapOf<String, PasteEventSignature>()

    fun registerCopyPasteEvents(name: String, onCopy: CopyEventSignature, onPaste: PasteEventSignature) {
        copyEvents[name] = onCopy
        pasteEvents[name] = onPaste
    }

    internal fun onCopy(shipsToBeSaved: List<ServerShip>): List<Pair<String, IFile>> {
        val toRemove = mutableListOf<String>()
        val toReturn = mutableListOf<Pair<String, IFile>>()
        for ((name, fn) in copyEvents) {
            val file = fn(shipsToBeSaved) {toRemove.add(name)} ?: continue
            toReturn.add(Pair(name, file))
        }
        toRemove.forEach { copyEvents.remove(it) }

        return toReturn
    }

    internal fun onPaste(loadedShips: List<Pair<ServerShip, Long>>, files: List<Pair<String, IFile>>) {
        val toRemove = mutableListOf<String>()
        files.forEach { (name, file) ->
            val event = pasteEvents[name] ?: return@forEach
            event(loadedShips, file) { toRemove.add(name) }
        }
        toRemove.forEach { pasteEvents.remove(it) }
    }
}