package net.spaceeye.vmod.rendering

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.rendering.types.PositionDependentRenderer
import net.spaceeye.vmod.rendering.types.TimedRenderer
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import org.valkyrienskies.mod.common.shipObjectWorld

object ReservedRenderingPages {
    const val TimedRenderingObjects = -1L
    const val ClientsideRenderingObjects = -2L

    val reservedPages = listOf(TimedRenderingObjects, ClientsideRenderingObjects)
}

private object RenderingSettings {
    val renderingArea = VMConfig.CLIENT.RENDERING.MAX_RENDERING_DISTANCE
}

fun renderInWorld(poseStack: PoseStack, camera: Camera, minecraft: Minecraft) {
    minecraft.profiler.push("vmod_rendering_ship_objects")
    renderShipObjects(poseStack, camera)
    minecraft.profiler.pop()

    minecraft.profiler.push("vmod_rendering_timed_objects")
    renderTimedObjects(poseStack, camera)
    minecraft.profiler.pop()

    minecraft.profiler.push("vmod_rendering_clientside_objects")
    renderClientsideObjects(poseStack, camera)
    minecraft.profiler.pop()
}

private inline fun renderShipObjects(poseStack: PoseStack, camera: Camera) {
    val level = Minecraft.getInstance().level!!

    try {
    for (ship in level.shipObjectWorld.loadedShips) {
        val data = SynchronisedRenderingData.clientSynchronisedData.getData()
        for ((_, render) in data[ship.id] ?: continue) {
            render.renderData(poseStack, camera)
        }
    }
    // let's hope that it never happens, but if it does, then do nothing
    } catch (e: ConcurrentModificationException) { ELOG("GOT ConcurrentModificationException WHILE RENDERING.\n${e.stackTraceToString()}"); }
}

private inline fun renderTimedObjects(poseStack: PoseStack, camera: Camera) {
    val cpos = Vector3d(Minecraft.getInstance().player!!.position())
    val now = getNow_ms()
    val toDelete = mutableListOf<Int>()
    val page = SynchronisedRenderingData.clientSynchronisedData.getData()[ReservedRenderingPages.TimedRenderingObjects] ?: return
    for ((idx, render) in page) {
        if (render !is TimedRenderer || render !is PositionDependentRenderer) { toDelete.add(idx); ELOG("FOUND RENDERING DATA ${render.javaClass.simpleName} IN renderTimedObjects THAT DIDN'T IMPLEMENT INTERFACE TimedRenderingData OR PositionDependentRenderingData."); continue }
        if (!render.wasActivated && render.activeFor_ms == -1L) { render.timestampOfBeginning = now }
        if (render.activeFor_ms + render.timestampOfBeginning < now) { toDelete.add(idx); continue }
        if ((render.renderingPosition - cpos).sqrDist() > RenderingSettings.renderingArea*RenderingSettings.renderingArea) { continue }

        render.wasActivated = true
        render.renderData(poseStack, camera)
    }

    if (toDelete.isEmpty()) {return}
    SynchronisedRenderingData.clientSynchronisedData.removeTimedRenderers(toDelete)
}

private inline fun renderClientsideObjects(poseStack: PoseStack, camera: Camera) {
    val page = SynchronisedRenderingData.clientSynchronisedData.getData()[ReservedRenderingPages.ClientsideRenderingObjects] ?: return
    for ((_, render) in page) {
        render.renderData(poseStack, camera)
    }
}