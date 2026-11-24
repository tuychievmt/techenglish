package com.techenglish.app.monitoring

import com.techenglish.app.BuildConfig
import com.techenglish.app.core.monitoring.ConfigurableCrashReporter
import com.techenglish.app.core.monitoring.CrashReporter
import com.techenglish.app.core.monitoring.sanitizeMonitoringMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Sp209CrashReporterAndCiTest {

    private fun findProjectRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, "settings.gradle.kts").exists()) cwd else cwd.parentFile
    }

    @Test
    fun `crash monitoring is disabled by default with zero secrets in repo and scrubs PII`() {
        assertFalse("Crash reporting must be disabled by default without owner DSN", BuildConfig.CRASH_REPORTING_ENABLED)
        assertEquals("SENTRY_DSN must be empty in repo", "", BuildConfig.SENTRY_DSN)

        val captured = mutableListOf<String>()
        val reporter: CrashReporter = ConfigurableCrashReporter(
            enabledFlag = true,
            dsn = "https://examplePublicKey@o0.ingest.sentry.io/0",
            sink = { msg -> captured += msg }
        )
        assertTrue(reporter.isEnabled)
        reporter.initialize()
        reporter.logBreadcrumb("auth", "Login failed for alice@company.dev with Bearer eyJhbGciOiJIUzI1NiJ9.abc.def")
        reporter.recordException(
            IllegalStateException("Sync failed for user@test.org"),
            tags = mapOf("screen" to "cloud_sync", "authorization" to "Bearer secret123")
        )

        assertTrue("Expected sanitized events to be forwarded to sink", captured.size >= 2)
        captured.forEach { line ->
            assertFalse("Email PII leaked in monitoring event: $line", line.contains("alice@company.dev"))
            assertFalse("Email PII leaked in monitoring event: $line", line.contains("user@test.org"))
            assertFalse("Bearer token leaked in monitoring event: $line", line.contains("eyJhbGciOiJIUzI1NiJ9"))
            assertFalse("Secret tag leaked in monitoring event: $line", line.contains("secret123"))
        }

        val scrubbed = sanitizeMonitoringMessage("Contact bob@example.com with Bearer tok_999")
        assertFalse(scrubbed.contains("bob@example.com"))
        assertFalse(scrubbed.contains("tok_999"))
    }

    @Test
    fun `CI workflow uploads Compose stability report artifact`() {
        val ciFile = File(findProjectRoot(), ".github/workflows/ci.yml")
        assertTrue("Expected .github/workflows/ci.yml", ciFile.exists())
        val yaml = ciFile.readText()
        assertTrue(
            "CI workflow must upload app/build/compose_compiler/ stability reports (SP2-09)",
            yaml.contains("app/build/compose_compiler/")
        )
    }
}
