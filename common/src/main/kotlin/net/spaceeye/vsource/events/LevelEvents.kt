package net.spaceeye.vsource.events

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.impl.util.events.EventEmitter
import org.valkyrienskies.core.impl.util.events.EventEmitterImpl

object LevelEvents {
    val clientLevelInitEvent = EventEmitterImpl<ClientLevelInitEvent>()

    data class ClientLevelInitEvent(val level: ClientLevel) {
        companion object : EventEmitter<ClientLevelInitEvent> by clientLevelInitEvent
    }

    val clientDisconnectEvent = EventEmitterImpl<ClientDisconnectEvent>()

    data class ClientDisconnectEvent(val level: ClientLevel) {
        companion object : EventEmitter<ClientDisconnectEvent> by clientDisconnectEvent
    }

    val serverLevelCloseEvent = EventEmitterImpl<ServerLevelCloseEvent>()

    data class ServerLevelCloseEvent(val level: ServerLevel) {
        companion object : EventEmitter<ServerLevelCloseEvent> by serverLevelCloseEvent
    }

    val serverStopEvent = EventEmitterImpl<ServerStopEvent>()

    data class ServerStopEvent(val server: MinecraftServer) {
        companion object : EventEmitter<ServerStopEvent> by serverStopEvent
    }
}