package net.spaceeye.vsource.items

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.spaceeye.vsource.LOG
import net.spaceeye.vsource.networking.RenderingData
import net.spaceeye.vsource.networking.SynchronisedRenderingData
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.utils.constraintsSaving.makeManagedConstraint
import net.spaceeye.vsource.utils.dataSynchronization.ServerChecksumsUpdatedPacket
import net.spaceeye.vsource.utils.posShipToWorld
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

// TODO make rope ends actual ray hit positions and not just at the center of the blocks
class RopeCreatorItem: BaseTool() {
    var sBlockPos: BlockPos? = null
    var cBlockPos: BlockPos? = null

    override fun activatePrimaryFunction(level: Level, player: Player, clipResult: BlockHitResult) {
        if (level is ServerLevel) {
            if (sBlockPos == null) {sBlockPos = clipResult.blockPos; return}
            if (sBlockPos == clipResult.blockPos) {resetState(); return}
        } else if (level is ClientLevel) {
            if (cBlockPos == null) {cBlockPos = clipResult.blockPos; return}
            if (cBlockPos == clipResult.blockPos) {resetState(); return}
        }

        val scBlockPos = (if (sBlockPos != null) {sBlockPos} else {cBlockPos})!!

        val ship1 = level.getShipManagingPos(scBlockPos)
        val ship2 = level.getShipManagingPos(clipResult.blockPos)

        if (ship1 == null && ship2 == null) { resetState(); return }
        if (ship1 == ship2) { resetState(); return }

        val point1 = Vector3d(scBlockPos) + 0.5
        val point2 = Vector3d(clipResult.blockPos) + 0.5

        if (level is ServerLevel) {
            var shipId1: ShipId = ship1?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
            var shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

            val rpoint1 = if (ship1 == null) point1 else posShipToWorld(ship1, point1)
            val rpoint2 = if (ship2 == null) point2 else posShipToWorld(ship2, point2)

            val constraint = VSRopeConstraint(
                shipId1, shipId2,
                1e-10,
                point1.toJomlVector3d(), point2.toJomlVector3d(),
                1e10,
                (rpoint1 - rpoint2).dist()
            )

            val id = level.makeManagedConstraint(constraint)


            val server = SynchronisedRenderingData.serverSynchronisedData

            val data = RenderingData(
                ship1 != null,
                ship2 != null,
                point1, point2
            )

            val page = server.data.getOrPut(shipId1) { mutableMapOf() }
            page[id!!.id] = data

            server.serverChecksumsUpdatedConnection().sendToClients(level.players(), ServerChecksumsUpdatedPacket(
                shipId1, mutableListOf(Pair(id!!.id, data.hash()))
            ))

            sBlockPos = null
        }
//        else if (level is ClientLevel) {
//            RenderEvents.WORLD.register {
//                poseStack, camera ->
//                val rpoint1 = if (ship1 == null) point1 else posShipToWorldRender(ship1 as ClientShip, point1)
//                val rpoint2 = if (ship2 == null) point2 else posShipToWorldRender(ship2 as ClientShip, point2)
//
//                val tesselator = Tesselator.getInstance()
//                val vBuffer = tesselator.builder
//
//                RenderSystem.enableDepthTest()
//                RenderSystem.depthFunc(GL11.GL_LEQUAL)
//                RenderSystem.depthMask(true)
//
////                RenderSystem.disableDepthTest()
////                RenderSystem.disableBlend()
////                RenderSystem.disableCull()
////                RenderSystem.disableScissor()
//
//                vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)
//
//                poseStack.pushPose()
//
//                val cameraPos = Vector3d(camera.position)
//
//                val tpos1 = rpoint1 - cameraPos
//                val tpos2 = rpoint2 - cameraPos
//
//                val matrix = poseStack.last().pose()
//                RenderingUtils.Quad.makeFlatRectFacingCamera(vBuffer, matrix,
//                    255, 0, 0, 255, 255, .2,
//                    tpos1, tpos2)
//
//                tesselator.end()
//
//                poseStack.popPose()
//            }
//            cBlockPos = null
//        }
    }

    override fun resetState() {
        LOG("RESETTING STATE")
        sBlockPos = null
        cBlockPos = null
    }
}