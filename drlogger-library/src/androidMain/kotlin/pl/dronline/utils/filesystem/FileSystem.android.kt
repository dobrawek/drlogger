/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.filesystem

import android.content.Context
import android.net.Uri
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalTime
internal actual object FileSystem {
    private var _context: Context? = null

    fun init(context: Context) {
        _context = context.applicationContext
    }

    actual fun getFileSize(path: String): Long {
        val context = _context
        if (context != null && path.startsWith("content://")) {
            return runCatching {
                context.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
                    it.statSize
                } ?: -1L
            }.getOrDefault(-1L)
        }
        val file = File(path)
        return if (file.exists()) file.length() else -1L
    }

    actual fun appendToFile(path: String, content: String): Boolean {
        val context = _context
        if (context != null && path.startsWith("content://")) {
            return runCatching {
                context.contentResolver.openOutputStream(Uri.parse(path), "wa")?.use {
                    it.write(content.toByteArray())
                }
                true
            }.getOrDefault(false)
        }
        return runCatching {
            File(path).appendText(content)
            true
        }.getOrDefault(false)
    }

    actual fun canWrite(path: String): Boolean {
        val context = _context
        if (context != null && path.startsWith("content://")) {
            return runCatching {
                context.contentResolver.openOutputStream(Uri.parse(path), "wa")?.use { }
                true
            }.getOrDefault(false)
        }
        val file = File(path)
        return if (file.exists()) {
            file.canWrite()
        } else {
            file.parentFile?.let { it.isDirectory && it.canWrite() } ?: false
        }
    }

    actual fun listFiles(directory: String): List<FileInfo> {
        // ContentResolver doesn't support directory listing in the same way
        // Use File API for directories
        val dir = File(directory)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles()
            ?.filter { it.isFile }
            ?.map { file ->
                FileInfo(
                    name = file.name,
                    size = file.length(),
                    modifiedTime = Instant.fromEpochMilliseconds(file.lastModified())
                )
            }
            ?: emptyList()
    }

    actual fun deleteFile(path: String): Boolean {
        val context = _context
        if (context != null && path.startsWith("content://")) {
            return runCatching {
                context.contentResolver.delete(Uri.parse(path), null, null) > 0
            }.getOrDefault(false)
        }
        return File(path).delete()
    }

    actual fun renameFile(from: String, to: String): Boolean {
        // ContentResolver doesn't support rename directly
        // Use File API
        return File(from).renameTo(File(to))
    }

    actual fun joinPath(directory: String, fileName: String): String {
        return File(directory, fileName).path
    }

    actual fun mkdir(path: String): Boolean {
        return File(path).mkdir()
    }

    actual fun exists(path: String): Boolean {
        return File(path).exists()
    }
}
