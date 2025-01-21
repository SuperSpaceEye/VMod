package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.constraintsManaging.util.MCAutoSerializable
import net.spaceeye.vmod.constraintsManaging.util.NewTwoShipsMConstraint
import net.spaceeye.vmod.constraintsManaging.util.mc
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSDistanceJoint
import org.valkyrienskies.core.apigame.joints.VSJointPose
import net.spaceeye.vmod.networking.TagSerializableItem.get
import net.spaceeye.vmod.utils.vs.copyAttachmentPoints
import net.spaceeye.vmod.utils.vs.tryMovePosition

class RopeMConstraint(): NewTwoShipsMConstraint(), MCAutoSerializable {
    override lateinit var sPos1: Vector3d
    override lateinit var sPos2: Vector3d
    override var shipId1: Long = -1
    override var shipId2: Long = -1

    var maxForce: Float by get(0, 0f)
    var ropeLength: Float by get(1, 0f)

    constructor(
        sPos1: Vector3d,
        sPos2: Vector3d,

        shipId1: ShipId,
        shipId2: ShipId,

        maxForce: Float,
        ropeLength: Float,
        attachmentPoints: List<BlockPos>,
    ): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2
        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()
        this.maxForce = maxForce
        this.ropeLength = ropeLength
        attachmentPoints_ = attachmentPoints.toMutableList()
    }

    override fun iCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
         return RopeMConstraint(
             tryMovePosition(sPos1, shipId1, level, mapped) ?: return null,
             tryMovePosition(sPos2, shipId2, level, mapped) ?: return null,
             mapped[shipId1] ?: return null,
             mapped[shipId2] ?: return null,
             maxForce, ropeLength,
             copyAttachmentPoints(sPos1, sPos2, shipId1, shipId2, attachmentPoints_, level, mapped),
        )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        ropeLength *= scaleBy.toFloat()
        onDeleteMConstraint(level)
        onMakeMConstraint(level)
    }

    override fun iOnMakeMConstraint(level: ServerLevel): Boolean {
        val mainConstraint = VSDistanceJoint(
            shipId1, VSJointPose(sPos1.toJomlVector3d(), Quaterniond()),
            shipId2, VSJointPose(sPos2.toJomlVector3d(), Quaterniond()),
            minDistance = 0f,
            maxDistance = ropeLength
        )
        mc(mainConstraint, cIDs, level) {return false}
        return true
    }
}