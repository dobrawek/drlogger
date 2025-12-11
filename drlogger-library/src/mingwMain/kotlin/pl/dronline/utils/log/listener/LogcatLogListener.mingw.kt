/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.log.listener

import kotlinx.cinterop.*
import pl.dronline.utils.log.ALogListener
import pl.dronline.utils.log.DrLoggerFactory.prepareMessage
import pl.dronline.utils.log.ILogListener
import platform.windows.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Windows implementation of LogcatLogListener.
 * Writes log messages to Windows Event Log (Application log).
 * View logs using Event Viewer (eventvwr.msc) under "Windows Logs" -> "Application".
 *
 * Note: The event source is registered on first use. If running without admin privileges,
 * the source may not be registered and logs will appear under "Application" source.
 */
@ExperimentalTime
@OptIn(ExperimentalForeignApi::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class LogcatLogListener : ALogListener("LogcatLogListener"), ILogListener {

    companion object {
        private const val EVENT_SOURCE = "DrLogger"

        private val levelToEventType = mapOf(
            ILogListener.Level.ERROR to EVENTLOG_ERROR_TYPE.toUShort(),
            ILogListener.Level.WARN to EVENTLOG_WARNING_TYPE.toUShort(),
            ILogListener.Level.INFO to EVENTLOG_INFORMATION_TYPE.toUShort(),
            ILogListener.Level.DEBUG to EVENTLOG_INFORMATION_TYPE.toUShort(),
            ILogListener.Level.TRACE to EVENTLOG_INFORMATION_TYPE.toUShort(),
        )
    }

    actual override fun writeLog(
        timestamp: Instant,
        level: ILogListener.Level,
        type: String,
        message: String,
        t: Throwable?
    ) {
        val fullMessage = "[$type] ${prepareMessage("", message, t)}"
        val eventType = levelToEventType[level] ?: EVENTLOG_INFORMATION_TYPE.toUShort()

        memScoped {
            val hEventLog = RegisterEventSourceW(null, EVENT_SOURCE)
            if (hEventLog != null) {
                try {
                    val messagePtr = fullMessage.wcstr.ptr
                    val messagesArray = allocArray<CPointerVar<UShortVar>>(1)
                    messagesArray[0] = messagePtr

                    ReportEventW(
                        hEventLog,
                        eventType,
                        0u,      // category
                        1u,      // event ID
                        null,    // user SID
                        1u,      // number of strings
                        0u,      // data size
                        messagesArray,
                        null     // raw data
                    )
                } finally {
                    DeregisterEventSource(hEventLog)
                }
            }
        }
    }

    actual override var enabled: Boolean = true
}
