package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.constraints.*
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TextComponent
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import net.spaceeye.vmod.toolgun.PlayerToolgunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.ServerClosable
import org.joml.Quaterniond
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.mod.common.getShipManagingPos
import java.awt.Color
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

object ClientPlayerSchematics {
    fun listSchematics(): List<Path> {
        try {
            Files.createDirectories(Paths.get("VMod-Schematics"))
        } catch (e: IOException) {return emptyList()}
        val files = Files.list(Paths.get("VMod-Schematics"))
        return files.filter { !it.isDirectory() }.toList()
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
    private val schematics = mutableMapOf<UUID, IShipSchematic>()

    override fun close() {
        schematics.clear()
    }
}

object SchemNetworking: BaseNetworking<SchemMode>() {
    val networkName = "schem_networking"

    val c2sSaveSchematic = "save_schematic" idWithConnc {
        object : C2SConnection<C2SSaveSchematic>(it, networkName) {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                serverObj!!.saveSchem(ClientPlayerSchematics.listSchematics())
            }
        }
    }

    val c2sLoadSchematic = "load_schematic" idWithConnc {
        object : C2SConnection<C2SLoadSchematic>(it, networkName) {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val player = context.player
                val constructor = {SchemMode()}

                var serverMode = ServerToolGunState.playersStates.getOrPut(player.uuid) { PlayerToolgunState(constructor()) }
                if (serverMode.mode !is SchemMode) { serverMode = PlayerToolgunState(constructor()); ServerToolGunState.playersStates[player.uuid] = serverMode }
                serverMode.mode.init(EnvType.Server)

                val pkt = C2SLoadSchematic(buf)
                val schem = ClientPlayerSchematics.loadSchematic(pkt.path)

                if (schem != null) {
                    Minecraft.getInstance().player!!.sendMessage(TextComponent("Schem Loaded"), Minecraft.getInstance().player!!.uuid)
                }

                serverObj!!.schem = schem
            }
        }
    }

    class C2SLoadSchematic(): Serializable {
        lateinit var path: Path

        constructor(path: Path): this() {this.path = path}
        constructor(buf: FriendlyByteBuf): this() {deserialize(buf)}

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()
            buf.writeUtf(path.toString())
            return buf
        }

        override fun deserialize(buf: FriendlyByteBuf) {
            path = Path(buf.readUtf())
        }
    }

    class C2SSaveSchematic(): Serializable {
        override fun serialize(): FriendlyByteBuf { return getBuffer() }
        override fun deserialize(buf: FriendlyByteBuf) {}
    }
}

class SchemMode: BaseMode {
    override fun serverSideVerifyLimits() {}
    override fun serialize(): FriendlyByteBuf { return getBuffer() }
    override fun deserialize(buf: FriendlyByteBuf) {}

    override fun init(type: BaseNetworking.EnvType) {
        SchemNetworking.init(this, type)
    }

    override val itemName: TranslatableComponent
        get() = TranslatableComponent("Schem")

    fun saveSchem(items: List<Path>) {
        var name = "new_schematic_1"

        val names = items.map { it.fileName.toString() }

        while (names.contains(name)) {
            name += "1"
        }

        ClientPlayerSchematics.saveSchematic(name, schem!!)
    }

    override fun makeGUISettings(parentWindow: UIBlock) {
        val paths = ClientPlayerSchematics.listSchematics()

        Button(Color.GRAY.brighter(), "Save") {
            SchemNetworking.c2sSaveSchematic.sendToServer(SchemNetworking.C2SSaveSchematic())
        }.constrain {
            x = 0.pixels()
            y = 0.pixels()

            width = ChildBasedSizeConstraint() + 4.pixels()
            height = ChildBasedSizeConstraint() + 4.pixels()
        } childOf parentWindow

        val itemsScroll = ScrollComponent().constrain {
            x = 1f.percent()
            y = SiblingConstraint() + 2.pixels()

            width = 98.percent()
            height = 98.percent()
        } childOf parentWindow

        for (path in paths) {
            val block = UIBlock().constrain {
                x = 0f.pixels()
                y = SiblingConstraint()

                width = 100.percent()
                height = ChildBasedMaxSizeConstraint() + 2.pixels()
            } childOf itemsScroll

            Button(Color.GRAY.brighter(), "Load") {
                SchemNetworking.c2sLoadSchematic.sendToServer(SchemNetworking.C2SLoadSchematic(path))
            }.constrain {
                x = 0.pixels()
                y = 0.pixels()

                width = ChildBasedSizeConstraint() + 4.pixels()
                height = ChildBasedSizeConstraint() + 4.pixels()
            } childOf block

            UIText(path.fileName.toString(), false).constrain {
                x = SiblingConstraint() + 2.pixels()
                y = 2.pixels()

                textScale = 1.pixels()
                color = Color.BLACK.toConstraint()
            } childOf block
        }
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            conn_secondary.sendToServer(this)
        }

        ClientPlayerSchematics.listSchematics()

        return EventResult.interruptFalse()
    }

    val conn_primary = register { object : C2SConnection<SchemMode>("schem_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SchemMode>(context.player, buf, ::SchemMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }
    val conn_secondary = register { object : C2SConnection<SchemMode>("schem_mode_secondary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SchemMode>(context.player, buf, ::SchemMode) { item, serverLevel, player, raycastResult -> item.activateSecondaryFunction(serverLevel, player, raycastResult) } } }

    var schem: IShipSchematic? = null

    fun activatePrimaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {resetState(); return}
        player as ServerPlayer

        val serverCaughtShip = level.getShipManagingPos(raycastResult.blockPosition) ?: return
        val schem = ShipSchematic.getSchematicConstructor().get()
        schem.makeFrom(serverCaughtShip)
        this.schem = schem
//        val file = schem.saveToFile()
//        WLOG("${(file as CompoundTagIFile).tag}")
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        val schem = schem ?: return
        if (raycastResult.state.isAir) {return}

        schem.placeAt(level, raycastResult.worldCenteredHitPos!!.toJomlVector3d(), Quaterniond())


//        val file = this.schem!!.saveToFile()
//        val bytes = file.toBytes()
//
//        val out = FileOutputStream("cool_ship_schem.nbt")
//        out.write(this.schem!!.saveToFile().toBytes().array())
//        out.close()
//
//        val newFile = CompoundTagIFile(CompoundTag())
//        newFile.fromBytes(bytes)
//        val loadedSchem = ShipSchematic.getSchematicConstructor().get()
//        loadedSchem.loadFromByteBuffer(newFile)
//
//        loadedSchem.placeAt(level, raycastResult.worldCenteredHitPos!!.toJomlVector3d(), Quaterniond())
    }
}