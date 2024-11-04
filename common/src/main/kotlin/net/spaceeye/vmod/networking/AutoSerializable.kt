package net.spaceeye.vmod.networking

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
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
        typeToFns[clazz]!!.second
    }

    val items = delegates.map { it.invoke(buf) }

    return this.primaryConstructor!!.call(*items.toTypedArray())
}

typealias SerializeFn = ((it: Any, buf: FriendlyByteBuf) -> Unit)
typealias DeserializeFn<T> = ((buf: FriendlyByteBuf) -> T)

object SerializableItem {
    val typeToDelegate = mutableMapOf<KClass<*>, (pos: Int, default: Any, verification: ((it: Any) -> Any)) -> SerializableItemDelegate<*>>()
    val typeToFns = mutableMapOf<KClass<*>, Pair<SerializeFn, DeserializeFn<*>>>()

    @JvmStatic fun <T: Any> registerSerializationItem(
        type: KClass<T>,
        serialize: ((it: T, buf: FriendlyByteBuf) -> Unit),
        deserialize: ((buf: FriendlyByteBuf) -> T)) {
        serialize as (Any, FriendlyByteBuf) -> Unit
        typeToDelegate[type] = { pos: Int, default: Any, verification: (it: Any) -> Any ->
            SerializableItemDelegate(pos, default, verification, serialize, deserialize) }
        typeToFns[type] = Pair(serialize, deserialize)
    }

    @JvmStatic fun <T: Enum<*>> registerSerializationEnum(type: KClass<T>) {
        registerSerializationItem(type, {it, buf -> buf.writeEnum(it) }, {buf -> buf.readEnum(type.java as Class<out Enum<*>>) as T })
    }

    init {
        registerSerializationItem(FriendlyByteBuf::class, {it, buf -> buf.writeByteArray(it.accessByteBufWithCorrectSize())}) {buf -> FriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray()))}
        registerSerializationItem(Quaterniond::class, {it, buf -> buf.writeQuatd(it)}) {buf -> buf.readQuatd()}
        registerSerializationItem(Vector3d::class, {it, buf -> buf.writeVector3d(it)}) {buf -> buf.readVector3d()}
        registerSerializationItem(ByteBuf::class, {it, buf -> buf.writeByteArray(it.array())}) {buf -> Unpooled.wrappedBuffer(buf.readByteArray())}
        registerSerializationItem(Boolean::class, {it, buf -> buf.writeBoolean(it)}) {buf -> buf.readBoolean()}
        registerSerializationItem(Double::class, {it, buf -> buf.writeDouble(it)}) {buf -> buf.readDouble()}
        registerSerializationItem(String::class, {it, buf -> buf.writeUtf(it)}) {buf -> buf.readUtf()}
        registerSerializationItem(Color::class, {it, buf -> buf.writeColor(it)}) {buf -> buf.readColor()}
        registerSerializationItem(Float::class, {it, buf -> buf.writeFloat(it)}) {buf -> buf.readFloat()}
        registerSerializationItem(Long::class, {it, buf -> buf.writeLong(it)}) {buf -> buf.readLong()}
        registerSerializationItem(UUID::class, {it, buf -> buf.writeUUID(it)}) {buf -> buf.readUUID()}
        registerSerializationItem(Int::class, {it, buf -> buf.writeInt(it)}) {buf -> buf.readInt()}

        registerSerializationItem(IntArray::class, {it, buf -> buf.writeCollection(it.asList()){buf, it -> buf.writeInt(it)}}) {buf -> buf.readCollection({mutableListOf<Int>()}) {buf.readInt()}.toIntArray()}
        registerSerializationItem(LongArray::class, {it, buf -> buf.writeCollection(it.asList()){buf, it -> buf.writeLong(it)}}) {buf -> buf.readCollection({mutableListOf<Long>()}) {buf.readInt()}.toLongArray()}
        registerSerializationItem(FloatArray::class, {it, buf -> buf.writeCollection(it.asList()){buf, it -> buf.writeFloat(it)}}) {buf -> buf.readCollection({mutableListOf<Float>()}) {buf.readInt()}.toFloatArray()}
        registerSerializationItem(DoubleArray::class, {it, buf -> buf.writeCollection(it.asList()){buf, it -> buf.writeDouble(it)}}) {buf -> buf.readCollection({mutableListOf<Double>()}) {buf.readInt()}.toDoubleArray()}
    }

    /**
     * Should be used in the body of the AutoSerializable class
     */
    @JvmStatic fun <T: Any> get(pos: Int, default: T,
                                verification: ((it: T) -> T) = {it},
                                customSerialize: ((it: T, buf: FriendlyByteBuf) -> Unit)? = null,
                                customDeserialize: ((buf: FriendlyByteBuf) -> T)? = null): SerializableItemDelegate<T> {
        verification as (Any) -> T
        customSerialize as ((Any, FriendlyByteBuf) -> Unit)?

        val res = typeToDelegate[default::class]
        if (res != null) return res.invoke(pos, default, verification) as SerializableItemDelegate<T>


        when (default) {
            is Enum<*> -> {
                val enumClass = default.javaClass as Class<out Enum<*>>
                return SerializableItemDelegate(pos, default, verification, {it, buf -> buf.writeEnum(it as Enum<*>)}) {buf -> buf.readEnum(enumClass) as T }
            }
        }

        return SerializableItemDelegate(pos, default, verification, customSerialize!!, customDeserialize!!)
    }
}