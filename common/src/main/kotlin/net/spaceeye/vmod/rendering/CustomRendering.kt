package net.spaceeye.vmod.rendering

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.rendering.types.PositionDependentRenderer
import net.spaceeye.vmod.rendering.types.TimedRenderer
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentSkipListSet

object ReservedRenderingPages {
    const val TimedRenderingObjects = -1L

    val reservedPagesList = mutableListOf(TimedRenderingObjects)
}

fun renderInWorld(poseStack: PoseStack, camera: Camera, minecraft: Minecraft) {
    minecraft.profiler.push("vmod_rendering_ship_objects")
    renderShipObjects(poseStack, camera)
    minecraft.profiler.pop()

    minecraft.profiler.push("vmod_rendering_timed_objects")
    renderTimedObjects(poseStack, camera)
    minecraft.profiler.pop()
}

private inline fun renderShipObjects(poseStack: PoseStack, camera: Camera) {
    val level = Minecraft.getInstance().level!!
    SynchronisedRenderingData.clientSynchronisedData.mergeData()

    try {
    for (ship in level.shipObjectWorld.loadedShips) {
        SynchronisedRenderingData.clientSynchronisedData.tryPoolDataUpdate(ship.id)
        for ((_, render) in SynchronisedRenderingData.clientSynchronisedData.cachedData[ship.id] ?: continue) {
            render.renderData(poseStack, camera)
        }
    }
    // let's hope that it never happens, but if it does, then do nothing
    } catch (e: ConcurrentModificationException) { ELOG("GOT ConcurrentModificationException WHILE RENDERING.\n${e.stackTraceToString()}"); }
}

private inline fun renderTimedObjects(poseStack: PoseStack, camera: Camera) {
    SynchronisedRenderingData.clientSynchronisedData.tryPoolDataUpdate(ReservedRenderingPages.TimedRenderingObjects)
    val cpos = Vector3d(Minecraft.getInstance().player!!.position())
    val now = getNow_ms()
    val toDelete = mutableListOf<Int>()
    val page = SynchronisedRenderingData.clientSynchronisedData.cachedData[ReservedRenderingPages.TimedRenderingObjects] ?: return
    for ((idx, render) in page) {
        if (render !is TimedRenderer || render !is PositionDependentRenderer) { toDelete.add(idx); ELOG("FOUND RENDERING DATA ${render.javaClass.simpleName} IN renderTimedObjects THAT DIDN'T IMPLEMENT INTERFACE TimedRenderingData OR PositionDependentRenderingData."); continue }
        if (!render.wasActivated && render.activeFor_ms == -1L) { render.timestampOfBeginning = now }
        if (render.activeFor_ms + render.timestampOfBeginning < now) { toDelete.add(idx); continue }
        if ((render.renderingPosition - cpos).sqrDist() > render.renderingArea*render.renderingArea) { continue }

        render.wasActivated = true
        render.renderData(poseStack, camera)
    }

    if (toDelete.isEmpty()) {return}
    SynchronisedRenderingData.clientSynchronisedData.pageIndicesToRemove.getOrPut(ReservedRenderingPages.TimedRenderingObjects) { ConcurrentSkipListSet(toDelete) }
}