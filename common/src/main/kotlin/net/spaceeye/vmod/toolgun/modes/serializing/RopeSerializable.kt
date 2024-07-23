package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.RopeMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesSerializable

interface RopeSerializable: MSerializable, PlacementModesSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as RopeMode

        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeDouble(fixedDistance)
        buf.writeDouble(width)
        buf.writeInt(segments)

        buf.writeBoolean(primaryFirstRaycast)

        pmSerialize(buf)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as RopeMode

        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        fixedDistance = buf.readDouble()
        width = buf.readDouble()
        segments = buf.readInt()

        primaryFirstRaycast = buf.readBoolean()

        pmDeserialize(buf)
    }

    override fun serverSideVerifyLimits() {
        this as RopeMode
        val limits = ServerLimits.instance

        compliance = limits.compliance.get(compliance)
        maxForce = limits.maxForce.get(maxForce)
        fixedDistance = limits.fixedDistance.get(fixedDistance)

        pmServerSideVerifyLimits()
    }
}