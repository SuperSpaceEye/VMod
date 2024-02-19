package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.ILOG
import net.spaceeye.vsource.gui.TextEntry
import net.spaceeye.vsource.networking.C2SConnection
import net.spaceeye.vsource.toolgun.ServerToolGunState
import net.spaceeye.vsource.utils.RaycastFunctions
import net.spaceeye.vsource.utils.ServerLevelHolder
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.utils.constraintsSaving.makeManagedConstraint
import net.spaceeye.vsource.utils.posShipToWorld
import org.joml.primitives.AABBi
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class AABBWeldMode : BaseMode {
    var compliance = 1e-10
    var maxForce = 1e10

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

    override val itemName: String = "AABB Weld"
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
        object : C2SConnection<AABBWeldMode>("aabb_weld_mode", "toolgun_command") {
            override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                val player = context.player
                val level = ServerLevelHolder.serverLevel!!

                var serverMode = ServerToolGunState.playerStates.getOrPut(player) {AABBWeldMode()}
                if (serverMode !is AABBWeldMode) { serverMode = AABBWeldMode(); ServerToolGunState.playerStates[player] = serverMode }
                serverMode.deserialize(buf)

                val raycastResult = RaycastFunctions.raycast(level, player, 100.0)
                serverMode.activatePrimaryFunction(level, player, raycastResult)
            }
        }
    }

    var blockPos: BlockPos? = null

    fun resetState() {
        ILOG("RESETTING STATE")
        blockPos = null
    }

    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (level.isClientSide) {return}
        if (level !is ServerLevel) {return}

        if (blockPos == null) {blockPos = raycastResult.blockPosition; return}
        if (blockPos == raycastResult.blockPosition) {resetState(); return}

        val ship1 = level.getShipManagingPos(blockPos!!)
        val ship2 = level.getShipManagingPos(raycastResult.blockPosition)

        if (ship1 == null && ship2 == null) { resetState(); return }
        if (ship1 == ship2) { resetState(); return }

        var shipId1: ShipId = ship1?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
        var shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        var ship1AttachmentPoints: MutableList<Vector3d> = mutableListOf()
        var ship2AttachmentPoints: MutableList<Vector3d> = mutableListOf()

        val saabbF = ship1?.shipAABB ?: AABBi(blockPos!!.x, blockPos!!.y, blockPos!!.z, blockPos!!.x+1, blockPos!!.y+1, blockPos!!.z+1)
        val saabbS = ship2?.shipAABB ?: AABBi(blockPos!!.x, blockPos!!.y, blockPos!!.z, blockPos!!.x+1, blockPos!!.y+1, blockPos!!.z+1)

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