package de.mg.imgcat.worker

import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Point
import de.mg.imgcat.locationDataPath
import de.mg.imgcat.util.info
import de.mg.imgcat.util.log
import rx.Single
import java.io.File
import java.lang.System.currentTimeMillis


class LocationService {

    private val tree: RTree<String, Point>

    init {
        val start = currentTimeMillis()
        var treeBuild = RTree.create<String, Point>()

        // data from http://download.geonames.org/export/dump/
        File(locationDataPath).forEachLine { line ->
            val split = line.split("\t")
            val country = split[8]
            val location = split[1]
            val latitude = split[4].toDouble()
            val longitude = split[5].toDouble()

            treeBuild = treeBuild.add("${country}_${location}", Geometries.pointGeographic(longitude, latitude));
        }
        tree = treeBuild
        info("location tree created in ${currentTimeMillis() - start} ms")
    }

    fun find(fname: String, longitude: Double, latitude: Double): String? {

        val reactiveResult = tree.nearest(Geometries.pointGeographic(longitude, latitude), 1.0, 1)
                .toSingle().map { it.value() }

        return waitForRxJavaResult(fname, reactiveResult)
    }

    private fun waitForRxJavaResult(fname: String, resultSingle: Single<String>): String? {
        var resultReceived = false
        var result: String? = null

        resultSingle.subscribe({
            result = it
            resultReceived = true
        }, {
            resultReceived = true
            if (!(it is NoSuchElementException))
                it.printStackTrace()
        })
        while (!resultReceived)
            Thread.sleep(20)

        log("$fname - got location $result")
        return result
    }

}
