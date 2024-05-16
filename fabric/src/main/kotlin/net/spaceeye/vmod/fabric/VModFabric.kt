package net.spaceeye.vmod.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.spaceeye.vmod.VM.init
import net.spaceeye.vmod.VMCommands

class VModFabric : ModInitializer {
    override fun onInitialize() {
        init()
        CommandRegistrationCallback.EVENT.register { dispatcher, _ -> VMCommands.registerServerCommands(dispatcher)}
    }
}

class VModFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
    }
}