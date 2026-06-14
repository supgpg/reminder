package com.reminder.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.personaDataStore by preferencesDataStore(name = "persona_prefs")

class PersonaPreferences(private val context: Context) {

    private val spiceLevelKey = stringPreferencesKey("spice_level")
    private val calendarSyncEnabledKey = booleanPreferencesKey("calendar_sync_enabled")

    val spiceLevel: Flow<SpiceLevel> = context.personaDataStore.data.map { prefs ->
        val raw = prefs[spiceLevelKey] ?: SpiceLevel.SPICY.name
        runCatching { SpiceLevel.valueOf(raw) }.getOrDefault(SpiceLevel.SPICY)
    }

    suspend fun setSpiceLevel(level: SpiceLevel) {
        context.personaDataStore.edit { prefs ->
            prefs[spiceLevelKey] = level.name
        }
    }

    val calendarSyncEnabled: Flow<Boolean> = context.personaDataStore.data.map { prefs ->
        prefs[calendarSyncEnabledKey] ?: false
    }

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        context.personaDataStore.edit { prefs ->
            prefs[calendarSyncEnabledKey] = enabled
        }
    }
}
