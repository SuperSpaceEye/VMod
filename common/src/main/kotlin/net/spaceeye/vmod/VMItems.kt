package net.spaceeye.vmod

import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.vmod.toolgun.ToolgunItem

object VMItems {
    val ITEMS = DeferredRegister.create(VM.MOD_ID, Registry.ITEM_REGISTRY)

    val TAB: CreativeModeTab = CreativeTabRegistry.create(
        ResourceLocation(
            VM.MOD_ID,
            "vmod_tab"
        )
    ) { ItemStack(LOGO.get()) }

    var LOGO: RegistrySupplier<Item> = ITEMS.register("vmod_logo") { Item(Item.Properties()) }

    var TOOLGUN: RegistrySupplier<ToolgunItem> = ITEMS.register("toolgun") { ToolgunItem() }

    fun register() {
        VMBlocks.registerItems(ITEMS)
        ITEMS.register()
    }
}