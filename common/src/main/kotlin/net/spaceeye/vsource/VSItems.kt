package net.spaceeye.vsource

import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.vsource.items.AABBWeldCreatorItem
import net.spaceeye.vsource.items.RopeCreatorItem
import net.spaceeye.vsource.items.TestTool

object VSItems {
    val ITEMS = DeferredRegister.create(VS.MOD_ID, Registry.ITEM_REGISTRY)

    val TAB: CreativeModeTab = CreativeTabRegistry.create(
        ResourceLocation(
            VS.MOD_ID,
            "vsource_tab"
        )
    ) { ItemStack(LOGO.get()) }

    var LOGO: RegistrySupplier<Item> = ITEMS.register("vsource_logo") { Item(Item.Properties()) }

//    var AXIS_CREATOR: RegistrySupplier<Item> = ITEMS.register("axis_creator") { Item(Item.Properties()) }
//    var BALL_SOCKET_CREATOR: RegistrySupplier<Item> = ITEMS.register("ball_socket_creator") {  }
//    var ELASTIC_CREATOR: RegistrySupplier<Item> = ITEMS.register("elastic_creator") { Item(Item.Properties()) }
//    var HYDRAULICS_CREATOR: RegistrySupplier<Item> = ITEMS.register("hydraulics_creator") { Item(Item.Properties()) }
//    var MOTOR_CREATOR: RegistrySupplier<Item> = ITEMS.register("motor_creator") { Item(Item.Properties()) }
//    var MUSCLE_CREATOR: RegistrySupplier<Item> = ITEMS.register("muscle_creator") { Item(Item.Properties()) }
//    var PULLEY_CREATOR: RegistrySupplier<Item> = ITEMS.register("pulley_creator") { Item(Item.Properties()) }
    var ROPE_CREATOR: RegistrySupplier<Item> = ITEMS.register("rope_creator") { RopeCreatorItem() }
//    var SLIDER_CREATOR: RegistrySupplier<Item> = ITEMS.register("slider_creator") { Item(Item.Properties()) }
//    var WELD_CREATOR: RegistrySupplier<Item> = ITEMS.register("weld_creator") { WeldCreatorItem() }
//    var WINCH_CREATOR: RegistrySupplier<Item> = ITEMS.register("winch_creator") { Item(Item.Properties()) }

    var AABBWELD_CREATOR: RegistrySupplier<Item> = ITEMS.register("aabb_weld_creator") { AABBWeldCreatorItem() }

    var TEST_ITEM = ITEMS.register("test_item") { TestTool() }

    fun register() {
        ITEMS.register()
    }
}