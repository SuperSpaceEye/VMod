package net.spaceeye.vmod.shipAttachments

import net.spaceeye.vmod.events.RandomEvents
import org.valkyrienskies.mod.api.vsApi

object VMAttachments {
    fun register() {
        try {
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
        } catch (e: Exception) {
            RandomEvents.serverOnTick.on { _, unsub ->
                register()
                unsub.invoke()
            }
        } catch (e: Error) {
            RandomEvents.serverOnTick.on { _, unsub ->
                register()
                unsub.invoke()
            }
        }
    }
}