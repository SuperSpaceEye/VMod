package net.spaceeye.vsource.utils

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderType

class CARenderType(
    p_173178_: String?, p_173179_: VertexFormat?, p_173180_: VertexFormat.Mode?, p_173181_: Int, p_173182_: Boolean,
    p_173183_: Boolean, p_173184_: Runnable?, p_173185_: Runnable?
) : RenderType(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_) {
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
            ).setTextureState(NO_TEXTURE).setCullState(NO_CULL).setLightmapState(LIGHTMAP).createCompositeState(false)
        )
    }
}