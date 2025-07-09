package net.spaceeye.vmod.vEntityManaging.util

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.vs.gtpa
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Make constraint or if failed, delete all and run callback
 */
fun mc(constraint: VSJoint, cIDs: ConcurrentLinkedQueue<VSJointId>, level: ServerLevel): CompletableFuture<Boolean> {
    val returnPromise = CompletableFuture<Boolean>()
    level.gtpa.addJoint(constraint).thenAccept { id ->
        returnPromise.complete(id != null)
        if (id == null) cIDs.forEach { level.gtpa.removeJoint(it) } else cIDs.add(id)
    }
    return returnPromise
}