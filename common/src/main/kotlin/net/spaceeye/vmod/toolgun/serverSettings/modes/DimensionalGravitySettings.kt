package net.spaceeye.vmod.toolgun.serverSettings.modes

import dev.architectury.networking.NetworkManager
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.guiElements.*
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.networking.NetworkingRegistrationFunctions.idWithConnc
import net.spaceeye.vmod.networking.NetworkingRegistrationFunctions.idWithConns
import net.spaceeye.vmod.networking.S2CConnection
import net.spaceeye.vmod.networking.Serializable
import net.spaceeye.vmod.toolgun.ClientToolGunState
import net.spaceeye.vmod.toolgun.ServerToolGunState
import net.spaceeye.vmod.toolgun.serverSettings.ServerSettingsGUIBuilder
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.vsStuff.VSGravityManager
import java.awt.Color

class DimensionalGravitySettings: ServerSettingsGUIBuilder {
    //TODO
    override val itemName: TranslatableComponent get() = TranslatableComponent("Dimensional Gravity")

    override fun makeGUISettings(parentWindow: UIContainer) {
        callback = {
            Button(Color(180, 180, 180), "Apply new Gravity Settings") {
                c2sTryUpdateGravityVectors.sendToServer(C2STryUpdateGravityVectors())
            } constrain {
                x = 2f.pixels
                y = SiblingConstraint() + 2f.pixels

                width = 98.percent
            } childOf parentWindow

            val currentDisplayed = mutableListOf<TextEntry>()
            DropDown("Levels", vectorData.map { DItem(it.first.removePrefix("minecraft:dimension:"), false) {
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

        //TODO is this fine?
        class S2CSendGravityVectors(): Serializable {
            override fun serialize(): FriendlyByteBuf {
                val buf = getBuffer()
                buf.writeCollection(VSGravityManager.__gravities.toList()) {buf, it -> buf.writeUtf(it.first); buf.writeVector3d(it.second)}
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

        val c2sRequestGravityVectors = "request_gravity_vectors" idWithConnc {
            object : C2SConnection<EmptyPacket>(it, "gravity_settings") {
                override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                    s2cSendGravityVectors.sendToClient(context.player as ServerPlayer, S2CSendGravityVectors())
                }
            }
        }

        val s2cSendGravityVectors = "send_gravity_vectors" idWithConns {
            object : S2CConnection<S2CSendGravityVectors>(it, "gravity_settings") {
                override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                    S2CSendGravityVectors().deserialize(buf)
                    callback?.invoke()
                    callback = null
                }
            }
        }

        val c2sTryUpdateGravityVectors = "try_update_gravity_vectors" idWithConnc {
            object : C2SConnection<C2STryUpdateGravityVectors>(it, "gravity_settings") {
                override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                    val player = context.player as ServerPlayer
                    if (!(ServerToolGunState.playerHasAccess(player) && player.hasPermissions(VMConfig.SERVER.PERMISSIONS.VMOD_CHANGING_SERVER_SETTINGS_LEVEL))) {
                        s2cDimensionalGravityUpdateWasRejected.sendToClient(player, EmptyPacket())
                        return
                    }

                    vectorData.forEach { VSGravityManager.setGravity(it.first, it.second) }
                }
            }
        }

        private val s2cDimensionalGravityUpdateWasRejected = "dimensional_gravity_update_was_rejected" idWithConns {
            object : S2CConnection<EmptyPacket>(it, "gravity_settings") {
                override fun clientHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) {
                    ClientToolGunState.closeGUI()
                    ClientToolGunState.addHUDError("Dimensional Gravity update was rejected")
                }
            }
        }
    }
}