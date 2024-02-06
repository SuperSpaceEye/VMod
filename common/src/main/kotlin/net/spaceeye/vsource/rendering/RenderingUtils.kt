package net.spaceeye.vsource.rendering

import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Matrix4f
import net.spaceeye.vsource.utils.Vector3d

object RenderingUtils {
    object Quad {
        inline fun makeBoxTube(buf: VertexConsumer, matrix: Matrix4f,
                               r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
                               b1: Vector3d, b2: Vector3d, b3: Vector3d, b4: Vector3d,
                               t1: Vector3d, t2: Vector3d, t3: Vector3d, t4: Vector3d,
                               ) {
            buf.vertex(matrix,  b1.x.toFloat(), b1.y.toFloat(), b1.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  t1.x.toFloat(), t1.y.toFloat(), t1.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  t2.x.toFloat(), t2.y.toFloat(), t2.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  b2.x.toFloat(), b2.y.toFloat(), b2.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()

            buf.vertex(matrix,  b2.x.toFloat(), b2.y.toFloat(), b2.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  t2.x.toFloat(), t2.y.toFloat(), t2.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  t3.x.toFloat(), t3.y.toFloat(), t3.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  b3.x.toFloat(), b3.y.toFloat(), b3.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()

            buf.vertex(matrix,  b3.x.toFloat(), b3.y.toFloat(), b3.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  t3.x.toFloat(), t3.y.toFloat(), t3.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  t4.x.toFloat(), t4.y.toFloat(), t4.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  b4.x.toFloat(), b4.y.toFloat(), b4.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()

            buf.vertex(matrix,  b4.x.toFloat(), b4.y.toFloat(), b4.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  t4.x.toFloat(), t4.y.toFloat(), t4.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  t1.x.toFloat(), t1.y.toFloat(), t1.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  b1.x.toFloat(), b1.y.toFloat(), b1.z.toFloat()).color(r, g, b, a).uv2(lightmapUV).endVertex()
        }
    }
}