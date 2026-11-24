package com.techenglish.app.audio

import android.content.Intent
import com.techenglish.app.MainActivity
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.notifications.NotificationConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Sp109AudioCacheAndNewIntentTest {

    @Test
    fun `audio cache key uses SHA-256 of locale speed and text so en-US differs from en-GB`() {
        val usKey = TechAudioController.buildCacheKey(localeTag = "en-US", speed = 1.0f, text = "idempotency")
        val gbKey = TechAudioController.buildCacheKey(localeTag = "en-GB", speed = 1.0f, text = "idempotency")
        val slowKey = TechAudioController.buildCacheKey(localeTag = "en-US", speed = 0.75f, text = "idempotency")
        val sameUsKey = TechAudioController.buildCacheKey(localeTag = "en-US", speed = 1.0f, text = "  idempotency  ")

        assertEquals(64, usKey.length) // SHA-256 hex
        assertEquals(usKey, sameUsKey)
        assertNotEquals("en-US and en-GB must produce distinct cache keys", usKey, gbKey)
        assertNotEquals("Different playback speeds must produce distinct cache keys", usKey, slowKey)
    }

    @Test
    fun `audio cache LRU eviction removes oldest files until under byte and count limits`() {
        val dir = Files.createTempDirectory("tts_lru_test").toFile()
        try {
            val f1 = File(dir, "clip_1.wav").apply { writeBytes(ByteArray(600)); setLastModified(1_000L) }
            val f2 = File(dir, "clip_2.wav").apply { writeBytes(ByteArray(600)); setLastModified(2_000L) }
            val f3 = File(dir, "clip_3.wav").apply { writeBytes(ByteArray(600)); setLastModified(3_000L) }

            TechAudioController.evictCacheLru(dir, maxBytes = 1_300L, maxFiles = 10)
            assertFalse("Oldest file should be evicted", f1.exists())
            assertTrue("Newer file f2 should stay", f2.exists())
            assertTrue("Newest file f3 should stay", f3.exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `MainActivity extracts EXTRA_DESTINATION from widget and push intents`() {
        val widgetIntent = Intent().apply {
            putExtra(NotificationConstants.EXTRA_DESTINATION, NotificationConstants.DEST_FLASHCARDS)
        }
        assertEquals(
            NotificationConstants.DEST_FLASHCARDS,
            MainActivity.extractDestinationFromIntent(widgetIntent)
        )
        assertEquals(null, MainActivity.extractDestinationFromIntent(Intent()))
    }
}
