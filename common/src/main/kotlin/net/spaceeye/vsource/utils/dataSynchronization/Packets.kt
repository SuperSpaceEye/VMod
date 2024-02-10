package net.spaceeye.vsource.utils.dataSynchronization

import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.networking.Serializable
import java.util.function.Supplier

interface Hashable {
    fun hash(): ByteArray
}

interface DataUnit: Hashable, Serializable

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

class ServerDataUpdateRequestResponsePacket<T : DataUnit>(val supplier: Supplier<T>?): Serializable {
    constructor(buf: FriendlyByteBuf, supplier: Supplier<T>?) : this(supplier) {deserialize(buf)}
    constructor(exists: Boolean, page: Long, supplier: Supplier<T>?) : this(supplier) {
        this.pageExists = exists
        this.page = page
    }
    constructor(exists: Boolean, page: Long, data: MutableList<Pair<Int, T>>, nullData: MutableList<Int>, supplier: Supplier<T>?): this(supplier) {
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
            buf.writeInt(item.first)
            buf.writeBytes(item.second.serialize())
        }
        buf.writeCollection(nullData) {buf, item -> buf.writeInt(item)}
        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        pageExists = buf.readBoolean()
        page = buf.readLong()
        if (!pageExists) {return}
        newData = buf.readCollection({ mutableListOf()}) {
            Pair(it.readInt(), supplier!!.get().deserialize(FriendlyByteBuf(it.readBytes(getBuffer()))))
        }
        nullData = buf.readCollection({ mutableListOf()}) {buf.readInt()}
    }
}