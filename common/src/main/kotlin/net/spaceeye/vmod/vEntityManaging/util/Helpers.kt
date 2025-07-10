package net.spaceeye.vmod.vEntityManaging.util

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.vs.gtpa
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Make constraint or if failed, delete all and run callback
 */
fun mc(constraint: VSJoint, cIDs: ConcurrentLinkedQueue<VSJointId>, level: ServerLevel, checkValid: ((VSJoint, PhysLevel) -> Boolean)? = null): CompletableFuture<Boolean> {
    val returnPromise = CompletableFuture<Boolean>()
    level.gtpa.addJoint(constraint, checkValid).thenAccept { id ->
        returnPromise.complete(id != -1)
        if (id == -1) cIDs.forEach { level.gtpa.removeJoint(it) } else cIDs.add(id)
    }
    return returnPromise
}