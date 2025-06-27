package net.spaceeye.vmod.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.spaceeye.vmod.VM.init
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric

class VModFabric : ModInitializer {
    override fun onInitialize() {
        ValkyrienSkiesModFabric().onInitialize()
        init()
    }
}

class VModFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
    }
}