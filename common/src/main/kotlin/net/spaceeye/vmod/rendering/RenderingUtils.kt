package net.spaceeye.vmod.rendering

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Vector4d
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object RenderingUtils {
    @JvmStatic val ropeTexture  = ResourceLocation(VM.MOD_ID, "textures/misc/rope.png")
    @JvmStatic val whiteTexture = ResourceLocation(VM.MOD_ID, "textures/misc/white.png")
    @JvmStatic val testTexture = ResourceLocation(VM.MOD_ID, "textures/misc/test_texture.png")
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
        @JvmStatic fun drawQuad(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int, lightmapUV: Int,
                                       lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d) {
            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv2(lightmapUV).endVertex()
        }

        @JvmStatic fun drawQuad(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int, lightmapUV: Int, overlayUV: Int,
                                       lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d, normal: Vector3d) {
            //u is width, v is length
            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv(0f, 0f).overlayCoords(overlayUV).uv2(lightmapUV).normal(tof(normal.x), tof(normal.y), tof(normal.z)).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv(0f, 0f).overlayCoords(overlayUV).uv2(lightmapUV).normal(tof(normal.x), tof(normal.y), tof(normal.z)).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv(0f, 0f).overlayCoords(overlayUV).uv2(lightmapUV).normal(tof(normal.x), tof(normal.y), tof(normal.z)).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv(0f, 0f).overlayCoords(overlayUV).uv2(lightmapUV).normal(tof(normal.x), tof(normal.y), tof(normal.z)).endVertex()
        }

        @JvmStatic fun drawQuad(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int,
                                leftLight: Int, rightLight: Int,
                                lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d,
                                u0: Float, u1: Float, v0: Float, v1: Float) {
            drawQuad(buf, matrix, r, g, b, a, leftLight, rightLight, lu, ld, rd, ru, u0, u1, v0, v1, 0f, 1f, 0f)
        }

        @JvmStatic fun drawQuad(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int,
                                leftLight: Int, rightLight: Int,
                                lu: Vector3d, ld: Vector3d, rd: Vector3d, ru: Vector3d,
                                u0: Float, u1: Float, v0: Float, v1: Float,
                                nx: Float, ny: Float, nz: Float) {
            buf.vertex(matrix, tof(lu.x), tof(lu.y), tof(lu.z)).color(r, g, b, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(leftLight ).normal(nx, ny, nz).endVertex()
            buf.vertex(matrix, tof(ld.x), tof(ld.y), tof(ld.z)).color(r, g, b, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(leftLight ).normal(nx, ny, nz).endVertex()
            buf.vertex(matrix, tof(rd.x), tof(rd.y), tof(rd.z)).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(rightLight).normal(nx, ny, nz).endVertex()
            buf.vertex(matrix, tof(ru.x), tof(ru.y), tof(ru.z)).color(r, g, b, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(rightLight).normal(nx, ny, nz).endVertex()
        }

        @JvmStatic fun makePolygonPoints(sides: Int, radius: Double, up: Vector3d, right: Vector3d, pos: Vector3d): List<Vector3d> {
            val segment = PI * 2.0 / sides
            val points = mutableListOf<Vector3d>()
            for (i in 0 until sides) {
                points.add(
                    (up * sin(segment * i)
                + right * cos(segment * i)) * radius + pos)
            }

            return points
        }

        @JvmStatic fun drawPolygonTube(buf: VertexConsumer, matrix: Matrix4f, r: Int, g: Int, b: Int, a: Int,
                                       leftLight: Int, rightLight: Int,
                                       luv0: Float, luv1: Float, wuvStart: Float, maxWidthUV: Float,
                                       lpoints: List<Vector3d>, rpoints: List<Vector3d>) {
            val times = lpoints.size

            var lastUV = wuvStart
            for (i in 0 until times-1) {
                val nextUV = lastUV + maxWidthUV / times
                drawQuad(buf, matrix, r, g, b, a, leftLight, rightLight,
                    lpoints[i], lpoints[i+1], rpoints[i+1], rpoints[i],
                    luv0, luv1, lastUV, nextUV)
                lastUV = nextUV
            }
            drawQuad(buf, matrix, r, g, b, a, leftLight, rightLight,
                lpoints[times-1], lpoints[0], rpoints[0], rpoints[times-1],
                luv0, luv1, lastUV, maxWidthUV)
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

        @JvmStatic fun lerp(a: Vector3d, b: Vector3d, f: Double) : Vector3d {
			return a + (b - a) * f
		}

		// x is position on rope (from 0 to 1)
		// l is available length (ie. initial distance - actual distance)
        @JvmStatic fun height(l: Double, x: Double) : Double {
			val a = 2*l
			return x*a*(x - 1)
		}

        @JvmStatic fun drawFlatRope(
            buf: VertexConsumer, matrix: Matrix4f,
            r: Int, g: Int, b: Int, a: Int,
            width: Double, segments: Int, initialLength: Double,
            pos1: Vector3d, pos2: Vector3d,
            luvStart: Float = 0f, luvIncMultiplier: Float = 1f,
            wuvStart: Float = 0f, wuvMultiplier: Float = 1f,
            lightmapUVFn: (Vector3d) -> Int,
        ) {
            var lastUV = luvStart
            //TODO do i want width to determine V?
            val widthUV = (width * 2f * wuvMultiplier).toFloat()
        ropePointsCreator(pos1, pos2, segments, initialLength) { i, last, current, up0, up1 ->
            val lu =  up0 * width + last
            val ld = -up0 * width + last
            val rd = -up1 * width + current
            val ru =  up1 * width + current

            val prev = lightmapUVFn(last)
            val curr = lightmapUVFn(current)

            var inc = (current - last).dist().toFloat() * luvIncMultiplier
            var nextUV = lastUV + inc

            drawQuad(buf, matrix, r, g, b, a, prev, curr, lu, ld, rd, ru, lastUV, nextUV, wuvStart, widthUV)

            lastUV = nextUV
        } }

        @JvmStatic fun drawTubeRope(
            buf: VertexConsumer, matrix: Matrix4f,
            r: Int, g: Int, b: Int, a: Int,
            radius: Double, segments: Int, sides: Int, initialLength: Double,
            pos1: Vector3d, pos2: Vector3d,
            up1: Vector3d, right1: Vector3d, up2: Vector3d, right2: Vector3d,
            lerpBetweenShipRotations: Boolean,
            useDefinedUpRight: Boolean,
            luvStart: Float = 0f, luvIncMultiplier: Float = 1f,
            wuvStart: Float = 0f, wuvMultiplier: Float = 1f,
            lightmapUVFn: (Vector3d) -> Int,
        ) {
            val rot1 = Matrix4d(right1.toJomlVector4d(), up1.toJomlVector4d(), right1.cross(up1).toJomlVector4d(), Vector4d())
            val rot2 = Matrix4d(right2.toJomlVector4d(), up2.toJomlVector4d(), right2.cross(up2).toJomlVector4d(), Vector4d())

            var cRot: Matrix4d = rot1

            var rPoints: List<Vector3d>
            var lPoints: List<Vector3d>

            var up:    Vector3d = up1
            var right: Vector3d = right1

            if (!useDefinedUpRight) {
                val l = (initialLength - (pos1-pos2).dist()).coerceAtLeast(0.0)
                val pos2 = lerp(pos1, pos2, 1.0 / segments)
                pos2.y += height(l, 1.0 / segments)

                val forward = (pos2 - pos1).snormalize()
                right = forward.cross(0, 1, 0).snormalize()
                up = right.cross(forward).snormalize()
            }

            var lLight = lightmapUVFn(pos1)

            lPoints = makePolygonPoints(sides, radius, up, right, pos1)

            //TODO do i want width to determine V?
            val widthUV = (radius * 2f * wuvMultiplier).toFloat()
            var leftUV = luvStart

            ropePointsCreator(pos1, pos2, segments, initialLength) { i, last, current, up0, up1 ->
                if ((current - pos2).sqrDist() < 0.001 && useDefinedUpRight) {
                    right = right2
                    up = up2
                } else if (lerpBetweenShipRotations) {
                    cRot = rot1.lerp(rot2, i.toDouble() / segments)

                    right = Vector3d(cRot.getColumn(0, JVector3d()))
                    up = Vector3d(cRot.getColumn(1, JVector3d()))
                } else {
                    val forward = (current - last).snormalize()
                    right = forward.cross(0, 1, 0).snormalize()
                    up = right.cross(forward).snormalize()
                }

                rPoints = makePolygonPoints(sides, radius, up, right, current)

                val rLight = lightmapUVFn(current)
                val rightUV = leftUV + (current - last).dist().toFloat() * luvIncMultiplier

                drawPolygonTube(buf, matrix, r, g, b, a, lLight, rLight, leftUV, rightUV, wuvStart, widthUV, lPoints, rPoints)

                leftUV = rightUV
                lPoints = rPoints
                lLight = rLight
            }
        }

        @JvmStatic fun ropePointsCreator(
            pos1: Vector3d, pos2: Vector3d,
            segments: Int, initialLength: Double,
            fn: (i: Int, last: Vector3d, current: Vector3d, up0: Vector3d, up1: Vector3d) -> Unit
        ) {
            if (segments < 1) return

            var l = initialLength - (pos1-pos2).dist()
            if (l < 0.0) l = 0.0

            for (i in 1..segments) {
                val x0 = (i-1).toDouble() / segments
                val x1 =  i   .toDouble() / segments
                var last    = lerp(pos1, pos2, x0)
                var current = lerp(pos1, pos2, x1)
                last   .y += height(l, x0)
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

                val up0 = last   .normalize().cross(d0).normalize()
                val up1 = current.normalize().cross(d1).normalize()

                fn(i, last, current, up0, up1)
            }
        }
    }
}