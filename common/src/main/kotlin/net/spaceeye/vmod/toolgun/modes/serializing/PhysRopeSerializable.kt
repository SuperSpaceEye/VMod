package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.PhysRopeMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementModesSerializable

interface PhysRopeSerializable: MSerializable, PlacementModesSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as PhysRopeMode

        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeDouble(fixedDistance)
        buf.writeInt(segments)
        buf.writeDouble(massPerSegment)
        buf.writeDouble(radius)

        buf.writeBoolean(primaryFirstRaycast)

        pmSerialize(buf)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as PhysRopeMode

        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        fixedDistance = buf.readDouble()
        segments = buf.readInt()
        massPerSegment = buf.readDouble()
        radius = buf.readDouble()

        primaryFirstRaycast = buf.readBoolean()

        pmDeserialize(buf)
    }

    override fun serverSideVerifyLimits() {
        this as PhysRopeMode
        val limits = ServerLimits.instance

        compliance = limits.compliance.get(compliance)
        maxForce = limits.maxForce.get(maxForce)
        fixedDistance = limits.fixedDistance.get(fixedDistance)
        massPerSegment = limits.physRopeMassPerSegment.get(massPerSegment)
        radius = limits.physRopeRadius.get(radius)
        segments = limits.physRopeSegments.get(segments)

        pmServerSideVerifyLimits()
    }
}