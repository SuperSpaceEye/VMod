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
import net.spaceeye.vmod.entities.events.ClientPhysEntitiesHolder
import net.spaceeye.vmod.entities.PhysRopeComponentEntity
import net.spaceeye.vmod.events.RandomEvents
import net.spaceeye.vmod.networking.AutoSerializable
import net.spaceeye.vmod.networking.SerializableItem.get
import net.spaceeye.vmod.rendering.RenderingUtils
import net.spaceeye.vmod.rendering.RenderingUtils.Quad.makePolygon
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.posShipToWorldRender
import org.lwjgl.opengl.GL11
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ships.ShipObjectClientWorld
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color

class PhysRopeRenderer(): BaseRenderer {
    class State: AutoSerializable {
        var shipId1: Long by get(0, -1L)
        var shipId2: Long by get(1, -1)

        var point1: Vector3d by get(2, Vector3d())
        var point2: Vector3d by get(3, Vector3d())

        var color: Color by get(4, Color(0))

        var width: Double by get(5, .2)
        var chainLength: Double by get(6, 1.0)

        var sides: Int by get(7, 8)
    }
    val state = State()

    inline var shipId1 get() = state.shipId1; set(value) {state.shipId1 = value}
    inline var shipId2 get() = state.shipId2; set(value) {state.shipId2 = value}
    inline var point1 get() = state.point1; set(value) {state.point1 = value}
    inline var point2 get() = state.point2; set(value) {state.point2 = value}
    inline var color get() = state.color; set(value) {state.color = value}
    inline var width get() = state.width; set(value) {state.width = value}
    inline var chainLength get() = state.chainLength; set(value) {state.chainLength = value}
    inline var sides get() = state.sides; set(value) {state.sides = value}

    var ids = listOf<Int>()
    var entities = mutableListOf<PhysRopeComponentEntity?>()


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

    private var highlightTimestamp = 0L
    override fun highlightUntil(until: Long) {
        if (until > highlightTimestamp) highlightTimestamp = until
    }

    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) {
        val level = Minecraft.getInstance().level!!

        val tesselator = Tesselator.getInstance()
        val vBuffer = tesselator.builder

        RenderSystem.enableDepthTest()
        RenderSystem.disableCull()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderTexture(0, RenderingUtils.ropeTexture)
        if (timestamp < highlightTimestamp) {
            RenderSystem.setShaderColor(0f, 1f, 0f, 0.5f)
        }

        vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        poseStack.pushPose()

        val light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, point1.toBlockPos()), level.getBrightness(LightLayer.SKY, point1.toBlockPos()))

        val cameraPos = Vector3d(camera.position)
        val matrix = poseStack.last().pose()

        // ========================
        val dir1 = Vector3d(chainLength, 0, 0) * 0.5
        val dir2 = -dir1

        for (entity in entities) {
            if (entity == null) {continue}

            val transform = entity.getRenderTransform(level.shipObjectWorld as ShipObjectClientWorld) ?: continue

            val pos1 = posShipToWorldRender(null, dir1, transform) - cameraPos
            val pos2 = posShipToWorldRender(null, dir2, transform) - cameraPos

            RenderingUtils.Quad.makeFlatRectFacingCameraTexture(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, width, pos1, pos2)
        }
        // =======================
//        val ship1 = level.shipObjectWorld.allShips.getById(shipId1)
//        val ship2 = level.shipObjectWorld.allShips.getById(shipId2)
//
//        val quat1 = Quaterniond(ship1?.renderTransform?.shipToWorldRotation ?: Quaterniond())
//        val quat2 = Quaterniond(ship2?.renderTransform?.shipToWorldRotation ?: Quaterniond())
//
//        val capsuleDir = -Vector3d(chainLength, 0, 0) * 0.5
//
//        var ppos = (if (ship1 == null) point1 else posShipToWorldRender(ship1, point1)) - cameraPos
//        var cpos: Vector3d = if (entities[0] != null && entities[0]!!.getRenderTransform(level.shipObjectWorld as ShipObjectClientWorld) != null) {
//            Vector3d(entities[0]!!.getRenderTransform(level.shipObjectWorld as ShipObjectClientWorld)!!.positionInWorld)
//        } else {
//            if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)
//        } - cameraPos
//
//        var up = getUpFromQuat(quat1)
//
//        var lPoints = makePoints(cpos, ppos, ppos, up)
//        var rPoints: List<Vector3d>
//
//        for (entity in entities) {
//            if (entity == null) { continue }
//            val transform = entity.getRenderTransform(level.shipObjectWorld as ShipObjectClientWorld) ?: continue
//            cpos = posShipToWorldRender(null, capsuleDir, transform) - cameraPos
//
//            up = getUpFromQuat(transform.shipToWorldRotation)
//            rPoints = makePoints(cpos, ppos, up, cpos)
//
//            drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, 0.0f, 1.0f, lPoints, rPoints)
//
//            lPoints = rPoints
//            ppos = cpos
//        }
//
//        cpos = if (ship2 == null) point2 else posShipToWorldRender(ship2, point2)
//        cpos = cpos - cameraPos
//
//        up = getUpFromQuat(quat2)
//
//        rPoints = makePoints(cpos, ppos, up, cpos)
//
//        drawPolygonTube(vBuffer, matrix, color.red, color.green, color.blue, color.alpha, light, 0.0f, 1.0f, lPoints, rPoints)

        tesselator.end()
        poseStack.popPose()

        if (timestamp < highlightTimestamp) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }

        RenderSystem.enableCull()
    }

    private inline fun makePoints(cpos: Vector3d, ppos: Vector3d, posToUse: Vector3d, up: Vector3d, ) = makePolygon(sides, width, up, (cpos - ppos).snormalize().scross(up), posToUse)

    override fun serialize(): FriendlyByteBuf {
        val buf = state.serialize()
        buf.writeCollection(ids) { buf, id -> buf.writeInt(id)}
        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        state.deserialize(buf)

        ids = buf.readCollection({ mutableListOf() }) {buf.readInt()}

        ids.forEachIndexed { i, id ->
            entities.add(null)
            val anEntity = Minecraft.getInstance().level!!.getEntity(id)
            if (anEntity != null) {
                entities[i] = anEntity as PhysRopeComponentEntity
                return@forEachIndexed
            }

            var got = false
            ClientPhysEntitiesHolder.clientEntityLoadedEvent.on {
                (anID, anEntity), unregister ->
                if (got) {unregister(); return@on}
                val entity = Minecraft.getInstance().level!!.getEntity(id)
                if (entity != null) {
                    entities[i] = entity as PhysRopeComponentEntity
                    unregister()
                    got = true
                }

                if (anID != id) {return@on}
                entities[i] = anEntity as PhysRopeComponentEntity
                unregister()
                got = true
            }

            //kinda stupid but it's needed because last entity id may not appear and idk why it doesn't
            var times = 0
            RandomEvents.clientOnTick.on {
                _, unregister ->
                if (got) {unregister(); return@on}
                times++
                if (times > 11) {unregister(); return@on}
                val entity = Minecraft.getInstance().level!!.getEntity(id) ?: return@on
                entities[i] = entity as PhysRopeComponentEntity
                unregister()
                got = true
            }
        }
    }

    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? { return null }
    override fun scaleBy(by: Double) {}
}