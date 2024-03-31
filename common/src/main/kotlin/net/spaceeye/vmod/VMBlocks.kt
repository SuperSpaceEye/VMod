package net.spaceeye.vmod

import dev.architectury.registry.registries.DeferredRegister
import net.minecraft.client.resources.model.Material
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockBehaviour
import net.spaceeye.vmod.blocks.SimpleMessager

object VMBlocks {
    private val BLOCKS = DeferredRegister.create(VM.MOD_ID, Registries.BLOCK)

    var SIMPLE_MESSAGER = BLOCKS.register("simple_messager") { SimpleMessager(BlockBehaviour.Properties.of().strength(2.0f)) }

    fun register() {BLOCKS.register()}
    fun registerItems(items: DeferredRegister<Item?>) {
        for (block in BLOCKS) {
            items.register(block.id) { BlockItem(block.get(), Item.Properties()) }
        }
    }
}