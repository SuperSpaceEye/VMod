package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.networking.makeS2C
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.toolgun.modes.extensions.BlockMenuOpeningExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementAssistExtension
import net.spaceeye.vmod.toolgun.modes.extensions.PlacementModesExtension
import net.spaceeye.vmod.utils.Registry

object ToolgunExtensions: Registry<ToolgunModeExtension>() {
    init {
        register(BasicConnectionExtension::class)
        register(PlacementModesExtension::class)
        register(PlacementAssistExtension::class)
        register(BlockMenuOpeningExtension::class)

        makeEvents()
    }

    private fun makeEvents() {
        PlayerEvent.PLAYER_JOIN.register {
            s2cSetSchema.sendToClient(it, S2CSetToolgunExtensionsSchema(getSchema()))
        }
    }

    class S2CSetToolgunExtensionsSchema(): Serializable {
        constructor(schema: Map<String, Int>) : this() {
            this.schema = schema
        }
        var schema = mapOf<String, Int>()

        override fun serialize(): FriendlyByteBuf {
            val buf = getBuffer()

            buf.writeCollection(schema.toList()) {buf, (key, idx) -> buf.writeUtf(key); buf.writeInt(idx) }

            return buf
        }

        override fun deserialize(buf: FriendlyByteBuf) {
            schema = buf.readCollection({mutableListOf<Pair<String, Int>>()}) {Pair(buf.readUtf(), buf.readInt())}.toMap()
        }
    }
    val s2cSetSchema = makeS2C<S2CSetToolgunExtensionsSchema>("set_schema", "toolgun_extensions") {
        setSchema(it.schema.map { Pair(it.value, it.key) }.toMap())
    }
}