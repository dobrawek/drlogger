/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.log.listener

import kotlinx.cinterop.ExperimentalForeignApi
import pl.dronline.utils.log.ALogListener
import pl.dronline.utils.log.ILogListener
import platform.posix.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Linux implementation of SyslogLogListener using native POSIX syslog API.
 *
 * ## Why we use null ident and prepend it manually
 *
 * The POSIX `openlog()` function stores the `ident` pointer as-is and does NOT copy the string.
 * From the man page (syslog(3)):
 *
 * > "The argument ident in the call of openlog() is probably stored as-is.
 * > Thus, if the string it points to is changed, syslog() may start prepending
 * > the changed string, and if the string it points to ceases to exist,
 * > the results are undefined."
 *
 * In Kotlin Native, when passing a String to a C function expecting `const char*`,
 * a temporary pointer is created that may be garbage collected after the function returns.
 * This causes undefined behavior when syslog() later tries to access the ident.
 *
 * Additionally, Kotlin Native's wrapper for `openlog()` accepts `String?` rather than
 * `CPointer<ByteVar>`, which means we cannot directly pass a manually allocated native
 * string pointer.
 *
 * Solution: We pass `null` as ident to `openlog()` and manually prepend the ident
 * string to each log message. This avoids the pointer lifetime issue entirely.
 *
 * @see <a href="https://man7.org/linux/man-pages/man3/syslog.3.html">syslog(3) man page</a>
 */
@ExperimentalTime
@OptIn(ExperimentalForeignApi::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class SyslogLogListener actual constructor(
    private val ident: String,
    private val facility: SyslogFacility
) : ALogListener("SyslogLogListener"), ILogListener {

    actual override var enabled: Boolean = true

    override fun onStart() {
        // Pass null ident - we prepend ident to each message manually
        // See class documentation for why this is necessary
        openlog(null, LOG_PID or LOG_CONS, facility.code shl 3)
    }

    actual override fun stopListening() {
        super.stopListening()
        closelog()
    }

    actual override fun writeLog(
        timestamp: Instant,
        level: ILogListener.Level,
        type: String,
        message: String,
        t: Throwable?
    ) {
        val priority = levelToSyslogPriority(level)
        val fullPriority = (facility.code shl 3) or priority

        // Prepend ident manually since we can't use openlog's ident parameter
        val logMessage = buildString {
            append("[")
            append(ident)
            append("] ")
            append(type)
            append(": ")
            append(message)
            if (t != null) {
                append("\n")
                append(t.stackTraceToString())
            }
        }

        syslog(fullPriority, "%s", logMessage)
    }

    /**
     * Maps DrLogger log levels to syslog priority levels.
     */
    private fun levelToSyslogPriority(level: ILogListener.Level): Int {
        return when (level) {
            ILogListener.Level.FATAL -> LOG_CRIT
            ILogListener.Level.ERROR -> LOG_ERR
            ILogListener.Level.WARN -> LOG_WARNING
            ILogListener.Level.INFO -> LOG_INFO
            ILogListener.Level.DEBUG -> LOG_DEBUG
            ILogListener.Level.TRACE -> LOG_DEBUG
        }
    }

}
