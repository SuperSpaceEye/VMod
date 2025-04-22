package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.util.*
import net.spaceeye.vmod.reflectable.TagSerializableItem
import net.spaceeye.vmod.utils.*
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint

class HydraulicsConstraint(): TwoShipsMConstraint(), VEAutoSerializable, Tickable {
    enum class ConnectionMode {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }

    override lateinit var sPos1: Vector3d
    override lateinit var sPos2: Vector3d
    override var shipId1: Long = -1
    override var shipId2: Long = -1

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
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
        return HydraulicsConstraint(
            tryMovePosition(sPos1, shipId1, centerPositions) ?: return null,
            tryMovePosition(sPos2, shipId2, centerPositions) ?: return null,
            sDir1, sDir2, sRot1, sRot2,
            mapped[shipId1] ?: return null,
            mapped[shipId2] ?: return null,
            maxForce, stiffness, damping,
            minLength, maxLength, extensionSpeed * 20f, channel, connectionMode
        )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        val scaleBy = scaleBy.toFloat()
        minLength *= scaleBy
        maxLength *= scaleBy
        extendedDist *= scaleBy
        extensionSpeed *= scaleBy

        sDir1.divAssign(scaleBy)
        sDir2.divAssign(scaleBy)

        super.iOnDeleteVEntity(level)
        iOnMakeVEntity(level)
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
        getExtensionsOfType<TickableVEntityExtension>().forEach { it.tick(server) }
        if (!tryExtendDist()) {return}

        if (lastExtended == extendedDist) {return}
        lastExtended = extendedDist

        super.iOnDeleteVEntity(server.overworld())
        iOnMakeVEntity(server.overworld())
    }

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        val maxForce = if (maxForce < 0) { Float.MAX_VALUE.toDouble() } else { maxForce.toDouble() }
        val compliance = if (stiffness <= 0f) { Float.MIN_VALUE.toDouble() } else { (1f / stiffness).toDouble() }
        val distance = (minLength + extendedDist).toDouble()

        val p11 = sPos1.toJomlVector3d()
        val p21 = (sPos2 - sDir2 * distance).toJomlVector3d()
        val p12 = (sPos1 + sDir1 * distance).toJomlVector3d()
        val p22 = sPos2.toJomlVector3d()

        val a1 = VSAttachmentConstraint(shipId1, shipId2, compliance, p11, p21, maxForce, 0.0)
        val a2 = VSAttachmentConstraint(shipId1, shipId2, compliance, p12, p22, maxForce, 0.0)

        mc(a1, cIDs, level) {return false}
        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {return true}
        mc(a2, cIDs, level) {return false}

        val r1 = when (connectionMode) {
            ConnectionMode.FIXED_ORIENTATION -> VSFixedOrientationConstraint(shipId1, shipId2, compliance, sRot1.invert(Quaterniond()), sRot2.invert(Quaterniond()), maxForce)
            ConnectionMode.HINGE_ORIENTATION -> VSHingeOrientationConstraint(shipId1, shipId2, compliance, getHingeRotation(sDir1), getHingeRotation(sDir2), maxForce)
            else -> throw AssertionError("Impossible")
        }
        mc(r1, cIDs, level) {return false}

        return true
    }

    override fun iOnDeleteVEntity(level: ServerLevel) {
        super.iOnDeleteVEntity(level)
        wasDeleted = true
    }

    companion object {
        init {
            TagSerializableItem.registerSerializationEnum(ConnectionMode::class)
        }
    }
}