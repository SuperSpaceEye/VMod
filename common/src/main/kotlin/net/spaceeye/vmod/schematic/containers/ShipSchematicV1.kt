package net.spaceeye.vmod.schematic.containers

import com.ibm.icu.text.DecimalFormat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.WLOG
import net.spaceeye.vmod.constraintsManaging.VSConstraintsKeeper
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IFile
import net.spaceeye.vmod.schematic.icontainers.IShipInfo
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IShipSchematicInfo
import net.spaceeye.vmod.utils.ServerLevelHolder
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3i
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld

typealias MVector3d = net.spaceeye.vmod.utils.Vector3d

data class BlockData(var pos: BlockPos, var paletteId: Int, var extraDataId: Int)

class ShipSchematicInfo(override val worldBounds: AABBdc,
                        override val shipInfo: List<IShipInfo>) : IShipSchematicInfo

class ShipInfo(
        override val id: Long,
        override val relPositionToCenter: Vector3d,
        override val shipBounds: AABBic,
        override val worldBounds: AABBdc,
        override val posInShip: Vector3d,
        override val shipScale: Double,
        override val rotation: Quaterniondc) : IShipInfo

class ShipSchematicV1(): IShipSchematic {
    override val schematicVersion: Int = 1

    val blockPalette = BlockPaletteHashMapV1()

    val blockData = mutableMapOf<ShipId, MutableList<BlockData>>()
    val flatExtraData = mutableListOf<CompoundTag>()

    var extraData = listOf<Pair<String, IFile>>()

    lateinit var schemInfo: IShipSchematicInfo

    override fun getInfo(): IShipSchematicInfo = schemInfo

    override fun placeAt(level: ServerLevel, pos: Vector3d, rotation: Quaterniondc): Boolean {
        val shipData = schemInfo.shipInfo

        val ships = shipData.map {
            val df = DecimalFormat("###.###")
            WLOG("${df.format(it.relPositionToCenter.x)} ${df.format(it.relPositionToCenter.y)} ${df.format(it.relPositionToCenter.z)}")
            val toPos = MVector3d(it.relPositionToCenter) + MVector3d(pos)

            val newShip = level.shipObjectWorld.createNewShipAtBlock(Vector3i(), false, it.shipScale, level.dimensionId)
            newShip.isStatic = true

            level.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(
                    toPos.toJomlVector3d(),
                    it.rotation,
                    newScale = it.shipScale
            ))
            Pair(newShip, it.id)
        }

        ships.forEach { (ship, id) ->
            blockData[id] ?: run {
                ships.forEach {(ship, id) -> level.shipObjectWorld.deleteShip(ship) }
                ELOG("SHIP ID EXISTS BUT NO BLOCK DATA WAS SAVED. NOT PLACING A SCHEMATIC.")
                return false
            }
        }

        for ((ship, id) in ships) {
            val blockData = blockData[id]!!
            val offset = MVector3d(
                    ship.chunkClaim.xStart * 16,
                    -64,
                    ship.chunkClaim.zStart * 16
            )

            blockData.forEach {
                val pos = BlockPos(it.pos.x + offset.x, it.pos.y + offset.y, it.pos.z + offset.z)
                val state = blockPalette.fromId(it.paletteId) ?: run {
                    ELOG("STATE UNDER ID ${it.paletteId} IS NULL. TRYING TO CONTINUE PASTING SCHEMATIC.")
                    return@forEach
                }

                level.getChunkAt(pos).setBlockState(pos, state, false)
                if (it.extraDataId != -1) {
                    val tag = flatExtraData[it.extraDataId]
                    level.getBlockEntity(pos)!!.load(tag)
                }
            }
        }

        ships.zip(shipData).forEach {
            (it, info) ->
            val toPos = MVector3d(info.relPositionToCenter) + MVector3d(pos)
            level.shipObjectWorld.teleportShip(it.first, ShipTeleportDataImpl(
                    toPos.toJomlVector3d()
            ))
        }

        return true
    }

    private fun saveShipBlocks(level: ServerLevel, originShip: ServerShip) {
        val data = blockData.getOrPut(originShip.id) { mutableListOf() }

        val boundsAABB = originShip.shipAABB!!
        val chunkMin = BlockPos(
                originShip.chunkClaim.xStart * 16,
                -64,
                originShip.chunkClaim.zStart * 16
        )

        for (oy in boundsAABB.minY() - 1 until boundsAABB.maxY() + 1) {
        for (ox in boundsAABB.minX() - 1 until boundsAABB.maxX() + 1) {
        for (oz in boundsAABB.minZ() - 1 until boundsAABB.maxZ() + 1) {
            val pos = BlockPos(ox, oy, oz)
            val state = level.getBlockState(pos)
            if (state.isAir) {continue}

            val be = level.getBlockEntity(pos)
            val fed = if (be == null) {-1} else {
                flatExtraData.add(be.saveWithFullMetadata())
                flatExtraData.size - 1
            }

            val id = blockPalette.toId(state)
             data.add(BlockData(BlockPos(
                    pos.x - chunkMin.x,
                    pos.y - chunkMin.y,
                    pos.z - chunkMin.z
            ), id, fed))
        }}}
    }

    private fun saveMetadata(ships: List<ServerShip>) {
        val objectAABB = AABBd(ships[0].worldAABB)
        ships.forEach {
            val b = it.worldAABB
            if (b.minX() < objectAABB.minX) { objectAABB.minX = b.minX() }
            if (b.maxX() > objectAABB.maxX) { objectAABB.maxX = b.maxX() }
            if (b.minY() < objectAABB.minY) { objectAABB.minY = b.minY() }
            if (b.maxY() > objectAABB.maxY) { objectAABB.maxY = b.maxY() }
            if (b.minZ() < objectAABB.minZ) { objectAABB.minZ = b.minZ() }
            if (b.maxZ() > objectAABB.maxZ) { objectAABB.maxZ = b.maxZ() }
        }

        val objectLogicalCenter = MVector3d(
                (objectAABB.maxX() - objectAABB.minX()) / 2 + objectAABB.minX(),
                (objectAABB.maxY() - objectAABB.minY()) / 2 + objectAABB.minY(),
                (objectAABB.maxZ() - objectAABB.minZ()) / 2 + objectAABB.minZ()
        )

        val sinfo = ships.map {
            ShipInfo(
                    it.id,
                    (MVector3d(it.transform.positionInWorld) - objectLogicalCenter).toJomlVector3d(),
                    AABBi(it.shipAABB!!),
                    AABBd(it.worldAABB),
                    Vector3d(it.transform.positionInShip),
                    MVector3d(it.transform.shipToWorldScaling).avg(),
                    Quaterniond(it.transform.shipToWorldRotation))
        }

        schemInfo = ShipSchematicInfo(
                objectAABB,
                sinfo
        )
    }

    override fun makeFrom(originShip: ServerShip): Boolean {
        val traversed = VSConstraintsKeeper.traverseGetConnectedShips(originShip.id)
        val level = ServerLevelHolder.overworldServerLevel!!

        val ships = traversed.traversedShipIds.mapNotNull { level.shipObjectWorld.allShips.getById(it) }

        extraData = ShipSchematic.onCopy(ships)

        saveMetadata(ships)
        ships.forEach { ship -> saveShipBlocks(level, ship) }

        return true
    }

    override fun saveToFile(): IFile {
        TODO("Not yet implemented")
    }

    override fun loadFromFile(file: IFile): Boolean {
        TODO("Not yet implemented")
    }
}