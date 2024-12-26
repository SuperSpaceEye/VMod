package net.spaceeye.vmod.constraintsManaging.extensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.constraintsManaging.util.ExtendableMConstraint
import net.spaceeye.vmod.constraintsManaging.util.MConstraintExtension
import net.spaceeye.vmod.constraintsManaging.util.TickableMConstraintExtension
import net.spaceeye.vmod.network.Message
import net.spaceeye.vmod.network.MessagingNetwork
import net.spaceeye.vmod.network.Signal
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.lang.reflect.Field
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class SignalActivator(): MConstraintExtension, TickableMConstraintExtension {
    lateinit var channelNameReflection: String
    lateinit var percentageNameReflection: String

    constructor(channelNameReflection: String, percentageNameReflection: String): this() {
        this.channelNameReflection = channelNameReflection
        this.percentageNameReflection = percentageNameReflection
    }

    var targetPercentage = 0.0

    var totalPercentage = 0.0
    var numMessages = 0

    private var wasDeleted = false

    private lateinit var percentageField: Field

    private fun signalTick(msg: Message) {
        if (msg !is Signal) { return }

        totalPercentage = min(max(msg.percentage, 0.0), 1.0)
        numMessages++
    }

    override fun tick(server: MinecraftServer) {
        if (numMessages != 0) {
            targetPercentage = totalPercentage / numMessages
            numMessages = 0
            totalPercentage = 0.0
        }

        percentageField.set(obj, targetPercentage)
    }

    private lateinit var obj: ExtendableMConstraint

    override fun onInit(obj: ExtendableMConstraint) {
        this.obj = obj

        percentageField = this.obj::class.memberProperties.find { it.name == percentageNameReflection }!!.javaField!!
        percentageField.isAccessible = true
    }

    override fun onAfterCopyMConstraint(level: ServerLevel, mapped: Map<ShipId, ShipId>, new: ExtendableMConstraint) {
        new.addExtension(SignalActivator(channelNameReflection, percentageNameReflection))
    }

    override fun onSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putString("channelNameReflection", channelNameReflection)
        tag.putString("percentageNameReflection", percentageNameReflection)

        return tag
    }

    override fun onDeserialize(tag: CompoundTag, lastDimensionIds: Map<ShipId, String>): Boolean {
        channelNameReflection = tag.getString("channelNameReflection")
        percentageNameReflection = tag.getString("percentageNameReflection")

        return true
    }

    override fun onMakeMConstraint(level: ServerLevel) {
        val channel = obj::class.memberProperties.find { it.name == channelNameReflection }!!.call(obj) as String

        MessagingNetwork.register(channel) {
                msg, unregister ->
            if (wasDeleted) {unregister(); return@register}

            signalTick(msg)
        }
    }

    override fun onDeleteMConstraint(level: ServerLevel) {
        wasDeleted = true
    }
}