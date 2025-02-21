package net.spaceeye.vmod.vEntityManaging.types.constraints

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.util.VEAutoSerializable
import net.spaceeye.vmod.vEntityManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.vEntityManaging.util.mc
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.reflectable.TagSerializableItem
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.*
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint

class ConnectionConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
    enum class ConnectionModes {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }
    override lateinit var sPos1: Vector3d
    override lateinit var sPos2: Vector3d
    override var shipId1: Long = -1
    override var shipId2: Long = -1
    @JsonIgnore private var i = 0

    var connectionMode: ConnectionModes by get(i++, ConnectionModes.FIXED_ORIENTATION)
    var distance: Float by get(i++, 0f)
    var maxForce: Float by get(i++, -1f)
    var stiffness: Float by get(i++, -1f)
    var damping: Float by get(i++, -1f)

    var sRot1: Quaterniond by get(i++, Quaterniond())
    var sRot2: Quaterniond by get(i++, Quaterniond())

    var sDir1: Vector3d by get(i++, Vector3d())
    var sDir2: Vector3d by get(i++, Vector3d())

    constructor(
        sPos1: Vector3d,
        sPos2: Vector3d,

        sDir1: Vector3d,
        sDir2: Vector3d,

        sRot1: Quaterniond,
        sRot2: Quaterniond,

        shipId1: ShipId,
        shipId2: ShipId,

        maxForce: Float,
        stiffness: Float,
        damping: Float,

        distance: Float,
        connectionMode: ConnectionModes,

        attachmentPoints: List<BlockPos>,
        ): this() {
        this.distance = distance
        this.connectionMode = connectionMode
        attachmentPoints_ = attachmentPoints.toMutableList()
        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()

        this.sDir1 = sDir1.copy()
        this.sDir2 = sDir2.copy()

        this.sRot1 = Quaterniond(sRot1)
        this.sRot2 = Quaterniond(sRot2)

        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.maxForce = maxForce
        this.stiffness = stiffness
        this.damping = damping
     }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>): VEntity? {
        val new = ConnectionConstraint(
            tryMovePosition(sPos1, shipId1, level, mapped) ?: return null,
            tryMovePosition(sPos2, shipId2, level, mapped) ?: return null,
            sDir1, sDir2, sRot1, sRot2,
            mapped[shipId1] ?: return null,
            mapped[shipId2] ?: return null,
            maxForce, stiffness, damping, distance, connectionMode,
            copyAttachmentPoints(sPos1, sPos2, shipId1, shipId2, attachmentPoints_, level, mapped),
        )
        new.sDir1 = sDir1.copy()
        new.sDir2 = sDir2.copy()
        new.sRot1 = Quaterniond(sRot1)
        new.sRot2 = Quaterniond(sRot2)

        return new
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        distance *= scaleBy.toFloat()
        onDeleteVEntity(level)
        onMakeVEntity(level)
    }

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        val maxForce = if (maxForce < 0) { Float.MAX_VALUE.toDouble() } else { maxForce.toDouble() }
        val compliance = if (stiffness <= 0f) { Float.MIN_VALUE.toDouble() } else { (1f / stiffness).toDouble() }

        val p11 = sPos1.toJomlVector3d()
        val p21 = (sPos2 - sDir2 * distance).toJomlVector3d()
        val p12 = (sPos1 + sDir1 * distance).toJomlVector3d()
        val p22 = sPos2.toJomlVector3d()

        val a1 = VSAttachmentConstraint(shipId1, shipId2, compliance, p11, p21, maxForce, 0.0)
        val a2 = VSAttachmentConstraint(shipId1, shipId2, compliance, p12, p22, maxForce, 0.0)

        mc(a1, cIDs, level) {return false}
        if (connectionMode == ConnectionModes.FREE_ORIENTATION) {return true}
        mc(a2, cIDs, level) {return false}

        val r1 = when (connectionMode) {
            ConnectionModes.FIXED_ORIENTATION -> VSFixedOrientationConstraint(shipId1, shipId2, compliance, sRot1.invert(Quaterniond()), sRot2.invert(Quaterniond()), maxForce)
            ConnectionModes.HINGE_ORIENTATION -> VSHingeOrientationConstraint(shipId1, shipId2, compliance, getHingeRotation(sDir1), getHingeRotation(sDir2), maxForce)
            else -> throw AssertionError("Impossible")
        }
        mc(r1, cIDs, level) {return false}

        return true
    }
    
    companion object {
        init {
            TagSerializableItem.registerSerializationEnum(ConnectionModes::class)
        }
    }
}