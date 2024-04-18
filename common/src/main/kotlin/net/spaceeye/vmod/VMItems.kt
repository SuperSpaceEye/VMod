package net.spaceeye.vmod

import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.vmod.toolgun.ToolgunItem

object VMItems {
    private val ITEMS = DeferredRegister.create(VM.MOD_ID, Registries.ITEM)

    private val TABS = DeferredRegister.create(VM.MOD_ID, Registries.CREATIVE_MODE_TAB)

    val TAB: RegistrySupplier<CreativeModeTab> = TABS.register(
        "vmod_tab"
    ) {
        CreativeTabRegistry.create(
            Component.translatable("itemGroup.valkyrien_mod.vmod_tab")
        ) { ItemStack(LOGO.get()) }
    }

    var LOGO: RegistrySupplier<Item> = ITEMS.register("vmod_logo") { Item(Item.Properties()) }

    var TOOLGUN: RegistrySupplier<ToolgunItem> = ITEMS.register("toolgun") { ToolgunItem() }

    fun register() {
        VMBlocks.registerItems(ITEMS)
        ITEMS.register()
        TABS.register()
    }
}