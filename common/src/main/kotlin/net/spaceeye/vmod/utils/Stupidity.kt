package net.spaceeye.vmod.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelHeightAccessor
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkSource
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.level.chunk.DataLayer
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LightChunkGetter
import net.minecraft.world.level.entity.EntityAccess
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.lighting.LayerLightEventListener
import net.minecraft.world.level.lighting.LevelLightEngine
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.AABB
import net.minecraft.world.ticks.LevelChunkTicks
import net.minecraft.world.ticks.ScheduledTick
import net.minecraft.world.ticks.SerializableTickContainer
import net.minecraft.world.ticks.TickContainerAccess
import java.util.UUID
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Stream

class SchemLightEngine(): LevelLightEngine(object : LightChunkGetter {
    override fun getChunkForLighting(chunkX: Int, chunkZ: Int): BlockGetter? = null
    override fun getLevel(): BlockGetter? = null
}, false, false) {
    class SchemLayerListener(): LayerLightEventListener {
        override fun getLightValue(levelPos: BlockPos): Int = 15

        override fun getDataLayerData(sectionPos: SectionPos): DataLayer? = null
        override fun checkBlock(pos: BlockPos) {}
        override fun onBlockEmissionIncrease(pos: BlockPos, emissionLevel: Int) {}
        override fun hasLightWork(): Boolean = false
        override fun runUpdates(pos: Int, isQueueEmpty: Boolean, updateBlockLight: Boolean): Int = pos
        override fun updateSectionStatus(pos: SectionPos, isQueueEmpty: Boolean) {}
        override fun enableLightSources(chunkPos: ChunkPos, isQueueEmpty: Boolean) {}
    }
    val layerListener = SchemLayerListener()

    override fun getRawBrightness(blockPos: BlockPos, amount: Int): Int = 15
    override fun getLightSectionCount(): Int = 2000000
    override fun getMinLightSection(): Int = -1000000
    override fun getMaxLightSection(): Int = 1000000

    override fun checkBlock(pos: BlockPos) {}
    override fun onBlockEmissionIncrease(pos: BlockPos, emissionLevel: Int) {}
    override fun hasLightWork(): Boolean = false
    override fun runUpdates(pos: Int, isQueueEmpty: Boolean, updateBlockLight: Boolean): Int = pos
    override fun updateSectionStatus(pos: SectionPos, isQueueEmpty: Boolean) {}
    override fun enableLightSources(chunkPos: ChunkPos, isQueueEmpty: Boolean) {}
    override fun getLayerListener(type: LightLayer): LayerLightEventListener? = layerListener
    override fun getDebugData(lightLayer: LightLayer, sectionPos: SectionPos): String? = "n/a"
    override fun queueSectionData(type: LightLayer, pos: SectionPos, array: DataLayer?, bl: Boolean) {}
    override fun retainData(pos: ChunkPos, retain: Boolean) {}
}

class DummyLevelEntityGetter<T: EntityAccess?>(): LevelEntityGetter<T> {
    override fun get(id: Int): T? = null
    override fun get(uuid: UUID): T? = null
    override fun getAll(): Iterable<T?>? = listOf()
    override fun <U : T?> get(test: EntityTypeTest<T?, U?>, consumer: Consumer<U?>) {}
    override fun get(boundingBox: AABB, consumer: Consumer<T?>) {}
    override fun <U : T?> get(test: EntityTypeTest<T?, U?>, bounds: AABB, consumer: Consumer<U?>) {}
}

class FakeTickContainer<T>(): TickContainerAccess<T>, SerializableTickContainer<T> {
    override fun schedule(tick: ScheduledTick<T?>) {}
    override fun hasScheduledTick(pos: BlockPos, type: T): Boolean = false
    override fun count(): Int = 0
    override fun save(gameTime: Long, idGetter: Function<T?, String?>): Tag? = CompoundTag()
}

class FakeChunkAccess(level: Level): LevelChunk(level, ChunkPos(0, 0), null, LevelChunkTicks(), LevelChunkTicks(), 0L, Array(0) {null}, null, null) {
    override fun setBlockState(pos: BlockPos, state: BlockState, isMoving: Boolean): BlockState? = state
    override fun setBlockEntity(blockEntity: BlockEntity) {}
    override fun addEntity(entity: Entity) {}
    override fun getStatus(): ChunkStatus? = ChunkStatus.EMPTY
    override fun removeBlockEntity(pos: BlockPos) {}
    override fun getBlockEntityNbtForSaving(pos: BlockPos): CompoundTag? = CompoundTag()
    override fun getLights(): Stream<BlockPos?>? = Stream.of<BlockPos>()
    override fun getBlockTicks(): TickContainerAccess<Block?>? = FakeTickContainer()
    override fun getFluidTicks(): TickContainerAccess<Fluid?>? = FakeTickContainer()
    override fun getTicksForSerialization(): TicksToSave? = TicksToSave(FakeTickContainer(), FakeTickContainer())
    override fun getBlockEntity(pos: BlockPos): BlockEntity? = null
    override fun getBlockState(pos: BlockPos): BlockState? = Blocks.AIR.defaultBlockState()
    override fun getFluidState(pos: BlockPos): FluidState? = Blocks.AIR.defaultBlockState().fluidState
}

class DummyChunkSource(val level: Level, val levelLightEngine: LevelLightEngine): ChunkSource() {
    val access = FakeChunkAccess(level)
    override fun getChunk(chunkX: Int, chunkZ: Int, requiredStatus: ChunkStatus, load: Boolean): ChunkAccess? = access
    override fun tick(hasTimeLeft: BooleanSupplier, tickChunks: Boolean) {}
    override fun gatherStats(): String? = ""
    override fun getLoadedChunksCount(): Int = 0
    override fun getLightEngine(): LevelLightEngine? = levelLightEngine
    override fun getLevel(): BlockGetter? = level
}