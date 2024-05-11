package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.DataStream
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.gui.SchemGUIBuilder
import net.spaceeye.vmod.toolgun.modes.inputHandling.SchemCRIHandler
import net.spaceeye.vmod.toolgun.modes.serializing.SchemSerializable
import net.spaceeye.vmod.toolgun.modes.state.ClientPlayerSchematics.SchemHolder
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.Either
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.ServerClosable
import org.joml.Quaterniond
import org.valkyrienskies.mod.common.getShipManagingPos
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

const val SCHEM_EXTENSION = "vschem"

//TODO Add rate limit
object ClientPlayerSchematics {
    //TODO add handling logic for receiverDataTransmissionFailed
    var saveSchemStream = DataStream(
        "save_schem_stream",
        NetworkManager.Side.S2C,
        NetworkManager.Side.C2S,
        0,
        ::SendSchemRequest,
        ::SchemHolder,
        receiverDataTransmitted = {uuid, data ->
            ClientToolGunState.currentMode?.let {
                if (it !is SchemMode) {return@let}
                it.schem = ShipSchematic.getSchematicFromBytes(data!!.data.array())
                it.saveSchem(listSchematics())
                it.reloadScrollItems()
            }
        },
    )

    var loadSchemStream = DataStream(
        "load_schem_stream",
        NetworkManager.Side.C2S,
        NetworkManager.Side.C2S,
        VMConfig.CLIENT.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE,
        ServerPlayerSchematics::SendLoadRequest,
        ::SchemHolder,
        transmitterRequestProcessor = {loadRequest ->
            val res = ClientToolGunState.currentMode?.let { mode ->
                if (mode !is SchemMode) { return@let null }
                mode.schem?.let { SchemHolder(it.saveToFile().toBytes(), loadRequest.requestUUID) }
            }
            if (res != null) { Either.Left(res) } else { Either.Right(DataStream.RequestFailurePkt()) }
        }
    )

    class SchemHolder(): Serializable {
        lateinit var data: ByteBuf
        lateinit var uuid: UUID

        constructor(data: ByteBuf, uuid: UUID = UUID(0L, 0L)): this() {
            this.data = data
            this.uuid = uuid
        }

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeUUID(uuid)
            buf.writeByteArray(data.array())

            return buf
        }
        override fun deserialize(buf: FriendlyByteBuf) {
            uuid = buf.readUUID()
            data = Unpooled.wrappedBuffer(buf.readByteArray())
        }
    }

    class SendSchemRequest(): Serializable {
        lateinit var uuid: UUID
        constructor(player: Player): this() {this.uuid = player.uuid}

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeUUID(uuid)

            return buf
        }
        override fun deserialize(buf: FriendlyByteBuf) {
            uuid = buf.readUUID()
        }
    }

    fun listSchematics(): List<Path> {
        try {
            Files.createDirectories(Paths.get("VMod-Schematics"))
        } catch (e: IOException) {return emptyList()}
        val files = Files.list(Paths.get("VMod-Schematics"))
        return files.filter { !it.isDirectory() && (it.extension == SCHEM_EXTENSION) }.toList()
    }

    fun loadSchematic(path: Path): IShipSchematic? {
        val bytes = Files.readAllBytes(path)
        return ShipSchematic.getSchematicFromBytes(bytes)
    }

    fun saveSchematic(name: String, schematic: IShipSchematic): Boolean {
        try {
            Files.write(Paths.get("VMod-Schematics/${name}"), schematic.saveToFile().toBytes().array())
        } catch (e: IOException) {return false}
        return true
    }
}

object ServerPlayerSchematics: ServerClosable() {
    var saveSchemStream = DataStream(
        "save_schem_stream",
        NetworkManager.Side.S2C,
        NetworkManager.Side.S2C,
        VMConfig.SERVER.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE,
        ClientPlayerSchematics::SendSchemRequest,
        ClientPlayerSchematics::SchemHolder,
        transmitterRequestProcessor = {
            val res = schematics[it.uuid] ?.let { SchemHolder(it.saveToFile().toBytes()) }
            if (res != null) { Either.Left(res) } else { Either.Right(DataStream.RequestFailurePkt()) }
        }
    )

    //TODO add handling logic for receiverDataTransmissionFailed
    var loadSchemStream = DataStream(
        "load_schem_stream",
        NetworkManager.Side.C2S,
        NetworkManager.Side.S2C,
        0,
        ::SendLoadRequest,
        ::SchemHolder,
        receiverDataTransmitted = {_, data ->
            schematics[data!!.uuid] = ShipSchematic.getSchematicFromBytes(data.data.array())
        }
    )

    class SendLoadRequest(): Serializable {
        lateinit var requestUUID: UUID

        constructor(uuid: UUID): this() {requestUUID = uuid}
        constructor(buf: FriendlyByteBuf): this() {deserialize(buf)}

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeUUID(requestUUID)

            return buf
        }
        override fun deserialize(buf: FriendlyByteBuf) {
            requestUUID = buf.readUUID()
        }
    }

    val schematics = mutableMapOf<UUID, IShipSchematic?>()

    override fun close() {
        schematics.clear()
    }

    init {
        PlayerEvent.PLAYER_QUIT.register {
            schematics.remove(it.uuid)
        }
    }
}

object SchemNetworking: BaseNetworking<SchemMode>() {
    val networkName = "schem_networking"

    init {
        ClientPlayerSchematics
        ServerPlayerSchematics
    }

    // transmitter can't begin transmitting data to receiver by itself
    val c2sLoadSchematic = "load_schematic" idWithConnc {
        object : C2SConnection<C2SLoadSchematic>(it, networkName) {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                ServerPlayerSchematics.loadSchemStream.r2tRequestData.transmitData(context, ServerPlayerSchematics.SendLoadRequest(context.player.uuid))
            }
        }
    }

    class C2SLoadSchematic(): Serializable {
        override fun serialize(): FriendlyByteBuf { return getBuffer() }
        override fun deserialize(buf: FriendlyByteBuf) {}
    }
}

class SchemMode: BaseMode, SchemGUIBuilder, SchemCRIHandler, SchemSerializable {
    override var itemsScroll: ScrollComponent? = null
    override lateinit var parentWindow: UIBlock

    override fun init(type: BaseNetworking.EnvType) {
        SchemNetworking.init(this, type)
    }

    fun saveSchem(items: List<Path>) {
        var name = filename
        val names = items.map { it.fileName.toString() }

        while (names.contains(name + ".${SCHEM_EXTENSION}")) { name += "_" }
        if (!name.endsWith(".${SCHEM_EXTENSION}")) { name += ".${SCHEM_EXTENSION}" }

        ClientPlayerSchematics.saveSchematic(name, schem!!)
    }

    val conn_primary = register { object : C2SConnection<SchemMode>("schem_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SchemMode>(context.player, buf, ::SchemMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }
    val conn_secondary = register { object : C2SConnection<SchemMode>("schem_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SchemMode>(context.player, buf, ::SchemMode) { item, serverLevel, player, raycastResult -> item.activateSecondaryFunction(serverLevel, player, raycastResult) } } }

    var schem: IShipSchematic? = null
    var filename = ""

    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {resetState(); return}
        player as ServerPlayer

        val serverCaughtShip = level.getShipManagingPos(raycastResult.blockPosition) ?: return
        val schem = ShipSchematic.getSchematicConstructor().get()
        schem.makeFrom(serverCaughtShip)
        ServerPlayerSchematics.schematics[player.uuid] = schem
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val schem = ServerPlayerSchematics.schematics[player.uuid] ?: return
        if (raycastResult.state.isAir) {return}

        schem.placeAt(level, raycastResult.worldCenteredHitPos!!.toJomlVector3d(), Quaterniond())
    }
}