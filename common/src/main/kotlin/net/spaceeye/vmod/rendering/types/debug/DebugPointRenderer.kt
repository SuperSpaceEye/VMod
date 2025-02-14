package net.spaceeye.vmod.rendering.types.debug

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.rendering.types.BaseRenderer
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils.Quad.drawQuad
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import org.lwjgl.opengl.GL11
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

class DebugPointRenderer(): BaseRenderer(), AutoSerializable, DebugRenderer {
    @JsonIgnore private var i = 0

    var shipId: Long by get(i++, -1L)
    var sPos: Vector3d by get(i++, Vector3d())

    constructor(shipId: ShipId, sPos: Vector3d): this() {
        this.shipId = shipId
        this.sPos = sPos
    }

    override fun renderData(
        poseStack: PoseStack,
        camera: Camera,
        timestamp: Long
    ) {
        val level = Minecraft.getInstance().level!!

        val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: return

        val rPos = posShipToWorldRender(ship, sPos)


        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.disableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(false)
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.enableBlend()

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        poseStack.pushPose()

        val cameraPos = Vector3d(camera.position)

        val color = Color(255, 0, 0)
        val light = Int.MAX_VALUE
        val width = 0.2

        val pos1 = Vector3d()
        val pos2 = rPos - cameraPos

        val matrix = poseStack.last().pose()


        val dir = (pos2 - pos1).normalize()
        val up = Vector3d(camera.upVector)

        val right = dir.cross(up)

        val lu =  up * width + -right * width + pos2
        val ld = -up * width + -right * width + pos2

        val rd = -up * width +  right * width + pos2
        val ru =  up * width +  right * width + pos2

        drawQuad(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, lu, ld, rd, ru)

        tesselator.end()
        poseStack.popPose()
    }

    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? {
        return null
    }

    override fun scaleBy(by: Double) {}
}