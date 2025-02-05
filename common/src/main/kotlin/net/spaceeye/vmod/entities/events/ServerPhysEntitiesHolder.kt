package net.spaceeye.vmod.entities.events

import net.minecraft.world.entity.Entity
import net.spaceeye.vmod.utils.SafeEventEmitter
import net.spaceeye.vmod.utils.ServerClosable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ServerPhysEntitiesHolder: ServerClosable() {
    val entities = ConcurrentHashMap<UUID, Entity>()

    val entityLoadedEvent = SafeEventEmitter<EntityLoadedEvent>()
    val serverRemovedEntity = SafeEventEmitter<ServerRemovedEntity>()

    data class EntityLoadedEvent(val uuid: UUID, val entity: Entity)
    data class ServerRemovedEntity(val uuid: UUID, val entity: Entity)

    // Entities load after VEntity manager so that's good
    fun entityLoaded(uuid: UUID, entity: Entity) {
        entities[uuid] = entity
        entityLoadedEvent.emit(EntityLoadedEvent(uuid, entity))
    }

    override fun close() {
        entities.clear()
    }
}