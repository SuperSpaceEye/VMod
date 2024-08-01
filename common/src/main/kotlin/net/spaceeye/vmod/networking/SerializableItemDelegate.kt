package net.spaceeye.vmod.networking

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.utils.*
import java.awt.Color
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

open class SerializableItemDelegate <T : Any>(
    var serializationPos: Int,
    var it: T,
    var verification: (it: Any) -> T,
    var serialize: (it: Any, buf: FriendlyByteBuf) -> Unit,
    var deserialize: (buf: FriendlyByteBuf) -> T) {

    lateinit var cachedName: String
    operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
        return it
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>?, value: Any) {
        it = value as T
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): SerializableItemDelegate<T> {
        cachedName = property.name
        return this
    }
}

interface AutoSerializable: Serializable {
    fun getSerializableItems() =
        this::class.declaredMemberProperties.mapNotNull { item ->
            val javaField = item.javaField
            if (javaField == null || !SerializableItemDelegate::class.java.isAssignableFrom(javaField.type)) return@mapNotNull null

            javaField.isAccessible = true
            val delegate = javaField.get(this) as SerializableItemDelegate<*>

            delegate
        }.sortedBy { it.serializationPos }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()
        getSerializableItems().forEach { it.serialize(it.it, buf) }
        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        getSerializableItems().forEach { it.setValue(null, null, it.deserialize(buf)) }
    }
}

object SerializableItem {
    private val typeToDelegate = mutableMapOf<KClass<*>, (pos: Int, default: Any, verification: ((it: Any) -> Any)) -> SerializableItemDelegate<*>>()

    @JvmStatic fun <T: Any> registerSerializationItem(
        type: KClass<*>,
        serialize: ((it: Any, buf: FriendlyByteBuf) -> Unit),
        deserialize: ((buf: FriendlyByteBuf) -> T)) {
        typeToDelegate[type] = { pos: Int, default: Any, verification: (it: Any) -> Any ->
            SerializableItemDelegate(pos, default, verification, serialize, deserialize) }
    }

    init {
        registerSerializationItem(Vector3d::class, {it, buf -> buf.writeVector3d(it as Vector3d)}) {buf -> buf.readVector3d()}
        registerSerializationItem(Boolean::class, {it, buf -> buf.writeBoolean(it as Boolean)}) {buf -> buf.readBoolean()}
        registerSerializationItem(Double::class, {it, buf -> buf.writeDouble(it as Double)}) {buf -> buf.readDouble()}
        registerSerializationItem(String::class, {it, buf -> buf.writeUtf(it as String)}) {buf -> buf.readUtf()}
        registerSerializationItem(Color::class, {it, buf -> buf.writeColor(it as Color)}) {buf -> buf.readColor()}
        registerSerializationItem(Float::class, {it, buf -> buf.writeFloat(it as Float)}) {buf -> buf.readFloat()}
        registerSerializationItem(Int::class, {it, buf -> buf.writeInt(it as Int)}) {buf -> buf.readInt()}
    }

    @JvmStatic fun <T: Any> get(pos: Int, default: T,
                                verification: ((it: Any) -> T) = {it as T},
                                customSerialize: ((it: Any, buf: FriendlyByteBuf) -> Unit)? = null,
                                customDeserialize: ((buf: FriendlyByteBuf) -> T)? = null): SerializableItemDelegate<T> {
        val res = typeToDelegate[default::class]
        if (res != null) return res.invoke(pos, default, verification) as SerializableItemDelegate<T>

        if (default is Enum<*>) {
            val enumClass = default.javaClass as Class<out Enum<*>>;
            return SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeEnum(it as Enum<*>)}) {buf -> buf.readEnum(enumClass) as T }
        }

        return SerializableItemDelegate(pos, default, verification, customSerialize!!, customDeserialize!!)
    }
}