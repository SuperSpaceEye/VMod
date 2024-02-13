package net.spaceeye.vsource.items

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.LOG
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.rendering.types.A2BRenderer
import net.spaceeye.vsource.utils.RaycastFunctions
import net.spaceeye.vsource.utils.Vector3d
import net.spaceeye.vsource.utils.constraintsSaving.makeManagedConstraint
import net.spaceeye.vsource.utils.dataSynchronization.ServerChecksumsUpdatedPacket
import net.spaceeye.vsource.utils.posShipToWorld
import net.spaceeye.vsource.utils.posWorldToShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.*
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class WeldCreatorItem : BaseTool() {
    var previousResult: RaycastFunctions.RaycastResult? = null

    override fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) {
        if (level !is ServerLevel) {return}

        if (previousResult == null) {previousResult = raycastResult; return}

        val ship1 = level.getShipManagingPos(previousResult!!.blockPosition)
        val ship2 = level.getShipManagingPos(raycastResult.blockPosition)

        if (ship1 == null && ship2 == null) { resetState(); return }
        if (ship1 == ship2) { resetState(); return }

        var shipId1: ShipId = ship1?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
        var shipId2: ShipId = ship2?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        var spoint1 = previousResult!!.globalHitPos
        var spoint2 = raycastResult.globalHitPos

        var rpoint1 = if (ship1 == null) spoint1 else posShipToWorld(ship1, previousResult!!.globalHitPos)
        var rpoint2 = if (ship2 == null) spoint2 else posShipToWorld(ship2, raycastResult.globalHitPos)

        val attachmentConstraint = VSAttachmentConstraint(
            shipId1, shipId2,
            1e-10,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            1e10,
            (rpoint1 - rpoint2).dist()
        )

        val id = level.makeManagedConstraint(attachmentConstraint)

        // RENDERING

        val server = SynchronisedRenderingData.serverSynchronisedData
        val data = A2BRenderer(
            ship1 != null,
            ship2 != null,
            spoint1, spoint2,
        )
        val idToAttachTo = if (ship1 != null) {shipId1} else {shipId2}

        server.data.getOrPut(shipId2) { mutableMapOf() }
        server.data.getOrPut(shipId1) { mutableMapOf() }
        val page = server.data[idToAttachTo]!!
        page[id!!.id] = data

        server.serverChecksumsUpdatedConnection().sendToClients(level.players(), ServerChecksumsUpdatedPacket(
            idToAttachTo, mutableListOf(Pair(id!!.id, data.hash()))
        ))

        //STOP OF RENDERING

        val dir = (rpoint1 - rpoint2).snormalize()

        rpoint1 = rpoint1 + dir
        rpoint2 = rpoint2 - dir

        spoint1 = if (ship1 != null) posWorldToShip(ship1, rpoint1) else Vector3d(rpoint1)
        spoint2 = if (ship2 != null) posWorldToShip(ship2, rpoint2) else Vector3d(rpoint2)

        val attachmentConstraint2 = VSAttachmentConstraint(
            shipId1, shipId2,
            1e-10,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            1e10,
            (rpoint1 - rpoint2).dist()
        )

        level.makeManagedConstraint(attachmentConstraint2)

        val rot1 = ship1?.transform?.shipToWorldRotation ?: ship2!!.transform.shipToWorldRotation
        val rot2 = ship2?.transform?.shipToWorldRotation ?: ship1!!.transform.shipToWorldRotation

        var rotConstraint: VSConstraint = VSRotDampingConstraint(
            shipId1, shipId2, 1e-10, rot1, rot2, 1e10, 1e10, VSRotDampingAxes.ALL_AXES)

        level.makeManagedConstraint(rotConstraint)

        resetState()
    }

    override fun resetState() {
        LOG("RESETTING")
        previousResult = null
    }
}