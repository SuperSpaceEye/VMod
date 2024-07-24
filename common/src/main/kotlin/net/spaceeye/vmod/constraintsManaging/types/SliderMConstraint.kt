package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.MConstraint
import net.spaceeye.vmod.constraintsManaging.MRenderable
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.commonCopy
import net.spaceeye.vmod.rendering.ServerRenderingData
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.deserializeBlockPositions
import net.spaceeye.vmod.utils.serializeBlockPositions
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId

class SliderMConstraint(): MConstraint, MRenderable {
    override var mID: ManagedConstraintId = -1

    lateinit var constraint1: VSAttachmentConstraint
    lateinit var constraint2: VSAttachmentConstraint

    override var renderer: BaseRenderer? = null
    var attachmentPoints_ = mutableListOf<BlockPos>()


    var rID: Int = -1
    override val typeName: String = "SliderMConstraint"

    val cIDs = mutableListOf<ConstraintId>()

    constructor(
        axisShipId: ShipId,
        sliderShipId: ShipId,

        axisSpos1: Vector3d,
        axisSpos2: Vector3d,
        sliderSpos1: Vector3d,
        sliderSpos2: Vector3d,

        compliance: Double,
        maxForce: Double,

        attachmentPoints: List<BlockPos>,
        renderer: BaseRenderer?
    ): this() {
        constraint1 = VSAttachmentConstraint(
            axisShipId, sliderShipId, compliance,
            axisSpos1.toJomlVector3d(), sliderSpos1.toJomlVector3d(),
            maxForce, 0.0
        )

        constraint2 = VSAttachmentConstraint(
            axisShipId, sliderShipId, compliance,
            axisSpos2.toJomlVector3d(), sliderSpos2.toJomlVector3d(),
            maxForce, 0.0
        )

        attachmentPoints_.addAll(attachmentPoints)

        this.renderer = renderer
    }

    override var saveCounter: Int = -1

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(constraint1.shipId0)
        val ship2Exists = allShips.contains(constraint1.shipId1)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(constraint1.shipId1))
                || (ship2Exists && dimensionIds.contains(constraint1.shipId0))
    }

    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(constraint1.shipId0)) {toReturn.add(constraint1.shipId0)}
        if (!dimensionIds.contains(constraint1.shipId1)) {toReturn.add(constraint1.shipId1)}

        return toReturn
    }

    override fun getAttachmentPoints(): List<BlockPos> = attachmentPoints_
    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        TODO("Not yet implemented")
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, constraint1, listOf(attachmentPoints_[0], attachmentPoints_[1]), null) {
            _, _, _, _, axis1, ship1, newAPPair1, _ ->
            commonCopy(level, mapped, constraint2, listOf(attachmentPoints_[2], attachmentPoints_[3]), renderer) {
                nShip1Id, nShip2Id, nShip1, nShip2, axis2, ship2, newAPPair2, newRenderer ->
                val level = level
                SliderMConstraint(nShip1Id, nShip2Id, axis1, axis2, ship1, ship2,
                    constraint1.compliance, constraint1.maxForce,
                    listOf(newAPPair1[0], newAPPair1[1], newAPPair2[0], newAPPair2[1]),
                    newRenderer)
            }
        }
    }

    // it doesn't need to do anything
    override fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {}
    override fun getVSIds(): Set<VSConstraintId> = cIDs.toSet()

    override fun nbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putInt("mID", mID)
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))
        serializeRenderer(tag)

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(constraint1) ?: return null)
        tag.put("c2", VSConstraintSerializationUtil.serializeConstraint(constraint2) ?: return null)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("mID")
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)
        deserializeRenderer(tag)

        VSConstraintDeserializationUtil.tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); constraint1 = (VSConstraintDeserializationUtil.deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        VSConstraintDeserializationUtil.tryConvertDimensionId(tag["c2"] as CompoundTag, lastDimensionIds); constraint2 = (VSConstraintDeserializationUtil.deserializeConstraint(tag["c2"] as CompoundTag) ?: return null) as VSAttachmentConstraint

        return this
    }

    private fun <T> clean(level: ServerLevel): T? {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        return null
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        if (renderer != null) { rID = ServerRenderingData.addRenderer(constraint1.shipId0, constraint1.shipId1, renderer!!)
        } else { renderer = ServerRenderingData.getRenderer(rID) }

        cIDs.add(level.shipObjectWorld.createNewConstraint(constraint1) ?: clean(level) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(constraint2) ?: clean(level) ?: return false)

        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        ServerRenderingData.removeRenderer(rID)
    }
}