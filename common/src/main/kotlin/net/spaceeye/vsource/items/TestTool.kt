package net.spaceeye.vsource.items

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.spaceeye.vsource.rendering.RenderEvents
import net.spaceeye.vsource.rendering.RenderingUtils
import net.spaceeye.vsource.utils.CARenderType
import net.spaceeye.vsource.utils.Vector3d

class TestTool: BaseTool() {
    override fun activatePrimaryFunction(level: Level, player: Player, clipResult: BlockHitResult) {
        when (level) {
            is ClientLevel -> {
                RenderEvents.WORLD.register {
                    poseStack, buffer, camera ->
                    val ivertexBuilder = buffer.getBuffer(CARenderType.WIRE)
                    val pos = Vector3d(clipResult.blockPos)

                    poseStack.pushPose()

                    val tpos = pos - Vector3d(camera.position)
                    poseStack.translate(tpos.x, tpos.y, tpos.z)

                    val matrix = poseStack.last().pose()

                    val b1 = Vector3d(0, 0, 0)
                    val b2 = Vector3d(0, 1, 0)
                    val b3 = Vector3d(1, 1, 0)
                    val b4 = Vector3d(1, 0, 0)

                    val t1 = Vector3d(0, 0, 1)
                    val t2 = Vector3d(0, 1, 1)
                    val t3 = Vector3d(1, 1, 1)
                    val t4 = Vector3d(1, 0, 1)

                    RenderingUtils.Quad.makeBoxTube(ivertexBuilder, matrix, 255, 0, 0, 255, 255, b1, b2, b3, b4, t1, t2, t3, t4)

                    poseStack.popPose()
                }
            }
            is ServerLevel -> {
            }
        }

    }

    override fun resetState() {}
}