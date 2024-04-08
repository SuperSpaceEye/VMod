package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.constraintsManaging.VSConstraintsKeeper
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IShipSchematicInfo
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.copyShipWithConnections
import org.joml.Quaterniond
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.mod.common.getShipManagingPos

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
    }

    fun activateSecondaryFunction(level: ServerLevel, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (schem == null) {return}
        if (raycastResult.state.isAir) {return}

        schem!!.placeAt(level, raycastResult.worldCenteredHitPos!!.toJomlVector3d(), Quaterniond())
    }
}