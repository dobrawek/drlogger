/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.filesystem

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalTime
@OptIn(ExperimentalForeignApi::class)
internal actual object FileSystem {
    actual fun getFileSize(path: String): Long {
        return memScoped {
            val statBuf = alloc<stat>()
            if (stat(path, statBuf.ptr) == 0) {
                statBuf.st_size.toLong()
            } else {
                -1L
            }
        }
    }

    actual fun appendToFile(path: String, content: String): Boolean {
        val file = fopen(path, "a+") ?: return false
        return try {
            fputs(content, file)
            true
        } finally {
            fclose(file)
        }
    }

    actual fun canWrite(path: String): Boolean {
        return access(path, W_OK) == 0
    }

    actual fun listFiles(directory: String): List<FileInfo> {
        val result = mutableListOf<FileInfo>()
        val dir = opendir(directory) ?: return emptyList()

        try {
            while (true) {
                val entry = readdir(dir) ?: break
                val fileName = entry.pointed.d_name.toKString()

                if (fileName == "." || fileName == "..") continue

                val filePath = "$directory/$fileName"
                memScoped {
                    val statBuf = alloc<stat>()
                    if (stat(filePath, statBuf.ptr) == 0) {
                        // Check if it's a regular file
                        if ((statBuf.st_mode.toInt() and S_IFMT) == S_IFREG) {
                            result.add(
                                FileInfo(
                                    name = fileName,
                                    size = statBuf.st_size.toLong(),
                                    modifiedTime = Instant.fromEpochMilliseconds(
                                        statBuf.st_mtim.tv_sec * 1000
                                    )
                                )
                            )
                        }
                    }
                }
            }
        } finally {
            closedir(dir)
        }

        return result
    }

    actual fun deleteFile(path: String): Boolean {
        return remove(path) == 0
    }

    actual fun renameFile(from: String, to: String): Boolean {
        return rename(from, to) == 0
    }

    actual fun joinPath(directory: String, fileName: String): String {
        return "$directory/$fileName"
    }

    actual fun mkdir(path: String): Boolean {
        return platform.posix.mkdir(path, 493u) == 0 // 0755 octal
    }

    actual fun exists(path: String): Boolean {
        return access(path, F_OK) == 0
    }
}
