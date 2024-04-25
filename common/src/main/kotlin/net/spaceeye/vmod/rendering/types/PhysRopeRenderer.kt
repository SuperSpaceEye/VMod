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
import net.spaceeye.vmod.entities.ClientEntitiesHolder
import net.spaceeye.vmod.entities.PhysRopeComponentEntity
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.RenderingUtils.Quad.drawPolygonTube
import net.spaceeye.vmod.rendering.RenderingUtils.Quad.makePolygon
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import org.joml.Quaterniond
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

class PhysRopeRenderer(): BaseRenderer {
    var shipId1: ShipId = -1
    var shipId2: ShipId = -1

    lateinit var point1: Vector3d
    lateinit var point2: Vector3d

    var color: Color = Color(0)

    var width: Double = .2
    var chainLength: Double = 1.0

    var ids = listOf<Int>()
    var entities = mutableListOf<PhysRopeComponentEntity?>()

    var sides: Int = 8

    constructor(shipId1: ShipId, shipId2: ShipId,
                point1: Vector3d, point2: Vector3d,
                color: Color, width: Double, chainLength: Double,
                uuids: List<Int>
        ): this() {
            this.shipId1 = shipId1
            this.shipId2 = shipId2

            this.point1 = point1
            this.point2 = point2

            this.color = color

            this.width = width
            this.chainLength = chainLength

            this.ids = uuids
        }

    override fun renderData(poseStack: PoseStack, camera: Camera) {
        val level = Minecraft.getInstance().level!!

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.disableCull()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderTexture(0, RenderingUtils.ropeTexture)

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        poseStack.pushPose()

        val light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, point1.toBlockPos()), level.getBrightness(LightLayer.SKY, point1.toBlockPos()))

        val cameraPos = Vector3d(camera.position)
        val matrix = poseStack.last().pose()

        // ========================
//        val dir1 = Vector3d(chainLength, 0, 0) * 0.5
//        val dir2 = -dir1
//
//        for (entity in entities) {
//            if (entity == null) {continue}
//
//            val transform = entity.getRenderTransform(level.shipObjectWorld as ShipObjectClientWorld) ?: continue
//
//            val pos1 = posShipToWorldRender(null, dir1, transform) - cameraPos
//            val pos2 = posShipToWorldRender(null, dir2, transform) - cameraPos
//
//            RenderingUtils.Quad.makeFlatRectFacingCameraTexture(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, width, pos1, pos2)
//        }
        // =======================
        val ship1 = level.shipObjectWorld.allShips.getById(shipId1)
        val ship2 = level.shipObjectWorld.allShips.getById(shipId2)

        val quat1 = Quaterniond(ship1?.renderTransform?.shipToWorldRotation ?: Quaterniond())
        val quat2 = Quaterniond(ship2?.renderTransform?.shipToWorldRotation ?: Quaterniond())

        val capsuleDir = -Vector3d(chainLength, 0, 0) * 0.5

        var ppos = (if (ship1 == null) point1 else posShipToWorldRender(ship1, point1)) - cameraPos
        var cpos: Vector3d = if (entities[0] != null && entities[0]!!.getRenderTransform(level.shipObjectWorld as ShipObjectClientWorld) != null) {
            Vector3d(entities[0]!!.getRenderTransform(level.shipObjectWorld as ShipObjectClientWorld)!!.positionInWorld)
        } else {
            if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)
        } - cameraPos

        var up = getUpFromQuat(quat1)

        var lPoints = makePoints(cpos, ppos, ppos, up)
        var rPoints: List<Vector3d>

        for (entity in entities) {
            if (entity == null) { continue }
            val transform = entity.getRenderTransform(level.shipObjectWorld as ShipObjectClientWorld) ?: continue
            cpos = posShipToWorldRender(null, capsuleDir, transform) - cameraPos

            up = getUpFromQuat(transform.shipToWorldRotation)
            rPoints = makePoints(cpos, ppos, up, cpos)

            drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, 0.0f, 1.0f, lPoints, rPoints)

            lPoints = rPoints
            ppos = cpos
        }

        cpos = if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)
        cpos = cpos - cameraPos

        up = getUpFromQuat(quat2)

        rPoints = makePoints(cpos, ppos, up, cpos)

        drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, 0.0f, 1.0f, lPoints, rPoints)

        tesselator.end()
        poseStack.popPose()

        RenderSystem.enableCull()
    }

    private inline fun makePoints(cpos: Vector3d, ppos: Vector3d, posToUse: Vector3d, up: Vector3d, ) = makePolygon(sides, width, up, (cpos - ppos).snormalize().scross(up), posToUse)

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeLong(shipId1)
        buf.writeLong(shipId2)

        buf.writeVector3d(point1)
        buf.writeVector3d(point2)

        buf.writeColor(color)

        buf.writeDouble(width)
        buf.writeDouble(chainLength)

        buf.writeInt(sides)

        buf.writeCollection(ids) { buf, id -> buf.writeInt(id)}

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        shipId1 = buf.readLong()
        shipId2 = buf.readLong()

        point1 = buf.readVector3d()
        point2 = buf.readVector3d()

        color = buf.readColor()

        width = buf.readDouble()
        chainLength = buf.readDouble()

        sides = buf.readInt()

        ids = buf.readCollection({ mutableListOf() }) {buf.readInt()}

        ids.forEachIndexed { i, id ->
            entities.add(null)
            val anEntity = Minecraft.getInstance().level!!.getEntity(id)
            if (anEntity != null) {
                entities[i] = anEntity as PhysRopeComponentEntity
                return@forEachIndexed
            }

            var got = false
            ClientEntitiesHolder.clientEntityLoadedEvent.on {
                (anID, anEntity), handler ->
                if (got) {handler.unregister(); return@on}
                val entity = Minecraft.getInstance().level!!.getEntity(id)
                if (entity != null) {
                    entities[i] = entity as PhysRopeComponentEntity
                    handler.unregister()
                    got = true
                }

                if (anID != id) {return@on}
                entities[i] = anEntity as PhysRopeComponentEntity
                handler.unregister()
                got = true
            }

            //kinda stupid but it's needed because last entity id may not appear and idk why it doesn't
            var times = 0
            RandomEvents.clientOnTick.on {
                _, handler ->
                if (got) {handler.unregister(); return@on}
                times++
                if (times > 11) {handler.unregister(); return@on}
                val entity = Minecraft.getInstance().level!!.getEntity(id) ?: return@on
                entities[i] = entity as PhysRopeComponentEntity
                handler.unregister()
                got = true
            }
        }
    }

    override val typeName = "PhysRopeRenderer"
}