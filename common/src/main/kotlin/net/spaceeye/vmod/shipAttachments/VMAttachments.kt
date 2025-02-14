package net.spaceeye.vmod.shipAttachments

import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.ValkyrienSkiesMod

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