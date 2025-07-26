package net.spaceeye.vmod.toolgun.modes.extensions

import gg.essential.elementa.components.UIContainer
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.guiElements.DItem
import net.spaceeye.vmod.guiElements.makeDropDown
import net.spaceeye.vmod.guiElements.makeTextEntry
import net.spaceeye.vmod.limits.ServerLimits
import net.spaceeye.vmod.reflectable.ReflectableItem.get
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.RenderingData
import net.spaceeye.vmod.rendering.types.special.PrecisePlacementAssistRenderer
import net.spaceeye.vmod.toolgun.gui.Presettable.Companion.presettable
import net.spaceeye.vmod.toolgun.modes.BaseNetworking
import net.spaceeye.vmod.toolgun.modes.ExtendableToolgunMode
import net.spaceeye.vmod.toolgun.modes.ToolgunModeExtension
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.translate.*

open class PlacementModesExtension(
    val showCenteredInBlock: Boolean,
): ToolgunModeExtension, ReflectableObject {
    private var precisePlacementAssistRendererId: Int = -1
    private var type: BaseNetworking.EnvType = BaseNetworking.EnvType.Server
    private lateinit var inst: ExtendableToolgunMode

    override fun onInit(mode: ExtendableToolgunMode, type: BaseNetworking.EnvType) {
        this.type = type
        this.inst = mode
    }

    var posMode: PositionModes by get(0, PositionModes.NORMAL).presettable()
    var precisePlacementAssistSideNum: Int by get(1, 3)
        .presettable()
        .setSetWrapper { old, new ->
            if (type == BaseNetworking.EnvType.Client && posMode == PositionModes.PRECISE_PLACEMENT) {
                RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId)
                precisePlacementAssistRendererId = RenderingData.client.addClientsideRenderer(PrecisePlacementAssistRenderer(new) {inst.instance.client.playerIsUsingToolgun()})
            }
            new
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
    }

    override fun serverSideVerifyLimits() {
        precisePlacementAssistSideNum = ServerLimits.instance.precisePlacementAssistSides.get(precisePlacementAssistSideNum)
    }

    override fun eOnOpenMode() {
        if (posMode != PositionModes.PRECISE_PLACEMENT) {return}
        RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId)
        precisePlacementAssistRendererId = RenderingData.client.addClientsideRenderer(PrecisePlacementAssistRenderer(precisePlacementAssistSideNum) {inst.instance.client.playerIsUsingToolgun()})
    }

    override fun eOnCloseMode() {
        RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId)
    }

    override fun eMakeGUISettings(parentWindow: UIContainer) {
        val offset = 2f
        makeTextEntry(PRECISE_PLACEMENT_ASSIST_SIDES.get(), ::precisePlacementAssistSideNum, offset, offset, parentWindow, ServerLimits.instance.precisePlacementAssistSides)
        if (showCenteredInBlock) {
            makeDropDown(
                HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
                DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL           ; RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE ; RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(CENTERED_IN_BLOCK.get(), posMode == PositionModes.CENTERED_IN_BLOCK) { posMode = PositionModes.CENTERED_IN_BLOCK; RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(PRECISE_PLACEMENT.get(), posMode == PositionModes.PRECISE_PLACEMENT) {
                    posMode = PositionModes.PRECISE_PLACEMENT
                    RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId)
                    precisePlacementAssistRendererId = RenderingData.client.addClientsideRenderer(PrecisePlacementAssistRenderer(precisePlacementAssistSideNum) {inst.instance.client.playerIsUsingToolgun()})
                })
            )
        } else {
            makeDropDown(HITPOS_MODES.get(), parentWindow, offset, offset, listOf(
                DItem(NORMAL.get(),            posMode == PositionModes.NORMAL)            { posMode = PositionModes.NORMAL           ; RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(CENTERED_ON_SIDE.get(),  posMode == PositionModes.CENTERED_ON_SIDE)  { posMode = PositionModes.CENTERED_ON_SIDE ; RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId) },
                DItem(PRECISE_PLACEMENT.get(), posMode == PositionModes.PRECISE_PLACEMENT) {
                    posMode = PositionModes.PRECISE_PLACEMENT
                    RenderingData.client.removeClientsideRenderer(precisePlacementAssistRendererId)
                    precisePlacementAssistRendererId = RenderingData.client.addClientsideRenderer(PrecisePlacementAssistRenderer(precisePlacementAssistSideNum) {inst.instance.client.playerIsUsingToolgun()})
                })
            )
        }
    }
}