package de.mg.imgcat.worker

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.lang.GeoLocation
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import de.mg.imgcat.util.err
import de.mg.imgcat.util.log
import java.nio.file.Files
import java.nio.file.Path
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


object ImageMetadataService {

    data class MetadataContainer(val date: LocalDateTime, val geolocation: GeoLocation?)

    fun extract(path: Path, imageBytes: ByteArray): MetadataContainer? {

        val metadata =
                try {
                    ImageMetadataReader.readMetadata(imageBytes.inputStream())
                } catch (e: ImageProcessingException) {
                    err("${path.fileName} - reading metadata failed: ${e.message}")
                    return MetadataContainer(determineDate(path), null)
                }

        val exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        val exifDate =
                if (exifDir != null) {
                    if (exifDir.hasErrors()) {
                        err("metadata errors with ${path.fileName}: ${exifDir.errors.joinToString(", ")}");
                        null
                    } else
                        exifDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
                } else null

        // use file change date if exif date is empty
        val localDateTime = determineDate(path, exifDate)

        val gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
        val geolocation = if (gpsDir != null) {
            if (gpsDir.hasErrors()) {
                err("metadata errors with ${path.fileName}: ${exifDir.errors.joinToString(", ")}")
                null
            } else
                gpsDir.geoLocation
        } else null

        // if (date == null) showAll(fname, imageBytes)

        val result = MetadataContainer(localDateTime, geolocation)
        log("${path.fileName} - meta data: $result")
        return result
    }

    private fun determineDate(path: Path, exifDate: Date? = null): LocalDateTime {
        if (exifDate != null)
            return exifDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

        val dateFromPath = extractDateFromFilename(path)
        if (dateFromPath != null)
            return dateFromPath.atStartOfDay()

        val fileDate = Files.getLastModifiedTime(path).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        return fileDate
    }

    private fun extractDateFromFilename(path: Path): LocalDate? {
        val datePattern = "[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]".toRegex()
        val match = datePattern.find(path.fileName.toString())
        if (match != null) {
            val value = match.value
            val y = value.substring(0, 4).toInt()
            val m = value.substring(4, 6).toInt()
            val d = value.substring(6, 8).toInt()
            try {
                val result = LocalDate.of(y, m, d)
                log("${path.fileName} - using file name date: $result")
                return result
            } catch (e: DateTimeException) {
                return null
            }
        } else
            return null
    }

    fun showAll(fname: String, imageBytes: ByteArray) {
        var s = "all meta data: $fname\n"
        val metadata = ImageMetadataReader.readMetadata(imageBytes.inputStream())
        for (directory in metadata.getDirectories()) {
            for (tag in directory.tags)
                s += String.format("[%s] - %s = %s\n", directory.name, tag.getTagName(), tag.getDescription())
        }
        log(s)
    }
}
