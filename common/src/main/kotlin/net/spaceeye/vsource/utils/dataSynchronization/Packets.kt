package net.spaceeye.vsource.utils.dataSynchronization

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.networking.RenderingTypes
import net.spaceeye.vsource.networking.Serializable

interface Hashable {
    fun hash(): ByteArray
}

interface DataUnit: Hashable, Serializable {
    fun getTypeName() : String
}

class ClientDataRequestPacket(): Serializable {
    constructor(buf: FriendlyByteBuf) : this() { deserialize(buf) }
    constructor(page: Long) : this() {
        this.page = page
    }
    var page: Long = 0

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeLong(page)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        page = buf.readLong()
    }
}

class ServerDataRequestResponsePacket(): Serializable {
    constructor(buf: FriendlyByteBuf): this() { deserialize(buf) }
    constructor(page: Long, pageExists: Boolean): this() {
        this.page = page
        this.pageExists = pageExists
    }
    constructor(page: Long, pageExists: Boolean, checksums: MutableList<Pair<Int, ByteArray>>): this() {
        this.page = page
        this.pageExists = pageExists
        this.checksums = checksums
    }

    var page: Long = 0L
    var pageExists: Boolean = false
    lateinit var checksums: MutableList<Pair<Int, ByteArray>>

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeLong(page)
        buf.writeBoolean(pageExists)

        if (!pageExists) {return buf}

        buf.writeCollection(checksums) {
                buf, item ->
            buf.writeInt(item.first)
            buf.writeByteArray(item.second)
        }

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        page = buf.readLong()
        pageExists = buf.readBoolean()

        if (!pageExists) {return}

        checksums = buf.readCollection({ mutableListOf() }) {
            Pair(it.readInt(), it.readByteArray())
        }
    }
}

class ClientDataUpdateRequestPacket(): Serializable {
    constructor(buf: FriendlyByteBuf) : this() {deserialize(buf)}
    constructor(page: Long, indicesToUpdate: MutableList<Int>): this() {
        this.page = page
        this.indicesToUpdate = indicesToUpdate
    }

    var page: Long = 0
    lateinit var indicesToUpdate: MutableList<Int>

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeLong(page)
        buf.writeCollection(indicesToUpdate) { buf, num -> buf.writeInt(num) }

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        page = buf.readLong()
        indicesToUpdate = buf.readCollection({ mutableListOf()}) {
            buf.readInt()
        }
    }
}

class ServerDataUpdateRequestResponsePacket<T : DataUnit>(): Serializable {
    constructor(buf: FriendlyByteBuf) : this() {deserialize(buf)}
    constructor(exists: Boolean, page: Long) : this() {
        this.pageExists = exists
        this.page = page
    }
    constructor(exists: Boolean, page: Long, data: MutableList<Pair<Int, T>>, nullData: MutableList<Int>): this() {
        this.pageExists = exists
        this.page = page
        this.newData = data
        this.nullData = nullData
    }

    var page = 0L
    var pageExists = false
    lateinit var newData: MutableList<Pair<Int, T>>
    lateinit var nullData: MutableList<Int>

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeBoolean(pageExists)
        buf.writeLong(page)
        if (!pageExists) {return buf}
        buf.writeCollection(newData) {
                buf, item ->
            buf.writeInt(RenderingTypes.typeToIdx(item.second.getTypeName())!!)
            buf.writeInt(item.first)
            buf.writeByteArray(item.second.serialize().array())
        }
        buf.writeCollection(nullData) {buf, item -> buf.writeInt(item)}
        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        pageExists = buf.readBoolean()
        page = buf.readLong()
        if (!pageExists) {return}
        newData = buf.readCollection({ mutableListOf()}) {
            val typeIdx = it.readInt()

            val int = it.readInt()
            val bytes = it.readByteArray()
            val buffer = FriendlyByteBuf(Unpooled.wrappedBuffer(bytes))

            val item = RenderingTypes.idxToSupplier(typeIdx).get()
            item.deserialize(buffer)

            Pair(int, item)
        }
        nullData = buf.readCollection({ mutableListOf()}) {buf.readInt()}
    }
}

class ServerChecksumsUpdatedPacket(): Serializable {
    constructor(buf: FriendlyByteBuf) : this() {deserialize(buf)}
    constructor(page: Long, updatedIndices: MutableList<Pair<Int, ByteArray>>): this() {
        this.page = page
        this.updatedIndices = updatedIndices
    }
    constructor(page: Long, wasRemoved: Boolean): this() {
        this.page = page
        this.wasRemoved = wasRemoved
    }

    var page: Long = 0
    var wasRemoved = false
    lateinit var updatedIndices: MutableList<Pair<Int, ByteArray>>

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeLong(page)
        buf.writeBoolean(wasRemoved)
        if (wasRemoved) {return buf}
        buf.writeCollection(updatedIndices) { buf, (idx, arr) -> buf.writeInt(idx); buf.writeByteArray(arr) }

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        page = buf.readLong()
        wasRemoved = buf.readBoolean()
        if (wasRemoved) {return}
        updatedIndices = buf.readCollection({ mutableListOf()}) {
            Pair(buf.readInt(), buf.readByteArray())
        }
    }
}