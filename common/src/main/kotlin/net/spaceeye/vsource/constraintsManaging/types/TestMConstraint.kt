package net.spaceeye.vsource.constraintsManaging.types

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vsource.constraintsManaging.ManagedConstraintId
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.abs
import kotlin.math.sin

private const val pi2 = Math.PI / 2

class TestMConstraint(): MConstraint {
    lateinit var aConstraint: VSAttachmentConstraint
    var vsID: VSConstraintId = 0
    var period = 60
    var cTick = 0
    var maxLength = 5.0
    var minLength = 2.0

    override lateinit var mID: ManagedConstraintId
    override val shipId0: ShipId get() = aConstraint.shipId0
    override val shipId1: ShipId get() = aConstraint.shipId1
    override val typeName: String
        get() = "TestMConstraint"

    constructor(
        shipId0: ShipId,
        shipId1: ShipId,
        compliance: Double,
        localPos0: Vector3dc,
        localPos1: Vector3dc,
        maxForce: Double,

        minDistance: Double,
        maxDistance: Double,
        period: Int = 60,
    ): this() {
        aConstraint = VSAttachmentConstraint(shipId0, shipId1, compliance, localPos0, localPos1, maxForce, minDistance)

        minLength = minDistance
        maxLength = maxDistance
        this.period = period
    }

    override fun nbtSerialize(): CompoundTag? {
        return CompoundTag()
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        return null
    }

    var wasDeleted = false

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        vsID = level.shipObjectWorld.createNewConstraint(aConstraint) ?: return false

        // TODO this is not very efficient
        VSEvents.tickEndEvent.on {
            (shipObjectWorld), handler ->
            if (wasDeleted) {
                handler.unregister()
                return@on
            }

            val progress = abs(sin(cTick / period.toDouble() * pi2))

            shipObjectWorld.updateConstraint(vsID, VSAttachmentConstraint(
                aConstraint.shipId0,
                aConstraint.shipId1,
                aConstraint.compliance,
                aConstraint.localPos0,
                aConstraint.localPos1,
                aConstraint.maxForce,
                minLength + (maxLength - minLength) * progress
            ))

            cTick++
        }

        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        wasDeleted = true
        level.shipObjectWorld.removeConstraint(vsID)
    }
}