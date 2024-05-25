package net.spaceeye.vmod.schematic.containers

import io.netty.buffer.ByteBuf
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.schematic.SchematicActionsQueue
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IFile
import net.spaceeye.vmod.schematic.icontainers.IShipSchematic
import net.spaceeye.vmod.schematic.icontainers.IShipSchematicInfo
import net.spaceeye.vmod.transformProviders.FixedPositionTransformProvider
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
import org.valkyrienskies.core.util.toAABBd
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID

typealias MVector3d = net.spaceeye.vmod.utils.Vector3d

class ShipSchematicV1(): IShipSchematic {
    override val schematicVersion: Int = 1

    val blockPalette = BlockPaletteHashMapV1()

    val blockData = mutableMapOf<ShipId, SchemBlockData<BlockItem>>()
    var flatExtraData = mutableListOf<CompoundTag>()

//    var worldEntityData = mutableMapOf<UUID, CompoundTag>()
//    var shipyardEntityData = mutableMapOf<ShipId, MutableMap<UUID, CompoundTag>>()

    var extraData = listOf<Pair<String, IFile>>()

    lateinit var schemInfo: IShipSchematicInfo

    override fun getInfo(): IShipSchematicInfo = schemInfo

    override fun placeAt(level: ServerLevel, uuid: UUID, pos: Vector3d, rotation: Quaterniondc): Boolean {
        val shipData = schemInfo.shipInfo

        val center = ShipTransformImpl(JVector3d(), JVector3d(), Quaterniond(), JVector3d(1.0, 1.0, 1.0))

        val newTransforms = mutableListOf<ShipTransform>()
        val ships = shipData.map {
            val thisTransform = ShipTransformImpl(
                it.relPositionToCenter,
                it.posInShip,
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

            val chunkMin = Vector3d(
                newShip.chunkClaim.xStart * 16,
                0,
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

        ShipSchematic.onPasteBeforeBlocksAreLoaded(level, ships, extraData)

        SchematicActionsQueue.queueShipsCreationEvent(level, uuid, ships, this) {
            ShipSchematic.onPasteAfterBlocksAreLoaded(level, ships, extraData)

            ships.zip(newTransforms).forEach {
                (it, transform) ->
                it.first.transformProvider = null
                val toPos = MVector3d(transform.positionInWorld) + MVector3d(pos)
                level.shipObjectWorld.teleportShip(it.first, ShipTeleportDataImpl(
                        toPos.toJomlVector3d(),
                        transform.shipToWorldRotation,
                        newScale = MVector3d(it.first.transform.shipToWorldScaling).avg()
                ))
                it.first.isStatic = false
            }
//
//            worldEntityData.forEach { (k, tag) ->
//                val entityOpt = EntityType.create(tag, level) ?: return@forEach
//                if (entityOpt.isEmpty) {return@forEach}
//                val entity = entityOpt.get()
//                val relPos = MVector3d(tag.getVector3d("VMOD_SCHEMATIC_REL_POS_DATA")!!)
//                entity.setPos((relPos.sadd(pos.x, pos.y, pos.z)).toMCVec3())
//                entity.uuid = UUID.randomUUID()
//                level.addFreshEntity(entity)
//            }
        }

        return true
    }

    // it will save ship data with origin ship unrotated
    private fun saveShipData(ships: List<ServerShip>, originShip: ServerShip): AABBd {
        val getWorldAABB = {it: ServerShip, newTransform: ShipTransform -> it.shipAABB!!.toAABBd(AABBd()).transform(newTransform.shipToWorld) }

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
        val objectCenter = normalizedMaxObjectPos + minPos

        val sinfo = ships.zip(newTransforms).map {(it, newTransform ) ->
            val chunkMin = MVector3d(
                    it.chunkClaim.xStart * 16,
                    0,
                    it.chunkClaim.zStart * 16
            )

            val shipAABB = AABBi(
                it.shipAABB!!.minX() - it.chunkClaim.xStart * 16,
                it.shipAABB!!.minY(),
                it.shipAABB!!.minZ() - it.chunkClaim.zStart * 16,
                it.shipAABB!!.maxX() - it.chunkClaim.xStart * 16,
                it.shipAABB!!.maxY(),
                it.shipAABB!!.maxZ() - it.chunkClaim.zStart * 16
            )

            ShipInfo(
                    it.id,
                    (MVector3d(newTransform.positionInWorld) - objectCenter).toJomlVector3d(),
                    shipAABB,
                    Vector3d(newTransform.positionInShip).sub(chunkMin.toJomlVector3d(), Vector3d()),
                    MVector3d(newTransform.shipToWorldScaling).avg(),
                    Quaterniond(newTransform.shipToWorldRotation))
        }

        schemInfo = ShipSchematicInfo(
            normalizedMaxObjectPos.toJomlVector3d(),
            sinfo
        )
        return objectAABB
    }

//    fun saveEntityData(level: ServerLevel, ships: List<ServerShip>, objectAABB: AABBd) {
//        val minPos = MVector3d(objectAABB.minX, objectAABB.minY, objectAABB.minZ)
//        val maxPos = MVector3d(objectAABB.maxX, objectAABB.maxY, objectAABB.maxZ)
//
//        val normalizedMaxObjectPos = (maxPos - minPos) / 2
//        val objectCenter = normalizedMaxObjectPos + minPos
//
//        ships
//            .map { level.getEntities(null, it.worldAABB.toMinecraft()) }
//            .reduce { acc, entities -> acc.addAll(entities); acc }
//            .forEach {
//                if (worldEntityData.containsKey(it.uuid)) {return@forEach}
//                try {
//                    val tag = CompoundTag()
//                    it.save(tag)
//                    tag.putVector3d("VMOD_SCHEMATIC_REL_POS_DATA", (MVector3d(it.position()) - objectCenter).toJomlVector3d())
//                    worldEntityData[it.uuid] = tag
//                } catch (_: Exception) {}
//            }
//    }

    override fun makeFrom(level: ServerLevel, uuid: UUID, originShip: ServerShip, postSaveFn: () -> Unit): Boolean {
        val traversed = traverseGetAllTouchingShips(level, originShip.id)

        val ships = traversed.mapNotNull { level.shipObjectWorld.allShips.getById(it) }

        extraData = ShipSchematic.onCopy(level, ships)

        val objectAABB = saveShipData(ships, originShip)
//        saveEntityData(level, ships, objectAABB)
        SchematicActionsQueue.queueShipsSavingEvent(level, uuid, ships, this, postSaveFn)

        return true
    }

    private fun serializeShipData(tag: CompoundTag) {
        val shipDataTag = CompoundTag()

        shipDataTag.putVector3d("maxObjectPos", schemInfo.maxObjectEdge)

        val shipsDataTag = ListTag()
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

            shipTag.putVector3d("pis", it.posInShip)
            shipTag.putDouble("sc", it.shipScale)
            shipTag.putQuaterniond("rot", it.rotation)

            shipsDataTag.add(shipTag)
        }

        shipDataTag.put("data", shipsDataTag)
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

    override fun saveToFile(): IFile {
        val saveTag = CompoundTag()

        serializeShipData(saveTag)
        serializeExtraData(saveTag)
        serializeBlockPalette(saveTag)
        serializeGridDataInfo(saveTag)
        serializeExtraBlockData(saveTag)

        return CompoundTagIFileWithTopVersion(saveTag, schematicVersion)
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

            Pair(name, RawBytesIFile(byteArray))
        }
    }

    private fun deserializeBlockPalette(tag: CompoundTag) {
        val paletteTag = tag.get("blockPalette") as ListTag

        val newPalette = paletteTag.mapIndexed { i, it ->
            val state = NbtUtils.readBlockState(it as CompoundTag)
            if (state.isAir) { ELOG("State under id $i is air. $state") }
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

    override fun loadFromByteBuffer(buf: ByteBuf): Boolean {
        val file = CompoundTagIFile(CompoundTag())
        file.fromBytes(buf)

        val saveTag = file.tag!!

        deserializeShipData(saveTag)
        deserializeExtraData(saveTag)
        deserializeBlockPalette(saveTag)
        deserializeGridDataInfo(saveTag)
        deserializeExtraBlockData(saveTag)

        return true
    }
}