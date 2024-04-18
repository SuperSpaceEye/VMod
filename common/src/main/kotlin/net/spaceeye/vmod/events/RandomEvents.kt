package net.spaceeye.vmod.events

import dev.architectury.event.events.common.TickEvent
import net.minecraft.server.MinecraftServer
import org.valkyrienskies.core.impl.util.events.EventEmitter
import org.valkyrienskies.core.impl.util.events.EventEmitterImpl

object RandomEvents {
    init {
        TickEvent.SERVER_PRE.register { serverOnTick.emit(ServerOnTick(it)) }
    }

    val serverOnTick = EventEmitterImpl<ServerOnTick>()

    data class ServerOnTick(val server: MinecraftServer) {
        companion object : EventEmitter<ServerOnTick> by serverOnTick
    }

}