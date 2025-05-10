package net.spaceeye.vmod.schematic

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.containers.v1.*
import net.spaceeye.valkyrien_ship_schematics.interfaces.IBlockStatePalette
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematicInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISerializable
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.SchemSerializeDataV1Impl
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.toolgun.SELOG
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.rotateAroundCenter
import net.spaceeye.vmod.utils.vs.traverseGetAllTouchingShips
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3i
import org.joml.primitives.AABBd
import org.joml.primitives.AABBi
import net.spaceeye.vmod.compat.vsBackwardsCompat.*
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.utils.vs.transformDirectionShipToWorld
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl
import org.valkyrienskies.core.util.toAABBd
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import kotlin.math.roundToInt

typealias MVector3d = net.spaceeye.vmod.utils.Vector3d

class VModShipSchematicV2(): IShipSchematic, IShipSchematicDataV1, SchemSerializeDataV1Impl {
    override var blockPalette: IBlockStatePalette = BlockPaletteHashMapV1()

    override var blockData = mutableMapOf<ShipId, ChunkyBlockData<BlockItem>>()
    override var entityData: MutableMap<ShipId, List<EntityItem>> = mutableMapOf()
    override var flatTagData: MutableList<CompoundTag> = mutableListOf()

    override var extraData: MutableList<Pair<String, ISerializable>> = mutableListOf()

    override var info: IShipSchematicInfo? = null
}

@OptIn(VsBeta::class)
fun IShipSchematicDataV1.placeAt(level: ServerLevel, player: ServerPlayer?, uuid: UUID, pos: Vector3d, rotation: Quaterniondc, postPlaceFn: (List<ServerShip>) -> Unit): Boolean {
    val newTransforms = mutableListOf<BodyTransform>()

    val shipInitializers = (this as IShipSchematic).createShipConstructors(level, pos, rotation, newTransforms)

    if (!verifyBlockDataIsValid(shipInitializers.map { it.second }, player)) { return false }

    SchematicActionsQueue.queueShipsCreationEvent(level, player, uuid, shipInitializers, this) { ships, centerPositions, entityCreationFn ->
        val createdShips = ships.map { it.first }
        createdShips.zip(newTransforms).forEach { (it, transform) ->
            val b = it.shipAABB!!
            var offset = MVector3d(it.transform.positionInModel) - MVector3d(
                (b.maxX() - b.minX()) / 2.0 + b.minX(),
                (b.maxY() - b.minY()) / 2.0 + b.minY(),
                (b.maxZ() - b.minZ()) / 2.0 + b.minZ(),
            )
            offset = transformDirectionShipToWorld(it, offset)
            val toPos = MVector3d(transform.position) + MVector3d(pos) + offset
            level.shipObjectWorld.teleportShip(it, ShipTeleportDataImpl(
                toPos.toJomlVector3d(),
                transform.rotation,
                JVector3d(), JVector3d(), level.dimensionId, transform.scaling.x()
            ))
        }

        entityCreationFn()
        ShipSchematic.onPasteAfterBlocksAreLoaded(level, ships, centerPositions, extraData.toMap())
        postPlaceFn(createdShips)
        SchematicActionsQueue.queueShipsUnfreezeEvent(uuid, createdShips, 10)
    }

    return true
}

private fun IShipSchematic.createShipConstructors(level: ServerLevel, pos: Vector3d, rotation: Quaterniondc, newTransforms: MutableList<BodyTransform>): List<Pair<() -> ServerShip, Long>> {
    val shipData = info!!.shipsInfo
    // during schem creation ship positions are normalized so that the center is at 0 0 0
    val center = ShipTransformImpl.create(JVector3d(), JVector3d(), Quaterniond(), JVector3d(1.0, 1.0, 1.0))

    return shipData.map { Pair({
        val thisTransform = ShipTransformImpl.create(
            it.relPositionToCenter,
            JVector3d(),
            it.rotation,
            JVector3d(it.shipScale, it.shipScale, it.shipScale)
        )
        val temp = rotateAroundCenter(center, thisTransform, rotation)

        // reusing posInShip as it's useless
        val newTransform = ShipTransformImpl(temp.position, it.previousCenterPosition, temp.rotation, JVector3d(it.shipScale, it.shipScale, it.shipScale))
        newTransforms.add(newTransform)

        //this is pointless and doesnt actually work
        var offset = MVector3d(it.previousCOMPosition) - MVector3d(it.previousCenterPosition)
        offset = transformDirectionShipToWorld(newTransform, offset)
        val toPos = MVector3d(newTransform.position) + MVector3d(pos) + offset

        val newShip = level.shipObjectWorld.createNewShipAtBlock(Vector3i(), false, it.shipScale, level.dimensionId)
        newShip.isStatic = true

        //TODO idk if i can calculate final position correctly, so maybe just teleport it to idk 100000000 100000000 100000000 while it's being created?
        level.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(
            toPos.toJomlVector3d(),
            newTransform.rotation,
            newScale = it.shipScale
        ))
        newShip
        }, it.id) }
}

fun IShipSchematicDataV1.verifyBlockDataIsValid(
    ids: List<Long>,
    player: ServerPlayer?,
): Boolean {
    ids.forEach { id ->
        blockData[id] ?: run {
            val str = "Ship ID exists not no block data was saved. Not placing a schematic."
            player?.also { SELOG(str, player, str, false) } ?: run { ELOG(str) }
            return false
        }
    }
    return true
}

fun IShipSchematicDataV1.makeFrom(level: ServerLevel, player: ServerPlayer?, uuid: UUID, originShip: ServerShip, postSaveFn: () -> Unit): Boolean {
    val traversed = traverseGetAllTouchingShips(level, originShip.id)

    // this is needed so that schem doesn't try copying phys entities
    val ships = traversed.mapNotNull { level.shipObjectWorld.allShips.getById(it) }

    extraData = ShipSchematic.onCopy(level, ships,
        ships.associate {
            val b = it.shipAABB!!
            Pair(it.id, JVector3d(
                (b.maxX() - b.minX()) / 2.0 + b.minX(),
                (b.maxY() - b.minY()) / 2.0 + b.minY(),
                (b.maxZ() - b.minZ()) / 2.0 + b.minZ(),
            ))
        }).toMutableList()

    (this as IShipSchematic).saveShipData(ships, originShip)
    // copy ship blocks separately
    SchematicActionsQueue.queueShipsSavingEvent(level, player, uuid, ships, this, true, postSaveFn)
    return true
}


private fun getWorldAABB(it: ServerShip, newTransform: BodyTransform): AABBd = it.shipAABB?.toAABBd(AABBd())?.transform(newTransform.toWorld) ?: AABBd(it.worldAABB)

fun IShipSchematic.saveShipData(ships: List<ServerShip>, originShip: ServerShip): AABBd {
    //TODO i kinda don't want to do this but idk what i want to do with this
//    val invRotation = originShip.transform.shipToWorldRotation.invert(Quaterniond())
    val newTransforms = ships.map {
//        rotateAroundCenter(originShip.transform, it.transform, invRotation)
        it.transform
    }

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

    val normalizedMaxObjectPos = (maxPos - minPos) / 2.0
    val objectCenterInWorld = normalizedMaxObjectPos + minPos

    val sinfo = ships.zip(newTransforms).map {(it, newTransform) ->
        val b = it.shipAABB!!
        val cX = (b.maxX() - b.minX()) / 2.0 + b.minX()
        val cY = (b.maxY() - b.minY()) / 2.0 + b.minY()
        val cZ = (b.maxZ() - b.minZ()) / 2.0 + b.minZ()

        val chunkCenter = MVector3d(cX, cY, cZ)

        val shipAABB = it.shipAABB
            ?.translate(-chunkCenter.x.roundToInt(), -chunkCenter.y.roundToInt(), -chunkCenter.z.roundToInt(), AABBi())
            ?: AABBi(0, 0, 0, 0, 0, 0)

        ShipInfo(
            it.id,
            (posShipToWorld(it, chunkCenter) - objectCenterInWorld).toJomlVector3d(),
            shipAABB,
            chunkCenter.toJomlVector3d(),
            it.transform.positionInModel.get(Vector3d()),
            MVector3d(newTransform.scaling).avg(),
            Quaterniond(newTransform.rotation)
        )
    }

    info = ShipSchematicInfo(
        normalizedMaxObjectPos.toJomlVector3d(),
        sinfo
    )
    return objectAABB
}