/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.filesystem

import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.*
import platform.posix.W_OK
import platform.posix.access
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalTime
@OptIn(ExperimentalForeignApi::class)
internal actual object FileSystem {
    actual fun getFileSize(path: String): Long {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(path)) {
            return -1L
        }
        val attributes = fileManager.attributesOfItemAtPath(path, null) ?: return -1L
        return (attributes[NSFileSize] as? NSNumber)?.longValue ?: -1L
    }

    actual fun appendToFile(path: String, content: String): Boolean {
        val fileManager = NSFileManager.defaultManager

        // Create file if it doesn't exist
        if (!fileManager.fileExistsAtPath(path)) {
            fileManager.createFileAtPath(path, null, null)
        }

        val fileHandle = NSFileHandle.fileHandleForWritingAtPath(path) ?: return false
        return try {
            fileHandle.seekToEndOfFile()
            fileHandle.writeData(content.toNSData())
            true
        } finally {
            fileHandle.closeFile()
        }
    }

    actual fun canWrite(path: String): Boolean {
        return access(path, W_OK) == 0
    }

    actual fun listFiles(directory: String): List<FileInfo> {
        val fileManager = NSFileManager.defaultManager

        // Check if directory exists
        memScoped {
            val isDirectory = alloc<BooleanVar>()
            if (!fileManager.fileExistsAtPath(directory, isDirectory.ptr) || !isDirectory.value) {
                return emptyList()
            }
        }

        val files = fileManager.contentsOfDirectoryAtPath(directory, null) as? List<*>
            ?: return emptyList()

        return files.mapNotNull { file ->
            val fileName = file as? String ?: return@mapNotNull null
            val filePath = "$directory/$fileName"

            val attributes = fileManager.attributesOfItemAtPath(filePath, null)
                ?: return@mapNotNull null

            // Check if it's a regular file
            val fileType = attributes[NSFileType] as? String
            if (fileType != NSFileTypeRegular) return@mapNotNull null

            val modificationDate = attributes[NSFileModificationDate] as? NSDate
                ?: return@mapNotNull null
            val fileSize = (attributes[NSFileSize] as? NSNumber)?.longValue ?: 0L

            FileInfo(
                name = fileName,
                size = fileSize,
                modifiedTime = Instant.fromEpochMilliseconds(
                    (modificationDate.timeIntervalSince1970 * 1000).toLong()
                )
            )
        }
    }

    actual fun deleteFile(path: String): Boolean {
        return NSFileManager.defaultManager.removeItemAtPath(path, null)
    }

    actual fun renameFile(from: String, to: String): Boolean {
        return NSFileManager.defaultManager.moveItemAtPath(from, to, null)
    }

    actual fun joinPath(directory: String, fileName: String): String {
        return "$directory/$fileName"
    }

    actual fun mkdir(path: String): Boolean {
        return NSFileManager.defaultManager.createDirectoryAtPath(
            path,
            withIntermediateDirectories = false,
            attributes = null,
            error = null
        )
    }

    actual fun exists(path: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }
}

@Suppress("CAST_NEVER_SUCCEEDS")
private fun String.toNSData(): NSData {
    return (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
}
