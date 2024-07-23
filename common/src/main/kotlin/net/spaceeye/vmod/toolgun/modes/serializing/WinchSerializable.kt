//package net.spaceeye.vmod.toolgun.modes.serializing
//
//import net.minecraft.network.FriendlyByteBuf
//import net.spaceeye.vmod.limits.ServerLimits
//import net.spaceeye.vmod.toolgun.modes.MSerializable
//import net.spaceeye.vmod.toolgun.modes.state.WinchMode
//import net.spaceeye.vmod.toolgun.modes.util.PlacementAssistSerialize
//import net.spaceeye.vmod.utils.readColor
//import net.spaceeye.vmod.utils.writeColor
//
//interface WinchSerializable: MSerializable, PlacementAssistSerialize {
//    override fun serialize(): FriendlyByteBuf {
//        this as WinchMode
//        val buf = getBuffer()
//
//        buf.writeDouble(compliance)
//        buf.writeDouble(maxForce)
//        buf.writeEnum(posMode)
//        buf.writeDouble(width)
//        buf.writeDouble(extensionDistance)
//        buf.writeDouble(extensionSpeed)
//        buf.writeUtf(channel)
//        buf.writeEnum(messageModes)
//        buf.writeColor(color)
//        buf.writeDouble(fixedMinLength)
//
//        buf.writeBoolean(primaryFirstRaycast)
//
//        paSerialize(buf)
//
//        return buf
//    }
//
//    override fun deserialize(buf: FriendlyByteBuf) {
//        this as WinchMode
//        compliance = buf.readDouble()
//        maxForce = buf.readDouble()
//        posMode = buf.readEnum(posMode.javaClass)
//        width = buf.readDouble()
//        extensionDistance = buf.readDouble()
//        extensionSpeed = buf.readDouble()
//        channel = buf.readUtf()
//        messageModes = buf.readEnum(messageModes.javaClass)
//        color = buf.readColor()
//        fixedMinLength = buf.readDouble()
//
//        primaryFirstRaycast = buf.readBoolean()
//
//        paDeserialize(buf)
//    }
//
//    override fun serverSideVerifyLimits() {
//        this as WinchMode
//        val limits = ServerLimits.instance
//
//        compliance = limits.compliance.get(compliance)
//        maxForce = limits.maxForce.get(maxForce)
//        extensionDistance = limits.extensionDistance.get(extensionDistance)
//        extensionSpeed = limits.extensionSpeed.get(extensionSpeed)
//        channel = limits.channelLength.get(channel)
//        paServerSideVerifyLimits()
//    }
//}