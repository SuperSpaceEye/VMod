package net.spaceeye.vmod.constraintsManaging

import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.rendering.RenderingTypes
import net.spaceeye.vmod.rendering.ServerRenderingData
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.RopeRenderer
import net.spaceeye.vmod.utils.RegistryObject
import net.spaceeye.vmod.utils.Vector3d
import org.jetbrains.annotations.ApiStatus.Internal
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId

interface Tickable {
    fun tick(server: MinecraftServer, unregister: () -> Unit)
}

interface MRenderable {
    var renderer: BaseRenderer?

    fun serializeRenderer(tag: CompoundTag) {
        if (renderer != null) {
            try {
                tag.putString("rendererType", renderer!!.typeName)
                tag.putByteArray("renderer", renderer!!.serialize().accessByteBufWithCorrectSize())
            } catch (e: Exception) { ELOG("FAILED TO SERIALIZE RENDERER WITH EXCEPTION\n${e.stackTraceToString()}")
            } catch (e: Error) { ELOG("FAILED TO SERIALIZE RENDERER WITH ERROR\n${e.stackTraceToString()}") }
        }
    }

    fun deserializeRenderer(tag: CompoundTag) {
        if (tag.contains("renderer")) {
            try {
                val type = tag.getString("rendererType")
                renderer = RenderingTypes.typeToSupplier(type).get()
                renderer!!.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(tag.getByteArray("renderer"))))
            } catch (e: Exception) { ELOG("FAILED TO DESERIALIZE RENDERER WITH EXCEPTION\n${e.stackTraceToString()}")
            } catch (e: Error) { ELOG("FAILED TO DESERIALIZE RENDERER WITH ERROR\n${e.stackTraceToString()}") }
        }
    }
}

interface MConstraint: RegistryObject {
    var mID: ManagedConstraintId
    // SHOULD BE SET TO -1.
    // DO NOT USE IT ANYWHERE. JUST IMPLEMENT AS SIMPLE VAR
    var saveCounter: Int

    fun stillExists(allShips: QueryableShipData<Ship>, dimensionIds: Collection<ShipId>): Boolean
    // SHOULDN'T RETURN GROUND SHIPID
    fun attachedToShips(dimensionIds: Collection<ShipId>): List<ShipId>

    // positions to which constraint is "attached" to the ship/world
    // is needed for strip tool, moving constraints on ship splitting
    fun getAttachmentPoints(): List<BlockPos>

    // is called on ship splitting
    fun moveShipyardPosition(level: ServerLevel, previous: BlockPos, new: BlockPos, newShipId: ShipId)

    fun copyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>): MConstraint?

    fun onScaleBy(level: ServerLevel, scaleBy: Double) {throw NotImplementedError("")}
    fun onScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) { onScaleBy(level, scaleBy) }
    fun getVSIds(): Set<VSConstraintId>

    fun nbtSerialize(): CompoundTag?
    fun nbtDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): MConstraint?

    @Internal fun onMakeMConstraint(level: ServerLevel): Boolean
    @Internal fun onDeleteMConstraint(level: ServerLevel)
}

fun updatePositions(
    newShipId: ShipId,
    previous: BlockPos,
    new: BlockPos,

    attachmentPoints: MutableList<BlockPos>,

    shipIds: MutableList<ShipId>,
    localPoints: MutableList<List<Vector3dc>>
) {
    for (i in attachmentPoints.indices) {
        val apoint = attachmentPoints[i]
        if (previous != apoint) {continue}
        shipIds[i] = newShipId
        localPoints[i] = localPoints[i].mapIndexed {
            i, it ->
            (Vector3d(it) - Vector3d(apoint) + Vector3d(new)).toJomlVector3d()
        }
        attachmentPoints[i] = new
    }
}

fun updateRenderer(
    localPos0: Vector3dc,
    localPos1: Vector3dc,
    shipId0: ShipId,
    shipId1: ShipId,
    id: Int
): BaseRenderer? {
    val renderer = ServerRenderingData.getRenderer(id) ?: return null

    when (renderer) {
        is RopeRenderer -> {
            renderer.point1 = Vector3d(localPos0)
            renderer.point2 = Vector3d(localPos1)
            ServerRenderingData.setRenderer(shipId0, shipId1, id, renderer)
        }

        is A2BRenderer -> {
            renderer.point1 = Vector3d(localPos0)
            renderer.point2 = Vector3d(localPos1)
            ServerRenderingData.setRenderer(shipId0, shipId1, id, renderer)
        }
    }
    return renderer
}