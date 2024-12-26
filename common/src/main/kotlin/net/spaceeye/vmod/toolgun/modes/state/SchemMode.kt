package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.containers.v1.ShipInfo
import net.spaceeye.valkyrien_ship_schematics.containers.v1.ShipSchematicInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematicInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.rendering.ClientRenderingData
import net.spaceeye.vmod.rendering.types.special.SchemOutlinesRenderer
import net.spaceeye.vmod.schematic.SchematicActionsQueue
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.gui.SchemGUI
import net.spaceeye.vmod.toolgun.modes.hud.SchemHUD
import net.spaceeye.vmod.toolgun.modes.state.ClientPlayerSchematics.SchemHolder
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.schematic.VModShipSchematicV1
import net.spaceeye.vmod.schematic.makeFrom
import net.spaceeye.vmod.schematic.placeAt
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.state.ServerPlayerSchematics.SendLoadRequest
import net.spaceeye.vmod.utils.*
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.core.util.readAABBi
import org.valkyrienskies.core.util.readQuatd
import org.valkyrienskies.core.util.writeAABBi
import org.valkyrienskies.core.util.writeQuatd
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

//TODO remove
//probably works? and if it doesn't, oh well
private fun bcGetSchematicFromBytes(bytes: ByteArray): IShipSchematic? {
    if (bytes[3].toInt() == 1) {
        val inst = VModShipSchematicV1()

        val buf = FriendlyByteBuf(Unpooled.wrappedBuffer(bytes))
        buf.readInt()

        inst.deserialize(buf)
        return inst
    } else {
        return ShipSchematic.getSchematicFromBytes(bytes)
    }
}

//TODO Add rate limit
object ClientPlayerSchematics {
    //TODO add handling logic for receiverDataTransmissionFailed
    var saveSchemStream = object : DataStream<SendSchemRequest, SchemHolder>(
        "save_schem_stream",
        NetworkManager.Side.S2C,
        NetworkManager.Side.C2S,
        0
    ) {
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

    var loadSchemStream = object : DataStream<ServerPlayerSchematics.SendLoadRequest, SchemHolder>(
        "load_schem_stream",
        NetworkManager.Side.C2S,
        NetworkManager.Side.C2S,
        VMConfig.CLIENT.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE
    ) {
        override fun requestPacketConstructor(buf: FriendlyByteBuf) = SendLoadRequest::class.constructor(buf)
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) { throw AssertionError("Invoked Receiver code on Transmitter side") }
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Client Load Schem Transmission Failed") }

        override fun transmitterRequestProcessor(req: ServerPlayerSchematics.SendLoadRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? {
            val res = ClientToolGunState.currentMode?.let { mode ->
                if (mode !is SchemMode) { return@let null }
                mode.schem?.let { SchemHolder(ShipSchematic.writeSchematicToBuffer(it)!!, req.requestUUID) }
            }
            return if (res != null) { Either.Left(res) } else { Either.Right(RequestFailurePkt()) }
        }
    }

    //TODO convert to AutoSerializable
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
        } catch (e: IOException) {return false}
        return true
    }
}

object ServerPlayerSchematics: ServerClosable() {
    var saveSchemStream = object : DataStream<ClientPlayerSchematics.SendSchemRequest, SchemHolder>(
        "save_schem_stream",
        NetworkManager.Side.S2C,
        NetworkManager.Side.S2C,
        VMConfig.SERVER.TOOLGUN.SCHEMATIC_PACKET_PART_SIZE
    ) {
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

    var loadSchemStream = object : DataStream<SendLoadRequest, SchemHolder>(
        "load_schem_stream",
        NetworkManager.Side.C2S,
        NetworkManager.Side.S2C,
        0
    ) {
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
        ServerPlayerSchematics.loadSchemStream.r2tRequestData.transmitData(ServerPlayerSchematics.SendLoadRequest(player.uuid), FakePacketContext(player))
    }

    val s2cSendShipInfo = regS2C<S2CSendShipInfo>("send_ship_info", networkName) {pkt ->
        val mode = ClientToolGunState.currentMode ?: return@regS2C
        if (mode !is SchemMode) { return@regS2C }
        mode.shipInfo = pkt.shipInfo
    }

    //TODO ?
    class S2CSendShipInfo(): Serializable {
        lateinit var shipInfo: IShipSchematicInfo

        constructor(info: IShipSchematicInfo): this() {shipInfo = info}
        constructor(buf: FriendlyByteBuf): this() {deserialize(buf)}

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeVector3d(Vector3d(shipInfo.maxObjectPos))
            buf.writeCollection(shipInfo.shipsInfo) { buf, item ->
                buf.writeVector3d(Vector3d(item.relPositionToCenter))
                buf.writeAABBi(item.shipAABB)
                buf.writeVector3d(Vector3d(item.positionInShip))
                buf.writeDouble(item.shipScale)
                buf.writeQuatd(item.rotation)
            }

            return buf
        }

        override fun deserialize(buf: FriendlyByteBuf) {
            val maxObjectEdge = buf.readVector3d().toJomlVector3d()
            val data = buf.readCollection({mutableListOf<ShipInfo>()}) {
                val relPositionToCenter = buf.readVector3d().toJomlVector3d()
                val shipBounds = buf.readAABBi()
                val posInShip = buf.readVector3d().toJomlVector3d()
                val shipScale = buf.readDouble()
                val rotation = buf.readQuatd()

                ShipInfo(0, relPositionToCenter, shipBounds, posInShip, shipScale, rotation)
            }

            shipInfo = ShipSchematicInfo(
                maxObjectEdge,
                data
            )
        }
    }
}

class SchemMode: ExtendableToolgunMode(), SchemGUI, SchemHUD {
    var rotationAngle: Ref<Double> by get(0, Ref(0.0), customSerialize = {it, buf -> buf.writeDouble((it).it)}, customDeserialize = {buf -> val rotationAngle = Ref(0.0) ; rotationAngle.it = buf.readDouble(); rotationAngle})

    override var itemsScroll: ScrollComponent? = null
    override lateinit var parentWindow: UIContainer

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

    var renderer: SchemOutlinesRenderer? = null

    private var rID = -1
    private var shipInfo_: IShipSchematicInfo? = null
    var shipInfo: IShipSchematicInfo?
        get() = shipInfo_
        set(info) {
            shipInfo_ = info
            if (info == null) {
                renderer = null
                ClientRenderingData.removeClientsideRenderer(rID)
                return
            }


            val center = ShipTransformImpl(JVector3d(), JVector3d(), Quaterniond(), JVector3d(1.0, 1.0, 1.0))

            val data = info.shipsInfo.map {
                Pair(ShipTransformImpl(
                    it.relPositionToCenter,
                    it.positionInShip,
                    it.rotation,
                    JVector3d(it.shipScale, it.shipScale, it.shipScale)
                ), it.shipAABB)
            }

            renderer = SchemOutlinesRenderer(Vector3d(info.maxObjectPos), rotationAngle, center, data)

            rID = ClientRenderingData.addClientsideRenderer(renderer!!)

            refreshHUD()
        }
    private var schem_: IShipSchematic? = null
    var schem: IShipSchematic?
        get() = schem_
        set(value) {schem_ = value; shipInfo = value?.info}

    var filename = ""

    var scrollAngle = Math.toRadians(10.0)

    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {
            resetState();
            ServerPlayerSchematics.schematics.remove(player.uuid)
            return}
        player as ServerPlayer
        // TODO
        if (SchematicActionsQueue.uuidIsQueuedInSomething(player.uuid)) {return}

        val serverCaughtShip = level.getShipManagingPos(raycastResult.blockPosition) ?: run {
            resetState();
            ServerPlayerSchematics.schematics.remove(player.uuid)
            null
        } ?: return
        val schem = VModShipSchematicV1()
        schem.makeFrom(player.level() as ServerLevel, player.uuid, serverCaughtShip) {
            SchemNetworking.s2cSendShipInfo.sendToClient(player, SchemNetworking.S2CSendShipInfo(schem.info!!))
            ServerPlayerSchematics.schematics[player.uuid] = schem
        }
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val schem = ServerPlayerSchematics.schematics[player.uuid] ?: return
        if (raycastResult.state.isAir) {return}
        //TODO
        if (SchematicActionsQueue.uuidIsQueuedInSomething(player.uuid)) {return}

        val info = schem.info!!

        val hitPos = raycastResult.worldHitPos!!
        val pos = hitPos + (raycastResult.worldNormalDirection!! * info.maxObjectPos.y)

        val rotation = Quaterniond()
            .mul(Quaterniond(AxisAngle4d(rotationAngle.it, raycastResult.worldNormalDirection!!.toJomlVector3d())))
            .mul(getQuatFromDir(raycastResult.worldNormalDirection!!))
            .normalize()

        (schem as IShipSchematicDataV1).placeAt(level, player.uuid, pos.toJomlVector3d(), rotation) {}
    }

    override fun eResetState() {
//        schem = null
//        shipInfo = null
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
                        ,primaryFunction       = { item, level, player, rr -> item.activatePrimaryFunction(level, player, rr) }
                        ,secondaryFunction     = { item, level, player, rr -> item.activateSecondaryFunction(level, player, rr) }
                        ,primaryClientCallback = { item -> item.shipInfo = null; item.refreshHUD() }
                    )
                }
            }
        }
    }
}