package net.spaceeye.vmod.entities.events

import net.minecraft.world.entity.Entity
import net.spaceeye.vmod.utils.SafeEventEmitter

object ClientPhysEntitiesHolder {
    val clientEntityLoadedEvent = SafeEventEmitter<ClientEntityLoadedEvent>()
    val clientRemovedEntity = SafeEventEmitter<ClientRemovedEntity>()

    data class ClientEntityLoadedEvent(val id: Int, val entity: Entity)
    data class ClientRemovedEntity(val id: Int, val entity: Entity)

    fun entityLoaded(id: Int, entity: Entity) {
        clientEntityLoadedEvent.emit(ClientEntityLoadedEvent(id, entity))
    }
}