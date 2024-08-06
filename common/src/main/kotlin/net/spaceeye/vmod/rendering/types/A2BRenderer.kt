package net.spaceeye.vmod.rendering.types

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.constraintsManaging.getCenterPos
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

//TODO move
fun updatePosition(old: Vector3d, newShip: Ship): Vector3d = old - Vector3d(getCenterPos(old.x.toInt(), old.z.toInt())) + Vector3d(getCenterPos(newShip.transform.positionInShip.x().toInt(), newShip.transform.positionInShip.z().toInt()))

open class A2BRenderer(): BaseRenderer {
    var shipId1 = -1L
    var shipId2 = -1L

    var point1: Vector3d = Vector3d()
    var point2: Vector3d = Vector3d()

    var color: Color = Color(0)

    var width: Double = .2

    constructor(shipId1: Long,
                shipId2: Long,
                point1: Vector3d,
                point2: Vector3d,
                color: Color,
                width: Double,
    ): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2
        this.point1 = point1
        this.point2 = point2
        this.color = color
        this.width = width
    }

    override val typeName = "A2BRenderer"

    override fun renderData(poseStack: PoseStack, camera: Camera) {
        val level = Minecraft.getInstance().level!!

        val ship1 = if (shipId1 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId1) ?: return } else null
        val ship2 = if (shipId2 != -1L) { level.shipObjectWorld.loadedShips.getById(shipId2) ?: return } else null

        val rpoint1 = if (ship1 == null) point1 else posShipToWorldRender(ship1, point1)
        val rpoint2 = if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.enableBlend()

        val light = Int.MAX_VALUE

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)

        poseStack.pushPose()

        val cameraPos = Vector3d(camera.position)

        val tpos1 = rpoint1 - cameraPos
        val tpos2 = rpoint2 - cameraPos

        val matrix = poseStack.last().pose()
        RenderingUtils.Quad.makeFlatRectFacingCamera(
            vBuffer, matrix,
            color.red, color.green, color.blue, color.alpha, light, width,
            tpos1, tpos2
        )

        tesselator.end()

        poseStack.popPose()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeColor(color)
        buf.writeLong(shipId1)
        buf.writeLong(shipId2)

        buf.writeDouble(width)

        buf.writeVector3d(point1)
        buf.writeVector3d(point2)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        color = buf.readColor()
        shipId1 = buf.readLong()
        shipId2 = buf.readLong()

        width = buf.readDouble()

        point1 = buf.readVector3d()
        point2 = buf.readVector3d()
    }

    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? {
        val spoint1 = if (shipId1 != -1L) {updatePosition(point1, oldToNew[shipId1]!!)} else {Vector3d(point1)}
        val spoint2 = if (shipId2 != -1L) {updatePosition(point2, oldToNew[shipId2]!!)} else {Vector3d(point2)}

        val newId1 = if (shipId1 != -1L) {oldToNew[shipId1]!!.id} else {-1}
        val newId2 = if (shipId2 != -1L) {oldToNew[shipId2]!!.id} else {-1}

        return A2BRenderer(newId1, newId2, spoint1, spoint2, color, width)
    }
}