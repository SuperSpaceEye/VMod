package net.spaceeye.vmod.constraintsManaging.util

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.vs.VSJointDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSJointSerializationUtil
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.reflect.KMutableProperty0


/**
 * Serialize VS constraint or if failed, run callback
 */
inline fun sc(name: String, constraint: VSJoint, tag: CompoundTag, ret: () -> Unit) {
    tag.put(name, VSJointSerializationUtil.serializeConstraint(constraint) ?: return ret())
}

/**
 * Deserialize VS constraint or if failed, run callback
 */
inline fun <T: VSJoint> dc(name: String, set: KMutableProperty0<T>, tag: CompoundTag, lastDimensionIds: Map<ShipId, String>, ret: () -> Unit) {
    VSJointDeserializationUtil.tryConvertDimensionId(tag[name] as CompoundTag, lastDimensionIds);
    set.set((VSJointDeserializationUtil.deserializeConstraint(tag[name] as CompoundTag) ?: return ret()) as T)
}

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