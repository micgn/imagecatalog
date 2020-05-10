package de.mg.imgcat.worker

import de.mg.imgcat.util.log
import java.security.MessageDigest


object ImageIdentityService {

    data class ImageId(val sizeInBytes: Int, val md5Hash: String) {
        fun identifier() = "$md5Hash/$sizeInBytes"
    }

    fun identify(fname: String, bytes: ByteArray): ImageId {

        val md = MessageDigest.getInstance("MD5")
        md.update(bytes)
        val digest = md.digest()
        val md5Hash = bytesToHex(digest)

        log("$fname - image id $md5Hash")

        return ImageId(bytes.size, md5Hash)
    }

    private fun bytesToHex(bytes: ByteArray): String =
            bytes.joinToString(separator = "") { String.format("%02X", it) }

}
