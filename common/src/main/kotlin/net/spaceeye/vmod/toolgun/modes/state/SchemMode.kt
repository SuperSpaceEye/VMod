package net.spaceeye.vmod.toolgun.modes.state

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
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.rendering.ClientRenderingData
import net.spaceeye.vmod.rendering.types.special.SchemOutlinesRenderer
import net.spaceeye.vmod.schematic.SchematicActionsQueue
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.containers.ShipInfo
import net.spaceeye.vmod.schematic.containers.ShipSchematicInfo
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IShipSchematicInfo
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.gui.SchemGUI
import net.spaceeye.vmod.toolgun.modes.hud.SchemHUD
import net.spaceeye.vmod.toolgun.modes.eventsHandling.SchemCEH
import net.spaceeye.vmod.toolgun.modes.state.ClientPlayerSchematics.SchemHolder
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.networking.SerializableItem.get
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

const val SCHEM_EXTENSION = "vschem"

//TODO Add rate limit
object ClientPlayerSchematics {
    //TODO add handling logic for receiverDataTransmissionFailed
    var saveSchemStream = object : DataStream<SendSchemRequest, SchemHolder>(
        "save_schem_stream",
        NetworkManager.Side.S2C,
        NetworkManager.Side.C2S,
        0
    ) {
        override fun requestPacketConstructor() = SendSchemRequest()
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Transmission Failed") }
        override fun transmitterRequestProcessor(req: SendSchemRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? { throw AssertionError("Invoked Transmitter code on Receiver side") }

        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) {
            ClientToolGunState.currentMode?.let {
                if (it !is SchemMode) {return@let}
                it.schem = ShipSchematic.getSchematicFromBytes(data!!.data.array())
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
        override fun requestPacketConstructor() = ServerPlayerSchematics.SendLoadRequest()
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) { throw AssertionError("Invoked Receiver code on Transmitter side") }
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Transmission Failed") }

        override fun transmitterRequestProcessor(req: ServerPlayerSchematics.SendLoadRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? {
            val res = ClientToolGunState.currentMode?.let { mode ->
                if (mode !is SchemMode) { return@let null }
                mode.schem?.let { SchemHolder(it.serialize().serialize(), req.requestUUID) }
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
            Files.write(Paths.get("VMod-Schematics/${name}"), schematic.serialize().serialize().array())
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
        override fun requestPacketConstructor() = ClientPlayerSchematics.SendSchemRequest()
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Transmission Failed") }
        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) { throw AssertionError("Invoked Receiver code on Transmitter side") }
        override fun uuidHasAccess(uuid: UUID): Boolean { return ServerToolGunState.playerHasAccess(ServerLevelHolder.server!!.playerList.getPlayer(uuid) ?: return false) }

        override fun transmitterRequestProcessor(req: ClientPlayerSchematics.SendSchemRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? {
            val res = schematics[req.uuid] ?.let { SchemHolder(it.serialize().serialize()) }
            return if (res != null) { Either.Left(res) } else { Either.Right(RequestFailurePkt()) }
        }
    }

    var loadSchemStream = object : DataStream<SendLoadRequest, SchemHolder>(
        "load_schem_stream",
        NetworkManager.Side.C2S,
        NetworkManager.Side.S2C,
        0
    ) {
        override fun requestPacketConstructor() = SendLoadRequest()
        override fun dataPacketConstructor() = SchemHolder()
        override fun receiverDataTransmissionFailed(failurePkt: RequestFailurePkt) { ELOG("Transmission Failed") }
        override fun transmitterRequestProcessor(req: SendLoadRequest, ctx: NetworkManager.PacketContext): Either<SchemHolder, RequestFailurePkt>? { throw AssertionError("Invoked Transmitter code on Receiver side") }
        override fun uuidHasAccess(uuid: UUID): Boolean { return ServerToolGunState.playerHasAccess(ServerLevelHolder.server!!.playerList.getPlayer(uuid) ?: return false) }

        override fun receiverDataTransmitted(uuid: UUID, data: SchemHolder?) {
            schematics[data!!.uuid] = ShipSchematic.getSchematicFromBytes(data.data.array())
            loadRequests.remove(data.uuid)
        }
    }

    val loadRequests = mutableMapOf<UUID, Long>()

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
    val c2sLoadSchematic = "load_schematic" idWithConnc {
        object : C2SConnection<EmptyPacket>(it, networkName) {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val lastReq = ServerPlayerSchematics.loadRequests[context.player.uuid]
                if (lastReq != null && getNow_ms() - lastReq < 10000L) { return }

                ServerPlayerSchematics.loadRequests[context.player.uuid] = getNow_ms()
                ServerPlayerSchematics.loadSchemStream.r2tRequestData.transmitData(ServerPlayerSchematics.SendLoadRequest(context.player.uuid), context)
            }
        }
    }

    val s2cSendShipInfo = "send_ship_info" idWithConns {
        object : S2CConnection<S2CSendShipInfo>(it, networkName) {
            override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val mode = ClientToolGunState.currentMode ?: return
                if (mode !is SchemMode) {return}
                val pkt = S2CSendShipInfo(buf)
                mode.shipInfo = pkt.shipInfo
            }
        }
    }

    class S2CSendShipInfo(): Serializable {
        lateinit var shipInfo: IShipSchematicInfo

        constructor(info: IShipSchematicInfo): this() {shipInfo = info}
        constructor(buf: FriendlyByteBuf): this() {deserialize(buf)}

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeVector3d(Vector3d(shipInfo.maxObjectEdge))
            buf.writeCollection(shipInfo.shipInfo) { buf, item ->
                buf.writeVector3d(Vector3d(item.relPositionToCenter))
                buf.writeAABBi(item.shipBounds)
                buf.writeVector3d(Vector3d(item.posInShip))
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

class SchemMode: BaseMode, SchemGUI, SchemCEH, SchemHUD {
    var rotationAngle: Ref<Double> by get(0, Ref(0.0), customSerialize = {it, buf -> buf.writeDouble((it as Ref<Double>).it)}, customDeserialize = {buf -> rotationAngle.it = buf.readDouble(); rotationAngle})

    val conn_primary = register { object : C2SConnection<SchemMode>("schem_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SchemMode>(context.player, buf, ::SchemMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }
    val conn_secondary = register { object : C2SConnection<SchemMode>("schem_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SchemMode>(context.player, buf, ::SchemMode) { item, serverLevel, player, raycastResult -> item.activateSecondaryFunction(serverLevel, player, raycastResult) } } }

    override var itemsScroll: ScrollComponent? = null
    override lateinit var parentWindow: UIContainer

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

            val data = info.shipInfo.map {
                Pair(ShipTransformImpl(
                    it.relPositionToCenter,
                    it.posInShip,
                    it.rotation,
                    JVector3d(it.shipScale, it.shipScale, it.shipScale)
                ), it.shipBounds)
            }

            renderer = SchemOutlinesRenderer(Vector3d(info.maxObjectEdge), rotationAngle, center, data)

            rID = ClientRenderingData.addClientsideRenderer(renderer!!)

            refreshHUD()
        }
    private var schem_: IShipSchematic? = null
    var schem: IShipSchematic?
        get() = schem_
        set(value) {schem_ = value; shipInfo = value?.getInfo()}

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
        val schem = ShipSchematic.getSchematicConstructor().get()
        RandomEvents.serverOnTick.on { (it), unregister ->
            schem.makeFrom(player.level() as ServerLevel, player.uuid, serverCaughtShip) {
                SchemNetworking.s2cSendShipInfo.sendToClient(player, SchemNetworking.S2CSendShipInfo(schem.getInfo()))
                ServerPlayerSchematics.schematics[player.uuid] = schem
            }
            unregister()
        }
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val schem = ServerPlayerSchematics.schematics[player.uuid] ?: return
        if (raycastResult.state.isAir) {return}
        //TODO
        if (SchematicActionsQueue.uuidIsQueuedInSomething(player.uuid)) {return}

        val info = schem.getInfo()

        val hitPos = raycastResult.worldHitPos!!
        val pos = hitPos + (raycastResult.worldNormalDirection!! * info.maxObjectEdge.y)

        val rotation = Quaterniond()
            .mul(Quaterniond(AxisAngle4d(rotationAngle.it, raycastResult.worldNormalDirection!!.toJomlVector3d())))
            .mul(getQuatFromDir(raycastResult.worldNormalDirection!!))
            .normalize()

        schem.placeAt(level, player.uuid, pos.toJomlVector3d(), rotation)
    }

    override fun resetState() {
//        schem = null
//        shipInfo = null
        rotationAngle.it = 0.0
    }
}