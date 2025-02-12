package net.spaceeye.vmod.vEntityManaging

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISchematicEvent
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISerializable
import net.spaceeye.vmod.vEntityManaging.VEntityManager.Companion.dimensionToGroundBodyIdImmutable
import net.spaceeye.vmod.vEntityManaging.VEntityManager.Companion.getInstance
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.function.Supplier

class VModVEntityManagerCopyPasteEvents: ISchematicEvent {
    override fun onCopy(
        level: ServerLevel,
        shipsToBeSaved: List<ServerShip>
    ): ISerializable? {
        val instance = getInstance()

        val toSave = shipsToBeSaved.filter { instance.shipToVEntity.containsKey(it.id) }
        if (toSave.isEmpty()) {return null}

        val vEntitiesToSave = toSave.mapNotNull { instance.shipToVEntity[it.id] }.flatten().toSet()
        val tag = CompoundTag()
        tag.put(SAVE_TAG_NAME_STRING, ListTag().also { tag ->
            vEntitiesToSave.forEach { instance.saveVEntityToList(it, dimensionToGroundBodyIdImmutable!!.values, tag) } })
        instance.saveDimensionIds(tag)

        return CompoundTagSerializable(tag)
    }

    override fun onPasteBeforeBlocksAreLoaded(
        level: ServerLevel,
        maybeLoadedShips: List<Pair<ServerShip, Long>>,
        emptyShip: Pair<ServerShip, Long>,
        data: Supplier<FriendlyByteBuf>?
    ) {}

    override fun onPasteAfterBlocksAreLoaded(
        level: ServerLevel,
        loadedShips: List<Pair<ServerShip, Long>>,
        data: Supplier<FriendlyByteBuf>?
    ) {
        if (data == null) {return}
        val instance = getInstance()

        val tag = CompoundTagSerializable(CompoundTag()).also { it.deserialize(data.get()) }.tag!!
        val lastDimensionIds = instance.loadDimensionIds(tag)
        val toInitVEntities = (tag[SAVE_TAG_NAME_STRING] as ListTag).mapNotNull { instance.loadVEntityFromTag(it as CompoundTag, lastDimensionIds) }

        val mapped = loadedShips.associate {
            if (lastDimensionIds.containsKey(it.second)) {
                Pair(level.shipObjectWorld.dimensionToGroundBodyIdImmutable[lastDimensionIds[it.second]]!!, it.first.id)
            } else {
                Pair(it.second, it.first.id)
            }
        }

        val changedIds = mutableMapOf<Int, Int>()
        for (it in toInitVEntities) {
            level.makeVEntity(it.copyVEntity(level, mapped) ?: continue) { newId ->
                changedIds[it.mID] = newId ?: return@makeVEntity
            }
        }
    }
}