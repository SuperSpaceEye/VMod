package net.spaceeye.vmod.shipAttachments

import net.spaceeye.vmod.compat.vsBackwardsCompat.*

object VMAttachments {
    @OptIn(VsBeta::class)
    fun register() {
        vsApi.registerAttachment(PhysgunController::class.java) {
            useTransientSerializer()
        }
        vsApi.registerAttachment(ThrustersController::class.java) {
            useTransientSerializer()
        }
        vsApi.registerAttachment(WeightSynchronizer::class.java)

        vsApi.registerAttachment(CustomMassSave::class.java)
        vsApi.registerAttachment(GravityController::class.java)
        vsApi.registerAttachment(AttachmentAccessor::class.java)


        PhysgunController
        ThrustersController
        WeightSynchronizer
        CustomMassSave
        GravityController
        AttachmentAccessor
    }
}