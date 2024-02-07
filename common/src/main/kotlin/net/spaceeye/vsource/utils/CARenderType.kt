package net.spaceeye.vsource.utils

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderType

class CARenderType(
    string: String?, vertexFormat: VertexFormat?, mode: VertexFormat.Mode?, i: Int, bl: Boolean,
    bl2: Boolean, runnable: Runnable?, runnable2: Runnable?
) : RenderType(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2) {
    companion object {
        val WIRE: RenderType = create(
            "wire",
            DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            CompositeState.builder().setShaderState(
                RENDERTYPE_LEASH_SHADER
            ).setTextureState(NO_TEXTURE).setCullState(CULL).setLightmapState(LIGHTMAP).createCompositeState(false)
        )
    }
}