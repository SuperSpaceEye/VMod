package net.spaceeye.vmod.shipAttachments

import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.ValkyrienSkiesMod

object VMAttachments {
    fun register() {
        ValkyrienSkiesMod
        ValkyrienSkiesMod.vsCore
        vsApi.registerAttachment(GravityController::class.java) {
            useTransientSerializer()
        }
        vsApi.registerAttachment(PhysgunController::class.java) {
            useTransientSerializer()
        }
        vsApi.registerAttachment(ThrustersController::class.java) {
            useTransientSerializer()
        }
        vsApi.registerAttachment(NOOP::class.java) {
            useTransientSerializer()
        }
        vsApi.registerAttachment(CustomMassSave::class.java)
    }
}