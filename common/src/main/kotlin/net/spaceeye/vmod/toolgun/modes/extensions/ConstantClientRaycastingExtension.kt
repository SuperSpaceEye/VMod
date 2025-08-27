package net.spaceeye.vmod.toolgun.modes.extensions

import net.minecraft.client.Minecraft
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModeExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.utils.Vector3d

class ConstantClientRaycastingExtension<T: ExtendableToolgunMode>(
    var clientTick: ((inst: T, rr: RaycastFunctions.RaycastResult) -> Unit),
    var clientRaycastingDistance: Double = 100.0
): ToolgunModeExtension {
    private lateinit var inst: T
    private var doWork = false

    override fun onInit(mode: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        inst = mode as T

        PersistentEvents.clientPreRender.on { (time), _ ->
            if (!doWork) {return@on}
            if (!inst.instance.client.playerIsUsingToolgun()) {return@on}

            val level = Minecraft.getInstance().level!!
            val camera = Minecraft.getInstance().gameRenderer.mainCamera!!

            val rr = RaycastFunctions.renderRaycast(
                level,
                RaycastFunctions.Source(Vector3d(camera.lookVector), Vector3d(camera.position)),
                clientRaycastingDistance
            )

            clientTick.invoke(inst, rr)
        }
    }

    override fun eOnOpenMode() { doWork = true }
    override fun eOnCloseMode() { doWork = false }
}