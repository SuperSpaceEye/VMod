package net.spaceeye.vmod

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import dev.architectury.event.events.client.ClientCommandRegistrationEvent.ClientCommandSourceStack
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.events.SessionEvents
import net.spaceeye.vmod.toolgun.gui.ClientSettingsGUI
import net.spaceeye.vmod.toolgun.gui.MainToolgunGUIWindow
import net.spaceeye.vmod.toolgun.gui.ServerSettingsGUI

object VMClientCommands {
    private fun lt(name: String) = LiteralArgumentBuilder.literal<ClientCommandSourceStack>(name)
    private fun <T> arg(name: String, type: ArgumentType<T>) = RequiredArgumentBuilder.argument<ClientCommandSourceStack, T>(name, type)

    fun registerClientCommands(dispatcher: CommandDispatcher<ClientCommandSourceStack>) {
        dispatcher.register(
            lt("vmod-client")
            .then(
                lt("open-client-settings").executes {
                    SessionEvents.clientOnTick.on { (_), unreg ->
                        unreg()
                        val gui = MainToolgunGUIWindow(false)
                        gui.currentWindow = ClientSettingsGUI(gui.mainWindow)
                        Minecraft.getInstance().setScreen(gui)
                    }
                    0
                }
            ).then(
                lt("open-server-settings").executes {
                    SessionEvents.clientOnTick.on { (_), unreg ->
                        unreg()
                        val gui = MainToolgunGUIWindow(false)
                        gui.currentWindow = ServerSettingsGUI(gui.mainWindow)
                        Minecraft.getInstance().setScreen(gui)
                    }
                    0
                }
            )
        )
    }
}