/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.filesystem

import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalTime
@OptIn(ExperimentalForeignApi::class)
internal actual object FileSystem {
    actual fun getFileSize(path: String): Long {
        return memScoped {
            val findData = alloc<WIN32_FIND_DATAW>()
            val hFind = FindFirstFileW(path, findData.ptr)
            if (hFind == INVALID_HANDLE_VALUE) {
                return -1L
            }
            FindClose(hFind)
            (findData.nFileSizeHigh.toLong() shl 32) or findData.nFileSizeLow.toLong()
        }
    }

    actual fun appendToFile(path: String, content: String): Boolean {
        return memScoped {
            val handle = CreateFileW(
                path,
                GENERIC_WRITE.toUInt(),
                FILE_SHARE_READ.toUInt(),
                null,
                OPEN_ALWAYS.toUInt(),
                FILE_ATTRIBUTE_NORMAL.toUInt(),
                null
            )
            if (handle == INVALID_HANDLE_VALUE) {
                return false
            }
            try {
                SetFilePointer(handle, 0, null, FILE_END.toUInt())
                val bytes = content.encodeToByteArray()
                val written = alloc<UIntVar>()
                bytes.usePinned { pinned ->
                    WriteFile(handle, pinned.addressOf(0), bytes.size.toUInt(), written.ptr, null)
                }
                true
            } finally {
                CloseHandle(handle)
            }
        }
    }

    actual fun canWrite(path: String): Boolean {
        return memScoped {
            _waccess(path.wcstr.ptr, 2) == 0 // 2 = W_OK for write access
        }
    }

    actual fun listFiles(directory: String): List<FileInfo> {
        val result = mutableListOf<FileInfo>()
        val searchPattern = "$directory\\*"

        memScoped {
            val findData = alloc<WIN32_FIND_DATAW>()
            val hFind = FindFirstFileW(searchPattern, findData.ptr)

            if (hFind == INVALID_HANDLE_VALUE) {
                return emptyList()
            }

            try {
                do {
                    val fileName = findData.cFileName.toKString()
                    if (fileName != "." && fileName != "..") {
                        // Check if it's a file (not a directory)
                        val isDirectory = (findData.dwFileAttributes.toInt() and FILE_ATTRIBUTE_DIRECTORY) != 0
                        if (!isDirectory) {
                            val fileSize = (findData.nFileSizeHigh.toLong() shl 32) or findData.nFileSizeLow.toLong()
                            val fileTimeMs = fileTimeToMillis(findData.ftLastWriteTime)

                            result.add(
                                FileInfo(
                                    name = fileName,
                                    size = fileSize,
                                    modifiedTime = Instant.fromEpochMilliseconds(fileTimeMs)
                                )
                            )
                        }
                    }
                } while (FindNextFileW(hFind, findData.ptr) != 0)
            } finally {
                FindClose(hFind)
            }
        }

        return result
    }

    actual fun deleteFile(path: String): Boolean {
        return DeleteFileW(path) != 0
    }

    actual fun renameFile(from: String, to: String): Boolean {
        return MoveFileW(from, to) != 0
    }

    actual fun joinPath(directory: String, fileName: String): String {
        return "$directory\\$fileName"
    }

    actual fun mkdir(path: String): Boolean {
        return CreateDirectoryW(path, null) != 0
    }

    actual fun exists(path: String): Boolean {
        return GetFileAttributesW(path) != INVALID_FILE_ATTRIBUTES
    }

    private fun fileTimeToMillis(fileTime: FILETIME): Long {
        // FILETIME is 100-nanosecond intervals since January 1, 1601
        // Convert to milliseconds since Unix epoch (January 1, 1970)
        val windowsTicks = (fileTime.dwHighDateTime.toLong() shl 32) or fileTime.dwLowDateTime.toLong()
        // Difference between 1601 and 1970 in 100-nanosecond intervals
        val epochDiff = 116444736000000000L
        return (windowsTicks - epochDiff) / 10000
    }
}
