package net.spaceeye.vmod.rendering.types

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.LightLayer
import net.spaceeye.vmod.entities.ServerEntitiesHolder
import net.spaceeye.vmod.entities.PhysRopeComponentEntity
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color
import java.util.UUID

//TODO this is a temporary measure
open class EA2BRenderer(): BaseRenderer {
    var color: Color = Color(0)

    var entity: PhysRopeComponentEntity? = null
    var uuid: UUID? = null

    var width: Double = .2
    var length: Double = 1.0

    constructor(entity: PhysRopeComponentEntity,
                color: Color,
                width: Double,
                length: Double
    ): this() {
        this.entity = entity
        this.color = color
        this.width = width
        this.length = length
        this.uuid = entity.uuid
    }

    override val typeName = "EA2BRenderer"

    override fun renderData(poseStack: PoseStack, camera: Camera) {
        val entity = entity ?: return
        if (entity.isRemoved) {return}
        val level = Minecraft.getInstance().level

        val transform = entity.getRenderTransform(Minecraft.getInstance().shipObjectWorld as ShipObjectClientWorld) ?: return

        val dir = Vector3d(length, 0, 0)

        val rpoint1 = posShipToWorldRender(null, dir * 0.5, transform)
        val rpoint2 = posShipToWorldRender(null,-dir * 0.5, transform)

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionColorShader)

        val light = LightTexture.pack(level!!.getBrightness(LightLayer.BLOCK, rpoint1.toBlockPos()), level.getBrightness(LightLayer.SKY, rpoint1.toBlockPos()))

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

        buf.writeInt(color.rgb)
        buf.writeDouble(width)
        buf.writeDouble(length)
        //TODO this is probably wrong
        buf.writeUUID(uuid ?: UUID(0, 0))

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        color = Color(buf.readInt())
        width = buf.readDouble()
        length = buf.readDouble()

        val uuid = buf.readUUID()
        synchronized(ServerEntitiesHolder.entities) {
            val anEntity = ServerEntitiesHolder.entities[uuid]
            if (anEntity != null) {
                entity = anEntity as PhysRopeComponentEntity
                return
            }
            ServerEntitiesHolder.entityLoadedEvent.on { (anUUID, anEntity), handler ->
                if (uuid != anUUID) { return@on }
                entity = anEntity as PhysRopeComponentEntity
                handler.unregister()
            }
        }

        val entity = ServerEntitiesHolder.entities[uuid] ?: return
        ServerEntitiesHolder.entityLoaded(uuid, entity)
    }
}