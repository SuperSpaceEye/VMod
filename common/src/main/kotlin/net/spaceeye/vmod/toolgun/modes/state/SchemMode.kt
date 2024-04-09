package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.containers.CompoundTagIFile
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.ServerClosable
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.mod.common.getShipManagingPos
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
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

class SchemMode: BaseMode {
    override fun serverSideVerifyLimits() {}
    override fun serialize(): FriendlyByteBuf { return getBuffer() }
    override fun deserialize(buf: FriendlyByteBuf) {}

    override val itemName: TranslatableComponent
        get() = TranslatableComponent("Schem")

    override fun makeGUISettings(parentWindow: UIBlock) {}

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
        val file = schem.saveToFile()
        WLOG("${(file as CompoundTagIFile).tag}")
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (schem == null) {return}
        if (raycastResult.state.isAir) {return}

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