package net.spaceeye.vmod.forge

import dev.architectury.platform.forge.EventBuses
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.VM.init

@Mod(VM.MOD_ID)
class VModForge {
    init {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(VM.MOD_ID, FMLJavaModLoadingContext.get().modEventBus)
        init()
    }
}