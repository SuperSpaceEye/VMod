package net.spaceeye.vmod.rendering.types.special

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Matrix4f
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientChunkCache
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.core.SectionPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ColorResolver
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
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
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.ELOG
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
import java.awt.Color
import java.util.function.Supplier
import kotlin.math.roundToInt

//TODO optimize more
//TODO won't work with sodium cuz it uses it's own buffer builder
//fun maybeFasterVertexBuilder(buffer: VertexConsumer, x: Float, y: Float, z: Float, r: Byte, g: Byte, b: Byte, a: Byte, u: Float, v: Float, combinedOverlay: Int, lightmapUV: Int, normalX: Float, normalY: Float, normalZ: Float) {
//    buffer as BufferBuilder
//    buffer as BufferBuilderAccessor
//
//    buffer.putFloat(0, x)
//    buffer.putFloat(4, y)
//    buffer.putFloat(8, z)
//    buffer.putByte(12, r)
//    buffer.putByte(13, g)
//    buffer.putByte(14, b)
//    buffer.putByte(15, a)
//    buffer.putFloat(16, u)
//    buffer.putFloat(20, v)
//    val i = if (buffer.`vmod$fullFormat`()) {
//        buffer.putShort(24, (combinedOverlay and 0xffff).toShort())
//        buffer.putShort(26, (combinedOverlay shr 16 and 0xffff).toShort())
//        28
//    } else { 24 }
//
//    buffer.putShort(i + 0, (lightmapUV and 0xffff).toShort())
//    buffer.putShort(i + 2, (lightmapUV shr 16 and 0xffff).toShort())
//    buffer.putByte(i + 4, BufferVertexConsumer.normalIntValue(normalX))
//    buffer.putByte(i + 5, BufferVertexConsumer.normalIntValue(normalY))
//    buffer.putByte(i + 6, BufferVertexConsumer.normalIntValue(normalZ))
//
//    buffer.`vmod$nextElementByte`(buffer.`vmod$nextElementByte`() + i + 8)
//
//    //not using endVertex to not call ensureCapacity
//    buffer.`vmod$vertices`(buffer.`vmod$vertices`() + 1)
//}

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

class DummyLevelData(): ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, false) {
    override fun getXSpawn(): Int = 0
    override fun getYSpawn(): Int = 0
    override fun getZSpawn(): Int = 0
    override fun getSpawnAngle(): Float = 0f
    override fun getGameTime(): Long = 0L
    override fun getDayTime(): Long = 0L
    override fun isThundering(): Boolean = false
    override fun isRaining(): Boolean = false
    override fun setRaining(raining: Boolean) {}
    override fun isHardcore(): Boolean = false
    override fun getGameRules(): GameRules? = Minecraft.getInstance().level!!.gameRules
    override fun getDifficulty(): Difficulty? = Difficulty.PEACEFUL
    override fun isDifficultyLocked(): Boolean = true
    override fun setXSpawn(xSpawn: Int) {}
    override fun setYSpawn(ySpawn: Int) {}
    override fun setZSpawn(zSpawn: Int) {}
    override fun setSpawnAngle(spawnAngle: Float) {}
}

//null, DummyLevelData(), level.dimension(), level.dimensionTypeRegistration(), 0, 0, null, null, level.isDebug, 0L
class FakeClientLevel(
    val level: ClientLevel,
    val data: ChunkyBlockData<BlockItem>,
    flatTagData: List<CompoundTag>,
    val palette: IBlockStatePalette,
    val infoItem: IShipInfo
): ClientLevel(
    null, DummyLevelData(), level.dimension(), level.registryAccess(), level.dimensionTypeRegistration(), Supplier{level.profiler}, level.isClientSide, level.isDebug, 0L, 0
) {
    val defaultState = Blocks.AIR.defaultBlockState()
    val defaultFluidState = Fluids.EMPTY.defaultFluidState()
    var offset = Vector3i(0, 0, 0)

    var blockEntities = mutableListOf<Pair<BlockPos, BlockEntity>>()

    init {
        data.forEach { x, y, z, item ->
            try {
                val state = palette.fromId(item.paletteId) ?: return@forEach
                if (!state.hasBlockEntity()) {return@forEach}
                val be = (state.block as EntityBlock).newBlockEntity(BlockPos(x, y, z), state) ?: return@forEach
                be.level = this
                if (item.extraDataId != -1 && flatTagData.size > item.extraDataId) {
                    be.load(flatTagData[item.extraDataId].copy())
                }
                blockEntities.add(BlockPos(x, y, z) to be)
            } catch (e: Exception) { ELOG("Failed to load block entity\n${e.stackTraceToString()}")
            } catch (e: Error) { ELOG("Failed to load block entity\n${e.stackTraceToString()}") }
        }
    }

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
    override fun isClientSide(): Boolean = true
    override fun blockEntityChanged(pos: BlockPos) {}
    override fun playSound(player: Player?, x: Double, y: Double, z: Double, sound: SoundEvent, category: SoundSource, volume: Float, pitch: Float) {}
    override fun playSound(player: Player?, entity: Entity, event: SoundEvent, category: SoundSource, volume: Float, pitch: Float) {}
    override fun setMapData(mapId: String, data: MapItemSavedData) {}
    override fun registryAccess(): RegistryAccess? { return Minecraft.getInstance().level!!.registryAccess() }
    override fun levelEvent(player: Player?, type: Int, pos: BlockPos, data: Int) {}
    override fun gameEvent(entity: Entity?, event: GameEvent, pos: BlockPos) {}
    override fun sendBlockUpdated(pos: BlockPos, oldState: BlockState, newState: BlockState, flags: Int) {}
    override fun destroyBlockProgress(breakerId: Int, pos: BlockPos, progress: Int) {}
    override fun getEntities(): LevelEntityGetter<Entity?>? = DummyLevelEntityGetter<Entity>()

    override fun getUncachedNoiseBiome(x: Int, y: Int, z: Int): Holder<Biome?>? { throw AssertionError("Shouldn't be called") }
    override fun gatherChunkSourceStats(): String? { throw AssertionError("Shouldn't be called")  }
    override fun getEntity(id: Int): Entity? { throw AssertionError("Shouldn't be called")  }
    override fun getMapData(mapName: String): MapItemSavedData? { throw AssertionError("Shouldn't be called")  }
    override fun getFreeMapId(): Int { throw AssertionError("Shouldn't be called")  }
    override fun getScoreboard(): Scoreboard? { throw AssertionError("Shouldn't be called")  }
    override fun getRecipeManager(): RecipeManager? { throw AssertionError("Shouldn't be called")  }
    override fun getBlockTicks(): LevelTickAccess<Block?>? { throw AssertionError("Shouldn't be called")  }
    override fun getFluidTicks(): LevelTickAccess<Fluid?>? { throw AssertionError("Shouldn't be called")  }
    override fun getChunkSource(): ClientChunkCache? { throw AssertionError("Shouldn't be called")  }
    override fun players(): List<AbstractClientPlayer?>? { throw AssertionError("Shouldn't be called")  }
}

class FakeBufferBuilder(val source: SchemMultiBufferSource): VertexConsumer {
    val vertices = mutableListOf<Vertex>()

    var defaultColor = Color(255, 255, 255, 255)
    var transparency = 0.5f

    var vertexOffset = org.joml.Vector3f(0f, 0f, 0f)
    var vertexMatrixIndex = 0

    data class Vertex(var x: Float, var y: Float, var z: Float, var red: Float, var green: Float, var blue: Float, var alpha: Float, var texU: Float, var texV: Float, var overlayUV: Int, var lightmapUV: Int, var normalX: Float, var normalY: Float, var normalZ: Float, var matrixIndex: Int) {
        fun apply(buffer: VertexConsumer, transparency: Float, matrices: List<org.joml.Matrix4f>) {
            val matrix = matrices[matrixIndex]

            var x_ = matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30()
            var y_ = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31()
            var z_ = matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32()

            buffer.vertex(x_, y_, z_, red, green, blue, alpha * transparency, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ)
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
    var transparency: Float = 0.5f

    override fun getBuffer(renderType: RenderType): FakeBufferBuilder {
        val buf = buffers.getOrPut(renderType) { FakeBufferBuilder(this) }
        buf.transparency = transparency
        return buf
    }
}

class TransparencyWrapperVertexConsumer(val b: VertexConsumer, val transparency: Float): VertexConsumer {
    override fun vertex(x: Double, y: Double, z: Double) = b.vertex(x, y, z)
    override fun color(red: Int, green: Int, blue: Int, alpha: Int) = b.color(red, green, blue, ((transparency * (alpha.toUByte().toInt() / 255f)) * 255f).toInt())
    override fun uv(u: Float, v: Float) = b.uv(u, v)
    override fun overlayCoords(u: Int, v: Int) = b.overlayCoords(u, v)
    override fun uv2(u: Int, v: Int) = b.uv2(u, v)
    override fun normal(x: Float, y: Float, z: Float) = b.normal(x, y, z)
    override fun endVertex() = b.endVertex()
    override fun defaultColor(defaultR: Int, defaultG: Int, defaultB: Int, defaultA: Int) = b.defaultColor(defaultR, defaultG, defaultB, ((transparency * (defaultA.toUByte().toInt() / 255f)) * 255f).toInt())
    override fun unsetDefaultColor() = b.unsetDefaultColor()
    override fun vertex(x: Float, y: Float, z: Float, red: Float, green: Float, blue: Float, alpha: Float, texU: Float, texV: Float, overlayUV: Int, lightmapUV: Int, normalX: Float, normalY: Float, normalZ: Float) = b.vertex(x, y, z, red, green, blue, alpha * transparency, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ)
}

class TransparencyWrapperBufferSource(val source: MultiBufferSource, val transparency: Float): MultiBufferSource {
    override fun getBuffer(renderType: RenderType): VertexConsumer? {
        //TODO renderType may not support transparency, but i can't just use renderType that supports transparency cuz it doesn't always work
        val buf = source.getBuffer(renderType)
        return TransparencyWrapperVertexConsumer(buf, transparency)
    }
}

class SchematicRenderer(val schem: IShipSchematic, val transparency: Float, val renderBlockEntities: Boolean = true) {
    val matrixList: List<Matrix4f>
    var fakeLevels = mutableListOf<FakeClientLevel>()
    val mySources = SchemMultiBufferSource()

    init {
        val info = schem.info!!.shipsInfo.associate { Pair(it.id, it) }
        val schem = schem as IShipSchematicDataV1

        mySources.transparency = transparency

        //unpack data without positional info
        val random = RandomSource.create()
        val level = Minecraft.getInstance().level!!
        val poseStack = PoseStack()
        val blockRenderer = Minecraft.getInstance().blockRenderer

        var matrixIndex = 0
        matrixList = schem.blockData.map { (shipId, data) ->
            val infoItem = info[shipId]!!
            val rotationQuat = infoItem.rotation.get(Quaternionf())

            val flevel = FakeClientLevel(level, data, schem.flatTagData, schem.blockPalette, infoItem)
            fakeLevels.add(flevel)

            //should be in the rotated world frame
            poseStack.pushPose()
            poseStack.translate(
                infoItem.relPositionToCenter.x,
                infoItem.relPositionToCenter.y,
                infoItem.relPositionToCenter.z,
            )
            //now in the ship frame
            poseStack.mulPose(rotationQuat)
            infoItem.shipScale.toFloat().also {
                poseStack.scale(it, it, it)
            }

            data.forEach { x, y, z, item ->
                val bpos = BlockPos(x, y, z)
                val state = flevel.getBlockState(bpos) ?: return@forEach

                val type = if (state.fluidState.isEmpty) {
                    RenderType.translucent()
                } else {
                    ItemBlockRenderTypes.getRenderLayer(state.fluidState)
                }

                val offset = infoItem.previousCenterPosition.let { it.sub(it.x.roundToInt().toDouble(), it.y.roundToInt().toDouble(), it.z.roundToInt().toDouble(), JVector3d()) }

                val buffer = mySources.getBuffer(type)

                buffer.vertexMatrixIndex = matrixIndex
                buffer.vertexOffset.set(
                    bpos.x.toDouble() + offset.x,
                    bpos.y.toDouble() + offset.y,
                    bpos.z.toDouble() + offset.z,
                )

                if (state.fluidState.isEmpty) {
                    blockRenderer.renderBatched(state, bpos, flevel, PoseStack(), buffer, true, random)
                } else {
                    //renderLiquid reduces position to a chunk so doing this is easier
                    flevel.offset.set(bpos.x, bpos.y, bpos.z)
                    blockRenderer.renderLiquid(BlockPos(0, 0, 0), flevel, buffer, state, state.fluidState)
                    flevel.offset.set(0, 0, 0)
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
//            actualBuffer as BufferBuilderAccessor
//
//            val vertexSize = (actualBuffer as BufferBuilderAccessor).`vmod$getVertexFormat`().vertexSize
//            var size = buf.vertices.size * vertexSize
//            (actualBuffer as BufferBuilderAccessor).`vmod$ensureCapacity`(size)

            buf.apply(actualBuffer, matrices)
        }

        if (!renderBlockEntities) {return}
        val renderer = Minecraft.getInstance().blockEntityRenderDispatcher
        val sources = TransparencyWrapperBufferSource(sources, transparency)
        fakeLevels.forEach { level ->
            val infoItem = level.infoItem
            poseStack.pushPose()
            poseStack.translate(
                infoItem.relPositionToCenter.x,
                infoItem.relPositionToCenter.y,
                infoItem.relPositionToCenter.z,
            )
            poseStack.mulPose(infoItem.rotation.toMinecraft())
            infoItem.shipScale.toFloat().also {
                poseStack.scale(it, it, it)
            }

            val toRemove = mutableListOf<Int>()
            level.blockEntities.forEachIndexed {i, (pos, be) ->
                val beRenderer = renderer.getRenderer(be) ?: return@forEachIndexed Unit.also { toRemove.add(i) }
                if (!be.type.isValid(be.blockState)) return@forEachIndexed Unit.also { toRemove.add(i) }

                poseStack.pushPose()

                val offset = infoItem.previousCenterPosition.let { it.sub(it.x.roundToInt().toDouble(), it.y.roundToInt().toDouble(), it.z.roundToInt().toDouble(), JVector3d()) }

                poseStack.translate(
                    pos.x.toDouble() + offset.x,
                    pos.y.toDouble() + offset.y,
                    pos.z.toDouble() + offset.z,
                )

                try {
                    PoseStack().also {
                        it.setIdentity()
                        it.mulPoseMatrix(poseStack.last().pose())
                        beRenderer.render(be, 0f, it, sources, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    }
                } catch (e: Exception) { ELOG("Failed to render block entity\n${e.stackTraceToString()}"); toRemove.add(i)
                } catch (e: Error) { ELOG("Failed to render block entity\n${e.stackTraceToString()}"); toRemove.add(i) }
                poseStack.popPose()
            }
            toRemove.reversed().forEach { level.blockEntities.removeAt(it) }
            poseStack.popPose()
        }
    }
}

class SchemRenderer(
    val schem: IShipSchematic,
    val rotationAngle: Ref<Double>,
    var transparency: Float = 0.5f,
    var renderBlockEntities: Boolean
): BlockRenderer() {
    var renderer: SchematicRenderer? = null

    init {
        Thread {
            renderer = SchematicRenderer(schem, transparency, renderBlockEntities)
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