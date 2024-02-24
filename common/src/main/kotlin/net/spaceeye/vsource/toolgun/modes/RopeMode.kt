package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.gui.makeTextEntry
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.rendering.types.RopeRenderer
import net.spaceeye.vsource.utils.RaycastFunctions
import net.spaceeye.vsource.constraintsSaving.makeManagedConstraint
import net.spaceeye.vsource.translate.GUIComponents.COMPLIANCE
import net.spaceeye.vsource.translate.GUIComponents.FIXED_DISTANCE
import net.spaceeye.vsource.translate.GUIComponents.MAX_FORCE
import net.spaceeye.vsource.translate.GUIComponents.ROPE
import net.spaceeye.vsource.translate.get
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class RopeMode : BaseMode {
    var compliance = 1e-10
    var maxForce = 1e10
    var fixedDistance = -1.0

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
        buf.writeDouble(fixedDistance)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        fixedDistance = buf.readDouble()
    }

    override val itemName = ROPE
    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 2.0f

        makeTextEntry(COMPLIANCE.get(),     compliance,    offset, offset, parentWindow, 0.0) {compliance = it}
        makeTextEntry(MAX_FORCE.get(),      maxForce,      offset, offset, parentWindow, 0.0) {maxForce   = it}
        makeTextEntry(FIXED_DISTANCE.get(), fixedDistance, offset, offset, parentWindow) {fixedDistance = it}
    }

    val conn_primary = register { object : C2SConnection<RopeMode>("rope_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<RopeMode>(context.player, buf, ::RopeMode, ::activatePrimaryFunction) } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = activateFunction(level, player, raycastResult, ::previousResult, ::resetState) {
        level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2 ->

        val dist = if (fixedDistance > 0) {fixedDistance} else {(rpoint1 - rpoint2).dist()}

        val constraint = VSRopeConstraint(
            shipId1, shipId2,
            1e-10,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            1e10,
            dist
        )

        val id = level.makeManagedConstraint(constraint)

        SynchronisedRenderingData.serverSynchronisedData
            .addConstraintRenderer(ship1, shipId1, shipId2, id!!.id,
                RopeRenderer(
                ship1 != null,
                ship2 != null,
                spoint1, spoint2,
                dist
            ))

        resetState()
    }

    fun resetState() {
        ILOG("RESETTING STATE")
        previousResult = null
    }
}