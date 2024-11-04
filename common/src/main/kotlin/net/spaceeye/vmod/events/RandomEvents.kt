package net.spaceeye.vmod.events

import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.utils.EnvExecutor
import net.fabricmc.api.EnvType
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
import net.spaceeye.vmod.utils.CancellableEventEmitter
import net.spaceeye.vmod.utils.SafeEventEmitter

object RandomEvents {
    init {
        TickEvent.SERVER_PRE.register { serverOnTick.emit(ServerOnTick(it)) }
        TickEvent.SERVER_POST.register { serverAfterTick.emit(ServerOnTick(it)) }

        EnvExecutor.runInEnv(EnvType.CLIENT) { Runnable {
            ClientTickEvent.CLIENT_PRE.register {
                clientOnTick.emit(ClientOnTick(it))
            }
        } }
    }

    val serverOnTick = SafeEventEmitter<ServerOnTick>()
    val clientOnTick = SafeEventEmitter<ClientOnTick>()

    val serverAfterTick = SafeEventEmitter<ServerOnTick>()

    val mouseMove = CancellableEventEmitter<OnMouseMove>()
    //why? arch event is cringe and doesn't actually cancel mc keybinds
    val keyPress = CancellableEventEmitter<OnKeyPress>()

    val clientPreRender = SafeEventEmitter<ClientPreRender>()

    data class ServerOnTick(val server: MinecraftServer)
    data class ClientOnTick(val minecraft: Minecraft)
    data class OnMouseMove(val x: Double, val y: Double)
    data class OnKeyPress(val key: Int, val scanCode: Int, val action: Int, val modifiers: Int)
    data class ClientPreRender(val timestamp: Long)
}