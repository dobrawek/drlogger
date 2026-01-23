/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.filesystem

import kotlin.jvm.JvmInline

/**
 * Represents a file size with a value in bytes.
 * Similar to [kotlin.time.Duration] but for file sizes.
 *
 * Usage:
 * ```
 * val size = 10.MB
 * val smallFile = 500.kB
 * val huge = 2.GB
 * ```
 */
@JvmInline
value class FileSize(val bytes: Long) : Comparable<FileSize> {

    val inBytes: Long get() = bytes
    val inKB: Long get() = bytes / 1024
    val inMB: Long get() = bytes / (1024 * 1024)
    val inGB: Long get() = bytes / (1024 * 1024 * 1024)

    operator fun plus(other: FileSize): FileSize = FileSize(bytes + other.bytes)
    operator fun minus(other: FileSize): FileSize = FileSize(bytes - other.bytes)
    operator fun times(factor: Int): FileSize = FileSize(bytes * factor)
    operator fun times(factor: Long): FileSize = FileSize(bytes * factor)
    operator fun div(factor: Int): FileSize = FileSize(bytes / factor)
    operator fun div(factor: Long): FileSize = FileSize(bytes / factor)

    override fun compareTo(other: FileSize): Int = bytes.compareTo(other.bytes)

    override fun toString(): String = when {
        bytes >= 1024 * 1024 * 1024 -> "${inGB}GB"
        bytes >= 1024 * 1024 -> "${inMB}MB"
        bytes >= 1024 -> "${inKB}kB"
        else -> "${bytes}B"
    }

    companion object {
        val ZERO = FileSize(0)

        fun bytes(value: Long) = FileSize(value)
        fun kilobytes(value: Long) = FileSize(value * 1024)
        fun megabytes(value: Long) = FileSize(value * 1024 * 1024)
        fun gigabytes(value: Long) = FileSize(value * 1024 * 1024 * 1024)
    }
}

// Extension properties for Int
val Int.bytes: FileSize get() = FileSize(this.toLong())
val Int.kB: FileSize get() = FileSize(this.toLong() * 1024)
val Int.MB: FileSize get() = FileSize(this.toLong() * 1024 * 1024)
val Int.GB: FileSize get() = FileSize(this.toLong() * 1024 * 1024 * 1024)

// Extension properties for Long
val Long.bytes: FileSize get() = FileSize(this)
val Long.kB: FileSize get() = FileSize(this * 1024)
val Long.MB: FileSize get() = FileSize(this * 1024 * 1024)
val Long.GB: FileSize get() = FileSize(this * 1024 * 1024 * 1024)