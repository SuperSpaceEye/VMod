package net.spaceeye.vmod.toolgun.serializing

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.utils.*
import java.awt.Color
import kotlin.reflect.KProperty

open class SerializableItemDelegate <T : Any>(
    var serializationPos: Int,
    var it: T,
    var verification: (it: Any) -> T,
    var serialize: (it: Any, buf: FriendlyByteBuf) -> Unit,
    var deserialize: (buf: FriendlyByteBuf) -> T) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
        return it
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>?, value: Any) {
        it = value as T
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): SerializableItemDelegate<T> {
        return this
    }
}

object SerializableItem {
    @JvmStatic fun <T: Any> get(pos: Int, default: T,
                                verification: ((it: Any) -> T) = {it as T},
                                customSerialize: ((it: Any, buf: FriendlyByteBuf) -> Unit)? = null,
                                customDeserialize: ((buf: FriendlyByteBuf) -> T)? = null): SerializableItemDelegate<T> {
        return when(default) {
            is Vector3d -> SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeVector3d(it as Vector3d)}) {buf -> buf.readVector3d() as T}
            is Boolean -> SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeBoolean(it as Boolean)}) {buf -> buf.readBoolean() as T}
            is Enum<*> -> {val enumClass = default.javaClass as Class<out Enum<*>>; SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeEnum(it as Enum<*>)}) {buf -> buf.readEnum(enumClass) as T }}
            is Double -> SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeDouble(it as Double)}) {buf -> buf.readDouble() as T}
            is String -> SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeUtf(it as String)}) {buf -> buf.readUtf() as T}
            is Color -> SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeColor(it as Color)}) {buf -> buf.readColor() as T}
            is Float -> SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeFloat(it as Float)}) {buf -> buf.readFloat() as T}
            is Int -> SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeInt(it as Int)}) {buf -> buf.readInt() as T}

            else -> SerializableItemDelegate(pos, default, verification, customSerialize!!, customDeserialize!!)
        }
    }
}