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
        if (exifDate != null) {
            val result = exifDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            if (isValid(result)) return result
        }

        val dateFromPath = extractDateFromFilename(path)
        if (dateFromPath != null)
            return dateFromPath.atStartOfDay()

        val fileDate = Files.getLastModifiedTime(path).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        if (!isValid(fileDate))
            error("invalid file date for ${path.fileName}")
        return fileDate
    }

    private fun extractDateFromFilename(path: Path): LocalDate? {
        val d1 = extractDateFromFilename(path, "[0-9]{8}", 0..3, 4..5, 6..7)
        if (d1 != null) return d1
        return extractDateFromFilename(path, "[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}", 0..3, 5..6, 7..8)
    }

    private fun extractDateFromFilename(path: Path, pattern: String, year: IntRange, month: IntRange, day: IntRange): LocalDate? {
        val datePattern = pattern.toRegex()
        val match = datePattern.find(path.fileName.toString())
        if (match != null) {
            val value = match.value
            val y = value.substring(year).toInt()
            val m = value.substring(month).toInt()
            val d = value.substring(day).toInt()
            try {
                val result = LocalDate.of(y, m, d)
                if (!isValid(result.atStartOfDay()))
                    return null
                println("${path.fileName} - using file name date: $result")
                return result
            } catch (e: DateTimeException) {
                return null
            }
        } else
            return null
    }

    private fun isValid(date: LocalDateTime) = date.isBefore(LocalDateTime.now()) && date.isAfter(LocalDateTime.of(1974, 1, 1, 0, 0))

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
