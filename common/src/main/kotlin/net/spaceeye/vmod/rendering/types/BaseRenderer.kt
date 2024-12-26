package net.spaceeye.vmod.rendering.types

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId

interface BaseRenderer: Serializable {
    fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long)
    fun copy(oldToNew: Map<ShipId, Ship>): BaseRenderer?
    fun scaleBy(by: Double)
    fun highlightUntil(until: Long) {}
}

interface BlockRenderer: BaseRenderer {
    override fun renderData(poseStack: PoseStack, camera: Camera, timestamp: Long) {}
    fun renderBlockData(poseStack: PoseStack, camera: Camera, buffer: MultiBufferSource, timestamp: Long)
}

interface PositionDependentRenderer: BaseRenderer {
    // position that will be used in calculation of whenever or not to render the object
    // doesn't need to be an actual position
    val renderingPosition: Vector3d
}

interface TimedRenderer: BaseRenderer {
    // timestamp of when it was started
    // if -1, then do not take beginning time into account, and always start executing it
    var timestampOfBeginning: Long
    // time for how long it should be active
    // if timestampOfBeginning + activeFor_mc > current time, then it will not render on client
    val activeFor_ms: Long

    // a flag for internal use that should be set to false at the beginning
    var wasActivated: Boolean
}