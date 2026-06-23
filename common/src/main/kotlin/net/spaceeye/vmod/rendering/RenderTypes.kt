package net.spaceeye.vmod.rendering

import com.google.common.collect.ImmutableMap
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance

data class ShaderState(val name: String, private val builderFn: (RenderType.CompositeState.CompositeStateBuilder) -> RenderType.CompositeState) {
    lateinit var shader: ShaderInstance
    val shaderSetter = {it: ShaderInstance -> shader = it}

    private val stateShard = RenderStateShard.ShaderStateShard {shader}
    private var type_: RenderType? = null
    val type: RenderType get() {
        if (type_ != null) return type_!!

        val builder = RenderType.CompositeState.builder().setShaderState(stateShard)
        type_ = RenderType.create(name, RenderTypes.VERTEX_FORMAT, VertexFormat.Mode.QUADS, 256, builderFn(builder))

        return type_!!
    }

    fun setupRenderState() = type.setupRenderState()
    fun clearRenderState() = type.clearRenderState()

    fun getUniform(name: String) = shader.getUniform(name)
    fun setSampler(name: String, textureId: Any) = shader.setSampler(name, textureId)
}

object RenderTypes {
    val states = mutableListOf<ShaderState>()

    val VERTEX_FORMAT = VertexFormat(ImmutableMap.copyOf(mapOf(
        "Position" to DefaultVertexFormat.ELEMENT_POSITION,
        "Color"    to DefaultVertexFormat.ELEMENT_COLOR,
        "UV0"      to DefaultVertexFormat.ELEMENT_UV0,
        "UV2"      to DefaultVertexFormat.ELEMENT_UV2,
        "Normal"   to DefaultVertexFormat.ELEMENT_NORMAL
    )))

    val textureArrayFull = ShaderState("texture_array_full") {
        it.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
          .setLightmapState(RenderStateShard.LIGHTMAP)
          .setOverlayState(RenderStateShard.OVERLAY)
          .createCompositeState(false)
    }.also { states.add(it) }

    val schematicBlock = ShaderState("schematic_block") {
        it.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
          .setLightmapState(RenderStateShard.LIGHTMAP)
          .setOverlayState(RenderStateShard.NO_OVERLAY)
          .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
          .createCompositeState(true)
    }.also { states.add(it) }
}