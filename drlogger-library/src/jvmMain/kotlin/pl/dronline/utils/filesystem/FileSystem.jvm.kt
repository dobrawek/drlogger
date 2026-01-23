/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.filesystem

import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalTime
internal actual object FileSystem {
    actual fun getFileSize(path: String): Long {
        val file = File(path)
        return if (file.exists()) file.length() else -1L
    }

    actual fun appendToFile(path: String, content: String): Boolean {
        return runCatching {
            File(path).appendText(content)
            true
        }.getOrDefault(false)
    }

    actual fun canWrite(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) {
            file.canWrite()
        } else {
            file.parentFile?.let { it.isDirectory && it.canWrite() } ?: false
        }
    }

    actual fun listFiles(directory: String): List<FileInfo> {
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
        return File(path).delete()
    }

    actual fun renameFile(from: String, to: String): Boolean {
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
