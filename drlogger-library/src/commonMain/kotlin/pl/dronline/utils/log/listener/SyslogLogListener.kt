/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.log.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import pl.dronline.utils.log.ILogListener
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A log listener implementation that sends log messages to the system syslog daemon.
 * This listener uses native syslog APIs on Linux and Apple platforms.
 *
 * Features:
 * - Native syslog integration
 * - Configurable syslog facility (source type)
 * - Configurable syslog identity (tag)
 *
 * This is an expect class implemented on Linux and Apple platforms only,
 * as syslog is a POSIX feature not directly available on JVM or Android.
 *
 * @param ident The identity string prepended to every message (typically app name)
 * @param facility The syslog facility indicating the type of program logging.
 *                 Defaults to USER for general user-level messages.
 */
@OptIn(ExperimentalTime::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class SyslogLogListener(
    ident: String = "drlogger",
    facility: SyslogFacility = SyslogFacility.USER
) : ILogListener {

    /**
     * Writes a log message to syslog.
     *
     * @param timestamp The time when the log event occurred
     * @param level The severity level of the log message
     * @param type The tag or category of the log message
     * @param message The content of the log message
     * @param t Optional throwable associated with the log message
     */
    override fun writeLog(
        timestamp: Instant,
        level: ILogListener.Level,
        type: String,
        message: String,
        t: Throwable?
    )

    override val name: String
    override var enabled: Boolean
    override var minLevel: ILogListener.Level
    override var messageRegex: Regex?
    override var tagRegex: Regex?

    override fun startListening(scope: CoroutineScope): Job
    override fun stopListening()
}

/**
 * Syslog facility codes as defined in RFC 3164.
 * These indicate what type of program is logging the message.
 */
enum class SyslogFacility(val code: Int) {
    /** kernel messages */
    KERN(0),
    /** user-level messages (default) */
    USER(1),
    /** mail system */
    MAIL(2),
    /** system daemons */
    DAEMON(3),
    /** security/authorization messages */
    AUTH(4),
    /** messages generated internally by syslogd */
    SYSLOG(5),
    /** line printer subsystem */
    LPR(6),
    /** network news subsystem */
    NEWS(7),
    /** UUCP subsystem */
    UUCP(8),
    /** clock daemon */
    CRON(9),
    /** security/authorization messages (private) */
    AUTHPRIV(10),
    /** FTP daemon */
    FTP(11),
    /** reserved for local use */
    LOCAL0(16),
    /** reserved for local use */
    LOCAL1(17),
    /** reserved for local use */
    LOCAL2(18),
    /** reserved for local use */
    LOCAL3(19),
    /** reserved for local use */
    LOCAL4(20),
    /** reserved for local use */
    LOCAL5(21),
    /** reserved for local use */
    LOCAL6(22),
    /** reserved for local use */
    LOCAL7(23)
}