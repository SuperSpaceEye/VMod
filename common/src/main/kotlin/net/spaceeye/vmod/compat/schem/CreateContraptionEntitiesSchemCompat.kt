package net.spaceeye.vmod.compat.schem

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IControlContraption
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.valkyrien_ship_schematics.ShipSchematic
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISerializable
import net.spaceeye.vmod.utils.ServerClosable
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.getCenterPos
import org.joml.primitives.AABBd
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.util.toAABBd
import org.valkyrienskies.mod.common.getShipManaging
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.*

class CreateContraptionEntitiesSchemCompat: SchemCompatItem, ServerClosable() {
    val copySync = mutableMapOf<Long, UUID>()
    val pasteSync = mutableMapOf<UUID, Entity>()

    init {
        ShipSchematic.registerCopyPasteEvents("Create Contraption Entities Schem Compat", ::onCopyEvent, { _, _, _, _, _ ->}, ::onBeforePaste)
    }

    override fun close() {
        copySync.clear()
        pasteSync.clear()
    }

    private fun onCopyEvent(level: ServerLevel, shipsToBeSaved: List<ServerShip>, globalMap: MutableMap<String, Any>, unregister: () -> Unit): ISerializable? {
        val entities = shipsToBeSaved
            .mapNotNull {ship -> Pair(ship.id,
                level
                    .getEntities(null, ship.shipAABB?.toAABBd(AABBd())?.toMinecraft() ?: return@mapNotNull null)
                    .filterIsInstance<AbstractContraptionEntity>()
                    .filter { ship == it.getShipManaging() })
            }

        if (entities.isEmpty()) return null

        val tag = CompoundTag()

        entities.forEach { (id, entities) ->
            if (entities.isEmpty()) {return@forEach}
            val tags = ListTag()
            entities.forEach { entity ->
                val entityTag = CompoundTag()
                EntitySavingPlatformUtils.saveToTag(entity, entityTag)

                val offset = getCenterPos(entity.position().x.toInt(), entity.position().z.toInt())
                val spos = Vector3d(entity.position()) - offset

                val posTag = ListTag()

                posTag.add(DoubleTag.valueOf(spos.x))
                posTag.add(DoubleTag.valueOf(spos.y))
                posTag.add(DoubleTag.valueOf(spos.z))

                entityTag.put("Pos", posTag)

                val contraptionTag = entityTag.getCompound("Contraption")
                val anchorTag = contraptionTag.getCompound("Anchor")

                val anchorPos = Vector3d(anchorTag.getInt("X"), anchorTag.getInt("Y"), anchorTag.getInt("Z")) + 0.5
                val sanchorPos = anchorPos - getCenterPos(anchorPos.x.toInt(), anchorPos.z.toInt())

                anchorTag.putInt("X", sanchorPos.x.toInt())
                anchorTag.putInt("Y", sanchorPos.y.toInt())
                anchorTag.putInt("Z", sanchorPos.z.toInt())

                //that's because you can't get bpos of controller from entity directly
                val relativeTag = entityTag.getCompound("ControllerRelative")
                val relativePos = Vector3d(relativeTag.getInt("X"), relativeTag.getInt("Y"), relativeTag.getInt("Z"))

                val bpos = (anchorPos + relativePos).toBlockPos()
                val syncUUID = UUID.randomUUID()
                entityTag.putUUID("VMOD SCHEM COMPAT", syncUUID)
                copySync[bpos.asLong()] = syncUUID

                tags.add(entityTag)
            }
            tag.put(id.toString(), tags)
        }

        return CompoundTagSerializable(tag)
    }

    private fun onBeforePaste(level: ServerLevel, loadedShips: List<Pair<ServerShip, Long>>, file: ISerializable?, globalMap: MutableMap<String, Any>, unregister: () -> Unit) {
        if (file == null) {return}
        val data = CompoundTagSerializable()
        data.deserialize(file.serialize())
        val tag = data.tag ?: return

        val oldToNew = loadedShips.associate { Pair(it.second, it.first) }

        tag.allKeys.mapNotNull { shipIdStr ->
            val shipId = shipIdStr.toLong()
            if (!oldToNew.containsKey(shipId)) {return@mapNotNull null}
            val ship = oldToNew[shipId] ?: return@mapNotNull null
            (tag.get(shipIdStr)!! as ListTag).forEach { entityTag ->
                entityTag as CompoundTag
                val shipCenter = getCenterPos(ship.transform.positionInShip.x().toInt(), ship.transform.positionInShip.z().toInt())

                val contraptionTag = entityTag.getCompound("Contraption")
                val anchorTag = contraptionTag.getCompound("Anchor")

                var anchorPos = Vector3d(anchorTag.getInt("X"), anchorTag.getInt("Y"), anchorTag.getInt("Z")) + 0.5
                anchorPos = anchorPos + shipCenter

                anchorTag.putInt("X", anchorPos.x.toInt())
                anchorTag.putInt("Y", anchorPos.y.toInt())
                anchorTag.putInt("Z", anchorPos.z.toInt())


                val posTag = entityTag.get("Pos") as ListTag
                var pos = Vector3d(posTag.getDouble(0), posTag.getDouble(1), posTag.getDouble(2))
                pos = pos + shipCenter

                val newPosTag = ListTag()
                newPosTag.add(DoubleTag.valueOf(pos.x))
                newPosTag.add(DoubleTag.valueOf(pos.y))
                newPosTag.add(DoubleTag.valueOf(pos.z))
                entityTag.put("Pos", newPosTag)



                val syncUUID = entityTag.getUUID("VMOD SCHEM COMPAT")

                val entity = EntityType.create(entityTag, level).get()
                entity.uuid = UUID.randomUUID()

                pasteSync[syncUUID] = entity

                level.addFreshEntity(entity)
            }
        }
    }

    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {
        val be = be ?: return

        val pos = be.blockPos ?: return
        val syncUUID = copySync[pos.asLong()] ?: return
        copySync.remove(pos.asLong())

        tag!!.putUUID("VMOD SCHEM COMPAT", syncUUID)
    }
    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState, delayLoading: (Boolean) -> Unit, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {
        if (!tag.contains("VMOD SCHEM COMPAT")) {return}
        val syncUUID = tag.getUUID("VMOD SCHEM COMPAT")

        delayLoading(false)

        afterPasteCallbackSetter {be ->
            val entity = pasteSync[syncUUID] ?: return@afterPasteCallbackSetter
            pasteSync.remove(syncUUID)
            if (entity !is ControlledContraptionEntity) return@afterPasteCallbackSetter
            if (be !is IControlContraption) return@afterPasteCallbackSetter
            be.attach(entity)
        }
    }
}