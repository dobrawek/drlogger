package pl.dronline.utils.log.listener

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.*
import kotlin.test.Test

class SyslogDirectTest {

    // Store ident as a class property to keep it alive
    private val identString = "kt-class-prop"

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testDirectSyslog() {
        println("Opening syslog...")
        openlog("kotlin-direct", LOG_PID or LOG_CONS, LOG_USER)
        println("LOG_USER = $LOG_USER, LOG_INFO = $LOG_INFO")

        syslog(LOG_INFO, "Direct test from Kotlin Native")
        syslog(LOG_ERR, "Error test from Kotlin Native")

        closelog()
        println("Done! Check: sudo grep 'Direct test' /var/log/syslog | tail -3")
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testSyslogWithClassProperty() {
        println("Opening syslog with class property ident...")
        openlog(identString, LOG_PID or LOG_CONS, LOG_USER)

        syslog(LOG_INFO, "Test with class property ident")

        closelog()
        println("Done! Check: sudo grep 'class property' /var/log/syslog | tail -3")
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testSyslogNullIdent() {
        // Test with null ident - syslog will use program name
        println("Opening syslog with null ident...")
        openlog(null, LOG_PID or LOG_CONS, LOG_USER)

        syslog(LOG_INFO, "Test with null ident - should use program name")

        closelog()
        println("Done! Check syslog for test.kexe entries")
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testSyslogSimpleMessage() {
        // Most simple test - just priority and message
        openlog("simple-test", LOG_PID, LOG_USER)

        // Use just LOG_INFO without any facility bits
        syslog(LOG_INFO, "Simple message test")

        closelog()
        println("Done! Check: sudo grep 'Simple message' /var/log/syslog | tail -3")
    }
}
