package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.util.VEAutoSerializable
import net.spaceeye.vmod.vEntityManaging.util.TwoShipsMConstraint
import net.spaceeye.vmod.vEntityManaging.util.mc
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.properties.ShipId
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint

class RopeConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
    override lateinit var sPos1: Vector3d
    override lateinit var sPos2: Vector3d
    override var shipId1: Long = -1
    override var shipId2: Long = -1

    var maxForce: Float by get(i++, -1f)
    var stiffness: Float by get(i++, 0f)
    var damping: Float by get(i++, 0f)
    var ropeLength: Float by get(i++, 0f)

     constructor(
        sPos1: Vector3d,
        sPos2: Vector3d,

        shipId1: ShipId,
        shipId2: ShipId,

        maxForce: Float,
        stiffness: Float,
        damping: Float,

        ropeLength: Float
    ): this() {
        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()

        this.maxForce = maxForce
        this.stiffness = stiffness
        this.damping = damping

        this.ropeLength = ropeLength
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>): VEntity? {
         return RopeConstraint(
             tryMovePosition(sPos1, shipId1, level, mapped) ?: return null,
             tryMovePosition(sPos2, shipId2, level, mapped) ?: return null,
             mapped[shipId1] ?: return null,
             mapped[shipId2] ?: return null,
             maxForce, stiffness, damping, ropeLength
         )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        ropeLength *= scaleBy.toFloat()
        onDeleteVEntity(level)
        onMakeVEntity(level)
    }

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        val maxForce = if (maxForce < 0) { Float.MAX_VALUE.toDouble() } else { maxForce.toDouble() }
        val compliance = if (stiffness <= 0f) { Float.MIN_VALUE.toDouble() } else { (1f / stiffness).toDouble() }

        val c = VSRopeConstraint(shipId1, shipId2, compliance, sPos1.toJomlVector3d(), sPos2.toJomlVector3d(), maxForce, ropeLength.toDouble())
        mc(c, cIDs, level) { return false }
        return true
    }
}