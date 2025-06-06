package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.util.*
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint

class SyncRotationConstraint(): TwoShipsMConstraint(), VEAutoSerializable {
    override var sPos1: Vector3d get() = Vector3d(); set(_) {}
    override var sPos2: Vector3d get() = Vector3d(); set(_) {}
    override var shipId1: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }
    override var shipId2: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }

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

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
        return SyncRotationConstraint(Quaterniond(sRot1), Quaterniond(sRot2), shipId1, shipId2, maxForce)
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}
    override fun iGetAttachmentPoints(shipId: ShipId): List<Vector3d> = emptyList()

    override fun iOnMakeVEntity(level: ServerLevel): Boolean {
        val maxForce = if (maxForce < 0) { Float.MAX_VALUE.toDouble() } else { maxForce.toDouble() }
        val compliance = Double.MIN_VALUE

        val c = VSFixedOrientationConstraint(shipId1, shipId2, compliance, sRot1, sRot2, maxForce)
        mc(c, cIDs, level) {return false}
        return true
    }
}