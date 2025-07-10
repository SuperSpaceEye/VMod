package net.spaceeye.vmod.vEntityManaging.extensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import net.spaceeye.vmod.vEntityManaging.util.VEntityExtension
import net.spaceeye.vmod.vEntityManaging.util.TickableVEntityExtension
import net.spaceeye.vmod.network.Message
import net.spaceeye.vmod.network.MessagingNetwork
import net.spaceeye.vmod.network.Signal
import net.spaceeye.vmod.utils.Vector3d
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.lang.reflect.Field
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class SignalActivator(): VEntityExtension, TickableVEntityExtension {
    lateinit var channelNameReflection: String
    lateinit var percentageNameReflection: String

    constructor(channelNameReflection: String, percentageNameReflection: String): this() {
        this.channelNameReflection = channelNameReflection
        this.percentageNameReflection = percentageNameReflection
    }

    var targetPercentage = 0.0f

    var totalPercentage = 0.0f
    var numMessages = 0

    private var wasDeleted = false

    private lateinit var percentageField: Field

    private fun signalTick(msg: Message) {
        if (msg !is Signal) { return }

        totalPercentage = min(max(msg.percentage.toFloat(), 0.0f), 1.0f)
        numMessages++
    }

    override fun tick(server: MinecraftServer) {
        if (numMessages != 0) {
            targetPercentage = totalPercentage / numMessages
            numMessages = 0
            totalPercentage = 0.0f
        }

        percentageField.set(obj, targetPercentage)
    }

    private lateinit var obj: ExtendableVEntity

    override fun onInit(obj: ExtendableVEntity) {
        this.obj = obj

        percentageField = this.obj::class.memberProperties.find { it.name == percentageNameReflection }!!.javaField!!
        percentageField.isAccessible = true
    }

    override fun onAfterCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>, new: ExtendableVEntity) {
        new.addExtension(SignalActivator(channelNameReflection, percentageNameReflection))
    }

    override fun onSerialize(): CompoundTag? {
        val tag = CompoundTag()

        tag.putString("channelNameReflection", channelNameReflection)
        tag.putString("percentageNameReflection", percentageNameReflection)

        return tag
    }

    override fun onDeserialize(tag: CompoundTag): Boolean {
        channelNameReflection = tag.getString("channelNameReflection")
        percentageNameReflection = tag.getString("percentageNameReflection")

        return true
    }

    override fun onMakeVEntity(level: ServerLevel) {
        val channel = obj::class.memberProperties.find { it.name == channelNameReflection }!!.call(obj) as String

        MessagingNetwork.register(channel) {
                msg, unregister ->
            if (wasDeleted) {unregister(); return@register}

            signalTick(msg)
        }
    }

    override fun onDeleteVEntity(level: ServerLevel) {
        wasDeleted = true
    }
}