package net.spaceeye.vmod.rendering.types.special

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.BufferVertexConsumer
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.core.SectionPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ColorResolver
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkSource
import net.minecraft.world.level.chunk.DataLayer
import net.minecraft.world.level.chunk.LightChunk
import net.minecraft.world.level.chunk.LightChunkGetter
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.lighting.LayerLightEventListener
import net.minecraft.world.level.lighting.LevelLightEngine
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import net.minecraft.world.phys.Vec3
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
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Ref
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getQuatFromDir
import org.joml.AxisAngle4d
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3i
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import java.awt.Color
import java.util.Random
import java.util.function.Supplier
import kotlin.math.roundToInt

//TODO optimize more
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

class SchemLightEngine(): LevelLightEngine(object : LightChunkGetter {
    override fun getChunkForLighting(chunkX: Int, chunkZ: Int): LightChunk? = null
    override fun getLevel(): BlockGetter? = null
}, false, false) {
    class SchemLayerListener(): LayerLightEventListener {
        override fun getLightValue(levelPos: BlockPos): Int = 15

        override fun getDataLayerData(sectionPos: SectionPos): DataLayer? = null
        override fun checkBlock(pos: BlockPos) {}
        override fun hasLightWork(): Boolean = false
        override fun runLightUpdates(): Int = 0
        override fun updateSectionStatus(pos: SectionPos, isQueueEmpty: Boolean) {}
        override fun setLightEnabled(chunkPos: ChunkPos, lightEnabled: Boolean) {}
        override fun propagateLightSources(chunkPos: ChunkPos) {}
    }
    val layerListener = SchemLayerListener()

    override fun getRawBrightness(blockPos: BlockPos, amount: Int): Int = 15
    override fun getLightSectionCount(): Int = 2000000
    override fun getMinLightSection(): Int = -1000000
    override fun getMaxLightSection(): Int = 1000000

    override fun checkBlock(pos: BlockPos) {}
    override fun hasLightWork(): Boolean = false
    override fun updateSectionStatus(pos: SectionPos, isQueueEmpty: Boolean) {}
    override fun getLayerListener(type: LightLayer): LayerLightEventListener? = layerListener
    override fun getDebugData(lightLayer: LightLayer, sectionPos: SectionPos): String? = "n/a"
    override fun retainData(pos: ChunkPos, retain: Boolean) {}
}

class BlockAndTintGetterWrapper(val level: ClientLevel, val data: ChunkyBlockData<BlockItem>, val palette: IBlockStatePalette): Level(
    level.levelData, level.dimension(), level.registryAccess(), level.dimensionTypeRegistration(), Supplier{level.profiler}, level.isClientSide, level.isDebug, 0L, 0
) {
    val defaultState = Blocks.AIR.defaultBlockState()
    val defaultFluidState = Fluids.EMPTY.defaultFluidState()
    var offset = Vector3i(0, 0, 0)

    override fun getShade(direction: Direction, shade: Boolean): Float = 1f

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

    override fun playSeededSound(player: Player?, x: Double, y: Double, z: Double, sound: Holder<SoundEvent?>, source: SoundSource, volume: Float, pitch: Float, seed: Long) {}
    override fun playSeededSound(player: Player?, entity: Entity, sound: Holder<SoundEvent?>, category: SoundSource, volume: Float, pitch: Float, seed: Long) {}

    private val dummyLightEngine = SchemLightEngine()
    override fun getLightEngine(): LevelLightEngine? = dummyLightEngine
    override fun getHeight(): Int = 2000000
    override fun getMinBuildHeight(): Int = -1000000

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
    override fun gameEvent(event: GameEvent, position: Vec3, context: GameEvent.Context) {}
    override fun gameEvent(entity: Entity?, event: GameEvent, pos: BlockPos) { throw AssertionError("Shouldn't be called")  }
    override fun registryAccess(): RegistryAccess? { throw AssertionError("Shouldn't be called")  }
    override fun enabledFeatures(): FeatureFlagSet? {return null}

    override fun players(): List<Player?>? { throw AssertionError("Shouldn't be called")  }
}

class FakeBufferBuilder(val source: SchemMultiBufferSource): VertexConsumer {
    val vertices = mutableListOf<Vertex>()

    var defaultColor = Color(255, 255, 255, 255)
    var transparency = 0.5f

    var vertexPose = PoseStack().also { it.setIdentity() }.last()!!
    var vertexOffset = org.joml.Vector3f(0f, 0f, 0f)
    var vertexMatrixIndex = 0

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
        vertices.add(Vertex(x + vertexOffset.x, y + vertexOffset.y, z + vertexOffset.z, red, green, blue, alpha, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ, vertexMatrixIndex))
    }

    fun clear() {
        vertices.clear()
        unsetDefaultColor()
    }

    fun apply(buffer: VertexConsumer, matrices: List<org.joml.Matrix4f>) {
        var i = 0
        val size = vertices.size
        while (i < size) {
            vertices[i].apply(buffer, transparency, matrices)
            i++
        }
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
    override fun endVertex() { vertices.add(temp) }
    override fun defaultColor(defaultR: Int, defaultG: Int, defaultB: Int, defaultA: Int) { defaultColor = Color(defaultR, defaultG, defaultB, defaultA) }
    override fun unsetDefaultColor() { defaultColor = Color(255, 255, 255, 255) }
}

class SchemMultiBufferSource: MultiBufferSource {
    val buffers = mutableMapOf<RenderType,  FakeBufferBuilder>()

    override fun getBuffer(renderType: RenderType): FakeBufferBuilder {
        val buf = buffers.getOrPut(renderType) { FakeBufferBuilder(this) }
        return buf
    }
}

class SchematicRenderer(val schem: IShipSchematic, val transparency: Float) {
    val matrixList: List<Matrix4f>
    val mySources = SchemMultiBufferSource()

    init {
        val info = schem.info!!.shipsInfo.associate { Pair(it.id, it) }
        val schem = schem as IShipSchematicDataV1

        //unpack data without positional info
        val random = RandomSource.create()
        val level = Minecraft.getInstance().level!!
        val poseStack = PoseStack()
        val blockRenderer = Minecraft.getInstance().blockRenderer

        var matrixIndex = 0
        matrixList = schem.blockData.map { (shipId, data) ->
            val levelWrapper = BlockAndTintGetterWrapper(level, data, schem.blockPalette)

            val infoItem = info[shipId]!!
            val rotationQuat = infoItem.rotation.get(Quaternionf())

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

                val offset = infoItem.previousCenterPosition.let { it.sub(it.x.roundToInt().toDouble(), it.y.roundToInt().toDouble(), it.z.roundToInt().toDouble(), JVector3d()) }

                val buffer = mySources.getBuffer(type)
                buffer.transparency = transparency

                buffer.vertexMatrixIndex = matrixIndex
                buffer.vertexPose = poseStack.last()
                buffer.vertexOffset.set(
                    bpos.x.toDouble() + offset.x,
                    bpos.y.toDouble() + offset.y,
                    bpos.z.toDouble() + offset.z,
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
    }

    fun render(sources: MultiBufferSource, poseStack: PoseStack) {
        val matrices = matrixList.map {
            poseStack.pushPose()
            poseStack.mulPoseMatrix(it)
            Matrix4f(poseStack.last().pose()).also { poseStack.popPose() }
        }

        mySources.buffers.forEach { (type, buf) ->
            val actualBuffer = sources.getBuffer(type)
            actualBuffer as BufferBuilderAccessor

            val vertexSize = (actualBuffer as BufferBuilderAccessor).`vmod$getVertexFormat`().vertexSize
            var size = buf.vertices.size * vertexSize
            (actualBuffer as BufferBuilderAccessor).`vmod$ensureCapacity`(size)

            buf.apply(actualBuffer, matrices)
        }
    }
}

class SchemRenderer(
    val schem: IShipSchematic,
    val rotationAngle: Ref<Double>,
    var transparency: Float = 0.5f
): BlockRenderer() {
    var renderer: SchematicRenderer? = null

    init {
        Thread {
            renderer = SchematicRenderer(schem, transparency)
        }.start()
    }

    override fun renderBlockData(poseStack: PoseStack, camera: Camera, sources: MultiBufferSource, timestamp: Long) {
        val mode = ClientToolGunState.currentMode
        if (mode !is SchemMode) {return}
        if (!ToolgunItem.playerIsUsingToolgun()) {return}
        val level = Minecraft.getInstance().level!!

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
            .get(Quaternionf())

        poseStack.pushPose()
        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)
        poseStack.translate(pos.x, pos.y, pos.z)
        poseStack.mulPose(rotation)

        renderer?.render(sources, poseStack)

        poseStack.popPose()
    }

    // only for internal use on client
    override fun serialize(): FriendlyByteBuf { throw AssertionError("Shouldn't be serialized") }
    override fun deserialize(buf: FriendlyByteBuf) { throw AssertionError("Shouldn't be deserialized") }
    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? { throw AssertionError("Shouldn't be copied") }
    override fun scaleBy(by: Double) { throw AssertionError("Shouldn't be scaled") }
}