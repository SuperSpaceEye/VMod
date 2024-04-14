package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.HydraulicsMode
import net.spaceeye.vmod.toolgun.modes.util.PlacementAssistSerialize

interface HydraulicsSerializable: MSerializable, PlacementAssistSerialize {
    override fun serialize(): FriendlyByteBuf {
        this as HydraulicsMode
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeEnum(posMode)
        buf.writeDouble(width)
        buf.writeDouble(extensionDistance)
        buf.writeDouble(extensionSpeed)
        buf.writeUtf(channel)
        buf.writeEnum(messageModes)

        buf.writeBoolean(primaryFirstRaycast)

        paSerialize(buf)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as HydraulicsMode
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()
        extensionDistance = buf.readDouble()
        extensionSpeed = buf.readDouble()
        channel = buf.readUtf()
        messageModes = buf.readEnum(messageModes.javaClass)

        primaryFirstRaycast = buf.readBoolean()

        paDeserialize(buf)
    }

    override fun serverSideVerifyLimits() {
        this as HydraulicsMode
        compliance = ServerLimits.instance.compliance.get(compliance)
        maxForce = ServerLimits.instance.maxForce.get(maxForce)
        extensionDistance = ServerLimits.instance.extensionDistance.get(extensionDistance)
        extensionSpeed = ServerLimits.instance.extensionSpeed.get(extensionSpeed)
        channel = ServerLimits.instance.channelLength.get(channel)
        paServerSideVerifyLimits()
    }
}