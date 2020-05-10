package de.mg.imgcat

import de.mg.imgcat.util.LogLevel
import java.nio.file.Path
import kotlin.system.exitProcess

val simulation = true
val filesLimit = 10_000_000
val workingDir = "/home/mgnatz/Schreibtisch/pics/"
val targetDir = "/home/mgnatz/Schreibtisch/pics/"

// data from http://download.geonames.org/export/dump/
val locationDataPath = "/home/mgnatz/Downloads/DE.txt" //allCountries.txt"

val logLevel = LogLevel.DEBUG
val concurrency = 3

object Main {

    @JvmStatic
    fun main(args: Array<String>) {

        val imageDataList = ImageCatalogPipeline().run(workingDir)
        ImageActions.also {
            val duplicates = it.deleteAndGetDuplicates(imageDataList)
            it.moveAndRename(imageDataList.filter { !duplicates.contains(it.filePath) }, Path.of(targetDir))
        }

        // no idea why it cannot complete by itself...
        exitProcess(1)
    }


}
