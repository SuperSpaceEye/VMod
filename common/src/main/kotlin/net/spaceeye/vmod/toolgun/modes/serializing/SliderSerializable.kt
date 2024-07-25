package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.SliderMode

interface SliderSerializable: MSerializable {
    override fun serverSideVerifyLimits() {}

    override fun serialize(): FriendlyByteBuf {
        this as SliderMode
        val buf = getBuffer()

        buf.writeEnum(posMode)
        buf.writeInt(precisePlacementAssistSideNum)
        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        this as SliderMode
        posMode = buf.readEnum(posMode.javaClass)
        precisePlacementAssistSideNum = buf.readInt()
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
    }
}