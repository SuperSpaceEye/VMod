package net.spaceeye.vmod.rendering

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Matrix4f
import net.minecraft.client.renderer.texture.OverlayTexture
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object RenderingUtils {
    @JvmStatic val ropeTexture  = ResourceLocation(VM.MOD_ID, "textures/misc/rope.png")
    @JvmStatic val whiteTexture = ResourceLocation(VM.MOD_ID, "textures/misc/white.png")
    @JvmStatic fun tof(n: Double) = n.toFloat()
    object Line {
        @JvmStatic fun renderLine(buf: VertexConsumer, matrix: Matrix4f, color: Color,
                                         start: Vector3d, stop: Vector3d, width:Double = 0.5) {
            val wdir = stop - start
            val pdir = (-start - wdir * ((-start).dot(wdir) / wdir.dot(wdir))).snormalize()
            val up = pdir.cross(wdir.normalize())

            val lu =  up * width + start
            val ld = -up * width + start

            val ru = -up * width + stop
            val rd =  up * width + stop

            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(color.rgb).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(color.rgb).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(color.rgb).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(color.rgb).endVertex()
        }
        @JvmStatic fun renderLineBox(buf: VertexConsumer, matrix: Matrix4f, color: Color, points: List<Vector3d>, width: Double = 0.1) {
            renderLine(buf, matrix, color, points[0], points[1], width)
            renderLine(buf, matrix, color, points[1], points[2], width)
            renderLine(buf, matrix, color, points[2], points[3], width)
            renderLine(buf, matrix, color, points[3], points[0], width)

            renderLine(buf, matrix, color, points[4], points[5], width)
            renderLine(buf, matrix, color, points[5], points[6], width)
            renderLine(buf, matrix, color, points[6], points[7], width)
            renderLine(buf, matrix, color, points[7], points[4], width)

            renderLine(buf, matrix, color, points[4], points[0], width)
            renderLine(buf, matrix, color, points[5], points[1], width)
            renderLine(buf, matrix, color, points[6], points[2], width)
            renderLine(buf, matrix, color, points[7], points[3], width)
        }
    }

    object Quad {
        @JvmStatic fun makeBoxTube(buf: VertexConsumer, matrix: Matrix4f,
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

        @JvmStatic fun drawQuad(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
                                       lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d) {
            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
        }

        @JvmStatic fun drawQuad(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int, lightmapUV: Int, overlayUV: Int,
                                       lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d, normal: Vector3d) {
            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv(0f, 0f).overlayCoords(overlayUV).uv2(lightmapUV).normal(tof(normal.x), tof(normal.y), tof(normal.z)).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv(0f, 0f).overlayCoords(overlayUV).uv2(lightmapUV).normal(tof(normal.x), tof(normal.y), tof(normal.z)).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv(0f, 0f).overlayCoords(overlayUV).uv2(lightmapUV).normal(tof(normal.x), tof(normal.y), tof(normal.z)).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv(0f, 0f).overlayCoords(overlayUV).uv2(lightmapUV).normal(tof(normal.x), tof(normal.y), tof(normal.z)).endVertex()
        }

        @JvmStatic fun drawQuad(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int,
                                       leftLight: Int, rightLight: Int,
                                       lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d, uv0: Float, uv1: Float) {
            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv(uv0, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(leftLight).normal(0f, 1f, 0f).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv(uv0, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(leftLight).normal(0f, 1f, 0f).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv(uv1, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(rightLight).normal(0f, 1f, 0f).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv(uv1, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(rightLight).normal(0f, 1f, 0f).endVertex()
        }

        @JvmStatic fun makePolygon(sides: Int, radius: Double, up: Vector3d, right: Vector3d, pos: Vector3d): List<Vector3d> {
            val segment = PI * 2.0 / sides
            val points = mutableListOf<Vector3d>()
            for (i in 0 until sides) {
                points.add(
                    (up * sin(segment * i)
                + right * cos(segment * i)) * radius + pos)
            }

            return points
        }

        @JvmStatic fun makePolygonTube(sides: Int, radius: Double,
                                          pos1: Vector3d, pos2: Vector3d, up: Vector3d?): Pair<List<Vector3d>, List<Vector3d>> {
            val ndir = (pos2 - pos1).snormalize()
            val sdir = ndir.add(ndir.y, ndir.z, ndir.x).snormalize()

            val up = up ?: sdir.cross(ndir)
            val right = ndir.cross(up)

            val lpoints = makePolygon(sides, radius, up, right, pos1)
            val rpoints = makePolygon(sides, radius, up, right, pos2)

            return Pair(lpoints, rpoints)
        }

        @JvmStatic fun drawPolygonTube(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int,
                                              leftLight: Int, rightLight: Int,
                                              uv0: Float, uv1: Float, lpoints: List<Vector3d>, rpoints: List<Vector3d>) {
            val times = lpoints.size

            for (i in 0 until times-1) {
                drawQuad(buf, matrix, r, g, b, a, leftLight, rightLight,
                    lpoints[i], lpoints[i+1], rpoints[i+1], rpoints[i],
                    uv0, uv1)
            }
            drawQuad(buf, matrix, r, g, b, a, leftLight, rightLight,
                lpoints[times-1], lpoints[0], rpoints[0], rpoints[times-1],
                uv0, uv1)
        }

        @JvmStatic fun makeBoxTube(buf: VertexConsumer, matrix: Matrix4f,
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

        @JvmStatic fun makeFlatRectFacingCamera(buf: VertexConsumer, matrix: Matrix4f,
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

            val rd = -up * width + pos2
            val ru =  up * width + pos2

            drawQuad(buf, matrix, r, g, b, a, lightmapUV, OverlayTexture.NO_OVERLAY, lu, ld, rd, ru, up)
        }

        @JvmStatic fun makeFlatRectFacingCameraTexture(buf: VertexConsumer, matrix: Matrix4f,
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

            drawQuad(buf, matrix, r, g, b, a, lightmapUV, lightmapUV, lu, ld, ru, rd, 0.0f, 1.0f)
        }

        @JvmStatic fun makeFlatRectFacingCameraTexture(
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

            drawQuad(buf, matrix, r, g, b, a, lightmapUV, lightmapUV, lu, ld, ru, rd, 0.0f, 1.0f)
            return Pair(ru, rd)
        }

        @JvmStatic fun lerp(a: Vector3d, b: Vector3d, f: Double) : Vector3d {
			return a + (b - a) * f
		}

		// x is position on rope (from 0 to 1)
		// l is available length (ie. initial distance - actual distance)
        @JvmStatic fun height(l: Double, x: Double) : Double {
			val a = 2*l
			return x*a*(x - 1)
		}

		// ilength is initial rope length
        @JvmStatic fun drawRope(buf: VertexConsumer, matrix: Matrix4f,
            r: Int, g: Int, b: Int, a: Int,
            width: Double, segments: Int, ilength: Double,
            pos1: Vector3d, pos2: Vector3d, lightmapUVFn: (Vector3d) -> Int,
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

                val prev = lightmapUVFn(last)
                val curr = lightmapUVFn(current)

				buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(prev).normal(0f, 1f, 0f).endVertex()
				buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(prev).normal(0f, 1f, 0f).endVertex()
				buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(curr).normal(0f, 1f, 0f).endVertex()
				buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(curr).normal(0f, 1f, 0f).endVertex()
			}
        }
    }
}