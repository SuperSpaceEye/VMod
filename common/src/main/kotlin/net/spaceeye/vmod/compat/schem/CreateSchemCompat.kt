package net.spaceeye.vmod.compat.schem

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.spaceeye.vmod.constraintsManaging.getCenterPos
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.containers.CompoundTagSerializable
import net.spaceeye.vmod.utils.Vector3d
import org.joml.primitives.AABBd
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.util.toAABBd
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.*

class CreateSchemCompat: SchemCompatItem {
    init {
        ShipSchematic.registerCopyPasteEvents("create_compat", ::onCopyEvent, ::onAfterPasteEvent)
    }

    private fun onCopyEvent(level: ServerLevel, shipsToBeSaved: List<ServerShip>, unregister: () -> Unit): Serializable {
        val tag = CompoundTag()

        shipsToBeSaved
        .mapNotNull {ship -> Pair(ship.id,
            level
                .getEntities(null, ship.shipAABB?.toAABBd(AABBd())?.toMinecraft() ?: return@mapNotNull null)
                .filterIsInstance<SuperGlueEntity>())
        }
        .forEach { (id, entities) ->
            if (entities.isEmpty()) {return@forEach}
            val tags = ListTag()
            entities.forEach {entity ->
                try {
                    val entityTag = CompoundTag()
                    entity.save(entityTag)
                    val offset = getCenterPos(entity.position().x.toInt(), entity.position().z.toInt())
                    val epos = Vector3d(entity.position()) - offset

                    val posTag = ListTag()

                    posTag.add(DoubleTag.valueOf(epos.x))
                    posTag.add(DoubleTag.valueOf(epos.y))
                    posTag.add(DoubleTag.valueOf(epos.z))

                    entityTag.put("Pos", posTag)

                    tags.add(entityTag)
                } catch (_: Exception) {}
            }
            tag.put(id.toString(), tags)
        }

        return CompoundTagSerializable(tag)
    }

    private fun onAfterPasteEvent(level: ServerLevel, loadedShips: List<Pair<ServerShip, Long>>, file: Serializable, unregister: () -> Unit) {
        val data = CompoundTagSerializable()
        data.deserialize(file.serialize())
        val tag = data.tag ?: return

        val oldToNew = loadedShips.associate { Pair(it.second, it.first) }

        tag.allKeys.mapNotNull { shipIdStr ->
            val shipId = shipIdStr.toLong()
            if (!oldToNew.containsKey(shipId)) {return@mapNotNull null}
            val ship = oldToNew[shipId] ?: return@mapNotNull null
            (tag.get(shipIdStr)!! as ListTag).forEach {
                it as CompoundTag
                val entity = SuperGlueEntity(level, AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
                entity.load(it)
                val npos = Vector3d(entity.position()) + getCenterPos(ship.transform.positionInShip.x().toInt(), ship.transform.positionInShip.z().toInt())
                entity.setPos(npos.toMCVec3())
                entity.uuid = UUID.randomUUID()
                level.addFreshEntity(entity)
            }
        }
    }


    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}
    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {}
}