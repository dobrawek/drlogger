/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.log.listener

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.*
import pl.dronline.utils.datetime.toString
import pl.dronline.utils.log.ALogListener
import pl.dronline.utils.log.DrLoggerFactory.prepareMessage
import pl.dronline.utils.log.ILogListener
import pl.dronline.utils.log.consoleError
import platform.posix.*
import platform.windows.*
import kotlin.concurrent.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalTime
@OptIn(ExperimentalForeignApi::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class DailyFileLogListener : ALogListener("DailyFileLogListener"), ILogListener {
    private val lock = SynchronizedObject()
    private val _path = AtomicReference<String?>(null)

    actual var path: String?
        get() = _path.value
        set(value) {
            _path.value = value
        }

    actual var namePrefix: String = ""
    actual var maxFileCount: Int = 30
    actual var maxFileAgeDays: Int = 90

    private val filename: String?
        get() {
            return _path.value?.let {
                "$it\\$namePrefix${Clock.System.now().toString("yyyyMMdd")}.log"
            }
        }

    private val canBeUsed: Boolean
        get() {
            return _access(path, 2) == 0 // 2 = W_OK for write access
        }

    actual override fun writeLog(
        timestamp: Instant,
        level: ILogListener.Level,
        type: String,
        message: String,
        t: Throwable?
    ) {
        synchronized(lock) {
            runCatching {
                if (canBeUsed) {
                    val logMessage = buildString {
                        append(timestamp.toString("HH:mm:ss.SSS"))
                        append(" [")
                        append(level.name)
                        append("] ")
                        append(prepareMessage(type, message, t))
                    }

                    val filePath = filename ?: return
                    fopen(filePath, "a+")?.let { file ->
                        try {
                            fputs(logMessage, file)
                        } catch (e: Exception) {
                            consoleError("CAN NOT WRITE: $level $type $message\n${e.stackTraceToString()}")
                        } finally {
                            fclose(file)
                        }
                    } ?: consoleError("CAN NOT WRITE: $level $type $message")
                }
            }.onFailure {
                consoleError("CAN NOT WRITE: $level $type $message\n${it.stackTraceToString()}")
            }
        }
    }

    override fun onStart() {
        performCleanup()
    }

    actual fun performCleanup() {
        synchronized(lock) {
            val logDirPath = _path.value ?: return

            runCatching {
                val searchPattern = "$logDirPath\\$namePrefix*.log"
                val logFiles = mutableListOf<LogFileInfo>()

                memScoped {
                    val findData = alloc<WIN32_FIND_DATAA>()
                    val hFind = FindFirstFileA(searchPattern, findData.ptr)

                    if (hFind == INVALID_HANDLE_VALUE) {
                        return
                    }

                    try {
                        do {
                            val fileName = findData.cFileName.toKString()
                            if (fileName != "." && fileName != "..") {
                                val filePath = "$logDirPath\\$fileName"

                                // Get file time
                                val fileTime = findData.ftLastWriteTime
                                val fileTimeMs = fileTimeToMillis(fileTime)

                                // Get file size
                                val fileSize = (findData.nFileSizeHigh.toLong() shl 32) or findData.nFileSizeLow.toLong()

                                logFiles.add(
                                    LogFileInfo(
                                        path = filePath,
                                        name = fileName,
                                        size = fileSize,
                                        modifiedTime = fileTimeMs
                                    )
                                )
                            }
                        } while (FindNextFileA(hFind, findData.ptr) != 0)
                    } finally {
                        FindClose(hFind)
                    }
                }

                if (logFiles.isEmpty()) return

                // Sort by modification time (oldest first)
                logFiles.sortBy { it.modifiedTime }

                val now = Clock.System.now()
                val cutoffTime = now - maxFileAgeDays.days

                val filesToDelete = mutableListOf<LogFileInfo>()
                val remainingFiles = logFiles.toMutableList()

                // Remove files older than maxFileAgeDays
                remainingFiles.forEach { file ->
                    val fileTime = Instant.fromEpochMilliseconds(file.modifiedTime)
                    if (fileTime < cutoffTime) {
                        filesToDelete.add(file)
                    }
                }
                remainingFiles.removeAll(filesToDelete)

                // Check file count limit
                if (remainingFiles.size > maxFileCount) {
                    val countToDelete = remainingFiles.size - maxFileCount
                    filesToDelete.addAll(remainingFiles.take(countToDelete))
                }

                // Delete identified files
                filesToDelete.forEach { file ->
                    if (remove(file.path) == 0) {
                        consoleError("Deleted old log file: ${file.name}")
                    } else {
                        consoleError("Failed to delete log file ${file.name}")
                    }
                }

            }.onFailure { e ->
                consoleError("Error during log cleanup: ${e.message}")
            }
        }
    }

    private fun fileTimeToMillis(fileTime: FILETIME): Long {
        // FILETIME is 100-nanosecond intervals since January 1, 1601
        // Convert to milliseconds since Unix epoch (January 1, 1970)
        val windowsTicks = (fileTime.dwHighDateTime.toLong() shl 32) or fileTime.dwLowDateTime.toLong()
        // Difference between 1601 and 1970 in 100-nanosecond intervals
        val epochDiff = 116444736000000000L
        return (windowsTicks - epochDiff) / 10000
    }

    private data class LogFileInfo(
        val path: String,
        val name: String,
        val size: Long,
        val modifiedTime: Long
    )

    actual override var enabled: Boolean = true
}
