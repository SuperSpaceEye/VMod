package net.spaceeye.vmod.schematic.containers

import io.netty.buffer.ByteBuf
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.schematic.SchematicActionsQueue
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.api.containers.v1.SchemBlockData
import net.spaceeye.vmod.schematic.api.containers.v1.BlockItem
import net.spaceeye.vmod.schematic.api.containers.v1.BlockPaletteHashMapV1
import net.spaceeye.vmod.schematic.api.containers.v1.ShipInfo
import net.spaceeye.vmod.schematic.api.interfaces.IBlockStatePalette
import net.spaceeye.vmod.schematic.api.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.vmod.vsStuff.VSMasslessShipsProcessor
import net.spaceeye.vmod.schematic.api.interfaces.IShipSchematic
import net.spaceeye.vmod.schematic.api.interfaces.IShipSchematicInfo
import net.spaceeye.vmod.schematic.api.containers.v1.ShipSchematicInfo
import net.spaceeye.vmod.schematic.interfaces.SchemPlaceAtMakeFrom
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

class ShipSchematicV1(): IShipSchematic, IShipSchematicDataV1, SchemPlaceAtMakeFromV1Impl, SchemSerializeDataV1Impl {
    override val schematicVersion: Int = 1

    override var blockPalette: IBlockStatePalette = BlockPaletteHashMapV1()

    override var blockData = mutableMapOf<ShipId, SchemBlockData<BlockItem>>()
    override var flatTagData = mutableListOf<CompoundTag>()

    override var extraData = mutableListOf<Pair<String, Serializable>>()

    override var schemInfo: IShipSchematicInfo? = null

    override fun getInfo(): IShipSchematicInfo = schemInfo!!
}

interface SchemPlaceAtMakeFromV1Impl: SchemPlaceAtMakeFrom {
    override fun placeAt(level: ServerLevel, uuid: UUID, pos: Vector3d, rotation: Quaterniondc, postPlaceFn: (List<ServerShip>) -> Unit): Boolean {
        val newTransforms = mutableListOf<ShipTransform>()

        val ships = createShips(level, pos, rotation, newTransforms)
        ships.forEach { VSMasslessShipsProcessor.shipsToBeCreated.add(it.first.id) }

        if (!verifyBlockDataIsValid(ships, level)) {
            ships.forEach { level.shipObjectWorld.deleteShip(it.first) }
            return false
        }

        ShipSchematic.onPasteBeforeBlocksAreLoaded(level, ships, extraData)
        SchematicActionsQueue.queueShipsCreationEvent(level, uuid, ships, this) {
            ShipSchematic.onPasteAfterBlocksAreLoaded(level, ships, extraData)

            val ships = ships.map { it.first }
            ships.zip(newTransforms).forEach {
                    (it, transform) ->
                val toPos = MVector3d(transform.positionInWorld) + MVector3d(pos)
                level.shipObjectWorld.teleportShip(it, ShipTeleportDataImpl(
                    toPos.toJomlVector3d(),
                    transform.shipToWorldRotation,
                    JVector3d(),
                    newScale = MVector3d(it.transform.shipToWorldScaling).avg(),
                ))
            }
            ships.forEach { VSMasslessShipsProcessor.shipsToBeCreated.remove(it.id) }
            postPlaceFn(ships)
            SchematicActionsQueue.queueShipsUnfreezeEvent(uuid, ships, 10)
        }

        return true
    }

    private fun createShips(level: ServerLevel, pos: Vector3d, rotation: Quaterniondc, newTransforms: MutableList<ShipTransform>): List<Pair<ServerShip, Long>> {
        val shipData = schemInfo!!.shipsInfo
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

    private fun verifyBlockDataIsValid(
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

    override fun makeFrom(level: ServerLevel, uuid: UUID, originShip: ServerShip, postSaveFn: () -> Unit): Boolean {
        val traversed = traverseGetAllTouchingShips(level, originShip.id)

        // this is needed so that schem doesn't try copying phys entities (TODO)
        val ships = traversed.mapNotNull { level.shipObjectWorld.allShips.getById(it) }

        extraData = ShipSchematic.onCopy(level, ships).toMutableList()

        saveShipData(ships, originShip)
        // copy ship blocks separately
        SchematicActionsQueue.queueShipsSavingEvent(level, uuid, ships, this, true, postSaveFn)
        return true
    }

    private fun getWorldAABB(it: ServerShip, newTransform: ShipTransform): AABBd = it.worldAABB.transform(it.worldToShip, AABBd()).transform(newTransform.shipToWorld)

    // it will save ship data with origin ship unrotated
    private fun saveShipData(ships: List<ServerShip>, originShip: ServerShip): AABBd {
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

        schemInfo = ShipSchematicInfo(
            normalizedMaxObjectPos.toJomlVector3d(),
            sinfo
        )
        return objectAABB
    }
}

interface SchemSerializeDataV1Impl: IShipSchematic, IShipSchematicDataV1 {
    var schemInfo: IShipSchematicInfo?

    override fun serialize(): Serializable {
        val saveTag = CompoundTag()

        serializeShipData(saveTag)
        serializeExtraData(saveTag)
        serializeBlockPalette(saveTag)
        serializeGridDataInfo(saveTag)
        serializeExtraBlockData(saveTag)

        return CompoundSerializableWithTopVersion(saveTag, schematicVersion)
    }

    override fun deserialize(buf: ByteBuf): Boolean {
        val file = CompoundTagSerializable(CompoundTag())
        file.deserialize(FriendlyByteBuf(buf))

        val saveTag = file.tag!!

        deserializeShipData(saveTag)
        deserializeExtraData(saveTag)
        deserializeBlockPalette(saveTag)
        deserializeGridDataInfo(saveTag)
        deserializeExtraBlockData(saveTag)

        return true
    }

    private fun serializeShipData(tag: CompoundTag) {
        val shipDataTag = CompoundTag()

        shipDataTag.putVector3d("maxObjectPos", schemInfo!!.maxObjectPos)

        val shipsDataTag = ListTag()
        schemInfo!!.shipsInfo.forEach {
            val shipTag = CompoundTag()

            shipTag.putLong("id", it.id)
            shipTag.putVector3d("rptc", it.relPositionToCenter)

            shipTag.putInt("sb_mx", it.shipAABB.minX())
            shipTag.putInt("sb_my", it.shipAABB.minY())
            shipTag.putInt("sb_mz", it.shipAABB.minZ())
            shipTag.putInt("sb_Mx", it.shipAABB.maxX())
            shipTag.putInt("sb_My", it.shipAABB.maxY())
            shipTag.putInt("sb_Mz", it.shipAABB.maxZ())

            shipTag.putVector3d("pis", it.positionInShip)
            shipTag.putDouble("sc", it.shipScale)
            shipTag.putQuaterniond("rot", it.rotation)

            shipsDataTag.add(shipTag)
        }

        shipDataTag.put("data", shipsDataTag)
        tag.put("shipData", shipDataTag)
    }

    private fun serializeExtraData(tag: CompoundTag) {
        val extraDataTag = CompoundTag()

        extraData.forEach { (name, file) -> extraDataTag.putByteArray(name, file.serialize().array()) }

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

        flatTagData.forEach { extraBlockData.add(it) }

        tag.put("extraBlockData", extraBlockData)
    }

    private fun serializeGridDataInfo(tag: CompoundTag) {
        val gridDataTag = CompoundTag()

        blockData.forEach {
                (shipId, data) ->
            val dataTag = ListTag()

            data.forEach {x, y, z, it ->
                val item = CompoundTag()

                item.putInt("x", x)
                item.putInt("y", y)
                item.putInt("z", z)
                item.putInt("pid", it.paletteId)
                item.putInt("edi", it.extraDataId)

                dataTag.add(item)
            }

            gridDataTag.put(shipId.toString(), dataTag)
        }

        tag.put("gridData", gridDataTag)
    }




    private fun deserializeShipData(tag: CompoundTag) {
        val shipDataTag = tag.get("shipData") as CompoundTag

        val maxObjectPos = shipDataTag.getVector3d("maxObjectPos")!!

        val shipsDataTag = shipDataTag.get("data") as ListTag

        schemInfo = ShipSchematicInfo( maxObjectPos,
            shipsDataTag.map {shipTag ->
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

            Pair(name, RawBytesSerializable(byteArray))
        }.toMutableList()
    }

    private fun deserializeBlockPalette(tag: CompoundTag) {
        val paletteTag = tag.get("blockPalette") as ListTag

        val newPalette = paletteTag.mapIndexed { i, it ->
            val state = NbtUtils.readBlockState(it as CompoundTag)
            if (state.isAir) { ELOG("State under id $i is air. $it") }
            Pair(i, state)
        }

        blockPalette.setPalette(newPalette)
    }

    private fun deserializeExtraBlockData(tag: CompoundTag) {
        val extraBlockData = tag.get("extraBlockData") as ListTag

        flatTagData = extraBlockData.map { it as CompoundTag }.toMutableList()
    }

    private fun deserializeGridDataInfo(tag: CompoundTag) {
        val gridDataTag = tag.get("gridData") as CompoundTag

        for (k in gridDataTag.allKeys) {
            val dataTag = gridDataTag.get(k) as ListTag
            val data = blockData.getOrPut(k.toLong()) { SchemBlockData() }

            dataTag.forEach {blockTag ->
                blockTag as CompoundTag

                data.add(
                    blockTag.getInt("x"),
                    blockTag.getInt("y"),
                    blockTag.getInt("z"),
                    BlockItem(
                        blockTag.getInt("pid"),
                        blockTag.getInt("edi")
                    )
                )
            }
        }
    }
}