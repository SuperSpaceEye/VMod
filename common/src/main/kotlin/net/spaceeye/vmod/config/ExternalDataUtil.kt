package net.spaceeye.vmod.config

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

object ExternalDataUtil {
    val folderPath = "config/VModData"

    fun writeObject(name: String, data: ByteArray) {
        Files.createDirectories(Paths.get(folderPath))
        Files.write(Paths.get(folderPath, name), data)
    }

    fun readObject(name: String): ByteArray? {
        return try {
            Files.readAllBytes(Paths.get(folderPath, name))
        } catch (e: IOException) {
            null
        }
    }
}