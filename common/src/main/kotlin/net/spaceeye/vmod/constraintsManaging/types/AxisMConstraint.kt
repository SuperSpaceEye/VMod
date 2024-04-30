package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.*
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.deserializeConstraint
import net.spaceeye.vmod.utils.vs.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.rendering.SynchronisedRenderingData
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.VSConstraintSerializationUtil
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.utils.vs.posWorldToShip
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId

//TODO do i need it? maybe just have WeldMConstraint with option to not make fixed orientation constraint
class AxisMConstraint(): MConstraint, MRenderable {
    lateinit var aconstraint1: VSAttachmentConstraint
    lateinit var aconstraint2: VSAttachmentConstraint
    lateinit var rconstraint: VSConstraint

    val cIDs = mutableListOf<ConstraintId>()
    var attachmentPoints_ = mutableListOf<BlockPos>()

    var fixedLength: Double = 0.0

    override var renderer: BaseRenderer? = null

    constructor(
        // shipyard pos
        spoint1: Vector3d,
        spoint2: Vector3d,
        // world pos
        rpoint1: Vector3d,
        rpoint2: Vector3d,
        ship1: Ship?,
        ship2: Ship?,
        shipId0: ShipId,
        shipId1: ShipId,
        compliance: Double,
        maxForce: Double,
        fixedLength: Double = -1.0,

        attachmentPoints: List<BlockPos>,

        renderer: BaseRenderer? = null
    ): this() {
        aconstraint1 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, if (fixedLength < 0) (rpoint1 - rpoint2).dist() else fixedLength)

        this.fixedLength = if (fixedLength < 0) (rpoint1 - rpoint2).dist() else fixedLength

        val dist1 = rpoint1 - rpoint2
        val len = dist1.dist()
        val dir = dist1.normalize() * ( if (len < 10 || len > 30) 20 else 40)

        val rpoint1 = rpoint1 + dir
        val rpoint2 = rpoint2 - dir

        val dist2 = rpoint1 - rpoint2
        val addDist = dist2.dist() - dist1.dist()

        val spoint1 = if (ship1 != null) posWorldToShip(ship1, rpoint1) else Vector3d(rpoint1)
        val spoint2 = if (ship2 != null) posWorldToShip(ship2, rpoint2) else Vector3d(rpoint2)

        aconstraint2 = VSAttachmentConstraint(
            shipId0, shipId1,
            compliance,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            maxForce, if (fixedLength < 0) (rpoint1 - rpoint2).dist() else fixedLength + addDist
        )

        val hrot1 = getHingeRotation(ship1?.transform, dir.normalize())
        val hrot2 = getHingeRotation(ship2?.transform, dir.normalize())

        rconstraint = VSHingeOrientationConstraint(
            shipId0, shipId1, compliance, hrot1, hrot2,
            maxForce)

        this.renderer = renderer
        attachmentPoints_ = attachmentPoints.toMutableList()
    }

    override var mID: ManagedConstraintId = -1
    override val typeName: String get() = "AxisMConstraint"
    override var saveCounter: Int = -1

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(aconstraint1.shipId0)
        val ship2Exists = allShips.contains(aconstraint1.shipId1)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(aconstraint1.shipId1))
                || (ship2Exists && dimensionIds.contains(aconstraint1.shipId0))
    }

    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(aconstraint1.shipId0)) {toReturn.add(aconstraint1.shipId0)}
        if (!dimensionIds.contains(aconstraint1.shipId1)) {toReturn.add(aconstraint1.shipId1)}

        return toReturn
    }

    override fun getAttachmentPoints(): List<BlockPos> = attachmentPoints_
    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        if (previous != attachmentPoints_[0] && previous != attachmentPoints_[1]) {return}
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        cIDs.clear()

        val shipIds = mutableListOf(aconstraint1.shipId0, aconstraint1.shipId1)
        val localPoints = mutableListOf(
            listOf(aconstraint1.localPos0, aconstraint2.localPos0),
            listOf(aconstraint1.localPos1, aconstraint2.localPos1)
        )
        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)

        aconstraint1 = VSAttachmentConstraint(shipIds[0], shipIds[1], aconstraint1.compliance, localPoints[0][0], localPoints[1][0], aconstraint1.maxForce, aconstraint1.fixedDistance)
        aconstraint2 = VSAttachmentConstraint(shipIds[0], shipIds[1], aconstraint2.compliance, localPoints[0][1], localPoints[1][1], aconstraint2.maxForce, aconstraint2.fixedDistance)

        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1)!!)
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2)!!)

        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], mID)

        renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(mID)
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        return commonCopy(level, mapped, aconstraint1, attachmentPoints_, renderer) {
            nShip1Id, nShip2Id, nShip1, nShip2, localPos0, localPos1, newAttachmentPoints, newRenderer ->
            commonCopy(level, mapped, aconstraint2, attachmentPoints_, renderer) {
                _, _, _, _, slocalPos0, slocalPos1, _, _ ->

                val srpoint1 = if (nShip1 != null) { posShipToWorld(nShip1, slocalPos0) } else slocalPos0
                val srpoint2 = if (nShip2 != null) { posShipToWorld(nShip2, slocalPos1) } else slocalPos1


                val rpoint1 = if (nShip1 != null) { posShipToWorld(nShip1, localPos0) } else localPos0
                val rpoint2 = rpoint1 + (srpoint1 - srpoint2).normalize() * fixedLength


                AxisMConstraint(localPos0, localPos1, rpoint1, rpoint2, nShip1, nShip2, nShip1Id, nShip2Id, aconstraint1.compliance, aconstraint1.maxForce, fixedLength, newAttachmentPoints, newRenderer)
            }
        }
    }

    override fun onScale(level: ServerLevel, scale: Double) {
        val ratio = aconstraint2.fixedDistance / aconstraint1.fixedDistance
        val newDistance = fixedLength * scale

        aconstraint1 = VSAttachmentConstraint(aconstraint1.shipId0, aconstraint1.shipId1, aconstraint1.compliance, aconstraint1.localPos0, aconstraint1.localPos1, aconstraint1.maxForce, newDistance)
        aconstraint2 = VSAttachmentConstraint(aconstraint2.shipId0, aconstraint2.shipId1, aconstraint2.compliance, aconstraint2.localPos0, aconstraint2.localPos1, aconstraint2.maxForce, newDistance * ratio)

        level.shipObjectWorld.removeConstraint(cIDs[0])
        level.shipObjectWorld.removeConstraint(cIDs[1])

        cIDs[0] = level.shipObjectWorld.createNewConstraint(aconstraint1)!!
        cIDs[1] = level.shipObjectWorld.createNewConstraint(aconstraint2)!!
    }

    override fun getVSIds(): Set<VSConstraintId> {
        return cIDs.toSet()
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.put("c1", VSConstraintSerializationUtil.serializeConstraint(aconstraint1) ?: return null)
        tag.put("c2", VSConstraintSerializationUtil.serializeConstraint(aconstraint2) ?: return null)
        tag.put("c3", VSConstraintSerializationUtil.serializeConstraint(rconstraint ) ?: return null)
        tag.putInt("managedID", mID)
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))
        tag.putDouble("fixedLength", fixedLength)

        serializeRenderer(tag)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = tag.getInt("managedID")
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)
        fixedLength = tag.getDouble("fixedLength")

        deserializeRenderer(tag)

        tryConvertDimensionId(tag["c1"] as CompoundTag, lastDimensionIds); aconstraint1 = (deserializeConstraint(tag["c1"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c2"] as CompoundTag, lastDimensionIds); aconstraint2 = (deserializeConstraint(tag["c2"] as CompoundTag) ?: return null) as VSAttachmentConstraint
        tryConvertDimensionId(tag["c3"] as CompoundTag, lastDimensionIds); rconstraint  = (deserializeConstraint(tag["c3"] as CompoundTag) ?: return null)

        return this
    }

    private fun <T> clean(level: ServerLevel): T? {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        return null
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint1) ?: clean(level) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(aconstraint2) ?: clean(level) ?: return false)
        cIDs.add(level.shipObjectWorld.createNewConstraint(rconstraint ) ?: clean(level) ?: return false)

        if (renderer != null) { SynchronisedRenderingData.serverSynchronisedData.addRenderer(aconstraint1.shipId0, aconstraint1.shipId1, mID, renderer!!)
        } else { renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(mID) }
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        cIDs.forEach { level.shipObjectWorld.removeConstraint(it) }
        SynchronisedRenderingData.serverSynchronisedData.removeRenderer(mID)
    }
}