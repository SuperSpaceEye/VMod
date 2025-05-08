package net.spaceeye.vmod.vEntityManaging

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.properties.ShipId

fun ServerLevel.makeVEntity(ventity: VEntity, callback: ((VEntityId?) -> Unit) = {}) = VEntityManager.getInstance().makeVEntity(this, ventity, callback)
fun ServerLevel.getVEntity(id: VEntityId)                = VEntityManager.getInstance().getVEntity(id)
fun ServerLevel.removeVEntity(ventity: VEntity)          = VEntityManager.getInstance().removeVEntity(this, ventity.mID)
fun ServerLevel.removeVEntity(ventityId: VEntityId)      = VEntityManager.getInstance().removeVEntity(this, ventityId)
fun ServerLevel.getAllVEntityIdsOfShipId(shipId: ShipId) = VEntityManager.getInstance().getAllVEntitiesIdOfId(shipId)
fun ServerLevel.getVEntityIdsOfPosition(pos: BlockPos)   = VEntityManager.getInstance().tryGetIdsOfPosition(pos)

fun ServerLevel.disableCollisionBetween(shipId1: ShipId, shipId2: ShipId, callback: (()->Unit)? = null) = VEntityManager.getInstance().disableCollisionBetween(this, shipId1, shipId2, callback)
fun ServerLevel.enableCollisionBetween(shipId1: ShipId, shipId2: ShipId)  = VEntityManager.getInstance().enableCollisionBetween(this, shipId1, shipId2)
fun ServerLevel.getAllDisabledCollisionsOfId(shipId: ShipId) = VEntityManager.getInstance().getAllDisabledCollisionsOfId(shipId)

@Internal fun ServerLevel.makeVEntityWithId(ventity: VEntity, id: Int, callback: ((VEntityId?) -> Unit)) = VEntityManager.getInstance().makeVEntityWithId(this, ventity, id, callback)