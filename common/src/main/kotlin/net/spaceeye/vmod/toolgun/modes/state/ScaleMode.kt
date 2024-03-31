package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.teleportShipWithConnected
import org.joml.Quaterniond
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.mod.common.getShipManagingPos

//TODO redo this
class ScaleMode: BaseMode {
    var scale = 1.0

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun serverSideVerifyLimits() {

    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeDouble(scale)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        scale = buf.readDouble()
    }

    override val itemName: TranslatableComponent
        get() = TranslatableComponent("Scale")

    override fun makeGUISettings(parentWindow: UIBlock) {
        makeTextEntry("Scale", ::scale, 2.0f, 2.0f, parentWindow, DoubleLimit(0.0))
    }

    val conn_primary = register { object : C2SConnection<ScaleMode>("scale_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<ScaleMode>(context.player, buf, ::ScaleMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return
        teleportShipWithConnected(level, ship, Vector3d(ship.transform.positionInWorld), Quaterniond(ship.transform.shipToWorldRotation), scale)
    }
}