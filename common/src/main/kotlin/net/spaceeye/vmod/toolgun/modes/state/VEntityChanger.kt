package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.translate.makeFake
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.VEntityTypes
import net.spaceeye.vmod.vEntityManaging.VEntityTypes.getType
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.getAllVEntityIdsOfShipId
import net.spaceeye.vmod.vEntityManaging.getVEntity
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import kotlin.math.max
import kotlin.math.min

class VEntityChanger: ExtendableToolgunMode() {
    override val itemName: TranslatableComponent
        get() = makeFake("VEntityChanger")

    private fun resetSelection(player: ServerPlayer) {
        s2cSendVEntities.sendToClient(player, S2CSendVEntities(mutableListOf()))
    }

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return resetSelection(player)}
        val ship = raycastResult.ship ?: return resetSelection(player)

        val ventities = level.getAllVEntityIdsOfShipId(ship.id)

        val data = ventities.mapNotNull { (level.getVEntity(it) as? ReflectableObject as? VEntity)?.let {
            Pair(it, if (it is ExtendableVEntity) {it.getExtensionsOfType<RenderableExtension>().first().rID} else {-1})
        } }.toMutableList()
        s2cSendVEntities.sendToClient(player, S2CSendVEntities(data))
    }

    override fun eOnMouseScrollEvent(amount: Double): EventResult {
        if (clientVEntities.isEmpty()) { return super.eOnMouseScrollEvent(amount) }

        if (amount < 0) {
            cursorPos += 1
        } else {
            cursorPos -= 1
        }

        cursorPos = max(min(cursorPos, clientVEntities.size-1), 0)

        return EventResult.interruptFalse()
    }

    fun entryConfirmation() {
        ELOG("NOT IMPLEMENTED")
    }

    companion object {
        var cursorPos = 0
        //int is renderer ID
        var clientVEntities = mutableListOf<Pair<VEntity, Int>>()

        class S2CSendVEntities(): Serializable {
            var data = mutableListOf<Pair<VEntity, Int>>()
            constructor(data: MutableList<Pair<VEntity, Int>>): this() { this.data = data }

            override fun serialize(): FriendlyByteBuf {
                val buf = getBuffer()

                buf.writeCollection(data) { buf, (it, rID) ->
                    buf.writeUtf(it.getType())
                    buf.writeInt(rID)
                    buf.writeByteArray(CompoundTagSerializable(it.nbtSerialize()!!).serialize().accessByteBufWithCorrectSize())
                }
                return buf
            }

            override fun deserialize(buf: FriendlyByteBuf) {
                cursorPos = 0
                data = buf.readCollection({mutableListOf()}) {
                    val type = it.readUtf()
                    val rID = it.readInt()
                    val data = CompoundTagSerializable().also{ tag -> tag.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(it.readByteArray()))) }
                    Pair(VEntityTypes
                        .strTypeToSupplier(type)
                        .get()
                        .nbtDeserialize(data.tag!!, mapOf()), rID)
                }
            }
        }
        val s2cSendVEntities = regS2C<S2CSendVEntities>("send_ventities", "ventity_changer") {
            clientVEntities = it.data
        }

        init {
            ToolgunModes.registerWrapper(VEntityChanger::class) {
                it.addExtension<VEntityChanger> {
                    BasicConnectionExtension<VEntityChanger>("ventity_changer"
                        ,leftFunction = { inst, level, player, rr -> inst.activatePrimaryFunction(level, player, rr) }

                        ,blockRight = { clientVEntities.isEmpty() }
                        ,rightClientCallback = { inst -> inst.entryConfirmation() }
                    )
                }
            }
        }
    }
}