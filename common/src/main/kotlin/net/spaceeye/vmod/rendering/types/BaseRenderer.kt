package net.spaceeye.vmod.rendering.types

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.utils.RegistryObject
import net.spaceeye.vmod.utils.Vector3d

interface BaseRenderer: Serializable, RegistryObject {
    fun renderData(poseStack: PoseStack, camera: Camera)
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