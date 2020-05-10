package de.mg.imgcat

import de.mg.imgcat.util.info
import de.mg.imgcat.util.log
import de.mg.imgcat.worker.*
import de.mg.imgcat.worker.ImageIdentityService.ImageId
import kotlinx.coroutines.*
import java.nio.file.Path
import java.util.concurrent.Executors

class ImageCatalogPipeline {

    private val scope = MainScope()
    private val threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            .asCoroutineDispatcher()
    private val locationService = LocationService()

    data class ImageData(val filePath: Path, val id: ImageId, val newFile: NewFileService.NewFile)

    fun run(dir: String): List<ImageData> {

        info("starting to collect")
        val filesSeq = FilesSequenceGenerator.generate(dir).take(filesLimit)

        fun <T> listOfSequences(amount: Int, seq: Sequence<T>): List<Sequence<T>> =
                (1..amount).map { pos -> seq.filterIndexed { i, _ -> i % amount == pos - 1 } }

        val filesSequences = listOfSequences(concurrency, filesSeq)

        val deferredImageDataLists: List<Deferred<List<ImageData>>> =
                filesSequences.map { filePathList ->
                    scope.async(threadPool) {
                        filePathList.mapNotNull { filePath ->
                            val fname = filePath.fileName.toString()
                            log("working on $fname")

                            val fileContent = scope.async(threadPool) {
                                AsyncFileReader.read(filePath)
                            }

                            val imageIdDeferred = scope.async(threadPool) {
                                ImageIdentityService.identify(fname, fileContent.await())
                            }

                            val newFileDeferred = scope.async(threadPool) {
                                mapToImageFileName(filePath, fileContent, fname)
                            }

                            runBlocking(threadPool) {
                                val newFile = newFileDeferred.await()
                                /* return: */
                                if (newFile == null) null
                                else ImageData(filePath, imageIdDeferred.await(), newFile)
                            }
                        }.asIterable().toList()  // list of deferred sequences -> list of deferred lists
                    }
                }

        val deferredImageDataList: Deferred<List<ImageData>> =
                scope.async(threadPool) {
                    deferredImageDataLists.flatMap { it.await() }
                }

        val imageDataList = runBlocking() {
            deferredImageDataList.await()
        }.toList()
        info("collected ${imageDataList.size} images")

        return imageDataList
    }


    private suspend fun mapToImageFileName(filePath: Path, fileContent: Deferred<ByteArray>, fname: String): NewFileService.NewFile? {
        val metadata = ImageMetadataService.extract(filePath, fileContent.await()) ?: return null
        val geolocation = metadata.geolocation
        val location =
                if (geolocation != null)
                    locationService.find(fname, geolocation.longitude, geolocation.latitude)
                else null

        return NewFileService.build(filePath, metadata, location)
    }

}
