package net.spaceeye.vmod.constraintsManaging.util

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraint
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId
import kotlin.reflect.KMutableProperty0


/**
 * Serialize VS constraint or if failed, run callback
 */
inline fun sc(name: String, constraint: VSConstraint, tag: CompoundTag, ret: () -> Unit) {
    tag.put(name, VSConstraintSerializationUtil.serializeConstraint(constraint) ?: return ret())
}

/**
 * Deserialize VS constraint or if failed, run callback
 */
inline fun <T: VSConstraint> dc(name: String, set: KMutableProperty0<T>, tag: CompoundTag, lastDimensionIds: Map<ShipId, String>, ret: () -> Unit) {
    VSConstraintDeserializationUtil.tryConvertDimensionId(tag[name] as CompoundTag, lastDimensionIds);
    set.set((VSConstraintDeserializationUtil.deserializeConstraint(tag[name] as CompoundTag) ?: return ret()) as T)
}

/**
 * Make constraint or if failed, delete all and run callback
 */
inline fun mc(constraint: VSConstraint, cIDs: MutableList<ConstraintId>, level: ServerLevel, ret: () -> Unit) {
    val id = level.shipObjectWorld.createNewConstraint(constraint)
    if (id == null) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        return ret()
    }
    cIDs.add(id)
}