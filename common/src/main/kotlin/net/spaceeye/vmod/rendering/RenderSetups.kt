package net.spaceeye.vmod.rendering

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.GameRenderer
import org.lwjgl.opengl.GL11

object RenderSetups {
    fun setupFullRendering(): VertexFormat {
        RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER.setupRenderState()

        RenderSystem.enableBlend()
        RenderSystem.disableCull()
        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getRendertypeTranslucentShader)
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer()

        return GameRenderer.getRendertypeTranslucentShader()!!.vertexFormat
    }

    fun clearFullRendering() {
        RenderSystem.enableCull()
    }

    fun setupPCRendering(): VertexFormat {
        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.enableBlend()
        RenderSystem.disableCull()
        RenderSystem.setShaderTexture(0, RenderingUtils.whiteTexture)

        return DefaultVertexFormat.POSITION_COLOR
    }

    fun clearPCRendering() {
        RenderSystem.enableCull()
    }
}