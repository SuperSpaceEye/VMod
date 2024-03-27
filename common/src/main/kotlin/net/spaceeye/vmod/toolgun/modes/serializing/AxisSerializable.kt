package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.AxisMode

interface AxisSerializable: MSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as AxisMode
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeEnum(posMode)
        buf.writeDouble(width)
        buf.writeBoolean(disableCollisions)
        buf.writeDouble(distanceFromBlock)

        buf.writeBoolean(secondaryFirstdRaycast)

        buf.writeEnum(primaryStage)
        buf.writeDouble(primaryAngle.it)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as AxisMode
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
        disableCollisions = buf.readBoolean()
        distanceFromBlock = buf.readDouble()

        secondaryFirstdRaycast = buf.readBoolean()

        primaryStage = buf.readEnum(primaryStage.javaClass)
        primaryAngle.it = buf.readDouble()
    }

    override fun serverSideVerifyLimits() {
        this as AxisMode
        val limits = ServerLimits.instance
        compliance = limits.compliance.get(compliance)
        maxForce = limits.maxForce.get(maxForce)
        fixedDistance = limits.fixedDistance.get(fixedDistance)
        distanceFromBlock = limits.distanceFromBlock.get(distanceFromBlock)
    }
}