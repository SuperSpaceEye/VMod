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
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.HydraulicsMConstraint
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.translate.GUIComponents.COMPLIANCE
import net.spaceeye.vmod.translate.GUIComponents.MAX_FORCE
import net.spaceeye.vmod.translate.get
import org.lwjgl.glfw.GLFW
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.limits.DoubleLimit
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.translate.GUIComponents.CENTERED_IN_BLOCK
import net.spaceeye.vmod.translate.GUIComponents.CENTERED_ON_SIDE
import net.spaceeye.vmod.translate.GUIComponents.CHANNEL
import net.spaceeye.vmod.translate.GUIComponents.EXTENSION_DISTANCE
import net.spaceeye.vmod.translate.GUIComponents.EXTENSION_SPEED
import net.spaceeye.vmod.translate.GUIComponents.HITPOS_MODES
import net.spaceeye.vmod.translate.GUIComponents.HYDRAULICS
import net.spaceeye.vmod.translate.GUIComponents.NORMAL
import net.spaceeye.vmod.translate.GUIComponents.WIDTH
import java.awt.Color

class HydraulicsMode : BaseMode {
    var compliance: Double = 1e-20
    var maxForce: Double = 1e10
    var width: Double = .2

    var extensionDistance: Double = 5.0
    var extensionSpeed: Double = 1.0

    var channel: String = "hydraulics"

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
        buf.writeDouble(extensionDistance)
        buf.writeDouble(extensionSpeed)
        buf.writeUtf(channel)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
        extensionDistance = buf.readDouble()
        extensionSpeed = buf.readDouble()
        channel = buf.readUtf()
    }

    override fun serverSideVerifyLimits() {
        compliance = ServerLimits.instance.compliance.get(compliance)
        maxForce = ServerLimits.instance.maxForce.get(maxForce)
        extensionDistance = ServerLimits.instance.extensionDistance.get(extensionDistance)
        extensionSpeed = ServerLimits.instance.extensionSpeed.get(extensionSpeed)
        channel = ServerLimits.instance.channelLength.get(channel)
    }

    override val itemName = HYDRAULICS
    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 2.0f
        val limits = ServerLimits.instance

        makeTextEntry(COMPLIANCE.get(), ::compliance, offset, offset, parentWindow, limits.compliance)
        makeTextEntry(MAX_FORCE.get(),  ::maxForce,   offset, offset, parentWindow, limits.maxForce)
        makeTextEntry(WIDTH.get(),      ::width,      offset, offset, parentWindow, DoubleLimit(0.0, 1.0))

        makeTextEntry(EXTENSION_DISTANCE.get(), ::extensionDistance, offset, offset, parentWindow, DoubleLimit())
        makeTextEntry(EXTENSION_SPEED.get(), ::extensionSpeed, offset, offset, parentWindow, limits.extensionSpeed)
        makeTextEntry(CHANNEL.get(), ::channel, offset, offset, parentWindow, limits.channelLength)
        makeDropDown(HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
            DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL },
            DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE },
            DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK },
        ))
    }

    val conn_primary = register { object : C2SConnection<HydraulicsMode>("hydraulics_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<HydraulicsMode>(context.player, buf, ::HydraulicsMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverTryActivateFunction(posMode, level, raycastResult, ::previousResult, ::resetState) {
        level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val dist = (rpoint1 - rpoint2).dist()
        level.makeManagedConstraint(HydraulicsMConstraint(
            spoint1, spoint2, rpoint1, rpoint2,
            ship1, ship2, shipId1, shipId2,
            compliance, maxForce,
            dist,
            dist + extensionDistance,
            extensionSpeed,
            channel,
            listOf(prresult.blockPosition, rresult.blockPosition),
            A2BRenderer(
                ship1 != null,
                ship2 != null,
                spoint1, spoint2,
                Color(62, 62, 200),
                width
            )
        )).addFor(player)

        resetState()
    }

    fun resetState() {
        ILOG("RESETTING")
        previousResult = null
    }
}