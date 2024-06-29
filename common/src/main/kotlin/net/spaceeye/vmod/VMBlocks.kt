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

object VMBlocks {
    private val BLOCKS = DeferredRegister.create(VM.MOD_ID, Registry.BLOCK_REGISTRY)

    private val dontRegister = mutableSetOf<Block>()

    var SIMPLE_MESSAGER: RegistrySupplier<SimpleMessager> = BLOCKS.register("simple_messager") { SimpleMessager(BlockBehaviour.Properties.of(Material.METAL).strength(2.0f)) }

    var CONE_THRUSTER: RegistrySupplier<Block> = BLOCKS.register("cone_thruster") { noRegister(Block(BlockBehaviour.Properties.of(Material.METAL))) }

    private fun <T: Block>noRegister(block: T): T { dontRegister.add(block); return block}
    fun register() {BLOCKS.register()}
    fun registerItems(items: DeferredRegister<Item?>) {
        for (block in BLOCKS) {
            if (dontRegister.contains(block.get())) {continue}
            items.register(block.id) { BlockItem(block.get(), Item.Properties().tab(TAB)) }
        }
    }
}