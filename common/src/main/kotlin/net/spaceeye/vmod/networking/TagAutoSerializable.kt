package net.spaceeye.vmod.networking

import com.fasterxml.jackson.annotation.JsonIgnore
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.ListTag
import net.spaceeye.vmod.networking.TagSerializableItem.typeToDelegate
import net.spaceeye.vmod.utils.*
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.joml.Quaterniond
import java.awt.Color
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

open class TagSerializableItemDelegate <T : Any>(
    var serializationPos: Int,
    var it: T?,
    var serialize: TagSerializeFn,
    var deserialize: TagDeserializeFn<T>) {

    lateinit var cachedName: String
    open operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
        return it!!
    }

    open operator fun setValue(thisRef: Any?, property: KProperty<*>?, value: Any) {
        it = value as T
    }

    open operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): TagSerializableItemDelegate<T> {
        cachedName = property.name
        return this
    }
}

interface TagSerializable {
    fun tSerialize(): CompoundTag
    fun tDeserialize(tag: CompoundTag)

    fun getBuffer() = CompoundTag()
}

/**
 * An interface that will add functionality for semi-automatic serialization/deserialization
 *
 * ### If added to a normal class:
 *
 * To mark parameter for automatic ser/deser will need to use delegate provided by [get][TagSerializableItem.get] like
 *
 * ```kotlin
 * class Test(): TagAutoSerializable {
 *  @JsonIgnore private var i = 0
 *
 *  val item1: Int by get(i++, 10)
 *  val item2: Double by get(i++, 20.0, {min(it, 10.0)})
 *  val item3: String by get(i++, "Hello", {it + " World"})
 *  val complexItem: SomeComplexType by get(i++,
 *      SomeComplexType.default(),
 *      {it, buf -> it.serialize(buf)},
 *      {buf -> SomeComplexType.default().deserialize(buf)})
 * }
 * ```
 *
 * ### If added to a data class:
 *
 * With data class you can just use registered types directly, without using [get][TagSerializableItem.get], though you can still use it for complex types
 * ```kotlin
 * data class Test(
 *  val item1: Int = 10,
 *  val item2: Double = 20.0,
 *  val item3: String = "Hello World"
 * ): TagAutoSerializable {
 * val complexItem: SomeComplexType by get(0,
 *     SomeComplexType.default(),
 *     {it, buf -> it.serialize(buf)},
 *     {buf -> SomeComplexType.default().deserialize(buf)})
 * }
 * ```
 */
interface TagAutoSerializable: TagSerializable {
    @JsonIgnore
    @NonExtendable
    fun getSerializableItems(): List<TagSerializableItemDelegate<*>> {
        val toReturn = mutableListOf<TagSerializableItemDelegate<*>>()

        val constrNames: MutableSet<String> = mutableSetOf()
        if (this::class.isData) {
            val order = this::class.primaryConstructor?.parameters ?: listOf()
            val orderNames = order.map { it.name!! }
            val members = orderNames.map { item -> this::class.memberProperties.find { it.name == item }!! }
            val delegates = members.mapNotNull { it ->
                val clazz = it.returnType.jvmErasure
                typeToDelegate[clazz]?.invoke(-1, it.call(this)!!)
            }

            toReturn.addAll(delegates)
            constrNames.addAll(orderNames)
        }

        val memberProperties = this::class.memberProperties.filter {
            !constrNames.contains(it.name)
        }.mapNotNull { item ->
            val javaField = item.javaField
            if (javaField == null || !TagSerializableItemDelegate::class.java.isAssignableFrom(javaField.type)) return@mapNotNull null

            javaField.isAccessible = true
            val delegate = javaField.get(this) as TagSerializableItemDelegate<*>

            delegate
        }.sortedBy { it.serializationPos }

        toReturn.addAll(memberProperties)

        return toReturn
    }

    @JsonIgnore
    @NonExtendable
    fun getDeserializableItems(): List<TagSerializableItemDelegate<*>> {
        val order = ( if (this::class.isData) this::class.primaryConstructor?.parameters?.map { it.name }?.toSet() else null) ?: setOf()
        return this::class.memberProperties.filter {
            !order.contains(it.name)
        }.mapNotNull { item ->
            val javaField = item.javaField
            if (javaField == null || !TagSerializableItemDelegate::class.java.isAssignableFrom(javaField.type)) return@mapNotNull null

            javaField.isAccessible = true
            val delegate = javaField.get(this) as TagSerializableItemDelegate<*>

            delegate
        }.sortedBy { it.serializationPos }
    }

    @JsonIgnore
    @NonExtendable
    override fun tSerialize(): CompoundTag {
        val buf = getBuffer()
        getSerializableItems().forEach { it.serialize(it.it!!, buf, it.cachedName) }
        return buf
    }

    @JsonIgnore
    @NonExtendable
    override fun tDeserialize(tag: CompoundTag) {
        getDeserializableItems().forEach { it.setValue(null, null, it.deserialize(tag, it.cachedName)) }
    }

    @JsonIgnore
    override fun getBuffer(): CompoundTag {
        return super.getBuffer()
    }
}

typealias TagSerializeFn = (it: Any, tag: CompoundTag, key: String) -> Unit
typealias TagDeserializeFn<T> = (tag: CompoundTag, key: String) -> T

object TagSerializableItem {
    val typeToDelegate = mutableMapOf<KClass<*>, (pos: Int, default: Any) -> TagSerializableItemDelegate<*>>()
    val typeToFns = mutableMapOf<KClass<*>, Pair<TagSerializeFn, TagDeserializeFn<*>>>()

    @JvmStatic fun <T: Any> registerSerializationItem(
        type: KClass<T>,
        serialize: ((it: T, buf: CompoundTag, key: String) -> Unit),
        deserialize: ((buf: CompoundTag, key: String) -> T)) {
        serialize as (it: Any, buf: CompoundTag, key: String) -> Unit
        typeToDelegate[type] = { pos: Int, default: Any ->
            TagSerializableItemDelegate(pos, default, serialize, deserialize) }
        typeToFns[type] = Pair(serialize, deserialize)
    }

    @JvmStatic fun <T: Enum<*>> registerSerializationEnum(type: KClass<T>) {
        registerSerializationItem(type, {it, buf, key -> buf.putString(key, it.name) }, {buf, key -> java.lang.Enum.valueOf(type.java as Class<out Enum<*>>, buf.getString(key)) as T })
    }

    init {
        registerSerializationItem(Quaterniond::class, {it, buf, key -> buf.putQuatd(key, it)}) {buf, key -> buf.getQuatd(key)!!}
        registerSerializationItem(Vector3d::class, {it, buf, key -> buf.putMyVector3d(key, it)}) {buf, key -> buf.getMyVector3d(key)}
        registerSerializationItem(BlockPos::class, {it, buf, key -> buf.putLong(key, it.asLong())}) { buf, key -> BlockPos.of(buf.getLong(key))}
        registerSerializationItem(ByteBuf::class, {it, buf, key -> buf.putByteArray(key, it.array())}) {buf, key -> Unpooled.wrappedBuffer(buf.getByteArray(key))}
        registerSerializationItem(Boolean::class, {it, buf, key -> buf.putBoolean(key, it)}) {buf, key -> buf.getBoolean(key)}
        registerSerializationItem(Double::class, {it, buf, key -> buf.putDouble(key, it)}) {buf, key -> buf.getDouble(key)}
        registerSerializationItem(String::class, {it, buf, key -> buf.putString(key, it)}) {buf, key -> buf.getString(key)}
        registerSerializationItem(Color::class, {it, buf, key -> buf.putColor(key, it)}) {buf, key -> buf.getColor(key)}
        registerSerializationItem(Float::class, {it, buf, key -> buf.putFloat(key, it)}) {buf, key -> buf.getFloat(key)}
        registerSerializationItem(Long::class, {it, buf, key -> buf.putLong(key, it)}) {buf, key -> buf.getLong(key)}
        registerSerializationItem(UUID::class, {it, buf, key -> buf.putUUID(key, it)}) {buf, key -> buf.getUUID(key)}
        registerSerializationItem(Int::class, {it, buf, key -> buf.putInt(key, it)}) {buf, key -> buf.getInt(key)}

        registerSerializationItem(IntArray::class, {it, buf, key -> buf.putIntArray(key, it)}, {buf, key -> buf.getIntArray(key)})
        registerSerializationItem(LongArray::class, {it, buf, key -> buf.putLongArray(key, it)}, {buf, key -> buf.getLongArray(key)})
        registerSerializationItem(FloatArray::class, {it, buf, key -> val tag = ListTag(); tag.addAll(it.map { FloatTag.valueOf(it) }); buf.put(key, tag) }, {buf, key -> (buf.get(key) as ListTag).map { (it as FloatTag).asFloat }.toFloatArray()})
        registerSerializationItem(DoubleArray::class, {it, buf, key -> val tag = ListTag(); tag.addAll(it.map { DoubleTag.valueOf(it) }); buf.put(key, tag) }, {buf, key -> (buf.get(key) as ListTag).map { (it as DoubleTag).asDouble }.toDoubleArray()})
    }

    /**
     * Should be used in the body of the [TagAutoSerializable] class
     */
    @JvmStatic fun <T: Any> get(pos: Int, default: T,
                                customSerialize: ((it: T, buf: CompoundTag, key: String) -> Unit)? = null,
                                customDeserialize: ((buf: CompoundTag, key: String) -> T)? = null): TagSerializableItemDelegate<T> {
        customSerialize as ((Any, CompoundTag, String) -> Unit)?

        val res = typeToDelegate[default::class]
        if (res != null) return res.invoke(pos, default) as TagSerializableItemDelegate<T>


        when (default) {
            is Enum<*> -> {
                val enumClass = default.javaClass as Class<out Enum<*>>
                return TagSerializableItemDelegate(pos, default, {it, buf, key -> buf.putString(key, (it as Enum<*>).name) }, {buf, key -> java.lang.Enum.valueOf(enumClass, buf.getString(key)) as T })
            }
        }

        return TagSerializableItemDelegate(pos, default, customSerialize!!, customDeserialize!!)
    }
}