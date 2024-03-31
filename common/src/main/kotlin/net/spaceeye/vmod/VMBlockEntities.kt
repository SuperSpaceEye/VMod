package net.spaceeye.vmod

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.Registrar
import dev.architectury.registry.registries.RegistrarManager.RegistryProvider
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.Util
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.core.BlockPos
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.entity.ai.attributes.DefaultAttributes
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.blockentities.SimpleMessagerBlockEntity

object VMBlockEntities {
    private val BLOCKENTITIES = DeferredRegister.create(VM.MOD_ID, Registries.BLOCK_ENTITY_TYPE)

    var SIMPLE_MESSAGER = VMBlocks.SIMPLE_MESSAGER makePair ::SimpleMessagerBlockEntity byName "simple_messager"

    private infix fun <T: BlockEntity, TT: Block> RegistrySupplier<TT>.makePair(blockEntity: (BlockPos, BlockState) -> T) = Pair(this, { bp: BlockPos, bs: BlockState -> blockEntity(bp, bs)})
    private infix fun <T: BlockEntity, TT: Block> Pair<RegistrySupplier<TT>, (BlockPos, BlockState) -> T>.byName(name: String): RegistrySupplier<BlockEntityType<T>> =
        BLOCKENTITIES.register(name) {
            val type = Util.fetchChoiceType(References.BLOCK_ENTITY, name)

            BlockEntityType.Builder.of(
                this.second,
                this.first.get()
            ).build(type)
        }

    fun register() {
        BLOCKENTITIES.register()
    }
}