package de.mg.imgcat.worker

import java.io.File
import java.nio.file.Path
import java.time.format.DateTimeFormatter

object NewFileService {

    private val alreadyCataloged = "C"
    private val separator = "_"

    data class NewFile(val fname: String, val path: String)

    fun build(currentFilePath: Path, metadata: ImageMetadataService.MetadataContainer, location: String?): NewFile? {

        if (currentFilePath.fileName.toString().startsWith(alreadyCataloged + separator))
            return null

        val date = metadata.date
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_hh_mm"))
        val newName = listOfNotNull(alreadyCataloged, dateStr, location, alreadyCataloged, currentFilePath.fileName)
                .joinToString(separator = separator)
        val newPath = "${date.year}${File.separator}${date.monthValue}"

        return NewFile(newName, newPath)
    }
}
