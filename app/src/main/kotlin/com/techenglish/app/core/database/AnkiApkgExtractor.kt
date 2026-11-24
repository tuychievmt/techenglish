package com.techenglish.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ФТ-7.6 — Safe streaming extractor for Anki `.apkg` archives.
 * Extracts `collection.anki2` or `collection.anki21` into `cacheDir` using an 8 KB buffer,
 * validates against Zip Slip path traversal, reads only the `notes` table (`flds`, `tags`),
 * ignores Anki `cards` scheduling so TechEnglish SM-2 governs review intervals,
 * and deterministically deletes the temporary SQLite file in `finally`.
 */
@Singleton
class AnkiApkgExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun extractNoteRows(apkgStream: InputStream): List<Pair<String, String>> {
        val tempDbFile = File.createTempFile("anki_import_", ".sqlite", context.cacheDir)
        try {
            var foundCollection = false
            ZipInputStream(apkgStream).use { zis ->
                val buffer = ByteArray(8 * 1024)
                var entry = zis.nextEntry
                while (entry != null) {
                    val cleanName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    if (!entry.isDirectory &&
                        !entry.name.contains("..") &&
                        (cleanName == "collection.anki2" || cleanName == "collection.anki21")
                    ) {
                        FileOutputStream(tempDbFile).use { fos ->
                            var read = zis.read(buffer)
                            while (read > 0) {
                                fos.write(buffer, 0, read)
                                read = zis.read(buffer)
                            }
                        }
                        foundCollection = true
                        // Prefer collection.anki21 if encountered, otherwise keep collection.anki2
                        if (cleanName == "collection.anki21") break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (!foundCollection || tempDbFile.length() == 0L) {
                throw IllegalArgumentException(
                    "Архив .apkg не содержит базу SQLite collection.anki2 / collection.anki21."
                )
            }

            val rows = mutableListOf<Pair<String, String>>()
            SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                db.rawQuery("SELECT flds, tags FROM notes", null).use { cursor ->
                    val fldsIdx = cursor.getColumnIndex("flds")
                    val tagsIdx = cursor.getColumnIndex("tags")
                    while (cursor.moveToNext()) {
                        val flds = if (fldsIdx >= 0) cursor.getString(fldsIdx).orEmpty() else ""
                        val tags = if (tagsIdx >= 0) cursor.getString(tagsIdx).orEmpty() else ""
                        if (flds.isNotBlank()) {
                            rows.add(flds to tags)
                        }
                    }
                }
            }
            return rows
        } finally {
            runCatching { tempDbFile.delete() }
        }
    }
}
