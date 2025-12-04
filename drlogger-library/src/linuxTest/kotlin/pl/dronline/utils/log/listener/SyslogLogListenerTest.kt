/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.log.listener

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import pl.dronline.utils.log.DrLoggerFactory
import pl.dronline.utils.log.ILogListener
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SyslogLogListenerTest {

    @Test
    fun testSyslogBasic() = runBlocking {
        val listener = SyslogLogListener(ident = "drlogger-test")
        listener.minLevel = ILogListener.Level.DEBUG

        // Zarejestruj listener w factory
        DrLoggerFactory.addListeners(listOf(listener))

        // Daj chwilę na start
        delay(100)

        // Wyślij kilka logów
        DrLoggerFactory.debug("TestTag", "Debug message from drlogger test")
        DrLoggerFactory.info("TestTag", "Info message from drlogger test")
        DrLoggerFactory.warn("TestTag", "Warning message from drlogger test")
        DrLoggerFactory.error("TestTag", "Error message from drlogger test")

        // Daj chwilę na przetworzenie
        delay(500)

        DrLoggerFactory.removeListener(listener)

        // Sprawdź w journalctl -f -t drlogger-test
        println("Check: journalctl -t drlogger-test --since '1 minute ago'")
    }

    @Test
    fun testSyslogWithCustomFacility() = runBlocking {
        val listener = SyslogLogListener(
            ident = "drlogger-local0",
            facility = SyslogFacility.LOCAL0
        )
        listener.minLevel = ILogListener.Level.DEBUG

        DrLoggerFactory.addListeners(listOf(listener))
        delay(100)

        DrLoggerFactory.error("CustomFacility", "This goes to LOCAL0")
        DrLoggerFactory.warn("CustomFacility", "This goes to LOCAL0")
        DrLoggerFactory.info("CustomFacility", "This goes to LOCAL0")

        delay(500)
        DrLoggerFactory.removeListener(listener)

        println("Check: journalctl -t drlogger-local0 --since '1 minute ago'")
    }
}