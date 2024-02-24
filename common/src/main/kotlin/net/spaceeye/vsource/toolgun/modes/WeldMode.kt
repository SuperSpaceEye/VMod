package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.gui.makeTextEntry
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.rendering.types.A2BRenderer
import net.spaceeye.vsource.utils.*
import net.spaceeye.vsource.constraintsSaving.makeManagedConstraint
import net.spaceeye.vsource.translate.GUIComponents.COMPLIANCE
import net.spaceeye.vsource.translate.GUIComponents.MAX_FORCE
import net.spaceeye.vsource.translate.GUIComponents.WELD
import net.spaceeye.vsource.translate.get
import org.joml.Quaterniond
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.apigame.constraints.*
import java.awt.Color
import net.spaceeye.vsource.gui.makeDropDown
import net.spaceeye.vsource.translate.GUIComponents.CENTERED_IN_BLOCK
import net.spaceeye.vsource.translate.GUIComponents.CENTERED_ON_SIDE
import net.spaceeye.vsource.translate.GUIComponents.HITPOS_MODES
import net.spaceeye.vsource.translate.GUIComponents.NORMAL

//TODO REFACTOR

class WeldMode : BaseMode {
    var compliance:Double = 1e-10
    var maxForce: Double = 1e10

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

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
    }

    override val itemName = WELD
    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 2.0f

        makeTextEntry(COMPLIANCE.get(), ::compliance, offset, offset, parentWindow, 0.0)
        makeTextEntry(MAX_FORCE.get(),  ::maxForce,   offset, offset, parentWindow, 0.0)
        makeDropDown(HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            Pair(NORMAL.get()) { posMode = PositionModes.NORMAL },
            Pair(CENTERED_ON_SIDE.get()) { posMode = PositionModes.CENTERED_ON_SIDE },
            Pair(CENTERED_IN_BLOCK.get()) { posMode = PositionModes.CENTERED_IN_BLOCK },
        ))
    }

    val conn_primary = register { object : C2SConnection<WeldMode>("weld_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<WeldMode>(context.player, buf, ::WeldMode, ::activatePrimaryFunction) } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = tryActivateFunction(posMode, level, raycastResult, ::previousResult, ::resetState) {
        level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2 ->

        val attachmentConstraint = VSAttachmentConstraint(
            shipId1, shipId2,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce,
            (rpoint1 - rpoint2).dist()
        )

        val id = level.makeManagedConstraint(attachmentConstraint)

        SynchronisedRenderingData.serverSynchronisedData
            .addConstraintRenderer(ship1, shipId1, shipId2, id!!.id,
                A2BRenderer(
                    ship1 != null,
                    ship2 != null,
                    spoint1, spoint2,
                    Color(62, 62, 62)
                )
            )

        val dir = (rpoint1 - rpoint2).snormalize()

        val rpoint1 = rpoint1 + dir
        val rpoint2 = rpoint2 - dir

        val spoint1 = if (ship1 != null) posWorldToShip(ship1, rpoint1) else Vector3d(rpoint1)
        val spoint2 = if (ship2 != null) posWorldToShip(ship2, rpoint2) else Vector3d(rpoint2)

        val attachmentConstraint2 = VSAttachmentConstraint(
            shipId1, shipId2,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce,
            (rpoint1 - rpoint2).dist()
        )

        level.makeManagedConstraint(attachmentConstraint2)

        val rot1 = ship1?.transform?.shipToWorldRotation ?: Quaterniond()
        val rot2 = ship2?.transform?.shipToWorldRotation ?: Quaterniond()

        level.makeManagedConstraint(VSSphericalTwistLimitsConstraint(
            shipId1, shipId2, 1e-10, rot2, rot1, 1e200, 0.0, 0.01
        ))

        level.makeManagedConstraint(VSSphericalSwingLimitsConstraint(
            shipId1, shipId2, 1e-10, rot2, rot1, 1e200, 0.0, 0.01
        ))

        resetState()
    }

    fun resetState() {
        ILOG("RESETTING")
        previousResult = null
    }
}