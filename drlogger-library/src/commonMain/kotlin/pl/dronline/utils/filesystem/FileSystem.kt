/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.filesystem

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalTime
internal data class FileInfo(
    val name: String,
    val size: Long,
    val modifiedTime: Instant
)

@ExperimentalTime
internal expect object FileSystem {
    fun getFileSize(path: String): Long // -1 if doesn't exist
    fun appendToFile(path: String, content: String): Boolean
    fun canWrite(path: String): Boolean
    fun listFiles(directory: String): List<FileInfo>
    fun deleteFile(path: String): Boolean
    fun renameFile(from: String, to: String): Boolean
    fun joinPath(directory: String, fileName: String): String
    fun mkdir(path: String): Boolean
    fun exists(path: String): Boolean
}

@ExperimentalTime
internal fun mkdirs(path: String): Boolean {
    if (FileSystem.exists(path)) return true

    val separator = if (path.contains("\\")) "\\" else "/"
    val parts = path.split(separator).filter { it.isNotEmpty() }
    var currentPath = if (path.startsWith(separator)) separator else ""

    for (part in parts) {
        currentPath = if (currentPath.isEmpty() || currentPath == separator) {
            "$currentPath$part"
        } else {
            "$currentPath$separator$part"
        }
        if (!FileSystem.exists(currentPath)) {
            if (!FileSystem.mkdir(currentPath)) {
                return false
            }
        }
    }
    return true
}
