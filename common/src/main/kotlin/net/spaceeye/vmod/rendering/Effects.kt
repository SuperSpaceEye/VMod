package net.spaceeye.vmod.rendering

import net.minecraft.world.entity.player.Player
import net.spaceeye.vmod.rendering.types.TimedA2BRenderer
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import java.awt.Color

object Effects {
    @JvmStatic
    fun rad(deg:Double) = deg * 0.017453292519943295

    //this looks "fine" from client that casted ray, but from another view it'll look like the ray originated
    // from player's head
    fun getRightSideClient(player: Player): Vector3d {
        val p = rad(player.xRot.toDouble()) // picth
        val y =-rad(player.yHeadRot.toDouble()) // yaw

        val up  = Vector3d(Math.sin(p) * Math.sin(y), Math.cos(p), Math.sin(p) * Math.cos(y))
        val dir = Vector3d(Math.cos(p) * Math.sin(y), -Math.sin(p), Math.cos(p) * Math.cos(y))
        val right = Vector3d(-Math.cos(y), 0, Math.sin(y))

        return Vector3d(player.eyePosition) - up * 0.1 + right * 0.25 + dir * 0.45
    }

    fun sendToolgunRayEffect(playerSource: Player, result: RaycastFunctions.RaycastResult, maxDistance: Double) {
        val endPos = result.worldHitPos ?: (result.origin + result.lookVec * maxDistance)
        RenderingData.server.addTimedRenderer(
            TimedA2BRenderer(
                getRightSideClient(playerSource),
                endPos,
                Color(0, 255, 255, 200),
                .05,
                getNow_ms(),
                200,
                result.origin
            )
        )
    }
}