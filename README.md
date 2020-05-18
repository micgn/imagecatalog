# imagecatalog

Experimental of usage of Kotlin's Coroutines not for some UI, but for building a catalog of images:
* read images from thee file system
* extract exif meta data
* map geolocation to the name of the nearest location, using an RTree with data from http://download.geonames.org/export/dump/
* find and delete image duplicates by building md5 hashes
* move and rename images including date and nearest location

My Coroutine Pipeline: https://github.com/micgn/imagecatalog/blob/master/src/main/java/de/mg/imgcat/ImageCatalogPipeline.kt

see <a href="http://michaelgnatz.de/image_catalog.html">project homepage</a>
