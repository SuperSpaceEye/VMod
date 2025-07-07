package net.spaceeye.vmod.fabric

import com.mojang.brigadier.CommandDispatcher
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.spaceeye.vmod.VM.init
import net.spaceeye.vmod.VMClientCommands
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric

class VModFabric : ModInitializer {
    override fun onInitialize() {
        ValkyrienSkiesModFabric().onInitialize()
        init()
        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            ClientCommandRegistrationCallback.EVENT.register { it, a -> VMClientCommands.registerClientCommands(it as CommandDispatcher<CommandSourceStack>) }
        } }
    }
}

class VModFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
    }
}