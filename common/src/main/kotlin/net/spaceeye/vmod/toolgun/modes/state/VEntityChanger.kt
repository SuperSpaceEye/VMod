package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.event.EventResult
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.valkyrien_ship_schematics.containers.CompoundTagSerializable
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.networking.regS2C
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModes
import net.spaceeye.vmod.toolgun.modes.extensions.BasicConnectionExtension
import net.spaceeye.vmod.utils.RaycastFunctions
import net.spaceeye.vmod.vEntityManaging.VEntity
import net.spaceeye.vmod.vEntityManaging.VEntityTypes
import net.spaceeye.vmod.vEntityManaging.VEntityTypes.getType
import net.spaceeye.vmod.vEntityManaging.extensions.RenderableExtension
import net.spaceeye.vmod.vEntityManaging.getAllVEntityIdsOfShipId
import net.spaceeye.vmod.vEntityManaging.getVEntity
import net.spaceeye.vmod.vEntityManaging.util.ExtendableVEntity
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import gg.essential.elementa.dsl.*
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.spaceeye.vmod.guiElements.Button
import net.spaceeye.vmod.guiElements.makeCheckBox
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntries
import net.spaceeye.vmod.networking.regC2S
import net.spaceeye.vmod.reflectable.ReflectableItemDelegate
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.VEntityChangerGUI
import net.spaceeye.vmod.toolgun.modes.hud.VEntityChangerHUD
import net.spaceeye.vmod.translate.YOU_DONT_HAVE_ACCESS_TO_THIS
import net.spaceeye.vmod.utils.FakeKProperty
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vEntityManaging.makeVEntityWithId
import net.spaceeye.vmod.vEntityManaging.removeVEntity
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID

class VEntityChangerGui(val constraint: VEntity): WindowScreen(ElementaVersion.V8) {
    internal val mainWindow = UIBlock(Color(240, 240, 240)).constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = 90f.percent()
        height = 90f.percent()
    } childOf window

    internal val scrollWindow = ScrollComponent().constrain {
        x = 0.pixels
        y = 0.pixels

        width = 100.percent
        height = 100.percent
    } childOf mainWindow

    fun <T> kp(it: ReflectableItemDelegate<*>) = FakeKProperty<T>({it.getValue(it, null) as T}) { value -> it.setValue(it, null, value as Any)}
    fun <T> fp(get: () -> T, set: (T) -> Unit) = FakeKProperty<T>(get, set)

    init {
        Button(Color(140, 140, 140), "Apply Changes") {
            VEntityChanger.c2sSendUpdate.sendToServer(VEntityChanger.Companion.C2SSendUpdate(constraint))
        }.constrain {
            x = 2.pixels
            y = 2.pixels

            width = 100.percent - 4.pixels
        } childOf scrollWindow
        constraint as ReflectableObject
        constraint.getAllReflectableItems().let {lis ->
            //TODO i don't like this
            (constraint as? ExtendableVEntity)?.getExtensionsOfType<RenderableExtension>()?.let { extensions ->
                mutableListOf<ReflectableItemDelegate<*>>().also {
                    it.addAll(lis)
                    it.add(ReflectableItemDelegate<Any>(-1, null).also { it.cachedName = "Renderer Settings:"})
                    it.addAll(extensions.mapNotNull { (it.renderer as? ReflectableObject)?.getAllReflectableItems() }.flatten())
                }
            } ?: lis
        }.forEach {
            val uiTextMaker = {
                UIText(it.cachedName, false).constrain {
                    x = 2.pixels
                    y = SiblingConstraint(2f) + 2f.pixels
                    color = Color.BLACK.toConstraint()
                } childOf scrollWindow
            }

            //TODO names are a bit uneven, fix later.
            when (it.it) {
                is BlockPos.MutableBlockPos -> { (it.it as BlockPos.MutableBlockPos).let{ v ->                   makeTextEntries(it.cachedName, listOf("x", "y", "z"),      listOf(fp({v.x}, {v.x = it}), fp({v.y}, {v.y = it}), fp({v.z}, {v.z = it})                       ), 2f, 2f, scrollWindow)}}
                is Quaterniond -> it.it.let { v -> v as Quaterniond;                                             makeTextEntries(it.cachedName, listOf("x", "y", "z", "w"), listOf(fp({v.x}, {v.x = it}), fp({v.y}, {v.y = it}), fp({v.z}, {v.z = it}), fp({v.w}, {v.w = it})), 2f, 2f, scrollWindow)}
                is Quaterniondc -> { it.setValue(it, null, (it.it as Quaterniondc).get(Quaterniond()).also{ v -> makeTextEntries(it.cachedName, listOf("x", "y", "z", "w"), listOf(fp({v.x}, {v.x = it}), fp({v.y}, {v.y = it}), fp({v.z}, {v.z = it}), fp({v.w}, {v.w = it})), 2f, 2f, scrollWindow)})}
                is Vector3d -> it.it.let { v -> v as Vector3d;                                                   makeTextEntries(it.cachedName, listOf("x", "y", "z"),      listOf(fp({v.x}, {v.x = it}), fp({v.y}, {v.y = it}), fp({v.z}, {v.z = it})                       ), 2f, 2f, scrollWindow)}
                is BlockPos -> { it.setValue(it, null, (it.it as BlockPos).mutable().also{ v ->                  makeTextEntries(it.cachedName, listOf("x", "y", "z"),      listOf(fp({v.x}, {v.x = it}), fp({v.y}, {v.y = it}), fp({v.z}, {v.z = it})                       ), 2f, 2f, scrollWindow)})}
                is ByteBuf -> uiTextMaker()
                is Boolean -> makeCheckBox(it.cachedName, kp<Boolean>(it), 2f, 2f, scrollWindow)
                is Double -> makeTextEntry(it.cachedName, kp<Double>(it), 2f, 2f, scrollWindow)
                is String -> makeTextEntry(it.cachedName, kp<String>(it), 2f, 2f, scrollWindow)
                is Float -> makeTextEntry(it.cachedName, kp<Float>(it), 2f, 2f, scrollWindow)
                is Long -> makeTextEntry(it.cachedName, kp<Long>(it), 2f, 2f, scrollWindow)
                is UUID -> uiTextMaker()
                is Int -> makeTextEntry(it.cachedName, kp<Int>(it), 2f, 2f, scrollWindow)
                is Color -> {//color is immutable so i need to do this
                    makeTextEntries(it.cachedName, listOf("r", "g", "b", "a"), listOf(
                        fp({(it.it as Color).red  }) {v -> val c = it.it as Color; it.setValue(it, null, Color(v,     c.green, c.blue, c.alpha))},
                        fp({(it.it as Color).green}) {v -> val c = it.it as Color; it.setValue(it, null, Color(c.red, v,       c.blue, c.alpha))},
                        fp({(it.it as Color).blue }) {v -> val c = it.it as Color; it.setValue(it, null, Color(c.red, c.green, v,      c.alpha))},
                        fp({(it.it as Color).alpha}) {v -> val c = it.it as Color; it.setValue(it, null, Color(c.red, c.green, c.blue, v      ))},
                    ), 2f, 2f, scrollWindow)
                }
                is Enum<*> -> makeDropDown(it.cachedName, kp<Enum<*>>(it), 2f, 2f, scrollWindow)
                else -> uiTextMaker()
            }
        }
    }
}

class VEntityChanger: ExtendableToolgunMode(), VEntityChangerHUD, VEntityChangerGUI {
    private fun resetSelection(player: ServerPlayer) {
        s2cSendVEntities.sendToClient(player, S2CSendVEntities(mutableListOf()))
    }

    fun activatePrimaryFunction(level: ServerLevel, player: ServerPlayer, raycastResult: RaycastFunctions.RaycastResult)  {
        if (raycastResult.state.isAir) {return resetSelection(player)}
        val ship = raycastResult.ship ?: return resetSelection(player)

        val ventities = level.getAllVEntityIdsOfShipId(ship.id)

        val data = ventities.mapNotNull { (level.getVEntity(it) as? ReflectableObject as? VEntity)?.let {
            Pair(it, if (it is ExtendableVEntity) {it.getExtensionsOfType<RenderableExtension>().let {if (it.isNotEmpty()) {it.first().rID} else {-1}}} else {-1})
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
        Minecraft.getInstance().setScreen(VEntityChangerGui(clientVEntities[cursorPos].first))
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
            ClientToolGunState.refreshHUD()
        }

        class C2SSendUpdate(): Serializable {
            lateinit var ventity: VEntity
            constructor(ventity: VEntity): this() {this.ventity = ventity}

            override fun serialize(): FriendlyByteBuf {
                val buf = getBuffer()
                buf.writeUtf(ventity.getType())
                buf.writeByteArray(CompoundTagSerializable(ventity.nbtSerialize()!!).serialize().accessByteBufWithCorrectSize())
                return buf
            }

            override fun deserialize(buf: FriendlyByteBuf) {
                val type = buf.readUtf()
                val data = CompoundTagSerializable().also{ tag -> tag.deserialize(FriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray()))) }

                ventity = VEntityTypes
                    .strTypeToSupplier(type)
                    .get()
                    .nbtDeserialize(data.tag!!, mapOf())!!
            }
        }
        val c2sSendUpdate = regC2S<C2SSendUpdate>("send_update", "ventity_changer",
            { ServerToolGunState.playerHasPermission(it, VEntityChanger::class.java as Class<BaseMode>) },
            { ServerToolGunState.s2cErrorHappened.sendToClient(it, ServerToolGunState.S2CErrorHappened(
                YOU_DONT_HAVE_ACCESS_TO_THIS.key, true, true)) }
        ) { pkt, player ->
            val level = player.level as ServerLevel
            val ventity = pkt.ventity

            level.shipObjectWorld.loadedShips

            level.removeVEntity(ventity.mID)
            level.makeVEntityWithId(ventity, ventity.mID) {  }
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