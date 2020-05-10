package de.mg.imgcat

import de.mg.imgcat.util.err
import de.mg.imgcat.util.info
import de.mg.imgcat.util.log
import java.nio.file.Files
import java.nio.file.Path

object ImageActions {

    fun deleteAndGetDuplicates(imageDataList: List<ImageCatalogPipeline.ImageData>): List<Path> {
        val duplicates = findDuplicates(imageDataList)
        info("found ${duplicates.size} duplicates: ${duplicates.take(10).joinToString(separator = ",\n")} ...")
        duplicates.forEach {
            log("rm ${it.fileName}")
            if (!simulation)
                Files.delete(it)
        }
        info("${duplicates.size} duplicates removed")
        return duplicates
    }

    private fun findDuplicates(imageDataList: List<ImageCatalogPipeline.ImageData>): List<Path> =
            imageDataList.groupBy { it.id.identifier() }
                    .filter { it.value.size > 1 }
                    .mapValues { it.value.dropLast(1) }
                    .flatMap { it.value }
                    .map { it.filePath }

    fun moveAndRename(imageDataList: List<ImageCatalogPipeline.ImageData>, targetRootDir: Path): Unit {

        if (!Files.exists(targetRootDir)) {
            err("target dir ${targetRootDir.fileName} does not exist")
            return
        }
        info("starting to move files")
        var moved = 0
        imageDataList.sortedBy { it.newFile.path }
                .forEach { imageData ->
                    val targetDir = Path.of(targetRootDir.toString(), imageData.newFile.path)
                    val targetFile = Path.of(targetDir.toString(), imageData.newFile.fname)
                    if (!imageData.filePath.equals(targetFile)) {
                        log("move ${imageData.filePath} -> ${targetFile}")
                        if (!simulation) {
                            Files.createDirectories(targetDir)
                            try {
                                Files.move(imageData.filePath, targetFile)
                                moved++
                            } catch (e: FileAlreadyExistsException) {
                                err("file already exists: ${imageData.filePath}")
                            }
                        } else
                            moved++
                    }
                }
        info("moved $moved files")
    }

}
