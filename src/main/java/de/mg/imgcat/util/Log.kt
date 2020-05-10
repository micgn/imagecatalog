package de.mg.imgcat.util

import de.mg.imgcat.logLevel
import java.lang.System.currentTimeMillis
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class LogLevel {
    DEBUG, INFO, ERROR
}

const val ANSI_RESET = "\u001B[0m"
const val ANSI_RED = "\u001B[31m"
const val ANSI_YELLOW = "\u001B[33m"

private val start = currentTimeMillis()

fun log(msg: String) {
    if (logLevel != LogLevel.DEBUG) return
    print("$ANSI_YELLOW$msg$ANSI_RESET")
}

fun info(msg: String) {
    if (logLevel == LogLevel.DEBUG || logLevel == LogLevel.INFO)
        print(msg)
}

fun err(msg: String) {
    print("${ANSI_RED}ERROR  $msg$ANSI_RESET")
}

private fun print(msg: String) {
    val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss"))
    val since = currentTimeMillis() - start
    println("[$time] [$since] [${Thread.currentThread().name}]: $msg")
}
