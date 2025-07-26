package net.spaceeye.vmod.toolgun.serverSettings.modes

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.guiElements.*
import net.spaceeye.vmod.networking.*
import net.spaceeye.vmod.toolgun.VMToolgun
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.translate.APPLY_NEW_GRAVITY_SETTINGS
import net.spaceeye.vmod.translate.DIMENSIONAL_GRAVITY
import net.spaceeye.vmod.translate.DIMENSIONAL_GRAVITY_UPDATE_WAS_REJECTED
import net.spaceeye.vmod.translate.LEVELS
import net.spaceeye.vmod.translate.get
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.vsStuff.VSGravityManager
import java.awt.Color

class DimensionalGravitySettings: ServerSettingsGUIBuilder {
    override val itemName = DIMENSIONAL_GRAVITY

    override fun makeGUISettings(parentWindow: UIContainer) {
        callback = {
            Button(Color(180, 180, 180), APPLY_NEW_GRAVITY_SETTINGS.get()) {
                c2sTryUpdateGravityVectors.sendToServer(C2STryUpdateGravityVectors())
            } constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels

                width = 98.percent
            } childOf parentWindow

            val currentDisplayed = mutableListOf<TextEntry>()
            DropDown(LEVELS.get(), vectorData.map { DItem(it.first.removePrefix("minecraft:dimension:"), false) {
                currentDisplayed.forEach { parentWindow.removeChild(it) }
                currentDisplayed.clear()

                currentDisplayed.add(makeTextEntry("x", FakeKProperty({it.second.x}) {value -> it.second.x = value}, 2f, 2f, parentWindow))
                currentDisplayed.add(makeTextEntry("y", FakeKProperty({it.second.y}) {value -> it.second.y = value}, 2f, 2f, parentWindow))
                currentDisplayed.add(makeTextEntry("z", FakeKProperty({it.second.z}) {value -> it.second.z = value}, 2f, 2f, parentWindow))
            } }, onClose =  {
                currentDisplayed.forEach { parentWindow.removeChild(it) }
            }, onOpen = {
                currentDisplayed.forEach { it childOf parentWindow }
            }) constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels
            } childOf parentWindow
        }
        c2sRequestGravityVectors.sendToServer(EmptyPacket())
    }

    companion object {
        var vectorData: MutableList<Pair<String, Vector3d>> = mutableListOf()
        var callback: (() -> Unit)? = null

        class S2CSendGravityVectors(): Serializable {
            override fun serialize(): FriendlyByteBuf {
                val buf = getBuffer()
                buf.writeCollection(VSGravityManager.gravities.toList()) { buf, it -> buf.writeUtf(it.first); buf.writeVector3d(it.second)}
                return buf
            }

            override fun deserialize(buf: FriendlyByteBuf) {
                vectorData = buf.readCollection({ mutableListOf() }) { buf-> Pair(buf.readUtf(), buf.readVector3d())}
            }
        }

        class C2STryUpdateGravityVectors(): Serializable {
            override fun serialize(): FriendlyByteBuf {
                val buf = getBuffer()
                buf.writeCollection(vectorData) {buf, it -> buf.writeUtf(it.first); buf.writeVector3d(it.second)}
                return buf
            }

            override fun deserialize(buf: FriendlyByteBuf) {
                vectorData = buf.readCollection({ mutableListOf() }) { buf-> Pair(buf.readUtf(), buf.readVector3d())}
            }
        }

        val c2sRequestGravityVectors = regC2S<EmptyPacket>(MOD_ID, "request_gravity_vectors", "gravity_settings") { pkt, player ->
            s2cSendGravityVectors.sendToClient(player, S2CSendGravityVectors())
        }

        val s2cSendGravityVectors = regS2C<S2CSendGravityVectors>(MOD_ID, "send_gravity_vectors", "gravity_settings") {pkt ->
            callback?.invoke()
            callback = null
        }

        val c2sTryUpdateGravityVectors = regC2S<C2STryUpdateGravityVectors>(MOD_ID, "try_update_gravity_vectors", "gravity_settings",
            { pkt, player -> player.hasPermissions(4)}, { pkt, player -> s2cDimensionalGravityUpdateWasRejected.sendToClient(player, EmptyPacket())}) { pkt, player ->
            vectorData.forEach { VSGravityManager.setGravity(it.first, it.second) }
        }

        val s2cDimensionalGravityUpdateWasRejected = regS2C<EmptyPacket>(MOD_ID, "dimensional_gravity_update_was_rejected", "gravity_settings") {
            VMToolgun.client.closeWithError(DIMENSIONAL_GRAVITY_UPDATE_WAS_REJECTED.get())
        }
    }
}