package net.spaceeye.vmod.constraintsManaging.types

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.*
import net.spaceeye.vmod.networking.TagSerializableItem
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.copyAttachmentPoints
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import net.spaceeye.vmod.networking.TagSerializableItem.get
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.apigame.joints.*
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore
import java.util.*

class HydraulicsMConstraint(): TwoShipsMConstraint(), MCAutoSerializable, Tickable {
    enum class ConnectionMode {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }

    override lateinit var sPos1: Vector3d
    override lateinit var sPos2: Vector3d
    override var shipId1: Long = -1
    override var shipId2: Long = -1

    @JsonIgnore private var i = 0

    var minLength: Float by get(i++, -1f)
    var maxLength: Float by get(i++, -1f)

    var extensionSpeed: Float by get(i++, 1f)
    var extendedDist: Float by get(i++, 0f)

    var channel: String by get(i++, "")

    var connectionMode: ConnectionMode by get(i++, ConnectionMode.FIXED_ORIENTATION)

    var sRot1: Quaterniond by get(i++, Quaterniond())
    var sRot2: Quaterniond by get(i++, Quaterniond())

    //shipyard direction and scale information
    var sDir1: Vector3d by get(i++, Vector3d())
    var sDir2: Vector3d by get(i++, Vector3d())

    var maxForce: Float by get(i++, -1f)
    var stiffness: Float by get(i++, 0f)
    var damping: Float by get(i++, 0f)


    private lateinit var distanceConstraint: VSJoint
    private lateinit var rotationConstraint: VSD6Joint
    private var dID = -1
    private var rID = -1

    constructor(
        sPos1: Vector3d,
        sPos2: Vector3d,

        sDir1: Vector3d,
        sDir2: Vector3d,

        sRot1: Quaterniondc,
        sRot2: Quaterniondc,

        shipId1: ShipId,
        shipId2: ShipId,

        maxForce: Float,
        stiffness: Float,
        damping: Float,

        minLength: Float,
        maxLength: Float,
        extensionSpeed: Float,

        channel: String,

        connectionMode: ConnectionMode,

        attachmentPoints: List<BlockPos>,
    ): this() {
        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()

        this.sDir1 = sDir1.copy()
        this.sDir2 = sDir2.copy()

        this.sRot1 = Quaterniond(sRot1)
        this.sRot2 = Quaterniond(sRot2)

        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.minLength = minLength
        this.maxLength = maxLength
        // extensionSpeed is in seconds. Constraint is being updated every mc tick
        this.extensionSpeed = extensionSpeed / 20f

        this.channel = channel
        this.connectionMode = connectionMode

        this.maxForce = maxForce
        this.stiffness = stiffness
        this.damping = damping

        attachmentPoints_ = attachmentPoints.toMutableList()
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return HydraulicsMConstraint(
            tryMovePosition(sPos1, shipId1, level, mapped) ?: return null,
            tryMovePosition(sPos2, shipId2, level, mapped) ?: return null,
            sDir1, sDir2, sRot1, sRot2,
            mapped[shipId1] ?: return null,
            mapped[shipId2] ?: return null,
            maxForce, stiffness, damping,
            minLength, maxLength, extensionSpeed * 20f, channel, connectionMode,
            copyAttachmentPoints(sPos1, sPos2, shipId1, shipId2, attachmentPoints_, level, mapped),
        )
    }

    private fun updateDistanceConstraint(shipObjectWorld: ServerShipWorldCore) {
        distanceConstraint = when (distanceConstraint) {
            is VSDistanceJoint -> {
                (distanceConstraint as VSDistanceJoint).copy(
                    minDistance = minLength + extendedDist,
                    maxDistance = minLength + extendedDist,
                )
            }
            is VSD6Joint -> {
                (distanceConstraint as VSD6Joint).copy(
                    linearLimits = EnumMap(mapOf(
                        Pair(VSD6Joint.D6Axis.X, VSD6Joint.LinearLimitPair(this.minLength + extendedDist, this.minLength + extendedDist, stiffness = stiffness, damping = damping)))
                    ),
                )
            }
            else -> throw AssertionError("should be impossible")
        }
        shipObjectWorld.updateConstraint(dID, distanceConstraint)
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        val scaleBy = scaleBy.toFloat()
        minLength *= scaleBy
        maxLength *= scaleBy
        extendedDist *= scaleBy
        extensionSpeed *= scaleBy

        sDir1.divAssign(scaleBy)
        sDir2.divAssign(scaleBy)

        updateDistanceConstraint(level.shipObjectWorld)
    }

    var wasDeleted = false
    var lastExtended: Float = 0f
    var targetPercentage = 0f

    private fun tryExtendDist(): Boolean {
        val length = maxLength - minLength

        val currentPercentage = extendedDist / length
        if (abs(currentPercentage - targetPercentage) < 1e-6) { return false }
        val left = targetPercentage - currentPercentage
        val percentageStep = extensionSpeed / length
        extendedDist += min(percentageStep, abs(left)) * length * left.sign
        return true
    }

    override fun tick(server: MinecraftServer, unregister: () -> Unit) {
        if (wasDeleted) {
            unregister()
            return
        }
        getExtensionsOfType<TickableMConstraintExtension>().forEach { it.tick(server) }
        if (!tryExtendDist()) {return}

        if (lastExtended == extendedDist) {return}
        lastExtended = extendedDist
        updateDistanceConstraint(server.shipObjectWorld)
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        val maxForceTorque = if (maxForce <= 0) {null} else {VSJointMaxForceTorque(maxForce, maxForce)}

        distanceConstraint = if (connectionMode == ConnectionMode.FREE_ORIENTATION) {
            VSDistanceJoint(
                shipId1, VSJointPose(sPos1.toJomlVector3d(), Quaterniond()),
                shipId2, VSJointPose(sPos2.toJomlVector3d(), Quaterniond()),
                maxForceTorque, this.minLength, this.minLength, stiffness = stiffness, damping = damping
            )
        } else {
            val rot1 = getHingeRotation(-sDir1.normalize())
            val rot2 = getHingeRotation(-sDir2.normalize())
            VSD6Joint(
                shipId1, VSJointPose(sPos1.toJomlVector3d(), rot1),
                shipId2, VSJointPose(sPos2.toJomlVector3d(), rot2),
                motions = EnumMap(mapOf(
                    Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.LIMITED),

                    Pair(VSD6Joint.D6Axis.TWIST, VSD6Joint.D6Motion.FREE),
                    Pair(VSD6Joint.D6Axis.SWING1, VSD6Joint.D6Motion.FREE),
                    Pair(VSD6Joint.D6Axis.SWING2, VSD6Joint.D6Motion.FREE),
                )),
                linearLimits = EnumMap(mapOf(
                    Pair(VSD6Joint.D6Axis.X, VSD6Joint.LinearLimitPair(this.minLength, this.minLength, stiffness = stiffness, damping = damping)))
                ),
                maxForceTorque = maxForceTorque
            )
        }
        mc(distanceConstraint, cIDs, level) {return false}
        dID = cIDs.last()

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) { return true }

        rotationConstraint = when(connectionMode) {
            ConnectionMode.FIXED_ORIENTATION -> {
                val rot1 = sRot1.invert(Quaterniond())
                val rot2 = sRot2.invert(Quaterniond())
                VSD6Joint(
                    shipId1, VSJointPose(sPos1.toJomlVector3d(), rot1),
                    shipId2, VSJointPose(sPos2.toJomlVector3d(), rot2),
                    motions = EnumMap(mapOf(
                        Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Y, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Z, VSD6Joint.D6Motion.FREE),
                    )),
                    maxForceTorque = maxForceTorque
                )
            }
            ConnectionMode.HINGE_ORIENTATION -> {
                val rot1 = getHingeRotation(sDir1)
                val rot2 = getHingeRotation(sDir2)
                VSD6Joint(
                    shipId1, VSJointPose(sPos1.toJomlVector3d(), rot1),
                    shipId2, VSJointPose(sPos2.toJomlVector3d(), rot2),
                    motions = EnumMap(mapOf(
                        Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Y, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.Z, VSD6Joint.D6Motion.FREE),
                        Pair(VSD6Joint.D6Axis.TWIST, VSD6Joint.D6Motion.FREE)
                    )),
                    maxForceTorque = maxForceTorque
                )
            }
            ConnectionMode.FREE_ORIENTATION -> throw AssertionError("how")
        }

        mc(rotationConstraint, cIDs, level) {return false}
        rID = cIDs.last()

        return true
    }

    override fun iOnDeleteMConstraint(level: ServerLevel) {
        super.iOnDeleteMConstraint(level)
        wasDeleted = true
    }

    companion object {
        init {
            TagSerializableItem.registerSerializationEnum(ConnectionMode::class)
        }
    }
}