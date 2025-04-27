package net.spaceeye.vmod.events

import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.utils.EnvExecutor
import net.fabricmc.api.EnvType
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
import net.spaceeye.vmod.utils.SafeEventEmitter

object SessionEvents {
    init {
        TickEvent.SERVER_PRE.register { serverOnTick.emit(ServerOnTick(it)) }
        TickEvent.SERVER_POST.register { serverAfterTick.emit(ServerOnTick(it)) }

        EnvExecutor.runInEnv(EnvType.CLIENT) { Runnable {
            ClientTickEvent.CLIENT_PRE.register {
                clientOnTick.emit(ClientOnTick(it))
            }
        } }

        LifecycleEvent.SERVER_STARTED.register {
            serverOnTick.clear()
            serverAfterTick.clear()
        }
        EnvExecutor.runInEnv(EnvType.CLIENT) { Runnable {
            ClientLifecycleEvent.CLIENT_STARTED.register {
                clientOnTick.clear()
            }
        } }
    }

    val serverOnTick = SafeEventEmitter<ServerOnTick>()
    val clientOnTick = SafeEventEmitter<ClientOnTick>()

    val serverAfterTick = SafeEventEmitter<ServerOnTick>()

    data class ServerOnTick(val server: MinecraftServer)
    data class ClientOnTick(val minecraft: Minecraft)

}