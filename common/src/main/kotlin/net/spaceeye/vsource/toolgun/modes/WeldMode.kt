package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.WLOG
import net.spaceeye.vsource.gui.TextEntry
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.rendering.types.A2BRenderer
import net.spaceeye.vsource.toolgun.ServerToolGunState
import net.spaceeye.vsource.utils.*
import net.spaceeye.vsource.utils.constraintsSaving.makeManagedConstraint
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

//TODO REFACTOR

class WeldMode() : BaseMode {
    override val itemName = "Weld"

    var compliance:Double = 1e-10
    var maxForce: Double = 1e10

    override fun handleKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        return EventResult.pass()
    }

    override fun handleMouseButtonEvent(button: Int, action: Int, mods: Int): EventResult {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            conn.sendToServer(this)
        }

        return EventResult.pass()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
    }

    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 4

        var entry = TextEntry("Compliance") {
            compliance = it.toDoubleOrNull() ?: return@TextEntry
        }
            .constrain {
            x = offset.pixels()
            y = offset.pixels()

            width = 100.percent() - (offset * 2).pixels()
        } childOf parentWindow
        entry.textArea.setText(compliance.toString())

        entry = TextEntry("Max Force") {
            maxForce = it.toDoubleOrNull() ?: return@TextEntry
        }
            .constrain {
            x = offset.pixels()
            y = SiblingConstraint(2f)

            width = 100.percent() - (offset * 2).pixels()
        } childOf parentWindow
        entry.textArea.setText(maxForce.toString())
    }

    val conn = register {
        object : C2SConnection<WeldMode>("weld_mode", "toolgun_command") {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val player = context.player
                val level = ServerLevelHolder.serverLevel!!

                var serverMode = ServerToolGunState.playerStates.getOrPut(player) {WeldMode()}
                if (serverMode !is WeldMode) { serverMode = WeldMode(); ServerToolGunState.playerStates[player] = serverMode }
                serverMode.deserialize(buf)

                val raycastResult = RaycastFunctions.raycast(level, player, 100.0)
                serverMode.activatePrimaryFunction(level, player, raycastResult)
            }
        }
    }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (level !is ServerLevel) {return}

        if (previousResult == null) {previousResult = raycastResult; return}

        val ship1 = level.getShipManagingPos(previousResult!!.blockPosition)
        val ship2 = level.getShipManagingPos(raycastResult.blockPosition)

        if (ship1 == null && ship2 == null) { resetState(); return }
        if (ship1 == ship2) { resetState(); return }

        var shipId1: ShipId = ship1?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
        var shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        var spoint1 = previousResult!!.globalHitPos
        var spoint2 = raycastResult.globalHitPos

        var rpoint1 = if (ship1 == null) spoint1 else posShipToWorld(ship1, previousResult!!.globalHitPos)
        var rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, raycastResult.globalHitPos)

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

        rpoint1 = rpoint1 + dir
        rpoint2 = rpoint2 - dir

        spoint1 = if (ship1 != null) posWorldToShip(ship1, rpoint1) else Vector3d(rpoint1)
        spoint2 = if (ship2 != null) posWorldToShip(ship2, rpoint2) else Vector3d(rpoint2)

        val attachmentConstraint2 = VSAttachmentConstraint(
            shipId1, shipId2,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce,
            (rpoint1 - rpoint2).dist()
        )

        level.makeManagedConstraint(attachmentConstraint2)

        val rot1 = ship1?.transform?.shipToWorldRotation ?: ship2!!.transform.shipToWorldRotation
        val rot2 = ship2?.transform?.shipToWorldRotation ?: ship1!!.transform.shipToWorldRotation

        var rotConstraint: VSConstraint = VSRotDampingConstraint(
            shipId1, shipId2, 1e-10, rot1, rot2, 1e200, 1e200, VSRotDampingAxes.ALL_AXES)

        level.makeManagedConstraint(rotConstraint)

        resetState()
    }

    fun resetState() {
        ILOG("RESETTING")
        previousResult = null
    }
}