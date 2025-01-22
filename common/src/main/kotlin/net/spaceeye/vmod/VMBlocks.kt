package net.spaceeye.vmod

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.spaceeye.vmod.VMItems.TAB
import net.spaceeye.vmod.blocks.SimpleMessager

private object NoTabBlocks {
    val BLOCKS = DeferredRegister.create(VM.MOD_ID, Registry.BLOCK_REGISTRY)

    var CONE_THRUSTER: RegistrySupplier<Block> = BLOCKS.register("cone_thruster") { Block(BlockBehaviour.Properties.of(Material.METAL)) }
}

object VMBlocks {
    private val BLOCKS = DeferredRegister.create(VM.MOD_ID, Registry.BLOCK_REGISTRY)

    var SIMPLE_MESSAGER: RegistrySupplier<SimpleMessager> = BLOCKS.register("simple_messager") { SimpleMessager(BlockBehaviour.Properties.of(Material.METAL).strength(2.0f)) }

    var CONE_THRUSTER = NoTabBlocks.CONE_THRUSTER

    fun register() {BLOCKS.register(); NoTabBlocks.BLOCKS.register()}
    fun registerItems(items: DeferredRegister<Item?>) {
        for (block in BLOCKS) {
            items.register(block.id) { BlockItem(block.get(), Item.Properties().tab(TAB)) }
        }
    }
}