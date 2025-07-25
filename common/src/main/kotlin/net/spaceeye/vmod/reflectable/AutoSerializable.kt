package net.spaceeye.vmod.reflectable

import com.fasterxml.jackson.annotation.JsonIgnore
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem.typeToSerDeser
import net.spaceeye.vmod.utils.*
import org.jetbrains.annotations.ApiStatus.NonExtendable
import org.joml.Quaterniond
import org.valkyrienskies.core.util.readQuatd
import org.valkyrienskies.core.util.writeQuatd
import java.awt.Color
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/**
 * WARNING!!!! IF CLASS USES CLIENT ONLY CLASSES THEN YOU CAN'T DIRECTLY USE THIS AS REFLECTION WILL TRY TO LOAD EVERYTHING.
 * An interface that will add functionality for semi-automatic serialization/deserialization
 *
 * ### If added to a normal class:
 *
 * To mark parameter for automatic ser/deser will need to use delegate provided by [get][ByteSerializableItem.get] like
 *
 * ```kotlin
 * class Test(): AutoSerializable {
 *  @JsonIgnore private var i = 0
 *
 *  val item1: Int by get(i++, 10)
 *  val item2: Double by get(i++, 20.0, {min(it, 10.0)})
 *  val item3: String by get(i++, "Hello", {it + " World"})
 *  val complexItem: SomeComplexType by get(i++,
 *      SomeComplexType.default(),
 *      {it.verify()},
 *      {it, buf -> it.serialize(buf)},
 *      {buf -> SomeComplexType.default().deserialize(buf)})
 * }
 * ```
 *
 * ### If added to a data class:
 *
 * With data class you can just use registered types directly, without using [get][ByteSerializableItem.get], though you can still use it for complex types
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
interface AutoSerializable: Serializable, ReflectableObject {
    @JsonIgnore
    @NonExtendable
    override fun serialize() = (this as ReflectableObject).serialize(getBuffer())

    @JsonIgnore
    @NonExtendable
    override fun deserialize(buf: FriendlyByteBuf) = (this as ReflectableObject).deserialize(buf)

    @JsonIgnore
    override fun getBuffer() = super.getBuffer()
}

fun ReflectableObject.serialize(buf: FriendlyByteBuf? = null) = (buf ?: FriendlyByteBuf(Unpooled.buffer(64))).also { buf ->
    getAllReflectableItems().forEach {
        (it.metadata["byteSerialize"] as? ByteSerializeFn)?.invoke(it.it!!, buf)
            ?: typeToSerDeser[it.it!!::class]?.let { (ser, deser) -> ser(it.it!!, buf) }
            ?: run {
                if (it.it !is Enum<*>) throw AssertionError("Can't serialize ${it.it!!::class.simpleName}")
                buf.writeEnum(it.it!! as Enum<*>)
            }
    }
}

fun ReflectableObject.deserialize(buf: FriendlyByteBuf) {
    getReflectableItemsWithoutDataclassConstructorItems().forEach {
        it.setValue(null, null,
            (it.metadata["byteDeserialize"] as? ByteDeserializeFn<Any>)?.invoke(buf)
                ?: typeToSerDeser[it.it!!::class]?.let { (ser, deser) -> deser(buf) }
                ?: let {_ ->
                    if (it.it !is Enum<*>) throw AssertionError("Can't deserialize ${it.it!!::class.simpleName}")
                    buf.readEnum(it.it!!.javaClass as Class<out Enum<*>>)
                }
        )
    }
}

/**
 * Used in networking for if the class is [AutoSerializable] dataclass.
 * Dataclasses need all items already deserialized and ready to be used in constructor
 */
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

    val deserializers = members.map {
        val clazz = it.returnType.jvmErasure
        if (!clazz.java.isEnum) {
            typeToSerDeser[clazz]!!.second
        } else {
            {buf -> buf.readEnum(clazz.java as Class<out Enum<*>>)}
        }
    }

    val items = deserializers.map { it.invoke(buf) }

    return this.primaryConstructor!!.call(*items.toTypedArray())
}

typealias ByteSerializeFn = ((it: Any, buf: FriendlyByteBuf) -> Unit)
typealias ByteDeserializeFn<T> = ((buf: FriendlyByteBuf) -> T)

object ByteSerializableItem {
    val typeToSerDeser = mutableMapOf<KClass<*>, Pair<ByteSerializeFn, ByteDeserializeFn<*>>>()

    @JvmStatic fun <T: Any> registerSerializationItem(
        type: KClass<T>,
        serialize: ((it: T, buf: FriendlyByteBuf) -> Unit),
        deserialize: ((buf: FriendlyByteBuf) -> T)) {
        typeToSerDeser[type] = Pair(serialize as ByteSerializeFn, deserialize)
    }

    @JvmStatic fun <T: Any> rsi(
        type: KClass<T>,
        serialize: ((it: T, buf: FriendlyByteBuf) -> Unit),
        deserialize: ((buf: FriendlyByteBuf) -> T)) = registerSerializationItem(type, serialize, deserialize)

    init {
        rsi(BlockPos.MutableBlockPos::class, {it, buf -> buf.writeBlockPos(it)}) {buf -> buf.readBlockPos().mutable()}
        rsi(ResourceLocation::class, {it, buf -> buf.writeUtf(it.toString())}) {buf -> ResourceLocation(buf.readUtf())}
        rsi(FriendlyByteBuf::class, {it, buf -> buf.writeByteArray(it.accessByteBufWithCorrectSize())}) {buf -> FriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray()))}
        rsi(CompoundTag::class, {tag, buf -> buf.writeByteArray(ByteBufOutputStream(Unpooled.buffer()).also { NbtIo.writeCompressed(tag, it) }.buffer().accessByteBufWithCorrectSize()) }) { buf -> NbtIo.readCompressed(ByteBufInputStream(Unpooled.wrappedBuffer(buf.readByteArray()))) }
        rsi(Quaterniond::class, {it, buf -> buf.writeQuatd(it)}) {buf -> buf.readQuatd()}
        rsi(Vector3d::class, {it, buf -> buf.writeVector3d(it)}) {buf -> buf.readVector3d()}
        rsi(BlockPos::class, { it, buf -> buf.writeBlockPos(it)}) {buf -> buf.readBlockPos()}
        rsi(ByteBuf::class, {it, buf -> buf.writeByteArray(it.array())}) {buf -> Unpooled.wrappedBuffer(buf.readByteArray())}
        rsi(Boolean::class, {it, buf -> buf.writeBoolean(it)}) {buf -> buf.readBoolean()}
        rsi(Double::class, {it, buf -> buf.writeDouble(it)}) {buf -> buf.readDouble()}
        rsi(String::class, {it, buf -> buf.writeUtf(it)}) {buf -> buf.readUtf()}
        rsi(Color::class, {it, buf -> buf.writeColor(it)}) {buf -> buf.readColor()}
        rsi(Float::class, {it, buf -> buf.writeFloat(it)}) {buf -> buf.readFloat()}
        rsi(Long::class, {it, buf -> buf.writeLong(it)}) {buf -> buf.readLong()}
        rsi(UUID::class, {it, buf -> buf.writeUUID(it)}) {buf -> buf.readUUID()}
        rsi(Int::class, {it, buf -> buf.writeInt(it)}) {buf -> buf.readInt()}

        rsi(IntArray::class, {it, buf -> buf.writeCollection(it.asList()){buf, it -> buf.writeInt(it)}}) {buf -> buf.readCollection({mutableListOf<Int>()}) {buf.readInt()}.toIntArray()}
        rsi(LongArray::class, {it, buf -> buf.writeCollection(it.asList()){buf, it -> buf.writeLong(it)}}) {buf -> buf.readCollection({mutableListOf<Long>()}) {buf.readLong()}.toLongArray()}
        rsi(FloatArray::class, {it, buf -> buf.writeCollection(it.asList()){buf, it -> buf.writeFloat(it)}}) {buf -> buf.readCollection({mutableListOf<Float>()}) {buf.readFloat()}.toFloatArray()}
        rsi(DoubleArray::class, {it, buf -> buf.writeCollection(it.asList()){buf, it -> buf.writeDouble(it)}}) {buf -> buf.readCollection({mutableListOf<Double>()}) {buf.readDouble()}.toDoubleArray()}
    }

    private fun makeByteSerDeser(verification: ((it: Nothing) -> Any), ser: ByteSerializeFn, deser: ByteDeserializeFn<Any>): MutableMap<String, Any> = mutableMapOf(
        Pair("verification", verification),
        Pair("byteSerialize", ser),
        Pair("byteDeserialize", deser)
    )

    @JvmStatic fun <T: Any> get(pos: Int, default: T, verification: (T) -> T = {it}) = get(pos, default, false, verification, null, null)
    @JvmStatic fun <T: Any> get(pos: Int, default: T, verifyOnGet: Boolean = false, verification: (T) -> T = {it}) = get(pos, default, verifyOnGet, verification, null, null)


    @JvmStatic fun <T: Any> get(pos: Int, default: T,
                                verification: ((it: T) -> T),
                                customSerialize: ((it: T, buf: FriendlyByteBuf) -> Unit)? = null,
                                customDeserialize: ((buf: FriendlyByteBuf) -> T)? = null) = get(pos, default, false, verification, customSerialize, customDeserialize)

    /**
     * Should be used in the body of the AutoSerializable class
     */
    @JvmStatic fun <T: Any> get(pos: Int, default: T,
                                verifyOnGet: Boolean = false,
                                verification: ((it: T) -> T),
                                customSerialize: ((it: T, buf: FriendlyByteBuf) -> Unit)? = null,
                                customDeserialize: ((buf: FriendlyByteBuf) -> T)? = null): ReflectableItemDelegate<T> {
        typeToSerDeser[default::class]?.let { return ReflectableItemDelegate(pos, default, mutableMapOf(Pair("verification", verification)), getWrapper = if (verifyOnGet) verification else null) }

        when (default) {
            //todo move it from here
            is Enum<*> -> {
                val enumClass = default.javaClass as Class<out Enum<*>>
                return ReflectableItemDelegate(pos, default, makeByteSerDeser(verification, { it, buf -> buf.writeEnum(it as Enum<*>)}) { buf -> buf.readEnum(enumClass) as T }, getWrapper = if (verifyOnGet) verification else null)
            }
        }

        if (customSerialize == null || customDeserialize == null) {
            throw AssertionError("type ${default.javaClass} was used without registered and with no custom ser/deser")
        }

        return ReflectableItemDelegate(pos, default, makeByteSerDeser(verification, customSerialize as (Any, FriendlyByteBuf) -> Unit, customDeserialize), getWrapper = if (verifyOnGet) verification else null)
    }
}