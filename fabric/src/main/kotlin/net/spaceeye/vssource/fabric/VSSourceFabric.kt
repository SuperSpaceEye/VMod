package net.spaceeye.vssource.fabric

import net.fabricmc.api.ModInitializer
import net.spaceeye.vssource.LOG
import net.spaceeye.vssource.VSS.init

class VSSourceFabric : ModInitializer {
    override fun onInitialize() {
        init()
    }
}