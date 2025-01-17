package net.spaceeye.vmod.constraintsManaging

import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.mixin.ShipObjectWorldAccessor
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.mod.common.shipObjectWorld

//TODO maybe i don't need this anymore

object VSJointsTracker {
    val shipToIds: Map<Long, Set<Int>> get() = (ServerLevelHolder.shipObjectWorld!! as ShipObjectWorldAccessor).shipIdToConstraints

    fun getVSJoints(ids: List<VSJointId>): List<Pair<VSJointId, VSJoint?>> {
        val acc = ServerLevelHolder.server!!.shipObjectWorld as ShipObjectWorldAccessor
        return ids.map { Pair(it, acc.constraints[it]) }
    }

    fun getIdsOfShip(shipId: ShipId): Set<Int> {
        return shipToIds.getOrDefault(shipId, setOf())
    }
}