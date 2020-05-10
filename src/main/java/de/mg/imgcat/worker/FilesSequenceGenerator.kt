package de.mg.imgcat.worker

import de.mg.imgcat.util.log
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FilesSequenceGenerator {

    fun generate(rootDir: String): Sequence<Path> = sequence {

        Files.walk(Paths.get(rootDir), 10).use { pathStream ->
            for (path in pathStream) {
                if (!Files.isDirectory(path)) {
                    log("requested $path")
                    yield(path)
                }
            }
        }
    }
}

