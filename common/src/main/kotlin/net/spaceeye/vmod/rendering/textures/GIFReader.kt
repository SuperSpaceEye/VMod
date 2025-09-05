package net.spaceeye.vmod.rendering.textures

import com.mojang.blaze3d.platform.NativeImage
import net.spaceeye.vmod.GIFUtils
import net.spaceeye.vmod.GLMaxTextureSize
import net.spaceeye.vmod.mixin.NativeImageInvoker
import org.lwjgl.system.MemoryUtil
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.InputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageInputStreamImpl

class WrappedByteArrayInputStream(val array: ByteArray): ImageInputStreamImpl() {
    override fun read(): Int {
        if (streamPos >= array.size) return -1
        val ret = array[streamPos.toInt()].toUByte().toInt()
        streamPos++
        return ret
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        if (b == null) throw NullPointerException("b == null!")
        if (off < 0 || len < 0 || off + len > b.size || off + len < 0) {
            throw IndexOutOfBoundsException("off < 0 || len < 0 || off+len > b.length || off+len < 0!")
        }

        bitOffset = 0

        if (len == 0) return 0
        if (streamPos + len > array.size) return -1

        var i = 0
        while (i < len) {
            b[off + i] = array[streamPos.toInt() + i]
            i++
        }
        streamPos += len

        return len
    }

    override fun length(): Long = array.size.toLong()

    override fun isCached(): Boolean = true
    override fun isCachedFile(): Boolean = true
    override fun isCachedMemory(): Boolean = true
}

//https://stackoverflow.com/a/18425922
object GIFReader {
    val reader = ImageIO.getImageReadersByFormatName("gif").next()

    data class ImageFrame(val image: BufferedImage, val delay: Int, val disposal: String)
    @JvmStatic fun readGIF(stream: InputStream): MutableList<ImageFrame> {
        reader.setInput(ImageIO.createImageInputStream(stream))

        val frames = mutableListOf<ImageFrame>()

        var width = -1
        var height = -1

        val metadata = reader.streamMetadata
        if (metadata != null) {
            val globalRoot = metadata.getAsTree(metadata.getNativeMetadataFormatName()) as IIOMetadataNode

            val globalScreenDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor")

            if (globalScreenDescriptor != null && globalScreenDescriptor.length > 0) {
                val screenDescriptor = globalScreenDescriptor.item(0) as IIOMetadataNode?

                if (screenDescriptor != null) {
                    width = screenDescriptor.getAttribute("logicalScreenWidth").toInt()
                    height = screenDescriptor.getAttribute("logicalScreenHeight").toInt()
                }
            }
        }

        var master: BufferedImage? = null
        var masterGraphics: Graphics2D? = null

        var frameIndex = 0
        while (true) {
            val image = try { reader.read(frameIndex) } catch (_: IndexOutOfBoundsException) { break }

            if (width == -1 || height == -1) {
                width = image.width
                height = image.height
            }

            val root = reader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode
            val gce = root.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
            val delay = gce.getAttribute("delayTime").toInt()
            val disposal = gce.getAttribute("disposalMethod")

            var x = 0
            var y = 0

            if (master == null) {
                master = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                masterGraphics = master.createGraphics()
                masterGraphics.background = Color(0, 0, 0, 0)
            } else {
                val children = root.childNodes
                for (nodeIndex in 0 until children.length) {
                    val nodeItem = children.item(nodeIndex)
                    if (nodeItem.nodeName == "ImageDescriptor") {
                        val map = nodeItem.attributes
                        x = map.getNamedItem("imageLeftPosition").nodeValue.toInt()
                        y = map.getNamedItem("imageTopPosition").nodeValue.toInt()
                    }
                }
            }
            masterGraphics!!.drawImage(image, x, y, null)

            val copy = BufferedImage(master.colorModel, master.copyData(null), master.isAlphaPremultiplied, null)
            frames.add(ImageFrame(copy, delay, disposal))

            if (disposal == "restoreToPrevious") {
                var from: BufferedImage? = null
                for (i in frameIndex - 1 downTo 0) {
                    if (frames[i].disposal != "restoreToPrevious" || frameIndex == 0) {
                        from = frames[i].image
                        break
                    }
                }

                master = BufferedImage(from!!.colorModel, from.copyData(null), from.isAlphaPremultiplied, null)
                masterGraphics = master.createGraphics()
                masterGraphics.background = Color(0, 0, 0, 0)
            } else if (disposal == "restoreToBackgroundColor") {
                masterGraphics.clearRect(x, y, image.width, image.height)
            }
            frameIndex++
        }
        reader.dispose()

        return frames
    }

    data class NativeTextureWithData(
        var image: NativeImage,
        var buffer: ByteBuffer,
        var ptr: Long,
        var delays: MutableList<Int>,
        var frameWidth: Int,
        var frameHeight: Int,
        var spriteWidth: Int,
        var spriteHeight: Int,
        var widthTiles: Int,
        var heightTiles: Int,
        var numFrames: Int,
    )

    class NativeTextureBuilder(
        val frameWidth: Int,
        val frameHeight: Int,
        var maxWidth: Int,
        var maxHeight: Int
    ) {
        data class Data(val spriteWidth: Int, val spriteHeight: Int, val widthTiles: Int, val heightTiles: Int, val numFrames: Int)
        fun calculateDimensions(requiredFrames: Int): Data {
            val maxWidthTiles = maxWidth / frameWidth
            val maxHeightTiles = maxHeight / frameHeight

            var remainderTiles = 0
            val widthTiles = if (maxWidthTiles >= requiredFrames) { requiredFrames } else { maxWidthTiles }
            val heightTiles = if (maxWidthTiles >= requiredFrames) { 1 } else {
                if (maxWidthTiles * maxHeightTiles >= requiredFrames) {
                    remainderTiles = requiredFrames % maxWidthTiles
                    requiredFrames / maxWidthTiles + if (remainderTiles == 0) 0 else 1
                } else {
                    maxHeightTiles
                }
            }

            val spriteWidth = widthTiles * frameWidth
            val spriteHeight = heightTiles * frameHeight

            val numFrames = if (remainderTiles == 0) {
                widthTiles * heightTiles
            } else {
                widthTiles * (heightTiles - 1) + remainderTiles
            }

            return Data(spriteWidth, spriteHeight, widthTiles, heightTiles, numFrames)
        }

        fun makeEmpty(requiredFrames: Int): NativeTextureWithData {
            val (spriteWidth, spriteHeight, widthTiles, heightTiles, numFrames) = calculateDimensions(requiredFrames)

            val size = spriteWidth * spriteHeight * 4
            val ptr = MemoryUtil.nmemAlloc(size.toLong())
            val buf = MemoryUtil.memByteBuffer(ptr, size)
            val img = NativeImageInvoker.theConstructor(NativeImage.Format.RGBA, spriteWidth, spriteHeight, false, ptr)

            return NativeTextureWithData(
                img, buf, ptr,
                mutableListOf(),
                frameWidth,
                frameHeight,
                spriteWidth,
                spriteHeight,
                widthTiles,
                heightTiles,
                numFrames
            )
        }
    }

    @JvmStatic inline fun argb2rgba(it: Int): Int {
        return 0 or
                ((it and 0x00ff0000) shr 2*8) or
                ((it and 0x0000ff00)) or
                ((it and 0x000000ff) shl 2*8) or
                ((it and -16777216)) // it's 0xff000000, java is stupid
    }

    @JvmStatic fun readGifToTexturesFaster(originalStream: InputStream): MutableList<NativeTextureWithData> {
        val bytes = originalStream.readAllBytes()

        var stream = WrappedByteArrayInputStream(bytes)
        reader.reset()
        reader.setInput(stream)

        reader.streamMetadata
        val numFrames = GIFUtils.getNumImages(stream)

        reader.reset()
        stream = WrappedByteArrayInputStream(bytes)
        reader.setInput(stream)

        var width = -1
        var height = -1

        val metadata = reader.streamMetadata
        if (metadata != null) {
            val globalRoot = metadata.getAsTree(metadata.getNativeMetadataFormatName()) as IIOMetadataNode

            val globalScreenDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor")

            if (globalScreenDescriptor != null && globalScreenDescriptor.length > 0) {
                val screenDescriptor = globalScreenDescriptor.item(0) as IIOMetadataNode?

                if (screenDescriptor != null) {
                    width = screenDescriptor.getAttribute("logicalScreenWidth").toInt()
                    height = screenDescriptor.getAttribute("logicalScreenHeight").toInt()
                }
            }
        }

        val textureBuilder = NativeTextureBuilder(width, height, width, GLMaxTextureSize)
        val textures = mutableListOf<NativeTextureWithData>()
        val (_, _, _, _, framesPerTexture) = textureBuilder.calculateDimensions(numFrames)

        var disposals = mutableListOf<String>()

        var master: BufferedImage? = null
        var masterGraphics: Graphics2D? = null
        var frameIndex = 0
        var remainingFrames = numFrames
        while (true) {
            val image = try { reader.read(frameIndex) } catch (_: IndexOutOfBoundsException) { break }

            if (width == -1 || height == -1) {
                width = image.width
                height = image.height
            }

            val root = reader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode
            val gce = root.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
            val delay = gce.getAttribute("delayTime").toInt()
            val disposal = gce.getAttribute("disposalMethod")

            var x = 0
            var y = 0

            if (master == null) {
                master = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                masterGraphics = master.createGraphics()
                masterGraphics.background = Color(0, 0, 0, 0)
            } else {
                val children = root.childNodes
                for (nodeIndex in 0 until children.length) {
                    val nodeItem = children.item(nodeIndex)
                    if (nodeItem.nodeName == "ImageDescriptor") {
                        val map = nodeItem.attributes
                        x = map.getNamedItem("imageLeftPosition").nodeValue.toInt()
                        y = map.getNamedItem("imageTopPosition").nodeValue.toInt()
                    }
                }
            }
            masterGraphics!!.drawImage(image, x, y, null)

            val texture = textures.getOrNull(frameIndex / framesPerTexture) ?: run {
                val texture = textureBuilder.makeEmpty(remainingFrames)
                textures.add(texture)
                remainingFrames -= texture.numFrames
                texture
            }
            texture.delays.add(delay)

            val inTexturePos = frameIndex % framesPerTexture
            val inFrameStart = inTexturePos * width * height

            val src = master.data.dataBuffer
            val dst = texture.buffer
            for (i in 0 until width * height) {
                dst.putInt((inFrameStart + i) * 4, argb2rgba(src.getElem(i)))
            }

            if (disposal == "restoreToPrevious") {
                var from: Int = -1
                for (i in frameIndex - 1 downTo 0) {
                    if (disposals[i] != "restoreToPrevious" || frameIndex == 0) {
                        from = i
                        break
                    }
                }

                val inTexturePos = from % framesPerTexture
                val inFrameStart = inTexturePos * width * height

                val src = textures[from / framesPerTexture].buffer
                val pixel = intArrayOf(0, 0, 0, 0)
                var argb: Int
                for (i in 0 until width * height) {
                    argb = src.getInt((inFrameStart + i) * 4)
                    pixel[0] = argb and 0x000000ff         // a
                    pixel[3] = argb and -16777216  shr 3*8 // r
                    pixel[2] = argb and 0x00ff0000 shr 2*8 // g
                    pixel[1] = argb and 0x0000ff00 shr 1*8 // b

                    master.raster.setPixel(i % width, i / width, pixel)
                }
            } else if (disposal == "restoreToBackgroundColor") {
                masterGraphics.clearRect(x, y, image.width, image.height)
            }

            disposals.add(disposal)
            frameIndex++
        }

        return textures
    }
}
