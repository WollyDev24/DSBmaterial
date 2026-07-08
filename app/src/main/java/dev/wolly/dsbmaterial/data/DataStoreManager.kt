package dev.wolly.dsbmaterial.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val CLASS_NAME = stringPreferencesKey("class_name")
        val SWAP_DATA = booleanPreferencesKey("swap_data")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SORT_PERIOD = booleanPreferencesKey("sort_period")
        val ARCHIVE = stringPreferencesKey("archive")
        val THEME_INDEX = androidx.datastore.preferences.core.intPreferencesKey("theme_index")
        val NAV_HIDDEN = booleanPreferencesKey("nav_hidden")
    }

    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[USERNAME] }
    val passwordFlow: Flow<String?> = context.dataStore.data.map { it[PASSWORD] }
    val classNameFlow: Flow<String?> = context.dataStore.data.map { it[CLASS_NAME] }
    val swapDataFlow: Flow<Boolean> = context.dataStore.data.map { it[SWAP_DATA] ?: true }
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR] ?: true }
    val sortPeriodFlow: Flow<Boolean> = context.dataStore.data.map { it[SORT_PERIOD] ?: true }
    val archiveFlow: Flow<String?> = context.dataStore.data.map { it[ARCHIVE] }
    val themeIndexFlow: Flow<Int> = context.dataStore.data.map { it[THEME_INDEX] ?: 0 }
    val navHiddenFlow: Flow<Boolean> = context.dataStore.data.map { it[NAV_HIDDEN] ?: false }

    suspend fun saveCredentials(username: String, password: String, className: String) {
        context.dataStore.edit { settings ->
            settings[USERNAME] = username
            settings[PASSWORD] = password
            settings[CLASS_NAME] = className
        }
    }

    suspend fun saveSwapPreference(isRoomFirst: Boolean) {
        context.dataStore.edit { it[SWAP_DATA] = isRoomFirst }
    }

    suspend fun saveDynamicColorPreference(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun saveThemeIndex(index: Int) {
        context.dataStore.edit { it[THEME_INDEX] = index }
    }

    suspend fun saveSortPreference(enabled: Boolean) {
        context.dataStore.edit { it[SORT_PERIOD] = enabled }
    }

    suspend fun saveArchive(json: String) {
        context.dataStore.edit { it[ARCHIVE] = json }
    }
    
    suspend fun saveNavHiddenPreference(hidden: Boolean) {
        context.dataStore.edit { it[NAV_HIDDEN] = hidden }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { it.clear() }
    }
}
