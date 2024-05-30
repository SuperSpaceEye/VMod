package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.deserializeConstraint
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.*
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*

//TODO remvoe in the future
class AxisMConstraint(): MConstraint, MRenderable {
    override var renderer: BaseRenderer? = null

    override var mID: ManagedConstraintId = -1
    override val typeName: String get() = "AxisMConstraint"
    override var saveCounter: Int = -1

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean { throw AssertionError("WAS REMOVED") }
    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> { throw AssertionError("WAS REMOVED") }
    override fun getAttachmentPoints(): List<BlockPos> = throw AssertionError("WAS REMOVED")
    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) { throw AssertionError("WAS REMOVED") }
    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? { throw AssertionError("WAS REMOVED") }
    override fun onScaleBy(level: ServerLevel, scaleBy: Double) { throw AssertionError("WAS REMOVED") }
    override fun getVSIds(): Set<VSConstraintId> { throw AssertionError("WAS REMOVED") }
    override fun nbtSerialize(): CompoundTag? { throw AssertionError("WAS REMOVED") }
    override fun onMakeMConstraint(level: ServerLevel): Boolean { throw AssertionError("WAS REMOVED") }
    override fun onDeleteMConstraint(level: ServerLevel) { throw AssertionError("WAS REMOVED") }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        val ret = ConnectionMConstraint()

        ret.mID = tag.getInt("managedID")
        ret.attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)
        ret.fixedLength = tag.getDouble("fixedLength")

        deserializeRenderer(tag)

        tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); ret.aconstraint1 = (deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c2"] as CompoundTag, lastDimensionIds); ret.aconstraint2 = (deserializeConstraint(tag["c2"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c3"] as CompoundTag, lastDimensionIds); ret.rconstraint  = (deserializeConstraint(tag["c3"] as CompoundTag) ?: return null) as VSTorqueConstraint

        ret.renderer = renderer
        ret.connectionMode = ConnectionMConstraint.ConnectionModes.HINGE_ORIENTATION

        return ret
    }
}