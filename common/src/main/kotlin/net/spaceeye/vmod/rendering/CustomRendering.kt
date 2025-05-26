package net.spaceeye.vmod.rendering

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.LevelRenderer.DIRECTIONS
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.mixin.BlockRenderDispatcherAccessor
import net.spaceeye.vmod.rendering.types.BlockRenderer
import net.spaceeye.vmod.rendering.types.PositionDependentRenderer
import net.spaceeye.vmod.rendering.types.TimedRenderer
import net.spaceeye.vmod.toolgun.CELOG
import net.spaceeye.vmod.translate.RENDERING_HAS_THROWN_AN_EXCEPTION
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.getNow_ms
import org.valkyrienskies.mod.common.shipObjectWorld
import java.awt.Color
import java.util.*
import kotlin.ConcurrentModificationException

object ReservedRenderingPages {
    const val TimedRenderingObjects = -1L
    const val ClientsideRenderingObjects = -2L

    val reservedPages = listOf(TimedRenderingObjects, ClientsideRenderingObjects)
}

private object RenderingSettings {
    val renderingArea = VMConfig.CLIENT.RENDERING.MAX_RENDERING_DISTANCE
}

internal object RenderingStuff {
    val blockRenderer = Minecraft.getInstance().blockRenderer
    val blockBuffer = Minecraft.getInstance().renderBuffers().bufferSource()

    private val random = Random()
    fun renderSingleBlock(
        state: BlockState,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
        color: Color = Color(255, 255, 255, 255)
    ) {
        val renderShape = state.renderShape
        when (renderShape) {
            RenderShape.MODEL -> {
                val bakedModel: BakedModel = blockRenderer.getBlockModel(state)
                val consumer = bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(state, false))
                val pose = poseStack.last()

                val r = color.red.toFloat() / 255f
                val g = color.green.toFloat() / 255f
                val b = color.blue.toFloat() / 255f

                DIRECTIONS.forEach { direction ->
                    bakedModel.getQuads(state, direction, random).forEach { quad ->
                        consumer.putBulkData(pose, quad, r, g, b, packedLight, packedOverlay)
                    }
                }

                bakedModel.getQuads(state, null, random).forEach { quad ->
                    consumer.putBulkData(pose, quad, r, g, b, packedLight, packedOverlay)
                }
            }

            RenderShape.ENTITYBLOCK_ANIMATED ->
                (blockRenderer as BlockRenderDispatcherAccessor).blockEntityRenderer
                    .renderByItem(ItemStack(state.block), ItemTransforms.TransformType.NONE, poseStack, bufferSource, packedLight, packedOverlay)

            RenderShape.INVISIBLE -> {}
        }
    }
}

private var renderTick = 0L

//TODO renderBlockRenderers is fucking stupid
//this is called from two places: GameRendererMixin and LevelRendererMixin.
// from GameRendererMixin it renders VEntities and from LevelRendererMixin it renders BlockRenderers
// why? if normal renderers are rendered in LevelRenderMixin, blending shits itself
// if BlockRenderers are rendered in GameRendererMixin, blocks "bob" with the hand, and i have 0 idea why. probably
// because of poseStack but idk
fun renderInWorld(poseStack: PoseStack, camera: Camera, minecraft: Minecraft, renderBlockRenderers: Boolean) {
    val now = getNow_ms()
    PersistentEvents.clientPreRender.emit(PersistentEvents.ClientPreRender(now))

    minecraft.profiler.push("vmod_rendering_ship_objects")
    renderShipObjects(poseStack, camera, renderBlockRenderers, now, renderTick++)
    minecraft.profiler.pop()

    minecraft.profiler.push("vmod_rendering_timed_objects")
    renderTimedObjects(poseStack, camera, renderBlockRenderers, now)
    minecraft.profiler.pop()

    minecraft.profiler.push("vmod_rendering_clientside_objects")
    renderClientsideObjects(poseStack, camera, renderBlockRenderers, now)
    minecraft.profiler.pop()
}

private fun renderShipObjects(poseStack: PoseStack, camera: Camera, renderBlockRenderers: Boolean, timestamp: Long, renderTick: Long) {
    val level = Minecraft.getInstance().level!!

    try {
    for (ship in level.shipObjectWorld.loadedShips) {
        val data = RenderingData.client.getData()
        for ((_, render) in data[ship.id] ?: continue) {
            val poseStack = PoseStack().also {
                it.setIdentity()
                it.mulPoseMatrix(poseStack.last().pose())
            }
            when (render) {
                is BlockRenderer -> if (renderBlockRenderers) if (render.renderingTick != renderTick) render.also { it.renderingTick = renderTick }.renderBlockData(poseStack, camera, RenderingStuff.blockBuffer, timestamp)
                else -> if(!renderBlockRenderers) if (render.renderingTick != renderTick) render.also { it.renderingTick = renderTick }.renderData(poseStack, camera, timestamp)
            }
        }
    }
    // let's hope that it never happens, but if it does, then do nothing
    } catch (e: ConcurrentModificationException) { CELOG("Got ConcurrentModificationException while rendering.\n${e.stackTraceToString()}", RENDERING_HAS_THROWN_AN_EXCEPTION);
    } catch (e: Exception) { ELOG("Renderer raised exception:\n${e.stackTraceToString()}")
    } catch (e: Error) { ELOG("Renderer raised error!!!\n${e.stackTraceToString()}") }
}

private fun renderTimedObjects(poseStack: PoseStack, camera: Camera, renderBlockRenderers: Boolean, timestamp: Long) {
    if (renderBlockRenderers) {return}
    val cpos = Vector3d(Minecraft.getInstance().player!!.position())
    val now = getNow_ms()
    val toDelete = mutableListOf<Int>()
    val page = RenderingData.client.getData()[ReservedRenderingPages.TimedRenderingObjects] ?: return
    for ((idx, render) in page) {
        if (render !is TimedRenderer || render !is PositionDependentRenderer) { toDelete.add(idx); CELOG("Found renderer in ${render.javaClass.simpleName} in renderTimedObjects that didn't implement interface TimedRenderingData or PositionDependentRenderingData.", RENDERING_HAS_THROWN_AN_EXCEPTION); continue }
        if (!render.wasActivated && render.activeFor_ms == -1L) { render.timestampOfBeginning = now }
        if (render.activeFor_ms + render.timestampOfBeginning < now) { toDelete.add(idx); continue }
        if ((render.renderingPosition - cpos).sqrDist() > RenderingSettings.renderingArea*RenderingSettings.renderingArea) { continue }

        val poseStack = PoseStack().also {
            it.setIdentity()
            it.mulPoseMatrix(poseStack.last().pose())
        }
        render.wasActivated = true
        render.renderData(poseStack, camera, timestamp)
    }

    if (toDelete.isEmpty()) {return}
    RenderingData.client.removeTimedRenderers(toDelete)
}

private fun renderClientsideObjects(poseStack: PoseStack, camera: Camera, renderBlockRenderers: Boolean, timestamp: Long) {
    val page = RenderingData.client.getData()[ReservedRenderingPages.ClientsideRenderingObjects] ?: return
    try {
    for ((_, render) in page) {
        val poseStack = PoseStack().also {
            it.setIdentity()
            it.mulPoseMatrix(poseStack.last().pose())
        }
        when (render) {
            is BlockRenderer -> if (renderBlockRenderers) if (render.renderingTick != renderTick) render.also { it.renderingTick = renderTick }.renderBlockData(poseStack, camera, RenderingStuff.blockBuffer, timestamp)
            else -> if(!renderBlockRenderers) if (render.renderingTick != renderTick) render.also { it.renderingTick = renderTick }.renderData(poseStack, camera, timestamp)
        }
    }
    // let's hope that it never happens, but if it does, then do nothing
    } catch (e: ConcurrentModificationException) { CELOG("Got ConcurrentModificationException while rendering.\n${e.stackTraceToString()}", RENDERING_HAS_THROWN_AN_EXCEPTION);
    } catch (e: Exception) { ELOG("Renderer raised exception:\n${e.stackTraceToString()}")
    } catch (e: Error) { ELOG("Renderer raised error!!!\n${e.stackTraceToString()}") }
}