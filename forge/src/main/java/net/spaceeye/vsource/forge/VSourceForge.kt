package net.spaceeye.vsource.forge

import dev.architectury.platform.forge.EventBuses
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.spaceeye.vsource.VS
import net.spaceeye.vsource.VS.init

@Mod(VS.MOD_ID)
class VSourceForge {
    init {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(VS.MOD_ID, FMLJavaModLoadingContext.get().modEventBus)
        init()
    }
}