package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIContainer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.SliderMConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.util.*
import net.spaceeye.vmod.utils.RaycastFunctions
import org.lwjgl.glfw.GLFW

class SliderMode: BaseMode {
    var posMode = PositionModes.CENTERED_IN_BLOCK
    var precisePlacementAssistSideNum = 3

    var compliance: Double = 1e-20
    var maxForce: Double = 1e20

    val conn_primary = register { object : C2SConnection<SliderMode>("slider_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SliderMode>(context.player, buf, ::SliderMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    var shipRes1: RaycastFunctions.RaycastResult? = null
    var shipRes2: RaycastFunctions.RaycastResult? = null
    var axisRes1: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycastAndActivateFn(posMode, precisePlacementAssistSideNum, level, raycastResult) {
        level, shipId, ship, spoint, rpoint, rresult ->

        if (shipRes1 == null && rresult.ship == null) { return@serverRaycastAndActivateFn resetState() }
        val shipRes1 = shipRes1 ?: run {
            shipRes1 = rresult
            return@serverRaycastAndActivateFn
        }

        val shipRes2 = shipRes2 ?: run {
            shipRes2 = rresult
            if (shipRes1.shipId != shipRes2!!.shipId) { return@serverRaycastAndActivateFn resetState() }
            return@serverRaycastAndActivateFn
        }

        val axisRes1 = axisRes1 ?: run {
            axisRes1 = rresult
            return@serverRaycastAndActivateFn
        }

        val axisRes2 = rresult

        if (axisRes1.shipId != axisRes2.shipId) { return@serverRaycastAndActivateFn resetState() }

        val axisPair = getModePositions(posMode, axisRes1, axisRes2, precisePlacementAssistSideNum)
        val shipPair = getModePositions(posMode, shipRes1, shipRes2, precisePlacementAssistSideNum)

        level.makeManagedConstraint(SliderMConstraint(
            axisRes1.shipId, shipRes1.shipId,
            axisPair.first, axisPair.second, shipPair.first, shipPair.second,
            compliance, maxForce, setOf(
                axisRes1.blockPosition, shipRes1.blockPosition,
                axisRes2.blockPosition, shipRes2.blockPosition).toList(), null
        )).addFor(player)

        resetState()
    }

    override fun onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }

    override fun onMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun serverSideVerifyLimits() {}

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeEnum(posMode)
        buf.writeInt(precisePlacementAssistSideNum)
        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        posMode = buf.readEnum(posMode.javaClass)
        precisePlacementAssistSideNum = buf.readInt()
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
    }

    override val itemName = TranslatableComponent("Slider")

    override fun makeGUISettings(parentWindow: UIContainer) {
    }

    override fun resetState() {
        shipRes1 = null
        shipRes2 = null
        axisRes1 = null
    }
}