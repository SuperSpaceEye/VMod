package net.spaceeye.vmod.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.spaceeye.vmod.VM.init

class VModFabric : ModInitializer {
    override fun onInitialize() {
        init()
    }
}

class VModFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
    }
}