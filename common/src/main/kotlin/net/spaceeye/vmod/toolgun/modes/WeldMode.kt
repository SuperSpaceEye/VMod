package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.ILOG
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.WeldMConstraint
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.translate.GUIComponents.COMPLIANCE
import net.spaceeye.vmod.translate.GUIComponents.MAX_FORCE
import net.spaceeye.vmod.translate.GUIComponents.WELD
import net.spaceeye.vmod.translate.get
import org.lwjgl.glfw.GLFW
import java.awt.Color
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.translate.GUIComponents.CENTERED_IN_BLOCK
import net.spaceeye.vmod.translate.GUIComponents.CENTERED_ON_SIDE
import net.spaceeye.vmod.translate.GUIComponents.FIXED_DISTANCE
import net.spaceeye.vmod.translate.GUIComponents.HITPOS_MODES
import net.spaceeye.vmod.translate.GUIComponents.NORMAL
import net.spaceeye.vmod.translate.GUIComponents.WIDTH

class WeldMode : BaseMode {
    var compliance: Double = 1e-20
    var maxForce: Double = 1e10
    var width: Double = .2

    var fixedDistance: Double = -1.0

    var posMode = PositionModes.NORMAL

    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        return EventResult.pass()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeEnum(posMode)
        buf.writeDouble(width)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
    }

    override fun serverSideVerifyLimits() {
        compliance = ServerLimits.instance.compliance.get(compliance)
        maxForce = ServerLimits.instance.maxForce.get(maxForce)
        fixedDistance = ServerLimits.instance.fixedDistance.get(fixedDistance)
    }

    override val itemName = WELD
    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(COMPLIANCE.get(), ::compliance, offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),  ::maxForce,   offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(WIDTH.get(),      ::width,      offset, offset, parentWindow, DoubleLimit(0.0, 1.0))
        makeTextEntry(FIXED_DISTANCE.get(), ::fixedDistance, offset, offset, parentWindow)
        makeDropDown(HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE },
            DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK },
        ))
    }

    val conn_primary = register { object : C2SConnection<WeldMode>("weld_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<WeldMode>(context.player, buf, ::WeldMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverTryActivateFunction(posMode, level, raycastResult, ::previousResult, ::resetState) {
        level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        level.makeManagedConstraint(WeldMConstraint(
            spoint1, spoint2, rpoint1, rpoint2,
            ship1, ship2, shipId1, shipId2,
            compliance, maxForce,
            fixedDistance,
            listOf(prresult.blockPosition, rresult.blockPosition),
            A2BRenderer(
                ship1 != null,
                ship2 != null,
                spoint1, spoint2,
                Color(62, 62, 62),
                width
            )
        )).addFor(player)

        resetState()
    }

    fun resetState() {
        previousResult = null
    }
}