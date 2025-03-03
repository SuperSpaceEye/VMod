package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.util.*
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSD6Joint
import org.valkyrienskies.core.apigame.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.apigame.joints.VSJointPose
import java.util.*

class SyncRotationConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
    override var sPos1: Vector3d get() = Vector3d(); set(_) {}
    override var sPos2: Vector3d get() = Vector3d(); set(_) {}
    override var shipId1: Long = -1
    override var shipId2: Long = -1

    var sRot1: Quaterniond by get(i++, Quaterniond())
    var sRot2: Quaterniond by get(i++, Quaterniond())

    var maxForce: Float by get(i++, -1f)

    constructor(
        sRot1: Quaterniondc,
        sRot2: Quaterniondc,
        shipId1: ShipId,
        shipId2: ShipId,
        maxForce: Float
    ): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2
        this.sRot1 = Quaterniond(sRot1)
        this.sRot2 = Quaterniond(sRot2)
        this.maxForce = maxForce
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>): VEntity? {
        return SyncRotationConstraint(Quaterniond(sRot1), Quaterniond(sRot2), shipId1, shipId2, maxForce)
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}
    override fun iGetAttachmentPoints(shipId: ShipId): List<Vector3d> { return emptyList() }
    override fun iGetAttachmentPositions(shipId: ShipId): List<BlockPos> { return emptyList() }

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        val maxForceTorque = if (maxForce < 0) {null} else {VSJointMaxForceTorque(maxForce, maxForce)}
//        val stiffness = if (stiffness < 0) {null} else {stiffness}
//        val damping = if (damping < 0) {null} else {damping}
        val mainConstraint = VSD6Joint(
            shipId1, VSJointPose(org.joml.Vector3d(), sRot1),
            shipId2, VSJointPose(org.joml.Vector3d(), sRot2),
            maxForceTorque,
            EnumMap(mapOf(
                Pair(VSD6Joint.D6Axis.X, VSD6Joint.D6Motion.FREE),
                Pair(VSD6Joint.D6Axis.Y, VSD6Joint.D6Motion.FREE),
                Pair(VSD6Joint.D6Axis.Z, VSD6Joint.D6Motion.FREE),
            ))
        )
        mc(mainConstraint, cIDs, level) {return false}
        return true
    }
}