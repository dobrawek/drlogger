/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.log.listener

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import pl.dronline.utils.filesystem.FileSize
import pl.dronline.utils.filesystem.FileSystem
import pl.dronline.utils.log.ILogListener
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class DailyFileLogListener : ILogListener {
    private val base = BaseDailyFileLogListener()

    /**
     * Creates a DailyFileLogListener without Context.
     * Use [DailyFileLogListener(Context)] for full Android compatibility.
     */
    actual constructor()

    /**
     * Creates a DailyFileLogListener with Android Context.
     * This enables proper file handling with ContentResolver for scoped storage.
     */
    constructor(context: Context) {
        FileSystem.init(context)
    }

    actual override val name: String get() = base.name
    actual override var enabled: Boolean
        get() = base.enabled
        set(value) { base.enabled = value }
    actual override var minLevel: ILogListener.Level
        get() = base.minLevel
        set(value) { base.minLevel = value }
    actual override var messageRegex: Regex?
        get() = base.messageRegex
        set(value) { base.messageRegex = value }
    actual override var tagRegex: Regex?
        get() = base.tagRegex
        set(value) { base.tagRegex = value }

    actual var path: String?
        get() = base.path
        set(value) { base.path = value }
    actual var namePrefix: String
        get() = base.namePrefix
        set(value) { base.namePrefix = value }
    actual var maxFileCount: Int
        get() = base.maxFileCount
        set(value) { base.maxFileCount = value }
    actual var maxFileAgeDays: Int
        get() = base.maxFileAgeDays
        set(value) { base.maxFileAgeDays = value }
    actual var maxFileSize: FileSize?
        get() = base.maxFileSize
        set(value) { base.maxFileSize = value }

    actual override fun writeLog(timestamp: Instant, level: ILogListener.Level, type: String, message: String, t: Throwable?) {
        base.writeLog(timestamp, level, type, message, t)
    }

    actual override fun startListening(scope: CoroutineScope): Job = base.startListening(scope)
    actual override fun stopListening() = base.stopListening()
    actual fun performCleanup() = base.performCleanup()
}
