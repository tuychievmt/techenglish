package com.techenglish.app.core.monitoring

import com.techenglish.app.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

private val EMAIL_REGEX = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
private val BEARER_REGEX = Regex("""(?i)Bearer\s+[A-Za-z0-9\-._~+/]+=*""")
private val SENSITIVE_TAG_KEYS = setOf("authorization", "token", "access_token", "refresh_token", "password", "email", "secret")

fun sanitizeMonitoringMessage(raw: String): String = raw
    .replace(EMAIL_REGEX, "[REDACTED_EMAIL]")
    .replace(BEARER_REGEX, "Bearer [REDACTED_TOKEN]")

/**
 * SP2-09 / BUG-26: Vendor-neutral crash & stability reporting abstraction (Sentry / Crashlytics).
 * Disabled by default unless explicitly enabled via build-time environment flags
 * (`CRASH_REPORTING_ENABLED=true` and `SENTRY_DSN=...`); zero secrets are stored in the repository.
 */
interface CrashReporter {
    val isEnabled: Boolean
    val backendName: String

    fun initialize()
    fun recordException(throwable: Throwable, tags: Map<String, String> = emptyMap())
    fun logBreadcrumb(category: String, message: String)
}

@Singleton
class ConfigurableCrashReporter internal constructor(
    private val enabledFlag: Boolean,
    private val dsn: String,
    private val sink: ((String) -> Unit)? = null
) : CrashReporter {

    @Inject
    constructor() : this(
        enabledFlag = BuildConfig.CRASH_REPORTING_ENABLED,
        dsn = BuildConfig.SENTRY_DSN,
        sink = null
    )

    override val isEnabled: Boolean
        get() = enabledFlag && dsn.isNotBlank()

    override val backendName: String
        get() = if (isEnabled) "sentry_compatible" else "noop"

    @Volatile
    private var initialized: Boolean = false

    override fun initialize() {
        if (!isEnabled || initialized) return
        initialized = true
        sink?.invoke("init:$backendName")
    }

    override fun recordException(throwable: Throwable, tags: Map<String, String>) {
        if (!isEnabled) return
        val safeMessage = sanitizeMonitoringMessage(throwable.message.orEmpty())
        val safeTags = tags
            .filterKeys { it.lowercase() !in SENSITIVE_TAG_KEYS }
            .mapValues { (_, v) -> sanitizeMonitoringMessage(v) }
        sink?.invoke("exception:${throwable.javaClass.simpleName}:$safeMessage:tags=$safeTags")
    }

    override fun logBreadcrumb(category: String, message: String) {
        if (!isEnabled) return
        val safeMsg = sanitizeMonitoringMessage(message)
        sink?.invoke("breadcrumb:$category:$safeMsg")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MonitoringModule {
    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: ConfigurableCrashReporter): CrashReporter
}
