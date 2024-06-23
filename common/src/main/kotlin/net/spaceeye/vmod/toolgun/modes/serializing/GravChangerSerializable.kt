package net.spaceeye.vmod.toolgun.modes.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.toolgun.modes.MSerializable
import net.spaceeye.vmod.toolgun.modes.state.GravChangerMode
import net.spaceeye.vmod.utils.readVector3d
import net.spaceeye.vmod.utils.writeVector3d

interface GravChangerSerializable: MSerializable {
    override fun serialize(): FriendlyByteBuf {
        this as GravChangerMode
        val buf = getBuffer()

        buf.writeVector3d(gravityVector)
        buf.writeEnum(mode)

        return buf
    }
    override fun deserialize(buf: FriendlyByteBuf) {
        this as GravChangerMode

        gravityVector = buf.readVector3d()
        mode = buf.readEnum(mode.javaClass)

    }
    override fun serverSideVerifyLimits() {
        this as GravChangerMode
    }
}