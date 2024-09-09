package net.spaceeye.vmod.compat.schem

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.schematic.ShipSchematic
import net.spaceeye.vmod.schematic.containers.CompoundTagSerializable
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.getCenterPos
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingData
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId
import org.valkyrienskies.core.impl.util.serialization.FastUtilModule
import org.valkyrienskies.core.impl.util.serialization.GuaveSerializationModule
import org.valkyrienskies.core.impl.util.serialization.JOMLSerializationModule
import org.valkyrienskies.core.impl.util.serialization.VSSerializationModule
import org.valkyrienskies.mod.common.getShipManagingPos

class ClockworkSchemCompat(): SchemCompatItem {
    init {
        ShipSchematic.registerCopyPasteEvents("vs_clockwork_compat", ::onCopyEvent, { _, _, _, _, _, -> }, ::onPasteBeforeEvent)
    }

    fun getMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        return mapper
            .registerModule(JOMLSerializationModule())
            .registerModule(VSSerializationModule())
            .registerModule(GuaveSerializationModule())
            .registerModule(FastUtilModule())
            .registerKotlinModule()
            .setVisibility(
            mapper.visibilityChecker
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.ANY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.ANY)
                .withSetterVisibility(JsonAutoDetect.Visibility.ANY)
            ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private fun onCopyEvent(level: ServerLevel, shipsToBeSaved: List<ServerShip>, globalMap: MutableMap<String, Any>, unregister: () -> Unit): Serializable {
        val tag = CompoundTag()

        val tagData = ListTag()

        val mapper = getMapper()

        shipsToBeSaved
            .mapNotNull { Pair(it.id, it.getAttachment(BearingController::class.java) ?: return@mapNotNull null) }
            .forEach {(id, data) ->
                val item = CompoundTag()

                val strData = mapper.writeValueAsBytes(data)

                item.putLong("oldId", id)
                item.putByteArray("data", strData)

                val posTag = ListTag()
                for ((k, item) in data.bearingData) {
                    val tag = CompoundTag()

                    val oldPos = Vector3d(item.bearingPosition ?: continue)

                    tag.putInt("k", k)
                    tag.putLong("ship", level.getShipManagingPos(oldPos.toBlockPos())!!.id)

                    posTag.add(tag)
                }
                item.put("posData", posTag)

                tagData.add(item)
        }

        tag.put("data", tagData)

        return CompoundTagSerializable(tag)
    }

    private fun onPasteBeforeEvent(level: ServerLevel, loadedShips: List<Pair<ServerShip, Long>>, file: Serializable?, globalMap: MutableMap<String, Any>, unregister: () -> Unit) {
        val data = CompoundTagSerializable()
        data.deserialize(file!!.serialize())
        val tag = data.tag ?: return

        val mapper = getMapper()

        val oldToShip = loadedShips.associate { Pair(it.second, it.first) }

        (tag["data"] as ListTag).map {
            it as CompoundTag

            val oldId = it.getLong("oldId")

            val attachment = mapper.readValue(it.getByteArray("data"), BearingController::class.java)

            val idData = (it["posData"] as ListTag).associate { it as CompoundTag; Pair(it.getInt("k"), it.getLong("ship")) }

            val atShip = oldToShip[oldId]!!
            attachment.bearingData.map { (key, item) ->
                val bearingShipId = idData[key]!!

                val beShip = oldToShip[bearingShipId]!!

                val newPos = Vector3d(item.bearingPosition!!) - getCenterPos(
                    item.bearingPosition!!.x().toInt(), item.bearingPosition!!.z().toInt()
                ) + getCenterPos(
                    beShip.chunkClaim.xMiddle * 16,
                    beShip.chunkClaim.zMiddle * 16
                )

                Pair(key, PhysBearingData(
                    newPos.toJomlVector3d(), item.bearingAxis, item.bearingAngle, item.bearingRPM, item.locked,
                    oldToShip[item.shiptraptionID]?.id ?: -1L,
                    VSConstraintAndId(-1, item.attachConstraint!!.let {
                        val pos1 = Vector3d(it.localPos1) - getCenterPos(it.localPos1.x().toInt(), it.localPos1.z().toInt()) + getCenterPos(beShip.chunkClaim.xMiddle * 16, beShip.chunkClaim.zMiddle * 16)
                        val pos2 = Vector3d(it.localPos0) - getCenterPos(it.localPos0.x().toInt(), it.localPos0.z().toInt()) + getCenterPos(atShip.chunkClaim.xMiddle * 16, atShip.chunkClaim.zMiddle * 16)
                        it.copy(atShip.id, beShip.id, localPos0 = pos2.toJomlVector3d(), localPos1 = pos1.toJomlVector3d())
                    }),
                    VSConstraintAndId(-1, item.hingeConstraint!!.copy(beShip.id, atShip.id)),
                    null,
                    null,
                    item.secondAttachConstraint?.let {
                        VSConstraintAndId(-1, item.secondAttachConstraint!!.let {
                            val pos1 = Vector3d(it.localPos1) - getCenterPos(it.localPos1.x().toInt(), it.localPos1.z().toInt()) + getCenterPos(beShip.chunkClaim.xMiddle * 16, beShip.chunkClaim.zMiddle * 16)
                            val pos2 = Vector3d(it.localPos0) - getCenterPos(it.localPos0.x().toInt(), it.localPos0.z().toInt()) + getCenterPos(atShip.chunkClaim.xMiddle * 16, atShip.chunkClaim.zMiddle * 16)
                            it.copy(atShip.id, beShip.id, localPos0 = pos2.toJomlVector3d(), localPos1 = pos1.toJomlVector3d())
                        })
                    }
                ))
            }.forEach { (key, data) -> attachment.bearingData[key] = data }
            atShip.saveAttachment(BearingController::class.java, attachment)
        }
    }

    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, tag: CompoundTag, state: BlockState, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {
        if (state.block != ClockworkBlocks.PHYS_BEARING.get()) {return}
        val id = tag.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
        val mapped = oldToNewId[id] ?: -1
        tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, mapped)
    }
}