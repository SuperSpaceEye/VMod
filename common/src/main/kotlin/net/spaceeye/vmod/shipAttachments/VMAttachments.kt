package net.spaceeye.vmod.shipAttachments

import net.spaceeye.vmod.compat.vsBackwardsCompat.*

object VMAttachments {
    @OptIn(VsBeta::class)
    fun register() {
        vsApi.registerAttachment(GravityController::class.java) {
            useTransientSerializer()
        }
        vsApi.registerAttachment(PhysgunController::class.java) {
            useTransientSerializer()
        }
        vsApi.registerAttachment(ThrustersController::class.java) {
            useTransientSerializer()
        }
        vsApi.registerAttachment(CustomMassSave::class.java)
        vsApi.registerAttachment(WeightSynchronizer::class.java)
    }
}