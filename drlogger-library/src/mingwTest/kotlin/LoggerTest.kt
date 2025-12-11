import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import pl.dronline.utils.Environment
import pl.dronline.utils.log.DrLogger
import pl.dronline.utils.log.listener.DailyFileLogListener
import pl.dronline.utils.log.listener.EmojiConsoleLogListener
import pl.dronline.utils.log.listener.LogcatLogListener
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class LoggerTest {

    @Test
    fun logToConsoleAndFile() {
        runBlocking {
            DrLogger.addListener(DailyFileLogListener().apply {
                enabled = true
                path = Environment.get("TEMP") ?: "C:\\Temp"
            }, EmojiConsoleLogListener())

            DrLogger("TEST").info("Hello world")

            DrLogger("TEST").info("TEST INFO")
            DrLogger("TEST").warn("TEST WARNING")
            DrLogger("TEST").error("TEST ERROR")
            DrLogger("TEST").debug("TEST DEBUG")
            DrLogger("TEST").error(Exception("TEST"), "TEST EXCEPTION")

            delay(10)
        }
    }

    @Test
    fun logToEventLog() {
        runBlocking {
            DrLogger.addListener(LogcatLogListener().apply {
                enabled = true
            })

            val logger = DrLogger("EventLogTest")

            logger.info("Test INFO message to Windows Event Log")
            logger.warn("Test WARNING message to Windows Event Log")
            logger.error("Test ERROR message to Windows Event Log")
            logger.debug("Test DEBUG message to Windows Event Log")
            logger.error(Exception("Test exception"), "Test EXCEPTION message to Windows Event Log")

            delay(10)
        }
    }

}
