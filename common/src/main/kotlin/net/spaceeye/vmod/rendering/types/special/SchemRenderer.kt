package net.spaceeye.vmod.rendering.types.special

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.BufferVertexConsumer
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Matrix4f
import com.mojang.math.Vector3f
import com.mojang.math.Vector4f
import io.netty.buffer.ByteBuf
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.ColorResolver
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkSource
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.lighting.LevelLightEngine
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.ticks.LevelTickAccess
import net.spaceeye.valkyrien_ship_schematics.containers.v1.BlockItem
import net.spaceeye.valkyrien_ship_schematics.containers.v1.ChunkyBlockData
import net.spaceeye.valkyrien_ship_schematics.interfaces.IBlockStatePalette
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.mixin.BufferBuilderAccessor
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.BlockRenderer
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import net.spaceeye.vmod.utils.DebugMap
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Ref
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getQuatFromDir
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3i
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.shadow.bu
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import java.awt.Color
import java.nio.ByteBuffer
import java.util.Random
import java.util.function.Supplier

//TODO add some methods to ChunkyBlockData
inline fun <T, Out>ChunkyBlockData<T>.mapNotNull(fn: (Int, Int, Int, T) -> Out?): MutableList<Out> {
    val toReturn = mutableListOf<Out>()
    this.forEach {x, y, z, item ->
        toReturn.add(fn(x, y, z, item) ?: return@forEach)
    }
    return toReturn
}

//TODO plan
// applying shit to buffer may not be the fastest thing, so instead of doing that i want to create buffer, then
// only change xyz positions of vertices each frame.

fun maybeFasterVertexBuilder(buffer: VertexConsumer, x: Float, y: Float, z: Float, r: Byte, g: Byte, b: Byte, a: Byte, u: Float, v: Float, combinedOverlay: Int, lightmapUV: Int, normalX: Float, normalY: Float, normalZ: Float) {
    buffer as BufferBuilder
    buffer as BufferBuilderAccessor

    //just check vertex size, xyz are first
//    println(buffer.`vmod$nextElementByte`() % 32 == 0)

    buffer.putFloat(0, x)
    buffer.putFloat(4, y)
    buffer.putFloat(8, z)
    buffer.putByte(12, r)
    buffer.putByte(13, g)
    buffer.putByte(14, b)
    buffer.putByte(15, a)
    buffer.putFloat(16, u)
    buffer.putFloat(20, v)
    val i = if (buffer.`vmod$fullFormat`()) {
        buffer.putShort(24, (combinedOverlay and 0xffff).toShort())
        buffer.putShort(26, (combinedOverlay shr 16 and 0xffff).toShort())
        28
    } else { 24 }

    buffer.putShort(i + 0, (lightmapUV and 0xffff).toShort())
    buffer.putShort(i + 2, (lightmapUV shr 16 and 0xffff).toShort())
    buffer.putByte(i + 4, BufferVertexConsumer.normalIntValue(normalX))
    buffer.putByte(i + 5, BufferVertexConsumer.normalIntValue(normalY))
    buffer.putByte(i + 6, BufferVertexConsumer.normalIntValue(normalZ))

    buffer.`vmod$nextElementByte`(buffer.`vmod$nextElementByte`() + i + 8)

    //not using endVertex to not call ensureCapacity
    buffer.`vmod$vertices`(buffer.`vmod$vertices`() + 1)
}

class BlockAndTintGetterWrapper(val level: ClientLevel, val data: ChunkyBlockData<BlockItem>, val palette: IBlockStatePalette): Level(
    level.levelData, level.dimension(), level.dimensionTypeRegistration(), Supplier{level.profiler}, level.isClientSide, level.isDebug, 0L
) {
    val defaultState = Blocks.AIR.defaultBlockState()
    val defaultFluidState = Fluids.EMPTY.defaultFluidState()
    var offset = Vector3i(0, 0, 0)

    override fun getShade(direction: Direction, shade: Boolean): Float {
        return 1f
        return level.getShade(direction, shade)
    }

    override fun getBlockTint(
        blockPos: BlockPos,
        colorResolver: ColorResolver
    ): Int {
        return level.getBlockTint(blockPos, colorResolver)
    }

    override fun getBlockEntity(pos: BlockPos): BlockEntity? = null
    override fun getBlockState(pos: BlockPos): BlockState? {
        val pos = BlockPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z)
        return data.blocks.get(BlockPos(pos.x shr 4, 0, pos.z shr 4))?.get(BlockPos(pos.x and 15, pos.y, pos.z and 15))?.let { palette.fromId(it.paletteId) } ?: defaultState
    }
    override fun getFluidState(pos: BlockPos): FluidState? {
        val pos = BlockPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z)
        return data.blocks.get(BlockPos(pos.x shr 4, 0, pos.z shr 4))?.get(BlockPos(pos.x and 15, pos.y, pos.z and 15))?.let { palette.fromId(it.paletteId) }?.fluidState ?: defaultFluidState
    }
    override fun getLightEngine(): LevelLightEngine? = level.lightEngine
    override fun getHeight(): Int = level.height
    override fun getMinBuildHeight(): Int = level.minBuildHeight

    override fun getUncachedNoiseBiome(x: Int, y: Int, z: Int): Holder<Biome?>? { throw AssertionError("Shouldn't be called") }
    override fun playSound(player: Player?, x: Double, y: Double, z: Double, sound: SoundEvent, category: SoundSource, volume: Float, pitch: Float) { throw AssertionError("Shouldn't be called")  }
    override fun playSound(player: Player?, entity: Entity, event: SoundEvent, category: SoundSource, volume: Float, pitch: Float) { throw AssertionError("Shouldn't be called")  }
    override fun gatherChunkSourceStats(): String? { throw AssertionError("Shouldn't be called")  }
    override fun sendBlockUpdated(pos: BlockPos, oldState: BlockState, newState: BlockState, flags: Int) { throw AssertionError("Shouldn't be called")  }
    override fun getEntity(id: Int): Entity? { throw AssertionError("Shouldn't be called")  }
    override fun getMapData(mapName: String): MapItemSavedData? { throw AssertionError("Shouldn't be called")  }
    override fun setMapData(mapId: String, data: MapItemSavedData) { throw AssertionError("Shouldn't be called")  }
    override fun getFreeMapId(): Int { throw AssertionError("Shouldn't be called")  }
    override fun destroyBlockProgress(breakerId: Int, pos: BlockPos, progress: Int) { throw AssertionError("Shouldn't be called")  }
    override fun getScoreboard(): Scoreboard? { throw AssertionError("Shouldn't be called")  }
    override fun getRecipeManager(): RecipeManager? { throw AssertionError("Shouldn't be called")  }
    override fun getEntities(): LevelEntityGetter<Entity?>? { throw AssertionError("Shouldn't be called")  }
    override fun getBlockTicks(): LevelTickAccess<Block?>? { throw AssertionError("Shouldn't be called")  }
    override fun getFluidTicks(): LevelTickAccess<Fluid?>? { throw AssertionError("Shouldn't be called")  }
    override fun getChunkSource(): ChunkSource? { throw AssertionError("Shouldn't be called")  }
    override fun levelEvent(player: Player?, type: Int, pos: BlockPos, data: Int) { throw AssertionError("Shouldn't be called")  }
    override fun gameEvent(entity: Entity?, event: GameEvent, pos: BlockPos) { throw AssertionError("Shouldn't be called")  }
    override fun registryAccess(): RegistryAccess? { throw AssertionError("Shouldn't be called")  }
    override fun players(): List<Player?>? { throw AssertionError("Shouldn't be called")  }
}

class FakeBufferBuilder(val source: SchemMultiBufferSource, val renderType: RenderType): VertexConsumer {
//    val vertices = mutableListOf<Vertex>()

    val buffer = BufferBuilder(renderType.format().vertexSize)
    val vertexBuffer = VertexBuffer()
    val positions = mutableListOf<Float>()
    val matrixIndexes = mutableListOf<Int>()

    var defaultColor = Color(255, 255, 255, 255)
    var transparency = 0.5f

    var vertexPose = PoseStack().also { it.setIdentity() }.last()!!
    var vertexOffset = org.joml.Vector3f(0f, 0f, 0f)
    var vertexMatrixIndex = 0

    init {
        buffer.begin(renderType.mode(), renderType.format())
    }

    data class Vertex(var x: Float, var y: Float, var z: Float, var red: Float, var green: Float, var blue: Float, var alpha: Float, var texU: Float, var texV: Float, var overlayUV: Int, var lightmapUV: Int, var normalX: Float, var normalY: Float, var normalZ: Float, var matrixIndex: Int) {
        fun apply(buffer: VertexConsumer, transparency: Float, matrices: List<org.joml.Matrix4f>) {
            val matrix = matrices[matrixIndex]

            var x_ = matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30()
            var y_ = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31()
            var z_ = matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32()

            maybeFasterVertexBuilder(buffer, x_, y_, z_, (red*255).toInt().toByte(), (green*255).toInt().toByte(), (blue*255).toInt().toByte(), (alpha * transparency*255).toInt().toByte(), texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ)
        }
    }

    override fun vertex(x: Float, y: Float, z: Float, red: Float, green: Float, blue: Float, alpha: Float, texU: Float, texV: Float, overlayUV: Int, lightmapUV: Int, normalX: Float, normalY: Float, normalZ: Float) {
        val x = x + vertexOffset.x
        val y = y + vertexOffset.y
        val z = z + vertexOffset.z

        positions.add(x)
        positions.add(y)
        positions.add(z)

        matrixIndexes.add(vertexMatrixIndex)

        //vertex positions will be overwritten
        buffer.vertex(x, y, z, red, green, blue, alpha * transparency, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ)
//        vertices.add(Vertex(x + vertexOffset.x, y + vertexOffset.y, z + vertexOffset.z, red, green, blue, alpha, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ, vertexMatrixIndex))
    }

    fun clear() {
//        vertices.clear()
        unsetDefaultColor()
    }

    fun apply(unused: VertexConsumer, matrices: List<org.joml.Matrix4f>, poseStack: PoseStack) {
        //java.lang.Float.intBitsToFloat
        val bufferBuilder = buffer as BufferBuilderAccessor
        val byteBuffer = bufferBuilder.`vmod$getBuffer`()


        var i = 0
        val size = matrixIndexes.size

        while (i < size) {
            val posOffset = i * 3
            val vertexOffset = i * renderType.format().vertexSize

            val matrix = matrices[matrixIndexes[i]]
            var x = positions[0 + posOffset]
            var y = positions[1 + posOffset]
            var z = positions[2 + posOffset]

            var x_ = matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30()
            var y_ = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31()
            var z_ = matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32()

            byteBuffer.putFloat(0 + vertexOffset, x_)
            byteBuffer.putFloat(4 + vertexOffset, y_)
            byteBuffer.putFloat(8 + vertexOffset, z_)

            i++
        }

        val matrixView = RenderSystem.getModelViewMatrix().copy()
        matrixView.multiply(poseStack.last().pose())

        renderType.setupRenderState()
//        RenderSystem.disableDepthTest()

        vertexBuffer.bind()
        vertexBuffer.drawWithShader(matrixView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader()!!)

//        RenderSystem.enableDepthTest()
        renderType.clearRenderState()

//        var i = 0
//        val size = vertices.size
//        while (i < size) {
//            vertices[i].apply(buffer, transparency, matrices)
//            i++
//        }
    }

    //liquid renderer uses this
    lateinit var temp: Vertex
    override fun vertex(x: Double, y: Double, z: Double): VertexConsumer? {
        temp = Vertex(
            x.toFloat() + vertexOffset.x,
            y.toFloat() + vertexOffset.y,
            z.toFloat() + vertexOffset.z,
            defaultColor.red  .toFloat() / 255f,
            defaultColor.green.toFloat() / 255f,
            defaultColor.blue .toFloat() / 255f,
            defaultColor.alpha.toFloat() / 255f,
            1f, 1f, 0, 0, 1f, 1f, 1f, vertexMatrixIndex)
        return this
    }
    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer? {
        temp.red   = red  .toFloat() / 255f
        temp.green = green.toFloat() / 255f
        temp.blue  = blue .toFloat() / 255f
        temp.alpha = alpha.toFloat() / 255f
        return this
    }
    override fun uv(u: Float, v: Float): VertexConsumer? { temp.texU = u;temp.texV = v;return this; }
    override fun overlayCoords(u: Int, v: Int): VertexConsumer? { temp.overlayUV = (u shl 16) or (v and 0xFFFF);return this; }
    override fun uv2(u: Int, v: Int): VertexConsumer? { temp.lightmapUV = (u shl 16) or (v and 0xFFFF);return this; }
    override fun normal(x: Float, y: Float, z: Float): VertexConsumer? { temp.normalX = x;temp.normalY = y;temp.normalZ = z;return this; }
    override fun endVertex() { with(temp) { buffer.vertex(x, y, z, red, green, blue, alpha * transparency, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ) } }
    override fun defaultColor(defaultR: Int, defaultG: Int, defaultB: Int, defaultA: Int) { defaultColor = Color(defaultR, defaultG, defaultB, defaultA) }
    override fun unsetDefaultColor() { defaultColor = Color(255, 255, 255, 255) }
}

class SchemMultiBufferSource: MultiBufferSource {
    val buffers = mutableMapOf<RenderType,  FakeBufferBuilder>()

    override fun getBuffer(renderType: RenderType): FakeBufferBuilder {
        val buf = buffers.getOrPut(renderType) { FakeBufferBuilder(this, renderType) }
        return buf
    }

    fun clear() {
        buffers.forEach { (_, buf) -> buf.clear() }
    }
}

class SchemRenderer(
    val schem: IShipSchematic
): BlockRenderer() {
    val transparency = 0.5f

    val matrixList: List<Matrix4f>
    val mySources = SchemMultiBufferSource()

    init {
        val info = schem.info!!.shipsInfo.associate { Pair(it.id, it) }
        val schem = schem as IShipSchematicDataV1

        //unpack data without positional info
        val random = Random(42)
        val level = Minecraft.getInstance().level!!
        val poseStack = PoseStack()
        val blockRenderer = Minecraft.getInstance().blockRenderer

        var matrixIndex = 0
        matrixList = schem.blockData.map { (shipId, data) ->
            val levelWrapper = BlockAndTintGetterWrapper(level, data, schem.blockPalette)

            val infoItem = info[shipId]!!
            val rotationQuat = infoItem.rotation.toMinecraft()

            //should be in the rotated world frame
            poseStack.pushPose()
            poseStack.translate(
                infoItem.relPositionToCenter.x,
                infoItem.relPositionToCenter.y,
                infoItem.relPositionToCenter.z,
            )
            //now in the ship frame
            poseStack.mulPose(rotationQuat)

            data.forEach { x, y, z, item ->
                val bpos = BlockPos(x, y, z)
                val state = levelWrapper.getBlockState(bpos) ?: return@forEach

                val type = if (state.fluidState.isEmpty) {
                    RenderType.translucent()
                } else {
                    ItemBlockRenderTypes.getRenderLayer(state.fluidState)
                }

                val buffer = mySources.getBuffer(type)
                buffer.transparency = transparency

                buffer.vertexMatrixIndex = matrixIndex
                buffer.vertexPose = poseStack.last()
                buffer.vertexOffset.set(
                    bpos.x.toDouble() - infoItem.positionInShip.x,
                    bpos.y.toDouble() - infoItem.positionInShip.y,
                    bpos.z.toDouble() - infoItem.positionInShip.z,
                )

                if (state.fluidState.isEmpty) {
                    blockRenderer.renderBatched(state, bpos, levelWrapper, PoseStack(), buffer, true, random)
                } else {
                    //renderLiquid reduces position to a chunk so doing this is easier
                    levelWrapper.offset.set(bpos.x, bpos.y, bpos.z)
                    blockRenderer.renderLiquid(BlockPos(0, 0, 0), levelWrapper, buffer, state, state.fluidState)
                    levelWrapper.offset.set(0, 0, 0)
                }
            }
            matrixIndex++
            poseStack.last().pose().also { poseStack.popPose() }
        }

        mySources.buffers.forEach {
            it.value.buffer.end()
            it.value.vertexBuffer.upload(it.value.buffer)
        }
    }

    fun applyBuffers(sources: MultiBufferSource, poseStack: PoseStack) {
        val matrices = matrixList.map {
            poseStack.pushPose()
            poseStack.mulPoseMatrix(it)
            org.joml.Matrix4f(poseStack.last().pose().toJOML()).also { poseStack.popPose() }
        }

        mySources.buffers.forEach { (type, buf) ->
            val actualBuffer = sources.getBuffer(type)
//            actualBuffer as BufferBuilderAccessor

//            val vertexSize = (actualBuffer as BufferBuilderAccessor).`vmod$getVertexFormat`().vertexSize
//            var size = buf.vertices.size * vertexSize
//            (actualBuffer as BufferBuilderAccessor).`vmod$ensureCapacity`(size)

            buf.apply(actualBuffer, matrices, poseStack)
        }
    }

    override fun renderBlockData(poseStack: PoseStack, camera: Camera, sources: MultiBufferSource, timestamp: Long) {
        val mode = ClientToolGunState.currentMode
        if (mode !is SchemMode) {return}
        if (!ToolgunItem.playerIsUsingToolgun()) {return}
        val level = Minecraft.getInstance().level!!
        val rotationAngle = DebugMap["rotationRef"] as Ref<Double>

        val raycastResult = RaycastFunctions.renderRaycast(
            level,
            RaycastFunctions.Source(
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.lookVector).snormalize(),
                Vector3d(Minecraft.getInstance().gameRenderer.mainCamera.position)
            ),
            200.0
        )

        val pos = (raycastResult.worldHitPos ?: return) + (raycastResult.worldNormalDirection ?: return) * schem.info!!.maxObjectPos.y

        val rotation = Quaterniond()
            .mul(Quaterniond(AxisAngle4d(rotationAngle.it, raycastResult.worldNormalDirection!!.toJomlVector3d())))
            .mul(getQuatFromDir(raycastResult.worldNormalDirection!!))
            .normalize()

        poseStack.pushPose()
        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)
        poseStack.translate(pos.x, pos.y, pos.z)
        poseStack.mulPose(rotation.toMinecraft())

        applyBuffers(sources, poseStack)

        poseStack.popPose()
    }

    // only for internal use on client
    override fun serialize(): FriendlyByteBuf { throw AssertionError("Shouldn't be serialized") }
    override fun deserialize(buf: FriendlyByteBuf) { throw AssertionError("Shouldn't be deserialized") }
    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? { throw AssertionError("Shouldn't be copied") }
    override fun scaleBy(by: Double) { throw AssertionError("Shouldn't be scaled") }
}