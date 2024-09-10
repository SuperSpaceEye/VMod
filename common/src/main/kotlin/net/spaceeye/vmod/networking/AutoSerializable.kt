package net.spaceeye.vmod.networking

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.SerializableItem.typeToDelegate
import net.spaceeye.vmod.networking.SerializableItem.typeToFns
import net.spaceeye.vmod.utils.*
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.joml.Quaterniond
import org.valkyrienskies.core.util.readQuatd
import org.valkyrienskies.core.util.writeQuatd
import java.awt.Color
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

open class SerializableItemDelegate <T : Any>(
    var serializationPos: Int,
    var it: T?,
    var verification: (it: Any) -> T,
    var serialize: (it: Any, buf: FriendlyByteBuf) -> Unit,
    var deserialize: (buf: FriendlyByteBuf) -> T) {

    lateinit var cachedName: String
    open operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
        return it!!
    }

    open operator fun setValue(thisRef: Any?, property: KProperty<*>?, value: Any) {
        it = value as T
    }

    open operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): SerializableItemDelegate<T> {
        cachedName = property.name
        return this
    }
}

/**
 * An interface that will add functionality for semi-automatic serialization/deserialization
 *
 * ### If added to a normal class:
 *
 * To mark parameter for automatic ser/deser will need to use delegate provided by [get][SerializableItem.get] like
 *
 * ```kotlin
 * class Test(): AutoSerializable {
 *  val item1: Int by get(1, 10)
 *  val item2: Double by get(2, 20.0, {min(it, 10.0)})
 *  val item3: String by get(3, "Hello", {it + " World"})
 *  val complexItem: SomeComplexType by get(4,
 *      SomeComplexType.default(),
 *      {it.verify()},
 *      {it, buf -> it.serialize(buf)},
 *      {buf -> SomeComplexType.default().deserialize(buf)})
 * }
 * ```
 *
 * ### If added to a data class:
 *
 * With data class you can just use registered types directly, without using [get][SerializableItem.get], though you can still use it for complex types
 * ```kotlin
 * data class Test(
 *  val item1: Int = 10,
 *  val item2: Double = 20.0,
 *  val item3: String = "Hello World"
 * ): AutoSerializable {
 * val complexItem: SomeComplexType by get(4,
 *     SomeComplexType.default(),
 *     {it.verify()},
 *     {it, buf -> it.serialize(buf)},
 *     {buf -> SomeComplexType.default().deserialize(buf)})
 * }
 * ```
 */
interface AutoSerializable: Serializable {
    @NonExtendable
    fun getSerializableItems(): List<SerializableItemDelegate<*>> {
        val toReturn = mutableListOf<SerializableItemDelegate<*>>()

        val constrNames: MutableSet<String> = mutableSetOf()
        if (this::class.isData) {
            val order = this::class.primaryConstructor?.parameters ?: listOf()
            val orderNames = order.map { it.name!! }
            val members = orderNames.map { item -> this::class.memberProperties.find { it.name == item }!! }
            val delegates = members.mapNotNull { it ->
                val clazz = it.returnType.jvmErasure
                typeToDelegate[clazz]?.invoke(-1, it.call(this)!!) { it }
            }

            toReturn.addAll(delegates)
            constrNames.addAll(orderNames)
        }

        val memberProperties = this::class.memberProperties.filter {
            !constrNames.contains(it.name)
        }.mapNotNull { item ->
            val javaField = item.javaField
            if (javaField == null || !SerializableItemDelegate::class.java.isAssignableFrom(javaField.type)) return@mapNotNull null

            javaField.isAccessible = true
            val delegate = javaField.get(this) as SerializableItemDelegate<*>

            delegate
        }.sortedBy { it.serializationPos }

        toReturn.addAll(memberProperties)

        return toReturn
    }

    @NonExtendable
    fun getDeserializableItems(): List<SerializableItemDelegate<*>> {
        val order = ( if (this::class.isData) this::class.primaryConstructor?.parameters?.map { it.name }?.toSet() else null) ?: setOf()
        return this::class.memberProperties.filter {
            !order.contains(it.name)
        }.mapNotNull { item ->
            val javaField = item.javaField
            if (javaField == null || !SerializableItemDelegate::class.java.isAssignableFrom(javaField.type)) return@mapNotNull null

            javaField.isAccessible = true
            val delegate = javaField.get(this) as SerializableItemDelegate<*>

            delegate
        }.sortedBy { it.serializationPos }
    }

    @NonExtendable
    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()
        getSerializableItems().forEach { it.serialize(it.it!!, buf) }
        return buf
    }

    @NonExtendable
    override fun deserialize(buf: FriendlyByteBuf) {
        getDeserializableItems().forEach { it.setValue(null, null, it.deserialize(buf)) }
    }
}
fun <T: Serializable> KClass<T>.constructor(buf: FriendlyByteBuf? = null): T {
    if (!this.isData) {
        return this.primaryConstructor!!.call()
    }

    val order = this.primaryConstructor?.parameters ?: listOf()
    if (order.isEmpty()) throw AssertionError("Impossible Situation. Dataclass has no members.")
    if (buf == null) {
        return this.primaryConstructor!!.call()
    }

    val members = order.map {item -> this.memberProperties.find { it.name == item.name }!! }

    val delegates = members.map {
        val clazz = it.returnType.jvmErasure
        typeToFns[clazz]!!.i2
    }

    val items = delegates.map { it.invoke(buf) }

    return this.primaryConstructor!!.call(*items.toTypedArray())
}

typealias SerializeFn = ((it: Any, buf: FriendlyByteBuf) -> Unit)
typealias DeserializeFn<T> = ((buf: FriendlyByteBuf) -> T)

object SerializableItem {
    val typeToDelegate = mutableMapOf<KClass<*>, (pos: Int, default: Any, verification: ((it: Any) -> Any)) -> SerializableItemDelegate<*>>()
    val typeToFns = mutableMapOf<KClass<*>, Tuple2<SerializeFn, DeserializeFn<*>>>()

    @JvmStatic fun <T: Any> registerSerializationItem(
        type: KClass<*>,
        serialize: ((it: Any, buf: FriendlyByteBuf) -> Unit),
        deserialize: ((buf: FriendlyByteBuf) -> T)) {
        typeToDelegate[type] = { pos: Int, default: Any, verification: (it: Any) -> Any ->
            SerializableItemDelegate(pos, default, verification, serialize, deserialize) }
        typeToFns[type] = Tuple.of(serialize, deserialize)
    }

    init {
        registerSerializationItem(Quaterniond::class, {it, buf -> buf.writeQuatd(it as Quaterniond)}) {buf -> buf.readQuatd()}
        registerSerializationItem(Vector3d::class, {it, buf -> buf.writeVector3d(it as Vector3d)}) {buf -> buf.readVector3d()}
        registerSerializationItem(Boolean::class, {it, buf -> buf.writeBoolean(it as Boolean)}) {buf -> buf.readBoolean()}
        registerSerializationItem(Double::class, {it, buf -> buf.writeDouble(it as Double)}) {buf -> buf.readDouble()}
        registerSerializationItem(String::class, {it, buf -> buf.writeUtf(it as String)}) {buf -> buf.readUtf()}
        registerSerializationItem(Color::class, {it, buf -> buf.writeColor(it as Color)}) {buf -> buf.readColor()}
        registerSerializationItem(Float::class, {it, buf -> buf.writeFloat(it as Float)}) {buf -> buf.readFloat()}
        registerSerializationItem(Long::class, {it, buf -> buf.writeLong(it as Long)}) {buf -> buf.readLong()}
        registerSerializationItem(UUID::class, {it, buf -> buf.writeUUID(it as UUID)}) {buf -> buf.readUUID()}
        registerSerializationItem(Int::class, {it, buf -> buf.writeInt(it as Int)}) {buf -> buf.readInt()}
    }

    /**
     * Should be used in the body of the AutoSerializable class
     */
    @JvmStatic fun <T: Any> get(pos: Int, default: T,
                                verification: ((it: Any) -> T) = {it as T},
                                customSerialize: ((it: Any, buf: FriendlyByteBuf) -> Unit)? = null,
                                customDeserialize: ((buf: FriendlyByteBuf) -> T)? = null): SerializableItemDelegate<T> {
        val res = typeToDelegate[default::class]
        if (res != null) return res.invoke(pos, default, verification) as SerializableItemDelegate<T>


        when (default) {
            is Enum<*> -> {
                val enumClass = default.javaClass as Class<out Enum<*>>;
                return SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeEnum(it as Enum<*>)}) {buf -> buf.readEnum(enumClass) as T }
            }
        }

        return SerializableItemDelegate(pos, default, verification, customSerialize!!, customDeserialize!!)
    }

    //TODO think about it
    @JvmStatic
    inline fun <reified T: Any> get(pos: Int, default: List<T>,
                                    noinline verification: ((it: Any) -> List<T>) = {it as List<T>},
                                    noinline customSerialize: ((it: Any, buf: FriendlyByteBuf) -> Unit)? = null,
                                    noinline customDeserialize: ((buf: FriendlyByteBuf) -> List<T>)? = null): SerializableItemDelegate<List<T>> {
        val (ser, deser) = if (customSerialize != null && customDeserialize != null) { Tuple.of(customSerialize, customDeserialize) } else { typeToFns[T::class]!! }
        return SerializableItemDelegate(pos, default, verification, {it, buf ->
            it as List<*>
            buf.writeCollection(it) {buf, it -> ser(it!!, buf) }
        }, {buf ->
            buf.readCollection({ mutableListOf() }) {buf -> deser(buf) as T}
        })
    }
}