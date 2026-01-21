package net.spaceeye.vmod.vEntityManaging.extensions

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.network.AngularSpeedSignal
import net.spaceeye.vmod.network.Message
import net.spaceeye.vmod.network.MessagingNetwork
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import net.spaceeye.vmod.vEntityManaging.util.VEntityExtension
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class MotorSignalActivator(): VEntityExtension {
    //...ParameterName
    var channelPN: String = ""
    var angularSpeedPN: String = ""

    constructor(channelPN: String, angularSpeedPN: String): this() {
        this.channelPN = channelPN
        this.angularSpeedPN = angularSpeedPN
    }

    lateinit var obj: ExtendableVEntity

    var channelProperty: KProperty1<out Any, String>? = null
        get() {
            if (field == null) { field = obj::class.memberProperties.find { it.name == channelPN } as KProperty1<out Any, String>? }
            return field
        }
    var angularSpeedProperty: KMutableProperty1<out Any, Double>? = null
        get() {
            if (field == null) { field = obj::class.memberProperties.find { it.name == angularSpeedPN } as KMutableProperty1<out Any, Double>?
            }
            return field
        }

    private var wasDeleted = false

    override fun onInit(obj: ExtendableVEntity) {
        if (obj !is IMotor) throw AssertionError("Object doesn't inherit from IMotor")
        this.obj = obj
    }

    private fun signalTick(msg: Message) {
        if (msg !is AngularSpeedSignal) return
        angularSpeedProperty!!.setter.call(obj, msg.angularSpeed)
    }

    override fun onAfterCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>, new: ExtendableVEntity) {
        new.addExtension(MotorSignalActivator(channelPN, angularSpeedPN))
    }

    override fun onSerialize(): CompoundTag? {
        return CompoundTag().also {
            it.putString("channelPN", channelPN)
            it.putString("angularSpeedPN", angularSpeedPN)
        }
    }

    override fun onDeserialize(tag: CompoundTag): Boolean {
        channelPN = tag.getString("channelPN")
        angularSpeedPN = tag.getString("angularSpeedPN")
        return true
    }

    override fun onMakeVEntity(level: ServerLevel) {
        val obj = obj as ReflectableObject

        MessagingNetwork.register(channelProperty!!.call(obj)) {
            msg, unregister ->
            if (wasDeleted) {unregister()}
            signalTick(msg)
        }
    }

    override fun onDeleteVEntity(level: ServerLevel) {
        wasDeleted = true
    }
}