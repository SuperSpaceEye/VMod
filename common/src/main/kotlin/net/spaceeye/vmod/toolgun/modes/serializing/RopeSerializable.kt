package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.RopeMode

interface RopeSerializable: MSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as RopeMode

        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeDouble(fixedDistance)
        buf.writeEnum(posMode)
        buf.writeDouble(width)
        buf.writeInt(segments)

        buf.writeBoolean(primaryFirstRaycast)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as RopeMode

        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        fixedDistance = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
        segments = buf.readInt()

        primaryFirstRaycast = buf.readBoolean()
    }

    override fun serverSideVerifyLimits() {
        this as RopeMode
        val limits = ServerLimits.instance

        compliance = limits.compliance.get(compliance)
        maxForce = limits.maxForce.get(maxForce)
        fixedDistance = limits.fixedDistance.get(fixedDistance)
    }
}