package pl.dronline.utils.log.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import pl.dronline.utils.log.ILogListener
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(markerClass = [ExperimentalTime::class])
@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class SyslogLogListener actual constructor(
    ident: String,
    facility: SyslogFacility
) : ILogListener {
    companion object {
        private const val JVM_NOT_SUPPORTED = "JVM is not supported"
    }

    actual override fun writeLog(
        timestamp: Instant,
        level: ILogListener.Level,
        type: String,
        message: String,
        t: Throwable?
    ) {
    }

    actual override val name: String = TODO(JVM_NOT_SUPPORTED)
    actual override var enabled: Boolean = TODO(JVM_NOT_SUPPORTED)
    actual override var minLevel: ILogListener.Level = TODO(JVM_NOT_SUPPORTED)
    actual override var messageRegex: Regex? = null
    actual override var tagRegex: Regex? = TODO(JVM_NOT_SUPPORTED)

    actual override fun startListening(scope: CoroutineScope): Job {
        TODO(JVM_NOT_SUPPORTED)
    }

    actual override fun stopListening() {
    }
}