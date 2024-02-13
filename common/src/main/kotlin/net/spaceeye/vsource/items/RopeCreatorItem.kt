package net.spaceeye.vsource.items

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.LOG
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.rendering.types.RopeRenderer
import net.spaceeye.vsource.utils.RaycastFunctions
import net.spaceeye.vsource.utils.constraintsSaving.makeManagedConstraint
import net.spaceeye.vsource.utils.dataSynchronization.ServerChecksumsUpdatedPacket
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

// TODO make rope ends actual ray hit positions and not just at the center of the blocks
class RopeCreatorItem: BaseTool() {
    var previousResult: RaycastFunctions.RaycastResult? = null


    override fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (level is ClientLevel) {return}
        if (level !is ServerLevel) {return}

        if (previousResult == null) {previousResult = raycastResult; return}

        val ship1 = level.getShipManagingPos(previousResult!!.blockPosition)
        val ship2 = level.getShipManagingPos(raycastResult.blockPosition)

        if (ship1 == null && ship2 == null) { resetState(); return }
        if (ship1 == ship2) { resetState(); return }

        val spoint1 = previousResult!!.globalHitPos
        val spoint2 = raycastResult.globalHitPos

        var shipId1: ShipId = ship1?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
        var shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        val rpoint1 = if (ship1 == null) spoint1 else previousResult!!.worldHitPos
        val rpoint2 = if (ship2 == null) spoint2 else raycastResult.worldHitPos

        val constraint = VSRopeConstraint(
            shipId1, shipId2,
            1e-10,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            1e10,
            (rpoint1 - rpoint2).dist()
        )

        val id = level.makeManagedConstraint(constraint)

        val server = SynchronisedRenderingData.serverSynchronisedData

        val data = RopeRenderer(
            ship1 != null,
            ship2 != null,
            spoint1, spoint2,
            (rpoint1 - rpoint2).dist()
        )
//        val data = A2BRenderer(
//            ship1 != null,
//            ship2 != null,
//            spoint1, spoint2,
//        )

        val idToAttachTo = if (ship1 != null) {shipId1} else {shipId2}

        server.data.getOrPut(shipId2) { mutableMapOf() }
        server.data.getOrPut(shipId1) { mutableMapOf() }
        val page = server.data[idToAttachTo]!!
        page[id!!.id] = data

        server.serverChecksumsUpdatedConnection().sendToClients(level.players(), ServerChecksumsUpdatedPacket(
            idToAttachTo, mutableListOf(Pair(id!!.id, data.hash()))
        ))

        resetState()

    }

    override fun resetState() {
        LOG("RESETTING STATE")
        previousResult = null
    }
}