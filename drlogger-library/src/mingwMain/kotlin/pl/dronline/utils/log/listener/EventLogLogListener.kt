/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2026 Przemysław Dobrowolski
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
 * Windows Event Log listener implementation.
 * Writes log messages to Windows Event Log (Application log).
 * View logs using Event Viewer (eventvwr.msc) under "Windows Logs" -> "Application".
 *
 * @param source The event source name (appears in Event Viewer's "Source" column)
 */
@ExperimentalTime
@OptIn(ExperimentalForeignApi::class)
class EventLogLogListener(
    private val source: String = "DrLogger"
) : ALogListener("EventLogLogListener"), ILogListener {

    companion object {
        private val levelToEventType = mapOf(
            ILogListener.Level.FATAL to EVENTLOG_ERROR_TYPE.toUShort(),
            ILogListener.Level.ERROR to EVENTLOG_ERROR_TYPE.toUShort(),
            ILogListener.Level.WARN to EVENTLOG_WARNING_TYPE.toUShort(),
            ILogListener.Level.INFO to EVENTLOG_INFORMATION_TYPE.toUShort(),
            ILogListener.Level.DEBUG to EVENTLOG_INFORMATION_TYPE.toUShort(),
            ILogListener.Level.TRACE to EVENTLOG_INFORMATION_TYPE.toUShort(),
        )
    }

    override fun writeLog(
        timestamp: Instant,
        level: ILogListener.Level,
        type: String,
        message: String,
        t: Throwable?
    ) {
        val fullMessage = "[$type] ${prepareMessage("", message, t)}"
        val eventType = levelToEventType[level] ?: EVENTLOG_INFORMATION_TYPE.toUShort()

        memScoped {
            val hEventLog = RegisterEventSourceW(null, source)
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

    override var enabled: Boolean = true
}