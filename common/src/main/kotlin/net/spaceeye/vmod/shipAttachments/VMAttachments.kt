package net.spaceeye.vmod.shipAttachments

import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.mod.api.vsApi

object VMAttachments {
    @OptIn(VsBeta::class)
    fun register() {
        vsApi.registerAttachment(PhysgunController::class.java) { useTransientSerializer() }
        vsApi.registerAttachment(ThrustersController::class.java) { useTransientSerializer() }
        vsApi.registerAttachment(AttachmentAccessor::class.java) { useTransientSerializer() }
        vsApi.registerAttachment(DebugAttachment::class.java) { useTransientSerializer() }

        vsApi.registerAttachment(WeightSynchronizer::class.java)
        vsApi.registerAttachment(CustomMassSave::class.java)
        vsApi.registerAttachment(GravityController::class.java)

        //TODO remove later
        vsApi.shipLoadEvent.on { val ship = it.ship
            GravityController.getOrCreate(ship)
            PhysgunController.getOrCreate(ship)
            ThrustersController.getOrCreate(ship)
            CustomMassSave.getOrCreate(ship)
            AttachmentAccessor.getOrCreate(ship)
        }


        PhysgunController
        ThrustersController
        WeightSynchronizer
        CustomMassSave
        GravityController
        AttachmentAccessor
    }
}