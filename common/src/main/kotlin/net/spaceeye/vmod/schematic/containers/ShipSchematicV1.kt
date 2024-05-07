package net.spaceeye.vmod.schematic.containers

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtUtils
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.constraintsManaging.VSConstraintsKeeper
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IFile
import net.spaceeye.vmod.schematic.icontainers.IShipInfo
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IShipSchematicInfo
import net.spaceeye.vmod.transformProviders.FixedPositionTransformProvider
import net.spaceeye.vmod.utils.*
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
import java.io.IOException

typealias MVector3d = net.spaceeye.vmod.utils.Vector3d

data class BlockData(var pos: BlockPos, var paletteId: Int, var extraDataId: Int)

class ShipSchematicInfo(override val worldBounds: AABBdc,
                        override var shipInfo: List<IShipInfo>) : IShipSchematicInfo

class ShipInfo(
        override val id: Long,
        override val relPositionToCenter: Vector3d,
        override val shipBounds: AABBic,
        override val worldBounds: AABBdc,
        override val posInShip: Vector3d,
        override val shipScale: Double,
        override val rotation: Quaterniondc) : IShipInfo

class CompoundTagIFile(var tag: CompoundTag): IFile {
    override fun toBytes(): ByteBuf {
        val buffer = ByteBufOutputStream(Unpooled.buffer())
        NbtIo.writeCompressed(tag, buffer)
        return buffer.buffer()
    }

    override fun fromBytes(buffer: ByteBuf): Boolean {
        val _buffer = ByteBufInputStream(buffer)
        try {
            tag = NbtIo.readCompressed(_buffer)
        } catch (e: IOException) {
            return false
        }

        return true
    }
}

class CompoundTagIFileWithTopVersion(var tag: CompoundTag, val version: Int): IFile {
    override fun toBytes(): ByteBuf {
        val buffer = ByteBufOutputStream(Unpooled.buffer())
        buffer.writeInt(version)
        NbtIo.writeCompressed(tag, buffer)
        return buffer.buffer()
    }

    // version was already written before calling fromBytes
    override fun fromBytes(buffer: ByteBuf): Boolean {
        val _buffer = ByteBufInputStream(buffer)
        try {
            tag = NbtIo.readCompressed(_buffer)
        } catch (e: IOException) {
            return false
        }

        return true
    }
}

class RawBytesIFile(val bytes: ByteArray): IFile {
    override fun toBytes(): ByteBuf {
        return Unpooled.wrappedBuffer(bytes)
    }

    override fun fromBytes(buffer: ByteBuf): Boolean {
        throw AssertionError("Not Implemented. Not going to be implemented.")
    }
}

class ShipSchematicV1(): IShipSchematic {
    override val schematicVersion: Int = 1

    val blockPalette = BlockPaletteHashMapV1()

    val blockData = mutableMapOf<ShipId, MutableList<BlockData>>()
    var flatExtraData = mutableListOf<CompoundTag>()

    var extraData = listOf<Pair<String, IFile>>()

    lateinit var schemInfo: IShipSchematicInfo

    override fun getInfo(): IShipSchematicInfo = schemInfo

    override fun placeAt(level: ServerLevel, pos: Vector3d, rotation: Quaterniondc): Boolean {
        val shipData = schemInfo.shipInfo

        val ships = shipData.map {
            val toPos = MVector3d(it.relPositionToCenter) + MVector3d(pos)

            val newShip = level.shipObjectWorld.createNewShipAtBlock(Vector3i(), false, it.shipScale, level.dimensionId)
            newShip.isStatic = true

            level.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(
                toPos.toJomlVector3d(),
                it.rotation,
                newScale = it.shipScale
            ))

            val chunkMin = Vector3d(
                newShip.chunkClaim.xStart * 16,
                -64,
                newShip.chunkClaim.zStart * 16
            )

            val posInShip = it.posInShip.add(chunkMin.toJomlVector3d(), Vector3d())

            newShip.transformProvider = FixedPositionTransformProvider(toPos.toJomlVector3d(), posInShip)
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
                    tag.putInt("x", pos.x)
                    tag.putInt("y", pos.y)
                    tag.putInt("z", pos.z)
                    level.getChunkAt(pos).getBlockEntity(pos)!!.load(tag)
                }
            }
        }

        ShipSchematic.onPaste(ships, extraData)

        ships.zip(shipData).forEach {
            (it, info) ->
            it.first.transformProvider = null
            val toPos = MVector3d(info.relPositionToCenter) + MVector3d(pos)
            level.shipObjectWorld.teleportShip(it.first, ShipTeleportDataImpl(
                    toPos.toJomlVector3d(),
                    it.first.transform.shipToWorldRotation,
                    newScale = MVector3d(it.first.transform.shipToWorldScaling).avg()
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

            val be = level.getChunkAt(pos).getBlockEntity(pos)
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
            val chunkMin = MVector3d(
                    it.chunkClaim.xStart * 16,
                    -64,
                    it.chunkClaim.zStart * 16
            )

            ShipInfo(
                    it.id,
                    (MVector3d(it.transform.positionInWorld) - objectLogicalCenter).toJomlVector3d(),
                    AABBi(it.shipAABB!!),
                    AABBd(it.worldAABB),
                    Vector3d(it.transform.positionInShip).sub(chunkMin.toJomlVector3d(), Vector3d()),
                    MVector3d(it.transform.shipToWorldScaling).avg(),
                    Quaterniond(it.transform.shipToWorldRotation))
        }

        schemInfo = ShipSchematicInfo(
                objectAABB,
                sinfo
        )
    }

    // TODO redo this
    // reworked steps:
    // 1. traverse + gather MConstraints info + gather server only info
    // 2. send this info to client
    // 3. client gathers block info by itself
    override fun makeFrom(originShip: ServerShip): Boolean {
        val traversed = VSConstraintsKeeper.traverseGetConnectedShips(originShip.id)
        val level = ServerLevelHolder.overworldServerLevel!!

        val ships = traversed.traversedShipIds.mapNotNull { level.shipObjectWorld.allShips.getById(it) }

        extraData = ShipSchematic.onCopy(ships)

        saveMetadata(ships)
        ships.forEach { ship -> saveShipBlocks(level, ship) }

        return true
    }


    private fun serializeMetadata(tag: CompoundTag) {
        val metadataTag = CompoundTag()

        tag.put("metadata", metadataTag)
    }

    private fun serializeShipData(tag: CompoundTag) {
        val shipDataTag = ListTag()

        schemInfo.shipInfo.forEach {
            val shipTag = CompoundTag()

            shipTag.putLong("id", it.id)
            shipTag.putVector3d("rptc", it.relPositionToCenter)

            shipTag.putInt("sb_mx", it.shipBounds.minX())
            shipTag.putInt("sb_my", it.shipBounds.minY())
            shipTag.putInt("sb_mz", it.shipBounds.minZ())
            shipTag.putInt("sb_Mx", it.shipBounds.maxX())
            shipTag.putInt("sb_My", it.shipBounds.maxY())
            shipTag.putInt("sb_Mz", it.shipBounds.maxZ())


            shipTag.putDouble("wb_mx", it.worldBounds.minX())
            shipTag.putDouble("wb_my", it.worldBounds.minY())
            shipTag.putDouble("wb_mz", it.worldBounds.minZ())
            shipTag.putDouble("wb_Mx", it.worldBounds.maxX())
            shipTag.putDouble("wb_My", it.worldBounds.maxY())
            shipTag.putDouble("wb_Mz", it.worldBounds.maxZ())

            shipTag.putVector3d("pis", it.posInShip)
            shipTag.putDouble("sc", it.shipScale)
            shipTag.putQuaterniond("rot", it.rotation)

            shipDataTag.add(shipTag)
        }

        tag.put("shipData", shipDataTag)
    }

    private fun serializeExtraData(tag: CompoundTag) {
        val extraDataTag = CompoundTag()

        extraData.forEach { (name, file) -> extraDataTag.putByteArray(name, file.toBytes().array()) }

        tag.put("extraData", extraDataTag)
    }

    private fun serializeBlockPalette(tag: CompoundTag) {
        val paletteTag = ListTag()

        for (i in 0 until blockPalette.getPaletteSize()) {
            val state = blockPalette.fromId(i)
            paletteTag.add(NbtUtils.writeBlockState(state ?: run { ELOG("BLOCK PALETTE RETURNED NULL FOR ID ${i}. HOW?") ; Blocks.AIR.defaultBlockState() }))
        }

        tag.put("blockPalette", paletteTag)
    }

    private fun serializeExtraBlockData(tag: CompoundTag) {
        val extraBlockData = ListTag()

        flatExtraData.forEach { extraBlockData.add(it) }

        tag.put("extraBlockData", extraBlockData)
    }

    private fun serializeGridDataInfo(tag: CompoundTag) {
        val gridDataTag = CompoundTag()

        blockData.forEach {
            (shipId, data) ->
            val dataTag = ListTag()

            data.forEach {
                val item = CompoundTag()

                item.putInt("x", it.pos.x)
                item.putInt("y", it.pos.y)
                item.putInt("z", it.pos.z)
                item.putInt("pid", it.paletteId)
                item.putInt("edi", it.extraDataId)

                dataTag.add(item)
            }

            gridDataTag.put(shipId.toString(), dataTag)
        }

        tag.put("gridData", gridDataTag)
    }

    override fun saveToFile(): IFile {
        val saveTag = CompoundTag()

        serializeMetadata(saveTag)
        serializeShipData(saveTag)
        serializeExtraData(saveTag)
        serializeBlockPalette(saveTag)
        serializeGridDataInfo(saveTag)
        serializeExtraBlockData(saveTag)

        return CompoundTagIFileWithTopVersion(saveTag, schematicVersion)
    }

    private fun deserializeMetadata(tag: CompoundTag) {
        val metadataTag = tag.get("metadata") as CompoundTag
    }

    private fun deserializeShipData(tag: CompoundTag) {
        val shipDataTag = tag.get("shipData") as ListTag

        schemInfo = ShipSchematicInfo( AABBd(),
                shipDataTag.map {shipTag ->
            shipTag as CompoundTag

            ShipInfo(
                shipTag.getLong("id"),
                shipTag.getVector3d("rptc")!!,
                AABBi(
                    shipTag.getInt("sb_mx"),
                    shipTag.getInt("sb_my"),
                    shipTag.getInt("sb_mz"),
                    shipTag.getInt("sb_Mx"),
                    shipTag.getInt("sb_My"),
                    shipTag.getInt("sb_Mz"),
                ),
                AABBd(
                    shipTag.getDouble("wb_mx"),
                    shipTag.getDouble("wb_my"),
                    shipTag.getDouble("wb_mz"),
                    shipTag.getDouble("wb_Mx"),
                    shipTag.getDouble("wb_My"),
                    shipTag.getDouble("wb_Mz"),
                ),
                shipTag.getVector3d("pis")!!,
                shipTag.getDouble("sc"),
                shipTag.getQuaterniond("rot")!!
            )
        }
        )
    }

    private fun deserializeExtraData(tag: CompoundTag) {
        val extraDataTag = tag.get("extraData") as CompoundTag

        extraData = extraDataTag.allKeys.map { name ->
            val byteArray = extraDataTag.getByteArray(name)

            Pair(name, RawBytesIFile(byteArray))
        }
    }

    private fun deserializeBlockPalette(tag: CompoundTag) {
        val paletteTag = tag.get("blockPalette") as ListTag

        val newPalette = paletteTag.mapIndexed { i, it ->
            val state = NbtUtils.readBlockState(it as CompoundTag)
            Pair(i, state)
        }

        blockPalette.setPalette(newPalette)
    }

    private fun deserializeExtraBlockData(tag: CompoundTag) {
        val extraBlockData = tag.get("extraBlockData") as ListTag

        flatExtraData = extraBlockData.map { it as CompoundTag }.toMutableList()
    }

    private fun deserializeGridDataInfo(tag: CompoundTag) {
        val gridDataTag = tag.get("gridData") as CompoundTag

        for (k in gridDataTag.allKeys) {
            val dataTag = gridDataTag.get(k) as ListTag
            val data = blockData.getOrPut(k.toLong()) { mutableListOf() }

            dataTag.forEach {blockTag ->
                blockTag as CompoundTag

                data.add(BlockData(
                    BlockPos(
                        blockTag.getInt("x"),
                        blockTag.getInt("y"),
                        blockTag.getInt("z")
                    ),
                    blockTag.getInt("pid"),
                    blockTag.getInt("edi")
                ))
            }
        }
    }

    override fun loadFromByteBuffer(buf: ByteBuf): Boolean {
        val file = CompoundTagIFile(CompoundTag())
        file.fromBytes(buf)

        val saveTag = file.tag

        deserializeMetadata(saveTag)
        deserializeShipData(saveTag)
        deserializeExtraData(saveTag)
        deserializeBlockPalette(saveTag)
        deserializeGridDataInfo(saveTag)
        deserializeExtraBlockData(saveTag)

        return true
    }
}