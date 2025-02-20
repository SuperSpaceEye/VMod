package net.spaceeye.vmod.vEntityManaging.util

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.mod.common.shipObjectWorld

/**
 * Make constraint or if failed, delete all and run callback
 */
inline fun mc(constraint: VSJoint, cIDs: MutableList<VSJointId>, level: ServerLevel, ret: () -> Unit) {
    val id = level.shipObjectWorld.createNewConstraint(constraint)
    if (id == null) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        return ret()
    }
    cIDs.add(id)
}