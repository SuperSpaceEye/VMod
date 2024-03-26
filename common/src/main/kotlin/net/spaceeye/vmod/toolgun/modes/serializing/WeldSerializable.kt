package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.WeldMode

interface WeldSerializable: MSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as WeldMode
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)
        buf.writeEnum(posMode)
        buf.writeDouble(width)

        buf.writeEnum(secondaryStage)
        buf.writeBoolean(primaryFirstRaycast)
        buf.writeDouble(secondaryAngle.it)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as WeldMode
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
        posMode = buf.readEnum(posMode.javaClass)
        width = buf.readDouble()

        primaryFirstRaycast = buf.readBoolean()

        secondaryStage = buf.readEnum(secondaryStage.javaClass)
        secondaryAngle.it = buf.readDouble()
    }

    override fun serverSideVerifyLimits() {
        this as WeldMode
        compliance = ServerLimits.instance.compliance.get(compliance)
        maxForce = ServerLimits.instance.maxForce.get(maxForce)
        fixedDistance = ServerLimits.instance.fixedDistance.get(fixedDistance)
    }
}