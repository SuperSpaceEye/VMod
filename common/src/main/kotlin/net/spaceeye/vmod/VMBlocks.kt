package net.spaceeye.vmod

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.spaceeye.vmod.blocks.SimpleMessager
import net.spaceeye.vmod.VMItems.TAB

//TODO does it actually 100% work?
private object NoTabBlocks {
    val BLOCKS = DeferredRegister.create(VM.MOD_ID, Registries.BLOCK)

    var CONE_THRUSTER: RegistrySupplier<Block> = BLOCKS.register("cone_thruster") { Block(BlockBehaviour.Properties.of().strength(2.0f)) }
}

object VMBlocks {
    private val BLOCKS = DeferredRegister.create(VM.MOD_ID, Registries.BLOCK)

    var SIMPLE_MESSAGER: RegistrySupplier<SimpleMessager> = BLOCKS.register("simple_messager") { SimpleMessager(BlockBehaviour.Properties.of().strength(2.0f)) }
    var CONE_THRUSTER = NoTabBlocks.CONE_THRUSTER

    fun register() {BLOCKS.register(); NoTabBlocks.BLOCKS.register()}
    fun registerItems(items: DeferredRegister<Item?>) {
        for (block in BLOCKS) {
            items.register(block.id) { BlockItem(block.get(), Item.Properties().`arch$tab`(TAB)) }
        }
    }
}