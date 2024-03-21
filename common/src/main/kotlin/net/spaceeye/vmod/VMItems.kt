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

//    var AXIS_CREATOR: RegistrySupplier<Item> = ITEMS.register("axis_creator") { Item(Item.Properties()) }
//    var BALL_SOCKET_CREATOR: RegistrySupplier<Item> = ITEMS.register("ball_socket_creator") {  }
//    var ELASTIC_CREATOR: RegistrySupplier<Item> = ITEMS.register("elastic_creator") { Item(Item.Properties()) }
//    var MOTOR_CREATOR: RegistrySupplier<Item> = ITEMS.register("motor_creator") { Item(Item.Properties()) }
//    var PULLEY_CREATOR: RegistrySupplier<Item> = ITEMS.register("pulley_creator") { Item(Item.Properties()) }
//    var SLIDER_CREATOR: RegistrySupplier<Item> = ITEMS.register("slider_creator") { Item(Item.Properties()) }
//    var WINCH_CREATOR: RegistrySupplier<Item> = ITEMS.register("winch_creator") { Item(Item.Properties()) }

    var TOOLGUN = ITEMS.register("toolgun") { ToolgunItem() }

    fun register() {
        VMBlocks.registerItems(ITEMS)
        ITEMS.register()
    }
}