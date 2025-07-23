package net.spaceeye.vmod.compat.schem

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.getMapper
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingCreateData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingData
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class ClockworkSchemCompat(): SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, centerPositions: Map<Long, JVector3d>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {
    //TODO

    //        if (state.block != ClockworkBlocks.PHYS_BEARING.get()) {return}
//        if (be !is PhysBearingBlockEntity) {return}
//
//        val ship = level.shipObjectWorld.loadedShips.getById(tag!!.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID) ?: return) ?: return
//        val controller = BearingController.getOrCreate(ship)!!
//        val bearingId = tag.getInt(ClockworkConstants.Nbt.BEARING_ID)
//
//        val data = controller.bearingData[bearingId] ?: return
//
//        tag.putLong("vmod schem compat bearingShipId", level.getShipObjectManagingPos(pos)!!.id)
//        tag.putByteArray("vmod schem compat", getMapper().writeValueAsBytes(data))
    }

    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, centerPositions: Map<Long, Pair<JVector3d, JVector3d>>, tag: CompoundTag, pos: BlockPos, state: BlockState, tagTransformer: (((CompoundTag?) -> CompoundTag?)?) -> Unit, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {
    //        if (state.block != ClockworkBlocks.PHYS_BEARING.get()) {return}
//        delayLoading(true) { tag ->
//            val id = tag!!.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
//            val mapped = oldToNewId[id] ?: return@delayLoading tag
//            val ship = level.shipObjectWorld.loadedShips.getById(mapped) ?: return@delayLoading tag
//            if (!tag.contains("vmod schem compat")) {return@delayLoading tag}
//            val data = getMapper().readValue(tag.getByteArray("vmod schem compat"), PhysBearingData::class.java)
//            val beShipId = tag.getLong("vmod schem compat bearingShipId")
//
//            val controller = BearingController.getOrCreate(ship)!!
//
//            val bearingId = controller.addPhysBearing(PhysBearingCreateData(
//                tryMovePosition(data.bearingPosition!!, beShipId, centerPositions)!!,
//                data.bearingAxis!!, data.bearingAngle, data.bearingRPM, data.locked, mapped,
//                VSConstraintAndId(-1, data.attachConstraint!!.let { it.copy(
//                    oldToNewId[it.shipId0]!!,
//                    oldToNewId[it.shipId1]!!,
//                    it.compliance,
//                    tryMovePosition(it.localPos0, it.shipId0, centerPositions)!!,
//                    tryMovePosition(it.localPos1, it.shipId1, centerPositions)!!,
//                    ) }),
//                VSConstraintAndId(-1, data.hingeConstraint!!.let { it.copy(oldToNewId[it.shipId0]!!, oldToNewId[it.shipId1]!!) }),
//                null, null,
//                data.secondAttachConstraint?.let { VSConstraintAndId(-1, it.copy(
//                    oldToNewId[it.shipId0]!!,
//                    oldToNewId[it.shipId1]!!,
//                    it.compliance,
//                    tryMovePosition(it.localPos0, it.shipId0, centerPositions)!!,
//                    tryMovePosition(it.localPos1, it.shipId1, centerPositions)!!,
//                ) ) }
//            ))
//
//            tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, mapped)
//            tag.putInt(ClockworkConstants.Nbt.BEARING_ID, bearingId)
//            tag
//        }
    }
}