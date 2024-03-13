package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.constraintsManaging.addFor
import net.spaceeye.vsource.constraintsManaging.getAllManagedConstraintIdsOfShipId
import net.spaceeye.vsource.guiElements.makeTextEntry
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.rendering.types.A2BRenderer
import net.spaceeye.vsource.utils.*
import net.spaceeye.vsource.constraintsManaging.makeManagedConstraint
import net.spaceeye.vsource.constraintsManaging.removeManagedConstraint
import net.spaceeye.vsource.constraintsManaging.types.WeldMConstraint
import net.spaceeye.vsource.guiElements.DItem
import net.spaceeye.vsource.translate.GUIComponents.COMPLIANCE
import net.spaceeye.vsource.translate.GUIComponents.MAX_FORCE
import net.spaceeye.vsource.translate.GUIComponents.WELD
import net.spaceeye.vsource.translate.get
import org.lwjgl.glfw.GLFW
import java.awt.Color
import net.spaceeye.vsource.guiElements.makeDropDown
import net.spaceeye.vsource.limits.DoubleLimit
import net.spaceeye.vsource.limits.ServerLimits
import net.spaceeye.vsource.translate.GUIComponents.CENTERED_IN_BLOCK
import net.spaceeye.vsource.translate.GUIComponents.CENTERED_ON_SIDE
import net.spaceeye.vsource.translate.GUIComponents.FIXED_DISTANCE
import net.spaceeye.vsource.translate.GUIComponents.HITPOS_MODES
import net.spaceeye.vsource.translate.GUIComponents.NORMAL
import net.spaceeye.vsource.translate.GUIComponents.STRIP
import net.spaceeye.vsource.translate.GUIComponents.WIDTH
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

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return}
        level as ServerLevel
        val ship = level.getShipManagingPos(raycastResult.blockPosition) ?: return
        level.getAllManagedConstraintIdsOfShipId(ship.id).forEach { level.removeManagedConstraint(it) }
    }

    fun resetState() {
        ILOG("RESETTING")
        previousResult = null
    }
}