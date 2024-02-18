package net.spaceeye.vsource.toolgun.modes

import dev.architectury.event.EventResult
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vsource.gui.TextEntry

class WeldMode() : BaseMode {
    override val itemName = "Weld"

    var compliance:Double = 1e-10
    var maxForce: Double = 1e10

    override fun handleKeyEvent(minecraft: Minecraft, key: Int, scancode: Int, action: Int, mods: Int): EventResult {
        return EventResult.pass()
    }

    override fun handleMouseButtonClickEvent(minecraft: Minecraft, button: Int, action: Int, mods: Int): EventResult {
        return EventResult.pass()
    }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeDouble(compliance)
        buf.writeDouble(maxForce)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        compliance = buf.readDouble()
        maxForce = buf.readDouble()
    }

    override fun makeGUISettings(parentWindow: UIBlock) {
        val offset = 4

        var entry = TextEntry("Compliance") {
            compliance = it.toDoubleOrNull() ?: return@TextEntry
        }
            .constrain {
            x = offset.pixels()
            y = offset.pixels()

            width = 100.percent() - (offset * 2).pixels()
        } childOf parentWindow
        entry.textArea.setText(compliance.toString())

        entry = TextEntry("Max Force") {
            maxForce = it.toDoubleOrNull() ?: return@TextEntry
        }
            .constrain {
            x = offset.pixels()
            y = SiblingConstraint(2f)

            width = 100.percent() - (offset * 2).pixels()
        } childOf parentWindow
        entry.textArea.setText(maxForce.toString())
    }
}