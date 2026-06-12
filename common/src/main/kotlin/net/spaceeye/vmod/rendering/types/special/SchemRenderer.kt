package net.spaceeye.vmod.rendering.types.special

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
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
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.ColorResolver
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.gameevent.GameEvent
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
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.rendering.RenderTypes
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.BlockRenderer
import net.spaceeye.vmod.toolgun.VMToolgun
import net.spaceeye.vmod.toolgun.modes.state.SchemMode
import net.spaceeye.vmod.utils.DummyChunkSource
import net.spaceeye.vmod.utils.DummyLevelEntityGetter
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Ref
import net.spaceeye.vmod.utils.SchemLightEngine
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getQuatFromDir
import org.joml.AxisAngle4d
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3i
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.util.function.Supplier
import kotlin.math.roundToInt

class FakeLevel(
    val level: ClientLevel,
    val data: ChunkyBlockData<BlockItem>,
    flatTagData: List<CompoundTag>,
    val palette: IBlockStatePalette,
    val infoItem: IShipInfo
): Level(
    null, level.dimension(), level.registryAccess(), level.dimensionTypeRegistration(), Supplier{level.profiler}, true, false, 0L, 0
) {
    val defaultState = Blocks.AIR.defaultBlockState()
    val defaultFluidState = Fluids.EMPTY.defaultFluidState()
    var offset = Vector3i(0, 0, 0)

    val blockEntities = mutableListOf<Pair<BlockPos, BlockEntity>>()
    private val dummyLightEngine = SchemLightEngine()
    private val fakeChunkSource = DummyChunkSource(this, dummyLightEngine)

    init {
        data.forEach { x, y, z, item ->
            try {
                val state = palette.fromId(item.paletteId) ?: return@forEach
                if (!state.hasBlockEntity()) return@forEach
                val be = (state.block as EntityBlock).newBlockEntity(BlockPos(x, y, z), state) ?: return@forEach
                be.level = this
                if (item.extraDataId != -1 && flatTagData.size > item.extraDataId) {
                    be.load(flatTagData[item.extraDataId].copy())
                }
                blockEntities.add(BlockPos(x, y, z) to be)
            } catch (e: Exception) {
                ELOG("Failed to load block entity\n${e.stackTraceToString()}")
            }
        }
    }

    override fun getShade(direction: Direction, shade: Boolean): Float = 1f

    override fun getBlockTint(blockPos: BlockPos, colorResolver: ColorResolver): Int {
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

    override fun getLightEngine(): LevelLightEngine? = dummyLightEngine
    override fun getHeight(): Int = 2000000
    override fun getMinBuildHeight(): Int = -1000000
    override fun getSectionsCount(): Int = 0
    override fun getMinSection(): Int = 0
    override fun getMaxSection(): Int = 0
    override fun isClientSide(): Boolean = true
    override fun blockEntityChanged(pos: BlockPos) {}
    override fun playSound(player: Player?, x: Double, y: Double, z: Double, sound: SoundEvent, category: SoundSource, volume: Float, pitch: Float) {}
    override fun playSound(player: Player?, entity: Entity, event: SoundEvent, category: SoundSource, volume: Float, pitch: Float) {}
    override fun setMapData(mapId: String, data: MapItemSavedData) {}
    override fun registryAccess(): RegistryAccess? { return Minecraft.getInstance().level!!.registryAccess() }
    override fun enabledFeatures(): FeatureFlagSet? = FeatureFlagSet.of()
    override fun levelEvent(player: Player?, type: Int, pos: BlockPos, data: Int) {}
    override fun gameEvent(event: GameEvent, position: Vec3, context: GameEvent.Context) {}

    override fun gameEvent(entity: Entity?, event: GameEvent, pos: BlockPos) {}
    override fun sendBlockUpdated(pos: BlockPos, oldState: BlockState, newState: BlockState, flags: Int) {}
    override fun destroyBlockProgress(breakerId: Int, pos: BlockPos, progress: Int) {}
    override fun getEntities(): LevelEntityGetter<Entity?>? = DummyLevelEntityGetter<Entity?>()
    override fun players(): List<AbstractClientPlayer?>? = emptyList()
    override fun getFreeMapId(): Int = 0
    override fun getEntity(id: Int): Entity? = null

    override fun getChunkSource() = fakeChunkSource

    override fun getUncachedNoiseBiome(x: Int, y: Int, z: Int): Holder<Biome?>? { throw AssertionError("Shouldn't be called") }
    override fun gatherChunkSourceStats(): String? { throw AssertionError("Shouldn't be called")  }
    override fun getMapData(mapName: String): MapItemSavedData? { throw AssertionError("Shouldn't be called")  }
    override fun getScoreboard(): Scoreboard? { throw AssertionError("Shouldn't be called")  }
    override fun getRecipeManager(): RecipeManager? { throw AssertionError("Shouldn't be called")  }
    override fun getBlockTicks(): LevelTickAccess<Block?>? { throw AssertionError("Shouldn't be called")  }
    override fun getFluidTicks(): LevelTickAccess<Fluid?>? { throw AssertionError("Shouldn't be called")  }
}

class SchemMultiBufferSource : MultiBufferSource {
    val buffers = mutableMapOf<RenderType, BufferBuilder>()

    override fun getBuffer(renderType: RenderType): BufferBuilder {
        return buffers.getOrPut(renderType) {
            BufferBuilder(renderType.bufferSize()).apply {
                begin(renderType.mode(), renderType.format())
            }
        }
    }

    fun endAll(): Map<RenderType, BufferBuilder.RenderedBuffer> {
        return buffers.mapValues { (_, builder) -> builder.end() }
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

class OffsetVertexConsumer(
    val delegate: VertexConsumer,
    val offsetX: Double,
    val offsetY: Double,
    val offsetZ: Double
) : VertexConsumer {
    override fun vertex(x: Double, y: Double, z: Double): VertexConsumer? {
        return delegate.vertex(x + offsetX, y + offsetY, z + offsetZ)
    }
    override fun color(red: Int, green: Int, blue: Int, alpha: Int) = delegate.color(red, green, blue, alpha)
    override fun uv(u: Float, v: Float) = delegate.uv(u, v)
    override fun overlayCoords(u: Int, v: Int) = delegate.overlayCoords(u, v)
    override fun uv2(u: Int, v: Int) = delegate.uv2(u, v)
    override fun normal(x: Float, y: Float, z: Float) = delegate.normal(x, y, z)
    override fun endVertex() = delegate.endVertex()
    override fun defaultColor(defaultR: Int, defaultG: Int, defaultB: Int, defaultA: Int) = delegate.defaultColor(defaultR, defaultG, defaultB, defaultA)
    override fun unsetDefaultColor() = delegate.unsetDefaultColor()
    override fun vertex(x: Float, y: Float, z: Float, red: Float, green: Float, blue: Float, alpha: Float, texU: Float, texV: Float, overlayUV: Int, lightmapUV: Int, normalX: Float, normalY: Float, normalZ: Float) = delegate.vertex(x + offsetX.toFloat(), y + offsetY.toFloat(), z + offsetZ.toFloat(), red, green, blue, alpha, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ)
}

class TransparencyWrapperBufferSource(val source: MultiBufferSource, val transparency: Float): MultiBufferSource {
    override fun getBuffer(renderType: RenderType): VertexConsumer? {
        val buf = source.getBuffer(renderType)
        return TransparencyWrapperVertexConsumer(buf, transparency)
    }
}

data class ShipRenderData(
    val renderedBuffers: Map<RenderType, BufferBuilder.RenderedBuffer>,
    val blockEntities: MutableList<Pair<BlockPos, BlockEntity>>,
    val localMatrix: Matrix4f,
    val infoItem: IShipInfo
) {
    var vertexBuffers: Map<RenderType, VertexBuffer> = emptyMap()
}

class SchematicRenderer(
    val schem: IShipSchematic,
    val transparency: Float,
    val renderBlockEntities: Boolean = true
) {
    val ships = mutableListOf<ShipRenderData>()
    private val mySources = SchemMultiBufferSource()

    init {
        val info = schem.info!!.shipsInfo.associate { it.id to it }
        val schemData = schem as IShipSchematicDataV1
        val level = Minecraft.getInstance().level!!
        val blockRenderer = Minecraft.getInstance().blockRenderer
        val random = RandomSource.create()

        schemData.blockData.forEach { (shipId, data) ->
            val infoItem = info[shipId]!!
            val flevel = FakeLevel(level, data, schemData.flatTagData, schemData.blockPalette, infoItem)

            val poseStack = PoseStack()
            val offset = infoItem.previousCenterPosition.let {
                it.sub(it.x.roundToInt().toDouble(), it.y.roundToInt().toDouble(), it.z.roundToInt().toDouble(), JVector3d())
            }

            data.forEach { x, y, z, item ->
                val bpos = BlockPos(x, y, z)
                val state = flevel.getBlockState(bpos) ?: return@forEach

                val type = when (state.fluidState.isEmpty) {
                    true -> RenderTypes.schematicBlock.type
                    false -> ItemBlockRenderTypes.getRenderLayer(state.fluidState)
                }

                val buffer = mySources.getBuffer(type)

                if (state.fluidState.isEmpty) {
                    poseStack.pushPose()
                    poseStack.translate(bpos.x.toDouble(), bpos.y.toDouble(), bpos.z.toDouble())
                    blockRenderer.renderBatched(state, bpos, flevel, poseStack, buffer, true, random)
                    poseStack.popPose()
                } else {
                    flevel.offset.set(bpos.x, bpos.y, bpos.z)
                    val wrappedBuffer = OffsetVertexConsumer(buffer, bpos.x.toDouble(), bpos.y.toDouble(), bpos.z.toDouble())
                    blockRenderer.renderLiquid(BlockPos(0, 0, 0), flevel, wrappedBuffer, state, state.fluidState)
                    flevel.offset.set(0, 0, 0)
                }
            }

            val localMatrix = Matrix4f()
                .translate(
                    infoItem.relPositionToCenter.x.toFloat(),
                    infoItem.relPositionToCenter.y.toFloat(),
                    infoItem.relPositionToCenter.z.toFloat()
                )
                .rotate(infoItem.rotation.get(Quaternionf()))
                .scale(infoItem.shipScale.toFloat())
                .translate(offset.x.toFloat(), offset.y.toFloat(), offset.z.toFloat())

            ships.add(ShipRenderData(
                renderedBuffers = mySources.endAll(),
                blockEntities = flevel.blockEntities,
                localMatrix = localMatrix,
                infoItem = infoItem
            ))
        }
    }

    fun uploadBuffers() {
        ships.forEach { ship ->
            ship.vertexBuffers = ship.renderedBuffers.mapValues { (_, renderedBuffer) ->
                VertexBuffer(VertexBuffer.Usage.STATIC).apply {
                    bind()
                    upload(renderedBuffer)
                    VertexBuffer.unbind()
                }
            }
        }
    }

    fun render(sources: MultiBufferSource, poseStack: PoseStack) {
        val projection = RenderSystem.getProjectionMatrix()
        val renderer = Minecraft.getInstance().blockEntityRenderDispatcher

        ships.forEach { ship ->
            poseStack.pushPose()
            poseStack.mulPoseMatrix(ship.localMatrix)

            RenderSystem.setShaderColor(1f, 1f, 1f, transparency)

            ship.vertexBuffers.forEach { (renderType, vertexBuffer) ->
                renderType.setupRenderState()
                val shader = RenderSystem.getShader()
                vertexBuffer.bind()
                vertexBuffer.drawWithShader(poseStack.last().pose(), projection, shader)
                VertexBuffer.unbind()
                renderType.clearRenderState()
            }

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            if (renderBlockEntities) {
                val wrappedSources = TransparencyWrapperBufferSource(sources, transparency)
                val toRemove = mutableListOf<Int>()
                ship.blockEntities.forEachIndexed { i, (pos, be) ->
                    val beRenderer = renderer.getRenderer(be) ?: run { toRemove.add(i); return@forEachIndexed }
                    if (!be.type.isValid(be.blockState)) { toRemove.add(i); return@forEachIndexed }

                    poseStack.pushPose()
                    poseStack.translate(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

                    try {
                        val bePose = PoseStack()
                        bePose.setIdentity()
                        bePose.mulPoseMatrix(poseStack.last().pose())
                        beRenderer.render(be, 0f, bePose, wrappedSources, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                    } catch (e: Exception) {
                        WLOG("Failed to render block entity\n${e.stackTraceToString()}")
                        toRemove.add(i)
                    }
                    poseStack.popPose()
                }
                toRemove.asReversed().forEach { ship.blockEntities.removeAt(it) }
            }

            poseStack.popPose()
        }
    }
}

open class SchemRenderer(
    val schem: IShipSchematic,
    val rotationAngle: Ref<Double>,
    var transparency: Float = 0.5f,
    var renderBlockEntities: Boolean,
    var doRender: () -> Boolean = { VMToolgun.client.currentMode is SchemMode && VMToolgun.client.playerIsUsingToolgun() }
): BlockRenderer() {
    @Volatile var renderer: SchematicRenderer? = null

    init { init() }

    open fun init() {
        Thread {
            val baked = SchematicRenderer(schem, transparency, renderBlockEntities)
            Minecraft.getInstance().execute {
                baked.uploadBuffers()
                renderer = baked
            }
        }.start()
    }

    override fun renderBlockData(poseStack: PoseStack, camera: Camera, sources: MultiBufferSource, timestamp: Long) {
        if (!doRender()) return
        val r = renderer ?: return
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

        renderBlocks(poseStack, sources, camera, pos, raycastResult.worldNormalDirection!!, rotationAngle.it)
    }

    open fun renderBlocks(poseStack: PoseStack, sources: MultiBufferSource, camera: Camera, pos: Vector3d, worldNormal: Vector3d, rotationAroundNormal: Double) {
        val r = renderer ?: return
        renderBlocksInternal(poseStack, sources, camera, pos, worldNormal, rotationAroundNormal, r)
    }

    private fun renderBlocksInternal(poseStack: PoseStack, sources: MultiBufferSource, camera: Camera, pos: Vector3d, worldNormal: Vector3d, rotationAroundNormal: Double, renderer: SchematicRenderer) {
        val rotation = Quaterniond()
            .mul(Quaterniond(AxisAngle4d(rotationAroundNormal, worldNormal.toJomlVector3d())))
            .mul(getQuatFromDir(worldNormal))
            .normalize()
            .get(Quaternionf())

        poseStack.pushPose()
        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)
        poseStack.translate(pos.x, pos.y, pos.z)
        poseStack.mulPose(rotation)

        renderer.render(sources, poseStack)

        poseStack.popPose()
    }

    override fun serialize(): FriendlyByteBuf = throw AssertionError("Shouldn't be serialized")
    override fun deserialize(buf: FriendlyByteBuf) { throw AssertionError("Shouldn't be deserialized") }
    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? = throw AssertionError("Shouldn't be copied")
    override fun scaleBy(by: Double) { throw AssertionError("Shouldn't be scaled") }
}