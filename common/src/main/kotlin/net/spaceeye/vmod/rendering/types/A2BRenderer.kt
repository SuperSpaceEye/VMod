package net.spaceeye.vmod.rendering.types

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.AutoSerializable
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import net.spaceeye.vmod.utils.vs.updatePosition
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

open class A2BRenderer(): BaseRenderer {
    val state = State()
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

    class State: AutoSerializable {
        var shipId1: Long by get(0, -1L)
        var shipId2: Long by get(1, -1L)

        var point1: Vector3d by get(2, Vector3d())
        var point2: Vector3d by get(3, Vector3d())

        var color: Color by get(4, Color(0))

        var width: Double by get(5, .2)
    }
    inline var shipId1 get() = state.shipId1; set(value) {state.shipId1 = value}
    inline var shipId2 get() = state.shipId2; set(value) {state.shipId2 = value}
    inline var point1 get() = state.point1; set(value) {state.point1 = value}
    inline var point2 get() = state.point2; set(value) {state.point2 = value}
    inline var color get() = state.color; set(value) {state.color = value}
    inline var width get() = state.width; set(value) {state.width = value}

    override fun serialize() = state.serialize()
    override fun deserialize(buf: FriendlyByteBuf) = state.deserialize(buf)

    private var highlightTimestamp = 0L
    override fun highlightUntil(until: Long) {
        if (until > highlightTimestamp) highlightTimestamp = until
    }

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) {
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

        val color = if (timestamp < highlightTimestamp) Color(255, 0, 0, 255) else color

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

    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? {
        val spoint1 = if (shipId1 != -1L) {updatePosition(point1, oldToNew[shipId1]!!)} else {Vector3d(point1)}
        val spoint2 = if (shipId2 != -1L) {updatePosition(point2, oldToNew[shipId2]!!)} else {Vector3d(point2)}

        val newId1 = if (shipId1 != -1L) {oldToNew[shipId1]!!.id} else {-1}
        val newId2 = if (shipId2 != -1L) {oldToNew[shipId2]!!.id} else {-1}

        return A2BRenderer(newId1, newId2, spoint1, spoint2, color, width)
    }

    override fun scaleBy(by: Double) {
        width *= by
    }
}