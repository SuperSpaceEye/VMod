package net.spaceeye.vsource.rendering

import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Matrix4f
import net.spaceeye.vsource.utils.Vector3d

object RenderingUtils {
    inline fun tof(n: Double) = n.toFloat()
    object Quad {
        inline fun makeBoxTube(buf: VertexConsumer, matrix: Matrix4f,
                               r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
                               b1: Vector3d, b2: Vector3d, b3: Vector3d, b4: Vector3d,
                               t1: Vector3d, t2: Vector3d, t3: Vector3d, t4: Vector3d,
                               ) {
            buf.vertex(matrix,  tof(b1.x), tof(b1.y), tof(b1.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(t1.x), tof(t1.y), tof(t1.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(t2.x), tof(t2.y), tof(t2.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(b2.x), tof(b2.y), tof(b2.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()

            buf.vertex(matrix,  tof(b2.x), tof(b2.y), tof(b2.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(t2.x), tof(t2.y), tof(t2.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(t3.x), tof(t3.y), tof(t3.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(b3.x), tof(b3.y), tof(b3.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()

            buf.vertex(matrix,  tof(b3.x), tof(b3.y), tof(b3.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(t3.x), tof(t3.y), tof(t3.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(t4.x), tof(t4.y), tof(t4.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(b4.x), tof(b4.y), tof(b4.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()

            buf.vertex(matrix,  tof(b4.x), tof(b4.y), tof(b4.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(t4.x), tof(t4.y), tof(t4.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(t1.x), tof(t1.y), tof(t1.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix,  tof(b1.x), tof(b1.y), tof(b1.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
        }

        inline fun makeBoxTube(buf: VertexConsumer, matrix: Matrix4f,
                               r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
                               width: Double,
                               pos1: Vector3d, pos2: Vector3d) {
            val ndir = (pos2 - pos1).snormalize()
            val sdir = ndir.add(ndir.y, ndir.z, ndir.x).snormalize()

            val up = sdir.cross(ndir)
            val right = ndir.cross(up)

            //left down bottom
            val ldb = (-up -right) * width + pos1
            val lub = ( up -right) * width + pos1
            val rub = ( up +right) * width + pos1
            val rdb = (-up +right) * width + pos1

            val ldt = (-up -right) * width + pos2
            val lut = ( up -right) * width + pos2
            val rut = ( up +right) * width + pos2
            val rdt = (-up +right) * width + pos2

            makeBoxTube(buf, matrix, r, g, b, a, lightmapUV, ldb, lub, rub, rdb, ldt, lut, rut, rdt)
        }

        inline fun makeFlatRectFacingCamera(buf: VertexConsumer, matrix: Matrix4f,
                                            r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
                                            width: Double,
                                            pos1: Vector3d, pos2: Vector3d
        ) {
            // wdir = worldPos2 - worldPos1
            // t = (cameraPos - worldPos1).dot(wdir) / wdir.dot(wdir)
            // closestPointToCameraOnLine = worldPos1 + wdir * t
            // perpendicularVector = (cameraPos - closestPointToCameraOnLine).snormalize()
            //
            // cameraPos should be origin, so it becomes

            val wdir = pos2 - pos1
            // perpendicular direction
            val pdir = (-pos1 - wdir * ((-pos1).dot(wdir) / wdir.dot(wdir))).snormalize()

            val up = pdir.cross(wdir.normalize())

            val lu =  up * width + pos1
            val ld = -up * width + pos1

            val ru = -up * width + pos1 + wdir
            val rd =  up * width + pos1 + wdir

            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
        }
    }
}