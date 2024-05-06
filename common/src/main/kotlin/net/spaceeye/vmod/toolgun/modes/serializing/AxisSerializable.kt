package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.AxisMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementAssistSerialize

interface AxisSerializable: MSerializable, PlacementAssistSerialize {
    override fun serialize(): FriendlyByteBuf {
        this as AxisMode
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeEnum(posMode)
        buf.writeDouble(width)

        buf.writeBoolean(secondaryFirstRaycast)

        paSerialize(buf)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as AxisMode
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()

        secondaryFirstRaycast = buf.readBoolean()

        paDeserialize(buf)
    }

    override fun serverSideVerifyLimits() {
        this as AxisMode
        val limits = ServerLimits.instance
        compliance = limits.compliance.get(compliance)
        maxForce = limits.maxForce.get(maxForce)
        fixedDistance = limits.fixedDistance.get(fixedDistance)
        paServerSideVerifyLimits()
    }
}