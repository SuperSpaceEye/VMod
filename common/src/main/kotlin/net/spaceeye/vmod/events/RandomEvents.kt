package net.spaceeye.vmod.events

import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.utils.EnvExecutor
import net.fabricmc.api.EnvType
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
import org.valkyrienskies.core.impl.util.events.EventEmitter
import org.valkyrienskies.core.impl.util.events.EventEmitterImpl

object RandomEvents {
    init {
        TickEvent.SERVER_PRE.register { serverOnTick.emit(ServerOnTick(it)) }

        EnvExecutor.runInEnv(EnvType.CLIENT) { Runnable {
            ClientTickEvent.CLIENT_PRE.register {
                clientOnTick.emit(ClientOnTick(it))
            }
        } }
    }

    val serverOnTick = EventEmitterImpl<ServerOnTick>()

    data class ServerOnTick(val server: MinecraftServer) {
        companion object : EventEmitter<ServerOnTick> by serverOnTick
    }

    val clientOnTick = EventEmitterImpl<ClientOnTick>()

    data class ClientOnTick(val minecraft: Minecraft) {
        companion object : EventEmitter<ClientOnTick> by clientOnTick
    }

}