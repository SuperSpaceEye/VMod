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
import net.spaceeye.vsource.utils.RaycastFunctions
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.constraintsSaving.makeManagedConstraint
import net.spaceeye.vsource.translate.GUIComponents.AABB_WELD
import net.spaceeye.vsource.translate.GUIComponents.COMPLIANCE
import net.spaceeye.vsource.translate.GUIComponents.MAX_FORCE
import net.spaceeye.vsource.translate.get
import net.spaceeye.vsource.utils.posShipToWorld
import org.joml.primitives.AABBi
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint

class AABBWeldMode : BaseMode {
    var compliance = 1e-10
    var maxForce = 1e10

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

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
    }

    override val itemName = AABB_WELD
    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 2.0f

        makeTextEntry(COMPLIANCE.get(), compliance, offset, offset, parentWindow, 0.0) {compliance = it}
        makeTextEntry(MAX_FORCE.get(),  maxForce,   offset, offset, parentWindow, 0.0) {maxForce   = it}
    }

    val conn_primary = register { object : C2SConnection<AABBWeldMode>("aabb_weld_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<AABBWeldMode>(context.player, buf, ::AABBWeldMode, ::activatePrimaryFunction) } }

    var previousResult: RaycastFunctions.RaycastResult? = null

    fun resetState() {
        ILOG("RESETTING STATE")
        previousResult = null
    }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = activateFunction(level, player, raycastResult, ::previousResult, ::resetState) {
        level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2 ->

        val bpos = previousResult!!.blockPosition
        val ship1AttachmentPoints: MutableList<Vector3d> = mutableListOf()
        val ship2AttachmentPoints: MutableList<Vector3d> = mutableListOf()

        val saabbF = ship1?.shipAABB ?: AABBi(bpos.x, bpos.y, bpos.z, bpos.x+1, bpos.y+1, bpos.z+1)
        val saabbS = ship2?.shipAABB ?: AABBi(bpos.x, bpos.y, bpos.z, bpos.x+1, bpos.y+1, bpos.z+1)

        val aabbFA = mutableListOf(
            Vector3d(saabbF.minX(), saabbF.minY(), saabbF.minZ()),
            Vector3d(saabbF.maxX(), saabbF.minY(), saabbF.minZ()),
            Vector3d(saabbF.minX(), saabbF.minY(), saabbF.maxZ()),
            Vector3d(saabbF.maxX(), saabbF.minY(), saabbF.maxZ()),

            Vector3d(saabbF.minX(), saabbF.maxY(), saabbF.minZ()),
            Vector3d(saabbF.maxX(), saabbF.maxY(), saabbF.minZ()),
            Vector3d(saabbF.minX(), saabbF.maxY(), saabbF.maxZ()),
            Vector3d(saabbF.maxX(), saabbF.maxY(), saabbF.maxZ()),
        )

        val aabbSA = mutableListOf(
            Vector3d(saabbS.minX(), saabbS.minY(), saabbS.minZ()),
            Vector3d(saabbS.maxX(), saabbS.minY(), saabbS.minZ()),
            Vector3d(saabbS.minX(), saabbS.minY(), saabbS.maxZ()),
            Vector3d(saabbS.maxX(), saabbS.minY(), saabbS.maxZ()),

            Vector3d(saabbS.minX(), saabbS.maxY(), saabbS.minZ()),
            Vector3d(saabbS.maxX(), saabbS.maxY(), saabbS.minZ()),
            Vector3d(saabbS.minX(), saabbS.maxY(), saabbS.maxZ()),
            Vector3d(saabbS.maxX(), saabbS.maxY(), saabbS.maxZ()),
        )

        // diagonal connections
        ship1AttachmentPoints.add(aabbFA[0])
        ship2AttachmentPoints.add(aabbSA[7])

        ship1AttachmentPoints.add(aabbFA[1])
        ship2AttachmentPoints.add(aabbSA[6])

        ship1AttachmentPoints.add(aabbFA[2])
        ship2AttachmentPoints.add(aabbSA[5])

        ship1AttachmentPoints.add(aabbFA[3])
        ship2AttachmentPoints.add(aabbSA[4])

        // edge connections
        for ((point1, point2) in aabbFA.zip(aabbSA)) {
            ship1AttachmentPoints.add(point1)
            ship2AttachmentPoints.add(point2)
        }

        for ((point1, point2) in ship1AttachmentPoints.zip(ship2AttachmentPoints)) {
            val rpoint1 = if (ship1 == null) point1 else posShipToWorld(ship1, point1)
            val rpoint2 = if (ship2 == null) point2 else posShipToWorld(ship2, point2)

            val constraint = VSAttachmentConstraint(
                shipId1, shipId2,
                1e-10,
                point1.toJomlVector3d(), point2.toJomlVector3d(),
                1e10, (rpoint1 - rpoint2).dist()
            )
            level.makeManagedConstraint(constraint)
        }

        resetState()
    }
}