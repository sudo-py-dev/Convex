package com.convex.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.convex.app.domain.model.AppLanguage
import com.convex.app.domain.model.SessionRecord
import com.convex.app.domain.model.SessionStatus
import com.convex.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "convex_prefs")

@Singleton
class AppPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val store = context.dataStore

        companion object {
            private val KEY_THEME = stringPreferencesKey("theme_mode")
            private val KEY_LANGUAGE = stringPreferencesKey("app_language")
            private val KEY_HISTORY = stringPreferencesKey("session_history")
            private val KEY_TECHNICAL_MODE = booleanPreferencesKey("technical_mode")
            private const val MAX_HISTORY = 100
        }

        val themeMode: Flow<ThemeMode> =
            store.data.map { prefs ->
                runCatching { ThemeMode.valueOf(prefs[KEY_THEME] ?: "") }.getOrDefault(ThemeMode.SYSTEM)
            }

        val language: Flow<AppLanguage> =
            store.data.map { prefs ->
                runCatching { AppLanguage.valueOf(prefs[KEY_LANGUAGE] ?: "") }.getOrDefault(AppLanguage.SYSTEM)
            }

        val technicalMode: Flow<Boolean> =
            store.data.map { prefs ->
                prefs[KEY_TECHNICAL_MODE] ?: false
            }

        val sessionHistory: Flow<List<SessionRecord>> =
            store.data.map { prefs ->
                val raw = prefs[KEY_HISTORY] ?: return@map emptyList()
                runCatching { Json.decodeFromString<List<SessionRecordDto>>(raw).map { it.toDomain() } }
                    .getOrDefault(emptyList())
            }

        suspend fun setThemeMode(mode: ThemeMode) {
            store.edit { it[KEY_THEME] = mode.name }
        }

        suspend fun setLanguage(lang: AppLanguage) {
            store.edit { it[KEY_LANGUAGE] = lang.name }
        }

        suspend fun setTechnicalMode(enabled: Boolean) {
            store.edit { it[KEY_TECHNICAL_MODE] = enabled }
        }

        suspend fun addSession(record: SessionRecord) {
            store.edit { prefs ->
                val current =
                    runCatching {
                        val raw = prefs[KEY_HISTORY] ?: "[]"
                        Json.decodeFromString<List<SessionRecordDto>>(raw)
                    }.getOrDefault(emptyList())

                val updated = (listOf(SessionRecordDto.fromDomain(record)) + current).take(MAX_HISTORY)
                prefs[KEY_HISTORY] = Json.encodeToString(updated)
            }
        }

        suspend fun clearHistory() {
            store.edit { it[KEY_HISTORY] = "[]" }
        }
    }

@kotlinx.serialization.Serializable
private data class SessionRecordDto(
    val id: String,
    val categoryId: String = "",
    val operationId: String = "",
    val operationLabel: String,
    val command: String,
    val outputPath: String,
    val status: String,
    val timestampUtc: Long,
    val durationMs: Long,
) {
    fun toDomain() =
        SessionRecord(
            id = id,
            categoryId = categoryId,
            operationId = operationId,
            operationLabel = operationLabel,
            command = command,
            outputPath = outputPath,
            status = runCatching { SessionStatus.valueOf(status) }.getOrDefault(SessionStatus.ERROR),
            timestampUtc = timestampUtc,
            durationMs = durationMs,
        )

    companion object {
        fun fromDomain(r: SessionRecord) =
            SessionRecordDto(
                id = r.id,
                categoryId = r.categoryId,
                operationId = r.operationId,
                operationLabel = r.operationLabel,
                command = r.command,
                outputPath = r.outputPath,
                status = r.status.name,
                timestampUtc = r.timestampUtc,
                durationMs = r.durationMs,
            )
    }
}
