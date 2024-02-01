package net.spaceeye.vssource.forge

import dev.architectury.platform.forge.EventBuses
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.spaceeye.vssource.VSS
import net.spaceeye.vssource.VSS.init

@Mod(VSS.MOD_ID)
class VSSourceForge {
    init {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(VSS.MOD_ID, FMLJavaModLoadingContext.get().modEventBus)
        init()
    }
}