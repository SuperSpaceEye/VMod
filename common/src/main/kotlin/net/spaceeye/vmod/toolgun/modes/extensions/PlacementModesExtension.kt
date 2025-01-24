package net.spaceeye.vmod.toolgun.modes.extensions

import gg.essential.elementa.components.UIContainer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.rendering.ClientRenderingData
import net.spaceeye.vmod.rendering.types.special.PrecisePlacementAssistRenderer
import net.spaceeye.vmod.toolgun.modes.ToolgunModeExtension
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.translate.*

open class PlacementModesExtension(
    val showCenteredInBlock: Boolean,
    val setPosMode: (PositionModes) -> Unit,
    val setPrecisePlacementAssistSideNum: (Int) -> Unit
): ToolgunModeExtension {
    var posMode: PositionModes = PositionModes.NORMAL
    var precisePlacementAssistSideNum: Int = 3

    var precisePlacementAssistRendererId: Int = -1

    private var internalPrecisePlacementAssistSideNum: Int
        get() = precisePlacementAssistSideNum
        set(value) {
            precisePlacementAssistSideNum = value
            if (posMode != PositionModes.PRECISE_PLACEMENT) {return}
            ClientRenderingData.removeClientsideRenderer(precisePlacementAssistRendererId)
            precisePlacementAssistRendererId = ClientRenderingData.addClientsideRenderer(PrecisePlacementAssistRenderer(precisePlacementAssistSideNum))
        }

    override fun serialize(): FriendlyByteBuf {
        val buf = getBuffer()

        buf.writeEnum(posMode)
        buf.writeInt(precisePlacementAssistSideNum)

        return buf
    }

    override fun deserialize(buf: FriendlyByteBuf) {
        posMode = buf.readEnum(posMode.javaClass)
        precisePlacementAssistSideNum = buf.readInt()

        setPosMode(posMode)
        setPrecisePlacementAssistSideNum(precisePlacementAssistSideNum)
    }

    override fun serverSideVerifyLimits() {
        precisePlacementAssistSideNum = ServerLimits.instance.precisePlacementAssistSides.get(precisePlacementAssistSideNum)
        setPrecisePlacementAssistSideNum(precisePlacementAssistSideNum)
    }

    override fun eOnOpenMode() {
        if (posMode != PositionModes.PRECISE_PLACEMENT) {return}
        precisePlacementAssistRendererId = ClientRenderingData.addClientsideRenderer(PrecisePlacementAssistRenderer(precisePlacementAssistSideNum))
    }

    override fun eOnCloseMode() {
        ClientRenderingData.removeClientsideRenderer(precisePlacementAssistRendererId)
    }

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        val offset = 2f
        makeTextEntry(PRECISE_PLACEMENT_ASSIST_SIDES.get(), ::internalPrecisePlacementAssistSideNum, offset, offset, parentWindow, ServerLimits.instance.precisePlacementAssistSides)
        if (showCenteredInBlock) {
            makeDropDown(
                HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
                DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL           ; ClientRenderingData.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE ; ClientRenderingData.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK; ClientRenderingData.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(PRECISE_PLACEMENT.get(), posMode == PositionModes.PRECISE_PLACEMENT) {
                    posMode = PositionModes.PRECISE_PLACEMENT
                    ClientRenderingData.removeClientsideRenderer(precisePlacementAssistRendererId)
                    precisePlacementAssistRendererId = ClientRenderingData.addClientsideRenderer(PrecisePlacementAssistRenderer(precisePlacementAssistSideNum))
                })
            )
        } else {
            makeDropDown(HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
                DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL           ; ClientRenderingData.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE ; ClientRenderingData.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(PRECISE_PLACEMENT.get(), posMode == PositionModes.PRECISE_PLACEMENT) {
                    posMode = PositionModes.PRECISE_PLACEMENT
                    ClientRenderingData.removeClientsideRenderer(precisePlacementAssistRendererId)
                    precisePlacementAssistRendererId = ClientRenderingData.addClientsideRenderer(PrecisePlacementAssistRenderer(precisePlacementAssistSideNum))
                })
            )
        }
    }
}