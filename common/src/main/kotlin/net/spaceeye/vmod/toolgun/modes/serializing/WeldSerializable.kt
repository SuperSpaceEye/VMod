package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.WeldMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementAssistSerialize

interface WeldSerializable: MSerializable, PlacementAssistSerialize {
    override fun serialize(): FriendlyByteBuf {
        this as WeldMode
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeEnum(posMode)
        buf.writeDouble(width)
        buf.writeDouble(fixedDistance)

        buf.writeBoolean(primaryFirstRaycast)

        paSerialize(buf)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as WeldMode
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
        fixedDistance = buf.readDouble()

        primaryFirstRaycast = buf.readBoolean()

        paDeserialize(buf)
    }

    override fun serverSideVerifyLimits() {
        this as WeldMode
        val limits = ServerLimits.instance

        compliance = limits.compliance.get(compliance)
        maxForce = limits.maxForce.get(maxForce)
        fixedDistance = limits.fixedDistance.get(fixedDistance)
        paServerSideVerifyLimits()
    }
}