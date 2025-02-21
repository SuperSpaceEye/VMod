package net.spaceeye.vmod.vEntityManaging.types.constraints

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.util.*
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.reflectable.TagSerializableItem
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getHingeRotation
import net.spaceeye.vmod.utils.vs.copyAttachmentPoints
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSSlideConstraint
import org.valkyrienskies.core.apigame.joints.*
import java.util.*

class SliderConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
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

    var connectionMode: ConnectionMode by get(i++, ConnectionMode.FIXED_ORIENTATION)
    var maxForce: Float by get(i++, -1f)

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

        connectionMode: ConnectionMode,

        attachmentPoints: List<BlockPos>,
    ): this() {
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

        attachmentPoints_.addAll(attachmentPoints)
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>): VEntity? {
        return SliderConstraint(
            tryMovePosition(sPos1, shipId1, level, mapped) ?: return null,
            tryMovePosition(sPos2, shipId2, level, mapped) ?: return null,
            sDir1, sDir2, sRot1, sRot2,
            mapped[shipId1] ?: return null,
            mapped[shipId2] ?: return null,
            maxForce, connectionMode,
            copyAttachmentPoints(sPos1, sPos2, shipId1, shipId2, attachmentPoints_, level, mapped),
        )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        val compliance = 1e-300
        val maxForce = if (maxForce < 0) { Float.MAX_VALUE.toDouble() } else {maxForce.toDouble()}
        val constraint = VSSlideConstraint(shipId1, shipId2, compliance,
            sPos1.toJomlVector3d(), sPos2.toJomlVector3d(),
            maxForce, sDir1.toJomlVector3d(), 100.0)

        mc(constraint, cIDs, level) {return false}

        return true
    }
    companion object {
        init {
            TagSerializableItem.registerSerializationEnum(ConnectionMode::class)
        }
    }
}