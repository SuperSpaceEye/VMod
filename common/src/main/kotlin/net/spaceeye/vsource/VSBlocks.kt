package net.spaceeye.vsource

import dev.architectury.registry.registries.DeferredRegister
import net.minecraft.core.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.spaceeye.vsource.VSItems.TAB
import net.spaceeye.vsource.blocks.SimpleMessager

object VSBlocks {
    private val BLOCKS = DeferredRegister.create(VS.MOD_ID, Registry.BLOCK_REGISTRY)

    var SIMPLE_MESSAGER = BLOCKS.register("simple_messager") { SimpleMessager(BlockBehaviour.Properties.of(Material.METAL).strength(2.0f)) }

    fun register() {BLOCKS.register()}
    fun registerItems(items: DeferredRegister<Item?>) {
        for (block in BLOCKS) {
            items.register(block.id) { BlockItem(block.get(), Item.Properties().tab(TAB)) }
        }
    }
}