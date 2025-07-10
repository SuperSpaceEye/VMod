package net.spaceeye.vmod.vEntityManaging

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.utils.Vector3d
import org.jetbrains.annotations.ApiStatus.Internal
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointId
import org.valkyrienskies.core.apigame.world.PhysLevelCore
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

interface Tickable {
    val alwaysTick: Boolean get() = false
    fun physTick(level: PhysLevelCore, delta: Double)
    fun serverTick(server: MinecraftServer, unregister: () -> Unit)
}

interface VSJointUser {
    fun getVSIds(): Set<VSJointId>
}

//VMod Entity -> VEntity
/**
 * -1 shipId is a ground connection
 */
interface VEntity {
    var mID: VEntityId
    var dimensionId: String?

    fun stillExists(allShips: QueryableShipData<Ship>): Boolean
    fun attachedToShips(): List<ShipId>

    // positions to which VEntity is "attached" to the ship/world
    // is needed for strip tool, moving VEntities on ship splitting
    /**
     * By default (-1) should return all attachment positions
     * If given shipId, should only return positions belonging to that shipId
     */
    fun getAttachmentPoints(shipId: Long = -1): List<Vector3d>

    // is called on ship splitting
    fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId)

    /**
     * @param centerPositions first is old, second is new
     */
    fun copyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity?

    fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d)

    fun nbtSerialize(): CompoundTag?
    fun nbtDeserialize(tag: CompoundTag): VEntity?

    /**
     * For joints convert -1 to null, also it should be first in the joint
     */
    @Internal fun onMakeVEntity(level: ServerLevel): List<CompletableFuture<Boolean>>
    @Internal fun onDeleteVEntity(level: ServerLevel)

    companion object {
        class HelperFn {
            val futures = mutableListOf<CompletableFuture<Boolean>>()

            fun mc(joint: VSJoint, cIDs: ConcurrentLinkedQueue<VSJointId>, level: ServerLevel) {
                futures.add(net.spaceeye.vmod.vEntityManaging.util.mc(joint, cIDs, level))
            }
        }

        fun VEntity.withFutures(fn: HelperFn.(inst: VEntity) -> Unit): List<CompletableFuture<Boolean>> {
            val inst = HelperFn()
            inst.fn(this)
            return inst.futures
        }
    }
}