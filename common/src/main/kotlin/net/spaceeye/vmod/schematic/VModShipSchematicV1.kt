package net.spaceeye.vmod.schematic

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.containers.v1.*
import net.spaceeye.valkyrien_ship_schematics.interfaces.IBlockStatePalette
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematicInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISerializable
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.SchemSerializeDataV1Impl
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.vsStuff.VSMasslessShipsProcessor
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.rotateAroundCenter
import net.spaceeye.vmod.utils.vs.traverseGetAllTouchingShips
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3i
import org.joml.primitives.AABBd
import org.joml.primitives.AABBi
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID

typealias MVector3d = net.spaceeye.vmod.utils.Vector3d

class VModShipSchematicV1(): IShipSchematic, IShipSchematicDataV1, SchemSerializeDataV1Impl {
    override var blockPalette: IBlockStatePalette = BlockPaletteHashMapV1()

    override var blockData = mutableMapOf<ShipId, ChunkyBlockData<BlockItem>>()
    override var flatTagData: MutableList<CompoundTag> = mutableListOf()

    override var extraData: MutableList<Pair<String, ISerializable>> = mutableListOf()

    override var info: IShipSchematicInfo? = null
}

fun IShipSchematicDataV1.placeAt(level: ServerLevel, uuid: UUID, pos: Vector3d, rotation: Quaterniondc, postPlaceFn: (List<ServerShip>) -> Unit): Boolean {
    val newTransforms = mutableListOf<ShipTransform>()

    val ships = (this as IShipSchematic).createShips(level, pos, rotation, newTransforms)
    ships.forEach { VSMasslessShipsProcessor.shipsToBeCreated.add(it.first.id) }

    if (!verifyBlockDataIsValid(ships, level)) {
        ships.forEach { level.shipObjectWorld.deleteShip(it.first) }
        return false
    }

    //TODO change onPasteBeforeBlocksAreLoaded to be on "per ship" basis so that you can make ships as you need instead of all at once
    // also have a onPasteBeforeBlockEntitiesAreLoaded
    ShipSchematic.onPasteBeforeBlocksAreLoaded(level, ships, extraData)
    SchematicActionsQueue.queueShipsCreationEvent(level, uuid, ships, this) {
        ShipSchematic.onPasteAfterBlocksAreLoaded(level, ships, extraData)

        val createdShips = ships.map { it.first }.onEach { VSMasslessShipsProcessor.shipsToBeCreated.remove(it.id) }
        createdShips.zip(newTransforms).forEach {
                (it, transform) ->
            val toPos = MVector3d(transform.positionInWorld) + MVector3d(pos)
            level.shipObjectWorld.teleportShip(it, ShipTeleportDataImpl(
                toPos.toJomlVector3d(),
                transform.shipToWorldRotation,
                JVector3d(),
                newScale = MVector3d(it.transform.shipToWorldScaling).avg(),
            ))
        }

        postPlaceFn(createdShips)
        SchematicActionsQueue.queueShipsUnfreezeEvent(uuid, createdShips, 10)
    }

    return true
}

private fun IShipSchematic.createShips(level: ServerLevel, pos: Vector3d, rotation: Quaterniondc, newTransforms: MutableList<ShipTransform>): List<Pair<ServerShip, Long>> {
    val shipData = info!!.shipsInfo
    // during schem creation ship positions are normalized so that the center is at 0 0 0
    val center = ShipTransformImpl(JVector3d(), JVector3d(), Quaterniond(), JVector3d(1.0, 1.0, 1.0))

    return shipData.map {
        val thisTransform = ShipTransformImpl(
            it.relPositionToCenter,
            it.positionInShip,
            it.rotation,
            JVector3d(it.shipScale, it.shipScale, it.shipScale)
        )
        val newTransform = rotateAroundCenter(center, thisTransform, rotation)
        newTransforms.add(newTransform)

        //TODO this is probably wrong?
        val toPos = MVector3d(newTransform.positionInWorld) + MVector3d(pos)

        val newShip = level.shipObjectWorld.createNewShipAtBlock(Vector3i(), false, it.shipScale, level.dimensionId)
        newShip.isStatic = true

        level.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(
            toPos.toJomlVector3d(),
            newTransform.shipToWorldRotation,
            newScale = it.shipScale
        ))
        Pair(newShip, it.id)
    }
}

fun IShipSchematicDataV1.verifyBlockDataIsValid(
    ships: List<Pair<ServerShip, Long>>,
    level: ServerLevel
): Boolean {
    ships.forEach { (ship, id) ->
        blockData[id] ?: run {
            ships.forEach { (ship, id) -> level.shipObjectWorld.deleteShip(ship) }
            ELOG("SHIP ID EXISTS BUT NO BLOCK DATA WAS SAVED. NOT PLACING A SCHEMATIC.")
            return false
        }
    }
    return true
}

fun IShipSchematicDataV1.makeFrom(level: ServerLevel, uuid: UUID, originShip: ServerShip, postSaveFn: () -> Unit): Boolean {
    val traversed = traverseGetAllTouchingShips(level, originShip.id)

    // this is needed so that schem doesn't try copying phys entities (TODO)
    val ships = traversed.mapNotNull { level.shipObjectWorld.allShips.getById(it) }

    extraData = ShipSchematic.onCopy(level, ships).toMutableList()

    (this as IShipSchematic).saveShipData(ships, originShip)
    // copy ship blocks separately
    SchematicActionsQueue.queueShipsSavingEvent(level, uuid, ships, this, true, postSaveFn)
    return true
}

private fun getWorldAABB(it: ServerShip, newTransform: ShipTransform): AABBd = it.worldAABB.transform(it.worldToShip, AABBd()).transform(newTransform.shipToWorld)

// it will save ship data with origin ship unrotated
fun IShipSchematic.saveShipData(ships: List<ServerShip>, originShip: ServerShip): AABBd {
    val invRotation = originShip.transform.shipToWorldRotation.invert(Quaterniond())
    val newTransforms = ships.map { rotateAroundCenter(originShip.transform, it.transform, invRotation) }

    val objectAABB = getWorldAABB(ships[0], newTransforms[0])
    ships.zip(newTransforms).forEach {(it, newTransform) ->
        val b = getWorldAABB(it, newTransform)
        if (b.minX() < objectAABB.minX) { objectAABB.minX = b.minX() }
        if (b.maxX() > objectAABB.maxX) { objectAABB.maxX = b.maxX() }
        if (b.minY() < objectAABB.minY) { objectAABB.minY = b.minY() }
        if (b.maxY() > objectAABB.maxY) { objectAABB.maxY = b.maxY() }
        if (b.minZ() < objectAABB.minZ) { objectAABB.minZ = b.minZ() }
        if (b.maxZ() > objectAABB.maxZ) { objectAABB.maxZ = b.maxZ() }
    }

    val minPos = MVector3d(objectAABB.minX, objectAABB.minY, objectAABB.minZ)
    val maxPos = MVector3d(objectAABB.maxX, objectAABB.maxY, objectAABB.maxZ)

    val normalizedMaxObjectPos = (maxPos - minPos) / 2
    val objectCenterInWorld = normalizedMaxObjectPos + minPos

    val sinfo = ships.zip(newTransforms).map {(it, newTransform ) ->
        val chunkMin = MVector3d(
            it.chunkClaim.xStart * 16,
            0,
            it.chunkClaim.zStart * 16
        )

        val shipAABB = it.shipAABB?.let { shipAABB -> AABBi(
            shipAABB.minX() - it.chunkClaim.xStart * 16,
            shipAABB.minY(),
            shipAABB.minZ() - it.chunkClaim.zStart * 16,
            shipAABB.maxX() - it.chunkClaim.xStart * 16,
            shipAABB.maxY(),
            shipAABB.maxZ() - it.chunkClaim.zStart * 16
        ) } ?: AABBi(0, 0, 0, 0, 0, 0)

        ShipInfo(
            it.id,
            (MVector3d(newTransform.positionInWorld) - objectCenterInWorld).toJomlVector3d(),
            shipAABB,
            Vector3d(newTransform.positionInShip).sub(chunkMin.toJomlVector3d(), Vector3d()),
            MVector3d(newTransform.shipToWorldScaling).avg(),
            Quaterniond(newTransform.shipToWorldRotation)
        )
    }

    info = ShipSchematicInfo(
        normalizedMaxObjectPos.toJomlVector3d(),
        sinfo
    )
    return objectAABB
}