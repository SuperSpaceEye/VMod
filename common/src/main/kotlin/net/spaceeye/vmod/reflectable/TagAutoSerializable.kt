package net.spaceeye.vmod.reflectable

import com.fasterxml.jackson.annotation.JsonIgnore
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.ListTag
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.reflectable.TagSerializableItem.typeToTagSerDeser
import net.spaceeye.vmod.utils.*
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.joml.Quaterniond
import org.joml.Quaterniondc
import java.awt.Color
import java.util.UUID
import kotlin.reflect.KClass

interface TagSerializable {
    fun tSerialize(): CompoundTag
    fun tDeserialize(tag: CompoundTag)
    fun tGetBuffer() = CompoundTag()
}

/**
 * WARNING!!!! IF CLASS USES CLIENT ONLY CLASSES THEN YOU CAN'T DIRECTLY USE THIS AS REFLECTION WILL TRY TO LOAD EVERYTHING.
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
interface TagAutoSerializable: TagSerializable, ReflectableObject {
    @JsonIgnore
    @NonExtendable
    override fun tSerialize() = (this as ReflectableObject).tSerialize(tGetBuffer())

    @JsonIgnore
    @NonExtendable
    override fun tDeserialize(tag: CompoundTag) = (this as ReflectableObject).tDeserialize(tag)

    @JsonIgnore override fun tGetBuffer() = super.tGetBuffer()
}

//TODO you can't define custom ser/deser
fun ReflectableObject.tSerialize(buf: CompoundTag? = null) = (buf ?: CompoundTag()).also { buf ->
    getAllReflectableItems().forEach {
        if (it.metadata.contains("NoTagSerialization")) {return@forEach}
        typeToTagSerDeser[it.it!!::class]?.let { (ser, deser) -> ser(it.it!!, buf, it.cachedName) }
            ?: run {
                if (it.it !is Enum<*>) throw AssertionError("Can't serialize ${it.it!!::class.simpleName}")
                buf.putString(it.cachedName, (it.it as Enum<*>).name)
            }
    }
}

fun ReflectableObject.tDeserialize(tag: CompoundTag) {
    getReflectableItemsWithoutDataclassConstructorItems().forEach {
        if (it.metadata.contains("NoTagSerialization")) {return@forEach}
        try {
        it.setValue(null, null,
            typeToTagSerDeser[it.it!!::class]?.let { (ser, deser) -> deser(tag, it.cachedName) }
                ?: let { _ ->
                    if (it.it !is Enum<*>) return@let it.it!! //throw AssertionError("Can't deserialize ${it.it!!::class.simpleName}")
                    java.lang.Enum.valueOf(it.it!!.javaClass as Class<out Enum<*>>, tag.getString(it.cachedName))
                }
        )
        } catch (e: Exception) {
            ELOG(e.stackTraceToString())
        }
    }
}

typealias TagSerializeFn = (it: Any, tag: CompoundTag, key: String) -> Unit
typealias TagDeserializeFn<T> = (tag: CompoundTag, key: String) -> T?

object TagSerializableItem {
    val typeToTagSerDeser = mutableMapOf<KClass<*>, Pair<TagSerializeFn, TagDeserializeFn<*>>>()

    @JvmStatic fun <T: Any> registerSerializationItem(
        type: KClass<T>,
        serialize: ((it: T, buf: CompoundTag, key: String) -> Unit),
        deserialize: ((buf: CompoundTag, key: String) -> T?)) {
        typeToTagSerDeser[type] = Pair(serialize as TagSerializeFn, deserialize)
    }

    @JvmStatic fun <T: Any> rsi(
        type: KClass<T>,
        serialize: ((it: T, buf: CompoundTag, key: String) -> Unit),
        deserialize: ((buf: CompoundTag, key: String) -> T?)) = registerSerializationItem(type, serialize, deserialize)

    @JvmStatic fun <T: Any> rsin(
        type: KClass<T>,
        serialize: ((it: T, buf: CompoundTag, key: String) -> Unit),
        deserialize: ((buf: CompoundTag, key: String) -> T?)) = registerSerializationItem(type, serialize) { buf, key -> if (buf.contains(key)) deserialize(buf, key) else null }

    init {
        rsin(BlockPos.MutableBlockPos::class, {it, buf, key -> buf.putLong(key, it.asLong())}) {buf, key -> BlockPos.of(buf.getLong(key)).let { BlockPos.MutableBlockPos(it.x, it.y, it.z) }}
        rsin(ResourceLocation::class, {it, buf, key -> buf.putString(key, it.toString())}) {buf, key -> if (!buf.contains(key)) null else ResourceLocation(buf.getString(key)) }
        rsi (Quaterniondc::class, {it, buf, key -> buf.putQuatd(key, it)}) {buf, key -> buf.getQuatd(key)}
        rsi (Quaterniond::class, {it, buf, key -> buf.putQuatd(key, it)}) {buf, key -> buf.getQuatd(key)}
        rsi (Vector3d::class, {it, buf, key -> buf.putMyVector3d(key, it)}) {buf, key -> buf.getMyVector3d(key)}
        rsin(BlockPos::class, {it, buf, key -> buf.putLong(key, it.asLong())}) { buf, key -> BlockPos.of(buf.getLong(key))}
        rsin(ByteBuf::class, {it, buf, key -> buf.putByteArray(key, it.array())}) {buf, key -> Unpooled.wrappedBuffer(buf.getByteArray(key))}
        rsin(Boolean::class, {it, buf, key -> buf.putBoolean(key, it)}) {buf, key -> buf.getBoolean(key)}
        rsin(Double::class, {it, buf, key -> buf.putDouble(key, it)}) {buf, key -> buf.getDouble(key)}
        rsin(String::class, {it, buf, key -> buf.putString(key, it)}) {buf, key -> buf.getString(key)}
        rsin(Color::class, {it, buf, key -> buf.putColor(key, it)}) {buf, key -> buf.getColor(key)}
        rsin(Float::class, {it, buf, key -> buf.putFloat(key, it)}) {buf, key -> buf.getFloat(key)}
        rsin(Long::class, {it, buf, key -> buf.putLong(key, it)}) {buf, key -> buf.getLong(key)}
        rsin(UUID::class, {it, buf, key -> buf.putUUID(key, it)}) {buf, key -> buf.getUUID(key)}
        rsin(Int::class, {it, buf, key -> buf.putInt(key, it)}) {buf, key -> buf.getInt(key)}

        rsin(IntArray::class, {it, buf, key -> buf.putIntArray(key, it)}, {buf, key -> buf.getIntArray(key)})
        rsin(LongArray::class, {it, buf, key -> buf.putLongArray(key, it)}, {buf, key -> buf.getLongArray(key)})
        rsin(FloatArray::class, {it, buf, key -> val tag = ListTag(); tag.addAll(it.map { FloatTag.valueOf(it) }); buf.put(key, tag) }, {buf, key -> (buf.get(key) as ListTag).map { (it as FloatTag).asFloat }.toFloatArray()})
        rsin(DoubleArray::class, {it, buf, key -> val tag = ListTag(); tag.addAll(it.map { DoubleTag.valueOf(it) }); buf.put(key, tag) }, {buf, key -> (buf.get(key) as ListTag).map { (it as DoubleTag).asDouble }.toDoubleArray()})
    }

//    /**
//     * Should be used in the body of the [TagAutoSerializable] class
//     */
//    @JvmStatic fun <T: Any> get(pos: Int, default: T,
//                                customSerialize: ((it: T, buf: CompoundTag, key: String) -> Unit)? = null,
//                                customDeserialize: ((buf: CompoundTag, key: String) -> T)? = null): TagSerializableItemDelegate<T> {
//        customSerialize as ((Any, CompoundTag, String) -> Unit)?
//
//        val res = typeToDelegate[default::class]
//        if (res != null) return res.invoke(pos, default) as TagSerializableItemDelegate<T>
//
//
//        when (default) {
//            is Enum<*> -> {
//                val enumClass = default.javaClass as Class<out Enum<*>>
//                return TagSerializableItemDelegate(pos, default, {it, buf, key -> buf.putString(key, (it as Enum<*>).name) }, {buf, key -> java.lang.Enum.valueOf(enumClass, buf.getString(key)) as T })
//            }
//        }
//
//        return TagSerializableItemDelegate(pos, default, customSerialize!!, customDeserialize!!)
//    }
}