package net.spaceeye.vmod.constraintsManaging.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.ManagedConstraintId
import net.spaceeye.vmod.constraintsManaging.VSConstraintDeserializationUtil
import net.spaceeye.vmod.constraintsManaging.VSConstraintDeserializationUtil.tryConvertDimensionId
import net.spaceeye.vmod.constraintsManaging.VSConstraintSerializationUtil
import net.spaceeye.vmod.rendering.SynchronisedRenderingData
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.deserializeBlockPositions
import net.spaceeye.vmod.utils.serializeBlockPositions
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.physics_api.ConstraintId
import java.lang.AssertionError

class RopeMConstraint(): MConstraint {
    lateinit var constraint: VSRopeConstraint
    private var renderer: BaseRenderer? = null

    var attachmentPoints_ = mutableListOf<BlockPos>()

    var ropeLength = 0.0

    var vsId: ConstraintId = -1

    constructor(
        shipId0: ShipId,
        shipId1: ShipId,
        compliance: Double,
        localPos0: Vector3dc,
        localPos1: Vector3dc,
        maxForce: Double,
        ropeLength: Double,
        attachmentPoints: List<BlockPos>,
        renderer: BaseRenderer?
    ): this() {
        constraint = VSRopeConstraint(shipId0, shipId1, compliance, localPos0, localPos1, maxForce, ropeLength)
        this.renderer = renderer
        attachmentPoints_ = attachmentPoints.toMutableList()
        this.ropeLength = ropeLength
    }

    override lateinit var mID: ManagedConstraintId
    override val typeName: String get() = "RopeMConstraint"
    override var saveCounter: Int = -1

    override fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean {
        val ship1Exists = allShips.contains(constraint.shipId0)
        val ship2Exists = allShips.contains(constraint.shipId1)

        return     (ship1Exists && ship2Exists)
                || (ship1Exists && dimensionIds.contains(constraint.shipId1))
                || (ship2Exists && dimensionIds.contains(constraint.shipId0))
    }

    override fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId> {
        val toReturn = mutableListOf<ShipId>()

        if (!dimensionIds.contains(constraint.shipId0)) {toReturn.add(constraint.shipId0)}
        if (!dimensionIds.contains(constraint.shipId1)) {toReturn.add(constraint.shipId1)}

        return toReturn
    }

    override fun getAttachmentPoints(): List<BlockPos> = attachmentPoints_
    override fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId) {
        if (previous != attachmentPoints_[0] && previous != attachmentPoints_[1]) {return}
        level.shipObjectWorld.removeConstraint(vsId)

        val shipIds = mutableListOf(constraint.shipId0, constraint.shipId1)
        val localPoints = mutableListOf(
            listOf(constraint.localPos0),
            listOf(constraint.localPos1)
        )
        updatePositions(newShipId, previous, new, attachmentPoints_, shipIds, localPoints)

        constraint = VSRopeConstraint(shipIds[0], shipIds[1], constraint.compliance, localPoints[0][0], localPoints[1][0], constraint.maxForce, constraint.ropeLength)

        vsId = level.shipObjectWorld.createNewConstraint(constraint)!!

        renderer = updateRenderer(localPoints[0][0], localPoints[1][0], shipIds[0], shipIds[1], mID)
    }

    override fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint? {
        if (!mapped.keys.containsAll(listOf(constraint.shipId0, constraint.shipId1))) { return null }

        val dimensionIds = level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values.toSet()

        val oShip1 = if (!dimensionIds.contains(constraint.shipId0)) level.shipObjectWorld.loadedShips.getById(constraint.shipId0) else null
        val oShip2 = if (!dimensionIds.contains(constraint.shipId1)) level.shipObjectWorld.loadedShips.getById(constraint.shipId1) else null

        val nShip1 = if (oShip1 != null) level.shipObjectWorld.loadedShips.getById(mapped[constraint.shipId0]!!) else null
        val nShip2 = if (oShip2 != null) level.shipObjectWorld.loadedShips.getById(mapped[constraint.shipId1]!!) else null

        val localPos0 = if (nShip1 != null) Vector3d(constraint.localPos0) - Vector3d(nShip1.transform.positionInShip) + Vector3d(oShip1!!.transform.positionInShip) else Vector3d(constraint.localPos0)
        val localPos1 = if (nShip2 != null) Vector3d(constraint.localPos1) - Vector3d(nShip2.transform.positionInShip) + Vector3d(oShip2!!.transform.positionInShip) else Vector3d(constraint.localPos1)

        val apoint1 = (if (nShip1 != null) Vector3d(attachmentPoints_[0]) - Vector3d(nShip1.transform.positionInShip) + Vector3d(oShip1!!.transform.positionInShip) else Vector3d(attachmentPoints_[0])).toBlockPos()
        val apoint2 = (if (nShip2 != null) Vector3d(attachmentPoints_[1]) - Vector3d(nShip2.transform.positionInShip) + Vector3d(oShip2!!.transform.positionInShip) else Vector3d(attachmentPoints_[1])).toBlockPos()

        val newAttachmentPoints = listOf(apoint1, apoint2)

        var newRenderer: BaseRenderer? = null
        if (renderer != null) {
            newRenderer = when (renderer) {
                is A2BRenderer -> {
                    val renderer = renderer as A2BRenderer
                    A2BRenderer(nShip1 != null, nShip2 != null, localPos0, localPos1, renderer.color, renderer.width)
                }
                is RopeRenderer -> {
                    val renderer = renderer as RopeRenderer
                    RopeRenderer(nShip1 != null, nShip2 != null, localPos0, localPos1, renderer.length, renderer.width, renderer.segments)
                }

                else -> {throw AssertionError()}
            }
        }

        return RopeMConstraint(nShip1?.id ?: constraint.shipId0, nShip2?.id ?: constraint.shipId1, constraint.compliance, localPos0.toJomlVector3d(), localPos1.toJomlVector3d(), constraint.maxForce, constraint.ropeLength, newAttachmentPoints, newRenderer)
    }

    override fun onScale(level: ServerLevel, scale: Double) {
        constraint = VSRopeConstraint(constraint.shipId0, constraint.shipId1, constraint.compliance, constraint.localPos0, constraint.localPos1, constraint.maxForce, ropeLength * scale)

        level.shipObjectWorld.removeConstraint(vsId)
        vsId = level.shipObjectWorld.createNewConstraint(constraint)!!
    }

    override fun getVSIds(): Set<VSConstraintId> {
        return setOf(vsId)
    }

    override fun nbtSerialize(): CompoundTag? {
        val tag = VSConstraintSerializationUtil.serializeConstraint(constraint) ?: return null

        tag.putInt("managedID", mID.id)
        tag.put("attachmentPoints", serializeBlockPositions(attachmentPoints_))
        tag.putDouble("ropeLength", ropeLength)

        return tag
    }

    override fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint? {
        mID = ManagedConstraintId(if (tag.contains("managedID")) tag.getInt("managedID") else -1)
        attachmentPoints_ = deserializeBlockPositions(tag.get("attachmentPoints")!!)

        tryConvertDimensionId(tag, lastDimensionIds); constraint = (VSConstraintDeserializationUtil.deserializeConstraint(tag) ?: return null) as VSRopeConstraint

        ropeLength = if (tag.contains("ropeLength")) tag.getDouble("ropeLength") else constraint.ropeLength

        return this
    }

    override fun onMakeMConstraint(level: ServerLevel): Boolean {
        vsId = level.shipObjectWorld.createNewConstraint(constraint) ?: return false
        if (renderer != null) { SynchronisedRenderingData.serverSynchronisedData.addRenderer(constraint.shipId0, constraint.shipId1, mID.id, renderer!!)
        } else { renderer = SynchronisedRenderingData.serverSynchronisedData.getRenderer(mID.id) }
        return true
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        level.shipObjectWorld.removeConstraint(vsId)
        SynchronisedRenderingData.serverSynchronisedData.removeRenderer(mID.id)
    }
}