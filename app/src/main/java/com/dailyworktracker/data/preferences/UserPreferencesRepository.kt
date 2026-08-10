package com.dailyworktracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * Persists user preferences and the Excel file URI across sessions.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val EXCEL_FILE_URI       = stringPreferencesKey("excel_file_uri")
        val EXCEL_LAST_UPDATED   = longPreferencesKey("excel_last_updated")
        val THEME_MODE           = stringPreferencesKey("theme_mode") // LIGHT | DARK | SYSTEM
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR    = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE  = intPreferencesKey("notification_minute")
        val CUSTOM_CATEGORIES    = stringPreferencesKey("custom_categories") // JSON array
    }

    // ─── Excel URI ────────────────────────────────────────────────────────────

    val excelFileUri: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.EXCEL_FILE_URI] }

    suspend fun saveExcelFileUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EXCEL_FILE_URI] = uri
            prefs[Keys.EXCEL_LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun clearExcelFileUri() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.EXCEL_FILE_URI)
            prefs.remove(Keys.EXCEL_LAST_UPDATED)
        }
    }

    val excelLastUpdated: Flow<Long> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.EXCEL_LAST_UPDATED] ?: 0L }

    // ─── Theme ────────────────────────────────────────────────────────────────

    val themeMode: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.THEME_MODE] ?: "SYSTEM" }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.NOTIFICATIONS_ENABLED] ?: false }

    val notificationHour: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.NOTIFICATION_HOUR] ?: 20 } // default 8 PM

    val notificationMinute: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.NOTIFICATION_MINUTE] ?: 0 }

    suspend fun saveNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
            prefs[Keys.NOTIFICATION_HOUR]     = hour
            prefs[Keys.NOTIFICATION_MINUTE]   = minute
        }
    }

    // ─── Custom Categories ────────────────────────────────────────────────────

    val customCategoriesJson: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CUSTOM_CATEGORIES] ?: "[]" }

    suspend fun saveCustomCategoriesJson(json: String) {
        context.dataStore.edit { it[Keys.CUSTOM_CATEGORIES] = json }
    }
}
