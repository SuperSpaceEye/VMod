package net.spaceeye.vmod.rendering.types.special

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.BufferVertexConsumer
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Matrix4f
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import com.mojang.math.Vector4f
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
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.mixin.BufferBuilderAccessor
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.BlockRenderer
import net.spaceeye.vmod.rendering.types.special.FakeBufferBuilder.BulkData
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ToolgunItem
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import net.spaceeye.vmod.utils.DebugMap
import net.spaceeye.vmod.utils.MPair
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Ref
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getQuatFromDir
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3i
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.util.toMinecraft
import java.awt.Color
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

fun maybeFasterVertexBuilder(buffer: VertexConsumer, x: Float, y: Float, z: Float, r: Byte, g: Byte, b: Byte, a: Byte, u: Float, v: Float, combinedOverlay: Int, lightmapUV: Int, normalX: Float, normalY: Float, normalZ: Float) {
    buffer as BufferBuilder
    buffer as BufferBuilderAccessor

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

class FakeBufferBuilder(val source: SchemMultiBufferSource): VertexConsumer {
    var transparency = 1f

    val vertices = mutableListOf<Vertex>()
    val bulkData = mutableListOf<BulkData>()

    var defaultColor = Color(255, 255, 255, 255)

    var vertexPose = PoseStack().also { it.setIdentity() }.last()!!

    data class Vertex(var x: Float, var y: Float, var z: Float, var red: Float, var green: Float, var blue: Float, var alpha: Float, var texU: Float, var texV: Float, var overlayUV: Int, var lightmapUV: Int, var normalX: Float, var normalY: Float, var normalZ: Float, var savedPose: PoseStack.Pose, var poseToApply: PoseStack.Pose) {
        fun apply(buffer: VertexConsumer, transparency: Float) {
            val pos = Vector4f(x, y, z, 1f)
            pos.transform(poseToApply.pose())

            maybeFasterVertexBuilder(buffer, pos.x(), pos.y(), pos.z(), (red*255).toInt().toByte(), (green*255).toInt().toByte(), (blue*255).toInt().toByte(), (alpha * transparency*255).toInt().toByte(), texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ)
        }
    }

    data class BulkData(var poseEntry: PoseStack.Pose,
                        var quad: BakedQuad,
                        var colorMuls: FloatArray,
                        var red: Float,
                        var green: Float,
                        var blue: Float,
                        var combinedLights: IntArray,
                        var combinedOverlay: Int,
                        var mulColor: Boolean,
        ) {

        //maybe better putBulkData
        fun apply(buffer: VertexConsumer, transparency: Byte) {
            val vertices = quad.vertices
            val intNormal = quad.direction.normal
            val normal = Vector3f(intNormal.x.toFloat(), intNormal.y.toFloat(), intNormal.z.toFloat())
            normal.transform(poseEntry.normal())

            val numVertices = vertices.size / 8
            var vertexNum = 0

            while (vertexNum < numVertices) {
                val offset = vertexNum * 8

                val x = java.lang.Float.intBitsToFloat(vertices[0 + offset])
                val y = java.lang.Float.intBitsToFloat(vertices[1 + offset])
                val z = java.lang.Float.intBitsToFloat(vertices[2 + offset])

                var r: Float = red   * colorMuls[vertexNum]
                var g: Float = green * colorMuls[vertexNum]
                var b: Float = blue  * colorMuls[vertexNum]

                if (mulColor) {
                    val packedColor = vertices[3 + offset]

                    r *= ((packedColor and 0xff    )       )
                    g *= ((packedColor and 0xff00  ) shr 8 )
                    b *= ((packedColor and 0xff0000) shr 16)
                }

                val u = java.lang.Float.intBitsToFloat(vertices[4 + offset])
                val v = java.lang.Float.intBitsToFloat(vertices[5 + offset])

                val pos = Vector4f(x, y, z, 1f)
                pos.transform(poseEntry.pose())

                maybeFasterVertexBuilder(buffer, pos.x(), pos.y(), pos.z(), r.toInt().toByte(), g.toInt().toByte(), b.toInt().toByte(), transparency, u, v, combinedOverlay, combinedLights[vertexNum], normal.x(), normal.y(), normal.z())

                vertexNum++
            }
        }
    }

    override fun vertex(x: Float, y: Float, z: Float, red: Float, green: Float, blue: Float, alpha: Float, texU: Float, texV: Float, overlayUV: Int, lightmapUV: Int, normalX: Float, normalY: Float, normalZ: Float) {
        vertices.add(Vertex(x, y, z, red, green, blue, alpha, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ, vertexPose, vertexPose))
    }

    override fun putBulkData(
        poseEntry: PoseStack.Pose,
        quad: BakedQuad,
        colorMuls: FloatArray,
        red: Float,
        green: Float,
        blue: Float,
        combinedLights: IntArray,
        combinedOverlay: Int,
        mulColor: Boolean
    ) {
        val data = BulkData(poseEntry, quad, colorMuls, red, green, blue, combinedLights, combinedOverlay, mulColor)
        source.shipIdAndPosToData.last().second.add(data)
        bulkData.add(data)
    }

    fun clear() {
        vertices.clear()
        bulkData.clear()
        unsetDefaultColor()
    }

    fun apply(buffer: VertexConsumer) {
        vertices.forEach { item-> item.apply(buffer, transparency) }
        val transparency = (255 * transparency).toInt().toByte()
        bulkData.forEach { item -> item.apply(buffer, transparency) }
    }

    // i don't think it's ever used but just in case
    lateinit var temp: Vertex
    override fun vertex(x: Double, y: Double, z: Double): VertexConsumer? {temp = Vertex(x.toFloat(), y.toFloat(), z.toFloat(), defaultColor.red  .toFloat() / 255f, defaultColor.green.toFloat() / 255f, defaultColor.blue .toFloat() / 255f, defaultColor.alpha.toFloat() / 255f, 1f, 1f, 0, 0, 1f, 1f, 1f, vertexPose, vertexPose);return this}
    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer? { temp.red   = red  .toFloat() / 255f;temp.green = green.toFloat() / 255f;temp.blue  = blue .toFloat() / 255f;temp.alpha = alpha.toFloat() / 255f;return this; }
    override fun uv(u: Float, v: Float): VertexConsumer? { temp.texU = u;temp.texV = v;return this; }
    override fun overlayCoords(u: Int, v: Int): VertexConsumer? { temp.overlayUV = (u shl 16) or (v and 0xFFFF);return this; }
    override fun uv2(u: Int, v: Int): VertexConsumer? { temp.lightmapUV = (u shl 16) or (v and 0xFFFF);return this; }
    override fun normal(x: Float, y: Float, z: Float): VertexConsumer? { temp.normalX = x;temp.normalY = y;temp.normalZ = z;return this; }
    override fun endVertex() { vertices.add(temp) }
    override fun defaultColor(defaultR: Int, defaultG: Int, defaultB: Int, defaultA: Int) { defaultColor = Color(defaultR, defaultG, defaultB, defaultA) }
    override fun unsetDefaultColor() { defaultColor = Color(255, 255, 255, 255) }
}

class SchemMultiBufferSource: MultiBufferSource {
    val buffers = mutableMapOf<RenderType,  FakeBufferBuilder>()
    val shipIdAndPosToData = mutableListOf<MPair<Matrix4f, MutableList<BulkData>>>()

    override fun getBuffer(renderType: RenderType): FakeBufferBuilder {
        val buf = buffers.getOrPut(renderType) { FakeBufferBuilder(this) }
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

    val updateList: List<Int>
    val mySources = SchemMultiBufferSource()

    init {
        val info = schem.info!!.shipsInfo.associate { Pair(it.id, it) }
        val schem = schem as IShipSchematicDataV1

        //unpack data without positional info
        val random = Random(42)
        val level = Minecraft.getInstance().level!!
        val poseStack = PoseStack()
        val blockRenderer = Minecraft.getInstance().blockRenderer
        val dummyMatrix = Matrix4f()

        updateList = schem.blockData.map { (shipId, data) ->
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

            var numRenderedBlocks = 0
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
                mySources.shipIdAndPosToData.add(MPair(dummyMatrix, mutableListOf()))

                if (state.fluidState.isEmpty) {
                    blockRenderer.renderBatched(state, bpos, levelWrapper, poseStack, buffer, true, random)
                    //if block didn't add anything to fake buffer, then it doesn't render anything, and should be removed
                    if(mySources.shipIdAndPosToData.last().second.isEmpty()) {
                        mySources.shipIdAndPosToData.removeLast()
                        return@forEach
                    }

                    poseStack.pushPose()
                    poseStack.translate(
                        bpos.x.toDouble() - infoItem.positionInShip.x,
                        bpos.y.toDouble() - infoItem.positionInShip.y,
                        bpos.z.toDouble() - infoItem.positionInShip.z,
                    )

                    mySources.shipIdAndPosToData.last().first = poseStack.last().pose()

                    poseStack.popPose()
                } else {
                    poseStack.pushPose()
                    poseStack.translate(
                        bpos.x.toDouble() - infoItem.positionInShip.x,
                        bpos.y.toDouble() - infoItem.positionInShip.y,
                        bpos.z.toDouble() - infoItem.positionInShip.z,
                    )

                    buffer.vertexPose = poseStack.last()

                    //renderLiquid checks level if it should render smth, but i can't figure out how to make correct
                    // poseStack with bpos other than 0 0 0 so that's why im adding offset
                    levelWrapper.offset.set(bpos.x, bpos.y, bpos.z)
                    blockRenderer.renderLiquid(BlockPos(0, 0, 0), levelWrapper, buffer, state, state.fluidState)
                    levelWrapper.offset.set(0, 0, 0)

                    poseStack.popPose()
                }
                numRenderedBlocks++
            }
            poseStack.popPose()
            numRenderedBlocks
        }
    }

    fun updatePoseStacks(poseStack: PoseStack) {
        val list = mySources.shipIdAndPosToData

        var block = 0
        updateList.forEach { numRenderedBlocksInAShip ->
            var i = 0
            while (i < numRenderedBlocksInAShip) {
                poseStack.pushPose()
                poseStack.mulPoseMatrix(list[block].first)
                for (it in list[block].second) {
                    it.poseEntry = poseStack.last()
                }
                poseStack.popPose()
                block++
                i++
            }
        }
        mySources.buffers.forEach { it.value.vertices.forEach {
            poseStack.pushPose()
            poseStack.mulPoseMatrix(it.savedPose.pose())
            it.poseToApply = poseStack.last()

            poseStack.popPose()
        } }
    }

    fun applyBuffers(sources: MultiBufferSource) {
        mySources.buffers.forEach { (type, buf) ->
            val actualBuffer = sources.getBuffer(type)

            val vertexSize = (actualBuffer as BufferBuilderAccessor).`vmod$getVertexFormat`().vertexSize
            var size = buf.vertices.size * vertexSize
            buf.bulkData.forEach { size += it.quad.vertices.size / 8 * vertexSize }
            (actualBuffer as BufferBuilderAccessor).`vmod$ensureCapacity`(size)

            buf.apply(actualBuffer)
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

        updatePoseStacks(poseStack)
        applyBuffers(sources)

        poseStack.popPose()
    }

    // only for internal use on client
    override fun serialize(): FriendlyByteBuf { throw AssertionError("Shouldn't be serialized") }
    override fun deserialize(buf: FriendlyByteBuf) { throw AssertionError("Shouldn't be deserialized") }
    override fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer? { throw AssertionError("Shouldn't be copied") }
    override fun scaleBy(by: Double) { throw AssertionError("Shouldn't be scaled") }
}