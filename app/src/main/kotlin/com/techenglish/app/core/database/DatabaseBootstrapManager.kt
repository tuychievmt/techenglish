package com.techenglish.app.core.database

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DatabaseBootstrapState {
    data object Idle : DatabaseBootstrapState
    data class Unpacking(
        val progress: Float,
        val bytesCopied: Long,
        val totalBytes: Long
    ) : DatabaseBootstrapState

    data class Ready(
        val wasFirstRun: Boolean,
        val durationMs: Long
    ) : DatabaseBootstrapState

    data class Error(val message: String) : DatabaseBootstrapState
}

/**
 * SP2-05 / R4: Off-main-thread first-run database unpacker from Play Asset Delivery (`:content_pack`,
 * merged into `AssetManager` for install-time packs). Reports chunked copy progress and performs
 * an atomic rename so partial writes never corrupt Room's database file.
 */
@Singleton
class DatabaseBootstrapManager internal constructor(
    private val context: Context,
    private val databaseName: String = TechEnglishDatabase.DATABASE_NAME,
    private val assetPath: String = TechEnglishDatabase.SEED_ASSET_PATH,
    private val onCopyStartedHook: (() -> Unit)? = null
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(
        context = context,
        databaseName = TechEnglishDatabase.DATABASE_NAME,
        assetPath = TechEnglishDatabase.SEED_ASSET_PATH,
        onCopyStartedHook = null
    )

    private val mutex = Mutex()
    private val _state = MutableStateFlow<DatabaseBootstrapState>(DatabaseBootstrapState.Idle)
    val state: StateFlow<DatabaseBootstrapState> = _state.asStateFlow()

    suspend fun ensureDatabaseReady(
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): DatabaseBootstrapState.Ready = withContext(dispatcher) {
        mutex.withLock {
            val current = _state.value
            if (current is DatabaseBootstrapState.Ready) {
                return@withLock current
            }

            val startNs = System.nanoTime()
            val targetFile = context.getDatabasePath(databaseName)
            if (targetFile.exists() && targetFile.length() > 0L) {
                val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(0L)
                val ready = DatabaseBootstrapState.Ready(wasFirstRun = false, durationMs = elapsedMs)
                _state.value = ready
                return@withLock ready
            }

            onCopyStartedHook?.invoke()
            targetFile.parentFile?.mkdirs()
            val tempFile = File("${targetFile.path}.tmp")
            tempFile.delete()

            val totalBytes = runCatching {
                context.assets.openFd(assetPath).use { it.length }
            }.getOrDefault(-1L)

            _state.value = DatabaseBootstrapState.Unpacking(
                progress = 0f,
                bytesCopied = 0L,
                totalBytes = totalBytes
            )

            try {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        var lastReportedStep = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            val progress = if (totalBytes > 0L) {
                                (copied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }
                            val step = (progress * 20).toInt()
                            if (step != lastReportedStep) {
                                lastReportedStep = step
                                _state.value = DatabaseBootstrapState.Unpacking(
                                    progress = progress,
                                    bytesCopied = copied,
                                    totalBytes = totalBytes
                                )
                            }
                        }
                        output.flush()
                    }
                }
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
                val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(0L)
                val ready = DatabaseBootstrapState.Ready(wasFirstRun = true, durationMs = elapsedMs)
                _state.value = ready
                ready
            } catch (ce: CancellationException) {
                tempFile.delete()
                _state.value = DatabaseBootstrapState.Idle
                throw ce
            } catch (exc: Exception) {
                tempFile.delete()
                _state.value = DatabaseBootstrapState.Error(
                    exc.localizedMessage ?: "Не удалось распаковать офлайн-базу данных"
                )
                throw exc
            }
        }
    }
}
