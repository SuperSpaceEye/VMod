package net.spaceeye.vmod.rendering.textures

import net.spaceeye.vmod.ELOG
import net.spaceeye.vmod.GIFUtils
import net.spaceeye.vmod.GLMaxTextureSize
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode

//https://stackoverflow.com/a/18425922
object GIFReader {
    @JvmStatic fun readGIF(stream: InputStream): MutableList<ImageFrame> {
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
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

    data class TextureWithData(
        val image: BufferedImage,
        val delays: MutableList<Int>,
        var frameWidth: Int,
        var frameHeight: Int,
        var widthTiles: Int,
        var heightTiles: Int,
        var numFrames: Int,
    )

    class TextureBuilder(
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

        fun makeEmpty(requiredFrames: Int, imageType: Int = BufferedImage.TYPE_INT_ARGB): TextureWithData {
            val (spriteWidth, spriteHeight, widthTiles, heightTiles, numFrames) = calculateDimensions(requiredFrames)
            return TextureWithData(
                BufferedImage(spriteWidth, spriteHeight, imageType),
                mutableListOf(),
                frameWidth,
                frameHeight,
                widthTiles,
                heightTiles,
                numFrames
            )
        }
    }

    @JvmStatic fun readGifToTextures(originalStream: InputStream): MutableList<TextureWithData> {
        val bytes = originalStream.readAllBytes()

        var stream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        reader.setInput(stream)

        reader.streamMetadata
        val numFrames = GIFUtils.getNumImages(stream)

        reader.reset()
        stream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
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

        val textureBuilder = TextureBuilder(width, height, GLMaxTextureSize, GLMaxTextureSize)
        val textures = mutableListOf<TextureWithData>()
        var textureGraphics: Graphics2D? = null
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
                textureGraphics = texture.image.createGraphics()
                textures.add(texture)
                remainingFrames -= texture.numFrames
                texture
            }
            texture.delays.add(delay)

            val inTexturePos = frameIndex % framesPerTexture

            val xTile = inTexturePos % texture.widthTiles
            val yTile = inTexturePos / texture.widthTiles

            val dx = xTile * texture.frameWidth
            val dy = yTile * texture.frameHeight
            textureGraphics!!.drawImage(master, dx, dy, dx + width, dy + height, 0, 0, width, height, null)

            if (disposal == "restoreToPrevious") {
                var from: Int = -1
                for (i in frameIndex - 1 downTo 0) {
                    if (disposals[i] != "restoreToPrevious" || frameIndex == 0) {
                        from = i
                        break
                    }
                }

                val texture = textures[from / framesPerTexture]
                val inTexturePos = from % framesPerTexture

                val xTile = inTexturePos % texture.widthTiles
                val yTile = inTexturePos / texture.widthTiles

                val sx = xTile * texture.frameWidth
                val sy = yTile * texture.frameHeight

                masterGraphics.drawImage(texture.image, 0, 0, width, height, sx, sy, sx + texture.frameWidth, sy + texture.frameHeight, null)
            } else if (disposal == "restoreToBackgroundColor") {
                masterGraphics.clearRect(x, y, image.width, image.height)
            }

            disposals.add(disposal)
            frameIndex++
        }

        ELOG("$numFrames $frameIndex")

        return textures
    }

    //TODO this doesnt fukcing work
    @JvmStatic fun copyData(source: DataBuffer, dest: DataBuffer, start: Int, end: Int, sourceOffset: Int, destOffset: Int) {
        for (i in start until end) {
            dest.setElem(i + destOffset, source.getElem(i + sourceOffset))
        }
    }

    @JvmStatic fun readGifToTexturesFaster(originalStream: InputStream): MutableList<TextureWithData> {
        val bytes = originalStream.readAllBytes()

        var stream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        reader.setInput(stream)

        reader.streamMetadata
        val numFrames = GIFUtils.getNumImages(stream)

        reader.reset()
        stream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
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

        val textureBuilder = TextureBuilder(width, height, width, GLMaxTextureSize)
        val textures = mutableListOf<TextureWithData>()
        var textureGraphics: Graphics2D? = null
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
                textureGraphics = texture.image.createGraphics()
                textures.add(texture)
                remainingFrames -= texture.numFrames
                texture
            }
            texture.delays.add(delay)

            val inTexturePos = frameIndex % framesPerTexture
            val inFrameStart = inTexturePos * width * height

            copyData(master.data.dataBuffer, texture.image.data.dataBuffer, 0, width * height, 0, inFrameStart)

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

                copyData(textures[from / framesPerTexture].image.data.dataBuffer, master.data.dataBuffer, 0, width * height, inFrameStart, 0)
            } else if (disposal == "restoreToBackgroundColor") {
                masterGraphics.clearRect(x, y, image.width, image.height)
            }

            disposals.add(disposal)
            frameIndex++
        }

        return textures
    }
}
