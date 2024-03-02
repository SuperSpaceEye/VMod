package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.gui.makeTextEntry
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.utils.*
import net.spaceeye.vsource.constraintsManaging.makeManagedConstraint
import net.spaceeye.vsource.constraintsManaging.types.MuscleMConstraint
import net.spaceeye.vsource.gui.DItem
import net.spaceeye.vsource.translate.GUIComponents.COMPLIANCE
import net.spaceeye.vsource.translate.GUIComponents.MAX_FORCE
import net.spaceeye.vsource.translate.get
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.apigame.constraints.*
import net.spaceeye.vsource.gui.makeDropDown
import net.spaceeye.vsource.rendering.types.A2BRenderer
import net.spaceeye.vsource.translate.GUIComponents.CENTERED_IN_BLOCK
import net.spaceeye.vsource.translate.GUIComponents.CENTERED_ON_SIDE
import net.spaceeye.vsource.translate.GUIComponents.HITPOS_MODES
import net.spaceeye.vsource.translate.GUIComponents.NORMAL
import net.spaceeye.vsource.translate.GUIComponents.WIDTH
import java.awt.Color

class MuscleMode : BaseMode {
    var compliance: Double = 1e-10
    var maxForce: Double = 1e10
    var width: Double = .2

    var additionalDistance = 3.0
    var ticksToWork = 60

    var posMode = PositionModes.NORMAL

    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        return EventResult.pass()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptTrue()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeEnum(posMode)
        buf.writeDouble(width)
        buf.writeDouble(additionalDistance)
        buf.writeInt(ticksToWork)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
        additionalDistance = buf.readDouble()
        ticksToWork = buf.readInt()
    }

    override val itemName = TranslatableComponent("Muscle mode")
    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 2.0f

        makeTextEntry(COMPLIANCE.get(), ::compliance, offset, offset, parentWindow, 0.0)
        makeTextEntry(MAX_FORCE.get(),  ::maxForce,   offset, offset, parentWindow, 0.0)
        makeTextEntry(WIDTH.get(),      ::width,      offset, offset, parentWindow, 0.0, 1.0)

        makeTextEntry("Additional Distance", ::additionalDistance, offset, offset, parentWindow, 0.0)
        makeTextEntry("Ticks to Work", ::ticksToWork, offset, offset, parentWindow, 0)
        makeDropDown(HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE },
            DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK },
        ))
    }

    val conn_primary = register { object : C2SConnection<MuscleMode>("muscle_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<MuscleMode>(context.player, buf, ::MuscleMode, ::activatePrimaryFunction) } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverTryActivateFunction(posMode, level, raycastResult, ::previousResult, ::resetState) {
        level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2 ->

        level.makeManagedConstraint(MuscleMConstraint(
            spoint1, spoint2, rpoint1, rpoint2,
            ship1, ship2, shipId1, shipId2,
            compliance, maxForce,
            (rpoint1 - rpoint2).dist(),
            (rpoint1 - rpoint2).dist() + additionalDistance,
            ticksToWork,
            A2BRenderer(
                ship1 != null,
                ship2 != null,
                spoint1, spoint2,
                Color(62, 62, 200),
                width
            )
        ))

        resetState()
    }

    fun resetState() {
        ILOG("RESETTING")
        previousResult = null
    }
}