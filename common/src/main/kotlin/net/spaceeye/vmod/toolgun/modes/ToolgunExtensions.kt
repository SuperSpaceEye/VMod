package net.spaceeye.vmod.toolgun.modes

import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.toolgun.modes.extensions.*
import net.spaceeye.vmod.utils.Registry

//TODO only register extensions that need syncing
object ToolgunExtensions: Registry<ToolgunModeExtension>() {
    init {
        register(BasicConnectionExtension::class)
        register(PlacementModesExtension::class)
        register(PlacementAssistExtension::class)
        register(BlockMenuOpeningExtension::class)
        register(ConstantClientRaycastingExtension::class)

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
    val s2cSetSchema = regS2C<S2CSetToolgunExtensionsSchema>(MOD_ID, "set_schema", "toolgun_extensions") {
        setSchema(it.schema.map { Pair(it.value, it.key) }.toMap())
    }
}