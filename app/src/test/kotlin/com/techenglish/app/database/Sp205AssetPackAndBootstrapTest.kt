package com.techenglish.app.database

import android.content.Context
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.database.DatabaseBootstrapManager
import com.techenglish.app.core.database.DatabaseBootstrapState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class Sp205AssetPackAndBootstrapTest {

    private fun findProjectRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, "settings.gradle.kts").exists()) cwd else cwd.parentFile
    }

    @Test
    fun `install-time Play Asset Delivery module content_pack hosts prebuilt DB outside base assets`() {
        val root = findProjectRoot()
        val packGradle = File(root, "content_pack/build.gradle.kts")
        assertTrue("Expected content_pack/build.gradle.kts for PAD", packGradle.exists())
        val packGradleText = packGradle.readText()
        assertTrue("Expected install-time deliveryType", packGradleText.contains("install-time"))

        val packDb = File(root, "content_pack/src/main/assets/databases/tech_english.db")
        assertTrue("Expected prebuilt DB inside content_pack assets", packDb.exists() && packDb.length() > 1_000_000L)

        val legacyBaseDb = File(root, "app/src/main/assets/databases/tech_english.db")
        assertFalse("Prebuilt DB must not be duplicated in base app/src/main/assets", legacyBaseDb.exists())
    }

    @Test
    fun `first-run DB unpack runs off main thread with progress and subsequent start is fast`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDbName = "sp205_bootstrap_${UUID.randomUUID()}.db"
        val targetFile = context.getDatabasePath(testDbName)
        targetFile.delete()

        var ranOnMainThread = true
        val observedStates = mutableListOf<DatabaseBootstrapState>()
        val manager = DatabaseBootstrapManager(
            context = context,
            databaseName = testDbName,
            onCopyStartedHook = {
                ranOnMainThread = Looper.getMainLooper().thread === Thread.currentThread()
            }
        )

        val collector = launch(Dispatchers.Unconfined) {
            manager.state.collect { observedStates += it }
        }

        try {
            // 1. First run: unpacks off main thread with progress
            val firstReady = manager.ensureDatabaseReady(Dispatchers.IO)
            assertFalse("DB copy must execute off the main thread (R4)", ranOnMainThread)
            assertTrue("First run should report wasFirstRun = true", firstReady.wasFirstRun)
            assertTrue("Target DB file must exist after unpack", targetFile.exists() && targetFile.length() > 1_000_000L)
            assertTrue(
                "Expected Unpacking progress states during first-run copy",
                observedStates.any { it is DatabaseBootstrapState.Unpacking }
            )
            println("SP2-05 first-run DB unpack duration: ${firstReady.durationMs} ms (${targetFile.length() / (1024 * 1024)} MiB)")

            // 2. Subsequent cold start (new manager instance, DB already present): instant Ready
            val secondManager = DatabaseBootstrapManager(context = context, databaseName = testDbName)
            val secondReady = secondManager.ensureDatabaseReady(Dispatchers.IO)
            assertFalse("Subsequent launch must not re-unpack DB", secondReady.wasFirstRun)
            println("SP2-05 subsequent startup DB check duration: ${secondReady.durationMs} ms")
            assertTrue("Subsequent startup check must be < 50 ms, actual=${secondReady.durationMs} ms", secondReady.durationMs < 50L)
        } finally {
            collector.cancel()
            targetFile.delete()
            File("${targetFile.path}.tmp").delete()
        }
    }
}
