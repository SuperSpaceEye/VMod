package net.spaceeye.vmod.vEntityManaging

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISchematicEvent
import net.spaceeye.valkyrien_ship_schematics.interfaces.ISerializable
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.VEntityManager.Companion.dimensionToGroundBodyIdImmutable
import net.spaceeye.vmod.vEntityManaging.VEntityManager.Companion.getInstance
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.function.Supplier

class VModVEntityManagerCopyPasteEvents: ISchematicEvent {
    override fun onCopy(
        level: ServerLevel,
        shipsToBeSaved: List<ServerShip>,
        centerPositions: Map<ShipId, JVector3d>,
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
        maybeLoadedShips: Map<Long, ServerShip>,
        emptyShip: Pair<Long, ServerShip>,
        centerPositions: Map<ShipId, Pair<org.joml.Vector3d, org.joml.Vector3d>>,
        data: Supplier<FriendlyByteBuf>?
    ) {}

    override fun onPasteAfterBlocksAreLoaded(
        level: ServerLevel,
        loadedShips: Map<Long, ServerShip>,
        centerPositions: Map<ShipId, Pair<org.joml.Vector3d, org.joml.Vector3d>>,
        data: Supplier<FriendlyByteBuf>?
    ) {
        if (data == null) {return}
        val centerPositions = centerPositions.map { Pair(it.key, Pair(Vector3d(it.value.first), Vector3d(it.value.second))) }.toMap()
        val instance = getInstance()

        val tag = CompoundTagSerializable(CompoundTag()).also { it.deserialize(data.get()) }.tag!!
        val lastDimensionIds = instance.loadDimensionIds(tag)
        val toInitVEntities = (tag[SAVE_TAG_NAME_STRING] as ListTag).mapNotNull { instance.loadVEntityFromTag(it as CompoundTag, lastDimensionIds) }

        val mapped = loadedShips.map { Pair(it.key, it.value.id) }.toMap()

        val changedIds = mutableMapOf<Int, Int>()
        for (it in toInitVEntities) {
            level.makeVEntity(it.copyVEntity(level, mapped, centerPositions) ?: continue) { newId ->
                changedIds[it.mID] = newId ?: return@makeVEntity
            }
        }
    }
}