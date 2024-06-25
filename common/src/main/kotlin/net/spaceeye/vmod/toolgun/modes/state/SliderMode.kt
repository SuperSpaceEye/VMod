package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIContainer
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.types.ConnectionMConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.StripGUI
import net.spaceeye.vmod.toolgun.modes.hud.StripHUD
import net.spaceeye.vmod.toolgun.modes.inputHandling.StripCRIH
import net.spaceeye.vmod.toolgun.modes.serializing.StripSerializable
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.ThreeClicksActivationSteps
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.utils.vs.posWorldToShip
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class SliderMode: BaseMode {
    var posMode = PositionModes.CENTERED_IN_BLOCK
    var primaryFirstRaycast = false

    val conn_primary = register { object : C2SConnection<SliderMode>("slider_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<SliderMode>(context.player, buf, ::SliderMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

//        val dir = (rpoint1 - rpoint2).snormalize()

        val pspoint1 = posWorldToShip(ship1, rpoint2)
        val pspoint2 = posWorldToShip(ship2, rpoint1)

        val c11 = VSAttachmentConstraint(shipId1, shipId2, 1e-8, spoint1.toJomlVector3d(), pspoint2.toJomlVector3d(), 1e20, 10.0)
        val c12 = VSAttachmentConstraint(shipId1, shipId2, 1e-8, spoint1.toJomlVector3d(), pspoint2.toJomlVector3d(), 1e20, 0.0)

        val c21 = VSAttachmentConstraint(shipId1, shipId2, 1e-8, pspoint1.toJomlVector3d(), spoint2.toJomlVector3d(), 1e20, 10.0)
        val c22 = VSAttachmentConstraint(shipId1, shipId2, 1e-8, pspoint1.toJomlVector3d(), spoint2.toJomlVector3d(), 1e20, 0.0)


//        val c1 = VSAttachmentConstraint(shipId1, shipId2, 1e-20, spoint1.toJomlVector3d(), pspoint2.toJomlVector3d(), 1e20, 0.0)
//        val c2 = VSAttachmentConstraint(shipId1, shipId2, 1e-20, pspoint1.toJomlVector3d(), spoint2.toJomlVector3d(), 1e20, 0.0)

        level.shipObjectWorld.createNewConstraint(c11)
        level.shipObjectWorld.createNewConstraint(c12)

        level.shipObjectWorld.createNewConstraint(c21)
        level.shipObjectWorld.createNewConstraint(c22)

        resetState()
    }

    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        if (ClientToolGunState.TOOLGUN_RESET_KEY.matches(key, scancode)) {
            resetState()
            return EventResult.interruptFalse()
        }

        return EventResult.pass()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            primaryFirstRaycast = !primaryFirstRaycast
            conn_primary.sendToServer(this)
        }

        return EventResult.interruptFalse()
    }

    override fun serverSideVerifyLimits() {}

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeEnum(posMode)
        buf.writeBoolean(primaryFirstRaycast)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        posMode = buf.readEnum(posMode.javaClass)
        primaryFirstRaycast = buf.readBoolean()
    }

    override val itemName: TranslatableComponent
        get() = TranslatableComponent("Slider")

    override fun makeGUISettings(parentWindow: UIContainer) {
    }

    override fun resetState() {
        previousResult = null
        primaryFirstRaycast = false
    }
}