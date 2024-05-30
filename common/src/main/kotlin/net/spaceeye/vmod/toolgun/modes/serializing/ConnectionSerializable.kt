package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.constraintsManaging.types.ConnectionMConstraint
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.ConnectionMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementAssistSerialize

interface ConnectionSerializable: MSerializable, PlacementAssistSerialize {
    override fun serialize(): FriendlyByteBuf {
        this as ConnectionMode
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeEnum(posMode)
        buf.writeDouble(width)
        buf.writeEnum(connectionMode)

        buf.writeBoolean(primaryFirstRaycast)

        paSerialize(buf)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as ConnectionMode
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
        connectionMode = buf.readEnum(connectionMode.javaClass)

        primaryFirstRaycast = buf.readBoolean()

        paDeserialize(buf)
    }

    override fun serverSideVerifyLimits() {
        this as ConnectionMode
        val limits = ServerLimits.instance
        compliance = limits.compliance.get(compliance)
        maxForce = limits.maxForce.get(maxForce)
        fixedDistance = limits.fixedDistance.get(fixedDistance)
        paServerSideVerifyLimits()
    }
}