package net.spaceeye.vsource.items

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vsource.LOG
import net.spaceeye.vsource.utils.RaycastFunctions
import net.spaceeye.vsource.utils.constraintsSaving.makeManagedConstraint
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
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

        val spoint1 = previousResult!!.globalHitPos
        val spoint2 = raycastResult.globalHitPos

        val rpoint1 = if (ship1 == null) spoint1 else previousResult!!.worldHitPos
        val rpoint2 = if (ship2 == null) spoint2 else raycastResult.worldHitPos

        val attachmentConstraint = VSAttachmentConstraint(
            shipId1, shipId2,
            1e-10,
            spoint1.toJomlVector3d(), spoint2.toJomlVector3d(),
            1e10,
            (rpoint1 - rpoint2).dist()
        )

        val id = level.makeManagedConstraint(attachmentConstraint)
    }

    override fun resetState() {
        LOG("RESETTING")
    }
}