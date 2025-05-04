package net.spaceeye.vmod.toolgun.modes.state

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.networking.NetworkManager
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematicInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.rendering.ClientRenderingData
import net.spaceeye.vmod.schematic.SchematicActionsQueue
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.gui.SchemGUI
import net.spaceeye.vmod.toolgun.modes.hud.SchemHUD
import net.spaceeye.vmod.toolgun.modes.state.ClientPlayerSchematics.SchemHolder
import net.spaceeye.vmod.reflectable.ByteSerializableItem.get
import net.spaceeye.vmod.reflectable.constructor
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.special.SchemRenderer
import net.spaceeye.vmod.schematic.VModShipSchematicV2
import net.spaceeye.vmod.schematic.makeFrom
import net.spaceeye.vmod.schematic.placeAt
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.state.ServerPlayerSchematics.SendLoadRequest
import net.spaceeye.vmod.translate.COULDNT_LOAD_VMODSCHEM_V1
import net.spaceeye.vmod.utils.*
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.valkyrienskies.mod.common.getShipManagingPos
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.math.sign

const val SCHEM_EXTENSION = "vschem"

private fun bcGetSchematicFromBytes(bytes: ByteArray): IShipSchematic? {
    val buf = FriendlyByteBuf(Unpooled.wrappedBuffer(bytes))
    return if (bytes[3].toInt() == 1) {
        if (Minecraft.getInstance()?.player != null) {
            ClientToolGunState.closeGUI()
            ClientToolGunState.addHUDError("Couldn't load schematic")
            Minecraft.getInstance().player!!.sendMessage(COULDNT_LOAD_VMODSCHEM_V1, null)
        }
        null
    } else if (buf.readUtf() == "vschem") {
        when (buf.readUtf()) {
            "VModShipSchematicV1" -> {
                if (Minecraft.getInstance()?.player != null) {
                    ClientToolGunState.closeGUI()
                    ClientToolGunState.addHUDError("Couldn't load schematic")
                    Minecraft.getInstance().player!!.sendMessage(COULDNT_LOAD_VMODSCHEM_V1, null)
                }
                null
            }
            else -> ShipSchematic.getSchematicFromBytes(bytes)
        }
    } else {
        ShipSchematic.getSchematicFromBytes(bytes)
    }
}

//TODO Add rate limit
object ClientPlayerSchematics {
    //TODO add handling logic for receiverDataTransmissionFailed
    var saveSchemStream = object : DataStream<SendSchemRequest, SchemHolder>(
        "save_schem_stream",
        NetworkManager.Side.S2C,
        NetworkManager.Side.C2S,
    ) {
        override val partByteAmount: Int get() = 0
        override fun requestPacketConstructor(buf: FriendlyByteBuf) = SendSchemRequest::class.constructor(buf)
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Client Save Schem Transmission Failed") }
        override fun transmitterRequestProcessor(req: SendSchemRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? { throw AssertionError("Invoked Transmitter code on Receiver side") }

        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) {
            ClientToolGunState.currentMode?.let {
                if (it !is SchemMode) {return@let}
                it.schem = bcGetSchematicFromBytes(data!!.data.array())
                it.saveSchem(listSchematics())
                it.reloadScrollItems()
            }
        }
    }

    var getSchemStream = object : DataStream<SendSchemRequest, SchemHolder>(
        "get_schem_stream",
        NetworkManager.Side.S2C,
        NetworkManager.Side.C2S,
    ) {
        override val partByteAmount: Int get() = 0
        override fun requestPacketConstructor(buf: FriendlyByteBuf) = SendSchemRequest::class.constructor(buf)
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) {  }
        override fun transmitterRequestProcessor(req: SendSchemRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? { throw AssertionError("Invoked Transmitter code on Receiver side") }

        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) {
            ClientToolGunState.currentMode?.let {
                if (it !is SchemMode) {return@let}
                it.schem = bcGetSchematicFromBytes(data!!.data.array())
            }
        }
    }

    var loadSchemStream = object : DataStream<SendLoadRequest, SchemHolder>(
        "load_schem_stream",
        NetworkManager.Side.C2S,
        NetworkManager.Side.C2S,
    ) {
        override val partByteAmount: Int get() = VMConfig.CLIENT.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE
        override fun requestPacketConstructor(buf: FriendlyByteBuf) = SendLoadRequest::class.constructor(buf)
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) { throw AssertionError("Invoked Receiver code on Transmitter side") }
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Client Load Schem Transmission Failed") }

        override fun transmitterRequestProcessor(req: SendLoadRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? {
            val res = ClientToolGunState.currentMode?.let { mode ->
                if (mode !is SchemMode) { return@let null }
                mode.schem?.let { SchemHolder(ShipSchematic.writeSchematicToBuffer(it)!!, req.requestUUID) }
            }
            return if (res != null) { Either.Left(res) } else { Either.Right(RequestFailurePkt()) }
        }
    }

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

    data class SendSchemRequest(var uuid: UUID): AutoSerializable

    fun listSchematics(): List<Path> {
        try {
            Files.createDirectories(Paths.get("VMod-Schematics"))
        } catch (e: IOException) {return emptyList()}
        val files = Files.list(Paths.get("VMod-Schematics"))
        return files.filter { !it.isDirectory() && (it.extension == SCHEM_EXTENSION) }.toList()
    }

    fun loadSchematic(path: Path): IShipSchematic? {
        try {
            val bytes = Files.readAllBytes(path)
            return bcGetSchematicFromBytes(bytes)
        } catch (e: Exception) {
            ELOG("Failed to load file because ${e.stackTraceToString()}")
            return null
        }
    }

    fun saveSchematic(name: String, schematic: IShipSchematic): Boolean {
        try {
            Files.write(Paths.get("VMod-Schematics/${name}"), ShipSchematic.writeSchematicToBuffer(schematic)!!.array())
        } catch (e: IOException) {
            ELOG("Failed to save schematic to file because ${e.stackTraceToString()}")
            return false
        }
        return true
    }
}

object ServerPlayerSchematics: ServerClosable() {
    var saveSchemStream = object : DataStream<ClientPlayerSchematics.SendSchemRequest, SchemHolder>(
        "save_schem_stream",
        NetworkManager.Side.S2C,
        NetworkManager.Side.S2C,
    ) {
        override val partByteAmount: Int get() = VMConfig.SERVER.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE
        override fun requestPacketConstructor(buf: FriendlyByteBuf) = ClientPlayerSchematics.SendSchemRequest::class.constructor(buf)
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Server Save Schem Transmission Failed") }
        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) { throw AssertionError("Invoked Receiver code on Transmitter side") }
        override fun uuidHasAccess(uuid: UUID): Boolean { return ServerToolGunState.playerHasAccess(ServerLevelHolder.server!!.playerList.getPlayer(uuid) ?: return false) }

        override fun transmitterRequestProcessor(req: ClientPlayerSchematics.SendSchemRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? {
            val res = schematics[req.uuid] ?.let { SchemHolder(ShipSchematic.writeSchematicToBuffer(it)!!) }
            return if (res != null) { Either.Left(res) } else { Either.Right(RequestFailurePkt()) }
        }
    }

    var getSchemStream = object : DataStream<ClientPlayerSchematics.SendSchemRequest, SchemHolder>(
        "get_schem_stream",
        NetworkManager.Side.S2C,
        NetworkManager.Side.S2C,
    ) {
        override val partByteAmount: Int get() = VMConfig.SERVER.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE
        override fun requestPacketConstructor(buf: FriendlyByteBuf) = ClientPlayerSchematics.SendSchemRequest::class.constructor(buf)
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Server Get Schem Transmission Failed") }
        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) { throw AssertionError("Invoked Receiver code on Transmitter side") }
        override fun uuidHasAccess(uuid: UUID): Boolean { return ServerToolGunState.playerHasAccess(ServerLevelHolder.server!!.playerList.getPlayer(uuid) ?: return false) }

        override fun transmitterRequestProcessor(req: ClientPlayerSchematics.SendSchemRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? {
            val res = schematics[req.uuid] ?.let { SchemHolder(ShipSchematic.writeSchematicToBuffer(it)!!) }
            return if (res != null) { Either.Left(res) } else { Either.Right(RequestFailurePkt()) }
        }
    }

    var loadSchemStream = object : DataStream<SendLoadRequest, SchemHolder>(
        "load_schem_stream",
        NetworkManager.Side.C2S,
        NetworkManager.Side.S2C,
    ) {
        override val partByteAmount: Int get() = 0
        override fun requestPacketConstructor(buf: FriendlyByteBuf) = SendLoadRequest::class.constructor(buf)
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Server Load Schem Transmission Failed") }
        override fun transmitterRequestProcessor(req: SendLoadRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? { throw AssertionError("Invoked Transmitter code on Receiver side") }
        override fun uuidHasAccess(uuid: UUID): Boolean { return ServerToolGunState.playerHasAccess(ServerLevelHolder.server!!.playerList.getPlayer(uuid) ?: return false) }

        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) {
            schematics[data!!.uuid] = bcGetSchematicFromBytes(data.data.array())
            loadRequests.remove(data.uuid)
        }
    }

    val loadRequests = mutableMapOf<UUID, Long>()

    data class SendLoadRequest(var requestUUID: UUID): AutoSerializable

    val schematics = mutableMapOf<UUID, IShipSchematic?>()

    override fun close() {
        schematics.clear()
        loadRequests.clear()
    }

    init {
        PlayerEvent.PLAYER_QUIT.register {
            schematics.remove(it.uuid)
            loadRequests.remove(it.uuid)
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
    val c2sLoadSchematic = regC2S<EmptyPacket>("load_schematic", networkName, {player ->
        val lastReq = ServerPlayerSchematics.loadRequests[player.uuid]
        lastReq == null || getNow_ms() - lastReq > 10000L
    }) {pkt, player ->
        ServerPlayerSchematics.loadRequests[player.uuid] = getNow_ms()
        ServerPlayerSchematics.loadSchemStream.r2tRequestData.transmitData(SendLoadRequest(player.uuid), FakePacketContext(player))
    }

    val s2cSendSchem = regS2C<EmptyPacket>("send_schem", networkName) {pkt ->
        ClientToolGunState.currentMode?.let {
            if (it !is SchemMode) {return@let}
            it.schem = null
        }
        ClientPlayerSchematics.getSchemStream.r2tRequestData.transmitData(ClientPlayerSchematics.SendSchemRequest(Minecraft.getInstance().player!!.uuid))
    }
}

class SchemMode: ExtendableToolgunMode(), SchemGUI, SchemHUD {
    @JsonIgnore private var i = 0

    var rotationAngle: Ref<Double> by get(i++, Ref(0.0), {it}, customSerialize = { it, buf -> buf.writeDouble((it).it)}, customDeserialize = { buf -> val rotationAngle = Ref(0.0) ; rotationAngle.it = buf.readDouble(); rotationAngle})

    //TODO doesn't matter right now, but when i will add setting presets i will need a way to get this value
    var transparency: Float = 0.5f

    override fun eInit(type: BaseNetworking.EnvType) {
        SchemNetworking.init(this, type)
    }

    fun saveSchem(items: List<Path>) {
        var name = filename
        val names = items.map { it.fileName.toString() }

        while (names.contains(name + ".${SCHEM_EXTENSION}")) { name += "_" }
        if (!name.endsWith(".${SCHEM_EXTENSION}")) { name += ".${SCHEM_EXTENSION}" }

        ClientPlayerSchematics.saveSchematic(name, schem!!)
    }

    var renderer: BaseRenderer? = null

    private var rID = -1
    var shipInfo: IShipSchematicInfo? = null
    var schem: IShipSchematic? = null
        set(value) {
            field = value;
            shipInfo = value?.info

            if (value == null) {
                renderer = null
                ClientRenderingData.removeClientsideRenderer(rID)
                refreshHUD()
                return
            }

            renderer = SchemRenderer(value, rotationAngle, transparency)
            rID = ClientRenderingData.addClientsideRenderer(renderer!!)
            refreshHUD()
        }

    var filename = ""

    var scrollAngle = Math.toRadians(10.0)

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        ClientRenderingData.removeClientsideRenderer(rID)
        if (raycastResult.state.isAir) {
            resetState();
            ServerPlayerSchematics.schematics.remove(player.uuid)
            SchemNetworking.s2cSendSchem.sendToClient(player, EmptyPacket())
            return
        }
        // TODO
        if (SchematicActionsQueue.uuidIsQueuedInSomething(player.uuid)) {return}

        val serverCaughtShip = level.getShipManagingPos(raycastResult.blockPosition) ?: run {
            resetState();
            ServerPlayerSchematics.schematics.remove(player.uuid)
            SchemNetworking.s2cSendSchem.sendToClient(player, EmptyPacket())
            null
        } ?: return
        val schem = VModShipSchematicV2()
        schem.makeFrom(player.level() as ServerLevel, player, player.uuid, serverCaughtShip) {
            ServerPlayerSchematics.schematics[player.uuid] = schem
            SchemNetworking.s2cSendSchem.sendToClient(player, EmptyPacket())
        }
    }

    fun activateSecondaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult) {
        val schem = ServerPlayerSchematics.schematics[player.uuid] ?: return
        if (raycastResult.state.isAir) {return}
        //TODO
        if (SchematicActionsQueue.uuidIsQueuedInSomething(player.uuid)) {return}

        val info = schem.info!!

        val pos = raycastResult.worldHitPos!! + (raycastResult.worldNormalDirection!! * info.maxObjectPos.y) // + (raycastResult.worldNormalDirection!! * 0.5)

        val rotation = Quaterniond()
            .mul(Quaterniond(AxisAngle4d(rotationAngle.it, raycastResult.worldNormalDirection!!.toJomlVector3d())))
            .mul(getQuatFromDir(raycastResult.worldNormalDirection!!))
            .normalize()

        (schem as IShipSchematicDataV1).placeAt(level, player, player.uuid, pos.toJomlVector3d(), rotation) {}
    }

    override fun eResetState() {
        rotationAngle.it = 0.0
    }

    override fun eOnMouseScrollEvent(amount: Double): EventResult {
        if (shipInfo == null) { return EventResult.pass() }

        rotationAngle.it += scrollAngle * amount.sign

        return EventResult.interruptFalse()
    }

    companion object {
        init {
            SchemNetworking
            ToolgunModes.registerWrapper(SchemMode::class) {
                it.addExtension<SchemMode> {
                    BasicConnectionExtension<SchemMode>("schem_mode"
                        ,allowResetting = true
                        ,leftFunction       = { item, level, player, rr -> item.activatePrimaryFunction(level, player, rr) }
                        ,rightFunction      = { item, level, player, rr -> item.activateSecondaryFunction(level, player, rr) }
                        ,leftClientCallback = { item -> item.shipInfo = null; item.refreshHUD() }
                    )
                }
            }
        }
    }
}