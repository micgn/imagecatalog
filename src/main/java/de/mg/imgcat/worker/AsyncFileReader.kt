package de.mg.imgcat.worker

import de.mg.imgcat.util.log
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Path
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object AsyncFileReader {

    // from https://github.com/Kotlin/coroutines-examples/tree/master/examples/io

    suspend fun read(path: Path): ByteArray {

        val channel = AsynchronousFileChannel.open(path)
        return channel.use {
            val buf = ByteBuffer.allocate(channel.size().toInt())
            channel.aRead(buf)
            val result = buf.array()
            log("read ${result.size} bytes from ${path.fileName}")
            result
        }
    }


    private suspend fun AsynchronousFileChannel.aRead(buf: ByteBuffer): Int =

            suspendCoroutine { cont ->
                read(buf, 0L, Unit, object : CompletionHandler<Int, Unit> {
                    override fun completed(bytesRead: Int, attachment: Unit) {
                        cont.resume(bytesRead)
                    }

                    override fun failed(exception: Throwable, attachment: Unit) {
                        cont.resumeWithException(exception)
                    }
                })
            }

}
