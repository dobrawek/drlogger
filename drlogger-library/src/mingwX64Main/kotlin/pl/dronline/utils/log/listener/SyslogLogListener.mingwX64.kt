package pl.dronline.utils.log.listener

@OptIn(markerClass = [kotlin.time.ExperimentalTime::class])
@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class SyslogLogListener actual constructor(
    ident: String,
    facility: pl.dronline.utils.log.listener.SyslogFacility
) : pl.dronline.utils.log.ILogListener {
    actual override fun writeLog(
        timestamp: kotlin.time.Instant,
        level: pl.dronline.utils.log.ILogListener.Level,
        type: String,
        message: String,
        t: Throwable?
    ) {
    }

    actual override val name: String
        get() = TODO("Not yet implemented")
    actual override var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual override var minLevel: pl.dronline.utils.log.ILogListener.Level
        get() = TODO("Not yet implemented")
        set(value) {}
    actual override var messageRegex: kotlin.text.Regex?
        get() = TODO("Not yet implemented")
        set(value) {}
    actual override var tagRegex: kotlin.text.Regex?
        get() = TODO("Not yet implemented")
        set(value) {}

    actual override fun startListening(scope: kotlinx.coroutines.CoroutineScope): kotlinx.coroutines.Job {
        TODO("Not yet implemented")
    }

    actual override fun stopListening() {
    }
}