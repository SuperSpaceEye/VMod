package net.spaceeye.vsource.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
//import net.spaceeye.vsource.VS.clientInit
import net.spaceeye.vsource.VS.init

class VSourceFabric : ModInitializer {
    override fun onInitialize() {
        init()
    }
}

class VSourceFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
//        clientInit()
    }
}