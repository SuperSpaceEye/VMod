package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.getAllManagedConstraintIdsOfShipId
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.constraintsManaging.removeManagedConstraint
import org.lwjgl.glfw.GLFW
import net.spaceeye.vmod.translate.GUIComponents.STRIP
import org.valkyrienskies.mod.common.getShipManagingPos

class StripMode : BaseMode {
    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        return EventResult.pass()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun serialize(): FriendlyByteBuf { return getBuffer() }
    override fun deserialize(buf: FriendlyByteBuf) {}
    override fun serverSideVerifyLimits() {}

    override val itemName = STRIP
    override fun makeGUISettings(parentWindow: UIBlock) {}

    val conn_primary = register { object : C2SConnection<StripMode>("strip_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<StripMode>(context.player, buf, ::StripMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return
        level.getAllManagedConstraintIdsOfShipId(ship.id).forEach { level.removeManagedConstraint(it) }
    }
}