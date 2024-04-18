package net.spaceeye.vmod.rendering

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Matrix4f

object RenderingUtils {
    val ropeTexture = ResourceLocation(VM.MOD_ID, "textures/misc/rope.png")
    @JvmStatic inline fun tof(n: Double) = n.toFloat()
    object Quad {
        @JvmStatic inline fun makeBoxTube(buf: VertexConsumer, matrix: Matrix4f,
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

        @JvmStatic inline fun drawQuad(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
                            lu: Vector3d, ld: Vector3d, ru: Vector3d, rd: Vector3d) {
            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
        }

        @JvmStatic inline fun drawQuad(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
                                       lu: Vector3d, ld: Vector3d, ru: Vector3d, rd: Vector3d, uv0: Float, uv1: Float) {
            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv(uv0, 1.0f).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv(uv0, 0.0f).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv(uv1, 0.0f).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv(uv1, 1.0f).uv2(lightmapUV).endVertex()
        }

        @JvmStatic inline fun makeBoxTube(buf: VertexConsumer, matrix: Matrix4f,
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

        @JvmStatic inline fun makeFlatRectFacingCamera(buf: VertexConsumer, matrix: Matrix4f,
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

            val ru = -up * width + pos2
            val rd =  up * width + pos2

            drawQuad(buf, matrix, r, g, b, a, lightmapUV, lu, ld, ru, rd)
        }

        @JvmStatic inline fun makeFlatRectFacingCameraTexture(buf: VertexConsumer, matrix: Matrix4f,
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

            val ru = -up * width + pos2
            val rd =  up * width + pos2

            drawQuad(buf, matrix, r, g, b, a, lightmapUV, lu, ld, ru, rd, 0.0f, 1.0f)
        }

        @JvmStatic inline fun makeFlatRectFacingCameraTexture(
            buf: VertexConsumer, matrix: Matrix4f,
            r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
            width: Double,
            lu: Vector3d, ld: Vector3d, pos1: Vector3d, pos2: Vector3d
        ): Pair<Vector3d, Vector3d> {
            val wdir = pos2 - pos1
            // perpendicular direction
            val pdir = (-pos1 - wdir * ((-pos1).dot(wdir) / wdir.dot(wdir))).snormalize()

            val up = pdir.cross(wdir.normalize())

            val ru = -up * width + pos2
            val rd =  up * width + pos2

            drawQuad(buf, matrix, r, g, b, a, lightmapUV, lu, ld, ru, rd, 0.0f, 1.0f)
            return Pair(ru, rd)
        }

        @JvmStatic inline fun lerp(a: Vector3d, b: Vector3d, f: Double) : Vector3d {
			return a + (b - a) * f
		}
		
		// x is position on rope (from 0 to 1)
		// l is available length (ie. initial distance - actual distance)
        @JvmStatic inline fun height(l: Double, x: Double) : Double {
			val a = 2*l
			return x*a*(x - 1)
		}
		
		// ilength is initial rope length
        @JvmStatic inline fun drawRope(buf: VertexConsumer, matrix: Matrix4f,
            r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
            width: Double, segments: Int, ilength: Double,
            pos1: Vector3d, pos2: Vector3d
        ) {
            if (segments < 1) return
			
			var l = ilength - (pos1-pos2).dist()
			if (l < 0.0) l = 0.0
			
			for (i in 1..segments) {
				val x0 = (i-1).toDouble() / segments
				val x1 = i.toDouble() / segments
				var last = lerp(pos1, pos2, x0)
				var current = lerp(pos1, pos2, x1)
				last.y += height(l, x0)
				current.y += height(l, x1)
				var d0 = last-current
				var d1 = d0
				if (i > 1) {
					val xm = (i-2).toDouble() / segments
					d0 = lerp(pos1, pos2, xm)-last + d0
					d0.y += height(l, xm)
				}
				if (i < segments) {
					val x2 = (i+1).toDouble() / segments
					d1 = current-lerp(pos1, pos2, x2) + d1
					d1.y -= height(l, x2)
				}
				d0 = d0.normalize()
				d1 = d1.normalize()

				val up0 = last.normalize().cross(d0).normalize()
				val up1 = current.normalize().cross(d1).normalize()
				
				val lu =  up0 * width + last
				val ld = -up0 * width + last
				val ru = -up1 * width + current
				val rd =  up1 * width + current

				buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv(tof(0.0), 0.0f).uv2(lightmapUV).endVertex()
				buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv(tof(0.0), 1.0f).uv2(lightmapUV).endVertex()
				buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv(tof(1.0), 1.0f).uv2(lightmapUV).endVertex()
				buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv(tof(1.0), 0.0f).uv2(lightmapUV).endVertex()
			}
        }
    }
}