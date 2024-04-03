package net.spaceeye.vmod.networking

import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.core.api.ships.properties.ShipId

class S2CSendTraversalInfo(): Serializable {
    var data: LongArray = longArrayOf()

    constructor(buf: FriendlyByteBuf): this() { deserialize(buf) }
    constructor(data: MutableSet<ShipId>): this() { this.data = data.toLongArray() }
    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeLongArray(data)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        data = buf.readLongArray()
    }
}