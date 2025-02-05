package net.spaceeye.vmod.vEntityManaging.util

import net.spaceeye.vmod.mixin.ShipObjectWorldAccessor
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.mod.common.shipObjectWorld

object VSJointsAccessor {
    @JvmStatic
    fun getVSJoints(ids: List<VSJointId>): List<Pair<VSJointId, VSJoint?>> {
        val acc = ServerLevelHolder.server!!.shipObjectWorld as ShipObjectWorldAccessor
        return ids.map { Pair(it, acc.constraints[it]) }
    }

    @JvmStatic
    fun getIdsOfShip(shipId: ShipId): Set<Int> {
        return (ServerLevelHolder.shipObjectWorld!! as ShipObjectWorldAccessor).shipIdToConstraints.getOrDefault(shipId, setOf())
    }
}