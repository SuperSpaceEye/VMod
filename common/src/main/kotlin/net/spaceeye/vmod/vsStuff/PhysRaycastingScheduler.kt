package net.spaceeye.vmod.vsStuff

import dev.architectury.event.events.common.TickEvent
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.Tuple
import net.spaceeye.vmod.utils.Tuple4
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.physics.RayCastResult
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.api.vsApi
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@OptIn(VsBeta::class)
object PhysRaycastingScheduler: ServerClosable() {
    val jobs: MutableMap<DimensionId,
            Pair<ReentrantLock, MutableMap<UUID, Tuple4<JVector3d, JVector3d, Double, RayCastResult?>>>
            > = mutableMapOf()

    val writeLock = ReentrantLock()
    override fun close() {
        writeLock.withLock { jobs.clear() }
    }

    init {
        TickEvent.SERVER_POST.register { server ->
            val distance = VMConfig.SERVER.TOOLGUN.MAX_RAYCAST_DISTANCE

            server.playerList.players.forEach { player ->
                val (lock, dimensionMap) = jobs.getOrPut(player.serverLevel().dimensionId) { writeLock.withLock { Pair(ReentrantLock(), mutableMapOf()) } }

                val item = player.mainHandItem.item
                if (ServerToolGunState.itemShouldRaycast(item)) {
                    val pos = Vector3d(player.eyePosition).toJomlVector3d()
                    val dir = Vector3d(player.lookAngle).toJomlVector3d()

                    if (!dimensionMap.contains(player.uuid)) {
                        lock.withLock { dimensionMap[player.uuid] = Tuple.of(pos, dir, distance, null) }
                    } else {
                        val item = dimensionMap[player.uuid]!!
                        item.i1 = pos
                        item.i2 = dir
                        item.i3 = distance
                    }
                } else {
                    if (dimensionMap.contains(player.uuid)) {
                        lock.withLock { dimensionMap.remove(player.uuid) }
                    }
                }
            }
        }

        vsApi.physTickEvent.on { val level = it.world;
            val (lock, jobs) = jobs[level.dimension] ?: return@on
            val keys = lock.withLock { jobs.keys.toList() }
            keys.forEach { key ->
                val item = lock.withLock { jobs[key] } ?: return@forEach
                item.i4 = level.rayCast(item.i1, item.i2, item.i3)
            }
        }
    }
}