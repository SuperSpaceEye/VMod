package net.spaceeye.vmod;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;

public class GIFUtils {
    public static boolean skipImage(ImageInputStream stream) throws IIOException {
        // Stream must be at the beginning of an image descriptor
        // upon exit

        try {
            while (true) {
                int blockType = stream.readUnsignedByte();

                if (blockType == 0x2c) {
                    stream.skipBytes(8);

                    int packedFields = stream.readUnsignedByte();
                    if ((packedFields & 0x80) != 0) {
                        // Skip color table if any
                        int bits = (packedFields & 0x7) + 1;
                        stream.skipBytes(3*(1 << bits));
                    }

                    stream.skipBytes(1);

                    int length = 0;
                    do {
                        length = stream.readUnsignedByte();
                        stream.skipBytes(length);
                    } while (length > 0);

                    return true;
                } else if (blockType == 0x3b) {
                    return false;
                } else if (blockType == 0x21) {
                    int label = stream.readUnsignedByte();

                    int length = 0;
                    do {
                        length = stream.readUnsignedByte();
                        stream.skipBytes(length);
                    } while (length > 0);
                } else if (blockType == 0x0) {
                    // EOF
                    return false;
                } else {
                    int length = 0;
                    do {
                        length = stream.readUnsignedByte();
                        stream.skipBytes(length);
                    } while (length > 0);
                }
            }
        } catch (EOFException e) {
            return false;
        } catch (IOException e) {
            throw new IIOException("I/O error locating image!", e);
        }
    }

    public static int locateImage(ImageInputStream stream, int imageIndex) throws IIOException {
        int index = 0;
        try {
            // Skip images until at desired index or last image found
            while (index < imageIndex) {
                if (!skipImage(stream)) {
                    --index;
                    return index;
                }
                ++index;
            }
        } catch (IOException e) {
            throw new IIOException("Couldn't seek!", e);
        }
        return index;
    }

    public static int getNumImages(ImageInputStream stream) throws IIOException {
        return locateImage(stream, Integer.MAX_VALUE) + 1;
    }
}
