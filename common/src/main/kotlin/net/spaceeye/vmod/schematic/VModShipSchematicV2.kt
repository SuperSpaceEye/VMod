package net.spaceeye.vmod.schematic

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Block
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.containers.v1.*
import net.spaceeye.valkyrien_ship_schematics.interfaces.IBlockStatePalette
import net.spaceeye.valkyrien_ship_schematics.interfaces.ICopyableForcesInducer
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematic
import net.spaceeye.valkyrien_ship_schematics.interfaces.IShipSchematicInfo
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISerializable
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.IShipSchematicDataV1
import net.spaceeye.valkyrien_ship_schematics.interfaces.v1.SchemSerializeDataV1Impl
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VM
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
import net.spaceeye.vmod.shipAttachments.AttachmentAccessor
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.transformProviders.SchemTempPositionSetter
import net.spaceeye.vmod.translate.makeFake
import net.spaceeye.vmod.utils.vs.posShipToWorld
import net.spaceeye.vmod.utils.vs.transformDirectionShipToWorld
import org.apache.logging.log4j.Logger
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

    override var extraData: MutableList<Pair<String, FriendlyByteBuf>> = mutableListOf()

    override var info: IShipSchematicInfo? = null
}

data class PasteSchematicSettings(
    var loadContainers: Boolean = true,
    var loadEntities: Boolean = true,

    var allowChunkPlacementInterruption: Boolean = true,
    var allowUpdateInterruption: Boolean = true,

    var blacklistMode: Boolean = true,
    var nbtLoadingBlacklist: Set<Block> = emptySet(),
    var nbtLoadingWhitelist: Set<Block> = emptySet(),

    var logger: Logger? = null,
    var nonfatalErrorsHandler: (numErrors: Int, schematic: IShipSchematicDataV1, player: ServerPlayer?) -> Unit = {_, _, _->}
)

@OptIn(VsBeta::class)
fun IShipSchematicDataV1.placeAt(
    level: ServerLevel, player: ServerPlayer?, uuid: UUID, pos: Vector3d, rotation: Quaterniondc,
     settings: PasteSchematicSettings = PasteSchematicSettings(
         logger = VM.logger,
         //TODO think of a way to make str into translatable
         nonfatalErrorsHandler = { numErrors, _, player -> player?.let { ServerToolGunState.sendErrorTo(it, "Schematic had $numErrors nonfatal errors") } }
     ), postPlaceFn: (List<ServerShip>) -> Unit): Boolean {
    extraData.forEach { (_, bytes) -> bytes.setIndex(0, bytes.accessByteBufWithCorrectSize().size) }

    val newTransforms = mutableListOf<BodyTransform>()

    val shipInitializers = (this as IShipSchematic).createShipConstructors(level, pos, rotation, newTransforms)

    if (!verifyBlockDataIsValid(shipInitializers.map { it.second }, player)) { return false }

    SchematicActionsQueue.queueShipsCreationEvent(level, player, uuid, shipInitializers, this, settings) { ships, centerPositions, entityCreationFn ->
        val createdShips = ships.map { it.second }

        val shipsMap = ships.toMap()
        try {
            loadAttachments(level, shipsMap, centerPositions, extraData)
        } catch (e: Throwable) {
            ELOG(e.stackTraceToString())
        }

        createdShips.zip(newTransforms).forEach { (it, transform) ->
            if (it.transformProvider is SchemTempPositionSetter) { it.transformProvider = null }
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
        ShipSchematic.onPasteAfterBlocksAreLoaded(level, shipsMap, centerPositions, extraData.toMap())
        postPlaceFn(createdShips)
        SchematicActionsQueue.queueShipsUnfreezeEvent(uuid, createdShips, 10)
    }

    return true
}

private class AttachmentsSerializable(): ISerializable {
    lateinit var data: MutableList<Pair<Long, MutableList<ICopyableForcesInducer?>>>

    constructor(data: MutableList<Pair<Long, MutableList<ICopyableForcesInducer>>>): this() {
        this.data = data as MutableList<Pair<Long, MutableList<ICopyableForcesInducer?>>>
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()
        val mapper = getMapper()

        buf.writeCollection(data) {buf, (shipId, attachments) ->
            buf.writeLong(shipId)
            buf.writeCollection(attachments) { buf, item ->
                buf.writeUtf(item!!.javaClass.name)
                buf.writeByteArray(mapper.writeValueAsBytes(item))
            }
        }

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        val mapper = getMapper()

        data = buf.readCollection({mutableListOf()}) { buf ->
            Pair(
                buf.readLong(),
                buf.readCollection({mutableListOf()}) { buf ->
                    try {
                        val className = buf.readUtf()
                        val clazz = Class.forName(className)
                        mapper.readValue(buf.readByteArray(), clazz)
                    } catch (e: Throwable) {
                        ELOG("Failed to deserialize attachment \n${e.stackTraceToString()}")
                        null
                    }
                }
            )
        }
    }
}

private fun IShipSchematicDataV1.saveAttachments(ships: List<ServerShip>, level: ServerLevel, centerPositions: Map<Long, Vector3d>) {
    val attachments = ships
        .mapNotNull { level.shipObjectWorld.loadedShips.getById(it.id) }
        .map { ship -> Pair(ship, AttachmentAccessor.getOrCreate(ship).forceInducers
            .filterIsInstance<ICopyableForcesInducer>()
            .mapNotNull { ship.getAttachment(it.javaClass) }
            .toMutableList()
        ) }
        .filter { it.second.isNotEmpty() }

    attachments.forEach { (ship, attachments) ->
        attachments.forEach {
            try {
                it.onCopy({level}, ship, ships, centerPositions)
            } catch (e: Throwable) {
                ELOG(e.stackTraceToString())
            }
        }
    }

    val data = attachments.map{ Pair(it.first.id, it.second) }.toMutableList()

    extraData.add(Pair("SavedAttachments", AttachmentsSerializable(data).serialize()))

    attachments.forEach { (ship, attachments) ->
        attachments.forEach {
            try {
                it.onAfterCopy({level}, ship, ships, centerPositions)
            } catch (e: Throwable) {
                ELOG(e.stackTraceToString())
            }
        }
    }
}

private fun loadAttachments(level: ServerLevel, ships: Map<Long, ServerShip>, centerPositions: Map<ShipId, Pair<JVector3d, JVector3d>>, extraData: MutableList<Pair<String, FriendlyByteBuf>>) {
    if (extraData.isEmpty() || extraData.last().first != "SavedAttachments") {return}

    val se = AttachmentsSerializable()
    se.deserialize(extraData.last().second)

    se.data.forEach { (oldId, attachments) ->
        val loadedShip = level.shipObjectWorld.loadedShips.getById(ships[oldId]!!.id) ?: return@forEach
        attachments.forEach {
            if (it == null) return@forEach
            //TODO remove this after 2.5.0. for some god forsaken reason VS allows you to add multiple separate attachments to one ship
            loadedShip.saveAttachment(it.javaClass, null)
            loadedShip.saveAttachment(it.javaClass, it)
            it.onPaste({level}, loadedShip, ships, centerPositions)
        }
    }
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

        val newShip = level.shipObjectWorld.createNewShipAtBlock(Vector3i(1000000000, 1000000000, 1000000000), false, it.shipScale, level.dimensionId)
        newShip.isStatic = true

        //TODO i don't like this but idk what else to do
        newShip.transformProvider = SchemTempPositionSetter(newShip, MVector3d(pos), MVector3d(newTransform.position), false)

        level.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(
            JVector3d(1000000000.0, 1000000000.0, 1000000000.0),
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
    val ships = traversed
        .mapNotNull { level.shipObjectWorld.allShips.getById(it) }
        .filter { (it.shipAABB != null).also { r -> if (!r && player != null) player.sendMessage(makeFake("${it.slug} has null shipAABB, ignoring"), UUID(0L, 0L)) } }
    val centerPositions = ships.associate {
        val b = it.shipAABB!!
        Pair(it.id, JVector3d(
            (b.maxX() - b.minX()) / 2.0 + b.minX(),
            (b.maxY() - b.minY()) / 2.0 + b.minY(),
            (b.maxZ() - b.minZ()) / 2.0 + b.minZ(),
        ))
    }
    extraData = ShipSchematic.onCopy(level, ships, centerPositions).map { Pair(it.first, it.second) }.toMutableList()

    (this as IShipSchematic).saveShipData(ships, originShip)
    this.saveAttachments(ships, level, centerPositions) // should always be saved last
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