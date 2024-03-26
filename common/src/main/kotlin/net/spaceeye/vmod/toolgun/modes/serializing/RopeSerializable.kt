package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.ModeSerializable
import net.spaceeye.vmod.toolgun.modes.state.RopeMode

interface RopeSerializable: ModeSerializable {
    override fun serverSideVerifyLimits() {
        this as RopeMode
        compliance = ServerLimits.instance.compliance.get(compliance)
        maxForce = ServerLimits.instance.maxForce.get(maxForce)
        fixedDistance = ServerLimits.instance.fixedDistance.get(fixedDistance)
    }

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
}