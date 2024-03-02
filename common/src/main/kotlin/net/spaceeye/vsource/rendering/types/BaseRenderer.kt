package net.spaceeye.vsource.rendering.types

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.spaceeye.vsource.networking.dataSynchronization.DataUnit
import net.spaceeye.vsource.rendering.SynchronisedRenderingData
import net.spaceeye.vsource.utils.Vector3d

interface BaseRenderer: DataUnit {
    fun renderData(poseStack: PoseStack, camera: Camera)

    override fun hash(): ByteArray = SynchronisedRenderingData.hasher.digest(serialize().accessByteBufWithCorrectSize())
}

interface PositionDependentRenderer: BaseRenderer {
    // if client is farther than rendering area, then it will not render
    val renderingArea: Double
    // position that will be used in calculation of whenever or not to render the object
    // doesn't need to be an actual position
    val renderingPosition: Vector3d
}

interface TimedRenderer: BaseRenderer {
    // timestamp of when it was started
    // if -1, then do not take beginning time into account, and always execute it
    var timestampOfBeginning: Long
    // time for how long it should be active
    // if timestampOfBeginning + activeFor_mc > current time, then it will not render on client
    val activeFor_ms: Long

    // a flag for internal use that should be set to false at the beginning
    var wasActivated: Boolean
}