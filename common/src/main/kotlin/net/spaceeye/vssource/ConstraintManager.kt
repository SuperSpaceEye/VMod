package net.spaceeye.vssource

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.vssource.utils.putQuaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.core.apigame.constraints.VSConstraintType.*
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.util.putVector3d

inline fun mMakeConstraint(level: ServerLevel, constraint: VSConstraint) {
    ConstraintManager.getInstance(level).makeConstraint(level, constraint)
}

//ShipId seem to be unique and are retained by ships after saving/loading

//TODO put constants into constants class
class ConstraintManager: SavedData() {
    val shipConstraints = mutableMapOf<ShipId, MutableList<VSConstraint>>()
    val toLoadConstraints = mutableMapOf<ShipId, MutableList<VSConstraint>>()

    private fun saveBaseConstraint(constraint: VSConstraint): CompoundTag {
        val ctag = CompoundTag()
        ctag.putInt("shipId0", constraint.shipId0.toInt())
        ctag.putInt("shipId1", constraint.shipId1.toInt())
        ctag.putDouble("compliance", constraint.compliance)
        ctag.putString("constraintType", constraint.constraintType.toString())
        return ctag
    }

    private fun saveForceConstraint(constraint: VSForceConstraint, ctag: CompoundTag): CompoundTag {
        ctag.putVector3d("localPos0", constraint.localPos0)
        ctag.putVector3d("localPos1", constraint.localPos1)
        ctag.putDouble("maxForce", constraint.maxForce)
        return ctag
    }

    private fun saveTorqueConstraint(constraint: VSTorqueConstraint, ctag: CompoundTag): CompoundTag {
        ctag.putQuaterniond("localRot0", constraint.localRot0)
        ctag.putQuaterniond("localRot1", constraint.localRot1)
        ctag.putDouble("maxTorque", constraint.maxTorque)
        return ctag
    }

    //TODO refactor this
    override fun save(tag: CompoundTag): CompoundTag {
        val shipsTags = CompoundTag()
        for ((k, v) in shipConstraints) {
            val constraintsTag = CompoundTag()
            for ((i, constraint) in v.withIndex()) {
                var ctag = saveBaseConstraint(constraint)

                when (constraint) {
                    is VSForceConstraint -> {
                        ctag = saveForceConstraint(constraint, ctag)
                        when (constraint.constraintType) {
                            ATTACHMENT -> {
                                ctag.putDouble("fixedDistance", (constraint as VSAttachmentConstraint).fixedDistance)
                            }
                            POS_DAMPING -> {
                                ctag.putDouble("posDamping", (constraint as VSPosDampingConstraint).posDamping)
                            }
                            ROPE -> {
                                ctag.putDouble("ropeLength", (constraint as VSRopeConstraint).ropeLength)
                            }
                            SLIDE -> {
                                constraint as VSSlideConstraint
                                ctag.putVector3d("localSlideAxis0", constraint.localSlideAxis0)
                                ctag.putDouble("maxDistBetweenPoints", constraint.maxDistBetweenPoints)
                            }
                            else -> { LOG("CAN'T SAVE TYPE ${constraint.constraintType} IN VSForceConstraint BLOCK"); continue}
                        }
                    }
                    is VSTorqueConstraint -> {
                        ctag = saveTorqueConstraint(constraint, ctag)

                        when (constraint.constraintType) {
                            HINGE_SWING_LIMITS -> {
                                constraint as VSHingeSwingLimitsConstraint
                                ctag.putDouble("minSwingAngle", constraint.minSwingAngle)
                                ctag.putDouble("maxSwingAngle", constraint.maxSwingAngle)
                            }
                            HINGE_TARGET_ANGLE -> {
                                constraint as VSHingeTargetAngleConstraint
                                ctag.putDouble("targetAngle", constraint.targetAngle)
                                ctag.putDouble("nextTickTargetAngle", constraint.nextTickTargetAngle)
                            }
                            ROT_DAMPING -> {
                                constraint as VSRotDampingConstraint
                                ctag.putDouble("rotDamping", constraint.rotDamping)
                                ctag.putString("rotDampingAxes", constraint.rotDampingAxes.toString())
                            }
                            SPHERICAL_SWING_LIMITS -> {
                                constraint as VSSphericalSwingLimitsConstraint
                                ctag.putDouble("minSwingAngle", constraint.minSwingAngle)
                                ctag.putDouble("maxSwingAngle", constraint.maxSwingAngle)
                            }
                            SPHERICAL_TWIST_LIMITS -> {
                                constraint as VSSphericalTwistLimitsConstraint
                                ctag.putDouble("minTwistAngle", constraint.minTwistAngle)
                                ctag.putDouble("maxTwistAngle", constraint.maxTwistAngle)
                            }
                            FIXED_ORIENTATION -> {}
                            HINGE_ORIENTATION -> {}
                            else -> { LOG("CAN'T SAVE TYPE ${constraint.constraintType} IN VSForceConstraint BLOCK"); continue}
                        }
                    }
                    else -> { LOG("CAN'T SAVE UNKNOWN VS CONSTRAINT TYPE ${constraint.constraintType}. SHOULDN'T HAPPEN!!!!!!"); continue }
                }
                constraintsTag.put(i.toString(), ctag)
            }
            shipsTags.put(k.toString(), constraintsTag)
        }
        tag.put("vs_source_ships_constraints", shipsTags)
        return tag
    }

    fun loadDataFromTag(tag: CompoundTag) {

    }

    fun createConstraints() {

    }

    fun load(tag: CompoundTag) {
        loadDataFromTag(tag)
        createConstraints()
    }

    fun makeConstraint(level: ServerLevel, constraint: VSConstraint) {
        level.shipObjectWorld.createNewConstraint(constraint)
        shipConstraints.computeIfAbsent(constraint.shipId0) { mutableListOf() }.add(constraint)
        shipConstraints.computeIfAbsent(constraint.shipId1) { mutableListOf() }.add(constraint)
    }

    companion object {
        private var instance: ConstraintManager? = null

        fun getInstance(level: Level): ConstraintManager {
            if (instance != null) {return instance!!}
            level as ServerLevel
            instance = level.server.overworld().dataStorage.computeIfAbsent(::load, ::create, VSS.MOD_ID)
            return instance!!
        }

        fun create(): ConstraintManager {return ConstraintManager() }
        fun load(tag: CompoundTag): ConstraintManager {
            val data = create()

            if (tag.contains("vs_source_ships_constraints") && tag["vs_source_ships_constraints"] is CompoundTag) {
                data.load(tag["vs_source_ships_constraints"]!! as CompoundTag)
            }

            return data
        }
    }
}